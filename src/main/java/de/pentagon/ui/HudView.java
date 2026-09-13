package de.pentagon.ui;

import com.jme3.font.*;
import com.jme3.font.Rectangle;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import de.pentagon.assets.AssetPipeline;
import de.pentagon.core.*;
import de.pentagon.entities.*;
import de.pentagon.inventory.*;
import de.pentagon.world.*;
import java.util.*;
import java.util.function.*;

/**
 * Retained HUD with screen-sized menu layouts. Input hit rectangles use the same virtual canvas.
 */
public final class HudView {
  private static final float W = 1440, H = 900;
  private static final int PAPER = 0xe8e4d8,
      MUTED = 0x9ba9ad,
      GOLD = 0xc6a570,
      TEAL = 0x77bbbc,
      DARK = 0x0c151c;
  private final GameApplication app;
  private final AssetPipeline assets;
  private final BitmapFont body, title;
  private final Node root = new Node("Interface"),
      hud = new Node("HUD"),
      page = new Node("Page"),
      toastRoot = new Node("Toasts");
  private static final float SLIDER_HIT = 42;
  private final List<Button> buttons = new ArrayList<>();
  private final List<Slider> sliders = new ArrayList<>();
  private final Map<String, BitmapText> texts = new HashMap<>();
  private final List<Toast> toasts = new ArrayList<>();
  private Slider dragging;

  private record Button(float x, float y, float w, float h, Runnable action) {}

  private record Slider(float x, float y, float w, DoubleSupplier value, DoubleConsumer setter) {}

  private record Toast(String message, double expires) {}

  private Geometry health, stamina, xp, bossHealth;
  private Node miniMap = new Node("MiniMap");
  private String selectedItem = "rust_sword";
  private ScreenMode shown;
  private boolean dirty = true, toastsDirty = true;
  private float scale = 1, offsetX, offsetY, miniTimer;
  private int width, height;
  private double elapsed;

  public HudView(GameApplication app) {
    this.app = app;
    assets = app.assets;
    body = assets.manager().loadFont("fonts/body.fnt");
    title = assets.manager().loadFont("fonts/title.fnt");
    root.attachChild(hud);
    root.attachChild(page);
    root.attachChild(toastRoot);
    app.getGuiNode().attachChild(root);
    buildHud();
  }

  public void invalidate() {
    dirty = true;
  }

  public void notice(String message) {
    if (toasts.size() >= 3) toasts.remove(0);
    toasts.add(new Toast(message, elapsed + 6));
    toastsDirty = true;
  }

  private Geometry rectangle(
      Node n, float x, float y, float w, float h, int color, float alpha, float depth) {
    Geometry g = new Geometry("Panel", new Quad(w, h));
    g.setMaterial(assets.flat(color, alpha));
    g.setLocalTranslation(x, H - y - h, depth);
    n.attachChild(g);
    return g;
  }

  private BitmapText text(
      Node n, String value, float x, float y, float size, int color, boolean serif) {
    BitmapText t = new BitmapText(serif ? title : body);
    t.setText(value);
    t.setSize(size);
    t.setColor(AssetPipeline.color(color));
    t.setLocalTranslation(x, H - y, 5);
    n.attachChild(t);
    return t;
  }

  private BitmapText wrapped(
      Node n, String value, float x, float y, float w, float h, float size, int color) {
    BitmapText t = text(n, value, x, y, size, color, false);
    t.setBox(new Rectangle(0, 0, w, h));
    t.setLineWrapMode(LineWrapMode.Word);
    return t;
  }

  private void label(String id, String value, float x, float y, float size, int color) {
    texts.put(id, text(hud, value, x, y, size, color, false));
  }

