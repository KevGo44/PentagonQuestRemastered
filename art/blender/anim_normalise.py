
"""Repairs the delivered character clips. Three faults, all of them in the asset.

1. Mixamo's whole "sword and shield" set is authored in a side-on guard stance, roughly 53 degrees
   out of the front, while its walk and run face straight ahead. A character built from both stands
   sideways and walks forwards. Every clip is turned so that it faces the glTF front (+Z), which is
   where the engine points a character with Quaternion.lookAt.

2. Every exported animation carried a leading sample of the rest pose, because the NLA strips began
   at frame 1 while the export sampled from frame 0. In the game that is a T-pose flash on every
   single clip change. The strips now begin at frame 0.

3. The idle source itself swings the whole body through 60 degrees ("sword and shield idle"), which
   reads as a character turning on the spot. It is replaced by "sword and shield idle (4)", whose
   stance holds to within a degree, grafted on through Copy Transforms constraints and a visual bake
   - the rest poses of the delivered rig and the Mixamo source differ by up to 22 degrees per bone,
   so copying curves across would deform the pose. World-space constraints do not care.

The turn is applied on the Root bone. Which of its axes yaws the body is measured, not read off the
rest matrix: art/blender/axis_test.py turned Root about all three axes and printed tilt and head
height, and only local Z is a clean yaw. The rest matrix suggests local Y, and that answer is wrong
by 37 degrees of tilt. Positive rotation about that axis lowers the measured yaw, so the correction
is the yaw itself, not its negative.
"""

import bpy, os, math, sys
from mathutils import Quaternion, Vector

CLIPS = ["Idle", "Walk", "Run", "Attack1", "Attack2", "Attack3",
         "Dodge", "Block", "Hit", "Death", "Cast"]
# A stance that is held is centred on the whole clip, so the stance faces the front. A clip that
# ends somewhere else on purpose - a swing, a roll, a fall, a spell - is pinned by its first frame,
# because that is the moment the player aims it.
HELD = {"Idle", "Walk", "Run", "Block", "Hit"}
# The file was exported from a 30 fps scene, so importing it into a 24 fps scene puts the keys on
# 0.8-frame steps. Scaling by 30/24 and setting the scene to 30 fps puts them back on whole frames,
# and sampled export then reproduces the original timing key for key instead of resampling at 24.
FPS_SCALE = 30.0 / 24.0
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
    """Degrees the pelvis is turned out of the front. The thigh roots are bolted to the hips, so
    nothing the legs do can move this reading."""
    bpy.context.scene.frame_set(int(round(frame)))
    left = arm.matrix_world @ arm.pose.bones["Thigh.L"].head
    right = arm.matrix_world @ arm.pose.bones["Thigh.R"].head
    lateral = left - right
    lateral.z = 0
    lateral.normalize()
    # Blender's front is -Y and up is +Z, so a character facing the front carries its left hip on +X.
    return math.degrees(math.atan2(lateral.y, lateral.x))


def mean_yaw(arm, action, samples=24):
    start, end = action.frame_range
    sines = cosines = 0.0
    for i in range(samples):
        angle = math.radians(pelvis_yaw(arm, start + (end - start) * i / (samples - 1.0)))
        sines += math.sin(angle)
        cosines += math.cos(angle)
    return math.degrees(math.atan2(sines, cosines))


def rescale(action):
    for fc in curves(action):
        for k in fc.keyframe_points:
            k.co.x *= FPS_SCALE
            k.handle_left.x *= FPS_SCALE
            k.handle_right.x *= FPS_SCALE
        fc.update()


def turn_root(action, degrees):
    """Turns the whole body by a constant yaw, written onto the Root bone's own keys."""
    turn = Quaternion((0, 0, 1), math.radians(degrees))
    rot = sorted((fc for fc in curves(action)
                  if fc.data_path == 'pose.bones["Root"].rotation_quaternion'),
                 key=lambda fc: fc.array_index)
    loc = sorted((fc for fc in curves(action) if fc.data_path == 'pose.bones["Root"].location'),
                 key=lambda fc: fc.array_index)
    if len(rot) != 4:
        raise SystemExit(f"{action.name}: Root carries {len(rot)} rotation curves, expected 4")
    count = len(rot[0].keyframe_points)
    for fc in rot:
        if len(fc.keyframe_points) != count:
            raise SystemExit(f"{action.name}: Root rotation curves disagree on key count")
    for i in range(count):
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


