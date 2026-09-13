# Release-Paket: JAR, Windows-EXE, Setup, macOS-App

Stand 13. September 2026. Alles hier wurde auf dem Windows-Rechner tatsächlich gebaut und
gestartet; die Belege stehen am Ende.

## Drei Stufen

| Stufe | Was der Spieler bekommt | Braucht auf dem Zielrechner | Größe |
|---|---|---|---|
| **JAR** | `target/aschensiegel-1.0.0.jar` — eine Datei mit Spiel, Engine, Assets und den nativen Bibliotheken aller Plattformen | ein installiertes **Java 17 oder neuer** | 114 MB |
| **Windows-Ordner** | `Aschensiegel\Aschensiegel.exe` mit eigener Java-Laufzeit, als `Aschensiegel-1.0.0-windows-x64.zip` | nichts | 140 MB gepackt, 194 MB entpackt |
| **Setup** | `Aschensiegel-1.0.0.exe`, installiert nach `%LOCALAPPDATA%\Aschensiegel`, Startmenü und Desktop-Verknüpfung | nichts; zum *Bauen* das WiX Toolset 3.x | wie der Ordner |

Für macOS erzeugt `tools/package-macos.sh` auf einem Mac dieselbe Stufe zwei als `Pentagon.app`.

## Stufe 1 — die JAR

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-23-temurin"
mvn -B clean verify
java -jar target\aschensiegel-1.0.0.jar
```

`verify` führt vorher die 72 Tests aus; `package` würde sie überspringen, ist aber nicht der
belegte Weg. Die JAR ist ein Shade-Paket (`maven-shade-plugin`, Hauptklasse `de.pentagon.core.Main`),
startet mit `java -jar` unter Windows, Linux und macOS (dort mit `-XstartOnFirstThread`) und
nimmt dieselben Optionen wie die Startskripte: `--fast`, `--no-audio`, `--smoke`.

Warum 114 MB: LWJGL 3 und Minie liefern ihre nativen Bibliotheken für Windows, Linux und macOS
(x64 und ARM) mit, dazu die jME-Shader und die eingebetteten Texturen der glTF-Modelle. Eine
Windows-only-JAR über Shade-Filter wäre etwa halb so groß — bisher nicht gemacht, weil die eine
JAR auf allen drei Systemen läuft.

## Stufe 2 — Windows-Ordner mit EXE

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-23-temurin"
tools\package-windows.cmd
```

Das Skript (`tools/package-windows.cmd`) macht Folgendes:

1. Baut die JAR mit Maven, falls sie fehlt (nach Quellcodeänderungen vorher selbst `mvn -B clean verify`).
2. Ruft **`jpackage --type app-image`** aus dem JDK auf. jpackage legt einen Ordner an, in dem
   ein nativer Launcher `Aschensiegel.exe` die JAR mit einer per `jlink` verkleinerten Java-Laufzeit
   startet. Die Laufzeit enthält nur die Module, die `jdeps --print-module-deps` für die JAR
   nennt (`java.base, java.compiler, java.desktop, java.management, java.prefs, java.sql,
   jdk.unsupported`, dazu deren Abhängigkeiten) — 80 MB statt einer vollen JDK-Kopie.
3. Schreibt die JVM-Optionen in `app\Aschensiegel.cfg`: `--enable-native-access=ALL-UNNAMED`,
   `-Xms256m`, `-Xmx2g` — dieselben wie in `run.bat`.
4. Setzt das Symbol `tools/icon/aschensiegel.ico` (der Glut-Fünfeck; dieselbe Grafik liegt als
   `src/main/resources/icons/aschensiegel-*.png` und wird von `Main.windowIcons()` als Fenster-
   und Taskleistensymbol gesetzt).
5. Packt den Ordner mit PowerShell `Compress-Archive` zu `target\native\Aschensiegel-1.0.0-windows-x64.zip`.

Ergebnis:

