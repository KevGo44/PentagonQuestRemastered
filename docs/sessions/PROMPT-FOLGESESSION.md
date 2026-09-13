# Übergabe-Prompt für eine Folgesitzung

Stand **13. September 2026, Ende der Sitzung**. Diesen Text in einer neuen Session einfügen und
unten die konkrete Aufgabe ergänzen. Er ist die einzige gepflegte Fassung; ältere Stände stehen in
der Git-Historie.

---

Du arbeitest an **PentagonQuest / „Das Aschensiegel"**, einem Java-17-Dungeon-Action-Adventure auf
jMonkeyEngine 3.8.1 (Maven, Paket `de.pentagon`). Das Repository liegt auf meinem Rechner unter
`C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered` und ist als Ordner mit dieser Session
verbunden.

**Lies zuerst die Projektdoku, bevor du irgendetwas änderst.** Der Wegweiser `docs/README.md` sagt,
was wofür ist. Die Reihenfolge:

* `docs/sessions/COWORK-BRIEFING.md` — die Arbeitsregeln und der Asset-Vertrag in Kurzform
* `docs/assets/asset-liste.md` — das Protokoll aller Arbeiten und Befunde, **das Ende zuerst**:
  dort stehen die letzten Änderungen, ihre Belege und die offenen Punkte
* `docs/assets/ASSETS.md` — die Austauschverträge: Pfade, Maße, Pivots, Rig, Bone-Namen, Sockets,
  Clips, Modul-IDs; `docs/assets/STYLE.md` — die Stilgrenzen (`Metallic <= 0,4`, `Roughness >= 0,55`)
* `docs/technik/ARCHITECTURE.md` (Code), `docs/spiel/GAME_DESIGN.md` (Inhalt),
  `docs/technik/VERIFICATION.md` (was als Beleg zählt), `docs/technik/RELEASE.md` (JAR, EXE, Setup)
* `README.md` im Wurzelverzeichnis — Steuerung, Start, Projektdateien

## Arbeitsregeln

* **Keine Commits, keine Pushes.** Das mache ich selbst. Wenn der Stop-Hook wegen uncommitteter
  Änderungen meckert: einfach ignorieren.
* **Kein Abo, keine Bezahlschranke.** Bestehende Guthaben (Meshy u. a.) darfst du vollständig
  aufbrauchen, eine Reserve muss nicht bleiben. Wenn ein Dienst ein Premium-Upsell zeigt
  (z. B. parallele Generierungen bei Meshy), schließen und einspurig weiterarbeiten.
* **Keine Logins für mich eintippen.** Ist eine Sitzung (Mixamo, Meshy) abgelaufen, sag es mir.
* Rohdaten im Repo sind in Ordnung, das Projekt ist privat und wird nicht veröffentlicht.
* **Wenn du Entscheidungen oder Inhalte von mir brauchst, kennzeichne das deutlich am Ende deiner
  Antwort.** Nicht im Fließtext vergraben.
* `mvn -B clean verify` muss grün bleiben: derzeit **72 Tests**. Nach jeder Asset- oder
  Ressourcenänderung zusätzlich den Rauchlauf fahren.
* Behauptungen über Aussehen werden **gerendert, nicht gerechnet**. Zahlen belegen, Bilder
  entscheiden. Belege nach `cowork-logs/`, Protokoll ans Ende von `docs/assets/asset-liste.md`,
  Verträge nach `docs/assets/ASSETS.md`, Prüfungen nach `docs/technik/VERIFICATION.md`.
* Doku auf Deutsch, Code-Kommentare auf Englisch, Java mit google-java-format 1.27.0
  (`C:\Users\kkfre\.m2\repository\com\google\googlejavaformat\google-java-format\1.27.0\google-java-format-1.27.0-all-deps.jar`).

## Umgebung — was funktioniert und was nicht

`device_bash` ist auf diesem Rechner defekt (Windows-Update vom 8. September). Als Ersatz benutze
ich den Blender-MCP als Shell: `mcp__remote-devices__Blender__execute_blender_code` führt Python im
laufenden Blender aus, und darin `subprocess`. `result` muss dort ein **dict** sein, sonst meldet
der Aufruf einen Fehler, obwohl der Code gelaufen ist — bei Skripten, die Dateien ändern, danach
nicht blind wiederholen, sondern erst den Zustand prüfen.

* **Blender GUI-MCP kann kein glTF importieren** (`bpy.context.object` fehlt im MCP-Kontext).
  Für alles mit Import/Export eine eigene CLI-Instanz starten:
  `C:\Program Files\Blender Foundation\Blender 5.2\blender.exe --background --factory-startup --python <skript.py> -- <args>`
