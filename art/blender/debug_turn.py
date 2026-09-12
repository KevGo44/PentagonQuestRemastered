
import bpy, os, math
from mathutils import Quaternion, Vector

R = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
bpy.ops.wm.read_homefile(use_empty=True)
bpy.ops.import_scene.gltf(filepath=os.path.join(R,"src","main","resources","models","characters","hero.glb"))
arm = next(o for o in bpy.data.objects if o.type=="ARMATURE")

def curves(a):
    return [fc for l in a.layers for s in l.strips for b in s.channelbags for fc in b.fcurves]

def yaw(frame):
    bpy.context.scene.frame_set(int(round(frame)))
    l = arm.matrix_world @ arm.pose.bones["Thigh.L"].head
    r = arm.matrix_world @ arm.pose.bones["Thigh.R"].head
    v = l - r; v.z = 0; v.normalize()
    return math.degrees(math.atan2(v.y, v.x))

a = bpy.data.actions["Idle"]
ad = arm.animation_data
print("[D] tracks muted:", [(t.name, t.mute) for t in ad.nla_tracks][:3], "count", len(ad.nla_tracks))
ad.action = a
print("[D] slots", [s.identifier for s in a.slots], "assigned", ad.action_slot)
if a.slots:
    ad.action_slot = a.slots[0]
print("[D] after slot assign:", ad.action_slot)
print("[D] yaw at 0 =", round(yaw(0),2), " pose Root quat =", tuple(round(v,4) for v in arm.pose.bones["Root"].rotation_quaternion))
print("[D] pose Root matrix (armature space):")
for row in arm.pose.bones["Root"].matrix: print("[D]  ", " ".join(f"{v:8.4f}" for v in row))
rot = sorted([fc for fc in curves(a) if fc.data_path == 'pose.bones["Root"].rotation_quaternion'], key=lambda f: f.array_index)
print("[D] root rot curves", len(rot), "keys", [len(fc.keyframe_points) for fc in rot])
print("[D] key0 values", [round(fc.keyframe_points[0].co.y,4) for fc in rot], "at frame", rot[0].keyframe_points[0].co.x if rot else None)
hips = sorted([fc for fc in curves(a) if fc.data_path == 'pose.bones["Hips"].rotation_quaternion'], key=lambda f: f.array_index)
print("[D] hips rot key0", [round(fc.keyframe_points[0].co.y,4) for fc in hips])
# Apply a 90 degree test turn on Root and see what happens.
turn = Quaternion((0,1,0), math.radians(90))
for i in range(len(rot[0].keyframe_points)):
    old = Quaternion([fc.keyframe_points[i].co.y for fc in rot])
    new = turn @ old
    for fc, val in zip(rot, new):
        k = fc.keyframe_points[i]
        k.co.y = val; k.handle_left.y = val; k.handle_right.y = val
for fc in rot: fc.update()
print("[D] after +90 about local Y: yaw at 0 =", round(yaw(0),2),
      "pose Root quat =", tuple(round(v,4) for v in arm.pose.bones["Root"].rotation_quaternion))
# And about local Z, for comparison.
turn2 = Quaternion((0,0,1), math.radians(90))
for i in range(len(rot[0].keyframe_points)):
    old = Quaternion([fc.keyframe_points[i].co.y for fc in rot])
    new = turn2 @ old
    for fc, val in zip(rot, new):
        k = fc.keyframe_points[i]
        k.co.y = val; k.handle_left.y = val; k.handle_right.y = val
for fc in rot: fc.update()
print("[D] then +90 about local Z: yaw at 0 =", round(yaw(0),2))
