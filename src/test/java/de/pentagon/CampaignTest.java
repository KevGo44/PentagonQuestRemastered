package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import de.pentagon.ai.EnemyType;
import de.pentagon.core.GameSession;
import de.pentagon.quest.QuestJournal;
import de.pentagon.world.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CampaignTest {
  @ParameterizedTest
  @EnumSource(Region.class)
  void everyRoomAndObjectiveIsReachable(Region region) {
    var map = new DungeonLayout(region);
    assertTrue(map.rooms.size() >= 5);
    assertTrue(map.walkable(map.spawnX(), map.spawnZ()));
    for (var r : map.rooms)
      assertFalse(
          map.path(
                  map.spawnX(),
                  map.spawnZ(),
                  r.x() * DungeonLayout.CELL,
                  r.z() * DungeonLayout.CELL)
              .isEmpty(),
          r.name());
    for (var o : map.objects) {
      assertTrue(map.walkable(o.x(), o.z()), o.id());
      assertFalse(map.path(map.spawnX(), map.spawnZ(), o.x(), o.z()).isEmpty(), o.id());
    }
    for (var e : map.enemies) assertTrue(map.walkable(e.x(), e.z()), e.id());
  }

  @Test
  void authoredIdsAreGloballyUnique() {
    Set<String> ids = new HashSet<>();
    for (Region region : Region.values()) {
      var m = new DungeonLayout(region);
      for (var e : m.enemies) assertTrue(ids.add(e.id()));
      for (var o : m.objects) assertTrue(ids.add(o.id()));
    }
  }

  @Test
  void returnRoutesAndBossExist() {
    for (Region r : Region.values())
      assertTrue(
          new DungeonLayout(r)
              .objects.stream().anyMatch(o -> o.kind() == DungeonLayout.Kind.PORTAL));
    assertEquals(
        1,
        new DungeonLayout(Region.THRONE)
            .enemies.stream().filter(e -> e.type() == EnemyType.KING).count());
  }

  @Test
  void parallelObjectivesDoNotRequireAnOrder() {
    for (boolean cryptFirst : new boolean[] {true, false}) {
      GameSession s = new GameSession();
      s.flags.add("mira_met");
      s.inventory.add(cryptFirst ? "crypt_seal" : "cave_seal", 1);
      assertFalse(s.sealsReady());
      s.inventory.add(cryptFirst ? "cave_seal" : "crypt_seal", 1);
      assertTrue(s.sealsReady());
    }
  }

  @Test
  void questRewardsCannotBeFarmed() {
    GameSession s = new GameSession();
    s.flags.add("mira_met");
    QuestJournal journal = new QuestJournal();
    journal.refresh(s, v -> {});
    int gold = s.player.gold, xp = s.player.xp;
    journal.refresh(s, v -> {});
    assertEquals(gold, s.player.gold);
    assertEquals(xp, s.player.xp);
    assertEquals(Set.of("mira"), s.rewardedQuests);
  }

  @Test
  void huntingObjectiveIsAchievableWithAuthoredGoblins() {
    long goblins =
        Arrays.stream(Region.values())
            .flatMap(r -> new DungeonLayout(r).enemies.stream())
            .filter(e -> e.type() == EnemyType.GOBLIN)
            .count();
    assertTrue(goblins >= 8);
  }

  @Test
  void allFiveChronicleFragmentsExist() {
    long lore =
        Arrays.stream(Region.values())
            .flatMap(r -> new DungeonLayout(r).objects.stream())
            .filter(o -> o.kind() == DungeonLayout.Kind.LORE)
            .count();
    assertEquals(5, lore);
  }

  @ParameterizedTest
  @EnumSource(Region.class)
  void trapsFillTheirCorridorsFromWallToWall(Region region) {
    // A trap is a strip across a corridor: every cell across it is floor, the cells beyond on
    // both sides are wall, and it does not sit inside a room. There are more of them now, and
    // the throne hall has none in front of the king - his court is his own trap.
    var map = new DungeonLayout(region);
    var traps = map.objects.stream().filter(o -> o.kind() == DungeonLayout.Kind.TRAP).toList();
    assertTrue(traps.size() >= (region == Region.REFUGE ? 1 : 3), region + " has traps");
    for (var trap : traps) {
      assertTrue(trap.value().matches("[xz][35]"), trap.id() + " axis and width " + trap.value());
      assertEquals("Verbindungsgang", map.roomAt(trap.x(), trap.z()), trap.id());
      boolean alongX = DungeonLayout.trapAxisX(trap);
      int cells = DungeonLayout.trapCells(trap);
      int cx = Math.round(trap.x() / DungeonLayout.CELL),
          cz = Math.round(trap.z() / DungeonLayout.CELL);
      int half = cells / 2;
      for (int d = -half - 1; d <= half + 1; d++) {
        int x = alongX ? cx : cx + d, z = alongX ? cz + d : cz;
        boolean floor = Math.abs(d) <= half;
        assertEquals(
            floor,
            map.walkable(x, z),
            trap.id() + " expects " + (floor ? "floor" : "wall") + " at " + x + "," + z);
      }
    }
    long total =
        Arrays.stream(Region.values())
            .flatMap(r -> new DungeonLayout(r).objects.stream())
            .filter(o -> o.kind() == DungeonLayout.Kind.TRAP)
            .count();
    assertTrue(total >= 15, "at least fifteen traps across the campaign, found " + total);
  }

  @Test
  void leavingARegionRevivesItsEnemiesButNotTheFallenKing() {
    GameSession s = new GameSession();
    s.region = Region.CRYPT;
    s.defeated.addAll(List.of("CRYPT_g0", "CRYPT_w0", "REFUGE_raider0"));
    s.enemies.put("CRYPT_o0", new GameSession.EnemySave(40, 1, 2));
    s.enemies.put("REFUGE_raider1", new GameSession.EnemySave(30, 3, 4));
    s.leave(Region.CRYPT);
    assertEquals(Set.of("REFUGE_raider0"), s.defeated);
    assertEquals(Set.of("REFUGE_raider1"), s.enemies.keySet());
    // The king stays dead once the crown has fallen; before that he comes back like anyone.
    s.defeated.add("THRONE_king");
    s.leave(Region.THRONE);
    assertFalse(s.defeated.contains("THRONE_king"));
    s.defeated.add("THRONE_king");
    s.flags.add("king_dead");
    s.leave(Region.THRONE);
    assertTrue(s.defeated.contains("THRONE_king"));
  }

  @Test
  void droppedItemsAreRememberedPerRegionAndValidated() {
    GameSession s = new GameSession();
    s.region = Region.CAVERNS;
    var first = s.drop("potion", 10, 12);
    var second = s.drop("potion", 10, 12);
    assertNotEquals(first.serial(), second.serial());
    assertEquals("CAVERNS", first.region());
    s.validate();
    s.drops.add(new GameSession.Drop(9, "CAVERNS", "not_an_item", 0, 0));
    assertThrows(IllegalArgumentException.class, s::validate);
  }

  @Test
  void navigationRejectsWallShortcut() {
    var m = new DungeonLayout(Region.REFUGE);
    assertFalse(
        m.clearLine(
            5 * DungeonLayout.CELL,
            7 * DungeonLayout.CELL,
            25 * DungeonLayout.CELL,
            7 * DungeonLayout.CELL));
    assertFalse(
        m.path(
                5 * DungeonLayout.CELL,
                7 * DungeonLayout.CELL,
                25 * DungeonLayout.CELL,
                7 * DungeonLayout.CELL)
            .isEmpty());
  }
}
