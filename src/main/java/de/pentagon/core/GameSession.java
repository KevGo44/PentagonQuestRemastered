package de.pentagon.core;

import de.pentagon.entities.PlayerStats;
import de.pentagon.inventory.Inventory;
import de.pentagon.world.Region;
import java.util.*;

/** Serializable campaign aggregate. Runtime objects are deliberately kept elsewhere. */
public final class GameSession {
  public static final int SAVE_VERSION = 1;
  public int version = SAVE_VERSION;
  public PlayerStats player = new PlayerStats();
  public Inventory inventory = new Inventory();
  public Region region = Region.REFUGE;
  public float x = 0, z = 0, yaw = 0;
  public String checkpoint = Region.REFUGE.name();
  public float checkpointX, checkpointZ;
  public double playSeconds;
  public final Set<String> flags = new LinkedHashSet<>();
  public final Set<String> defeated = new LinkedHashSet<>();
  public final Set<String> opened = new LinkedHashSet<>();
  public final Set<String> rewardedQuests = new LinkedHashSet<>();
  public final Map<String, Integer> counters = new LinkedHashMap<>();
  public final Map<String, EnemySave> enemies = new LinkedHashMap<>();
  public final Map<String, Set<Integer>> explored = new LinkedHashMap<>();

  public record EnemySave(float health, float x, float z) {}

  public boolean flag(String key) {
    return flags.contains(key);
  }

  public void event(String key) {
    counters.merge(key, 1, Integer::sum);
  }

  public int count(String key) {
    return counters.getOrDefault(key, 0);
  }

  public boolean sealsReady() {
    return inventory.count("crypt_seal") > 0 && inventory.count("cave_seal") > 0;
  }

  public void validate() {
    if (version != SAVE_VERSION)
      throw new IllegalArgumentException("Nicht unterstuetzte Spielstandversion: " + version);
    if (player == null || inventory == null || region == null || checkpoint == null)
      throw new IllegalArgumentException("Unvollstaendiger Spielstand");
    player.validate();
    inventory.validate();
    Region.valueOf(checkpoint);
    if (!Float.isFinite(x)
        || !Float.isFinite(z)
        || !Float.isFinite(yaw)
        || Math.abs(x) > 500
        || Math.abs(z) > 500
        || !Float.isFinite(checkpointX)
        || !Float.isFinite(checkpointZ)) throw new IllegalArgumentException("Ungültige Position");
    if (!Double.isFinite(playSeconds)
        || playSeconds < 0
        || flags == null
        || defeated == null
        || opened == null
        || counters == null
        || enemies == null
        || explored == null
        || rewardedQuests == null) throw new IllegalArgumentException("Ungültige Weltdaten");
    if (flags.size() + defeated.size() + opened.size() > 10000
        || enemies.size() > 1000
        || explored.size() > Region.values().length)
      throw new IllegalArgumentException("Spielstand zu groß");
    for (var e : enemies.values())
      if (e == null
          || !Float.isFinite(e.health)
          || e.health < 0
          || e.health > 10000
          || !Float.isFinite(e.x)
          || !Float.isFinite(e.z)
          || Math.abs(e.x) > 500
          || Math.abs(e.z) > 500) throw new IllegalArgumentException("Ungültiger Gegner");
    for (var v : counters.values())
      if (v == null || v < 0 || v > 100000)
        throw new IllegalArgumentException("Ungültiger Zaehler");
    for (var e : explored.entrySet()) {
      Region.valueOf(e.getKey());
      if (e.getValue() == null
          || e.getValue().stream().anyMatch(v -> v == null || v < 0 || v >= 31 * 31))
        throw new IllegalArgumentException("Ungültige Karte");
    }
  }
}
