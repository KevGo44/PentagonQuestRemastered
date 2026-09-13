# Modularer Dungeon-Kit — Spezifikation für Phase 5

Stand 2026-09-12. Diese Datei ist die Bauvorlage für die Dungeon-Module in Blender. Sie ist so
geschrieben, dass **keine Maßangabe geraten werden muss**: jede Zahl steht mit Fundstelle
(Datei und Zeile) dabei. Zeilennummern beziehen sich auf den Stand **nach** der in diesem
Durchgang gelieferten Erweiterung von `WorldView`.

Der Weg ist der in [ASSETS.md](ASSETS.md) ausdrücklich vorgesehene: „Die Modulwände/Böden bleiben
derzeit codegeneriert; für eine komplett neue modulare Architektur wird `WorldView` erweitert."
(`docs/assets/ASSETS.md:48`). Genau das ist passiert — die prozeduralen Platzhalter bleiben vollständig
erhalten und sind weiterhin die Rückfallebene.

---

## 1. Der Vertrag in fünf Punkten

1. Einzige Auflösungsstelle bleibt `AssetPipeline.model(id, fallback)`
   (`src/main/java/de/pentagon/assets/AssetPipeline.java:101–108`). Es wird **kein eigener
   Ladecode** geschrieben; die Fallback-Kette `.glb → .gltf → .j3o → .obj` ist unangetastet.
2. Ein Modul liegt unter `src/main/resources/models/props/<id>.glb`. Der Dateiname **ist** die
   Verdrahtung. Liegt keine Datei da, baut `WorldView` exakt die heutige `box()`-Geometrie.
3. Alle Kit-IDs beginnen mit `kit_`. Damit kollidieren sie garantiert nicht mit den bestehenden
   Prop-IDs (`shrine`, `portal`, `chest`, `lore`, `rune`, `seal`, `crystal`, `throne`), die die
   kleingeschriebenen Konstanten von `DungeonLayout.Kind` sind (`DungeonLayout.java:15–27`) und
   nicht umbenannt werden dürfen.
4. Einheiten metrisch, 1 Einheit = 1 m, **+Y oben, +Z vorwärts**, Blender-Export mit
   `export_yup=True`, Rotation angewendet, Skalierung `(1,1,1)`.
5. **Pivot unten mittig** — wie bei allen Props (`ASSETS.md:48`, `STYLE.md:168`). Die einzige
   begründete Ausnahme ist `kit_rock`, siehe Abschnitt 5.

### Warum der Pivot so wichtig ist

`WorldView.slot(...)` (`WorldView.java:189–199`) setzt ein Modul um seine halbe Blockhöhe tiefer
als die prozedurale `Box`, weil eine `Box` auf ihrer Translation **zentriert** ist und ein Modul
seinen Pivot unten hat. Verankert ist damit die **Unterseite** des Blocks — das ist die Fläche,
gegen die die Kollisionsboxen aus dem Layout gemessen sind.

Folge, und das ist die häufigste Fehlerquelle: **die Bauhöhe eines Moduls muss der Höhe des
Platzhalters entsprechen**, sonst wandert die sichtbare Oberseite. Ein 0,50 m dickes Bodenmodul
statt 0,24 m legt seine Oberkante auf `y = +0,25` statt `y = −0,01` und der Spieler steht 26 cm
im Boden. Die einzige Stelle, an der nach oben abgewichen werden darf, ist die Wand — dazu
Abschnitt 4.2.

---

## 2. Gitter, belegt

| Größe | Wert | Fundstelle |
|---|---|---|
| Zellmaß `CELL` | **2,8 m** | `DungeonLayout.java:9` |
| Gittergröße `SIZE` | 31 × 31 Zellen = **86,8 × 86,8 m** | `DungeonLayout.java:8` |
| Zellmittelpunkt in Weltkoordinaten | `(x · 2,8, ·, z · 2,8)` | `WorldView.java:74, 86, 100, 112` |
| Bodenplatte der Physik | Mittelpunkt `(42; −0,35; 42)`, Halbmaße `(47; 0,35; 47)`, Oberkante **`y = 0`** | `WorldView.java:64` |
| Chunk-Raster für das Batching | 6 × 6 Zellen = **16,8 × 16,8 m** | `WorldView.java:68` |
| Raumgröße | `(2·rx+1) × (2·rz+1)` Zellen | `DungeonLayout.java:48–51` |
| Größter Raum | „Hof der Asche", `rx = rz = 6` → 13 × 13 Zellen = **36,4 × 36,4 m** | `DungeonLayout.java:242` |
| Gangbreite | 3 Zellen = **8,4 m** | `DungeonLayout.java:61, 65` |

Was tatsächlich verbaut wird (nachgerechnet durch exakte Nachbildung von `room`, `carve`,
`connect`, `walkable` und `wall` aus `DungeonLayout.java:48–69, 274–285`):

| Gebiet | Räume | Bodenzellen | Wandzellen | Deckenplatten | Rippen |
|---|---:|---:|---:|---:|---:|
| REFUGE | 7 | 477 | 198 | 468 | 128 |
| CRYPT | 7 | 418 | 169 | 412 | 112 |
| CAVERNS | 7 | 463 | 178 | 456 | 125 |
| PRISON | 7 | 450 | 180 | 443 | 126 |
| THRONE | 5 | 394 | 128 | 388 | 99 |
| **Summe** | **33** | **2 202** | **853** | **2 167** | **590** |

Die 33 Räume und fünf Gebiete des Briefings sind damit bestätigt. Belegte Nebenbefunde:

* **Deckenlöcher:** `x % 7 == 1 && z % 7 == 1` lässt die Deckenplatte weg (`WorldView.java:81–82`) —
  in REFUGE sind das 9 Löcher (477 − 468). Die Lichtschächte sind Absicht, kein Fehler.
* **Rippen** sitzen nur auf jeder vierten Spalte, `x % 4 == 0` (`WorldView.java:96`).
* Maximale Belegung eines Chunks: **36 Zellen** (voller 6 × 6-Block), Mittel 24,5 bis 27,0.

---

## 3. Maße der Platzhalter, aus denen die Module abgeleitet werden

`AssetPipeline.box(name, x, y, z, mat)` erzeugt eine `com.jme3.scene.shape.Box` mit **Halbmaßen**
(`AssetPipeline.java:86–92`). Unten stehen daher überall die **vollen** Maße.

| Element | Halbmaße im Code | Volle Maße (m) | y-Bereich (m) | Fundstelle |
|---|---|---|---|---|
| `FloorTile` | 1,4 / 0,12 / 1,4 @ y −0,13 | 2,80 × **0,24** × 2,80 | −0,25 … **−0,01** | `WorldView.java:75–78` |
| `VaultCeiling` | 1,4 / 0,18 / 1,4 @ y 7,1 | 2,80 × **0,36** × 2,80 | **6,92** … 7,28 | `WorldView.java:87–90` |
| `CeilingRib` | 0,16 / 0,16 / 1,4 @ y 6,4 | 0,32 × 0,32 × **2,80** (Lauf in Z) | 6,24 … 6,56 | `WorldView.java:101–104` |
| `Wall` | 1,4 / 3,4 / 1,4 @ y 3,3 | 2,80 × **6,80** × 2,80 | **−0,10** … 6,70 | `WorldView.java:113–117` |
| `Cornice` | 1,54 / 0,16 / 1,54 @ y 4,8 | **3,08** × 0,32 × 3,08 | 4,64 … 4,96 | `WorldView.java:134–137` |
| `Plinth` | 0,66 / 0,18 / 0,66 @ y 0,18 | 1,32 × 0,36 × 1,32 | 0,00 … 0,36 | `WorldView.java:325–327` |
| `Shaft` | 0,4 / 2,8 / 0,4 @ y 3,0 | 0,80 × 5,60 × 0,80 | 0,20 … 5,80 | `WorldView.java:328–330` |
| `Capital` | 0,68 / 0,24 / 0,68 @ y 5,9 | 1,36 × 0,48 × 1,36 | 5,66 … 6,14 | `WorldView.java:331–333` |
| `ArchStone` × 13 | 0,48 / 0,22 / 0,36, Ring r = 3,55 um `(0; 3,3; 0)` | 0,96 tangential × **0,44 radial** × 0,72 tief | Ring 3,3 … 7,07 | `WorldView.java:345–357` |
| `ArchSupport` × 2 | 0,24 / 1,65 / 0,35 @ x ±3,55 | 0,48 × 3,30 × 0,70 | 0,00 … 3,30 | `WorldView.java:360–362` |
| `BrazierStand` | 0,09 / 1,3 / 0,09 @ y 1,3 | 0,18 × 2,60 × 0,18 | 0,00 … 2,60 | `WorldView.java:396–398` |
| `Brazier` (Schale) | 0,27 / 0,1 / 0,27 @ y 2,6 | 0,54 × 0,20 × 0,54 | 2,50 … 2,70 | `WorldView.java:399–401` |
| `Flame` | Kugel r 0,16, Skalierung (1; 2,2; 1) @ y 2,86 | 0,32 × 0,70 × 0,32 | 2,51 … 3,21 | `WorldView.java:405–407` |
| `HangingBanner` | 0,62 / 1,3 / 0,03 @ y 4,4 | 1,24 × 2,60 × 0,06 | **3,10** … 5,70 | `WorldView.java:296–308` |
| `WornBannerCarpet` | min(2,2; dx·0,4) / 0,009 / dz·0,75 @ y 0,009 | **4,40** × 0,018 × (5,67 … 22,47) | 0,00 … 0,018 | `WorldView.java:276–291` |
| `CaveRock` | Kugel r 1, Skalierung (s; s·1,6; s) @ y s·0,6 | s = 1,2 → 2,40 × 3,84 × 2,40 | −1,20 … 2,64 | `WorldView.java:371–386` |

