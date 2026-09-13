"""Third pass over the character clips: the spin stays on the spot, and the hero learns to parry.

Runs on the GLBs in src/main/resources (already polished by anim_polish.py) and writes to
art/gen/pinned. Measured with art/probe/ClipTimeline (column hipsDrift) against the polished files:

* Attack2 carried 3.16 m of forward travel in the Hips bone - "sword and shield attack (2)" is a
  leaping spin, and the graft kept its root motion. The physics capsule moves 1.1 m/s during an
  attack, so the mesh ran three metres ahead of the body and snapped back at the end. Every other
  clip stays within 0.09 m. This pass pins the Hips' horizontal world position to its first frame
  for every clip whose hips travel more than PIN_ABOVE metres, frame by frame, in the bone's own
  space, so the spin turns where the hero stands; the legs keep their motion and slide a little
  over the 0.567 s, which is what a pivot looks like. On the hero that is Attack2 alone; Mira's
  rotation-retargeted Dodge carried 1.2 m as well, and AssetTest now holds every clip of every
  rig to 0.35 m.
* Parry (hero only): "sword and shield block (2)" - a shield jolt, 16 frames at 30 fps - grafted
  as an extra, optional clip named Parry. CharacterFactory.CLIPS does not require it; the game
  uses it when it is there.
"""

import bpy, os, math, sys
from mathutils import Quaternion, Vector

CLIPS = ["Idle", "Walk", "Run", "Attack1", "Attack2", "Attack3",
         "Dodge", "Block", "Hit", "Death", "Cast"]
FPS = 30
PIN_ABOVE = .3
# name -> [(clip, source fbx)] grafted in addition to CLIPS
EXTRA = {"hero": [("Parry", "sword and shield block (2).fbx")]}
RENAME = {"UpperArm.L": "LeftArm", "Forearm.L": "LeftForeArm", "Hand.L": "LeftHand",
          "UpperArm.R": "RightArm", "Forearm.R": "RightForeArm", "Hand.R": "RightHand",
          "Thigh.L": "LeftUpLeg", "Shin.L": "LeftLeg", "Foot.L": "LeftFoot",
          "Thigh.R": "RightUpLeg", "Shin.R": "RightLeg", "Foot.R": "RightFoot"}


def curves(action):
    return [fc for layer in action.layers for strip in layer.strips
            for bag in strip.channelbags for fc in bag.fcurves]


def drive(arm, action):
    arm.animation_data.action = action
    if action.slots:
        arm.animation_data.action_slot = action.slots[0]


def at(frame):
    whole = int(math.floor(frame))
    bpy.context.scene.frame_set(whole, subframe=frame - whole)


def pelvis_yaw(arm, frame):
    at(frame)
    left = arm.matrix_world @ arm.pose.bones["Thigh.L"].head
    right = arm.matrix_world @ arm.pose.bones["Thigh.R"].head
    lateral = left - right
    lateral.z = 0
    lateral.normalize()
    return math.degrees(math.atan2(lateral.y, lateral.x))


def root_curves(action):
    rot = sorted((fc for fc in curves(action)
                  if fc.data_path == 'pose.bones["Root"].rotation_quaternion'),
                 key=lambda fc: fc.array_index)
    loc = sorted((fc for fc in curves(action) if fc.data_path == 'pose.bones["Root"].location'),
                 key=lambda fc: fc.array_index)
    if len(rot) != 4:
        raise SystemExit(f"{action.name}: Root carries {len(rot)} rotation curves, expected 4")
    return rot, loc


def turn_root(action, degrees_at):
    rot, loc = root_curves(action)
    for i in range(len(rot[0].keyframe_points)):
        turn = Quaternion((0, 0, 1), math.radians(degrees_at(rot[0].keyframe_points[i].co.x)))
        new = turn @ Quaternion([fc.keyframe_points[i].co.y for fc in rot])
        for fc, value in zip(rot, new):
            k = fc.keyframe_points[i]
            k.co.y = k.handle_left.y = k.handle_right.y = value
    if len(loc) == 3:
        for i in range(len(loc[0].keyframe_points)):
            x = loc[0].keyframe_points[i].co.x
            matrix = Quaternion((0, 0, 1), math.radians(degrees_at(x))).to_matrix()
            new = matrix @ Vector([fc.keyframe_points[i].co.y for fc in loc])
            for fc, value in zip(loc, new):
                k = fc.keyframe_points[i]
                k.co.y = k.handle_left.y = k.handle_right.y = value
    for fc in rot + loc:
        fc.update()


