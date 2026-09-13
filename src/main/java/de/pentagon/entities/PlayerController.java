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

  /**
   * Trailing camera: distance behind the shoulder target, its base height, and how far the target
   * sits to the hero's right so he no longer covers the point the reticle marks. The camera used to
   * hang 6.3 m straight behind him at 2 m; the hero stood dead centre, and where he was looking was
   * anyone's guess.
   */
  public static final float CAMERA_DISTANCE = 5.4f, CAMERA_HEIGHT = 1.6f, SHOULDER = .7f;

  /** Eye height of the first-person camera, used until the rig's Head joint has been placed. */
  public static final float EYE_HEIGHT = 1.62f;

  /**
   * First person: the camera sits this far behind the Head joint and this much above it, so the
   * hero's own arms, sword and shield stay in the picture. The head itself is collapsed (its joint
   * scaled to nothing) so it never blocks the view.
   */
  public static final float HEAD_BACK = .16f, HEAD_UP = .12f;

  /** How far the aim ray reaches when it meets nothing. */
  public static final float AIM_RANGE = 40;

  /**
   * The point under the reticle: where the camera's centre ray meets the world, set every frame by
   * the campaign ({@link #aim}). Attacks and casts turn towards it - the camera hangs over the
   * hero's shoulder, so its line and his are 0.7 m apart, and a spell fired along his yaw passed a
   * target the reticle sat on.
   */
  public final Vector3f aimPoint = new Vector3f();

  private boolean aimed;

  public final Node node = new Node("Player");
  public final CharacterFactory.Rig rig;
  public final BetterCharacterControl body;
  private final Node facingMarker = new Node("FacingMarker");
  private boolean firstPerson;
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
    // A chevron on the floor a metre ahead, turning with the body: the hero's look direction
    // made visible in the trailing view. The node is turned by the character control, so local
    // +Z is always where he faces.
    for (int sign : new int[] {-1, 1}) {
      Geometry wing = assets.box("FacingWing", .02f, .012f, .2f, assets.flame(0x77bbbc, .5f, .55f));
      wing.setQueueBucket(Bucket.Transparent);
      wing.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
      wing.setLocalRotation(new Quaternion().fromAngleAxis(sign * .6f, Vector3f.UNIT_Y));
      wing.setLocalTranslation(sign * .13f, 0, -.14f);
      facingMarker.attachChild(wing);
    }
    facingMarker.setLocalTranslation(0, .04f, 1.05f);
    node.attachChild(facingMarker);
    // Between the composer and the skinning: the clips carry scale keys for every joint, so a
    // scale set once would be overwritten on the next frame.
    Spatial animated = rig.composer().getSpatial();
    int after = 0;
    for (int i = 0; i < animated.getNumControls(); i++)
      if (animated.getControl(i) == rig.composer()) after = i + 1;
    animated.addControlAt(after, headHider);
  }

  private final HeadHider headHider = new HeadHider();

  /** Collapses the Head joint after the animation has posed it, while first person is on. */
  private final class HeadHider extends com.jme3.scene.control.AbstractControl {
    boolean on;

    @Override
    protected void controlUpdate(float tpf) {
      if (!on) return;
      com.jme3.anim.Joint head = rig.skinning().getArmature().getJoint("Head");
      if (head != null) head.setLocalScale(new Vector3f(.001f, .001f, .001f));
    }

    @Override
    protected void controlRender(
        com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}
  }

  /**
   * First person keeps the hero visible - his arms, sword and shield are the point - but collapses
   * his head so the camera behind it looks past nothing, and hides the floor chevron.
   */
  public void firstPerson(boolean on) {
    firstPerson = on;
    headHider.on = on;
    if (!on) {
      com.jme3.anim.Joint head = rig.skinning().getArmature().getJoint("Head");
      if (head != null) head.setLocalScale(new Vector3f(1, 1, 1));
    }
    facingMarker.setCullHint(on ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
    // The trailing view rests at a downward tilt of 0.31; carried into first person that is a
    // stare at the floor, so the eyes come up to level on the switch.
    pitch = FastMath.clamp(on ? Math.min(pitch, .05f) : pitch, minPitch(), maxPitch());
    if (on) faceCamera();
  }

  public boolean firstPerson() {
    return firstPerson;
  }

  /**
   * The trailing camera sits {@link #CAMERA_HEIGHT} + 4.5 sin(pitch) above its target, so the view
   * is level only where that sum is about zero: the old floor of -0.1 left it 1.15 m above the
   * shoulder and always tilted twelve degrees down - the reticle could never look straight ahead.
   */
  private float minPitch() {
    return firstPerson ? -.75f : -.36f;
  }

  private float maxPitch() {
    return firstPerson ? .75f : .85f;
  }

  /** Where the camera looks in first person; the trailing view looks along the yaw only. */
  public Vector3f viewDirection() {
    float p = firstPerson ? pitch + dodgeDip() : 0;
    return new Vector3f(
        FastMath.sin(yaw) * FastMath.cos(p), -FastMath.sin(p), FastMath.cos(yaw) * FastMath.cos(p));
  }

  /** In first person the eyes follow the roll: a nod down and up over the dodge. */
  private float dodgeDip() {
    if (dodgeLeft <= 0) return 0;
    return FastMath.sin((1 - dodgeLeft / DODGE_TIME) * FastMath.PI) * .55f;
  }

  /** Horizontal direction from the hero to the point under the reticle. */
  public Vector3f aimDirection() {
    if (!aimed) return new Vector3f(FastMath.sin(yaw), 0, FastMath.cos(yaw));
    Vector3f d = aimPoint.subtract(node.getWorldTranslation());
    d.y = 0;
    return d.lengthSquared() < .01f
        ? new Vector3f(FastMath.sin(yaw), 0, FastMath.cos(yaw))
        : d.normalizeLocal();
  }

  /**
   * Casts the camera's centre ray against the world and the enemies and remembers where it lands;
   * without a hit the point lies {@link #AIM_RANGE} out. Hits nearer the camera than the hero's own
   * head are skipped in the trailing view, so a pillar beside the shoulder is not the target.
   */
  public void aim(Camera camera, java.util.List<Enemy> enemies, Node... blockers) {
    Ray ray = new Ray(camera.getLocation(), camera.getDirection());
    ray.setLimit(AIM_RANGE);
    float nearest = AIM_RANGE;
    float skip =
        firstPerson ? .3f : camera.getLocation().distance(node.getWorldTranslation()) * .6f;
    CollisionResults results = new CollisionResults();
    for (Node blocker : blockers) if (blocker != null) blocker.collideWith(ray, results);
    for (Enemy enemy : enemies) if (enemy.alive()) enemy.node.collideWith(ray, results);
    for (CollisionResult hit : results) {
      if (hit.getDistance() < skip) continue;
      if (hit.getGeometry().getQueueBucket() == Bucket.Transparent) continue;
      nearest = Math.min(nearest, hit.getDistance());
      break;
    }
    aimPoint.set(ray.getOrigin()).addLocal(ray.getDirection().mult(nearest));
    aimed = true;
  }

  public void resetInput() {
    forward = back = left = right = sprint = blocking = false;
    body.setWalkDirection(Vector3f.ZERO);
  }

  public void warp(float x, float z) {
    body.warp(new Vector3f(x, .12f, z));
    body.setViewDirection(facing);
    // The remembered aim point belongs to the old position; until the next frame has cast a
    // new one, attacks turn along the camera yaw as before.
    aimed = false;
  }

  public void look(float horizontal, float vertical) {
    yaw -= horizontal * 1.8f;
    pitch = FastMath.clamp(pitch + vertical * 1.8f, minPitch(), maxPitch());
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

  /** Turns the hero towards the point under the reticle (or along the camera yaw before any). */
  public void faceCamera() {
    facing.set(aimDirection());
    body.setViewDirection(facing);
  }

  /** The direction a spell leaves {@code from} to pass through the point under the reticle. */
  public Vector3f castDirection(Vector3f from) {
    if (!aimed) return facing.clone();
    Vector3f d = aimPoint.subtract(from);
    return d.lengthSquared() < .01f ? facing.clone() : d.normalizeLocal();
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
    if (firstPerson) {
      // In first person the body always faces where the eyes look; WASD strafes.
      facing.set(direction);
      body.setViewDirection(facing);
    } else if (movement.lengthSquared() > .01f
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
    if (firstPerson) {
      // Behind the (collapsed) head, so the camera follows every step and the whole roll.
      Node head = rig.skinning().getAttachmentsNode("Head");
      Vector3f eye =
          head.getWorldTranslation().lengthSquared() > 0
              ? head.getWorldTranslation().add(0, HEAD_UP, 0)
              : node.getWorldTranslation().add(0, EYE_HEIGHT, 0);
      Vector3f look = viewDirection();
      Vector3f flat = new Vector3f(FastMath.sin(yaw), 0, FastMath.cos(yaw));
      // Looking down, the camera slides forward over the neck instead of showing it.
      float back = HEAD_BACK - .12f * Math.max(0, pitch / maxPitch());
      camera.setLocation(eye.subtract(flat.mult(back)));
      camera.lookAt(camera.getLocation().add(look), Vector3f.UNIT_Y);
      return;
    }
    Vector3f centre = node.getWorldTranslation().add(0, 1.5f, 0);
    Vector3f forward = new Vector3f(FastMath.sin(yaw), 0, FastMath.cos(yaw)),
        right = forward.cross(Vector3f.UNIT_Y).normalizeLocal();
    // Over the right shoulder, but never into a wall: the side offset shrinks where a ray from
    // the hero's centre meets geometry.
    float shoulder = SHOULDER;
    CollisionResults side = new CollisionResults();
    Ray sideRay = new Ray(centre, right);
    sideRay.setLimit(SHOULDER + .3f);
    for (Node blocker : blockers) if (blocker != null) blocker.collideWith(sideRay, side);
    for (CollisionResult hit : side) {
      if (hit.getGeometry().getQueueBucket() == Bucket.Transparent) continue;
      shoulder = Math.max(0, Math.min(shoulder, hit.getDistance() - .3f));
      break;
    }
    Vector3f target = centre.add(right.mult(shoulder));
    Vector3f desired =
        new Vector3f(
            -FastMath.sin(yaw) * CAMERA_DISTANCE * FastMath.cos(pitch),
            CAMERA_HEIGHT + FastMath.sin(pitch) * 4.5f,
            -FastMath.cos(yaw) * CAMERA_DISTANCE * FastMath.cos(pitch));
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
