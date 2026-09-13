package de.pentagon.assets;

import com.jme3.math.FastMath;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

/**
 * A tongue of fire as a lathe: narrow at the embers, fullest at a third of its height, drawn to a
 * point at the top. The first shrine fire was three cones, and a cone is a cone - a hard rim at the
 * base and a straight edge that no fire has. This profile has no rim (the base is a small disc that
 * sits inside the ember bed) and a belly, and three of them at different heights under an additive
 * material overlap into one luminous shape.
 *
 * <p>Authored with the base at the origin and the tip at y = 1, radius 0.87 at the belly (a third
 * of the way up); callers scale.
 */
public final class FlameShapes {
  private FlameShapes() {}

  public static Mesh tongue(int rings, int segments) {
    int count = (rings + 1) * (segments + 1);
    float[] positions = new float[count * 3];
    int p = 0;
    for (int r = 0; r <= rings; r++) {
      float t = r / (float) rings;
      float radius = profile(t);
      for (int s = 0; s <= segments; s++) {
        float a = s * FastMath.TWO_PI / segments;
        // A gentle twist keeps the facets from lining up into visible vertical ribs.
        a += t * .9f;
        positions[p++] = FastMath.cos(a) * radius;
        positions[p++] = t;
        positions[p++] = FastMath.sin(a) * radius;
      }
    }
    int[] indices = new int[rings * segments * 6];
    int i = 0;
    for (int r = 0; r < rings; r++)
      for (int s = 0; s < segments; s++) {
        int a = r * (segments + 1) + s, b = a + segments + 1;
        indices[i++] = a;
        indices[i++] = b;
        indices[i++] = a + 1;
        indices[i++] = a + 1;
        indices[i++] = b;
        indices[i++] = b + 1;
      }
    Mesh mesh = new Mesh();
    mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(positions));
    mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indices));
    mesh.updateBound();
    mesh.updateCounts();
    return mesh;
  }

  /** Radius over height: 0.3 at the base, 0.87 at the belly (t ≈ 0.38), 0 at the tip. */
  static float profile(float t) {
    if (t >= 1) return 0;
    float belly = FastMath.sin(FastMath.PI * FastMath.pow(t, .72f));
    return (.3f + .7f * belly) * FastMath.pow(1 - t, .3f);
  }
}
