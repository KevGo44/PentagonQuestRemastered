package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.AssetManager;
import com.jme3.scene.*;
import com.jme3.system.JmeSystem;
import de.pentagon.assets.*;
import org.junit.jupiter.api.*;

class AssetTest {
  private AssetManager manager;

  @BeforeEach
  void setup() {
    System.setProperty("java.awt.headless", "true");
    manager = JmeSystem.newAssetManager(JmeSystem.getPlatformAssetConfigURL());
  }

  @Test
  void referenceGltfIsImportedAsRealGeometry() {
    Spatial model = manager.loadModel("models/props/crystal.gltf");
    model.updateGeometricState();
    assertNotNull(model.getWorldBound());
    assertTrue(model.getTriangleCount() >= 8);
  }

  @Test
  void placeholderHasGpuSkinningBuffersAndAllActions() {
    var rig = new CharacterFactory(new AssetPipeline(manager)).create("hero", 0x556677, false);
    assertEquals(16, rig.skinning().getArmature().getJointCount());
    for (String clip : CharacterFactory.CLIPS) {
      rig.restart(clip);
      rig.root().updateLogicalState(.05f);
      assertTrue(rig.composer().hasAnimClip(clip));
    }
    rig.root()
        .depthFirstTraversal(
            s -> {
              if (s instanceof Geometry g && g.getMesh().isAnimated()) {
                Mesh mesh = g.getMesh();
                assertNotNull(mesh.getBuffer(VertexBuffer.Type.BindPosePosition));
                mesh.prepareForAnim(false);
                assertNotNull(mesh.getBuffer(VertexBuffer.Type.HWBoneIndex).getData());
              }
            });
  }

  @Test
  void customPostFilterAcceptsEngineSamplingParameters() {
    var material = new com.jme3.material.Material(manager, "shaders/Atmosphere.j3md");
    material.clearParam("NumSamples");
    material.clearParam("NumSamplesDepth");
  }

  @Test
  void probeHasCompleteCubemapAndDiffuseCoefficients() {
    var probe = EnvironmentLighting.dungeonProbe();
    assertEquals(6, probe.getPrefilteredEnvMap().getImage().getData().size());
    assertEquals(9, probe.getShCoeffs().length);
    assertTrue(probe.isReady());
  }
}
