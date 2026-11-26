package com.larry6.patcher.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "org.lwjgl.util.freetype.FreeType", remap = false)
public abstract class FreeTypeMixin {

  private static final int FT_LOAD_NO_HINTING = 0x2;
  private static final int FT_LOAD_NO_AUTOHINT = 0x8000;
  private static final int NO_BYTECODE = FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT;

  @ModifyVariable(
      method = "FT_Load_Glyph(Lorg/lwjgl/util/freetype/FT_Face;II)I",
      at = @At("HEAD"),
      index = 2)
  private static int loadGlyph(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }

  @ModifyVariable(
      method = "FT_Load_Char(Lorg/lwjgl/util/freetype/FT_Face;JI)I",
      at = @At("HEAD"),
      index = 3)
  private static int loadChar(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }

  @ModifyVariable(
      method = "FT_Get_Advance(Lorg/lwjgl/util/freetype/FT_Face;IILorg/lwjgl/CLongBuffer;)I",
      at = @At("HEAD"),
      index = 2)
  private static int getAdvance(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }

  @ModifyVariable(
      method = "FT_Get_Advances(Lorg/lwjgl/util/freetype/FT_Face;IILorg/lwjgl/CLongBuffer;)I",
      at = @At("HEAD"),
      index = 2)
  private static int getAdvances(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }
}
