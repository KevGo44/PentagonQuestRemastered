package de.pentagon.world;

import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import de.pentagon.assets.*;
import de.pentagon.core.GameSession;
import de.pentagon.physics.PhysicsWorld;
import java.util.*;
import java.util.function.Supplier;
import jme3tools.optimize.GeometryBatchFactory;

public final class WorldView {
  public final Node root = new Node("Dungeon"),
      occluders = new Node("CollisionGeometry"),
      decor = new Node("Decorations"),
      interactives = new Node("Interactives");
  public final Map<String, Node> objects = new LinkedHashMap<>();

  private record Torch(PointLight light, Vector3f position, float phase) {}

  /** A shrine fire: its tongues flicker in update(), each about its authored scale. */
  private record Flame(
      List<Spatial> tongues, List<Vector3f> scales, PointLight light, float phase) {}

  /** A filled kit slot; orientation and level of detail only apply to one of the two sources. */
  private record Slot(Spatial spatial, boolean module) {}

  /** Which wall module a cell wants, and the yaw that turns its authored front face outward. */
  private record WallKit(String id, float yaw) {}

  /** Passed where a module ships its final look because the placeholder colour is region-wide. */
  private static final int NO_TINT = -1;

  private final List<Torch> torches = new ArrayList<>();
  private final List<Flame> flames = new ArrayList<>();
  private float torchScale = 1;
  private final AssetPipeline assets;
  private final PhysicsWorld physics;
  private final Random random = new Random(51);
  private final Material iron, gold;
  private final int floorTint;
  private final Map<String, Spatial> modules = new HashMap<>();
  private final Map<Integer, Node> drops = new HashMap<>();
  public final DungeonLayout layout;

  public WorldView(
      AssetPipeline assets, PhysicsWorld physics, DungeonLayout layout, GameSession session) {
    this.assets = assets;
    this.physics = physics;
    this.layout = layout;
    // Dull, tarnished iron and brass within STYLE.md; the polished values (0.5 / 0.8 and
    // 0.45 / 0.65) read as black under the constant probe.
    iron = assets.pbr("metal", 0x45494e, .6f, .35f);
    gold = assets.pbr("metal", 0x987246, .55f, .4f);
    floorTint = layout.region == Region.CAVERNS ? 0x687771 : 0x8c8b86;
    root.attachChild(occluders);
    root.attachChild(decor);
    root.attachChild(interactives);
    root.setShadowMode(ShadowMode.CastAndReceive);
    buildTiles();
    buildRooms();
    for (var object : layout.objects) buildObject(object, session);
  }

