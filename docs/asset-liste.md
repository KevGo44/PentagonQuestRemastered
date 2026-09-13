# Asset-Liste

Abgeleitet aus dem Java-Code (Stand: Analyse vom 2026-09-11), nicht aus Wunschdenken.
Maßgeblich sind [COWORK-BRIEFING.md](COWORK-BRIEFING.md) und [ASSETS.md](ASSETS.md).

Grundlage der Ableitung:

* `de.pentagon.assets.AssetPipeline.model(id, fallback)` — die **einzige** Auflösungsstelle.
  Sucht `models/<id>` in der Reihenfolge `.glb` → `.gltf` → `.j3o` → `.obj`.
* `de.pentagon.assets.CharacterFactory.create(id, …)` — lädt `characters/<id>`, Aufrufer:
  `PlayerController` (`hero`), `Enemy` (`EnemyType.name().toLowerCase()`),
  `WorldView` für `Kind.NPC` / `Kind.PRISONER` (`ObjectSpec.value()`).
* `de.pentagon.world.WorldView.buildObject(…)` — lädt `props/<Kind.name().toLowerCase()>`,
  **außer** für `NPC`, `PRISONER` und `TRAP`.
* `de.pentagon.ai.EnemyType` — fünf Gegnertypen mit Skalierung und Tönung.
* `de.pentagon.world.DungeonLayout` — Vorkommen/Anzahl pro Typ.

