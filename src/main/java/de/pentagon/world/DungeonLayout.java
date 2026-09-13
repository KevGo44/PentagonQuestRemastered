package de.pentagon.world;

import de.pentagon.ai.EnemyType;
import java.util.*;

/** Authored room graph on a navigation grid; deterministic across saves and builds. */
public final class DungeonLayout {
  public static final int SIZE = 31;
  public static final float CELL = 2.8f;

  public record Room(String name, int x, int z, int rx, int rz) {}

  public record Spawn(String id, EnemyType type, float x, float z) {}

  public enum Kind {
    SHRINE,
    PORTAL,
    NPC,
    CHEST,
    LORE,
    RUNE,
    SEAL,
    CRYSTAL,
    PRISONER,
    THRONE,
    TRAP,
    /** Not authored: an item the player laid down, see GameSession.Drop. */
    DROP
  }

  public record ObjectSpec(String id, Kind kind, String label, float x, float z, String value) {}

  public final Region region;
  public final List<Room> rooms = new ArrayList<>();
  public final List<Spawn> enemies = new ArrayList<>();
  public final List<ObjectSpec> objects = new ArrayList<>();
  private final boolean[][] floor = new boolean[SIZE][SIZE];

  public DungeonLayout(Region region) {
    this.region = region;
    switch (region) {
      case REFUGE -> refuge();
      case CRYPT -> crypt();
      case CAVERNS -> caverns();
      case PRISON -> prison();
      case THRONE -> throne();
    }
  }

  private void room(String name, int x, int z, int rx, int rz) {
    rooms.add(new Room(name, x, z, rx, rz));
    for (int a = x - rx; a <= x + rx; a++) for (int b = z - rz; b <= z + rz; b++) carve(a, b);
  }

  private void carve(int x, int z) {
    if (x > 0 && z > 0 && x < SIZE - 1 && z < SIZE - 1) floor[x][z] = true;
  }

