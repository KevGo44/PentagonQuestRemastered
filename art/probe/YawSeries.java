
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** Pelvis yaw over the length of every clip: a tilted clip or a clip that turns in itself? */
public final class YawSeries extends SimpleApplication {
  public static void main(String[] args) {
    new YawSeries().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    for (String clip : CharacterFactory.CLIPS) {
      CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x556677, false);
      Armature armature = rig.skinning().getArmature();
      rig.restart(clip);
      double length = rig.composer().getAnimClip(clip).getLength();
      StringBuilder line = new StringBuilder();
      int samples = 13;
      for (int s = 0; s < samples; s++) {
        rig.composer().setTime((float) (length * s / (samples - 1.0)));
        rig.root().updateLogicalState(0);
        rig.root().updateGeometricState();
        armature.update();
        Transform space = rig.skinning().getSpatial().getWorldTransform();
        Vector3f lateral =
            space
                .transformVector(
                    armature.getJoint("Thigh.L").getModelTransform().getTranslation(),
                    new Vector3f())
                .subtract(
                    space.transformVector(
                        armature.getJoint("Thigh.R").getModelTransform().getTranslation(),
                        new Vector3f()));
        lateral.y = 0;
        lateral.normalizeLocal();
        line.append(String.format("%6.1f", Math.toDegrees(Math.atan2(lateral.x, -lateral.z))));
      }
      System.out.printf("[YAW] %-8s len=%.2f %s%n", clip, length, line);
    }
    stop();
  }
}
