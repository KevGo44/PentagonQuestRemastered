package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.anim.tween.action.BlendableAction;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/**
 * Pelvis yaw of one clip on every frame (30 fps), blend-free, in the engine. Usage: DodgeYaw hero
 * Dodge. The joint space of the delivered rigs is Blender's - height along -Z, skinning spatial at
 * identity - so the vertical to project out is read off the rig (Root to Hips), not assumed to be
 * y. That assumption cost an afternoon: with y zeroed a straight roll read 30 to 50 degrees
 * crooked while Blender measured zero on the same file.
 */
public final class DodgeYaw extends SimpleApplication {
  private static String id = "hero", clip = "Dodge";

  public static void main(String[] args) {
    if (args.length > 0) id = args[0];
    if (args.length > 1) clip = args[1];
    new DodgeYaw().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    CharacterFactory.Rig rig =
        new CharacterFactory(new AssetPipeline(assetManager)).create(id, 0x556677, false);
    ((BlendableAction) rig.composer().action(clip)).setTransitionLength(0);
    Armature armature = rig.skinning().getArmature();
    Vector3f up =
        armature
            .getJoint("Hips")
            .getInitialTransform()
            .getTranslation()
            .subtract(armature.getJoint("Root").getInitialTransform().getTranslation())
            .normalizeLocal();
    rig.restart(clip);
    double length = rig.composer().getAnimClip(clip).getLength();
    StringBuilder line = new StringBuilder();
    for (int f = 0; f / 30.0 < length; f++) {
      rig.composer().setTime((float) Math.min(length - .001, f / 30.0));
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
      lateral.subtractLocal(up.mult(lateral.dot(up)));
      lateral.normalizeLocal();
      line.append(String.format(" %d:%.0f", f, Math.toDegrees(Math.acos(Math.min(1, lateral.x)))));
    }
    System.out.printf("[FRAMEYAW] %s %s up=%s len=%.3f%s%n", id, clip, up, length, line);
    stop();
  }
}
