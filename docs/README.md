# Dokumentation — Wegweiser

Stand 13. September 2026. Alles ist Markdown; Bilder liegen als PNG daneben. Die Doku ist nach
**Zweck** sortiert, nicht nach Entstehungszeit — wer etwas sucht, geht über diese Tabelle.

| Ordner | Datei | Wofür | Lesen, wenn … |
|---|---|---|---|
| `spiel/` | [GAME_DESIGN.md](spiel/GAME_DESIGN.md) | Die fünf Bastionen, Aufträge, Gegner, Fähigkeiten, Balance, Lösungsweg | … du wissen willst, was das Spiel *enthält* |
| `spiel/` | [ORIGINAL.md](spiel/ORIGINAL.md) | Analyse des Python-Originals „Die PentagonQuest" und was davon übernommen wurde | … es um die Herkunft geht |
| `technik/` | [ARCHITECTURE.md](technik/ARCHITECTURE.md) | Schichten, Pakete, Datenfluss, Erweiterungspunkte | … du Code änderst oder ergänzt |
| `technik/` | [VERIFICATION.md](technik/VERIFICATION.md) | Was geprüft ist, wie, und mit welchem Beleg; bekannte Grenzen | … du wissen willst, was *wirklich* gesichert ist |
| `technik/` | [RELEASE.md](technik/RELEASE.md) | JAR, Windows-Ordner mit EXE, Setup, macOS-App: bauen und weitergeben | … jemand das Spiel ohne IDE spielen soll |
| `assets/` | [ASSETS.md](assets/ASSETS.md) | **Die Austauschverträge**: Pfade, Maße, Pivots, Rig, Bone-Namen, Sockets, Clips, Modul-IDs | … ein Modell, eine Textur oder ein Clip ins Spiel soll |
| `assets/` | [STYLE.md](assets/STYLE.md) | Stilanker: Farbwelt, Materialgrenzen (`Roughness ≥ 0,55`, `Metallic ≤ 0,4`), Meshy-Prompts | … du etwas Neues *gestaltest* |
| `assets/` | [dungeon-kit.md](assets/dungeon-kit.md) | Bauvorlage des modularen Dungeon-Kits mit jeder Maßangabe samt Fundstelle | … Module oder Räume gebaut werden |
| `assets/` | [asset-liste.md](assets/asset-liste.md) | **Das Protokoll**: jedes Asset, jeder Befund, jede Reparatur, chronologisch — das Ende zuerst lesen | … du wissen willst, was zuletzt passiert ist und warum |
| `assets/referenz/` | Bilder | Stilreferenzen, Maßstab, Materialtest, Kandidaten | … STYLE.md ein Bild zitiert |
| `sessions/` | [COWORK-BRIEFING.md](sessions/COWORK-BRIEFING.md) | Arbeitsregeln und Verträge für Sitzungen, die das Projekt nicht kennen | … eine neue Sitzung (Mensch oder Claude) anfängt |
| `sessions/` | [PROMPT-FOLGESESSION.md](sessions/PROMPT-FOLGESESSION.md) | Fertiger Übergabe-Prompt: Umgebung, Stolperfallen, offene Punkte | … du eine Folgesitzung startest |
| `bilder/screenshots/` | Bilder | Aktuelle Spielbilder aus dem Rauchlauf (Grafikprofil „Atmosphärisch") | … das README oder eine Präsentation Bilder braucht |
| `bilder/werkstatt/` | Bilder | Werkstattbilder der Asset-Arbeit: Meshy-Rohlinge, Bindeposen, Posenbögen, Vorher/Nachher | … ein Protokolleintrag ein Bild erwähnt |

Belege (Build-, Test- und Messprotokolle) liegen außerhalb der Doku in [`../cowork-logs/`](../cowork-logs/);
die Doku zitiert sie mit Dateinamen.

## Lesereihenfolge für eine neue Sitzung

1. [sessions/COWORK-BRIEFING.md](sessions/COWORK-BRIEFING.md) — Regeln und der Asset-Vertrag in Kurzform.
2. [assets/asset-liste.md](assets/asset-liste.md), **von hinten**: die letzten Abschnitte sind der aktuelle Stand.
3. Je nach Aufgabe [assets/ASSETS.md](assets/ASSETS.md) (Verträge), [technik/ARCHITECTURE.md](technik/ARCHITECTURE.md) (Code)
   oder [spiel/GAME_DESIGN.md](spiel/GAME_DESIGN.md) (Inhalt).
4. Vor jeder Erfolgsmeldung [technik/VERIFICATION.md](technik/VERIFICATION.md): dort steht, was als Beleg zählt.

## Wohin neue Inhalte gehören

* **Verträge** (was ein Asset erfüllen muss) → `assets/ASSETS.md`; Stilentscheidungen → `assets/STYLE.md`.
* **Protokoll** (was getan wurde, Messwerte, Bilder, Sackgassen) → ans Ende von `assets/asset-liste.md`.
* **Prüfungen** (Tests, Rauchlaufprüfungen, Belegzeilen) → `technik/VERIFICATION.md`.
* **Bilder**: Spielbilder nach `bilder/screenshots/`, Arbeitsbilder nach `bilder/werkstatt/`, Stilreferenzen nach `assets/referenz/`.
* **Logs** → `../cowork-logs/`, sprechend benannt (`verify-<thema>.log`, `smoke-<thema>.log`).

Die Verzeichnisstruktur wurde am 13. September 2026 so geordnet; vorher lagen alle Dateien flach in
`docs/`, die Stilreferenzen unter `docs/style-reference/` und die Werkstattbilder in einem Ordner
`Claude outputs/` im Projektwurzelverzeichnis. Alle Verweise in Doku, Code-Kommentaren und
Blender-Skripten wurden mitgezogen.
