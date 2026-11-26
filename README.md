# 🧩 Minecraft2shell Patcher

Official download: [Modrinth](https://modrinth.com/mod/minecraft2shell-fix)

This mod blocks Minecraft2Shell, a remote code execution (RCE) vulnerability in the Minecraft: Java Edition client. Without a fix, a server can run arbitrary native code on a player's machine by sending a crafted resource pack. No user interaction is necessary past accepting the pack.

## The vulnerability

Minecraft renders TrueType (`.ttf`) fonts from resource packs through FreeType, a native library loaded via LWJGL. TrueType fonts can contain hinting instructions: bytecode that FreeType's TrueType interpreter executes when it rasterizes a glyph. A memory-safety bug in this interpreter (FreeType issue 1419) lets a crafted font corrupt memory in the client process and get code execution.

The attack surface is the resource pack pipeline:

1. The player joins a server that sends a resource pack, or installs a pack from a third party.
2. The pack registers a `ttf` font provider that points to the malicious font.
3. The client loads the font and renders a glyph. FreeType runs the font's hinting bytecode.
4. The bytecode triggers the bug and runs the attacker's payload with the privileges of the player's user account.

The code runs in native code, outside the JVM, so Java sandboxing and the Minecraft security manager give no protection. A public proof of concept was released on 22 November 2026.

## Affected versions

| Minecraft version | Bundled FreeType | Status |
|---|---|---|
| 26.3 | 2.14.x, then 2.15.0 | Exploited. Public PoC confirmed on Windows x64. Fixed by Mojang (FreeType 2.15.0). |
| 26.1 to 26.2 | 2.14.1 | Vulnerable code present. Exploit not confirmed. |
| 1.20.5 to 1.21.11 | 2.13.2 | Vulnerable code theoretically present. Not tested. |
| 1.20.4 and earlier | None (stb_truetype) | Not affected by this bug. These versions do not use FreeType. |

The upstream fix is in FreeType 2.15.0. Mojang has since shipped it for 26.3 through a launcher library update. On 26.3, fully restart your launcher to get it. Some third-party launchers keep the old version file, so check yours. Older versions have no official fix, and this mod is for them.

## How the patch works

This mod is intentionally minimal. It has no configuration, no network access and no dependencies other than the mod loader. The fix is one mixin. The toast described below is a second, optional mixin.

The fix targets LWJGL's FreeType bindings, mainly `FT_Load_Glyph`, and adds `FT_LOAD_NO_HINTING` and `FT_LOAD_NO_AUTOHINT` to the load flags. With these flags set, FreeType does not run the TrueType bytecode interpreter, so the vulnerable code path is never reached. The malicious instructions in the font stay as inert data.

Both flags are needed. FreeType ignores `FT_LOAD_NO_HINTING` for "tricky" fonts, and it decides a font is tricky from the font's own name or checksums, which an attacker controls. FreeType only keeps hinting off for those fonts when `FT_LOAD_NO_AUTOHINT` is set as well.

The mod hooks the bindings, not one call in Minecraft, because the game loads glyphs in two places: `TrueTypeGlyphProvider` and `NativeImage.copyFromFont`. A fix at only one of them leaves the other open. `FT_Load_Char`, `FT_Get_Advance` and `FT_Get_Advances` are hooked too, in case a future version uses them.

Outlines still render normally. Hinting only adjusts glyph outlines to the pixel grid at small sizes, and Minecraft rasterizes TrueType fonts at fixed sizes into a glyph atlas, so the visual difference is very small for most fonts.

## Server pack warning

When a TrueType font from a server resource pack loads, the mod shows a toast with a barrier icon. It tells you the server's pack uses TrueType fonts, which is the path this exploit goes through, and that the patch is active. It does not mean the pack is malicious; most TrueType fonts are harmless. Fonts from your own packs and from mods do not trigger it. If the toast code ever fails to load, the fix still applies.

## Installation

1. Download the jar for your Minecraft version from the Releases page: `1.21-1.21.11` for any 1.21.x version, or `26.1-26.2` for 26.1 to 26.2.
2. Put it in your `mods` folder.
3. Start the game. No setup is necessary.

Each jar carries the toast for every version it supports and picks the right one at launch. On 26.3, use Mojang's fix above. Players on 1.20.5 or 1.20.6 should update to 1.21 or later.

The mod is client-side only. Servers cannot install a fix for their players. Each player must install it, or update to a version that ships a patched FreeType.

## Limitations

- This is a mitigation, not an upstream fix. It removes the attack path through the hinting interpreter. It does not fix other bugs in FreeType that do not use hinting.
- Some fonts that depend on hinting for legibility at small sizes can look slightly different.
- Only download this mod from this repository or the Modrinth page linked above. Fake "security fix" jars are a common way to spread malware during a disclosure like this. If you are not sure, read the source and build it yourself.

## Reporting

To report a bypass or a problem with the patch, open a private security advisory on this repository. Do not open a public issue for a working bypass.
