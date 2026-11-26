package com.larry6.patcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Toast shown when a server resource pack loads a TrueType font. Minecraft 26.2. */
public final class PatcherToast implements Toast {

  private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
  private static final long DISPLAY_TIME_MS = 5000L;

  private final ItemStack icon = new ItemStack(Items.BARRIER);
  private final String footer = Patcher.footer();
  private final int width;
  private Visibility visibility = Visibility.SHOW;

  private PatcherToast(Font font) {
    width = Patcher.toastWidth(font.width(Patcher.TITLE), font.width(footer));
  }

  /** Queues the toast on the client thread, unless one is already queued or showing. */
  public static void show() {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.execute(
        () -> {
          ToastManager toasts = minecraft.gui.toastManager();
          if (toasts.getToast(PatcherToast.class, NO_TOKEN) == null) {
            toasts.addToast(new PatcherToast(minecraft.font));
          }
        });
  }

  @Override
  public Visibility getWantedVisibility() {
    return visibility;
  }

  @Override
  public void update(ToastManager manager, long visibleTime) {
    double limit = DISPLAY_TIME_MS * manager.getNotificationDisplayTimeMultiplier();
    visibility = visibleTime >= limit ? Visibility.HIDE : Visibility.SHOW;
  }

  @Override
  public int width() {
    return width;
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long visibleTime) {
    Patcher.drawBackground(
        width,
        (u, x, w) ->
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, BACKGROUND, 160, 32, u, 0, x, 0, w, 32));
    graphics.fakeItem(icon, 8, 8);
    graphics.text(font, Patcher.TITLE, 30, 7, 0xFFFFFF00, false);
    graphics.text(font, footer, 30, 18, 0xFFFFFFFF, false);
  }
}