Damit ist die Liste **abgeschlossen**: 8 Charakter-IDs und 8 Prop-IDs. Alles andere hat
derzeit keinen Ladepfad im Spiel (siehe Abschnitt „Nicht ladbar").

## Statusvokabular

`offen` → `generiert` (Mesh liegt vor) → `gerigged` (Armature + 11 Clips, Namen korrekt)
→ `exportiert` (GLB im Klassenpfad) → `integriert: ja` (Build grün + Sichttest).
Solange nicht `integriert: ja`, gilt `integriert: nein`.

---

## 1. Hero-Assets — Spielercharakter, NPCs, Boss

Alle unter `src/main/resources/models/characters/<id>.glb`. Standardhöhe 1,95 m, Pivot am
Boden, Skalierung (1,1,1), keine Root Motion. Pflichtclips (case-sensitive):
**Idle, Walk, Run, Attack1, Attack2, Attack3, Dodge, Block, Hit, Death, Cast**.

| Name | Kategorie | Priorität | Quelle | Status |
|---|---|---|---|---|
| `characters/hero.glb` — Spielercharakter, Ödland-Wanderer | Charakter (Held) | 1 | **Quaternius CC0** (Superhero-Basis + Ranger-Outfit) → Blender → Mixamo | **integriert: ja** (2026-09-12) |
| `characters/king.glb` — Ork-König, dreiphasiger Endboss (Ingame ×1,85, Kronen-Geometrie im Platzhalter) | Charakter (Boss) | 1 | **Meshy 6 Lite** → Blender (Arm freigeschnitten, Eren-Rig) | **integriert: ja** (2026-09-12) |
| `characters/mira.glb` — Mira, letzte Kartografin (NPC, Zuflucht, Quest-Geberin) | Charakter (NPC) | 1 | **Quaternius CC0** (Ranger-Basis, Mira-Tönung) → Blender → Mixamo | **integriert: ja** (2026-09-12) |
| `characters/eren.glb` — Eren, der Gefangene (NPC, Kettenverlies) | Charakter (NPC) | 1 | **Quaternius CC0** (Peasant-Outfit, Eren-Tönung) → Blender → Mixamo | **integriert: ja** (2026-09-12) |

Anmerkung: Auch NPCs brauchen **alle elf** Clips. `CharacterFactory` prüft ohne Ausnahme
und wirft `IllegalStateException` — ein NPC mit nur `Idle` verhindert den Spielstart.

## 2. Standard-Gegner

Alle unter `src/main/resources/models/characters/<id>.glb`, gleicher Vertrag wie oben.
**Unskaliert in Standardgröße liefern** — die Faktoren setzt `EnemyType`.

| Name | Kategorie | Priorität | Quelle | Status |
|---|---|---|---|---|
| `characters/goblin.glb` — Goblin, ×0,8, 14 Spawns (häufigster Gegner) | Gegner | 2 | **Meshy 6 Lite** → Blender (Rig aus Eren umgezielt) | **integriert: ja** (2026-09-12) |
| `characters/orc.glb` — Ork-Brecher, ×1,15, 7 Spawns | Gegner | 2 | **Meshy 6 Lite** → Blender (Eren-Rig unverändert, Finger entfernt) | **integriert: ja** (2026-09-12) |
| `characters/shaman.glb` — Aschenrufer, ×1,0, 5 Spawns, Fernkampf (nutzt `Cast`) | Gegner | 2 | **Meshy 6 Lite** → Blender (Arm freigeschnitten, Eren-Rig) | **integriert: ja** (2026-09-12) |
| `characters/warden.glb` — Eiserner Wächter, ×1,2, 4 Spawns, Miniboss | Gegner | 2 | **Meshy 6 Lite** → Blender (Arm freigeschnitten, Eren-Rig) | **integriert: ja** (2026-09-12) |

## 3. Props / Deko

Alle unter `src/main/resources/models/props/<id>.glb`. Pivot unten mittig, statisch, kein
Rig. Fallback ist `null` — fehlt ein Prop, bleibt die prozedurale Geometrie stehen, ohne
Fehler. Deshalb risikoarm und gut in Blender direkt baubar.

| Name | Kategorie | Priorität | Quelle | Status |
|---|---|---|---|---|
| `props/crystal.glb` — Kristallader, 1,4×2,4×1,4 m, 3 Vorkommen | Prop | 3 | **bereits vorhanden** als `crystal.gltf` + `.bin` | integriert: ja |
| `props/chest.glb` — Truhe, geliefert **1,34×0,84×0,92 m**, 72 Dreiecke, 5 Vorkommen | Prop | 3 | **Meshy-frei, parametrisch in Blender** | **integriert: ja** (2026-09-12) |
| `props/shrine.glb` — Schrein/Rastpunkt, geliefert **1,40×0,52×1,40 m** (Kristall bleibt prozedural), 48 Dreiecke, 5 Vorkommen | Prop | 3 | **Meshy-frei, parametrisch in Blender** | **integriert: ja** (2026-09-12) |
| `props/portal.glb` — Gebietstor, geliefert **3,90×5,25×0,96 m**, 72 Dreiecke, 8 Vorkommen | Prop | 3 | **Meshy-frei, parametrisch in Blender** | **integriert: ja** (2026-09-12) |
| `props/lore.glb` — Chronik-Altar, geliefert **1,30×1,20×0,90 m**, 36 Dreiecke, 5 Vorkommen | Prop | 3 | **Meshy-frei, parametrisch in Blender** | **integriert: ja** (2026-09-12) |
| `props/rune.glb` — Runenaltar, geliefert **1,30×1,20×0,90 m**, 36 Dreiecke, 3 Vorkommen | Prop | 3 | **Meshy-frei, parametrisch in Blender** | **integriert: ja** (2026-09-12) |
| `props/seal.glb` — Siegelaltar, geliefert **1,30×1,20×0,90 m**, 36 Dreiecke, 2 Vorkommen | Prop | 3 | **Meshy-frei, parametrisch in Blender** | **integriert: ja** (2026-09-12) |
| `props/throne.glb` — Aschenthron, geliefert **2,60×5,10×2,30 m**, 82 Dreiecke, 1 Vorkommen (Bossraum) | Prop | 3 | **Meshy-frei, parametrisch in Blender** | **integriert: ja** (2026-09-12) |

`crystal` ist der einzige Eintrag mit echtem Asset. Ein `crystal.glb` würde das vorhandene
`crystal.gltf` **überschreiben** (GLB gewinnt in der Suchreihenfolge) — nur ersetzen, wenn
es bewusst gewollt ist; `AssetTest` lädt das aktuelle Asset.

## 4. Nicht ladbar — Code fehlt, keine Generierung sinnvoll

Diese Dinge existieren im Spiel, haben aber **keinen Aufruf von `AssetPipeline.model`**.
Eine generierte Datei würde hier nie geladen. Sie stehen hier, damit klar ist, dass sie
bewusst nicht Teil der Generierung sind — nicht, weil sie übersehen wurden.

| Name | Kategorie | Priorität | Quelle | Status |
|---|---|---|---|---|
| Fallenplatte + Stacheln (`Kind.TRAP`, 6 Vorkommen) | Prop | 4 | prozedural in `WorldView`; von der Prop-Ladeschleife ausgenommen | kein Ladepfad |
| Modulares Dungeon-Kit: Wände, Böden, Gewölbedecken, Rippen, Kranz, Säulen, Bögen, Kohlebecken, Banner, Teppich, Felsen | Umgebung | 4 | **15 Module unter `props/kit_*`**, parametrisch in Blender; Rechenregel und Verifikation in `docs/dungeon-kit.md` | **integriert: ja** (2026-09-12) |
| Waffenmodelle (`rust_sword`, `short_sword`, `long_sword`, `ember_blade`, `oath_blade`) | Waffe | 4 | Schwert ist fest im Rig am `WeaponSocket`; `ItemCatalog` kennt nur Werte, kein Mesh | kein Ladepfad |
| Rüstungsmodelle (`cloth`, `leather`, `chain`, `warden_armor`) | Ausrüstung | 4 | rein statistisch, keine Sichtbarkeit im Code | kein Ladepfad |
| Verbrauchsgüter/Schlüssel/Relikt (Tränke, Siegel, Krone) | Item | 4 | nur HUD-Text, keine Weltdarstellung | kein Ladepfad |
| Projektil-Orb, Trefferfunken, Angriffs-Telegraph | VFX | 4 | `ProjectileSystem` / `Effects` / `Enemy`, Geometrie im Code | kein Ladepfad |

---

## Animationsbedarf (gilt für alle 8 Charaktere)

| Clip | Zweck | Timing-Vorgabe |
|---|---|---|
| `Idle` | Ruhepose | Loop, Ende = Anfang |
| `Walk` | Gehen | Loop, **in-place** |
| `Run` | Laufen | Loop, **in-place** |
| `Attack1` | Kombo 1 | 0,52 s, Treffer ab 0,15 s |
| `Attack2` | Kombo 2 | 0,58 s, Treffer ab 0,19 s |
| `Attack3` | Kombo 3 | 0,78 s, Treffer ab 0,28 s |
| `Dodge` | Ausweichrolle | einmalig |
| `Block` | Blocken/Parieren | Halteposition |
| `Hit` | Trefferreaktion | von der Simulation beendet |
| `Death` | Tod | von der Simulation beendet |
| `Cast` | Magie (Held + Aschenrufer) | einmalig |

8 Charaktere × 11 Clips = **88 Clips**. Mixamo liefert `Block` und `Cast` nicht unter
diesen Namen — passende Ersatzclips wählen (z. B. „Standing Block Idle", „Standing 2H
Magic Attack 01") und **in Blender umbenennen**. Ebenso `Attack1..3` aus Sword-Slash-
Varianten. Ohne exakte Namen startet das Spiel nicht.

---

## Abweichungen von den Phasen-Vorgaben — bitte lesen

Drei Punkte in der Phasenplanung widersprechen dem Briefing. Ich richte mich nach dem
Briefing, will das aber offenlegen statt still zu korrigieren:

1. **`/assets/models` und `/assets/animations` gibt es nicht.**
   Abschnitt 1 des Briefings: Dateien im Projektwurzel-`/assets` „werden nie geladen".
   Ziel ist der Klassenpfad: `src/main/resources/models/characters|props/`. Für rohe
   Zwischenstände (FBX von Mixamo, Blender-Dateien) schlage ich `art/` vor — so nutzt es
   `ASSETS.md` bereits in den Blender-Beispielen.

2. **Phase 6 „AssetManager-Ladecode ergänzen" entfällt.**
   Abschnitt 6 des Briefings sagt das ausdrücklich: es gibt nichts zu verdrahten. Der
   Dateiname *ist* die Verdrahtung. Phase 6 wird damit zu: neu bauen, `mvn verify` grün
   halten, `AssetTest` um Zusicherungen für die neuen Assets ergänzen, Sichttest, Status
   hier auf `integriert: ja` setzen.

3. **Mixamo-Rig ≠ Skelettvertrag.**
   Das Spiel erwartet 16 Joints mit festen Namen (`Hips`, `Spine`, `Head`, `UpperArm.L`,
   … `Foot.R`) plus `WeaponSocket`/`ShieldSocket`. Mixamo liefert ~65 Bones als
   `mixamorig:Hips` usw. `CharacterFactory` prüft zur Laufzeit zwar nur Composer,
   SkinningControl und Clipnamen — aber die Sockets hängen an `Hand.R`/`Hand.L`, und
   `docs/ASSETS.md` schreibt die Namen fest. Zwischen Mixamo und Ablage liegt also ein
   **zwingender Blender-Schritt**: Bones umbenennen/reduzieren, Clips umbenennen,
   `tools/blender_export.py --character` laufen lassen (das Skript prüft Bone- und
   Clipnamen bereits).

## Credit-Rechnung Meshy (Budget 200)

**Korrektur vom 2026-09-11.** Die erste Schätzung in diesem Dokument ging von ~10 Credits pro
Generierung aus. Das war falsch. Laut Meshys Preisdokumentation:

| Vorgang | Credits |
|---|---:|
| Image-to-3D (Meshy 7) | 25 |
| Image-to-3D / Text-to-3D (Meshy 6) | 20 |
| AI-Texturierung (nachträglich) | 10 |
| Remesh | 0 |
| Auto-Rigging | 0 |

Damit gilt:

* Die ursprüngliche Phasenvorgabe (2–3 Varianten je Hero-Asset, 2 je Gegner) sind 20
  Generierungen → **400 bis 500 Credits**. Zwei- bis zweieinhalbfach über Budget.
* Alle 8 Charaktere je **einmal**, ohne Varianten: 200 Credits auf Meshy 7 (punktgenau, keine
  Reserve) bzw. **160 auf Meshy 6** mit 40 Credits für zwei Fehlversuche.

**Entschieden (2026-09-11):** Alle 8, keine Varianten, Meshy 6. Preis wird vor der ersten
Generierung an der Oberfläche gegengeprüft, nicht nur aus der Doku übernommen.

**Überholt (2026-09-12):** Held, Mira und Eren sind aus **Quaternius-CC0-Teilen** gebaut, nicht
generiert. Dafür wurde **kein einziger Credit** verbraucht. Die 100 Meshy- und 200 Tripo-Credits
stehen damit vollständig für die fünf nicht-menschlichen Gegner (`goblin`, `orc`, `warden`,
`shaman`, `king`) zur Verfügung, für die Quaternius keine Basis liefert.

Nebenbefund: **Auto-Rigging kostet 0 Credits.** Mixamo ist damit optional statt zwingend — der
Blender-Schritt zum Umbenennen der Bones und Clips bleibt in jedem Fall nötig.

Lizenzhinweis: Auf dem kostenlosen Plan (100 Credits/Monat) stehen Ausgaben unter **CC BY 4.0**,
kommerzielle Nutzung also nur mit Namensnennung. Bezahlte Pläne geben volle kommerzielle Rechte.

## Offen für Phase 2

Stilanker fehlt noch. Bevor generiert wird, brauchen wir Referenzbilder in
`docs/style-reference/`. Die Farbwerte aus dem Code sind ein guter Ausgangspunkt:
Held `#547079`, Goblin `#5b805c`, Ork `#667758`, Wächter `#87939c`, Aschenrufer `#79608f`,
König `#8a5d4e`, Mira `#50747b`, Eren `#806e56`; Gebietstöne von `#6d7e88` (Zuflucht) bis
`#6b636e` (Aschenthron).


---

## Held integriert — Protokoll und Belege (2026-09-12)

`src/main/resources/models/characters/hero.glb`, 6,27 MB, 13 Texturen à 512 px,
elf Clips, 66 Joints (Obermenge der 16 Vertrags-Joints), Pivot am Boden, 1,95 m.
**Ohne einen einzigen Generierungs-Credit.**

### Herkunft

| Teil | Quelle | Lizenz |
|---|---|---|
| Kopf | Quaternius *Universal Base Characters*, Superhero-Basis | CC0 |
| Körper, Kapuze, Stiefel, Gürtel | Quaternius *Modular Character Outfits – Fantasy*, Male Ranger | CC0 |
| Rig | Mixamo Auto-Rigger, Standard-Skelett 65 Bones | Adobe, lizenzfrei nutzbar |
| Animationen | Mixamo *Sword And Shield Pack* (49 Clips) + *Stand To Roll* | Adobe, lizenzfrei nutzbar |

Rohdaten unter `art/mixamo/hero/` und `art/textures/`.

### Aufbereitung in Blender

* Basiskörper unterhalb 1,50 m entfernt — er lag unsichtbar unter dem Outfit
  (41 300 → 31 872 Dreiecke). Drei ungenutzte UV-Ebenen aus dem Join entfernt.
* Palette **in die Albedo-Texturen gerechnet** (Sättigung ×0,32, Helligkeit ×0,55,
  45 % Farbmischung zum Heldenton `#547079`), weil glTF keine Shader-Nodes transportiert.
  Roughness des Ranger-Outfits aus dem Grünkanal der ORM-Map gelöst.
* 65 Bones auf die Vertragsnamen umbenannt, `Root` über `Hips` ergänzt,
  6435 Kanalpfade nachgezogen.
* Wurzelbewegung entfernt, wo sie auftrat: Walk 1,80 m, Run 3,16 m, Attack2 und Death.
  Bullet besitzt Position und Richtung.
* Alle Clips beginnen bei t = 0.

### Angriffszeiten gegen `combat/AttackTimeline.java`

Treffermoment über die Spitzengeschwindigkeit der rechten Hand gemessen, vorne Vorbereitung
abgeschnitten und dann skaliert, damit der Treffer auf dem verlangten Bruchteil landet.

| Clip | Quelle | Soll Länge / Treffer | Ist Länge / Treffer |
|---|---|---|---|
| `Attack1` | *sword and shield attack (4)* | 0,52 s / 0,15 s | 0,53 s / 0,156 s |
| `Attack2` | *sword and shield attack (2)* | 0,58 s / 0,19 s | 0,57 s / 0,183 s |
| `Attack3` | *sword and shield slash (5)* | 0,78 s / 0,28 s | 0,77 s / 0,277 s |

### Verifikation

* `mvn verify`: **BUILD SUCCESS, 50 Tests**, 0 Fehler.
* Smoke-Run: **alle 32 Stufen**, `target/smoke-ok.txt`, 13 Aufnahmen,
  **null** nicht zuordenbare Buffer-Views.
* Sichttest: Held animiert im laufenden Spiel. Belegt mit einem eigenen AppState, der genau
  dann auslöst, wenn Composer-Geschwindigkeit 1 und Zeit > 0,8 s ist:
  `Forearm.R = (0.080, 0.006, -0.662, 0.745)` gegen die Bindepose
  `(-0.035, 0.0005, -0.015, 0.999)`. Aufnahmen unter `art/probe/live/`.

### Gelernt: Geschwindigkeit 0 zeigt die Bindepose

`AnimComposer.setGlobalSpeed(0)` hält die Animation nicht in ihrer letzten Pose an — jME
interpoliert dann gar nicht und stellt die **Bindepose** dar. Nachgewiesen mit einem
Render-Prüfer: identische Szene, einmal mit Pause bei Zeit 0 (Bindepose), einmal ohne
(korrekte Idle-Pose), einmal mit Pause und anschließendem Fortsetzen (korrekt).

Beim Platzhalter fällt das nicht auf, weil seine **Rest-Pose die Arme unten hat**
(`UpperArm.L` bei (-0,42/0,15/0), `Forearm.L` bei (0/-0,35/0)). Ein Mixamo-Rig hat eine
T-Pose als Rest-Pose und macht den Effekt sichtbar.

**Für den Helden ist das folgenlos.** Betroffen sind nur Frames unmittelbar nach einem
`loadRegion` — dort erzeugt das Spiel ein neues Rig mit Zeit 0, und `takeScreenshot()` nimmt
erst am Frame-Ende auf, also nach dem Neuaufbau. Genau das zeigen `pentagon-7` und
`pentagon-8`. Im Spielverlauf ist der Zustand nach einem Frame vorbei, und in Menüs verdecken
die Overlays die Figur; im Hauptmenü liegt sie außerhalb des Bildes.

### Offener Punkt für die Gegner-Assets

`ai/EnemyBrain.java:32` friert Leichen ein:

```java
if (e.deathTime > 1.2f) e.rig.composer().setGlobalSpeed(0);
```

`Enemy.cleanup` läuft erst beim Gebietswechsel, Leichen bleiben also liegen. Nach dem
oben belegten Mechanismus bedeutet das: **eine Leiche springt nach 1,2 s aus der Todespose
in ihre Rest-Pose.** Beim Platzhalter ist das ein aufrecht stehender Toter, bei einem
Mixamo-gerigten Gegner eine T-Pose. Der Schluss folgt zwingend aus dem gemessenen
Mechanismus, ist aber **noch nicht im Bild bestätigt** — das sollte vor der Gegner-Integration
geprüft werden. Ein Asset kann das nicht lösen: gewünscht ist das Halten der Todespose,
und dafür darf die Geschwindigkeit nicht auf 0 gehen.

---

## Mira integriert — Protokoll und Belege (2026-09-12)

### Herkunft

Gleiche Quelle wie beim Helden: **Quaternius CC0** (Public Domain, keine Namensnennung
nötig), Ranger-Outfit als Basis. Kein Meshy- oder Tripo-Credit verbraucht. Die in Abschnitt 1
ursprünglich eingetragene Quelle „Meshy generiert" ist damit überholt; die Tabelle ist
korrigiert.

Tönung nach `docs/style-reference/STYLE.md`: **`#50747b`**, in die Albedo-Pixel gebacken
(numpy), weil glTF keine Shader-Nodes transportiert. Fünf Materialien `pq_mira_*`.

### Rig — zweiter Durchlauf war nötig

Der **erste** Mixamo-Auto-Rig war falsch: Kinn-Marker zu tief gesetzt, Ergebnis

```
arm_vec   (0.923, -0.157, -0.661)   → Arme schief im Raum
head_z    1.455 m                   → zu niedrig für 1,95 m Standardhöhe
```

Neu hochgeladen und mit korrigierten Markern gerigt (Kinn y=171 statt 182, Knie y=404 statt
392):

```
arm_vec      (1.267, -0.001, 0.000)
arm_tilt     0°
head_z       1.731 m
mesh_dims    1.81 × 0.418 × 1.951 m
```

Das ist im Protokoll festgehalten, weil derselbe Fehler bei den Gegnern droht: **der
Kinn-Marker gehört hoch an den Kiefer, nicht an den Hals.**

### Aufbereitung in Blender

* 65 Mixamo-Bones umbenannt, `Root` ergänzt, **6435 F-Curve-Pfade** umgeschrieben.
* Vertragsjoints vorhanden (`contract_missing: []`); die Vertragsnamen `Root … Foot.R`
  existieren, das Rig trägt darüber hinaus die vollen Mixamo-Ketten inkl. Finger.
* Farb-Attribute entfernt, eine UV-Lage behalten.
* 11 Actions `MiraIdle … MiraCast`. Die **NLA-Tracks** tragen die Vertragsnamen
  `Idle … Cast`, weil glTF den Animationsnamen vom Track nimmt, nicht von der Action.
* Root Motion flachgelegt: Walk Achse 2, Run Achse 2, Attack2 Achsen 0/1/2, Death Achsen 1/2.
* Alle Clips beginnen auf Frame 0.

### Angriffszeiten gegen `combat/AttackTimeline.java`

| Clip | Soll Dauer | Ist | Soll Treffer | Ist |
|---|---|---|---|---|
| Attack1 | 0,52 s | 0,533 s | 0,15 s | 0,156 s |
| Attack2 | 0,58 s | 0,567 s | 0,19 s | 0,183 s |
| Attack3 | 0,78 s | 0,767 s | 0,28 s | 0,277 s |

Retiming durch **Messen des Trefferframes und Beschneiden vorn**, nicht durch gleichmäßiges
Stauchen — sonst verliert der Schlag seine Beschleunigung.

### Export

```
export_animation_mode = "NLA_TRACKS"   # ACTIONS dupliziert die Animationen
use_active_scene      = True           # sonst schreibt Blender alle Szenen ins glTF
export_yup            = True
```

`use_active_scene` ist nicht optional. Ohne den Schalter schrieb Blender fünf Szenen ins
glTF und setzte die Default-Szene auf Index 4; jME stürzte in `GltfLoader.readScenes` mit
`IndexOutOfBoundsException: Index 4 out of bounds for length 1` ab. Der Held wurde deshalb
mit demselben Schalter neu exportiert.

Ergebnis `src/main/resources/models/characters/mira.glb`: **6,27 MB**, 11 Animationen,
`scenes: [{"name": "PQ_MiraBuild", "nodes": [67]}]`, `scene_default: 0`, Texturen auf 512
herunterskaliert.

### Verifikation

| Prüfung | Ergebnis |
|---|---|
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** |
| Smoke-Run | alle **32 Stufen**, `target/smoke-ok.txt`, 13 Screenshots, keine Exception |
| Blender-Renders front/side | `art/probe/views/mira-blender-*.png` — Kapuze, Gesicht sichtbar, Ranger-Outfit, Palette |
| Sichttest im Spiel | `target/screenshots/pentagon-2.png`, Zuflucht am Feuer — Mira verformt (nicht Bindepose), Füße auf dem Boden, Maßstab passt zu den Bodenplatten |

Der „Blockkopf", der im Java-`View`-Probe auffiel, war die **Kapuze aus einem unvorteilhaften
Winkel** — in den Blender-Renders und im Spiel ist der Kopf korrekt.

### Nicht gelungen und verworfen

Ein Weight-Transfer als Abkürzung (Gewichte des Helden auf Miras Mesh) schlug fehl:
Rest-Dimensionen 3,53 × 3,79 × 3,62 m. Ursache ist der Raumkonflikt aus dem FBX-Rundlauf
(Armature +90° X, Mesh −90° X). Ich habe den Weg **abgebrochen** statt weiter zu debuggen
und die Szene `PQ_Mira` gelöscht; der reguläre Mixamo-Weg war schneller.

---

## Eren integriert — Protokoll und Belege (2026-09-12)

`src/main/resources/models/characters/eren.glb`, **5,45 MB**, 13 Texturen à 512 px (Augen 256),
elf Clips, 66 Joints, Pivot am Boden, **1,95 m**. Wieder **ohne Generierungs-Credit**.

### Herkunft

| Teil | Quelle | Lizenz |
|---|---|---|
| Kopf, Haar, Augen | Quaternius *Universal Base Characters* (Superhero-/Regular-Male, Hair_1) | CC0 |
| Kleidung | Quaternius *Modular Character Outfits – Fantasy*, **Peasant** | CC0 |
| Rig | Mixamo Auto-Rigger, Standard-Skelett 65 Bones | Adobe, lizenzfrei nutzbar |
| Animationen | Mixamo *Sword And Shield Pack* (10 Clips) + *Stand To Roll* (Dodge) | Adobe, lizenzfrei nutzbar |

Rohdaten unter `art/mixamo/eren/` (50 FBX) und `art/textures/`. Das Peasant-Outfit passt zur
Figur: Eren ist der Gefangene im Kettenverlies, kein Kämpfer in Rüstung.

### Rig

Marker mit der bei Mira gelernten Korrektur gesetzt (Kinn **hoch am Kiefer**, nicht am Hals).
Ergebnis beim **ersten** Durchlauf brauchbar — kein zweiter Anlauf nötig:

```
Rest-Bounding-Box (Weltraum)   1,934 × 0,378 × 1,950 m
Füße                            z = 0,000   (Pivot am Boden)
Höhe                            1,950 m     (Standardhöhe)
contract_missing                []
```

### Tönung — die Rezeptur ist jetzt festgehalten

Palette-Ton **`#806e56`**. Bisher war nur „in die Albedo-Pixel gebacken" dokumentiert; das war
zu wenig, um es zu wiederholen. Die Rezeptur wurde deshalb aus den fertigen Helden-Maps
**zurückgerechnet** (kleinste Quadrate über je 262 144 Pixel) und lautet im **linearen** Raum:

```
lum   = 0.2126·R + 0.7152·G + 0.0722·B
tint_n = tint_linear / lum(tint_linear)
out   = g · ( src + s · (lum · tint_n − src) )
```

| Materialklasse | s | g | RMSE der Rückrechnung am Helden |
|---|---:|---:|---:|
| Outfit (Ranger/Peasant) | 0,52 | 0,615 | 0,025 |
| Körper (Superhero) | 0,54 | 0,667 | 0,017 |
| Haut (Regular) | 0,54 | 0,667 | 0,017 |
| Haar | 0,22 | 0,524 | **0,004** |
| Augen | 0,50 | 0,647 | 0,030 |

Die Rückrechnung ist keine exakte Rekonstruktion — bei Haar trifft sie praktisch exakt, bei
Outfit und Haut bleibt ein Rest. Als **Konsistenzprüfung** dient deshalb die mittlere lineare
Leuchtdichte je Map; sie muss der des Helden entsprechen:

| Map | Held | Eren |
|---|---:|---:|
| Outfit | 0,1533 | 0,1389 |
| Körper | 0,3043 | 0,3073 |
| Haut | 0,3040 | 0,3060 |
| Haar | 0,2952 | 0,2955 |
| Augen | 0,3068 | 0,2999 |

Fünf Materialien `PQE_MI_*`, Aufbau identisch zu Mira: Albedo (sRGB) → Base Color,
Normal-Map-Node, Roughness-Map wo vorhanden, sonst Skalar. `Metallic = 0,12`,
`Roughness = 0,82` — beides innerhalb der Grenzen aus `STYLE.md` (Metallic ≤ 0,4,
Roughness ≥ 0,55). Die Peasant-Roughness kommt aus dem Grünkanal der ORM-Map
(min 0,64 / Mittel 0,92); der Metallic-Kanal ist mit Mittel 0,016 praktisch null.

### Aufbereitung in Blender

* 65 Mixamo-Bones umbenannt, `Root` als Eltern von `Hips` ergänzt, **6500 F-Curve-Pfade**
  umgeschrieben.
* Farb-Attribute entfernt; von vier UV-Lagen bleibt `UVMap` — die einzige, die für **alle fünf**
  Materialslots belegte UVs trägt (die anderen drei sind außerhalb je eines Slots null; geprüft,
  nicht angenommen).
* 11 Actions `EREN_Idle … EREN_Cast`; die **NLA-Tracks** tragen die Vertragsnamen
  `Idle … Cast`, weil glTF den Animationsnamen vom Track nimmt.
* Alle Clips beginnen auf Frame 0.

### Root Motion — diesmal nach Regel statt nach Gefühl

Die Hips-Achsen wurden erst **bestimmt**, nicht geraten: die Hips-Rest-Matrix ist die Einheit,
das Armature-Objekt ist um +90° X gedreht, und über den Run-Clip bewegt sich die Hüfte
2,981 Einheiten auf Welt-**Y** bei konstanter Welt-**Z**-Höhe. Also gilt lokal
**0 = seitlich, 1 = vertikal, 2 = vorwärts**.

Regel: **beide waagerechten Achsen (0 und 2) werden auf 0 gelegt, die senkrechte (1) bleibt.**
Damit besitzt Bullet Position und Richtung (Vorgabe aus `COWORK-BRIEFING.md`), während Sturz,
Hocke und Rolle ihre Höhenbewegung behalten.

Entfernte Wanderung (Auswahl): Run 2,981 · Walk 1,697 · Death 1,321 · Dodge 1,577 ·
Attack2 1,626. Bei den drei Angriffen wurde zusätzlich der **konstante Versatz** genullt, den
das Beschneiden vorn hinterlässt (Attack2 stand sonst 1,0 m vor seinem Root).

### Angriffszeiten gegen `combat/AttackTimeline.java`

Trefferframe gemessen als Frame der **maximalen Hand.R-Geschwindigkeit**, dann vorn beschnitten
(nicht gleichmäßig gestaucht) und auf ganze Zielframes neu abgetastet:

| Clip | Quelle | Treffer in der Quelle | Soll Dauer | Ist (in der Engine gemessen) |
|---|---|---:|---:|---:|
| Attack1 | `sword and shield slash.fbx` | Frame 19 | 0,52 s | **0,533 s** |
| Attack2 | `sword and shield attack (2).fbx` | Frame 17 | 0,58 s | **0,567 s** |
| Attack3 | `sword and shield slash (3).fbx` | Frame 26 | 0,78 s | **0,767 s** |

Abweichung je ≤ 0,013 s — das ist die Auflösung eines Frames bei 30 fps, genauer geht es mit
ganzen Frames nicht.

### Clip-Quellen vollständig

| Clip | Quelle | Länge |
|---|---|---:|
| Idle | `sword and shield idle.fbx` | 3,533 s |
| Walk | `sword and shield walk.fbx` | 1,100 s |
| Run | `sword and shield run.fbx` | 0,700 s |
| Attack1 | `sword and shield slash.fbx` | 0,533 s |
| Attack2 | `sword and shield attack (2).fbx` | 0,567 s |
| Attack3 | `sword and shield slash (3).fbx` | 0,767 s |
| Dodge | `Stand To Roll` (mit **In Place**) | 2,367 s |
| Block | `sword and shield block idle.fbx` | 1,400 s |
| Hit | `sword and shield impact.fbx` | 0,700 s |
| Death | `sword and shield death.fbx` | 2,300 s |
| Cast | `sword and shield casting.fbx` | 2,967 s |

Die Quellen von Held und Mira waren nicht dokumentiert; sie wurden über die Framelängen
rekonstruiert. Bis auf **Block** (Eren 42 statt 40 Frames) und **Hit** (21 statt 23) sind Erens
Clips deckungsgleich mit Miras. Beide Clips sind nicht zeitvertraglich gebunden — nur die drei
Angriffe sind es.

### Export

```
export_animation_mode = "NLA_TRACKS"
use_active_scene      = True
export_yup            = True
export_force_sampling = True
```

Ergebnis: `scenes: [{"name": "PQ_ErenBuild", "nodes": [67]}]`, `scene_default: 0`,
11 Animationen (`anim_missing: []`, `anim_extra: []`), 66 Joints (`contract_missing: []`),
5 Materialien, 13 Texturen, je Animation 198 Kanäle.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests, 0 Fehler** | `cowork-logs/verify-current.log` |
| Smoke-Run | **alle 33 Stufen (0–32)**, 30 `[CHECK]`, keine Exception | `cowork-logs/smoke-eren.log` |
| `target/smoke-ok.txt` | PASS, alle fünf Gebiete, beide Enden | — |
| Eren lädt im Spiel | `Hardware skinning engaged for ErenArmature` (2×) und `[GAME] Aschenklinge erhalten. Erens Blick folgt dir.` | ebd. |
| Clipvertrag in der Engine | alle 11 Clips vorhanden, Längen wie oben, 66 Joints | `cowork-logs/probe-eren.log` |
| Engine-Renders | Idle, Attack1 am Treffer, Death — korrekt verformt, Texturen und Palette stimmen | `art/probe/eren/*.png` |
| Blender-Renders front/side | kahl, hager, Peasant-Kittel im Ockerton, Stiefel; keine gebrochenen Gelenke | `art/probe/views/eren-blender-*.png` |

Der Engine-Probe (`ErenProbe`, temporär außerhalb des Repos kompiliert) geht durch **denselben**
Pfad wie das Spiel: `AssetPipeline.model` → jME-glTF-Loader → `CharacterFactory.create` mit
seiner Vertragsprüfung. Er ist damit aussagekräftiger als ein Blender-Render.

**Nicht belegt:** Eren ist auf keinem Smoke-Screenshot zu sehen — der Durchlauf fotografiert die
Schleuse des Kettenverlieses, nicht den Raum mit Eren. Dass er lädt, animiert und korrekt
aussieht, ist über Log und Engine-Probe belegt, ein Spielfoto von ihm fehlt.

### Testabdeckung erweitert

`AssetTest` prüfte bis hierher nur `hero`. Der Test heißt jetzt
`deliveredCharacterAssetsSatisfyTheContract` und läuft über **alle drei gelieferten IDs**
(`hero`, `mira`, `eren`). Er lädt jedes GLB durch den echten jME-AssetManager und prüft:

* alle 16 Vertrags-Joints vorhanden (mehr Joints sind erlaubt),
* alle 11 Clips vorhanden **und** mit Tracks und Länge > 0 — `hasAnimClip` allein würde einen
  leeren Clip durchlassen,
* `BindPosePosition`, `BoneIndex`, `BoneWeight` auf jedem animierten Mesh — ohne Bindepose
  überspringt jME das Skinning und rendert unverformt, ohne Fehlermeldung,
* die drei Angriffslängen gegen `AttackTimeline.DURATION` mit 0,034 s Toleranz (ein Frame
  bei 30 fps).

Neue Charaktere müssen in die ID-Liste dieses Tests eingetragen werden, sonst prüft sie nichts.

`mvn -B verify`: **BUILD SUCCESS, 50 Tests** (`AssetTest` 6 davon) —
`cowork-logs/verify-current.log`.

---

## Goblin integriert — Protokoll und Belege (2026-09-12)

`src/main/resources/models/characters/goblin.glb`, **2,43 MB**, 20 000 Dreiecke, 11 Clips,
**26 Joints**, Pivot am Boden, 1,95 m (im Spiel ×0,8 → 1,56 m). Kosten: **20 Meshy-Credits.**

Der erste Gegner war teuer an Erkenntnissen. Vier davon gelten für alle weiteren.

### Was die Plattformen im kostenlosen Plan wirklich hergeben

| | Tripo | Meshy |
|---|---|---|
| Guthaben | 200 → **170** | 100 → **80** |
| Generierung | 40 (v3.0/v2.5), 55 (v3.1) | **10** (Meshy 6 Lite) |
| Texturierung | inklusive | **+10** (PBR-Schalter) |
| Export im Free-Plan | **nur** Modelle aus v2.5 („H2.5"), 15/Monat | **nur** Meshy 6 Lite, 10 Downloads/Monat |
| DCC-Bridge nach Blender | Pro-only | — |
| T-Pose-Schalter | **kostenlos** | zahlungspflichtig |
| Lizenz | öffentlich, nicht kommerziell | CC BY 4.0 |

**Fehler, den ich gemacht habe:** Ich habe auf Tripo mit **v3.0** generiert (30 Credits), ohne
vorher zu prüfen, welche Modellstufe der kostenlose Plan exportieren darf. Das Modell war gut
und ist **nicht herauszubekommen**. Die Credits sind verloren. Vor jeder weiteren Generierung
gilt: **erst die Exportbedingung der Stufe prüfen, dann generieren.**

Damit steht der Weg fest: **Meshy 6 Lite, 20 Credits je Gegner.** 80 Credits reichen für die
vier restlichen — ohne Reserve.

### Der Meshy-Rohexport ist topologisch zerschossen

Das GLB sah dicht aus, bestand aber aus **292 unverbundenen Flicken**: an jeder UV-Naht liegen
die Vertices doppelt und unverschweißt. Das ist kein Schönheitsfehler — kein Rigger kann darauf
arbeiten.

`remove_doubles(threshold=0.0005)` entfernt **7 939** doppelte Vertices → **eine**
zusammenhängende Fläche, UV-Layout unversehrt (Blender speichert UVs pro Face-Corner, nicht pro
Vertex). Danach die Prüfung: 10 000 Vertices, 20 000 Dreiecke, **0** non-manifold-Kanten, **0**
Ränder, **0** entartete Flächen, **0** Duplikate.

**Dieser Schritt gehört bei jedem Meshy-Modell an den Anfang.**

### Mixamo rigt diesen Goblin nicht — vier Versuche

| Versuch | Zustand des Meshes | Skeleton LOD | Ergebnis |
|---|---|---|---|
| 1 | Original-Pose (Arme am Körper), verschweißt | Standard (65) | Abbruch ohne Meldung |
| 2 | wie 1 | No Fingers (25) | Abbruch |
| 3 | Hals aufgerichtet, Arme in A-Pose | Standard (65) | Abbruch |
| 4 | volle T-Pose, Arme waagerecht | No Fingers (25) | Abbruch |

Das Mesh war zu diesem Zeitpunkt messbar sauber, korrekt skaliert, ein einzelnes Objekt, in
genau der Pose, die Mixamo verlangt. Meine verbleibende Erklärung sind die Proportionen: **Kopf
samt Ohren belegt 26 % der Körperhöhe**, beim Menschen sind es 13 %. Belegt ist das nicht —
Mixamo gibt keine Fehlermeldung heraus. Festgehalten ist die Beobachtung, nicht die Ursache.

Den Kopf zu verkleinern, um Mixamo zufriedenzustellen, habe ich verworfen: Ohren und Schnauze
sind das, was die Figur zum Goblin macht.

### Der Weg, der funktioniert: Erens Rig umzielen

`eren.glb` importiert in Blender mit **allen elf Actions** unter ihren Vertragsnamen
(`Idle` … `Cast`) — glTF nimmt die Action-Namen aus den Animationen. Damit liegt ein geprüftes
Rig samt geprüfter Angriffszeiten vor. Das wird auf den Gegner umgezielt:

1. **26 Knochen behalten**, die 40 Fingerknochen entfernen (`CharacterFactory` prüft keine
   Joint-Anzahl; die Clips bewegen Finger ohnehin kaum).
2. **Nur Längen skalieren, Richtungen und Rolls exakt übernehmen.** Das ist der entscheidende
   Punkt. Beim ersten Anlauf habe ich die Knochen auf gemessene Gelenkpositionen gesetzt —
   damit ändern sich die lokalen Achsen, auf die sich die Animationen beziehen, und das Ergebnis
   war eine zusammengeknickte Figur mit über dem Kopf gekreuzten Armen. Richtig ist:
   `head = tail(Eltern) + Versatz·s`, `tail = head + richtung_eren · länge_eren · s`.
3. Skalierungsfaktoren aus gemessenen Landmarken: Rumpf **1,60** (Goblin hat kurze Beine und
   einen langen Oberkörper), Hals/Kopf **1,01**, Arme **0,929**, Oberschenkel/Schienbein
   **0,503**, Füße **1,0**. Hüfte und Oberschenkelköpfe absolut gesetzt.
4. **Automatische Gewichte** (`parent_set(type="ARMATURE_AUTO")`), 26 Vertexgruppen.
5. Actions von 24 fps (glTF-Import) auf **30 fps** umgerechnet (×1,25) — damit stimmen die
   Framezahlen exakt mit Eren: Idle 106, Walk 33, Run 21, Attack1 16, Attack2 17, Attack3 23,
   Dodge 71, Block 42, Hit 21, Death 69, Cast 89.

### Die Bindepose muss zur Ruhepose des Rigs passen

Drei Mesh-Eingriffe waren nötig, jeder mit einem messbaren Grund:

| Eingriff | Messung | Warum |
|---|---|---|
| Hals aufgerichtet | Kopf **0,28 m** vor den Schultern | Mixamo-Clips setzen einen Kopf über den Schultern voraus |
| Arme in T-Pose | 90° um (x 0,24 / z 1,42) | Erens Ruhepose ist eine T-Pose; die Gewichte werden aus der Ruhepose berechnet |
| Kopfneigung | Schnauze **24° → 10°** unter der Waagerechten | Erens Kopfknochen steht aufrecht; ohne das kippte der Kopf im Spiel ~40° zu weit nach vorn |

**Armauswahl nicht über Koordinaten, sondern über Bereichswachstum:** Dijkstra entlang der
Mesh-Kanten von der Fingerspitze, begrenzt auf 0,95 m. Der Schurz hängt zwar im gleichen
x-Bereich, ist aber geodätisch über 1,2 m entfernt und damit **per Konstruktion** außen vor. Der
erste Versuch mit einem reinen `|x| > 0,24`-Filter hatte Beine, Füße und Schurz mitgedreht
(Spannweite 2,9 m statt 1,8 m).

### Tönung

Palette-Ton **`#5b805c`**. Rezeptur wie bei Eren (`s = 0,54`), danach auf die mittlere lineare
Leuchtdichte der Helden-Hautmap (**0,304**) skaliert — Faktor 1,861, weil `g = 0,667` die
ohnehin dunkle Meshy-Textur zu weit abgesenkt hätte. Ein Material `PQG_MI_Goblin`,
Metallic 0,12, Roughness-Map aus dem Grünkanal der ORM-Map und **auf ≥ 0,55 geklemmt**
(`STYLE.md`); der Metallic-Kanal liegt bei 0,002, also praktisch null.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| GLB-Struktur | 1 Szene (Index 0), 11 Animationen (`anim_missing: []`), 26 Joints (`contract_missing: []`), 1 Material, 3 Texturen | — |
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-current.log` |
| `AssetTest` | Goblin in der ID-Liste; Joints, Clips, Tracks, Bindepose-Buffer und Angriffszeiten gegen `AttackTimeline` geprüft | ebd. |
| Smoke-Run | alle **33 Stufen**, `smoke-ok.txt`, 13 Screenshots, keine Exception | `cowork-logs/smoke-goblin.log` |
| Goblin lädt | `Hardware skinning engaged for GoblinArmature` **26×** | ebd. |
| Sichttest im Spiel | `target/screenshots/pentagon-6.png`, Kettenverlies — grün, spitze Ohren, Maßstab ×0,8 korrekt neben dem Platzhalter-Gegner | — |
| Blender-Posen | Idle, Walk, Attack1, Block, Death — aufrecht, Schritt greift, kein gebrochenes Gelenk | `art/probe/views/goblin-rig3-*.png` |

### Offen und ehrlich benannt

* Die **Gewichte sind automatisch**, nicht gemalt. Schulter und Ellbogen zeigen bei starker
  Beugung eine dünne Stelle. Für einen Gegner, der ×0,8 skaliert und meist in Bewegung ist,
  halte ich das für tragbar; nachbessern ließe es sich jederzeit.
* Der **Hals ist sehr kurz**, der Schädel sitzt tief auf den Schultern. Folge der Aufrichtung —
  sieht für einen Goblin passend aus, ist aber eine Abweichung von der Vorlage.
* Der **Hunch geht verloren**, sobald animiert wird: die Clips bestimmen die Haltung, nicht die
  Bindepose. Das gilt für jede Mixamo-Animation und ist nicht behebbar, ohne eigene Clips zu
  bauen.
* Der Goblin steht auf `pentagon-6.png` in der **T-Pose** — das ist der bereits dokumentierte
  Aufnahmezeitpunkt direkt nach `loadRegion`, kein Fehler am Asset.

### Vorlage für Ork, Wächter, Aschenrufer und König

Der Ablauf steht damit und ist wiederholbar:

```
Meshy 6 Lite (10) → PBR-Textur (10) → GLB laden
  → remove_doubles                      (die 292 Flicken verschweißen)
  → auf 1,95 m / Pivot am Boden normieren
  → Hals aufrichten, Kopf waagerecht    (gemessen, nicht geschätzt)
  → Arme per Bereichswachstum in die T-Pose
  → auf 20 000 Dreiecke dezimieren
  → Erens Rig umzielen (nur Längen!)    (Faktoren je Gegner neu messen)
  → automatische Gewichte
  → Palette-Ton backen, ein Material
  → NLA-Tracks Idle…Cast, use_active_scene, export_yup
  → ID in AssetTest eintragen, verify + Smoke
```

Beim **König** (×1,85, laut `STYLE.md` ausdrücklich kein skalierter Mensch) ist dieser Weg nicht
nur der bequemere, sondern der einzige: Mixamo würde eine Figur mit diesen Proportionen
erwartbar ebenfalls abweisen.

---

## Belege umgezogen, drei Logs verloren (2026-09-12)

Die Logs lagen unter `target/cowork-logs/`. **Das war ein Fehler im Aufbau:** `target/` ist das
Verzeichnis, das `mvn clean` löscht. Zusätzlich ignoriert `.gitignore` pauschal `*.log` — die
Belege, die dieses Dokument zitiert, waren also **nie eingecheckt**.

Beim ersten `mvn clean verify` ist genau das passiert: der Lauf hat `target/` geleert und dabei
`verify-hero*.log`, `verify-mira*.log`, `verify-eren*.log` und `verify-goblin.log` **gelöscht**.
Die `smoke-*`- und `probe-*`-Logs haben überlebt, weil der Lauf an einer offenen Datei
abgebrochen ist.

Behoben:

* Alle Belege liegen jetzt unter **`cowork-logs/`** im Projektwurzelverzeichnis, außerhalb der
  Reichweite von `mvn clean`. Alle Verweise in diesem Dokument sind umgeschrieben.
* `.gitignore` nimmt diesen Ordner ausdrücklich aus dem `*.log`-Muster heraus, damit die Belege
  versioniert sind. 3,7 MB für 16 Dateien.
* Die vier verlorenen `verify`-Logs sind durch **`cowork-logs/verify-current.log`** ersetzt. Das
  ist kein Notbehelf, sondern der stärkere Beleg: `AssetTest` prüft inzwischen **alle vier**
  gelieferten IDs in einem Lauf, die Einzellogs waren bereits veraltet.

`cowork-logs/verify-current.log`: `mvn -B clean verify` → **BUILD SUCCESS, 50 Tests**, `AssetTest`
6 davon, mit `hero`, `mira`, `eren`, `goblin`.

---

## Dungeon aufgewertet — Texturen und Modul-Vorbereitung (2026-09-12)

Parallel zu den Charakteren, weil reiner Java-Code weder Blender noch den Browser braucht. Zwei
Stränge, getrennte Dateien, beide gegen das ausgelieferte Jar kompiliert.

### Texturen: `tools/AssetBaker.java`

Vorher: 256², zwei Oberflächen, Albedo als **reines Graustufenbild** mit hartem Fugenraster,
**Roughness als flache Konstante** (220 bzw. 135), Metallic flach. Nur die Normal-Map hatte Inhalt
— und `metal-normal.png` war **byte-identisch** mit `stone-normal.png`.

Nachher: **512²**, positionsgehasht statt `java.util.Random`, deshalb überhaupt kachelbar und über
Läufe hinweg byte-identisch (zwei Läufe, 12/12 Maps identisch).

| Map | alt | neu |
|---|---|---|
| stone-albedo | Mittel 185,5 · sd 35,4 | Mittel 183,9 · sd 37,7 |
| stone-roughness | **konstant 220, sd 0** | Mittel 215,6 · **sd 19,9** · 174–248 |
| metal-albedo | Mittel 182,0 · sd **10,1** | Mittel 175,6 · sd **25,4** |
| metal-roughness | **konstant 135, sd 0** | Mittel 237,0 · sd 9,4 |
| metal-metallic | **konstant 238** | Mittel 99,6 · gedeckelt auf 127 |

**Drei echte Fehler im Bestand gefunden und behoben:**

1. `stone-albedo` hatte einen **harten Kachelfehler**: `y % 64 < 3` setzte eine Fuge in Zeile 0,
   aber keine in Zeile 255. Nahtmaß (Kantendifferenz ÷ innerer Nachbardifferenz, 1,0 = nicht
   unterscheidbar) **4,95 links/rechts und 8,09 oben/unten** → jetzt 0,46 / 0,29.
2. Der Kursversatz lag auf dem Zellgitter, dadurch lief eine **durchgehende senkrechte
   Mörtellinie** durch jede waagerechte Wiederholung.
3. `metal-metallic` lag bei effektiv **0,746** und verletzte damit die Obergrenze 0,4 aus
   `STYLE.md` klar. Von der Texturseite geheilt (Deckel 127 → 0,398).

**Eine Grenze bleibt unerreichbar:** `WorldView` multipliziert Eisen und Gold mit Roughness 0,5
bzw. 0,45. Für die Untergrenze 0,55 aus `STYLE.md` müsste die Map über 1,0 liegen. Die Map ist so
hoch gezogen, wie es geht (0,80–0,988), das Produkt landet bei 0,35–0,55. Der Rest wäre eine
Änderung der beiden Floats in `WorldView` — **bewusst nicht gemacht**, weil das Spielverhalten
ändert und in denselben Lauf gehörte wie der Modul-Umbau.

Auflösung 512 begründet gemessen: acht Maps kosten 0,41 MB bei 256, **1,95 MB bei 512**, 6,58 MB
bei 1024. 1024 wurde gebacken und verworfen — das Briefing warnt vor Binärdateien dieser Größe in
der Git-Historie, und der Höhennebel des `AtmosphereFilter` verschluckt das Mehr an Detail hinter
wenigen Metern.

Zusätzlich erzeugt: **`wood-*`** (Plankenbrett). **Von keinem Material referenziert** — der
Anschluss wäre `assets.pbr("", …)` → `assets.pbr("wood", …)` an der Truhe (`WorldView:451`).
Bewusst nicht verdrahtet.

### Modul-Vorbereitung: `WorldView` und `docs/dungeon-kit.md`

`docs/dungeon-kit.md` ist die Spezifikation, aus der die Blender-Module gebaut werden: jede Zahl
aus dem Code abgeleitet und mit Fundstelle belegt.

`WorldView` nimmt jetzt ein glTF-Modul, **wenn** eines unter `models/props/<id>` liegt, und sonst
unverändert die heutige `box()`-Geometrie. Kein eigener Ladecode — einzige Auflösungsstelle bleibt
`AssetPipeline.model(id, fallback)` mit der prozeduralen Geometrie als `fallback`.

* **Pivot-Regel:** ein Modul wird um `half` tiefer gesetzt als die zentrierte `Box`, weil
  Prop-Pivots unten-mittig sitzen. Die Unterseite bleibt damit dort, wo die Layout-Kollider
  gemessen sind.
* **Wandvarianten** werden aus den zum Boden offenen Seiten klassifiziert. Über alle fünf Gebiete
  nachgerechnet: 726 `kit_wall_face`, 111 `kit_wall_corner`, 8 `kit_wall_span`, 8 `kit_wall_pier`,
  **0** `kit_wall_free`, und alle 853 Zellen treffen nach der Drehung ihre offenen Seiten
  (0 Fehlausrichtungen).
* **Tönung** erreicht ein Modul wie den prozeduralen Stein: als Faktor auf `BaseColor`, nach der
  in diesem Dokument festgehaltenen Rezeptur leuchtdichte-normiert. Rohes Multiplizieren würde um
  etwa Faktor 6 verdunkeln. Material wird vorher geklont (sonst blutet es in den Asset-Cache) und
  **einmal am Prototyp** getönt — sonst bekäme jede Zelle ein eigenes Material und
  `GeometryBatchFactory`, das nach Material gruppiert, würde in einen Batch je Zelle zerfallen.
* **Kollision, LOD und Flamme bleiben prozedural.** Die Wand-Collider kommen weiter aus dem
  Layout, nicht aus der Modulgeometrie.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| `javac` gegen das Jar | 63 Klassen (61 + 2 neue Records), 0 Fehler | — |
| `mvn -B clean verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-current.log` |
| Smoke-Run | alle **33 Stufen**, `smoke-ok.txt`, 13 Screenshots, keine Exception | `cowork-logs/smoke-dungeon.log` |
| Sichttest | Kettenverlies und Zuflucht — Mauerwerk mit Lagen und Tiefe, Bodenplatten mit Variation, Fackellicht fängt jetzt das Relief | `target/screenshots/pentagon-6.png`, `-8.png` |
| `pentagonradio.wav` | unverändert: 35 305 132 B, mtime 2026-08-24, md5 `d7cd015234a9fb0316011d30097f844b` | — |

### Offen

* Der Dungeon ist **merklich dunkler** als vorher. Das entspricht der Vorgabe „düsterer
  Dark-Fantasy-Realismus", ist aber eine Geschmacksentscheidung und in einem Zug zurückzunehmen.
* Der **komplette Modulpfad ist noch nie gelaufen** — es liegt keine `kit_*`-Datei im Repository.
  Belegt ist nur, dass er kompiliert und dass ohne Moduldatei jeder Pfad den `box()`-Zweig nimmt.
  Ob `GeometryBatchFactory.optimize` über importierte Netze sauber arbeitet, ist das erste, was
  mit dem ersten echten Modul zu prüfen ist.
* Die **Vorzeichenkonvention der Normal-Map** (`-dx, -dy, +1`, OpenGL +Y) ist unverändert
  übernommen. Ob sie stimmt, lässt sich nur gerendert beurteilen und ist nicht nachgemessen.
* `google-java-format` ist **nicht gelaufen** (Maven Central über den Proxy gesperrt).
  Formatierung von Hand nach Bestandsmuster, 2 Leerzeichen, ≤ 100 Spalten geprüft.

---

## Ork integriert — Protokoll und Belege (2026-09-12)

`src/main/resources/models/characters/orc.glb`, **2,36 MB**, 20 000 Dreiecke, 9 994 Vertices,
11 Clips, **26 Joints**, ein Material `PQO_MI_Orc`, drei Texturen à 512². `EnemyType.ORC`
skaliert ihn im Spiel auf ×1,15, also **2,24 m**.

### Herkunft

**Meshy 6 Lite**, Text-zu-3D, eine Generation, Lizenz CC BY 4.0, Pose „Keine" (A-/T-Pose-Steuerung
ist zahlungspflichtig). Kosten **10 Credits** für das Netz, **10 Credits** für die PBR-Texturen,
Guthaben danach **90**. Der Prompt steht in `art/gen/` neben der Rohdatei und nennt ausdrücklich
*no weapon, no shield, no base, no pedestal* — Waffen hängt `CharacterFactory` selbst an
`WeaponSocket` (`CharacterFactory.java:133`), ein Sockel würde die Fußhöhe verfälschen.

Rohlieferung: 132 816 Dreiecke, 77 365 Vertices. Nach `remove_doubles(0,0005)` bleiben
**66 381 Vertices** und **eine** zusammenhängende Komponente — die UV-Naht-Duplikate, die beim
Goblin 292 Einzelflicken erzeugt hatten, sind damit auch hier weg. **15 nicht-mannigfaltige
Kanten** bleiben übrig (Goblin: 0); sie liegen an den Klauenspitzen und am Lendenschurzsaum und
haben weder Rig noch Export gestört.

### Der Umweg, der diesmal entfiel: kein Retarget

Beim Goblin habe ich Erens Rig mit gemessenen Skalierungsfaktoren nachgebaut. Für den Ork war das
**nicht nötig**, und das ist gemessen, nicht geraten:

| Strecke | Eren | Ork | Verhältnis |
|---|---:|---:|---:|
| Schulter → Handwurzel | 0,577 m | 0,595 m | **1,031** |
| Schulterhöhe | 1,587 m | 1,533 m | 0,966 |
| Gesamthöhe | 1,95 m | 1,95 m | 1,000 |

Bei 3 % Abweichung in der Armlänge lohnt kein Retarget. Ich habe deshalb **Erens Rig unverändert
übernommen** (`ErenRigSrc`, Ruhepose bitgenau) und nur die 40 Fingerknochen entfernt — der Ork hat
drei Klauen je Hand, Erens fünf Fingerketten hätten dort nichts zu greifen. Übrig bleiben genau
die **26 Joints**, die auch der Goblin trägt:

```
Root Hips Spine Spine1 Spine2
LeftShoulder UpperArm.L Forearm.L Hand.L
RightShoulder UpperArm.R Forearm.R Hand.R
Neck Head HeadTop_End
Thigh.L Shin.L Foot.L LeftToeBase LeftToe_End
Thigh.R Shin.R Foot.R RightToeBase RightToe_End
```

Weil die Ruhepose identisch ist, laufen **Erens eigene Aktionen** (`EREN_Idle` … `EREN_Cast`)
ohne Umzielung. Das ist der genauest mögliche Fall: die Clips wurden für exakt diese Ruhepose
gebacken.

### Ausrichtung — zweimal falsch gemessen, dann gerendert

Meine erste Heuristik war, die Blickrichtung aus dem Fußabdruck zu lesen: Zehen weiter vorn als
Ferse. Das Ergebnis war **falsch**, weil der Ork Stiefel mit Fellkragen trägt und der Kragen
hinten 0,275 m heraussteht, die Zehen vorn nur 0,189 m. Die zweite Heuristik über die Kopfmasse
war ebenfalls falsch, weil die Schulterplatten in das Messband ragen.

Entschieden hat ein **Render** aus −Y (`art/probe/orc/orc-front_minusY.png` zeigte den Rücken,
`orc2-front_minusY.png` das Gesicht) und die Gegenprobe am Rig: `GoblinArmature.LeftToe_End`
zeigt mit `y = −0,397` nach **−Y**. Beide Gegner und das Eren-Rig blicken also nach −Y. Meshy
hatte den Ork von Anfang an richtig geliefert; meine Drehung war der Fehler.

**Merksatz:** Blickrichtung wird gerendert, nicht gerechnet.

### T-Pose über geodätisches Wachstum — drei Versuche

| Versuch | Tor | Ergebnis |
|---|---|---|
| 1 | `x > 0,12 ∧ z > 0,95` | **Zacken.** Die Klauen hängen bis `z ≈ 0,75` herab, das Tor hat sie ausgeschlossen; sie blieben stehen, während die Hand sich hob. |
| 2 | kein Tor, Mischzone `d ∈ [0,60; 0,82]` | stetig, aber Drehpunkt aus dem Band bei `z = 1,27` — zu tief, Arm hob nur 25°. |
| 3 | kein Tor, Drehpunkt aus `d ∈ [0,60; 0,70] ∧ z > 1,35`, Richtung über den **Handschwerpunkt** | Arme waagerecht. Restneigung nach zwei Durchläufen **−4,05°** / **−3,01°** gegen Erens −2,7°. |

Dass gar kein Tor nötig war, ist ebenfalls gemessen: die ungetorte geodätische Kugel um die
Klauenspitze hat bis `d = 0,5` **kein** Vertex mit `x < 0,209` — Hand und Lendenschurz sind beim
Ork **nicht** verschweißt. Beim Goblin waren sie es; die 0,95-m-Kappe von damals war hier die
falsche Lehre.

Als Absicherung gegen genau den Zacken-Fehler läuft jetzt eine **Stetigkeitsprüfung**: für jede
Kante wird `|w(a) − w(b)|` gemessen, und kein Wert darf 0,35 überschreiten. Versuch 3 kam auf
0,23 bis 0,37; die 0,37 liegt an einzelnen 4-cm-Kanten im Lendenschurz, nicht an einer Kante
zwischen bewegtem und stehendem Netz.

### Was die T-Pose gekostet hat

**Ehrlich benannt:** die Mischzone der Schulterdrehung läuft geodätisch **um den Rumpf herum**.
Weil Teilgewichte auf Kreisbögen um den Schulterpunkt wandern, ist die Brust dabei leicht
**aufgebläht** und der Brustgurt gestreckt; die Deltamuskeln sind flacher als im Meshy-Original.
Vergleich: `art/probe/orc/orc2-front_minusY.png` (Original) gegen `orc-t4_front.png` (T-Pose).
Auf Spielentfernung — die Figur ist 2,24 m hoch und steht in einem dunklen Gewölbe — ist das
nicht zu sehen. Es ist trotzdem eine Abweichung, und sie ist der Preis dafür, dass die Bindepose
zur Ruhepose passt.

Der **Kopf** neigt sich 8,96° nach vorn, Erens Kopfknochen steht senkrecht. Ich habe das
**nicht** korrigiert: bei 9° liest sich die Neigung als drohende Haltung, und beim Goblin waren
es 40°, was tatsächlich gebrochen aussah. Der Versatz bleibt konstant über alle Clips.

### Textur

Meshy liefert Albedo, ORM und Normal je 2048². Auf **512²** herunterskaliert (Ordnung wie beim
Goblin) und in `art/textures/pq_orc_albedo.png`, `pq_orc_orm.png`, `pq_orc_normal.png` gelegt.

| Größe | vorher | nachher | Vorgabe |
|---|---:|---:|---|
| Albedo, mittlere lineare Luminanz | 0,0367 | **0,0367** | Farbton zur Palette, Helligkeit unverändert |
| Rauheit, Mittel | 0,6806 | **0,8318** | `STYLE.md:43` ≥ 0,55 |
| Metallizität, Mittel | 0,0195 | **0,0068** | `STYLE.md:43` ≤ 0,4 |

Der Palettenton `#667758` aus `EnemyType.ORC` ist mit Sättigung **0,55** eingemischt, nicht
voll: Meshys eigene Olivtöne sind schon nah an der Palette, und volle Einmischung hätte die
Hauttöne vereinheitlicht. Die Rauheit wurde auf `0,58 + 0,37·r` abgebildet, damit die Untergrenze
aus `STYLE.md` **im Bild** steht und nicht erst im Skalar.

### Ein Fehler im Bestand, den das aufgedeckt hat

Beim Eintragen des Orks in `AssetTest` stellte sich heraus: die ID-Liste auf dem Rechner stand
noch auf `{"hero", "mira", "eren"}`. **Der Goblin war dort nie eingetragen** — nur in meiner
Arbeitskopie. Abschnitt „Goblin integriert" nennt „6 Tests, mit `hero`, `mira`, `eren`,
`goblin`"; geprüft wurde auf dem Rechner tatsächlich nur bis `eren`. Die Datei ist jetzt
zusammengeführt und die Liste lautet `{"hero", "mira", "eren", "goblin", "orc"}`. Der Lauf
darunter ist der erste, der Goblin **und** Ork wirklich prüft.

### Bildrate — der Fallstrick beim Export

Der erste Export lieferte Attack1 mit **0,667 s** statt 0,52 s. Ursache: die Szene stand auf
**24 fps**, Erens Clips sind für **30 fps** gebacken. Nach `scene.render.fps = 30`:

| Clip | Länge | `AttackTimeline` | Toleranz ±0,034 |
|---|---:|---:|---|
| Attack1 | **0,533 s** | 0,52 s | ✔ |
| Attack2 | **0,567 s** | 0,58 s | ✔ |
| Attack3 | **0,767 s** | 0,78 s | ✔ |

Idle 3,533 · Walk 1,100 · Run 0,700 · Dodge 2,367 · Block 1,400 · Hit 0,700 · Death 2,300 ·
Cast 2,967.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-orc.log` |
| `AssetTest` | 6 Tests grün, ID-Liste jetzt mit `goblin` **und** `orc`: Joints, Clips, Tracks, Bindepose-Buffer, Angriffszeiten | ebd. |
| Ork lädt | `Hardware skinning engaged for OrcArmature` | `cowork-logs/smoke-orc.log` |
| Automatische Gewichte | **0** von 9 994 Vertices ohne Gewicht | Blender-Messung, Abschnitt oben |
| Blender-Ansichten | Ausrichtung, T-Pose, Seitenriss | `art/probe/orc/orc2-front_minusY.png`, `orc-t4_front.png`, `orc-t4_side.png` |

### Offen

* Die Gewichte sind **automatisch** (Heatmap), nicht gemalt. Schulter und Ellbogen werden unter
  starker Beugung dünn. Beim Goblin ist es dieselbe Einschränkung.
* Mehr als vier Joints je Vertex: der Exporter hat auf die vier stärksten normalisiert
  (`WARNING: There are more than 4 joint vertex influences`). Das ist die glTF-Grenze und kein
  Fehler, kann aber an den Klauen minimal anders aussehen als in Blender.
* Die **Brustverformung** aus der T-Pose (siehe oben) ist nicht zurückgenommen.
* Der **Buckel** des Meshy-Originals verschwindet, sobald ein Clip läuft — die Clips bestimmen
  die Haltung. Gleiches Verhalten wie beim Goblin.

---

## Wächter integriert — Protokoll und Belege (2026-09-12)

`src/main/resources/models/characters/warden.glb`, **2,61 MB**, 20 868 Dreiecke, 11 Clips,
**26 Joints**, ein Material `PQW_MI_Warden`, drei Texturen à 512². `EnemyType.WARDEN` skaliert
ihn auf ×1,2, also **2,34 m**.

**Meshy 6 Lite**, 10 Credits Netz + 10 Credits PBR, Guthaben danach **70**. Rohlieferung
105 838 Vertices / 180 086 Dreiecke; nach `remove_doubles(0,0005)` **89 840 Vertices**, eine
Komponente, **124 nicht-mannigfaltige Kanten** (Ork: 15, Goblin: 0) — die Rüstungsplatten sind
offene Schalen. Gestört hat es nichts, ist aber der schlechteste Wert der drei.

Ausrichtung: Meshy lieferte **nach −Y**, also richtig. Diesmal habe ich gar nicht gerechnet,
sondern nur gerendert (`art/probe/warden/warden-raw-front.png`). Das ist die Lehre aus dem Ork.

### Das Rezept vom Ork hat hier nicht funktioniert

Der Wächter trägt die Arme **dicht am Körper**: Gesamtbreite nur 0,74 m auf 1,95 m Höhe, die
Hände liegen neben den Beintaschen. Die geodätische Kugel um die Fingerspitze erreicht deshalb
schon bei `d = 0,45` die Mittellinie (`x = 0,06`) — Hand und Schürzenblech sind **verschweißt**.
Drei Versuche, in dieser Reihenfolge:

| Versuch | Verfahren | Ergebnis |
|---|---|---|
| 1 | Geodäte mit `x`/`z`-Tor | Kantensprung **1,0** — harter Schnitt, Zacken |
| 2 | Tor + 30 Runden Laplace-Glättung der Gewichte | Sprung auf 0,33–0,71 gedrückt, aber die Arme zogen einen **Umhang** aus Schürzengeometrie hoch |
| 3 | **Zylinderauswahl + Freischnitt** | sauber |

Versuch 2 ist der interessante Fehlschlag: die Glättung beseitigt den *Sprung*, aber nicht das
*Problem*. Wenn Hand und Schürze dieselben Vertices teilen, zieht jede Drehung die Schürze mit,
egal wie stetig das Gewichtsfeld ist.

### Versuch 3: erst messen, dann schneiden

Die Trennung ist **gemessen**, nicht geschätzt. Für jeden Vertex der Radius `r` zur Armachse
Schulter → Hand, in 3-cm-Klassen, dazu der mittlere `x`-Wert der Klasse:

| r | Anzahl | mittleres x | was das ist |
|---:|---:|---:|---|
| 0,00 | 7 | 0,328 | Handmitte |
| 0,03 | 139 | 0,318 | Hand |
| 0,06 | 260 | 0,287 | Unterarm, Stulpe |
| 0,09 | 91 | 0,235 | Oberarm |
| **0,12** | **66** | **0,160** | **Tal — hier liegt die Grenze** |
| 0,15 | 178 | 0,150 | Beintasche |
| 0,18 | 156 | 0,134 | Rumpf |

Der Arm endet bei `r ≈ 0,09`, ab `0,12` beginnt der Rumpf, und zwischen beiden liegt ein
Zählminimum. Gewählt `R(s) = 0,11 + 0,10 · max(0, s − 0,18)`, damit die Stulpe unten mehr Platz
hat.

Dann: **Flutfüllung** von der Hand aus innerhalb des Zylinders (692 bzw. 681 Vertices),
`split_edges` auf den 123 bzw. 109 Randkanten — damit ist der Arm ein **eigenes Flächenstück** —,
**starre** Drehung um 75,75° bzw. 75,69° auf Erens Ruherichtung, und zum Schluss
`bridge_loops`, das die Achselhöhle mit 414 bzw. 429 Flächen wieder schließt. Kein Gewichtsfeld,
kein Sprung, nichts wird gezogen. Dreieckszahl steigt dadurch von 19 997 auf **20 868**; das sind
4 % über dem Budget aus `STYLE.md` und die Gegenleistung für eine intakte Schürze.

**Merksatz:** wenn zwei Körperteile verschweißt sind, hilft kein Gewichtsfeld — dann wird
geschnitten und wieder zugenäht.

### 18 Vertices ohne Gewicht — ein Fehler, der sichtbar geworden wäre

`ARMATURE_AUTO` ließ **18** von 9 879 Vertices ohne Gewicht. Ein ungewichteter Vertex hängt beim
Skinning am Modellursprung, also wären beim ersten Clip 18 Zacken vom Hüftbereich zum Fußpunkt
gefahren. Sie lagen alle in der Achsel bei `z ≈ 0,91` — dort, wo `bridge_loops` neue Flächen
eingezogen hat, die es bei der automatischen Gewichtung noch nicht gab. Behoben, indem jeder
dieser Vertices die normierten Gruppen seines **nächsten gewichteten Nachbarn** bekommt
(Abstände 0,033 bis 0,056 m). Danach **0** ungewichtete Vertices.

Für Aschenrufer und König steht das jetzt als Pflichtprüfung im Skript: `rig()` gibt
`unweighted` zurück, und der Wert muss 0 sein.

### Textur

Meshy lieferte den Wächter fast **schwarz**: mittlere lineare Luminanz der Albedo **0,0213**. In
einem dunklen Gewölbe wäre das eine Silhouette ohne Form. Angehoben mit Faktor **2,583** auf
**0,0550** und den Palettenton `#87939c` aus `EnemyType.WARDEN` mit Sättigung 0,60 eingemischt.

| Größe | vorher | nachher | Vorgabe |
|---|---:|---:|---|
| Albedo, mittlere lineare Luminanz | 0,0213 | **0,0550** | lesbar im Gewölbe |
| Rauheit | 0,4751 | **0,7558** | `STYLE.md` ≥ 0,55 |
| Metallizität | 0,4788 | **0,1676** | `STYLE.md` ≤ 0,4 |

Meshys Metallizität von 0,479 lag **über** der Grenze aus `STYLE.md`; 0,168 ist deutlich matter
als echte Plattenrüstung, entspricht aber der Absicht in `STYLE.md:38–45`, Glanz in die Albedo zu
malen statt in den Metallkanal.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-warden.log` |
| `AssetTest` | 6 Tests grün, ID-Liste `{hero, mira, eren, goblin, orc, warden}` | ebd. |
| Clip-Längen | Attack1 **0,533** · Attack2 **0,567** · Attack3 **0,767** s | ebd. |
| Gewichte | **0** von 9 879 Vertices ohne Gewicht | Blender-Messung |
| Blender-Ansichten | Ausrichtung, gezogene Fehlversuche, fertiger Freischnitt | `art/probe/warden/warden-raw-front.png`, `warden-cyl-front.png`, `warden-cut-front.png` |

### Offen

* Der **Freischnitt** ist eine echte Naht. `bridge_loops` zieht eine gerade Manschette ein; unter
  der Schulterplatte ist sie verdeckt, an der Achselinnenseite ist sie eine sichtbar glatte
  Fläche ohne Nieten. Wer das poliert, malt dort Gewichte von Hand.
* Der **Waffenrock** hängt bis zu den Knöcheln und ist starr gebunden. Bei Walk und Run wird er
  durch die Beine schneiden. Das ist beim Goblin-Lendenschurz dieselbe Einschränkung, hier aber
  wegen der Länge deutlicher.
* **124 nicht-mannigfaltige Kanten** sind unberührt geblieben.
* Der Helmkamm reicht bis auf 1,95 m, der Körper endet darunter. Die Figur ist damit effektiv
  etwas kleiner als 1,95 m, weil der Kamm mitskaliert wurde. Erens `Head`-Knochen reicht bis
  1,952 und deckt Kamm und Schädel trotzdem ab.

---

## Aschenrufer und König integriert — alle fünf Gegner stehen (2026-09-12)

| Datei | Größe | Dreiecke | Joints | Material | Ingame |
|---|---:|---:|---:|---|---:|
| `characters/shaman.glb` | 2,43 MB | 21 741 | 26 | `PQS_MI_Shaman` | ×1,0 = 1,95 m |
| `characters/king.glb` | 2,43 MB | 20 375 | 26 | `PQKO_MI_King` | ×1,85 = 3,61 m |

Beide über **Meshy 6 Lite**, je 10 Credits Netz + 10 Credits PBR. Beide blicken von Meshy aus
nach **−Y**, also richtig; geprüft am Render, nicht gerechnet.

### Der Freischnitt ist jetzt das Standardverfahren

Beim Wächter war er die Notlösung, bei diesen beiden war er von Anfang an richtig. Das
Radius-Histogramm zur Armachse (`arm_radius_profile()`) zeigt bei allen drei Figuren dasselbe
Bild — ein dichter Armkern, ein Zählminimum, dann der Rumpf:

| Figur | Armkern bis r | Tal bei r (Anzahl) | Gewählt `r_base` |
|---|---:|---|---:|
| Wächter | 0,09 | 0,12 (66) | 0,11 |
| Aschenrufer | 0,09 | 0,15 (90) | 0,125 |
| Ork-König | 0,09 | 0,12 (31) und 0,15 (26) | 0,13 |

Beim König ist das Tal mit 31 und 26 Vertices am deutlichsten — der Arm steht schon in der
Rohlieferung frei vom Rumpf (`weld_check` meldet den nächsten Punkt bei `x = 0,197`, also keine
Verschweißung). Trotzdem habe ich geschnitten statt das Gewichtsfeld zu nehmen: das Verfahren ist
belegt, und ein starr gedrehter Arm kann nichts mitziehen.

Drehwinkel: Aschenrufer 71,05° / 70,84°, König 66,51° / 66,53°. Beim Aschenrufer kommen die
weiten Ärmel als Stoff mit hoch, was für einen Roben­träger richtig aussieht.

### Ein Fehler, der ohne Messung durchgegangen wäre: der König war gar nicht gebunden

`ARMATURE_AUTO` meldete beim König **0 von 9 998 Vertices gewichtet** und Blender schrieb dazu
nur eine Warnung in die Konsole:

```
Warnung: Bone Heat Weighting: failed to find solution for one or more bones
```

`verify` wäre daran **nicht** gescheitert — `AssetTest` prüft, dass Bindepose-Buffer vorhanden
sind, nicht dass sie sinnvolle Werte tragen. Die Figur wäre im Spiel als Klumpen am
Modellursprung erschienen.

Ursache: Blenders Knochen-Wärmeverteilung löst **pro zusammenhängender Komponente**. Der
Freischnitt hatte sechs Splitter hinterlassen — 32, 27, 8, 4, 4 und 4 Vertices, alle um
`(±0,30; −0,17; 0,85)`, also dort, wo vorher die Hand stand. Eine Insel ohne Knochen darin lässt
den ganzen Löser scheitern, nicht nur diese Insel.

Behoben mit `remove_islands()`: alles außer der größten Komponente, das unter 120 Vertices liegt,
fällt weg. Danach 9 919 Vertices, **26 Gruppen, 0 ungewichtet**. Die Funktion steht jetzt im
Skript und gehört nach jedem `cut_arms()` aufgerufen.

### Textur

| Figur | Albedo vorher | Faktor | Albedo nachher | Rauheit | Metallizität | Ton |
|---|---:|---:|---:|---:|---:|---|
| Aschenrufer | 0,0319 | 1,566 | **0,0500** | 0,815 | 0,001 | `#79608f`, Sättigung 0,55 |
| Ork-König | 0,0335 | 1,253 | **0,0420** | 0,809 | 0,019 | `#8a5d4e`, Sättigung 0,50 |

Beide liegen damit innerhalb `STYLE.md:43`. Der König ist etwas heller angesetzt als der Ork
(0,042 gegen 0,037), weil er mit 3,61 m im Thronsaal die Blickachse füllt.

Das Material des Königs heißt `PQKO_MI_King`, nicht `PQK_MI_King` — `PQK_` ist schon vom
Dungeon-Kit belegt (`PQK_Wall`, `PQK_Iron`, `PQK_Rock`, `PQK_Cloth`, `PQK_Carpet`).

### Ein Renderfehler meinerseits, kein Modellfehler

Meine ersten Kontrollbilder von König und Aschenrufer zeigten zusammengeknüllte Figuren. Ursache
war **mein Ablauf**, nicht das Modell: ich hatte nach `export()` gerendert, und da stehen alle
elf NLA-Spuren aktiv — die Posen überlagern sich. Mit `armature.data.pose_position = "REST"`
zeigen `art/probe/king/king-bind-front.png` und `art/probe/shaman/shaman-bind-front.png` die
tatsächliche Bindepose, und die ist korrekt. Für Ork und Wächter hatte ich vor dem Export
gerendert, deshalb war es dort nicht aufgefallen.

### Verifikation — alle acht Charaktere in einem Lauf

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-alle-gegner.log` |
| `AssetTest` | 6 Tests grün über `{hero, mira, eren, goblin, orc, warden, shaman, king}` — Joints, alle 11 Clips, Tracks, Bindepose-Buffer, Angriffszeiten | ebd. |
| Smoke-Run | alle **33 Stufen**, `smoke-ok.txt`, 13 Screenshots, keine Exception | `cowork-logs/smoke-alle-gegner.log` |
| Alle fünf Gegner laden | `Hardware skinning engaged` — Goblin 26×, Ork 13×, Wächter 6×, Aschenrufer 9×, König 3× | ebd. |
| Bindeposen | T-Pose, Ausrichtung, Proportionen | `art/probe/shaman/shaman-bind-front.png`, `art/probe/king/king-bind-front.png` |

### Credit-Abrechnung, vollständig

| Schritt | Kosten | Guthaben danach |
|---|---:|---:|
| Start dieser Sitzung | — | 110 |
| Ork: Netz + PBR | 20 | 90 |
| Wächter: Netz + PBR | 20 | 70 |
| **Aschenrufer, erster Versuch: von Meshys Inhaltsfilter abgelehnt** | **10** | **60** |
| Aschenrufer: Netz + PBR | 20 | 40 |
| König: Netz + PBR | 20 | **20** |

Der abgelehnte Lauf ist ein echter Verlust von 10 Credits. Meshy meldete „Inhalt eingeschränkt"
mit Verweis auf die NSFW-Richtlinie. Auslöser war mit hoher Wahrscheinlichkeit das Wort
**„fetishes"** im Prompt (gemeint waren Ritualanhänger); mit „carved wooden charms" lief derselbe
Prompt durch. Der Endstand von 20 Credits passt nur auf, wenn der abgelehnte Lauf später
rückerstattet wurde — 110 − 20 − 20 − 10 − 20 − 20 = 20, also **ohne** Rückerstattung. Die
10 Credits sind weg.

**Lehre für weitere Prompts:** Begriffe mit doppelter Bedeutung vermeiden. Der Filter prüft den
Prompt, und ein abgelehnter Lauf kostet trotzdem.

### Offen

* Der **Freischnitt** hinterlässt bei allen drei Figuren eine glatte Manschette in der Achsel.
  Beim Aschenrufer sind die Übergänge zwischen Ärmel und Rumpf als kantige Streifen sichtbar; auf
  Spielentfernung lesen sie sich als Stofffalten, aus der Nähe sind sie Geometrie ohne Absicht.
* Dreiecksbudget: Aschenrufer **21 741** und König **20 375** liegen über den 20 000 aus
  `STYLE.md`. Die Mehrflächen kommen aus `bridge_loops`.
* Die **Roben und Schürzen** von Aschenrufer, Wächter und König sind starr gebunden und werden
  bei Walk und Run durch die Beine schneiden.
* Gewichte sind bei allen fünf Gegnern **automatisch**, nicht gemalt.
* `docs/ASSETS.md` und `docs/VERIFICATION.md` sind noch nicht auf den neuen Stand gebracht.

---

## Props geliefert — und ein Befund im Ladepfad (2026-09-12)

Sieben Prop-Module unter `models/props/`, alle in der Engine nachgemessen
(`cowork-logs/kitprobe-props.log`):

| ID | Maße glTF (m) | Dreiecke | Materialien | Pivot |
|---|---|---:|---|---|
| `chest.glb` | 1,34 × 0,84 × 0,92 | 72 | `PQP_Wood`, `PQK_Iron` | unten mittig |
| `shrine.glb` | 1,40 × 0,52 × 1,40 | 48 | `PQP_Stone` | unten mittig |
| `rune.glb`, `seal.glb`, `lore.glb` | 1,30 × 1,20 × 0,90 | je 36 | `PQP_Stone` | unten mittig |
| `throne.glb` | 2,60 × 5,10 × 2,30 | 82 | `PQP_Stone`, `PQK_Iron` | unten mittig |
| `portal.glb` | 3,90 × 5,25 × 0,96 | 72 | `PQP_Stone` | unten mittig |

Kein Meshy-Credit dafür verbraucht — alles parametrisch in Blender gebaut, wie der Dungeon-Kit.
Guthaben bleibt bei **20**.

Materialziele, aus den Platzhaltern hergeleitet und nachgemessen getroffen:

| Material | Herleitung | Albedo-Ziel | Rauheit |
|---|---|---:|---:|
| `PQP_Stone` | `pbr("stone", 0x94918b, .85f, 0)` → 0,5100 × lum(`#94918b`) | **0,1450** | **0,7185** |
| `PQP_Wood` | `pbr("", 0x614b36, .75f, .1f)` → ohne Map ist die Farbe selbst das Ziel | **0,0790** | **0,7500** |

Die Truhenbänder tragen `PQK_Iron` statt des Platzhalter-`gold`. Das ist eine **Abweichung**:
`gold` ist wärmer. Ich habe kein zweites Metallset dafür angelegt, weil Eisenbänder an einer
Holztruhe plausibel sind und `PQK_Iron` schon innerhalb `STYLE.md` liegt.

### Der Befund: ein Modul hätte jedem Interaktionsobjekt das Leuchten genommen

`WorldView.buildObject` baut zuerst den Platzhalter und ersetzt ihn dann:

```java
if (imported != null) {
  n.detachAllChildren();
  n.attachChild(imported);
}
```

`detachAllChildren()` entfernt **auch** die Glühkristalle — bei Schrein, Rune, Siegel, Chronik
und Thron — und beim Tor den transparenten `PortalVeil`. Diese Teile tragen das `glow`-Material,
das den **Bloom-Pass** speist; ein glTF-Material kann das nicht erzeugen. Ein geliefertes
`shrine.glb` hätte den Schrein also in ein Stück Stein ohne Leuchtmarke verwandelt — und genau
diese Marke ist im dunklen Gewölbe der Hinweis, dass dort etwas zu tun ist. Der Fehler wäre weder
`verify` noch dem Smoke-Run aufgefallen; beide prüfen nicht, ob ein Objekt leuchtet.

Behoben in `WorldView.buildObject`: die leuchtenden Kinder werden vor dem Tausch gesammelt und
danach wieder angehängt. **Ausnahme `CRYSTAL`** — dort *ist* die Leuchtkugel das ganze Objekt, ein
Modul ersetzt sie also; ohne diese Ausnahme hätte `crystal.gltf` plus wieder angehängte Kugel
einen doppelten Kristall ergeben.

Deshalb sind Schrein (0,52 m) und Altar (1,20 m) **niedriger** als die Zielmaße in
`docs/ASSETS.md` von 1,8 bzw. 1,2 m: die Kristalle kommen nicht mehr aus dem Modul.

### Vier von dreizehn Screenshots zeigen die Kamera in der Geometrie

Aufgefallen beim Sichttest: `pentagon-4` bis `pentagon-7` sind rund 495 KB groß statt 2 MB und
zeigen aus allen vier betroffenen Gebieten dasselbe Bild — Wandtextur aus Nahdistanz mit HUD
darüber. Ich habe das **A/B gemessen**, nicht vermutet: mit den sieben Prop-Modulen
beiseitegeschoben (`smoke-props-aus.log`) sind dieselben vier Bilder weiterhin 495 KB, während
`pentagon-2`, `-8` und `-9` mit 2,0 bis 2,1 MB normal rendern. Beide Läufe gehen über alle
**33 Stufen** und schreiben `smoke-ok.txt`.

Damit sind **die Props ausgeschlossen**, und die Änderung an `buildObject` ebenfalls: ohne
Moduldatei läuft der neue Zweig gar nicht. Die verbleibende Erklärung ist die bereits
dokumentierte Aufnahmezeit direkt nach `loadRegion` — dieselbe, die den Helden in der T-Pose
zeigt. Was sich geändert hat, ist die Umgebung: die Kit-Wände füllen die **ganze** Zelle (2,80 m
Tiefe) und es gibt jetzt Decken, also ist die Stelle, an der die Kamera in diesem Moment steht,
inzwischen von Geometrie umschlossen. Das ist eine Erklärung mit Beleg für den Ausschluss, aber
**nicht** die nachgewiesene Ursache. Zu messen wäre die Kameraposition zum Aufnahmezeitpunkt.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| Props in der Engine | 7 Module laden, Maße und Pivot unten mittig bestätigt | `cowork-logs/kitprobe-props.log` |
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-props.log` |
| Smoke-Run | alle **33 Stufen**, `smoke-ok.txt`, 13 Screenshots, keine Exception | `cowork-logs/smoke-props.log` |
| A/B ohne Props | alle 33 Stufen, `smoke-ok.txt`, dieselben vier dunklen Aufnahmen | `cowork-logs/smoke-props-aus.log` |
| Codeänderung | kompiliert sauber, `verify` grün | ebd. |

### Offen

* Die Prop-Module sind **Kastenkompositionen**, keine modellierte Kunst: gestufte Sockel, Bänder,
  Fasen. Sie sitzen maßgenau auf den Platzhaltern und tragen echte PBR-Maps, aber eine Truhe mit
  Schloss, Scharnieren und Beschlag ist etwas anderes.
* Der **Bloom** der Interaktionsobjekte hängt weiter an den prozeduralen Kristallen. Wer sie
  ersetzen will, braucht einen Weg, `glow` aus einem Modul zu erzeugen — den gibt es nicht.
* `trap`, `npc` und `prisoner` sind vom Modulpfad ausgeschlossen (`buildObject`), dort ist also
  kein Austausch möglich, ohne den Code zu ändern.
* Vier von dreizehn Screenshots bleiben unbrauchbar, siehe oben.

---

## Die vier dunklen Screenshots: Ursache gemessen (2026-09-12)

Nachtrag zum Abschnitt oben. Die Vermutung dort — „Aufnahmezeit direkt nach `loadRegion`" — war
**falsch**. Gemessen ist etwas anderes, und es ist ein echter Fehler im Spiel, nicht im Test.

### Was ausgeschlossen ist

| Verdacht | Messung | Ergebnis |
|---|---|---|
| Aufnahmezeit / ein Frame zu früh | `SmokeScenario.update`: `if (timer < 2.5f) return;` | **2,5 s** je Stufe — kein Timing-Artefakt |
| `--fast` verkürzt Stufen | `GameApplication:49`, `this.highQuality = !fast` | setzt nur das Grafikprofil |
| Die Prop-Module | A/B-Lauf mit den sieben Dateien beiseite (`smoke-props-aus.log`) | dieselben vier dunklen Bilder |
| Die Änderung an `buildObject` | ohne Moduldatei läuft der Zweig nicht | ausgeschlossen |
| `GeometryBatchFactory` zerstört die Strahlkollision | `RayProbe` gegen `kit_wall_face.glb`, gebatcht und ungebatcht, gedreht und ungedreht | **4 Treffer in allen vier Varianten** — Batching ist unschuldig (`cowork-logs/rayprobe.log`) |

### Was gemessen ist

Temporäre Instrumentierung in `SmokeScenario` (danach wieder entfernt, Quelle ist sauber),
zwei Läufe: `cowork-logs/smoke-caminstr.log` und `smoke-hitinstr.log`.

Kameralage bei den betroffenen Stufen, identisch in CRYPT, CAVERNS und PRISON:

```
[CAM] step=7 region=CRYPT   player=(42.0, 0.0, 70.0) camera=(42.0, 4.945, 76.0) dist=7.775
[CAM] step=8 region=CAVERNS player=(42.0, 0.0, 70.0) camera=(42.0, 4.945, 76.0) dist=7.775
[CAM] step=9 region=PRISON  player=(42.0, 0.0, 70.0) camera=(42.0, 4.945, 76.0) dist=7.775
```

`dist` ist der **volle** Sollabstand — die Kamera wurde also nie eingezogen. Und der Strahl von
der Kamera zum Spieler zeigt, warum:

```
[HIT] step=7 node=root         n=1 first=portalMesh_0 at=0.780
[HIT] step=7 node=occluders    n=0 first=-            at=-1.0
[HIT] step=7 node=decor        n=0 first=-            at=-1.0
[HIT] step=7 node=interactives n=1 first=portalMesh_0 at=0.780
```

**0,78 m vor der Kamera steht das Eingangstor**, und es hängt im Knoten `interactives`.
`PlayerController.camera` prüft seine fünf Kollisionsstrahlen aber nur gegen `occluders`
(`PlayerController.java:190`, `occluders.collideWith(ray, results)`). Das Tor ist für die Kamera
deshalb nicht vorhanden.

### Warum das ein Spielfehler ist, kein Testartefakt

`CampaignState.loadRegion` setzt den Spieler bei `savedPosition == false` auf
`layout.spawnX()/spawnZ()` und `yaw = FastMath.PI` (`CampaignState.java:139–143`). Der Sollplatz
der Kamera ist daraus 6,0 m in **+Z** und 3,5 m hoch (`PlayerController.java:173–177`). Der
Spawn liegt in jedem Gebiet **unmittelbar am Eingangstor**, und der Sturz des Tores reicht von
`y = 4,55` bis `5,25` — die Kamera landet bei `y = 4,945` also **im Sturz**. Wer ein Gebiet
betritt, sieht in diesem Moment Stein statt Raum.

Das gilt **unabhängig von meinen Modulen**: der Platzhaltersturz ist `box("GateLintel", 1.85f,
.35f, .4f)` bei `y = 4.9`, also genau dasselbe Band 4,55 … 5,25. Der Fehler ist Bestand und
älter als der Dungeon-Kit. Er war nur nie belegt, weil `pentagon-4` bis `-7` vorher niemand
aufgehellt hat — bei Normalhelligkeit sieht das Bild nach einem dunklen Raum aus, der Mittelwert
liegt bei 17 von 255.

### Der Fehler ist nicht behoben

Die Korrektur wäre klein — die Strahlen zusätzlich gegen `interactives` prüfen —, aber sie
verändert **Kameraverhalten**, nicht Assets: danach zieht auch eine Truhe, ein Altar oder ein
Glühkristall die Kamera ein, und der transparente `PortalVeil` läge ebenfalls im Weg. Ob das
gewollt ist, ist eine Spielgefühlentscheidung und keine, die ich ohne Rückfrage treffe. Der
Befund ist belegt, die Quelle ist unverändert, die Instrumentierung ist entfernt.

Mögliche Varianten, falls die Entscheidung fällt:

1. `interactives` mitprüfen und Geometrien im `Transparent`-Bucket überspringen — trifft das Tor,
   lässt den Schleier aus.
2. Den Spawn ein bis zwei Zellen vom Tor weg verlegen — ändert Layout und Questwege.
3. Beim Gebietswechsel die Kamera einmal auf Mindestabstand setzen und nach außen einfahren.

---

## Schlussstand der Assetliste (2026-09-12)

**Alles, was einen Ladepfad hat, ist geliefert und integriert.** 20 Zeilen stehen auf
`integriert: ja`:

| Gruppe | Anzahl | Stand |
|---|---:|---|
| Charaktere (`hero`, `mira`, `eren`) | 3 | integriert |
| Gegner (`goblin`, `orc`, `warden`, `shaman`, `king`) | 5 | integriert |
| Dungeon-Kit (`props/kit_*`) | 15 Module, 1 Zeile | integriert |
| Props (`chest`, `shrine`, `portal`, `rune`, `seal`, `lore`, `throne`) | 7 | integriert |
| Kristall (`props/crystal.gltf`) | Bestand | integriert |

Fünf Zeilen bleiben auf **`kein Ladepfad`**, und das ist keine fehlende Arbeit an Assets,
sondern eine Eigenschaft des Codes. Ein Modell dafür zu bauen wäre verschwendet, weil es
nichts lädt:

| Zeile | Warum kein Ladepfad | Was fehlen würde |
|---|---|---|
| Fallenplatte + Stacheln | `WorldView.buildObject` nimmt `Kind.TRAP` ausdrücklich von der Modulschleife aus | eine Zeile in der Ausnahmeliste |
| Waffenmodelle (5) | `ItemCatalog` kennt nur Werte; das Schwert ist fest im Rig am `WeaponSocket` | Mesh-ID am Item und ein `assets.model`-Aufruf beim Anlegen |
| Rüstungsmodelle (4) | rein statistisch, keine Sichtbarkeit im Code | austauschbare Körperteile oder Overlay-Meshes am Charakter |
| Verbrauchsgüter, Schlüssel, Relikt | nur HUD-Text | Weltdarstellung überhaupt |
| Projektil-Orb, Trefferfunken, Telegraph | Geometrie prozedural in `ProjectileSystem`, `Effects`, `Enemy` | Partikel- oder Mesh-Pfad |

Das sind Codeerweiterungen, keine Blender-Arbeit. Nach dem COWORK-BRIEFING schreibe ich keinen
Ladecode dazu; die eine Ausnahme war die Bloom-Korrektur in `buildObject`, weil dort ein
geliefertes Modul sonst den Bloom-Pass zerstört hätte.

**Die Falle ist der Sonderfall, der es wert ist:** `Kind.TRAP` von der Ausnahmeliste zu nehmen
kostet eine Zeile und macht `props/trap.glb` möglich. Die Fallenplatte hat einen
`glow`-Warnmarker (`TrapWarning`), der mit der Korrektur von heute erhalten bliebe. Das ist der
nächste naheliegende Schritt, wenn der Ladepfad erweitert werden darf.

---

## Drei Fehler, die der Sichttest des Nutzers aufgedeckt hat (2026-09-12)

Kevin hat gemeldet: der Held steht beim Spielen dauerhaft in der T-Pose, und trägt er nicht
eigentlich ein Schwert? Beides stimmte. Ich hatte die T-Pose auf den Screenshots vorher als
Aufnahmezeit abgetan — das war für die Gegner richtig und für den Helden **falsch**.

### 1. `hero.glb` hatte flache Animationen

Gemessen mit `ClipProbe` gegen die gelieferten Dateien (`cowork-logs/clipprobe.log`):

| Figur | Clip | Tracks | konstant | bewegt | größter Ausschlag |
|---|---|---:|---:|---:|---|
| **hero (vorher)** | Idle | 66 | **66** | **0** | — |
| **hero (vorher)** | Walk | 66 | **66** | **0** | — |
| **hero (vorher)** | Attack1 | 66 | **66** | **0** | — |
| mira | Idle | 66 | 46 | 20 | 48,5 Grad auf Head |
| eren | Attack1 | 66 | 45 | 21 | 113,1 Grad auf Hand.R |
| goblin | Walk | 26 | 4 | 22 | 45,3 Grad auf Shin.R |

Die Keyframe-Zahlen waren korrekt (107 / 34 / 17), jeder Track hielt aber in jedem Key denselben
Wert. Deshalb hat `AssetTest` nichts gemerkt: der Test prüft `getTracks().length > 0` und
`getLength() > 0`, nicht ob sich etwas **bewegt**.

Ursache in Blender: `Armature.004` — das Rig des Helden — hatte NLA-Spuren mit den richtigen
Namen, aber **alle Strip-Listen waren leer**: `["Idle", [], false]`. Bei Mira hängt dort
`MiraIdle`, bei Eren `EREN_Idle`. Mit `export_force_sampling=True` hat der Exporter also
66-mal die Ruhepose abgetastet.

Behoben: die Actions `Idle` … `Cast` (je 660 Fcurves) in die NLA-Spuren gehängt, Szene auf
30 fps, neu exportiert. Nachher: Idle 20 bewegte Tracks, Walk 35, Attack1 21 mit 113,1 Grad auf
`Hand.R` — dasselbe Profil wie Eren. Datei 6 234 020 auf 5 479 428 Byte.
Beleg `cowork-logs/clipprobe-hero-fix.log`.

**Lehre:** „Clip vorhanden" ist nicht „Clip animiert". Ein Test, der Länge und Trackzahl prüft,
fällt auf einen flach abgetasteten Export herein.

### 2. Kein Charakter trug eine Waffe

`WeaponSocket`, Klinge, Parierstange, `ShieldSocket` und Schildplatte wurden in
`CharacterFactory.placeholder(...)` gebaut. Diese Methode ist aber nur der **Rückfall**:

```java
Spatial loaded = assets.model("characters/" + id, () -> placeholder(color, king));
```

Sobald `hero.glb` im Klassenpfad liegt, läuft `placeholder` nicht mehr — und damit verschwanden
Schwert und Schild bei **jeder** Figur. Gemessen mit `HeroProbe`: `WeaponSocket children=0` bei
hero, goblin und orc. Das ist dieselbe Fehlerklasse wie die Glühkristalle bei den Props: eine
gelieferte Datei nimmt dem Platzhalter etwas weg, das nicht zum Platzhalter gehört.

Behoben: die Sockel sind aus `placeholder` heraus in eine eigene Methode `equip(skin, tint)`
gewandert, die `create` in **beiden** Fällen aufruft. Sie prüft vorher, ob das Rig `Hand.R`
bzw. `Hand.L` überhaupt hat, damit ein späteres Rig ohne diese Joints nicht abstürzt.
Nachher: `WeaponSocket children=1  ShieldSocket children=1` bei hero, mira, goblin und king.

### 3. Kamera: Methode A umgesetzt

`PlayerController.camera` prüft seine fünf Strahlen jetzt gegen **`occluders` und
`interactives`** (variadisch, damit weitere Knoten ohne Signaturänderung dazukommen können).
Geometrien im `Transparent`-Bucket werden übersprungen — das ist genau der `PortalVeil`, also
der Durchgang, durch den man laufen soll, und kein Hindernis.

Wirkung, gemessen an der Dateigröße der Screenshots (eine Kamera in der Wand liefert ein
fast einfarbiges Bild):

| Aufnahme | vorher | nachher |
|---|---:|---:|
| `pentagon-4` | 498 178 | **2 100 126** |
| `pentagon-5` | 495 753 | **1 934 291** |
| `pentagon-6` | 495 762 | **1 934 415** |
| `pentagon-7` | 498 262 | **2 098 359** |

Alle vier zeigen jetzt den Raum. Sichttest: `pentagon-6` zeigt das Kettenverlies mit Pfeilern,
Teppich, Goblin und Ork — und den Helden mit Schwert und Schild.

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| `mvn -B verify` | **BUILD SUCCESS, 50 Tests** | `cowork-logs/verify-kamera-a.log` |
| Smoke-Run | alle **33 Stufen**, `smoke-ok.txt`, 13 Screenshots, keine Exception | `cowork-logs/smoke-kamera-a.log` |
| Clips bewegen sich | hero Idle 20 / Walk 35 / Attack1 21 bewegte Tracks | `cowork-logs/clipprobe-hero-fix.log` |
| Waffe und Schild | `children=1` bei hero, mira, goblin, king | `cowork-logs/heroprobe-final.log` |
| Kamera | vier Aufnahmen von ~495 KB auf 1,9–2,1 MB | `cowork-logs/smoke-kamera-a.log` |

### Offen

* `AssetTest` prüft weiterhin **nicht**, ob ein Clip Bewegung enthält. Eine Zusicherung
  „mindestens ein Track dreht sich um mehr als ein halbes Grad" hätte diesen Fehler gefangen
  und würde ihn beim nächsten Export wieder fangen. Das ist Testcode, kein Asset.
* Mira und Eren haben in `Idle` 46 von 66 konstanten Tracks. Das ist normal für eine Atemschleife
  (Finger und Zehen ruhen), aber nicht nachgeprüft.
* Das Platzhaltermaterial `steel` liegt mit Rauheit 0,38 und Metallizität 0,72 **außerhalb**
  `STYLE.md` (>= 0,55 / <= 0,4). Das betrifft Klinge und Schienen und ist Bestand.
* Auf den Aufnahmen direkt nach `loadRegion` stehen die Figuren weiterhin in der Bindepose. Das
  ist jetzt belegt harmlos: Stufe 6 des Smoke-Harness ruft `loadRegion` **vor**
  `screen(PLAYING)`, und `loadRegion` endet mit `pause(app.mode() != PLAYING)` — der Composer
  startet also mit Geschwindigkeit 0 und läuft erst nach dem Moduswechsel an.

---

## Die Falle bekommt kein Modul — und warum (2026-09-12)

Ich hatte im Schlussstand geschrieben, `Kind.TRAP` aus der Ausnahmeliste zu nehmen koste „eine
Zeile". **Das war falsch, und zwar weil ich nur `buildObject` gelesen hatte.** Die Stacheln werden
nicht dort bewegt, sondern in `CampaignState.traps` (Zeile 277–280):

```java
Node node = world.objects.get(spec.id());
for (Spatial child : node.getChildren())
  if (child.getName().equals("Spike"))
    child.setLocalTranslation(child.getLocalTranslation().x, active ? .5f : -.5f, ...);
```

Die Animation läuft über die **direkten Kinder** des Objektknotens, gesucht nach dem Namen
`"Spike"`. Ein importiertes Modul hat solche Kinder nicht — ein glTF-Import legt seine Meshes eine
Ebene tiefer und unter Blender-Namen ab. Ein `props/trap.glb` hätte also:

* die Stachelbewegung **stillgelegt** (die Schleife findet nichts), und
* den Schaden **trotzdem ausgelöst**, weil er aus Phase und Spielerposition kommt, nicht aus der
  Geometrie.

Das Ergebnis wäre eine unsichtbar zuschlagende Falle, also schlechter als der Platzhalter. Die
Zeile bleibt auf `kein Ladepfad`. Wer die Falle austauschen will, muss vorher die Stachelbewegung
umbauen — etwa auf eine rekursive Suche oder auf einen benannten Unterknoten, den das Modul
mitbringt.

---

## Testlücke geschlossen: ein Clip muss sich bewegen (2026-09-12)

Der flache Held ist durch `AssetTest` gerutscht, weil der Test `getTracks().length > 0` und
`getLength() > 0` prüft — beides war erfüllt. Jetzt prüft er zusätzlich, dass in jedem Clip
mindestens ein Joint sich um **mehr als ein halbes Grad** gegen den ersten Keyframe dreht.

Die Schwelle ist gemessen, nicht geraten. Ausschlag je Clip über alle acht Charaktere
(`cowork-logs/allclipsprobe.log`, Format `bewegte Tracks / größter Ausschlag in Grad`):

| Clip | Held / Mira / Eren | Gegner | Ausschlag |
|---|---|---|---:|
| Idle | 20 | 20 | 48 |
| Walk | 34–35 | 22 | 45–48 |
| Run | 22 | 22 | 92–95 |
| Attack1 | 21–22 | 21 | 97–113 |
| Attack2 | 22 | 22 | 165–174 |
| Attack3 | 22 | 22 | 103–105 |
| Dodge | 42 | 22 | 175 |
| **Block** | **25** | **12** | **2–3** |
| Hit | 47 | 21 | 65 |
| Death | 22 | 22 | 119 |
| Cast | 22 | 22 | 100 |

`Block` ist der schwächste Fall: eine gehaltene Haltung, die nur 2 bis 3 Grad wippt. Eine
Schwelle von 5 Grad hätte den Test also fälschlich rot gemacht. Ein halbes Grad trennt
„gehalten" von „eingefroren" und lässt allen 88 Clips Luft.

**Dass der Test den Fehler gefangen hätte, ist abgeleitet, nicht demonstriert:** der flache
Held hatte gemessen `largestSwing = 0,0` in allen drei geprüften Clips, und `0,0 > 0,5` ist
falsch. Einen echten Gegentest mit absichtlich flachem Export habe ich nicht gefahren.

Beleg des Laufs mit der neuen Zusicherung: `cowork-logs/verify-animtest.log`, **BUILD SUCCESS,
50 Tests**.

### Offen dazu

Das Platzhaltermaterial `steel` (`CharacterFactory.equip`, jetzt auch Klinge und Parierstange)
hat Rauheit **0,38** und Metallizität **0,72**. `STYLE.md:43` verlangt Rauheit >= 0,55 und
Metallizität <= 0,4. Das Material ist Bestand, aber ich habe es beim Verschieben der Sockel
angefasst — die Entscheidung, ob das Schwert matter werden soll, gehört zu Kevin.

---

## Schwert, Schild und ein Vierteldreh (2026-09-12)

Drei Aufträge von Kevin: ein richtiges Schwert und einen richtigen Schild besorgen oder generieren
lassen, den Sitz in der Hand sauber machen, und Mira die Ausrüstung wieder abnehmen. Alle drei sind
erledigt — und beim Nachsehen im Bild fiel ein vierter Fehler auf, der vorher niemandem aufgefallen
war.

### Die Modelle

Der Tripo-Katalog fiel durch: 1 886 429 Flächen und ein juwelenbesetztes Fantasieschwert, beides
gegen `STYLE.md`. Generiert habe ich stattdessen auf **Meshy 6 Lite** („a single medieval knight
sword, straight double-edged steel blade with a shallow fuller, plain iron crossguard,
leather-wrapped grip, round pommel"), **ohne** die PBR-Texturierung für 10 Credits: die Materialien
kommen aus unserer eigenen Palette und halten damit die Stilgrenzen ein.

Rohdatei `art/gen/sword-meshy6lite.glb` (252 444 Byte, 13 980 Flächen). In Blender: 180° um X, damit
die Klinge nach +Z statt −Z zeigt, Skalierung **0,5163** auf 0,98 m Gesamtlänge, Verschiebung
**+0,391**, damit die Griffmitte im Ursprung liegt. Dezimiert auf **2 400** Dreiecke, Materialien
nach Höhe zugewiesen (879 Flächen Stahl, 547 Eisen, 974 Leder).

| Material | Albedo (Mittel) | Rauheit | Metallizität | STYLE |
|---|---:|---:|---:|---|
| `PQW_Steel` | 0,22 | 0,58 | 0,38 | eingehalten |
| `PQW_Leather` | 0,05 | 0,70 | 0 | eingehalten |

Der Schild ist parametrisch gebaut, nicht generiert: Scheibe mit Radius 0,31 m, Eisenrand bis
0,325, Mittelbuckel bis 0,082 vor der Fläche, `PQP_Wood` für die Planken (hier sind die Plankenfugen
gewollt) und `PQK_Iron` für Rand und Buckel — **204 Dreiecke**.

Beide Dateien liegen unter `props/` und laufen damit über denselben Ladepfad wie jedes andere Modul;
`CharacterFactory` behält die Kistengeometrie als Rückfall in derselben Konvention.

### Der Sitz: gemessen, dann gerendert

Die Handrahmen habe ich an den gelieferten Rigs gemessen (`cowork-logs/hand2probe.log`,
`seatvariants2.log`): **+Y entlang der Finger, +X über den Daumen hinaus, +Z aus der Handfläche.**
Die vier Fingerwurzeln liegen innerhalb von neun Grad um +Y, Zeigefinger auf +X, kleiner Finger auf
−X, der Daumen steht nach +Z ab.

Mein erster Sitz war trotzdem falsch: ich habe die Klinge entlang der Finger gelegt, und das ergibt
eine Lanze, die waagerecht aus der Faust steht (`cowork-logs/handseat.log`, `probe-hand*.png`). Eine
Faust hält den Griff **quer** zur Handfläche. Der Sitz ist jetzt:

* Schwert: Vierteldrehung um Z (Klinge auf lokal −X, also nach unten-vorn hängend) und eine weitere
  Vierteldrehung um die Klinge selbst, damit die Flachseiten zur Seite zeigen statt Schneide nach
  oben; Versatz `(0, 0,095, 0,015)`.
* Schild: keine Drehung — die Schildfläche folgt der Handfläche (+Z) — Versatz `(0, 0,085, 0,085)`.
  Die 0,085 vor der Faust sind der Grund, dass der Unterarm **hinter** der Scheibe bleibt; mit dem
  ersten Versatz `(0, 0,13, 0)` lag die Scheibe in der Armebene und der Arm steckte quer durch die
  Schildfläche.

Vier Kandidaten wurden nebeneinander gerendert und nach Augenmaß entschieden
(`target/probe-shots/v2-shot*.png`), nicht nach Skalarprodukt. Die gewählte Variante ist in Idle,
Walk, Block und Attack1 nachgesehen (`ship-shot1..5.png`).

### Der vierte Fehler: alle acht Rigs standen quer

Beim ersten Blick durch die Spielkamera stand der Held **im Profil**. Ursache: die Clips kommen von
Erens Armature und tragen dessen Vierteldrehung gegen die glTF-Front mit. Die **Bindepose** ist
korrekt — Arme entlang X, Front auf +Z — deshalb hat es keine der bisherigen Prüfungen gemerkt:
Joints da, Clips da, Längen da, und die Figur läuft seitwärts.

Das betrifft **alle acht gelieferten Rigs** (Held, Mira, Eren, Goblin, Ork, Wächter, Schamane,
König), weil alle Erens Clips benutzen. Die Korrektur ist eine Drehung um +90° um Y auf einem eigenen
Wrapper-Knoten in `CharacterFactory` (`AUTHORED_FACING`) — nicht auf dem geladenen Wurzelknoten, der
die Exporter-Transformation trägt.

Wichtig für später: **eine kopflose Messung kann die Blickrichtung nicht sicher feststellen.** Ich
habe es mit drei Metriken versucht und alle drei sind pose-abhängig:

| Metrik | Ergebnis über 88 Clips |
|---|---|
| Schulterlinie `UpperArm.R → UpperArm.L` | im Kampfstand um bis zu 45° verdreht |
| Zehenrichtung `Foot → Toe_End` | z zwischen −1,00 (Run) und +1,00 (Attack2) |
| Beckenrahmen `Hips`-Rotation | lokales Z zwischen 0,13 und 0,89 auf +Z |

Der Test hält deshalb die **Bindepose** fest, die einzige Pose, die ein kopfloser Test verlässlich
liest: mit Korrektur laufen die Bindearme entlang Z, ohne sie entlang X. Das ist keine Prüfung der
Optik, sondern eine Sperre gegen das stille Verschwinden der Korrektur — die Optik selbst ist im
Bild geprüft.

**Merksatz, zum zweiten Mal in diesem Projekt: Blickrichtung wird gerendert, nicht gerechnet.**

### Mira trägt nichts mehr

`create` hat eine vierte Stelle bekommen: `armed`. Nur `PlayerController` setzt sie. Der neue Test
`onlyTheArmedHeroCarriesGearAndItSitsInTheFist` prüft beides — dass Miras Handsockel leer sind, dass die
Sockel des Helden die echten Modelle tragen (> 100 Dreiecke, also nicht die Rückfallkisten) und dass
der Griff zwischen 0,05 m und 0,25 m vom Handgelenk sitzt, also in der Faust und nicht am
Handgelenkspunkt oder einen Meter daneben.

### Belege

* `cowork-logs/verify-seat2.log` — **BUILD SUCCESS, 51 Tests** (vorher 50).
* `cowork-logs/smoke-seat.log` — Rauchlauf über alle 32 Stufen, `target/smoke-ok.txt` geschrieben,
  13 Bildschirmfotos; `target/screenshots/pentagon-2.png` zeigt den Held von hinten mit Schwert in
  der Rechten und Mira ohne Ausrüstung.
* `art/probe/*.java` — die Sonden: `HandProbe` (Bilder), `SeatVariants2` (Kandidaten),
  `FacingProbe`, `AcrossProbe`, `ToeProbe`, `HipProbe`, `WhereProbe`, `PoseProbe` (Messungen).

### Offen

* Das Platzhaltermaterial `steel` (Rauheit 0,38 / Metallizität 0,72) ist weiter im Rückfallpfad und
  verstößt gegen `STYLE.md`. Die gelieferten Modelle bringen jetzt eigene, stilkonforme Materialien
  mit, der Verstoß betrifft also nur noch den Fall „Modell fehlt".
* Der Schild ist mit 0,65 m Durchmesser groß; in `Block` verdeckt er den Kopf zu einem Teil.
  Historisch ist das normal, es bleibt eine Geschmacksfrage.
* In `Block` zeigt die Klinge waagerecht nach hinten, weil die Hand in dieser Haltung mit der
  Handfläche nach unten liegt. Ohne Eingriff in die Clips ist das nicht besser zu machen.

---

## Die Figuren standen quer: drei Fehler im Asset (2026-09-12, später Abend)

Kevins Befund nach dem Spielen: „Jetzt ist die Laufanimation kaputt und der Charakter ist immer
seitlich", und dazu „das Schild zeigt auf die Innenseite vom Charakter". Beides war richtig, und die
Ursache lag nicht im Code, sondern in den Clips. Der Vierteldreh, den ich am Nachmittag in
`CharacterFactory` eingebaut hatte, hat den Laufzyklus gerade gezogen und alles andere verschlimmert
— er behandelte einen Einzelwert, wo elf verschiedene standen.

### Wie es gemessen wurde

Zuerst das Bild: `art/probe/ClipSheet` stellt elf Helden nebeneinander, jeder mit einem anderen Clip,
alle mit der Drehung, die das Spiel anlegt. Auf dem Blatt zeigt jede Figur in eine andere Richtung.

Dann die Zahl. Drei Metriken habe ich verworfen, weil sie posenabhängig sind (Schulterlinie bis 45°
verdreht, Zehenrichtung über 88 Clips zwischen −1,0 und +1,0, Beckenrahmen zwischen 0,13 und 0,89).
Belastbar ist die **Beckenachse `Thigh.L − Thigh.R`**: die Oberschenkelwurzeln hängen mit festem
Versatz an der Hüfte, also ist ihre Verbindungslinie die Beckenorientierung, und kein Beinschwung
kann sie drehen.

### Fehler 1: die Quelle steht seitlich

Mixamos komplettes „sword and shield"-Set ist in einer **seitlichen Deckungshaltung** authored,
Walk und Run derselben Bibliothek dagegen geradeaus. Gemessen an den Original-FBX
(`cowork-logs/measure_sources.log`, Winkel gegen die Blender-Front):

| Quelle | Winkel | Bemerkung |
|---|---:|---|
| `sword and shield walk` | +1° | geradeaus |
| `sword and shield run` | −10° | geradeaus |
| `sword and shield idle` | −53° bis −86° | seitlich, **und in sich um 60° schwankend** |
| `sword and shield idle (4)` | −53,1° konstant | seitlich, aber ruhig |
| `sword and shield block idle` | −65,5° konstant | seitlich |
| `sword and shield impact` | −65° | seitlich |
| `sword and shield death` | −53° → +7° | dreht beim Fallen |

Eine Figur aus beiden Hälften **steht seitlich und läuft vorwärts**. Das ist keine Panne meiner
Kette, das ist die Quelle — und es war von Anfang an so.

### Fehler 2: ein Ruhepose-Abtastwert vor jedem Clip

Jede exportierte Animation begann mit einem Abtastwert der **Ruhepose**: die NLA-Strips lagen ab
Frame 1, der Export tastete ab Frame 0. Im Spiel ist das ein T-Pose-Blitz bei **jedem** Clipwechsel —
und weil Clips im Kampf ständig wechseln, ist das genau das „kaputt" am Laufen. In der Messung
war es daran zu erkennen, dass alle elf Clips bei exakt demselben Winkel (−1,1°) starteten und dann
auseinanderliefen.

### Fehler 3: der Idle dreht sich selbst

`sword and shield idle` schwenkt den Körper über 3,5 s um 60°. Zentrieren hilft da nichts, das bleibt
ein Drehen auf der Stelle. Ersetzt durch `sword and shield idle (4)`, der seine Haltung auf ein Grad
hält — **eingepfropft über Copy-Transforms-Constraints und einen Visual-Bake**, nicht über kopierte
Kurven: die Restposen von geliefertem Rig und Mixamo-Quelle liegen bis zu **22° je Bone** und 5,6 cm
auseinander, ein Kurvenkopie hätte die Pose verformt. Weltraum-Constraints interessiert das nicht.

### Die Reparatur

`art/blender/anim_normalise.py` (neu, 11 KB) macht für alle acht Charaktere:

1. Keys von 24 auf 30 fps skalieren, damit sie wieder auf ganzen Frames liegen und der Export die
   Zeiten schlüsselgenau reproduziert statt neu abzutasten.
2. Den stabilen Idle einpfropfen.
3. Jeden Clip auf die Front drehen — gehaltene Clips (Idle, Walk, Run, Block, Hit) auf ihren
   **Kreismittelwert**, einmalige (Attacken, Dodge, Death, Cast) auf ihr **erstes Bild**, weil das
   der Moment ist, in dem der Spieler sie ausrichtet.
4. Strips ab Frame 0, Szene ab Frame 0 — damit fällt der Ruhepose-Abtastwert weg.

**Die Drehachse ist gemessen, nicht abgelesen.** `art/blender/axis_test.py` dreht den Root-Bone um
alle drei Achsen und misst Gierwinkel, Kippung und Kopfhöhe:

| Achse | +90° ergibt | Kippung | Kopfhöhe |
|---|---|---:|---:|
| lokal X | Gier −55° | −53° | −0,07 m |
| lokal Y | Gier −36° | +37° | −0,10 m |
| **lokal Z** | **Gier −90°** | **unverändert** | **unverändert** |

Die Restmatrix legt lokal Y nahe (ihre Spalte 1 zeigt auf die Welt-Hochachse). Diese Antwort ist
falsch. **Merksatz: auch Achsen werden gemessen, nicht gerechnet.**

### Ergebnis in der Engine

`cowork-logs/pelvis-fixed.log`, Beckenwinkel je Clip (90° = genau auf der Front):

| Clip | Mittel | Spanne | Bewertung |
|---|---:|---:|---|
| Idle | 91,1 | 2,1 | steht still |
| Walk | 89,0 | 3,3 | natürliche Hüftdrehung |
| Run | 88,6 | 7,5 | natürlich |
| Block | 90,8 | 2,6 | hält |
| Hit | 88,3 | 6,6 | hält |
| Attack1 | 85,0 | 11,8 | Körpereinsatz im Schlag |
| Death | 74,4 | 46,0 | dreht beim Fallen, startet auf der Front |
| Cast | 77,2 | 63,4 | dreht in der Beschwörung, startet auf der Front |
| Attack2 | — | 251,8 | **Drehschlag um 180°**, siehe offen |
| Attack3 | — | 298,8 | **dreht 96° heraus**, siehe offen |
| Dodge | 91,4 | 314,3 | Rolle, startet und endet auf der Front |

Alle elf Clips starten auf **exakt 90,0°**. Der Vierteldreh in `CharacterFactory` ist ersatzlos
entfallen; `AssetTest` prüft stattdessen für **8 Charaktere × 11 Clips**, dass das erste Bild auf der
Front steht (Toleranz 14°, gemessen wird 0,0°).

### Schild auf die Handrückenseite

Gemessen in der neuen Idle-Pose: die linke Handfläche zeigt nach **(0,91 | −0,04 | 0,41)** bei einer
Front von (0 | 0 | −1) — also auf die eigenen Rippen. Mixamos Set dreht die Handfläche zum Körper.
Der Schild sitzt deshalb jetzt auf der Gegenseite (lokal −Z) und 0,11 m davor; dann zeigt er nach
außen-vorn, der Unterarm bleibt hinter der Scheibe, und die gekrümmten Finger stoßen nicht durch die
Schildfläche. Das Schwert bleibt, wie es war — Knauf oben in der Faust, Parierstange unten, Klinge
nach unten-hinten: so hält man ein Schwert mit der Spitze nach unten, und die Spitze bleibt 30 cm
über dem Boden (nachgerechnet aus Handhöhe 1,128 m und Klingenlänge 0,882 m).

### Ein Beleg, der gelogen hat

Im ersten Rauchlauf nach der Reparatur stand der Held auf den Regionsbildern in der T-Pose. Das war
**kein Spielfehler, sondern ein Aufnahmefehler**: `ScreenshotAppState` speichert am Ende des Frames,
in dem man ihn anfordert — und die Regionsstufen des Rauchlaufs bauen die Region in genau diesem
Frame. Die Figuren existieren dann, aber keine Animation hat sie gestellt, also zeigt das Bild die
Bindepose. `SmokeScenario.shoot()` fordert das Bild jetzt zwei Frames im Voraus an. Das Bild aus der
Zufluchtsstufe war schon vorher richtig, weil dort zweieinhalb Sekunden zwischen Bau und Aufnahme
lagen.

### Belege

* `cowork-logs/verify-final.log` — **BUILD SUCCESS, 51 Tests**.
* `cowork-logs/smoke-verified.log` — Rauchlauf über alle 32 Stufen mit `target/smoke-ok.txt`.
* `cowork-logs/pelvis-fixed.log`, `yawseries-fixed.log` — Beckenwinkel je Clip und Zeitverlauf.
* `cowork-logs/measure_sources.log` — die 52 Mixamo-Quellen, Winkel über die Zeit.
* `cowork-logs/axis_test.log` — die drei Achsen des Root-Bones.
* `target/probe-shots/seat-shot*.png` — Sitz aus Spielkamera, Front, Nahaufnahmen, Walk, Block.
* `art/gen/pre-normalise/*.glb` — die acht Dateien vor der Reparatur, zum Vergleich behalten.

### Offen

* **Attack2 dreht 180°, Attack3 dreht 96° heraus.** Das ist der Inhalt der Mixamo-Quellen
  („slash"/„attack"-Varianten sind Drehschläge). Sie starten jetzt auf der Front, aber sie enden
  verdreht, und der nächste Clip schnappt zurück. Saubere Lösung: andere Quellen wählen
  (`slash (5)` schwingt nur ±35°) und auf 0,58 s bzw. 0,78 s umtakten. Das ist ein eigener
  Arbeitsschritt und braucht eine Entscheidung, weil es das Kampfgefühl ändert.
* Die Dateien sind gewachsen (Held 5,48 → 6,54 MB), weil der eingepfropfte Idle dichte Keys für alle
  66 Bones trägt.
* Der Idle ist mit 2,43 s kürzer als vorher (3,53 s).

---

## Der Schild zeigte noch immer zur Seite (2026-09-12, Nachtrag)

Kevin hat das gerenderte Bild angesehen und gefragt, was mir auffällt. Meine erste Antwort war zur
Hälfte falsch, und das gehört genauso ins Protokoll wie die Korrektur.

**Falsch war:** „die Klinge läuft durch das rechte Bein". Nachgemessen beträgt der Abstand der
Klinge zu den Beinknochen **0,45 bis 0,98 m** über alle elf Clips
(`cowork-logs/clearance-final.log`). Was ich für ein Durchdringen hielt, war reine Überdeckung aus
diesem Blickwinkel — die Klinge liegt 40 cm vor dem Bein.

**Richtig war:** der Schild zeigte zur Seite statt nach vorn, gemessen 53° bis 102° neben der Front.

### Der Denkfehler

Ich hatte angenommen, die Scheibe müsse den Unterarm **enthalten**, sonst stehe der Arm durch sie
hindurch. Damit blieb als Schildnormale nur eine Richtung senkrecht zum Unterarm — und weil Mixamos
linker Arm im Idle nach vorn zeigt, konnte der Schild höchstens 65° neben der Front landen. Die
Annahme ist falsch herum: ein Buckelschild sitzt **quer** auf dem Arm, mit der Fläche in
Armrichtung, und der Unterarm läuft vom Handgelenk nach *hinten* — also vollständig hinter der
Scheibe.

Abgesucht über alle Richtungen im Handrahmen, bewertet als mittlere Frontdeckung über Idle, Walk,
Run, Block und Hit:

| Schildnormale (Handrahmen) | Frontdeckung (Mittel je Clip) |
|---|---|
| Handfläche (+Z) | −0,50 bis −0,11 — zeigt nach hinten |
| Handrücken (−Z) | 0,11 bis 0,50 — zeigt zur Seite |
| **Unterarm (+Y)** | **0,84 bis 0,93** |

Gesetzt ist jetzt die Unterarmachse: Vierteldrehung um X, Versatz 0,175 m entlang des Arms.
Ergebnis, wieder an den Attachment-Knoten gemessen (dieselbe Transformation, die der Renderer
benutzt):

| Clip | Schild neben der Front | vorher |
|---|---|---|
| Block | 9° bis 64° | 69° bis 89° |
| Hit | 13° bis 34° | 56° bis 77° |
| Walk | 22° bis 42° | 53° bis 68° |
| Run | 6° bis 63° | 27° bis 84° |
| Idle | 24° bis 91° | 65° bis 91° |

Im Bild ist der Buckel jetzt zentriert zu sehen und der Arm liegt dahinter
(`target/probe-shots/seat-shot1.png`, `seat-shot7.png`).

### Eine Messung, die gelogen hat

Für die Abstandsmessung habe ich zwischendurch die Sockeltransformation **selbst** aus den Gelenken
zusammengesetzt, weil ich den Attachment-Knoten kopflos nicht traute. Das Ergebnis: Schwertspitze
**0,96 m unter dem Boden** — was das Bild klar widerlegt. Die eigene Komposition war falsch, nicht
der Knoten. `ClearanceProbe` liest deshalb die Attachment-Knoten, mit einem Kommentar, der genau das
festhält: **widersprechen sich Rechnung und Bild, hat das Bild recht.**

### Offen dazu

Die Schwertspitze taucht in `Dodge` (−0,40 m), `Death` (−0,23 m) und `Cast` (−0,24 m) unter den
Boden. Bei der Rolle und beim Sterben liegt der Körper selbst am Boden, das ist üblich; beim Zaubern
kniet die Figur. Wer das vermeiden will, braucht eine kürzere Klinge oder eine Scheide.


---

## Funktionsprüfung des Gesamtstands: fünf Befunde, vier behoben (2026-09-12, Nachtsitzung)

Kevins Auftrag: das Projekt einmal komplett durchgehen und alles auf Funktion prüfen — Spiel,
Animationen, Modelle, Proportionen, Materialien, Anordnung — und Fehler im gleichen Zug beheben,
Testlücken schließen. Ausgangslage geprüft: `mvn -B verify` grün mit 51 Tests
(`cowork-logs/mvn-verify-baseline.log`), Rauchlauf grün über 32 Stufen
(`cowork-logs/smoke-baseline.log`). Danach gerendert (`ClipSheet`, `SeatShots`, ein neuer
`Lineup` aller acht Figuren und aller Props) und gemessen (zwei neue Sonden, s. u.).

### Befund 1: Die Physik lief auf dem falschen Thread — 2 894 Warnungen pro Rauchlauf

`smoke-baseline.log` enthält 2 894-mal `com.jme3.bullet.PhysicsSpace update — WARNUNG: invoked
from wrong thread`. Minie bindet einen `PhysicsSpace` an den Thread, der ihn erzeugt hat
(`NativeLibrary.jniEnvId()`), und warnt bei jedem Schritt von einem anderen. Ursache:
`GameApplication` reichte `new BulletAppState()` an den `SimpleApplication`-Konstruktor. Der
Konstruktor läuft auf dem Java-Hauptthread und hängt den State sofort an; Minie erzeugt den
nativen Space beim Anhängen. Alle späteren Schritte kamen vom Renderthread `jME3 Main`.

Behoben: der State wird in `simpleInitApp` angehängt, der Konstruktor bekommt ein leeres
State-Array (damit auch FlyCam und StatsAppState nicht mit hereinkommen — letzterer hört auf F5,
die Speichertaste). Der Rauchlauf prüft jetzt in Stufe 3
`physics.space().jniEnvId() == NativeLibrary.jniEnvId()`. Nachher: **null** Warnungen
(`cowork-logs/smoke-fix1.log`).

### Befund 2: Jede Figur betrat den Raum in der T-Pose — und der Test dagegen war blind

`target/screenshots/pentagon-8.png` aus dem Baseline-Lauf zeigt Held und Mira zwei Bilder nach
`loadRegion` mit ausgebreiteten Armen. Das war kein Aufnahmefehler mehr, sondern messbar:
`art/probe/ClipTimeline` (neu) tastet die gestellte Armature alle 1/30 s ab. Für **jeden** Clip
stehen bei t = 0 beide Hände auf 1,512 m Höhe (Schulterhöhe, Bindepose), bei t = 0,1 s auf
1,30 m, ab t = 0,3 s auf 1,12 m (die dokumentierte Handhöhe im Idle).

Die Ursache liegt nicht im Asset, sondern in jME: `BlendableAction` blendet jede neue Action über
`transitionLength = 0,4 s` von der **aktuellen Gelenkpose** her ein
(`collectTransform` interpoliert Frame für Frame vom Ist-Wert zum Clipwert). Ein frisches Rig steht
in der Bindepose, also faltet sich jede Figur nach dem Laden erst in den Idle. Dieselbe Blende
lag vor jedem Angriff: Attack1 trifft nach 0,15 s, da war der Arm zu zwei Dritteln noch in der
vorigen Pose.

Behoben in `CharacterFactory.create`: der Idle startet bei t = 0,25 s (jenseits seiner Blende,
in einer Schleife unsichtbar), Bewegungsclips blenden mit 0,25 s, alle Einmalclips (Attack1-3,
Dodge, Hit, Death, Cast) mit 0,1 s. `pentagon-8.png` aus `smoke-fix1` zeigt beide Figuren gestellt.

**Der Test war blind:** `AssetTest` misst die Blickrichtung über `restart(clip);
updateLogicalState(0)` — bei t = 0 ist das Blendgewicht null, gemessen wurde also die Bindepose,
und die steht per Konstruktion auf der Front. Deshalb „exakt 90,0°" bei allen 88 Clips. Der
Test setzt die Blende jetzt vor der Messung auf 0 und misst das echte erste Bild. Er bleibt grün
— die Clips starten wirklich auf der Front — aber jetzt aus einem Grund.

Neue Zusicherungen: `aFreshRigIsPosedOnItsFirstFrameNotInTheBindPose` (Hände nach einem Bild
unter 1,35 m oder asymmetrisch), `oneShotClipsBlendInFasterThanTheirWindup` (Blende ≤ 0,15 s).

### Befund 3: Gegner starben im Stehen, der Held erreichte den Boden nie

`EnemyBrain` fror den Composer 1,2 s nach dem Tod ein; `CampaignState` schaltete 1,1 s nach dem
Tod auf Game Over. Der gelieferte Death-Clip ist **2,3 s** lang, die Hüfte erreicht den Boden
bei **1,7 s** (ClipTimeline: Hüfthöhe 1,16 → 0,93 bei 1,2 s → 0,23 bei 1,7 s). Jede Leiche
stand also vornübergebeugt in der Luft, der Held fiel nie um. Und weil `setCurrentAction`
schleift, wäre ohne das Einfrieren jeder Tote nach 2,3 s wieder aufgestanden.

Behoben: `Rig.once(clip)` spielt einen Clip genau einmal (`setCurrentAction(name, layer,
false)`) und hält das letzte Bild — die Engine räumt die Action am Ende ab, die Gelenke bleiben
stehen. Death läuft bei Gegnern und Held so; das Einfrieren entfällt. Game Over kommt nach
`DEATH_HOLD = 1,9 s` (Körper liegt; die Rauchlauf-Stufe prüft nach 2,5 s). Auch `stun` spielt
Hit einmal: die 1,7 s Parade-Betäubung hatte den 0,7-s-Zucker zweieinhalbmal abgespielt.

Rauchlauf neu: `Death clip runs once and holds`, `Dead enemy lies on the floor` (Hüfte unter
0,6 m × Skalierung, an den Attachment-Knoten gemessen), `Hero is on the floor at Game Over`.
Test neu: `aClipPlayedOnceHoldsItsLastFrame` am Platzhalter-Rig.

### Befund 4: Der Gegnerhieb fiel, bevor der Schaden kam

Der Gegner startete Attack3 (0,77 s, Schlagmoment bei 0,28 s) beim Eintritt in `ATTACK` und
verteilte den Schaden erst am Ende der Aufladung: Goblin 0,55 s, Ork 0,95 s, Wächter 1,05 s,
König 1,15 s. Der Hieb war also längst vorbei, wenn er traf, bei Ork, Wächter und König lief der
Clip sogar ein zweites Mal an. Danach schnitt `RECOVER` sofort auf Idle.

Behoben: beim Eintritt in `ATTACK` hält der Gegner Idle über seinem Telegraph; der Clip startet
einmalig, wenn die Restzeit dem Schlagmoment entspricht (`MELEE_STRIKE = 0,28 s` aus
`AttackTimeline`, `CAST_RELEASE = 1,6 s` — der Aschenrufer holt laut ClipTimeline bei 1,6 s
aus; sein Wind-up von 1,05 s ist kürzer, also startet Cast bei 0,55 s im Clip). `RECOVER`
lässt den Ausschwung zu Ende laufen und geht erst dann auf Idle.

### Befund 5, nicht behoben: Dodge und Cast passen nicht in ihre Spielfenster

| Clip | Länge | Spielfenster | Was man sieht |
|---|---:|---:|---|
| Dodge | 2,33 s | 0,58 s (`dodgeLeft`) | Hüfte 1,16 → 0,90 m, dann Schnitt auf Idle. Die Rolle (Hüfte 0,21 m bei 1,2 s, wieder oben bei 2,0 s) kommt nie. |
| Cast | 2,97 s | 0,60 s (`castLeft`) | Hand geht bis 0,3 s hoch und hält; die eigentliche Entladung bei 1,6 s kommt nie. Als kurze Geste tragbar. |
| Hit | 0,70 s | 0,23 s | Zucker, tragbar. |

Dodge ist der echte Bruch: der Spieler duckt sich und steht wieder. Drei Wege, alle mit
Spielgefühl-Folgen, deshalb Entscheidung von Kevin (s. u.).

### Sichtprüfung der Assets (`target/probe-shots/lineup-shot1..5.png`)

Alle acht Figuren im Idle in Spielgröße mit 1,95-m-Pfosten, dahinter alle Props und fünf
Kit-Module. Proportionen stimmen: Held, Mira, Eren auf 1,95 m; Goblin gebückt und klein; Ork
breit; Wächter aufrecht und kantig; Schamane hoch und schmal; König massiv mit kleinerem Kopf —
alle fünf Silhouetten sind unterscheidbar. Materialien lesen sich matt, kein poliertes Metall.
Waffe und Schild sitzen. Nichts davon ist ein Fehler.

Was auffällt, aber Gestaltung ist (für die nächste Phase):

* Der **Thron** ist ein 5,1 m hoher Ziegelblock mit schwarzer Platte; als Thron nicht lesbar.
* Die drei **Altäre** (rune/seal/lore) und der Schrein sind Ziegelquader; alle Props tragen
  dieselbe Steintextur wie die Wände.
* Die **Dekokristalle** der Höhlen sind unbeleuchtete `Sphere(4,5)`-Platzhalter und wirken als
  flache Cyanflächen (`pentagon-5.png`); `props/crystal.gltf` ersetzt nur die drei
  Interaktionskristalle. `kit_rock` erscheint als ziegelgemusterte Kuppel.
* **Mira und Eren** stehen im Schwert-und-Schild-Kampfidle (Mixamo `idle (4)`), ohne Waffen;
  eine Kartografin und ein Gefangener bräuchten einen eigenen Ruheclip.
* Platzhaltermaterialien `iron` (Rauheit 0,5 / Metallizität 0,8) und `gold` (0,45 / 0,65) in
  `WorldView` verletzen `STYLE.md`; sichtbar nur an der Falle (Platte, Stacheln), die kein
  Modul hat, und an den Rückfallpfaden.
* HUD versprach **F12 Screenshot**, belegt war nur die Engine-Taste Druck. F12 ist jetzt gemappt.

### Neue Sonden

* `art/probe/RestLead` — größte Gelenkabweichung von der Bindepose je Keyframe; weist nach, dass
  kein Clip mehr mit einem Ruhepose-Abtastwert beginnt (Beleg `cowork-logs/restlead.log`).
* `art/probe/ClipTimeline` — Hüft-, Kopf- und Handhöhe plus Handgeschwindigkeit alle 1/30 s;
  daraus stammen Schlagmoment, Entladung, Rollenspanne und Bodenkontakt
  (`cowork-logs/cliptimeline-hero.log`). Achtung: der Gelenk-Modellraum der gelieferten Rigs ist
  Blenders, die Höhe läuft entlang −Z.
* `art/probe/Lineup` — alle acht Figuren in Spielgröße, alle Props, fünf Kit-Module, fünf
  Kameras (`target/probe-shots/lineup-shot*.png`).

### Verifikation

| Prüfung | Ergebnis | Beleg |
|---|---|---|
| `mvn -B verify` vorher | BUILD SUCCESS, 51 Tests | `cowork-logs/mvn-verify-baseline.log` |
| `mvn -B verify` nachher | **BUILD SUCCESS, 54 Tests** | `cowork-logs/mvn-verify-fix2.log` |
| Rauchlauf vorher | 32 Stufen, `smoke-ok.txt`, **2 894** Thread-Warnungen | `cowork-logs/smoke-baseline.log` |
| Rauchlauf nachher | 32 Stufen, `smoke-ok.txt`, 36 Prüfungen, **0** Thread-Warnungen | `cowork-logs/smoke-fix1.log` |
| Figuren nach `loadRegion` gestellt | Held und Mira im Idle statt T-Pose | `target/screenshots/pentagon-8.png` |
| Leichen am Boden | Prüfung in Stufe 22 und 29 grün | `cowork-logs/smoke-fix1.log` |
| Endstand nach Formatierung | BUILD SUCCESS, 54 Tests; Rauchlauf grün, 36 Prüfungen | `cowork-logs/mvn-verify-final.log`, `cowork-logs/smoke-final.log` |

### Offen — Entscheidungen für Kevin

1. **Dodge**: (a) Clip in Blender auf ~0,9 s umtakten und `dodgeLeft` auf 0,9 s heben
   (Unverwundbarkeitsfenster mitziehen), (b) `dodgeLeft` bei 0,58 s lassen und den Clip auf ~0,6 s
   stauchen (Rolle wird hektisch), (c) anderen Mixamo-Clip wählen (kurzer Sidestep statt Rolle).
2. **Cast**: 0,6-s-Geste belassen, oder Clip auf eine 0,6-s-Entladung kürzen.
3. **Mira/Eren-Idle**: eigene Ruheclips (Mixamo „idle"/„sitting"), betrifft nur zwei Dateien.
4. **STYLE-Verstoß** der Platzhaltermetalle in `WorldView`/`CharacterFactory`: auf 0,6 / 0,35
   setzen oder Bestand lassen.
5. Attack2 (180°) und Attack3 (96°) Drehschläge — unverändert offen aus der vorigen Session.

---

## Der Held hielt das Schwert verkehrt herum (2026-09-13)

Kevins Frage: „Ist das gewollt, dass der Held das Schwert mit der Klinge nach unten hält?" Nein.
Der Sitz vom Vortag ließ die Klinge auf der Kleinfinger-Seite der Faust austreten (lokal −X im
Handrahmen) — das ist der umgekehrte Griff mit hängender Spitze, im Protokoll damals als „so hält
man ein Schwert mit der Spitze nach unten" begründet. Der gewöhnliche Griff hat die Klinge auf der
Daumenseite (+X). Geändert ist ein Vorzeichen in `CharacterFactory.equip`
(`fromAngleAxis(-HALF_PI, UNIT_Z)`), Versatz und Vierteldrehung um die Klinge bleiben.

Gerendert (`target/probe-shots/seat-shot1..7.png`, `clips-sheet1..4.png`): im Idle steht die
Klinge schräg vor der Brust nach oben, in Attack1-3 und Cast hebt der Held sie über den Kopf, im
Walk zeigt sie nach vorn, im Block nach unten-hinten (die Hand liegt dort mit der Fläche nach unten,
unverändert). So ist Mixamos Schwert-und-Schild-Set offensichtlich gemeint. `ClearanceProbe`
(`cowork-logs/grip-probes.log`): Abstand Klinge zu Bein 0,58–1,27 m (Dodge 0,12 m, die Rolle),
tiefster Punkt der Spitze in Dodge/Death/Cast **−0,21 / −0,05 / −0,03 m** statt vorher
−0,40 / −0,23 / −0,24 m — der offene Punkt „Schwertspitze unter dem Boden" ist damit weitgehend
entschärft.

Verifikation: `cowork-logs/verify-smoke-grip.log` — BUILD SUCCESS, 54 Tests; Rauchlauf 32 Stufen,
36 Prüfungen, `smoke-ok.txt`. `target/screenshots/pentagon-2.png` zeigt den Held von hinten mit
der Klinge nach oben-vorn.


---

## Vier Clips neu geschnitten: Drehschlag, Finisher, Rolle, Zauber (2026-09-13)

Kevins Befund: „Bei der zweiten Kombo dreht sich der Held nur 180° und schlägt nach hinten." Die
Zeitreihe (`cowork-logs/yawseries-fixed.log`) bestätigt es: Attack2 dreht in den ersten 0,15 s
von 90° auf −91° und bleibt dort. Ursache: die Quelle `sword and shield attack (2)` ist eine volle
360°-Drehung von 1,3 s, die in einer früheren Session auf die Kombo-Länge **abgeschnitten** statt
umgetaktet wurde — sie endete mitten in der Drehung. Attack3 (`attack (3)`, ebenfalls eine Drehung)
war auf dieselbe Weise bei 96° abgeschnitten. Und Dodge und Cast hatten ihre volle Quellenlänge
(2,33 s und 2,97 s) gegen Spielfenster von 0,58 s und 0,6 s.

### Die Reparatur: `art/blender/anim_polish.py`

Zweiter Durchlauf über die normalisierten GLBs (Import bei 30 fps, deshalb keine Umskalierung
mehr nötig), Ausgabe nach `art/gen/polished/`, der Stand davor liegt unter `art/gen/pre-polish/`.

| Clip | Quelle | Bilder | Verfahren |
|---|---|---:|---|
| Attack2 | `sword and shield attack (2)`, 39 Bilder, 360° | 17 (0,567 s) | neu gepfropft (Copy Transforms + Bake), **zeitlich gestaucht** statt geschnitten; erstes Bild auf die Front gedreht und der Rest der Drehung (−58°) als Rampe über den Clip verteilt, damit die Drehung **exakt auf der Front endet** |
| Attack3 | `sword and shield attack (4)`, 30 Bilder, schwerer Vorwärtshieb ohne Drehung | 23 (0,767 s) | neu gepfropft, gestaucht, Front gepinnt (Rampe 0°) |
| Dodge | bisheriger Clip (`Stand To Roll`) | 70 → 23 (0,767 s) | gestaucht |
| Cast | `sword and shield casting (2)`, 31 Bilder, Einhand-Geste | 18 (0,6 s) | neu gepfropft, gestaucht, Front gepinnt |

Beckenwinkel danach (0° = Front), aus `cowork-logs/anim_polish-hero.log` und `-rest.log`,
für alle acht Figuren identisch bis auf Zehntel:

* Attack2: `0, −32, −111, −136, 130, 81, 62, 31, 5, −23, −12, −3, 0` — eine volle Umdrehung, Start
  und Ende auf der Front.
* Attack3: `0, −8, −8, 4, 35, 55, 44, 32, 8, −6, −6, −3, 0` — Körpereinsatz in den Hieb, Ende vorn.
* Dodge: `0, 10, 26, 36, 42, 62, 104, 70, −8, −9, −13, −5, −1` — die Rolle, Ende vorn.
* Cast: `0, 8, 13, 15, 15, 15, 15, 15, 15, 12, 9, 6, 0`.

Gerendert mit der neuen Sonde `art/probe/ClipStrip` (zwölf Standbilder eines Clips nebeneinander,
Front und Draufsicht; `target/probe-shots/strip-hero-*.png`): Attack2 dreht einmal durch und
schlägt am Ende waagerecht nach vorn; Attack3 holt über den Kopf aus und schlägt nach vorn-unten;
Dodge ist Hocke – Rolle (Kopf unten) – Aufstehen in 0,77 s.

Zeitachsen (`cowork-logs/cliptimeline-hero-polished.log`): Attack2 Handgeschwindigkeit 16–20 m/s
zwischen 0,17 und 0,27 s (Treffer bei 0,19 s passt); Attack3 Klinge kommt bei 0,30–0,37 s
herunter (Treffer bei 0,28 s, eine Winzigkeit früh, im Rahmen); Dodge Hüfte verlässt die Standhöhe
bei 0,10 s, ist bei 0,37–0,47 s am Boden (0,20 m) und bei 0,63 s wieder oben; Cast Hand oben ab
0,17 s.

### Spielseite

* `PlayerController.DODGE_TIME = 23/30 s`, `CAST_TIME = 18/30 s`; die Ausweichgeschwindigkeit
  ist so gesetzt, dass die Strecke bei 5,8 m bleibt; Unverwundbarkeit 0,10–0,65 s nach Start —
  genau die Zeit, in der die Hüfte unten ist.
* **Attack2 trifft rundum** (`CombatSystem`, Kosinus −1 statt 0,3): eine 360°-Drehung streift jede
  Seite; Schaden und Multiplikator (1,15) unverändert. Das ist eine Spieländerung — s. offen.
* `EnemyBrain.CAST_RELEASE = 0,2 s` (Hand oben) statt 1,6 s.
* `AssetTest` prüft jetzt für alle acht Figuren: Dodge und Cast haben die Fensterlänge, und Attack2,
  Attack3, Dodge und Cast **enden** auf der Front (Beckenlinie am letzten Bild, Toleranz 20°).

### STYLE-Metalle

`AssetPipeline.pbr` weist jetzt jedes Material ab, das `STYLE.md` verletzt (Rauheit < 0,55 oder
Metallizität > 0,4), mit klarer Fehlermeldung — die Grenze wohnt an der einen Stelle, durch die
jedes prozedurale Material geht. `iron` (0,5/0,8 → 0,6/0,35), `gold` (0,45/0,65 → 0,55/0,4) in
`WorldView`, `steel` (0,38/0,72 → 0,58/0,38, wie `PQW_Steel`) und `gold` in `CharacterFactory`
sind angepasst. Test `styleLimitsAreEnforcedAtTheMaterialGateway`.

### Verifikation

`cowork-logs/verify-smoke-polish.log`: **BUILD SUCCESS, 55 Tests**; Rauchlauf 32 Stufen,
36 Prüfungen, `smoke-ok.txt`, keine Ausnahme.

### Offen

* Attack2 trifft rundum — falls das zu stark ist: Kosinus zurück auf 0,3 (eine Zahl in
  `CombatSystem`).
* Die Handgeschwindigkeit des Attack3-Hiebs liegt 0,02–0,09 s hinter dem Trefferfenster.
* Cast ist jetzt eine Geste der linken (Schild-)Hand; für den Helden akzeptabel, kein Zauberstab.


---

## Ruheclips, Thron und Altäre, Kristalle und Felsen, Licht (2026-09-13)

Kevins Freigabe: Mixamo-Download ja, Attack2 trifft rundum, Aufhübschen in der Reihenfolge
Thron/Altäre → Höhlen → Licht.

### Mira und Eren stehen nicht mehr in Kampfhaltung

Die 52 lokalen Mixamo-Quellen sind alle Schwert-und-Schild. Aus Mixamo (Browser war eingeloggt,
Erens Figur `EREN-MIXAMO-GEOM` noch aktiv) geladen, FBX ohne Skin bei 30 fps, abgelegt unter
`art/mixamo/npc/`:

* `Sad Idle.fbx` (121 Bilder, Kopf gesenkt, Gewicht auf einem Bein) → **Eren**, der Gefangene.
* `Breathing Idle.fbx` (299 Bilder, ruhiges Atmen mit leichtem Wiegen) → **Mira**.

`art/blender/npc_idle.py` pfropft sie ein. Eren trägt genau Erens Mixamo-Skelett, also Copy
Transforms wie bisher. Miras Rig hat eigene Knochenlängen, und Miras Mesh ließ sich nicht hochladen
(Chrome ist bei Mixamo ausgeloggt, der eingebaute Browser kann keine Dateien wählen) — deshalb für
sie **nur die Weltraum-Rotationen** kopiert; ihre Gelenke behalten ihre Positionen, und die Hüfte
wird danach so verschoben, dass der tiefste Fußpunkt wieder da liegt, wo er im alten Idle lag
(gemessen: +0,145 m nötig; danach 0,012–0,041 m über Grund, also 3 cm Wippen beim Gewichtswechsel).
Beide Clips per Kreismittel auf die Front gedreht (−1,2° / +1,8°).

Gerendert mit `ClipStrip mira Idle` / `ClipStrip eren Idle` (`target/probe-shots/strip-mira-Idle-*.png`,
`strip-eren-Idle-*.png`): Mira steht entspannt, Arme hängen, keine Verzerrung sichtbar; Eren steht
mit gesenktem Kopf. `verify-smoke-npcidle.log`: 55 Tests, Rauchlauf grün.

### Thron, Altäre, Schrein — `art/blender/props_builder.py`

Alle drei waren Ziegelquader in der Wandtextur, der Thron ein 5,1 m hoher Monolith. Neu gebaut
aus Quadern und Achteckprismen mit 1,5–2 cm Fasen, kastenprojizierten UVs und einer neuen
Albedo **ohne Ziegelmuster** (`art/textures/prop_carved_albedo.png`, prozedural: abgewetzter
Stein mit feinen Rissen), dazu das Kit-Eisen (`kit_iron_albedo.png`) und ein dunkler Aschestoff.
Materialien innerhalb STYLE (Stein 0,9 / 0, Eisen 0,6 / 0,35, Stoff 0,95 / 0).

| Prop | Maße (B × H × T) | Dreiecke | Aufbau |
|---|---|---:|---|
| `throne` | 2,60 × **3,90** × 2,30 (vorher 5,10 hoch) | 792 | zweistufiger Sockel, Sitzblock mit Kissen, Armlehnen mit Eisenkappen, Vorderpfosten, Rückenlehne mit drei Zacken, drei Eisenbändern und dunklem Rückentuch |
| `rune`, `seal`, `lore` | 1,30 × 1,23 × 0,90 | 360 | Plinthe, Schaft mit Eisenband, Deckplatte, achteckige Eisen-Runenscheibe mit Aschekern |
| `shrine` | 1,33 × 0,62 × 1,33 | 368 | achteckiger Sockel, Schale mit Eisenkragen, Glutkegel |

Grundflächen bleiben die gelieferten (Interaktionsradius und Kamera sind darauf abgestimmt),
die Leuchtteile bleiben im Code (`WorldView` behält den `Crystal`-Kindknoten): Schreinglut sitzt
unter dem Kristall bei 0,68 m+, Altarplatten enden bei 1,22 m unter dem Kristall bei 1,33 m+, die
Thronglut schwebt bei 2,5 m vor der Lehne. Der Stand davor liegt unter `art/gen/pre-polish/props/`.

Gerendert: `target/probe-shots/lineup-shot6.png` (Front, nah) und die neue Sonde
`art/probe/RoomShots` — die echte Region, wie `WorldView` sie baut, mit Kit, Tönung und Fackeln,
fotografiert vor dem benannten Objekt (`room-throne-*.png`, `room-crypt-*.png`).

### Höhlen: Kristalle und Felsen

* Die Dekokristalle waren `Sphere(4,5)` unter Unshaded-Glühen: flache Cyanflächen. Neu:
  `assets/CrystalShapes.cluster(seed)` — drei bis vier sechseckige Schäfte mit Spitze, flache
  Normalen, im Rahmen der alten Kugel (±0,5 × −1..+1), damit alle Aufrufer-Skalierungen gelten —
  und `AssetPipeline.crystal(color)`: PBR mit dunklem Körper, `Emissive` in der Farbe
  (PBRLighting hat eine Glow-Technik über Emissive, der Bloom greift also weiter), Rauheit 0,6.
  Erster Schnitt hatte Schäfte ein Zehntel so breit wie hoch und Emissive 2,2 — „Lichthalme",
  weiß ausgebrannt (`before-light/room-caverns-2.png`). Jetzt breite Schäfte, Emissive 1,3 / 0,6.
  Der Name `Crystal` bleibt tragend für `buildObject`.
* `kit_rock` war eine ziegelgemusterte Kuppel. `art/blender/rock_builder.py` baut einen
  Ikosaeder-Findling (320 Flächen) mit geglättetem Zufallsrelief und der neuen Albedo
  `kit_rock_mottled_albedo.png` (prozedural, kein Ziegel); Mittelpunkt-Pivot wie dokumentiert.
* Die Fallenplatte (`TrapWarning`) leuchtete als gesättigt orange Fläche; jetzt dunkles Glutrot
  über dasselbe Kristallmaterial.

Was die Höhlen weiterhin nicht sind: Höhlen. Wände, Boden und Decke bleiben das Ziegel-Kit in
Grüntönung; eigene Höhlenmodule sind ein eigener Block (s. offen).

### Licht — `assets/SceneLighting`

Das Grundlicht (Mond, Füllung, Sonde, Ambient) wohnt jetzt in einer Klasse, die Spiel **und**
`RoomShots` benutzen, damit ein Sondenbild dasselbe Licht zeigt wie das Spiel. Der Grundton war
flach: Ambient 0,30/0,36/0,42 plus Mond 0,43 leuchteten jede Wand gleich aus, die Fackeln (2,3 bei
14 m) waren kaum zu sehen. Jetzt Ambient 0,20/0,24/0,29, Mond 0,30, Füllung 0,10/0,13/0,18,
Fackeln 3,8 bei 17 m mit stärkerem Flackern — die Räume lesen sich als warme Feuerinseln in
blaugrauem Schatten (`room-refuge-2.png`, `room-crypt-2.png` gegen `before-light/`).

### Verifikation

| Schritt | Beleg | Ergebnis |
|---|---|---|
| Ruheclips | `verify-smoke-npcidle.log` | 55 Tests, Rauchlauf 36 Prüfungen, grün |
| Props | `verify-smoke-props.log` | grün |
| Kristalle/Felsen | `verify-smoke-caverns.log` | grün |
| Licht | `verify-smoke-light.log` | grün |

### Offen

* **Höhlenmodule** (Wand/Boden/Decke als Fels statt Ziegel) — eigener Block über `kit_builder`.
* Der Schreinkristall (helles `0x70d7d6`) brennt trotz reduziertem Emissive noch fast weiß aus;
  eine dunklere Grundfarbe oder eine Flamme statt Kristall wäre die Alternative.
* Miras Idle ist ein Rotations-Retarget von Erens Skelett; falls beim Spielen etwas an ihr
  auffällt, ist der saubere Weg der Upload ihres Meshes bei Mixamo (Chrome-Login nötig).
* `Lineup` zeigt die Props von hinten (sie stehen mit der Front zu +Z); `RoomShots` ist die
  bessere Sonde für Props.


---

## Kampf, Fallen, Parade, Epilog, Höhlen (2026-09-13, zweiter Teil)

Kevins Liste, in dieser Reihenfolge abgearbeitet: Aschenrufer in der Ecke, Attack2 auf der
Stelle, Eingesammeltes verschwindet, Ablegen aus dem Inventar, Fallen neu, Parade mit eigener
Animation, Wiederbeleben je Ebene, Kampfbalance, zwei Abschlusssequenzen, Schreinfeuer,
Höhlenmodule. Keine Rückfragen mehr, alles war zu machen.

### Attack2 lief drei Meter voraus — `art/blender/anim_pin.py`

Mit der neuen Spalte `hipsDrift` in `art/probe/ClipTimeline` gemessen (`cliptimeline-drift.log`):
die Hüfte des Helden wandert in Attack2 **3,16 m** horizontal, in allen anderen Clips unter
0,09 m (`cliptimeline-drift-all.log`). Der Sprung-Drehschlag aus Mixamo trägt seine Wurzelbewegung
im Hips-Knochen, und die Physikkapsel bewegt sich während eines Angriffs nur 1,1 m/s — das Mesh
lief dem Körper drei Meter davon und sprang am Ende zurück. Die dritte Clip-Passage pinnt die
horizontale Weltposition der Hüfte Bild für Bild auf das erste Bild, gerechnet im Knochenraum
(Armaturmatrix × Elternpose × Ruheversatz, nichts davon hängt vom Schlüssel selbst ab, eine
Passage ist exakt). Ergebnis 3,16 → 0,000 m auf allen acht Rigs (`anim_pin-hero.log`,
`anim_pin-rest.log`); Miras rotations-retargetetes Dodge trug ebenfalls 1,22 m und wurde
mitgepinnt (`anim_pin-all.log`). `AssetTest` hält jetzt **jeden Clip jedes Rigs** unter 0,35 m
Hüftdrift. Die Beine behalten ihre Bewegung und rutschen über die 0,567 s ein wenig — so sieht
eine Pirouette aus. Stand davor unter `art/gen/pre-pin/`.

### Parade — eigener Clip, Betäubung, Riposte

Dieselbe Passage pfropft dem Helden `sword and shield block (2)` (15 Bilder, 0,5 s, ein Schildstoß)
als **optionalen** Clip `Parry` ein. `CharacterFactory.OPTIONAL_CLIPS` kennt ihn, `CLIPS` verlangt
ihn nicht; `Rig.has("Parry")`, Rückfall ist `Block`. Im Spiel (`PlayerController.parry()`,
`CombatSystem.hurt`): gelingt der Block im Paradefenster (erste 0,23 s), spielt der Held den
Stoß einmal und steht 0,5 s, der Angreifer ist **2,0 s** betäubt (`PARRY_STUN`, vorher 1,7), und
**2,0 s** lang ist der nächste Treffer auf einen betäubten Gegner eine **Riposte** mit
**2,5-fachem** Schaden (`RIPOSTE_WINDOW`, `RIPOSTE_MULTIPLIER`), gemeldet als „RIPOSTE +n". Der
Block ist danach gelöst; wer weiter parieren will, drückt neu. Abgewehrte Zauber melden sich
eigens. `theHeroCarriesAParryClipThatEndsOnTheFront` prüft Länge, Einblendung und Front.

### Der Aschenrufer in der Ecke

`EnemyBrain.fleeGoal` sucht die Flucht nicht mehr blind „fünf Meter vom Spieler weg", sondern
prüft sieben Richtungen (0°, ±35°, ±70°, ±110°) in fünf und zweieinhalb Metern: Ziel muss Boden
sein, auf gerader Linie erreichbar und weiter vom Spieler entfernt als der jetzige Stand. Gibt es
kein solches Ziel — das ist eine Ecke —, **hält er stand** (`Enemy.holdGround` 2,5 s), dreht sich
um und wirft sofort (Cooldown auf 0,3 s gekürzt). `Run` läuft nur noch, während `walk()` tatsächlich
bewegt, sonst `Idle`. Test `aCorneredCasterFindsNoWayOutAndAnOpenRoomOffersOne` an der Nordostecke
des Saals der Namen.

### Eingesammeltes verschwindet, Abgelegtes bleibt liegen

`WorldView.refreshObject` skalierte Truhe, Siegel und Kristall auf 0,6 — „kleiner" statt „weg".
Jetzt: die Kristallader wird ausgeblendet, der Siegelaltar verliert seinen Leuchtsplitter (das
Siegel) und behält den Stein, die Truhe bleibt, wie sie ist. Ablegen: Knopf **Ablegen** auf der
Inventarseite → `CampaignState.dropItem` legt ein Stück 0,7 m vor dem Helden ab
(`GameSession.Drop`, mit Gebiet, Position und laufender Nummer, im Spielstand gespeichert und
validiert), `WorldView.addDrop` baut ein Bündel mit kleinem Splitter, `nearby()` bietet es mit
[E] zum Aufheben an, auch im Kampf. Schlüssel und Reliquien lassen sich nicht ablegen, das letzte
Stück der getragenen Waffe oder Rüstung auch nicht.

### Fallen — `world/TrapMechanism`, `DungeonLayout.trap`

Das alte Konzept: eine leuchtende Platte in der Korridormitte, die im Takt hochfuhr und 28
Schaden tat; man ging außen herum. Das neue: ein **gangbreiter** Streifen (`value` = Achse und
Zellenbreite, z. B. `z3`, `x5`), **ohne Platte** — der Boden bleibt, wie er ist, das einzige
Zeichen sind zwei Reihen dunkler Schlitze über die Breite. Erst als Platte in dunklerem Bodenton
gebaut, die auf dem Höhlenboden als gepflasterter Streifen zu sehen war (`room-caverns-5.png`,
davor); weg damit. Zeitlauf, in `TrapMechanism` rein gerechnet und in `CombatTest` gehalten:
Auslösung bei **2,4 m** Abstand längs des Gangs, **0,4 s** Klicken (Spitzen schauen 7 cm heraus),
dann **0,75 s** Feuer mit **60** unblockbarem Schaden für jeden auf dem 2 m tiefen Streifen unter
0,75 m Höhe, **1,5 s** Rückzug, danach scharf — wer noch davor steht, löst sofort wieder aus. Die
Rechnung: Gehen (4,1 m/s) und Sprinten (7 m/s) enden beim Feuer auf dem Streifen; eine Rolle auf
das Klicken hin ist von 0,1 bis 0,65 s unverwundbar und kommt durch, eine Rolle nach 0,35 s nicht
mehr; Springen (über 0,75 m) hilft. Stehenbleiben löst aus, trifft aber nicht.

Siebzehn Fallen statt sechs, alle in Verbindungsgängen, Wand zu Wand
(`trapsFillTheirCorridorsFromWallToWall` prüft für jede: Boden über die ganze Breite, Wand
daneben, kein Raum): REFUGE 1 (vor dem versiegelten Aufgang), CRYPT 4, CAVERNS 4, PRISON 5, THRONE
3 (zwei davon fünf Zellen breit, wo zwei Gänge zusammenlaufen). Der Rauchlauf stellt den Helden
zwei Meter vor `REFUGE_trap0`, sieht sie auslösen, ohne dass er Schaden nimmt, stellt ihn dann
auf den Streifen und sieht die Wiederbewaffnung zubeißen (Stufen 31–33, Bild `pentagon-13.png`).

### Wiederbeleben je Ebene

`GameSession.leave(region)` beim Portalwechsel: alle Besiegten und gespeicherten Gegnerstände des
verlassenen Gebiets werden gelöscht, beim nächsten Besuch stehen sie zu Hause mit vollem Leben.
Ausnahme der Ork-König, sobald `king_dead` steht; der Kerkermeister kommt zurück, den Schlüssel
gibt es aber nur, solange keiner im Gepäck liegt. Test
`leavingARegionRevivesItsEnemiesButNotTheFallenKing`.

### Kampfbalance

| | vorher | jetzt |
|---|---|---|
| Goblin | 62 LP, 13 Schaden, 3,9 m/s, Aufladung 0,55 s | **70 / 16 / 4,3 / 0,5** |
| Ork-Brecher | 145 / 25 / 2,6 / 0,95 | **170 / 30 / 2,8 / 0,9** |
| Eiserner Wächter | 230 / 29 / 2,3 / 1,05 | **260 / 34 / 2,4 / 1,0** |
| Aschenrufer | 90 / 19 / 2,5 / 1,05 | **100 / 24 / 2,7 / 1,0** |
| Ork-König | 1300 / 38 / 2,7 / 1,15 | **1500 / 46 / 2,9 / 1,1** |
| Aggro-Reichweite | 13 m (König 17) | **16 m (König 20)** |
| Cooldown / Erholung nach dem Schlag | 1,1 s (Rufer 2,2) / 0,65 s | **0,9 s (Rufer 1,8, König 1,1) / 0,55 s (König 0,8)** |
| Trankfund | jeder dritte Gegner | **jeder vierte** |
| Starttränke | 5 | **3** (`Inventory.START_POTIONS`) |

Held unverändert (140 LP, 18 Grundschaden). Auf dem Papier braucht ein ungelevelter Held mit dem
rostigen Schwert vier Öffner für einen Goblin und der Goblin neun Bisse; ein Orkschlag kostet ein
Fünftel, ein Königsschlag ein Drittel der Leiste (`raisedEnemyValuesStillLeaveEveryFightWinnableOnPaper`).
Der Rauchlauf rechnet die Königsphasen seither relativ (`maxHealth · 0,34`). **Das ist eine
Papierbalance**; gespielt hat sie noch niemand.

### Abschlusssequenzen — `ScreenMode.EPILOGUE`

Nach dem Urteil am Thron folgt nicht mehr sofort die Textseite. `CampaignState.Epilogue` fährt
26 s lang eine Kamerabahn vor dem Thron (±40° um die Achse, 7 → 10 m, 2,2 → 4,5 m hoch; eine erste
Halbkreisbahn steckte im Pfeiler des Bogens, `pentagon-11.png` davor), fünf Untertitel je Ende in
Letterbox, und das Siegel antwortet: **zerbrochen** — die Glut flackert, birst bei 9 s in Funken
(`effects.burst`) und ist fort, die Feuer sinken auf ein Viertel (`WorldView.torchScale`);
**bewahrt** — die Glut wächst auf das 2,4-Fache und die Fackeln flammen auf 1,7. Enter/Esc
überspringen. Danach die Endseite mit nur noch **Zum Hauptmenü** und **Spiel verlassen**; Esc
führt dort nicht mehr zurück ins Spiel. Flagge `campaign_complete`. Rauchlauf Stufen 24/25 prüfen
Sequenz, Untertitel, Kamera am Thron und beide Enden.

### Schreinfeuer — `assets/FlameShapes`, `WorldView.flame`

Der Schreinkristall (`0x70d7d6`) brannte weiß aus. Die Schreine heißen „Feuer" und sind jetzt
welche: Glutbett und vier Zungen als Drehkörper mit Bauch (`FlameShapes.tongue`, kein Kegel — der
erste Schnitt waren drei Kegel, und ein Kegel hat einen harten Rand), additiv geblendet
(`AssetPipeline.flame`), Kern hellgelb, außen orange und rot, eine kleine schräge Zunge, alles
flackert in `WorldView.update`. Das Schreinlicht ist warm (`0xff9a4a` × 3,2, Radius 9,5) statt
cyan. Der Knoten heißt `Flame` und überlebt wie `Crystal` das Prop-Modul. Bild
`room-refuge-1.png`, `room-caverns-1.png`.

### Höhlenmodule — `art/blender/cave_builder.py`

Neun Module unter `props/kit_*_cave*`, die `WorldView.buildTiles` in CAVERNS vor dem Ziegel-Kit
versucht (Rippen und Kränze entfallen dort): Boden und Decke je zwei Varianten, Wand `face` in
zwei Varianten, `corner`, `span`, `pier`. Gleicher Vertrag wie der Kit (Pivot unten mittig, ganze
Zelle, Sichtseiten wie beim Ziegel), Relief aus deterministischem Wertrauschen, das an den
Zellkanten auf null ausläuft, damit Nachbarn ohne Riss aneinanderstoßen; Preis ist die
Wiederholung je Zelle, die zwei Varianten (mit verschobenem Texturfenster) und die Gebietstönung
mildern. Boden hebt sich höchstens 3 cm über die Physikebene und senkt sich bis 9 cm; Stalaktiten
hängen bis 0,9 m unter die Deckenplatte. Erste Textur war die gefleckte Findlingsalbedo (mittlere
lineare Leuchtdichte 0,118) — unter den Fackeln Sand. Neue `kit_cave_albedo.png` (1024², dunkler
rissiger Fels, 0,089, im Wandband aus `docs/dungeon-kit.md`). Dreiecke: Boden 246, Decke 342, Wand
244–532. Bilder `room-caverns-1/4/5.png`.

### Verifikation

| Schritt | Beleg | Ergebnis |
|---|---|---|
| Kampf, Fallen, Parade, Epilog, Wiederbeleben | `verify-smoke-kampf.log` | `clean verify` BUILD SUCCESS, **67 Tests**; Rauchlauf **34 Stufen, 42 Prüfungen**, `smoke-ok.txt`, 0 Thread-Warnungen |
| Schreinfeuer, Höhlenmodule, Fallen ohne Platte | `verify-smoke-hoehlen.log` | `clean verify` BUILD SUCCESS, **68 Tests**; Rauchlauf 42 Prüfungen, `smoke-ok.txt` |
| Endstand (hellere, 1,25 m hohe Spitzen; Fallenbild zum Feuermoment; keine Meldungen im Epilog) | `verify-smoke-final3.log` | BUILD SUCCESS, 68 Tests; Rauchlauf 34 Stufen, 42 Prüfungen, 0 Thread-Warnungen; Bilder `target/screenshots/pentagon-11.png` (Epilog), `pentagon-13.png` (Falle feuert, 82/140), `target/probe-shots/strip-hero-Attack2-*.png` (Drehung auf der Stelle) |
| Hüftdrift | `cliptimeline-drift.log`, `cliptimeline-drift-all.log`, `anim_pin-*.log` | Attack2 3,16 → 0,000 m auf acht Rigs, Mira Dodge 1,22 → 0 |
| Höhlen | `cave_builder.log`, `roomshots-caves2.log` | neun Module, Maße oben |

Zwei Stolpersteine dieses Durchgangs: `mvn clean` löscht `target/probe-shots` und
`target/probe-classes`, die Sonden also danach neu übersetzen; und nach `device_commit_files`
ist ein Neubau nur sicher mit `clean` — ein Rauchlauf lief einmal mit einem Stand, der die
geänderte Datei nicht enthielt.

### Offen

* Die Höhlenwände wiederholen ihr Relief je Zelle; ein periodisches Rauschen entlang der Wand
  (statt Auslaufen an der Kante) würde die senkrechten Nähte tilgen, braucht aber Sonderfälle für
  Innenecken.
* ~~Dekokristalle sattes Cyan~~ — auf Kevins Wunsch abgedunkelt: Deko `0x1f5d5c` (tiefes Teal),
  die zu reinigenden Adern `0x3d9e98`, damit die Ziele heller bleiben als der Schmuck
  (`room-caverns-2.png`, `verify-smoke-kristalle.log`).
* Die Balance ist gerechnet, nicht gespielt.
