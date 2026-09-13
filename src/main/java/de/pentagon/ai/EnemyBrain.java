package de.pentagon.ai;

import com.jme3.math.*;
import com.jme3.scene.Spatial;
import de.pentagon.combat.AttackTimeline;
import de.pentagon.combat.CombatRules;
import de.pentagon.entities.Enemy;
import de.pentagon.world.DungeonLayout;
import java.util.function.*;

/** Per-enemy finite state machine. Attacks lock their aim during a readable wind-up. */
public final class EnemyBrain {
  /**
   * Seconds into the attack clip at which the blow lands, so the swing can be started that long
   * before the damage. Attack3 shares the player's timeline (its blade comes down at 0.30-0.37 s,
   * art/probe/ClipTimeline); the 0.6 s Cast has its hand up at 0.2 s.
   */
  public static final float MELEE_STRIKE = AttackTimeline.WINDUP[2], CAST_RELEASE = .2f;

  /**
   * Pacing. Enemies notice the player from further away, swing again sooner and settle faster after
   * a swing than they did; together with the raised EnemyType values this is the "more demanding"
   * combat Kevin asked for. The king keeps his own, slower rhythm because every one of his blows is
   * telegraphed for longer.
   */
  public static final float AGGRO_RANGE = 16,
      KING_AGGRO_RANGE = 20,
      COOLDOWN = .9f,
      SHAMAN_COOLDOWN = 1.8f,
      KING_COOLDOWN = 1.1f,
      RECOVER = .55f,
      KING_RECOVER = .8f,
      FLEE_DISTANCE = 5,
      HOLD_GROUND = 2.5f;

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
      // Death runs once and holds its last frame on its own; nothing to freeze here.
      e.deathTime += dt;
      return;
    }
    float distance = e.position().distance(player);
    e.cooldown = Math.max(0, e.cooldown - dt);
    e.pathTimer -= dt;
    e.timer -= dt;
    e.holdGround = Math.max(0, e.holdGround - dt);
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
        if (sees && distance < (e.type == EnemyType.KING ? KING_AGGRO_RANGE : AGGRO_RANGE)) {
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
        if (e.type == EnemyType.SHAMAN && distance < FLEE_DISTANCE && e.holdGround <= 0) {
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
          // The swing itself starts later, timed so that its strike frame lands when the damage
          // does; until then the enemy holds still over its telegraph.
          e.swung = false;
          e.rig.play("Idle");
        } else {
          walk(e, player, layout, 1 + (e.phase - 1) * .10f);
          e.rig.play("Run");
        }
      }
      case ATTACK -> {
        e.stop();
        e.telegraph.setLocalTranslation(0, .05f + .04f * FastMath.sin(time * 24), 0);
        float strike = e.type == EnemyType.SHAMAN ? CAST_RELEASE : MELEE_STRIKE;
        if (!e.swung && e.timer <= strike) {
          e.swung = true;
          e.rig.once(e.type == EnemyType.SHAMAN ? "Cast" : "Attack3");
          // A wind-up shorter than the clip's own run-up starts inside the clip instead.
          if (e.timer < strike) e.rig.composer().setTime(strike - e.timer);
        }
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
            e.timer = e.type == EnemyType.KING ? KING_RECOVER : RECOVER;
          }
          e.cooldown =
              e.type == EnemyType.SHAMAN
                  ? SHAMAN_COOLDOWN
                  : e.type == EnemyType.KING ? KING_COOLDOWN : COOLDOWN;
        }
      }
      case RECOVER -> {
        e.stop();
        // The follow-through of the swing plays out before the enemy settles back into Idle.
        if (e.rig.finished()) e.rig.play("Idle");
        if (e.timer <= 0) e.state = Enemy.State.CHASE;
      }
      case FLEE -> {
        Vector3f goal = fleeGoal(e.position(), player, layout);
        if (goal == null) {
          // Cornered. Turn and fight: no more fleeing for a while, and the next cast comes at
          // once - a caster with his back to the wall is dangerous, not helpless.
          e.holdGround = HOLD_GROUND;
          e.cooldown = Math.min(e.cooldown, .3f);
          e.state = Enemy.State.CHASE;
          e.stop();
          e.face(player);
          e.rig.play("Idle");
          break;
        }
        boolean moving = walk(e, goal, layout, 1.15f);
        // Run only while actually moving; a blocked step used to play Run against the wall.
        e.rig.play(moving ? "Run" : "Idle");
        if (e.timer <= 0) {
          e.state = Enemy.State.CHASE;
          e.cooldown = e.type == EnemyType.SHAMAN ? .6f : 3;
        }
      }
      default -> {}
    }
  }

  /**
   * Where to run: the first direction, from straight away from the player through ever wider
   * angles, whose five-metre point is floor, is reachable in a straight line and is further from
   * the player than the enemy stands now. Null when no such point exists, which is what a corner
   * looks like on the grid.
   */
  public static Vector3f fleeGoal(Vector3f from, Vector3f player, DungeonLayout layout) {
    Vector3f away = from.subtract(player);
    away.y = 0;
    if (away.lengthSquared() < .01f) away.set(0, 0, 1);
    away.normalizeLocal();
    float now = from.distance(player);
    for (float reach : new float[] {FLEE_DISTANCE, FLEE_DISTANCE * .5f})
      for (float degrees : new float[] {0, 35, -35, 70, -70, 110, -110}) {
        Vector3f direction =
            new Quaternion()
                .fromAngleAxis(degrees * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y)
                .mult(away);
        Vector3f goal = from.add(direction.mult(reach));
        goal.y = 0;
        if (layout.walkable(goal.x, goal.z)
            && layout.clearLine(from.x, from.z, goal.x, goal.z)
            && goal.distance(new Vector3f(player.x, 0, player.z)) > now + reach * .3f) return goal;
      }
    return null;
  }

  private float windup(Enemy e) {
    return e.type.windup - (e.type == EnemyType.KING ? (e.phase - 1) * .12f : 0);
  }

  /** Steers towards {@code goal}; true while the enemy is actually moving this frame. */
  private boolean walk(Enemy e, Vector3f goal, DungeonLayout layout, float multiplier) {
    Vector3f target = goal;
    if (!layout.clearLine(e.position().x, e.position().z, goal.x, goal.z)) {
      if (e.pathTimer <= 0) {
        e.path = layout.path(e.position().x, e.position().z, goal.x, goal.z);
        e.pathIndex = 0;
        e.pathTimer = .6f;
      }
      if (e.pathIndex >= e.path.size()) {
        e.stop();
        return false;
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
      return false;
    }
    move.normalizeLocal();
    Vector3f ahead = e.position().add(move.mult(.8f));
    if (!layout.walkable(ahead.x, ahead.z)) {
      e.stop();
      return false;
    }
    e.face(target);
    e.body.setWalkDirection(move.mult(e.type.speed * multiplier));
    return true;
  }
}
