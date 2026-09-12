
# Parametric generator for the PentagonQuest dungeon wall modules.
# One profile, four visible-side combinations -> kit_wall_face / _corner / _span / _pier / kit_wall.
#
# Coordinate contract (see docs/dungeon-kit.md section 12):
#   Blender is Z-up and the glTF exporter runs with export_yup=True, which maps
#   Blender +Z -> glTF +Y and Blender +Y -> glTF -Z. So a face that must look
#   toward glTF +Z is built toward Blender -Y.
import bpy, bmesh, math

CELL = 2.80
HALF = CELL / 2.0          # 1.40
SEG  = 2                   # segments per cell side

# (height z, outward offset relative to HALF) - taken vertex for vertex from the
# delivered kit_wall_face so every variant carries the identical silhouette.
PROFILE = [
    (0.00,  0.06),   # plinth foot, protrudes into the room
    (0.45,  0.06),   # plinth top, outer edge
    (0.45,  0.00),   # step back onto the wall face
    (2.40,  0.04),   # slight batter up to the string course
    (2.70,  0.04),   # string course top
    (2.70,  0.00),   # step back onto the wall face
    (6.80,  0.00),   # wall face top
    (7.02, -0.10),   # top chamfer, hidden inside the ceiling slab
]

# Blender side order, counter-clockwise seen from +Z, starting at -Y.
# -Y is the side that ends up as glTF +Z.
SIDES = ("-Y", "+X", "+Y", "-X")

VISIBLE = {
    "kit_wall_face":   {"-Y"},
    "kit_wall_corner": {"-Y", "+X"},
    "kit_wall_span":   {"-Y", "+Y"},
    "kit_wall_pier":   {"-Y", "+X", "+Y"},
    "kit_wall":        {"-Y", "+X", "+Y", "-X"},
    "kit_wall_free":   {"-Y", "+X", "+Y", "-X"},
}


def _offsets(visible, oz):
    """Outward offset per side at one profile level."""
    return {s: (oz if s in visible else 0.0) for s in SIDES}


def _ring(visible, z, oz):
    """One closed horizontal cross-section as a list of (x, y, z)."""
    o = _offsets(visible, oz)
    xp, xm = HALF + o["+X"], -(HALF + o["-X"])
    yp, ym = HALF + o["+Y"], -(HALF + o["-Y"])
    # corners in the same order as SIDES: each side runs from corner i to corner i+1
    corners = [(xm, ym), (xp, ym), (xp, yp), (xm, yp)]
    pts = []
    for i in range(4):
        ax, ay = corners[i]
        bx, by = corners[(i + 1) % 4]
        for k in range(SEG):
            t = k / SEG
            pts.append((ax + (bx - ax) * t, ay + (by - ay) * t, z))
    return pts


def build(name, visible):
    rings = [_ring(visible, z, oz) for z, oz in PROFILE]
    n = len(rings[0])

    bm = bmesh.new()
    vrings = []
    for r in rings:
        vrings.append([bm.verts.new(p) for p in r])
    bm.verts.index_update()

    def area(vs):
        # rough quad area; skips the zero-height slivers a hidden side produces
        import mathutils
        a = (vs[1].co - vs[0].co).cross(vs[2].co - vs[0].co).length
        b = (vs[2].co - vs[0].co).cross(vs[3].co - vs[0].co).length
        return 0.5 * (a + b)

    for k in range(len(vrings) - 1):
        lo, hi = vrings[k], vrings[k + 1]
        for i in range(n):
            j = (i + 1) % n
            vs = [lo[i], lo[j], hi[j], hi[i]]
            if len({v.co.copy().freeze() for v in vs}) < 3:
                continue
            if area(vs) < 1e-7:
                continue
            bm.faces.new(vs)

    bm.faces.new(list(reversed(vrings[0])))   # bottom cap, normal toward -Z
    bm.faces.new(list(vrings[-1]))            # top cap, normal toward +Z

    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    me = bpy.data.meshes.new(name + "Mesh")
    bm.to_mesh(me)
    bm.free()

    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    for p in me.polygons:
        p.use_smooth = False
    return ob
