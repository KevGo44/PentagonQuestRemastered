# Pentagon · Das Aschensiegel

Ein eigenständiges 3D-Dungeon-Action-Adventure in Java und jMonkeyEngine — die Neuauflage von
**PentagonQuest**. Das Ödland, der Ork-König und die ursprünglichen Gegner stammen aus dem
Original; Architektur, Welt, Geschichte und Echtzeitsysteme sind neu.

Die Kampagne führt durch **fünf Bastionen mit 33 Räumen**, zehn Aufträge, 31 Gegner samt eines
Bosses mit drei Phasen, vier ausbaubare Fähigkeiten, eine moralische Entscheidung im Verlies und
**zwei Enden mit eigener Schlusssequenz**. Krypta und Kristallhöhlen lassen sich in beliebiger
Reihenfolge erkunden.

| | |
|---|---|
| ![Die Letzte Zuflucht — Schulterkamera, Retikel und Blickmarke](docs/bilder/screenshots/zuflucht.png) | ![Die Gläserne Tiefe — Höhlenmodule und Kristalladern](docs/bilder/screenshots/glaeserne-tiefe.png) |
| ![Kampf im Verbindungsgang](docs/bilder/screenshots/kampf.png) | ![Erste Person mit Schwert und Schild](docs/bilder/screenshots/erste-person.png) |
| ![Inventar mit Charakterschablone und Ausrüstungsslots](docs/bilder/screenshots/inventar.png) | ![Epilog auf dem Aschenthron](docs/bilder/screenshots/epilog.png) |

*Bilder aus dem automatisierten Rauchlauf vom 13. September 2026, Grafikprofil „Atmosphärisch",
1440 × 900. Weitere unter [`docs/bilder/`](docs/bilder/).*

Charaktere, Dungeon-Module und Requisiten sind echte glTF-Modelle (Meshy-Generierung, in Blender
nachbearbeitet, Mixamo-Animationen); Musik und Klänge sind weiterhin synthetisch erzeugt. Was
noch Platzhalter ist und was nicht, steht in [`docs/assets/asset-liste.md`](docs/assets/asset-liste.md).

## Spielen ohne Entwicklungsumgebung

Das fertige Windows-Paket ist ein Ordner mit `Aschensiegel.exe` und eigener Java-Laufzeit — kein
installiertes Java nötig. Entpacken, starten:

```
Aschensiegel-1.0.0-windows-x64.zip  →  Aschensiegel\Aschensiegel.exe
```

Wie das Paket entsteht (auch als Setup-Programm und für macOS) beschreibt
[`docs/technik/RELEASE.md`](docs/technik/RELEASE.md). Windows zeigt beim ersten Start einer
unsignierten EXE den SmartScreen-Hinweis; „Weitere Informationen → Trotzdem ausführen".

## Aus dem Quellcode starten

Voraussetzungen: **JDK 17 oder neuer**, **Maven 3.9+**, ein OpenGL-3.3-fähiger Grafiktreiber.
Der erste Build lädt Abhängigkeiten von Maven Central.

```bat
rem Windows
mvn -B verify
run.bat
```

```bash
# macOS / Linux
./build.sh verify
./run.sh
```

`run.bat` / `./run.sh` bauen die JAR, wenn sie fehlt; **nach Quellcodeänderungen erneut `verify`
ausführen**. Das eigenständige Paket liegt danach in `target/aschensiegel-1.0.0.jar` und enthält
Engine, Assets und native Bibliotheken — es läuft überall mit `java -jar`, wo ein Java 17+ liegt.

```bash
./run.sh --fast          # Grafikprofil „Schnell" (ohne Nachbearbeitung), im Spiel auch F3
./run.sh --no-audio      # für Systeme ohne Audioausgabe
./run.sh --smoke         # automatisierter Integrationstest: 34 Stufen, 46 Prüfungen, ~100 s
```

Auf macOS setzt der Launcher `-XstartOnFirstThread`; AWT läuft für ImageIO im Headless-Modus.

## Steuerung

