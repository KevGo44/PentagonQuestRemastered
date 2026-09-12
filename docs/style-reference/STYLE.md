# Stilanker — Pentagon: Das Aschensiegel

Festgelegt am 2026-09-11. **Richtung: düsterer Dark-Fantasy-Realismus**, mit den unten
begründeten Abstrichen. Dieses Dokument ist die Grundlage für jedes Meshy-Prompt und jede
Blender-Sitzung in Phase 3 bis 5. Abweichungen bitte hier eintragen, nicht stillschweigend.

Alle Zahlen und Farbwerte sind aus dem Code gelesen, nicht geschätzt. Die Quelle steht jeweils
dabei, damit man sie nachprüfen kann.

---

## 1. Die Abstriche — warum Realismus hier nicht voll durchgezogen wird

Der Wunsch ist Souls-artige Düsternis. Fünf Stellen im Projekt setzen dem harte Grenzen. Sie
lassen sich nicht durch bessere Assets umgehen, nur durch bewusste Gestaltung.

### 1.1 Kein Gewicht in den Angriffen — `combat/AttackTimeline.java`

```
WINDUP   = {0,15 s, 0,19 s, 0,28 s}
DURATION = {0,52 s, 0,58 s, 0,78 s}
```

Ein schwerer Souls-Hieb holt 0,6 bis 1,2 s aus. Hier liegt der Treffer nach **0,15 s**. Eine
realistisch träge Animation ist in diesem Fenster physisch nicht darstellbar — sie würde
treffen, bevor die Waffe oben ist.

**Abstrich:** Animationen werden **schnell und trocken**, keine ausholenden Schwünge. Das
Gewicht muss aus **Masse und Material** kommen (breite Schultern, dicke Platten, tiefe Töne),
nicht aus der Bewegungsdauer. Die Werte sind auf funktionierenden Fortschritt abgestimmt und
werden nicht angefasst.

### 1.2 Metall wirkt tot — `assets/EnvironmentLighting.java`

Die Umgebungssonde ist ein **1×1-Pixel-Cubemap** mit konstant `RGB(18,24,32)`, SH-Grundton
`(.16, .21, .27)`. PBR-Metall lebt von Spiegelungen; hier gibt es nichts zu spiegeln.

Siehe `material-test.png` in diesem Ordner: bei `Metallic 1.0` fallen die Kugeln fast ins
Schwarz und behalten nur einen Fackelpunkt. Erst `Metallic 0.0` mit `Roughness 0.35–0.55`
liest sich als Stein und Leder.

**Abstrich:** **Kein poliertes Metall.** Rüstungen als stumpfes, angelaufenes Eisen
(`Metallic ≤ 0.4`, `Roughness ≥ 0.55`). Glanzlichter und Kantenabrieb werden **in die
Albedo-Textur gemalt**, nicht dem Shader überlassen. Politur erst, wenn pro Abschnitt echte
Indoor-Probes gebacken sind — das steht in `ASSETS.md` als offener Punkt.

### 1.3 Keine Gesichter, keine Finger — 16-Joint-Vertrag

```
Root → Hips → Spine → Head
                    → UpperArm.L/R → Forearm.L/R → Hand.L/R
       Hips → Thigh.L/R → Shin.L/R → Foot.L/R
```

Ein Joint pro Hand, keiner im Gesicht. Realistische Mimik und Fingerhaltung sind nicht
animierbar — ein detailliertes Gesicht bliebe eine starre Maske, und offene Hände würden beim
Greifen der Waffe brechen.

**Abstrich, und zwar ein glücklicher:** **Kapuzen, Helme, Masken, Panzerhandschuhe,
Handwickel.** Das ist Dark-Fantasy-Kanon und versteckt genau das, was das Rig nicht kann.
Kein Charakter zeigt ein volles Gesicht; wo ein Gesicht sichtbar sein soll (Mira, Eren), liegt
es im Schatten einer Kapuze und trägt keine animierte Mimik.

