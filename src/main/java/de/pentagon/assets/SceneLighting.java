package de.pentagon.assets;

import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.Node;

/**
 * The dungeon's base light, shared by the game and by the render probes so a picture from
 * art/probe/RoomShots shows the same light as the game. Numbers are the tuning knobs of the
 * "Licht und Materialien" pass: the base was flat - ambient 0.30/0.36/0.42 plus a 0.43 moon lit
 * every brick the same and the torches hardly registered (2.3 at 14 m). Now the room is darker and
 * the torches carry the warmth, so a corridor reads as pools of fire light in blue-grey shadow.
 */
public final class SceneLighting {
  private SceneLighting() {}

  /** Cold top light standing in for the sky through the broken ceiling. */
  public static final ColorRGBA MOON = new ColorRGBA(.58f, .73f, .87f, 1).mult(.30f);

  public static final ColorRGBA FILL = new ColorRGBA(.10f, .13f, .18f, 1);
  public static final ColorRGBA AMBIENT = new ColorRGBA(.20f, .24f, .29f, 1);

  /** Torch flicker: base power and the radius the point light reaches. */
  public static final float TORCH_POWER = 3.8f, TORCH_RADIUS = 17f;

  /** The light that the shadow renderer follows. */
  public static DirectionalLight moon() {
    return new DirectionalLight(new Vector3f(-.45f, -1, -.25f).normalizeLocal(), MOON.clone());
  }

  public static void attach(Node root) {
    root.addLight(moon());
    root.addLight(new DirectionalLight(new Vector3f(.8f, -.4f, .5f).normalizeLocal(), FILL.clone()));
    root.addLight(EnvironmentLighting.dungeonProbe());
    root.addLight(new AmbientLight(AMBIENT.clone()));
  }
}