  private void connect(int first, int second) {
    Room a = rooms.get(first), b = rooms.get(second);
    int x = a.x, z = a.z;
    while (x != b.x) {
      for (int d = -1; d <= 1; d++) carve(x, z + d);
      x += Integer.signum(b.x - x);
    }
    while (z != b.z) {
      for (int d = -1; d <= 1; d++) carve(x + d, z);
      z += Integer.signum(b.z - z);
    }
    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) carve(x + dx, z + dz);
  }

  private void object(String id, Kind kind, String label, float x, float z, String value) {
    objects.add(new ObjectSpec(region.name() + "_" + id, kind, label, x * CELL, z * CELL, value));
  }

  private void enemy(String id, EnemyType type, float x, float z) {
    enemies.add(new Spawn(region.name() + "_" + id, type, x * CELL, z * CELL));
  }

  /** Cells a corridor is wide; connect() carves three. */
  public static final int CORRIDOR_CELLS = 3;

  /**
   * A trap fills a corridor from wall to wall. {@code axis} is the direction of travel through
   * it ("x" or "z"); {@code cells} how many cells wide the opening is at that point - three for a
   * plain corridor, five where two corridors meet. Stored in the object's value as e.g. "z3" so
   * WorldView and CampaignState read the same geometry; see {@link #trapAxisX} and {@link
   * #trapCells}.
   */
  private void trap(String id, String label, float x, float z, char axis, int cells) {
    object(id, Kind.TRAP, label, x, z, axis + Integer.toString(cells));
  }

  public static boolean trapAxisX(ObjectSpec trap) {
    return trap.value().charAt(0) == 'x';
  }

  public static int trapCells(ObjectSpec trap) {
    return trap.value().length() > 1 ? Integer.parseInt(trap.value().substring(1)) : CORRIDOR_CELLS;
  }

  /** Metres from wall to wall across a trap. */
  public static float trapWidth(ObjectSpec trap) {
    return trapCells(trap) * CELL;
  }

  private void refuge() {
    room("Halle der Reisenden", 15, 24, 4, 4);
    room("Westliche Wacht", 5, 24, 3, 3);
    room("Archiv der Fünf", 25, 24, 3, 3);
    room("Kreuzgang", 15, 14, 4, 3);
    room("Tor der Toten", 5, 7, 3, 4);
    room("Tor des Berges", 25, 7, 3, 4);
    room("Versiegelter Aufgang", 15, 4, 3, 2);
    connect(0, 1);
    connect(0, 2);
    connect(0, 3);
    connect(3, 4);
    connect(3, 5);
    connect(3, 6);
    object("shrine", Kind.SHRINE, "Feuer der Zuflucht", 15, 24, "");
    object("mira", Kind.NPC, "Mira, letzte Kartografin", 17, 22, "mira");
    object("supplies", Kind.CHEST, "Vorrat der Grenzlaeufer", 5, 24, "leather");
    object(
        "lore",
        Kind.LORE,
        "Chronik I - Die fünf Bastionen",
        25,
        24,
        "Einst hielt jede Bastion ein Siegel. Der König nahm vier mit Gewalt. Das fuenfte gaben"
            + " wir ihm aus Angst.");
    object("crypt", Kind.PORTAL, "Zur Krypta der Eide", 5, 5, "CRYPT");
    object("cave", Kind.PORTAL, "Zur gläsernen Tiefe", 25, 5, "CAVERNS");
    object("prison", Kind.PORTAL, "Zum Kettenverlies", 15, 3, "PRISON");
    trap("trap0", "Dornengang", 15, 9, 'z', 3);
    enemy("raider0", EnemyType.GOBLIN, 13, 13);
    enemy("raider1", EnemyType.GOBLIN, 17, 12);
  }

  private void crypt() {
    room("Die Schwelle", 15, 26, 3, 2);
    room("Saal der Namen", 15, 19, 4, 3);
    room("Sonnengruft", 5, 20, 3, 3);
    room("Mondgruft", 5, 8, 3, 3);
    room("Sternengruft", 25, 19, 3, 3);
    room("Die stumme Wacht", 25, 7, 3, 3);
    room("Altar des Eides", 15, 5, 4, 3);
    connect(0, 1);
    connect(1, 2);
    connect(2, 3);
    connect(1, 4);
    connect(4, 5);
    connect(3, 6);
    connect(5, 6);
    object("return", Kind.PORTAL, "Zur letzten Zuflucht", 15, 27, "REFUGE");
    object("shrine", Kind.SHRINE, "Feuer der Namen", 17, 25, "");
    object(
        "lore",
        Kind.LORE,
        "Chronik II - Der alte Eid",
        14,
        20,
        "Wenn die Sonne sinkt, wacht der Mond. Erst dann zeigt uns der Stern den Weg. Erneuere den"
            + " Eid in dieser Reihenfolge.");
    object("sun", Kind.RUNE, "Rune der Sonne", 5, 20, "0");
    object("moon", Kind.RUNE, "Rune des Mondes", 5, 8, "1");
    object("star", Kind.RUNE, "Rune des Sterns", 25, 19, "2");
    object("seal", Kind.SEAL, "Altar der Toten", 15, 4, "crypt_seal");
    object("chest", Kind.CHEST, "Sarkophag der Wacht", 25, 7, "chain");
    trap("trap0", "Dornengang", 9.5f, 19, 'x', 3);
    trap("trap1", "Dornengang", 5, 14, 'z', 3);
    trap("trap2", "Dornengang", 25, 13, 'z', 3);
    trap("trap3", "Dornengang", 9.5f, 8, 'x', 3);
    enemy("g0", EnemyType.GOBLIN, 14, 18);
    enemy("g1", EnemyType.GOBLIN, 16, 18);
    enemy("g2", EnemyType.GOBLIN, 5, 22);
    enemy("g3", EnemyType.GOBLIN, 4, 7);
    enemy("g4", EnemyType.GOBLIN, 26, 20);
    enemy("o0", EnemyType.ORC, 24, 7);
    enemy("s0", EnemyType.SHAMAN, 16, 6);
    enemy("w0", EnemyType.WARDEN, 14, 7);
  }

  private void caverns() {
    room("Wurzelbruch", 15, 26, 3, 2);
    room("Flüsterbecken", 15, 18, 4, 4);
    room("Westliche Ader", 5, 22, 3, 3);
    room("Tiefer Spalt", 5, 10, 3, 4);
    room("Östliche Ader", 25, 18, 3, 3);
    room("Kristallgarten", 25, 6, 3, 3);
    room("Herzkammer", 15, 5, 4, 3);
    connect(0, 1);
    connect(1, 2);
    connect(2, 3);
    connect(1, 4);
    connect(4, 5);
    connect(3, 6);
    connect(5, 6);
    object("return", Kind.PORTAL, "Zur letzten Zuflucht", 15, 27, "REFUGE");
    object("shrine", Kind.SHRINE, "Wurzelfeuer", 17, 25, "");
    object("crystal0", Kind.CRYSTAL, "Kristallader reinigen", 5, 22, "");
    object("crystal1", Kind.CRYSTAL, "Kristallader reinigen", 5, 9, "");
    object("crystal2", Kind.CRYSTAL, "Kristallader reinigen", 25, 6, "");
    object("seal", Kind.SEAL, "Kristallherz", 15, 4, "cave_seal");
    object("chest", Kind.CHEST, "Verlassene Ausruestung", 25, 18, "short_sword");
    object(
        "lore",
        Kind.LORE,
        "Chronik III - Der Gesang",
        16,
        18,
        "Der Berg war nie stumm. Drei Adern tragen seinen Gesang. Reinige sie, und das Herz wird"
            + " dir antworten.");
    trap("trap0", "Splittergang", 5, 15.5f, 'z', 3);
    trap("trap1", "Splittergang", 25, 12, 'z', 3);
    trap("trap2", "Splittergang", 9.5f, 10, 'x', 3);
    trap("trap3", "Splittergang", 20.5f, 18, 'x', 3);
    enemy("g0", EnemyType.GOBLIN, 14, 19);
    enemy("g1", EnemyType.GOBLIN, 16, 16);
    enemy("g2", EnemyType.GOBLIN, 5, 23);
    enemy("g3", EnemyType.GOBLIN, 4, 20);
    enemy("g4", EnemyType.GOBLIN, 26, 18);
    enemy("g5", EnemyType.GOBLIN, 24, 18);
    enemy("s0", EnemyType.SHAMAN, 5, 10);
    enemy("s1", EnemyType.SHAMAN, 25, 7);
    enemy("o0", EnemyType.ORC, 15, 6);
    enemy("o1", EnemyType.ORC, 17, 6);
  }

  private void prison() {
    room("Schleuse", 15, 26, 3, 2);
    room("Kettenhalle", 15, 18, 4, 3);
    room("Verlassene Waffenkammer", 5, 23, 3, 3);
    room("Die Namenlosen", 5, 10, 3, 4);
    room("Hof der Urteile", 25, 17, 3, 4);
    room("Kerkermeister", 25, 6, 3, 3);
    room("Aufstieg zur Krone", 15, 5, 3, 3);
    connect(0, 1);
    connect(1, 2);
    connect(2, 3);
    connect(1, 4);
    connect(4, 5);
    connect(3, 6);
    connect(5, 6);
    object("return", Kind.PORTAL, "Zur letzten Zuflucht", 15, 27, "REFUGE");
    object("shrine", Kind.SHRINE, "Feuer der Ketten", 17, 25, "");
    object("eren", Kind.PRISONER, "Eren, der Gefangene", 5, 9, "eren");
    object("chest", Kind.CHEST, "Arsenal der Wacht", 5, 23, "long_sword");
    object(
        "lore",
        Kind.LORE,
        "Chronik IV - Die Last der Krone",
        24,
        16,
        "Jeder Gefangene nährt das Siegel. Solange ihre Ketten halten, wird die Krone den König"
            + " stärken.");
    object("throne", Kind.PORTAL, "Zum Aschenthron", 15, 4, "THRONE");
    trap("trap0", "Klingengang", 9.5f, 18, 'x', 3);
    trap("trap1", "Klingengang", 5, 15.5f, 'z', 3);
    trap("trap2", "Klingengang", 25, 11, 'z', 3);
    trap("trap3", "Klingengang", 10, 10, 'x', 3);
    trap("trap4", "Klingengang", 20, 6, 'x', 3);
    enemy("g0", EnemyType.GOBLIN, 13, 19);
    enemy("o0", EnemyType.ORC, 17, 18);
    enemy("o1", EnemyType.ORC, 5, 21);
    enemy("o2", EnemyType.ORC, 4, 11);
    enemy("s0", EnemyType.SHAMAN, 25, 17);
    enemy("w0", EnemyType.WARDEN, 24, 19);
    enemy("w1", EnemyType.WARDEN, 16, 6);
    enemy("keeper", EnemyType.WARDEN, 25, 6);
  }

  private void throne() {
    room("Letztes Feuer", 15, 27, 3, 2);
    room("Halle der Banner", 15, 20, 4, 3);
    room("Westlicher Umgang", 5, 18, 3, 3);
    room("Oestlicher Umgang", 25, 18, 3, 3);
    room("Hof der Asche", 15, 8, 6, 6);
    connect(0, 1);
    connect(1, 2);
    connect(1, 3);
    connect(1, 4);
    connect(2, 4);
    connect(3, 4);
    object("return", Kind.PORTAL, "Zurück zum Verlies", 15, 28, "PRISON");
    object("shrine", Kind.SHRINE, "Das letzte Feuer", 17, 26, "");
    object(
        "lore",
        Kind.LORE,
        "Chronik V - Ein Urteil",
        5,
        18,
        "Zerbrich das Siegel, und niemand wird je wieder seine Kraft tragen. Bewahre es, und du"
            + " musst besser herrschen als er.");
    object("chest", Kind.CHEST, "Letzter Vorrat", 25, 18, "greater_potion");
    object("throne", Kind.THRONE, "Das Aschensiegel", 15, 3, "");
    trap("trap0", "Klingengang", 15, 15.5f, 'z', 3);
    trap("trap1", "Klingengang", 9.5f, 19, 'x', 5);
    trap("trap2", "Klingengang", 20.5f, 19, 'x', 5);
    enemy("o0", EnemyType.ORC, 14, 20);
    enemy("s0", EnemyType.SHAMAN, 17, 19);
    enemy("king", EnemyType.KING, 15, 8);
  }

  public float spawnX() {
    return rooms.get(0).x * CELL;
  }

  public float spawnZ() {
    return (rooms.get(0).z - 1) * CELL;
  }

  public boolean walkable(int x, int z) {
    return x >= 0 && z >= 0 && x < SIZE && z < SIZE && floor[x][z];
  }

  public boolean walkable(float x, float z) {
    return walkable(Math.round(x / CELL), Math.round(z / CELL));
  }

  public boolean wall(int x, int z) {
    return !walkable(x, z)
        && (walkable(x - 1, z) || walkable(x + 1, z) || walkable(x, z - 1) || walkable(x, z + 1));
  }

  public String roomAt(float x, float z) {
    for (Room r : rooms)
      if (Math.abs(x / CELL - r.x) <= r.rx + .5f && Math.abs(z / CELL - r.z) <= r.rz + .5f)
        return r.name;
    return "Verbindungsgang";
  }

  /**
   * Grid visibility for perception and attacks. Physical camera and projectile sweeps are separate.
   */
  public boolean clearLine(float ax, float az, float bx, float bz) {
    int n = Math.max(1, (int) Math.ceil(Math.hypot(bx - ax, bz - az) / .45));
    for (int i = 0; i <= n; i++)
      if (!walkable(ax + (bx - ax) * i / n, az + (bz - az) * i / n)) return false;
    return true;
  }

  public List<Integer> path(float ax, float az, float bx, float bz) {
    int start = Math.round(ax / CELL) + Math.round(az / CELL) * SIZE,
        end = Math.round(bx / CELL) + Math.round(bz / CELL) * SIZE;
    if (start < 0
        || end < 0
        || start >= SIZE * SIZE
        || end >= SIZE * SIZE
        || !walkable(end % SIZE, end / SIZE)) return List.of();
    int[] previous = new int[SIZE * SIZE];
    Arrays.fill(previous, -1);
    previous[start] = start;
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    queue.add(start);
    while (!queue.isEmpty() && previous[end] < 0) {
      int p = queue.remove();
      int px = p % SIZE, pz = p / SIZE;
      int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      for (int[] d : steps) {
        int nx = px + d[0], nz = pz + d[1], q = nx + nz * SIZE;
        if (walkable(nx, nz) && previous[q] < 0) {
          previous[q] = p;
          queue.add(q);
        }
      }
    }
    if (previous[end] < 0) return List.of();
    LinkedList<Integer> result = new LinkedList<>();
    for (int p = end; p != start; p = previous[p]) result.addFirst(p);
    return result;
  }
}
