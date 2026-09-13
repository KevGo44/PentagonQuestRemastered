package de.pentagon.combat;

import com.jme3.collision.CollisionResults;
import com.jme3.math.*;
import com.jme3.scene.Spatial;
import de.pentagon.ai.*;
import de.pentagon.audio.AudioDirector;
import de.pentagon.core.GameSession;
import de.pentagon.entities.*;
import de.pentagon.physics.PhysicsWorld;
import de.pentagon.world.*;
import java.util.*;
import java.util.function.Consumer;

public final class CombatSystem implements EnemyBrain.Attacks {
  private final PlayerController player;
  private final GameSession session;
  private final WorldView world;
  private final List<Enemy> enemies;
  private final Effects effects;
  private final ProjectileSystem projectiles;
  private final AudioDirector audio;
  private final AtmosphereFilter atmosphere;
  private final PhysicsWorld physics;
  private final Consumer<String> notice;
  private final EnemyBrain brain = new EnemyBrain();
  private float damageGrace;
  private int riposte;

  /** Every fourth defeated enemy carries a potion; it was every third, with five to start. */
  public static final int POTION_DROP_EVERY = 4;

  public CombatSystem(
      PlayerController player,
      GameSession session,
      WorldView world,
      List<Enemy> enemies,
      Effects effects,
      ProjectileSystem projectiles,
      AudioDirector audio,
      AtmosphereFilter atmosphere,
      PhysicsWorld physics,
      Consumer<String> notice) {
    this.player = player;
    this.session = session;
    this.world = world;
    this.enemies = enemies;
    this.effects = effects;
    this.projectiles = projectiles;
    this.audio = audio;
    this.atmosphere = atmosphere;
    this.physics = physics;
    this.notice = notice;
  }

  public void update(float dt, float time) {
    damageGrace = Math.max(0, damageGrace - dt);
    if (player.attack.update(dt)) {
      int count = 0;
      for (Enemy enemy : enemies)
        if (enemy.alive()) {
          Vector3f d = enemy.position().subtract(player.node.getWorldTranslation());
          // Attack2 is a full spin (art/blender/anim_polish.py): the blade passes every side, so
          // it reaches all round; the finisher sweeps wide, the opener hits what is in front.
          if (CombatRules.inArc(
                  d.x,
                  d.z,
                  player.facing.x,
                  player.facing.z,
                  2.6f + enemy.type.scale * .3f,
                  player.attack.combo() == 1 ? -1f : player.attack.combo() == 2 ? -.15f : .30f)
              && Math.abs(d.y) < 2
              && visible(player.node.getWorldTranslation(), enemy.position())) {
            float damage = session.player.damage(session.inventory) * player.attack.multiplier();
            if (enemy.type == EnemyType.WARDEN
                && enemy.state != Enemy.State.STUNNED
                && enemy.state != Enemy.State.ATTACK) damage *= .55f;
            // The riposte: the first blow on an enemy the last parry left stunned.
            if (enemy.state == Enemy.State.STUNNED && player.riposteReady()) {
              damage *= PlayerController.RIPOSTE_MULTIPLIER;
              player.riposteSpent();
              riposte = Math.round(damage);
              effects.burst(enemy.position().add(0, 1.2f, 0), 30);
            }
            damageEnemy(enemy, damage);
            count++;
          }
        }
      if (riposte > 0) {
        notice.accept("RIPOSTE  +" + riposte);
        riposte = 0;
      } else if (count > 0)
        notice.accept(
            player.attack.combo() == 2
                ? "KOMBO  III  -  Brechender Hieb"
                : "Treffer  +"
                    + Math.round(
                        session.player.damage(session.inventory) * player.attack.multiplier()));
    }
    if (player.attack.readyForNext() && session.player.spend(18)) {
      player.attack.next();
      player.rig.restart("Attack" + (player.attack.combo() + 1));
      audio.play("swing");
      player.regenDelay = .7f;
    } else player.attack.finishIfExpired();
    for (Enemy enemy : enemies) {
      brain.update(
          enemy, dt, time, player.node.getWorldTranslation(), world.layout, this::visible, this);
      if (!enemy.alive() && enemy.deathTime > 4 && enemy.node.getParent() != null)
        enemy.cleanup(physics);
    }
    projectiles.update(
        dt,
        world.occluders,
        enemies,
        player.node.getWorldTranslation(),
        this::damageEnemy,
        (source, damage) -> hurt(damage, source, false, null),
        effects);
  }

