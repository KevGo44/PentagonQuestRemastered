
import bpy, os, math, glob
from mathutils import Vector

SRC = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered\art\mixamo\hero"
files = sorted(f for f in glob.glob(os.path.join(SRC, "*.fbx")) if "geom" not in os.path.basename(f))

def pelvis_yaw(arm, frame):
    bpy.context.scene.frame_set(frame)
    left = arm.matrix_world @ arm.pose.bones["mixamorig:LeftUpLeg"].head
    right = arm.matrix_world @ arm.pose.bones["mixamorig:RightUpLeg"].head
    lateral = (left - right)
    lateral.z = 0
    lateral.normalize()
    # A Mixamo character faces -Y in Blender, so its left hip should sit on +X.
    return math.degrees(math.atan2(lateral.y, lateral.x))

for path in files:
    bpy.ops.wm.read_homefile(use_empty=True)
    try:
        bpy.ops.import_scene.fbx(filepath=path, automatic_bone_orientation=False,
                                 ignore_leaf_bones=True)
    except Exception as e:
        print("[ERR]", os.path.basename(path), e)
        continue
    arms = [o for o in bpy.data.objects if o.type == "ARMATURE"]
    if not arms:
        print("[ERR]", os.path.basename(path), "no armature")
        continue
    arm = arms[0]
    action = arm.animation_data.action if arm.animation_data else None
    if action is None:
        print("[ERR]", os.path.basename(path), "no action")
        continue
    start, end = (int(round(v)) for v in action.frame_range)
    frames = max(1, end - start)
    samples = [start + round(frames * i / 10) for i in range(11)]
    yaws = [pelvis_yaw(arm, f) for f in samples]
    series = " ".join(f"{y:6.1f}" for y in yaws)
    print(f"[SRC] {os.path.basename(path)[:-4]:<34} frames={start}..{end} sec={frames/30:5.2f} {series}")
