# Asset-Pipeline und Austauschverträge

Die Anwendung startet vollständig mit eigenen Platzhaltern. PBR, echtes GPU-Skinning, glTF-Import und die Effektpipeline sind bereits im laufenden Spiel aktiv. Platzhalter sind keine Behauptung finaler High-End-Art.

## Ablage

```
src/main/resources/
  models/characters/  hero, goblin, orc, warden, shaman, king, mira, eren
  models/props/       shrine, portal, chest, lore, rune, seal, crystal, throne
  textures/           Albedo / Normal / Roughness / Metallic
  animations/         Dokumentierter Clipvertrag; Clips selbst liegen im Character-Asset
  audio/              WAV-Stems und Effekte
  fonts/              Eigene gerasterte Fontatlanten
  shaders/            Atmosphärenfilter
```

`AssetPipeline.model` sucht nach stabiler Asset-ID in der Reihenfolge **GLB → glTF → J3O → OBJ**. Fehlt ein Asset, erzeugt der mitgelieferte Factory-Code das passende Modell. Ein vorhandenes, aber defektes Charakterasset erzeugt eine konkrete Fehlermeldung statt unbemerkt auf einen falschen Rig zurückzufallen. Nach Austausch neu bauen.

Die drei großen Kristalladern der Höhlen verwenden bereits `models/props/crystal.gltf` mit eigenem Binärbuffer. `AssetTest` lädt dieses Modell durch den echten jME-AssetManager. OBJ ist für statische Props gedacht; es transportiert nicht den Charakter-Skelettvertrag. **FBX wird über Blender in glTF/GLB umgewandelt**, nicht als ungetesteter direkter Laufzeitimport versprochen. Der Engine-Importworkflow ist in der [jME-Dokumentation](https://wiki.jmonkeyengine.org/docs/3.9/sdk/model_loader_and_viewer.html) beschrieben.

## Koordinaten und Rig

* Metrische Einheiten; 1 Einheit = 1 m.
* jME: +Y oben, +Z vorwärts; Blender-Exporter: `export_yup=True`.
* Charakterpivot am Boden zwischen den Füßen; Standardhöhe ca. 1,95 m.
* Objekttransformation: Rotation angewendet, Skalierung `(1,1,1)`.
* Laufanimationen ohne Root Motion. Bullet kontrolliert Position und Richtung.
* Für Gegner wird die Standardgröße im Spiel multipliziert: Goblin 0,8; Ork 1,15; Wächter 1,2; König 1,85.

Feste Bone-Namen:

```
Root
  Hips
    Spine
      Head
      UpperArm.L → Forearm.L → Hand.L
      UpperArm.R → Forearm.R → Hand.R
    Thigh.L → Shin.L → Foot.L
    Thigh.R → Shin.R → Foot.R
```

Sockets: `WeaponSocket` an `Hand.R`, `ShieldSocket` an `Hand.L`. Finale Modelle müssen dieselben Attachment-Pivots tragen. Benötigte Clips: **Idle, Walk, Run, Attack1, Attack2, Attack3, Dodge, Block, Hit, Death, Cast**. `CharacterFactory` prüft Composer und SkinningControl sowie alle Clipnamen. Die Engine-API dafür: [SkinningControl](https://javadoc.jmonkeyengine.org/v3.8.0-stable/com/jme3/anim/SkinningControl.html).

Angriffszeiten: Attack1 0,52 s / Treffer ab 0,15 s; Attack2 0,58 s / Treffer ab 0,19 s; Attack3 0,78 s / Treffer ab 0,28 s. Die Endposition einer Bewegungsschleife muss der Anfangsposition entsprechen. Finishing/Hit-Animationen werden durch die Simulation beendet.

Props haben ihren Pivot unten in der Mitte. Zielmaße entsprechen den Platzhaltern: Truhe etwa 1,3×0,84×0,9 m; Altar 1,3×1,2×0,9 m; Tor 3,7×5,25×0,8 m; Schrein 1,4×1,8×1,4 m; Kristall 1,4×2,4×1,4 m. Die Modulwände/Böden bleiben derzeit codegeneriert; für eine komplett neue modulare Architektur wird `WorldView` erweitert.

## Blender

```bash
blender --background --python tools/blender_export.py -- \
  --input art/hero.fbx \
  --output src/main/resources/models/characters/hero.glb \
  --character

blender --background --python tools/blender_export.py -- \
  --input art/chest.obj \
  --output src/main/resources/models/props/chest.glb
```

Das Skript ist für Blender 4.x ausgelegt, prüft Bone-/Clipnamen und die Armature-Skalierung. Auf diesem Rechner steht Blender nicht zur Verfügung; der Export selbst wurde deshalb nicht ausgeführt. Python-Syntax und jME-glTF-Laufzeitimport werden separat geprüft.

## Darstellung

PBR-Materialien besitzen Albedo-, Normal-, Roughness- und Metallic-Maps. Farben/Albedo werden korrekt als sRGB behandelt, Normals und skalare Maps linear. Ein gültiger konstanter IBL-Probe stellt diffuses Umgebungslicht und Spiegelungsradiance bereit; finale Indoor-Probes sollen pro Abschnitt gebacken werden.

Fackeln flackern über zwei Frequenzen. Drei kaskadierte Schattenkarten (PSSM) nutzen Software-PCF für den getesteten Apple-Treiber. SSAO, Bloom und ein eigener FilterPostProcessor-Filter liefern Tiefenwirkung, ACES-artiges Tonemapping und Vignette. Der Filter integriert Höhendichte in zwölf Schritten bis zur sichtbaren Oberfläche und ergänzt lokale Lichtschächte. **Das ist eine begrenzte, analytische Volumetrik, keine vollständige Schattenvolumetrik oder globale Beleuchtung.**

Die synthetischen WAVs und prozeduralen Texturen lassen sich mit `AssetBaker.java` reproduzieren. Fontatlanten werden aus Java-Logical-Fonts erzeugt; die Rasterung kann je nach installierter Systemschrift leicht variieren.
