
# Turns a Meshy character into a PentagonQuest enemy GLB.
#
# The recipe is the one proven on the orc (docs/assets/asset-liste.md, "Ork integriert"):
# weld UV-seam duplicates, normalise to 1.95 m, bring the arms into Eren's rest pose with a
# geodesic weight field, decimate, then bind Eren's rig UNCHANGED - only the finger bones are
# dropped - so Eren's own clips run without retargeting.
#
# Nothing here guesses the facing direction. render_views() writes orthographic views and the
# caller looks at them; turn() applies the correction. That order is deliberate: two different
# geometric heuristics got the orc's facing wrong, a render did not.
import bpy, bmesh, heapq, math, mathutils, os, re

HEIGHT = 1.95                 # STYLE.md delivery size, EnemyType scales in game
TRI_BUDGET = 20000            # STYLE.md
WELD = 0.0005
WRIST_D = 0.28                # geodesic radius that captures hand plus claws
FULL, ZERO = 0.62, 0.76       # arm weight ramp, measured on the orc
WEIGHT_JUMP_LIMIT = 0.40      # a stationary vertex beside a moved one is what makes spikes
# Eren's rest pose runs the arm almost straight along X, 2.7 degrees below horizontal.
ARM_TARGET = mathutils.Vector((1.0, -0.002, -0.048)).normalized()
FINGER = re.compile(r"Hand(Index|Middle|Pinky|Ring|Thumb)\d", re.I)
CLIPS = ["Idle","Walk","Run","Attack1","Attack2","Attack3","Dodge","Block","Hit","Death","Cast"]
LR, LG, LB = 0.2126, 0.7152, 0.0722


def _drop(name):
    for o in [o for o in bpy.data.objects if o.name == name or o.name.startswith(name + ".")]:
        bpy.data.objects.remove(o, do_unlink=True)


def _cen(vs):
    n = len(vs)
    return mathutils.Vector((sum(c[0] for c in vs)/n, sum(c[1] for c in vs)/n,
                             sum(c[2] for c in vs)/n))


def _geodesic(bm, seed, cap):
    INF = float("inf")
    d = [INF]*len(bm.verts)
    d[seed] = 0.0
    pq = [(0.0, seed)]
    while pq:
        dv, i = heapq.heappop(pq)
        if dv > d[i] + 1e-12 or dv > cap:
            continue
        v = bm.verts[i]
        for e in v.link_edges:
            w = e.other_vert(v)
            nd = dv + e.calc_length()
            if nd < d[w.index]:
                d[w.index] = nd
                heapq.heappush(pq, (nd, w.index))
    return d


def _sstep(a, b, x):
    if x <= a: return 1.0
    if x >= b: return 0.0
    t = (x - a) / (b - a)
    return 1.0 - t*t*(3 - 2*t)


def load(path, name):
    """Import, weld the UV-seam duplicates, normalise. Returns a report; no geometry decisions."""
    _drop(name)
    before = set(o.name for o in bpy.data.objects)
    bpy.ops.import_scene.gltf(filepath=path)
    new = [o for o in bpy.data.objects if o.name not in before]
    ob = [o for o in new if o.type == "MESH"][0]
    ob.name = name
    ob.data.name = name + "Mesh"
    bpy.ops.object.select_all(action="DESELECT")
    bpy.context.view_layer.objects.active = ob
    ob.select_set(True)
    bpy.ops.object.parent_clear(type="CLEAR_KEEP_TRANSFORM")
    for o in new:
        if o is not ob:
            bpy.data.objects.remove(o, do_unlink=True)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

    v0 = len(ob.data.vertices)
    bm = bmesh.new(); bm.from_mesh(ob.data)
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=WELD)
    bm.to_mesh(ob.data); bm.free(); ob.data.update()

    bm = bmesh.new(); bm.from_mesh(ob.data); bm.verts.ensure_lookup_table()
    seen = set(); comps = []
    for v in bm.verts:
        if v.index in seen: continue
        stack = [v]; seen.add(v.index); c = 0
        while stack:
            w = stack.pop(); c += 1
            for e in w.link_edges:
                o2 = e.other_vert(w)
                if o2.index not in seen:
                    seen.add(o2.index); stack.append(o2)
        comps.append(c)
    nonman = sum(1 for e in bm.edges if not e.is_manifold)
    bm.free()

    co = [v.co.copy() for v in ob.data.vertices]
    mn = [min(c[i] for c in co) for i in range(3)]
    mx = [max(c[i] for c in co) for i in range(3)]
    s = HEIGHT / (mx[2] - mn[2])
    cx, cy = (mn[0]+mx[0])/2, (mn[1]+mx[1])/2
    for v in ob.data.vertices:
        v.co = mathutils.Vector(((v.co[0]-cx)*s, (v.co[1]-cy)*s, (v.co[2]-mn[2])*s))
    ob.data.update()
    ob.data.calc_loop_triangles()
    return {"verts_before": v0, "verts_after": len(ob.data.vertices),
            "tris": len(ob.data.loop_triangles), "components": sorted(comps, reverse=True)[:4],
            "nonmanifold_edges": nonman, "scale_applied": round(s, 5)}