### Belegte Eigenheiten, die beim Modellieren zählen

* **Die 0,22-m-Fuge über der Wand.** Wandoberkante 6,70 m, Deckenunterkante 6,92 m
  (`WorldView.java:113, 87`). Wandzellen bekommen keine Decke, Bodenzellen keine Wand — zwischen
  6,70 und 6,92 m steht am Wandfuß der Decke also nichts. Die Fuge existiert heute schon.
  **Empfehlung:** das Wandmodul 7,02 m hoch bauen (Unterkante −0,10 m, Oberkante 6,92 m), dann
  schließt sie, und weil die Deckenplatte bis 7,28 m reicht, ragt nichts heraus. 6,80 m
  reproduziert den Bestand eins zu eins.
* **Der Kranz ragt in den Raum.** 3,08 m Kantenlänge auf einer 2,8-m-Zelle
  (`WorldView.java:137`, Faktor `cell * .55f`) — 14 cm Überstand auf jeder Seite, auf Höhe
  4,64 … 4,96 m. Das ist gewollte Gesimswirkung, kein Rundungsfehler.
* **Der Boden liegt 1 cm unter der Physik.** Sichtbare Bodenoberkante −0,01 m, Physik-Oberkante
  0,00 m (`WorldView.java:75, 64`). Der Spieler steht 1 cm über dem Bodenmodul. Nicht ändern.
* **Der Teppich ist immer 4,40 m breit.** `min(2.2f, dx * .4f)` ist in allen 26 Nicht-CAVERNS-Räumen
  gekappt, weil dort durchweg `rx ≥ 3` gilt, also `dx ≥ 6,58 m` und `dx · 0,4 ≥ 2,632 > 2,2`
  (`WorldView.java:276`, Raumdaten `DungeonLayout.java:79–264`). Variabel ist nur die Länge, und
  zwar in genau vier Werten: **5,67 / 9,87 / 14,07 / 22,47 m** (für `rz` = 2 / 3 / 4 / 6).
* **Der Bogen steht frei im Raum, nicht in einer Wandöffnung.** Er wird auf
  `(x, z − dz + 0,6)` gesetzt (`WorldView.java:275`), also 3,22 m innerhalb der Raumkante. Lichte
  Weite zwischen den Pfeilern 6,62 m, Kämpferhöhe 3,30 m, Scheitel-Unterkante 6,63 m, Gesamtbreite
  8,06 m. Ein Modul „Durchgang" im Sinne einer Türöffnung existiert im Code **nicht**; siehe
  Abschnitt 9.

---

## 4. Die Modulliste

Alle IDs liegen unter `models/props/`. Die Spalte „Ton" sagt, ob das Modul die Gebietstönung
bekommt (Abschnitt 6).

### 4.1 Boden, Decke, Kranz

| ID | Maße (m) | Anzahl je Gebiet | Pivot | Ausrichtung | Ton |
|---|---|---:|---|---|---|
| `kit_floor` | 2,80 × **0,24** × 2,80 | 394 – 477 | unten mittig, XZ-Mitte der Zelle | keine, wird nicht gedreht | ja, Bodenton |
| `kit_ceiling` | 2,80 × **0,36** × 2,80 | 388 – 468 | unten mittig | keine | ja, Steinton |
| `kit_rib` | 0,32 × 0,32 × **2,80** | 99 – 128 | unten mittig, Lauf entlang **+Z** | keine | ja, Steinton |
| `kit_cornice` | **3,08** × 0,32 × 3,08 | = Wandzellen | unten mittig | keine | **nein** |

Boden und Decke sind quadratisch und symmetrisch; sie werden nicht gedreht, also muss das Modul
an allen vier Kanten stoßfest sein (gleiche Randhöhe, keine einseitige Profilierung). Die Rippe
läuft immer in Z, weil ihr Halbmaß `(0.16, 0.16, cell * .5f)` ist.

`kit_cornice` bekommt **keine** Tönung, weil der Platzhalter das `iron`-Material benutzt
(`WorldView.java:137`, `iron` aus `WorldView.java:47`, `#45494e`) und damit in allen fünf Gebieten
dieselbe Farbe hat. Das Modul bringt seine endgültige Erscheinung mit.

### 4.2 Wand

Ein Wandblock füllt die **ganze Zelle**: 2,80 × 2,80 m im Grundriss, 6,80 m hoch (empfohlen 7,02 m,
siehe oben), Unterkante −0,10 m.

| ID | Sichtseiten unrotiert | Anzahl (alle 5 Gebiete) | Ton |
|---|---|---:|---|
| `kit_wall_face` | eine, nach **+Z** | **726** | ja, Steinton |
| `kit_wall_corner` | zwei, nach **+Z und +X** (konvexe Außenecke) | **111** | ja |
| `kit_wall_span` | zwei gegenüberliegende, **+Z und −Z** | **8** | ja |
| `kit_wall_pier` | drei, **+Z, +X, −Z** (geschlossene Seite −X) | **8** | ja |
| `kit_wall_free` | vier | **0** — wird nie gebraucht, siehe unten | ja |
| `kit_wall` | allseitig, drehungsinvariant | Ersatz für jede der obigen | ja |

`WorldView.wallKit(x, z)` (`WorldView.java:233–253`) klassifiziert jede Wandzelle nach den Seiten,
die auf begehbaren Boden zeigen, und dreht das Modul um **+Y** in Vierteldrehungen. Die
Seitenreihenfolge ist `+Z, +X, −Z, −X`; eine Vierteldrehung um +Y bildet `+Z → +X → −Z → −X` ab.
Ein Modul, dessen Sichtseite unrotiert nach **+Z** zeigt, wird also mit `i · 90°` auf die Seite `i`
gezielt.

`kit_wall_free` kommt in keinem der fünf Gebiete vor, und zwar nicht zufällig:
`DungeonLayout.wall(x, z)` meldet eine Zelle nur, wenn mindestens ein 4er-Nachbar begehbar ist
(`DungeonLayout.java:282–285`) — eine Wandzelle ohne Sichtseite kann es per Definition nicht
geben, und vier Sichtseiten treten in den handgeschriebenen Layouts nicht auf. Die ID bleibt als
Reserve im Code stehen, falls später Layouts dazukommen.

`kit_wall` ist der generische Ersatz: fehlt die spezifische Variante, wird er verwendet — **und
trotzdem gedreht**. Er muss deshalb unter allen vier Vierteldrehungen gleich aussehen.

**Eine Innenecke braucht kein Modul.** Die Diagonalzelle an einer Raumecke ist weder Boden noch
Wand (kein begehbarer 4er-Nachbar) und bekommt gar keine Geometrie — in REFUGE sind das 30 solcher
Zellen, in THRONE 10. Sichtbar wird das nur, wenn die Wandmodule **nicht** die volle Zelltiefe
füllen. Eine dünne Wandplatte an der Zellkante würde an jeder Raumecke ins Leere blicken lassen.
Das ist der harte Grund für die 2,80 m Tiefe.

### 4.3 Deko

| ID | Maße (m) | Anzahl | Pivot | Ton |
|---|---|---:|---|---|
| `kit_pillar` | ≤ 1,36 × **6,14** × 1,36 | 104 (26 Nicht-CAVERNS-Räume × 4) | unten mittig, `y = 0` | ja, Steinton |
| `kit_arch` | **8,06** × **7,07** × 0,72 | 26 | unten mittig, `y = 0`, Öffnung in **Z** durchschreitbar | ja, Steinton |
| `kit_brazier` | ≤ 0,54 × **2,70** × 0,54 | 66 | unten mittig, `y = 0` | **nein** |
| `kit_banner` | 1,24 × **2,60** × 0,06 | 52 | **unten** mittig (Oberkante landet bei 5,70 m) | **nein** |
| `kit_carpet` | **1,00 × 1,00** in XZ, Dicke frei (Platzhalter 0,018) | 26 | unten mittig, `y = 0` | ja, Teppichton |
| `kit_rock` | in einen **2 × 2 × 2 m** Würfel eingepasst | 28 (nur CAVERNS) | **Mitte** — Ausnahme, siehe unten | **nein** |

* `kit_pillar` ersetzt alle drei Platzhalterkästen (Sockel, Schaft, Kapitell) als **ein** Modul.
  Der Kollisionskörper bleibt unverändert `(0,96 × 6,00 × 0,96)` um `y = 3,0`
  (`WorldView.java:335`) — das Modul darf also schlanker sein als 0,96 m, aber nicht wesentlich
  dicker, sonst schiebt es sichtbar durch seine Kollisionsbox.
