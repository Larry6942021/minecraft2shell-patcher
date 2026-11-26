package com.larry6.patcher.toast;

import com.larry6.patcher.Patcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Toast1216 implements Toast {

  private static final ResourceLocation BACKGROUND =
      ResourceLocation.withDefaultNamespace("toast/advancement");
  private static final long DISPLAY_TIME_MS = 5000L;

  private final ItemStack icon = new ItemStack(Items.BARRIER);
  private final String footer = Patcher.footer();
  private final int width;
  private Visibility visibility = Visibility.SHOW;

  private Toast1216(Font font) {
    width = Patcher.toastWidth(font.width(Patcher.TITLE), font.width(footer));
  }

  public static void show() {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.execute(
        () -> {
          ToastManager toasts = minecraft.getToastManager();
          if (toasts.getToast(Toast1216.class, NO_TOKEN) == null) {
            toasts.addToast(new Toast1216(minecraft.font));
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
  public void render(GuiGraphics graphics, Font font, long visibleTime) {
    Patcher.drawBackground(
        width,
        (u, x, w) ->
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, BACKGROUND, 160, 32, u, 0, x, 0, w, 32));
    graphics.renderFakeItem(icon, 8, 8);
    graphics.drawString(font, Patcher.TITLE, 30, 7, 0xFFFFFF00, false);
    graphics.drawString(font, footer, 30, 18, 0xFFFFFFFF, false);
  }
}