def face_front(arm, action, ramp):
    drive(arm, action)
    start, end = action.frame_range
    first = pelvis_yaw(arm, start)
    turn_root(action, lambda x: first)
    residual = 0.0
    if ramp and end > start:
        residual = pelvis_yaw(arm, end)
        turn_root(action, lambda x: residual * (x - start) / (end - start))
    return first, residual


def retime_from_zero(action):
    """Shifts the keys so the clip starts at frame 0 (Mixamo exports start at 1)."""
    start, _ = action.frame_range
    for fc in curves(action):
        for k in fc.keyframe_points:
            k.co.x -= start
            k.handle_left.x -= start
            k.handle_right.x -= start
        fc.update()


def graft(arm, fbx, clip):
    known = {o for o in bpy.data.objects}
    bpy.ops.import_scene.fbx(filepath=fbx, automatic_bone_orientation=False, ignore_leaf_bones=True)
    source = next(o for o in bpy.data.objects if o not in known and o.type == "ARMATURE")
    action = source.animation_data.action
    start, end = (int(round(v)) for v in action.frame_range)
    bones = {b.name for b in source.data.bones}
    for pb in arm.pose.bones:
        wanted = "mixamorig:" + RENAME.get(pb.name, pb.name)
        if wanted not in bones:
            continue
        constraint = pb.constraints.new("COPY_TRANSFORMS")
        constraint.target = source
        constraint.subtarget = wanted
    bpy.context.scene.frame_start, bpy.context.scene.frame_end = start, end
    bpy.context.view_layer.objects.active = arm
    arm.select_set(True)
    bpy.ops.object.mode_set(mode="POSE")
    bpy.ops.nla.bake(frame_start=start, frame_end=end, step=1, only_selected=False,
                     visual_keying=True, clear_constraints=True, clear_parents=False,
                     use_current_action=False, clean_curves=False, bake_types={"POSE"})
    bpy.ops.object.mode_set(mode="OBJECT")
    baked = arm.animation_data.action
    old = bpy.data.actions.get(clip)
    if old is not None:
        old.name = clip + "_replaced"
    baked.name = clip
    for obj in [o for o in bpy.data.objects if o not in known]:
        bpy.data.objects.remove(obj, do_unlink=True)
    print(f"[G] grafted {clip} from {os.path.basename(fbx)}: {end - start} source frames")
    return baked


def hips_world(arm):
    return arm.matrix_world @ arm.pose.bones["Hips"].head


def horizontal_drift(arm, action, frames):
    drive(arm, action)
    at(frames[0])
    origin = hips_world(arm)
    worst = 0.0
    for f in frames:
        at(f)
        d = hips_world(arm) - origin
        d.z = 0
        worst = max(worst, d.length)
    return worst


def pin_hips(arm, action):
    """Moves every Hips location key so the Hips' world position keeps the x/y of the first frame.
    The key is in the bone's rest space; the rotation that maps it to world is the armature's
    matrix times the parent's pose matrix times the bone's rest offset from its parent, none of
    which depends on the key itself - so one pass is exact."""
    drive(arm, action)
    hips = arm.pose.bones["Hips"]
    loc = sorted((fc for fc in curves(action) if fc.data_path == 'pose.bones["Hips"].location'),
                 key=lambda fc: fc.array_index)
    if len(loc) != 3:
        return None
    frames = [k.co.x for k in loc[0].keyframe_points]
    before = horizontal_drift(arm, action, frames)
    at(frames[0])
    origin = hips_world(arm)
    for i, f in enumerate(frames):
        at(f)
        delta = origin - hips_world(arm)
        delta.z = 0
        if hips.parent is not None:
            frame = (arm.matrix_world @ hips.parent.matrix
                     @ hips.parent.bone.matrix_local.inverted() @ hips.bone.matrix_local)
        else:
            frame = arm.matrix_world @ hips.bone.matrix_local
        local = frame.to_3x3().inverted() @ delta
        for fc, value in zip(loc, local):
            k = fc.keyframe_points[i]
            k.co.y += value
            k.handle_left.y += value
            k.handle_right.y += value
    for fc in loc:
        fc.update()
    after = horizontal_drift(arm, action, frames)
    return before, after


