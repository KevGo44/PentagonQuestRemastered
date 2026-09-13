# Übergabe-Prompt: Design- und Stilarbeit an den Assets

Diesen Text in einer neuen Session einfügen und unten die konkrete Aufgabe ergänzen.

---

Du arbeitest an **PentagonQuest / „Das Aschensiegel"**, einem Java-17-Dungeon-Action-Adventure auf
jMonkeyEngine 3.8.1 (Maven, Paket `de.pentagon`). Das Repository liegt auf meinem Rechner unter
`C:\Users\kkfre\Daten\IdeaProjects\PentagonQuestRemastered` und ist als Ordner mit dieser Session
verbunden.

**Lies zuerst die Projektdoku, bevor du irgendetwas änderst** — im Ordner `docs` steht alles als
Markdown, und deine Anpassungen müssen sich an diese Voraussetzungen halten:

* `docs/COWORK-BRIEFING.md` — die Arbeitsregeln für diese Zusammenarbeit
* `docs/ASSETS.md` — die Austauschverträge: Koordinaten, Rig, Bone-Namen, Sockets, Pivots, Clips
* `docs/STYLE.md` — die Stilgrenzen (u. a. `Metallic <= 0,4`, `Roughness >= 0,55`)
* `docs/asset-liste.md` — das Protokoll aller Assets und aller bisherigen Befunde, **das Ende
  zuerst**: dort stehen die letzten Reparaturen und die offenen Punkte
* `docs/dungeon-kit.md`, `docs/VERIFICATION.md`, `docs/ARCHITECTURE.md`, `docs/GAME_DESIGN.md`

## Arbeitsregeln

* **Keine Commits, keine Pushes.** Das mache ich selbst. Wenn der Stop-Hook wegen uncommitteter
  Änderungen meckert: einfach ignorieren.
* **Kein Abo, keine Bezahlschranke.** Bestehende Guthaben (Meshy u. a.) darfst du vollständig
  aufbrauchen, eine Reserve muss nicht bleiben. Wenn ein Dienst ein Premium-Upsell zeigt
  (z. B. parallele Generierungen bei Meshy), schließen und einspurig weiterarbeiten.
* Rohdaten im Repo sind in Ordnung, das Projekt ist privat und wird nicht veröffentlicht.
* **Wenn du Entscheidungen oder Inhalte von mir brauchst, kennzeichne das deutlich am Ende deiner
  Antwort.** Nicht im Fließtext vergraben.
* `mvn verify` muss grün bleiben: derzeit **51 Tests**. Nach jeder Asset-Änderung zusätzlich den
  Rauchlauf fahren.
* Behauptungen über Aussehen werden **gerendert, nicht gerechnet**. Zahlen belegen, Bilder
  entscheiden. Belege nach `cowork-logs/`, Protokoll nach `docs/asset-liste.md`, Verträge nach
  `docs/ASSETS.md`.

## Umgebung — was funktioniert und was nicht

`device_bash` ist auf diesem Rechner defekt (Windows-Update vom 8. September). Als Ersatz benutze
ich den Blender-MCP als Shell: `mcp__remote-devices__Blender__execute_blender_code` führt Python im
laufenden Blender aus, und darin `subprocess`.

* **Blender GUI-MCP kann kein glTF importieren** (`bpy.context.object` fehlt im MCP-Kontext).
  Für alles mit Import/Export eine eigene CLI-Instanz starten:
  `C:\Program Files\Blender Foundation\Blender 5.2\blender.exe --background --factory-startup --python <skript.py> -- <args>`
* **Maven**: `C:\Program Files\jmonkeyplatform\java\maven\bin\mvn.cmd`, dazu
  `JAVA_HOME=C:\Program Files\Java\jdk-23-temurin` setzen (nicht JDK 24/26).
  Bauen: `mvn -B verify` (~45 s). Danach liegt `target/aschensiegel-1.0.0.jar` (Shaded, mit allen
  Ressourcen).
* **Rauchlauf** (echtes Spiel, 32 Stufen, ~90 s):
  `java -jar target/aschensiegel-1.0.0.jar --smoke --no-audio --fast`
  Erfolg = `target/smoke-ok.txt` existiert; Bildschirmfotos in `target/screenshots/`.
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
  Materialien setzen.

## Sonden (fertig, unter `art/probe/`)

Alle gegen das Shaded-JAR kompilieren und mit `-cp <jar>;target\probe-classes` starten:

* `ClipSheet` — rendert alle elf Clips nebeneinander, mit der Drehung, die das Spiel anlegt.
  Das Werkzeug für Blickrichtungs- und Posenfragen.
