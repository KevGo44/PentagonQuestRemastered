# Texturen

Deterministisch von `tools/AssetBaker.java` erzeugte Originalmaps:
`stone` / `metal` / `wood` × `albedo`, `normal`, `roughness`, `metallic`, je 512².

Albedo liegt in **sRGB**, Normal- und skalare Materialmaps **linear**. Normals folgen
der OpenGL-Konvention (+Y). glTF-Materialien aus Blender können zusätzlich gepackte
Metallic/Roughness-Texturen mitbringen.

Positionsgehasht statt `Random`, damit die Maps kachelbar und über Läufe hinweg
byte-identisch sind. Die Albedos bleiben nahezu neutral, weil `AssetPipeline.pbr`
die Gebietsfarbe als Tint darüber multipliziert.

`wood` wird erzeugt, ist aber noch von keinem Material referenziert.
