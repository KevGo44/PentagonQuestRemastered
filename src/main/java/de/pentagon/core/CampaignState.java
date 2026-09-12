package de.pentagon.core;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.*;
import com.jme3.scene.*;
import de.pentagon.assets.*;
import de.pentagon.combat.*;
import de.pentagon.entities.*;
import de.pentagon.inventory.*;
import de.pentagon.physics.PhysicsWorld;
import de.pentagon.quest.QuestJournal;
import de.pentagon.save.SaveService;
import de.pentagon.world.*;
import java.io.IOException;
import java.util.*;

/** Coordinates independent simulation, view, campaign and persistence services. */
public final class CampaignState extends BaseAppState {
  private GameApplication app;
  public GameSession session = new GameSession();
  public WorldView world;
  public PlayerController player;
  public PhysicsWorld physics;
  public CombatSystem combat;
  public final List<Enemy> enemies = new ArrayList<>();
  public final QuestJournal quests = new QuestJournal();
  public Effects effects;
  public ProjectileSystem projectiles;
  private float time, questTimer, transitionLeft;
  private float deathTime = -1;
  private Region pendingRegion;
  private final Map<String, Float> trapCooldowns = new HashMap<>();
  private Vector3f lastSafe = new Vector3f();

  public record Dialogue(String speaker, String text, List<String> options, String id) {}

  public Dialogue dialogue;
  private GameSession checkpointState;

  @Override
  protected void initialize(Application application) {
    app = (GameApplication) application;
    physics = new PhysicsWorld(getState(BulletAppState.class).getPhysicsSpace());
    effects = new Effects(app.assets);
    projectiles = new ProjectileSystem(app.assets, physics);
    app.getRootNode().attachChild(effects.root);
    app.getRootNode().attachChild(projectiles.root);
    loadRegion(Region.REFUGE, false);
    pause(true);
  }

  public void newGame() {
    session = new GameSession();
    session.yaw = FastMath.PI;
    loadRegion(Region.REFUGE, false);
    session.x = world.layout.spawnX();
    session.z = world.layout.spawnZ();
    checkpointState = app.saves.copy(session);
    app.screen(ScreenMode.PLAYING);
    app.notice("Willkommen im Ödland. Folge dem Feuer zu Mira. [E]");
  }

  public boolean loadGame() {
    try {
      SaveService.Loaded loaded = app.saves.load();
      session = loaded.session();
      checkpointState = app.saves.copy(session);
      loadRegion(session.region, true);
      app.screen(ScreenMode.PLAYING);
      app.notice(
          loaded.recoveredBackup() ? "Sicherungskopie wiederhergestellt." : "Spielstand geladen.");
      return true;
    } catch (IOException ex) {
      app.notice("Laden fehlgeschlagen: " + ex.getMessage());
      return false;
    }
  }

  public void save(boolean automatic) {
    if (session.player.health <= 0) {
      app.notice("Ein besiegter Held kann nicht speichern.");
      return;
    }
    if (combat.inCombat()
        || projectiles.active() > 0
        || !player.body.isOnGround()
        || player.dodgeLeft > 0
        || player.attack.active()) {
      if (!automatic) app.notice("Speichern ist nur sicher und am Boden möglich.");
      return;
    }
    capture();
    try {
      app.saves.save(session);
      checkpointState = app.saves.copy(session);
      if (!automatic) app.notice("Spiel gespeichert.");
    } catch (IOException | IllegalArgumentException e) {
      app.notice("Speichern fehlgeschlagen: " + e.getMessage());
    }
  }

  private void capture() {
    Vector3f at = player.node.getWorldTranslation();
    session.x = at.x;
    session.z = at.z;
    session.yaw = player.yaw;
    for (Enemy e : enemies)
      if (e.alive())
        session.enemies.put(
            e.id, new GameSession.EnemySave(e.health, e.position().x, e.position().z));
  }

  public void transition(Region destination) {
    capture();
    pendingRegion = destination;
    transitionLeft = .55f;
    app.screen(ScreenMode.TRANSITION);
  }

