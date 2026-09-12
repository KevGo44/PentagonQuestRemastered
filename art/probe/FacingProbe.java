
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** Which way does a delivered rig face, and where do its hands end up? Numbers only. */
public final class FacingProbe extends SimpleApplication {
  public static void main(String[] args) {
    new FacingProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    for (String id : new String[] {"hero", "mira", "eren", "goblin", "orc"}) {
      CharacterFactory.Rig rig = new CharacterFactory(assets).create(id, 0x556677, false);
      Node root = new Node("probe");
      root.attachChild(rig.root());
      rig.restart("Idle");
      for (int i = 0; i < 4; i++) {
        root.updateLogicalState(.05f);
        root.updateGeometricState();
      }
      Armature armature = rig.skinning().getArmature();
      armature.update();
      Transform spatial = rig.skinning().getSpatial().getWorldTransform();
      System.out.println("[RIG] " + id + " spatialRot=" + spatial.getRotation()
          + " bound=" + root.getWorldBound());
      System.out.println("[RIG]   " + id + " worldHand.R=" + world(armature, spatial, "Hand.R")
          + " worldHand.L=" + world(armature, spatial, "Hand.L"));
      Vector3f foot = world(armature, spatial, "Foot.R"), toe = world(armature, spatial, "RightToe_End");
      if (foot != null && toe != null) {
        Vector3f forward = toe.subtract(foot);
        forward.y = 0;
        System.out.println("[RIG]   " + id + " toeForward=" + forward.normalize());
      }
      Vector3f hr = world(armature, spatial, "Hand.R"), hl = world(armature, spatial, "Hand.L");
      if (hr != null && hl != null) {
        Vector3f across = hl.subtract(hr);
        across.y = 0;
        System.out.println("[RIG]   " + id + " shoulderLine(R->L)=" + across.normalize());
      }
      // Local +Y of the hand frames in world space: the direction the fingers point.
      for (String hand : new String[] {"Hand.R", "Hand.L"}) {
        Joint j = armature.getJoint(hand);
        if (j == null) continue;
        Quaternion r = spatial.getRotation().mult(j.getModelTransform().getRotation());
        System.out.println("[RIG]   " + id + " " + hand
            + " fingersWorld=" + r.mult(Vector3f.UNIT_Y)
            + " localX=" + r.mult(Vector3f.UNIT_X)
            + " localZ=" + r.mult(Vector3f.UNIT_Z));
      }
    }
    stop();
  }

  private Vector3f world(Armature armature, Transform spatial, String joint) {
    Joint j = armature.getJoint(joint);
    if (j == null) return null;
    return spatial.transformVector(j.getModelTransform().getTranslation(), new Vector3f());
  }
}