* **Maven**: `C:\Program Files\jmonkeyplatform\java\maven\bin\mvn.cmd`, dazu
  `JAVA_HOME=C:\Program Files\Java\jdk-23-temurin` setzen (nicht JDK 24/26).
  Bauen: `mvn -B clean verify` (~2,5 min mit Tests). Danach liegt `target/aschensiegel-1.0.0.jar`
  (Shaded, mit allen Ressourcen).
* **Rauchlauf** (echtes Spiel, 34 Stufen, 46 Prüfungen, ~100 s):
  `java -jar target/aschensiegel-1.0.0.jar --smoke --no-audio --fast`
  Erfolg = `target/smoke-ok.txt` existiert; Bildschirmfotos in `target/screenshots/`.
* **Windows-Paket**: `tools\package-windows.cmd` (jpackage, 34 s) →
  `target\native\Aschensiegel\Aschensiegel.exe` und das ZIP; die EXE nimmt `--smoke` genauso.
* **Ausgabe von subprocess nicht direkt lesen**: deutsche Locale liefert kein UTF-8 und reißt die
  MCP-Brücke ab. Immer in eine Datei schreiben (`open(log,"wb")`) und mit
  `errors="replace"` zurücklesen.
* Lange Läufe mit `subprocess.Popen` starten und pollen — ein MCP-Aufruf darf nur ~60 s dauern.
* **Dateien auf den Rechner schreiben**: entweder direkt per Python über die Blender-Brücke, oder
  `device_commit_files` mit einem **frischen** Staging-Pfad und danach **MD5 zurücklesen**. Wird
  derselbe Staging-Pfad zweimal benutzt, schreibt das Werkzeug stillschweigend den alten Stand.
* **Bilder ansehen**: mit `device_stage_files` in den Container holen, dann mit `Read` öffnen.
* **Browser**: für Asset-Recherche und Generatoren verfügbar. Meshy-Guthaben vorher am Abzeichen
  prüfen; Texturierung dort kostet extra und ist meist unnötig, weil wir eigene stilkonforme
  Materialien setzen. Mixamo war zuletzt ausgeloggt.

## Sonden und Skripte

Sonden unter `art/probe/`, alle gegen das Shaded-JAR kompilieren
(`javac -cp target\aschensiegel-1.0.0.jar -d target\probe-classes art\probe\X.java`) und mit
`-cp <jar>;target\probe-classes` starten:

* `ClipSheet` — alle elf Clips nebeneinander, mit der Drehung, die das Spiel anlegt.
* `ClipStrip <figur> <clip>` — zwölf Standbilder eines Clips, Front und Draufsicht; für Drehungen,
  Rollen, Schlagmomente.
* `ClipTimeline` — Hüft-, Kopf- und Handhöhe, Handgeschwindigkeit und `hipsDrift` alle 1/30 s.
* `DodgeYaw` — Beckengier je Bild mit der richtigen Aufwärtsachse (s. Fallen).
* `SeatShots`, `ClearanceProbe`, `ShieldSearch`, `ShieldAim` — Sitz von Waffe und Schild.
* `PelvisProbe`, `YawSeries`, `RestLead`, `FacingProbe`, `WhereProbe`, `FistProbe`, `SeatProbe` — Rohmaße.
* `Lineup` — alle acht Figuren, alle Props, fünf Kit-Module, fünf Kameras.
* `RoomShots REGION id...` — die echte Region wie `WorldView` sie baut, fotografiert vor dem
  benannten Objekt; die Sonde für Props, Module und Licht.

Blender-Skripte unter `art/blender/`: `anim_normalise.py` (Clip-Reparatur auf die Front),
`anim_polish.py` (Neupfropfen/Stauchen von Attack2, Attack3, Cast, Dodge), `anim_pin.py`
(Wurzelbewegung entfernen, Parry-Graft, Dodge geradeziehen), `npc_idle.py`, `enemy_pipeline.py`,
`kit_builder.py` (Dungeon-Module), `cave_builder.py` (Höhlenmodule), `rock_builder.py`,
`props_builder.py`, `measure_sources.py`, `measure_export.py`, `axis_test.py`.

## Stand (13. September 2026)

* **Acht Charaktere** unter `src/main/resources/models/characters/`: hero, mira, eren, goblin, orc,
  warden, shaman, king. Elf Clips (Idle, Walk, Run, Attack1-3, Dodge, Block, Hit, Death, Cast),
  der Held zusätzlich **Parry**. Alle auf der glTF-Front, **ohne Wurzelbewegung**, die Rolle
  gerade. Stände davor: `art/gen/pre-normalise/`, `pre-pin/`, `pre-straighten/`.
* **Waffe und Schild**: `props/sword.glb`, `props/shield.glb`, griffzentriert; nur der Spieler
  trägt sie.
