package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.math.Vector3f;
import de.pentagon.combat.*;
import org.junit.jupiter.api.Test;

class CombatTest {
  @Test
  void meleeHasDirectionAndRange() {
    assertTrue(CombatRules.inArc(0, -2, 0, -1, 2.6f, .3f));
    assertFalse(CombatRules.inArc(0, 2, 0, -1, 2.6f, .3f));
    assertFalse(CombatRules.inArc(0, -3, 0, -1, 2.6f, .3f));
  }

  @Test
  void activeWindowIsNotSkippedByLongFrame() {
    AttackTimeline a = new AttackTimeline();
    a.request();
    assertTrue(a.update(.6f));
    assertFalse(a.update(.01f));
    a.finishIfExpired();
    assertFalse(a.active());
  }

  @Test
  void comboRequiresBufferedInputAndHasThreeAttacks() {
    AttackTimeline a = new AttackTimeline();
    assertTrue(a.request());
    a.update(.2f);
    a.request();
    a.update(.4f);
    assertTrue(a.readyForNext());
    a.next();
    assertEquals(1, a.combo());
    a.update(.2f);
    a.request();
    a.update(.4f);
    a.next();
    assertEquals(2, a.combo());
    assertEquals(1.7f, a.multiplier());
    a.update(.9f);
    a.request();
    assertFalse(a.readyForNext());
    a.finishIfExpired();
    assertFalse(a.active());
  }

  @Test
  void parryWindowCostsStaminaAndPreventsDamage() {
    var hit = CombatRules.defend(30, 5, true, true, .1f, 100, false);
    assertTrue(hit.parried());
    assertEquals(0, hit.healthDamage());
    assertEquals(8, hit.staminaCost());
  }

  @Test
  void sustainedGuardReducesDamage() {
    var hit = CombatRules.defend(30, 5, true, true, .6f, 100, false);
    assertFalse(hit.parried());
    assertEquals(2, hit.healthDamage(), .001);
    assertEquals(21, hit.staminaCost(), .001);
  }

  @Test
  void attackBehindGuardAndShockwaveBypassBlock() {
    assertEquals(25, CombatRules.defend(30, 5, true, false, .1f, 100, false).healthDamage());
    assertEquals(25, CombatRules.defend(30, 5, true, true, .1f, 100, true).healthDamage());
  }

  @Test
  void emptyGuardBreaks() {
    var hit = CombatRules.defend(30, 5, true, true, .6f, 4, false);
    assertTrue(hit.guardBroken());
    assertEquals(4, hit.staminaCost());
    assertEquals(20, hit.healthDamage());
  }

  @Test
  void bossPhaseThresholds() {
    assertEquals(1, CombatRules.bossPhase(1));
    assertEquals(2, CombatRules.bossPhase(.67f));
    assertEquals(3, CombatRules.bossPhase(.34f));
  }

  @Test
  void fastProjectileCrossingTargetHits() {
    assertTrue(
        ProjectileSystem.segmentHit(
                new Vector3f(0, 1, 0), new Vector3f(0, 1, 20), new Vector3f(0, 1, 10), .5f)
            >= 0);
    assertEquals(
        -1,
        ProjectileSystem.segmentHit(
            new Vector3f(0, 1, 0), new Vector3f(0, 1, 20), new Vector3f(2, 1, 10), .5f));
  }
}