| Taste | Aktion |
|---|---|
| WASD / Maus | Bewegen / Kamera. Das Retikel in der Bildmitte ist die Zielrichtung für Schlag und Zauber; es färbt sich orange, sobald ein Gegner in Reichweite steht |
| **V** | Sicht umschalten: Verfolgerkamera über der rechten Schulter oder erste Person |
| Shift / Leertaste / Alt | Sprint / Sprung / Ausweichrolle (unverwundbar) |
| Linke Maustaste | Nahkampf; erneuter Klick während des Angriffs merkt den nächsten Komboschlag vor |
| Rechte Maustaste | Blocken. Innerhalb von 230 ms nach Beginn des Blockens wird **pariert**: eigene Animation, Gegner 2 s betäubt, der nächste Schlag ist eine Riposte mit 2,5-fachem Schaden |
| Q / R | Äthergeschoss / Heiltrank |
| E | Dialog, Beute, Schrein, Rätsel, Bereichswechsel, abgelegte Gegenstände aufheben |
| I / J / K / M | Inventar / Aufträge / Fähigkeiten / Karte |
| Escape | Pause oder Ansicht schließen |
| F5 / F9 | Speichern / letzten Spielstand laden |
| F3 / F10 / F12 | Grafikprofil / Ton umschalten / Screenshot |
| 1–3 | Antwort in einem Dialog auswählen |

**Einstellungen** (Hauptmenü oder Pause): Lautstärke für Gesamt, Musik und Effekte; Grafikprofil;
Sicht; **Schwierigkeit** — *Einfach*, *Mittel* oder *Sehr schwer*. Sehr schwer lässt Raum für ein,
zwei Fehler: jeder Gegner leert die Leiste in zwei bis drei Treffern, Fallen treffen doppelt, es
gibt keine Trankfunde und nur einen Trank am Schrein. Die Einstellung wirkt sofort und wird in
`settings.json` gespeichert.

Sprich zuerst mit **Mira rechts vom ersten Schrein**. Am Rastfeuer werden Leben und Ausdauer
wiederhergestellt und Heiltränke aufgefüllt (drei auf Einfach). Speicherstände sind nur außerhalb
des Kampfes, am Boden und ohne aktive Projektile möglich. Nach dem Tod wird der letzte
Speicherpunkt wiederhergestellt.

Ein paar Dinge, die das Ödland nicht erklärt: Die orange Markierung kündigt gegnerische Angriffe
an. Orks schlagen langsam und hart, Wächter sind außerhalb ihrer Angriffe gepanzert, Aschenrufer
halten Abstand und fliehen — bis sie in einer Ecke stehen. Fallen sind **nicht sichtbar**: schmale
Schlitze quer über den Gang, die auf zwei Meter auslösen; wer die Schlitze sieht, springt oder
rollt. Verlässt man eine Bastion, kehren ihre Gegner zurück; das Inventar zeigt rechts, was
angelegt ist und was ein Gegenstand bringt, und mit „Ablegen" landen Dinge auf dem Boden, wo man
sie wieder aufheben kann.

## Spielstände und Dateien

* Spielstand: `~/.pentagon-aschensiegel/saves/campaign.json`, automatische Sicherung des
  vorherigen gültigen Stands in `campaign.backup.json`.
* Einstellungen: `~/.pentagon-aschensiegel/saves/settings.json`. Eine beschädigte Datei fällt
  still auf die Standardwerte zurück und blockiert nie den Start.
* Screenshots (F12): `~/.pentagon-aschensiegel/screenshots/`.
* Rauchlauf: ausschließlich `target/smoke-saves/` und `target/screenshots/`, unabhängig vom
  normalen Spielstand.

Abweichender Speicherordner beim direkten Java-Start: `-Dpentagon.saveDir=/absoluter/pfad`
**vor** `-jar`.

## Technik und Dokumentation

Java 17 als Bytecode-Ziel, jME **3.8.1-stable**, Minie **9.0.3** für natives Bullet, LWJGL3,
Gson, JUnit 5; Abhängigkeiten fest versioniert. `mvn verify` baut die JAR und führt **72 Tests**
aus, der Rauchlauf spielt anschließend die Kampagne durch.

Die Dokumentation liegt unter [`docs/`](docs/README.md) — der Wegweiser dort sagt, was wofür ist.
Kurzfassung:

