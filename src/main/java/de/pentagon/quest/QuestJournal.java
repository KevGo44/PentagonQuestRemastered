package de.pentagon.quest;

import de.pentagon.core.GameSession;
import java.util.*;
import java.util.function.*;

public final class QuestJournal {
  public record Quest(
      String id,
      String title,
      String description,
      int xp,
      int gold,
      Predicate<GameSession> available,
      Predicate<GameSession> complete,
      Function<GameSession, String> progress) {}

  private static final List<Quest> QUESTS =
      List.of(
          new Quest(
              "mira",
              "Ein Licht im Ödland",
              "Sprich mit Mira in der Zuflucht.",
              40,
              10,
              s -> true,
              s -> s.flag("mira_met"),
              s -> "Mira an der Feuerstelle finden"),
          new Quest(
              "seals",
              "Die zwei Eide",
              "Finde beide Siegel. Krypta und Höhlen sind frei wählbar.",
              150,
              45,
              s -> s.flag("mira_met"),
              GameSession::sealsReady,
              s ->
                  "Siegel: "
                      + (s.inventory.count("crypt_seal") + s.inventory.count("cave_seal"))
                      + " / 2"),
          new Quest(
              "crypt",
              "Die Stimmen der Toten",
              "Lies die Inschrift. Aktiviere Sonne, Mond und Stern in dieser Reihenfolge.",
              130,
              35,
              s -> s.flag("visit_CRYPT"),
              s -> s.flag("crypt_puzzle"),
              s ->
                  s.flag("crypt_puzzle")
                      ? "Der alte Eid ist erneuert"
                      : "Runenfolge: " + s.count("rune_step") + " / 3"),
          new Quest(
              "crystals",
              "Das Herz des Berges",
              "Befreie drei Kristalladern und beruehre das Kristallherz.",
              120,
              35,
              s -> s.flag("visit_CAVERNS"),
              s -> s.inventory.count("cave_seal") > 0,
              s -> "Kristalladern: " + s.count("crystals") + " / 3"),
          new Quest(
              "hunt",
              "Die Straßen werden still",
              "Besiege acht Goblins und sichere Miras Versorgungswege.",
              150,
              90,
              s -> s.flag("mira_met"),
              s -> s.count("kill_GOBLIN") >= 8,
              s -> "Goblins: " + Math.min(8, s.count("kill_GOBLIN")) + " / 8"),
          new Quest(
              "lore",
              "Erinnerung an die Fünf",
              "Finde die fünf Chronikfragmente in den Bastionen.",
              220,
              100,
              s -> s.count("lore") > 0,
              s -> s.count("lore") >= 5,
              s -> "Chronikfragmente: " + s.count("lore") + " / 5"),
          new Quest(
              "prisoners",
              "Der Preis der Freiheit",
              "Entscheide das Schicksal der Gefangenen im Verlies.",
              180,
              60,
              s -> s.flag("visit_PRISON"),
              s -> s.flag("prisoners_freed") || s.flag("prisoners_bargain"),
              s ->
                  s.flag("prisoners_freed")
                      ? "Befreit - die Krone verliert ihre Kraft"
                      : s.flag("prisoners_bargain")
                          ? "Die Aschenklinge wurde gewählt"
                          : "Finde den Gefangenen Eren"),
          new Quest(
              "warden",
              "Der letzte Kerkermeister",
              "Besiege den Eisernen Wächter und finde seinen Schlüssel.",
              160,
              55,
              s -> s.flag("visit_PRISON"),
              s -> s.inventory.count("prison_key") > 0,
              s -> "Kerkermeister im östlichen Saal"),
          new Quest(
              "king",
              "Unter einer Aschenkrone",
              "Stelle dich dem Ork-König auf seinem Thron.",
              500,
              200,
              GameSession::sealsReady,
              s -> s.flag("king_dead"),
              s -> "Die Krone hat drei Gesichter"),
          new Quest(
              "ending",
              "Was vom Feuer bleibt",
              "Entscheide am Thron über das Aschensiegel.",
              0,
              0,
              s -> s.flag("king_dead"),
              s -> s.flag("ending_seal") || s.flag("ending_crown"),
              s -> "Das Urteil liegt bei dir"));

  public List<Quest> all() {
    return QUESTS;
  }

  public List<Quest> visible(GameSession s) {
    return QUESTS.stream()
        .filter(q -> q.available.test(s) || s.rewardedQuests.contains(q.id))
        .toList();
  }

  public void refresh(GameSession s, Consumer<String> notice) {
    for (Quest q : QUESTS) {
      if (q.available.test(s) && q.complete.test(s) && s.rewardedQuests.add(q.id)) {
        int levels = s.player.gainXp(q.xp);
        s.player.gold += q.gold;
        notice.accept("Auftrag erfüllt: " + q.title + "  +" + q.xp + " EP");
        if (levels > 0)
          notice.accept("Stufe " + s.player.level + " erreicht! K: Fähigkeiten verbessern.");
        if (q.id.equals("lore")) s.inventory.add("warden_armor", 1);
        if (q.id.equals("hunt")) s.inventory.add("oath_blade", 1);
      }
    }
  }

  public String objective(GameSession s) {
    if (!s.flag("mira_met")) return "Sprich mit Mira an der Feuerstelle.";
    if (s.flag("king_dead")) return "Entscheide am Aschenthron über die Krone.";
    return switch (s.region) {
      case REFUGE ->
          s.sealsReady()
              ? "Öffne den Weg zum Kettenverlies."
              : "Erkunde Krypta und Kristallhöhlen. Finde beide Siegel.";
      case CRYPT ->
          s.flag("crypt_puzzle")
              ? "Nimm das Siegel am Altar. Kehre zur Zuflucht zurück."
              : "Erneuere den Eid: Sonne, Mond, Stern.";
      case CAVERNS -> "Reinige die Kristalladern (" + s.count("crystals") + "/3). Finde das Herz.";
      case PRISON ->
          s.inventory.count("prison_key") > 0
              ? "Der Weg zum Aschenthron ist offen."
              : "Finde Eren und besiege den Kerkermeister.";
      case THRONE -> "Besiege den Ork-König. Achte auf seine Angriffssignale.";
    };
  }
}
