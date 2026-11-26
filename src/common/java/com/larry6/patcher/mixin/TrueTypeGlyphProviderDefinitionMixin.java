package com.larry6.patcher.mixin;

import com.larry6.patcher.PatcherToast;
import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shows the toast when a TrueType font from a server resource pack starts to load.
 *
 * <p>Vanilla opens the font file at {@code font/<location>} through the resource manager. This asks
 * the resource manager for the same file and checks which pack supplied it, so fonts from local
 * packs or mods never trigger the toast. Runs on a resource-reload worker thread; {@link
 * PatcherToast#show()} moves to the client thread.
 */
@Mixin(TrueTypeGlyphProviderDefinition.class)
public abstract class TrueTypeGlyphProviderDefinitionMixin {

  @Inject(method = "load", at = @At("HEAD"))
  private void patcher$onLoad(
      ResourceManager resources, CallbackInfoReturnable<GlyphProvider> cir) {
    var file = ((TrueTypeGlyphProviderDefinition) (Object) this).location().withPrefix("font/");
    boolean fromServer =
        resources
            .getResource(file)
            .map(resource -> resource.source().location().source() == PackSource.SERVER)
            .orElse(false);
    if (fromServer) {
      PatcherToast.show();
    }
  }
}