* `kit_arch` ersetzt die 13 Keilsteine **und** die zwei Kämpfer als ein Modul. Die beiden
  Kollisionskörper der Kämpfer bleiben bei `x = ±3,55`, `y = 1,65`, Halbmaße `(0,24; 1,65; 0,35)`
  (`WorldView.java:364`).
* `kit_brazier` ersetzt **nur** Ständer und Schale. Die Flamme bleibt prozedural, weil ihr
  `glow`-Material den Bloom-Pass speist und das `PointLight` darüber von `update()` animiert wird
  (`WorldView.java:403–415`, `WorldView.java:529–538`). **Der Raum 0,32 × 0,70 × 0,32 m um
  `y = 2,86` muss frei bleiben**, die Lichtquelle sitzt bei `y = 3,05`.
* `kit_carpet` wird in X und Z auf die volle Ausdehnung skaliert, Y bleibt 1
  (`WorldView.java:294`). Das Modul ist deshalb als **1 × 1 m** Fliese zu bauen. Die
  Streckungsfaktoren sind 4,40 in X und 5,67 / 9,87 / 14,07 / 22,47 in Z — siehe Risiko R4.
* `kit_rock` ist die **einzige** Pivot-Ausnahme: der Platzhalter ist eine um ihren Mittelpunkt
  skalierte Kugel, und Skalierung, Translation und Kollisionsbox hängen daran
  (`WorldView.java:383–386`). Das Modul gehört daher mittig in einen 2 × 2 × 2 m Würfel; der Code
  skaliert es anschließend mit `(s; s · 1,6; s)`, `s ∈ {0,9; 1,2}`, und setzt es auf `y = s · 0,6`.
  Wie der Platzhalter steckt es dann um `s` im Boden — das ist beabsichtigt.

---

## 5. Wie ein Modul im Gitter landet — die Rechenregel

```
Zelle (x, z)  →  Weltmitte (x · 2,8, ·, z · 2,8)                      WorldView.java:74
Modul-Translation  =  (Weltmitte.x,  y_Platzhalter − Halbhöhe,  Weltmitte.z)
                                                                      WorldView.java:189–199
Modul-Rotation     =  Vierteldrehung um +Y, nur bei Wandzellen         WorldView.java:125–128
```

Konkret, damit man es in Blender nachrechnen kann:

| ID | Translation y | Höhe des Moduls | Oberkante |
|---|---:|---:|---:|
| `kit_floor` | −0,25 | 0,24 | −0,01 |
| `kit_ceiling` | 6,92 | 0,36 | 7,28 |
| `kit_rib` | 6,24 | 0,32 | 6,56 |
| `kit_wall_*` | −0,10 | 6,80 (empf. 7,02) | 6,70 (empf. 6,92) |
| `kit_cornice` | 4,64 | 0,32 | 4,96 |
| `kit_banner` | 3,10 | 2,60 | 5,70 |
| `kit_carpet` | 0,00 | frei | frei |
| `kit_pillar`, `kit_arch`, `kit_brazier` | 0,00 | wie Tabelle 4.3 | — |

---

## 6. Die Gebietstönung — die kritische Designfrage

### Wie es heute funktioniert

`AssetPipeline.pbr(surface, tint, roughness, metallic)` (`AssetPipeline.java:32–51`) legt den
Gebietston als **`BaseColor`** auf ein `PBRLighting.j3md`-Material und hängt die gemeinsamen
Stein-Maps daran. Der Ton kommt aus `Region.stone` (`Region.java:4–14`):

| Gebiet | Steinton | Fundstelle |
|---|---|---|
| I Die letzte Zuflucht | `#6d7e88` | `Region.java:8` |
| II Krypta der Eide | `#667886` | `Region.java:11` |
| III Die gläserne Tiefe | `#5b716e` | `Region.java:12` |
| IV Das Kettenverlies | `#817060` | `Region.java:13` |
| V Der Aschenthron | `#6b636e` | `Region.java:14` |

Dazu die nicht-gebietsabhängigen Töne derselben Bauteile: Boden `#8c8b86`, in CAVERNS `#687771`
(`WorldView.java:59`); Teppich `#243c48` in REFUGE, sonst `#492a2d` (`WorldView.java:257`).

### Warum das mit einem glTF-Modul nicht von selbst zusammengeht

Ein glTF-Modul bringt sein eigenes Material mit. Zwei Belege direkt aus dem ausgelieferten
jME-Jar (`target/aschensiegel-1.0.0.jar`):

* `com/jme3/scene/plugins/gltf/PBRMetalRoughMaterialAdapter.class` bildet
  `baseColorFactor → BaseColor`, `baseColorTexture → BaseColorMap`,
  `metallicFactor → Metallic`, `roughnessFactor → Roughness`,
  `metallicRoughnessTexture → MetallicRoughnessMap` ab — das Modul landet also auf **demselben**
  `Common/MatDefs/Light/PBRLighting.j3md`, das `AssetPipeline.pbr` benutzt.
* In `Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib` steht in Zeile 338
  `vec4 baseColor = texture2D(m_BaseColorMap, newTexCoord) * Color;` — **`BaseColor` ist ein
  Multiplikator auf die Albedo-Textur**, kein Ersatz.

Damit ist die Lösung sauber möglich, und sie ist in `WorldView.tint(...)`
(`WorldView.java:210–225`) umgesetzt: der Gebietston wird als Faktor auf das `BaseColor` des
Moduls multipliziert. Das Modul behält seine eigenen Maps, UVs und Normalen und braucht nur eine
**neutrale (farblose) Albedo**.

### Die Rezeptur

Ein naives Multiplizieren mit dem Steinton verdunkelt um etwa Faktor sechs, weil die Gebietstöne
mittlere Graustufen sind. Der Faktor wird deshalb über die eigene Leuchtdichte normiert — dieselbe
Rezeptur, die `docs/assets/asset-liste.md:399–420` für die gebackenen Charaktertönungen festhält:

```
lum   = 0.2126·R + 0.7152·G + 0.0722·B        (linear)
Faktor = Ton_linear / lum(Ton_linear)          Alpha auf 1 zurückgesetzt
BaseColor_neu = BaseColor_Modul · Faktor
```

`AssetPipeline.color(hex)` rechnet den Hexwert über `setAsSrgb` nach linear
(`AssetPipeline.java:27–30`), die Multiplikation findet also korrekt im linearen Raum statt. Die
Faktoren, ausgerechnet:

| Ton | Hex | linear R / G / B | Leuchtdichte | Faktor R / G / B |
|---|---|---|---:|---|
| REFUGE Stein | `#6d7e88` | 0,1529 / 0,2086 / 0,2462 | 0,1995 | 0,767 / 1,046 / 1,234 |
| CRYPT Stein | `#667886` | 0,1329 / 0,1878 / 0,2384 | 0,1798 | 0,739 / 1,045 / 1,326 |
| CAVERNS Stein | `#5b716e` | 0,1046 / 0,1651 / 0,1559 | 0,1516 | 0,690 / 1,089 / 1,029 |
| PRISON Stein | `#817060` | 0,2195 / 0,1620 / 0,1170 | 0,1710 | 1,284 / 0,948 / 0,684 |
| THRONE Stein | `#6b636e` | 0,1470 / 0,1248 / 0,1559 | 0,1318 | 1,116 / 0,947 / 1,183 |
| Boden, nicht CAVERNS | `#8c8b86` | 0,2623 / 0,2582 / 0,2384 | 0,2576 | 1,018 / 1,002 / 0,925 |
| Boden CAVERNS | `#687771` | 0,1384 / 0,1845 / 0,1651 | 0,1733 | 0,799 / 1,065 / 0,953 |
| Teppich REFUGE | `#243c48` | 0,0176 / 0,0452 / 0,0648 | 0,0407 | 0,433 / 1,109 / 1,590 |
| Teppich sonst | `#492a2d` | 0,0666 / 0,0232 / 0,0262 | 0,0326 | 2,043 / 0,710 / 0,804 |

Die Normierung heißt: **die Helligkeit des Moduls bleibt, nur der Farbstich wechselt.** Damit
verschiebt sich aber die Verantwortung für die Helligkeit auf die Albedo des Moduls — und das ist
der Punkt, an dem man sich vertun kann.

### Zielhelligkeit der Modul-Albedo, gemessen

Der Platzhalter kommt auf `Albedotextur × BaseColor`. Die mitgelieferte
`src/main/resources/textures/stone-albedo.png` (512 × 512) hat eine mittlere **lineare**
Leuchtdichte von **0,5100** (Minimum 0,0773, Maximum 0,8826; gemessen über alle 262 144 Pixel,
Dateistand 2026-09-12 16:13). **Achtung:** diese Datei wird von `tools/AssetBaker.java` erzeugt und
war während dieses Durchgangs in einem parallelen Strang in Arbeit. Ändert sich der Backer, ist die
folgende Tabelle neu zu messen — die Rechnung selbst bleibt gültig.

