
"""Throne, altars and shrine as readable furniture instead of brick blocks.

The first delivery built every interactable from the same brick texture as the walls: the throne
was a 5.1 m brick monolith with a black plate, the three altars brick cubes on brick plinths. This
generator rebuilds them from boxes and octagonal prisms with chamfered edges, a worn carved-stone
albedo (art/textures/prop_carved_albedo.png, no bricks) and the kit's riveted iron.

Contract (docs/assets/ASSETS.md): metres, pivot bottom-centre, Blender Z up and the glTF exporter run
with export_yup=True, so a face that must look at glTF +Z is built towards Blender -Y. The glow
parts stay in code (WorldView keeps its Crystal child), so nothing here emits light: the shrine's
embers sit at z 0.52-0.62 under the crystal that WorldView hangs at y 0.68-1.43, the altar tops end
at 1.22 under the crystal at 1.33-1.67, and the throne's ember glow at y 2.5 hovers over a seat top
at 1.39 in front of a back that rises to 3.9.

Footprints stay within the delivered ones, which is what the interaction radius and the camera
were tuned against: throne 2.6 x 2.3, altar 1.3 x 0.9, shrine 1.4 x 1.4. Only the throne's height
changes, 5.1 -> 3.9 m.

Materials within STYLE.md: carved stone rough 0.9 / metallic 0, iron 0.6 / 0.35, ash 0.95 / 0.
"""

import bpy, bmesh, math, os, sys
from mathutils import Vector, Matrix

ROOT = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
TEX = os.path.join(ROOT, "art", "textures")
OUT = os.path.join(ROOT, "art", "gen", "props")
SLOTS = ["PQP_Carved", "PQK_Iron", "PQP_Ash"]
TEXTURES = {"PQP_Carved": ("prop_carved_albedo.png", .9, 0.0, .55),
            "PQK_Iron": ("kit_iron_albedo.png", .6, .35, .5),
            "PQP_Ash": (None, .95, 0.0, 1.0)}
ASH_COLOR = (0.06, 0.055, 0.05, 1)


def material(name):
    image, rough, metal, scale = TEXTURES[name]
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nodes = m.node_tree.nodes
    bsdf = nodes["Principled BSDF"]
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Metallic"].default_value = metal
    if image is None:
        bsdf.inputs["Base Color"].default_value = ASH_COLOR
    else:
        tex = nodes.new("ShaderNodeTexImage")
        tex.image = bpy.data.images.load(os.path.join(TEX, image))
        m.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    return m


class Builder:
    def __init__(self):
        self.bm = bmesh.new()

    def _add(self, geom, slot, matrix):
        verts = [g for g in geom if isinstance(g, bmesh.types.BMVert)]
        bmesh.ops.transform(self.bm, matrix=matrix, verts=verts)
        for f in {f for v in verts for f in v.link_faces}:
            f.material_index = SLOTS.index(slot)
            f.smooth = False

    def box(self, cx, cy, z0, sx, sy, sz, slot="PQP_Carved"):
        """Full sizes; z0 is the bottom."""
        r = bmesh.ops.create_cube(self.bm, size=1.0)
        m = Matrix.Translation((cx, cy, z0 + sz / 2)) @ Matrix.Diagonal((sx, sy, sz, 1))
        self._add(r["verts"], slot, m)

    def prism(self, cx, cy, z0, r_bottom, r_top, height, segments=8, slot="PQP_Carved", twist=0.0):
        r = bmesh.ops.create_cone(self.bm, cap_ends=True, segments=segments,
                                  radius1=r_bottom, radius2=r_top, depth=height)
        m = (Matrix.Translation((cx, cy, z0 + height / 2))
             @ Matrix.Rotation(math.radians(twist), 4, "Z"))
        self._add(r["verts"], slot, m)

    def finish(self, name, bevel=0.02, uv_scale=None):
        bm = self.bm
        bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=1e-5)
        if bevel > 0:
            bmesh.ops.bevel(bm, geom=bm.edges[:] + bm.verts[:], offset=bevel, segments=1,
                            affect="EDGES", profile=0.7, clamp_overlap=True)
        bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
        # Box-projected UVs so the stone reads at the same scale on every face.
        uv = bm.loops.layers.uv.new("UVMap")
        for f in bm.faces:
            n = f.normal
            axis = max(range(3), key=lambda i: abs(n[i]))
            scale = (uv_scale or {}).get(SLOTS[f.material_index], TEXTURES[SLOTS[f.material_index]][3])
            for loop in f.loops:
                p = loop.vert.co
                a, b = [(p.y, p.z), (p.x, p.z), (p.x, p.y)][axis]
                loop[uv].uv = (a / scale, b / scale)
        me = bpy.data.meshes.new(name + "Mesh")
        bm.to_mesh(me)
        bm.free()
        ob = bpy.data.objects.new(name, me)
        bpy.context.scene.collection.objects.link(ob)
        for slot in SLOTS:
            ob.data.materials.append(bpy.data.materials[slot])
        return ob


