
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

/** Four unambiguous seats, built from angle-axis turns, measured and rendered. */
public final class SeatVariants2 extends SimpleApplication {
  private int frame;
  private ScreenshotAppState shots;
  private final List<Node> swords = new ArrayList<>(), shields = new ArrayList<>();
  private Node hand, off;
  private AssetPipeline assets;

  public static void main(String[] args) {
    SeatVariants2 probe = new SeatVariants2();
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
    shots = new ScreenshotAppState("v2-", "shot", 0);
    stateManager.attach(shots);
    flyCam.setEnabled(false);
    assets = new AssetPipeline(assetManager);
    CharacterFactory.Rig rig = new CharacterFactory(assets).create("hero", 0x547079, false);
    Node pivot = new Node("Player");
    pivot.attachChild(rig.root());
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
    viewPort.setBackgroundColor(new ColorRGBA(.24f, .26f, .3f, 1));
    hand = rig.skinning().getAttachmentsNode("Hand.R");
    off = rig.skinning().getAttachmentsNode("Hand.L");
    Quaternion toX = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Z);
    Quaternion toMinusX = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
    Quaternion roll = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
    sword("V1_bladePlusX", toX, new Vector3f(0, .095f, .015f));
    sword("V2_bladePlusX_rolled", toX.mult(roll), new Vector3f(0, .095f, .015f));
    sword("V3_bladeMinusX", toMinusX, new Vector3f(0, .095f, .015f));
    sword("V4_bladeMinusX_rolled", toMinusX.mult(roll), new Vector3f(0, .095f, .015f));
    // Shield: face (+Z of the model) on the palm side (+Z of the hand) and on the back side.
    shield("D_palm", new Quaternion(), new Vector3f(0, .085f, .085f));
    shield("D_back", new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y),
        new Vector3f(0, .085f, -.085f));
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
      case 2 -> {
        for (String n : new String[] {"Hand.R", "Hand.L"}) {
          Node h = n.equals("Hand.R") ? hand : off;
          Quaternion q = h.getWorldRotation();
          System.out.println("[AXES] " + n + " at=" + h.getWorldTranslation()
              + " X=" + q.mult(Vector3f.UNIT_X) + " Y=" + q.mult(Vector3f.UNIT_Y)
              + " Z=" + q.mult(Vector3f.UNIT_Z));
        }
        for (Node n : swords)
          System.out.println("[BLADE] " + n.getName()
              + " worldBlade=" + n.getWorldRotation().mult(Vector3f.UNIT_Y)
              + " worldFlat=" + n.getWorldRotation().mult(Vector3f.UNIT_Z));
        for (Node n : shields)
          System.out.println("[FACE] " + n.getName()
              + " worldFace=" + n.getWorldRotation().mult(Vector3f.UNIT_Z));
      }
      case 3, 4, 5, 6 -> {
        show(swords, stage - 3);
        Vector3f p = hand.getWorldTranslation();
        look(p, p.add(-.55f, .35f, -1.2f));
        shots.takeScreenshot();
      }
      case 7, 8 -> {
        show(swords, 0);
        show(shields, stage - 7);
        Vector3f p = off.getWorldTranslation();
        look(p, p.add(.55f, .35f, -1.2f));
        shots.takeScreenshot();
      }
      case 9 -> stop();
      default -> {}
    }
  }

  private void look(Vector3f at, Vector3f from) {
    cam.setLocation(from);
    cam.lookAt(at, Vector3f.UNIT_Y);
  }
}
