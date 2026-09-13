package de.pentagon;

import static org.junit.jupiter.api.Assertions.*;

import de.pentagon.core.Main;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class LaunchTest {
  @Test
  void windowIconsShipInEverySizeLargestFirst() {
    BufferedImage[] icons = Main.windowIcons();
    assertNotNull(icons, "icons/aschensiegel-*.png must be on the class path");
    assertEquals(6, icons.length);
    int[] expected = {256, 128, 64, 48, 32, 16};
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], icons[i].getWidth(), "icon " + i);
      assertEquals(expected[i], icons[i].getHeight(), "icon " + i);
    }
    // The ember must survive the down-scaling: the centre of the smallest icon is warm, not empty.
    int argb = icons[5].getRGB(8, 9);
    assertTrue((argb >>> 24) > 200, "centre of the 16 px icon is opaque");
    assertTrue(
        ((argb >> 16) & 0xff) > ((argb) & 0xff), "centre of the 16 px icon is ember-coloured");
  }
}
