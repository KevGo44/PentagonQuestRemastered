import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Random;
import javax.imageio.ImageIO;

/** Offline deterministic first-party placeholders. Run: java tools/AssetBaker.java */
public class AssetBaker {
  static Path root = Path.of("src/main/resources");

  /**
   * Texture edge length. One repeat covers a 2.8 m dungeon cell (DungeonLayout.CELL), so 256 gave
   * only ~91 px per metre on walls and floors the player stands directly against. 512 doubles that
   * to ~183 px/m. Measured cost of the eight shipped maps: 0.41 MB at 256, 1.95 MB at 512, 6.58 MB
   * at 1024 (~2.8 / 11.2 / 44.7 MB of RGBA8 plus mips in VRAM). 1024 was rejected: the briefing
   * warns that binaries of that size stay in the git history forever, and AtmosphereFilter's
   * height fog removes the extra detail past a few metres anyway. Keep it a power of two so mip
   * generation halves cleanly.
   */
  static final int RES = 512;

  /**
   * Joint lines per tile on both axes. Eight courses over the 3.4 m vertical wall repeat (the wall
   * box scales its texture coordinates by 2) gives a ~0.43 m block course, and eight columns over
   * 2.8 m gives stones of 0.35 m or 0.70 m depending on which optional joints exist.
   */
  static final int LATTICE = 8;

  enum Surface {
    STONE,
    METAL,
    WOOD
  }

  public static void main(String[] args) throws Exception {
    Files.createDirectories(root.resolve("textures"));
    Files.createDirectories(root.resolve("fonts"));
    Files.createDirectories(root.resolve("audio"));
    texture("stone", Surface.STONE);
    texture("metal", Surface.METAL);
    // Generated but deliberately not wired up: AssetPipeline.pbr("") leaves the chest, the
    // carpets and the banners with no maps at all. Switching those call sites to "wood" is a
    // WorldView change and is left to whoever owns that file.
    texture("wood", Surface.WOOD);
    font("body", "SansSerif", 32);
    font("title", "Serif", 64);
    sound("exploration", 12, 0);
    sound("combat", 12, 1);
    sound("ambient", 8, 2);
    sound("swing", .28, 3);
    sound("hit", .32, 4);
    sound("parry", .5, 5);
    sound("spell", .6, 6);
    sound("chime", 1.2, 7);
    sound("step", .16, 8);
    sound("hurt", .45, 9);
    System.out.println("Baked 12 PBR maps, 2 font atlases and 10 PCM sound assets.");
  }

  // ------------------------------------------------------------------ textures

