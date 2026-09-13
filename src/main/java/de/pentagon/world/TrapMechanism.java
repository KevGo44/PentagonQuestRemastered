package de.pentagon.world;

/**
 * One corridor trap: a strip of spikes flush with the floor, from wall to wall, that fires when the
 * player comes close and catches whoever is still on it.
 *
 * <p>The old plate was a glowing square in the middle of a corridor that popped up on a timer; it
 * was walked around. This one is hidden until it clicks, and the timing is what makes it fair: from
 * the trigger line at {@link #TRIGGER} m a walking player (4.1 m/s) covers 1.6 m during the {@link
 * #TELEGRAPH} and stands on the strip when it fires; a sprinter (7 m/s) is 0.4 m past the centre,
 * still on it; a dodge started on the click is invulnerable from 0.1 s to 0.65 s of its 0.77 s and
 * rolls through - so the click has to be answered, not noticed. After {@link #FIRE} seconds the
 * spikes drop and the trap re-arms {@link #RESET} seconds later; anyone still in front of it then
 * triggers it again at once.
 *
 * <p>Pure logic: coordinates come in already projected onto the trap's own axes.
 */
public final class TrapMechanism {
  public enum Phase {
    ARMED,
    TELEGRAPH,
    FIRING,
    RESET
  }

  public enum Event {
    NONE,
    TRIGGERED,
    FIRED,
    CAUGHT
  }

  /** Metres along the corridor from the strip's centre at which a player sets it off. */
  public static final float TRIGGER = 2.4f;

  /** Half depth of the spike strip along the corridor, metres. */
  public static final float HALF_DEPTH = 1f;

  /** Heads higher than this are jumping over it. */
  public static final float CLEAR_HEIGHT = .75f;

  public static final float TELEGRAPH = .4f, FIRE = .75f, RESET = 1.5f;
  public static final float DAMAGE = 60;

  public Phase phase = Phase.ARMED;
  private float timer;
  private boolean caught;

  /**
   * @param along metres from the strip's centre along the direction of travel
   * @param across metres from the corridor's centre line
   * @param height the player's feet over the floor
   * @param halfWidth half the corridor width
   * @param invulnerable true while the player cannot be hurt (rolling); the trap still fires
   */
  public Event update(
      float dt, float along, float across, float height, float halfWidth, boolean invulnerable) {
    timer -= dt;
    boolean inLane = Math.abs(across) < halfWidth;
    switch (phase) {
      case ARMED -> {
        if (inLane && Math.abs(along) < TRIGGER && height < 1.5f) {
          phase = Phase.TELEGRAPH;
          timer = TELEGRAPH;
          return Event.TRIGGERED;
        }
      }
      case TELEGRAPH -> {
        if (timer <= 0) {
          phase = Phase.FIRING;
          timer = FIRE;
          caught = false;
          return Event.FIRED;
        }
      }
      case FIRING -> {
        if (!caught
            && inLane
            && Math.abs(along) < HALF_DEPTH
            && height < CLEAR_HEIGHT
            && !invulnerable) {
          caught = true;
          return Event.CAUGHT;
        }
        if (timer <= 0) {
          phase = Phase.RESET;
          timer = RESET;
        }
      }
      case RESET -> {
        if (timer <= 0) phase = Phase.ARMED;
      }
    }
    return Event.NONE;
  }

  /** 0 = spikes below the floor, 1 = fully up. The telegraph only shows their tips. */
  public float rise() {
    return switch (phase) {
      case ARMED -> 0;
      case TELEGRAPH -> .07f;
      case FIRING -> Math.min(1, (FIRE - timer) / .06f);
      case RESET -> Math.max(0, 1 - (RESET - timer) / .35f);
    };
  }
}
