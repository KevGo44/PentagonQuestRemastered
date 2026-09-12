# Briefing für Cowork- und Fremdsessions

Diese Datei ist der Einstiegspunkt für jede Session, die dieses Projekt nicht kennt. Sie beschreibt
die Verträge, die eingehalten werden müssen, damit gelieferte Assets tatsächlich im Spiel landen.
Inhaltliche Details stehen in [ARCHITECTURE.md](ARCHITECTURE.md), [ASSETS.md](ASSETS.md) und
[GAME_DESIGN.md](GAME_DESIGN.md).

## Das Projekt in fünf Sätzen

Java 17 / jMonkeyEngine 3.8.1, Maven, Paket `de.pentagon`, Hauptklasse `de.pentagon.core.Main`.
Ein Dungeon-Action-Adventure mit fünf Gebieten, 33 Räumen, 31 Gegnern und einem dreiphasigen Boss.
Physik über Minie/Bullet, PBR-Rendering, eigener Atmosphärenfilter, HUD auf einer virtuellen
1440×900-Zeichenfläche. **Alle mitgelieferten Modelle, Texturen und Klänge sind prozedurale
Platzhalter**, erzeugt von `tools/AssetBaker.java` und `de.pentagon.assets.CharacterFactory`.
Genau diese Platzhalter sollen durch echte Assets ersetzt werden.

---

## 1. Der Asset-Vertrag — der wichtigste Abschnitt

Assets werden **nicht** über eigenen Ladecode eingebunden. Es gibt genau eine Auflösungsstelle,
`AssetPipeline.model(String id, Supplier<Spatial> fallback)`:

```java
for (String suffix : List.of(".glb", ".gltf", ".j3o", ".obj")) {
  String path = "models/" + id + suffix;
  if (getClassLoader().getResource(path) != null) return manager.loadModel(path);
}
return fallback.get();
```

Daraus folgt alles Weitere:

* Assets gehören in den **Klassenpfad**, also nach `src/main/resources/models/…` — **nicht** in
  einen Ordner `/assets` im Projektwurzelverzeichnis. Dort abgelegte Dateien werden nie geladen.
* Der Dateiname **ist** die Verdrahtung. Liegt die Datei am richtigen Pfad, ersetzt sie den
  Platzhalter automatisch. Es ist **kein Java-Code zu schreiben, zu ergänzen oder zu registrieren.**
* Reihenfolge `.glb` → `.gltf` → `.j3o` → `.obj`. GLB ist das Zielformat.
* Nach dem Ablegen neu bauen, sonst liegt die Datei nicht im Klassenpfad des letzten Builds.

### Erwartete Dateien

| Kategorie | Pfad | IDs |
|---|---|---|
| Charaktere | `src/main/resources/models/characters/<id>.glb` | `hero`, `goblin`, `orc`, `warden`, `shaman`, `king`, `mira`, `eren` |
| Props | `src/main/resources/models/props/<id>.glb` | `shrine`, `portal`, `chest`, `lore`, `rune`, `seal`, `crystal`, `throne` |

Die Prop-IDs sind die kleingeschriebenen Konstanten von `DungeonLayout.Kind`; sie dürfen nicht
umbenannt werden. **Stand der Lieferung: `hero`, `mira` und `eren` sind echte
Assets** (Details und Belege in [asset-liste.md](asset-liste.md)); alles andere ist noch
Platzhalter. `props/crystal.gltf` ist bereits ein echtes Asset mit eigenem `.bin`-Buffer —
dessen Referenz im JSON nicht brechen.

---

## 2. Charaktere brechen laut, Props still

Das ist bewusst so und muss beim Testen eingeplant werden.

**Props:** Fallback ist `null`. Fehlt ein Prop, bleibt die prozedurale Geometrie stehen. Kein Fehler.

**Charaktere:** `CharacterFactory.create` prüft jedes geladene Modell und wirft eine
`IllegalStateException`, wenn

* `AnimComposer` oder `SkinningControl` fehlen, oder
* auch nur einer dieser elf Clips fehlt:
  `Idle, Walk, Run, Attack1, Attack2, Attack3, Dodge, Block, Hit, Death, Cast`

Die Namen sind **case-sensitive**. Ein Mixamo-Export heißt standardmäßig anders
(`mixamo.com`, `Armature|mixamo.com|Layer0` o. ä.) — **Clips müssen vor dem Ablegen umbenannt
werden**, sonst startet das Spiel nicht mehr. Das ist Absicht: ein halb passendes Charakterasset
soll nicht unbemerkt auf ein falsches Rig zurückfallen.

### Skelettvertrag

16 Joints, feste Namen:

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