  public void loadRegion(Region region, boolean savedPosition) {
    deathTime = -1;
    pause(true);
    if (player != null) player.cleanup(physics);
    for (Enemy e : enemies) e.cleanup(physics);
    enemies.clear();
    projectiles.clear();
    effects.clear();
    trapCooldowns.clear();
    if (world != null) world.cleanup();
    session.region = region;
    session.flags.add("visit_" + region.name());
    DungeonLayout layout = new DungeonLayout(region);
    world = new WorldView(app.assets, physics, layout, session);
    app.getRootNode().attachChild(world.root);
    player = new PlayerController(app.assets, physics);
    app.getRootNode().attachChild(player.node);
    float x = savedPosition && layout.walkable(session.x, session.z) ? session.x : layout.spawnX(),
        z = savedPosition && layout.walkable(session.x, session.z) ? session.z : layout.spawnZ();
    player.yaw = savedPosition ? session.yaw : FastMath.PI;
    player.warp(x, z);
    player.node.setLocalTranslation(x, .12f, z);
    lastSafe.set(x, .12f, z);
    if (session.checkpointX == 0) {
      session.checkpointX = x;
      session.checkpointZ = z;
    }
    for (var spawn : layout.enemies)
      if (!session.defeated.contains(spawn.id())) {
        Enemy e = new Enemy(spawn, app.assets, physics, session);
        enemies.add(e);
        app.getRootNode().attachChild(e.node);
      }
    combat =
        new CombatSystem(
            player,
            session,
            world,
            enemies,
            effects,
            projectiles,
            app.audio,
            app.atmosphere,
            physics,
            app::notice);
    app.atmosphere.region(region);
    app.getViewPort().setBackgroundColor(AssetPipeline.color(region.fog));
    player.node.updateGeometricState();
    world.root.updateGeometricState();
    player.camera(app.getCamera(), .1f, true, world.occluders, world.interactives);
    app.ui.invalidate();
    pause(app.mode() != ScreenMode.PLAYING);
  }

  public void pause(boolean value) {
    BulletAppState bullet = getState(BulletAppState.class);
    if (bullet != null) bullet.setEnabled(!value);
    if (player != null) {
      player.resetInput();
      player.rig.pause(value);
    }
    for (Enemy enemy : enemies) enemy.rig.pause(value);
    if (world != null)
      world.interactives.depthFirstTraversal(
          s -> {
            var c = s.getControl(com.jme3.anim.AnimComposer.class);
            if (c != null) c.setGlobalSpeed(value ? 0 : 1);
          });
  }

  @Override
  public void update(float rawDt) {
    float dt = Math.min(rawDt, .08f);
    if (app.mode() == ScreenMode.PLAYING) time += dt;
    if (app.mode() == ScreenMode.TRANSITION) {
      transitionLeft -= dt;
      if (transitionLeft <= 0) {
        loadRegion(pendingRegion, false);
        app.screen(ScreenMode.PLAYING);
        app.notice(pendingRegion.title);
      }
      return;
    }
    if (world == null) return;
    world.update(time, player.node.getWorldTranslation());
    if (app.mode() != ScreenMode.PLAYING) {
      if (app.mode() == ScreenMode.MAIN_MENU) {
        Vector3f at = new Vector3f(42, 3.3f, 74);
        app.getCamera().setLocation(at);
        app.getCamera().lookAt(new Vector3f(43, 1.9f, 60), Vector3f.UNIT_Y);
      }
      return;
    }
    session.playSeconds += dt;
    if (session.player.health <= 0) {
      updateDeath(dt);
      return;
    }
    if (player.update(dt, session)) app.audio.play("step");
    combat.update(dt, time);
    effects.update(dt);
    if (session.player.health <= 0) {
      updateDeath(dt);
      return;
    }
    Vector3f p = player.node.getWorldTranslation();
    if (p.y < -5 || p.y > 30) {
      player.warp(lastSafe.x, lastSafe.z);
      app.notice("Zum letzten sicheren Boden zurückgesetzt.");
    } else if (player.body.isOnGround() && world.layout.walkable(p.x, p.z)) lastSafe.set(p);
    player.camera(app.getCamera(), dt, false, world.occluders, world.interactives);
    var visited =
        session.explored.computeIfAbsent(session.region.name(), k -> new LinkedHashSet<>());
    int cx = Math.round(p.x / DungeonLayout.CELL), cz = Math.round(p.z / DungeonLayout.CELL);
    for (int dx = -4; dx <= 4; dx++)
      for (int dz = -4; dz <= 4; dz++) {
        int x = cx + dx, z = cz + dz;
        if (x >= 0
            && z >= 0
            && x < DungeonLayout.SIZE
            && z < DungeonLayout.SIZE
            && dx * dx + dz * dz <= 20) visited.add(x + z * DungeonLayout.SIZE);
      }
    for (var room : world.layout.rooms)
      if (Math.abs(p.x / DungeonLayout.CELL - room.x()) <= room.rx()
          && Math.abs(p.z / DungeonLayout.CELL - room.z()) <= room.rz())
        app.atmosphere.beam(
            new Vector3f(room.x() * DungeonLayout.CELL, 6, room.z() * DungeonLayout.CELL));
    traps(dt, p);
    questTimer -= dt;
    if (questTimer <= 0) {
      quests.refresh(session, app::notice);
      questTimer = .4f;
    }
  }

