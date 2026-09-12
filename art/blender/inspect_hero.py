
import bpy, sys, os, math
from mathutils import Vector

R = r"C:\\Users\\kkfre\\Daten\\IdeaProjects\\PentagonQuestRemastered"
path = os.path.join(R, "src", "main", "resources", "models", "characters", "hero.glb")
bpy.ops.wm.read_homefile(use_empty=True)
bpy.ops.import_scene.gltf(filepath=path)
scene = bpy.context.scene
print("[I] fps", scene.render.fps, "start", scene.frame_start, "end", scene.frame_end)
arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
print("[I] armature", arm.name, "bones", len(arm.data.bones), "rot", tuple(round(v, 4) for v in arm.rotation_euler))
for o in bpy.data.objects:
    print("[I] object", o.name, o.type, "parent", o.parent.name if o.parent else None)
for a in sorted(bpy.data.actions, key=lambda a: a.name):
    rng = tuple(round(v, 2) for v in a.frame_range)
    hips = [fc for fc in a.fcurves if "Hips" in fc.data_path and "rotation" in fc.data_path]
    print(f"[A] {a.name!r} range={rng} curves={len(a.fcurves)} users={a.users} hipsRotCurves={len(hips)}"
          f" keys={[len(fc.keyframe_points) for fc in hips]}")
    if hips:
        xs = sorted({round(k.co[0], 3) for fc in hips for k in fc.keyframe_points})
        print(f"[A]   hips key frames: first five {xs[:5]} last {xs[-3:]} count {len(xs)}")
ad = arm.animation_data
print("[I] action on armature:", ad.action.name if ad and ad.action else None)
for t in (ad.nla_tracks if ad else []):
    print("[T]", t.name, "mute", t.mute, [(s.name, round(s.frame_start,2), round(s.frame_end,2),
          s.action.name if s.action else None, s.mute, round(s.action_frame_start,2), round(s.action_frame_end,2)) for s in t.strips])
