package de.pentagon.core;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.post.*;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.shadow.*;
import de.pentagon.assets.AssetPipeline;
import de.pentagon.assets.EnvironmentLighting;
import de.pentagon.audio.AudioDirector;
import de.pentagon.save.SaveService;
import de.pentagon.save.SettingsService;
import de.pentagon.ui.HudView;
import de.pentagon.world.AtmosphereFilter;
import java.nio.file.*;

public final class GameApplication extends SimpleApplication
    implements ActionListener, AnalogListener {
  public AssetPipeline assets;
  public AudioDirector audio;
  public SaveService saves;
  public HudView ui;
  public CampaignState game;
  public AtmosphereFilter atmosphere;
  private SettingsService preferences;
  private SettingsService.Settings settings = new SettingsService.Settings();
  private ScreenMode mode = ScreenMode.MAIN_MENU;
  private ScreenMode settingsReturn = ScreenMode.PAUSED;
  private final Path saveDirectory;
  private final boolean smoke, noAudio;
  private boolean highQuality;
  private SSAOFilter ssao;
  private BloomFilter bloom;
  private DirectionalLightShadowRenderer shadows;
  private ScreenshotAppState screenshots;
  private SmokeScenario smokeScenario;

  public GameApplication(Path saveDirectory, boolean smoke, boolean noAudio, boolean fast) {
    super(new BulletAppState());
    this.saveDirectory = saveDirectory;
    this.smoke = smoke;
    this.noAudio = noAudio;
    this.highQuality = !fast;
  }

  @Override
  public void simpleInitApp() {
    setDisplayFps(false);
    setDisplayStatView(false);
    if (flyCam != null) flyCam.setEnabled(false);
    if (inputManager.hasMapping(INPUT_MAPPING_EXIT)) inputManager.deleteMapping(INPUT_MAPPING_EXIT);
    assets = new AssetPipeline(assetManager);
    saves = new SaveService(saveDirectory);
    audio = new AudioDirector(assetManager, !noAudio);
    preferences = new SettingsService(saveDirectory);
    settings = preferences.load();
    audio.master(settings.master);
    audio.music(settings.music);
    audio.effects(settings.effects);
    audio.radio(settings.radio);
    audio.muted(settings.muted);
    cameraSetup();
    lighting();
    ui = new HudView(this);
    game = new CampaignState();
    stateManager.attach(game);
    inputs();
    inputManager.setCursorVisible(true);
    Path shots =
        smoke
            ? Path.of("target", "screenshots").toAbsolutePath()
            : saveDirectory.getParent().resolve("screenshots");
    try {
      Files.createDirectories(shots);
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
    screenshots =
        new ScreenshotAppState(
            shots.toString() + System.getProperty("file.separator"), "pentagon-");
    stateManager.attach(screenshots);
    if (smoke) smokeScenario = new SmokeScenario(this, screenshots);
  }

  private void cameraSetup() {
    cam.setFrustumPerspective(58, cam.getWidth() / (float) cam.getHeight(), .12f, 160);
  }

  private void lighting() {
    DirectionalLight moon =
        new DirectionalLight(
            new Vector3f(-.45f, -1, -.25f).normalizeLocal(),
            new ColorRGBA(.58f, .73f, .87f, 1).mult(.43f));
    rootNode.addLight(moon);
    DirectionalLight fill =
        new DirectionalLight(
            new Vector3f(.8f, -.4f, .5f).normalizeLocal(), new ColorRGBA(.12f, .16f, .21f, 1));
    rootNode.addLight(fill);
    rootNode.addLight(EnvironmentLighting.dungeonProbe());
    rootNode.addLight(new AmbientLight(new ColorRGBA(.3f, .36f, .42f, 1)));
    shadows = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
    shadows.setLight(moon);
    shadows.setLambda(.65f);
    shadows.setShadowIntensity(.45f);
    shadows.setShadowCompareMode(CompareMode.Software);
    shadows.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
    shadows.setShadowZExtend(65);
    shadows.setShadowZFadeLength(15);
    if (highQuality) viewPort.getProcessors().add(0, shadows);
    FilterPostProcessor filters = new FilterPostProcessor(assetManager);
    ssao = new SSAOFilter(1.5f, 2.5f, .18f, .16f);
    ssao.setApproximateNormals(true);
    ssao.setEnabled(highQuality);
    filters.addFilter(ssao);
    bloom = new BloomFilter(BloomFilter.GlowMode.SceneAndObjects);
    bloom.setBloomIntensity(.65f);
    bloom.setExposurePower(2.6f);
    bloom.setBlurScale(1.1f);
    bloom.setEnabled(highQuality);
    filters.addFilter(bloom);
    atmosphere = new AtmosphereFilter();
    filters.addFilter(atmosphere);
    viewPort.addProcessor(filters);
  }

  public void toggleQuality() {
    highQuality = !highQuality;
    ssao.setEnabled(highQuality);
    bloom.setEnabled(highQuality);
    // Shadows render into the scene buffer before post-processing. Appending them
    // after FilterPostProcessor instead corrupts the next frame's depth/color buffer.
    if (highQuality) viewPort.getProcessors().add(0, shadows);
    else viewPort.removeProcessor(shadows);
    ui.invalidate();
    notice("Grafik: " + (highQuality ? "Atmosphärisch" : "Schnell"));
  }

  public boolean highQuality() {
    return highQuality;
  }

  public ScreenMode mode() {
    return mode;
  }

  /** Settings are reachable from the title screen and from the pause page; return where we came. */
  public void openSettings() {
    settingsReturn = mode == ScreenMode.MAIN_MENU ? ScreenMode.MAIN_MENU : ScreenMode.PAUSED;
    screen(ScreenMode.SETTINGS);
  }

  public void closeSettings() {
    storeSettings();
    screen(settingsReturn);
  }

  public void storeSettings() {
    if (preferences == null || audio == null) return;
    settings.master = audio.master();
    settings.music = audio.music();
    settings.effects = audio.effects();
    settings.radio = audio.radio();
    settings.muted = audio.muted();
    preferences.save(settings);
  }

  public void screen(ScreenMode next) {
    mode = next;
    inputManager.setCursorVisible(next != ScreenMode.PLAYING);
    if (game != null && game.isInitialized()) game.pause(next != ScreenMode.PLAYING);
    if (ui != null) ui.invalidate();
  }

  public void notice(String message) {
    if (ui != null) ui.notice(message);
    System.out.println("[GAME] " + message);
  }

  private void key(String name, int code) {
    inputManager.addMapping(name, new KeyTrigger(code));
    inputManager.addListener(this, name);
  }

  private void inputs() {
    key("Forward", KeyInput.KEY_W);
    key("Back", KeyInput.KEY_S);
    key("Left", KeyInput.KEY_A);
    key("Right", KeyInput.KEY_D);
    key("Sprint", KeyInput.KEY_LSHIFT);
    key("Jump", KeyInput.KEY_SPACE);
    key("Dodge", KeyInput.KEY_LMENU);
    key("Spell", KeyInput.KEY_Q);
    key("Interact", KeyInput.KEY_E);
    key("Potion", KeyInput.KEY_R);
    key("Pause", KeyInput.KEY_ESCAPE);
    key("Inventory", KeyInput.KEY_I);
    key("Journal", KeyInput.KEY_J);
    key("Skills", KeyInput.KEY_K);
    key("Map", KeyInput.KEY_M);
    key("Save", KeyInput.KEY_F5);
    key("Load", KeyInput.KEY_F9);
    key("Quality", KeyInput.KEY_F3);
    key("Mute", KeyInput.KEY_F10);
    key("Choice1", KeyInput.KEY_1);
    key("Choice2", KeyInput.KEY_2);
    key("Choice3", KeyInput.KEY_3);
    key("Enter", KeyInput.KEY_RETURN);
    inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addMapping("Block", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(this, "Attack", "Block");
    inputManager.addMapping("LookLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true));
    inputManager.addMapping("LookRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
    inputManager.addMapping("LookUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
    inputManager.addMapping("LookDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
    inputManager.addListener(this, "LookLeft", "LookRight", "LookUp", "LookDown");
  }

  @Override
  public void onAction(String name, boolean pressed, float tpf) {
    if (game == null || game.player == null) return;
    // Releasing the button ends a slider drag wherever the cursor happens to be.
    if (!pressed && name.equals("Attack") && ui != null) ui.release();
    if (mode == ScreenMode.PLAYING && game.session.player.health <= 0) return;
    if (pressed) {
      if (name.equals("Quality")) {
        toggleQuality();
        return;
      }
      if (name.equals("Mute")) {
        audio.toggleMute();
        ui.invalidate();
        return;
      }
      if (name.equals("Enter") && mode == ScreenMode.MAIN_MENU) {
        game.newGame();
        return;
      }
      if (name.equals("Attack") && mode != ScreenMode.PLAYING) {
        Vector2f p = inputManager.getCursorPosition();
        ui.click(p.x, p.y);
        return;
      }
      if (mode == ScreenMode.DIALOGUE && name.startsWith("Choice")) {
        game.choose(Integer.parseInt(name.substring(6)) - 1);
        return;
      }
      if (name.equals("Pause")) {
        if (mode == ScreenMode.SETTINGS) closeSettings();
        else if (mode == ScreenMode.PLAYING) screen(ScreenMode.PAUSED);
        else if (mode != ScreenMode.MAIN_MENU
            && mode != ScreenMode.GAME_OVER
            && mode != ScreenMode.TRANSITION) screen(ScreenMode.PLAYING);
        return;
      }
      if (name.equals("Load") && (mode == ScreenMode.PLAYING || mode == ScreenMode.PAUSED)) {
        game.loadGame();
        return;
      }
      if (name.equals("Save") && mode == ScreenMode.PAUSED) {
        game.save(false);
        return;
      }
      ScreenMode overlay =
          switch (name) {
            case "Inventory" -> ScreenMode.INVENTORY;
            case "Journal" -> ScreenMode.JOURNAL;
            case "Skills" -> ScreenMode.SKILLS;
            case "Map" -> ScreenMode.MAP;
            default -> null;
          };
      if (overlay != null && (mode == ScreenMode.PLAYING || mode == overlay)) {
        screen(mode == overlay ? ScreenMode.PLAYING : overlay);
        return;
      }
    }
    if (mode == ScreenMode.PLAYING) game.input(name, pressed);
  }

  @Override
  public void onAnalog(String name, float value, float tpf) {
    if (mode != ScreenMode.PLAYING || game == null || game.player == null) return;
    switch (name) {
      case "LookLeft" -> game.player.look(-value, 0);
      case "LookRight" -> game.player.look(value, 0);
      case "LookUp" -> game.player.look(0, -value);
      case "LookDown" -> game.player.look(0, value);
      default -> {}
    }
  }

  @Override
  public void simpleUpdate(float tpf) {
    if (smokeScenario != null) smokeScenario.update(tpf);
    if (ui != null) ui.update(tpf);
    if (audio != null)
      audio.update(
          tpf,
          game != null && game.combat != null && game.combat.inCombat(),
          // The settings page must not duck, or the sliders would lie about the level.
          mode != ScreenMode.PLAYING && mode != ScreenMode.SETTINGS);
  }

  @Override
  public void reshape(int w, int h) {
    super.reshape(w, h);
    if (cam != null && h > 0) cam.setFrustumPerspective(58, w / (float) h, .12f, 160);
  }

  @Override
  public void destroy() {
    storeSettings();
    if (audio != null) audio.cleanup();
    super.destroy();
  }
}
