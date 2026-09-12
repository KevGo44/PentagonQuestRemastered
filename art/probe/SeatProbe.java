
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** Prints the hand frames and the raw prop extents. Headless: numbers only, no window. */
public final class SeatProbe extends SimpleApplication {
  public static void main(String[] args) {
    SeatProbe probe = new SeatProbe();
    probe.start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    for (String id : new String[] {"props/sword", "props/shield"}) {
      Spatial s = assets.model(id, () -> new Node("none"));
      s.updateGeometricState();
      BoundingBox b = (BoundingBox) s.getWorldBound();
      System.out.println("[RAW] " + id + " center=" + b.getCenter()
          + " extent=(" + b.getXExtent() + ", " + b.getYExtent() + ", " + b.getZExtent() + ")"
          + " tris=" + s.getTriangleCount());
    }
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false);
    rootNode.attachChild(rig.root());
    rig.restart("Idle");
    rig.root().updateLogicalState(.05f);
    rig.root().updateGeometricState();
    Armature armature = rig.skinning().getArmature();
    armature.update();
    for (String hand : new String[] {"Hand.R", "Hand.L"}) {
      Joint j = armature.getJoint(hand);
      Transform t = j.getModelTransform();
      Quaternion inverse = t.getRotation().inverse();
      Vector3f at = t.getTranslation();
      System.out.println("[HAND] " + hand + " model=" + at + " scale=" + t.getScale());
      for (Joint child : j.getChildren())
        System.out.println("[HAND]   child " + child.getName() + " localdir="
            + inverse.mult(child.getModelTransform().getTranslation().subtract(at)).normalize()
            + " dist=" + child.getModelTransform().getTranslation().distance(at));
      Joint parent = j.getParent();
      System.out.println("[HAND]   arm " + parent.getName() + " localdir="
          + inverse.mult(at.subtract(parent.getModelTransform().getTranslation())).normalize());
      for (String other : new String[] {"Hips", "Spine", "Head"}) {
        Joint o = armature.getJoint(other);
        if (o == null) continue;
        System.out.println("[HAND]   toward " + other + " localdir="
            + inverse.mult(o.getModelTransform().getTranslation().subtract(at)).normalize());
      }
      // World axes of the socket, so the rendered picture can be predicted.
      System.out.println("[HAND]   X=" + t.getRotation().mult(Vector3f.UNIT_X)
          + " Y=" + t.getRotation().mult(Vector3f.UNIT_Y)
          + " Z=" + t.getRotation().mult(Vector3f.UNIT_Z));
    }
    for (String name : new String[] {"Hand.R", "Hand.L", "Forearm.R", "UpperArm.R", "Hips"}) {
      Joint j = armature.getJoint(name);
      if (j != null) System.out.println("[POS] " + name + " " + j.getModelTransform().getTranslation());
    }
    System.out.println("[JOINTS] " + armature.getJointList().size());
    StringBuilder all = new StringBuilder();
    for (Joint j : armature.getJointList()) all.append(j.getName()).append(' ');
    System.out.println("[NAMES] " + all);
    stop();
  }
}