  private void buildHud() {
    rectangle(hud, 28, 27, 350, 113, DARK, .8f, 0);
    rectangle(hud, 28, 27, 3, 113, GOLD, .95f, 1);
    label("level", "01", 48, 49, 31, GOLD);
    label("name", "WANDERER DES ÖDLANDS", 103, 45, 12, PAPER);
    label("hp", "140 / 140", 103, 67, 15, PAPER);
    rectangle(hud, 103, 87, 245, 7, 0x26343a, 1, 1);
    health = rectangle(hud, 103, 87, 245, 7, 0xb96555, 1, 2);
    rectangle(hud, 103, 103, 245, 5, 0x26343a, 1, 1);
    stamina = rectangle(hud, 103, 103, 245, 5, 0x73a999, 1, 2);
    rectangle(hud, 48, 124, 300, 2, 0x26343a, 1, 1);
    xp = rectangle(hud, 48, 124, 300, 2, GOLD, 1, 2);
    label("region", "DIE LETZTE ZUFLUCHT", 530, 34, 19, PAPER);
    label("room", "Halle der Reisenden", 560, 65, 13, MUTED);
    rectangle(hud, 1070, 27, 342, 107, DARK, .8f, 0);
    label("questTitle", "DAS ASCHENSIEGEL", 1088, 46, 12, GOLD);
    BitmapText objective = wrapped(hud, "", 1088, 72, 303, 63, 14, PAPER);
    texts.put("objective", objective);
    rectangle(hud, 560, 809, 320, 37, DARK, .78f, 0);
    label("interact", "", 577, 817, 15, PAPER);
    label(
        "controls",
        "WASD  Bewegen    MAUS  Blick    LMB  Kombo    RMB  Parieren    Q  Magie    LEER  Sprung   "
            + " ALT  Rolle",
        32,
        867,
        12,
        MUTED);
    label(
        "shortcuts",
        "I  Inventar     J  Aufträge     K  Skills     M  Karte     ESC  Pause",
        870,
        867,
        11,
        MUTED);
    label("quick", "R   HEILTRANK   5      Q   ÄTHER  BEREIT", 36, 811, 13, PAPER);
    label("enemy", "", 485, 149, 16, PAPER);
    rectangle(hud, 485, 174, 470, 5, 0x293135, .5f, 0);
    bossHealth = rectangle(hud, 485, 174, 470, 5, 0xc57658, 1, 2);
    label("crosshair", "+", 716, 444, 14, 0xbbc7c5);
    hud.attachChild(miniMap);
  }

