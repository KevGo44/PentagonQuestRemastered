
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/**
 * The pelvis axis is bolted to the hips: Thigh.L and Thigh.R are children of Hips at fixed offsets,
 * so the direction between them is the pelvis orientation and nothing the legs do can turn it. That
 * makes it the one facing metric that survives every pose.
 */
public final class PelvisProbe extends SimpleApplication {
  public static void main(String[] args) {
    new PelvisProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    for (String id : new String[] {"hero", "mira", "eren", "orc", "king"}) {
      for (String clip : CharacterFactory.CLIPS) {
        CharacterFactory.Rig rig = new CharacterFactory(assets).create(id, 0x556677, false);
        Armature armature = rig.skinning().getArmature();
        rig.restart(clip);
        double length = rig.composer().getAnimClip(clip).getLength();
        double sumSin = 0, sumCos = 0, min = 999, max = -999;
        int samples = 24;
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
          // Forward is the pelvis axis turned a quarter: with left on +X the front is on +Z.
          double yaw = Math.toDegrees(Math.atan2(lateral.x, -lateral.z));
          sumSin += Math.sin(Math.toRadians(yaw));
          sumCos += Math.cos(Math.toRadians(yaw));
          min = Math.min(min, yaw);
          max = Math.max(max, yaw);
        }
        double mean = Math.toDegrees(Math.atan2(sumSin / samples, sumCos / samples));
        System.out.printf(
            "[PELVIS] %-7s %-8s mean=%7.1f  min=%7.1f  max=%7.1f  spread=%5.1f%n",
            id, clip, mean, min, max, max - min);
      }
    }
    stop();
  }
}
