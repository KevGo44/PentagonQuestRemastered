
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** The headless facing metric: is the line between the upper arms across the body? */
public final class AcrossProbe extends SimpleApplication {
  public static void main(String[] args) {
    new AcrossProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    for (String id : new String[] {
        "hero", "mira", "eren", "goblin", "orc", "warden", "shaman", "king", "placeholder_probe"}) {
      for (String clip : new String[] {"Idle", "Walk", "Attack1", "Block", "Death"}) {
        CharacterFactory.Rig rig = new CharacterFactory(assets).create(id, 0x556677, false);
        rig.restart(clip);
        rig.root().updateLogicalState(.05f);
        rig.root().updateGeometricState();
        Armature armature = rig.skinning().getArmature();
        armature.update();
        Transform space = rig.skinning().getSpatial().getWorldTransform();
        Vector3f left = at(armature, space, "UpperArm.L"), right = at(armature, space, "UpperArm.R");
        Vector3f across = left.subtract(right);
        across.y = 0;
        across.normalizeLocal();
        System.out.println("[ACROSS] " + id + " " + clip + " = " + across
            + " |x|=" + Math.abs(across.x));
      }
    }
    stop();
  }

  private Vector3f at(Armature armature, Transform space, String joint) {
    return space.transformVector(
        armature.getJoint(joint).getModelTransform().getTranslation(), new Vector3f());
  }
}