| Ordner | Inhalt |
|---|---|
| [`docs/spiel/`](docs/spiel/) | Kampagne, Systeme, Balance, Lösungsweg; Analyse des Originals |
| [`docs/technik/`](docs/technik/) | Architektur, Prüfungen und Belege, Release-Paket |
| [`docs/assets/`](docs/assets/) | Asset-Verträge, Stilanker, Dungeon-Kit, Asset-Protokoll, Stilreferenzen |
| [`docs/sessions/`](docs/sessions/) | Briefing und Übergabe-Prompt für Arbeitssitzungen |
| [`docs/bilder/`](docs/bilder/) | Aktuelle Screenshots und die Werkstattbilder der Asset-Arbeit |
| `cowork-logs/` | Build-, Test- und Messprotokolle, die die Doku zitiert |

### Was ist was im Projekt

| Datei / Ordner | Zweck | Braucht der Spieler? |
|---|---|---|
| `pom.xml` | Die Bauanleitung für Maven: welche Bibliotheken (jME, Minie, LWJGL, Gson, JUnit) in welcher Version, Java-Zielversion 17, und dass am Ende **eine** JAR mit allem drin entsteht (Shade-Plugin, Hauptklasse `de.pentagon.core.Main`) | nein |
| `src/main/java` | Der Spielcode | nein — steckt kompiliert in der JAR |
| `src/main/resources` | Modelle, Texturen, Klänge, Schriften, Symbole; landen 1:1 in der JAR | nein — steckt in der JAR |
| `src/test/java` | Die 72 Tests, laufen bei `mvn verify` | nein |
| `run.bat` / `run.sh` | Bequemer Start aus dem Quellcode: bauen die JAR, falls sie fehlt, und starten sie mit den richtigen JVM-Optionen (Windows / macOS-Linux) | nein |
| `build.sh` | Ruft Maven mit einem projektlokalen Cache (`.cache/maven`) auf; auf Windows nicht zu verwenden, dort direkt `mvn` | nein |
| `tools/` | Werkzeuge der Entwicklung: `package-windows.cmd` (EXE-Ordner + ZIP), `package-macos.sh` (App), `format.sh` (Java-Formatter), `AssetBaker.java` (erzeugt die synthetischen Texturen, Schriftatlanten und WAVs neu), `blender_export.py` (FBX/OBJ → geprüftes GLB), `make_reference_model.py` (kleines glTF-Testmodell), `icon/` (Symbol) | nein |
| `art/` | Rohmaterial und Skripte der Asset-Arbeit: Mixamo-FBX, Blender-Skripte, Zwischenstände, Sonden | nein |
| `docs/`, `cowork-logs/` | Dokumentation und Belege | nein |
| `.github/workflows/build.yml` | CI: baut und testet auf Ubuntu, macOS und Windows, wenn das Repo auf GitHub liegt | nein |
| `.run/`, `.idea/` | IntelliJ-Startkonfigurationen und -Einstellungen | nein |
| `target/` | Alles Erzeugte (von `.gitignore` ausgeschlossen): die JAR, Testberichte, der EXE-Ordner, Rauchlauf-Bilder | **die JAR bzw. der Ordner `target/native/Aschensiegel`** |

Was der Spieler bekommt, ist also nur eines von beiden: die JAR (dann braucht er ein installiertes
Java 17+) oder den Ordner mit `Aschensiegel.exe` (dann braucht er nichts). Alle Bibliotheken
stecken im Paket; zur Laufzeit wird nichts vom Rechner nachgeladen. Beim ersten Start entpackt
das Spiel seine nativen Bibliotheken (Bullet-Physik, LWJGL) nach
`%USERPROFILE%\.pentagon-aschensiegel\native\` und legt daneben Spielstände und Einstellungen ab;
das ist der einzige Ort außerhalb des Pakets, den es anfasst. Vom System selbst braucht es nur
einen Grafiktreiber mit OpenGL 3.3 und — bei der JAR — das Java.

```bash
mvn -B verify                          # Tests + ausführbares Paket
tools/package-windows.cmd              # Windows: Ordner mit EXE und Laufzeit + ZIP
./tools/package-macos.sh               # macOS: Pentagon.app
./tools/format.sh                      # google-java-format 1.27.0
java tools/AssetBaker.java             # eigene Texturen, Fontatlanten und WAVs neu erzeugen
```

Arbeitsregeln für Sitzungen mit Claude (keine Commits, Belege statt Behauptungen, Rauchlauf nach
Asset-Änderungen) stehen in [`docs/sessions/COWORK-BRIEFING.md`](docs/sessions/COWORK-BRIEFING.md).