* `SeatShots` — Sitz von Waffe und Schild in Idle, Walk und Block, aus Spielkamera, Front und nah.
* `ClearanceProbe` — Abstand Klinge zu Bein, tiefster Punkt der Spitze, Schildwinkel gegen die Front.
* `PelvisProbe`, `YawSeries` — Beckenwinkel je Clip, Mittel/Spanne und Zeitverlauf.
* `ShieldSearch`, `ShieldAim` — sucht Schildrichtungen im Handrahmen ab und bewertet die Frontdeckung.
* `FacingProbe`, `WhereProbe`, `FistProbe`, `SeatProbe` — Rohmaße an Rig und Props.

Blender-Skripte unter `art/blender/`: `anim_normalise.py` (die Clip-Reparatur, s. u.),
`axis_test.py` (Achsenmessung am Root-Bone), `enemy_pipeline.py` (Gegner-Rezept),
`kit_builder.py` (Dungeon-Module), `measure_sources.py` (Mixamo-Quellen ausmessen).

## Stand der Assets (12. September 2026)

* **Acht Charaktere** unter `src/main/resources/models/characters/`: hero, mira, eren, goblin, orc,
  warden, shaman, king. Alle elf Clips (Idle, Walk, Run, Attack1-3, Dodge, Block, Hit, Death, Cast)
  sind auf die glTF-Front normalisiert, jeder Clip **startet auf der Front**, der Idle ist der
  ruhige `sword and shield idle (4)`, eingepfropft über Copy-Transforms und Bake. Kein führender
  Ruhepose-Abtastwert mehr. Die Dateien vor dieser Reparatur liegen zum Vergleich unter
  `art/gen/pre-normalise/`.
* **Waffe und Schild**: `props/sword.glb` (2 400 Dreiecke, 0,98 m, `PQW_Steel`/`PQK_Iron`/
  `PQW_Leather`), `props/shield.glb` (204 Dreiecke, 0,65 m). Beide **griffzentriert** authored:
  Klinge auf +Y, Schildfläche auf +Z. Nur der Spieler trägt sie (`create(..., armed = true)`).
* **Dungeon-Kit**: fünfzehn Module `props/kit_*`, dazu chest, shrine, rune, seal, lore, throne,
  portal, crystal. Texturen unter `art/textures/`.
* Offen und dokumentiert: Gewichte der Gegner sind automatisch (nicht gemalt), Achselmanschette aus
  dem Armschnitt ist glatt, Roben/Schürzen sind starr gebunden und schneiden bei Walk/Run durch die
  Beine, Schamane (21 741) und König (20 375) liegen über dem Dreiecksbudget von 20 000.

## Gelernte Fallen (nicht wiederholen)

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
* Der Bildschirmfoto-Zeitpunkt des Rauchlaufs: `ScreenshotAppState` speichert am Ende des
  angeforderten Frames. `SmokeScenario.shoot()` fordert deshalb zwei Frames im Voraus an, sonst
  zeigt das Bild Figuren in der Bindepose.

## Offene Entscheidungen (die ich noch treffen muss)

1. **Attack2 dreht 180°, Attack3 96° heraus** — Inhalt der Mixamo-Drehschläge. Andere Quellen wählen
   (`slash (5)` schwingt nur ±35°) und auf 0,58 / 0,78 s umtakten? Ändert das Kampfgefühl.
2. **Schwertspitze unter dem Boden** in Dodge (−0,40 m), Death (−0,23 m), Cast (−0,24 m).
3. **Schildgröße** 0,65 m — verdeckt in `Block` einen Teil des Kopfes.
4. **Platzhaltermaterial `steel`** (Rauheit 0,38 / Metallizität 0,72) verstößt gegen STYLE, betrifft
   aber nur noch den Rückfallpfad ohne Modell.
5. **First Person** — eigener Arbeitsblock, das Pitch-Modell müsste umgebaut werden.

## Deine Aufgabe in dieser Session

Fang damit an, den Stand zu prüfen, bevor du etwas änderst: `mvn -B verify` fahren und einen
Renderlauf (`SeatShots` oder `ClipSheet`) ansehen, damit du weißt, wovon du ausgehst.

Dann:

> **[HIER die konkrete Aufgabe eintragen, z. B.: „Ich will den Stil des Dungeons dunkler und
> kontrastreicher — Wände, Boden, Licht." / „Der Held soll einen Umhang bekommen." / „Die
> Gegnermaterialien wirken zu glänzend, bring sie auf STYLE." / „Entwirf das HUD neu."]**
