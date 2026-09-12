
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/**
 * Where should a round shield look? Searched over every direction in the hand's own frame, scored by
 * how much of the front it covers across the clips a player watches, and rejected when the forearm
 * would stand through the disc. The only constraint is that: the arm has to stay behind the plane.
 */
public final class ShieldSearch extends SimpleApplication {
  private static final String[] CLIPS = {"Idle", "Walk", "Run", "Block", "Hit"};
  private static final float FIST = .095f, STAND = .11f, FOREARM = .25f, RADIUS = .325f;

  public static void main(String[] args) {
    new ShieldSearch().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false);
    rootNode.attachChild(rig.root());
    Node probe = new Node("probe");
    rig.skinning().getAttachmentsNode("Hand.L").attachChild(probe);
    Armature armature = rig.skinning().getArmature();
    Vector3f front = Vector3f.UNIT_Z;
    // Candidate normals in hand-local coordinates, on a coarse sphere.
    java.util.List<Vector3f> candidates = new java.util.ArrayList<>();
    for (int pitch = -80; pitch <= 80; pitch += 20)
      for (int yaw = 0; yaw < 360; yaw += 20) {
        float p = pitch * FastMath.DEG_TO_RAD, y = yaw * FastMath.DEG_TO_RAD;
        candidates.add(new Vector3f(FastMath.cos(p) * FastMath.cos(y), FastMath.sin(p),
            FastMath.cos(p) * FastMath.sin(y)).normalizeLocal());
      }
    candidates.add(Vector3f.UNIT_Y.clone());
    candidates.add(Vector3f.UNIT_Y.negate());
    Vector3f best = null;
    float bestScore = -2;
    for (Vector3f normal : candidates) {
      // The forearm runs back from the wrist along -Y; the disc plane sits STAND out along the
      // normal. Elbow behind the plane means -FOREARM * (Y . n) - STAND < 0.
      float elbow = -FOREARM * (-normal.y) - STAND;
      if (elbow > -.02f) continue;
      float score = 0;
      int samples = 0;
      for (String clip : CLIPS) {
        rig.restart(clip);
        double length = rig.composer().getAnimClip(clip).getLength();
        for (int s = 0; s < 24; s++) {
          rig.composer().setTime((float) (length * s / 23.0));
          rig.root().updateLogicalState(0);
          rig.root().updateGeometricState();
          armature.update();
          score += probe.getWorldRotation().mult(normal).dot(front);
          samples++;
        }
      }
      score /= samples;
      if (score > bestScore) {
        bestScore = score;
        best = normal.clone();
      }
    }
    System.out.printf("[SEARCH] best normal in hand space %s, mean forward %.3f (%.0f deg off)%n",
        best, bestScore, Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, bestScore)))));
    // And the three obvious ones, for comparison, per clip.
    for (Vector3f normal : new Vector3f[] {
        Vector3f.UNIT_Y.clone(), Vector3f.UNIT_Z.negate(), Vector3f.UNIT_Z.clone(), best}) {
      StringBuilder line = new StringBuilder();
      for (String clip : CLIPS) {
        rig.restart(clip);
        double length = rig.composer().getAnimClip(clip).getLength();
        float sum = 0, worst = 1;
        for (int s = 0; s < 24; s++) {
          rig.composer().setTime((float) (length * s / 23.0));
          rig.root().updateLogicalState(0);
          rig.root().updateGeometricState();
          armature.update();
          float forward = probe.getWorldRotation().mult(normal).dot(front);
          sum += forward;
          worst = Math.min(worst, forward);
        }
        line.append(String.format("  %s mean=%5.2f worst=%5.2f", clip, sum / 24, worst));
      }
      System.out.println("[AIM] " + normal + line);
    }
    stop();
  }
}
