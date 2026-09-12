
package de.pentagon.probe;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.AppSettings;
import de.pentagon.assets.*;
import java.util.*;

/** Renders candidate weapon seats side by side so the right one can be chosen by eye. */
public final class SeatVariants extends SimpleApplication {
  private int frame;
  private ScreenshotAppState shots;
  private final List<Node> swords = new ArrayList<>(), shields = new ArrayList<>();
  private Node hand, off;
  private AssetPipeline assets;

  public static void main(String[] args) {
    SeatVariants probe = new SeatVariants();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1100, 1100);
    settings.setSamples(4);
    settings.setTitle("SeatVariants");
    probe.setSettings(settings);
    probe.setShowSettings(false);
    probe.setDisplayStatView(false);
    probe.setDisplayFps(false);
    probe.start();
  }

  private static Quaternion axes(Vector3f x, Vector3f y, Vector3f z) {
    Quaternion q = new Quaternion();
    q.fromAxes(x, y, z);
    return q;
  }

  @Override
  public void simpleInitApp() {
    shots = new ScreenshotAppState("var-", "shot", 0);
    stateManager.attach(shots);
    flyCam.setEnabled(false);
    assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false);
    Node model = new Node("Model");
    model.attachChild(rig.root());
    // The clips were baked facing model -X; the game rotates the node so that +Z is forward.
    model.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
    Node pivot = new Node("Player");
    pivot.attachChild(model);
    Quaternion facing = new Quaternion();
    facing.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
    pivot.setLocalRotation(facing);
    rootNode.attachChild(pivot);
    rig.restart("Idle");
    rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.85f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(-.4f, -.55f, -.73f).normalizeLocal(),
        ColorRGBA.White.mult(2.3f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(.8f, -.2f, .55f).normalizeLocal(),
        new ColorRGBA(1.0f, 1.1f, 1.35f, 1)));
    rootNode.addLight(new DirectionalLight(new Vector3f(0, .85f, -.5f).normalizeLocal(),
        ColorRGBA.White.mult(.8f)));
    viewPort.setBackgroundColor(new ColorRGBA(.24f, .26f, .3f, 1));
    hand = rig.skinning().getAttachmentsNode("Hand.R");
    off = rig.skinning().getAttachmentsNode("Hand.L");
    // Sword candidates: blade (+Y of the model) out past the thumb (+X of the hand).
    sword("S1", axes(new Vector3f(0, 0, 1), Vector3f.UNIT_X, Vector3f.UNIT_Y),
        new Vector3f(0, .095f, .015f));
    sword("S2", axes(new Vector3f(0, 0, -1), Vector3f.UNIT_X, new Vector3f(0, -1, 0)),
        new Vector3f(0, .095f, .015f));
    sword("S3", axes(new Vector3f(0, -1, 0), Vector3f.UNIT_X, Vector3f.UNIT_Z),
        new Vector3f(0, .095f, .015f));
    sword("S4", new Quaternion(), new Vector3f(0, .13f, 0));
    shield("D1", new Quaternion(), new Vector3f(0, .085f, .075f));
    shield("D2", axes(new Vector3f(-1, 0, 0), Vector3f.UNIT_Y, new Vector3f(0, 0, -1)),
        new Vector3f(0, .085f, -.075f));
    show(swords, -1);
    show(shields, -1);
  }

  private void sword(String name, Quaternion rotation, Vector3f offset) {
    Node n = new Node(name);
    n.attachChild(assets.model("props/sword", () -> new Node("x")));
    n.setLocalRotation(rotation);
    n.setLocalTranslation(offset);
    swords.add(n);
    hand.attachChild(n);
  }

  private void shield(String name, Quaternion rotation, Vector3f offset) {
    Node n = new Node(name);
    n.attachChild(assets.model("props/shield", () -> new Node("x")));
    n.setLocalRotation(rotation);
    n.setLocalTranslation(offset);
    shields.add(n);
    off.attachChild(n);
  }

  private void show(List<Node> group, int index) {
    for (int i = 0; i < group.size(); i++)
      group.get(i).setCullHint(i == index ? CullHint.Never : CullHint.Always);
  }

  @Override
  public void simpleUpdate(float dt) {
    frame++;
    int stage = frame / 10, sub = frame % 10;
    if (sub != 5) return;
    switch (stage) {
      case 2, 3, 4, 5 -> { // one shot per sword candidate, close on the sword hand
        show(swords, stage - 2);
        Vector3f p = hand.getWorldTranslation();
        look(p, p.add(-.25f, .45f, -1.15f));
        shots.takeScreenshot();
      }
      case 6, 7 -> { // one shot per shield candidate, close on the shield hand
        show(swords, 0);
        show(shields, stage - 6);
        Vector3f p = off.getWorldTranslation();
        look(p, p.add(.3f, .4f, -1.15f));
        shots.takeScreenshot();
      }
      case 8 -> { // full body, front, with S1 + D1
        show(swords, 0);
        show(shields, 0);
        look(new Vector3f(0, 1.05f, 0), new Vector3f(1.2f, 1.5f, -3.6f));
        shots.takeScreenshot();
      }
      case 9 -> { // full body, from the game camera behind the character
        look(new Vector3f(0, 1.05f, 0), new Vector3f(0, 1.6f, 4.2f));
        shots.takeScreenshot();
      }
      case 10 -> stop();
      default -> {}
    }
  }

  private void look(Vector3f at, Vector3f from) {
    cam.setLocation(from);
    cam.lookAt(at, Vector3f.UNIT_Y);
  }
}
