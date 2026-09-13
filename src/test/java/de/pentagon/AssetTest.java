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
        new CharacterFactory(new AssetPipeline(manager))
            .create("placeholder_probe", 0x556677, false);
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
    for (String id :
        new String[] {"hero", "mira", "eren", "goblin", "orc", "warden", "shaman", "king"}) {
      var rig = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
      var joints = new java.util.HashSet<String>();
      rig.skinning().getArmature().getJointList().forEach(j -> joints.add(j.getName()));
      // A delivered rig may carry more joints than the documented sixteen; what matters is that
      // the named ones exist, because WeaponSocket and ShieldSocket hang off Hand.R / Hand.L.
      for (String joint :
          new String[] {
            "Root",
            "Hips",
            "Spine",
            "Head",
            "UpperArm.L",
            "Forearm.L",
            "Hand.L",
            "UpperArm.R",
            "Forearm.R",
            "Hand.R",
            "Thigh.L",
            "Shin.L",
            "Foot.L",
            "Thigh.R",
            "Shin.R",
            "Foot.R"
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
        // Without this the measurement is of the bind pose, not of the clip: an action blends in
        // from whatever pose the joints hold, and at t = 0 that blend weight is zero. The bind
        // pose faces the front by construction, so the old assertion could never fail.
        ((com.jme3.anim.tween.action.BlendableAction) posed.composer().action(clip))
            .setTransitionLength(0);
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
        flatten(lateral, armature);
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
      // Dodge and Cast are cut to the game's windows too; a 2.33 s roll in a 0.58 s dodge was a
      // crouch that stood up again.
      assertEquals(
          de.pentagon.entities.PlayerController.DODGE_TIME,
          rig.composer().getAnimClip("Dodge").getLength(),
          0.034f,
          id + " Dodge does not match the dodge window");
      assertEquals(
          de.pentagon.entities.PlayerController.CAST_TIME,
          rig.composer().getAnimClip("Cast").getLength(),
          0.034f,
          id + " Cast does not match the cast window");
      // The spin and the finisher end where they started, so the next clip has nothing to snap
      // back from: pelvis line on the last frame, same measure as the first-frame check above.
      for (String clip : new String[] {"Attack2", "Attack3", "Dodge", "Cast"}) {
        var posed = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
        ((com.jme3.anim.tween.action.BlendableAction) posed.composer().action(clip))
            .setTransitionLength(0);
        posed.restart(clip);
        posed.composer().setTime(posed.composer().getAnimClip(clip).getLength() - .001);
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
        flatten(lateral, armature);
        lateral.normalizeLocal();
        assertTrue(
            lateral.x > .94,
            id
                + " "
                + clip
                + " does not end on the front: "
                + Math.round(Math.toDegrees(Math.acos(Math.min(1, lateral.x))))
                + " degrees out");
      }
      // The roll goes straight. "Stand To Roll" is a shoulder roll and yawed the pelvis out to
      // 103 degrees in the middle, so the hero rolled diagonally across the direction the game
      // pushed him; art/blender/anim_pin.py locks the pelvis to the front on every key.
      {
        var posed = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
        ((com.jme3.anim.tween.action.BlendableAction) posed.composer().action("Dodge"))
            .setTransitionLength(0);
        posed.restart("Dodge");
        float length = (float) posed.composer().getAnimClip("Dodge").getLength();
        // Sampled on the clip's own frames (30 fps): the lock holds on every key, and between
        // two keys the Root's and the hips' interpolations may disagree by a few degrees for a
        // thirtieth of a second, which no eye catches.
        for (int step = 0; step * (1 / 30f) < length; step++) {
          posed.composer().setTime(Math.min(length - .001f, step * (1 / 30f)));
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
          flatten(lateral, armature);
          lateral.normalizeLocal();
          assertTrue(
              lateral.x > .94,
              id
                  + " Dodge rolls crooked at step "
                  + step
                  + ": pelvis "
                  + Math.round(Math.toDegrees(Math.acos(Math.min(1, lateral.x))))
                  + " degrees out");
        }
      }
      // The clips animate the joints, the physics capsule moves the character. Attack2 used to
      // carry 3.16 m of forward travel in Hips ("sword and shield attack (2)" is a leaping spin),
      // so the mesh ran three metres ahead of its body and snapped back; every other clip stays
      // within 0.09 m. art/blender/anim_pin.py pins the hips; this holds every clip to it.
      for (String clip : CharacterFactory.CLIPS) {
        var posed = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
        ((com.jme3.anim.tween.action.BlendableAction) posed.composer().action(clip))
            .setTransitionLength(0);
        var hips = posed.skinning().getAttachmentsNode("Hips");
        posed.restart(clip);
        posed.root().updateLogicalState(0);
        posed.root().updateGeometricState();
        Vector3f start = hips.getWorldTranslation().clone();
        float length = (float) posed.composer().getAnimClip(clip).getLength(), drift = 0;
        for (int step = 1; step <= 12; step++) {
          posed.composer().setTime(Math.min(length - .001f, length * step / 12));
          posed.root().updateLogicalState(0);
          posed.root().updateGeometricState();
          Vector3f at = hips.getWorldTranslation();
          drift = Math.max(drift, (float) Math.hypot(at.x - start.x, at.z - start.z));
        }
        assertTrue(
            drift < .35f, id + " " + clip + " carries " + drift + " m of root motion in its hips");
      }
    }
  }

  @Test
  void theHeroCarriesAParryClipThatEndsOnTheFront() {
    // The parry is its own move now: a shield jolt of 15 frames grafted from "sword and shield
    // block (2)". It is optional in the contract - other rigs fall back to Block - but the hero
    // must have it, or the player never sees a successful parry.
    var rig =
        new CharacterFactory(new AssetPipeline(manager)).create("hero", 0x547079, false, true);
    assertTrue(rig.has("Parry"), "hero.glb carries the optional Parry clip");
    assertFalse(
        new CharacterFactory(new AssetPipeline(manager)).create("goblin", 0, false).has("Parry"));
    float length = (float) rig.composer().getAnimClip("Parry").getLength();
    assertEquals(de.pentagon.entities.PlayerController.PARRY_TIME, length, .034f);
    assertTrue(
        ((com.jme3.anim.tween.action.BlendableAction) rig.composer().action("Parry"))
                .getTransitionLength()
            <= .1,
        "a parry snaps in like any other one-shot");
    var posed = new CharacterFactory(new AssetPipeline(manager)).create("hero", 0x547079, false);
    ((com.jme3.anim.tween.action.BlendableAction) posed.composer().action("Parry"))
        .setTransitionLength(0);
    posed.restart("Parry");
    posed.composer().setTime(length - .001);
    posed.root().updateLogicalState(0);
    posed.root().updateGeometricState();
    var armature = posed.skinning().getArmature();
    armature.update();
    var space = posed.skinning().getSpatial().getWorldTransform();
    Vector3f lateral =
        space
            .transformVector(
                armature.getJoint("Thigh.L").getModelTransform().getTranslation(), new Vector3f())
            .subtract(
                space.transformVector(
                    armature.getJoint("Thigh.R").getModelTransform().getTranslation(),
                    new Vector3f()));
    flatten(lateral, armature);
    lateral.normalizeLocal();
    assertTrue(lateral.x > .94, "Parry ends on the front, pelvis line " + lateral);
  }

  @Test
  void aFlameTongueIsClosedAtTheTipAndNarrowAtTheEmbers() {
    // The shrine fire is a lathe with a belly, not a cone: a cone shows a hard rim at its base.
    Mesh tongue = FlameShapes.tongue(9, 10);
    assertEquals(9 * 10 * 2, tongue.getTriangleCount());
    var bound = (com.jme3.bounding.BoundingBox) tongue.getBound();
    assertEquals(1, bound.getYExtent() * 2, 1e-4, "unit height");
    assertTrue(bound.getXExtent() > .8f && bound.getXExtent() <= 1.001f, "belly near unit radius");
    var positions = tongue.getFloatBuffer(VertexBuffer.Type.Position);
    float baseRadius = (float) Math.hypot(positions.get(0), positions.get(2));
    assertTrue(baseRadius < .35f && baseRadius > .2f, "narrow base " + baseRadius);
    int last = (tongue.getVertexCount() - 1) * 3;
    assertEquals(0, Math.hypot(positions.get(last), positions.get(last + 2)), 1e-5, "closed tip");
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

  /**
   * Removes the vertical component of a joint-space vector. The delivered rigs' joint space is
   * Blender's, height along -Z with the skinning spatial at identity (a hips joint at z = -1.16),
   * so zeroing y - the obvious move - zeroed a horizontal axis instead. The facing checks never
   * noticed because at rest the pelvis line is x alone; the roll check did: with the wrong axis a
   * straight forward roll read as 30 to 50 degrees crooked while Blender measured zero. The up axis
   * is read off the rig itself, Root to Hips in the bind pose, so a Y-up rig passes too.
   */
  private static void flatten(Vector3f v, com.jme3.anim.Armature armature) {
    Vector3f up =
        armature
            .getJoint("Hips")
            .getInitialTransform()
            .getTranslation()
            .subtract(armature.getJoint("Root").getInitialTransform().getTranslation());
    if (up.lengthSquared() < 1e-6f) up.set(0, 1, 0);
    up.normalizeLocal();
    v.subtractLocal(up.mult(v.dot(up)));
  }

  /** Hand height over the character origin, world space; the bind pose holds both at shoulder. */
  private static float handHeight(CharacterFactory.Rig rig, String hand) {
    rig.root().updateGeometricState();
    return rig.skinning().getAttachmentsNode(hand).getWorldTranslation().y
        - rig.root().getWorldTranslation().y;
  }

  @Test
  void aFreshRigIsPosedOnItsFirstFrameNotInTheBindPose() {
    // Every character used to enter a region with its arms out and fold into Idle over a third of
    // a second: jME blends a new action in from the current joint pose over 0.4 s, and a fresh
    // rig's current pose is the T-pose. Measured on hero.glb before the fix: both hands at 1.51 m
    // at t = 0, at 1.12 m only from t = 0.3 s. The factory now starts Idle past its blend.
    for (String id : new String[] {"hero", "goblin"}) {
      var rig = new CharacterFactory(new AssetPipeline(manager)).create(id, 0x556677, false);
      rig.root().updateLogicalState(1 / 60f);
      float right = handHeight(rig, "Hand.R"), left = handHeight(rig, "Hand.L");
      assertTrue(
          Math.abs(right - left) > .05f || right < 1.35f,
          id + " still shows the bind pose after one frame: hands at " + right + " / " + left);
    }
  }

  @Test
  void aClipPlayedOnceHoldsItsLastFrame() {
    // Death has to end on the floor, not loop back to standing. The placeholder rig is enough:
    // its Death turns Root a quarter turn and drops it to 0.12 m over 1.3 s.
    var rig =
        new CharacterFactory(new AssetPipeline(manager))
            .create("placeholder_probe", 0x556677, false);
    // Created up front: an attachment node is only placed by the armature update that follows.
    var head = rig.skinning().getAttachmentsNode("Head");
    rig.once("Death");
    assertFalse(rig.finished());
    for (int i = 0; i < 60; i++) rig.root().updateLogicalState(.05f);
    assertTrue(rig.finished(), "a one-shot clip ends");
    rig.root().updateGeometricState();
    float held = head.getWorldTranslation().y;
    for (int i = 0; i < 20; i++) rig.root().updateLogicalState(.05f);
    rig.root().updateGeometricState();
    assertEquals(held, head.getWorldTranslation().y, 1e-4);
    assertTrue(held < 1.0f, "the fallen head stays down, it was at " + held);
    // Looping is still the default for everything else.
    rig.play("Idle");
    rig.root().updateLogicalState(3);
    assertFalse(rig.finished());
  }

  @Test
  void oneShotClipsBlendInFasterThanTheirWindup() {
    // Attack1 lands its hit 0.15 s in; the engine default blend of 0.4 s meant the arm was still
    // mostly in the previous pose when the damage was dealt.
    var rig = new CharacterFactory(new AssetPipeline(manager)).create("hero", 0x556677, false);
    for (String clip :
        new String[] {"Attack1", "Attack2", "Attack3", "Dodge", "Hit", "Death", "Cast"})
      assertTrue(
          ((com.jme3.anim.tween.action.BlendableAction) rig.composer().action(clip))
                  .getTransitionLength()
              <= de.pentagon.combat.AttackTimeline.WINDUP[0],
          clip + " blends in slower than the first hit lands");
  }

  @Test
  void styleLimitsAreEnforcedAtTheMaterialGateway() {
    // docs/assets/STYLE.md: no polished metal under the constant probe. Every procedural
    // material passes through AssetPipeline.pbr, so that is where the limit lives.
    var assets = new AssetPipeline(manager);
    assertThrows(IllegalArgumentException.class, () -> assets.pbr("", 0x939a9e, .38f, .72f));
    assertThrows(IllegalArgumentException.class, () -> assets.pbr("", 0x939a9e, .6f, .5f));
    assertNotNull(assets.pbr("", 0x939a9e, .58f, .38f));
  }

  @Test
  void radioTrackStreamsAndRewindsForLooping() {
    AudioData data = manager.loadAsset(new AudioKey("audio/" + AudioDirector.RADIO + ".wav", true));
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
