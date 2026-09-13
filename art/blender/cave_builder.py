"""Cave modules for the CAVERNS: rock instead of masonry.

WorldView tries these ids before the masonry kit when the region is CAVERNS (see buildTiles):

  kit_floor_cave, kit_floor_cave2        2.80 x 0.24 x 2.80, pivot bottom-centre, top relief
  kit_ceiling_cave, kit_ceiling_cave2    2.80 x 0.36 x 2.80 plus stalactites hanging below
  kit_wall_cave_face, kit_wall_cave_face2, kit_wall_cave_corner, kit_wall_cave_span,
  kit_wall_cave_pier                     2.80 x 7.12 x 2.80 blocks, visible sides in relief

Same contract as docs/dungeon-kit.md: bottom-centre pivots, the module fills the whole cell, a
wall's unrotated visible side faces glTF +Z (Blender -Y), corner +Z/+X, span +Z/-Z, pier
+Z/+X/-Z. Every relief fades to zero at the cell edges so neighbours meet without cracks - the
price is that the pattern repeats per cell, which the two variants and the region tint hide well
enough in torchlight. The floor rises no more than 3 cm over its nominal top (the physics floor
is flat) and dips up to 9 cm; stalactites reach 0.9 m below the ceiling slab, well over a head.
Flat shading throughout: the facets are what catch the torches. Material PQC_Rock carries the
cave rock albedo (art/textures/kit_cave_albedo.png: dark cracked stone, mean linear luminance
0.089, inside the wall band of docs/dungeon-kit.md section 6; the mottled boulder albedo at 0.118
made the first walls read as sand under the torches); WorldView multiplies the region tone into
BaseColor like for every module.
"""

import bpy, bmesh, os, math, random, sys
from mathutils import Vector

ROOT = r"C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered"
OUT = os.path.join(ROOT, "art", "gen", "props")
TEX = os.path.join(ROOT, "art", "textures", "kit_cave_albedo.png")
CELL, HALF = 2.8, 1.4


# ---- deterministic value noise -------------------------------------------------------------
def _hash(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 977) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def _smooth(t):
    return t * t * (3 - 2 * t)


def vnoise(x, y, z, seed):
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = _smooth(x - ix), _smooth(y - iy), _smooth(z - iz)
    def c(dx, dy, dz):
        return _hash(ix + dx, iy + dy, iz + dz, seed)
    x00 = c(0, 0, 0) + (c(1, 0, 0) - c(0, 0, 0)) * fx
    x10 = c(0, 1, 0) + (c(1, 1, 0) - c(0, 1, 0)) * fx
    x01 = c(0, 0, 1) + (c(1, 0, 1) - c(0, 0, 1)) * fx
    x11 = c(0, 1, 1) + (c(1, 1, 1) - c(0, 1, 1)) * fx
    y0 = x00 + (x10 - x00) * fy
    y1 = x01 + (x11 - x01) * fy
    return y0 + (y1 - y0) * fz


def fbm(x, y, z, seed, octaves=3):
    total, amplitude, frequency, norm = 0.0, 1.0, 1.0, 0.0
    for _ in range(octaves):
        total += vnoise(x * frequency, y * frequency, z * frequency, seed) * amplitude
        norm += amplitude
        amplitude *= .5
        frequency *= 2.1
    return total / norm  # 0..1, mean ~.5


def edge_fade(u, width=.16):
    """1 in the middle of a span, 0 at both ends; u in 0..1."""
    a = min(1.0, u / width)
    b = min(1.0, (1 - u) / width)
    return _smooth(a) * _smooth(b)


# ---- mesh helpers -----------------------------------------------------------------------------
def finish(name, bm, uv_scale=1 / CELL):
    """Box-projected UVs, flat shading, mesh + object, material. Every module shows the same
    texture window per cell; the second variant of each kind shifts its window so two
    neighbouring cells do not repeat the same cracks."""
    shift = (.5, .37) if name.endswith("2") else (0, 0)
    uv = bm.loops.layers.uv.new("UVMap")
    bm.normal_update()
    for f in bm.faces:
        f.smooth = False
        n = f.normal
        axis = max(range(3), key=lambda i: abs(n[i]))
        for loop in f.loops:
            p = loop.vert.co
            a, b = [(p.y, p.z), (p.x, p.z), (p.x, p.y)][axis]
            loop[uv].uv = (a * uv_scale + shift[0], b * uv_scale + shift[1])
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    me = bpy.data.meshes.new(name + "Mesh")
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    me.materials.append(material())
    return ob


