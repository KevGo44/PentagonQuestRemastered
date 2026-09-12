
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

public final class WhereProbe extends SimpleApplication {
  public static void main(String[] args) {
    new WhereProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x556677, false);
    rig.restart("Idle");
    for (int i = 0; i < 5; i++) rig.root().updateLogicalState(.05f);
    rig.root().updateGeometricState();
    Armature armature = rig.skinning().getArmature();
    armature.update();
    Transform space = rig.skinning().getSpatial().getWorldTransform();
    System.out.println("[WHERE] wrapperRot=" + rig.root().getLocalRotation()
        + " skinnedWorldRot=" + space.getRotation());
    for (String j : new String[] {
        "Hips", "Head", "UpperArm.L", "UpperArm.R", "Hand.L", "Hand.R", "Foot.L", "Foot.R",
        "LeftShoulder", "RightShoulder"}) {
      Joint joint = armature.getJoint(j);
      if (joint == null) { System.out.println("[WHERE] " + j + " absent"); continue; }
      Vector3f model = joint.getModelTransform().getTranslation();
      System.out.println("[WHERE] " + j + " joint=" + model
          + " world=" + space.transformVector(model, new Vector3f()));
    }
    stop();
  }
}
