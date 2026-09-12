
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** Does a headless update actually pose the skeleton, or only the bind pose? */
public final class PoseProbe extends SimpleApplication {
  public static void main(String[] args) {
    new PoseProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x556677, false);
    Armature armature = rig.skinning().getArmature();
    Joint hand = armature.getJoint("Hand.R");
    rig.restart("Idle");
    for (int i = 0; i < 6; i++) {
      rig.root().updateLogicalState(.05f);
      rig.root().updateGeometricState();
      armature.update();
      System.out.println("[POSE] after " + (i + 1) + " updates: handLocal="
          + hand.getLocalTranslation() + " handRot=" + hand.getLocalRotation()
          + " handModel=" + hand.getModelTransform().getTranslation()
          + " time=" + rig.composer().getTime());
    }
    // And with the control's own render path, which is what the window shows.
    rig.skinning().setHardwareSkinningPreferred(false);
    System.out.println("[POSE] armature joint count " + armature.getJointList().size());
    stop();
  }
}
