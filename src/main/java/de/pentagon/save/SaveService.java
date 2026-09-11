package de.pentagon.save;

import com.google.gson.*;
import de.pentagon.core.GameSession;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Versioned, validated snapshots; replace atomically and keep the last readable save. */
public final class SaveService {
  private final Path file;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public SaveService(Path directory) {
    file = directory.resolve("campaign.json");
  }

  public Path path() {
    return file;
  }

  public GameSession copy(GameSession session) {
    return gson.fromJson(gson.toJson(session), GameSession.class);
  }

  public boolean exists() {
    return Files.isRegularFile(file) || Files.isRegularFile(backup());
  }

  private Path backup() {
    return file.resolveSibling("campaign.backup.json");
  }

  public void save(GameSession session) throws IOException {
    session.validate();
    Files.createDirectories(file.getParent());
    Path temporary = Files.createTempFile(file.getParent(), "campaign-", ".tmp");
    try {
      Files.writeString(temporary, gson.toJson(session), StandardCharsets.UTF_8);
      // Never replace a good backup with a corrupt primary file.
      if (Files.exists(file)) {
        try {
          read(file);
          Files.copy(file, backup(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
          /* Preserve the previous valid backup. */
        }
      }
      try {
        Files.move(
            temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public record Loaded(GameSession session, boolean recoveredBackup) {}

  public Loaded load() throws IOException {
    try {
      return new Loaded(read(file), false);
    } catch (IOException primary) {
      try {
        return new Loaded(read(backup()), true);
      } catch (IOException secondary) {
        primary.addSuppressed(secondary);
        throw primary;
      }
    }
  }

  private GameSession read(Path path) throws IOException {
    if (Files.size(path) > 2_000_000) throw new IOException("Spielstand ueberschreitet 2 MB");
    try {
      String json = Files.readString(path, StandardCharsets.UTF_8);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      for (String key :
          new String[] {
            "version",
            "player",
            "inventory",
            "region",
            "flags",
            "defeated",
            "opened",
            "enemies",
            "explored"
          })
        if (!root.has(key) || root.get(key).isJsonNull())
          throw new IllegalArgumentException("Fehlendes Feld: " + key);
      GameSession state = gson.fromJson(root, GameSession.class);
      state.validate();
      return state;
    } catch (RuntimeException e) {
      throw new IOException("Spielstand ist beschädigt oder inkompatibel: " + e.getMessage(), e);
    }
  }
}
