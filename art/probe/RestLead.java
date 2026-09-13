
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.asset.AssetManager;
import com.jme3.math.*;
import com.jme3.system.JmeSystem;
import de.pentagon.assets.*;
import java.util.*;

/**
 * Does a clip open on the bind pose? For every character and clip: the largest joint rotation away
 * from the bind pose at each of the first keyframes, and the time at which the clip first leaves
 * the bind pose by more than ten degrees. A leading rest sample shows up as ~0 at t=0.
 */
public final class RestLead {
  public static void main(String[] args) {
    System.setProperty("java.awt.headless", "true");
    AssetManager manager = JmeSystem.newAssetManager(JmeSystem.getPlatformAssetConfigURL());
    String[] ids = args.length > 0 ? args : new String[] {"hero", "mira", "eren", "goblin", "orc", "warden", "shaman", "king"};
    for (String id : ids) {
      CharacterFactory.Rig rig = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
      Map<String, Joint> byName = new HashMap<>();
      for (Joint j : rig.skinning().getArmature().getJointList()) byName.put(j.getName(), j);
      for (String clip : CharacterFactory.CLIPS) {
        AnimClip c = rig.composer().getAnimClip(clip);
        float[] times = null;
        TreeMap<Float, Double> devByTime = new TreeMap<>();
        for (AnimTrack<?> t : c.getTracks()) {
          if (!(t instanceof TransformTrack tt)) continue;
          if (!(tt.getTarget() instanceof Joint j)) continue;
          Quaternion bind = j.getInitialTransform().getRotation();
          Quaternion[] rots = tt.getRotations();
          float[] ts = tt.getTimes();
          if (rots == null) continue;
          for (int k = 0; k < rots.length; k++) {
            double dot = Math.abs(bind.dot(rots[k]));
            double deg = Math.toDegrees(2 * Math.acos(Math.min(1, dot)));
            devByTime.merge(ts[k], deg, Math::max);
          }
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        float leaves = -1;
        for (var e : devByTime.entrySet()) {
          if (n < 6) sb.append(String.format(Locale.ROOT, " %.3f:%.1f", e.getKey(), e.getValue()));
          if (leaves < 0 && e.getValue() > 10) leaves = e.getKey();
          n++;
        }
        System.out.printf(Locale.ROOT, "%-7s %-8s len=%.3f keys=%d leavesBindAt=%.3f first:%s%n",
            id, clip, c.getLength(), devByTime.size(), leaves, sb);
      }
    }
  }
}