def throne():
    b = Builder()
    b.box(0, 0, 0.00, 2.60, 2.30, 0.22)            # dais, lower step
    b.box(0, 0.05, 0.22, 2.10, 1.85, 0.22)         # dais, upper step  -> 0.44
    b.box(0, 0.08, 0.44, 1.50, 1.05, 0.95)         # seat block        -> 1.39
    b.box(0, 0.02, 1.39, 1.30, 0.86, 0.06, "PQP_Ash")   # worn cushion
    for sx in (-1, 1):
        b.box(sx * 0.90, 0.08, 1.39, 0.30, 1.00, 0.32)         # armrest
        b.box(sx * 0.90, 0.08, 1.71, 0.34, 1.04, 0.05, "PQK_Iron")  # iron cap
        b.box(sx * 0.90, -0.40, 0.44, 0.34, 0.34, 0.95)        # front leg post
    b.box(0, 0.74, 0.44, 1.60, 0.28, 2.50)         # back slab         -> 2.94
    b.box(0, 0.74, 2.94, 0.36, 0.28, 0.96)         # central spike     -> 3.90
    for sx in (-1, 1):
        b.box(sx * 0.62, 0.74, 2.94, 0.30, 0.28, 0.56)   # side spikes  -> 3.50
    for z in (1.10, 2.10, 2.75):
        b.box(0, 0.74, z, 1.66, 0.34, 0.08, "PQK_Iron")  # iron bands across the back
    b.box(0, 0.58, 1.45, 1.20, 0.06, 1.30, "PQP_Ash")    # dark backrest cloth in front of the slab
    return b.finish("throne", bevel=0.02)


def altar(name):
    b = Builder()
    b.box(0, 0, 0.00, 1.30, 0.90, 0.14)            # plinth
    b.box(0, 0, 0.14, 1.00, 0.62, 0.90)            # shaft             -> 1.04
    b.box(0, 0, 0.58, 1.06, 0.68, 0.06, "PQK_Iron")   # iron band
    b.box(0, 0, 1.04, 1.30, 0.90, 0.16)            # top slab          -> 1.20
    b.prism(0, 0, 1.20, 0.30, 0.30, 0.02, 8, "PQK_Iron", 22.5)   # rune disc
    b.prism(0, 0, 1.22, 0.16, 0.16, 0.01, 8, "PQP_Ash", 22.5)    # ash-filled centre
    return b.finish(name, bevel=0.015)


def shrine():
    b = Builder()
    b.prism(0, 0, 0.00, 0.72, 0.72, 0.14, 8, "PQP_Carved", 22.5)   # octagonal base
    b.prism(0, 0, 0.14, 0.50, 0.63, 0.38, 8, "PQP_Carved", 22.5)   # bowl        -> 0.52
    b.prism(0, 0, 0.14, 0.44, 0.44, 0.10, 8, "PQK_Iron", 22.5)    # iron collar
    b.prism(0, 0, 0.52, 0.52, 0.30, 0.10, 8, "PQP_Ash", 22.5)      # heap of embers -> 0.62
    return b.finish("shrine", bevel=0.015)


def export(ob, path):
    for o in bpy.data.objects:
        o.select_set(o is ob)
    bpy.context.view_layer.objects.active = ob
    bpy.ops.export_scene.gltf(filepath=path, export_format="GLB", use_selection=True,
                              export_yup=True, export_apply=True, export_animations=False,
                              export_materials="EXPORT", export_image_format="AUTO")
    d = ob.dimensions
    tris = sum(len(p.vertices) - 2 for p in ob.data.polygons)
    print(f"[PROP] {ob.name:<7} {d.x:.2f} x {d.z:.2f} x {d.y:.2f} m (w x h x d), {tris} tris, "
          f"{os.path.getsize(path)} bytes")


def main():
    bpy.ops.wm.read_homefile(use_empty=True)
    os.makedirs(OUT, exist_ok=True)
    for slot in SLOTS:
        material(slot)
    wanted = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    builders = {"throne": throne, "shrine": shrine,
                "rune": lambda: altar("rune"), "seal": lambda: altar("seal"), "lore": lambda: altar("lore")}
    for name, build in builders.items():
        if wanted and name not in wanted:
            continue
        ob = build()
        export(ob, os.path.join(OUT, name + ".glb"))


main()
