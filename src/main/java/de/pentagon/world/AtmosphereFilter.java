package de.pentagon.world;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.Filter;
import com.jme3.renderer.*;
import de.pentagon.assets.AssetPipeline;

/** Depth-limited height fog integration with local shafts and filmic tone mapping. */
public final class AtmosphereFilter extends Filter {
  private Camera camera;
  private ColorRGBA fog = AssetPipeline.color(0x14222e);
  private final Vector3f beam = new Vector3f(42, 6, 42);
  private float time, damage, exposure = 1.2f;

  public AtmosphereFilter() {
    super("Dungeon atmosphere");
  }

  @Override
  protected boolean isRequiresDepthTexture() {
    return true;
  }

  @Override
  protected void initFilter(
      AssetManager assets, RenderManager rm, ViewPort vp, int width, int height) {
    camera = vp.getCamera();
    material = new Material(assets, "shaders/Atmosphere.j3md");
  }

  @Override
  protected Material getMaterial() {
    return material;
  }

  @Override
  protected void preFrame(float tpf) {
    time += tpf;
    damage = Math.max(0, damage - tpf * 1.8f);
    material.setMatrix4("InverseViewProjection", camera.getViewProjectionMatrix().invert());
    material.setVector3("CameraPosition", camera.getLocation());
    material.setColor("FogColor", fog);
    material.setVector3("BeamPosition", beam);
    material.setFloat("Time", time);
    material.setFloat("Damage", damage);
    material.setFloat("Exposure", exposure);
  }

  public void region(Region region) {
    fog = AssetPipeline.color(region.fog);
  }

  public void beam(Vector3f position) {
    beam.set(position);
  }

  public void hit() {
    damage = .65f;
  }

  public void exposure(float value) {
    exposure = value;
  }
}
