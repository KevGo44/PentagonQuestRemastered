
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
 * One clip as a film strip: the same character twelve times, each frozen at a later moment of the
 * clip, front view and top view. Reads a spin, a roll or a strike as a sequence instead of as four
 * lucky snapshots. Usage: ClipStrip <character> <clip> [armed]
 */
public final class ClipStrip extends SimpleApplication {
  private static final int TILES = 12;
  private static String id = "hero", clip = "Attack2";
  private static boolean armed = true;
  private int frame;
  private ScreenshotAppState shots;

  public static void main(String[] args) {
    if (args.length > 0) id = args[0];
    if (args.length > 1) clip = args[1];
    if (args.length > 2) armed = Boolean.parseBoolean(args[2]);
    ClipStrip probe = new ClipStrip();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(2400, 700);
    settings.setSamples(4);
    probe.setSettings(settings);
    probe.setShowSettings(false);
    probe.setDisplayStatView(false);
    probe.setDisplayFps(false);
    probe.start();
  }

  @Override
  public void simpleInitApp() {
    shots = new ScreenshotAppState("strip-" + id + "-" + clip + "-", "", 0);
    stateManager.attach(shots);
    flyCam.setEnabled(false);
    AssetPipeline assets = new AssetPipeline(assetManager);
    Quaternion facing = new Quaternion();
    facing.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
    for (int i = 0; i < TILES; i++) {
      CharacterFactory.Rig rig = new CharacterFactory(assets).create(id, 0x547079, false, armed);
      double length = rig.composer().getAnimClip(clip).getLength();
      // No blend, so every tile shows the clip itself and not a mix with the bind pose.
      ((com.jme3.anim.tween.action.BlendableAction) rig.composer().action(clip))
          .setTransitionLength(0);
      rig.restart(clip);
      rig.composer().setTime(length * i / (TILES - 1.0) * .999);
      rig.pause(true);
      Node pivot = new Node("T" + i);
      pivot.attachChild(rig.root());
      pivot.setLocalRotation(facing);
      pivot.setLocalTranslation((TILES / 2f - .5f - i) * 1.9f, 0, 0);
      rootNode.attachChild(pivot);
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
      case 10 -> { // front, slightly above
        cam.setLocation(new Vector3f(0, 2.6f, -13f));
        cam.lookAt(new Vector3f(0, .9f, 0), Vector3f.UNIT_Y);
        shots.takeScreenshot();
      }
      case 20 -> { // top-down, front of the character towards the bottom of the picture
        cam.setLocation(new Vector3f(0, 14f, 0));
        cam.lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1));
        shots.takeScreenshot();
      }
      case 30 -> stop();
      default -> {}
    }
  }
}