def turn(name, degrees=180.0):
    ob = bpy.data.objects[name]
    R = mathutils.Matrix.Rotation(math.radians(degrees), 4, 'Z')
    for v in ob.data.vertices:
        v.co = R @ v.co
    ob.data.update()
    return {"turned": degrees}


def weld_check(name):
    """Is the hand welded to a skirt or loincloth? If not, the arm needs no spatial gate."""
    ob = bpy.data.objects[name]
    bm = bmesh.new(); bm.from_mesh(ob.data); bm.verts.ensure_lookup_table()
    out = {}
    for side, sgn in (("L", 1.0), ("R", -1.0)):
        tip = max(bm.verts, key=lambda v: sgn * v.co[0])
        d = _geodesic(bm, tip.index, 0.55)
        reach = [bm.verts[i].co for i in range(len(bm.verts)) if d[i] <= 0.50]
        out[side] = {"n": len(reach),
                     "x_nearest_centre": round(min(sgn*c[0] for c in reach), 3),
                     "z_lowest": round(min(c[2] for c in reach), 3)}
    bm.free()
    return out


def tpose(name, passes=2):
    """Rotate each arm onto Eren's rest direction with a geodesic weight ramp."""
    ob = bpy.data.objects[name]
    rep = {"passes": []}
    for _ in range(passes):
        bm = bmesh.new(); bm.from_mesh(ob.data)
        bm.verts.ensure_lookup_table(); bm.edges.ensure_lookup_table()
        NV = len(bm.verts)
        step = {}
        newco = {}
        for side, sgn in (("L", 1.0), ("R", -1.0)):
            tip = max(bm.verts, key=lambda v: sgn * v.co[0])
            d = _geodesic(bm, tip.index, ZERO + 0.05)
            band = [bm.verts[i].co for i in range(NV)
                    if 0.60 <= d[i] <= 0.70 and bm.verts[i].co[2] > 1.35]
            if not band:
                step[side] = {"error": "no shoulder ring found"}
                continue
            P = _cen(band)
            H = _cen([bm.verts[i].co for i in range(NV) if d[i] <= WRIST_D])
            u = (H - P).normalized()
            t = mathutils.Vector((sgn*ARM_TARGET.x, ARM_TARGET.y, ARM_TARGET.z)).normalized()
            axis = u.cross(t); ang = u.angle(t)
            if axis.length < 1e-9:
                step[side] = {"angle_deg": 0.0}
                continue
            axis.normalize()
            ws = [_sstep(FULL, ZERO, d[i]) if d[i] <= ZERO else 0.0 for i in range(NV)]
            jump = max(abs(ws[e.verts[0].index] - ws[e.verts[1].index]) for e in bm.edges)
            for i in range(NV):
                if ws[i] <= 0.0: continue
                R = mathutils.Matrix.Rotation(ang*ws[i], 4, axis)
                base = newco.get(i, bm.verts[i].co)
                newco[i] = P + (R @ (base - P))
            step[side] = {"pivot": [round(x, 3) for x in P], "angle_deg": round(math.degrees(ang), 2),
                          "max_edge_weight_jump": round(jump, 3),
                          "over_limit": jump > WEIGHT_JUMP_LIMIT}
        for i, c in newco.items():
            bm.verts[i].co = c
        bm.to_mesh(ob.data); bm.free(); ob.data.update()
        rep["passes"].append(step)
    # residual drop against Eren's -2.7 degrees
    bm = bmesh.new(); bm.from_mesh(ob.data); bm.verts.ensure_lookup_table()
    NV = len(bm.verts)
    for side, sgn in (("L", 1.0), ("R", -1.0)):
        tip = max(bm.verts, key=lambda v: sgn * v.co[0])
        d = _geodesic(bm, tip.index, 0.72)
        band = [bm.verts[i].co for i in range(NV) if 0.60 <= d[i] <= 0.70 and bm.verts[i].co[2] > 1.35]
        if not band: continue
        P = _cen(band); H = _cen([bm.verts[i].co for i in range(NV) if d[i] <= WRIST_D])
        v = H - P
        rep["drop_deg_" + side] = round(math.degrees(math.asin(v.z / v.length)), 2)
        rep["wrist_" + side] = [round(x, 3) for x in H]
    bm.free()
    return rep


