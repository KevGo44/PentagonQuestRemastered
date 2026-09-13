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

  /**
   * docs/style-reference/STYLE.md: the probe is a 1x1 constant cubemap, so polished metal has
   * nothing to reflect and falls to black. Every procedural material passes through here, so the
   * limit is enforced here rather than remembered at each call site.
   */
  public static final float STYLE_MIN_ROUGHNESS = .55f, STYLE_MAX_METALLIC = .4f;

  public Material pbr(String surface, int tint, float roughness, float metallic) {
    if (roughness < STYLE_MIN_ROUGHNESS || metallic > STYLE_MAX_METALLIC)
      throw new IllegalArgumentException(
          String.format(
              java.util.Locale.ROOT,
              "Material %s roughness %.2f / metallic %.2f is outside STYLE.md (>= %.2f / <= %.2f)",
              surface.isBlank() ? Integer.toHexString(tint) : surface,
              roughness,
              metallic,
              STYLE_MIN_ROUGHNESS,
              STYLE_MAX_METALLIC));
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

  /**
   * A lit, glowing crystal: dark body of the given hue, emissive in the same hue so the bloom pass
   * picks it up (PBRLighting's Glow technique reads Emissive). Facets show under the torches where
   * the unshaded glow material was a flat cut-out. Within STYLE.md: rough 0.6, no metal.
   */
  public Material crystal(int color) {
    return materials.computeIfAbsent(
        "crystal" + color,
        k -> {
          Material m = new Material(manager, "Common/MatDefs/Light/PBRLighting.j3md");
          ColorRGBA c = color(color);
          m.setColor("BaseColor", new ColorRGBA(c.r * .3f, c.g * .3f, c.b * .3f, 1));
          m.setFloat("Roughness", .6f);
          m.setFloat("Metallic", 0);
          // Emissive below the clip so the hue survives; at power 2.2 every shard burnt to white.
          m.setColor("Emissive", c);
          m.setFloat("EmissivePower", 1.3f);
          m.setFloat("EmissiveIntensity", .6f);
          return m;
        });
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

  /**
   * A tongue of fire: unshaded, additive, feeding the bloom pass through GlowColor. Additive
   * blending is what lets three overlapping cones read as one luminous flame instead of three
   * painted shapes; the alpha thins the outer layers.
   */
  public Material flame(int color, float power, float alpha) {
    Material m = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
    ColorRGBA c = color(color);
    c.a = alpha;
    m.setColor("Color", c);
    m.setColor("GlowColor", color(color).mult(power));
    m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
    m.getAdditionalRenderState().setDepthWrite(false);
    return m;
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

  private static String path(String id) {
    for (String suffix : List.of(".glb", ".gltf", ".j3o", ".obj")) {
      String candidate = "models/" + id + suffix;
      if (AssetPipeline.class.getClassLoader().getResource(candidate) != null) return candidate;
    }
    return null;
  }

  public Spatial model(String id, Supplier<Spatial> fallback) {
    String path = path(id);
    return path == null ? fallback.get() : manager.loadModel(path);
  }
}
