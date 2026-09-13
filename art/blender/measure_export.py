"""Reads an exported character GLB back and prints the pelvis yaw and the Root yaw of one clip
per whole frame - what the engine will see, not what the action said. Usage:
blender --background --factory-startup --python measure_export.py -- <glb> <clip>"""
import bpy, sys, math
args = sys.argv[sys.argv.index("--") + 1:]
path, clip = args[0], args[1]
bpy.ops.wm.read_homefile(use_empty=True)
bpy.context.scene.render.fps = 30
bpy.ops.import_scene.gltf(filepath=path)
arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
arm.animation_data_create()
action = bpy.data.actions[clip]
arm.animation_data.action = action
if action.slots:
    arm.animation_data.action_slot = action.slots[0]
for t in arm.animation_data.nla_tracks:
    t.mute = True
start, end = action.frame_range
out = []
for f in range(int(start), int(math.ceil(end)) + 1):
    bpy.context.scene.frame_set(f)
    left = arm.matrix_world @ arm.pose.bones["Thigh.L"].head
    right = arm.matrix_world @ arm.pose.bones["Thigh.R"].head
    lat = left - right
    lat.z = 0
    lat.normalize()
    yaw = math.degrees(math.atan2(lat.y, lat.x))
    rq = arm.pose.bones["Root"].rotation_quaternion
    ryaw = math.degrees(rq.to_euler().z)
    out.append(f"{f}:{yaw:.0f}/{ryaw:.0f}")
print("[EXPORT]", clip, "frames", start, end, " ".join(out))
curves = [fc for layer in action.layers for strip in layer.strips for bag in strip.channelbags for fc in bag.fcurves]
rootc = [fc for fc in curves if 'bones["Root"]' in fc.data_path]
print("[EXPORT] root curves", len(rootc), "keys", [len(fc.keyframe_points) for fc in rootc][:8])
model = []
for f in range(8, 16):
    bpy.context.scene.frame_set(f)
    rm = arm.matrix_world @ arm.pose.bones["Root"].matrix
    hm = arm.matrix_world @ arm.pose.bones["Hips"].matrix
    rx = rm.to_3x3() @ __import__("mathutils").Vector((1, 0, 0))
    hx = hm.to_3x3() @ __import__("mathutils").Vector((1, 0, 0))
    model.append(f"{f}:root({math.degrees(math.atan2(rx.y, rx.x)):.0f})hips({math.degrees(math.atan2(hx.y, hx.x)):.0f})")
print("[BMODEL]", " ".join(model))