* **Dungeon-Kit**: fünfzehn Module `props/kit_*` plus **neun Höhlenmodule** `kit_*_cave*` (die
  Gläserne Tiefe), dazu chest, shrine (ein **Feuer**, `FlameShapes`), rune, seal, lore, throne,
  portal, crystal.
* **Spielsysteme seit dem 13. September**: Parade mit eigenem Clip, Betäubung und Riposte;
  gangbreite unsichtbare Fallen (`TrapMechanism`, 17 Stück); Wiederbeleben je Ebene; Ablegen und
  Aufheben von Gegenständen; zwei Enden mit Epilog; drei Schwierigkeitsgrade (`combat/Difficulty`);
  erste Person (`V`), Schulterkamera mit Retikel, Zielen über den Kamerastrahl; Inventar mit
  Charakterschablone; Fenster- und Launcher-Symbol; Windows-Paket (`docs/technik/RELEASE.md`).
* Offen und dokumentiert: Gewichte der Gegner automatisch (nicht gemalt), Roben/Schürzen starr,
  Schamane und König über dem Dreiecksbudget, senkrechte Naht der Höhlenwände je Zelle.

## Gelernte Fallen (nicht wiederholen)

* `mvn clean` löscht `target/probe-shots` und `target/probe-classes`; die Sonden danach neu
  übersetzen.
* Nach `device_commit_files` nur mit `mvn -B clean verify` bauen: ein Rauchlauf lief einmal mit
  einem Stand, der die gerade geschriebene Datei nicht enthielt.
* Wurzelbewegung in Mixamo-Clips steckt im Hips-Knochen; `ClipTimeline` zeigt sie als
  `hipsDrift`, `anim_pin.py` entfernt sie. Der Test lässt 0,35 m zu.
* **Beckenlinie messen**: der Gelenkraum der gelieferten Rigs ist Blenders (Höhe entlang −Z,
  Skinning-Spatial auf Identität). Wer eine Gier misst, projiziert die Aufwärtsachse heraus, die er
  vom Rig abliest (Root → Hips, `AssetTest.flatten`) — `lateral.y = 0` löscht eine waagerechte
  Achse und las eine gerade Rolle als 30–50° schief.
* Die Clips tragen Skalierungsschlüssel für jeden Joint: eine Joint-Skala, die bleiben soll
  (Kopf in der ersten Person), setzt ein Control **nach** dem Composer jedes Bild neu.
