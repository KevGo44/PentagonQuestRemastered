package de.pentagon.assets;

import com.jme3.anim.*;
import com.jme3.anim.tween.action.BlendableAction;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;
import java.nio.FloatBuffer;
import java.util.*;

/** A real weighted armature, with the same contract as future Blender characters. */
public final class CharacterFactory {
  public static final String[] CLIPS = {
    "Idle", "Walk", "Run", "Attack1", "Attack2", "Attack3", "Dodge", "Block", "Hit", "Death", "Cast"
  };

  /**
   * Clips a rig may carry on top of {@link #CLIPS}. The game uses them where they exist and falls
   * back where they do not: Parry (a shield jolt, hero.glb) stands in for Block on a rig without
   * it.
   */
  public static final String[] OPTIONAL_CLIPS = {"Parry"};

  private final AssetPipeline assets;

  /** Clips that end and are then replaced by whatever the simulation chooses next. */
  private static final Set<String> ONE_SHOTS =
      Set.of("Attack1", "Attack2", "Attack3", "Dodge", "Hit", "Death", "Cast", "Parry");

  /**
   * How long a switch blends from the pose the joints are in towards the new clip. jME's default is
   * 0.4 s for every action, which is longer than the wind-up of Attack1 (0.15 s): the hit landed
   * while the arm was still mostly where the previous clip had left it. Locomotion keeps a soft
   * blend; anything the player triggers snaps quickly.
   */
  private static final double LOCOMOTION_BLEND = .25, ONE_SHOT_BLEND = .1;

  public record Rig(Node root, AnimComposer composer, SkinningControl skinning) {
    /** Loops {@code clip}; a no-op while it is already the current clip. */
    public void play(String clip) {
      if (!clip.equals(root.getUserData("clip"))) {
        composer.setCurrentAction(clip);
        root.setUserData("clip", clip);
      }
    }

    /** Restarts {@code clip} from its first frame, looping, even if it is already playing. */
    public void restart(String clip) {
      composer.setCurrentAction(clip);
      composer.setTime(0);
      root.setUserData("clip", clip);
    }

    /**
     * Plays {@code clip} exactly once and then holds its last frame until the next {@link #play} or
     * {@link #restart}. Death has to end on the floor, not loop back to standing, and an attack
     * should finish its follow-through instead of being cut where the simulation stops caring about
     * it.
     */
    public void once(String clip) {
      composer.setCurrentAction(clip, AnimComposer.DEFAULT_LAYER, false);
      root.setUserData("clip", clip);
    }

    /** True once a clip started with {@link #once} has run to its end. */
    public boolean finished() {
      return composer.getCurrentAction() == null;
    }

    public void pause(boolean paused) {
      composer.setGlobalSpeed(paused ? 0 : 1);
    }

    /** True if the rig carries {@code clip}, required or optional. */
    public boolean has(String clip) {
      return composer.hasAnimClip(clip);
    }
  }

  public CharacterFactory(AssetPipeline assets) {
    this.assets = assets;
  }

  public Rig create(String id, int color, boolean king) {
    return create(id, color, king, false);
  }

  /**
   * @param armed hangs sword and shield on the rig. Only the player carries gear: Mira and Eren are
   *     not fighters, and the enemies bring their own claws and armour with the mesh.
   */
  public Rig create(String id, int color, boolean king, boolean armed) {
    Spatial loaded = assets.model("characters/" + id, () -> placeholder(color, king));
    Node model = loaded instanceof Node n ? n : new Node("CharacterRoot");
    if (!(loaded instanceof Node)) model.attachChild(loaded);
    AnimComposer composer = find(model, AnimComposer.class);
    SkinningControl skin = find(model, SkinningControl.class);
    if (composer == null || skin == null)
      throw new IllegalStateException(
          "Character asset "
              + id
              + " requires AnimComposer and SkinningControl; see docs/assets/ASSETS.md");
    for (String clip : CLIPS)
      if (!composer.hasAnimClip(clip))
        throw new IllegalStateException(id + " is missing animation " + clip);
    model.setShadowMode(ShadowMode.CastAndReceive);
    if (armed) equip(skin, color);
    List<String> clips = new ArrayList<>(List.of(CLIPS));
    for (String clip : OPTIONAL_CLIPS) if (composer.hasAnimClip(clip)) clips.add(clip);
    for (String clip : clips)
      if (composer.action(clip) instanceof BlendableAction action)
        action.setTransitionLength(ONE_SHOTS.contains(clip) ? ONE_SHOT_BLEND : LOCOMOTION_BLEND);
    Rig rig = new Rig(model, composer, skin);
    rig.play("Idle");
    // A blend starts from the pose the joints are in, and a fresh rig is in its bind pose: every
    // character entered a region with arms out and folded into Idle over the first third of a
    // second, measured on hero.glb (hands at 1.51 m at t=0, at 1.12 m from t=0.3 s). Starting
    // Idle past its blend skips that; Idle is a loop, so where it starts is invisible.
    composer.setTime(LOCOMOTION_BLEND);
    return rig;
  }