Netto ergibt das heute:

| Bauteil | Ton | Y(Ton) | Y(Textur) | **Y(netto)** | Neutralgrau mit dieser Leuchtdichte |
|---|---|---:|---:|---:|---|
| Wand / Decke / Rippe REFUGE | `#6d7e88` | 0,1995 | 0,5100 | **0,1018** | ≈ `#5a5a5a` |
| Wand CRYPT | `#667886` | 0,1798 | 0,5100 | **0,0917** | ≈ `#555555` |
| Wand CAVERNS | `#5b716e` | 0,1516 | 0,5100 | **0,0773** | ≈ `#4f4f4f` |
| Wand PRISON | `#817060` | 0,1710 | 0,5100 | **0,0872** | ≈ `#535353` |
| Wand THRONE | `#6b636e` | 0,1318 | 0,5100 | **0,0672** | ≈ `#494949` |
| Boden nicht-CAVERNS | `#8c8b86` | 0,2576 | 0,5100 | **0,1314** | ≈ `#656565` |
| Boden CAVERNS | `#687771` | 0,1733 | 0,5100 | **0,0884** | ≈ `#545454` |

**Vorgabe für die Modul-Albedo:** mittlere lineare Leuchtdichte **0,07 bis 0,10** für Wand, Decke,
Rippe, Pfeiler und Bogen, **0,09 bis 0,13** für den Boden — in sRGB grob `#494949` bis `#656565`.
Wer eine Albedo bei 0,22 linear (`#808080`) baut, bekommt Wände, die **zwei- bis dreimal heller**
sind als heute. Die Konsistenzprüfung ist dieselbe wie bei den Charakteren: mittlere lineare
Leuchtdichte je Map messen und gegen diese Tabelle halten.

### Rauheit und Metallizität

Ebenfalls aus dem Shader belegt (`PBRLightingUtils.glsllib:354–365`): `Roughness` und `Metallic`
sind **multiplikativ** auf ihre Maps. Die Rauheitsmap des Platzhalters hat im Mittel 0,8453
(Spanne 0,6824 … 0,9725), die Metallic-Map ist exakt 0 — gemessen an `stone-roughness.png` und
`stone-metallic.png`, gleicher Dateistand wie oben; dazu die Skalare aus
`WorldView.java:60, 61, 256` und `AssetPipeline.java:32`:

| Bauteil | Skalar | × Map | **effektiv** |
|---|---:|---:|---:|
| Wand / Decke / Rippe | 0,94 | 0,8453 | **0,795** |
| Boden | 0,86 | 0,8453 | **0,727** |
| Pfeiler / Bogen | 0,90 | 0,8453 | **0,761** |
| Metallizität, alle Steinteile | 0,00 | 0,0 | **0,000** |

Zielwerte für die Steinmodule also **Rauheit 0,73 – 0,80 effektiv, Metallizität 0**. Das liegt
komfortabel innerhalb der Grenzen aus `STYLE.md:43` (`Metallic ≤ 0.4`, `Roughness ≥ 0.55`).

Für die beiden Metallteile des Kits gilt etwas anderes, ebenfalls gemessen an `metal-*.png`
(512 × 512, `metal-metallic` Mittel 0,3907 / Spanne 0,149 … 0,498; `metal-roughness` Mittel 0,9295
/ Spanne 0,800 … 0,988):

| Bauteil | Material | Metallizität effektiv | Rauheit effektiv |
|---|---|---:|---:|
| `kit_cornice` | `iron` (0,8 / 0,5), `WorldView.java:47` | 0,8 × 0,3907 = **0,313** | 0,5 × 0,9295 = **0,465** |
| `kit_brazier`, Schale | `gold` (0,65 / 0,45), `WorldView.java:48` | 0,65 × 0,3907 = **0,254** | 0,45 × 0,9295 = **0,418** |

**Ehrlich benannt:** die effektive Rauheit dieser beiden Platzhalter liegt mit 0,465 und 0,418
**unter** der Untergrenze 0,55 aus `STYLE.md:43`. Der Bestand verletzt seine eigene Vorgabe.
Für die Module empfehle ich, sich an `STYLE.md` zu halten (Rauheit ≥ 0,55, Metallizität ≤ 0,4)
und nicht an den Platzhalter; das Metall wird dadurch matter, was der Absicht in `STYLE.md:38–45`
entspricht. Glanz und Kantenabrieb gehören laut `STYLE.md:43–45` in die Albedo gemalt.

### Was an dieser Lösung ehrlich benannt werden muss

1. **Die Tönung ist nie ausgeführt worden.** Es liegt kein Modul im Repository, also hat
   `WorldView.tint(...)` noch nie ein echtes glTF-Material in der Hand gehabt. Belegt sind der
   Parametername, der Materialdefinitionspfad und die multiplikative Shaderzeile — aus den
   Klassendateien und der GLSL-Bibliothek im Jar. **Nicht belegt** ist, wie es aussieht.
2. **Die Tönung trifft jedes Material des Moduls.** `tint` läuft über alle Geometrien und
   multipliziert jedes `BaseColor`. Ein Wandmodul mit Messing-Einlage bekommt den Gebietston auch
   auf das Messing. Trennung geht nur durch Aufteilen in mehrere Module — so sind Kranz, Banner
   und Kohlebecken bereits getrennt und ungetönt.
3. **`KHR_materials_unlit` ist verboten.** `UnlitMaterialAdapter` bildet auf
   `Common/MatDefs/Misc/Unshaded.j3md` mit dem Parameter `Color` ab — kein `BaseColor`, also keine
   Tönung, und kein Licht. Module müssen metallic-roughness sein.
4. **Ohne `baseColorFactor` greift der Matdef-Standardwert** `BaseColor : 1.0 1.0 1.0 1.0`
   (`PBRLighting.j3md:14`). Die Multiplikation funktioniert dann trotzdem. Findet `tint` gar kein
   `BaseColor`, lässt es das Material unangetastet — es gibt keinen Absturz, nur keine Tönung.
5. **Eine ID trägt genau einen Ton pro Gebiet.** Der Ton wird beim ersten Auflösen auf den
   Prototypen gelegt (`WorldView.java:170–181`); alle Klone teilen ihn. Das ist per Konstruktion
   erfüllt, weil jede ID im Code mit genau einem Ton angefragt wird — wer eine ID an zwei Stellen
   mit verschiedenen Tönen benutzen will, muss das ändern.
6. **Verworfene Alternative A:** das Modulmaterial durch `assets.pbr("stone", tint, …)` ersetzen.
   Der Ton wäre exakt, aber die eigenen Texturen des Moduls wären weg und die gemeinsame Stein-Map
   läge 1:1 auf fremden UVs. Das nimmt dem Kit genau den Zweck.
7. **Verworfene Alternative B:** je Gebiet ein eigener Modulsatz (`kit_wall_face_crypt` …). Exakte
   Kontrolle, aber fünffache Modellierarbeit und fünffacher Speicher. Bleibt die Rückfalloption,
   falls die multiplizierte Tönung im Sichttest nicht trägt.

---

## 7. Was ausdrücklich **nicht** über Module läuft

| Sache | Warum | Fundstelle |
|---|---|---|
| **Kollision** | Alle `physics.box(...)`-Aufrufe kommen weiter aus dem Layout, nicht aus der Modulgeometrie. Die zusammengefassten Wandläufe (`(start+x−1)·cell/2`, `y = 3,3`, Halbmaße `((x−start)·cell/2; 3,4; 1,4)`) sind unverändert. Ein Modul kann die Kollision also nicht kaputtmachen — aber auch nicht verfeinern. | `WorldView.java:142–156, 335, 364, 386, 64` |
| **`DistanceLodControl`** | Die Kontrolle tauscht das `Mesh` einer `Geometry` und kann an einem importierten Knoten nicht hängen. Sie wird deshalb **nur** im prozeduralen Felspfad gesetzt. Ein `kit_rock` hat keinen LOD-Wechsel und muss seine Dreieckszahl selbst niedrig halten. Das deckt sich mit `STYLE.md:162`. | `WorldView.java:373–381`, `assets/DistanceLodControl.java:21–29` |
| **Schattenmodus** | `root` steht auf `CastAndReceive`, die Deckenplatte wird auf `Receive` gesetzt, die Flamme auf `Off`. Das gilt für Modul und Platzhalter gleich, weil es auf dem zurückgegebenen `Spatial` gesetzt wird. | `WorldView.java:52, 93, 408` |
| **Kristalle** | `crystal(...)` bleibt prozedural. Es gibt bereits ein echtes `models/props/crystal.gltf`; würde die Deko-Funktion darauf umgestellt, änderte sich das Verhalten **sofort** und die Bedingung „verhält sich wie heute" wäre gebrochen. | `WorldView.java:521–527` |
| **Props** | `shrine`, `portal`, `chest`, `lore`, `rune`, `seal`, `crystal`, `throne` werden weiter am Ende von `buildObject` ersetzt, ungetönt und unverändert. | `WorldView.java:499–508` |
| **Flamme und Fackellicht** | Bleiben prozedural, siehe 4.3. | `WorldView.java:403–415` |