**Nachtrag vom 2026-09-11, Korrektur:** Die 16 Joints sind der Vertrag der *Platzhalter*, nicht
eine Prüfung der Engine. `CharacterFactory.create` prüft ausschließlich `AnimComposer`,
`SkinningControl` und die elf Clipnamen — **Joint-Anzahl und Joint-Namen werden zur Laufzeit
nicht geprüft.** Ein Rig mit mehr Joints lädt also. Geprüft am Quaternius-Basisrig: 65 Joints
inklusive vollständiger Finger (`index_01_l` … `thumb_03_r`).

Daraus folgt: Finger sind kein Ausschlusskriterium, sie werden von den elf Clips lediglich nicht
bewegt. Verbindlich bleibt das Umbenennen von `Hand.R`/`Hand.L`, weil `WeaponSocket` und
`ShieldSocket` daran hängen und `ASSETS.md` die Namen festschreibt. Die Gestaltungsempfehlung
oben gilt unverändert — Kapuzen und Helme bleiben richtig, aber aus Stilgründen, nicht aus
technischem Zwang.

### 1.4 Silhouette schlägt Oberfläche — Nebel + 14 Goblins

`world/AtmosphereFilter.java` integriert Höhendichte in zwölf Schritten bis zur Oberfläche,
dazu SSAO, Bloom und Vignette. `DungeonLayout` setzt **14 Goblins**, 7 Orks, 5 Aschenrufer.
Auf Distanz frisst der Nebel jede Oberflächendetaillierung; was bleibt, ist der Umriss.

**Abstrich:** **Lesbare Silhouette vor Hautporen.** Jeder Gegnertyp braucht eine auf 20 m
eindeutige Kontur — Goblin schmal und gebückt, Ork breit mit hängenden Armen, Wächter
kastenförmig und aufrecht, Aschenrufer mit hoher Kapuzenspitze und wehendem Saum. Wenn zwei
Typen als Schattenriss verwechselbar sind, ist der Entwurf gescheitert.

### 1.5 Der König ist kein vergrößerter Mensch — `ai/EnemyType.java`

Die Skalierung wird im Spiel auf **ein** Basismodell von 1,95 m angewendet:

| Typ | Faktor | Ergebnis |
|---|---:|---:|
| Goblin | 0,80 | 1,56 m |
| Aschenrufer | 1,00 | 1,95 m |
| Ork-Brecher | 1,15 | 2,24 m |
| Eiserner Wächter | 1,20 | 2,34 m |
| **Ork-König** | **1,85** | **3,61 m** |

Siehe `massstab.png`. Gleichmäßig hochskalierte Menschenproportionen lesen sich bei 3,6 m als
Riesenpuppe, nicht als Boss — der Kopf wirkt zu groß, die Gliedmaßen zu dünn.

**Abstrich:** Der König wird **in eigenen Proportionen modelliert** (kleinerer Kopf im
Verhältnis, massivere Schultern, kürzere Unterschenkel), so dass er **nach** der Multiplikation
mit 1,85 richtig sitzt. Gleiches milder für Goblin: gebückte Haltung statt nur 80 % Mensch.
Geliefert wird trotzdem **in 1,95-m-Standardgröße** — das Spiel skaliert selbst.

---

## 2. Palette

Alle Werte aus dem Code. Diese Farben sind die Verdrahtung, nicht ein Vorschlag.

### Charaktertönungen — `EnemyType.color`, `PlayerController`, `WorldView`

| Asset | Hex | Quelle |
|---|---|---|
| Held | `#547079` | `PlayerController` |
| Goblin | `#5b805c` | `EnemyType.GOBLIN` |
| Ork-Brecher | `#667758` | `EnemyType.ORC` |
| Eiserner Wächter | `#87939c` | `EnemyType.WARDEN` |
| Aschenrufer | `#79608f` | `EnemyType.SHAMAN` |
| Ork-König | `#8a5d4e` | `EnemyType.KING` |
| Mira (NPC) | `#50747b` | `WorldView` Kind.NPC |
| Eren (Gefangener) | `#806e56` | `WorldView` Kind.PRISONER |

### Gebietston und Nebel — `world/Region.java`

| Abschnitt | Stein | Nebel |
|---|---|---|
| I Die letzte Zuflucht | `#6d7e88` | `#14222e` |
| II Krypta der Eide | `#667886` | `#14252d` |
| III Die gläserne Tiefe | `#5b716e` | `#102a2b` |
| IV Das Kettenverlies | `#817060` | `#241919` |
| V Der Aschenthron | `#6b636e` | `#221929` |

