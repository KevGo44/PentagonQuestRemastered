package de.pentagon.assets;

import com.jme3.asset.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.TangentBinormalGenerator;
import java.util.*;
import java.util.function.Supplier;

/** One import/material gateway. Asset IDs have stable metres, pivots, joints and sockets. */
public final class AssetPipeline {
  private final AssetManager manager;
  private final Map<String, Material> materials = new HashMap<>();

  public AssetPipeline(AssetManager manager) {
    this.manager = manager;
  }

  public AssetManager manager() {
    return manager;
  }

  public static ColorRGBA color(int hex) {
    return new ColorRGBA()
        .setAsSrgb(((hex >> 16) & 255) / 255f, ((hex >> 8) & 255) / 255f, (hex & 255) / 255f, 1);
  }

  public Material pbr(String surface, int tint, float roughness, float metallic) {
    String key = surface + tint + ":" + roughness + ":" + metallic;
    return materials.computeIfAbsent(
        key,
        k -> {
          Material m = new Material(manager, "Common/MatDefs/Light/PBRLighting.j3md");
          m.setColor("BaseColor", color(tint));
          m.setFloat("Roughness", roughness);
          m.setFloat("Metallic", metallic);
          if (!surface.isBlank()) {
            m.setTexture("BaseColorMap", texture("textures/" + surface + "-albedo.png", true));
            m.setTexture("NormalMap", texture("textures/" + surface + "-normal.png", false));
            m.setFloat("NormalType", 1);
            m.setFloat("NormalScale", .7f);
            m.setTexture("RoughnessMap", texture("textures/" + surface + "-roughness.png", false));
            m.setTexture("MetallicMap", texture("textures/" + surface + "-metallic.png", false));
          }
          return m;
        });
  }

  private Texture texture(String path, boolean srgb) {
    TextureKey key = new TextureKey(path, false);
    key.setGenerateMips(true);
    Texture t = manager.loadTexture(key);
    t.getImage().setColorSpace(srgb ? ColorSpace.sRGB : ColorSpace.Linear);
    t.setWrap(Texture.WrapMode.Repeat);
    t.setAnisotropicFilter(4);
    return t;
  }

  public Material glow(int color, float power) {
    return materials.computeIfAbsent(
        "glow" + color + power,
        k -> {
          Material m = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
          m.setColor("Color", color(color));
          m.setColor("GlowColor", color(color).mult(power));
          return m;
        });
  }

  public Material flat(int color, float alpha) {
    Material m = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
    ColorRGBA c = color(color);
    c.a = alpha;
    m.setColor("Color", c);
    if (alpha < 1) {
      m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
      m.getAdditionalRenderState().setDepthWrite(false);
    }
    return m;
  }

  public Geometry box(String name, float x, float y, float z, Material mat) {
    Box mesh = new Box(x, y, z);
    TangentBinormalGenerator.generate(mesh);
    Geometry g = new Geometry(name, mesh);
    g.setMaterial(mat);
    return g;
  }

  public Geometry sphere(String name, float radius, Material mat) {
    Sphere mesh = new Sphere(12, 16, radius);
    Geometry g = new Geometry(name, mesh);
    g.setMaterial(mat);
    return g;
  }

  public Spatial model(String id, Supplier<Spatial> fallback) {
    for (String suffix : List.of(".glb", ".gltf", ".j3o", ".obj")) {
      String path = "models/" + id + suffix;
      if (AssetPipeline.class.getClassLoader().getResource(path) != null)
        return manager.loadModel(path);
    }
    return fallback.get();
  }
}
