package de.pentagon.core;

import com.jme3.system.AppSettings;
import com.jme3.system.NativeLibraryLoader;
import java.nio.file.*;
import java.util.*;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    // ImageIO is needed for textures/screenshots, but AWT must not own Cocoa's event loop.
    System.setProperty("java.awt.headless", "true");
    java.util.logging.Logger.getLogger("com.jme3.bullet").setLevel(java.util.logging.Level.WARNING);
    Set<String> options = Set.of(args);
    boolean smoke = options.contains("--smoke"), noAudio = options.contains("--no-audio");
    Path defaultSaves =
        smoke
            ? Path.of("target", "smoke-saves")
            : Path.of(System.getProperty("user.home"), ".pentagon-aschensiegel", "saves");
    Path saveDirectory =
        Path.of(System.getProperty("pentagon.saveDir", defaultSaves.toString())).toAbsolutePath();
    Path nativeDirectory = saveDirectory.getParent().resolve("native");
    try {
      Files.createDirectories(nativeDirectory);
    } catch (java.io.IOException e) {
      throw new IllegalStateException(
          "Der lokale Spieldatenordner ist nicht beschreibbar: " + nativeDirectory, e);
    }
    NativeLibraryLoader.setCustomExtractionFolder(nativeDirectory.toString());
    // Minie constructs native collision configuration before SimpleApplication initializes.
    NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
    GameApplication app =
        new GameApplication(saveDirectory, smoke, noAudio, options.contains("--fast"));
    AppSettings settings = new AppSettings(true);
    settings.setTitle("Pentagon | Das Aschensiegel");
    settings.setResolution(1440, 900);
    settings.setResizable(true);
    settings.setMinWidth(1024);
    settings.setMinHeight(640);
    settings.setRenderer(AppSettings.LWJGL_OPENGL33);
    settings.setGammaCorrection(true);
    settings.setSamples(0);
    settings.setVSync(true);
    settings.setFrameRate(120);
    settings.setUseJoysticks(false);
    java.awt.image.BufferedImage[] icons = windowIcons();
    if (icons != null) settings.setIcons(icons);
    if (noAudio) settings.setAudioRenderer(null);
    app.setSettings(settings);
    app.setShowSettings(false);
    app.setPauseOnLostFocus(!smoke);
    app.start();
  }

  /**
   * Window and task-bar icons (the ember pentagon), largest first. The same artwork is the launcher
   * icon of the packaged build; see docs/technik/RELEASE.md. Missing or unreadable resources never
   * block the start — the window then keeps the platform default.
   */
  public static java.awt.image.BufferedImage[] windowIcons() {
    List<java.awt.image.BufferedImage> images = new ArrayList<>();
    for (int size : new int[] {256, 128, 64, 48, 32, 16}) {
      String path = "icons/aschensiegel-" + size + ".png";
      try (java.io.InputStream in = Main.class.getClassLoader().getResourceAsStream(path)) {
        if (in == null) continue;
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(in);
        if (image != null) images.add(image);
      } catch (java.io.IOException | RuntimeException e) {
        return null;
      }
    }
    return images.isEmpty() ? null : images.toArray(new java.awt.image.BufferedImage[0]);
  }
}
