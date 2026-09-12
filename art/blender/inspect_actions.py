
import bpy, os, math
from mathutils import Vector

R = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
bpy.ops.wm.read_homefile(use_empty=True)
bpy.ops.import_scene.gltf(filepath=os.path.join(R, "src", "main", "resources", "models",
                                                 "characters", "hero.glb"))
arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")

def curves(action):
    out = []
    for layer in action.layers:
        for strip in layer.strips:
            for bag in strip.channelbags:
                out.extend(bag.fcurves)
    return out

print("[I] armature", arm.name, "objects", [o.name for o in bpy.data.objects])
root = arm.data.bones.get("Root")
print("[I] Root rest matrix_local:")
if root:
    for row in root.matrix_local:
        print("[I]  ", " ".join(f"{v:8.4f}" for v in row))
    print("[I] Root parent", root.parent.name if root.parent else None,
          "children", [b.name for b in root.children])
hips = arm.data.bones.get("Hips")
if hips:
    print("[I] Hips head", tuple(round(v,4) for v in hips.head_local),
          "parent", hips.parent.name if hips.parent else None)
    for row in hips.matrix_local:
        print("[I]  H", " ".join(f"{v:8.4f}" for v in row))

for a in sorted(bpy.data.actions, key=lambda a: a.name):
    fcs = curves(a)
    bones = sorted({fc.data_path.split('"')[1] for fc in fcs if '"' in fc.data_path})
    keys = sorted({round(k.co[0], 3) for fc in fcs for k in fc.keyframe_points})
    print(f"[A] {a.name!r} range={tuple(round(v,2) for v in a.frame_range)} curves={len(fcs)}"
          f" bones={len(bones)} keyFrames={len(keys)} first={keys[:4]} last={keys[-2:]}"
          f" rootAnimated={'Root' in bones}")

ad = arm.animation_data
print("[I] active action", ad.action.name if ad and ad.action else None)
for t in (ad.nla_tracks if ad else []):
    print("[T]", t.name, "mute", t.mute,
          [(s.name, round(s.frame_start,2), round(s.frame_end,2), s.action.name if s.action else None,
            s.mute, round(s.action_frame_start,2), round(s.action_frame_end,2)) for s in t.strips])

# Is the first frame of a clip the rest pose?
def rest_distance(frame):
    bpy.context.scene.frame_set(frame)
    worst = 0.0
    for pb in arm.pose.bones:
        m = pb.matrix_basis
        worst = max(worst, (m.to_quaternion().angle if m.to_quaternion().angle else 0))
    return worst

for name in ["Idle", "Walk", "Attack1"]:
    action = bpy.data.actions.get(name)
    if not action:
        continue
    ad.action = action
    s, e = (int(round(v)) for v in action.frame_range)
    print(f"[R] {name} frames {s}..{e}: restAngle(first)={math.degrees(rest_distance(s)):6.2f}"
          f" restAngle(second)={math.degrees(rest_distance(s+1)):6.2f}"
          f" restAngle(mid)={math.degrees(rest_distance((s+e)//2)):6.2f}")