def decimate(name):
    ob = bpy.data.objects[name]
    ob.data.calc_loop_triangles()
    before = len(ob.data.loop_triangles)
    bpy.ops.object.select_all(action="DESELECT")
    bpy.context.view_layer.objects.active = ob; ob.select_set(True)
    for m in list(ob.modifiers): ob.modifiers.remove(m)
    if before > TRI_BUDGET:
        d = ob.modifiers.new("Decimate", "DECIMATE")
        d.decimate_type = "COLLAPSE"; d.ratio = TRI_BUDGET / before
        bpy.ops.object.modifier_apply(modifier=d.name)
    ob.data.validate(verbose=False)
    ob.data.calc_loop_triangles()
    return {"before": before, "after": len(ob.data.loop_triangles), "verts": len(ob.data.vertices)}


def rig(name, arm_name):
    """Eren's rig, bit for bit, minus the finger bones. Same rest pose means his clips just run."""
    _drop(arm_name)
    src = bpy.data.objects["ErenRigSrc"]
    arm = bpy.data.objects.new(arm_name, src.data.copy())
    arm.data.name = arm_name + "Data"
    bpy.context.scene.collection.objects.link(arm)
    arm.matrix_world = src.matrix_world.copy()
    bpy.ops.object.select_all(action="DESELECT")
    bpy.context.view_layer.objects.active = arm; arm.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    eb = arm.data.edit_bones
    for b in [b for b in eb if FINGER.search(b.name)]:
        eb.remove(b)
    bpy.ops.object.mode_set(mode="OBJECT")

    ob = bpy.data.objects[name]
    for m in list(ob.modifiers): ob.modifiers.remove(m)
    ob.parent = None
    ob.vertex_groups.clear()
    bpy.ops.object.select_all(action="DESELECT")
    ob.select_set(True); arm.select_set(True)
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.parent_set(type="ARMATURE_AUTO")
    unw = sum(1 for v in ob.data.vertices if sum(g.weight for g in v.groups) <= 1e-6)
    return {"bones": len(arm.data.bones), "groups": len(ob.vertex_groups), "unweighted": unw}


def _s2l(c): return c/12.92 if c <= 0.04045 else ((c+0.055)/1.055)**2.4
def _l2s(c):
    c = 0.0 if c < 0 else (1.0 if c > 1 else c)
    return c*12.92 if c <= 0.0031308 else 1.055*c**(1/2.4)-0.055