  private void buildTiles() {
    Material wall = assets.pbr("stone", layout.region.stone, .94f, 0),
        floor = assets.pbr("stone", floorTint, .86f, 0);
    float cell = DungeonLayout.CELL;
    Map<String, Node> chunks = new HashMap<>();
    physics.box(42, -.35f, 42, 47, .35f, 47);
    for (int z = 0; z < DungeonLayout.SIZE; z++)
      for (int x = 0; x < DungeonLayout.SIZE; x++) {
        if (!layout.walkable(x, z) && !layout.wall(x, z)) continue;
        String chunk = x / 6 + ":" + z / 6;
        Node n = chunks.computeIfAbsent(chunk, k -> new Node("Chunk_" + k));
        // The caverns wear rock instead of masonry: cave variants of floor, ceiling and wall are
        // tried first and alternate by cell parity so the relief does not repeat cell by cell;
        // the masonry ids stay behind them as the fallback, and ribs and cornices - vault
        // details - are not built in a cave at all.
        boolean cave = layout.region == Region.CAVERNS;
        String variant = (x + z) % 2 == 0 ? "" : "2";
        if (layout.walkable(x, z)) {
          n.attachChild(
              slot(
                      floorTint,
                      x * cell,
                      -.13f,
                      z * cell,
                      .12f,
                      () -> assets.box("FloorTile", cell * .5f, .12f, cell * .5f, floor),
                      cave ? "kit_floor_cave" + variant : "kit_floor",
                      "kit_floor")
                  .spatial());
          // A broken ceiling admits isolated light shafts; it never blocks navigation.
          if (!(x % 7 == 1 && z % 7 == 1)) {
            Spatial ceiling =
                slot(
                        layout.region.stone,
                        x * cell,
                        7.1f,
                        z * cell,
                        .18f,
                        () -> assets.box("VaultCeiling", cell * .5f, .18f, cell * .5f, wall),
                        cave ? "kit_ceiling_cave" + variant : "kit_ceiling",
                        "kit_ceiling")
                    .spatial();
            ceiling.setShadowMode(ShadowMode.Receive);
            n.attachChild(ceiling);
          }
          if (x % 4 == 0 && !cave)
            n.attachChild(
                slot(
                        layout.region.stone,
                        x * cell,
                        6.4f,
                        z * cell,
                        .16f,
                        () -> assets.box("CeilingRib", .16f, .16f, cell * .5f, wall),
                        "kit_rib")
                    .spatial());
        } else {
          WallKit kit = wallKit(x, z);
          Slot block =
              slot(
                  layout.region.stone,
                  x * cell,
                  3.3f,
                  z * cell,
                  3.4f,
                  () -> {
                    Geometry g = assets.box("Wall", cell * .5f, 3.4f, cell * .5f, wall);
                    g.getMesh().scaleTextureCoordinates(new Vector2f(1, 2));
                    return g;
                  },
                  cave ? kit.id().replace("kit_wall_", "kit_wall_cave_") + variant : kit.id(),
                  cave ? kit.id().replace("kit_wall_", "kit_wall_cave_") : kit.id(),
                  kit.id(),
                  "kit_wall");
          // Only a module has a front face. The procedural block is square in plan, but its
          // texture repeat would turn with it, so it is never rotated.
          if (block.module())
            block
                .spatial()
                .setLocalRotation(new Quaternion().fromAngleAxis(kit.yaw(), Vector3f.UNIT_Y));
          n.attachChild(block.spatial());
          if (!cave)
            n.attachChild(
                slot(
                        NO_TINT,
                        x * cell,
                        4.8f,
                        z * cell,
                        .16f,
                        () -> assets.box("Cornice", cell * .55f, .16f, cell * .55f, iron),
                        "kit_cornice")
                    .spatial());
        }
      }
    // Merge adjacent wall colliders into runs; much cheaper than a body per brick. The runs come
    // from the layout, never from a module, so collision is identical with and without the kit.
    for (int z = 0; z < DungeonLayout.SIZE; z++) {
      int x = 0;
      while (x < DungeonLayout.SIZE) {
        if (!layout.wall(x, z)) {
          x++;
          continue;
        }
        int start = x;
        while (x < DungeonLayout.SIZE && layout.wall(x, z)) x++;
        physics.box(
            (start + x - 1) * cell / 2, 3.3f, z * cell, (x - start) * cell / 2, 3.4f, cell / 2);
      }
    }
    for (Node chunk : chunks.values()) {
      GeometryBatchFactory.optimize(chunk);
      occluders.attachChild(chunk);
    }
  }

  /**
   * Resolves a kit module through the single import gateway, trying the ids in order so a specific
   * variant can be added later without touching this class, and hands out a clone that shares the
   * prototype's materials. Cloning materials per cell would give every tile its own instance, and
   * GeometryBatchFactory groups by material — the chunk merge would then collapse into one batch
   * per tile.
   */
  private Spatial module(int tint, String... ids) {
    for (String id : ids) {
      if (!modules.containsKey(id)) {
        Spatial resolved = assets.model("props/" + id, () -> null);
        if (resolved != null && tint != NO_TINT) tint(resolved, tint);
        modules.put(id, resolved);
      }
      Spatial prototype = modules.get(id);
      if (prototype != null) return prototype.clone(false);
    }
    return null;
  }

