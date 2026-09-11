package de.pentagon.inventory;

import static de.pentagon.inventory.Item.Kind.*;
import static de.pentagon.inventory.Item.Rarity.*;

import java.util.*;

public final class ItemCatalog {
  private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

  static {
    add(
        "rust_sword",
        "Rostiges Schwert",
        WEAPON,
        COMMON,
        4,
        10,
        "Eine alte Klinge. Drei leichte Hiebe bilden eine Kombo.");
    add(
        "short_sword",
        "Grenzlaeufer-Klinge",
        WEAPON,
        RARE,
        12,
        65,
        "Aus Miras verbliebenen Bestaenden. Zuverlaessig und scharf.");
    add(
        "long_sword",
        "Langschwert der Wacht",
        WEAPON,
        RARE,
        20,
        100,
        "Geborgen aus dem Kettenverlies. Hoher physischer Schaden.");
    add(
        "ember_blade",
        "Aschenklinge",
        WEAPON,
        EPIC,
        30,
        190,
        "Eine verbotene Klinge. Der Preis ist die Freiheit anderer.");
    add(
        "oath_blade",
        "Eidbrecher",
        WEAPON,
        EPIC,
        26,
        170,
        "Belohnung für einen gehaltenen Eid an die Vergessenen.");
    add(
        "cloth",
        "Reisemantel",
        ARMOR,
        COMMON,
        2,
        5,
        "Staub, Flicken und Erinnerungen an das Ödland.");
    add(
        "leather",
        "Verstärktes Leder",
        ARMOR,
        RARE,
        6,
        55,
        "Leichtes Leder der alten Kundschafter.");
    add(
        "chain",
        "Kettenhemd",
        ARMOR,
        RARE,
        11,
        100,
        "Schützt gegen Hiebe, ohne die Ausweichrolle zu verhindern.");
    add(
        "warden_armor",
        "Rüstung der Fünf",
        ARMOR,
        EPIC,
        16,
        160,
        "Die letzte Wacht trägt das Zeichen der fünf Bastionen.");
    add(
        "potion",
        "Heiltrank",
        POTION,
        COMMON,
        65,
        20,
        "Stellt bis zu 65 Lebenspunkte wieder her. Taste R.");
    add(
        "greater_potion",
        "Großer Heiltrank",
        POTION,
        RARE,
        130,
        45,
        "Stellt bis zu 130 Lebenspunkte wieder her.");
    add(
        "tonic",
        "Äthertrank",
        TONIC,
        COMMON,
        75,
        18,
        "Stellt Ausdauer wieder her und setzt den Zauber-Cooldown zurück.");
    add(
        "crypt_seal",
        "Siegel der Toten",
        KEY,
        EPIC,
        0,
        0,
        "Der erste Schlüssel zum Kettenverlies.");
    add("cave_seal", "Kristallherz", KEY, EPIC, 0, 0, "Der zweite Schlüssel zum Kettenverlies.");
    add(
        "prison_key",
        "Schlüssel des Kerkermeisters",
        KEY,
        RARE,
        0,
        0,
        "Öffnet den Weg zum Aschenthron.");
    add(
        "crown",
        "Gebrochene Krone",
        RELIC,
        EPIC,
        0,
        0,
        "Am Thron kann ihr Schicksal entschieden werden.");
  }

  private ItemCatalog() {}

  private static void add(
      String id,
      String name,
      Item.Kind kind,
      Item.Rarity rarity,
      int power,
      int value,
      String text) {
    ITEMS.put(id, new Item(id, name, kind, rarity, power, value, text));
  }

  public static Item get(String id) {
    Item item = ITEMS.get(id);
    if (item == null) throw new IllegalArgumentException("Unbekanntes Item: " + id);
    return item;
  }

  public static boolean contains(String id) {
    return ITEMS.containsKey(id);
  }

  public static Collection<Item> all() {
    return Collections.unmodifiableCollection(ITEMS.values());
  }
}