def textures(name, prefix, tint_hex, out_dir, size=512, sat=0.55,
             rough_lo=0.58, rough_span=0.37, metal_cap=0.35, target_mean=None,
             mat_prefix=None):
    """512 maps, palette nudge on the albedo, STYLE.md limits baked into the ORM."""
    ob = bpy.data.objects[name]
    m = ob.data.materials[0]
    nt = m.node_tree
    role = {}
    for l in nt.links:
        if l.from_node.type == "TEX_IMAGE":
            if l.to_socket.name == "Base Color": role["albedo"] = l.from_node.image
        if l.from_node.type == "TEX_IMAGE" and l.to_node.type == "SEPARATE_COLOR":
            role["orm"] = l.from_node.image
        if l.from_node.type == "TEX_IMAGE" and l.to_node.type == "NORMAL_MAP":
            role["normal"] = l.from_node.image
    rep = {"found": sorted(role)}

    def prep(img, non_color, tag):
        cp = img.copy(); cp.name = tag
        cp.colorspace_settings.name = "Non-Color" if non_color else "sRGB"
        cp.scale(size, size)
        return cp

    alb = prep(role["albedo"], False, prefix + "_albedo")
    orm = prep(role["orm"], True, prefix + "_orm") if "orm" in role else None
    nrm = prep(role["normal"], True, prefix + "_normal") if "normal" in role else None

    tr, tg, tb = (_s2l(((tint_hex>>16)&255)/255), _s2l(((tint_hex>>8)&255)/255), _s2l((tint_hex&255)/255))
    tl = LR*tr + LG*tg + LB*tb
    tn = (tr/tl, tg/tl, tb/tl)
    p = list(alb.pixels)
    n0 = len(p) // 4
    m0 = sum(LR*_s2l(p[i]) + LG*_s2l(p[i+1]) + LB*_s2l(p[i+2]) for i in range(0, len(p), 4)) / n0
    # Meshy delivers some characters almost black; a gain lifts them to a readable floor before
    # the palette tint goes in. Without it the figure is a silhouette in a dark vault.
    gain = 1.0 if target_mean is None else target_mean / m0
    m1 = 0.0
    for i in range(0, len(p), 4):
        r, g, b = _s2l(p[i])*gain, _s2l(p[i+1])*gain, _s2l(p[i+2])*gain
        lum = LR*r + LG*g + LB*b
        nr = min(max(r + sat*(lum*tn[0]-r), 0), 1)
        ng = min(max(g + sat*(lum*tn[1]-g), 0), 1)
        nb = min(max(b + sat*(lum*tn[2]-b), 0), 1)
        m1 += LR*nr + LG*ng + LB*nb
        p[i], p[i+1], p[i+2] = _l2s(nr), _l2s(ng), _l2s(nb)
    alb.pixels = p
    n = n0
    rep.update({"albedo_mean_before": round(m0, 5), "gain": round(gain, 3),
                "albedo_mean_after": round(m1/n, 4)})

    if orm:
        p = list(orm.pixels)
        r0 = r1 = k0 = k1 = 0.0
        for i in range(0, len(p), 4):
            rg, mt = p[i+1], p[i+2]
            r0 += rg; k0 += mt
            nrg = rough_lo + rough_span*rg
            nmt = metal_cap*mt
            r1 += nrg; k1 += nmt
            p[i] = 1.0; p[i+1] = nrg; p[i+2] = nmt; p[i+3] = 1.0
        orm.pixels = p
        rep.update({"rough_before": round(r0/n, 4), "rough_after": round(r1/n, 4),
                    "metal_before": round(k0/n, 4), "metal_after": round(k1/n, 4)})

    sizes = {}
    for img, fn in ((alb, prefix + "_albedo.png"), (orm, prefix + "_orm.png"),
                    (nrm, prefix + "_normal.png")):
        if img is None: continue
        img.filepath_raw = os.path.join(out_dir, fn); img.file_format = "PNG"; img.save()
        sizes[fn] = os.path.getsize(os.path.join(out_dir, fn))
    rep["files"] = sizes

    mat_name = mat_prefix or ("PQ_MI_" + prefix.upper())
    if mat_name in bpy.data.materials: bpy.data.materials.remove(bpy.data.materials[mat_name])
    mat = bpy.data.materials.new(mat_name); mat.use_nodes = True
    t = mat.node_tree; b = t.nodes["Principled BSDF"]
    def tex(img, x, y):
        nn = t.nodes.new("ShaderNodeTexImage"); nn.image = img; nn.location = (x, y); return nn
    t.links.new(tex(alb, -700, 300).outputs["Color"], b.inputs["Base Color"])
    if orm:
        sep = t.nodes.new("ShaderNodeSeparateColor"); sep.location = (-500, 0)
        t.links.new(tex(orm, -900, 0).outputs["Color"], sep.inputs["Color"])
        t.links.new(sep.outputs["Green"], b.inputs["Roughness"])
        t.links.new(sep.outputs["Blue"], b.inputs["Metallic"])
    if nrm:
        nmn = t.nodes.new("ShaderNodeNormalMap"); nmn.location = (-400, -350)
        t.links.new(tex(nrm, -900, -350).outputs["Color"], nmn.inputs["Color"])
        t.links.new(nmn.outputs["Normal"], b.inputs["Normal"])
    ob.data.materials.clear(); ob.data.materials.append(mat)
    rep["material"] = mat_name
    return rep


