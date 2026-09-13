package de.pentagon.entities;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.*;
import com.jme3.scene.*;
import de.pentagon.ai.EnemyType;
import de.pentagon.assets.*;
import de.pentagon.core.GameSession;
import de.pentagon.physics.PhysicsWorld;
import de.pentagon.world.DungeonLayout;
import java.util.*;

public final class Enemy {
  public enum State {
    PATROL,
    ALERT,
    CHASE,
    ATTACK,
    RECOVER,
    FLEE,
    STUNNED,
    DEAD
  }

  public final String id;
  public final EnemyType type;
  public final Node node = new Node();
  public final CharacterFactory.Rig rig;
  public final BetterCharacterControl body;
  public final Vector3f home, heading = new Vector3f(0, 0, 1), aim = new Vector3f();
  public final Geometry telegraph;
  public float health, maxHealth, timer, cooldown, pathTimer, deathTime;

  /**
   * Seconds a fleeing type stands its ground instead of fleeing again. Set when no way out exists:
   * an Aschenrufer backed into a corner used to run against the wall for ever, because "flee" meant
   * "walk five metres away from the player" whether or not that point was inside the room.
   */
  public float holdGround;

  public int phase = 1, attackCount, pathIndex;
  public boolean hitEmitted, removed, attackUnblockable, swung;
  public State state = State.PATROL;
  public List<Integer> path = List.of();

  public Enemy(
      DungeonLayout.Spawn spawn, AssetPipeline assets, PhysicsWorld physics, GameSession session) {
    this(spawn, assets, physics, session, de.pentagon.combat.Difficulty.EASY);
  }

  public Enemy(
      DungeonLayout.Spawn spawn,
      AssetPipeline assets,
      PhysicsWorld physics,
      GameSession session,
      de.pentagon.combat.Difficulty difficulty) {
    id = spawn.id();
    type = spawn.type();
    maxHealth =
        Math.round(
            type.hp
                * (type == EnemyType.KING && session.flag("prisoners_freed") ? .8f : 1)
                * difficulty.enemyHealth);
    health = maxHealth;
    GameSession.EnemySave saved = session.enemies.get(id);
    home = new Vector3f(spawn.x(), 0, spawn.z());
    Vector3f at = saved != null ? new Vector3f(saved.x(), .1f, saved.z()) : home.add(0, .1f, 0);
    if (saved != null) health = Math.min(maxHealth, saved.health());
    rig =
        new CharacterFactory(assets)
            .create(type.name().toLowerCase(Locale.ROOT), type.color, type == EnemyType.KING);
    rig.root().setLocalScale(type.scale);
    node.setName(id);
    node.attachChild(rig.root());
    body = new BetterCharacterControl(.38f * type.scale, 1.85f * type.scale, 80 * type.scale);
    node.addControl(body);
    physics.space().add(body);
    body.warp(at);
    telegraph = assets.sphere("AttackTell", 1, assets.flat(0xe47b4e, .22f));
    telegraph.setLocalScale(type.reach, .025f, type.reach);
    telegraph.setLocalTranslation(0, .04f, 0);
    telegraph.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
    telegraph.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
    telegraph.setCullHint(Spatial.CullHint.Always);
    node.attachChild(telegraph);
  }

  public Vector3f position() {
    return node.getWorldTranslation();
  }

  public boolean alive() {
    return state != State.DEAD;
  }

  public void stop() {
    body.setWalkDirection(Vector3f.ZERO);
  }

  public void face(Vector3f toward) {
    heading.set(toward).subtractLocal(position());
    heading.y = 0;
    if (heading.lengthSquared() > .01f) {
      heading.normalizeLocal();
      body.setViewDirection(heading);
    }
  }

  public void stun(float duration) {
    if (!alive()) return;
    state = State.STUNNED;
    timer = duration;
    telegraph.setCullHint(Spatial.CullHint.Always);
    stop();
    // Once, not looped: a 1.7 s parry stun used to replay the 0.7 s flinch two and a half times.
    rig.once("Hit");
  }

  public void cleanup(PhysicsWorld physics) {
    if (!removed) {
      physics.space().remove(body);
      removed = true;
    }
    node.removeFromParent();
  }
}
