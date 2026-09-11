package de.pentagon.combat;

/** Buffered three-hit chain. Crossing the active interval still hits at a low frame rate. */
public final class AttackTimeline {
  public static final float[] WINDUP = {.15f, .19f, .28f};
  public static final float[] DURATION = {.52f, .58f, .78f};
  public static final float[] MULTIPLIER = {1f, 1.15f, 1.7f};
  private float time;
  private int combo;
  private boolean active, emitted, queued;

  public boolean request() {
    if (active) {
      if (time >= .10f) queued = true;
      return false;
    }
    combo = 0;
    start();
    return true;
  }

  private void start() {
    time = 0;
    active = true;
    emitted = false;
  }

  public boolean update(float dt) {
    if (!active) return false;
    time += dt;
    if (!emitted && time >= WINDUP[combo]) {
      emitted = true;
      return true;
    }
    return false;
  }

  /** Caller pays stamina for every follow-up before advancing. */
  public boolean readyForNext() {
    return active && time >= DURATION[combo] && queued && combo < 2;
  }

  public void next() {
    combo++;
    queued = false;
    start();
  }

  public void finishIfExpired() {
    if (active && time >= DURATION[combo]) cancel();
  }

  public void cancel() {
    active = false;
    queued = false;
    emitted = false;
  }

  public boolean active() {
    return active;
  }

  public int combo() {
    return combo;
  }

  public float multiplier() {
    return MULTIPLIER[combo];
  }
}
