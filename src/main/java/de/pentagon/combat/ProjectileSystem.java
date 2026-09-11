package de.pentagon.combat;

import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import de.pentagon.assets.AssetPipeline;
import de.pentagon.entities.Enemy;
import de.pentagon.physics.PhysicsWorld;
import java.util.*;
import java.util.function.*;

/** Native Bullet integration plus swept hit tests, preventing tunnelling at low FPS. */
public final class ProjectileSystem {
  private static final int CAPACITY = 32;

  private static final class Bolt {
    Node node = new Node("AetherBolt");
    RigidBodyControl body;
    Vector3f previous = new Vector3f();
    float life, damage;
    boolean friendly;
  }

  public final Node root = new Node("Projectiles");
  private final Bolt[] bolts = new Bolt[CAPACITY];
  private final PhysicsWorld physics;

  public ProjectileSystem(AssetPipeline assets, PhysicsWorld physics) {
    this.physics = physics;
    for (int i = 0; i < CAPACITY; i++) {
      Bolt b = new Bolt();
      Geometry orb = assets.sphere("BoltCore", .14f, assets.glow(0x85e8ea, 2));
      b.node.attachChild(orb);
      b.body = new RigidBodyControl(new SphereCollisionShape(.12f), .2f);
      b.body.setContactResponse(false);
      b.body.setCcdMotionThreshold(.05f);
      b.body.setCcdSweptSphereRadius(.1f);
      b.node.addControl(b.body);
      bolts[i] = b;
    }
  }

  public boolean shoot(Vector3f from, Vector3f direction, boolean friendly, float damage) {
    for (Bolt b : bolts)
      if (b.life <= 0) {
        b.friendly = friendly;
        b.damage = damage;
        b.life = 3.5f;
        b.previous.set(from);
        root.attachChild(b.node);
        physics.space().add(b.body);
        b.body.setPhysicsLocation(from);
        b.body.setGravity(Vector3f.ZERO);
        b.body.setLinearVelocity(direction.normalize().mult(friendly ? 23 : 12));
        return true;
      }
    return false;
  }

  public void update(
      float dt,
      Node blockers,
      List<Enemy> enemies,
      Vector3f player,
      BiConsumer<Enemy, Float> enemyHit,
      BiConsumer<Vector3f, Float> playerHit,
      Effects effects) {
    for (Bolt b : bolts)
      if (b.life > 0) {
        b.life -= dt;
        Vector3f current = b.body.getPhysicsLocation(), delta = current.subtract(b.previous);
        float length = delta.length(), nearest = length + 1;
        Enemy victim = null;
        boolean playerVictim = false, blocked = false;
        if (length > .0001f) {
          CollisionResults collisions = new CollisionResults();
          Ray ray = new Ray(b.previous, delta.divide(length));
          ray.setLimit(length);
          blockers.collideWith(ray, collisions);
          if (collisions.size() > 0) {
            nearest = collisions.getClosestCollision().getDistance();
            blocked = true;
          }
        }
        if (b.friendly) {
          for (Enemy enemy : enemies)
            if (enemy.alive()) {
              float hit =
                  segmentHit(
                      b.previous,
                      current,
                      enemy.position().add(0, enemy.type.scale, 0),
                      .6f * enemy.type.scale);
              if (hit >= 0 && hit < nearest) {
                nearest = hit;
                victim = enemy;
                blocked = false;
              }
            }
        } else {
          float hit = segmentHit(b.previous, current, player.add(0, 1, 0), .6f);
          if (hit >= 0 && hit < nearest) {
            nearest = hit;
            playerVictim = true;
            blocked = false;
          }
        }
        if (victim != null || playerVictim || blocked) {
          effects.burst(current, 9);
          if (victim != null) enemyHit.accept(victim, b.damage);
          if (playerVictim) playerHit.accept(b.previous, b.damage);
          b.life = 0;
        }
        if (b.life <= 0) retire(b);
        else b.previous.set(current);
      }
  }

  public static float segmentHit(Vector3f a, Vector3f b, Vector3f center, float radius) {
    Vector3f delta = b.subtract(a);
    float squared = delta.lengthSquared();
    float t = squared < 1e-8 ? 0 : FastMath.clamp(center.subtract(a).dot(delta) / squared, 0, 1);
    return a.add(delta.mult(t)).distanceSquared(center) <= radius * radius
        ? (float) Math.sqrt(squared) * t
        : -1;
  }

  private void retire(Bolt b) {
    physics.space().remove(b.body);
    b.node.removeFromParent();
    b.life = 0;
  }

  public void clear() {
    for (Bolt b : bolts) if (b.life > 0) retire(b);
  }

  public int active() {
    int n = 0;
    for (Bolt b : bolts) if (b.life > 0) n++;
    return n;
  }
}
