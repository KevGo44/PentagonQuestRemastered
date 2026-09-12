
package de.pentagon.probe;

import com.jme3.anim.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.system.JmeContext;
import de.pentagon.assets.*;

/** The toe metric: does the foot point along +Z, the engine's forward, in every clip? */
public final class ToeProbe extends SimpleApplication {
  public static void main(String[] args) {
    new ToeProbe().start(JmeContext.Type.Headless);
  }

  @Override
  public void simpleInitApp() {
    AssetPipeline assets = new AssetPipeline(assetManager);
    for (String id : new String[] {"hero", "mira", "eren", "goblin", "orc", "warden", "shaman", "king"}) {
      for (String clip : CharacterFactory.CLIPS) {
        CharacterFactory.Rig rig = new CharacterFactory(assets).create(id, 0x556677, false);
        rig.restart(clip);
        for (int i = 0; i < 6; i++) rig.root().updateLogicalState(.05f);
        rig.root().updateGeometricState();
        Armature armature = rig.skinning().getArmature();
        armature.update();
        Transform space = rig.skinning().getSpatial().getWorldTransform();
        Vector3f sum = new Vector3f();
        for (String[] pair : new String[][] {
            {"Foot.R", "RightToe_End"}, {"Foot.L", "LeftToe_End"}}) {
          Vector3f heel = at(armature, space, pair[0]), toe = at(armature, space, pair[1]);
          Vector3f step = toe.subtract(heel);
          step.y = 0;
          sum.addLocal(step.normalizeLocal());
        }
        sum.normalizeLocal();
        System.out.println("[TOE] " + id + " " + clip + " forward=" + sum + " z=" + sum.z);
      }
    }
    stop();
  }

  private Vector3f at(Armature armature, Transform space, String joint) {
    return space.transformVector(
        armature.getJoint(joint).getModelTransform().getTranslation(), new Vector3f());
  }
}
