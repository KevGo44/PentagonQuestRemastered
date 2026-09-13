# Asset-Pipeline und Austauschverträge

Die Anwendung startet vollständig mit eigenen Platzhaltern. PBR, echtes GPU-Skinning, glTF-Import und die Effektpipeline sind bereits im laufenden Spiel aktiv. Platzhalter sind keine Behauptung finaler High-End-Art.

## Ablage

```
src/main/resources/
  models/characters/  hero, goblin, orc, warden, shaman, king, mira, eren
  models/props/       shrine, portal, chest, lore, rune, seal, crystal, throne
  textures/           Albedo / Normal / Roughness / Metallic
  animations/         Dokumentierter Clipvertrag; Clips selbst liegen im Character-Asset
  audio/              WAV-Stems, Effekte und der gestreamte Radiotitel
  fonts/              Eigene gerasterte Fontatlanten
  shaders/            Atmosphärenfilter
```

`AssetPipeline.model` sucht nach stabiler Asset-ID in der Reihenfolge **GLB → glTF → J3O → OBJ**. Fehlt ein Asset, erzeugt der mitgelieferte Factory-Code das passende Modell. Ein vorhandenes, aber defektes Charakterasset erzeugt eine konkrete Fehlermeldung statt unbemerkt auf einen falschen Rig zurückzufallen. Nach Austausch neu bauen.

