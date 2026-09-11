# Prüfung und Qualitätsgrenzen

Stand: 10. September 2026. Arbeitsverzeichnis: `/Users/bastian/Developer/projects/Python`. Das Ausgangsverzeichnis besitzt kein Git-Repository; deshalb gibt es keinen Commit, Push, PR oder ausgeführten GitHub-Workflow und keinen vorgetäuschten Git-Diff-Check.

## Reproduzierbare Prüfungen

```bash
./build.sh verify
./run.sh --smoke
```

Maven erstellt die eigenständig startbare JAR und führt **40 automatisierte Tests** aus:

* 8 Fortschritts-/Inventartests: mehrere Levelaufstiege, Skills, Attributänderungen, Ausdauer, Ausrüstungswechsel, Stapelverbrauch.
* 9 Kampftests: Ausrichtung/Reichweite, aktive Angriffsfenster bei großen Frames, Dreierkombo, Parieren, Blockbruch, Angriffe von hinten, Bossphasengrenzen und schnelle Projektilüberschneidungen.
* 12 Kampagnentests: alle Räume und Ziele in allen fünf Gebieten erreichbar, eindeutige IDs, Rückwege, freie Reihenfolge der Siegel, einmalige Belohnungen, erreichbare Jagd-/Chronikziele, Wegfindung um Wände.
* 7 Spielstandtests: vollständiger Roundtrip, atomarer Ersatz, beschädigte Hauptdatei, Erhalt gültiger Backups, unbekannte Version, fehlende Daten, ungültige Werte und unabhängige Snapshots.
* 4 Assettests: tatsächlicher glTF-Import, vollständiger 16-Bone-Rig mit GPU-Puffern und allen Clips, Filtermaterial-Kompatibilität und vollständiger IBL-Probe.

Der opt-in Smoke-Test startet die **echte Engine mit OpenGL und Bullet**. Er verwendet einen eigenen Speicherordner und prüft Bewegung, einen tatsächlich ausgelösten Nahkampftreffer, alle fünf gerenderten Abschnitte, Menüs, Speichern/Laden, einen unabhängigen neuen Kampagnen-Checkpoint, Runenfehler/-lösung, beide Siegel, beide Gefangenenentscheidungen, beide Bossstärken, alle Bossphasen, zehn abgeschlossene Aufträge, beide Epiloge sowie Kamerakollision. Am Ende werden Tod, Rückkehr zum Checkpoint und der Wechsel zwischen beiden Grafikprofilen geprüft. Schatten müssen beim Wiedereinschalten vor dem FilterPostProcessor stehen; eine falsche Reihenfolge erzeugt Bildspuren.

Für deterministische Kampagnenprüfungen teleportiert das Skript zwischen Zielen und besiegt Gegner teilweise durch direkte Schadensaufrufe. **Das ist kein vollständiger menschlicher Durchlauf und keine Aussage über die endgültige Schwierigkeit.** Die normale Spielfassung enthält keinen automatisch aktivierten Test-/Cheatmodus.

Ergebnisdateien:

* `target/surefire-reports/`: JUnit-Berichte.
* `target/smoke.log`: Start-, Renderer-, Audio- und Assertionsprotokoll, wenn der Aufruf dorthin umgeleitet wird.
* `target/smoke-ok.txt`: wird zu Testbeginn gelöscht und nur bei bestandenem Gesamtcheck neu geschrieben.
* `target/render-metrics.json`: gemessene Frames/FPS/95.-Perzentil inklusive Ladearbeit und Screenshots.
* `target/screenshots/`: Screenshots aus dem realen Framebuffer, keine Mockups.

## Lokal geprüfte Plattform

macOS 26.5.2, Apple Silicon / Apple M5, OpenGL 4.1 über den Apple-Treiber, Temurin Java 25.0.3 und Maven 3.9.16; Fensterauflösung 1440×900. Bytecode-Ziel ist Java 17. Windows und Linux werden als Start-/Buildziele unterstützt, wurden hier aber nicht nativ ausgeführt. Die vorbereitete CI-Matrix wird erst nach Aufnahme in ein GitHub-Repository tatsächlich laufen.

Die mit `jpackage` erzeugte **Pentagon.app mit gebündelter Java-Laufzeit** wurde direkt gestartet und über echte Maus-/Tastatureingaben geprüft: neues Spiel, Inventar und Gegenstandsauswahl, Fähigkeiten, Pause, Grafikwechsel in beide Richtungen und Rückkehr zum Hauptmenü. Die reservierte Status-Fußzeile verdeckt keine Menübuttons. Die App enthält nach SHA-256-Vergleich dieselbe JAR wie der abschließende erfolgreiche Maven-Build.

Im vollständigen grafischen Kampagnenlauf einschließlich Grafikwechsel wurden **117,25 FPS im Mittel und 13,10 ms für das 95.-Perzentil** bei 9.108 erfassten Frames gemessen. Das ist ein lokaler Messwert dieser Platzhalter-Szene, keine allgemeine Leistungsgarantie für andere Hardware oder finale Assets. Folgeläufe können abweichen; die JSON-Datei enthält den jeweiligen jüngsten Messwert.

## Umgesetzt und bewusst begrenzt

| Bereich | Geliefert | Offene Produktionsarbeit |
|---|---|---|
| Welt | 5 Abschnitte, 33 Räume, 31 Gegner, Rückwege, Erkundungskarte, Fallen | Größere Mengen individuell modellierter Architektur, vertikale Speziallevel |
| Kampf | Bullet-Controller, Combos, Parieren/Block, Dodge, Sprung, Magie, 5 Gegnertypen inkl. Boss | Längeres Balancing, anspruchsvollere Crowd-KI und weitere Movesets |
| Progression | 16 Itemarten, Stapel, Ausrüstung, Skills, Level, Händler, Checkpoints | Größere Beutetabellen, weitere Händler und Ausrüstungsmodelle |
| Geschichte | 10 Aufträge, NPC-Dialoge, Runenrätsel, zwei Entscheidungen mit Folgen | Weitere Dialogbäume, professionelles Narrative Editing und Vertonung |
| Grafik | PBR-Maps, IBL, PSSM, SSAO, Bloom, Tonemapping, begrenzte raymarchende Nebel-/Lichtschächte | Hochwertige finale Kunst, gebackene Raum-Probes, komplexere Volumetrik, finale Beleuchtung |
| Animation | Gewichtetes 16-Bone-Skelett, Hardware-Skinning, 11 AnimComposer-Clips | Professionell animierte Figuren, bessere Übergänge, Cloth/Ragdolls |
| Sound | Synthetische Ambient-/Musikstems, situative Überblendung, sieben Effekte | Aufgenommenes Foley, finale Komposition und Mixing |
| Pipeline | Echter glTF-Import, OBJ/GLB/J3O-Auflösung, Blender-FBX-Konvertierungsskript | Blender-Export hier mangels Blender-Installation noch nicht ausgeführt |
| Performance | Geometriebatches, Frustum Culling, Felsen-LOD, begrenzte Pools | Weitere Charakter-LOD-Stufen und Profile mit finalen Assets |

Die Umsetzung ist eine spielbare Kampagne und ein ausbaufähiges technisches Fundament. Sie wird ausdrücklich nicht als fertig produzierte AAA-/High-End-Veröffentlichung bezeichnet.
