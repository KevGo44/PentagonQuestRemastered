package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import de.pentagon.ai.EnemyType;
import de.pentagon.combat.*;
import de.pentagon.world.TrapMechanism;
import org.junit.jupiter.api.Test;

class DifficultyTest {
  private static final int HERO = 140, CLOTH = 2;

  /** Blows of {@code type} that empty a full, unarmoured-but-for-cloth hero on {@code level}. */
  private static int blowsToDie(Difficulty level, EnemyType type) {
    float health = HERO;
    int blows = 0;
    while (health > 0 && blows < 99) {
      float raw = level.enemyHit(type.damage, false, HERO);
      health -= CombatRules.defend(raw, CLOTH, false, false, 0, 100, false).healthDamage();
      blows++;
    }
    return blows;
  }

  @Test
  void easyIsTheGameAsTuned() {
    assertEquals(1f, Difficulty.EASY.enemyDamage);
    assertEquals(1f, Difficulty.EASY.enemyHealth);
    assertEquals(1f, Difficulty.EASY.pace);
    assertEquals(46f, Difficulty.EASY.enemyHit(46, false, HERO));
    assertEquals(60f, Difficulty.EASY.enemyHit(60, true, HERO));
    assertEquals(CombatSystem.POTION_DROP_EVERY, Difficulty.EASY.potionEvery);
    assertEquals(Difficulty.EASY, Difficulty.of(0));
    assertEquals(Difficulty.EASY, Difficulty.of(7), "a stored value out of range reads as easy");
  }

  @Test
  void veryHardLeavesRoomForOneOrTwoMistakesAndNoMore() {
    for (EnemyType type : EnemyType.values()) {
      int blows = blowsToDie(Difficulty.VERY_HARD, type);
      assertTrue(blows >= 2 && blows <= 3, type + " kills a full hero in " + blows + " blows");
    }
    // No single hit - not the king's, not a trap's - takes the whole bar.
    assertTrue(Difficulty.VERY_HARD.enemyHit(EnemyType.KING.damage, false, HERO) <= HERO * .7f);
    assertTrue(Difficulty.VERY_HARD.enemyHit(TrapMechanism.DAMAGE, true, HERO) <= HERO * .7f);
    assertTrue(Difficulty.VERY_HARD.enemyHit(TrapMechanism.DAMAGE, true, HERO) > HERO * .6f);
    assertFalse(Difficulty.VERY_HARD.dropsPotion(0), "no potion drops at all");
    assertEquals(1, Difficulty.VERY_HARD.potions);
    assertTrue(Difficulty.VERY_HARD.pace < Difficulty.MEDIUM.pace);
  }

  @Test
  void mediumSitsBetween() {
    for (EnemyType type : EnemyType.values()) {
      int easy = blowsToDie(Difficulty.EASY, type),
          medium = blowsToDie(Difficulty.MEDIUM, type),
          hard = blowsToDie(Difficulty.VERY_HARD, type);
      assertTrue(
          hard <= medium && medium < easy, type + ": " + easy + " / " + medium + " / " + hard);
    }
    assertTrue(Difficulty.MEDIUM.dropsPotion(0) && !Difficulty.MEDIUM.dropsPotion(1));
    assertEquals(Difficulty.VERY_HARD, Difficulty.MEDIUM.next());
    assertEquals(Difficulty.EASY, Difficulty.VERY_HARD.next());
  }
}
