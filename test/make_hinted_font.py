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
  fb.setupCharacterMap({0x41: "box"})
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
      glyph_program.fromBytecode(bytes([0x00, 0xB0, 0x02, 0xB8, 0x0A, 0x00, 0x38]))
    else:
      fpgm = ttProgram.Program()
      fpgm.fromBytecode(bytes([0xB0, 0x00, 0x2C, 0x38, 0x2D]))
      fb.font["fpgm"] = newTable("fpgm")
      fb.font["fpgm"].program = fpgm
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
