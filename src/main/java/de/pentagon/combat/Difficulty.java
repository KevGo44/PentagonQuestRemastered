package de.pentagon.combat;

/**
 * The three difficulty settings. EASY is the game as tuned on 13 September; the others scale it
 * without touching the authored values, so the smoke run and the balance tests keep their meaning.
 *
 * <p>VERY_HARD is defined by Kevin's sentence: "room for one or two mistakes, then it is over".
 * Every enemy needs two or three blows to empty a full health bar (a goblin bite lands 49, an orc's
 * swing 94), traps take 120, and there are no potion drops. {@link #maxHitFraction} is the other
 * half of that promise: no single blow takes more than 70 % of the bar, so a full hero always
 * survives the first mistake - the king's 46 × 3.2 would otherwise be a one-hit kill.
 */
public enum Difficulty {
  EASY("Einfach", 1f, 1f, 1f, 1f, 1f, CombatSystem.POTION_DROP_EVERY, 3),
  MEDIUM("Mittel", 1.7f, 1.3f, .85f, 1.5f, .8f, 6, 2),
  VERY_HARD("Sehr schwer", 3.2f, 1.6f, .7f, 2f, .7f, 0, 1);

  public final String title;

  /** Factor on every enemy blow, cast, shockwave. */
  public final float enemyDamage;

  /** Factor on enemy health at spawn. */
  public final float enemyHealth;

  /** Factor on the pauses between enemy attacks; below 1 they swing more often. */
  public final float pace;

  /** Factor on trap damage. */
  public final float trapDamage;

  /** No single hit takes more than this share of the hero's maximum health. */
  public final float maxHitFraction;

  /** Every n-th defeated enemy drops a potion; 0 means none. */
  public final int potionEvery;

  /** Potions a new hero starts with, and the number a shrine refills up to. */
  public final int potions;

  Difficulty(
      String title,
      float enemyDamage,
      float enemyHealth,
      float pace,
      float trapDamage,
      float maxHitFraction,
      int potionEvery,
      int potions) {
    this.title = title;
    this.enemyDamage = enemyDamage;
    this.enemyHealth = enemyHealth;
    this.pace = pace;
    this.trapDamage = trapDamage;
    this.maxHitFraction = maxHitFraction;
    this.potionEvery = potionEvery;
    this.potions = potions;
  }

  /** Clamps a stored ordinal to a valid setting; unknown values read as EASY. */
  public static Difficulty of(int ordinal) {
    return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : EASY;
  }

  public Difficulty next() {
    return values()[(ordinal() + 1) % values().length];
  }

  /** The damage of one enemy blow or trap against a hero with {@code maxHealth}, before armour. */
  public float enemyHit(float raw, boolean trap, int maxHealth) {
    return Math.min(raw * (trap ? trapDamage : enemyDamage), maxHealth * maxHitFraction);
  }

  public boolean dropsPotion(int hash) {
    return potionEvery > 0 && Math.floorMod(hash, potionEvery) == 0;
  }
}
