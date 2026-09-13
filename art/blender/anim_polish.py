
"""Second pass over the normalised character clips: the four clips that did not fit the game.

Run on the GLBs that are in src/main/resources (already turned onto the front and at 30 fps by
anim_normalise.py); writes to art/gen/polished. Findings this pass answers, all measured with
art/probe/ClipTimeline and art/probe/YawSeries against the delivered files:

* Attack2 turned 180 degrees and stayed there - the source spin had been cut at the combo length
  instead of retimed, so the hero struck behind himself. It is regrafted from "sword and shield
  attack (2)", a full 360-degree spin, and time-scaled to the combo length; a small yaw ramp makes
  the spin end exactly on the front, so the next clip has nothing to snap back from.
* Attack3 turned 96 degrees out for the same reason. Regrafted from "sword and shield attack (4)",
  a heavy forward slash without a spin, time-scaled to 0.767 s.
* Dodge was 2.33 s long against a 0.58 s dodge window, so the player crouched and stood up
  again; the roll (hips on the floor at 1.2 s) never happened. Time-scaled to 0.767 s; the game
  window follows the clip.
* Cast was 2.97 s long against a 0.6 s cast window and released only at 1.6 s. Regrafted from
  "sword and shield casting (2)", a 1.03 s one-hand cast, time-scaled to 0.6 s.

Frame counts are whole frames at 30 fps: 17 (0.567 s), 23 (0.767 s), 23, 18 (0.6 s). The
engine test allows one frame of slack against AttackTimeline.
"""

import bpy, os, math, sys
from mathutils import Quaternion, Vector

CLIPS = ["Idle", "Walk", "Run", "Attack1", "Attack2", "Attack3",
         "Dodge", "Block", "Hit", "Death", "Cast"]
FPS = 30
# clip -> (source fbx, target frame count, extra yaw ramp so the clip also ends on the front)
GRAFTS = {"Attack2": ("sword and shield attack (2).fbx", 17, True),
          "Attack3": ("sword and shield attack (4).fbx", 23, True),
          "Cast": ("sword and shield casting (2).fbx", 18, True)}
RETIMES = {"Dodge": 23}
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


def pelvis_yaw(arm, frame):
    """Degrees the pelvis is turned out of the front (Blender front is -Y, left hip on +X)."""
    bpy.context.scene.frame_set(int(round(frame)))
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
    """Yaws the whole body on the Root bone by degrees_at(frame) - a constant or a ramp. Root's
    yaw axis is its local Z, measured by axis_test.py, and a positive turn lowers the yaw."""
    rot, loc = root_curves(action)
    count = len(rot[0].keyframe_points)
    for i in range(count):
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


def retime(action, frames):
    """Scales every key so the clip spans exactly `frames` frames from 0."""
    start, end = action.frame_range
    factor = frames / (end - start)
    for fc in curves(action):
        for k in fc.keyframe_points:
            k.co.x = (k.co.x - start) * factor
            k.handle_left.x = (k.handle_left.x - start) * factor
            k.handle_right.x = (k.handle_right.x - start) * factor
        fc.update()


def graft(arm, fbx, clip):
    """Bakes a Mixamo animation onto the delivered rig through world-space constraints; the
    sources were exported for this very character, so proportions agree."""
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


def face_front(arm, action, ramp):
    """Pins the first frame to the front; with `ramp`, also the last frame, by spreading the
    residual turn over the clip."""
    drive(arm, action)
    start, end = action.frame_range
    first = pelvis_yaw(arm, start)
    turn_root(action, lambda x: first)
    residual = 0.0
    if ramp and end > start:
        residual = pelvis_yaw(arm, end)
        turn_root(action, lambda x: residual * (x - start) / (end - start))
    return first, residual


def polish(name, source, target, mixamo, report):
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
    before = {c: bpy.data.actions[c].frame_range[1] for c in CLIPS}
    for clip, (fbx, frames, ramp) in GRAFTS.items():
        path = os.path.join(mixamo, fbx)
        if not os.path.exists(path):
            raise SystemExit(f"{name}: {path} is missing")
        action = graft(arm, path, clip)
        scene.render.fps = FPS
        retime(action, frames)
        first, residual = face_front(arm, action, ramp)
        report.append(f"[POL] {name:<7} {clip:<8} {before[clip]:5.1f} -> {frames} frames, "
                      f"turn {first:6.1f}, end ramp {residual:6.1f}")
    for clip, frames in RETIMES.items():
        action = bpy.data.actions[clip]
        retime(action, frames)
        report.append(f"[POL] {name:<7} {clip:<8} {before[clip]:5.1f} -> {frames} frames")
    for track in list(arm.animation_data.nla_tracks):
        arm.animation_data.nla_tracks.remove(track)
    arm.animation_data.action = None
    longest = 0
    for clip in CLIPS:
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
    for clip in CLIPS:
        action = bpy.data.actions[clip]
        drive(arm, action)
        start, end = action.frame_range
        series = [round(pelvis_yaw(arm, start + (end - start) * i / 12.0), 1) for i in range(13)]
        report.append(f"[YAW] {name:<7} {clip:<8} frames={end:6.1f} yaw={series}")


def main():
    root = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
    models = os.path.join(root, "src", "main", "resources", "models", "characters")
    out = os.path.join(root, "art", "gen", "polished")
    os.makedirs(out, exist_ok=True)
    folder = {"hero": "hero", "mira": "mira", "eren": "eren", "goblin": "eren", "orc": "eren",
              "warden": "eren", "shaman": "eren", "king": "eren"}
    report = []
    for name in sys.argv[sys.argv.index("--") + 1:]:
        polish(name, os.path.join(models, name + ".glb"), os.path.join(out, name + ".glb"),
               os.path.join(root, "art", "mixamo", folder[name]), report)
        print(f"[OK] {name} {os.path.getsize(os.path.join(out, name + '.glb'))} bytes")
    print("\n".join(report))


main()
