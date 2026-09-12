
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/**
 * Blade clearance and shield aim, read off the attachment nodes - the same transforms the renderer
 * uses. Composing the socket transform by hand from the joints was tried and thrown away: it put the
 * sword point a metre below the floor, which the pictures flatly contradict. When a calculation and
 * a rendered image disagree, the image wins and the calculation is wrong.
 */
public final class ClearanceProbe extends SimpleApplication {
  public static void main(String[] args) {
    new ClearanceProbe().start(JmeContext.Type.Headless);
  }

  private static float distance(Vector3f a0, Vector3f a1, Vector3f b0, Vector3f b1) {
    float best = Float.MAX_VALUE;
    for (int i = 0; i <= 40; i++) {
      Vector3f p = a0.clone().interpolateLocal(a1, i / 40f);
      for (int j = 0; j <= 40; j++)
        best = Math.min(best, p.distance(b0.clone().interpolateLocal(b1, j / 40f)));
    }
    return best;
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false, true);
    rootNode.attachChild(rig.root());
    Node sword = (Node) rig.skinning().getAttachmentsNode("Hand.R").getChild("WeaponSocket");
    Node shield = (Node) rig.skinning().getAttachmentsNode("Hand.L").getChild("ShieldSocket");
    Armature armature = rig.skinning().getArmature();
    for (String clip : CharacterFactory.CLIPS) {
      rig.restart(clip);
      double length = rig.composer().getAnimClip(clip).getLength();
      float blade = Float.MAX_VALUE, aimBest = 999, aimWorst = -999, tipLow = 99;
      for (int s = 0; s < 24; s++) {
        rig.composer().setTime((float) (length * s / 23.0));
        rig.root().updateLogicalState(0);
        rig.root().updateGeometricState();
        armature.update();
        Vector3f grip = sword.getWorldTranslation();
        Vector3f tip = grip.add(sword.getWorldRotation().mult(Vector3f.UNIT_Y).mult(.882f));
        Transform space = rig.skinning().getSpatial().getWorldTransform();
        Vector3f thigh = at(armature, space, "Thigh.R"), shin = at(armature, space, "Shin.R"),
            foot = at(armature, space, "Foot.R");
        blade = Math.min(blade,
            Math.min(distance(grip, tip, thigh, shin), distance(grip, tip, shin, foot)));
        tipLow = Math.min(tipLow, tip.y);
        float off = (float) Math.toDegrees(Math.acos(Math.max(-1, Math.min(1,
            shield.getWorldRotation().mult(Vector3f.UNIT_Z).dot(Vector3f.UNIT_Z)))));
        aimBest = Math.min(aimBest, off);
        aimWorst = Math.max(aimWorst, off);
      }
      System.out.printf("[CLEAR] %-8s blade to right leg %.3f m, lowest tip %.2f m | "
          + "shield %.0f to %.0f deg off the front%n", clip, blade, tipLow, aimBest, aimWorst);
    }
    stop();
  }

  private Vector3f at(Armature armature, Transform space, String joint) {
    return space.transformVector(
        armature.getJoint(joint).getModelTransform().getTranslation(), new Vector3f());
  }
}
