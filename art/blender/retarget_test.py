
import bpy, os, math
from mathutils import Quaternion, Vector

R = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
bpy.ops.wm.read_homefile(use_empty=True)
bpy.ops.import_scene.gltf(filepath=os.path.join(R,"src","main","resources","models","characters","hero.glb"))
ours = next(o for o in bpy.data.objects if o.type=="ARMATURE")
names_ours = [b.name for b in ours.data.bones]
before = set(bpy.data.objects)
bpy.ops.import_scene.fbx(filepath=os.path.join(R,"art","mixamo","hero","sword and shield idle (4).fbx"),
                         automatic_bone_orientation=False, ignore_leaf_bones=True)
theirs = next(o for o in bpy.data.objects if o.type=="ARMATURE" and o is not ours)
names_theirs = [b.name for b in theirs.data.bones]
print("[M] ours", len(names_ours), "theirs", len(names_theirs))
print("[M] theirs sample", names_theirs[:6])
print("[M] ours sample", names_ours[:6])

RENAME = {"LeftArm":"UpperArm.L","LeftForeArm":"Forearm.L","LeftHand":"Hand.L",
          "RightArm":"UpperArm.R","RightForeArm":"Forearm.R","RightHand":"Hand.R",
          "LeftUpLeg":"Thigh.L","LeftLeg":"Shin.L","LeftFoot":"Foot.L",
          "RightUpLeg":"Thigh.R","RightLeg":"Shin.R","RightFoot":"Foot.R"}
def ours_name(mixamo):
    plain = mixamo.split(":")[-1]
    return RENAME.get(plain, plain)

mapped, missing = {}, []
for n in names_theirs:
    target = ours_name(n)
    if target in names_ours:
        mapped[n] = target
    else:
        missing.append((n, target))
print("[M] mapped", len(mapped), "missing", missing)
# Rest pose comparison: same rig or not?
worst_pos = worst_rot = 0
for src, dst in mapped.items():
    a = theirs.data.bones[src]
    b = ours.data.bones[dst]
    worst_pos = max(worst_pos, (a.head_local - b.head_local).length)
    q = a.matrix_local.to_quaternion().rotation_difference(b.matrix_local.to_quaternion())
    worst_rot = max(worst_rot, math.degrees(q.angle))
print(f"[M] rest pose difference: worst head {worst_pos:.5f} m, worst bone rotation {worst_rot:.3f} deg")
print("[M] theirs object scale", tuple(round(v,4) for v in theirs.scale),
      "ours scale", tuple(round(v,4) for v in ours.scale))
print("[M] theirs rot", tuple(round(v,4) for v in theirs.rotation_euler),
      "ours rot", tuple(round(v,4) for v in ours.rotation_euler))
act = theirs.animation_data.action
print("[M] action", act.name, "range", tuple(round(v,2) for v in act.frame_range))
def curves(a):
    return [fc for l in a.layers for s in l.strips for b in s.channelbags for fc in b.fcurves]
paths = sorted({fc.data_path for fc in curves(act)})
print("[M] curve count", len(curves(act)), "sample paths", paths[:3])
