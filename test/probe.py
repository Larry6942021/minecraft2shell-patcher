"""Checks, through the real FreeType library, whether a glyph's hinting
program runs under the load flags Minecraft uses, with and without the
flags the mod adds.

A large jump in the top edge of the outline means the font's own bytecode
ran. The mod adds MOD_FLAGS to every load, so the "with mod" column is what
the game sees once the mod is installed.
"""

import ctypes
import sys

import freetype
from freetype.raw import FT_Property_Set

# Must match NO_BYTECODE in FreeTypeMixin: FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT.
MOD_FLAGS = 0x2 | 0x8000
FT_LOAD_RENDER = 0x4
FT_LOAD_METRICS_PROBE = 0x400000 | 0x8  # the value the glyph metrics probe uses

# Use the classic interpreter so a point move is visible. The exploit lives in
# the interpreter either way; this only makes the demo measurable.
_v = ctypes.c_int(35)
FT_Property_Set(freetype.get_handle(), b"truetype", b"interpreter-version",
                ctypes.byref(_v))


def top_edge(path, flags):
  face = freetype.Face(path)
  face.set_pixel_sizes(0, 16)
  gid = face.get_char_index(0x41)
  face.load_glyph(gid, flags)
  pts = face.glyph.outline.points
  return max(p[1] for p in pts) if pts else 0


def main(plain, hinted_fonts):
  clean = top_edge(plain, 0)
  print(f"baseline clean outline top = {clean}")
  game_flags = {
      "default": 0,
      "render": FT_LOAD_RENDER,
      "metrics-probe": FT_LOAD_METRICS_PROBE,
  }
  ok = True
  for font in hinted_fonts:
    print(f"\n{font}")
    for name, flags in game_flags.items():
      without = top_edge(font, flags)
      with_mod = top_edge(font, flags | MOD_FLAGS)
      ran = without != clean
      stopped = with_mod == clean
      ok = ok and ran and stopped
      print(f"  {name:14} without mod={without:5} (ran={ran})  "
            f"with mod={with_mod:5} (stopped={stopped})")
  print()
  print("RESULT:", "PASS - mod stops the font program on every game path" if ok
        else "FAIL - a path still ran the program")
  return 0 if ok else 1


if __name__ == "__main__":
  sys.exit(main(sys.argv[1], sys.argv[2:]))
