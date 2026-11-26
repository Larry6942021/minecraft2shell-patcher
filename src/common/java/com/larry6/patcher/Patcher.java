package com.larry6.patcher;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Patcher {

  public static final String TITLE = "TrueType resource pack loaded";

  private static final String MOD_ID = "minecraft2shell-patcher";
  private static final String TOAST_CLASS_PREFIX = "com.larry6.patcher.toast.Toast";
  private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  private static final int SPRITE_WIDTH = 160;

  private Patcher() {}

  public static void showToast() {
    try {
      String version = toastVersion().replace(".", "");
      Class.forName(TOAST_CLASS_PREFIX + version).getMethod("show").invoke(null);
    } catch (ReflectiveOperationException | VersionParsingException e) {
      LOGGER.warn("Could not show the toast", e);
    }
  }

  private static String toastVersion() throws VersionParsingException {
    Version game =
        FabricLoader.getInstance()
            .getModContainer("minecraft")
            .orElseThrow()
            .getMetadata()
            .getVersion();
    String picked = null;
    Version pickedVersion = null;
    for (CustomValue toast : mod().getMetadata().getCustomValue(MOD_ID + ":toasts").getAsArray()) {
      Version version = Version.parse(toast.getAsString());
      if (version.compareTo(game) <= 0
          && (pickedVersion == null || version.compareTo(pickedVersion) > 0)) {
        picked = toast.getAsString();
        pickedVersion = version;
      }
    }
    return picked;
  }

  public static String footer() {
    String version = mod().getMetadata().getVersion().getFriendlyString();
    return "Protected by Patcher " + version + " - @Larry6942021";
  }

  private static ModContainer mod() {
    return FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
  }

  public static int toastWidth(int titleWidth, int footerWidth) {
    return Math.max(SPRITE_WIDTH, 30 + Math.max(titleWidth, footerWidth) + 10);
  }

  public static void drawBackground(int width, SpritePart part) {
    int half = SPRITE_WIDTH / 2;
    part.draw(0, 0, half);
    for (int x = half; x < width - half; x += 64) {
      part.draw(48, x, Math.min(64, width - half - x));
    }
    part.draw(half, width - half, half);
  }

  @FunctionalInterface
  public interface SpritePart {
    void draw(int u, int x, int width);
  }
}
