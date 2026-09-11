package de.pentagon.assets;

import com.jme3.light.LightProbe;
import com.jme3.math.Vector3f;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/** Constant-radiance placeholder probe: valid diffuse SH and prefiltered specular cubemap. */
public final class EnvironmentLighting {
  private EnvironmentLighting() {}

  public static LightProbe dungeonProbe() {
    ArrayList<ByteBuffer> faces = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      ByteBuffer face = BufferUtils.createByteBuffer(3);
      face.put((byte) 18).put((byte) 24).put((byte) 32).flip();
      faces.add(face);
    }
    TextureCubeMap environment =
        new TextureCubeMap(new Image(Image.Format.RGB8, 1, 1, 0, faces, ColorSpace.Linear));
    environment.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
    environment.setMagFilter(Texture.MagFilter.Bilinear);
    Vector3f[] sh = new Vector3f[9];
    for (int i = 0; i < 9; i++) sh[i] = new Vector3f();
    sh[0].set(.16f, .21f, .27f);
    LightProbe probe = new LightProbe();
    probe.setPosition(new Vector3f(42, 3, 42));
    probe.getArea().setRadius(150);
    probe.setPrefilteredMap(environment);
    probe.setNbMipMaps(1);
    probe.setShCoeffs(sh);
    probe.setReady(true);
    return probe;
  }
}
