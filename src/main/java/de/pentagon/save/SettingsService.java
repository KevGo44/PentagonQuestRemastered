package de.pentagon.save;

import com.google.gson.*;
import de.pentagon.audio.AudioDirector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Preferences beside the campaign save: audio, difficulty and the last view. Unlike a save game
 * these must never interrupt play: an unreadable or foreign file silently falls back to the
 * defaults.
 */
public final class SettingsService {
  private static final int VERSION = 1;
  private static final long MAX_BYTES = 64_000;

  private final Path file;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  /** Plain data; Gson serializes this and never an engine object. */
  public static final class Settings {
    public int version = VERSION;
    public float master = .75f, music = .8f, effects = .8f;
    public boolean muted;
    public boolean radio = true;

    /** Ordinal of {@link de.pentagon.combat.Difficulty}; 0 is EASY, the game as tuned. */
    public int difficulty;

    /** Last chosen view: false is the trailing camera, true first person ([V]). */
    public boolean firstPerson;

    public void clamp() {
      master = AudioDirector.clamp(master);
      music = AudioDirector.clamp(music);
      effects = AudioDirector.clamp(effects);
      difficulty = de.pentagon.combat.Difficulty.of(difficulty).ordinal();
    }
  }

  public SettingsService(Path directory) {
    file = directory.resolve("settings.json");
  }

  public Path path() {
    return file;
  }

  public Settings load() {
    try {
      if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) return new Settings();
      Settings settings =
          gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), Settings.class);
      if (settings == null || settings.version != VERSION) return new Settings();
      settings.clamp();
      return settings;
    } catch (IOException | RuntimeException e) {
      return new Settings();
    }
  }

  public void save(Settings settings) {
    settings.clamp();
    settings.version = VERSION;
    try {
      Files.createDirectories(file.getParent());
      Path temporary = Files.createTempFile(file.getParent(), "settings-", ".tmp");
      try {
        Files.writeString(temporary, gson.toJson(settings), StandardCharsets.UTF_8);
        try {
          Files.move(
              temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
          Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (IOException | RuntimeException e) {
      // Best effort: losing preferences must never interrupt a running campaign.
    }
  }
}
