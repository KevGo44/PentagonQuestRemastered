
"""Quiet idles for the two non-combatants. Mira and Eren stood in Mixamo's sword-and-shield guard
stance without weapons; a cartographer and a chained prisoner should not. Their Idle is replaced:

* eren  <- "Sad Idle" (Mixamo, 121 frames, head down, weight on one leg)
* mira  <- "Breathing Idle" (Mixamo, 299 frames, calm breathing with a little sway)

Both sources were exported from Mixamo on Eren's uploaded mesh, so they carry Eren's skeleton.
For Eren that is the very rig the delivered GLB wears, and the clip is baked on through world-space
Copy Transforms like every other clip (anim_normalise.py). Mira's rig has her own bone lengths, so
for her only the world-space *rotations* are copied; her joints keep their own positions and the
hips are then lowered or raised so that the lowest foot point sits where it did in her old Idle.
That grounding step runs for both, and the log prints how far it had to move.

Held clips are turned onto the front by their circular mean yaw, as in anim_normalise.py.
Input: the polished GLBs in src/main/resources. Output: art/gen/npc-idle/<name>.glb.
"""

import bpy, os, math, sys
from mathutils import Quaternion, Vector

CLIPS = ["Idle", "Walk", "Run", "Attack1", "Attack2", "Attack3",
         "Dodge", "Block", "Hit", "Death", "Cast"]
FPS = 30
SOURCES = {"eren": ("Sad Idle.fbx", "transforms"), "mira": ("Breathing Idle.fbx", "rotations")}
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
    bpy.context.scene.frame_set(int(round(frame)))
    left = arm.matrix_world @ arm.pose.bones["Thigh.L"].head
    right = arm.matrix_world @ arm.pose.bones["Thigh.R"].head
    lateral = left - right
    lateral.z = 0
    lateral.normalize()
    return math.degrees(math.atan2(lateral.y, lateral.x))


def mean_yaw(arm, action, samples=24):
    start, end = action.frame_range
    sines = cosines = 0.0
    for i in range(samples):
        angle = math.radians(pelvis_yaw(arm, start + (end - start) * i / (samples - 1.0)))
        sines += math.sin(angle)
        cosines += math.cos(angle)
    return math.degrees(math.atan2(sines, cosines))


def lowest_point(arm, frame):
    """World z of the lowest foot/toe joint - the ground contact of the pose."""
    bpy.context.scene.frame_set(int(round(frame)))
    z = []
    for pb in arm.pose.bones:
        if pb.name.startswith(("Foot", "Toe")) or "Toe" in pb.name:
            z.append((arm.matrix_world @ pb.head).z)
            z.append((arm.matrix_world @ pb.tail).z)
    return min(z)


def turn_root(action, degrees):
    turn = Quaternion((0, 0, 1), math.radians(degrees))
    rot = sorted((fc for fc in curves(action)
                  if fc.data_path == 'pose.bones["Root"].rotation_quaternion'),
                 key=lambda fc: fc.array_index)
    loc = sorted((fc for fc in curves(action) if fc.data_path == 'pose.bones["Root"].location'),
                 key=lambda fc: fc.array_index)
    if len(rot) != 4:
        raise SystemExit(f"{action.name}: Root carries {len(rot)} rotation curves, expected 4")
    for i in range(len(rot[0].keyframe_points)):
        new = turn @ Quaternion([fc.keyframe_points[i].co.y for fc in rot])
        for fc, value in zip(rot, new):
            k = fc.keyframe_points[i]
            k.co.y = k.handle_left.y = k.handle_right.y = value
    if len(loc) == 3:
        matrix = turn.to_matrix()
        for i in range(len(loc[0].keyframe_points)):
            new = matrix @ Vector([fc.keyframe_points[i].co.y for fc in loc])
            for fc, value in zip(loc, new):
                k = fc.keyframe_points[i]
                k.co.y = k.handle_left.y = k.handle_right.y = value
    for fc in rot + loc:
        fc.update()