_material = None


def material():
    global _material
    if _material is None:
        m = bpy.data.materials.new("PQC_Rock")
        m.use_nodes = True
        bsdf = m.node_tree.nodes["Principled BSDF"]
        bsdf.inputs["Roughness"].default_value = .95
        bsdf.inputs["Metallic"].default_value = 0
        tex = m.node_tree.nodes.new("ShaderNodeTexImage")
        tex.image = bpy.data.images.load(TEX)
        m.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
        _material = m
    return _material


def grid_faces(bm, verts, nu, nv):
    for i in range(nu):
        for j in range(nv):
            bm.faces.new((verts[i][j], verts[i + 1][j], verts[i + 1][j + 1], verts[i][j + 1]))


def slab(name, seed, thickness, relief, n=10):
    """A cell-sized slab from z=0 to z=thickness; relief(u, v) displaces the top (dz)."""
    bm = bmesh.new()
    top = [[None] * (n + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        for j in range(n + 1):
            u, v = i / n, j / n
            x, y = -HALF + u * CELL, -HALF + v * CELL
            dz = relief(u, v, x, y) * edge_fade(u) * edge_fade(v)
            top[i][j] = bm.verts.new((x, y, thickness + dz))
    grid_faces(bm, top, n, n)
    corners = [bm.verts.new((sx * HALF, sy * HALF, 0)) for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1))]
    # Sides: fan from each top edge segment down to the bottom corners.
    edges = [
        [top[i][0] for i in range(n + 1)],            # y = -HALF, from x- to x+
        [top[n][j] for j in range(n + 1)],            # x = +HALF
        [top[i][n] for i in reversed(range(n + 1))],  # y = +HALF, from x+ to x-
        [top[0][j] for j in reversed(range(n + 1))],  # x = -HALF
    ]
    for k, edge in enumerate(edges):
        a, b = corners[k], corners[(k + 1) % 4]
        bm.faces.new([a, b] + list(reversed(edge)))
    bm.faces.new(list(reversed(corners)))
    return finish(name, bm)


def floor_module(name, seed):
    def relief(u, v, x, y):
        h = (fbm(x * 1.3 + 7, y * 1.3 + 3, seed * .37, seed, 3) - .5) * .18
        return min(.03, h)  # dips, and only a whisper of a rise: the physics floor is flat
    ob = slab(name, seed, .24, relief, n=10)
    return ob


def ceiling_module(name, seed):
    # Built upside down as a slab with its relief on top, then mirrored so the relief hangs.
    rnd = random.Random(seed)
    tips = [(rnd.uniform(-.9, .9), rnd.uniform(-.9, .9), rnd.uniform(.45, .9), rnd.uniform(.14, .24))
            for _ in range(rnd.randint(2, 3))]

    def relief(u, v, x, y):
        h = fbm(x * 1.1 + 5, y * 1.1 + 9, seed * .53, seed, 3) * .6
        for tx, ty, th, tr in tips:
            d = math.hypot(x - tx, y - ty)
            if d < tr * 2.2:
                h = max(h, th * max(0.0, 1 - d / (tr * 2.2)) ** 1.6)
        return h
    ob = slab(name, seed, .36, relief, n=12)
    # Mirror in Z: the flat face becomes the top at z = .36 (WorldView places the module's
    # origin at the slab's underside, 6.92 m) and the relief hangs below z = 0 into the room.
    me = ob.data
    for v in me.vertices:
        v.co.z = .36 - v.co.z
    for p in me.polygons:
        p.flip()
    me.update()
    return ob