  /**
   * Fills one kit slot. A module is placed {@code half} lower than the procedural box, because prop
   * pivots sit bottom-centre while a Box is centred on its translation. The underside of the block
   * therefore stays where it is, which is the surface the layout-derived colliders were measured
   * against.
   */
  private Slot slot(
      int tint, float x, float y, float z, float half, Supplier<Geometry> box, String... ids) {
    Spatial module = module(tint, ids);
    if (module == null) {
      Geometry g = box.get();
      g.setLocalTranslation(x, y, z);
      return new Slot(g, false);
    }
    module.setLocalTranslation(x, y - half, z);
    return new Slot(module, true);
  }

  /**
   * Region colour reaches a module the way it reaches the procedural stone: as a factor on
   * BaseColor, so the module keeps its own maps and UVs and only has to ship a neutral albedo. The
   * factor is normalised by its own luminance — the recipe recorded in docs/assets/asset-liste.md —
   * because the region tones are mid-greys and multiplying by one raw would darken the module by
   * roughly a factor of six. The material is cloned first: loadModel hands out a spatial whose
   * material still belongs to the asset cache, so writing through it would bleed into the next
   * region.
   */
  private void tint(Spatial module, int color) {
    ColorRGBA c = AssetPipeline.color(color);
    float luminance = .2126f * c.r + .7152f * c.g + .0722f * c.b;
    if (luminance <= 0) return;
    ColorRGBA factor = c.mult(1 / luminance);
    factor.a = 1;
    module.depthFirstTraversal(
        s -> {
          if (!(s instanceof Geometry g)) return;
          MatParam param = g.getMaterial().getParam("BaseColor");
          if (param == null || !(param.getValue() instanceof ColorRGBA base)) return;
          Material tinted = g.getMaterial().clone();
          tinted.setColor("BaseColor", base.mult(factor));
          g.setMaterial(tinted);
        });
  }

  /**
   * Classifies a wall cell by the sides that face walkable floor. Sides are indexed +Z, +X, -Z, -X,
   * one quarter turn about +Y apart, so a module authored facing +Z is aimed at side i by a yaw of
   * i quarter turns. DungeonLayout.wall only reports cells with at least one such side, so there is
   * always a face to show and kit_wall_free stays a reserve.
   */
  private WallKit wallKit(int x, int z) {
    boolean[] open = {
      layout.walkable(x, z + 1),
      layout.walkable(x + 1, z),
      layout.walkable(x, z - 1),
      layout.walkable(x - 1, z)
    };
    int count = 0;
    for (boolean side : open) if (side) count++;
    for (int i = 0; i < 4; i++) {
      if (count == 1 && open[i]) return new WallKit("kit_wall_face", i * FastMath.HALF_PI);
      if (count == 2 && open[i] && open[(i + 1) % 4])
        return new WallKit("kit_wall_corner", i * FastMath.HALF_PI);
      if (count == 2 && i < 2 && open[i] && open[(i + 2) % 4])
        return new WallKit("kit_wall_span", i * FastMath.HALF_PI);
      // The pier shows three faces and is aimed by its closed side, which faces -X unrotated.
      if (count == 3 && !open[(i + 3) % 4])
        return new WallKit("kit_wall_pier", i * FastMath.HALF_PI);
    }
    return new WallKit("kit_wall_free", 0);
  }

