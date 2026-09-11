package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import de.pentagon.core.GameSession;
import de.pentagon.entities.PlayerStats;
import de.pentagon.save.SaveService;
import de.pentagon.world.Region;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveTest {
  @TempDir Path dir;

  @Test
  void roundTripPersistsCampaignAndEquipment() throws Exception {
    GameSession s = new GameSession();
    s.player.gainXp(350);
    s.player.unlock(PlayerStats.Skill.BLADE);
    s.inventory.add("long_sword", 1);
    s.inventory.equip("long_sword");
    s.region = Region.PRISON;
    s.x = 42;
    s.z = 17;
    s.flags.add("prisoners_freed");
    s.defeated.add("CRYPT_g0");
    s.opened.add("REFUGE_lore");
    s.rewardedQuests.add("mira");
    s.counters.put("lore", 3);
    s.enemies.put("PRISON_keeper", new GameSession.EnemySave(12, 70, 16));
    s.explored.put("PRISON", new HashSet<>(List.of(14, 15, 16)));
    SaveService service = new SaveService(dir);
    service.save(s);
    GameSession actual = service.load().session();
    assertEquals(s.region, actual.region);
    assertEquals(s.player.level, actual.player.level);
    assertEquals(1, actual.player.rank(PlayerStats.Skill.BLADE));
    assertEquals("long_sword", actual.inventory.weapon().id());
    assertEquals(s.flags, actual.flags);
    assertEquals(s.defeated, actual.defeated);
    assertEquals(s.enemies, actual.enemies);
    assertEquals(s.explored, actual.explored);
    assertEquals(17, actual.z);
  }

  @Test
  void brokenPrimaryRecoversPreviousSnapshot() throws Exception {
    SaveService service = new SaveService(dir);
    GameSession s = new GameSession();
    service.save(s);
    s.player.gold = 99;
    service.save(s);
    Files.writeString(service.path(), "{broken");
    var loaded = service.load();
    assertTrue(loaded.recoveredBackup());
    assertEquals(35, loaded.session().player.gold);
  }

  @Test
  void brokenPrimaryDoesNotOverwriteGoodBackup() throws Exception {
    SaveService service = new SaveService(dir);
    GameSession s = new GameSession();
    service.save(s);
    s.player.gold = 45;
    service.save(s);
    Files.writeString(service.path(), "broken");
    s.player.gold = 60;
    service.save(s);
    Files.writeString(service.path(), "broken again");
    assertEquals(35, service.load().session().player.gold);
  }

  @Test
  void futureVersionRejected() throws Exception {
    SaveService service = new SaveService(dir);
    service.save(new GameSession());
    Files.writeString(
        service.path(),
        Files.readString(service.path()).replace("\"version\": 1", "\"version\": 999"));
    assertThrows(IOException.class, service::load);
  }

  @Test
  void truncatedObjectRejected() throws Exception {
    SaveService service = new SaveService(dir);
    Files.writeString(service.path(), "{}");
    assertThrows(IOException.class, service::load);
  }

  @Test
  void invalidStatsNeverReplaceExistingSave() throws Exception {
    SaveService service = new SaveService(dir);
    GameSession s = new GameSession();
    service.save(s);
    s.player.health = Float.NaN;
    assertThrows(IllegalArgumentException.class, () -> service.save(s));
    assertEquals(140, service.load().session().player.health);
  }

  @Test
  void snapshotIsIndependent() {
    SaveService service = new SaveService(dir);
    GameSession source = new GameSession();
    GameSession copy = service.copy(source);
    source.inventory.add("potion", 1);
    source.flags.add("changed");
    assertEquals(5, copy.inventory.count("potion"));
    assertFalse(copy.flag("changed"));
  }
}