```
target\native\Aschensiegel\
  Aschensiegel.exe          Launcher (nimmt Kommandozeilenoptionen entgegen, z. B. --fast)
  app\aschensiegel-1.0.0.jar
  app\Aschensiegel.cfg      Klassenpfad, Hauptklasse, JVM-Optionen
  runtime\                  Java 23.0.2, 11 Module
target\native\Aschensiegel-1.0.0-windows-x64.zip
```

Der Ordner ist **portabel**: an beliebiger Stelle entpacken und `Aschensiegel.exe` starten.
Spielstände landen wie beim JAR-Start unter `%USERPROFILE%\.pentagon-aschensiegel\saves\`, die
nativen Bibliotheken werden beim ersten Start daneben nach `native\` entpackt. Der Launcher
öffnet **kein Konsolenfenster**; wer die Protokollausgabe braucht, startet die JAR in `app\`
direkt mit `runtime\bin\java.exe -jar app\aschensiegel-1.0.0.jar`.

Die EXE ist **nicht signiert**. Windows zeigt beim ersten Start den SmartScreen-Hinweis
(„Der Computer wurde durch Windows geschützt"): *Weitere Informationen → Trotzdem ausführen*.
Ein Code-Signing-Zertifikat würde das beheben; das ist eine Kaufentscheidung, keine technische.

## Stufe 3 — Setup-Programm

```bat
winget install WiXToolset.WiXToolset     -- einmalig, WiX 3.x (candle.exe / light.exe)
tools\package-windows.cmd installer
```

Dasselbe Skript ruft danach `jpackage --type exe` mit `--win-menu --win-shortcut
--win-dir-chooser --win-per-user-install` auf und erzeugt `target\native\Aschensiegel-1.0.0.exe`.
Ohne WiX im `PATH` bricht das Skript nach dem Ordner mit einem Hinweis ab (Rückgabewert 2) — der
Ordner und das ZIP sind dann trotzdem fertig. **Auf diesem Rechner ist WiX nicht installiert;
Stufe 3 ist daher beschrieben, aber nicht belegt.**

## Was in Git liegt und was nicht

Versioniert: `tools/package-windows.cmd`, `tools/package-macos.sh`, `tools/icon/`,
`src/main/resources/icons/`. **Nicht** versioniert (unter `target/`): JAR, Ordner, ZIP, Setup.
Wer das Paket weitergeben will, hängt das ZIP an ein GitHub-Release oder legt es sonstwo ab; die
CI-Matrix in `.github/workflows/build.yml` baut bisher nur die JAR als Artefakt.

## Belege

| Schritt | Beleg | Ergebnis |
|---|---|---|
| Build mit Symbolen und Starttest | `cowork-logs/verify-release.log` | `clean verify` BUILD SUCCESS, **72 Tests** (`LaunchTest`: sechs Symbolgrößen, 256 → 16 px, Glutfarbe in der Mitte) |
| Modulbedarf | `cowork-logs/jdeps-modules.log` | `java.base,java.compiler,java.desktop,java.management,java.prefs,java.sql,jdk.unsupported` |
| Paket | `cowork-logs/package-windows.log` | jpackage app-image in 34 s; Ordner 194 MB / 159 Dateien; ZIP 140 MB |
| Start der EXE | `cowork-logs/smoke-exe.log`, `target/smoke-ok.txt` | `Aschensiegel.exe --smoke --no-audio --fast` aus dem Projektordner, Ausgabe umgeleitet: 34 Stufen, **46 Prüfungen**, 0 Thread-Warnungen, `smoke-ok.txt` neu geschrieben — die EXE spielt dieselbe Kampagne durch wie die JAR |
| Launcher-Symbol | `cowork-logs/exe-icon.log`, `target/native/exe-icon.png` | aus der EXE extrahiert (`ExtractAssociatedIcon`, 32 px): der Glut-Fünfeck |

Nicht belegt: das Setup (kein WiX auf diesem Rechner) und das Fenstersymbol im laufenden Spiel —
`LaunchTest` prüft nur, dass die sechs PNGs geladen werden; dass LWJGL sie ans Fenster hängt, ist
nicht fotografiert.
