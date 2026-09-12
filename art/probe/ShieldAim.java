
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/**
 * A shield strapped to a forearm can only face somewhere perpendicular to that forearm, otherwise
 * the arm stands through the disc. So two things decide how far forward it can look: where the
 * forearm points, and which of the perpendicular directions the mount picks. This measures the
 * first and searches the second.
 */
public final class ShieldAim extends SimpleApplication {
  private static final String[] CLIPS = {"Idle", "Walk", "Run", "Block", "Hit"};

  public static void main(String[] args) {
    new ShieldAim().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false);
    Node pivot = new Node("Player");
    pivot.attachChild(rig.root());
    rootNode.attachChild(pivot);
    Node probe = new Node("probe");
    rig.skinning().getAttachmentsNode("Hand.L").attachChild(probe);
    Armature armature = rig.skinning().getArmature();
    Vector3f front = Vector3f.UNIT_Z;

    // How far forward can a shield on this forearm look at all?
    for (String clip : CLIPS) {
      rig.restart(clip);
      double length = rig.composer().getAnimClip(clip).getLength();
      float bestPossible = 180, worstPossible = 0;
      for (int s = 0; s < 24; s++) {
        sample(rig, armature, length, s);
        Vector3f forearm = probe.getWorldRotation().mult(Vector3f.UNIT_Y);
        // The closest a perpendicular direction can come to the front.
        float limit = (float) Math.toDegrees(Math.asin(Math.min(1, Math.abs(forearm.dot(front)))));
        bestPossible = Math.min(bestPossible, limit);
        worstPossible = Math.max(worstPossible, limit);
      }
      System.out.printf("[LIMIT] %-6s forearm leaves the shield at best %.0f to %.0f deg off the front%n",
          clip, bestPossible, worstPossible);
    }

    // Which roll about the forearm aims it best, averaged over the clips a player looks at?
    Quaternion current = new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y);
    float bestRoll = 0, bestScore = -2;
    for (int degrees = 0; degrees < 360; degrees += 10) {
      Quaternion roll =
          new Quaternion().fromAngleAxis(degrees * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y);
      float score = 0;
      int samples = 0;
      for (String clip : CLIPS) {
        rig.restart(clip);
        double length = rig.composer().getAnimClip(clip).getLength();
        for (int s = 0; s < 24; s++) {
          sample(rig, armature, length, s);
          probe.setLocalRotation(roll.mult(current));
          probe.updateGeometricState();
          score += probe.getWorldRotation().mult(Vector3f.UNIT_Z).dot(front);
          samples++;
        }
      }
      score /= samples;
      System.out.printf("[ROLL] %3d deg: mean forward %.3f (%.0f deg off)%n",
          degrees, score, Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, score)))));
      if (score > bestScore) {
        bestScore = score;
        bestRoll = degrees;
      }
    }
    System.out.printf("[BEST] roll %.0f deg, mean forward %.3f%n", bestRoll, bestScore);
    stop();
  }

  private void sample(CharacterFactory.Rig rig, Armature armature, double length, int s) {
    rig.composer().setTime((float) (length * s / 23.0));
    rig.root().updateLogicalState(0);
    rig.root().updateGeometricState();
    armature.update();
  }
}