  private void updateDeath(float dt) {
    if (deathTime < 0) {
      deathTime = 0;
      player.resetInput();
      player.attack.cancel();
      player.rig.restart("Death");
      for (Enemy enemy : enemies) enemy.stop();
    }
    deathTime += dt;
    player.camera(app.getCamera(), dt, false, world.occluders, world.interactives);
    if (deathTime >= 1.1f) app.screen(ScreenMode.GAME_OVER);
  }

  private void traps(float dt, Vector3f position) {
    trapCooldowns.replaceAll((k, v) -> Math.max(0, v - dt));
    for (var spec : world.layout.objects)
      if (spec.kind() == DungeonLayout.Kind.TRAP) {
        float phase = (time + Math.abs(spec.id().hashCode() % 7)) % 3.6f;
        boolean active = phase > 2.6f;
        Node node = world.objects.get(spec.id());
        for (Spatial child : node.getChildren())
          if (child.getName().equals("Spike"))
            child.setLocalTranslation(
                child.getLocalTranslation().x, active ? .5f : -.5f, child.getLocalTranslation().z);
        if (active
            && Math.abs(position.x - spec.x()) < 1.5f
            && Math.abs(position.z - spec.z()) < 1.5f
            && position.y < .9f
            && trapCooldowns.getOrDefault(spec.id(), 0f) <= 0) {
          combat.hurt(28, new Vector3f(spec.x(), 0, spec.z()), true, null);
          trapCooldowns.put(spec.id(), 1.1f);
        }
      }
  }

  public DungeonLayout.ObjectSpec nearby() {
    if (world == null) return null;
    Vector3f p = player.node.getWorldTranslation();
    return world.layout.objects.stream()
        .filter(o -> o.kind() != DungeonLayout.Kind.TRAP)
        .filter(
            o ->
                !session.opened.contains(o.id())
                    || o.kind() == DungeonLayout.Kind.LORE
                    || o.kind() == DungeonLayout.Kind.NPC
                    || o.kind() == DungeonLayout.Kind.PRISONER)
        .filter(o -> Math.hypot(p.x - o.x(), p.z - o.z()) < 3.1)
        .min(Comparator.comparingDouble(o -> Math.hypot(p.x - o.x(), p.z - o.z())))
        .orElse(null);
  }