  public void update(float dt) {
    elapsed += dt;
    resize();
    CampaignState game = app.game;
    if (game == null || game.player == null) return;
    ScreenMode mode = app.mode();
    // A drag must never survive the page it belongs to, or the mouse would keep moving a
    // slider that is no longer on screen.
    if (mode != shown) dragging = null;
    if (dragging != null) drag(app.getInputManager().getCursorPosition().x);
    if (dirty || mode != shown) {
      shown = mode;
      dirty = false;
      buildPage(mode);
      toastsDirty = true;
    }
    hud.setCullHint(mode == ScreenMode.PLAYING ? Spatial.CullHint.Never : Spatial.CullHint.Always);
    if (mode == ScreenMode.EPILOGUE && texts.containsKey("subtitle"))
      texts.get("subtitle").setText(game.epilogueLine());
    var s = game.session;
    health.setLocalScale(Math.max(.001f, s.player.health / s.player.maxHealth()), 1, 1);
    stamina.setLocalScale(Math.max(.001f, s.player.stamina / s.player.maxStamina()), 1, 1);
    xp.setLocalScale(Math.max(.001f, s.player.xp / (float) s.player.xpNeeded()), 1, 1);
    texts.get("level").setText(String.format(Locale.ROOT, "%02d", s.player.level));
    texts.get("hp").setText(Math.round(s.player.health) + " / " + s.player.maxHealth());
    texts.get("region").setText(s.region.title.toUpperCase(Locale.GERMAN));
    texts
        .get("room")
        .setText(
            game.world.layout.roomAt(
                game.player.node.getWorldTranslation().x,
                game.player.node.getWorldTranslation().z));
    texts.get("objective").setText(game.quests.objective(s));
    texts
        .get("quick")
        .setText(
            "R   HEILTRANK   "
                + (s.inventory.count("potion") + s.inventory.count("greater_potion"))
                + "      Q   "
                + (game.player.spellCooldown > 0
                    ? String.format(Locale.ROOT, "%.1f s", game.player.spellCooldown)
                    : "ÄTHER BEREIT"));
    var object = game.nearby();
    texts.get("interact").setText(object == null ? "" : "[E]  " + object.label());
    Enemy enemy = game.combat.nearest();
    texts
        .get("enemy")
        .setText(
            enemy == null
                ? ""
                : enemy.type.title
                    + (enemy.type == de.pentagon.ai.EnemyType.KING
                        ? "  /  PHASE " + enemy.phase
                        : "")
                    + "   "
                    + Math.round(enemy.health)
                    + " / "
                    + Math.round(enemy.maxHealth));
    bossHealth.setCullHint(enemy == null ? Spatial.CullHint.Always : Spatial.CullHint.Never);
    if (enemy != null)
      bossHealth.setLocalScale(Math.max(.001f, enemy.health / enemy.maxHealth), 1, 1);
    miniTimer -= dt;
    if (mode == ScreenMode.PLAYING && miniTimer <= 0) {
      miniTimer = .3f;
      miniMap.detachAllChildren();
      drawMap(miniMap, 1190, 613, 210, false);
    }
    int before = toasts.size();
    // The closing sequence keeps its letterbox clean: notices wait for the Ending page.
    if (mode == ScreenMode.EPILOGUE) toasts.clear();
    toasts.removeIf(t -> t.expires < elapsed);
    if (before != toasts.size()) toastsDirty = true;
    if (toastsDirty) {
      toastsDirty = false;
      toastRoot.detachAllChildren();
      boolean playing = mode == ScreenMode.PLAYING;
      // Menus reserve a footer for the newest notice, clear of all click targets.
      int first = playing ? 0 : Math.max(0, toasts.size() - 1);
      float x = playing ? 35 : 78, y = playing ? 692 : 852;
      for (int i = first; i < toasts.size(); i++) {
        rectangle(toastRoot, x, y, 660, 28, DARK, .8f, 10);
        text(toastRoot, toasts.get(i).message, x + 12, y + 5, 13, PAPER, false)
            .setLocalTranslation(x + 12, H - y - 5, 12);
        y += 31;
      }
    }
  }

  private void resize() {
    int w = app.getCamera().getWidth(), h = app.getCamera().getHeight();
    if (w == width && h == height) return;
    width = w;
    height = h;
    scale = Math.min(w / W, h / H);
    offsetX = (w - W * scale) / 2;
    offsetY = (h - H * scale) / 2;
    root.setLocalScale(scale);
    root.setLocalTranslation(offsetX, offsetY, 0);
    dirty = true;
  }

  private void button(String label, float x, float y, float w, Runnable action) {
    rectangle(page, x, y, w, 43, 0x1c2d35, .95f, 2);
    rectangle(page, x, y, 2, 43, GOLD, .9f, 3);
    text(page, label, x + 16, y + 11, 16, PAPER, false);
    buttons.add(new Button(x, y, w, 43, action));
  }

  /** A labelled track in the same palette as the HUD bars; drag or click anywhere on it. */
  private void slider(
      String label, float x, float y, float w, DoubleSupplier value, DoubleConsumer setter) {
    float level = (float) Math.max(0, Math.min(1, value.getAsDouble()));
    text(page, label, x, y, 15, PAPER, false);
    BitmapText readout = text(page, Math.round(level * 100) + " %", x, y, 15, GOLD, false);
    readout.setLocalTranslation(x + w - readout.getLineWidth(), H - y, 5);
    float track = y + 27;
    rectangle(page, x, track, w, 8, 0x26343a, 1, 2);
    // A zero-width Quad is degenerate geometry; keep a hairline of fill instead.
    rectangle(page, x, track, Math.max(.6f, w * level), 8, GOLD, .95f, 3);
    rectangle(page, x + w * level - 3, track - 6, 6, 20, PAPER, 1, 4);
    sliders.add(new Slider(x, y, w, value, setter));
  }

  private void pageTitle(String eyebrow, String heading, String subtitle) {
    pageTitle(eyebrow, heading, subtitle, () -> app.screen(ScreenMode.PLAYING));
  }

