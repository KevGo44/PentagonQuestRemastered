package de.pentagon.assets;

import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;

/** Shared immutable meshes; switch only on threshold crossings, keep spatial frustum culling. */
public final class DistanceLodControl extends AbstractControl {
  private final Mesh near, far;
  private boolean distant;

  public DistanceLodControl(Mesh near, Mesh far) {
    this.near = near;
    this.far = far;
  }

  @Override
  protected void controlUpdate(float tpf) {}

  @Override
  protected void controlRender(RenderManager rm, ViewPort vp) {
    boolean next =
        vp.getCamera().getLocation().distanceSquared(spatial.getWorldTranslation())
            > (distant ? 22 * 22 : 26 * 26);
    if (next != distant) {
      ((Geometry) spatial).setMesh(next ? far : near);
      distant = next;
    }
  }
}