def process(name, source, target, mixamo, report):
    bpy.ops.wm.read_homefile(use_empty=True)
    scene = bpy.context.scene
    scene.render.fps = FPS
    bpy.ops.import_scene.gltf(filepath=source)
    scene.render.fps = FPS
    arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    arm.animation_data_create()
    missing = [c for c in CLIPS if c not in bpy.data.actions]
    if missing:
        raise SystemExit(f"{source}: missing actions {missing}")
    clips = list(CLIPS)
    for clip in CLIPS:
        action = bpy.data.actions[clip]
        loc = [fc for fc in curves(action) if fc.data_path == 'pose.bones["Hips"].location']
        if len(loc) != 3:
            report.append(f"[PIN] {name:<7} {clip:<8} has no Hips location keys")
            continue
        drift = horizontal_drift(arm, action, [k.co.x for k in loc[0].keyframe_points])
        if drift <= PIN_ABOVE:
            report.append(f"[PIN] {name:<7} {clip:<8} hips drift {drift:.3f} m, kept")
            continue
        result = pin_hips(arm, action)
        report.append(f"[PIN] {name:<7} {clip:<8} hips drift {result[0]:.3f} -> {result[1]:.3f} m")
    for clip, fbx in EXTRA.get(name, []):
        path = os.path.join(mixamo, fbx)
        if not os.path.exists(path):
            raise SystemExit(f"{name}: {path} is missing")
        action = graft(arm, path, clip)
        scene.render.fps = FPS
        retime_from_zero(action)
        first, residual = face_front(arm, action, True)
        clips.append(clip)
        report.append(f"[NEW] {name:<7} {clip:<8} {action.frame_range[1]:.0f} frames, "
                      f"turn {first:6.1f}, end ramp {residual:6.1f}")
    for track in list(arm.animation_data.nla_tracks):
        arm.animation_data.nla_tracks.remove(track)
    arm.animation_data.action = None
    longest = 0
    for clip in clips:
        action = bpy.data.actions[clip]
        track = arm.animation_data.nla_tracks.new()
        track.name = clip
        strip = track.strips.new(clip, 0, action)
        strip.frame_start = 0
        strip.frame_end = action.frame_range[1]
        track.mute = False
        longest = max(longest, action.frame_range[1])
    scene.frame_start = 0
    scene.frame_end = int(math.ceil(longest))
    bpy.ops.export_scene.gltf(
        filepath=target, export_format="GLB", use_active_scene=True, export_yup=True,
        export_apply=False, export_animations=True, export_animation_mode="NLA_TRACKS",
        export_force_sampling=True, export_bake_animation=False,
        export_optimize_animation_size=False, export_optimize_animation_keep_anim_object=True,
        export_current_frame=False, export_skins=True, export_morph=False)
    for clip in clips:
        action = bpy.data.actions[clip]
        drive(arm, action)
        start, end = action.frame_range
        series = [round(pelvis_yaw(arm, start + (end - start) * i / 12.0), 1) for i in range(13)]
        report.append(f"[YAW] {name:<7} {clip:<8} frames={end:6.1f} yaw={series}")


def main():
    root = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
    models = os.path.join(root, "src", "main", "resources", "models", "characters")
    out = os.path.join(root, "art", "gen", "pinned")
    os.makedirs(out, exist_ok=True)
    folder = {"hero": "hero", "mira": "mira", "eren": "eren", "goblin": "eren", "orc": "eren",
              "warden": "eren", "shaman": "eren", "king": "eren"}
    report = []
    for name in sys.argv[sys.argv.index("--") + 1:]:
        process(name, os.path.join(models, name + ".glb"), os.path.join(out, name + ".glb"),
                os.path.join(root, "art", "mixamo", folder[name]), report)
        print(f"[OK] {name} {os.path.getsize(os.path.join(out, name + '.glb'))} bytes")
    print("\n".join(report))


main()
