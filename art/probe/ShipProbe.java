
package de.pentagon.probe;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import de.pentagon.assets.*;

/** The shipped seat, in the poses the player sees, from where the game camera sits. */
public final class ShipProbe extends SimpleApplication {
  private int frame;
  private ScreenshotAppState shots;
  private CharacterFactory.Rig rig;

  public static void main(String[] args) {
    ShipProbe probe = new ShipProbe();
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
    shots = new ScreenshotAppState("ship-", "shot", 0);
    stateManager.attach(shots);
    flyCam.setEnabled(false);
    AssetPipeline assets = new AssetPipeline(assetManager);
    // Exactly what PlayerController does.
    rig = new CharacterFactory(assets).create("hero", 0x547079, false, true);
    Node pivot = new Node("Player");
    pivot.attachChild(rig.root());
    Quaternion facing = new Quaternion();
    facing.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
    pivot.setLocalRotation(facing);
    rootNode.attachChild(pivot);
    rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.8f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(-.4f, -.55f, -.73f).normalizeLocal(),
        ColorRGBA.White.mult(2.2f)));
    rootNode.addLight(new DirectionalLight(new Vector3f(.8f, -.2f, .55f).normalizeLocal(),
        new ColorRGBA(1.0f, 1.1f, 1.35f, 1)));
    viewPort.setBackgroundColor(new ColorRGBA(.25f, .27f, .31f, 1));
    // The facing metric a test can assert without a renderer.
    for (String id : new String[] {
        "hero", "mira", "eren", "goblin", "orc", "warden", "shaman", "king", "placeholder_probe"}) {
      CharacterFactory.Rig r = new CharacterFactory(assets).create(id, 0x556677, false);
      r.restart("Idle");
      r.root().updateLogicalState(.05f);
      r.root().updateGeometricState();
      Armature armature = r.skinning().getArmature();
      armature.update();
      Transform world = r.skinning().getSpatial().getWorldTransform();
      Vector3f rs = at(armature, world, "RightShoulder"), ls = at(armature, world, "LeftShoulder");
      if (rs == null) {
        rs = at(armature, world, "UpperArm.R");
        ls = at(armature, world, "UpperArm.L");
      }
      Vector3f across = ls.subtract(rs);
      across.y = 0;
      System.out.println("[FACE] " + id + " shoulderLine(R->L)=" + across.normalize());
    }
  }

  private Vector3f at(Armature armature, Transform world, String joint) {
    Joint j = armature.getJoint(joint);
    return j == null
        ? null
        : world.transformVector(j.getModelTransform().getTranslation(), new Vector3f());
  }

  @Override
  public void simpleUpdate(float dt) {
    frame++;
    int stage = frame / 12, sub = frame % 12;
    if (sub == 1) {
      switch (stage) {
        case 1 -> rig.restart("Idle");
        case 2 -> rig.restart("Idle");
        case 3 -> rig.restart("Block");
        case 4 -> rig.restart("Walk");
        case 5 -> rig.restart("Attack1");
        default -> {}
      }
    }
    if (sub != 6) return;
    switch (stage) {
      case 1 -> { // idle, from the game camera
        look(new Vector3f(0, 1.05f, 0), new Vector3f(0, 1.65f, 4.2f));
        shots.takeScreenshot();
      }
      case 2 -> { // idle, three quarter front
        look(new Vector3f(0, 1.05f, 0), new Vector3f(2.2f, 1.5f, -3.0f));
        shots.takeScreenshot();
      }
      case 3 -> { // block, from the front
        look(new Vector3f(0, 1.15f, 0), new Vector3f(.8f, 1.4f, -2.6f));
        shots.takeScreenshot();
      }
      case 4 -> { // walk, from the game camera
        look(new Vector3f(0, 1.05f, 0), new Vector3f(0, 1.65f, 4.2f));
        shots.takeScreenshot();
      }
      case 5 -> { // mid swing
        look(new Vector3f(0, 1.15f, 0), new Vector3f(2.4f, 1.6f, 2.4f));
        shots.takeScreenshot();
      }
      case 6 -> stop();
      default -> {}
    }
  }

  private void look(Vector3f at, Vector3f from) {
    cam.setLocation(from);
    cam.lookAt(at, Vector3f.UNIT_Y);
  }
}