Attachment-Punkte: `WeaponSocket` an `Hand.R`, `ShieldSocket` an `Hand.L`.

### Geometrie und Einheiten

* Metrisch, 1 Einheit = 1 m. jME: **+Y oben, +Z vorwärts**. Blender-Export mit `export_yup=True`.
* Charakterpivot am Boden zwischen den Füßen, Standardhöhe ca. **1,95 m**.
* Rotation angewendet, Skalierung `(1,1,1)`.
* Gegner werden im Spiel skaliert: Goblin 0,8 / Ork 1,15 / Wächter 1,2 / König 1,85. Modelle also
  **in Standardgröße** liefern, nicht vorskaliert.
* Keine Root Motion — Bullet besitzt Position und Richtung. Laufschleifen müssen an ihrer
  Startpose enden.
* Props: Pivot unten mittig. Zielmaße: Truhe 1,3×0,84×0,9 m, Altar 1,3×1,2×0,9 m, Tor 3,7×5,25×0,8 m,
  Schrein 1,4×1,8×1,4 m, Kristall 1,4×2,4×1,4 m.

### Animationszeiten

Angriffe müssen zu `combat/AttackTimeline.java` passen: Attack1 0,52 s mit Treffer ab 0,15 s;
Attack2 0,58 s ab 0,19 s; Attack3 0,78 s ab 0,28 s. Hit- und Death-Clips werden von der Simulation
beendet, nicht von der Animation.

---

## 3. Bauen und Prüfen auf diesem Rechner

`build.sh` und `run.sh` sind für macOS/Linux. **Unter Windows nicht verwenden** — `bash` zeigt hier
auf WSL, nicht auf Git Bash. Maven ist nicht global installiert; IntelliJ bringt es mit:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-23-temurin"
$mvn="C:\Users\kkfre\AppData\Local\Programs\IntelliJ IDEA Ultimate\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn -B verify
```

`verify` muss grün bleiben: derzeit **51 Tests**. `AssetTest` lädt echte Assets durch den jME-
AssetManager und ist damit die erste Instanz, die ein kaputtes Modell bemerkt. Neue Assets sollten
dort eine Zusicherung bekommen.

Alternativ in IntelliJ: Run-Konfiguration **Pentagon** (liegt als `.run/Pentagon.run.xml` im Projekt).

Der Integrationstest `mvn verify` ersetzt **nicht** den Sichttest. Ein Modell kann laden und
trotzdem falsch skaliert, verdreht oder im Boden versunken sein.

---

## 4. Konventionen

* **Dokumentation auf Deutsch, Codekommentare auf Englisch.** Bitte beibehalten.
* Kommentare erklären *warum*, nicht *was*. Der Bestand ist bewusst sparsam kommentiert.
* Formatierung: `tools/format.sh` (google-java-format 1.27.0, fest versioniert).
* Kurze Accessoren ohne `get`-Präfix: `muted()`, `master()`, `master(float)`, `highQuality()`.
* Kleine Wertetypen sind `record`s.
* **Keine Erfolgsmeldungen ohne Beleg.** `docs/VERIFICATION.md` trennt strikt zwischen
  tatsächlich Ausgeführtem und nicht Geprüftem. Diese Ehrlichkeit ist Teil des Projekts: nicht
  „funktioniert", wenn es nur kompiliert.

## 5. Stand der Versionskontrolle

Git-Repo mit Remote `origin` auf GitHub. Bislang existiert **ein einziger Commit** (`init`).
Vor größeren Änderungen committen, damit es einen Rückweg gibt.

Achtung Dateigröße: `src/main/resources/audio/pentagonradio.wav` ist 33,7 MB und liegt im
Klassenpfad. Binärdateien dieser Größe bleiben dauerhaft in der Git-Historie. Für Assets im
Zweifel vorher klären, ob sie eingecheckt oder außerhalb verwaltet werden sollen.

## 6. Was diese Session nicht tun soll

* Keinen „AssetManager-Ladecode" ergänzen — siehe Abschnitt 1, es gibt nichts zu verdrahten.
* Die Fallback-Kette in `AssetPipeline.model` nicht umbauen; sie ist die Rückfallebene, solange
  noch nicht alle Assets existieren.
* Asset-IDs und Clip-Namen nicht umbenennen, um ein Asset „passend" zu machen — stattdessen das
  Asset anpassen.
* Prozedurale Platzhalter nicht löschen. Sie sind die Referenz für Maßstab und Rig und halten das
  Spiel lauffähig, solange echte Assets fehlen.
* `pentagonradio.wav` nicht durch `AssetBaker.java` überschreiben lassen; es ist die einzige
  aufgenommene Datei und wird nicht generiert.
