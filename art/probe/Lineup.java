
package de.pentagon.probe;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import de.pentagon.ai.EnemyType;
import de.pentagon.assets.*;
import java.util.*;

/**
 * All eight characters at the scale the game gives them, in Idle, with a 1.95 m post between each
 * pair; behind them the delivered props with a 1 m post. One frame answers proportion, material and
 * scale questions that no number can - the same rule as for facing: rendered, not computed.
 */
public final class Lineup extends SimpleApplication {
  private int frame;
  private ScreenshotAppState shots;
  private final List<CharacterFactory.Rig> rigs = new ArrayList<>();
  private final String[] chars = {"hero", "mira", "eren", "goblin", "orc", "warden", "shaman", "king"};
  private final float[] scale = {1, 1, 1, EnemyType.GOBLIN.scale, EnemyType.ORC.scale, EnemyType.WARDEN.scale, EnemyType.SHAMAN.scale, EnemyType.KING.scale};
  private final int[] tint = {0x547079, 0x50747b, 0x806e56, EnemyType.GOBLIN.color, EnemyType.ORC.color, EnemyType.WARDEN.color, EnemyType.SHAMAN.color, EnemyType.KING.color};
  private final String[] props = {"chest", "shrine", "rune", "seal", "lore", "throne", "portal", "crystal", "sword", "shield", "kit_pillar", "kit_brazier", "kit_wall_face", "kit_rock", "kit_arch"};

  public static void main(String[] args) {
    Lineup probe = new Lineup();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(2200, 1000);
    settings.setSamples(4);
    probe.setSettings(settings);
    probe.setShowSettings(false);
    probe.setDisplayStatView(false);
    probe.setDisplayFps(false);
    probe.start();
  }

  @Override
  public void simpleInitApp() {
    shots = new ScreenshotAppState("lineup-", "shot", 0);
    stateManager.attach(shots);
    flyCam.setEnabled(false);
    AssetPipeline assets = new AssetPipeline(assetManager);
    Quaternion facing = new Quaternion();
    facing.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
    Material post = assets.pbr("", 0xd8d2c4, .8f, 0);
    float x = -12;
    for (int i = 0; i < chars.length; i++) {
      CharacterFactory.Rig rig = new CharacterFactory(assets).create(chars[i], tint[i], chars[i].equals("king"), chars[i].equals("hero"));
      rig.root().setLocalScale(scale[i]);
      Node pivot = new Node(chars[i]);
      pivot.attachChild(rig.root());
      pivot.setLocalRotation(facing);
      x += 1.4f + scale[i] * .9f;
      pivot.setLocalTranslation(x, 0, 0);
      rootNode.attachChild(pivot);
      rigs.add(rig);
      Geometry g = new Geometry("post", new Box(.03f, .975f, .03f));
      g.setMaterial(post);
      g.setLocalTranslation(x + .7f + scale[i] * .45f, .975f, 0);
      rootNode.attachChild(g);
    }
    float px = -14;
    for (String id : props) {
      Spatial s = assets.model("props/" + id, () -> null);
      if (s == null) continue;
      px += 3.2f;
      s.setLocalTranslation(px, 0, -9);
      rootNode.attachChild(s);
      Geometry g = new Geometry("post", new Box(.03f, .5f, .03f));
      g.setMaterial(post);
      g.setLocalTranslation(px + 1.3f, .5f, -9);
      rootNode.attachChild(g);
    }
    Geometry floor = new Geometry("floor", new Box(30, .02f, 20));
    floor.setMaterial(assets.pbr("", 0x3c4046, .9f, 0));
    floor.setLocalTranslation(0, -.02f, -5);
    rootNode.attachChild(floor);
    rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.7f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(-.3f, -.6f, -.75f).normalizeLocal(), ColorRGBA.White.mult(2.0f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(.7f, -.3f, .65f).normalizeLocal(), new ColorRGBA(.9f, 1.0f, 1.2f, 1)));
    rootNode.addLight(EnvironmentLighting.dungeonProbe());
    viewPort.setBackgroundColor(new ColorRGBA(.24f, .26f, .30f, 1));
  }

  @Override
  public void simpleUpdate(float dt) {
    frame++;
    switch (frame) {
      case 40 -> { // characters from the front, at eye height
        cam.setLocation(new Vector3f(0, 1.7f, -16));
        cam.lookAt(new Vector3f(0, 1.5f, 0), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 55 -> { // characters three-quarter
        cam.setLocation(new Vector3f(-9, 3.2f, -13));
        cam.lookAt(new Vector3f(0, 1.4f, 0), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 70 -> { // props from the front
        cam.setLocation(new Vector3f(0, 3.2f, -26));
        cam.lookAt(new Vector3f(0, 1.6f, -9), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 85 -> { // props three-quarter, closer
        cam.setLocation(new Vector3f(-10, 4f, -22));
        cam.lookAt(new Vector3f(2, 1.4f, -9), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 92 -> { // props from their front (+Z), high and close, characters behind the camera
        cam.setLocation(new Vector3f(2, 6f, -1.5f));
        cam.lookAt(new Vector3f(0, 1.2f, -9), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 96 -> { // throne, altar and shrine close, from the front left
        cam.setLocation(new Vector3f(9, 3.5f, -3.5f));
        cam.lookAt(new Vector3f(4.5f, 1.4f, -9), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 100 -> { // characters from behind, game camera height
        cam.setLocation(new Vector3f(0, 3.5f, 14));
        cam.lookAt(new Vector3f(0, 1.2f, 0), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 110 -> stop();
      default -> {}
    }
  }
}