  private void buildRooms() {
    Material stone = assets.pbr("stone", layout.region.stone, .9f, 0);
    int carpetTint = layout.region == Region.REFUGE ? 0x243c48 : 0x492a2d;
    int index = 0;
    for (var room : layout.rooms) {
      float x = room.x() * DungeonLayout.CELL,
          z = room.z() * DungeonLayout.CELL,
          dx = (room.rx() - .65f) * DungeonLayout.CELL,
          dz = (room.rz() - .65f) * DungeonLayout.CELL;
      for (int sign : new int[] {-1, 1}) {
        if (layout.region == Region.CAVERNS) {
          rock(x + sign * dx, z + dz, 1.2f);
          rock(x + sign * dx, z - dz, .9f);
        } else {
          pillar(x + sign * dx, z - dz, stone);
          pillar(x + sign * dx, z + dz, stone);
        }
        torch(x + sign * (dx - .65f), z - dz + .4f, index++);
      }
      if (layout.region != Region.CAVERNS) {
        arch(x, z - dz + .6f, stone);
        float carpetX = Math.min(2.2f, dx * .4f), carpetZ = dz * .75f;
        Slot carpet =
            slot(
                carpetTint,
                x,
                .009f,
                z,
                .009f,
                () ->
                    assets.box(
                        "WornBannerCarpet",
                        carpetX,
                        .009f,
                        carpetZ,
                        assets.pbr("", carpetTint, .97f, 0)),
                "kit_carpet");
        // The carpet footprint follows the room, so the module is authored as a one metre tile and
        // stretched in X and Z only; its modelled thickness stays untouched.
        if (carpet.module()) carpet.spatial().setLocalScale(carpetX * 2, 1, carpetZ * 2);
        decor.attachChild(carpet.spatial());
        for (int sign : new int[] {-1, 1})
          decor.attachChild(
              slot(
                      NO_TINT,
                      x + sign * dx,
                      4.4f,
                      z,
                      1.3f,
                      () ->
                          assets.box(
                              "HangingBanner", .62f, 1.3f, .03f, assets.pbr("", 0x392a32, .9f, 0)),
                      "kit_banner")
                  .spatial());
      } else
        for (int i = 0; i < 6; i++) {
          float rx = x + (random.nextFloat() - .5f) * dx * 1.6f,
              rz = z + (random.nextFloat() - .5f) * dz * 1.6f;
          // Deep teal rather than the saturated cyan Kevin asked to have darkened: the veins the
          // player has to find (CRYSTAL objects) stay brighter than the decoration.
          crystal(decor, rx, .6f, rz, .4f + random.nextFloat() * .55f, 0x1f5d5c);
        }
    }
  }

  private void pillar(float x, float z, Material mat) {
    Node n = new Node("Pillar");
    n.setLocalTranslation(x, 0, z);
    decor.attachChild(n);
    Spatial module = module(layout.region.stone, "kit_pillar");
    if (module != null) n.attachChild(module);
    else {
      Geometry base = assets.box("Plinth", .66f, .18f, .66f, mat);
      base.setLocalTranslation(0, .18f, 0);
      n.attachChild(base);
      Geometry column = assets.box("Shaft", .4f, 2.8f, .4f, mat);
      column.setLocalTranslation(0, 3, 0);
      n.attachChild(column);
      Geometry cap = assets.box("Capital", .68f, .24f, .68f, mat);
      cap.setLocalTranslation(0, 5.9f, 0);
      n.attachChild(cap);
    }
    physics.box(x, 3, z, .48f, 3, .48f);
    // Also include pillars in camera/projectile ray tests.
    n.removeFromParent();
    occluders.attachChild(n);
  }

  private void arch(float x, float z, Material stone) {
    Node arch = new Node("VaultedArch");
    arch.setLocalTranslation(x, 0, z);
    decor.attachChild(arch);
    float radius = 3.55f;
    Spatial module = module(layout.region.stone, "kit_arch");
    if (module != null) arch.attachChild(module);
    else
      for (int i = 0; i <= 12; i++) {
        float angle = i * FastMath.PI / 12;
        Geometry voussoir = assets.box("ArchStone", .48f, .22f, .36f, stone);
        voussoir.setLocalTranslation(
            FastMath.cos(angle) * radius, 3.3f + FastMath.sin(angle) * radius, 0);
        voussoir.setLocalRotation(
            new Quaternion().fromAngleAxis(angle + FastMath.HALF_PI, Vector3f.UNIT_Z));
        arch.attachChild(voussoir);
      }
    for (int sign : new int[] {-1, 1}) {
      if (module == null) {
        Geometry support = assets.box("ArchSupport", .24f, 1.65f, .35f, stone);
        support.setLocalTranslation(sign * radius, 1.65f, 0);
        arch.attachChild(support);
      }
      physics.box(x + sign * radius, 1.65f, z, .24f, 1.65f, .35f);
    }
    arch.removeFromParent();
    occluders.attachChild(arch);
  }

