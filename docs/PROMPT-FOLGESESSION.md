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
* `mvn verify` muss grün bleiben: derzeit **68 Tests**. Nach jeder Asset-Änderung zusätzlich den
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
* **Rauchlauf** (echtes Spiel, 34 Stufen, 42 Prüfungen, ~100 s):
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
* `ClipTimeline` — Hüft-, Kopf- und Handhöhe plus Handgeschwindigkeit alle 1/30 s je Clip;
  daraus stammen Schlagmoment (Attack3: 0,28 s), Entladung (Cast: 1,6 s), Rollenspanne (Dodge:
  0,3–2,0 s) und Bodenkontakt (Death: 1,7 s). Der Gelenk-Modellraum ist Blenders, Höhe = −Z.
* `RestLead` — größte Gelenkabweichung von der Bindepose je Keyframe (führender Ruhepose-Wert?).
* `Lineup` — alle acht Figuren in Spielgröße, alle Props und fünf Kit-Module, fünf Kameras.
* `ClipStrip <figur> <clip>` — zwölf Standbilder eines Clips nebeneinander, Front und Draufsicht;
  das Werkzeug für Drehungen, Rollen und Schlagmomente.
* `RoomShots REGION id...` — die echte Region wie `WorldView` sie baut (Kit, Tönung, Fackeln,
  `SceneLighting`), fotografiert vor dem benannten Objekt; die Sonde für Props und Licht.

Blender-Skripte `art/blender/npc_idle.py` (Ruheclips Mira/Eren aus `art/mixamo/npc/`),
`props_builder.py` (Thron, Altäre, Schrein), `rock_builder.py` (`kit_rock`) und `anim_polish.py`: zweiter Durchlauf (Neupfropfen und Stauchen von
Attack2, Attack3, Cast; Stauchen von Dodge; Front-Rampe, damit ein Clip auch vorn endet).

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

* `mvn clean` löscht `target/probe-shots` und `target/probe-classes`; die Sonden danach neu
  übersetzen (`javac -cp target\aschensiegel-1.0.0.jar -d target\probe-classes art\probe\X.java`).
* Nach `device_commit_files` nur mit `mvn -B clean verify` bauen: ein Rauchlauf lief einmal mit
  einem Stand, der die gerade geschriebene Datei nicht enthielt.
* Wurzelbewegung in Mixamo-Clips steckt im Hips-Knochen; `art/probe/ClipTimeline` zeigt sie als
  `hipsDrift`, `art/blender/anim_pin.py` entfernt sie. Der Test lässt 0,35 m zu.
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

## Offene Entscheidungen (die ich noch treffen muss)

0. ~~Dodge und Cast passen nicht in ihre Spielfenster~~ — erledigt am 13. September mit
   `art/blender/anim_polish.py` (Dodge 0,767 s, Cast 0,6 s).

1. ~~Attack2 dreht 180°, Attack3 96° heraus~~ — erledigt: Attack2 ist jetzt die volle 360°-Drehung
   aus `attack (2)`, gestaucht statt geschnitten, Attack3 der Vorwärtshieb `attack (4)`. Attack2
   trifft rundum (Kosinus −1) — falls zu stark, eine Zahl in `CombatSystem`.
2. **Schwertspitze unter dem Boden** — seit dem gewöhnlichen Griff (Klinge auf der Daumenseite, 13. September) nur noch Dodge −0,21 m, Death −0,05 m, Cast −0,03 m; praktisch erledigt.
3. **Schildgröße** 0,65 m — verdeckt in `Block` einen Teil des Kopfes.
6. ~~Höhlenmodule~~ — erledigt am 13. September: neun Felsmodule aus `art/blender/cave_builder.py`
   (`kit_*_cave*`), Relief je Zelle, zwei Varianten. Offen bleibt die senkrechte Naht je Zelle.
7. ~~Schreinkristall~~ — erledigt: der Schrein ist ein Feuer (`FlameShapes`, `WorldView.flame`).
8. **Balance** vom 13. September ist gerechnet, nicht gespielt (`EnemyType`, `EnemyBrain`-Konstanten,
   `TrapMechanism`, `PlayerController.PARRY_*`/`RIPOSTE_*`) — beim Spielen nachziehen.
9. ~~Dekokristalle sattes Cyan~~ — abgedunkelt (`0x1f5d5c` Deko, `0x3d9e98` Adern).
4. ~~Platzhaltermaterial `steel` verstößt gegen STYLE~~ — erledigt; `AssetPipeline.pbr` weist
   STYLE-Verstöße jetzt ab.
5. **First Person** — eigener Arbeitsblock, das Pitch-Modell müsste umgebaut werden.

## Deine Aufgabe in dieser Session

Fang damit an, den Stand zu prüfen, bevor du etwas änderst: `mvn -B verify` fahren und einen
Renderlauf (`SeatShots` oder `ClipSheet`) ansehen, damit du weißt, wovon du ausgehst.

Dann:

> **[HIER die konkrete Aufgabe eintragen, z. B.: „Ich will den Stil des Dungeons dunkler und
> kontrastreicher — Wände, Boden, Licht." / „Der Held soll einen Umhang bekommen." / „Die
> Gegnermaterialien wirken zu glänzend, bring sie auf STYLE." / „Entwirf das HUD neu."]**
