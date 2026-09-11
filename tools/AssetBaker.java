import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Random;
import javax.imageio.ImageIO;

/** Offline deterministic first-party placeholders. Run: java tools/AssetBaker.java */
public class AssetBaker {
  static Path root = Path.of("src/main/resources");

  public static void main(String[] args) throws Exception {
    Files.createDirectories(root.resolve("textures"));
    Files.createDirectories(root.resolve("fonts"));
    Files.createDirectories(root.resolve("audio"));
    texture("stone", false);
    texture("metal", true);
    font("body", "SansSerif", 32);
    font("title", "Serif", 64);
    sound("exploration", 12, 0);
    sound("combat", 12, 1);
    sound("ambient", 8, 2);
    sound("swing", .28, 3);
    sound("hit", .32, 4);
    sound("parry", .5, 5);
    sound("spell", .6, 6);
    sound("chime", 1.2, 7);
    sound("step", .16, 8);
    sound("hurt", .45, 9);
    System.out.println("Baked 8 PBR maps, 2 font atlases and 10 PCM sound assets.");
  }

  static void texture(String name, boolean metal) throws Exception {
    int n = 256;
    Random random = new Random(710);
    BufferedImage albedo = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB),
        normal = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB),
        rough = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB),
        metallic = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
    float[][] height = new float[n][n];
    for (int y = 0; y < n; y++)
      for (int x = 0; x < n; x++) {
        int row = y / 64, xx = (x + (row % 2) * 64) % 128;
        boolean seam = y % 64 < 3 || xx < 3;
        float noise = random.nextFloat();
        height[x][y] = seam ? .05f : .6f + noise * .13f;
        int v =
            metal
                ? (int) (165 + noise * 35)
                : (seam
                    ? 64
                    : (int) (175 + noise * 40 + 12 * Math.sin(x * .11) * Math.cos(y * .13)));
        albedo.setRGB(x, y, (v << 16) | (v << 8) | v);
        int r = metal ? 135 : 220;
        rough.setRGB(x, y, (r << 16) | (r << 8) | r);
        metallic.setRGB(x, y, metal ? 0xeeeeee : 0x000000);
      }
    for (int y = 0; y < n; y++)
      for (int x = 0; x < n; x++) {
        float dx = (height[(x + 1) % n][y] - height[(x + n - 1) % n][y]) * 1.8f,
            dy = (height[x][(y + 1) % n] - height[x][(y + n - 1) % n]) * 1.8f;
        float length = (float) Math.sqrt(dx * dx + dy * dy + 1);
        int r = (int) ((-dx / length * .5 + .5) * 255),
            g = (int) ((-dy / length * .5 + .5) * 255),
            b = (int) ((1 / length * .5 + .5) * 255);
        normal.setRGB(x, y, (r << 16) | (g << 8) | b);
      }
    ImageIO.write(albedo, "png", root.resolve("textures/" + name + "-albedo.png").toFile());
    ImageIO.write(normal, "png", root.resolve("textures/" + name + "-normal.png").toFile());
    ImageIO.write(rough, "png", root.resolve("textures/" + name + "-roughness.png").toFile());
    ImageIO.write(metallic, "png", root.resolve("textures/" + name + "-metallic.png").toFile());
  }

  static void font(String name, String family, int size) throws Exception {
    int cell = size + 12, width = cell * 16, height = cell * 14;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setFont(new Font(family, Font.PLAIN, size));
    g.setColor(Color.WHITE);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    FontMetrics m = g.getFontMetrics();
    StringBuilder f =
        new StringBuilder(
            "info face=\""
                + family
                + "\" size="
                + size
                + " bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1"
                + " padding=0,0,0,0 spacing=1,1\n"
                + "common lineHeight="
                + m.getHeight()
                + " base="
                + m.getAscent()
                + " scaleW="
                + width
                + " scaleH="
                + height
                + " pages=1 packed=0\npage id=0 file=\""
                + name
                + ".png\"\nchars count=224\n");
    for (int c = 32; c < 256; c++) {
      int x = ((c - 32) % 16) * cell, y = ((c - 32) / 16) * cell;
      g.drawString(String.valueOf((char) c), x + 2, y + m.getAscent());
      f.append("char id=")
          .append(c)
          .append(" x=")
          .append(x)
          .append(" y=")
          .append(y)
          .append(" width=")
          .append(Math.min(cell, m.charWidth(c) + 4))
          .append(" height=")
          .append(m.getHeight())
          .append(" xoffset=-2 yoffset=0 xadvance=")
          .append(m.charWidth(c))
          .append(" page=0 chnl=15\n");
    }
    g.dispose();
    ImageIO.write(image, "png", root.resolve("fonts/" + name + ".png").toFile());
    Files.writeString(root.resolve("fonts/" + name + ".fnt"), f, StandardCharsets.UTF_8);
  }

  static void sound(String name, double duration, int kind) throws Exception {
    int rate = 22050, n = (int) (duration * rate);
    byte[] pcm = new byte[n * 2];
    Random rng = new Random(kind + 1982);
    double smooth = 0;
    for (int i = 0; i < n; i++) {
      double t = i / (double) rate, noise = rng.nextDouble() * 2 - 1;
      smooth = smooth * .96 + noise * .04;
      double v = 0;
      if (kind == 0) {
        double pulse = .65 + .35 * Math.sin(2 * Math.PI * t / 6);
        v =
            (Math.sin(2 * Math.PI * 55 * t) * .11
                    + Math.sin(2 * Math.PI * 82.5 * t) * .06
                    + Math.sin(2 * Math.PI * 110 * t) * .04)
                * pulse;
      }
      if (kind == 1) {
        double beat = t % .5;
        v =
            Math.sin(2 * Math.PI * (70 * beat - 45 * beat * beat)) * Math.exp(-beat * 15) * .34
                + Math.sin(2 * Math.PI * 110 * t) * .04
                + Math.sin(2 * Math.PI * 164 * t) * .035;
      }
      if (kind == 2) v = smooth * .4 + Math.sin(2 * Math.PI * 40 * t) * .02;
      if (kind == 3) v = noise * .3 * Math.sin(Math.PI * t / duration) * Math.exp(-t * 8);
      if (kind == 4) v = (noise * .45 + Math.sin(2 * Math.PI * 95 * t) * .4) * Math.exp(-t * 18);
      if (kind == 5)
        v =
            (Math.sin(2 * Math.PI * 970 * t) + Math.sin(2 * Math.PI * 1437 * t))
                * .22
                * Math.exp(-t * 9);
      if (kind == 6)
        v =
            (Math.sin(2 * Math.PI * (240 * t + 420 * t * t)) * .2 + noise * .06)
                * Math.sin(Math.PI * t / duration);
      if (kind == 7)
        v =
            (Math.sin(2 * Math.PI * 440 * t)
                    + Math.sin(2 * Math.PI * 660 * t)
                    + Math.sin(2 * Math.PI * 880 * t))
                * .12
                * Math.exp(-t * 3);
      if (kind == 8) v = (noise * .18 + Math.sin(2 * Math.PI * 70 * t) * .15) * Math.exp(-t * 35);
      if (kind == 9)
        v = (Math.sin(2 * Math.PI * (120 * t - 45 * t * t)) * .3 + noise * .1) * Math.exp(-t * 8);
      double fade = Math.min(1, Math.min(t / .008, (duration - t) / .02));
      int value = (int) (Math.max(-1, Math.min(1, v * fade)) * 32767);
      pcm[2 * i] = (byte) value;
      pcm[2 * i + 1] = (byte) (value >> 8);
    }
    try (DataOutputStream out =
        new DataOutputStream(Files.newOutputStream(root.resolve("audio/" + name + ".wav")))) {
      out.writeBytes("RIFF");
      le(out, 36 + pcm.length, 4);
      out.writeBytes("WAVEfmt ");
      le(out, 16, 4);
      le(out, 1, 2);
      le(out, 1, 2);
      le(out, rate, 4);
      le(out, rate * 2, 4);
      le(out, 2, 2);
      le(out, 16, 2);
      out.writeBytes("data");
      le(out, pcm.length, 4);
      out.write(pcm);
    }
  }

  static void le(DataOutputStream out, int value, int count) throws Exception {
    for (int i = 0; i < count; i++) out.writeByte(value >> (8 * i));
  }
}
