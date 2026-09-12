package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.AssetManager;
import com.jme3.audio.*;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.system.JmeSystem;
import de.pentagon.assets.*;
import de.pentagon.audio.AudioDirector;
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
    // An id with no asset on the classpath, so this keeps exercising the procedural fallback
    // even once every character has been replaced by a real model.
    var rig =
        new CharacterFactory(new AssetPipeline(manager)).create("placeholder_probe", 0x556677, false);
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
  void deliveredCharacterAssetsSatisfyTheContract() {
    // Only the ids that actually ship a GLB. Every further character has to be added here when
    // it lands, otherwise nothing checks it before the game starts.
    for (String id : new String[] {
          "hero", "mira", "eren", "goblin", "orc", "warden", "shaman", "king"
        }) {
      var rig = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
      var joints = new java.util.HashSet<String>();
      rig.skinning().getArmature().getJointList().forEach(j -> joints.add(j.getName()));
      // A delivered rig may carry more joints than the documented sixteen; what matters is that
      // the named ones exist, because WeaponSocket and ShieldSocket hang off Hand.R / Hand.L.
      for (String joint :
          new String[] {
            "Root", "Hips", "Spine", "Head",
            "UpperArm.L", "Forearm.L", "Hand.L",
            "UpperArm.R", "Forearm.R", "Hand.R",
            "Thigh.L", "Shin.L", "Foot.L",
            "Thigh.R", "Shin.R", "Foot.R"
          }) {
        assertTrue(joints.contains(joint), id + " is missing contract joint " + joint);
      }
      // A clip that exists but carries no tracks would still satisfy hasAnimClip, and a mesh
      // without a bind pose is skipped by the skinning pass and renders undeformed. Both fail
      // silently at runtime, so assert them here.
      for (String clip : CharacterFactory.CLIPS) {
        assertTrue(rig.composer().hasAnimClip(clip), id + " is missing animation " + clip);
        var animClip = rig.composer().getAnimClip(clip);
        assertNotNull(animClip, id + " " + clip + " is absent");
        assertTrue(animClip.getTracks().length > 0, id + " " + clip + " carries no tracks");
        assertTrue(animClip.getLength() > 0, id + " " + clip + " has zero length");
        // "Clip present" is not "clip animated". hero.glb once shipped with all 66 tracks sampled
        // from the rest pose: right key counts, identical values, and every assertion above
        // passed it while the character stood frozen in the game. The weakest real clip is Block,
        // a held stance that still sways 2 degrees, so half a degree separates held from frozen.
        double swing = 0;
        for (var track : animClip.getTracks()) {
          if (!(track instanceof com.jme3.anim.TransformTrack transform)) continue;
          var rotations = transform.getRotations();
          if (rotations == null || rotations.length < 2) continue;
          for (int r = 1; r < rotations.length; r++) {
            float dot = Math.abs(rotations[0].dot(rotations[r]));
            swing = Math.max(swing, Math.toDegrees(2 * Math.acos(Math.min(1, dot))));
          }
        }
        assertTrue(
            swing > .5,
            id + " " + clip + " is frozen: largest joint rotation is " + swing + " degrees");
      }
      rig.root()
          .depthFirstTraversal(
              s -> {
                if (s instanceof Geometry g && g.getMesh().isAnimated()) {
                  Mesh mesh = g.getMesh();
                  assertNotNull(
                      mesh.getBuffer(VertexBuffer.Type.BindPosePosition),
                      g.getName() + " has no bind pose, so skinning would be skipped");
                  assertNotNull(mesh.getBuffer(VertexBuffer.Type.BoneIndex));
                  assertNotNull(mesh.getBuffer(VertexBuffer.Type.BoneWeight));
                }
              });
      // Which way does the character face? Every assertion above passes while he walks sideways:
      // the joints are there, the clips run, the lengths match. Mixamo's sword-and-shield set is
      // authored in a side-on guard stance about 53 degrees out of the front while its walk and run
      // face straight ahead, so a character built from both stood still sideways and walked
      // forwards. art/blender/anim_normalise.py turns every clip onto the front; this is the guard.
      //
      // The pelvis is what gets measured. Thigh.L and Thigh.R hang off Hips at fixed offsets, so
      // the line between them is the pelvis orientation and nothing the legs do can turn it - the
      // shoulder line, the toe direction and the hips' own frame were all tried first and all three
      // swing by 60 degrees or more across the clips. With the glTF front on +Z, the left hip sits
      // on +X, and all eleven clips measure that to a tenth of a degree at their first frame.
      for (String clip : CharacterFactory.CLIPS) {
        var posed = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
        posed.restart(clip);
        posed.root().updateLogicalState(0);
        posed.root().updateGeometricState();
        var armature = posed.skinning().getArmature();
        armature.update();
        var space = posed.skinning().getSpatial().getWorldTransform();
        Vector3f lateral =
            space
                .transformVector(
                    armature.getJoint("Thigh.L").getModelTransform().getTranslation(),
                    new Vector3f())
                .subtract(
                    space.transformVector(
                        armature.getJoint("Thigh.R").getModelTransform().getTranslation(),
                        new Vector3f()));
        lateral.y = 0;
        lateral.normalizeLocal();
        assertTrue(
            lateral.x > .97,
            id
                + " "
                + clip
                + " does not start on the front: the pelvis line is "
                + lateral
                + ", which is "
                + Math.round(Math.toDegrees(Math.acos(Math.min(1, lateral.x))))
                + " degrees out");
      }
      // The combo reads its lengths from AttackTimeline, so a clip of the wrong length either
      // cuts the swing off or leaves the character posed after the hit window closed. One frame
      // at 30 fps is 0.034 s, so that is the tolerance a retimed clip can actually hold.
      for (int i = 0; i < 3; i++) {
        assertEquals(
            de.pentagon.combat.AttackTimeline.DURATION[i],
            rig.composer().getAnimClip("Attack" + (i + 1)).getLength(),
            0.034f,
            id + " Attack" + (i + 1) + " does not match AttackTimeline");
      }
    }
  }

  @Test
  void onlyTheArmedHeroCarriesGearAndItSitsInTheFist() {
    var assets = new AssetPipeline(manager);
    var armed = new CharacterFactory(assets).create("hero", 0x547079, false, true);
    var plain = new CharacterFactory(assets).create("mira", 0x50747b, false);
    armed.root().updateGeometricState();
    plain.root().updateGeometricState();
    // Mira and Eren are not fighters: they went through the same factory and once carried a sword
    // and a shield because the sockets were hung on every character it built.
    assertNull(plain.skinning().getAttachmentsNode("Hand.R").getChild("WeaponSocket"));
    assertNull(plain.skinning().getAttachmentsNode("Hand.L").getChild("ShieldSocket"));
    var sword = armed.skinning().getAttachmentsNode("Hand.R").getChild("WeaponSocket");
    var shield = armed.skinning().getAttachmentsNode("Hand.L").getChild("ShieldSocket");
    assertNotNull(sword, "The player carries a sword");
    assertNotNull(shield, "The player carries a shield");
    assertTrue(sword.getTriangleCount() > 100, "props/sword.glb must be the real model");
    assertTrue(shield.getTriangleCount() > 100, "props/shield.glb must be the real model");
    // The grip belongs in the fist, not at the wrist joint and not a metre away from it. Both
    // props are authored grip-at-origin, so the socket offset is the distance that is checked.
    for (Spatial gear : new Spatial[] {sword, shield}) {
      float reach = gear.getLocalTranslation().length();
      assertTrue(
          reach > .05f && reach < .25f,
          gear.getName() + " sits " + reach + " m from the wrist, which is not inside the fist");
    }
  }

  @Test
  void radioTrackStreamsAndRewindsForLooping() {
    AudioData data =
        manager.loadAsset(new AudioKey("audio/" + AudioDirector.RADIO + ".wav", true));
    assertInstanceOf(AudioStream.class, data, "The long track must not be loaded into a buffer");
    AudioStream stream = (AudioStream) data;
    // A looping stream is rewound through SeekableStream.setTime(0); without this the track
    // would play once and then fall silent.
    assertTrue(stream.isSeekable());
    assertEquals(2, stream.getChannels());
    assertTrue(stream.getDuration() > 60, "Expected a full-length music track");
    assertDoesNotThrow(() -> stream.setTime(0));
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