* **`--fast` zeigt nicht, was der Spieler sieht.** Der Rauchlauf läuft mit `--fast` (Profil
  „Schnell", ohne Bloom, SSAO, Atmosphärenfilter). Kevin spielt „Atmosphärisch": Emissives blüht
  dort auf, die Szene bekommt einen warmen Schleier. Bilder, die über Aussehen entscheiden, ohne
  `--fast` machen (`smoke-screenshots-hq.log`, `docs/bilder/screenshots/`).
* Die Schulterkamera hängt `1,6 m + 4,5 · sin(Neigung)` über dem Ziel; „geradeaus" ist deshalb
  Neigung ≈ −0,36, nicht 0 (`PlayerController.minPitch`).
* Nach einem Teleport gilt der gemerkte Zielpunkt des Retikels nicht mehr (`warp()` verwirft ihn).
* Die Kamerabahn des Epilogs darf nicht um den Thron kreisen: er steht unter dem Bogen, dessen
  Pfeiler bei ±3,55 m stehen. Bahn nur raumseitig (+Z), ±40°.
* **Die Engine-Front ist +Z**: `Quaternion.lookAt` legt lokal +Z auf die Blickrichtung. Mixamos
  ganzes „sword and shield"-Set ist dagegen 53° seitlich authored, Walk und Run geradeaus.
* **Blickrichtung ist kopflos nicht berechenbar** — außer als Beckenachse `Thigh.L − Thigh.R` am
  ersten Bild. Schulterlinie, Zehenrichtung und Beckenrahmen schwanken über die Clips um 60°+.
* **Die Gier-Achse des Root-Bones ist lokal Z**, gemessen mit `axis_test.py`. Die Restmatrix legt
  lokal Y nahe — das kippt den Körper um 37°.
* **Sockeltransformationen kommen aus den Attachment-Knoten.** Selbst zusammensetzen führte zu
  „Schwertspitze 96 cm unter dem Boden", was das Bild widerlegt.
* **Handrahmen beider Hände**: lokal +Y entlang der Finger, +X über den Daumen, +Z aus der
  Handfläche. Ein Buckelschild sitzt **quer** auf dem Arm (Fläche in Armrichtung, lokal +Y), nicht
  auf der Handfläche und nicht auf dem Handrücken.
* **Blender 5**: Actions sind geschlitzt (`action.layers[].strips[].channelbags[].fcurves`), der Slot
  muss zugewiesen werden (`animation_data.action_slot`); `bone.select` gibt es nicht mehr — `nla.bake`
  mit `only_selected=False`.
* **Szene auf 30 fps.** `AttackTimeline` verlangt 0,52 / 0,58 / 0,78 s mit 0,034 s Toleranz.
* **glTF packt Rauheit in G und Metallizität in B einer Textur** — zwei getrennte Bilder lassen
  Blender eins verwerfen. Lösung: kombinierte ORM-PNG plus `Separate Color`.
* `image.pixels` liefert bei 8-Bit-Bildern **sRGB**; Umrechnung nach linear explizit machen.
* **jME blendet jede Action 0,4 s von der aktuellen Gelenkpose her ein** (`BlendableAction`,
  `transitionLength`). Ein frisches Rig steht in der Bindepose, also faltete sich jede Figur
  nach `loadRegion` aus der T-Pose in den Idle. `CharacterFactory` startet Idle deshalb bei
  0,25 s und setzt Einmalclips auf 0,1 s Blende. **Und: eine Messung bei t = 0 nach
  `restart()` misst die Bindepose, nicht den Clip** — Blende vorher auf 0 setzen.
* **`setCurrentAction(name)` schleift.** Death, Hit und Gegnerangriffe laufen über `Rig.once`
  (`setCurrentAction(name, layer, false)`): am Ende räumt die Engine die Action ab und die
  Gelenke halten das letzte Bild. `Rig.finished()` meldet das.
* **`BulletAppState` nie dem `SimpleApplication`-Konstruktor geben.** Der läuft auf dem
  Java-Hauptthread, Minie bindet den Space an diesen Thread und warnt dann bei jedem Schritt
  vom Renderthread. Anhängen in `simpleInitApp`; der Rauchlauf prüft die `jniEnvId`.
* Der Bildschirmfoto-Zeitpunkt des Rauchlaufs: `ScreenshotAppState` speichert am Ende des
  angeforderten Frames. `SmokeScenario.shoot()` fordert deshalb zwei Frames im Voraus an, sonst
  zeigt das Bild Figuren in der Bindepose.
* `AssetPipeline.pbr()` **wirft** bei Rauheit < 0,55 oder Metallizität > 0,4 (STYLE-Tor). Ein
  neues Material mit glänzendem Metall bricht den Start, nicht nur den Test.

## Offene Punkte

Entschieden und abgehakt sind: Höhlenmodule, Schreinfeuer, Dekokristalle (Kevin: „passt so"),
erste Person, Attack2/Attack3/Dodge/Cast-Fenster, Schwertspitze, STYLE-Tor. Was bleibt:

1. **Balance und Schwierigkeitsgrade** sind gerechnet, nicht gespielt (`EnemyType`,
   `EnemyBrain`-Konstanten, `TrapMechanism`, `PlayerController.PARRY_*`/`RIPOSTE_*`,
   `combat/Difficulty` — besonders der 70-%-Deckel und die Trankregel auf Sehr schwer).
2. **Schildgröße** 0,65 m — verdeckt in `Block` einen Teil des Kopfes; in der ersten Person bei
   0,16 m Kameraabstand nur am Bildrand.
3. **Echte Vorwärtsrolle**: „Stand To Roll" ist eine geradegezogene Schulterrolle. Eine neue
   Mixamo-Quelle braucht Kevins Login.
4. **Senkrechte Naht** der Höhlenwände je Zelle (`cave_builder.py`, Relief wiederholt sich).
5. **Setup-Programm** (`tools\package-windows.cmd installer`) braucht WiX 3.x — nicht installiert,
   nicht belegt. EXE ist unsigniert (SmartScreen-Hinweis).
6. **Windows-only-JAR / CI-Paket**: die JAR trägt Natives aller Plattformen (114 MB); ein
   Shade-Profil nur für Windows wäre etwa halb so groß, und `build.yml` könnte
   `tools/package-windows.cmd` auf dem Windows-Läufer aufrufen und das ZIP anhängen.
7. **Fenstersymbol im laufenden Spiel** ist nicht fotografiert (nur das Laden der PNGs getestet
   und das Launcher-Symbol aus der EXE extrahiert).

## Deine Aufgabe in dieser Session

Fang damit an, den Stand zu prüfen, bevor du etwas änderst: `mvn -B clean verify` und den
Rauchlauf fahren, `docs/assets/asset-liste.md` von hinten lesen.

Dann:

> **[HIER die konkrete Aufgabe eintragen, z. B.: „Spiel die Balance auf Mittel nach — hier meine
> Beobachtungen: …" / „Der Held soll einen Umhang bekommen." / „Bau das Setup-Programm, WiX ist
> jetzt installiert." / „Entwirf das HUD neu."]**
