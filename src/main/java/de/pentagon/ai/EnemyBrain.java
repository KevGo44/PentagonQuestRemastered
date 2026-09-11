package de.pentagon.ai;

import com.jme3.math.*;
import com.jme3.scene.Spatial;
import de.pentagon.combat.CombatRules;
import de.pentagon.entities.Enemy;
import de.pentagon.world.DungeonLayout;
import java.util.function.*;

/** Per-enemy finite state machine. Attacks lock their aim during a readable wind-up. */
public final class EnemyBrain {
  public interface Attacks {
    void melee(Enemy enemy);

    void cast(Enemy enemy);

    void shockwave(Enemy enemy);

    void phaseChanged(Enemy enemy);
  }

  public void update(
      Enemy e,
      float dt,
      float time,
      Vector3f player,
      DungeonLayout layout,
      BiPredicate<Vector3f, Vector3f> visible,
      Attacks attacks) {
    if (!e.alive()) {
      e.deathTime += dt;
      if (e.deathTime > 1.2f) e.rig.composer().setGlobalSpeed(0);
      return;
    }
    float distance = e.position().distance(player);
    e.cooldown = Math.max(0, e.cooldown - dt);
    e.pathTimer -= dt;
    e.timer -= dt;
    if (e.type == EnemyType.KING) {
      int next = CombatRules.bossPhase(e.health / e.maxHealth);
      if (next > e.phase) {
        e.phase = next;
        e.stun(1.4f);
        attacks.phaseChanged(e);
      }
    }
    if (e.state == Enemy.State.STUNNED) {
      e.stop();
      if (e.timer <= 0) e.state = Enemy.State.CHASE;
      return;
    }
    if (distance > 33) {
      e.stop();
      e.rig.play("Idle");
      return;
    }
    boolean sees = distance < 20 && visible.test(e.position(), player);
    switch (e.state) {
      case PATROL -> {
        e.rig.play("Walk");
        Vector3f patrol =
            e.home.add(
                FastMath.sin(time * .28f + e.home.x) * 2.2f,
                0,
                FastMath.cos(time * .28f + e.home.z) * 2.2f);
        walk(e, patrol, layout, .65f);
        if (sees && distance < (e.type == EnemyType.KING ? 17 : 13)) {
          e.state = Enemy.State.ALERT;
          e.timer = .6f;
          e.stop();
        }
      }
      case ALERT -> {
        e.face(player);
        e.rig.play("Idle");
        if (e.timer <= 0) e.state = Enemy.State.CHASE;
      }
      case CHASE -> {
        if (distance > 26) {
          e.state = Enemy.State.PATROL;
          break;
        }
        if (e.type == EnemyType.GOBLIN && e.health < e.maxHealth * .23f && e.cooldown <= 0) {
          e.state = Enemy.State.FLEE;
          e.timer = 2.2f;
          break;
        }
        float range = e.type.reach;
        if (e.type == EnemyType.SHAMAN && distance < 5) {
          e.state = Enemy.State.FLEE;
          e.timer = 1.2f;
          break;
        }
        if (distance < range && sees && e.cooldown <= 0) {
          e.stop();
          e.face(player);
          e.aim.set(player);
          e.state = Enemy.State.ATTACK;
          e.timer = windup(e);
          e.hitEmitted = false;
          e.attackUnblockable = e.type == EnemyType.KING && e.phase >= 2 && e.attackCount % 3 == 2;
          e.telegraph.setLocalScale(
              e.attackUnblockable ? 6 : e.type.reach,
              .025f,
              e.attackUnblockable ? 6 : e.type.reach);
          e.telegraph.setCullHint(Spatial.CullHint.Inherit);
          e.rig.restart(e.type == EnemyType.SHAMAN ? "Cast" : "Attack3");
        } else {
          walk(e, player, layout, 1 + (e.phase - 1) * .10f);
          e.rig.play("Run");
        }
      }
      case ATTACK -> {
        e.stop();
        e.telegraph.setLocalTranslation(0, .05f + .04f * FastMath.sin(time * 24), 0);
        if (e.timer <= 0 && !e.hitEmitted) {
          e.hitEmitted = true;
          e.attackCount++;
          if (e.attackUnblockable) attacks.shockwave(e);
          else if (e.type == EnemyType.SHAMAN) attacks.cast(e);
          else attacks.melee(e);
          // Phase three adds a ranged follow-up. All attacks remain telegraphed.
          if (e.type == EnemyType.KING && e.phase == 3 && e.attackCount % 2 == 0) attacks.cast(e);
          e.telegraph.setCullHint(Spatial.CullHint.Always);
          if (e.state != Enemy.State.STUNNED) {
            e.state = Enemy.State.RECOVER;
            e.timer = e.type == EnemyType.KING ? .85f : .65f;
          }
          e.cooldown = e.type == EnemyType.SHAMAN ? 2.2f : 1.1f;
        }
      }
      case RECOVER -> {
        e.stop();
        e.rig.play("Idle");
        if (e.timer <= 0) e.state = Enemy.State.CHASE;
      }
      case FLEE -> {
        Vector3f away = e.position().subtract(player);
        away.y = 0;
        away.normalizeLocal();
        walk(e, e.position().add(away.mult(5)), layout, 1.15f);
        e.rig.play("Run");
        if (e.timer <= 0) {
          e.state = Enemy.State.CHASE;
          e.cooldown = 3;
        }
      }
      default -> {}
    }
  }

  private float windup(Enemy e) {
    return e.type.windup - (e.type == EnemyType.KING ? (e.phase - 1) * .12f : 0);
  }

  private void walk(Enemy e, Vector3f goal, DungeonLayout layout, float multiplier) {
    Vector3f target = goal;
    if (!layout.clearLine(e.position().x, e.position().z, goal.x, goal.z)) {
      if (e.pathTimer <= 0) {
        e.path = layout.path(e.position().x, e.position().z, goal.x, goal.z);
        e.pathIndex = 0;
        e.pathTimer = .6f;
      }
      if (e.pathIndex >= e.path.size()) {
        e.stop();
        return;
      }
      int cell = e.path.get(e.pathIndex);
      target =
          new Vector3f(
              cell % DungeonLayout.SIZE * DungeonLayout.CELL,
              0,
              cell / DungeonLayout.SIZE * DungeonLayout.CELL);
      Vector3f flat = e.position().clone();
      flat.y = 0;
      if (flat.distanceSquared(target) < .7f && e.pathIndex < e.path.size() - 1) e.pathIndex++;
    }
    Vector3f move = target.subtract(e.position());
    move.y = 0;
    if (move.lengthSquared() < .15f) {
      e.stop();
      return;
    }
    move.normalizeLocal();
    Vector3f ahead = e.position().add(move.mult(.8f));
    if (!layout.walkable(ahead.x, ahead.z)) {
      e.stop();
      return;
    }
    e.face(target);
    e.body.setWalkDirection(move.mult(e.type.speed * multiplier));
  }
}
