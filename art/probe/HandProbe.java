
package de.pentagon.probe;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import de.pentagon.assets.*;

/**
 * Renders the hero the way the game orients him - a node rotated by Quaternion.lookAt, exactly what
 * BetterCharacterControl.setViewDirection does - so both the facing and the weapon seat can be
 * judged from the picture instead of from a dot product.
 */
public final class HandProbe extends SimpleApplication {
  private int frame;
  private ScreenshotAppState shots;
  private Node hand, off, pivot;

  public static void main(String[] args) {
    HandProbe probe = new HandProbe();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1100, 1100);
    settings.setTitle("HandProbe");
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
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false, true);
    pivot = new Node("Player");
    pivot.attachChild(rig.root());
    // What the game does: facing (0,0,-1) at yaw = PI.
    Quaternion facing = new Quaternion();
    facing.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
    pivot.setLocalRotation(facing);
    rootNode.attachChild(pivot);
    rig.restart("Idle");
    rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.9f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(-.35f, -.6f, -.72f).normalizeLocal(),
        ColorRGBA.White.mult(2.4f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(.75f, -.25f, .6f).normalizeLocal(),
        new ColorRGBA(1.1f, 1.2f, 1.4f, 1)));
    rootNode.addLight(new DirectionalLight(new Vector3f(0, .8f, -.6f).normalizeLocal(),
        ColorRGBA.White.mult(.9f)));
    viewPort.setBackgroundColor(new ColorRGBA(.22f, .24f, .28f, 1));
    hand = rig.skinning().getAttachmentsNode("Hand.R");
    off = rig.skinning().getAttachmentsNode("Hand.L");
  }

  @Override
  public void simpleUpdate(float dt) {
    frame++;
    switch (frame) {
      case 20 -> { // from behind, where the game camera sits
        look(new Vector3f(0, 1.05f, 0), new Vector3f(0, 1.35f, 4.0f));
        shots.takeScreenshot();
      }
      case 30 -> { // from the front
        look(new Vector3f(0, 1.05f, 0), new Vector3f(0, 1.25f, -4.0f));
        shots.takeScreenshot();
      }
      case 40 -> { // from the character's right
        look(new Vector3f(0, 1.05f, 0), new Vector3f(-4.0f, 1.25f, 0));
        shots.takeScreenshot();
      }
      case 50 -> { // sword hand, from the front right
        Vector3f p = hand.getWorldTranslation();
        look(p, p.add(-.7f, .3f, -.9f));
        shots.takeScreenshot();
      }
      case 60 -> { // shield hand, from the front left
        Vector3f p = off.getWorldTranslation();
        look(p, p.add(.7f, .3f, -.9f));
        shots.takeScreenshot();
      }
      case 70 -> {
        System.out.println("[SEAT] Hand.R=" + hand.getWorldTranslation()
            + " fingers=" + hand.getWorldRotation().mult(Vector3f.UNIT_Y));
        System.out.println("[SEAT] Hand.L=" + off.getWorldTranslation()
            + " fingers=" + off.getWorldRotation().mult(Vector3f.UNIT_Y));
      }
      case 75 -> stop();
      default -> {}
    }
  }

  private void look(Vector3f at, Vector3f from) {
    cam.setLocation(from);
    cam.lookAt(at, Vector3f.UNIT_Y);
  }
}