### Und eine Stelle, an der Modulgeometrie doch spielrelevant ist

`occluders` enthält die Chunks, Pfeiler, Bögen und Felsen. Dieser Knoten wird für
**Strahltests** benutzt:

* Kamerakollision, fünf Strahlen — `entities/PlayerController.java:171–196`
* Sichtlinie der Gegner — `combat/CombatSystem.java:105–115`
* Projektile — `combat/CombatSystem.java:97–99`

Damit gilt: **ein Fenster, eine Schießscharte oder ein Loch im Wandmodul ist eine Sichtlinie.**
Gegner würden dadurch hindurchsehen, und die Kamera würde daran hängen. Die Sichtlinie wird zwar
zuerst über das Gitter geprüft (`DungeonLayout.clearLine`, `DungeonLayout.java:297–302`), aber der
Strahltest kann nur zusätzlich blockieren, nie freigeben — ein Vorsprung an der Decke unter
etwa 7 m Höhe zieht die Kamera heran. Wandmodule bleiben deshalb geschlossen.

---

## 8. Dreiecksbudget

Die Chunks werden je Material zu einem Netz verschmolzen (`GeometryBatchFactory.optimize`,
`WorldView.java:158`). Maximale Belegung eines Chunks: 36 Zellen.

Der heutige Platzhalterstand, größtes Gebiet REFUGE — `Box` = 12 Dreiecke, `Sphere(12,16)` = 320:

```
Boden    477 × 12 =  5 724      Pfeiler  28 ×  36 = 1 008
Decke    468 × 12 =  5 616      Bogen     7 × 180 = 1 260
Rippe    128 × 12 =  1 536      Fackel   14 × 344 = 4 816
Wand     198 × 12 =  2 376      Teppich   7 ×  12 =    84
Kranz    198 × 12 =  2 376      Banner   14 ×  12 =   168
                                          Summe   = 24 964
```

**Vorschlag** (kein gemessener Wert, eine Empfehlung), zum Vergleich: der Goblin hat 20 000
Dreiecke (`asset-liste.md:554`), Props sollen laut `STYLE.md:162` bei 4 000 – 8 000 liegen —
das gilt für Einzelprops, nicht für ein 477-fach instanziiertes Bodenmodul.

| ID | Dreiecke je Modul | × Anzahl REFUGE | Summe |
|---|---:|---:|---:|
| `kit_floor` | 80 | 477 | 38 160 |
| `kit_ceiling` | 160 | 468 | 74 880 |
| `kit_rib` | 48 | 128 | 6 144 |
| `kit_wall_face` | 400 | 170 | 68 000 |
| `kit_wall_corner` | 600 | 24 | 14 400 |
| `kit_wall_span` | 600 | 2 | 1 200 |
| `kit_wall_pier` | 700 | 2 | 1 400 |
| `kit_cornice` | 96 | 198 | 19 008 |
| `kit_pillar` | 900 | 28 | 25 200 |
| `kit_arch` | 2 400 | 7 | 16 800 |
| `kit_brazier` | 500 | 14 | 7 000 |
| `kit_carpet` | 8 | 7 | 56 |
| `kit_banner` | 48 | 14 | 672 |
| | | **Summe** | **272 920** |

CAVERNS mit `kit_rock` bei 600 Dreiecken kommt auf 233 888. Das ist etwa elfmal der
Platzhalterstand und in derselben Größenordnung wie die Charaktere eines Abschnitts. Wer deutlich
darüber geht, sollte vorher messen — ein Sichttest ist dafür nicht nötig, die Multiplikation
reicht.

---

## 9. Was der Kit **nicht** abdeckt

* **Türöffnung / echter Durchgang.** Im Code gibt es keine Wandöffnung: Gänge sind drei Zellen
  breiter Boden, und der Bogen steht frei im Raum. Ein `kit_doorway` bräuchte in `WorldView` eine
  neue Platzierungsregel (etwa: Wandzelle, die zwei Räume trennt) und ist deshalb **nicht**
  verdrahtet. Das ist die naheliegendste nächste Erweiterung, aber sie ist eine Layout-Änderung,
  keine Asset-Lieferung.
* **Treppen, Höhenstufen.** Das Layout ist ein flaches Gitter (`boolean[][] floor`,
  `DungeonLayout.java:35`). Es gibt keine zweite Ebene.
* **Gebietsspezifische Silhouetten.** Alle fünf Gebiete benutzen denselben Modulsatz und
  unterscheiden sich nur über Ton, Boden-Sonderfarbe und Fels statt Pfeiler
  (`WorldView.java:265–271`).
* **Deckenlöcher als Modul.** Die Lichtschächte entstehen dadurch, dass eine Platte **fehlt**. Ein
  eigenes Rahmenmodul dafür wäre neuer Code.

---

## 10. Prüfliste für die erste Lieferung

Ein Modul gilt erst als geliefert, wenn diese Punkte belegt sind — derselbe Maßstab wie in
`asset-liste.md`:

1. Datei liegt unter `src/main/resources/models/props/kit_*.glb`, danach **neu gebaut**.
2. Maße im Blender-Export gegen Abschnitt 3 und 5 geprüft: Grundfläche, Höhe, Pivot unten mittig,
   Rotation angewendet, Skalierung `(1,1,1)`, `export_yup=True`.
3. Ein einziges Mesh je Modul, `topology = triangle`, keine losen Vertices, keine
   non-manifold-Kanten (der `remove_doubles`-Schritt aus `asset-liste.md:579–590` gilt auch hier).
4. Material metallic-roughness, **nicht** unlit. Albedo neutral, mittlere lineare Leuchtdichte
   gegen die Tabelle in Abschnitt 6 gemessen. Effektive Rauheit 0,73 – 0,80, Metallizität 0.
5. `mvn -B verify` grün, 50 Tests. Es gibt noch **keine** Zusicherung für Kit-Module in
   `AssetTest`; eine analog zu `referenceGltfIsImportedAsRealGeometry`
   (`src/test/java/de/pentagon/AssetTest.java:22–28`) wäre der richtige Ort.
6. Smoke-Run über alle 33 Stufen ohne Exception.
7. Sichttest in **allen fünf** Gebieten — die Tönung ist genau die Sache, die nur dort auffällt.
   Zu prüfen: Helligkeit gegen den Platzhalter, Fuge über der Wand, Raumecken, Kameraklemmen an
   Pfeilern und Bögen.
8. Bildrate vor und nach der Lieferung notieren; das Batching ist die Stelle, an der es kippen
   kann (Risiko R1).

---

## 11. Offene Punkte und Risiken

**R1 — Batching mit importierten Netzen ist ungeprüft.** `GeometryBatchFactory.optimize` fasst je
Chunk nach Material zusammen (`WorldView.java:158`). Der Zusammenbau verlangt zueinander passende
Vertex-Buffer; importierte Netze mit unterschiedlichen Buffersätzen (mal mit, mal ohne Tangenten)
können dabei brechen oder still falsche Daten liefern. Gegenmaßnahme in der Spezifikation: **ein**
Mesh je Modul, immer mit Position, Normale, Tangente und einem UV-Satz. Zu prüfen mit dem ersten
echten Modul. Damit die Verschmelzung überhaupt greift, teilen alle Klone eines Moduls ein
Material — `clone(false)` statt `clone()` (`WorldView.java:178`); sonst hätte jede Zelle ihr
eigenes Material und der Chunk zerfiele in einen Batch je Zelle.

**R2 — Die Tönung ist nur gerechnet, nicht gesehen.** Siehe Abschnitt 6, Punkt 1. Das ist das
größte Restrisiko dieser Lieferung.

**R3 — Modulhelligkeit.** Die Normierung der Tönung verlegt die Helligkeit in die Albedo. Wird sie
zu hell gebaut, wird der Dungeon flach und hell — genau gegen `STYLE.md`. Die gemessene Zieltabelle
in Abschnitt 6 ist die Absicherung, aber sie beschreibt nur die **mittlere** Leuchtdichte, nicht
die Verteilung.

**R4 — Der Teppich wird gestreckt.** Faktor 4,40 in X und bis 22,47 in Z auf eine 1-m-Fliese
(`WorldView.java:294`). Eine Textur, die dafür nicht gebaut ist, verschmiert sichtbar. Optionen:
UVs so legen, dass sie sich wiederholen; oder den Teppich prozedural lassen — er ist mit 26 Stück
und 12 Dreiecken der unwichtigste Posten im Kit.

**R5 — Der Kollisionskörper bleibt ein Kasten.** Ein rund modellierter Pfeiler steckt weiter in
einer 0,96-m-Box (`WorldView.java:335`), ein organischer Fels in einer Box von `s·1,4` Breite
(`WorldView.java:386`). Bei stark abweichender Silhouette entsteht eine spürbare Lücke zwischen
Optik und Kollision. Das ist eine bewusste Grenze dieser Lieferung: die Kollision soll aus dem
Layout kommen.