Die drei großen Kristalladern der Höhlen verwenden bereits `models/props/crystal.gltf` mit eigenem Binärbuffer. `AssetTest` lädt dieses Modell durch den echten jME-AssetManager. OBJ ist für statische Props gedacht; es transportiert nicht den Charakter-Skelettvertrag. **FBX wird über Blender in glTF/GLB umgewandelt**, nicht als ungetesteter direkter Laufzeitimport versprochen. Der Engine-Importworkflow ist in der [jME-Dokumentation](https://wiki.jmonkeyengine.org/docs/3.9/sdk/model_loader_and_viewer.html) beschrieben.

## Koordinaten und Rig

* Metrische Einheiten; 1 Einheit = 1 m.
* jME: +Y oben, +Z vorwärts; Blender-Exporter: `export_yup=True`.
* **Die gelieferten Rigs animieren auf die glTF-Front (+Z)** — aber erst seit dem 12. September 2026, und nur, weil die Clips dafür umgerechnet wurden. Mixamos ganzes „sword and shield"-Set ist in einer seitlichen Deckungshaltung authored, rund 53° aus der Front gedreht, während Walk und Run geradeaus zeigen; eine Figur aus beiden Quellen **steht seitlich und läuft vorwärts**. Zusätzlich trug jede exportierte Animation einen führenden Abtastwert der Ruhepose, weil die NLA-Strips bei Frame 1 begannen und der Export ab Frame 0 abtastete — im Spiel ein T-Pose-Blitz bei **jedem** Clipwechsel. Beides ist in `art/blender/anim_normalise.py` repariert: jeder Clip wird über die Root-Kurve auf die Front gedreht (gehaltene Clips auf ihren Mittelwert, einmalige auf ihr erstes Bild), die Strips beginnen bei Frame 0, und der Idle kommt aus „sword and shield idle (4)", der seine Haltung auf ein Grad hält — eingepfropft über Copy-Transforms-Constraints und einen Bake, weil die Restposen von geliefertem Rig und Mixamo-Quelle um bis zu 22° je Bone auseinanderliegen. **Die Drehachse ist gemessen, nicht abgelesen:** `art/blender/axis_test.py` dreht Root um alle drei Achsen; nur lokal Z ist ein reiner Gier-Dreh, lokal Z senkt den Winkel (die Restmatrix legt lokal Y nahe, das kippt den Körper um 37°). `AssetTest` prüft für alle acht Charaktere und alle elf Clips, dass das erste Bild auf der Front steht.
* Blickrichtung ist **nicht kopflos aus Gelenken zu berechnen**: Schulterlinie, Zehenrichtung und Beckenrahmen schwanken über die Clips um 60° und mehr. Belastbar ist allein die Beckenachse `Thigh.L − Thigh.R` — die Oberschenkelwurzeln sitzen fest an der Hüfte — und auch die nur am ersten Bild. Alles andere gehört ins Bild: `art/probe/ClipSheet` rendert alle elf Clips nebeneinander.
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

Sockets: `WeaponSocket` an `Hand.R`, `ShieldSocket` an `Hand.L` — **nur beim Spieler**. `CharacterFactory.create(id, farbe, king, armed)` hängt die Ausrüstung nur bei `armed = true` an, und das setzt allein `PlayerController`; Mira und Eren sind keine Kämpfer, die Gegner bringen Klauen und Rüstung mit dem Mesh. Beide Hände laufen mit lokal **+Y entlang der Finger, +X über den Daumen hinaus und +Z aus der Handfläche** (an den gelieferten Rigs gemessen: die vier Fingerwurzeln liegen innerhalb von neun Grad um +Y, der Zeigefinger auf +X, der kleine Finger auf −X, der Daumen steht nach +Z ab). Daraus folgt der Sitz: die Faust hält den Griff **quer** zur Handfläche, also verlässt die Klinge die Hand entlang lokal X — und zwar auf der **Daumenseite (+X)**, der gewöhnliche Griff mit Klinge nach oben-vorn (Stand 13. September 2026; vorher −X, ein umgekehrter Griff mit hängender Spitze, den Kevin im Bild als falsch erkannt hat) — plus eine Vierteldrehung um die Klinge selbst, damit die Flachseiten zur Seite zeigen; der Schild sitzt **quer auf dem Arm**, mit der Fläche entlang der Unterarmachse (lokal +Y) und 0,175 m vom Handgelenk. So wird ein Buckelschild getragen, und nur so zeigt er nach vorn: abgesucht über alle Richtungen im Handrahmen und bewertet über Idle, Walk, Run, Block und Hit deckt die Unterarmachse **0,84 bis 0,93** der Front, die Handfläche −0,50 bis −0,11 (zeigt nach hinten, weil Mixamos Set die linke Handfläche zum Körper dreht) und der Handrücken 0,11 bis 0,50 (zeigt zur Seite). Der Unterarm läuft vom Handgelenk nach hinten und bleibt damit vollständig hinter der Scheibe. Der Schwertsockel sitzt 0,095 m vom Handgelenk in der Faustmitte. Finale Modelle müssen dieselben Attachment-Pivots tragen. Benötigte Clips: **Idle, Walk, Run, Attack1, Attack2, Attack3, Dodge, Block, Hit, Death, Cast**. Optional (`CharacterFactory.OPTIONAL_CLIPS`, `Rig.has`): **Parry** — ein Schildstoß von 15 Bildern (0,5 s, `PlayerController.PARRY_TIME`), den nur `hero.glb` trägt; ohne ihn spielt eine gelungene Parade `Block`. `CharacterFactory` prüft Composer und SkinningControl sowie alle Pflicht-Clipnamen. Die Engine-API dafür: [SkinningControl](https://javadoc.jmonkeyengine.org/v3.8.0-stable/com/jme3/anim/SkinningControl.html).

Angriffszeiten: Attack1 0,52 s / Treffer ab 0,15 s; Attack2 0,58 s / Treffer ab 0,19 s; Attack3 0,78 s / Treffer ab 0,28 s. Die Gegner nutzen Attack3 und starten ihn so, dass sein Schlagmoment (0,28 s) auf das Ende ihrer Aufladung fällt; der Aschenrufer nutzt Cast, dessen Entladung im gelieferten Clip bei 1,6 s liegt (`EnemyBrain.CAST_RELEASE`, gemessen mit `art/probe/ClipTimeline`). Death (2,3 s, Boden bei 1,7 s) und Hit laufen **einmal** und halten das letzte Bild (`CharacterFactory.Rig.once`). Einmalclips blenden mit 0,1 s ein, Bewegungsclips mit 0,25 s; jMEs Vorgabe von 0,4 s ist länger als der erste Treffer. Dodge ist 0,767 s (23 Bilder) und Cast 0,6 s (18 Bilder) lang — `PlayerController.DODGE_TIME` / `CAST_TIME`, von `AssetTest` gehalten. Attack2 ist eine volle 360°-Drehung, Attack3 ein Vorwärtshieb; beide wie Dodge und Cast **beginnen und enden auf der Front** (`art/blender/anim_polish.py`). **Kein Clip trägt Wurzelbewegung**: die Hüfte bleibt horizontal innerhalb von 0,35 m ihres ersten Bildes (`art/blender/anim_pin.py`, von `AssetTest` über alle Clips aller Rigs gehalten) — die Physikkapsel bewegt die Figur, nicht der Clip; Attack2 trug 3,16 m und lief dem Körper davon. Die Endposition einer Bewegungsschleife muss der Anfangsposition entsprechen. Finishing/Hit-Animationen werden durch die Simulation beendet.

Waffe und Schild sind die Ausnahme von der Pivotregel: sie sind **griffzentriert** authored — der Ursprung liegt in der Griffmitte, die Klinge läuft nach +Y (Knauf −0,098, Parierstange +0,110, Spitze +0,882), die Schildfläche zeigt nach +Z und der Buckel steht bis +0,082 vor. Die Rückfallgeometrie in `CharacterFactory` folgt derselben Konvention, damit ein fehlendes Modell denselben Sitz hat.

Props haben ihren Pivot **unten in der Mitte**, der Knoten sitzt auf `y = 0`. Ein vorhandenes Modul ersetzt die prozedurale Steinarbeit vollständig (`WorldView.buildObject`); die **leuchtenden Teile bleiben erhalten** — Kindknoten namens `Crystal`, `Flame` und `PortalVeil` —, weil ihr Glüh-Material den Bloom-Pass speist und kein glTF-Material das kann. Ausnahme ist `CRYSTAL`: dort *ist* die Leuchtkugel das Objekt, ein Modul ersetzt sie. Der Schrein trägt seit dem 13. September ein **Feuer** (`WorldView.flame`, `assets/FlameShapes`, additiv über `AssetPipeline.flame`) über der Schale von `props/shrine.glb` (Oberkante 0,62 m) und ein warmes Licht; der cyanfarbene Kristall ist Geschichte.

Geliefert (Stand 12. September 2026), Maße in der Engine nachgemessen:

| ID | Maße (m) | Dreiecke | Materialien |
|---|---|---:|---|
| `props/chest.glb` | 1,34 × 0,84 × 0,92 | 72 | `PQP_Wood`, `PQK_Iron` |
| `props/shrine.glb` | 1,33 × 0,62 × 1,33 | 368 | `PQP_Carved`, `PQK_Iron`, `PQP_Ash` (achteckige Schale, 13. Sept.) |
| `props/rune.glb`, `seal.glb`, `lore.glb` | 1,30 × 1,23 × 0,90 | 360 | `PQP_Carved`, `PQK_Iron`, `PQP_Ash` (Altar mit Runenscheibe, 13. Sept.) |
| `props/throne.glb` | 2,60 × 3,90 × 2,30 | 792 | `PQP_Carved`, `PQK_Iron`, `PQP_Ash` (Sockel, Sitz, Lehne mit Zacken, 13. Sept.) |
| `props/portal.glb` | 3,90 × 5,25 × 0,96 | 72 | `PQP_Stone` |
| `props/crystal.gltf` | Bestand, eigener Binärbuffer | — | — |
| `props/sword.glb` | 0,24 × 0,98 × 0,05 | 2 400 | `PQW_Steel`, `PQK_Iron`, `PQW_Leather` |
| `props/shield.glb` | 0,65 × 0,65 × 0,10 | 204 | `PQP_Wood`, `PQK_Iron` |

Thron, Altäre und Schrein stammen seit dem 13. September aus `art/blender/props_builder.py` (Fasen, kastenprojizierte UVs, Albedo `prop_carved_albedo.png` ohne Ziegel). Schrein und Altar sind **niedriger** als die alten Zielmaße (1,4 × 1,8 und 1,3 × 1,2 einschließlich Kristall), weil die Kristalle jetzt erhalten bleiben und nicht mehr mitmodelliert werden müssen. `refreshObject` blendet eine gereinigte Kristallader aus und nimmt dem Siegelaltar seinen Leuchtsplitter; die Truhe bleibt nach dem Öffnen, wie sie ist (vorher schrumpfte alles auf 0,6).

**Der Dungeon-Kit ist nicht mehr codegeneriert.** Fünfzehn Module unter `props/kit_*` ersetzen Boden, Wand, Decke, Rippe, Kranz, Pfeiler, Bogen, Kohlebecken, Banner, Teppich und Fels; die Rechenregel, die Maße und die Verifikation stehen in `docs/dungeon-kit.md`. Der Platzhalterpfad bleibt vollständig erhalten und greift, sobald eine Moduldatei fehlt. Die **Höhlen** (CAVERNS) versuchen zuerst neun Felsmodule `props/kit_floor_cave[2]`, `kit_ceiling_cave[2]`, `kit_wall_cave_face[2]`, `_corner`, `_span`, `_pier` (`art/blender/cave_builder.py`, Albedo `art/textures/kit_cave_albedo.png`), abwechselnd nach Zellparität; Rippen und Kränze werden dort nicht gebaut.

**Fallen** (`Kind.TRAP`) tragen in `value` Achse und Zellenbreite (`z3`, `x5`); `WorldView.trap` baut keine Platte, nur zwei Reihen Schlitze und darunter Spitzen namens `Spike` über die ganze Gangbreite, die `CampaignState` nach `world/TrapMechanism` hebt. **Abgelegte Gegenstände** (`GameSession.Drop`) bekommen ein Bündel über `WorldView.addDrop` und tauchen als `Kind.DROP` in `CampaignState.nearby()` auf.

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

Das Skript ist für Blender 4.x ausgelegt, prüft Bone-/Clipnamen und die Armature-Skalierung.

**Korrektur (12. September 2026):** Der Satz „Auf diesem Rechner steht Blender nicht zur Verfügung" stimmt nicht mehr. Auf diesem Rechner läuft **Blender 5.2.1 LTS**, und alle seit dem 12. September gelieferten Assets sind damit exportiert worden — fünf Gegner, fünfzehn Kit-Module und sieben Props. Der Weg ist nicht `tools/blender_export.py`, sondern zwei eigene Skripte, die den Ablauf festhalten statt ihn jedes Mal neu zu erfinden:

* `art/blender/enemy_pipeline.py` — Meshy-Lieferung bis fertigem Charakter-GLB: Verschweißen, Normalisieren auf 1,95 m, Arme in Erens Ruhepose (Geodäte **oder** Freischnitt, je nach Figur), Dezimieren, Rig, Texturen, NLA-Export. Enthält die Messfunktionen `arm_radius_profile`, `weld_check`, `fix_unweighted` und `remove_islands`.
* `art/blender/kit_builder.py` — der parametrische Generator für die vier Wandvarianten aus einem Profil.

Verbindliche Exporteinstellungen, jede einzeln belegt: `export_animation_mode="NLA_TRACKS"` (`ACTIONS` verdoppelt Animationen), `use_active_scene=True` (jME stürzt bei mehreren Szenen ab), `export_yup=True`, `export_force_sampling=True`, `export_apply=False` für geriggte Meshes. **Bildrate der Szene auf 30 fps** — Erens Clips sind dafür gebacken, bei 24 fps verfehlen die Angriffe `AttackTimeline`.

## Darstellung

PBR-Materialien besitzen Albedo-, Normal-, Roughness- und Metallic-Maps. Farben/Albedo werden korrekt als sRGB behandelt, Normals und skalare Maps linear. Ein gültiger konstanter IBL-Probe stellt diffuses Umgebungslicht und Spiegelungsradiance bereit; finale Indoor-Probes sollen pro Abschnitt gebacken werden.

Fackeln flackern über zwei Frequenzen. Drei kaskadierte Schattenkarten (PSSM) nutzen Software-PCF für den getesteten Apple-Treiber. SSAO, Bloom und ein eigener FilterPostProcessor-Filter liefern Tiefenwirkung, ACES-artiges Tonemapping und Vignette. Der Filter integriert Höhendichte in zwölf Schritten bis zur sichtbaren Oberfläche und ergänzt lokale Lichtschächte. **Das ist eine begrenzte, analytische Volumetrik, keine vollständige Schattenvolumetrik oder globale Beleuchtung.**

Die synthetischen WAVs und prozeduralen Texturen lassen sich mit `AssetBaker.java` reproduzieren. Fontatlanten werden aus Java-Logical-Fonts erzeugt; die Rasterung kann je nach installierter Systemschrift leicht variieren.

`audio/pentagonradio.wav` ist die einzige aufgenommene Datei und wird **nicht** von `AssetBaker.java` erzeugt; beim Neubacken der synthetischen Assets muss sie erhalten bleiben. Sie lädt als Stream statt als Buffer, weil 48 kHz Stereo über drei Minuten rund 34 MB rohes PCM sind. Looping funktioniert, weil der WAV-Loader `SeekableStream` implementiert und der Renderer am Titelende `setTime(0)` aufruft.