  private void rock(float x, float z, float size) {
    Spatial boulder = module(NO_TINT, "kit_rock");
    if (boulder == null) {
      Sphere near = new Sphere(12, 16, 1), far = new Sphere(5, 7, 1);
      com.jme3.util.mikktspace.MikktspaceTangentGenerator.generate(near);
      com.jme3.util.mikktspace.MikktspaceTangentGenerator.generate(far);
      Geometry rock = new Geometry("CaveRock", near);
      rock.setMaterial(assets.pbr("stone", 0x63736b, .96f, 0));
      // The control swaps the mesh of a Geometry, so it cannot carry an imported module; a module
      // has to hold its own triangle count down instead.
      rock.addControl(new DistanceLodControl(near, far));
      boulder = rock;
    }
    boulder.setLocalScale(size, size * 1.6f, size);
    boulder.setLocalTranslation(x, size * .6f, z);
    occluders.attachChild(boulder);
    physics.box(x, size * .6f, z, size * .7f, size, size * .7f);
  }

  private void torch(float x, float z, int index) {
    Node n = new Node("Torch");
    n.setLocalTranslation(x, 0, z);
    decor.attachChild(n);
    Spatial module = module(NO_TINT, "kit_brazier");
    if (module != null) n.attachChild(module);
    else {
      Geometry stand = assets.box("BrazierStand", .09f, 1.3f, .09f, iron);
      stand.setLocalTranslation(0, 1.3f, 0);
      n.attachChild(stand);
      Geometry bowl = assets.box("Brazier", .27f, .1f, .27f, gold);
      bowl.setLocalTranslation(0, 2.6f, 0);
      n.attachChild(bowl);
    }
    // The flame stays procedural in either case: its glow material feeds the bloom pass and the
    // light below is what update() animates.
    Geometry flame = assets.sphere("Flame", .16f, assets.glow(0xffbc69, 2));
    flame.setLocalScale(1, 2.2f, 1);
    flame.setLocalTranslation(0, 2.86f, 0);
    flame.setShadowMode(ShadowMode.Off);
    n.attachChild(flame);
    PointLight light = new PointLight();
    Vector3f pos = new Vector3f(x, 3.05f, z);
    light.setPosition(pos);
    light.setRadius(SceneLighting.TORCH_RADIUS);
    root.addLight(light);
    torches.add(new Torch(light, pos, index * 1.71f));
  }

