
"""kit_rock: a boulder instead of a brick dome.

WorldView.rock() scales the module by (s, 1.6 s, s), sets it at y = 0.6 s and gives it a box
collider of half extents (0.7 s, s, 0.7 s) - the frame of the placeholder unit sphere, whose
centre pivot is the one documented exception to bottom-centre (docs/assets/dungeon-kit.md, section 5).
So this is a unit-radius shape around the origin: an icosphere with a smoothed random radial
displacement, flat-shaded so the facets catch the torches, wearing the mottled cave rock albedo
(art/textures/kit_rock_mottled_albedo.png, generated without any brick pattern).
"""

import bpy, bmesh, os, random
from mathutils import Vector

ROOT = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
OUT = os.path.join(ROOT, "art", "gen", "props")


def main():
    bpy.ops.wm.read_homefile(use_empty=True)
    os.makedirs(OUT, exist_ok=True)
    random.seed(19)
    bm = bmesh.new()
    bmesh.ops.create_icosphere(bm, subdivisions=3, radius=1.0)
    # Two octaves of vertex noise: a coarse one shared by neighbours (via averaging) and a fine one.
    coarse = {v: random.uniform(-.22, .22) for v in bm.verts}
    for _ in range(2):
        coarse = {v: (coarse[v] + sum(coarse[e.other_vert(v)] for e in v.link_edges) / len(v.link_edges)) / 2
                  for v in bm.verts}
    for v in bm.verts:
        r = 1 + coarse[v] + random.uniform(-.05, .05)
        v.co = v.co.normalized() * r
        v.co.z *= .92   # a boulder sits a little squat
    uv = bm.loops.layers.uv.new("UVMap")
    for f in bm.faces:
        f.smooth = False
        n = f.normal
        axis = max(range(3), key=lambda i: abs(n[i]))
        for loop in f.loops:
            p = loop.vert.co
            a, b = [(p.y, p.z), (p.x, p.z), (p.x, p.y)][axis]
            loop[uv].uv = (a / 1.6, b / 1.6)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    me = bpy.data.meshes.new("kit_rockMesh")
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new("kit_rock", me)
    bpy.context.scene.collection.objects.link(ob)
    m = bpy.data.materials.new("PQP_Rock")
    m.use_nodes = True
    bsdf = m.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Roughness"].default_value = .92
    bsdf.inputs["Metallic"].default_value = 0
    tex = m.node_tree.nodes.new("ShaderNodeTexImage")
    tex.image = bpy.data.images.load(os.path.join(ROOT, "art", "textures", "kit_rock_mottled_albedo.png"))
    m.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    me.materials.append(m)
    ob.select_set(True)
    bpy.context.view_layer.objects.active = ob
    path = os.path.join(OUT, "kit_rock.glb")
    bpy.ops.export_scene.gltf(filepath=path, export_format="GLB", use_selection=True,
                              export_yup=True, export_apply=True, export_animations=False)
    d = ob.dimensions
    print(f"[PROP] kit_rock {d.x:.2f} x {d.z:.2f} x {d.y:.2f} m, {len(me.polygons)} faces, "
          f"{os.path.getsize(path)} bytes")


main()