  public boolean visible(Vector3f a, Vector3f b) {
    if (!world.layout.clearLine(a.x, a.z, b.x, b.z)) return false;
    Vector3f from = a.add(0, 1, 0), to = b.add(0, 1, 0), delta = to.subtract(from);
    float distance = delta.length();
    if (distance < .01f) return true;
    Ray ray = new Ray(from, delta.divide(distance));
    ray.setLimit(distance);
    CollisionResults results = new CollisionResults();
    world.occluders.collideWith(ray, results);
    return results.size() == 0;
  }

  public void castPlayer() {
    Vector3f from =
        player.node.getWorldTranslation().add(0, 1.2f, 0).addLocal(player.facing.mult(.8f));
    projectiles.shoot(from, player.facing, true, session.player.spellDamage());
    audio.play("spell");
  }

  public void damageEnemy(Enemy enemy, float damage) {
    if (!enemy.alive()) return;
    enemy.health = Math.max(0, enemy.health - damage);
    audio.play("hit");
    effects.burst(enemy.position().add(0, 1, 0), 12);
    if (enemy.health <= 0) {
      enemy.state = Enemy.State.DEAD;
      enemy.stop();
      // Played once: the clip ends on the floor and stays there. Looping it and freezing the
      // composer after a fixed 1.2 s left every corpse standing bent over, because the delivered
      // Death clip is 2.3 s long and its hips reach the ground at 1.7 s.
      enemy.rig.once("Death");
      enemy.telegraph.setCullHint(Spatial.CullHint.Always);
      physics.space().remove(enemy.body);
      enemy.removed = true;
      session.defeated.add(enemy.id);
      session.enemies.remove(enemy.id);
      session.event("kill_" + enemy.type.name());
      int levels = session.player.gainXp(enemy.type.xp);
      int gold = enemy.type == EnemyType.KING ? 100 : 8 + enemy.type.ordinal() * 5;
      session.player.gold += gold;
      if (Math.floorMod(enemy.id.hashCode(), POTION_DROP_EVERY) == 0)
        session.inventory.add("potion", 1);
      notice.accept(enemy.type.title + " besiegt  +" + enemy.type.xp + " EP  +" + gold + " Gold");
      if (levels > 0) {
        notice.accept("STUFE " + session.player.level + "  -  Fähigkeitspunkt erhalten [K]");
        audio.play("chime");
      }
      // The keeper comes back with the rest of his level (GameSession.leave); the key does not.
      if (enemy.id.equals("PRISON_keeper") && session.inventory.count("prison_key") == 0) {
        session.inventory.add("prison_key", 1);
        notice.accept("Schlüssel des Kerkermeisters erhalten.");
      }
      if (enemy.type == EnemyType.KING) {
        session.flags.add("king_dead");
        session.inventory.add("crown", 1);
        notice.accept("Die Krone faellt. Dein Urteil steht noch aus.");
        audio.play("chime");
      }
    } else if (enemy.type != EnemyType.KING
        && (enemy.type != EnemyType.WARDEN || player.attack.combo() == 2)) enemy.stun(.28f);
    else if (enemy.state == Enemy.State.PATROL || enemy.state == Enemy.State.ALERT)
      enemy.state = Enemy.State.CHASE;
  }