**R6 — Wandmodulhöhe.** 6,80 m reproduziert den Bestand samt der 0,22-m-Fuge, 7,02 m schließt sie
und weicht damit vom Platzhalter ab. Das ist eine **Gestaltungsentscheidung**, die ich nicht
allein treffe; hier steht sie als belegte Wahl.

**R7 — `AssetTest` deckt den Kit nicht ab.** Heute prüft nichts, ob ein Kit-Modul lädt. Ein
fehlerhaftes Modul fällt still auf den Platzhalter zurück (Props brechen still,
`COWORK-BRIEFING.md:60`) — bequem im Betrieb, unangenehm beim Suchen. Eine Zusicherung je
geliefertem Modul gehört nachgezogen.

**R8 — Mehrkosten im Auflösungspfad.** Pro Gebietsaufbau werden jetzt bis zu zwölf Kit-IDs
einmalig über `AssetPipeline.model` nachgefragt, also bis zu 48 zusätzliche
`getResource`-Aufrufe (vier Endungen je ID). Das Ergebnis wird pro `WorldView` gecacht
(`WorldView.java:39, 170–181`), auch die Abwesenheit. Das ist der **einzige** messbare Unterschied,
solange keine Moduldatei existiert.

---

## 12. Belegstand dieser Datei

| Angabe | Belegt durch |
|---|---|
| Alle Maße, Positionen, Anzahlen je Modul | Direkt aus `WorldView.java`, `DungeonLayout.java`, `Region.java`, `AssetPipeline.java` gelesen; Fundstelle jeweils in der Tabelle |
| Zellzahlen je Gebiet, Wandklassifikation, Chunk-Belegung | Exakte Nachbildung von `room`/`carve`/`connect`/`walkable`/`wall` und von `WorldView.wallKit`; alle 853 Wandzellen treffen nach der berechneten Drehung genau ihre offenen Seiten (0 Fehlausrichtungen) |
| Tönungsfaktoren, Leuchtdichten | Aus den Hexwerten im Code gerechnet; `stone-albedo.png`, `stone-roughness.png`, `stone-metallic.png`, `metal-metallic.png`, `metal-roughness.png` über alle 262 144 Pixel gemessen (Dateistand 2026-09-12 16:13; der Backer war parallel in Arbeit) |
| `BaseColor` als Multiplikator, Parameternamen des glTF-Imports, Rauheit multiplikativ | Aus `PBRMetalRoughMaterialAdapter.class`, `UnlitMaterialAdapter.class`, `PBRLighting.j3md` und `PBRLightingUtils.glsllib` im ausgelieferten Jar gelesen |
| Kompilierbarkeit der Erweiterung | `javac` über alle Hauptquellen, 63 Klassen, keine Fehler |
| **Nicht belegt** | Aussehen, Bildrate, Batching mit echten Modulen, `mvn verify`, Smoke-Run, Sichttest — nichts davon wurde in diesem Durchgang ausgeführt |

---

## 13. Erste Lieferung: `kit_floor` und `kit_wall_face` (2026-09-12)

Zwei Module gebaut und **der Modulpfad damit erstmals überhaupt gelaufen**. Bis hierher war
belegt, dass er kompiliert; jetzt ist belegt, dass er trägt.

| ID | Maße | Dreiecke | Datei |
|---|---|---:|---|
| `kit_floor` | 2,80 × 0,237 × 2,80 | 122 | `models/props/kit_floor.glb`, 668 KB |
| `kit_wall_face` | 2,80 × 7,02 × 2,86 | 106 | `models/props/kit_wall_face.glb`, 660 KB |

`kit_floor` ist eine Platte mit umlaufender Fase und einer flachen Mulde in der Mitte, viertelweise
symmetrisch — weil alle 394–477 Instanzen **dasselbe** Modul sind, würde jede asymmetrische Form
ein sichtbares Richtungsraster ergeben. `kit_wall_face` ist ein über X gezogenes Profil mit Sockel
(0–0,45 m, 6 cm vorstehend), Gurtband (2,40–2,70 m, 4 cm) und oberer Abschrägung; links/rechts
symmetrisch aus demselben Grund. Höhe **7,02** statt 6,80, damit die im Dokument belegte 0,22-m-Fuge
zur Decke geschlossen ist.

### Zwei Fehler, die nur durch Messen auffielen

**1. Farbraum.** Blenders `image.pixels` liefert bei 8-Bit-Bildern **sRGB**-Werte, nicht linear.
Der erste Anlauf hat die Albedo deshalb im falschen Raum skaliert. Kontrollmessung nach der
Korrektur: mittlere lineare Leuchtdichte von `stone-albedo.png` = **0,5100** — genau der in
Abschnitt 6 belegte Wert. Damit stimmen die Faktoren:

| Modul | Zielband (Abschnitt 6) | Faktor linear | erreicht |
|---|---|---:|---:|
| `kit_floor` | 0,09 – 0,13 | 0,2157 | **0,1100** |
| `kit_wall_face` | 0,07 – 0,10 | 0,1667 | **0,0850** |

Der Roughness-Skalar der Platzhalter (Boden 0,86, Wand 0,94) ist **in die Map gebacken**, weil
Blender bei angeschlossener Textur `roughnessFactor = 1` schreibt. Effektive Mittel: 0,727 und
0,795.

**2. Achsen.** Der erste Export war um 90° gekippt — Boden auf der Kante, Wand liegend. Ursache:
in Blender als Höhe Y gebaut, Blender ist aber **Z-up**, und `export_yup=True` dreht dann noch
einmal. Ebenso zeigte die Wandvorderseite nach **−Z** statt +Z, weil Blender +Y auf glTF −Z
abgebildet wird. Beides gemessen an der Bounding-Box im Engine-Probe, nicht geschätzt:

```
kit_floor      xExtent 1,400  yExtent 0,118  zExtent 1,400   Mitte y = 0,118  -> Unterkante 0
kit_wall_face  xExtent 1,400  yExtent 3,510  zExtent 1,430   Mitte z = +0,030 -> Sockel nach +Z
```

**Merksatz für die weiteren Module: in Blender ist Z die Höhe, und die Vorderseite wird nach −Y
gebaut.**

### Belegt, dass die Tönung greift

Die Sorge war, dass Blender keinen `baseColorFactor` ins glTF schreibt (bestätigt: das Feld fehlt)
und `WorldView.tint()` dann über `getParam("BaseColor") == null` **lautlos** aussetzt. Gemessen im
Engine-Probe: jMEs `PBRMetalRoughMaterialAdapter` setzt den Standard trotzdem —

```
BaseColor param = ColorRGBA Color[1.0, 1.0, 1.0, 1.0]
Roughness = 1.0   Metallic = 0.0   NormalType = 1.0
BaseColorMap / NormalMap / MetallicRoughnessMap gesetzt, je 512x512
```

