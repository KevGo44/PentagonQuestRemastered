
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** How far does the closed left fist reach along the forearm? The shield plate has to clear it. */
public final class FistProbe extends SimpleApplication {
  public static void main(String[] args) {
    new FistProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false);
    rootNode.attachChild(rig.root());
    Armature armature = rig.skinning().getArmature();
    for (String clip : new String[] {"Idle", "Block"}) {
      rig.restart(clip);
      rig.root().updateLogicalState(0);
      rig.root().updateGeometricState();
      armature.update();
      Joint hand = armature.getJoint("Hand.L");
      Transform frame = hand.getModelTransform();
      Quaternion inverse = frame.getRotation().inverse();
      Vector3f at = frame.getTranslation();
      float reach = 0;
      String farthest = "";
      for (Joint j : armature.getJointList()) {
        if (!j.getName().startsWith("LeftHand")) continue;
        Vector3f local = inverse.mult(j.getModelTransform().getTranslation().subtract(at));
        if (local.y > reach) {
          reach = local.y;
          farthest = j.getName();
        }
      }
      System.out.printf("[FIST] %-6s farthest finger joint along the forearm: %s at %.3f m%n",
          clip, farthest, reach);
    }
    stop();
  }
}
