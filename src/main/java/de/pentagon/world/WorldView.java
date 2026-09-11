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
import jme3tools.optimize.GeometryBatchFactory;

public final class WorldView {
  public final Node root = new Node("Dungeon"),
      occluders = new Node("CollisionGeometry"),
      decor = new Node("Decorations"),
      interactives = new Node("Interactives");
  public final Map<String, Node> objects = new LinkedHashMap<>();

  private record Torch(PointLight light, Vector3f position, float phase) {}

  private final List<Torch> torches = new ArrayList<>();
  private final AssetPipeline assets;
  private final PhysicsWorld physics;
  private final Random random = new Random(51);
  private final Material iron, gold;
  public final DungeonLayout layout;

  public WorldView(
      AssetPipeline assets, PhysicsWorld physics, DungeonLayout layout, GameSession session) {
    this.assets = assets;
    this.physics = physics;
    this.layout = layout;
    iron = assets.pbr("metal", 0x45494e, .5f, .8f);
    gold = assets.pbr("metal", 0x987246, .45f, .65f);
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
        floor = assets.pbr("stone", layout.region == Region.CAVERNS ? 0x687771 : 0x8c8b86, .86f, 0);
    float cell = DungeonLayout.CELL;
    Map<String, Node> chunks = new HashMap<>();
    physics.box(42, -.35f, 42, 47, .35f, 47);
    for (int z = 0; z < DungeonLayout.SIZE; z++)
      for (int x = 0; x < DungeonLayout.SIZE; x++) {
        if (!layout.walkable(x, z) && !layout.wall(x, z)) continue;
        String chunk = x / 6 + ":" + z / 6;
        Node n = chunks.computeIfAbsent(chunk, k -> new Node("Chunk_" + k));
        if (layout.walkable(x, z)) {
          Geometry g = assets.box("FloorTile", cell * .5f, .12f, cell * .5f, floor);
          g.setLocalTranslation(x * cell, -.13f, z * cell);
          n.attachChild(g);
          // A broken ceiling admits isolated light shafts; it never blocks navigation.
          if (!(x % 7 == 1 && z % 7 == 1)) {
            Geometry ceiling = assets.box("VaultCeiling", cell * .5f, .18f, cell * .5f, wall);
            ceiling.setLocalTranslation(x * cell, 7.1f, z * cell);
            ceiling.setShadowMode(ShadowMode.Receive);
            n.attachChild(ceiling);
          }
          if (x % 4 == 0) {
            Geometry beam = assets.box("CeilingRib", .16f, .16f, cell * .5f, wall);
            beam.setLocalTranslation(x * cell, 6.4f, z * cell);
            n.attachChild(beam);
          }
        } else {
          Geometry block = assets.box("Wall", cell * .5f, 3.4f, cell * .5f, wall);
          block.setLocalTranslation(x * cell, 3.3f, z * cell);
          block.getMesh().scaleTextureCoordinates(new Vector2f(1, 2));
          n.attachChild(block);
          Geometry crown = assets.box("Cornice", cell * .55f, .16f, cell * .55f, iron);
          crown.setLocalTranslation(x * cell, 4.8f, z * cell);
          n.attachChild(crown);
        }
      }
    // Merge adjacent wall colliders into runs; much cheaper than a body per brick.
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

  private void buildRooms() {
    Material stone = assets.pbr("stone", layout.region.stone, .9f, 0);
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
        Geometry carpet =
            assets.box(
                "WornBannerCarpet",
                Math.min(2.2f, dx * .4f),
                .009f,
                dz * .75f,
                assets.pbr("", layout.region == Region.REFUGE ? 0x243c48 : 0x492a2d, .97f, 0));
        carpet.setLocalTranslation(x, .009f, z);
        decor.attachChild(carpet);
        for (int sign : new int[] {-1, 1}) {
          Geometry banner =
              assets.box("HangingBanner", .62f, 1.3f, .03f, assets.pbr("", 0x392a32, .9f, 0));
          banner.setLocalTranslation(x + sign * dx, 4.4f, z);
          decor.attachChild(banner);
        }
      } else
        for (int i = 0; i < 6; i++) {
          float rx = x + (random.nextFloat() - .5f) * dx * 1.6f,
              rz = z + (random.nextFloat() - .5f) * dz * 1.6f;
          crystal(decor, rx, .6f, rz, .4f + random.nextFloat() * .55f, 0x439c9a);
        }
    }
  }

  private void pillar(float x, float z, Material mat) {
    Node n = new Node("Pillar");
    n.setLocalTranslation(x, 0, z);
    decor.attachChild(n);
    Geometry base = assets.box("Plinth", .66f, .18f, .66f, mat);
    base.setLocalTranslation(0, .18f, 0);
    n.attachChild(base);
    Geometry column = assets.box("Shaft", .4f, 2.8f, .4f, mat);
    column.setLocalTranslation(0, 3, 0);
    n.attachChild(column);
    Geometry cap = assets.box("Capital", .68f, .24f, .68f, mat);
    cap.setLocalTranslation(0, 5.9f, 0);
    n.attachChild(cap);
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
      Geometry support = assets.box("ArchSupport", .24f, 1.65f, .35f, stone);
      support.setLocalTranslation(sign * radius, 1.65f, 0);
      arch.attachChild(support);
      physics.box(x + sign * radius, 1.65f, z, .24f, 1.65f, .35f);
    }
    arch.removeFromParent();
    occluders.attachChild(arch);
  }

  private void rock(float x, float z, float size) {
    Sphere near = new Sphere(12, 16, 1), far = new Sphere(5, 7, 1);
    com.jme3.util.mikktspace.MikktspaceTangentGenerator.generate(near);
    com.jme3.util.mikktspace.MikktspaceTangentGenerator.generate(far);
    Geometry rock = new Geometry("CaveRock", near);
    rock.setMaterial(assets.pbr("stone", 0x63736b, .96f, 0));
    rock.setLocalScale(size, size * 1.6f, size);
    rock.setLocalTranslation(x, size * .6f, z);
    rock.addControl(new DistanceLodControl(near, far));
    occluders.attachChild(rock);
    physics.box(x, size * .6f, z, size * .7f, size, size * .7f);
  }

  private void torch(float x, float z, int index) {
    Node n = new Node("Torch");
    n.setLocalTranslation(x, 0, z);
    decor.attachChild(n);
    Geometry stand = assets.box("BrazierStand", .09f, 1.3f, .09f, iron);
    stand.setLocalTranslation(0, 1.3f, 0);
    n.attachChild(stand);
    Geometry bowl = assets.box("Brazier", .27f, .1f, .27f, gold);
    bowl.setLocalTranslation(0, 2.6f, 0);
    n.attachChild(bowl);
    Geometry flame = assets.sphere("Flame", .16f, assets.glow(0xffbc69, 2));
    flame.setLocalScale(1, 2.2f, 1);
    flame.setLocalTranslation(0, 2.86f, 0);
    flame.setShadowMode(ShadowMode.Off);
    n.attachChild(flame);
    PointLight light = new PointLight();
    Vector3f pos = new Vector3f(x, 3.05f, z);
    light.setPosition(pos);
    light.setRadius(14);
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
        crystal(n, 0, 1.05f, 0, .5f, 0x70d7d6);
        PointLight l =
            new PointLight(
                new Vector3f(spec.x(), 2, spec.z()), AssetPipeline.color(0x53c5cd).mult(3), 8);
        root.addLight(l);
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
      case CRYSTAL -> crystal(n, 0, 1.2f, 0, .8f, 0x55cfc5);
      case THRONE -> {
        Geometry seat = assets.box("Throne", 1.2f, .55f, 1, iron);
        seat.setLocalTranslation(0, .55f, 0);
        n.attachChild(seat);
        Geometry back = assets.box("ThroneBack", 1.2f, 2.4f, .3f, iron);
        back.setLocalTranslation(0, 2.7f, -.9f);
        n.attachChild(back);
        crystal(n, 0, 2.5f, 0, .45f, 0xe28b51);
      }
      case TRAP -> {
        Geometry plate = assets.box("PressurePlate", 1.25f, .04f, 1.25f, iron);
        plate.setLocalTranslation(0, .02f, 0);
        n.attachChild(plate);
        Geometry warning =
            assets.box("TrapWarning", 1.15f, .01f, 1.15f, assets.glow(0xce5d40, .25f));
        warning.setLocalTranslation(0, .07f, 0);
        n.attachChild(warning);
        for (int i = -1; i <= 1; i++)
          for (int j = -1; j <= 1; j++) {
            Geometry spike = assets.box("Spike", .045f, .5f, .045f, iron);
            spike.setLocalTranslation(i * .7f, -.5f, j * .7f);
            n.attachChild(spike);
          }
      }
    }
    if (spec.kind() != DungeonLayout.Kind.NPC
        && spec.kind() != DungeonLayout.Kind.PRISONER
        && spec.kind() != DungeonLayout.Kind.TRAP) {
      Spatial imported =
          assets.model("props/" + spec.kind().name().toLowerCase(Locale.ROOT), () -> null);
      if (imported != null) {
        n.detachAllChildren();
        n.attachChild(imported);
      }
    }
    refreshObject(spec, session);
  }

  public void refreshObject(DungeonLayout.ObjectSpec spec, GameSession session) {
    Node n = objects.get(spec.id());
    if (n == null) return;
    if (session.opened.contains(spec.id())
        && (spec.kind() == DungeonLayout.Kind.CHEST
            || spec.kind() == DungeonLayout.Kind.SEAL
            || spec.kind() == DungeonLayout.Kind.CRYSTAL)) n.setLocalScale(.6f);
  }

  private void crystal(Node parent, float x, float y, float z, float scale, int color) {
    Geometry g = new Geometry("Crystal", new Sphere(4, 5, 1));
    g.setMaterial(assets.glow(color, .9f));
    g.setLocalScale(scale * .6f, scale * 1.5f, scale * .6f);
    g.setLocalTranslation(x, y, z);
    parent.attachChild(g);
  }

  public void update(float time, Vector3f player) {
    for (Torch torch : torches) {
      float fade = torch.position.distanceSquared(player) > 32 * 32 ? 0 : 1;
      float power =
          2.3f
              + FastMath.sin(time * 6 + torch.phase) * .2f
              + FastMath.sin(time * 13 + torch.phase) * .12f;
      torch.light.setColor(AssetPipeline.color(0xffad5b).mult(power * fade));
    }
  }

  public void cleanup() {
    root.removeFromParent();
    physics.clear();
  }
}
