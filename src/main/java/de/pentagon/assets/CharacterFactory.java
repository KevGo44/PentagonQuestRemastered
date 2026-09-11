package de.pentagon.assets;

import com.jme3.anim.*;
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
  private final AssetPipeline assets;

  public record Rig(Node root, AnimComposer composer, SkinningControl skinning) {
    public void play(String clip) {
      if (!clip.equals(root.getUserData("clip"))) {
        composer.setCurrentAction(clip);
        root.setUserData("clip", clip);
      }
    }

    public void restart(String clip) {
      composer.setCurrentAction(clip);
      composer.setTime(0);
      root.setUserData("clip", clip);
    }

    public void pause(boolean paused) {
      composer.setGlobalSpeed(paused ? 0 : 1);
    }
  }

  public CharacterFactory(AssetPipeline assets) {
    this.assets = assets;
  }

  public Rig create(String id, int color, boolean king) {
    Spatial loaded = assets.model("characters/" + id, () -> placeholder(color, king));
    Node model = loaded instanceof Node n ? n : new Node("CharacterRoot");
    if (!(loaded instanceof Node)) model.attachChild(loaded);
    AnimComposer composer = find(model, AnimComposer.class);
    SkinningControl skin = find(model, SkinningControl.class);
    if (composer == null || skin == null)
      throw new IllegalStateException(
          "Character asset "
              + id
              + " requires AnimComposer and SkinningControl; see docs/ASSETS.md");
    for (String clip : CLIPS)
      if (!composer.hasAnimClip(clip))
        throw new IllegalStateException(id + " is missing animation " + clip);
    model.setShadowMode(ShadowMode.CastAndReceive);
    Rig rig = new Rig(model, composer, skin);
    rig.play("Idle");
    return rig;
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
        steel = assets.pbr("", 0x939a9e, .38f, .72f),
        cloth = assets.pbr("", king ? 0x672932 : 0x243844, .9f, 0),
        gold = assets.pbr("", 0xd1a05b, .34f, .65f);
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
    Node sword = new Node("WeaponSocket");
    Geometry blade = assets.box("Blade", .055f, .49f, .027f, steel);
    blade.setLocalTranslation(0, -.5f, 0);
    sword.attachChild(blade);
    Geometry hilt = assets.box("Guard", .19f, .04f, .06f, gold);
    hilt.setLocalTranslation(0, -.02f, 0);
    sword.attachChild(hilt);
    skin.getAttachmentsNode("Hand.R").attachChild(sword);
    Node shield = new Node("ShieldSocket");
    Geometry plate = assets.box("Shield", .25f, .34f, .055f, body);
    plate.setLocalTranslation(-.1f, -.05f, .13f);
    shield.attachChild(plate);
    skin.getAttachmentsNode("Hand.L").attachChild(shield);
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