  public void interact() {
    var o = nearby();
    if (o == null) return;
    if (combat.inCombat() && o.kind() != DungeonLayout.Kind.LORE) {
      app.notice("Zuerst die Umgebung sichern.");
      return;
    }
    switch (o.kind()) {
      case PORTAL -> {
        Region to = Region.valueOf(o.value());
        if (to == Region.PRISON && session.region == Region.REFUGE && !session.sealsReady()) {
          app.notice("Die zwei Siegel fehlen: Krypta und Kristallherz.");
          return;
        }
        if (to == Region.THRONE && session.inventory.count("prison_key") == 0) {
          app.notice("Der Kerkermeister trägt den Schlüssel.");
          return;
        }
        transition(to);
      }
      case SHRINE -> {
        session.player.restore();
        session.checkpoint = session.region.name();
        session.checkpointX = player.node.getWorldTranslation().x;
        session.checkpointZ = player.node.getWorldTranslation().z;
        session.inventory.add("potion", Math.max(0, 3 - session.inventory.count("potion")));
        save(false);
        app.audio.play("chime");
        app.notice("Am Feuer gerastet. Leben und Ausdauer erneuert.");
      }
      case NPC -> {
        session.flags.add("mira_met");
        dialogue =
            new Dialogue(
                "Mira / Letzte Kartografin",
                "Der König hat das Ödland gebrochen. Seine Kraft kommt von unten. Hol das Siegel"
                    + " der Toten aus der Krypta und das Kristallherz aus den Höhlen. Du"
                    + " entscheidest, welchen Weg du zuerst gehst.\n\n"
                    + "Im Verlies nährt jede gefangene Seele die Krone. Denk daran, bevor du ihren"
                    + " Preis bestimmst.",
                List.of(
                    "Ich finde die beiden Siegel.",
                    "Heiltrank kaufen - 20 Gold",
                    "Grenzlaeufer-Klinge kaufen - 65 Gold"),
                "mira");
        app.screen(ScreenMode.DIALOGUE);
      }
      case PRISONER -> {
        if (session.flag("prisoners_freed") || session.flag("prisoners_bargain")) {
          app.notice(
              session.flag("prisoners_freed")
                  ? "Eren: Wir werden die Kraft der Krone brechen."
                  : "Eren schweigt. Die Ketten bleiben.");
          return;
        }
        dialogue =
            new Dialogue(
                "Eren / Gefangener der Krone",
                "Hörst du die Stimmen? Unsere Ketten speisen den König. Öffne die Zellen und wir"
                    + " zerstören die Anker seiner Krone.\n\n"
                    + "Oder nimm die Aschenklinge aus dem versiegelten Arsenal. Dann bleiben die"
                    + " Zellen zu. Beides kannst du nicht haben.",
                List.of(
                    "Die Gefangenen befreien. Die Krone schwächen.",
                    "Die Aschenklinge nehmen. Die Ketten bleiben.",
                    "Noch nicht entscheiden."),
                "eren");
        app.screen(ScreenMode.DIALOGUE);
      }
      case CHEST -> {
        if (!session.inventory.add(o.value(), o.value().equals("greater_potion") ? 3 : 1)) {
          app.notice("Inventar voll.");
          return;
        }
        session.inventory.add("potion", 2);
        session.player.gold += 25;
        session.opened.add(o.id());
        app.notice(ItemCatalog.get(o.value()).name() + " gefunden. +25 Gold, +2 Heiltränke.");
        app.audio.play("chime");
      }
      case LORE -> {
        if (session.opened.add(o.id())) session.event("lore");
        dialogue = new Dialogue(o.label(), o.value(), List.of("Chronik schließen."), "lore");
        app.screen(ScreenMode.DIALOGUE);
      }
      case RUNE -> {
        if (session.flag("crypt_puzzle")) {
          app.notice("Der Eid ist bereits erneuert.");
          return;
        }
        int expected = session.count("rune_step"), actual = Integer.parseInt(o.value());
        if (actual == expected) {
          session.event("rune_step");
          app.notice(o.label() + " antwortet. " + session.count("rune_step") + " / 3");
          app.audio.play("chime");
          if (session.count("rune_step") == 3) {
            session.flags.add("crypt_puzzle");
            app.notice("Der Altar im Norden ist erwacht.");
          }
        } else {
          session.counters.put("rune_step", 0);
          app.notice("Der Eid verstummt. Beginne mit der Sonne.");
          app.audio.play("hurt");
        }
      }
      case CRYSTAL -> {
        session.opened.add(o.id());
        session.event("crystals");
        session.player.gainXp(35);
        app.notice("Kristallader gereinigt. " + session.count("crystals") + " / 3");
        app.audio.play("chime");
      }
      case SEAL -> {
        boolean ready =
            o.value().equals("crypt_seal")
                ? session.flag("crypt_puzzle")
                : session.count("crystals") >= 3;
        if (!ready) {
          app.notice(
              o.value().equals("crypt_seal")
                  ? "Erneuere zuerst den Runeneid."
                  : "Reinige zuerst alle drei Kristalladern.");
          return;
        }
        session.inventory.add(o.value(), 1);
        session.opened.add(o.id());
        app.notice(ItemCatalog.get(o.value()).name() + " erhalten!");
        app.audio.play("chime");
      }
      case THRONE -> {
        if (!session.flag("king_dead")) {
          app.notice("Die Krone ist noch an den König gebunden.");
          return;
        }
        dialogue =
            new Dialogue(
                "Das Aschensiegel",
                "Die fünf Bastionen warten auf dein Urteil. Das Siegel kann niemandem mehr dienen,"
                    + " wenn du es zerbrichst. Bewahrst du es, liegt die Verantwortung für das"
                    + " Ödland bei dir.",
                List.of(
                    "Das Siegel zerbrechen. Das Ödland befreien.",
                    "Die Krone bewahren. Die Wacht übernehmen.",
                    "Noch einmal zurueckkehren."),
                "ending");
        app.screen(ScreenMode.DIALOGUE);
      }
      default -> {}
    }
    world.refreshObject(o, session);
    quests.refresh(session, app::notice);
  }

