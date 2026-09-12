
import bpy, os, math
from mathutils import Quaternion, Vector

R = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
bpy.ops.wm.read_homefile(use_empty=True)
bpy.ops.import_scene.gltf(filepath=os.path.join(R,"src","main","resources","models","characters","hero.glb"))
arm = next(o for o in bpy.data.objects if o.type=="ARMATURE")
def curves(a):
    return [fc for l in a.layers for s in l.strips for b in s.channelbags for fc in b.fcurves]
def state(frame):
    bpy.context.scene.frame_set(int(round(frame)))
    l = arm.matrix_world @ arm.pose.bones["Thigh.L"].head
    r = arm.matrix_world @ arm.pose.bones["Thigh.R"].head
    head = arm.matrix_world @ arm.pose.bones["Head"].head
    v = l - r
    tilt = math.degrees(math.asin(max(-1, min(1, v.normalized().z))))
    v.z = 0; v.normalize()
    return math.degrees(math.atan2(v.y, v.x)), tilt, head.z
a = bpy.data.actions["Idle"]
ad = arm.animation_data
ad.action = a
if a.slots: ad.action_slot = a.slots[0]
rot = sorted([fc for fc in curves(a) if fc.data_path == 'pose.bones["Root"].rotation_quaternion'],
             key=lambda f: f.array_index)
base = [[k.co.y for k in fc.keyframe_points] for fc in rot]
def set_turn(q):
    for i in range(len(rot[0].keyframe_points)):
        old = Quaternion([base[j][i] for j in range(4)])
        new = q @ old
        for fc, val in zip(rot, new):
            k = fc.keyframe_points[i]
            k.co.y = val; k.handle_left.y = val; k.handle_right.y = val
    for fc in rot: fc.update()
print("[T] reference", tuple(round(v,2) for v in state(0)))
for axis, name in [((1,0,0),"localX"), ((0,1,0),"localY"), ((0,0,1),"localZ")]:
    for angle in (30, 90):
        set_turn(Quaternion(axis, math.radians(angle)))
        yaw, tilt, head = state(0)
        print(f"[T] {name} +{angle:3d}: yaw={yaw:8.2f} tilt={tilt:6.2f} headZ={head:5.2f}")
set_turn(Quaternion((1,0,0,0)))
print("[T] restored", tuple(round(v,2) for v in state(0)))