  private void buildObject(DungeonLayout.ObjectSpec spec, GameSession session) {
    Node n = new Node(spec.id());
    n.setLocalTranslation(spec.x(), 0, spec.z());
    objects.put(spec.id(), n);
    interactives.attachChild(n);
    Material stone = assets.pbr("stone", 0x94918b, .85f, 0);
    switch (spec.kind()) {
      case SHRINE -> {
        Geometry plinth = assets.box("ShrineBase", .7f, .18f, .7f, stone);
        plinth.setLocalTranslation(0, .18f, 0);
        n.attachChild(plinth);
        // A fire, not a crystal: the shrines are "Feuer" by name and the cyan shard burnt out to
        // white on every screenshot. The bowl of props/shrine.glb ends at 0.62 m.
        flame(n, 0, .6f, 0, 1f);
      }
      case PORTAL -> {
        for (int sign : new int[] {-1, 1}) {
          Geometry side = assets.box("GatePillar", .35f, 2.6f, .4f, stone);
          side.setLocalTranslation(sign * 1.5f, 2.6f, 0);
          n.attachChild(side);
        }
        Geometry top = assets.box("GateLintel", 1.85f, .35f, .4f, stone);
        top.setLocalTranslation(0, 4.9f, 0);
        n.attachChild(top);
        Geometry plane = assets.box("PortalVeil", 1.2f, 2.05f, .025f, assets.flat(0x58bfca, .17f));
        plane.setLocalTranslation(0, 2.2f, 0);
        plane.setQueueBucket(Bucket.Transparent);
        plane.setShadowMode(ShadowMode.Off);
        n.attachChild(plane);
      }
      case CHEST -> {
        Geometry box = assets.box("Chest", .65f, .42f, .45f, assets.pbr("", 0x614b36, .75f, .1f));
        box.setLocalTranslation(0, .42f, 0);
        n.attachChild(box);
        Geometry band = assets.box("ChestBand", .08f, .43f, .46f, gold);
        band.setLocalTranslation(0, .43f, 0);
        n.attachChild(band);
      }
      case NPC, PRISONER -> {
        var rig =
            new CharacterFactory(assets)
                .create(
                    spec.value(),
                    spec.kind() == DungeonLayout.Kind.NPC ? 0x50747b : 0x806e56,
                    false);
        n.attachChild(rig.root());
      }
      case RUNE, SEAL, LORE -> {
        Geometry base = assets.box("Altar", .65f, .6f, .45f, stone);
        base.setLocalTranslation(0, .6f, 0);
        n.attachChild(base);
        crystal(n, 0, 1.5f, 0, .23f, spec.kind() == DungeonLayout.Kind.LORE ? 0xd7b675 : 0x85ccdc);
      }
      case CRYSTAL -> crystal(n, 0, 1.2f, 0, .8f, 0x3d9e98);
      case THRONE -> {
        Geometry seat = assets.box("Throne", 1.2f, .55f, 1, iron);
        seat.setLocalTranslation(0, .55f, 0);
        n.attachChild(seat);
        Geometry back = assets.box("ThroneBack", 1.2f, 2.4f, .3f, iron);
        back.setLocalTranslation(0, 2.7f, -.9f);
        n.attachChild(back);
        crystal(n, 0, 2.5f, 0, .45f, 0xe28b51);
      }
      case TRAP -> trap(n, spec);
      default -> {}
    }
    if (spec.kind() != DungeonLayout.Kind.NPC
        && spec.kind() != DungeonLayout.Kind.PRISONER
        && spec.kind() != DungeonLayout.Kind.TRAP) {
      Spatial imported =
          assets.model("props/" + spec.kind().name().toLowerCase(Locale.ROOT), () -> null);
      if (imported != null) {
        // A module replaces the procedural stonework, but not the light-emitting parts: their
        // glow material feeds the bloom pass and no glTF material can produce it. Dropping them
        // would take the visual cue off every interactable the player has to find. A CRYSTAL is
        // the exception - there the glow sphere is the whole object, so its module replaces it.
        List<Spatial> emissive = new ArrayList<>();
        if (spec.kind() != DungeonLayout.Kind.CRYSTAL)
          for (Spatial child : new ArrayList<>(n.getChildren()))
            if (child.getName().equals("Crystal")
                || child.getName().equals("Flame")
                || child.getName().equals("PortalVeil")) emissive.add(child);
        n.detachAllChildren();
        n.attachChild(imported);
        for (Spatial part : emissive) n.attachChild(part);
      }
    }
    refreshObject(spec, session);
  }

  /**
   * What a collected object looks like afterwards. Everything used to shrink to 0.6, which read as
   * "the chest got smaller", not "the crystal is gone". Now the crystal vein vanishes, the seal
   * altar loses the seal it held (its glowing shard) and keeps its stone, and the chest stays as it
   * is - a container is still there after it has been emptied.
   */
  public void refreshObject(DungeonLayout.ObjectSpec spec, GameSession session) {
    Node n = objects.get(spec.id());
    if (n == null || !session.opened.contains(spec.id())) return;
    switch (spec.kind()) {
      case CRYSTAL -> n.setCullHint(Spatial.CullHint.Always);
      case SEAL -> {
        for (Spatial child : new ArrayList<>(n.getChildren()))
          if (child.getName().equals("Crystal")) child.removeFromParent();
      }
      default -> {}
    }
  }

