import static org.lwjgl.util.freetype.FreeType.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Vector;

/**
 * Loads the test fonts through the exact LWJGL FreeType natives Minecraft ships and reports whether
 * each font's hinting program ran. Uses the same FreeType.FT_Load_Glyph call and flag values as the
 * game. A top edge above the clean baseline means the font's bytecode executed.
 *
 * <p>Run it with the LWJGL core and FreeType jars, plus their natives-linux jars, that the target
 * Minecraft version lists in its version JSON (LWJGL 3.3.3 for 1.20.5 to 1.21.11, 3.4.1 for 26.1
 * to 26.2). First build the fonts with make_hinted_font.py, then:
 *
 * <pre>
 * CP=lwjgl.jar:lwjgl-natives-linux.jar:lwjgl-freetype.jar:lwjgl-freetype-natives-linux.jar
 * javac -cp $CP NativeProbe.java
 * java -cp .:$CP NativeProbe test v40
 * </pre>
 *
 * <p>Pass {@code v35} instead of {@code v40} to force the classic interpreter, which makes the
 * non-tricky fonts' programs visible too.
 */
public final class NativeProbe {

  private static final int RENDER = 0x4;
  private static final int METRICS_PROBE = 0x400008;
  private static final int OLD_FIX = 0x2;
  private static final int MOD_FIX = 0x2 | 0x8000;

  public static void main(String[] args) throws Exception {
    Path dir = Path.of(args[0]);
    try (MemoryStack stack = MemoryStack.stackPush()) {
      PointerBuffer lib = stack.mallocPointer(1);
      check(FT_Init_FreeType(lib));
      long library = lib.get(0);
      IntBuffer a = stack.mallocInt(1), b = stack.mallocInt(1), c = stack.mallocInt(1);
      FT_Library_Version(library, a, b, c);
      System.out.printf("bundled FreeType %d.%d.%d, interpreter %s%n",
          a.get(0), b.get(0), c.get(0), args[1]);
      if (args[1].equals("v35")) {
        ByteBuffer v = stack.malloc(4).putInt(0, 35);
        check(FT_Property_Set(library, "truetype", "interpreter-version", v));
      }

      int clean = top(library, dir.resolve("plain.ttf"), 0);
      System.out.println("clean baseline top = " + clean);
      boolean ok = true;
      for (String font : new String[] {"hinted.ttf", "fpgm.ttf", "tricky.ttf"}) {
        for (int[] call : new int[][] {{RENDER}, {METRICS_PROBE}}) {
          int f = call[0];
          int none = top(library, dir.resolve(font), f);
          int old = top(library, dir.resolve(font), f | OLD_FIX);
          int mod = top(library, dir.resolve(font), f | MOD_FIX);
          boolean stopped = mod == clean;
          ok &= stopped;
          System.out.printf("  %-11s %-13s unpatched=%5d  old 0x2=%5d  mod 0x8002=%5d  %s%n",
              font, f == RENDER ? "render" : "metrics-probe", none, old, mod,
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
      PointerBuffer pf = stack.mallocPointer(1);
      check(FT_New_Memory_Face(library, data, 0, pf));
      FT_Face face = FT_Face.create(pf.get(0));
      check(FT_Set_Pixel_Sizes(face, 0, 16));
      check(FT_Load_Glyph(face, FT_Get_Char_Index(face, 'A'), flags));
      var outline = face.glyph().outline();
      FT_Vector.Buffer pts = outline.points();
      int max = Integer.MIN_VALUE;
      for (int i = 0; i < outline.n_points(); i++) {
        max = (int) Math.max(max, pts.get(i).y());
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