def export(name, arm_name, out_path):
    """AttackTimeline assumes 30 fps, the same as the delivered Eren."""
    bpy.context.scene.render.fps = 30
    bpy.context.scene.render.fps_base = 1.0
    arm = bpy.data.objects[arm_name]
    arm.animation_data_clear()
    ad = arm.animation_data_create()
    missing = []
    for c in CLIPS:
        a = bpy.data.actions.get("EREN_" + c)
        if a is None:
            missing.append(c); continue
        tr = ad.nla_tracks.new(); tr.name = c
        tr.strips.new(c, int(a.frame_range[0]), a)
        tr.mute = False
    ad.action = None
    ob = bpy.data.objects[name]
    bpy.ops.object.select_all(action="DESELECT")
    ob.select_set(True); arm.select_set(True)
    bpy.context.view_layer.objects.active = arm
    bpy.ops.export_scene.gltf(filepath=out_path, export_format="GLB", use_selection=True,
        use_active_scene=True, export_yup=True, export_apply=False,
        export_animation_mode="NLA_TRACKS", export_force_sampling=True,
        export_materials="EXPORT", export_skins=True)
    lens = {}
    for t in arm.animation_data.nla_tracks:
        for s in t.strips:
            lens[t.name] = round((s.action.frame_range[1]-s.action.frame_range[0])/30.0, 3)
    return {"missing": missing, "clip_seconds": lens, "bytes": os.path.getsize(out_path)}


def render_views(name, out_dir, tag, ortho=2.6):
    """Facing and pose are decided on pictures, never on a heuristic."""
    os.makedirs(out_dir, exist_ok=True)
    sc = bpy.context.scene
    for nm in ("PQProbeCam", "PQProbeLight"):
        if nm in bpy.data.objects: bpy.data.objects.remove(bpy.data.objects[nm], do_unlink=True)
    cam = bpy.data.objects.new("PQProbeCam", bpy.data.cameras.new("PQProbeCam"))
    sc.collection.objects.link(cam)
    cam.data.type = "ORTHO"; cam.data.ortho_scale = ortho
    lt = bpy.data.objects.new("PQProbeLight", bpy.data.lights.new("PQProbeLight", type="SUN"))
    sc.collection.objects.link(lt); lt.data.energy = 4.0
    lt.rotation_euler = (math.radians(55), 0, math.radians(35))
    ob = bpy.data.objects[name]
    prev = {o.name: o.hide_render for o in bpy.data.objects}
    for o in bpy.data.objects: o.hide_render = (o not in (ob, lt))
    sc.camera = cam
    sc.render.resolution_x = sc.render.resolution_y = 520
    paths = {}
    for view, (loc, rot) in {
        "front": ((0, -4, HEIGHT/2), (math.radians(90), 0, 0)),
        "side":  ((4, 0, HEIGHT/2), (math.radians(90), 0, math.radians(90))),
    }.items():
        cam.location = loc; cam.rotation_euler = rot
        pth = os.path.join(out_dir, "%s-%s-%s.png" % (name.lower(), tag, view))
        sc.render.filepath = pth
        bpy.ops.render.render(write_still=True)
        paths[view] = pth
    for o in bpy.data.objects: o.hide_render = prev.get(o.name, False)
    return paths


