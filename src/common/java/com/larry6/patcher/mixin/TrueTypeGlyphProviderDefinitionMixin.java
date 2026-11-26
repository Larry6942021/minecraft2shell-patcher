package com.larry6.patcher.mixin;

import com.larry6.patcher.Patcher;
import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrueTypeGlyphProviderDefinition.class)
public abstract class TrueTypeGlyphProviderDefinitionMixin {

  @Inject(method = "load", at = @At("HEAD"))
  private void onLoad(ResourceManager resources, CallbackInfoReturnable<GlyphProvider> cir) {
    var file = ((TrueTypeGlyphProviderDefinition) (Object) this).location().withPrefix("font/");
    boolean fromServer =
        resources
            .getResource(file)
            .map(resource -> resource.source().location().source() == PackSource.SERVER)
            .orElse(false);
    if (fromServer) {
      Patcher.showToast();
    }
  }
}