  private void pageTitle(String eyebrow, String heading, String subtitle, Runnable back) {
    rectangle(page, 0, 0, W, H, 0x071015, .90f, 1);
    text(page, eyebrow, 78, 62, 12, GOLD, false);
    text(page, heading, 74, 99, 51, PAPER, true);
    text(page, subtitle, 78, 174, 15, MUTED, false);
    rectangle(page, 78, 210, 1284, 1, 0x4b575b, .7f, 2);
    button("Zurück  [ESC]", 1134, 61, 226, back);
  }

  private void buildPage(ScreenMode mode) {
    page.detachAllChildren();
    buttons.clear();
    sliders.clear();
    var game = app.game;
    if (game == null || game.player == null) return;
    var s = game.session;
    switch (mode) {
      case MAIN_MENU -> {
        rectangle(page, 0, 0, 630, H, 0x08131a, .92f, 1);
        rectangle(page, 630, 0, 3, H, GOLD, .4f, 2);
        text(page, "EIN ABENTEUER IM ÖDLAND", 78, 116, 13, GOLD, false);
        text(page, "PENTAGON", 72, 173, 75, PAPER, true);
        text(page, "DAS ASCHENSIEGEL", 79, 276, 24, PAPER, false);
        rectangle(page, 80, 335, 66, 2, GOLD, 1, 2);
        wrapped(
            page,
            "Unter fünf zerbrochenen Bastionen\nwartet ein König, der nicht sterben will.",
            80,
            372,
            460,
            100,
            19,
            MUTED);
        float entry = 507;
        button("Neues Abenteuer", 80, entry, 384, game::newGame);
        entry += 58;
        if (app.saves.exists()) {
          button("Reise fortsetzen", 80, entry, 384, game::loadGame);
          entry += 58;
        }
        button("Einstellungen", 80, entry, 384, app::openSettings);
        button("Spiel verlassen", 80, entry + 58, 384, app::stop);
        text(
            page,
            "WASD  Bewegung   /   Maus  Blick   /   E  Interaktion",
            80,
            756,
            12,
            MUTED,
            false);
        text(page, "Fünf Gebiete. Zwei Wege. Ein Urteil.", 80, 830, 12, GOLD, false);
        text(page, "JAVA  /  jMONKEYENGINE", 1155, 858, 11, MUTED, false);
      }
      case PAUSED -> {
        pageTitle("DIE ZEIT STEHT STILL", "Am Rand des Feuers", "Deine Reise wartet.");
        button("Weiterreisen", 78, 255, 410, () -> app.screen(ScreenMode.PLAYING));
        button("Spiel speichern  [F5]", 78, 313, 410, () -> game.save(false));
        button("Letzten Spielstand laden  [F9]", 78, 371, 410, game::loadGame);
        button("Einstellungen", 78, 429, 410, app::openSettings);
        button("Zum Hauptmenü", 78, 487, 410, () -> app.screen(ScreenMode.MAIN_MENU));
        button("Spiel verlassen", 78, 545, 410, app::stop);
        text(page, "STEUERUNG", 620, 257, 13, GOLD, false);
        wrapped(
            page,
            "WASD   Bewegung\n"
                + "Maus   Kamera\n"
                + "Shift   Sprinten\n"
                + "Leertaste   Springen\n"
                + "Alt   Ausweichrolle mit Unverwundbarkeitsfenster\n"
                + "Linke Maustaste   Dreierkombo (Folgehieb vormerken)\n"
                + "Rechte Maustaste   Blocken / kurz vor Treffer parieren\n"
                + "Q   Äthergeschoss\n"
                + "R   Heiltrank\n"
                + "E   Interagieren / Schreine / Gebietswechsel\n"
                + "I / J / K / M   Inventar / Aufträge / Skills / Karte\n"
                + "F5 / F9   Speichern / Laden\n"
                + "F3   Grafikqualität    F10   Ton an/aus    F12   Screenshot",
            620,
            295,
            675,
            445,
            17,
            PAPER);
        button(
            "Grafik: " + (app.highQuality() ? "Atmosphärisch" : "Schnell") + " [F3]",
            78,
            650,
            410,
            app::toggleQuality);
        button(
            "Ton: " + (app.audio.muted() ? "Aus" : "An") + " [F10]",
            78,
            708,
            410,
            () -> {
              app.audio.toggleMute();
              invalidate();
            });
      }
      case SETTINGS -> {
        pageTitle(
            "KLANG & DARSTELLUNG",
            "Einstellungen",
            "Regler ziehen oder anklicken. Änderungen wirken sofort.",
            app::closeSettings);
        text(page, "LAUTSTÄRKE", 78, 258, 13, GOLD, false);
        slider("Gesamt", 78, 296, 470, app.audio::master, v -> app.audio.master((float) v));
        slider("Musik", 78, 372, 470, app.audio::music, v -> app.audio.music((float) v));
        slider("Effekte", 78, 448, 470, app.audio::effects, v -> app.audio.effects((float) v));
        button(
            "Ton: " + (app.audio.muted() ? "Aus" : "An") + "   [F10]",
            78,
            528,
            470,
            () -> {
              app.audio.toggleMute();
              invalidate();
            });
        button(
            "Erkundungsmusik: " + (app.audio.radio() ? "Pentagon Radio" : "Synthetischer Stem"),
            78,
            586,
            470,
            () -> {
              app.audio.toggleRadio();
              invalidate();
            });
        text(page, "DARSTELLUNG", 700, 258, 13, GOLD, false);
        button(
            "Grafik: " + (app.highQuality() ? "Atmosphärisch" : "Schnell") + "   [F3]",
            700,
            296,
            470,
            app::toggleQuality);
        wrapped(
            page,
            "Gesamt regelt alles. Musik betrifft Erkundung, Kampf und Ambient, Effekte die"
                + " Kampf- und Schrittgeräusche.\n\n"
                + "Pentagon Radio ist der durchgehende Titel. Schaltest du ihn ab, übernimmt"
                + " wieder der synthetische Erkundungs-Stem. Die Kampfmusik blendet in beiden"
                + " Fällen darüber, sobald ein Gegner angreift.\n\n"
                + "Ton aus schaltet stumm, ohne deine Regler zu verändern.",
            700,
            372,
            580,
            310,
            17,
            MUTED);
        text(
            page,
            "Gespeichert in settings.json neben deinen Spielständen.",
            78,
            700,
            13,
            MUTED,
            false);
      }
      case INVENTORY -> {
        pageTitle(
            "AUSRÜSTUNG & VORRÄTE",
            "Dein Reisegepäck",
            s.player.gold
                + " Gold   /   "
                + s.inventory.slots()
                + " von "
                + Inventory.CAPACITY
                + " Plätzen belegt");
        int i = 0;
        for (var e : s.inventory.stacks().entrySet()) {
          Item item = ItemCatalog.get(e.getKey());
          int column = i / 10, row = i % 10;
          float x = 78 + column * 348, y = 239 + row * 51;
          String id = e.getKey();
          button(
              (s.inventory.equipped(id) ? "* " : "")
                  + item.name()
                  + (e.getValue() > 1 ? " x" + e.getValue() : ""),
              x,
              y,
              330,
              () -> {
                selectedItem = id;
                invalidate();
              });
          i++;
        }
        Item selected = ItemCatalog.get(selectedItem);
        rectangle(page, 827, 239, 533, 463, 0x16232a, 1, 2);
        text(page, selected.rarity().name(), 854, 267, 12, GOLD, false);
        text(page, selected.name(), 854, 309, 28, PAPER, true);
        wrapped(page, selected.description(), 854, 364, 468, 110, 18, MUTED);
        text(page, "Wirkung / Attribut: " + selected.power(), 854, 490, 18, TEAL, false);
        button(
            selected.kind() == Item.Kind.WEAPON || selected.kind() == Item.Kind.ARMOR
                ? "Ausrüsten"
                : "Benutzen / Ansehen",
            854,
            544,
            380,
            () -> game.useItem(selectedItem));
        if (selected.kind() != Item.Kind.KEY && selected.kind() != Item.Kind.RELIC)
          button("Ablegen", 854, 597, 380, () -> game.dropItem(selectedItem));
        text(
            page,
            "ANGRIFF  "
                + s.player.damage(s.inventory)
                + "      RÜSTUNG  "
                + s.inventory.armor().power(),
            854,
            649,
            16,
            PAPER,
            false);
        wrapped(
            page,
            "Schnellzugriff: R verwendet zuerst kleine, dann große Heiltränke. Abgelegtes bleibt"
                + " am Boden liegen und lässt sich mit E wieder aufheben. Schlüsselitems bleiben"
                + " dauerhaft im Gepäck.",
            78,
            788,
            970,
            55,
            14,
            MUTED);
      }
      case JOURNAL -> {
        pageTitle(
            "DEINE GESCHICHTE",
            "Aufträge & Chronik",
            "Erkunde beide Siegelpfade in beliebiger Reihenfolge.");
        int i = 0;
        for (var q : game.quests.visible(s)) {
          int col = i % 2, row = i / 2;
          float x = 78 + col * 650, y = 238 + row * 115;
          boolean done = s.rewardedQuests.contains(q.id());
          rectangle(page, x, y, 620, 101, done ? 0x132422 : 0x15232b, 1, 2);
          text(
              page,
              (done ? "ERFÜLLT / " : "") + q.title(),
              x + 16,
              y + 14,
              19,
              done ? TEAL : PAPER,
              false);
          wrapped(page, q.description(), x + 16, y + 42, 580, 41, 13, MUTED);
          text(page, q.progress().apply(s), x + 16, y + 78, 12, GOLD, false);
          i++;
        }
      }
      case SKILLS -> {
        pageTitle(
            "WACHSE AN DEINER REISE",
            "Fähigkeiten",
            "Stufe "
                + s.player.level
                + "   /   "
                + s.player.xp
                + " von "
                + s.player.xpNeeded()
                + " EP   /   "
                + s.player.skillPoints
                + " freie Punkte");
        int i = 0;
        for (var skill : PlayerStats.Skill.values()) {
          float x = 78 + i * 325;
          rectangle(page, x, 270, 302, 390, 0x15252c, 1, 2);
          text(page, "0" + (i + 1), x + 22, 295, 14, GOLD, false);
          text(page, skill.title, x + 22, 348, 26, PAPER, true);
          wrapped(page, skill.description, x + 22, 403, 255, 80, 17, MUTED);
          text(page, "RANG " + s.player.rank(skill) + " / 5", x + 22, 507, 16, TEAL, false);
          button(
              "Verbessern",
              x + 22,
              574,
              256,
              () -> {
                if (s.player.unlock(skill)) {
                  app.audio.play("chime");
                  invalidate();
                } else app.notice("Kein Punkt verfügbar oder maximaler Rang erreicht.");
              });
          i++;
        }
        text(
            page,
            "Jeder Stufenaufstieg bringt einen Punkt. Leben und Grundschaden wachsen zusätzlich"
                + " mit der Stufe.",
            78,
            728,
            17,
            MUTED,
            false);
      }
      case MAP -> {
        pageTitle(
            "KARTOGRAFIE DER FÜNF",
            "Karte der Bastion",
            s.region.title
                + "  /  "
                + game.world.layout.roomAt(
                    game.player.node.getWorldTranslation().x,
                    game.player.node.getWorldTranslation().z));
        drawMap(page, 85, 242, 566, true);
        text(page, "DIE WEGE DURCH DAS ÖDLAND", 750, 257, 13, GOLD, false);
        wrapped(
            page,
            "DIE LETZTE ZUFLUCHT\n"
                + "   |-- Krypta der Eide: Runen & Totensiegel\n"
                + "   |-- Gläserne Tiefe: Kristalladern & Herz\n"
                + "   |-- Kettenverlies: benötigt beide Siegel\n"
                + "         |-- Aschenthron: Schlüssel des Wächters\n\n"
                + "Weiß  /  Deine Position\n"
                + "Gold   /  Interaktion und Wege\n"
                + "Türkis / Rastfeuer\n"
                + "Rot    / Gegner in deiner Nähe\n\n"
                + "Neue Wege werden beim Erkunden aufgedeckt.\n"
                + "Schreine speichern und füllen deine Kräfte auf.",
            750,
            305,
            580,
            500,
            18,
            PAPER);
      }
      case DIALOGUE -> {
        var d = game.dialogue;
        if (d == null) break;
        rectangle(page, 0, 0, W, H, 0x071015, .7f, 1);
        rectangle(page, 210, 190, 1020, 535, DARK, .97f, 2);
        rectangle(page, 210, 190, 3, 535, GOLD, 1, 3);
        text(page, d.speaker(), 254, 233, 30, PAPER, true);
        wrapped(page, d.text(), 254, 298, 918, 235, 20, PAPER);
        int i = 0;
        for (String option : d.options()) {
          final int index = i;
          button((i + 1) + "   " + option, 254, 540 + i * 53, 928, () -> game.choose(index));
          i++;
        }
      }
      case GAME_OVER -> {
        pageTitle(
            "DAS FEUER ERLISCHT", "Gefallen im Ödland", "Deine Geschichte ist noch nicht zu Ende.");
        button("Am gespeicherten Feuer erwachen", 78, 285, 520, game::respawn);
        button("Zum Hauptmenü", 78, 348, 520, () -> app.screen(ScreenMode.MAIN_MENU));
        wrapped(
            page,
            "Pariere kurz vor dem Treffer: der Gegner taumelt, und dein nächster Hieb ist eine"
                + " Riposte. Halte Ausdauer für eine Rolle bereit. Die orangefarbene Fläche verrät"
                + " einen Angriff, ein Klicken im Gang eine Falle. Der Aschenwelle des Königs"
                + " kannst du auch mit einem Sprung entgehen.",
            78,
            458,
            700,
            160,
            21,
            MUTED);
      }
      case ENDING -> {
        pageTitle(
            "EPILOG / DAS ASCHENSIEGEL",
            s.flag("ending_seal") ? "Ein Ödland ohne Krone" : "Die neue Wacht",
            "Dein Urteil hat die fünf Bastionen verändert.");
        wrapped(
            page,
            s.flag("ending_seal")
                ? "Das Siegel zerbricht. Zum ersten Mal seit einer Generation gehört die Stille"
                    + " unter den Bastionen niemandem. Mira zeichnet eine Karte, auf der keine"
                    + " Grenzen mehr brennen."
                : "Du bewahrst das Siegel. Die Krone bleibt schwer, auch ohne König. Mira zeichnet"
                    + " keine Grenzen auf ihre neue Karte. Sie lässt Platz für die"
                    + " Entscheidungen, die du noch treffen musst.",
            78,
            278,
            1040,
            170,
            27,
            PAPER);
        text(
            page,
            s.flag("prisoners_freed")
                ? "Eren und die Befreiten tragen das Licht zurück ins Ödland."
                : "Unter den Bastionen warten die Gefangenen noch immer.",
            78,
            504,
            21,
            TEAL,
            false);
        text(
            page,
            "Stufe "
                + s.player.level
                + "   /   "
                + s.defeated.size()
                + " Gegner besiegt   /   "
                + s.rewardedQuests.size()
                + " Aufträge abgeschlossen",
            78,
            579,
            17,
            MUTED,
            false);
        // The story is told; from here the game only leads out. The campaign is saved at the
        // throne, so "Reise fortsetzen" on the title page still opens the hall.
        button("Zum Hauptmenü", 78, 672, 495, () -> app.screen(ScreenMode.MAIN_MENU));
        button("Spiel verlassen", 78, 732, 495, app::stop);
      }
      case EPILOGUE -> {
        // Letterbox and a subtitle line; the picture underneath is the throne hall.
        rectangle(page, 0, 0, W, 96, 0x000000, 1, 2);
        rectangle(page, 0, H - 132, W, 132, 0x000000, 1, 2);
        text(page, "EPILOG", 78, 36, 12, GOLD, false);
        text(page, "ENTER  überspringen", 1210, 36, 12, MUTED, false);
        BitmapText subtitle = wrapped(page, "", 170, H - 104, 1100, 80, 22, PAPER);
        subtitle.setAlignment(BitmapFont.Align.Center);
        texts.put("subtitle", subtitle);
      }
      case TRANSITION -> {
        rectangle(page, 0, 0, W, H, 0x081119, .98f, 2);
        text(page, "JENSEITS DER SCHWELLE", 486, 407, 34, PAPER, true);
        text(page, "Die nächste Bastion erwacht ...", 563, 470, 16, GOLD, false);
      }
      default -> {}
    }
  }