Damit ist das Risiko R-Tönung aus Abschnitt 11 erledigt.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| Engine-Probe | beide Module laden, Maße und Pivot korrekt, `BaseColor` vorhanden | `KitProbe`, temporär außerhalb des Repos |
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-kit.log` |
| Smoke-Run | alle **33 Stufen**, `smoke-ok.txt`, 13 Screenshots, keine Exception | `cowork-logs/smoke-kit.log` |
| Sichttest | Boden mit Zellfugen, Wände mit Sockel und Gurtband, **Gierdrehung** richtet die Vorderseite in den Raum, Gebietston liegt an | `target/screenshots/pentagon-6.png`, `-8.png` |

### Offen

* Die Bodenfuge bildet jetzt ein **regelmäßiges 2,8-m-Raster** über den ganzen Raum, zusätzlich zum
  Kachelmuster der Textur. Das liest sich als große Platten, ist aber doppelt gerastert. Wer es
  ruhiger will, nimmt die Fase zurück.
* Jedes Modul trägt seine Texturen **eingebettet** (je ~660 KB). Bei 13 Modulen wären das rund
  8 MB mehrfach abgelegtes Identisches. Sauberer wäre `.gltf` mit externen Bild-URIs auf
  `textures/` — `props/crystal.gltf` zeigt, dass jME das trägt. Bewusst zurückgestellt, bis der
  Pfad stand.
* `GeometryBatchFactory.optimize` über Modulgeometrie ist **nicht** gemessen worden, nur dass der
  Lauf ohne Exception durchgeht. Risiko R1 aus Abschnitt 11 bleibt offen.
* Elf Module fehlen noch: Decke, Rippe, Kranz, die drei weiteren Wandvarianten, Pfeiler, Bogen,
  Kohlebecken, Banner, Teppich, Fels.

---

## 14. Die restlichen elf Module (2026-09-12)

Damit liegt der **vollständige Kit** im Repository: 13 Modul-IDs plus die generische Reserve
`kit_wall`. Alle Maße sind von `KitProbe` **in der Engine** nachgemessen, nicht in Blender
abgelesen — Belege `cowork-logs/kitprobe-module-set.log` und `kitprobe-deko-set.log`.

### Ein Generator für alle Wandvarianten

Die vier Wandvarianten unterscheiden sich nur darin, welche der vier Seiten das Profil trägt. Sie
werden deshalb von **einer** Funktion erzeugt, `art/blender/kit_builder.py`. Das Profil ist Punkt
für Punkt aus dem gelieferten `kit_wall_face` übernommen, damit keine Variante eine andere
Silhouette hat:

| Höhe z | Auswärtsversatz gegen 1,40 | Bauteil |
|---:|---:|---|
| 0,00 | +0,06 | Sockelfuß |
| 0,45 | +0,06 → 0,00 | Sockeloberkante, Rücksprung |
| 2,40 | +0,04 | leichte Anlauf-Schräge |
| 2,70 | +0,04 → 0,00 | Gurtband, Rücksprung |
| 6,80 | 0,00 | Wandfläche oben |
| 7,02 | −0,10 | Fase, liegt in der Deckenplatte |

Der Aufbau ist ein **Stapel waagerechter Ringe**: pro Profilhöhe ein Rechteck, dessen vier Seiten
je ihren eigenen Versatz tragen — sichtbare Seite Profil, blinde Seite 0. Die Gehrung an einer
Ecke entsteht dadurch von selbst als Schnittpunkt zweier Versätze, und Flächen mit Nullfläche
(zwei gleiche Ringe auf einer blinden Seite) werden übersprungen. Die Varianten sind damit
**konstruktiv** stoßfest zueinander.

Gemessen in der Engine, glTF-Achsen, Höhe 7,02 m, Unterkante bei 0:

| ID | Sichtseiten | xExtent | zExtent | Dreiecke | Budget |
|---|---|---:|---:|---:|---:|
| `kit_wall_face` | +Z | 1,400 | **1,430** | 106 | 600 |
| `kit_wall_corner` | +Z, +X | **1,430** | **1,430** | 108 | 600 |
| `kit_wall_span` | +Z, −Z | 1,400 | **1,460** | 108 | 600 |
| `kit_wall_pier` | +Z, +X, −Z | **1,430** | **1,460** | 116 | 700 |
| `kit_wall` | alle vier | **1,460** | **1,460** | 124 | Reserve |

Ein Halbmaß von 1,460 bedeutet: der Sockel steht auf **beiden** Seiten 0,06 m in den Raum, ein
1,430 nur auf einer. Das deckt sich mit Abschnitt 4.2 Zeile für Zeile. `kit_wall_free` ist nicht
gebaut — es kommt in keinem der fünf Gebiete vor; `kit_wall` deckt den Fall ab, falls später
Layouts dazukommen.

### Decke, Rippe, Kranz

| ID | Maße glTF (x × y × z) | Pivot | Dreiecke | Budget |
|---|---|---|---:|---:|
| `kit_ceiling` | 2,80 × **0,36** × 2,80 | unten mittig | 44 | 160 |
| `kit_rib` | 0,32 × 0,32 × **2,80** | unten mittig | 28 | 48 |
| `kit_cornice` | **3,08** × 0,32 × 3,08 | unten mittig | 28 | 96 |

`kit_ceiling` ist ein **zweistufiges Kassettenfeld**: Randstreifen auf voller Dicke, dann zwei
Rücksprünge nach oben (2,20 m bei z = 0,08 und 1,70 m bei z = 0,18). Weil die Platte nicht
gedreht wird, ist sie an allen vier Kanten gleich hoch und stößt nahtlos an die Nachbarzelle.

`kit_rib` läuft entlang glTF **+Z**, wie das Halbmaß `(0.16, 0.16, cell * .5f)` es verlangt. Der
Querschnitt ist ein achtpunktiger Kiel, oben 0,32 breit, unten auf 0,08 zusammengezogen.

`kit_cornice` ist ein auskragendes Band: unten 2,80, ab z = 0,24 auf **3,08** ausgestellt, also
0,14 Überstand je Seite über die Zelle.

### Pfeiler und Bogen

| ID | Maße glTF | Dreiecke | Budget |
|---|---|---:|---:|
| `kit_pillar` | **1,257** × 6,14 × 1,257 | 140 | 900 |
| `kit_arch` | **8,06** × **7,07** × **0,72** | 116 | 2 400 |

`kit_pillar` ist achtkantig, mit Fuß (Umkreisradius 0,68), verjüngtem Schaft (0,44) und
Kapitell (0,68). Der Schaft ist über die Ecken 0,88 m dick und bleibt damit unter der
Kollisionsbox von 0,96 m aus `WorldView.java:335` — er schiebt nicht durch.

`kit_arch` ist als **Profilzug entlang der Öffnungskontur** gebaut, nicht als Keilsteinreihe: die
Innenkontur läuft senkrecht bis zum Kämpfer bei z = 3,40 und dann als Halbkreis mit R = 3,25; die
Außenkontur beginnt bei 4,03 und schrumpft zum Scheitel auf 3,67, damit der Bogen **genau** auf
7,07 m endet. Scheitelstärke 0,42 m, Kämpferstärke 0,78 m. Die Öffnung ist in glTF-Z
durchschreitbar, die beiden Kollisionskörper bei x = ±3,55 liegen in den Pfeilern.

### Deko

| ID | Maße glTF | Pivot | Dreiecke |
|---|---|---|---:|
| `kit_brazier` | 0,499 × **2,70** × 0,499 | unten mittig | 140 |
| `kit_banner` | 1,24 × **2,60** × 0,06 | unten mittig | 24 |
| `kit_carpet` | **1,00** × 0,018 × **1,00** | unten mittig | 20 |
| `kit_rock` | 2,00 × 2,00 × 2,00 | **Mitte** | 320 |

`kit_brazier` ist eine offene Schale auf schlankem Ständer. Der Innenradius liegt bei z = 2,51
noch bei 0,21 m — die geforderte Freizone 0,32 × 0,70 × 0,32 um y = 2,86 bleibt damit leer, die
prozedurale Flamme und das `PointLight` bei y = 3,05 stehen frei.

`kit_banner` ist eine Tuchbahn mit **Schwalbenschwanzsaum**, beidseitig geschlossen (0,06 m
Dicke), weil jMEs PBR-Material rückseitig cullt und eine Einzelfläche von hinten verschwunden
wäre.

`kit_carpet` ist die geforderte **1 × 1-m-Fliese**; `WorldView.java:294` streckt sie im Spiel auf
die Raumgröße.

`kit_rock` ist die **einzige Pivot-Ausnahme**: mittig in einem 2 × 2 × 2-Würfel, gemessen
`Center (0,0,0)`, Extent 1,0 in allen drei Achsen. Es ist eine Icosphere (Unterteilung 3) mit
zwei Oktaven deterministischem Rauschen, anschließend achsweise exakt auf ±1 eingepasst. Ohne
`DistanceLodControl` — ein Modul muss seine Dreieckszahl selbst niedrig halten, 320 statt der
600 aus dem Budget.

### Vier neue Materialien

Die Steinmodule (Wand, Decke, Rippe, Pfeiler, Bogen) tragen alle `PQK_Wall`, also den bereits
abgenommenen Wand-Texturensatz. Neu dazu:

| Material | Module | Albedo-Ziel (mittlere lineare Luminanz) | Rauheit | Metallizität |
|---|---|---:|---:|---:|
| `PQK_Iron` | `kit_cornice`, `kit_brazier` | **0,0337** nach `#45494e` | **0,620** | **0,350** |
| `PQK_Rock` | `kit_rock` | **0,0830** nach `#63736b` | 0,8115 | 0 |
| `PQK_Cloth` | `kit_banner` | **0,0285** nach `#392a32` | 0,899 | 0 |
| `PQK_Carpet` | `kit_carpet` | **0,0370** neutral | 0,956 | 0 |

Alle Zielwerte sind **nachgemessen getroffen** (Abweichung ≤ 0,0006). Die Herleitung:

* `kit_rock`: der Platzhalter ist `pbr("stone", 0x63736b, .96f, 0)`, also Steinalbedo × Farbton.
  0,5100 × 0,1628 = **0,0830**. Der Ton ist eingebacken, weil `WorldView` das Modul nicht tönt.
* `kit_banner`: Platzhalter `pbr("", 0x392a32, .9f, 0)` — ohne Map, also ist die Farbe selbst das
  Ziel: lineare Luminanz von `#392a32` = **0,0285**.
* `kit_carpet` **wird** getönt (`carpetTint`, `WorldView.java:257`). `tint()` normiert den Faktor
  auf Luminanz 1, also muss die Map die Luminanz des Zieltons tragen: 0,0407 für REFUGE, 0,0338
  für die übrigen, gewählt **0,0370** als Mitte.
* `PQK_Iron` folgt bewusst **`STYLE.md`, nicht dem Platzhalter** — genau wie in Abschnitt „Rauheit
  und Metallizität" empfohlen. Rauheit 0,62 statt 0,465, Metallizität 0,35 statt 0,313.
  Das Metall ist damit matter als der Bestand und liegt innerhalb `Metallic ≤ 0.4`,
  `Roughness ≥ 0.55`. Das Albedo-Ziel 0,0337 liegt 13 % über dem Platzhalterprodukt
  0,4427 × 0,0674 = 0,0298; das ist der Aufschlag für den Glanz, der laut `STYLE.md:43–45` in
  die Albedo gemalt gehört.

### Ein Exportfehler, der ohne Messung durchgegangen wäre