def shift_hips(action, arm, dz):
    """Moves the whole clip up or down by dz metres, through the Hips location curves."""
    loc = sorted((fc for fc in curves(action) if fc.data_path == 'pose.bones["Hips"].location'),
                 key=lambda fc: fc.array_index)
    if len(loc) != 3:
        raise SystemExit(f"{action.name}: Hips carries {len(loc)} location curves")
    # Hips location is expressed in the bone's own rest frame; find which local axis is world up.
    rest = arm.matrix_world @ arm.data.bones["Hips"].matrix_local
    up = rest.to_3x3().inverted() @ Vector((0, 0, 1))
    for fc, component in zip(loc, up):
        for k in fc.keyframe_points:
            k.co.y += dz * component
            k.handle_left.y += dz * component
            k.handle_right.y += dz * component
        fc.update()


def graft(arm, fbx, clip, mode):
    known = {o for o in bpy.data.objects}
    bpy.ops.import_scene.fbx(filepath=fbx, automatic_bone_orientation=False, ignore_leaf_bones=True)
    source = next(o for o in bpy.data.objects if o not in known and o.type == "ARMATURE")
    action = source.animation_data.action
    start, end = (int(round(v)) for v in action.frame_range)
    bones = {b.name for b in source.data.bones}
    matched = 0
    for pb in arm.pose.bones:
        wanted = "mixamorig:" + RENAME.get(pb.name, pb.name)
        if wanted not in bones:
            continue
        kind = "COPY_TRANSFORMS" if mode == "transforms" else "COPY_ROTATION"
        constraint = pb.constraints.new(kind)
        constraint.target = source
        constraint.subtarget = wanted
        matched += 1
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
    if start != 0:
        for fc in curves(baked):
            for k in fc.keyframe_points:
                k.co.x -= start
                k.handle_left.x -= start
                k.handle_right.x -= start
            fc.update()
    for obj in [o for o in bpy.data.objects if o not in known]:
        bpy.data.objects.remove(obj, do_unlink=True)
    print(f"[G] grafted {clip} from {os.path.basename(fbx)} by {mode}: {end - start} frames, "
          f"{matched} bones")
    return baked


def run(name, source, target, fbx, mode, report):
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
    old_idle = bpy.data.actions["Idle"]
    drive(arm, old_idle)
    ground = lowest_point(arm, old_idle.frame_range[0])
    idle = graft(arm, fbx, "Idle", mode)
    scene.render.fps = FPS
    drive(arm, idle)
    turn = mean_yaw(arm, idle)
    turn_root(idle, turn)
    start, end = idle.frame_range
    lows = [lowest_point(arm, start + (end - start) * i / 8.0) for i in range(9)]
    dz = ground - min(lows)
    shift_hips(idle, arm, dz)
    after = [lowest_point(arm, start + (end - start) * i / 8.0) for i in range(9)]
    series = [round(pelvis_yaw(arm, start + (end - start) * i / 12.0), 1) for i in range(13)]
    report.append(f"[NPC] {name:<5} Idle <- {os.path.basename(fbx)} ({mode}) frames={end:.0f} "
                  f"turn={turn:6.1f} ground={ground:.3f} shifted={dz:+.3f} "
                  f"lowest after={min(after):.3f}..{max(after):.3f} yaw={series}")
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


def main():
    root = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
    models = os.path.join(root, "src", "main", "resources", "models", "characters")
    out = os.path.join(root, "art", "gen", "npc-idle")
    os.makedirs(out, exist_ok=True)
    report = []
    for name in sys.argv[sys.argv.index("--") + 1:]:
        fbx, mode = SOURCES[name]
        run(name, os.path.join(models, name + ".glb"), os.path.join(out, name + ".glb"),
            os.path.join(root, "art", "mixamo", "npc", fbx), mode, report)
        print(f"[OK] {name} {os.path.getsize(os.path.join(out, name + '.glb'))} bytes")
    print("\n".join(report))


main()
