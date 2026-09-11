package de.pentagon.combat;

public final class CombatRules {
  private CombatRules() {}

  public static boolean inArc(
      float dx, float dz, float forwardX, float forwardZ, float reach, float cosine) {
    float distanceSquared = dx * dx + dz * dz;
    if (distanceSquared > reach * reach) return false;
    if (distanceSquared < .001f) return true;
    return (dx * forwardX + dz * forwardZ) / (float) Math.sqrt(distanceSquared) >= cosine;
  }

  public record Hit(float healthDamage, float staminaCost, boolean parried, boolean guardBroken) {}

  public static Hit defend(
      float raw,
      int armor,
      boolean blocking,
      boolean inFront,
      float parryAge,
      float stamina,
      boolean unblockable) {
    float damage = Math.max(2, raw - armor);
    if (blocking && inFront && !unblockable) {
      if (parryAge <= .23f && stamina >= 8) return new Hit(0, 8, true, false);
      float cost = raw * .7f;
      if (stamina >= cost) return new Hit(damage * .08f, cost, false, false);
      return new Hit(damage * .8f, stamina, false, true);
    }
    return new Hit(damage, 0, false, false);
  }

  public static int bossPhase(float healthFraction) {
    return healthFraction > .67f ? 1 : healthFraction > .34f ? 2 : 3;
  }
}