### Licht und Akzente

| Element | Hex | Quelle |
|---|---|---|
| Fackellicht | `#ffad5b` | `WorldView.update`, zwei Flackerfrequenzen |
| Kristall / Magie | `#55cfc5`, `#85ccdc` | `WorldView.crystal` |
| Lore-Kristall | `#d7b675` | `WorldView` Kind.LORE |
| Projektil | `#85e8ea` | `ProjectileSystem` |
| Trefferfunken | `#e5a764` | `Effects` |
| Angriffsmarkierung | `#e47b4e` | `Enemy.telegraph` |
| Thronglut | `#e28b51` | `WorldView` Kind.THRONE |

**Lesart:** entsättigtes Blaugrün als Grundton, **warmes Orange nur als Lichtquelle**, Cyan
ausschließlich für Magie und Kristall. Ein Charakter-Albedo darf kein gesättigtes Cyan oder
Orange enthalten — diese Farben sind für Leuchtendes reserviert, sonst bricht die Lesbarkeit
im Kampf.

---

## 3. Technische Vorgaben für die Generierung

Für Meshy (Stand 2026-09, siehe Notizen in `docs/asset-liste.md`):

| Parameter | Wert | Begründung |
|---|---|---|
| `enable_pbr` | `true` | Die Engine erwartet Albedo + Normal + Roughness + Metallic (`AssetPipeline.pbr`) |
| `target_polycount` | Charaktere 18 000–20 000, Props 4 000–8 000 | Voreinstellung 30 000 ist für 31 Gegner zu viel; `DistanceLodControl` greift nur bei codegenerierten Felsen, nicht bei importierten GLBs |
| `topology` | `triangle` | Quad-Netze bringen hier keinen Vorteil, es wird nicht weiter modelliert |
| `target_formats` | `glb` | Zielformat der Fallback-Kette; FBX nur als Mixamo-Zwischenschritt |
| `should_texture` | `true` | sonst fehlt die Albedo |

Geometrie: metrisch, 1 Einheit = 1 m, **+Y oben, +Z vorwärts**, Pivot am Boden zwischen den
Füßen, Rotation angewendet, Skalierung `(1,1,1)`, keine Root Motion. Props: Pivot unten mittig.

---

## 4. Dateien in diesem Ordner

| Datei | Was sie festlegt |
|---|---|
| `STYLE.md` | dieses Dokument, verbindlich |
| `massstab.png` | Maßstabsvergleich der sechs Figurengrößen mit 1,95-m-Vertragslinie; in Blender gerendert |
| `material-test.png` | Roughness × Metallic unter den echten Lichtwerten des Spiels; zeigt, warum poliertes Metall ausfällt |
| `ingame-*.png` | unbearbeitete Aufnahmen aus dem laufenden Spiel als Palette- und Maßstabsanker |
| `kandidat-quaternius-basis.png` | Quaternius-Basismesh (Superhero, 1,81 m, 14 318 Dreiecke) unter Dungeon-Licht |
| `kandidat-quaternius-ranger-original.png` | Ranger-Outfit wie ausgeliefert — zu hell und zu gesättigt für die Palette |
| `kandidat-quaternius-ranger-palette.png` | dasselbe Outfit nach Umstimmung auf die Palette; Beleg, dass die Geometrie den Look trägt |

Die In-Engine-Aufnahmen stammen aus dem Smoke-Run vom 2026-09-11 und zeigen die
**Platzhalter**. Sie sind die Referenz für Farbe, Licht und Proportion — **nicht** für Form.

---

## 5. Offen

Meshys Image-to-3D braucht pro Asset ein **Eingangsbild**. Ein solches Konzeptbild existiert
noch nicht, und die In-Engine-Aufnahmen taugen nicht dafür: wer einen Kastenmann hineingibt,
bekommt einen Kastenmann heraus. Bevor Phase 3 startet, muss entschieden sein, woher die
Eingangsbilder kommen. Stand der Entscheidung: **offen.**
