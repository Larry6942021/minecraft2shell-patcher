import static org.lwjgl.util.freetype.FreeType.FT_Done_Face;
import static org.lwjgl.util.freetype.FreeType.FT_Get_Char_Index;
import static org.lwjgl.util.freetype.FreeType.FT_Init_FreeType;
import static org.lwjgl.util.freetype.FreeType.FT_Library_Version;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;
import static org.lwjgl.util.freetype.FreeType.FT_New_Memory_Face;
import static org.lwjgl.util.freetype.FreeType.FT_Property_Set;
import static org.lwjgl.util.freetype.FreeType.FT_Set_Pixel_Sizes;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Outline;
import org.lwjgl.util.freetype.FT_Vector;

public final class NativeProbe {

  private static final int RENDER = 0x4;
  private static final int METRICS_PROBE = 0x400008;
  private static final int OLD_FIX = 0x2;
  private static final int MOD_FIX = 0x2 | 0x8000;

  private NativeProbe() {}

  public static void main(String[] args) throws Exception {
    Path dir = Path.of(args[0]);
    try (MemoryStack stack = MemoryStack.stackPush()) {
      PointerBuffer lib = stack.mallocPointer(1);
      check(FT_Init_FreeType(lib));
      long library = lib.get(0);
      IntBuffer major = stack.mallocInt(1);
      IntBuffer minor = stack.mallocInt(1);
      IntBuffer patch = stack.mallocInt(1);
      FT_Library_Version(library, major, minor, patch);
      System.out.printf(
          "bundled FreeType %d.%d.%d, interpreter %s%n",
          major.get(0), minor.get(0), patch.get(0), args[1]);
      if (args[1].equals("v35")) {
        ByteBuffer version = stack.malloc(4).putInt(0, 35);
        check(FT_Property_Set(library, "truetype", "interpreter-version", version));
      }

      int clean = top(library, dir.resolve("plain.ttf"), 0);
      System.out.println("clean baseline top = " + clean);
      boolean ok = true;
      for (String font : new String[] {"hinted.ttf", "fpgm.ttf", "tricky.ttf"}) {
        for (int flags : new int[] {RENDER, METRICS_PROBE}) {
          int none = top(library, dir.resolve(font), flags);
          int old = top(library, dir.resolve(font), flags | OLD_FIX);
          int mod = top(library, dir.resolve(font), flags | MOD_FIX);
          boolean stopped = mod == clean;
          ok &= stopped;
          System.out.printf(
              "  %-11s %-13s unpatched=%5d  old 0x2=%5d  mod 0x8002=%5d  %s%n",
              font,
              flags == RENDER ? "render" : "metrics-probe",
              none,
              old,
              mod,
              stopped ? "stopped" : "RAN");
        }
      }
      System.out.println(ok ? "RESULT: PASS" : "RESULT: FAIL");
    }
  }

  private static int top(long library, Path path, int flags) throws Exception {
    byte[] bytes = Files.readAllBytes(path);
    ByteBuffer data = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
    try (MemoryStack stack = MemoryStack.stackPush()) {
      PointerBuffer faceBuffer = stack.mallocPointer(1);
      check(FT_New_Memory_Face(library, data, 0, faceBuffer));
      FT_Face face = FT_Face.create(faceBuffer.get(0));
      check(FT_Set_Pixel_Sizes(face, 0, 16));
      check(FT_Load_Glyph(face, FT_Get_Char_Index(face, 'A'), flags));
      FT_Outline outline = face.glyph().outline();
      FT_Vector.Buffer points = outline.points();
      int max = Integer.MIN_VALUE;
      for (int i = 0; i < outline.n_points(); i++) {
        max = (int) Math.max(max, points.get(i).y());
      }
      FT_Done_Face(face);
      return max;
    } finally {
      MemoryUtil.memFree(data);
    }
  }

  private static void check(int err) {
    if (err != 0) {
      throw new IllegalStateException("FreeType error " + err);
    }
  }
}