glTF erwartet Rauheit und Metallizität in **einer** Textur, Rauheit in G, Metallizität in B. Zwei
getrennte Bilder an `Roughness` und `Metallic` quittiert Blender mit *„More than one shader node
tex image used for a texture"* und schreibt nur eines davon. Gemerkt habe ich es an der Warnung,
bewiesen hat es die Engine-Probe. Behoben mit `art/textures/kit_iron_orm.png` (R = 1, G =
Rauheit, B = Metallizität) und einem `Separate Color`-Knoten. Nachher in der Engine:

```
kit_cornice  Metallic = 1.0  Roughness = 1.0  MetallicRoughnessMap 512x512 gesetzt
```

Skalar 1,0 × Map 0,620 / 0,350 ergibt die Zielwerte — der Skalar von 1,0 ist hier richtig, weil
die Werte aus dem Bild kommen sollen.

### Der Teppich war ein Brett

Erster Versuch: `kit_carpet_albedo` aus `wood-albedo.png` heruntergetönt. Im Spiel las sich der
Teppich als **rote Holzplanken** — die Brettfugen der Holztextur sind zu markant
(`target/screenshots/pentagon-11.png` vom Zwischenlauf). Ersetzt durch ein **prozedural
gewebtes** Muster: Kett- und Schussfäden mit Periode 8 px, Leinwandbindung, Fadenschattierung,
zwei Oktaven Fleckigkeit und eine eigene Normal-Map `kit_weave_normal.png` aus dem Höhenfeld.
Danach liest sich die Fläche als Gewebe. Dasselbe Gewebe trägt `kit_banner`.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| Engine-Probe, 15 Module | alle laden, Maße und Pivots wie in Abschnitt 4, `BaseColor` vorhanden | `cowork-logs/kitprobe-module-set.log`, `kitprobe-deko-set.log` |
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-kit-komplett.log` |
| Smoke-Run | alle **33 Stufen**, `smoke-ok.txt`, 13 Screenshots, keine Exception | `cowork-logs/smoke-kit-komplett.log` |
| Sichttest | Pfeiler mit Fuß und Kapitell, Bogen über dem Durchgang, Kohlebecken mit freier Flamme, Teppich als Gewebe | `target/screenshots/pentagon-6.png`, `-7.png` |

### Offen

* Jedes Modul trägt seine Texturen **eingebettet**. 15 Module × rund 0,66 MB sind etwa **10 MB
  mehrfach abgelegtes Identisches** im Repository und, schlimmer, 15 getrennte Texturobjekte im
  VRAM. `.gltf` mit externen Bild-URIs auf `textures/` würde beides lösen; `props/crystal.gltf`
  zeigt, dass jME das trägt. Bewusst zurückgestellt.
* Risiko R1 ist **gemessen und erledigt** — siehe Abschnitt 15.
* `kit_wall` und `kit_wall_free` sind **nie im Spiel gelaufen** — `wallKit` wählt sie nie. Sie
  sind gebaut und in der Engine geprüft, aber nicht am Layout erprobt.
* Die **Bodenfuge** bildet nach wie vor ein doppeltes Raster (Fase plus Texturkachel).
* Die **Vorzeichenkonvention der Normal-Map** der neuen Gewebe-Map ist konstruiert, nicht
  gerendert gegengeprüft.


---

## 15. Risiko R1 nachgemessen: `GeometryBatchFactory` (2026-09-12)

Die Sorge war, dass die Modulgeometrie die Zusammenfassung sprengt, weil `optimize` **nach
Material** gruppiert und jeder glTF-Import seine eigene `Material`-Instanz mitbringt. Gemessen
mit `BatchProbe` gegen die echten Moduldateien, Instanzzahlen aus Abschnitt 4 auf ein Zehntel
gerundet (`cowork-logs/batchprobe.log`):

```
[B] prototype kit_floor       geoms=1 materials=1 tris=122
[B] prototype kit_wall_face   geoms=1 materials=1 tris=106
[B] prototype kit_wall_corner geoms=1 materials=1 tris=108
[B] prototype kit_ceiling     geoms=1 materials=1 tris=44
[B] prototype kit_rib         geoms=1 materials=1 tris=28
[B] prototype kit_cornice     geoms=1 materials=1 tris=28
[B] second load shares material instance = false
[B] before optimize: geometries=193 materials=6 tris=16362
[B] after  optimize: geometries=6   materials=6 tris=16362 in 14 ms
```

**193 Geometrien fallen auf 6 zusammen** — genau eine je Modul-ID, und das in 14 ms. Kein
Dreieck geht verloren.

Der Grund steht im Code und ist damit belegt: `WorldView.module()` hält **einen** Prototypen je ID
im Cache und gibt `prototype.clone(false)` heraus. `clone(false)` klont das Material **nicht**,
also teilen alle Klone einer ID dieselbe Instanz. Der Kommentar an `module()` behauptet das; jetzt
ist es nachgemessen. Die Zeile `second load shares material instance = false` zeigt die Gegenprobe:
ein zweiter `loadModel` desselben Assets liefert eine **neue** Material-Instanz. Ohne den Cache in
`WorldView` wäre der Verdacht also berechtigt gewesen.

Vergleich zum Platzhalterpfad: `AssetPipeline.pbr()` merkt sich Materialien unter dem Schlüssel
`surface + tint + roughness + metallic` (`AssetPipeline.java:32–34`). Ein Chunk kam damit auf
**drei** Materialien — Boden (`stone`, 0,86), Wand/Decke/Rippe (`stone`, 0,94) und Kranz (`iron`).
Mit Modulen sind es **sechs**, weil jede Datei ihr eigenes Material bringt. Also eine
**Verdopplung der Batches je Chunk**, nicht die befürchtete Verfünfzehnfachung.

Wer die sechs auf drei drücken will, braucht den `.gltf`-Weg mit externen Bild-URIs **und**
zusätzlich eine Materialzusammenführung in `WorldView` — die Texturen allein reichen nicht, weil
`Material`-Identität zählt, nicht Textur-Identität. Das ist eine bewusste Entscheidung für später;
bei sechs Batches je Chunk lohnt es nicht.

---

## Nachtrag 13. September 2026: `kit_rock` neu

`kit_rock.glb` war eine ziegelgemusterte Kuppel (dieselbe Albedo wie die Wand). `art/blender/rock_builder.py` baut jetzt einen Ikosaeder-Findling (320 Flächen, geglättetes Zufallsrelief, Albedo `art/textures/kit_rock_mottled_albedo.png` ohne Ziegel). Rahmen unverändert: Einheitsradius um den Mittelpunkt, `WorldView.rock()` skaliert mit (s, 1,6 s, s) und setzt den Kollisionsquader wie zuvor. Der alte Stand liegt unter `art/gen/pre-polish/props/kit_rock.glb`.

## Nachtrag 13. September 2026: Höhlenmodule

Die CAVERNS bekommen Fels statt Ziegel — neun Module aus `art/blender/cave_builder.py`, die
`WorldView.buildTiles` vor dem Kit versucht (Rippe und Kranz entfallen in der Höhle):

| ID | Maße (B × H × T) | Dreiecke | Bemerkung |
|---|---|---:|---|
| `kit_floor_cave`, `kit_floor_cave2` | 2,80 × 0,27 × 2,80 | 246 | Relief oben, höchstens +3 cm über der Physikebene, bis −9 cm |
| `kit_ceiling_cave`, `kit_ceiling_cave2` | 2,80 × 0,79 / 1,06 × 2,80 | 342 | Platte 0,36 m, Stalaktiten hängen bis 0,7 m darunter |
| `kit_wall_cave_face`, `kit_wall_cave_face2` | 2,80 × 7,12 × ≤ 3,08 | 244 | Sichtseite +Z wie `kit_wall_face`, Relief −0,2 … +0,36 m |
| `kit_wall_cave_corner` | ≤ 3,09 × 7,12 × 2,95 | 388 | +Z und +X |
| `kit_wall_cave_span` | 2,80 × 7,12 × 3,38 | 388 | +Z und −Z |
| `kit_wall_cave_pier` | 3,02 × 7,12 × 3,27 | 532 | +Z, +X, −Z |

Vertrag wie in Abschnitt 4 (Pivot unten mittig, ganze Zelle, Drehung nach `wallKit`). Das Relief
ist deterministisches Wertrauschen, das an den Zellkanten auf null ausläuft — Nachbarn stoßen
ohne Riss aneinander, dafür wiederholt sich das Muster je Zelle; die zweite Variante jedes Typs
(Zellparität `(x + z) % 2`) verschiebt ihr Texturfenster. An der Bodenlinie gibt es keine
Vertiefung, weil dort die Bodenplatte endet. Material `PQC_Rock` mit `kit_cave_albedo.png`
(1024², mittlere lineare Leuchtdichte **0,089**, im Wandband aus Abschnitt 6; die gefleckte
Findlingsalbedo lag bei 0,118 und las sich unter den Fackeln als Sand). Belege `cave_builder.log`,
`roomshots-caves2.log`, Bilder `target/probe-shots/room-caverns-*.png`.
