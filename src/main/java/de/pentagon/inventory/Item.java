package de.pentagon.inventory;

/** Stable IDs, not translated names, are stored in save files. */
public record Item(
    String id, String name, Kind kind, Rarity rarity, int power, int value, String description) {
  public enum Kind {
    WEAPON,
    ARMOR,
    POTION,
    TONIC,
    KEY,
    RELIC
  }

  public enum Rarity {
    COMMON,
    RARE,
    EPIC
  }
}