  // The delivered rigs face the glTF front, which is where the engine points a character with
  // Quaternion.lookAt. That was not always true: every clip carried the side-on guard stance of
  // Mixamo's sword-and-shield set, about 53 degrees out of the front, while walk and run faced
  // straight ahead - so the character stood sideways and walked forwards. A quarter turn in this
  // factory papered over the walk and made everything else worse. The repair belongs in the asset
  // and sits in art/blender/anim_normalise.py; AssetTest measures the result on every clip.

  /** Distance from the wrist joint into the middle of the fist, measured on the delivered rigs. */
  private static final float FIST = .095f;

  /**
   * Hangs sword and shield on the rig. This used to sit inside {@link #placeholder}, which meant
   * every delivered GLB arrived unarmed: the placeholder only runs when no model file exists, so
   * shipping hero.glb silently removed the weapon. The sockets belong to the character, not to the
   * placeholder mesh.
   */
  private void equip(SkinningControl skin, int tint) {
    boolean right = false, left = false;
    for (Joint j : skin.getArmature().getJointList()) {
      if (j.getName().equals("Hand.R")) right = true;
      if (j.getName().equals("Hand.L")) left = true;
    }
    Material steel = assets.pbr("", 0x939a9e, .58f, .38f),
        gold = assets.pbr("", 0xd1a05b, .55f, .4f),
        body = assets.pbr("", tint, .75f, .12f);
    // Both hands run their local +Y along the fingers, +X out past the thumb and +Z out of the
    // palm. Measured on the delivered rigs: the four finger joints sit within nine degrees of +Y,
    // the index at +X and the pinky at -X, and the thumb stands off towards +Z.
    if (right) {
      // A fist holds the grip across the palm, not along the fingers, so the blade leaves the hand
      // along its local X - and on the thumb side (+X), which is the ordinary grip: pommel by the
      // little finger, blade up and forward. The mirror of this, -X, is the reverse grip with the
      // point hanging down, and that is how the hero held it until Kevin asked. The second quarter
      // turn is about the blade itself and puts the flats to the sides.
      Node sword = new Node("WeaponSocket");
      sword.attachChild(assets.model("props/sword", () -> swordFallback(steel, gold)));
      sword.setLocalRotation(
          new Quaternion()
              .fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Z)
              .mult(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y)));
      sword.setLocalTranslation(0, FIST, .015f);
      skin.getAttachmentsNode("Hand.R").attachChild(sword);
    }
    if (left) {
      // The shield sits across the end of the arm and looks where the arm points - that is how a
      // boss shield is carried, and it is also the only mounting that faces the front. Two wrong
      // answers came first: on the palm (the model's own +Z) it looked at the character's ribs,
      // because Mixamo's set turns the left palm inwards; on the back of the hand it looked
      // sideways. Searched over every direction in the hand's frame and scored against the front
      // across Idle, Walk, Run, Block and Hit, the forearm axis wins by a wide margin - mean cover
      // 0.84 to 0.93 where the palm gives -0.50 to -0.11 and the back of the hand 0.11 to 0.50.
      // The forearm runs back from the wrist, so it stays wholly behind the disc.
      Node shield = new Node("ShieldSocket");
      shield.attachChild(assets.model("props/shield", () -> shieldFallback(body)));
      shield.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
      shield.setLocalTranslation(0, .175f, 0);
      skin.getAttachmentsNode("Hand.L").attachChild(shield);
    }
  }

  /** Same convention as props/sword.glb: grip at the origin, blade along +Y. */
  private Spatial swordFallback(Material steel, Material gold) {
    Node sword = new Node("SwordFallback");
    Geometry blade = assets.box("Blade", .04f, .36f, .012f, steel);
    blade.setLocalTranslation(0, .45f, 0);
    sword.attachChild(blade);
    Geometry guard = assets.box("Guard", .12f, .022f, .03f, gold);
    guard.setLocalTranslation(0, .095f, 0);
    sword.attachChild(guard);
    Geometry grip = assets.box("Grip", .022f, .085f, .022f, gold);
    sword.attachChild(grip);
    return sword;
  }

  /** Same convention as props/shield.glb: boss at the origin, face along +Z. */
  private Spatial shieldFallback(Material body) {
    Geometry plate = assets.box("ShieldFallback", .31f, .31f, .022f, body);
    plate.setLocalTranslation(0, 0, -.03f);
    return plate;
  }

  private <T extends com.jme3.scene.control.Control> T find(Spatial s, Class<T> type) {
    T c = s.getControl(type);
    if (c != null) return c;
    if (s instanceof Node n)
      for (Spatial child : n.getChildren()) {
        T found = find(child, type);
        if (found != null) return found;
      }
    return null;
  }

  private Node placeholder(int tint, boolean king) {
    Node model = new Node("CharacterRoot");
    List<Joint> list = new ArrayList<>();
    Joint root = joint(list, "Root", null, 0, 0, 0),
        hips = joint(list, "Hips", root, 0, .93f, 0),
        spine = joint(list, "Spine", hips, 0, .34f, 0);
    Joint head = joint(list, "Head", spine, 0, .47f, 0);
    Joint al = joint(list, "UpperArm.L", spine, -.42f, .15f, 0),
        fl = joint(list, "Forearm.L", al, 0, -.35f, 0),
        hl = joint(list, "Hand.L", fl, 0, -.28f, 0);
    Joint ar = joint(list, "UpperArm.R", spine, .42f, .15f, 0),
        fr = joint(list, "Forearm.R", ar, 0, -.35f, 0),
        hr = joint(list, "Hand.R", fr, 0, -.28f, 0);
    Joint tl = joint(list, "Thigh.L", hips, -.19f, -.05f, 0),
        sl = joint(list, "Shin.L", tl, 0, -.4f, 0),
        fol = joint(list, "Foot.L", sl, 0, -.38f, .06f);
    Joint tr = joint(list, "Thigh.R", hips, .19f, -.05f, 0),
        sr = joint(list, "Shin.R", tr, 0, -.4f, 0),
        fori = joint(list, "Foot.R", sr, 0, -.38f, .06f);
    Armature armature = new Armature(list.toArray(Joint[]::new));
    armature.update();
    armature.saveBindPose();
    armature.saveInitialPose();
    Material body = assets.pbr("", tint, .75f, .12f),
        steel = assets.pbr("", 0x939a9e, .58f, .38f),
        cloth = assets.pbr("", king ? 0x672932 : 0x243844, .9f, 0),
        gold = assets.pbr("", 0xd1a05b, .55f, .4f);
    part(model, hips, .28f, .16f, .17f, 0, 0, 0, cloth);
    part(model, spine, .33f, .28f, .2f, 0, .02f, 0, body);
    part(model, head, .21f, .22f, .20f, 0, 0, 0, steel);
    part(
        model,
        head,
        .19f,
        .035f,
        .015f,
        0,
        -.015f,
        .211f,
        assets.glow(king ? 0xfc5936 : 0x6ce8ee, .3f));
    for (Joint arm : List.of(al, ar)) {
      part(model, arm, .16f, .12f, .19f, 0, 0, 0, steel);
      part(model, arm, .105f, .19f, .11f, 0, -.17f, 0, body);
    }
    for (Joint fore : List.of(fl, fr)) part(model, fore, .09f, .16f, .1f, 0, -.14f, 0, steel);
    for (Joint leg : List.of(tl, tr)) part(model, leg, .125f, .22f, .14f, 0, -.2f, 0, cloth);
    for (Joint shin : List.of(sl, sr)) part(model, shin, .13f, .17f, .15f, 0, -.18f, 0, steel);
    for (Joint foot : List.of(fol, fori))
      part(model, foot, .14f, .09f, .22f, 0, -.01f, .08f, cloth);
    part(model, spine, .30f, .5f, .035f, 0, -.20f, -.23f, cloth);
    if (king)
      for (int i = -1; i <= 1; i++) part(model, head, .045f, .18f, .045f, i * .17f, .28f, 0, gold);
    AnimComposer composer = new AnimComposer();
    for (String clip : CLIPS) composer.addAnimClip(animation(clip, list));
    model.addControl(composer);
    SkinningControl skin = new SkinningControl(armature);
    model.addControl(skin);
    return model;
  }

  private Joint joint(List<Joint> list, String name, Joint parent, float x, float y, float z) {
    Joint j = new Joint(name);
    j.setLocalTranslation(new Vector3f(x, y, z));
    if (parent != null) parent.addChild(j);
    list.add(j);
    return j;
  }

  private void part(
      Node model,
      Joint joint,
      float x,
      float y,
      float z,
      float ox,
      float oy,
      float oz,
      Material material) {
    boolean rounded = y > .10f && z > .08f;
    Mesh box = rounded ? new Sphere(8, 12, 1) : new Box(x, y, z);
    Vector3f at = joint.getModelTransform().getTranslation().add(ox, oy, oz);
    FloatBuffer positions = box.getFloatBuffer(Type.Position);
    for (int i = 0; i < positions.limit(); i += 3) {
      positions.put(i, positions.get(i) * (rounded ? x : 1) + at.x);
      positions.put(i + 1, positions.get(i + 1) * (rounded ? y : 1) + at.y);
      positions.put(i + 2, positions.get(i + 2) * (rounded ? z : 1) + at.z);
    }
    if (rounded) {
      FloatBuffer normals = box.getFloatBuffer(Type.Normal);
      for (int i = 0; i < normals.limit(); i += 3) {
        Vector3f normal =
            new Vector3f(normals.get(i) / x, normals.get(i + 1) / y, normals.get(i + 2) / z)
                .normalizeLocal();
        normals.put(i, normal.x);
        normals.put(i + 1, normal.y);
        normals.put(i + 2, normal.z);
      }
    }
    float[] weights = new float[box.getVertexCount() * 4];
    byte[] indices = new byte[weights.length];
    for (int i = 0; i < weights.length; i += 4) {
      weights[i] = 1;
      indices[i] = (byte) joint.getId();
    }
    if (!rounded) TangentBinormalGenerator.generate(box);
    box.setBuffer(Type.BoneWeight, 4, weights);
    box.setBuffer(Type.BoneIndex, 4, indices);
    box.setBuffer(new VertexBuffer(Type.HWBoneIndex));
    box.setBuffer(new VertexBuffer(Type.HWBoneWeight));
    box.setMaxNumWeights(1);
    box.generateBindPose();
    box.updateBound();
    Geometry g = new Geometry(joint.getName(), box);
    g.setMaterial(material);
    model.attachChild(g);
  }

  private AnimClip animation(String name, List<Joint> joints) {
    float duration =
        switch (name) {
          case "Walk" -> .9f;
          case "Run" -> .62f;
          case "Attack1" -> .52f;
          case "Attack2" -> .58f;
          case "Attack3" -> .78f;
          case "Dodge" -> .6f;
          case "Hit" -> .32f;
          case "Death" -> 1.3f;
          case "Cast" -> .6f;
          default -> 2.4f;
        };
    List<AnimTrack<?>> tracks = new ArrayList<>();
    int frames = 9;
    for (Joint j : joints) {
      float[] times = new float[frames];
      Vector3f[] positions = new Vector3f[frames];
      Quaternion[] rotations = new Quaternion[frames];
      for (int k = 0; k < frames; k++) {
        float t = k / (frames - 1f), wave = FastMath.sin(t * FastMath.TWO_PI);
        times[k] = t * duration;
        positions[k] = j.getLocalTranslation().clone();
        float rx = 0, ry = 0, rz = 0;
        String n = j.getName();
        if (name.equals("Idle") && n.equals("Spine")) rx = wave * .025f;
        if (name.equals("Walk") || name.equals("Run")) {
          float amplitude = name.equals("Run") ? .8f : .48f;
          if (n.startsWith("Thigh")) rx = wave * amplitude * (n.endsWith("L") ? 1 : -1);
          if (n.startsWith("Shin")) rx = Math.max(0, -wave * (n.endsWith("L") ? 1 : -1)) * .7f;
          if (n.startsWith("UpperArm")) rx = wave * .4f * (n.endsWith("L") ? -1 : 1);
          if (n.equals("Hips")) positions[k].y += Math.abs(wave) * .045f;
        }
        if (name.startsWith("Attack")) {
          float strike = FastMath.sin(t * FastMath.PI);
          int variant = name.charAt(6) - '0';
          if (n.equals("UpperArm.R")) {
            rx = -strike * 2.4f;
            rz = strike * (variant == 2 ? 1.2f : -.5f);
          }
          if (n.equals("Spine")) ry = strike * (variant == 2 ? -.6f : .55f);
        }
        if (name.equals("Dodge")) {
          if (n.equals("Hips")) {
            rx = t * FastMath.TWO_PI;
            positions[k].y -= FastMath.sin(t * FastMath.PI) * .32f;
          }
          if (n.startsWith("Thigh")) rx = -.9f;
          if (n.startsWith("Shin")) rx = 1.4f;
        }
        if (name.equals("Block") && n.equals("UpperArm.L")) {
          rx = -1.05f;
          rz = -.3f;
        }
        if (name.equals("Hit") && n.equals("Spine")) rx = -FastMath.sin(t * FastMath.PI) * .5f;
        if (name.equals("Death") && n.equals("Root")) {
          rx = -Math.min(1, t * 2) * FastMath.HALF_PI;
          positions[k].y = .12f;
        }
        if (name.equals("Cast") && n.equals("UpperArm.L")) rx = -FastMath.sin(t * FastMath.PI) * 2;
        rotations[k] = new Quaternion().fromAngles(rx, ry, rz);
      }
      tracks.add(new TransformTrack(j, times, positions, rotations, null));
    }
    AnimClip clip = new AnimClip(name);
    clip.setTracks(tracks.toArray(AnimTrack[]::new));
    return clip;
  }
}
