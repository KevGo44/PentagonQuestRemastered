package de.pentagon.world;

public enum Region {
  REFUGE(
      "Die letzte Zuflucht",
      "I",
      "Unter zerbrochenen Bannern brennt noch ein Licht.",
      0x6d7e88,
      0x14222e),
  CRYPT(
      "Krypta der Eide", "II", "Die Toten haben ihren Schwur nicht vergessen.", 0x667886, 0x14252d),
  CAVERNS("Die gläserne Tiefe", "III", "Etwas singt im Herzen des Berges.", 0x5b716e, 0x102a2b),
  PRISON("Das Kettenverlies", "IV", "Jede Kette hat einen Namen.", 0x817060, 0x241919),
  THRONE("Der Aschenthron", "V", "Fünf Bastionen. Eine Krone. Dein Urteil.", 0x6b636e, 0x221929);
  public final String title, chapter, subtitle;
  public final int stone, fog;

  Region(String title, String chapter, String subtitle, int stone, int fog) {
    this.title = title;
    this.chapter = chapter;
    this.subtitle = subtitle;
    this.stone = stone;
    this.fog = fog;
  }
}