  private void drawMap(Node parent, float x, float y, float size, boolean full) {
    var game = app.game;
    var s = game.session;
    var layout = game.world.layout;
    float cell = size / DungeonLayout.SIZE;
    rectangle(parent, x - 9, y - 9, size + 18, size + 18, DARK, .92f, 2);
    Set<Integer> explored = s.explored.getOrDefault(s.region.name(), Set.of());
    for (int gx = 0; gx < DungeonLayout.SIZE; gx++)
      for (int gz = 0; gz < DungeonLayout.SIZE; gz++)
        if (explored.contains(gx + gz * DungeonLayout.SIZE) && layout.walkable(gx, gz))
          rectangle(parent, x + gx * cell, y + gz * cell, cell - .4f, cell - .4f, 0x425960, .9f, 3);
    for (var o : layout.objects) {
      int gx = Math.round(o.x() / DungeonLayout.CELL), gz = Math.round(o.z() / DungeonLayout.CELL);
      if (explored.contains(gx + gz * DungeonLayout.SIZE) && o.kind() != DungeonLayout.Kind.TRAP)
        rectangle(
            parent,
            x + gx * cell,
            y + gz * cell,
            Math.max(3, cell * .7f),
            Math.max(3, cell * .7f),
            o.kind() == DungeonLayout.Kind.SHRINE ? TEAL : GOLD,
            1,
            4);
    }
    for (Enemy e : game.enemies)
      if (e.alive()
          && e.position().distanceSquared(game.player.node.getWorldTranslation()) < 13 * 13) {
        float gx = e.position().x / DungeonLayout.CELL, gz = e.position().z / DungeonLayout.CELL;
        rectangle(
            parent,
            x + gx * cell,
            y + gz * cell,
            Math.max(3, cell * .5f),
            Math.max(3, cell * .5f),
            0xce6a53,
            1,
            4);
      }
    Vector3f p = game.player.node.getWorldTranslation();
    rectangle(
        parent,
        x + p.x / DungeonLayout.CELL * cell - 2,
        y + p.z / DungeonLayout.CELL * cell - 2,
        full ? 8 : 5,
        full ? 8 : 5,
        PAPER,
        1,
        5);
  }

  public void click(float screenX, float screenY) {
    float x = (screenX - offsetX) / scale, y = H - (screenY - offsetY) / scale;
    // Sliders claim the press first so a drag can start anywhere on the track.
    for (Slider s : List.copyOf(sliders))
      if (x >= s.x() - 10 && x <= s.x() + s.w() + 10 && y >= s.y() && y <= s.y() + SLIDER_HIT) {
        dragging = s;
        drag(screenX);
        return;
      }
    for (Button b : List.copyOf(buttons))
      if (x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h) {
        b.action.run();
        return;
      }
  }

  public void release() {
    dragging = null;
  }

  private void drag(float screenX) {
    Slider s = dragging;
    if (s == null) return;
    float value = Math.max(0, Math.min(1, ((screenX - offsetX) / scale - s.x()) / s.w()));
    // Rebuilding the page on every frame of a still mouse would be pure waste.
    if (Math.abs(value - s.value().getAsDouble()) < .002f) return;
    s.setter().accept(value);
    invalidate();
  }

  public void cleanup() {
    root.removeFromParent();
  }
}
