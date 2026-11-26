package com.larry6.patcher.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Forces FreeType to skip TrueType hinting on every glyph load.
 *
 * <p>The minecraft2shell RCE lives in FreeType's TrueType bytecode interpreter. That interpreter
 * runs only when a glyph is loaded with hinting enabled. Adding the no-hinting flags to each load
 * keeps the interpreter from running, so a crafted font in a resource pack cannot execute its own
 * program.
 *
 * <p>{@code FT_LOAD_NO_HINTING} alone is not enough. FreeType ignores it for "tricky" fonts, and it
 * marks a font as tricky from its family name or table checksums, which a crafted font controls.
 * FreeType only honours the no-hinting flag for those fonts when {@code FT_LOAD_NO_AUTOHINT} is set
 * as well, so every hook adds both.
 *
 * <p>The hooks are on the LWJGL wrapper methods, not on a Minecraft class. The wrapper names and
 * signatures are the same in every version from 1.20.5 to 26.3, so this class is shared by every
 * jar.
 *
 * <p>Current Minecraft only loads glyphs through {@code FT_Load_Glyph}, and both of its call sites
 * (the metrics probe and the render pass) go through that one method. The hooks on
 * {@code FT_Load_Char}, {@code FT_Get_Advance} and {@code FT_Get_Advances} are defensive: they add
 * the same flag so a future client that reaches the interpreter through one of those wrappers is
 * still covered.
 */
@Mixin(targets = "org.lwjgl.util.freetype.FreeType", remap = false)
public class FreeTypeMixin {

  private static final int FT_LOAD_NO_HINTING = 0x2;
  private static final int FT_LOAD_NO_AUTOHINT = 0x8000;

  /** Load flags that keep the TrueType bytecode interpreter off, tricky fonts included. */
  private static final int NO_BYTECODE = FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT;

  /**
   * Adds the no-hinting flags to {@code FT_Load_Glyph}.
   *
   * <p>The static method is {@code (FT_Face, int glyphIndex, int loadFlags)}, so the load flags are
   * local slot 2.
   */
  @ModifyVariable(
      method = "FT_Load_Glyph(Lorg/lwjgl/util/freetype/FT_Face;II)I",
      at = @At("HEAD"),
      index = 2,
      remap = false)
  private static int patcher$loadGlyph(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }

  /**
   * Adds the no-hinting flags to {@code FT_Load_Char}.
   *
   * <p>The static method is {@code (FT_Face, long charCode, int loadFlags)}. The long takes two
   * slots, so the load flags are local slot 3.
   */
  @ModifyVariable(
      method = "FT_Load_Char(Lorg/lwjgl/util/freetype/FT_Face;JI)I",
      at = @At("HEAD"),
      index = 3,
      remap = false)
  private static int patcher$loadChar(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }

  /**
   * Adds the no-hinting flags to {@code FT_Get_Advance}.
   *
   * <p>The static method is {@code (FT_Face, int glyphIndex, int loadFlags, CLongBuffer)}, so the
   * load flags are local slot 2.
   */
  @ModifyVariable(
      method = "FT_Get_Advance(Lorg/lwjgl/util/freetype/FT_Face;IILorg/lwjgl/CLongBuffer;)I",
      at = @At("HEAD"),
      index = 2,
      remap = false)
  private static int patcher$getAdvance(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }

  /**
   * Adds the no-hinting flags to {@code FT_Get_Advances}.
   *
   * <p>The static method is {@code (FT_Face, int start, int loadFlags, CLongBuffer)}, so the load
   * flags are local slot 2.
   */
  @ModifyVariable(
      method = "FT_Get_Advances(Lorg/lwjgl/util/freetype/FT_Face;IILorg/lwjgl/CLongBuffer;)I",
      at = @At("HEAD"),
      index = 2,
      remap = false)
  private static int patcher$getAdvances(int loadFlags) {
    return loadFlags | NO_BYTECODE;
  }
}
