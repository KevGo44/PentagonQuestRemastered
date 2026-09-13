package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import de.pentagon.entities.PlayerStats;
import de.pentagon.inventory.*;
import org.junit.jupiter.api.Test;

class ProgressionTest {
  @Test
  void multipleLevelsAwardExactlyOnePointEach() {
    PlayerStats s = new PlayerStats();
    s.health = 4;
    assertEquals(3, s.gainXp(610));
    assertEquals(4, s.level);
    assertEquals(10, s.xp);
    assertEquals(3, s.skillPoints);
    assertEquals(s.maxHealth(), s.health);
  }

  @Test
  void skillPurchaseChangesDerivedStatsAndCannotOverspend() {
    PlayerStats s = new PlayerStats();
    s.skillPoints = 1;
    assertTrue(s.unlock(PlayerStats.Skill.VITALITY));
    assertEquals(165, s.maxHealth());
    assertEquals(165, s.health);
    assertFalse(s.unlock(PlayerStats.Skill.VITALITY));
  }

  @Test
  void skillRanksHaveACap() {
    PlayerStats s = new PlayerStats();
    s.skillPoints = 8;
    for (int i = 0; i < 5; i++) assertTrue(s.unlock(PlayerStats.Skill.ARCANE));
    assertFalse(s.unlock(PlayerStats.Skill.ARCANE));
    assertEquals(3, s.skillPoints);
  }

  @Test
  void spendingCannotProduceNegativeStamina() {
    PlayerStats s = new PlayerStats();
    assertFalse(s.spend(111));
    assertEquals(110, s.stamina);
    assertTrue(s.spend(110));
    assertEquals(0, s.stamina);
    s.recover(1, true);
    assertEquals(25, s.stamina);
  }

  @Test
  void equipmentUsesSingleAuthoritativeSlot() {
    Inventory i = new Inventory();
    i.add("long_sword", 1);
    assertTrue(i.equip("long_sword"));
    assertFalse(i.equipped("rust_sword"));
    assertEquals(20, i.weapon().power());
    assertFalse(i.remove("long_sword", 1));
    assertFalse(i.equip("potion"));
  }

  @Test
  void consumingLastPotionRemovesStack() {
    Inventory i = new Inventory();
    assertTrue(i.remove("potion", Inventory.START_POTIONS));
    assertEquals(0, i.count("potion"));
    assertFalse(i.stacks().containsKey("potion"));
  }

  @Test
  void invalidInventoryMutationsAreRejected() {
    Inventory i = new Inventory();
    assertFalse(i.add("potion", -2));
    assertFalse(i.remove("potion", -1));
    assertThrows(IllegalArgumentException.class, () -> i.add("unknown", 1));
    assertEquals(Inventory.START_POTIONS, i.count("potion"));
  }

  @Test
  void negativeXpRejected() {
    assertThrows(IllegalArgumentException.class, () -> new PlayerStats().gainXp(-1));
  }
}
