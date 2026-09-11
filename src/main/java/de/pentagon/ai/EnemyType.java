package de.pentagon.ai;

public enum EnemyType {
  GOBLIN("Goblin", 62, 13, 3.9f, 1.9f, .55f, 35, 0x5b805c, .8f),
  ORC("Ork-Brecher", 145, 25, 2.6f, 2.5f, .95f, 70, 0x667758, 1.15f),
  WARDEN("Eiserner Wächter", 230, 29, 2.3f, 2.9f, 1.05f, 115, 0x87939c, 1.2f),
  SHAMAN("Aschenrufer", 90, 19, 2.5f, 12f, 1.05f, 65, 0x79608f, 1f),
  KING("Ork-König", 1300, 38, 2.7f, 3.7f, 1.15f, 500, 0x8a5d4e, 1.85f);
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