def arm_radius_profile(name, sgn=1.0, bins=0.03):
    """Histogram of the distance to the arm axis. The valley between arm and torso is where the
    cut belongs; the warden's sat at r = 0.12. Never guess this number, measure it."""
    ob = bpy.data.objects[name]
    co = [v.co.copy() for v in ob.data.vertices]
    S = _cen([c for c in co if 1.36 <= c[2] <= 1.46 and sgn*c[0] > 0.16])
    H = _cen([c for c in co if 1.02 <= c[2] <= 1.18 and sgn*c[0] > 0.24])
    a = (H - S).normalized()
    acc = {}
    for c in co:
        rel = c - S
        s = rel.dot(a)
        if not (0.05 <= s <= 0.42):
            continue
        r = (rel - a*s).length
        k = round(round(r/bins)*bins, 2)
        e = acc.setdefault(k, [0, 0.0])
        e[0] += 1
        e[1] += sgn*c[0]
    return {"shoulder": [round(x, 3) for x in S], "hand": [round(x, 3) for x in H],
            "bins": {k: {"n": v[0], "x": round(v[1]/v[0], 3)} for k, v in sorted(acc.items())}}


def cut_arms(name, r_base=0.11, r_grow=0.10, s_grow=0.18, s_min=0.045, bridge_radius=0.40):
    """Free the arms with a hard cut and turn them onto Eren's rest direction.

    For a figure whose hands are welded to a skirt or tassets - the warden - no weight field can
    help: a shared vertex drags the skirt whatever the ramp looks like. So the arm is flood filled
    inside a cylinder around its own axis, split off with split_edges, rotated RIGIDLY, and the
    armpit is closed again with bridge_loops. Pick r_base from arm_radius_profile().
    """
    ob = bpy.data.objects[name]
    rep = {}
    for side, sgn in (("L", 1.0), ("R", -1.0)):
        co = [v.co.copy() for v in ob.data.vertices]
        sh = [c for c in co if 1.36 <= c[2] <= 1.46 and sgn*c[0] > 0.16]
        hd = [c for c in co if 1.02 <= c[2] <= 1.18 and sgn*c[0] > 0.24]
        if not sh or not hd:
            rep[side] = {"error": "no landmark"}
            continue
        S, H = _cen(sh), _cen(hd)
        a = (H - S).normalized()

        def inside(c):
            rel = c - S
            s = rel.dot(a)
            if s < s_min:
                return False
            return (rel - a*s).length < r_base + r_grow * max(0.0, s - s_grow)

        bm = bmesh.new(); bm.from_mesh(ob.data)
        bm.verts.ensure_lookup_table(); bm.edges.ensure_lookup_table()
        seed = min(bm.verts, key=lambda v: (v.co - H).length)
        A = set()
        if inside(seed.co):
            stack = [seed]; A.add(seed.index)
            while stack:
                v = stack.pop()
                for e in v.link_edges:
                    w = e.other_vert(v)
                    if w.index in A or not inside(w.co):
                        continue
                    A.add(w.index); stack.append(w)
        boundary = [e for e in bm.edges if (e.verts[0].index in A) != (e.verts[1].index in A)]
        bmesh.ops.split_edges(bm, edges=boundary)
        bm.verts.ensure_lookup_table(); bm.edges.ensure_lookup_table()
        seed2 = min(bm.verts, key=lambda v: (v.co - H).length)
        comp = {seed2.index}; stack = [seed2]
        while stack:
            v = stack.pop()
            for e in v.link_edges:
                w = e.other_vert(v)
                if w.index not in comp:
                    comp.add(w.index); stack.append(w)
        t = mathutils.Vector((sgn*ARM_TARGET.x, ARM_TARGET.y, ARM_TARGET.z)).normalized()
        axis = a.cross(t); ang = a.angle(t)
        if axis.length > 1e-9:
            axis.normalize()
            R = mathutils.Matrix.Rotation(ang, 4, axis)
            for i in comp:
                bm.verts[i].co = S + (R @ (bm.verts[i].co - S))
        openers = [e for e in bm.edges
                   if len(e.link_faces) == 1 and (e.verts[0].co - S).length < bridge_radius]
        bridged = 0
        try:
            bridged = len(bmesh.ops.bridge_loops(bm, edges=openers).get("faces", []))
        except Exception as ex:
            rep.setdefault("bridge_errors", []).append(str(ex))
        bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
        bm.to_mesh(ob.data); bm.free(); ob.data.update()
        rep[side] = {"shoulder": [round(x, 3) for x in S], "hand": [round(x, 3) for x in H],
                     "arm_verts": len(A), "component": len(comp), "boundary_edges": len(boundary),
                     "angle_deg": round(math.degrees(ang), 2), "bridged_faces": bridged}
    ob.data.validate(verbose=False)
    ob.data.calc_loop_triangles()
    rep["tris"] = len(ob.data.loop_triangles)
    return rep


