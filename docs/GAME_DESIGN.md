# Kampagne und Spielsysteme

## Die fünf Bastionen

| Abschnitt | Räume | Inhalt |
|---|---:|---|
| Letzte Zuflucht | 7 | Mira, Händler, erstes Rastfeuer, zwei Plünderer, Archiv, drei Tore |
| Krypta der Eide | 7 | Drei Runengruften, Chronikhinweis, Wächter, Aschenrufer, Totensiegel |
| Gläserne Tiefe | 7 | Drei Kristalladern, mehrere Goblin-Gruppen, Schamanen, Kristallherz |
| Kettenverlies | 7 | Waffenlager, Eren, moralische Entscheidung, Kerkermeister, Thronschlüssel |
| Aschenthron | 5 | Letztes Feuer, Seitenkapellen, Vorräte, Bossarena, Epilogentscheidung |

Die Abschnitte enthalten insgesamt 31 platzierte Gegner. Gegner und Truhen regenerieren nach einem Gebietswechsel nicht. Das macht Erkundung und Rückwege dauerhaft sicherer; Rastfeuer dienen der Erholung und nicht als endlose XP-Quelle.

## Ziele und Belohnungen

Zehn Aufträge werden im Journal sichtbar, sobald ihr Kontext bekannt ist. Abschlüsse werden mit stabilen IDs gespeichert; ein bereits belohnter Auftrag vergibt keine zweite Belohnung. Es gibt sowohl die Hauptquest als auch Goblin-Jagd und die Suche nach allen fünf Chronikfragmenten. Die Jagd belohnt mit einer besonderen Klinge, die Chronik mit einer besonderen Rüstung.

Levelaufstiege erfordern `100 × aktuelle Stufe` EP, geben +15 maximales Leben, +3 Grundschaden und einen Fähigkeitspunkt. Vier Fähigkeiten besitzen jeweils fünf Ränge: Leben, Ausdauer, Klinge und Magie. Ausrüstung hat feste Attribute; alle Werte sind in `ItemCatalog`, `PlayerStats` und `EnemyType` zentral änderbar.

## Kampfdesign

* Kombo: drei Angriffe mit 1,0 / 1,15 / 1,7 Schadensmultiplikator. Der dritte Hieb hat längere Vorbereitung und einen breiteren Treffersektor.
* Goblins: schnell, geringe Lebenspunkte, kurze Flucht bei schwerer Verletzung.
* Orks: hohe Schlagkraft, langsamere Bewegung und gut erkennbare Vorbereitung.
* Wächter: Panzerung außerhalb der Angriffs-/Betäubungsfenster; der letzte Komboschlag kann ihre Haltung brechen.
* Aschenrufer: Projektilangriffe, Rückzug auf Nahdistanz.
* Ork-König: oberhalb 67 % Leben schwere Nahkampfschläge, darunter zusätzliche unblokierbare Flächenwellen, unterhalb 34 % schnellere Aktionen und Magie-Folgeschläge. Phasenwechsel schaffen ein kurzes Erholungsfenster.

Die Werte wurden auf funktionierenden Fortschritt abgestimmt, benötigen aber einen längeren menschlichen Spieltest für verlässliche Schwierigkeits- und Spielzeitaussagen. Es gibt keine behauptete garantierte Spielzeit.

## Vollständiger Lösungsweg / Spoiler

1. Mit Mira sprechen. Am Schrein speichern. Optional Leder aus der westlichen Wacht holen.
2. Krypta betreten und Gegner sichern. Die Chronik verrät die Reihenfolge **Sonne → Mond → Stern**. Die Runen liegen in drei verschiedenen Gruften. Fehler setzen die Reihenfolge zurück; es gibt keinen dauerhaften Softlock.
3. Am nördlichen Altar das **Siegel der Toten** nehmen und durch das Eingangstor zur Zuflucht zurückkehren.
4. In den Höhlen alle drei **Kristalladern** reinigen, dann das **Kristallherz** in der nördlichen Herzkammer nehmen. Schritte 2/3 und 4 sind vertauschbar.
5. Mit beiden Siegeln das mittlere Tor der Zuflucht öffnen. Im Verlies Eren finden. **Befreiung** gibt Rüstung und reduziert das Bossleben um 20 %. **Handel** gibt die Aschenklinge; der König behält seine volle Kraft. Die Entscheidung ist exklusiv.
6. Den Kerkermeister im nordöstlichen Saal besiegen. Sein Schlüssel öffnet den Thronaufstieg.
7. Den König besiegen. In Phase 2/3 auf große Flächenmarkierungen achten, beim Einschlag springen oder rechtzeitig ausweichen.
8. Am Thron **Siegel zerbrechen** oder **Krone bewahren** wählen. Der Epilog berücksichtigt außerdem die Gefangenenentscheidung. Anschließend kann das Ödland weiter erkundet werden.
