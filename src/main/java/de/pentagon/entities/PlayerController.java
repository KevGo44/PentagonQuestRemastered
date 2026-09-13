package de.pentagon.entities;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import de.pentagon.assets.*;
import de.pentagon.combat.AttackTimeline;
import de.pentagon.core.GameSession;
import de.pentagon.physics.PhysicsWorld;

public final class PlayerController {
  /**
   * Seconds a dodge and a cast occupy; the Dodge and Cast clips are cut to exactly these lengths
   * (23 and 18 frames at 30 fps, art/blender/anim_polish.py), and AssetTest holds them to it. The
   * dodge used to be 0.58 s against a 2.33 s clip, so the player crouched and stood up without ever
   * rolling. Distance stays what it was: 5.8 m, now over 0.77 s.
   */
  public static final float DODGE_TIME = 23 / 30f,
      CAST_TIME = 18 / 30f,
      DODGE_SPEED = 5.8f / DODGE_TIME;

  /**
   * A successful parry is its own move now. The hero plays the Parry clip (a shield jolt of 15
   * frames, art/blender/anim_pin.py) and stands for its length; the attacker is stunned for {@link
   * #PARRY_STUN}; and for {@link #RIPOSTE_WINDOW} seconds the next blow that lands on a stunned
   * enemy is a riposte at {@link #RIPOSTE_MULTIPLIER} times the damage.
   */
  public static final float PARRY_TIME = 15 / 30f,
      PARRY_STUN = 2f,
      RIPOSTE_WINDOW = 2f,
      RIPOSTE_MULTIPLIER = 2.5f;

  public final Node node = new Node("Player");
  public final CharacterFactory.Rig rig;
  public final BetterCharacterControl body;
  public final AttackTimeline attack = new AttackTimeline();
  public final Vector3f facing = new Vector3f(0, 0, -1),
      movement = new Vector3f(),
      dodgeDirection = new Vector3f();
  public boolean forward, back, left, right, sprint, blocking;
  public float yaw = FastMath.PI,
      pitch = .31f,
      dodgeLeft,
      hitLeft,
      castLeft,
      parryLeft,
      riposteLeft,
      spellCooldown,
      blockAge,
      regenDelay;
  private float footstep;

  public PlayerController(AssetPipeline assets, PhysicsWorld physics) {
    rig = new CharacterFactory(assets).create("hero", 0x547079, false, true);
    node.attachChild(rig.root());
    body = new BetterCharacterControl(.38f, 1.85f, 75);
    body.setJumpForce(new Vector3f(0, 420, 0));
    node.addControl(body);
    physics.space().add(body);
  }

  public void resetInput() {
    forward = back = left = right = sprint = blocking = false;
    body.setWalkDirection(Vector3f.ZERO);
  }

  public void warp(float x, float z) {
    body.warp(new Vector3f(x, .12f, z));
    body.setViewDirection(facing);
  }

  public void look(float horizontal, float vertical) {
    yaw -= horizontal * 1.8f;
    pitch = FastMath.clamp(pitch + vertical * 1.8f, -.1f, .85f);
  }

  public void block(boolean pressed) {
    if (pressed
        && (attack.active() || dodgeLeft > 0 || castLeft > 0 || hitLeft > 0 || parryLeft > 0))
      return;
    if (pressed && !blocking) blockAge = 0;
    blocking = pressed;
  }

  /** Called by the combat system when a block turned a blow aside inside the parry window. */
  public void parry() {
    parryLeft = PARRY_TIME;
    riposteLeft = RIPOSTE_WINDOW;
    blocking = false;
    blockAge = 1;
    attack.cancel();
    body.setWalkDirection(Vector3f.ZERO);
    if (rig.has("Parry")) rig.once("Parry");
    else rig.restart("Block");
  }

  /** True while the last parry still promises a riposte. */
  public boolean riposteReady() {
    return riposteLeft > 0;
  }

  public void riposteSpent() {
    riposteLeft = 0;
  }

  public boolean dodge(GameSession s) {
    if (dodgeLeft > 0 || hitLeft > 0 || parryLeft > 0 || !body.isOnGround() || !s.player.spend(26))
      return false;
    dodgeDirection.set(movement.lengthSquared() > .01f ? movement.normalize() : facing);
    dodgeLeft = DODGE_TIME;
    attack.cancel();
    blocking = false;
    regenDelay = .65f;
    rig.restart("Dodge");
    return true;
  }

  public boolean jump(GameSession s) {
    if (body.isOnGround() && dodgeLeft <= 0 && s.player.spend(12)) {
      body.jump();
      regenDelay = .5f;
      return true;
    }
    return false;
  }

  public boolean attack(GameSession s) {
    if (dodgeLeft > 0 || hitLeft > 0 || castLeft > 0 || parryLeft > 0 || blocking) return false;
    if (attack.active()) {
      attack.request();
      return false;
    }
    if (!s.player.spend(16)) return false;
    faceCamera();
    attack.request();
    rig.restart("Attack1");
    regenDelay = .7f;
    return true;
  }

  public boolean cast(GameSession s) {
    if (spellCooldown > 0
        || dodgeLeft > 0
        || hitLeft > 0
        || parryLeft > 0
        || attack.active()
        || !s.player.spend(24)) return false;
    faceCamera();
    castLeft = CAST_TIME;
    spellCooldown = s.player.spellCooldown();
    blocking = false;
    rig.restart("Cast");
    regenDelay = .7f;
    return true;
  }

  private void faceCamera() {
    facing.set(FastMath.sin(yaw), 0, FastMath.cos(yaw));
    body.setViewDirection(facing);
  }

