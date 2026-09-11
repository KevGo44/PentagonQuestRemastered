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