  public void choose(int index) {
    if (dialogue == null || index < 0 || index >= dialogue.options().size()) return;
    switch (dialogue.id()) {
      case "mira" -> {
        if (index > 0) {
          String id = index == 1 ? "potion" : "short_sword";
          int price = ItemCatalog.get(id).value();
          if (session.player.gold < price) {
            app.notice("Nicht genug Gold.");
            return;
          }
          if (!session.inventory.add(id, 1)) {
            app.notice("Inventar voll.");
            return;
          }
          session.player.gold -= price;
          app.notice(ItemCatalog.get(id).name() + " gekauft.");
          app.ui.invalidate();
          return;
        }
      }
      case "eren" -> {
        if (index == 0) {
          session.flags.add("prisoners_freed");
          session.inventory.add("chain", 1);
          app.notice("Die Gefangenen sind frei. Der König verliert 20% seiner Lebenskraft.");
        } else if (index == 1) {
          session.flags.add("prisoners_bargain");
          session.inventory.add("ember_blade", 1);
          app.notice("Aschenklinge erhalten. Erens Blick folgt dir.");
        }
      }
      case "ending" -> {
        if (index < 2) {
          session.flags.add(index == 0 ? "ending_seal" : "ending_crown");
          quests.refresh(session, app::notice);
          capture();
          try {
            app.saves.save(session);
          } catch (IOException e) {
            app.notice("Epilog konnte nicht gespeichert werden: " + e.getMessage());
          }
          app.screen(ScreenMode.ENDING);
          return;
        }
      }
      default -> {}
    }
    dialogue = null;
    quests.refresh(session, app::notice);
    app.screen(ScreenMode.PLAYING);
  }

  public void useItem(String id) {
    if (session.inventory.count(id) == 0) return;
    Item item = ItemCatalog.get(id);
    switch (item.kind()) {
      case WEAPON, ARMOR -> {
        session.inventory.equip(id);
        app.notice(item.name() + " ausgerüstet.");
      }
      case POTION -> {
        if (session.player.health >= session.player.maxHealth()) {
          app.notice("Leben bereits voll.");
          return;
        }
        session.player.heal(item.power());
        session.inventory.remove(id, 1);
        app.audio.play("chime");
      }
      case TONIC -> {
        session.player.stamina = session.player.maxStamina();
        player.spellCooldown = 0;
        session.inventory.remove(id, 1);
        app.audio.play("chime");
      }
      default -> app.notice(item.description());
    }
    app.ui.invalidate();
  }

  public void potion() {
    String id = session.inventory.count("potion") > 0 ? "potion" : "greater_potion";
    if (session.inventory.count(id) == 0) app.notice("Keine Heiltränke mehr.");
    else useItem(id);
  }

  public void respawn() {
    // Use this campaign's checkpoint, never an unrelated older adventure on disk.
    if (checkpointState != null) {
      session = app.saves.copy(checkpointState);
      loadRegion(session.region, true);
      app.screen(ScreenMode.PLAYING);
      return;
    }
    session.player.gold = Math.max(0, session.player.gold - 20);
    session.player.restore();
    session.x = session.checkpointX;
    session.z = session.checkpointZ;
    loadRegion(Region.valueOf(session.checkpoint), true);
    app.screen(ScreenMode.PLAYING);
  }

  public void input(String name, boolean pressed) {
    if (player == null) return;
    switch (name) {
      case "Forward" -> player.forward = pressed;
      case "Back" -> player.back = pressed;
      case "Left" -> player.left = pressed;
      case "Right" -> player.right = pressed;
      case "Sprint" -> player.sprint = pressed;
      case "Block" -> player.block(pressed);
      default -> {
        if (!pressed) return;
        switch (name) {
          case "Attack" -> {
            if (player.attack(session)) app.audio.play("swing");
          }
          case "Dodge" -> player.dodge(session);
          case "Jump" -> player.jump(session);
          case "Spell" -> {
            if (player.cast(session)) combat.castPlayer();
          }
          case "Interact" -> interact();
          case "Potion" -> potion();
          case "Save" -> save(false);
          default -> {}
        }
      }
    }
  }

  @Override
  protected void cleanup(Application app) {
    if (player != null) player.cleanup(physics);
    for (Enemy e : enemies) e.cleanup(physics);
    projectiles.clear();
    world.cleanup();
  }

  @Override
  protected void onEnable() {}

  @Override
  protected void onDisable() {
    pause(true);
  }
}
