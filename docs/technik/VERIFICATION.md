# Prüfung und Qualitätsgrenzen

Ursprünglicher Stand: 10. September 2026, Arbeitsverzeichnis `/Users/bastian/Developer/projects/Python`, ohne Git-Repository — deshalb kein Commit, Push, PR oder ausgeführter GitHub-Workflow und kein vorgetäuschter Git-Diff-Check.

**Fortgeschrieben am 12. September 2026** auf einem anderen Rechner: Windows, `C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered`. Die Messwerte des ursprünglichen macOS-Abschnitts weiter unten bleiben unverändert stehen — sie waren dort echt und sind hier **nicht** nachgemessen worden.

## Reproduzierbare Prüfungen

```bash
./build.sh verify
./run.sh --smoke
```

Auf Windows ist `build.sh` nicht zu verwenden (`bash` zeigt dort auf WSL). Der belegte Aufruf lautet:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-23-temurin"
& "C:\Program Files\jmonkeyplatform\java\maven\bin\mvn.cmd" -B verify
java -jar target\aschensiegel-1.0.0.jar --smoke --fast --no-audio
```

Maven erstellt die eigenständig startbare JAR und führt **72 automatisierte Tests** aus:

* 8 Fortschritts-/Inventartests: mehrere Levelaufstiege, Skills, Attributänderungen, Ausdauer, Ausrüstungswechsel, Stapelverbrauch.
* 13 Kampftests: Ausrichtung/Reichweite, aktive Angriffsfenster bei großen Frames, Dreierkombo, Parieren, Blockbruch, Angriffe von hinten, Bossphasengrenzen, schnelle Projektilüberschneidungen — und seit dem 13. September die **Falle** (Gehen und Sprinten werden gefangen, eine Rolle aufs Klicken nicht, eine späte Rolle doch; Nachbargang, Stehenbleiben, Wiederbewaffnung, Sprung), der **eingekesselte Aschenrufer** (keine Flucht aus der Ecke, Flucht aus dem offenen Raum) und die **Papierbalance** der Gegnerwerte.
* 19 Kampagnentests: alle Räume und Ziele in allen fünf Gebieten erreichbar, eindeutige IDs, Rückwege, freie Reihenfolge der Siegel, einmalige Belohnungen, erreichbare Jagd-/Chronikziele, Wegfindung um Wände; seit dem 13. September je Gebiet, dass **jede Falle ihren Gang von Wand zu Wand füllt** und in keinem Raum liegt, das **Wiederbeleben je Ebene** (der gefallene König bleibt liegen) und die gespeicherten **Ablagen**.
* 7 Spielstandtests: vollständiger Roundtrip, atomarer Ersatz, beschädigte Hauptdatei, Erhalt gültiger Backups, unbekannte Version, fehlende Daten, ungültige Werte und unabhängige Snapshots.
* **12 Assettests**: tatsächlicher glTF-Import, das Flammen-Drehteil des Schreins, der optionale Parry-Clip des Helden, Filtermaterial-Kompatibilität, vollständiger IBL-Probe, der streambare rückspulbare Radiotitel — und der erweiterte Liefervertrag über **alle acht** vorhandenen Charaktere (`hero`, `mira`, `eren`, `goblin`, `orc`, `warden`, `shaman`, `king`): Joints, alle elf Clips mit nichtleeren Tracks und Länge > 0, Bindepose-, Bone-Index- und Bone-Weight-Puffer sowie die Angriffslängen gegen `combat/AttackTimeline` mit einer Toleranz von einem Frame (0,034 s). Seit dem 12. September prüft der Test zusätzlich, dass **jeder Clip Bewegung enthält** — mindestens ein Joint dreht sich um mehr als ein halbes Grad gegen den ersten Keyframe — und seit dem 13. September, dass **kein Clip Wurzelbewegung trägt** (Hüfte horizontal innerhalb von 0,35 m; Attack2 lief vorher 3,16 m voraus) und dass **die Rolle gerade läuft** (Beckenlinie auf jedem Bild innerhalb von 20° der Front). Die Beckenmessung projiziert die Aufwärtsachse des Rigs heraus (Root → Hips), nicht y: der Gelenkraum der gelieferten Rigs hat die Höhe entlang −Z. Diese Zusicherung fehlte, und `hero.glb` ist deshalb mit 66 flach abgetasteten Tracks durchgekommen: richtige Keyframe-Zahl, identische Werte, Figur im Spiel eingefroren.
* 3 Schwierigkeitstests: Einfach ist der abgestimmte Stand, Sehr schwer lässt jedem Gegnertyp zwei bis drei Treffer bis zur leeren Leiste und deckelt jeden Treffer bei 70 %, Mittel liegt dazwischen.
* 1 Starttest: die sechs Fenstersymbole (`icons/aschensiegel-256…16.png`) liegen im Klassenpfad, kommen größtenmäßig sortiert und behalten in 16 px die Glutfarbe in der Mitte.
* 8 Einstellungstests: Roundtrip aller Kanäle samt Schwierigkeit und Sicht, fehlende Datei, beschädigtes JSON, unbekannte Version, übergroße Datei, Begrenzung beim Schreiben und Lesen sowie der Lautstärke-Clamp.

Der opt-in Smoke-Test startet die **echte Engine mit OpenGL und Bullet**. Er verwendet einen eigenen Speicherordner und prüft Bewegung, einen tatsächlich ausgelösten Nahkampftreffer, alle fünf gerenderten Abschnitte, Menüs, Speichern/Laden, einen unabhängigen neuen Kampagnen-Checkpoint, Runenfehler/-lösung, beide Siegel, beide Gefangenenentscheidungen, beide Bossstärken, alle Bossphasen, zehn abgeschlossene Aufträge, beide Abschlusssequenzen (Untertitel, Kamera am Thron, Endseite) sowie Kamerakollision. Am Ende werden Tod, Rückkehr zum Checkpoint, der Wechsel zwischen beiden Grafikprofilen und die **Korridorfalle** geprüft (löst auf zwei Meter aus, trifft den Stehenden nicht, fängt den auf dem Streifen), die **erste Person** (Kamera am Kopfknoten, Kopf-Joint zusammengeklappt, Schwert und Schild im Bild) und die **Schwierigkeit** (auf Sehr schwer nimmt ein Königshieb 60–70 % der Leiste, nie alles) — **34 Stufen, 46 Prüfungen**. Schatten müssen beim Wiedereinschalten vor dem FilterPostProcessor stehen; eine falsche Reihenfolge erzeugt Bildspuren.

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
| Welt | 5 Abschnitte, 33 Räume, 31 Gegner, Rückwege, Erkundungskarte, Fallen, **vollständiger modularer Dungeon-Kit** | Vertikale Speziallevel, gemalte statt prozeduraler Texturen |
| Kampf | Bullet-Controller, Combos, Parade mit eigenem Clip, Betäubung und Riposte, Dodge, Sprung, Magie, 5 Gegnertypen inkl. Boss, gangbreite Fallen, Wiederbeleben je Ebene | Gespieltes Balancing (die Werte vom 13. September sind gerechnet), anspruchsvollere Crowd-KI und weitere Movesets |
| Progression | 16 Itemarten, Stapel, Ausrüstung, Skills, Level, Händler, Checkpoints | Größere Beutetabellen, weitere Händler und Ausrüstungsmodelle |
| Geschichte | 10 Aufträge, NPC-Dialoge, Runenrätsel, zwei Entscheidungen mit Folgen | Weitere Dialogbäume, professionelles Narrative Editing und Vertonung |
| Grafik | PBR-Maps, IBL, PSSM, SSAO, Bloom, Tonemapping, begrenzte raymarchende Nebel-/Lichtschächte | Hochwertige finale Kunst, gebackene Raum-Probes, komplexere Volumetrik, finale Beleuchtung |
| Animation | Gewichtetes Skelett (26 Joints bei den Gegnern, 66 bei Held, Mira und Eren), Hardware-Skinning, 11 AnimComposer-Clips je Figur | Professionell animierte Figuren, bessere Übergänge, **Cloth** — Roben und Schürzen sind starr gebunden und schneiden bei Walk/Run durch die Beine |
| Sound | Synthetische Ambient-/Musikstems, situative Überblendung, sieben Effekte | Aufgenommenes Foley, finale Komposition und Mixing |
| Pipeline | Echter glTF-Import, OBJ/GLB/J3O-Auflösung, **Blender 5.2.1 im Einsatz**: fünf Gegner, fünfzehn Kit-Module und sieben Props exportiert und in der Engine nachgemessen | Externe Texturreferenzen statt eingebetteter Maps; gemalte Gewichte statt automatischer |
| Performance | Geometriebatches, Frustum Culling, Felsen-LOD, begrenzte Pools | Weitere Charakter-LOD-Stufen und Profile mit finalen Assets |

Die Umsetzung ist eine spielbare Kampagne und ein ausbaufähiges technisches Fundament. Sie wird ausdrücklich nicht als fertig produzierte AAA-/High-End-Veröffentlichung bezeichnet.


---

## Belege der Läufe vom 12. September 2026

Alle Protokolle liegen unter `cowork-logs/` und sind bewusst versioniert (`.gitignore` hat dafür eine Ausnahme, weil `*.log` sonst greift).

| Lauf | Beleg | Ergebnis |
|---|---|---|
| Gegner vollständig | `verify-alle-gegner.log` | BUILD SUCCESS, 50 Tests, `AssetTest` über acht IDs |
| Gegner vollständig | `smoke-alle-gegner.log` | alle 33 Stufen, `smoke-ok.txt`, 13 Screenshots, keine Exception |
| Dungeon-Kit | `verify-kit-komplett.log`, `smoke-kit-komplett.log` | dito |
| Kit-Module in der Engine | `kitprobe-module-set.log`, `kitprobe-deko-set.log` | 15 Module, Maße und Pivots wie in `docs/assets/dungeon-kit.md` |
| Props in der Engine | `kitprobe-props.log` | 7 Props, Pivot unten mittig bestätigt |
| Batch-Messung | `batchprobe.log` | 193 Geometrien → **6 Batches** in 14 ms |
| Funktionsprüfung, vorher | `mvn-verify-baseline.log`, `smoke-baseline.log` | 51 Tests grün; Rauchlauf grün, aber **2 894** Minie-Warnungen „invoked from wrong thread" |
| Funktionsprüfung, nachher | `mvn-verify-fix2.log`, `smoke-fix1.log` | **54 Tests**, 36 Rauchlauf-Prüfungen, **0** Thread-Warnungen; Physik-Thread, Leichen am Boden und Held am Boden werden jetzt geprüft |
| Funktionsprüfung, Endstand | `mvn-verify-final.log`, `smoke-final.log` | BUILD SUCCESS, 54 Tests; Rauchlauf 32 Stufen, `smoke-ok.txt`, 36 Prüfungen, 0 Thread-Warnungen (nach google-java-format) |
| Clips neu geschnitten | `verify-smoke-polish.log`, `anim_polish-hero.log`, `anim_polish-rest.log` | 55 Tests, Rauchlauf grün; Attack2 volle Drehung, Attack3/Dodge/Cast in ihren Fenstern, alle acht Figuren |
| Ruheclips, Props, Höhlen, Licht | `verify-smoke-npcidle.log`, `verify-smoke-props.log`, `verify-smoke-caverns.log`, `verify-smoke-light.log` | je 55 Tests und Rauchlauf grün; Bilder unter `target/probe-shots/room-*.png`, `strip-mira-Idle-*.png`, `strip-eren-Idle-*.png` |
| Clip-Zeitachsen | `cliptimeline-hero.log`, `restlead.log` | Schlagmoment, Entladung, Bodenkontakt und Rollenspanne der gelieferten Clips |
| Hüftdrift | `cliptimeline-drift.log`, `cliptimeline-drift-all.log`, `anim_pin-hero.log`, `anim_pin-rest.log`, `anim_pin-all.log` | Attack2 3,16 → 0,000 m auf acht Rigs, Mira Dodge 1,22 → 0 |
| Kampf, Fallen, Parade, Epilog, Wiederbeleben | `verify-smoke-kampf.log` | `clean verify` BUILD SUCCESS, 67 Tests; Rauchlauf 34 Stufen, 42 Prüfungen, 0 Thread-Warnungen |
| Doku neu geordnet, Fenstersymbol, Windows-Paket | `verify-release.log`, `jdeps-modules.log`, `package-windows.log`, `smoke-exe.log`, `smoke-screenshots-hq.log` | BUILD SUCCESS, **72 Tests**; `Aschensiegel.exe --smoke` 46 Prüfungen, `smoke-ok.txt`; Rauchlauf ohne `--fast` für die README-Bilder (`docs/bilder/screenshots/`); Einzelheiten in `RELEASE.md` |
| Zielen über den Kamerastrahl, erste Person mit Waffe, gerade Rolle, Inventar mit Figur | `verify-smoke-zielen.log`, `anim_pin-dodge.log`, `dodgeyaw-after.log` | BUILD SUCCESS, 71 Tests; Rauchlauf 46 Prüfungen; Bilder `target/screenshots/pentagon-3.png` (Inventar), `pentagon-13.png` (erste Person mit Schwert und Schild) |
| Schwierigkeitsgrade, erste Person, Schulterkamera | `verify-difficulty.log`, `smoke-difficulty.log`, `verify-smoke-sicht.log` | BUILD SUCCESS, **71 Tests**; Rauchlauf 46 Prüfungen; Bilder `target/screenshots/pentagon-2.png` (Schulterkamera, Retikel, Blickmarke), `pentagon-12.png` (erste Person) |
| Schreinfeuer, Höhlenmodule | `verify-smoke-hoehlen.log`, `verify-smoke-final3.log`, `cave_builder.log`, `roomshots-caves2.log` | BUILD SUCCESS, **68 Tests**; Rauchlauf 42 Prüfungen; Bilder `target/probe-shots/room-caverns-*.png`, `room-refuge-1.png`, `target/screenshots/pentagon-11.png` (Epilog), `pentagon-13.png` (Falle) |

**Was diese Läufe nicht abdecken:** `verify` prüft, dass Bindepose-Puffer *vorhanden* sind, nicht dass sie sinnvolle Werte tragen. Beim Ork-König waren nach dem Freischnitt 0 von 9 998 Vertices gewichtet — `verify` wäre grün geblieben, die Figur im Spiel ein Klumpen am Modellursprung. Gefunden hat das nur eine Messung in Blender. Der Sichttest bleibt also Pflicht, und die Bindepose wird mit `armature.data.pose_position = "REST"` gerendert, nicht nach dem Export mit aktiven NLA-Spuren.

Nicht nachgemessen auf diesem Rechner: FPS und Perzentile, die CI-Matrix und der macOS-Abschnitt oben. `jpackage` ist seit dem 13. September auch hier belegt (Windows-Ordner mit EXE, siehe [RELEASE.md](RELEASE.md)); das Setup-Programm nicht, weil WiX fehlt.
