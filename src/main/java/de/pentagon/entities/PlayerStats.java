package de.pentagon.entities;

import de.pentagon.inventory.Inventory;
import java.util.*;

/** Simulation state only: no scene graph, input, rendering or filesystem access. */
public final class PlayerStats {
  public enum Skill {
    VITALITY("Lebenskraft", "+25 maximales Leben"),
    ENDURANCE("Ausdauer", "+18 Ausdauer, schnellere Erholung"),
    BLADE("Klingenmeister", "+5 Nahkampfschaden"),
    ARCANE("Aschenmagie", "+12 Zauberschaden, kuerzerer Cooldown");
    public final String title, description;

    Skill(String title, String description) {
      this.title = title;
      this.description = description;
    }
  }

  public int level = 1, xp, skillPoints, gold = 35;
  public float health = 140, stamina = 110;
  public final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);

  public int rank(Skill skill) {
    return skills.getOrDefault(skill, 0);
  }

  public int maxHealth() {
    return 140 + (level - 1) * 15 + rank(Skill.VITALITY) * 25;
  }

  public int maxStamina() {
    return 110 + rank(Skill.ENDURANCE) * 18;
  }

  public int damage(Inventory inventory) {
    return 18 + (level - 1) * 3 + rank(Skill.BLADE) * 5 + inventory.weapon().power();
  }

  public int spellDamage() {
    return 38 + (level - 1) * 3 + rank(Skill.ARCANE) * 12;
  }

  public float spellCooldown() {
    return Math.max(1.1f, 3.5f - rank(Skill.ARCANE) * .45f);
  }

  public int xpNeeded() {
    return level * 100;
  }

  public int gainXp(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Negative XP");
    xp += amount;
    int gained = 0;
    while (xp >= xpNeeded() && level < 50) {
      xp -= xpNeeded();
      level++;
      skillPoints++;
      gained++;
      health = maxHealth();
      stamina = maxStamina();
    }
    return gained;
  }

  public boolean unlock(Skill skill) {
    if (skillPoints < 1 || rank(skill) >= 5) return false;
    int oldMax = maxHealth();
    skills.put(skill, rank(skill) + 1);
    skillPoints--;
    health += maxHealth() - oldMax;
    return true;
  }

  public boolean spend(float amount) {
    if (stamina < amount) return false;
    stamina -= amount;
    return true;
  }

  public void recover(float dt, boolean resting) {
    if (resting) stamina = Math.min(maxStamina(), stamina + dt * (25 + rank(Skill.ENDURANCE) * 4));
  }

  public float heal(float amount) {
    float before = health;
    health = Math.min(maxHealth(), health + Math.max(0, amount));
    return health - before;
  }

  public void restore() {
    health = maxHealth();
    stamina = maxStamina();
  }

  public void validate() {
    if (level < 1
        || level > 50
        || xp < 0
        || xp > 100000
        || skillPoints < 0
        || skillPoints > 100
        || gold < 0
        || gold > 1000000) throw new IllegalArgumentException("Ungültiger Fortschritt");
    if (skills == null || skills.values().stream().anyMatch(v -> v == null || v < 0 || v > 5))
      throw new IllegalArgumentException("Ungültige Skills");
    if (!Float.isFinite(health)
        || health < 0
        || health > maxHealth()
        || !Float.isFinite(stamina)
        || stamina < 0
        || stamina > maxStamina()) throw new IllegalArgumentException("Ungültige Vitalwerte");
  }
}
