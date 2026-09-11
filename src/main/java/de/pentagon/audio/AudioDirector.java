package de.pentagon.audio;

import com.jme3.asset.AssetManager;
import com.jme3.audio.*;
import java.util.*;

/** Crossfaded music stems and a bounded voice pool; no per-hit asset loading. */
public final class AudioDirector {
  private final AudioNode exploration, combat, ambient;
  private final Map<String, AudioNode[]> effects = new HashMap<>();
  private final Map<String, Integer> cursors = new HashMap<>();
  private float mix, volume = .75f;
  private boolean muted;

  public AudioDirector(AssetManager assets, boolean enabled) {
    if (!enabled) {
      exploration = combat = ambient = null;
      return;
    }
    exploration = loop(assets, "exploration");
    combat = loop(assets, "combat");
    ambient = loop(assets, "ambient");
    for (String name : List.of("swing", "hit", "parry", "spell", "chime", "step", "hurt")) {
      AudioNode[] voices = new AudioNode[4];
      for (int i = 0; i < voices.length; i++) {
        voices[i] = new AudioNode(assets, "audio/" + name + ".wav", AudioData.DataType.Buffer);
        voices[i].setPositional(false);
        voices[i].setVolume(name.equals("step") ? .12f : .6f);
      }
      effects.put(name, voices);
    }
  }

  private AudioNode loop(AssetManager a, String name) {
    AudioNode n = new AudioNode(a, "audio/" + name + ".wav", AudioData.DataType.Buffer);
    n.setPositional(false);
    n.setLooping(true);
    n.setVolume(0);
    n.play();
    return n;
  }

  public void update(float dt, boolean danger, boolean paused) {
    if (exploration == null) return;
    mix += (danger ? 1 - mix : -mix) * Math.min(1, dt * (danger ? 2 : .5f));
    float gain = muted ? 0 : volume * (paused ? .45f : 1);
    exploration.setVolume(gain * (1 - mix * .85f) * .5f);
    combat.setVolume(gain * mix * .7f);
    ambient.setVolume(gain * .38f);
  }

  public void play(String id) {
    if (muted) return;
    AudioNode[] pool = effects.get(id);
    if (pool == null) return;
    int cursor = cursors.getOrDefault(id, 0);
    AudioNode node = pool[cursor];
    node.stop();
    node.play();
    cursors.put(id, (cursor + 1) % pool.length);
  }

  public void toggleMute() {
    muted = !muted;
    if (muted) effects.values().forEach(p -> Arrays.stream(p).forEach(AudioNode::stop));
  }

  public boolean muted() {
    return muted;
  }

  public void cleanup() {
    if (exploration != null) {
      exploration.stop();
      combat.stop();
      ambient.stop();
    }
    effects.values().forEach(p -> Arrays.stream(p).forEach(AudioNode::stop));
  }
}
