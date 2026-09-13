
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.asset.AssetManager;
import com.jme3.math.*;
import com.jme3.system.JmeSystem;
import de.pentagon.assets.*;
import java.util.*;

/**
 * When does a clip do what? Samples the posed armature every 1/30 s and prints the height of Hips
 * and Head plus the speed of both hands, so the strike moment of an attack, the release of a cast,
 * the span of a roll and the moment a corpse reaches the floor can be read off as numbers before
 * the game timings are matched to them.
 */
public final class ClipTimeline {
  public static void main(String[] args) {
    System.setProperty("java.awt.headless", "true");
    AssetManager manager = JmeSystem.newAssetManager(JmeSystem.getPlatformAssetConfigURL());
    String id = args.length > 0 ? args[0] : "hero";
    String[] clips = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : CharacterFactory.CLIPS;
    for (String clip : clips) {
      CharacterFactory.Rig rig = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
      AnimClip c = rig.composer().getAnimClip(clip);
      float length = (float) c.getLength(), step = 1 / 30f;
      System.out.printf(Locale.ROOT, "== %s %s length=%.3f%n", id, clip, length);
      rig.restart(clip);
      rig.root().updateLogicalState(0);
      rig.root().updateGeometricState();
      Armature a = rig.skinning().getArmature();
      a.update();
      com.jme3.math.Transform space = rig.skinning().getSpatial().getWorldTransform();
      Vector3f prevR = pos(space, a, "Hand.R"), prevL = pos(space, a, "Hand.L");
      // The joint model space of the delivered rigs is Blender's: height runs along -Z (the hand
      // in Idle reads 1.12 m, the documented hand height), so heights are printed as -z.
      Vector3f hips0 = pos(space, a, "Hips");
      System.out.println("   t     hipsH  headH  handRH handLH  vR   vL   hipsDrift");
      for (float t = 0; t <= length + 1e-3f; t += step) {
        Vector3f hips = pos(space, a, "Hips"), head = pos(space, a, "Head"), r = pos(space, a, "Hand.R"), l = pos(space, a, "Hand.L");
        float vR = r.distance(prevR) / step, vL = l.distance(prevL) / step;
        // Horizontal travel of the hips since the first frame: root motion the game does not want.
        float drift = (float) Math.hypot(hips.x - hips0.x, hips.y - hips0.y);
        System.out.printf(Locale.ROOT, "  %.3f  %.3f  %.3f  %.3f  %.3f  %5.1f %5.1f  %.3f%n",
            t, -hips.z, -head.z, -r.z, -l.z, vR, vL, drift);
        prevR = r;
        prevL = l;
        rig.root().updateLogicalState(step);
        rig.root().updateGeometricState();
        a.update();
      }
    }
  }

  /** World space: the armature node carries the exporter's rotation and scale. */
  private static Vector3f pos(com.jme3.math.Transform space, Armature a, String joint) {
    return space.transformVector(a.getJoint(joint).getModelTransform().getTranslation(), new Vector3f());
  }
}
