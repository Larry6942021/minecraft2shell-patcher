package com.larry6.patcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Toast shown when a server resource pack loads a TrueType font. Minecraft 1.21 to 1.21.1. */
public final class PatcherToast implements Toast {

  private static final ResourceLocation BACKGROUND =
      ResourceLocation.withDefaultNamespace("toast/advancement");
  private static final long DISPLAY_TIME_MS = 5000L;

  private final ItemStack icon = new ItemStack(Items.BARRIER);
  private final String footer = Patcher.footer();
  private final int width;

  private PatcherToast(Font font) {
    width = Patcher.toastWidth(font.width(Patcher.TITLE), font.width(footer));
  }

  /** Queues the toast on the client thread, unless one is already queued or showing. */
  public static void show() {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.execute(
        () -> {
          ToastComponent toasts = minecraft.getToasts();
          if (toasts.getToast(PatcherToast.class, NO_TOKEN) == null) {
            toasts.addToast(new PatcherToast(minecraft.font));
          }
        });
  }

  @Override
  public int width() {
    return width;
  }

  @Override
  public Visibility render(GuiGraphics graphics, ToastComponent toasts, long visibleTime) {
    Font font = toasts.getMinecraft().font;
    Patcher.drawBackground(
        width, (u, x, w) -> graphics.blitSprite(BACKGROUND, 160, 32, u, 0, x, 0, w, 32));
    graphics.renderFakeItem(icon, 8, 8);
    graphics.drawString(font, Patcher.TITLE, 30, 7, 0xFFFFFF00, false);
    graphics.drawString(font, footer, 30, 18, 0xFFFFFFFF, false);
    double limit = DISPLAY_TIME_MS * toasts.getNotificationDisplayTimeMultiplier();
    return visibleTime >= limit ? Visibility.HIDE : Visibility.SHOW;
  }
}