  /**
   * A corridor trap, built along local +Z and turned for an x corridor. There is no plate: the
   * first cut had a slab in a darker floor tone across the corridor, and on the cave floor it read
   * as a paved strip you could not miss. The floor tile stays what it is; the only tell is two rows
   * of dark slits the width of the opening, and under every slit a spike, named "Spike", that
   * CampaignState raises through the slit when the trap fires. No glow: this one is meant to be
   * found by looking, or the hard way.
   */
  private void trap(Node n, DungeonLayout.ObjectSpec spec) {
    float width = DungeonLayout.trapWidth(spec) - .3f;
    if (DungeonLayout.trapAxisX(spec))
      n.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
    Material slot = assets.pbr("metal", 0x141517, .8f, .2f),
        blade = assets.pbr("metal", 0x7d8187, .58f, .4f);
    int columns = Math.max(3, Math.round(width / .55f));
    for (int i = 0; i < columns; i++) {
      float x = -width / 2 + (i + .5f) * width / columns;
      for (float z : new float[] {-.45f, .45f}) {
        Geometry slit = assets.box("TrapSlit", .028f, .012f, .11f, slot);
        slit.setLocalTranslation(x, .006f, z);
        n.attachChild(slit);
        Geometry spike = assets.box("Spike", .035f, .62f, .035f, blade);
        spike.setLocalTranslation(x, -.7f, z);
        n.attachChild(spike);
      }
    }
  }

  /** Raises the spikes of a trap: 0 below the floor, 1 fully up (tips at 1.25 m). */
  public void raiseSpikes(String trapId, float rise) {
    Node n = objects.get(trapId);
    if (n == null) return;
    float y = -.7f + 1.33f * rise;
    for (Spatial child : n.getChildren())
      if (child.getName().equals("Spike"))
        child.setLocalTranslation(child.getLocalTranslation().x, y, child.getLocalTranslation().z);
  }

  /** An item laid on the floor: a small bundle with a faint shard so it can be found again. */
  public void addDrop(GameSession.Drop drop) {
    Node n = new Node("Drop_" + drop.serial());
    n.setLocalTranslation(drop.x(), 0, drop.z());
    Geometry bundle = assets.box("Bundle", .17f, .11f, .14f, assets.pbr("", 0x5a4634, .85f, 0));
    bundle.setLocalTranslation(0, .11f, 0);
    bundle.setLocalRotation(new Quaternion().fromAngleAxis(drop.serial() * .7f, Vector3f.UNIT_Y));
    n.attachChild(bundle);
    crystal(n, 0, .34f, 0, .11f, 0xd7b675);
    interactives.attachChild(n);
    drops.put(drop.serial(), n);
  }

  public void removeDrop(int serial) {
    Node n = drops.remove(serial);
    if (n != null) n.removeFromParent();
  }

  /**
   * The name "Crystal" is load-bearing: buildObject keeps children of that name when a module
   * replaces the procedural stonework. The shard cluster is authored in the old sphere's frame, so
   * every caller's position and scale still hold; the seed varies the cluster per position.
   */
  private void crystal(Node parent, float x, float y, float z, float scale, int color) {
    Geometry g = new Geometry("Crystal", CrystalShapes.cluster(Float.floatToIntBits(x * 7 + z)));
    g.setMaterial(assets.crystal(color));
    g.setShadowMode(ShadowMode.Off);
    g.setLocalScale(scale * .6f, scale * 1.5f, scale * .6f);
    g.setLocalTranslation(x, y, z);
    parent.attachChild(g);
  }

  /** Scales every torch: the epilogue lets the fires die down or flare. 1 is the game's light. */
  public void torchScale(float scale) {
    torchScale = scale;
  }

