package de.pentagon.inventory;

import java.util.*;

public final class Inventory {
  public static final int CAPACITY = 24;

  /** Potions a new hero sets out with; five made the early fights a matter of pressing R. */
  public static final int START_POTIONS = 3;

  private final Map<String, Integer> stacks = new LinkedHashMap<>();
  private String weapon = "rust_sword";
  private String armor = "cloth";

  public Inventory() {
    add(weapon, 1);
    add(armor, 1);
    add("potion", START_POTIONS);
    add("tonic", 2);
  }

  public boolean add(String id, int count) {
    Item item = ItemCatalog.get(id);
    if (count <= 0 || count > 999) return false;
    // Plot items must never be lost because the pack is full.
    if (!stacks.containsKey(id)
        && slots() >= CAPACITY
        && item.kind() != Item.Kind.KEY
        && item.kind() != Item.Kind.RELIC) return false;
    stacks.put(id, Math.min(999, count(id) + count));
    return true;
  }

  public boolean remove(String id, int count) {
    if (count <= 0 || count(id) < count) return false;
    int remaining = count(id) - count;
    if (remaining == 0 && (id.equals(weapon) || id.equals(armor))) return false;
    if (remaining == 0) stacks.remove(id);
    else stacks.put(id, remaining);
    return true;
  }

  public boolean equip(String id) {
    if (count(id) < 1) return false;
    Item item = ItemCatalog.get(id);
    if (item.kind() == Item.Kind.WEAPON) weapon = id;
    else if (item.kind() == Item.Kind.ARMOR) armor = id;
    else return false;
    return true;
  }

  public int count(String id) {
    return stacks.getOrDefault(id, 0);
  }

  public int slots() {
    return (int)
        stacks.keySet().stream()
            .filter(
                id ->
                    ItemCatalog.get(id).kind() != Item.Kind.KEY
                        && ItemCatalog.get(id).kind() != Item.Kind.RELIC)
            .count();
  }

  public Item weapon() {
    return ItemCatalog.get(weapon);
  }

  public Item armor() {
    return ItemCatalog.get(armor);
  }

  public boolean equipped(String id) {
    return id.equals(weapon) || id.equals(armor);
  }

  public Map<String, Integer> stacks() {
    return Collections.unmodifiableMap(stacks);
  }

  public void validate() {
    if (stacks.size() > 64 || !ItemCatalog.contains(weapon) || !ItemCatalog.contains(armor))
      throw new IllegalArgumentException("Ungueltiges Inventar");
    for (var e : stacks.entrySet())
      if (!ItemCatalog.contains(e.getKey())
          || e.getValue() == null
          || e.getValue() < 1
          || e.getValue() > 999) throw new IllegalArgumentException("Ungültiger Stapel");
    if (count(weapon) == 0
        || count(armor) == 0
        || weapon().kind() != Item.Kind.WEAPON
        || armor().kind() != Item.Kind.ARMOR)
      throw new IllegalArgumentException("Ungültige Ausruestung");
  }
}
