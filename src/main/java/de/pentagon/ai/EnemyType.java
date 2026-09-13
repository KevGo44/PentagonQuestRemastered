package de.pentagon.ai;

/**
 * Enemy values. Raised across the board against a hero of 140 health and 18 base damage: a goblin
 * now takes four opener hits instead of three and lands 16 a bite, an orc's swing costs a fifth of
 * the health bar, the king's blows a third. Wind-ups are shorter than they were, so the parry
 * window (the first 0.23 s of a block) has to be read from the telegraph, not waited for.
 */
public enum EnemyType {
  GOBLIN("Goblin", 70, 16, 4.3f, 1.9f, .5f, 35, 0x5b805c, .8f),
  ORC("Ork-Brecher", 170, 30, 2.8f, 2.5f, .9f, 70, 0x667758, 1.15f),
  WARDEN("Eiserner Wächter", 260, 34, 2.4f, 2.9f, 1f, 115, 0x87939c, 1.2f),
  SHAMAN("Aschenrufer", 100, 24, 2.7f, 12f, 1f, 65, 0x79608f, 1f),
  KING("Ork-König", 1500, 46, 2.9f, 3.7f, 1.1f, 500, 0x8a5d4e, 1.85f);
  public final String title;
  public final int hp, damage, xp, color;
  public final float speed, reach, windup, scale;

  EnemyType(
      String title,
      int hp,
      int damage,
      float speed,
      float reach,
      float windup,
      int xp,
      int color,
      float scale) {
    this.title = title;
    this.hp = hp;
    this.damage = damage;
    this.speed = speed;
    this.reach = reach;
    this.windup = windup;
    this.xp = xp;
    this.color = color;
    this.scale = scale;
  }
}
