
package de.pentagon.probe;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import de.pentagon.assets.*;

/** The shipped seat in the shipped poses, framed so hand, weapon and body stay in one picture. */
public final class SeatShots extends SimpleApplication {
  private int frame;
  private ScreenshotAppState shots;
  private CharacterFactory.Rig rig;
  private Node hand, off;

  public static void main(String[] args) {
    SeatShots probe = new SeatShots();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1100, 1100);
    settings.setSamples(4);
    probe.setSettings(settings);
    probe.setShowSettings(false);
    probe.setDisplayStatView(false);
    probe.setDisplayFps(false);
    probe.start();
  }

  @Override
  public void simpleInitApp() {
    shots = new ScreenshotAppState("seat-", "shot", 0);
    stateManager.attach(shots);
    flyCam.setEnabled(false);
    AssetPipeline assets = new AssetPipeline(assetManager);
    rig = new CharacterFactory(assets).create("hero", 0x547079, false, true);
    Node pivot = new Node("Player");
    pivot.attachChild(rig.root());
    Quaternion facing = new Quaternion();
    facing.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
    pivot.setLocalRotation(facing);
    rootNode.attachChild(pivot);
    rig.restart("Idle");
    rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.95f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(-.35f, -.5f, -.79f).normalizeLocal(),
        ColorRGBA.White.mult(2.4f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(.75f, -.25f, .6f).normalizeLocal(),
        new ColorRGBA(1.1f, 1.2f, 1.4f, 1)));
    rootNode.addLight(new DirectionalLight(new Vector3f(0, .8f, .4f).normalizeLocal(),
        ColorRGBA.White.mult(1.1f)));
    viewPort.setBackgroundColor(new ColorRGBA(.27f, .29f, .33f, 1));
    hand = rig.skinning().getAttachmentsNode("Hand.R");
    off = rig.skinning().getAttachmentsNode("Hand.L");
  }

  @Override
  public void simpleUpdate(float dt) {
    frame++;
    int stage = frame / 12;
    if (frame % 12 == 1) {
      if (stage == 4) rig.restart("Walk");
      if (stage == 6) rig.restart("Block");
    }
    if (frame % 12 != 6) return;
    switch (stage) {
      case 1 -> { // in front of the character, from its right
        look(new Vector3f(0, 1.05f, 0), new Vector3f(1.9f, 1.4f, -2.6f));
        shots.takeScreenshot();
      }
      case 2 -> { // behind, where the game camera sits
        look(new Vector3f(0, 1.05f, 0), new Vector3f(0, 1.55f, 3.9f));
        shots.takeScreenshot();
      }
      case 3 -> { // close on the sword hand, aimed between hand and hip
        Vector3f p = hand.getWorldTranslation().add(0, -.15f, 0);
        look(p, p.add(1.0f, .5f, -1.1f));
        shots.takeScreenshot();
      }
      case 4 -> { // close on the shield hand
        Vector3f p = off.getWorldTranslation();
        look(p, p.add(-.9f, .35f, -1.1f));
        shots.takeScreenshot();
      }
      case 5 -> { // walk, from behind
        look(new Vector3f(0, 1.05f, 0), new Vector3f(0, 1.55f, 3.9f));
        shots.takeScreenshot();
      }
      case 6 -> { // walk, from the front left
        look(new Vector3f(0, 1.05f, 0), new Vector3f(-1.9f, 1.4f, -2.6f));
        shots.takeScreenshot();
      }
      case 7 -> { // block, from the front
        look(new Vector3f(0, 1.15f, 0), new Vector3f(.4f, 1.4f, -2.6f));
        shots.takeScreenshot();
      }
      case 8 -> stop();
      default -> {}
    }
  }

  private void look(Vector3f at, Vector3f from) {
    cam.setLocation(from);
    cam.lookAt(at, Vector3f.UNIT_Y);
  }
}
