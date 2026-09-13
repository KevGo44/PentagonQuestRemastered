package de.pentagon.assets;

import com.jme3.math.*;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.util.*;

/**
 * A cluster of faceted shards that reads as a crystal from any angle. The placeholder was a
 * four-by-five sphere under an unshaded material: a flat cyan cut-out on every screenshot. These
 * shards carry flat normals, so a lit material shows their facets while the emissive term keeps
 * them glowing.
 *
 * <p>Authored in the placeholder's frame so every caller's scale stays valid: the cluster fits in
 * x and z within ±0.5 and rises from y = −1 (the buried root) to y = +1 (the tallest tip), the
 * same box the unit sphere filled.
 */
public final class CrystalShapes {
  private CrystalShapes() {}

  public static Mesh cluster(long seed) {
    Random random = new Random(seed);
    List<Vector3f> positions = new ArrayList<>(), normals = new ArrayList<>();
    int shards = 3 + random.nextInt(2);
    for (int i = 0; i < shards; i++) {
      boolean main = i == 0;
      // Chunky, not grassy: the first cut had shards a tenth as wide as tall and they read as
      // blades of light on every screenshot.
      float height = main ? 2f : .9f + random.nextFloat() * .5f;
      float radius = main ? .38f : .18f + random.nextFloat() * .10f;
      float tilt = main ? random.nextFloat() * .12f : .3f + random.nextFloat() * .35f;
      float around = random.nextFloat() * FastMath.TWO_PI;
      Vector3f base =
          main
              ? new Vector3f(0, -1, 0)
              : new Vector3f(
                  FastMath.cos(around) * (.18f + random.nextFloat() * .14f),
                  -1,
                  FastMath.sin(around) * (.18f + random.nextFloat() * .14f));
      Quaternion lean =
          new Quaternion()
              .fromAngleAxis(tilt, new Vector3f(FastMath.cos(around), 0, FastMath.sin(around)))
              .mult(new Quaternion().fromAngleAxis(random.nextFloat() * FastMath.TWO_PI, Vector3f.UNIT_Y));
      shard(positions, normals, base, lean, radius, height);
    }
    Mesh mesh = new Mesh();
    mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(positions.toArray(Vector3f[]::new)));
    mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(Vector3f[]::new)));
    int[] indices = new int[positions.size()];
    for (int i = 0; i < indices.length; i++) indices[i] = i;
    mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indices));
    mesh.updateBound();
    mesh.updateCounts();
    return mesh;
  }

  /** A hexagonal column with a pointed tip: six side quads and six tip triangles, flat-shaded. */
  private static void shard(
      List<Vector3f> positions,
      List<Vector3f> normals,
      Vector3f base,
      Quaternion lean,
      float radius,
      float height) {
    float shoulder = height * .72f;
    Vector3f[] lower = new Vector3f[6], upper = new Vector3f[6];
    for (int k = 0; k < 6; k++) {
      float a = k * FastMath.TWO_PI / 6;
      lower[k] = new Vector3f(FastMath.cos(a) * radius, 0, FastMath.sin(a) * radius);
      upper[k] = new Vector3f(FastMath.cos(a) * radius * .85f, shoulder, FastMath.sin(a) * radius * .85f);
    }
    Vector3f tip = new Vector3f(0, height, 0);
    for (int k = 0; k < 6; k++) {
      int n = (k + 1) % 6;
      quad(positions, normals, base, lean, lower[k], lower[n], upper[n], upper[k]);
      tri(positions, normals, base, lean, upper[k], upper[n], tip);
    }
  }

  private static void quad(
      List<Vector3f> p, List<Vector3f> n, Vector3f base, Quaternion q,
      Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
    tri(p, n, base, q, a, b, c);
    tri(p, n, base, q, a, c, d);
  }

  private static void tri(
      List<Vector3f> p, List<Vector3f> n, Vector3f base, Quaternion q,
      Vector3f a, Vector3f b, Vector3f c) {
    Vector3f wa = q.mult(a).addLocal(base), wb = q.mult(b).addLocal(base), wc = q.mult(c).addLocal(base);
    Vector3f normal = wb.subtract(wa).crossLocal(wc.subtract(wa)).normalizeLocal();
    // Wind the face outward: the centroid of a convex shard lies on the inner side of every face.
    Vector3f centre = q.mult(new Vector3f(0, .5f, 0)).addLocal(base);
    if (normal.dot(wa.subtract(centre)) < 0) {
      Vector3f t = wb;
      wb = wc;
      wc = t;
      normal.negateLocal();
    }
    p.add(wa);
    p.add(wb);
    p.add(wc);
    n.add(normal);
    n.add(normal.clone());
    n.add(normal.clone());
  }
}