def fix_unweighted(name):
    """An unweighted vertex is pinned to the model origin once skinned, so it fires a spike from
    the body to (0,0,0). bridge_loops creates faces after ARMATURE_AUTO ran, so this must be
    checked on every rigged enemy - the warden had 18 of them."""
    ob = bpy.data.objects[name]
    unw = [v for v in ob.data.vertices if sum(g.weight for g in v.groups) <= 1e-6]
    weighted = [v for v in ob.data.vertices if sum(g.weight for g in v.groups) > 1e-6]
    far = 0.0
    for v in unw:
        best = min(weighted, key=lambda w: (w.co - v.co).length_squared)
        far = max(far, (best.co - v.co).length)
        tot = sum(g.weight for g in best.groups)
        for g in best.groups:
            ob.vertex_groups[g.group].add([v.index], g.weight / tot, "REPLACE")
    ob.data.update()
    left = sum(1 for v in ob.data.vertices if sum(g.weight for g in v.groups) <= 1e-6)
    return {"patched": len(unw), "max_distance": round(far, 4), "remaining": left}


def remove_islands(name, min_verts=120):
    """Drop loose islands left over from cut_arms.

    Blender's bone-heat weighting solves per connected component. An island that contains no bone
    makes the whole solve fail - the king came back with 0 of 9998 vertices weighted because six
    fragments of 4 to 32 vertices were floating near the old hand position.
    """
    ob = bpy.data.objects[name]
    bm = bmesh.new(); bm.from_mesh(ob.data); bm.verts.ensure_lookup_table()
    seen = set(); comps = []
    for v in bm.verts:
        if v.index in seen:
            continue
        stack = [v]; seen.add(v.index); mem = []
        while stack:
            x = stack.pop(); mem.append(x)
            for e in x.link_edges:
                o = e.other_vert(x)
                if o.index not in seen:
                    seen.add(o.index); stack.append(o)
        comps.append(mem)
    comps.sort(key=len, reverse=True)
    dropped = [{"n": len(c), "centre": [round(x, 3) for x in _cen([v.co.copy() for v in c])]}
               for c in comps[1:] if len(c) < min_verts]
    doomed = [v for c in comps[1:] if len(c) < min_verts for v in c]
    if doomed:
        bmesh.ops.delete(bm, geom=doomed, context="VERTS")
    bm.to_mesh(ob.data); bm.free(); ob.data.update()
    ob.data.validate(verbose=False)
    ob.data.calc_loop_triangles()
    return {"kept": len(comps[0]), "dropped": dropped,
            "verts": len(ob.data.vertices), "tris": len(ob.data.loop_triangles)}
