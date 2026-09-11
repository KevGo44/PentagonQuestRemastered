"""Bake a small original glTF 2.0 crystal to exercise the actual runtime importer."""
from pathlib import Path
import json
import math
import struct

root = Path(__file__).resolve().parents[1] / "src/main/resources/models/props"
root.mkdir(parents=True, exist_ok=True)
points = [(0, 2.4, 0), (.7, .85, 0), (0, .85, .7), (-.7, .85, 0), (0, .85, -.7), (0, .1, 0)]
triangles = [(0, 2, 1), (0, 3, 2), (0, 4, 3), (0, 1, 4), (5, 1, 2), (5, 2, 3), (5, 3, 4), (5, 4, 1)]
positions, normals = [], []
for indices in triangles:
    a, b, c = (points[i] for i in indices)
    u = [b[i] - a[i] for i in range(3)]
    v = [c[i] - a[i] for i in range(3)]
    n = [u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0]]
    length = math.sqrt(sum(x*x for x in n))
    for p in (a, b, c):
        positions.extend(p)
        normals.extend(x / length for x in n)
binary = struct.pack("<72f72f24H", *positions, *normals, *range(24))
(root / "crystal.bin").write_bytes(binary)
gltf = {
    "asset": {"version": "2.0", "generator": "Pentagon original reference model"},
    "scene": 0,
    "scenes": [{"nodes": [0]}],
    "nodes": [{"name": "CrystalRoot", "mesh": 0}],
    "meshes": [{"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2, "material": 0}]}],
    "materials": [{"name": "LivingCrystal", "doubleSided": True,
                   "pbrMetallicRoughness": {"baseColorFactor": [.14, .55, .52, 1], "metallicFactor": .35, "roughnessFactor": .22},
                   "emissiveFactor": [.02, .17, .14]}],
    "buffers": [{"uri": "crystal.bin", "byteLength": len(binary)}],
    "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": 288, "target": 34962},
                    {"buffer": 0, "byteOffset": 288, "byteLength": 288, "target": 34962},
                    {"buffer": 0, "byteOffset": 576, "byteLength": 48, "target": 34963}],
    "accessors": [{"bufferView": 0, "componentType": 5126, "count": 24, "type": "VEC3", "min": [-.7, .1, -.7], "max": [.7, 2.4, .7]},
                  {"bufferView": 1, "componentType": 5126, "count": 24, "type": "VEC3"},
                  {"bufferView": 2, "componentType": 5123, "count": 24, "type": "SCALAR"}]
}
(root / "crystal.gltf").write_text(json.dumps(gltf, indent=2) + "\n", encoding="utf-8")
print(f"Wrote {root / 'crystal.gltf'} ({len(binary)} bytes of mesh data)")