SIDES = ("-Y", "+X", "+Y", "-X")
VISIBLE = {
    "face": {"-Y"},
    "corner": {"-Y", "+X"},
    "span": {"-Y", "+Y"},
    "pier": {"-Y", "+X", "+Y"},
}
Z0, Z1 = -.10, 7.02


def wall_module(name, kind, seed, nu=7, nv=12):
    visible = VISIBLE[kind]
    bm = bmesh.new()
    corners = {  # x, y of the four vertical edges, counter-clockwise from (-,-)
        0: (-HALF, -HALF), 1: (HALF, -HALF), 2: (HALF, HALF), 3: (-HALF, HALF)}
    normals = {"-Y": (0, -1), "+X": (1, 0), "+Y": (0, 1), "-X": (-1, 0)}
    # Shared vertical edge columns so sides meet exactly.
    columns = {k: [bm.verts.new((cx, cy, Z0 + (Z1 - Z0) * j / nv)) for j in range(nv + 1)]
               for k, (cx, cy) in corners.items()}
    for k, side in enumerate(SIDES):
        a, b = corners[k], corners[(k + 1) % 4]
        nx, ny = normals[side]
        if side not in visible:
            # Uses the whole shared columns, so the block stays a closed manifold.
            bm.faces.new(list(columns[k]) + list(reversed(columns[(k + 1) % 4])))
            continue
        grid = [columns[k]]
        for i in range(1, nu):
            u = i / nu
            x, y = a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u
            col = []
            for j in range(nv + 1):
                v = j / nv
                z = Z0 + (Z1 - Z0) * v
                r = fbm(x * 1.1 + nx * 3, y * 1.1 + ny * 3, z * .6 + seed, seed, 4) - .45
                d = r * .85 * edge_fade(u, .1) * (1 - _smooth(max(0.0, (v - .9) / .1)))
                # Never a recess at the floor line: the floor tile ends at the cell edge and a
                # hollow there would show its underside. Bulges are welcome down there (talus).
                d = max(-.2 * _smooth(min(1.0, v / .15)), min(.36, d))
                col.append(bm.verts.new((x + nx * d, y + ny * d, z)))
            grid.append(col)
        grid.append(columns[(k + 1) % 4])
        grid_faces(bm, grid, nu, nv)
    bm.faces.new([columns[k][0] for k in (3, 2, 1, 0)])
    bm.faces.new([columns[k][nv] for k in (0, 1, 2, 3)])
    return finish(name, bm)


def export(ob, name):
    bpy.ops.object.select_all(action="DESELECT")
    ob.select_set(True)
    bpy.context.view_layer.objects.active = ob
    path = os.path.join(OUT, name + ".glb")
    bpy.ops.export_scene.gltf(filepath=path, export_format="GLB", use_selection=True,
                              export_yup=True, export_apply=True, export_animations=False)
    d = ob.dimensions
    print(f"[CAVE] {name:<22} {d.x:.2f} x {d.z:.2f} x {d.y:.2f} m (w x h x d), "
          f"{sum(len(p.vertices) - 2 for p in ob.data.polygons)} tris, {os.path.getsize(path)} bytes")


def main():
    bpy.ops.wm.read_homefile(use_empty=True)
    os.makedirs(OUT, exist_ok=True)
    built = [
        ("kit_floor_cave", floor_module("kit_floor_cave", 11)),
        ("kit_floor_cave2", floor_module("kit_floor_cave2", 23)),
        ("kit_ceiling_cave", ceiling_module("kit_ceiling_cave", 31)),
        ("kit_ceiling_cave2", ceiling_module("kit_ceiling_cave2", 47)),
        ("kit_wall_cave_face", wall_module("kit_wall_cave_face", "face", 5)),
        ("kit_wall_cave_face2", wall_module("kit_wall_cave_face2", "face", 17)),
        ("kit_wall_cave_corner", wall_module("kit_wall_cave_corner", "corner", 29)),
        ("kit_wall_cave_span", wall_module("kit_wall_cave_span", "span", 41)),
        ("kit_wall_cave_pier", wall_module("kit_wall_cave_pier", "pier", 53)),
    ]
    for name, ob in built:
        export(ob, name)


main()
