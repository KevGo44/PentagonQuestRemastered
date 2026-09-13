package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.math.Vector3f;
import de.pentagon.ai.*;
import de.pentagon.combat.*;
import de.pentagon.entities.PlayerController;
import de.pentagon.world.*;
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

  /** Runs a trap while the player walks along the corridor axis at {@code speed} m/s. */
  private static TrapMechanism.Event walkInto(float start, float speed, float dodgeAt) {
    TrapMechanism trap = new TrapMechanism();
    float along = start, t = 0, dt = 1 / 120f;
    TrapMechanism.Event worst = TrapMechanism.Event.NONE;
    while (t < 3) {
      boolean rolling = dodgeAt >= 0 && t >= dodgeAt && t < dodgeAt + PlayerController.DODGE_TIME;
      float v = rolling ? PlayerController.DODGE_SPEED : speed;
      // Invulnerable for the middle of the roll, exactly as PlayerController.invulnerable().
      boolean invulnerable =
          rolling && t - dodgeAt > .1f && t - dodgeAt < .65f;
      TrapMechanism.Event e = trap.update(dt, along, 0, 0, 4.2f, invulnerable);
      if (e.ordinal() > worst.ordinal()) worst = e;
      along += v * dt;
      t += dt;
    }
    return worst;
  }

  @Test
  void corridorTrapCatchesWalkersAndSprintersButNotADodgeOnTheClick() {
    // From 2.4 m out, walking (4.1 m/s) and sprinting (7 m/s) both end on the strip when it
    // fires 0.4 s after the click; a dodge started on the click rolls through invulnerable.
    assertEquals(TrapMechanism.Event.CAUGHT, walkInto(-TrapMechanism.TRIGGER + .01f, 4.1f, -1));
    assertEquals(TrapMechanism.Event.CAUGHT, walkInto(-TrapMechanism.TRIGGER + .01f, 7f, -1));
    assertEquals(TrapMechanism.Event.FIRED, walkInto(-TrapMechanism.TRIGGER + .01f, 4.1f, 0));
    // A dodge answers the click within 0.3 s or not at all: the roll's invulnerable frames begin
    // 0.1 s in, and the spikes are up 0.4 s after the click.
    assertEquals(TrapMechanism.Event.FIRED, walkInto(-TrapMechanism.TRIGGER + .01f, 4.1f, .25f));
    assertEquals(TrapMechanism.Event.CAUGHT, walkInto(-TrapMechanism.TRIGGER + .01f, 4.1f, .35f));
  }

  @Test
  void corridorTrapIgnoresTheNextLaneAndStandingShortAndReArms() {
    TrapMechanism trap = new TrapMechanism();
    // Beside the corridor: nothing.
    assertEquals(TrapMechanism.Event.NONE, trap.update(.1f, 0, 5, 0, 4.2f, false));
    // Two metres short: it clicks and fires, but nobody is on the strip.
    assertEquals(TrapMechanism.Event.TRIGGERED, trap.update(.1f, -2, 0, 0, 4.2f, false));
    assertEquals(0.07f, trap.rise(), .001f);
    TrapMechanism.Event seen = TrapMechanism.Event.NONE;
    for (int i = 0; i < 6; i++) {
      var e = trap.update(.1f, -2, 0, 0, 4.2f, false);
      if (e != TrapMechanism.Event.NONE) seen = e;
    }
    assertEquals(TrapMechanism.Event.FIRED, seen);
    assertEquals(1, trap.rise(), .001f);
    // It drops and re-arms after the reset (the player has stepped back out of reach) ...
    for (int i = 0; i < 25; i++) trap.update(.1f, -5, 0, 0, 4.2f, false);
    assertEquals(TrapMechanism.Phase.ARMED, trap.phase);
    // ... and fires again at once on whoever then stands on it.
    assertEquals(TrapMechanism.Event.TRIGGERED, trap.update(.01f, 0, 0, 0, 4.2f, false));
    for (int i = 0; i < 5; i++) trap.update(.1f, 0, 0, 0, 4.2f, false);
    assertEquals(TrapMechanism.Event.CAUGHT, trap.update(.01f, 0, 0, 0, 4.2f, false));
    // Only once per firing, and never on someone in the air.
    assertEquals(TrapMechanism.Event.NONE, trap.update(.01f, 0, 0, 0, 4.2f, false));
    TrapMechanism jumped = new TrapMechanism();
    jumped.update(.01f, 0, 0, 0, 4.2f, false);
    for (int i = 0; i < 5; i++) jumped.update(.1f, 0, 0, 1.2f, 4.2f, false);
    assertEquals(TrapMechanism.Event.NONE, jumped.update(.01f, 0, 0, 1.2f, 4.2f, false));
  }

  @Test
  void aCorneredCasterFindsNoWayOutAndAnOpenRoomOffersOne() {
    var map = new DungeonLayout(Region.CRYPT);
    // Saal der Namen spans cells 11..19 x 16..22; its north-east corner (19, 16) has solid wall
    // on both sides (the west wall has a corridor mouth at z 18..20, so that one is no corner).
    Vector3f corner = new Vector3f(19 * DungeonLayout.CELL - .4f, 0, 16 * DungeonLayout.CELL - .4f);
    Vector3f player = corner.add(-1.6f, 0, 1.6f);
    assertNull(EnemyBrain.fleeGoal(corner, player, map), "the corner offers no flight");
    Vector3f middle = new Vector3f(15 * DungeonLayout.CELL, 0, 19 * DungeonLayout.CELL);
    Vector3f goal = EnemyBrain.fleeGoal(middle, middle.add(0, 0, 3), map);
    assertNotNull(goal);
    assertTrue(map.walkable(goal.x, goal.z));
    assertTrue(goal.distance(middle.add(0, 0, 3)) > 3 + 1, "the goal is further from the player");
  }

  @Test
  void raisedEnemyValuesStillLeaveEveryFightWinnableOnPaper() {
    // Hits an unlevelled hero with the rusty sword needs per enemy, and how many blows each enemy
    // needs to empty his 140 health: the numbers the balance pass was tuned against.
    int heroDamage = 18 + 4;
    for (EnemyType type : EnemyType.values()) {
      int hitsToKill = (int) Math.ceil(type.hp / (float) heroDamage);
      int blowsToDie = (int) Math.ceil(140 / (float) Math.max(2, type.damage - 2));
      assertTrue(hitsToKill >= 4, type + " should not fold to a single combo");
      assertTrue(blowsToDie >= 3, type + " should not kill a full hero in two blows");
    }
    assertTrue(EnemyType.GOBLIN.speed > 4, "goblins outrun a walking hero");
    assertTrue(EnemyBrain.AGGRO_RANGE > 13 && EnemyBrain.COOLDOWN < 1.1f);
  }
}