  public boolean update(float dt, GameSession s) {
    spellCooldown = Math.max(0, spellCooldown - dt);
    dodgeLeft = Math.max(0, dodgeLeft - dt);
    hitLeft = Math.max(0, hitLeft - dt);
    castLeft = Math.max(0, castLeft - dt);
    parryLeft = Math.max(0, parryLeft - dt);
    riposteLeft = Math.max(0, riposteLeft - dt);
    regenDelay = Math.max(0, regenDelay - dt);
    if (blocking) blockAge += dt;
    Vector3f direction = new Vector3f(FastMath.sin(yaw), 0, FastMath.cos(yaw)),
        side = new Vector3f(-direction.z, 0, direction.x);
    movement.set(0, 0, 0);
    if (forward) movement.addLocal(direction);
    if (back) movement.subtractLocal(direction);
    if (right) movement.addLocal(side);
    if (left) movement.subtractLocal(side);
    movement.normalizeLocal();
    boolean running =
        sprint
            && !blocking
            && !attack.active()
            && movement.lengthSquared() > .1f
            && s.player.stamina > 1;
    if (running) {
      s.player.spend(Math.min(s.player.stamina, dt * 15));
      regenDelay = .4f;
    }
    float speed = running ? 7f : 4.1f;
    if (blocking) speed = 1.8f;
    if (attack.active() || castLeft > 0) speed = 1.1f;
    if (hitLeft > 0 || parryLeft > 0) speed = 0;
    if (dodgeLeft > 0) body.setWalkDirection(dodgeDirection.mult(DODGE_SPEED));
    else body.setWalkDirection(movement.mult(speed));
    if (movement.lengthSquared() > .01f
        && !attack.active()
        && castLeft == 0
        && hitLeft == 0
        && parryLeft == 0) {
      facing.set(movement);
      body.setViewDirection(facing);
    }
    if (parryLeft > 0) {
      // The Parry one-shot is running; nothing may replace it until it has jolted through.
    } else if (blocking) {
      faceCamera();
      rig.play("Block");
    } else if (hitLeft > 0) rig.play("Hit");
    else if (dodgeLeft > 0) rig.play("Dodge");
    else if (attack.active()) rig.play("Attack" + (attack.combo() + 1));
    else if (castLeft > 0) rig.play("Cast");
    else rig.play(movement.lengthSquared() > .01f ? (running ? "Run" : "Walk") : "Idle");
    s.player.recover(dt, regenDelay == 0 && !blocking && dodgeLeft == 0 && !attack.active());
    if (movement.lengthSquared() > .1f && body.isOnGround()) {
      footstep += dt;
      if (footstep > (running ? .29f : .43f)) {
        footstep = 0;
        return true;
      }
    }
    return false;
  }

  /** The roll itself: hips leave standing height at 0.1 s and are back up at 0.65 s of the clip. */
  public boolean invulnerable() {
    return dodgeLeft > DODGE_TIME - .65f && dodgeLeft < DODGE_TIME - .1f;
  }

  public void camera(Camera camera, Node occluders, float dt, boolean snap) {
    camera(camera, dt, snap, occluders);
  }

  /**
   * Places the trailing camera and pulls it in where geometry is in the way.
   *
   * <p>Every node that can block the view has to be passed in. Interactables used to be missing,
   * and that is measurable: on entering a region the player spawns at the gate, the camera sits 6 m
   * behind and 3.5 m up, and the gate lintel spans 4.55 to 5.25 m - so the camera landed inside the
   * stonework while all five rays reported nothing.
   */
  public void camera(Camera camera, float dt, boolean snap, Node... blockers) {
    Vector3f target = node.getWorldTranslation().add(0, 1.42f, 0);
    Vector3f desired =
        new Vector3f(
            -FastMath.sin(yaw) * 6.3f * FastMath.cos(pitch),
            2f + FastMath.sin(pitch) * 5,
            -FastMath.cos(yaw) * 6.3f * FastMath.cos(pitch));
    float distance = desired.length();
    Vector3f direction = desired.normalize();
    float allowed = distance;
    // Five rays cover the near plane as well as its centre.
    for (Vector3f offset :
        new Vector3f[] {
          Vector3f.ZERO,
          new Vector3f(.22f, 0, 0),
          new Vector3f(-.22f, 0, 0),
          new Vector3f(0, .2f, 0),
          new Vector3f(0, -.2f, 0)
        }) {
      CollisionResults results = new CollisionResults();
      Ray ray = new Ray(target.add(offset), direction);
      ray.setLimit(distance);
      for (Node blocker : blockers) if (blocker != null) blocker.collideWith(ray, results);
      for (CollisionResult hit : results) {
        // The portal veil is the one thing the camera must still pass through: it is the doorway
        // the player walks into, not an obstacle.
        if (hit.getGeometry().getQueueBucket() == Bucket.Transparent) continue;
        allowed = Math.min(allowed, Math.max(.5f, hit.getDistance() - .3f));
        break;
      }
    }
    Vector3f end = target.add(direction.mult(allowed));
    // Snap inward on collision; smooth only outward so interpolation cannot cross a wall.
    if (snap || allowed < distance - .05f) camera.setLocation(end);
    else camera.setLocation(camera.getLocation().interpolateLocal(end, Math.min(1, dt * 13)));
    camera.lookAt(target, Vector3f.UNIT_Y);
  }

  public void cleanup(PhysicsWorld physics) {
    physics.space().remove(body);
    node.removeFromParent();
  }
}