def graft(arm, fbx, clip):
    """Bakes a Mixamo animation onto the delivered rig through world-space constraints."""
    known = {o for o in bpy.data.objects}
    bpy.ops.import_scene.fbx(filepath=fbx, automatic_bone_orientation=False, ignore_leaf_bones=True)
    source = next(o for o in bpy.data.objects if o not in known and o.type == "ARMATURE")
    action = source.animation_data.action
    start, end = (int(round(v)) for v in action.frame_range)
    bones = {b.name for b in source.data.bones}
    attached = 0
    for pb in arm.pose.bones:
        wanted = "mixamorig:" + RENAME.get(pb.name, pb.name)
        if wanted not in bones:
            continue
        constraint = pb.constraints.new("COPY_TRANSFORMS")
        constraint.target = source
        constraint.subtarget = wanted
        attached += 1
    # The enemies wear a reduced 26-bone version of the rig with no fingers or toes, so the count
    # alone says nothing. What has to be there is the contract: hips, spine, head, both arms, both
    # legs. Anything beyond that is detail the enemy rig does not carry.
    contract = ["Hips", "Spine", "Head", "UpperArm.L", "Forearm.L", "Hand.L",
                "UpperArm.R", "Forearm.R", "Hand.R", "Thigh.L", "Shin.L", "Foot.L",
                "Thigh.R", "Shin.R", "Foot.R"]
    absent = [b for b in contract
              if b in {p.name for p in arm.pose.bones}
              and "mixamorig:" + RENAME.get(b, b) not in bones]
    if absent:
        raise SystemExit(f"{clip}: {os.path.basename(fbx)} cannot drive {absent}")
    bpy.context.scene.frame_start, bpy.context.scene.frame_end = start, end
    bpy.context.view_layer.objects.active = arm
    arm.select_set(True)
    bpy.ops.object.mode_set(mode="POSE")
    # only_selected=False takes every bone, so nothing has to be selected - which is just as well,
    # since bone selection moved in Blender 5.
    bpy.ops.nla.bake(frame_start=start, frame_end=end, step=1, only_selected=False,
                     visual_keying=True, clear_constraints=True, clear_parents=False,
                     use_current_action=False, clean_curves=False, bake_types={"POSE"})
    bpy.ops.object.mode_set(mode="OBJECT")
    baked = arm.animation_data.action
    old = bpy.data.actions.get(clip)
    if old is not None:
        old.name = clip + "_replaced"
    baked.name = clip
    # The bake starts at the source's first frame; shift it to zero so every clip starts there.
    if start != 0:
        for fc in curves(baked):
            for k in fc.keyframe_points:
                k.co.x -= start
                k.handle_left.x -= start
                k.handle_right.x -= start
            fc.update()
    for obj in [o for o in bpy.data.objects if o not in known]:
        bpy.data.objects.remove(obj, do_unlink=True)
    print(f"[G] grafted {clip} from {os.path.basename(fbx)}: frames 0..{end - start}, "
          f"{attached} bones matched")
    return baked


def normalise(name, source, target, mixamo, report):
    bpy.ops.wm.read_homefile(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=source)
    scene = bpy.context.scene
    arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    arm.animation_data_create()
    missing = [c for c in CLIPS if c not in bpy.data.actions]
    if missing:
        raise SystemExit(f"{source}: missing actions {missing}")
    scene.render.fps = 30
    for clip in CLIPS:
        rescale(bpy.data.actions[clip])
    idle = os.path.join(mixamo, "sword and shield idle (4).fbx")
    if os.path.exists(idle):
        graft(arm, idle, "Idle")
        scene.render.fps = 30
    turns, series = {}, {}
    for clip in CLIPS:
        action = bpy.data.actions[clip]
        drive(arm, action)
        turns[clip] = mean_yaw(arm, action) if clip in HELD else pelvis_yaw(arm, action.frame_range[0])
        turn_root(action, turns[clip])
        start, end = action.frame_range
        series[clip] = [round(pelvis_yaw(arm, start + (end - start) * i / 12.0), 1)
                        for i in range(13)]
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
        report.append(f"[FIX] {name:<7} {clip:<8} turn={turns[clip]:7.1f} "
                      f"frames={bpy.data.actions[clip].frame_range[1]:6.1f} yaw={series[clip]}")


def main():
    root = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
    models = os.path.join(root, "src", "main", "resources", "models", "characters")
    out = os.path.join(root, "art", "gen", "normalised")
    os.makedirs(out, exist_ok=True)
    # The enemies wear Eren's rig and Eren's clips, so their replacement idle comes from his folder.
    folder = {"hero": "hero", "mira": "mira", "eren": "eren", "goblin": "eren", "orc": "eren",
              "warden": "eren", "shaman": "eren", "king": "eren"}
    report = []
    for name in sys.argv[sys.argv.index("--") + 1:]:
        normalise(name, os.path.join(models, name + ".glb"), os.path.join(out, name + ".glb"),
                  os.path.join(root, "art", "mixamo", folder[name]), report)
        print(f"[OK] {name} {os.path.getsize(os.path.join(out, name + '.glb'))} bytes")
    print("\n".join(report))


main()
