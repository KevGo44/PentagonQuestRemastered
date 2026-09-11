package de.pentagon.combat;

import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import de.pentagon.assets.AssetPipeline;
import java.util.*;

public final class Effects {
  private static final int CAPACITY = 120;

  private record Spark(Geometry geometry, Vector3f velocity) {}

  private final Spark[] sparks = new Spark[CAPACITY];
  private final float[] life = new float[CAPACITY];
  private int cursor;
  private final Random random = new Random(901);
  public final Node root = new Node("CombatParticles");

  public Effects(AssetPipeline assets) {
    for (int i = 0; i < CAPACITY; i++) {
      Geometry g = assets.box("Spark", .035f, .035f, .09f, assets.glow(0xe5a764, 1));
      g.setShadowMode(ShadowMode.Off);
      g.setCullHint(Spatial.CullHint.Always);
      root.attachChild(g);
      sparks[i] = new Spark(g, new Vector3f());
    }
  }

  public void burst(Vector3f position, int count) {
    for (int i = 0; i < count; i++) {
      int k = cursor++ % CAPACITY;
      Spark s = sparks[k];
      s.geometry.setLocalTranslation(position);
      s.geometry.setCullHint(Spatial.CullHint.Inherit);
      s.velocity.set(
          (random.nextFloat() - .5f) * 5, random.nextFloat() * 4, (random.nextFloat() - .5f) * 5);
      life[k] = .25f + random.nextFloat() * .4f;
    }
  }

  public void update(float dt) {
    for (int i = 0; i < CAPACITY; i++)
      if (life[i] > 0) {
        life[i] -= dt;
        Spark s = sparks[i];
        if (life[i] <= 0) s.geometry.setCullHint(Spatial.CullHint.Always);
        else {
          s.geometry.move(s.velocity.x * dt, s.velocity.y * dt, s.velocity.z * dt);
          s.velocity.y -= 9 * dt;
        }
      }
  }

  public void clear() {
    Arrays.fill(life, 0);
    for (Spark s : sparks) s.geometry.setCullHint(Spatial.CullHint.Always);
  }
}
