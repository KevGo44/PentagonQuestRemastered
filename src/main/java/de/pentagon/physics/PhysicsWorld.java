package de.pentagon.physics;

import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import java.util.*;

public final class PhysicsWorld {
  private final PhysicsSpace space;
  private final List<PhysicsRigidBody> statics = new ArrayList<>();

  public PhysicsWorld(PhysicsSpace space) {
    this.space = space;
    space.setGravity(new Vector3f(0, -24, 0));
    space.setAccuracy(1f / 120);
    space.setMaxSubSteps(8);
  }

  public PhysicsSpace space() {
    return space;
  }

  public void box(float x, float y, float z, float hx, float hy, float hz) {
    PhysicsRigidBody body =
        new PhysicsRigidBody(new BoxCollisionShape(new Vector3f(hx, hy, hz)), 0);
    body.setPhysicsLocation(new Vector3f(x, y, z));
    body.setFriction(.8f);
    space.addCollisionObject(body);
    statics.add(body);
  }

  public void clear() {
    for (var body : statics) space.removeCollisionObject(body);
    statics.clear();
  }
}
