package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import de.pentagon.audio.AudioDirector;
import de.pentagon.save.SettingsService;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {
  @TempDir Path dir;

  @Test
  void roundTripPersistsEveryChannel() {
    SettingsService service = new SettingsService(dir);
    SettingsService.Settings written = new SettingsService.Settings();
    written.master = .4f;
    written.music = .15f;
    written.effects = 1f;
    written.muted = true;
    written.radio = false;
    written.difficulty = 2;
    written.firstPerson = true;
    service.save(written);
    SettingsService.Settings read = service.load();
    assertEquals(.4f, read.master, 1e-6);
    assertEquals(.15f, read.music, 1e-6);
    assertEquals(1f, read.effects, 1e-6);
    assertTrue(read.muted);
    assertFalse(read.radio);
    assertEquals(2, read.difficulty);
    assertTrue(read.firstPerson);
    // An out-of-range difficulty from a hand-edited file falls back to easy.
    written.difficulty = 9;
    service.save(written);
    assertEquals(0, service.load().difficulty);
  }

  @Test
  void missingFileUsesDefaults() {
    SettingsService.Settings settings = new SettingsService(dir).load();
    assertEquals(.75f, settings.master, 1e-6);
    assertEquals(.8f, settings.music, 1e-6);
    assertEquals(.8f, settings.effects, 1e-6);
    assertFalse(settings.muted);
    assertTrue(settings.radio, "The recorded track is the default exploration music");
    assertEquals(0, settings.difficulty, "easy by default");
    assertFalse(settings.firstPerson, "the trailing camera by default");
  }

  @Test
  void corruptFileFallsBackToDefaultsInsteadOfThrowing() throws Exception {
    SettingsService service = new SettingsService(dir);
    Files.writeString(service.path(), "{not json");
    assertEquals(.75f, service.load().master, 1e-6);
  }

  @Test
  void unknownVersionIsIgnored() throws Exception {
    SettingsService service = new SettingsService(dir);
    Files.writeString(service.path(), "{\"version\":99,\"master\":0.1}");
    assertEquals(.75f, service.load().master, 1e-6);
  }

  @Test
  void oversizedFileIsIgnored() throws Exception {
    SettingsService service = new SettingsService(dir);
    Files.writeString(service.path(), "{\"version\":1,\"master\":0.1," + " ".repeat(70_000) + "}");
    assertEquals(.75f, service.load().master, 1e-6);
  }

  @Test
  void outOfRangeValuesAreClampedOnWriteAndRead() throws Exception {
    SettingsService service = new SettingsService(dir);
    SettingsService.Settings settings = new SettingsService.Settings();
    settings.master = 4.5f;
    settings.music = -2f;
    settings.effects = Float.NaN;
    service.save(settings);
    assertEquals(1f, settings.master, 1e-6, "save() normalises the object it was handed");
    SettingsService.Settings read = service.load();
    assertEquals(1f, read.master, 1e-6);
    assertEquals(0f, read.music, 1e-6);
    assertEquals(0f, read.effects, 1e-6);
  }

  @Test
  void handEditedFileIsBoundedOnLoad() throws Exception {
    SettingsService service = new SettingsService(dir);
    Files.writeString(service.path(), "{\"version\":1,\"master\":9,\"music\":-1,\"effects\":0.5}");
    SettingsService.Settings read = service.load();
    assertEquals(1f, read.master, 1e-6);
    assertEquals(0f, read.music, 1e-6);
    assertEquals(.5f, read.effects, 1e-6);
  }

  @Test
  void volumeClampRejectsEveryInvalidLevel() {
    assertEquals(0f, AudioDirector.clamp(-.3f), 1e-6);
    assertEquals(1f, AudioDirector.clamp(12f), 1e-6);
    assertEquals(0f, AudioDirector.clamp(Float.NaN), 1e-6);
    assertEquals(1f, AudioDirector.clamp(Float.POSITIVE_INFINITY), 1e-6);
    assertEquals(0f, AudioDirector.clamp(Float.NEGATIVE_INFINITY), 1e-6);
    assertEquals(.42f, AudioDirector.clamp(.42f), 1e-6);
  }
}