  /**
   * Every field below is built on lattices that wrap at their own period and on pixel-position
   * hashes rather than a running Random, so the maps tile exactly by construction instead of by
   * luck. The previous generator tested the mortar with {@code y % 64 < 3}, which put a joint at
   * row 0 but none at row 255 and left a hard visible seam on every vertical wrap.
   */
  static void texture(String name, Surface surface) throws Exception {
    int n = RES;
    BufferedImage albedo = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB),
        normal = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB),
        rough = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB),
        metallic = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
    float[][] height = new float[n][n];
    // Traced before the per-pixel pass so albedo, height and roughness can all react to the same
    // line; a crack that only darkens the albedo reads as a painted-on scratch.
    float[][] crack =
        surface == Surface.METAL ? new float[n][n] : cracks(n, surface == Surface.STONE ? 11 : 5);
    for (int y = 0; y < n; y++)
      for (int x = 0; x < n; x++)
        switch (surface) {
          case STONE -> stone(x, y, n, crack[x][y], height, albedo, rough, metallic);
          case METAL -> metal(x, y, n, height, albedo, rough, metallic);
          case WOOD -> wood(x, y, n, crack[x][y], height, albedo, rough, metallic);
        }
    // Stone carries a deep joint trough, so a modest slope gain is enough; plate and plank relief
    // is shallow and spread over many pixels and needs far more gain to register at all.
    normals(height, normal, surface == Surface.STONE ? 3.2f : 9f);
    write(albedo, name, "albedo");
    write(normal, name, "normal");
    write(rough, name, "roughness");
    write(metallic, name, "metallic");
  }

  /**
   * Ashlar masonry. The visual budget goes to block-to-block identity first: the old map painted
   * every stone the same grey and let a single white-noise term do all the work, which reads as
   * one flat wall no matter how good the normal map is.
   */
  static void stone(
      int x,
      int y,
      int n,
      float crack,
      float[][] height,
      BufferedImage albedo,
      BufferedImage rough,
      BufferedImage metallic) {
    int cell = n / LATTICE, course = y / cell;
    // A free pixel shift per course, not a multiple of the cell. Any integer shift still wraps,
    // because the joint set is periodic in sx with period n - but a shift locked to the cell grid
    // put a joint at x == 0 in every single course, which drew one continuous vertical mortar line
    // down the whole wall at every horizontal repeat.
    int shift = (int) (hash01(course, 91, 5) * n), sx = (x + shift) % n;
    int index = sx / cell, left = index, right = (index + 1) % LATTICE;
    while (!joint(left, course)) left = (left + LATTICE - 1) % LATTICE;
    while (!joint(right, course)) right = (right + 1) % LATTICE;

    float dv = Math.min(y % cell, cell - y % cell), dh = cell;
    for (int i = 0; i < LATTICE; i++)
      if (joint(i, course)) {
        float d = Math.abs(sx - i * cell);
        dh = Math.min(dh, Math.min(d, n - d));
      }
    float d = Math.min(dh, dv);
    // Two stages instead of one hard step: a narrow deep trough for the joint itself inside a
    // wider chamfer for the broken block edge. The old 3 px cut aliased into a dark grid at
    // grazing angles and gave the normal map a vertical cliff. The chamfer is kept mostly out of
    // the albedo - weighting it there framed every stone in a dark vignette and the whole wall
    // read as glazed subway tile.
    float trough = 1 - smoothstep(n / 170f, n / 60f, d),
        chamfer = 1 - smoothstep(n / 60f, n / 26f, d);

    float u = (x + .5f) / n, v = (y + .5f) / n;
    // Everything derived from the block identity is faded out inside the trough: mortar is mortar,
    // not stone, and without this the identity of two different blocks met head-on at the vertical
    // texture wrap and showed as a step even though the joint itself was continuous.
    float body = 1 - trough;
    float tone = (hash01(left, course, 21) * 2 - 1) * body,
        lift = (hash01(left, course, 33) * 2 - 1) * body,
        warmth = (hash01(left, course, 47) * 2 - 1) * body,
        coarse = (hash01(left, course, 59) * 2 - 1) * body;
    int width = ((right - left + LATTICE) % LATTICE) * cell;
    float lu = Math.floorMod(sx - left * cell, n) / (float) width, lv = (y % cell) / (float) cell;
    // Plateau over the middle of the stone face: where boots and hands actually touch it.
    float face = bump(lu) * bump(lv) * body;

    // Five octaves starting at a 16 px lattice, not one broad term: the detail has to reach down
    // to a few pixels or the block face stays a smooth cloud and the extra resolution is wasted.
    float meso = fbm(u, v, LATTICE * 4, 5, 11) - .5f,
        grain = fbm(u, v, n / 8, 2, 23) - .5f, // sandy tooth just above the mip floor
        aggregate = worley(u, v, 64, 31) - .5f, // hard inclusions, grouped the way a sum cannot
        damp = smoothstep(.46f, .80f, fbm(u, v, 3, 3, 41)), // standing moisture and grime
        streak = fbm2(u, v, 26, 3, 3, 53) - .5f; // faint run-off, high across, low along
    float bleed = crack * (1 - chamfer); // a crack stops where the mortar starts

    height[x][y] =
        .62f
            + lift * .035f
            + meso * .070f
            + aggregate * .030f
            + grain * .020f
            - chamfer * .050f
            - trough * .40f
            - bleed * .30f;

    // Base and ceiling are tuned so the finished map lands on the old map's mean level
    // (~185/255). WorldView's light rig and AtmosphereFilter are balanced against that mean and
    // are not ours to retune, so a darker albedo would quietly darken the whole dungeon.
    float lum =
        .855f
            + tone * .075f
            + meso * .105f
            + aggregate * .050f
            + grain * .030f
            + streak * .025f
            + face * .015f
            - chamfer * .055f
            - trough * .300f
            - damp * .150f
            - bleed * .300f;
    lum = clamp(lum, .26f, .94f);
    // Region.stone multiplies over this, so the albedo itself has to stay close to neutral: a
    // +-3 % warm/cool split per stone reads as different quarried rock without moving the palette.
    float warm = warmth * .028f + streak * .010f - damp * .012f;
    put(albedo, x, y, lum * (1 + warm), lum * (1 + warm * .15f + damp * .012f), lum * (1 - warm));

    float r =
        .880f
            + trough * .070f
            + chamfer * .030f // mortar keeps the most tooth
            + coarse * .020f
            + meso * .050f
            - damp * .170f // standing water fills the pores
            - face * .085f; // treads and handled edges polish down
    // STYLE.md 1.2 puts the floor for readable stone at Roughness 0.55 and WorldView's smallest
    // stone multiplier is 0.85 (the shrine), so this map must not drop below 0.55/0.85 = 0.647.
    r = clamp(r, .682f, .972f);
    put(rough, x, y, r, r, r);
    // Stone is a dielectric and every WorldView stone material already passes Metallic 0, which
    // multiplies this map away. Flat black is the honest value, not a decorative gradient.
    put(metallic, x, y, 0, 0, 0);
  }

  /**
   * Riveted, hammered, corroded plate. Metal used to share stone's height field verbatim, so
   * every cornice and chest band carried a brick pattern.
   */
  static void metal(
      int x,
      int y,
      int n,
      float[][] height,
      BufferedImage albedo,
      BufferedImage rough,
      BufferedImage metallic) {
    int plate = n / 2;
    float u = (x + .5f) / n, v = (y + .5f) / n;
    float dp = Math.min(Math.min(x % plate, plate - x % plate), Math.min(y % plate, plate - y % plate));
    float seam = 1 - smoothstep(n / 200f, n / 50f, dp);
    float rivet = rivets(x, y, n, plate, 1f);

    // A dome ring just outside each head: the dark seating shadow is what actually makes a rivet
    // read as a separate piece of hardware rather than as a light speck.
    float seat = clamp(rivets(x, y, n, plate, 1.5f) - rivet * 1.4f, 0, 1);

    // Facets, not blobs. worley() alone gives a smooth distance field; hashing the winning cell
    // gives each planished area its own flat tone, which is how hammered iron actually reads.
    // 42 cells over the tile is a ~12 px facet. At 13 cells the facets were 40 px across and read
    // as camouflage patches; they also have to survive the cornice box, which stretches this map
    // across 3.08 m of width against 0.32 m of height.
    float facet = facet(u, v, 42, 67) - .5f,
        dimple = 1 - worley(u, v, 42, 67), // the dished centre of each of those facets
        // Pitting clusters where the patina already sits; scattered evenly it looked like a
        // printed halftone rather than corrosion.
        // Several small patches rather than one large one: at 9 cells the corrosion formed a
        // single distinctive shape, and a distinctive shape is exactly what the eye picks up as
        // repetition once the map tiles across a run of cornices.
        patina = smoothstep(.48f, .70f, fbm(u, v, 16, 4, 83)),
        pit = (1 - smoothstep(.12f, .34f, worley(u, v, 34, 71))) * clamp(.25f + patina, 0, 1),
        scale = fbm(u, v, 8, 4, 73) - .5f, // broad forge scale
        file = fbm2(u, v, 96, 5, 3, 89) - .5f, // grinding striations, anisotropic on purpose
        tooth = fbm(u, v, n / 16, 2, 79) - .5f,
        lift = hash01(x / plate, y / plate, 97) - .5f;

    height[x][y] =
        .55f
            + lift * .04f
            - seam * .24f
            + rivet * .16f
            - seat * .05f
            + facet * .022f
            - dimple * .030f
            - pit * .055f
            + scale * .04f
            + file * .025f
            + tooth * .02f;

    // STYLE.md 1.2: the environment probe is a single constant pixel, so there is nothing for the
    // shader to reflect. Rivet crowns and rubbed facet edges are painted bright here instead.
    float polish = clamp(rivet * 1.5f + Math.max(0, facet) * 1.3f, 0, 1) * (1 - patina);
    // Same argument as for stone: hold the old mean (~182/255) so the cornices do not get darker
    // than they were, on top of the Metallic drop that is already changing how they read.
    float lum =
        .735f
            + polish * .150f
            + facet * .045f
            + scale * .050f
            + file * .055f
            + tooth * .030f
            - seat * .150f
            - patina * .170f
            - pit * .150f
            - seam * .150f;
    lum = clamp(lum, .18f, .95f);
    float warm = patina * .030f + pit * .015f - polish * .012f;
    put(albedo, x, y, lum * (1 + warm), lum * (1 + warm * .3f), lum * (1 - warm));

    // STYLE.md 1.2 wants iron at Roughness >= 0.55, but WorldView multiplies this map by 0.5
    // (iron) and 0.45 (gold), so 0.55 is unreachable from the texture side at all. The map is
    // pushed as high as it can usefully go, which lands the product in the 0.35-0.55 band the
    // material test reads as dull iron rather than the old 0.27.
    float r =
        clamp(
            .935f + patina * .045f + pit * .030f + tooth * .030f + file * .020f - polish * .125f,
            .80f,
            .988f);
    put(rough, x, y, r, r, r);
    // 0.498 * 0.8 = 0.398 keeps iron just under STYLE.md's Metallic <= 0.4 ceiling; the old flat
    // 0xee mapped to 0.746 and sank the cornices almost to black under the constant probe.
    float m = clamp(.46f + polish * .04f - patina * .28f - pit * .12f, .15f, .498f);
    put(metallic, x, y, m, m, m);
  }

  /** Planked board with running grain, for the chest and the banners. Not referenced by code yet. */
  static void wood(
      int x,
      int y,
      int n,
      float crack,
      float[][] height,
      BufferedImage albedo,
      BufferedImage rough,
      BufferedImage metallic) {
    int board = n / 4, index = y / board;
    float u = (x + .5f) / n, v = (y + .5f) / n;
    float db = Math.min(y % board, board - y % board);
    float gap = 1 - smoothstep(n / 256f, n / 80f, db);
    float tone = hash01(index, 3, 101) * 2 - 1;
    // Grain flows along the plank: low frequency across x, high frequency across y.
    float flow = fbm2(u, v, 3, 13, 3, 103) - .5f;
    // 26 is even, so the ring pattern closes on itself at the vertical wrap.
    float rings = (float) Math.abs(Math.sin(Math.PI * (v * 26 + flow * 5)));
    float fibre = fbm2(u, v, 5, n / 8, 2, 107) - .5f;
    float knot = 1 - smoothstep(0f, .20f, worley(u, v, 6, 109));
    float wear = smoothstep(.45f, .80f, fbm(u, v, 3, 3, 113));
    // A split runs along a board, it does not jump the gap to the next one.
    float split = crack * (1 - gap);

    height[x][y] =
        .60f
            + tone * .02f
            - gap * .34f
            - rings * .030f
            + fibre * .020f
            - knot * .045f
            - split * .12f;

    float lum =
        .700f + tone * .055f - rings * .105f + fibre * .040f - knot * .140f - gap * .240f
            - wear * .050f - split * .20f;
    lum = clamp(lum, .22f, .88f);
    // The chest tint 0x614b36 supplies the brown; the map only carries the light/dark of the
    // grain, plus a hair of extra warmth in the late-wood rings.
    float warm = rings * .030f + knot * .020f;
    put(albedo, x, y, lum * (1 + warm), lum * (1 + warm * .25f), lum * (1 - warm));

    // WorldView's chest passes Roughness 0.75, so 0.55/0.75 = 0.733 is the floor here.
    float r = clamp(.900f + rings * .050f + knot * .040f - wear * .130f + fibre * .030f, .740f, .985f);
    put(rough, x, y, r, r, r);
    put(metallic, x, y, 0, 0, 0);
  }

  /**
   * Every second lattice line always carries a joint, the ones in between only sometimes. That
   * mixes full and half-width stones without ever leaving the wrapping lattice.
   */
  static boolean joint(int i, int course) {
    return i % 4 == 0 || hash01(i, course, 13) > .45f;
  }

  /**
   * Rivet domes spaced along the plate seams. Only the nearest head on each axis is evaluated, and
   * the head centres sit on a lattice that divides the texture, so no rivet is cut by the wrap.
   */
  static float rivets(int x, int y, int n, int plate, float grow) {
    int step = plate / 4;
    // n/44 leaves a ~12 px head against a 64 px spacing. At n/72 the heads were pinpricks that
    // read as dirt specks instead of hardware. `grow` widens the same field for the seating ring.
    float radius = n / 44f * grow;
    int sx = Math.round(x / (float) plate) * plate, sy = Math.round(y / (float) plate) * plate;
    int kx = (int) Math.floor((x - step / 2f) / step) * step + step / 2,
        ky = (int) Math.floor((y - step / 2f) / step) * step + step / 2;
    float best = 0;
    for (int a = 0; a <= 1; a++) {
      best = Math.max(best, dome(x, y, sx, ky + a * step, radius, n));
      best = Math.max(best, dome(x, y, kx + a * step, sy, radius, n));
    }
    return best;
  }

  /** Hemisphere, with the rim eased off: raw sqrt(1-d^2) has infinite slope at d=1 and would
   * stamp a hard ring into the normal map. */
  static float dome(int x, int y, int cx, int cy, float radius, int n) {
    float dx = wrap(x - cx, n) / radius, dy = wrap(y - cy, n) / radius;
    float d2 = dx * dx + dy * dy;
    if (d2 >= 1) return 0;
    return (float) Math.sqrt(1 - d2) * smoothstep(1f, .75f, (float) Math.sqrt(d2));
  }

  /** Wandering hairlines stamped into a wrapping buffer, so a crack that leaves one edge carries
   * on at the opposite one. Fixed seed keeps the map reproducible. */
  static float[][] cracks(int n, int count) {
    float[][] field = new float[n][n];
    Random rng = new Random(4141);
    for (int c = 0; c < count; c++) {
      double px = rng.nextDouble() * n, py = rng.nextDouble() * n, angle = rng.nextDouble() * Math.PI * 2;
      int steps = n / 3 + rng.nextInt(n / 2);
      float radius = (.9f + rng.nextFloat() * 1.2f) * n / 512f;
      for (int s = 0; s < steps; s++) {
        angle += (rng.nextDouble() - .5) * .34;
        px += Math.cos(angle);
        py += Math.sin(angle);
        // Taper both ends so a crack does not start or stop in mid-air.
        stamp(field, n, px, py, radius, (float) Math.sin(Math.PI * (s + .5) / steps));
      }
    }
    return field;
  }

  static void stamp(float[][] field, int n, double px, double py, float radius, float weight) {
    int cx = (int) Math.floor(px), cy = (int) Math.floor(py), r = (int) Math.ceil(radius) + 1;
    for (int dy = -r; dy <= r; dy++)
      for (int dx = -r; dx <= r; dx++) {
        double d = Math.hypot(cx + dx + .5 - px, cy + dy + .5 - py);
        float a = weight * (1 - smoothstep(0, radius, (float) d));
        int ix = Math.floorMod(cx + dx, n), iy = Math.floorMod(cy + dy, n);
        // Max, not sum: a walk that crosses itself must not punch a hole.
        if (a > field[ix][iy]) field[ix][iy] = a;
      }
  }

  /**
   * Sobel rather than the central difference the old generator used: the 3x3 kernel averages along
   * the perpendicular axis, which keeps the fine grain from turning into single-pixel spikes that
   * shimmer once mips kick in. Gradients scale with 1/RES for a fixed feature size, so they are
   * renormalised by n/512 to keep the relief the same at any resolution. The (-dx, -dy, +1) sign
   * convention is kept exactly as it was: AssetPipeline pins NormalType 1 (OpenGL +Y) and a
   * flipped channel cannot be verified without running the game.
   */
  static void normals(float[][] height, BufferedImage normal, float strength) {
    int n = height.length;
    float k = strength * n / 512f;
    for (int y = 0; y < n; y++)
      for (int x = 0; x < n; x++) {
        int xm = (x + n - 1) % n, xp = (x + 1) % n, ym = (y + n - 1) % n, yp = (y + 1) % n;
        float dx =
            (height[xp][ym]
                    + 2 * height[xp][y]
                    + height[xp][yp]
                    - height[xm][ym]
                    - 2 * height[xm][y]
                    - height[xm][yp])
                * .25f
                * k;
        float dy =
            (height[xm][yp]
                    + 2 * height[x][yp]
                    + height[xp][yp]
                    - height[xm][ym]
                    - 2 * height[x][ym]
                    - height[xp][ym])
                * .25f
                * k;
        float length = (float) Math.sqrt(dx * dx + dy * dy + 1);
        put(normal, x, y, -dx / length * .5f + .5f, -dy / length * .5f + .5f, 1 / length * .5f + .5f);
      }
  }

  // ------------------------------------------------------- procedural helpers

  /**
   * Position hash instead of a running Random. The old generator drew from a single Random in
   * scan order, so a value could not be looked up again for a wrapped neighbour and nothing could
   * be made to tile.
   */
  static float hash01(int x, int y, int seed) {
    int h = x * 374761393 + y * 668265263 + seed * 1274126177;
    h = (h ^ (h >>> 13)) * 1274126177;
    h ^= h >>> 16;
    return (h >>> 8) / (float) (1 << 24);
  }

  /** Value noise on a lattice that wraps at its own period, which is what makes the map tileable
   * regardless of whether the period divides the texture size. */
  static float value(float u, float v, int px, int py, int seed) {
    float fx = u * px, fy = v * py;
    int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
    float tx = smooth(fx - x0), ty = smooth(fy - y0);
    int xa = Math.floorMod(x0, px), ya = Math.floorMod(y0, py);
    int xb = (xa + 1) % px, yb = (ya + 1) % py;
    return lerp(
        lerp(hash01(xa, ya, seed), hash01(xb, ya, seed), tx),
        lerp(hash01(xa, yb, seed), hash01(xb, yb, seed), tx),
        ty);
  }

  static float fbm(float u, float v, int period, int octaves, int seed) {
    return fbm2(u, v, period, period, octaves, seed);
  }

  static float fbm2(float u, float v, int px, int py, int octaves, int seed) {
    float sum = 0, amp = 1, norm = 0;
    for (int o = 0; o < octaves; o++) {
      sum += amp * value(u, v, px << o, py << o, seed + o * 131);
      norm += amp;
      amp *= .5f;
    }
    return sum / norm;
  }

  /** Cellular F1 distance on a wrapping grid; gives grouped blotches that a value-noise sum
   * cannot, which is what makes stone read as crystalline rather than cloudy. */
  static float worley(float u, float v, int cells, int seed) {
    float fx = u * cells, fy = v * cells;
    int cx = (int) Math.floor(fx), cy = (int) Math.floor(fy);
    float best = 9;
    for (int dy = -1; dy <= 1; dy++)
      for (int dx = -1; dx <= 1; dx++) {
        int gx = cx + dx, gy = cy + dy;
        int wx = Math.floorMod(gx, cells), wy = Math.floorMod(gy, cells);
        float ex = gx + hash01(wx, wy, seed) - fx, ey = gy + hash01(wx, wy, seed + 77) - fy;
        best = Math.min(best, ex * ex + ey * ey);
      }
    return Math.min(1, (float) Math.sqrt(best));
  }

  /** Same wrapping cellular grid, but returns the winning cell's own hash instead of the distance
   * to it. Gives flat-toned facets with hard borders, which is what a distance field cannot. */
  static float facet(float u, float v, int cells, int seed) {
    float fx = u * cells, fy = v * cells;
    int cx = (int) Math.floor(fx), cy = (int) Math.floor(fy);
    float best = 9, tone = 0;
    for (int dy = -1; dy <= 1; dy++)
      for (int dx = -1; dx <= 1; dx++) {
        int wx = Math.floorMod(cx + dx, cells), wy = Math.floorMod(cy + dy, cells);
        float ex = cx + dx + hash01(wx, wy, seed) - fx, ey = cy + dy + hash01(wx, wy, seed + 77) - fy;
        float d = ex * ex + ey * ey;
        if (d < best) {
          best = d;
          tone = hash01(wx, wy, seed + 313);
        }
      }
    return tone;
  }

  /** Plateau over the middle of a face, easing off towards its edges. Kept narrow so the polish
   * reads as a rubbed centre rather than as a soft vignette over the whole stone. */
  static float bump(float t) {
    return smoothstep(0f, .26f, 1 - Math.abs(2 * t - 1));
  }

  static float smooth(float t) {
    return t * t * (3 - 2 * t);
  }

  static float smoothstep(float e0, float e1, float t) {
    float k = clamp((t - e0) / (e1 - e0), 0, 1);
    return k * k * (3 - 2 * k);
  }

  static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  static float clamp(float v, float lo, float hi) {
    return v < lo ? lo : v > hi ? hi : v;
  }

  static float wrap(int d, int n) {
    int w = Math.floorMod(d, n);
    return w > n / 2 ? w - n : w;
  }

  static void put(BufferedImage image, int x, int y, float r, float g, float b) {
    image.setRGB(x, y, (level(r) << 16) | (level(g) << 8) | level(b));
  }

  static int level(float v) {
    return (int) clamp(Math.round(v * 255), 0, 255);
  }

  static void write(BufferedImage image, String name, String map) throws Exception {
    ImageIO.write(image, "png", root.resolve("textures/" + name + "-" + map + ".png").toFile());
  }

  static void font(String name, String family, int size) throws Exception {
    int cell = size + 12, width = cell * 16, height = cell * 14;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setFont(new Font(family, Font.PLAIN, size));
    g.setColor(Color.WHITE);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    FontMetrics m = g.getFontMetrics();
    StringBuilder f =
        new StringBuilder(
            "info face=\""
                + family
                + "\" size="
                + size
                + " bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1"
                + " padding=0,0,0,0 spacing=1,1\n"
                + "common lineHeight="
                + m.getHeight()
                + " base="
                + m.getAscent()
                + " scaleW="
                + width
                + " scaleH="
                + height
                + " pages=1 packed=0\npage id=0 file=\""
                + name
                + ".png\"\nchars count=224\n");
    for (int c = 32; c < 256; c++) {
      int x = ((c - 32) % 16) * cell, y = ((c - 32) / 16) * cell;
      g.drawString(String.valueOf((char) c), x + 2, y + m.getAscent());
      f.append("char id=")
          .append(c)
          .append(" x=")
          .append(x)
          .append(" y=")
          .append(y)
          .append(" width=")
          .append(Math.min(cell, m.charWidth(c) + 4))
          .append(" height=")
          .append(m.getHeight())
          .append(" xoffset=-2 yoffset=0 xadvance=")
          .append(m.charWidth(c))
          .append(" page=0 chnl=15\n");
    }
    g.dispose();
    ImageIO.write(image, "png", root.resolve("fonts/" + name + ".png").toFile());
    Files.writeString(root.resolve("fonts/" + name + ".fnt"), f, StandardCharsets.UTF_8);
  }

  static void sound(String name, double duration, int kind) throws Exception {
    int rate = 22050, n = (int) (duration * rate);
    byte[] pcm = new byte[n * 2];
    Random rng = new Random(kind + 1982);
    double smooth = 0;
    for (int i = 0; i < n; i++) {
      double t = i / (double) rate, noise = rng.nextDouble() * 2 - 1;
      smooth = smooth * .96 + noise * .04;
      double v = 0;
      if (kind == 0) {
        double pulse = .65 + .35 * Math.sin(2 * Math.PI * t / 6);
        v =
            (Math.sin(2 * Math.PI * 55 * t) * .11
                    + Math.sin(2 * Math.PI * 82.5 * t) * .06
                    + Math.sin(2 * Math.PI * 110 * t) * .04)
                * pulse;
      }
      if (kind == 1) {
        double beat = t % .5;
        v =
            Math.sin(2 * Math.PI * (70 * beat - 45 * beat * beat)) * Math.exp(-beat * 15) * .34
                + Math.sin(2 * Math.PI * 110 * t) * .04
                + Math.sin(2 * Math.PI * 164 * t) * .035;
      }
      if (kind == 2) v = smooth * .4 + Math.sin(2 * Math.PI * 40 * t) * .02;
      if (kind == 3) v = noise * .3 * Math.sin(Math.PI * t / duration) * Math.exp(-t * 8);
      if (kind == 4) v = (noise * .45 + Math.sin(2 * Math.PI * 95 * t) * .4) * Math.exp(-t * 18);
      if (kind == 5)
        v =
            (Math.sin(2 * Math.PI * 970 * t) + Math.sin(2 * Math.PI * 1437 * t))
                * .22
                * Math.exp(-t * 9);
      if (kind == 6)
        v =
            (Math.sin(2 * Math.PI * (240 * t + 420 * t * t)) * .2 + noise * .06)
                * Math.sin(Math.PI * t / duration);
      if (kind == 7)
        v =
            (Math.sin(2 * Math.PI * 440 * t)
                    + Math.sin(2 * Math.PI * 660 * t)
                    + Math.sin(2 * Math.PI * 880 * t))
                * .12
                * Math.exp(-t * 3);
      if (kind == 8) v = (noise * .18 + Math.sin(2 * Math.PI * 70 * t) * .15) * Math.exp(-t * 35);
      if (kind == 9)
        v = (Math.sin(2 * Math.PI * (120 * t - 45 * t * t)) * .3 + noise * .1) * Math.exp(-t * 8);
      double fade = Math.min(1, Math.min(t / .008, (duration - t) / .02));
      int value = (int) (Math.max(-1, Math.min(1, v * fade)) * 32767);
      pcm[2 * i] = (byte) value;
      pcm[2 * i + 1] = (byte) (value >> 8);
    }
    try (DataOutputStream out =
        new DataOutputStream(Files.newOutputStream(root.resolve("audio/" + name + ".wav")))) {
      out.writeBytes("RIFF");
      le(out, 36 + pcm.length, 4);
      out.writeBytes("WAVEfmt ");
      le(out, 16, 4);
      le(out, 1, 2);
      le(out, 1, 2);
      le(out, rate, 4);
      le(out, rate * 2, 4);
      le(out, 2, 2);
      le(out, 16, 2);
      out.writeBytes("data");
      le(out, pcm.length, 4);
      out.write(pcm);
    }
  }

  static void le(DataOutputStream out, int value, int count) throws Exception {
    for (int i = 0; i < count; i++) out.writeByte(value >> (8 * i));
  }
}
