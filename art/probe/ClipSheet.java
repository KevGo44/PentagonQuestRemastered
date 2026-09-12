
package de.pentagon.probe;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import de.pentagon.assets.*;
import java.util.*;

/**
 * Every clip, side by side, oriented the way the game orients a character. One picture decides
 * whether the clips agree on a front, instead of eleven dot products that each depend on the pose.
 */
public final class ClipSheet extends SimpleApplication {
  private int frame;
  private ScreenshotAppState shots;
  private final List<CharacterFactory.Rig> rigs = new ArrayList<>();

  public static void main(String[] args) {
    ClipSheet probe = new ClipSheet();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1920, 700);
    settings.setSamples(4);
    probe.setSettings(settings);
    probe.setShowSettings(false);
    probe.setDisplayStatView(false);
    probe.setDisplayFps(false);
    probe.start();
  }

  @Override
  public void simpleInitApp() {
    shots = new ScreenshotAppState("clips-", "sheet", 0);
    stateManager.attach(shots);
    flyCam.setEnabled(false);
    AssetPipeline assets = new AssetPipeline(assetManager);
    Quaternion facing = new Quaternion();
    facing.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
    for (int i = 0; i < CharacterFactory.CLIPS.length; i++) {
      CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false, true);
      Node pivot = new Node("Player" + i);
      pivot.attachChild(rig.root());
      pivot.setLocalRotation(facing);
      pivot.setLocalTranslation((i - 5) * 1.5f, 0, 0);
      rootNode.attachChild(pivot);
      rig.restart(CharacterFactory.CLIPS[i]);
      rigs.add(rig);
    }
    rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.9f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(-.3f, -.55f, -.78f).normalizeLocal(),
        ColorRGBA.White.mult(2.2f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(.7f, -.2f, .68f).normalizeLocal(),
        new ColorRGBA(1.0f, 1.1f, 1.3f, 1)));
    viewPort.setBackgroundColor(new ColorRGBA(.26f, .28f, .32f, 1));
  }

  @Override
  public void simpleUpdate(float dt) {
    frame++;
    switch (frame) {
      case 12 -> { // front: the camera stands where the character is looking
        cam.setLocation(new Vector3f(0, 1.5f, -12.5f));
        cam.lookAt(new Vector3f(0, .95f, 0), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 24 -> { // straight down: the facing angle is readable without perspective
        cam.setLocation(new Vector3f(0, 12f, 0));
        cam.lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, -1));
        shots.takeScreenshot();
      }
      case 40 -> { // front again, later in every clip
        cam.setLocation(new Vector3f(0, 1.5f, -12.5f));
        cam.lookAt(new Vector3f(0, .95f, 0), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 52 -> {
        cam.setLocation(new Vector3f(0, 12f, 0));
        cam.lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, -1));
        shots.takeScreenshot();
      }
      case 60 -> stop();
      default -> {}
    }
  }
}
