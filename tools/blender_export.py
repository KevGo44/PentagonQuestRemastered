"""Blender 4.x+: import FBX/OBJ/BLEND and export a validated, metre-scale GLB.

blender --background --python tools/blender_export.py -- --input art/hero.fbx \
    --output src/main/resources/models/characters/hero.glb --character
"""
import argparse
from pathlib import Path
import sys
import bpy

BONES = {"Root", "Hips", "Spine", "Head", "UpperArm.L", "Forearm.L", "Hand.L",
         "UpperArm.R", "Forearm.R", "Hand.R", "Thigh.L", "Shin.L", "Foot.L",
         "Thigh.R", "Shin.R", "Foot.R"}
CLIPS = {"Idle", "Walk", "Run", "Attack1", "Attack2", "Attack3", "Dodge", "Block",
         "Hit", "Death", "Cast"}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--character", action="store_true")
    args = parser.parse_args(sys.argv[sys.argv.index("--") + 1:])
    source = args.input.resolve()
    if not source.is_file():
        raise ValueError(f"Missing source: {source}")
    if args.output.suffix.lower() != ".glb":
        raise ValueError("The runtime export must be .glb")
    if source.suffix.lower() == ".blend":
        bpy.ops.wm.open_mainfile(filepath=str(source))
    else:
        bpy.ops.object.select_all(action="SELECT")
        bpy.ops.object.delete(use_global=False)
        if source.suffix.lower() == ".fbx":
            bpy.ops.import_scene.fbx(filepath=str(source), automatic_bone_orientation=False)
        elif source.suffix.lower() == ".obj":
            bpy.ops.wm.obj_import(filepath=str(source))
        elif source.suffix.lower() in (".glb", ".gltf"):
            bpy.ops.import_scene.gltf(filepath=str(source))
        else:
            raise ValueError("Supported sources: FBX, OBJ, BLEND, glTF, GLB")
    bpy.context.scene.unit_settings.system = "METRIC"
    bpy.context.scene.unit_settings.scale_length = 1.0
    if args.character:
        armatures = [o for o in bpy.context.scene.objects if o.type == "ARMATURE"]
        if len(armatures) != 1:
            raise ValueError("Characters require exactly one armature")
        rig = armatures[0]
        missing = BONES - {bone.name for bone in rig.data.bones}
        if missing:
            raise ValueError(f"Missing bones: {sorted(missing)}")
        missing_clips = CLIPS - {a.name for a in bpy.data.actions}
        if missing_clips:
            raise ValueError(f"Missing clips: {sorted(missing_clips)}")
        if any(abs(component - 1.0) > 0.001 for component in rig.scale):
            raise ValueError("Apply armature scale before export; runtime expects (1, 1, 1)")
        # Unlinked actions are otherwise silently omitted by Blender's exporter.
        for action in bpy.data.actions:
            action.use_fake_user = True
    args.output.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(filepath=str(args.output.resolve()), export_format="GLB",
                              export_yup=True, export_animations=True,
                              export_animation_mode="ACTIONS", export_skins=True,
                              export_apply=False, export_texcoords=True, export_normals=True)
    print(f"Exported {args.output.resolve()}")

if __name__ == "__main__":
    main()
