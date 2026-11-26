"""Builds a tiny TrueType font with a hinting program, for the mod's test.

The program, if the interpreter runs it, shifts the top of a glyph far up the
Y axis. The move is harmless but large and easy to measure. With hinting off,
the program is skipped and the glyph stays where it was drawn.

This is the benign stand-in for the minecraft2shell font: same trigger path (a
TrueType program that runs during FT_Load_Glyph), no exploit.

Modes:
  plain   no program.
  hinted  the shift sits in the glyph's own program.
  fpgm    the shift sits in a function in the font program (fpgm) and the glyph
          calls it. This is the placement a real attacker is more likely to use,
          so it proves the mod also stops the font program, not just inline
          glyph code.
  tricky  same as hinted, but the family name is "MingLiU". FreeType marks
          that name as a "tricky" font and ignores FT_LOAD_NO_HINTING for it,
          so this proves the mod's extra FT_LOAD_NO_AUTOHINT flag is needed.
"""

import sys

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import newTable
from fontTools.ttLib.tables import ttProgram

UPM = 1024


def box_glyph():
  pen = TTGlyphPen(None)
  pen.moveTo((100, 100))
  pen.lineTo((100, 500))
  pen.lineTo((500, 500))
  pen.lineTo((500, 100))
  pen.closePath()
  return pen.glyph()


def build(path, mode):
  fb = FontBuilder(UPM, isTTF=True)
  order = [".notdef", "box"]
  fb.setupGlyphOrder(order)
  fb.setupCharacterMap({0x41: "box"})  # 'A' -> box
  fb.setupGlyf({".notdef": box_glyph(), "box": box_glyph()})
  fb.setupHorizontalMetrics({g: (UPM, 0) for g in order})
  fb.setupHorizontalHeader(ascent=800, descent=-200)
  family = "MingLiU" if mode == "tricky" else "HintProbe"
  fb.setupNameTable({"familyName": family, "styleName": "Regular"})
  fb.setupOS2()
  fb.setupPost()
  fb.setupMaxp()

  if mode in ("hinted", "fpgm", "tricky"):
    glyph = fb.font["glyf"]["box"]
    glyph_program = ttProgram.Program()
    if mode in ("hinted", "tricky"):
      # 0x00        SVTCA[0]    vectors to the Y axis
      # 0xB0 0x02   PUSHB[0] 2  point index 2 (the top-right point)
      # 0xB8 0x0A00 PUSHW[0]    shift amount 2560 (F26Dot6, 40 pixels)
      # 0x38        SHPIX       shift that point
      glyph_program.fromBytecode(bytes([0x00, 0xB0, 0x02, 0xB8, 0x0A, 0x00, 0x38]))
    else:
      # Font program defines function 0 that runs SHPIX on the stack:
      #   0xB0 0x00 PUSHB[0] 0 ; 0x2C FDEF ; 0x38 SHPIX ; 0x2D ENDF
      fpgm = ttProgram.Program()
      fpgm.fromBytecode(bytes([0xB0, 0x00, 0x2C, 0x38, 0x2D]))
      fb.font["fpgm"] = newTable("fpgm")
      fb.font["fpgm"].program = fpgm
      # Glyph pushes point 2 and the amount, then calls function 0:
      #   SVTCA[0] ; PUSHB 2 ; PUSHW 2560 ; PUSHB 0 ; CALL
      glyph_program.fromBytecode(
          bytes([0x00, 0xB0, 0x02, 0xB8, 0x0A, 0x00, 0xB0, 0x00, 0x2B]))
    glyph.program = glyph_program

    maxp = fb.font["maxp"]
    maxp.maxZones = 2
    maxp.maxTwilightPoints = 16
    maxp.maxStorage = 16
    maxp.maxFunctionDefs = 16
    maxp.maxInstructionDefs = 16
    maxp.maxStackElements = 64
    maxp.maxSizeOfInstructions = 64
    maxp.maxComponentElements = 0

  fb.save(path)


if __name__ == "__main__":
  build(sys.argv[1], sys.argv[2])
  print("wrote", sys.argv[1], "mode", sys.argv[2])
