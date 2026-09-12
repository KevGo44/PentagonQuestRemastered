package de.pentagon.audio;

import com.jme3.asset.AssetManager;
import com.jme3.audio.*;
import java.util.*;

/** Crossfaded music stems and a bounded voice pool; no per-hit asset loading. */
public final class AudioDirector {
  /** The long recorded track; short stems stay buffered. */
  public static final String RADIO = "pentagonradio";

  private static final Map<String, Float> EFFECT_GAIN =
      Map.of(
          "swing", .6f,
          "hit", .6f,
          "parry", .6f,
          "spell", .6f,
          "chime", .6f,
          "hurt", .6f,
          "step", .12f);

  private final AudioNode exploration, combat, ambient, radio;
  private final Map<String, AudioNode[]> pools = new HashMap<>();
  private final Map<String, Integer> cursors = new HashMap<>();
  private float mix;
  private float master = .75f, music = .8f, effectLevel = .8f;
  private boolean muted, radioOn = true;

  public AudioDirector(AssetManager assets, boolean enabled) {
    if (!enabled) {
      exploration = combat = ambient = radio = null;
      return;
    }
    exploration = loop(assets, "exploration", AudioData.DataType.Buffer);
    combat = loop(assets, "combat", AudioData.DataType.Buffer);
    ambient = loop(assets, "ambient", AudioData.DataType.Buffer);
    // Three minutes of 48 kHz stereo PCM. Streaming keeps it off the heap and out of the
    // startup path; the WAV loader rewinds a looping stream through SeekableStream.setTime(0).
    radio = loop(assets, RADIO, AudioData.DataType.Stream);
    for (String name : EFFECT_GAIN.keySet()) {
      AudioNode[] voices = new AudioNode[4];
      for (int i = 0; i < voices.length; i++) {
        voices[i] = new AudioNode(assets, "audio/" + name + ".wav", AudioData.DataType.Buffer);
        voices[i].setPositional(false);
        voices[i].setVolume(EFFECT_GAIN.get(name));
      }
      pools.put(name, voices);
    }
  }

  private AudioNode loop(AssetManager a, String name, AudioData.DataType type) {
    AudioNode n = new AudioNode(a, "audio/" + name + ".wav", type);
    n.setPositional(false);
    n.setLooping(true);
    n.setVolume(0);
    n.play();
    return n;
  }

  /** Bounds a slider value; a broken preferences file must not silence or deafen the game. */
  public static float clamp(float value) {
    if (Float.isNaN(value)) return 0;
    return Math.max(0, Math.min(1, value));
  }

  public void update(float dt, boolean danger, boolean ducked) {
    if (exploration == null) return;
    mix += (danger ? 1 - mix : -mix) * Math.min(1, dt * (danger ? 2 : .5f));
    float gain = (muted ? 0 : master * music) * (ducked ? .45f : 1);
    float calm = 1 - mix * .85f;
    // The radio replaces the synthetic exploration stem; combat still fades in over both.
    radio.setVolume(radioOn ? gain * calm * .6f : 0);
    exploration.setVolume(radioOn ? 0 : gain * calm * .5f);
    combat.setVolume(gain * mix * .7f);
    ambient.setVolume(gain * .38f);
  }

  public void play(String id) {
    if (muted) return;
    AudioNode[] pool = pools.get(id);
    if (pool == null) return;
    int cursor = cursors.getOrDefault(id, 0);
    AudioNode node = pool[cursor];
    node.setVolume(EFFECT_GAIN.getOrDefault(id, .6f) * master * effectLevel);
    node.stop();
    node.play();
    cursors.put(id, (cursor + 1) % pool.length);
  }

  public float master() {
    return master;
  }

  public void master(float value) {
    master = clamp(value);
  }

  public float music() {
    return music;
  }

  public void music(float value) {
    music = clamp(value);
  }

  public float effects() {
    return effectLevel;
  }

  public void effects(float value) {
    effectLevel = clamp(value);
  }

  public boolean radio() {
    return radioOn;
  }

  public void radio(boolean on) {
    radioOn = on;
  }

  public void toggleRadio() {
    radioOn = !radioOn;
  }

  public void toggleMute() {
    muted(!muted);
  }

  public void muted(boolean value) {
    muted = value;
    if (muted) pools.values().forEach(p -> Arrays.stream(p).forEach(AudioNode::stop));
  }

  public boolean muted() {
    return muted;
  }

  public void cleanup() {
    if (exploration != null) {
      exploration.stop();
      combat.stop();
      ambient.stop();
      radio.stop();
    }
    pools.values().forEach(p -> Arrays.stream(p).forEach(AudioNode::stop));
  }
}