  public void hurt(float raw, Vector3f source, boolean unblockable, Enemy attacker) {
    if (session.player.health <= 0 || damageGrace > 0 || player.invulnerable()) return;
    Vector3f delta = source.subtract(player.node.getWorldTranslation());
    delta.y = 0;
    boolean front = delta.lengthSquared() < .01f || delta.normalizeLocal().dot(player.facing) > .1f;
    CombatRules.Hit hit =
        CombatRules.defend(
            raw,
            session.inventory.armor().power(),
            player.blocking,
            front,
            player.blockAge,
            session.player.stamina,
            unblockable);
    session.player.stamina = Math.max(0, session.player.stamina - hit.staminaCost());
    session.player.health = Math.max(0, session.player.health - hit.healthDamage());
    player.regenDelay = .9f;
    if (hit.parried()) {
      audio.play("parry");
      effects.burst(player.node.getWorldTranslation().add(0, 1.2f, 0), 24);
      player.parry();
      if (attacker != null) attacker.stun(PlayerController.PARRY_STUN);
      notice.accept(
          attacker != null ? "PARIERT  -  Riposte offen" : "PARIERT  -  Zauber abgewehrt");
    } else if (hit.healthDamage() > 2) {
      audio.play("hurt");
      atmosphere.hit();
      player.hitLeft = hit.guardBroken() ? .8f : .23f;
      player.attack.cancel();
      if (hit.guardBroken()) {
        player.blocking = false;
        notice.accept("Deckung gebrochen!");
      }
      damageGrace = .23f;
    } else audio.play("parry");
  }

  @Override
  public void melee(Enemy enemy) {
    Vector3f d = player.node.getWorldTranslation().subtract(enemy.position());
    if (CombatRules.inArc(d.x, d.z, enemy.heading.x, enemy.heading.z, enemy.type.reach + .35f, .25f)
        && Math.abs(d.y) < 2.1f
        && visible(enemy.position(), player.node.getWorldTranslation()))
      hurt(enemy.type.damage * (1 + (enemy.phase - 1) * .18f), enemy.position(), false, enemy);
    audio.play("swing");
  }

  @Override
  public void cast(Enemy enemy) {
    Vector3f origin = enemy.position().add(0, 1.2f * enemy.type.scale, 0);
    Vector3f aim = enemy.aim.add(0, 1, 0).subtract(origin).normalizeLocal();
    projectiles.shoot(origin.add(aim.mult(.8f)), aim, false, enemy.type.damage);
    audio.play("spell");
  }

  @Override
  public void shockwave(Enemy enemy) {
    effects.burst(enemy.position().add(0, .5f, 0), 45);
    audio.play("hit");
    Vector3f p = player.node.getWorldTranslation();
    if (enemy.position().distance(p) < 6 && p.y < .75f && visible(enemy.position(), p))
      hurt(enemy.type.damage * 1.3f, enemy.position(), true, enemy);
    notice.accept("ASCHENWELLE  -  Springen oder ausweichen!");
  }

  @Override
  public void phaseChanged(Enemy enemy) {
    notice.accept(
        "ORK-KÖNIG  -  PHASE "
            + enemy.phase
            + (enemy.phase == 2 ? "  |  Die Erde bebt" : "  |  Die Krone brennt"));
    effects.burst(enemy.position().add(0, 2, 0), 45);
    audio.play("spell");
  }

  public boolean inCombat() {
    return enemies.stream()
        .anyMatch(
            e ->
                e.alive()
                    && e.state != Enemy.State.PATROL
                    && e.position().distanceSquared(player.node.getWorldTranslation()) < 27 * 27);
  }

  public Enemy nearest() {
    return enemies.stream()
        .filter(Enemy::alive)
        .filter(
            e ->
                e.position().distanceSquared(player.node.getWorldTranslation()) < 17 * 17
                    && visible(player.node.getWorldTranslation(), e.position()))
        .min(
            Comparator.comparingDouble(
                e -> e.position().distanceSquared(player.node.getWorldTranslation())))
        .orElse(null);
  }
}
