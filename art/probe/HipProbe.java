
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** Is the pelvis frame stable enough to serve as a headless facing metric? */
public final class HipProbe extends SimpleApplication {
  public static void main(String[] args) {
    new HipProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    for (String id : new String[] {"hero", "orc", "king"}) {
      for (String clip : CharacterFactory.CLIPS) {
        CharacterFactory.Rig rig = new CharacterFactory(assets).create(id, 0x556677, false);
        rig.restart(clip);
        for (int i = 0; i < 6; i++) rig.root().updateLogicalState(.05f);
        rig.root().updateGeometricState();
        Armature armature = rig.skinning().getArmature();
        armature.update();
        Quaternion space = rig.skinning().getSpatial().getWorldTransform().getRotation();
        Quaternion hips = space.mult(armature.getJoint("Hips").getModelTransform().getRotation());
        System.out.println("[HIP] " + id + " " + clip
            + " X=" + hips.mult(Vector3f.UNIT_X)
            + " Y=" + hips.mult(Vector3f.UNIT_Y)
            + " Z=" + hips.mult(Vector3f.UNIT_Z));
      }
    }
    stop();
  }
}
