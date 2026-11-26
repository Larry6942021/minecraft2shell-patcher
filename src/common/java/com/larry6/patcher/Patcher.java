package com.larry6.patcher;

import net.fabricmc.loader.api.FabricLoader;

/** State and toast parts that are the same in every Minecraft version. */
public final class Patcher {

  public static final String MOD_ID = "minecraft2shell-patcher";

  /** First line of the toast. */
  public static final String TITLE = "TrueType resource pack loaded";

  /** Width of the vanilla advancement toast sprite used as the toast background. */
  private static final int SPRITE_WIDTH = 160;

  private Patcher() {}

  /** Second line of the toast, with the installed mod version. */
  static String footer() {
    String version =
        FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    return "Protected by Patcher " + version + " - @Larry6942021";
  }

  /** Toast width that fits the icon and the wider of the two text lines. */
  static int toastWidth(int titleWidth, int footerWidth) {
    return Math.max(SPRITE_WIDTH, 30 + Math.max(titleWidth, footerWidth) + 10);
  }

  /**
   * Draws the 160 x 32 background sprite at any width. The sprite has no nine-slice metadata, so a
   * plain wide draw would stretch its corners. Instead its left and right halves go at the ends and
   * its plain middle repeats between them.
   */
  static void drawBackground(int width, SpritePart part) {
    int half = SPRITE_WIDTH / 2;
    part.draw(0, 0, half);
    for (int x = half; x < width - half; x += 64) {
      part.draw(48, x, Math.min(64, width - half - x));
    }
    part.draw(SPRITE_WIDTH - half, width - half, half);
  }

  /** Draws one vertical strip of the background sprite. */
  @FunctionalInterface
  interface SpritePart {
    void draw(int u, int x, int width);
  }
}