  /**
   * A shrine fire: an ember bed and three cones of additive glow - a bright core, a wider, thinner
   * outer tongue and a small side lick - over a warm point light. update() flickers the tongues and
   * the light. The node is named "Flame" and, like "Crystal", survives a prop module.
   */
  private void flame(Node parent, float x, float y, float z, float scale) {
    Node fire = new Node("Flame");
    fire.setLocalTranslation(x, y, z);
    parent.attachChild(fire);
    Geometry bed =
        new Geometry("Embers", new Cylinder(2, 14, .26f * scale, .26f * scale, .05f, true, false));
    bed.setMaterial(assets.flame(0x8f3414, 1.2f, .9f));
    bed.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
    bed.setLocalTranslation(0, .03f, 0);
    bed.setQueueBucket(Bucket.Transparent);
    bed.setShadowMode(ShadowMode.Off);
    fire.attachChild(bed);
    List<Spatial> tongues = new ArrayList<>();
    List<Vector3f> scales = new ArrayList<>();
    float[][] layers = {
      // belly radius, height, colour, glow power, alpha, lean
      {.13f, .95f, 0xffe2a0, 2.6f, .9f, 0},
      {.21f, .72f, 0xff9a3c, 1.8f, .5f, 0},
      {.30f, .5f, 0xe0501c, 1.3f, .35f, 0},
      {.09f, .55f, 0xffb45a, 2f, .7f, .5f},
    };
    Mesh tongue = FlameShapes.tongue(9, 10);
    for (float[] c : layers) {
      Geometry g = new Geometry("Tongue", tongue);
      g.setMaterial(assets.flame((int) c[2], c[3], c[4]));
      g.setQueueBucket(Bucket.Transparent);
      g.setShadowMode(ShadowMode.Off);
      Vector3f size = new Vector3f(c[0] * scale, c[1] * scale, c[0] * scale);
      g.setLocalScale(size);
      g.setLocalRotation(new Quaternion().fromAngleAxis(c[5], Vector3f.UNIT_Z));
      fire.attachChild(g);
      tongues.add(g);
      scales.add(size);
    }
    Vector3f at = parent.getLocalTranslation().add(x, y + 1.3f, z);
    PointLight light = new PointLight(at, AssetPipeline.color(0xff9a4a).mult(3.2f), 9.5f);
    root.addLight(light);
    flames.add(new Flame(tongues, scales, light, flames.size() * 2.3f));
  }

  public void update(float time, Vector3f player) {
    for (Flame flame : flames) {
      for (int i = 0; i < flame.tongues.size(); i++) {
        float f = time * (7 + i * 2.6f) + flame.phase + i;
        float stretch = 1 + FastMath.sin(f) * .14f + FastMath.sin(f * 2.7f) * .07f;
        float sway = FastMath.sin(f * .8f) * .1f;
        Vector3f base = flame.scales.get(i);
        flame.tongues.get(i).setLocalScale(base.x * (1 + sway * .5f), base.y * stretch, base.z);
        flame.tongues.get(i).setLocalTranslation(sway * .06f, 0, FastMath.cos(f * .6f) * .04f);
      }
      float power =
          3.2f
              + FastMath.sin(time * 5.5f + flame.phase) * .35f
              + FastMath.sin(time * 12 + flame.phase) * .2f;
      flame.light.setColor(AssetPipeline.color(0xff9a4a).mult(power * torchScale));
    }
    for (Torch torch : torches) {
      float fade = torch.position.distanceSquared(player) > 32 * 32 ? 0 : torchScale;
      float power =
          SceneLighting.TORCH_POWER
              + FastMath.sin(time * 6 + torch.phase) * .3f
              + FastMath.sin(time * 13 + torch.phase) * .18f;
      torch.light.setColor(AssetPipeline.color(0xffad5b).mult(power * fade));
    }
  }

  public void cleanup() {
    root.removeFromParent();
    physics.clear();
  }
}
