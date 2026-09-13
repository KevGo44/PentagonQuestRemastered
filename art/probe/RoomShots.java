
package de.pentagon.probe;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.system.AppSettings;
import com.jme3.system.NativeLibraryLoader;
import de.pentagon.assets.*;
import de.pentagon.core.GameSession;
import de.pentagon.physics.PhysicsWorld;
import de.pentagon.world.*;
import java.util.*;

/**
 * The real region as WorldView builds it - kit, props, torches, region tint, the game's lights -
 * photographed from in front of each named object at the trailing camera's height. The smoke run
 * only ever shows the spawn end of a region; this shows the throne, the altars and the shrine
 * where they stand. Usage: RoomShots REGION id [id...]   (ids as in DungeonLayout, e.g. THRONE_throne)
 */
public final class RoomShots extends SimpleApplication {
  private static Region region = Region.THRONE;
  private static List<String> ids = List.of("THRONE_throne");
  private int frame;
  private ScreenshotAppState shots;
  private WorldView world;

  public static void main(String[] args) {
    if (args.length > 0) region = Region.valueOf(args[0]);
    if (args.length > 1) ids = Arrays.asList(args).subList(1, args.length);
    NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
    RoomShots probe = new RoomShots();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1600, 900);
    settings.setSamples(4);
    settings.setGammaCorrection(true);
    probe.setSettings(settings);
    probe.setShowSettings(false);
    probe.setDisplayStatView(false);
    probe.setDisplayFps(false);
    probe.start();
  }

  private RoomShots() {
    super(new com.jme3.app.state.AppState[0]);
  }

  @Override
  public void simpleInitApp() {
    shots = new ScreenshotAppState("room-" + region.name().toLowerCase(Locale.ROOT) + "-", "", 0);
    stateManager.attach(shots);
    if (flyCam != null) flyCam.setEnabled(false);
    AssetPipeline assets = new AssetPipeline(assetManager);
    BulletAppState bullet = new BulletAppState();
    stateManager.attach(bullet);
    PhysicsWorld physics = new PhysicsWorld(bullet.getPhysicsSpace());
    world = new WorldView(assets, physics, new DungeonLayout(region), new GameSession());
    rootNode.attachChild(world.root);
    // The game's base light, minus the post filters.
    SceneLighting.attach(rootNode);
    viewPort.setBackgroundColor(AssetPipeline.color(region.fog));
    cam.setFrustumPerspective(58, cam.getWidth() / (float) cam.getHeight(), .12f, 160);
  }

  @Override
  public void simpleUpdate(float dt) {
    frame++;
    world.update(frame / 60f, new Vector3f(42, 0, 42));
    int index = frame / 15 - 1;
    if (frame % 15 != 0) return;
    if (index < 0) return;
    if (index >= ids.size() * 2) {
      stop();
      return;
    }
    String id = ids.get(index / 2);
    var spec = world.layout.objects.stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
    Vector3f at = new Vector3f(spec.x(), 1.3f, spec.z());
    // First from the front where the player arrives, then three-quarter; a THRONE stands with its
    // back to -Z like the player faces after the gate, so "front" is +Z.
    Vector3f from = index % 2 == 0
        ? at.add(0, 2.2f, 6.5f)
        : at.add(4.5f, 2.6f, 4.5f);
    cam.setLocation(from);
    cam.lookAt(at, Vector3f.UNIT_Y);
    // The torches only light what is within 32 m of the "player".
    world.update(frame / 60f, from);
    shots.takeScreenshot();
  }
}
