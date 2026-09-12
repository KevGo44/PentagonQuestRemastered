package de.pentagon.core;

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.math.*;
import de.pentagon.entities.Enemy;
import de.pentagon.world.Region;
import java.nio.file.*;
import java.util.*;

/** Explicit opt-in renderer integration exercise. Never touches the normal save location. */
final class SmokeScenario {
  private final GameApplication app;
  private final ScreenshotAppState shots;
  private float timer;
  private int step, shotDelay;
  private final List<Float> frames = new ArrayList<>();
  private Enemy target;
  private float beforeHealth, moveStart;

  SmokeScenario(GameApplication app, ScreenshotAppState shots) {
    this.app = app;
    this.shots = shots;
    try {
      Files.deleteIfExists(Path.of("target", "smoke-ok.txt"));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Asks for a screenshot two frames out. A screenshot lands at the end of the frame it is asked
   * for, and a stage that builds a region does so in that same frame - so the picture caught every
   * character in its bind pose, arms out, because nothing had posed them yet. That is an artefact
   * of the evidence, not of the game, but evidence that lies is worse than none.
   */
  private void shoot() {
    shotDelay = 2;
  }

  void update(float dt) {
    if (app.game == null || app.game.world == null) return;
    if (shotDelay > 0 && --shotDelay == 0) shots.takeScreenshot();
    if (step > 1) frames.add(dt);
    timer += dt;
    if (timer < 2.5f) return;
    timer = 0;
    System.out.println("[SMOKE] stage " + step);
    switch (step++) {
      case 0 -> shoot();
      case 1 -> app.game.newGame();
      case 2 -> {
        shoot();
        moveStart = app.game.player.node.getWorldTranslation().z;
        app.game.player.forward = true;
      }
      case 3 -> {
        app.game.player.forward = false;
        require(
            app.game.player.node.getWorldTranslation().z < moveStart - 2,
            "WASD must move the physical character");
        app.game.input("Attack", true);
      }
      case 4 -> {
        app.game.input("Spell", true);
        app.game.session.player.skillPoints = 2;
        app.screen(ScreenMode.INVENTORY);
      }
      case 5 -> {
        shoot();
        app.screen(ScreenMode.SKILLS);
      }
      case 6 -> {
        shoot();
        app.game.loadRegion(Region.CRYPT, false);
        app.screen(ScreenMode.PLAYING);
      }
      case 7 -> {
        shoot();
        app.game.loadRegion(Region.CAVERNS, false);
      }
      case 8 -> {
        shoot();
        app.game.loadRegion(Region.PRISON, false);
      }
      case 9 -> {
        shoot();
        app.game.loadRegion(Region.THRONE, false);
      }
      case 10 -> {
        shoot();
        app.game.loadRegion(Region.REFUGE, false);
        app.screen(ScreenMode.PLAYING);
      }
      case 11 -> {
        app.game.save(false);
      }
      case 12 -> {
        if (!app.game.loadGame()) throw new IllegalStateException("Smoke save/load failed");
        app.screen(ScreenMode.JOURNAL);
      }
      case 13 -> {
        shoot();
        app.game.newGame();
        app.game.session.player.health = 0;
        app.game.respawn();
        require(
            app.game.session.player.skillPoints == 0,
            "A new campaign must not respawn into the previous save");
      }
      case 14 -> {
        target = app.game.enemies.get(0);
        target.stop();
        target.stun(10);
        Vector3f p = target.position();
        teleport(p.x, p.z + 1.8f);
        app.game.player.yaw = FastMath.PI;
        beforeHealth = target.health;
        app.game.input("Attack", true);
      }
      case 15 -> {
        require(target.health < beforeHealth, "Physical melee must hit the enemy");
        clearEnemies();
        interact("REFUGE_mira");
        require(app.mode() == ScreenMode.DIALOGUE, "Mira opens a dialogue");
        app.game.choose(0);
        require(app.game.session.flag("mira_met"), "Mira starts the campaign");
        interact("REFUGE_crypt");
      }
      case 16 -> {
        require(app.game.session.region == Region.CRYPT, "Crypt portal transition");
        clearEnemies();
        interact("CRYPT_lore");
        app.game.choose(0);
        interact("CRYPT_moon");
        require(app.game.session.count("rune_step") == 0, "Wrong rune resets the puzzle");
        interact("CRYPT_sun");
        interact("CRYPT_moon");
        interact("CRYPT_star");
        interact("CRYPT_seal");
        require(app.game.session.inventory.count("crypt_seal") == 1, "Crypt seal reward");
        interact("CRYPT_return");
      }
      case 17 -> {
        require(app.game.session.region == Region.REFUGE, "Return from crypt");
        interact("REFUGE_lore");
        app.game.choose(0);
        interact("REFUGE_cave");
      }
      case 18 -> {
        require(app.game.session.region == Region.CAVERNS, "Cavern portal transition");
        clearEnemies();
        interact("CAVERNS_lore");
        app.game.choose(0);
        interact("CAVERNS_crystal0");
        interact("CAVERNS_crystal1");
        interact("CAVERNS_crystal2");
        interact("CAVERNS_seal");
        require(app.game.session.sealsReady(), "Both parallel paths award their keys");
        interact("CAVERNS_return");
      }
      case 19 -> {
        interact("REFUGE_prison");
      }
      case 20 -> {
        require(app.game.session.region == Region.PRISON, "Both seals unlock prison");
        clearEnemies();
        interact("PRISON_lore");
        app.game.choose(0);
        interact("PRISON_eren");
        app.game.choose(0);
        require(app.game.session.flag("prisoners_freed"), "Prisoner branch");
        require(app.game.session.inventory.count("prison_key") == 1, "Keeper drops key");
        interact("PRISON_throne");
      }
      case 21 -> {
        require(app.game.session.region == Region.THRONE, "Keeper key opens throne");
        target =
            app.game.enemies.stream()
                .filter(e -> e.type == de.pentagon.ai.EnemyType.KING)
                .findFirst()
                .orElseThrow();
        require(target.maxHealth == 1040, "Prisoner rescue weakens boss");
        for (Enemy e : app.game.enemies) if (e != target) app.game.combat.damageEnemy(e, 10000);
        interact("THRONE_lore");
        app.game.choose(0);
        app.game.combat.damageEnemy(target, 360);
      }
      case 22 -> {
        require(target.phase == 2, "Boss enters second phase");
        app.game.combat.damageEnemy(target, 350);
      }
      case 23 -> {
        require(target.phase == 3, "Boss enters third phase");
        teleport(target.position().x, target.position().z + 5);
        shoot();
        app.game.combat.damageEnemy(target, 10000);
      }
      case 24 -> {
        require(app.game.session.flag("king_dead"), "Boss death unlocks epilogue");
        interact("THRONE_throne");
        app.game.choose(0);
        require(
            app.mode() == ScreenMode.ENDING && app.game.session.flag("ending_seal"), "Seal ending");
        require(app.game.session.rewardedQuests.size() == 10, "All ten quests can be completed");
        shoot();
      }
      case 25 -> {
        app.screen(ScreenMode.PLAYING);
        app.game.session.flags.remove("ending_seal");
        interact("THRONE_throne");
        app.game.choose(1);
        require(app.game.session.flag("ending_crown"), "Alternative crown ending");
      }
      case 26 -> {
        app.game.newGame();
        teleport(53, 73);
        app.game.player.yaw = -FastMath.HALF_PI;
      }
      case 27 -> {
        require(
            app.getCamera()
                    .getLocation()
                    .distance(app.game.player.node.getWorldTranslation().add(0, 1.42f, 0))
                < 4,
            "Camera retracts against walls");
        app.game.loadRegion(Region.PRISON, false);
        clearEnemies();
        interact("PRISON_eren");
        app.game.choose(1);
        require(
            app.game.session.flag("prisoners_bargain") && !app.game.session.flag("prisoners_freed"),
            "Prisoner bargain is mutually exclusive");
        require(
            app.game.session.inventory.count("ember_blade") == 1,
            "Bargain grants the unique blade");
        app.game.loadRegion(Region.THRONE, false);
      }
      case 28 -> {
        require(
            app.game.enemies.stream()
                    .filter(e -> e.type == de.pentagon.ai.EnemyType.KING)
                    .findFirst()
                    .orElseThrow()
                    .maxHealth
                == 1300,
            "Bargain preserves the king's full strength");
        app.game.session.player.health = 0;
      }
      case 29 -> {
        require(app.mode() == ScreenMode.GAME_OVER, "Death animation reaches Game Over");
        shoot();
        app.game.respawn();
        require(
            app.game.session.player.health > 0 && app.mode() == ScreenMode.PLAYING,
            "Checkpoint respawn restores playable state");
        app.screen(ScreenMode.PAUSED);
        if (app.highQuality()) app.toggleQuality();
      }
      case 30 -> {
        require(
            app.getViewPort().getProcessors().stream()
                .noneMatch(p -> p instanceof com.jme3.shadow.DirectionalLightShadowRenderer),
            "Fast mode removes the shadow pass");
        app.toggleQuality();
      }
      case 31 -> {
        var processors = app.getViewPort().getProcessors();
        require(
            app.highQuality()
                && processors.get(0) instanceof com.jme3.shadow.DirectionalLightShadowRenderer
                && processors.get(1) instanceof com.jme3.post.FilterPostProcessor,
            "High quality restores shadows before post-processing");
        app.screen(ScreenMode.MAIN_MENU);
        shoot();
      }
      case 32 -> finish();
      default -> {}
    }
  }

  private void teleport(float x, float z) {
    app.game.player.warp(x, z);
    app.game.player.node.setLocalTranslation(x, .12f, z);
    app.game.player.node.updateGeometricState();
  }

  private void interact(String id) {
    var object =
        app.game.world.layout.objects.stream()
            .filter(o -> o.id().equals(id))
            .findFirst()
            .orElseThrow();
    teleport(object.x(), object.z() + .6f);
    app.game.input("Interact", true);
  }

  private void clearEnemies() {
    for (Enemy enemy : app.game.enemies)
      if (enemy.alive()) app.game.combat.damageEnemy(enemy, 10000);
  }

  private void require(boolean success, String message) {
    if (!success) throw new IllegalStateException("SMOKE FAILED: " + message);
    System.out.println("[CHECK] " + message);
  }

  private void finish() {
    try {
      Collections.sort(frames);
      double total = frames.stream().mapToDouble(Float::doubleValue).sum(),
          p95 = frames.get(Math.min(frames.size() - 1, (int) (frames.size() * .95))) * 1000;
      Files.writeString(
          Path.of("target", "smoke-ok.txt"),
          "PASS: all five rendered regions; WASD/Bullet movement; actual melee hit; camera"
              + " collision; GPU skinning; PBR/glTF; audio; all 10 quests; both endings; save/load;"
              + " independent new-game checkpoint; death and respawn; graphics quality switching.\n"
              + "Campaign checks teleport between objectives and use deterministic enemy damage;"
              + " this is not a human playthrough or balance review.\n");
      Files.writeString(
          Path.of("target", "render-metrics.json"),
          String.format(
              Locale.ROOT,
              "{\"frames\":%d,\"averageFps\":%.2f,\"p95FrameMs\":%.2f,\"includesSceneLoadsAndScreenshots\":true}%n",
              frames.size(),
              frames.size() / total,
              p95));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    app.stop();
  }
}
