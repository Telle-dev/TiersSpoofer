package com.tierspoofer;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses user-entered "fake name" strings that may contain Minecraft-style
 * formatting codes written with '&' instead of the real section-sign '§'
 * (since players can't type '§' easily, '&' is the conventional stand-in
 * used by basically every Minecraft plugin/mod with a config-driven name
 * or message field), plus an optional "&#RRGGBB" hex-color extension.
 *
 * Two outputs are derived from a single raw input, and BOTH must be used
 * correctly by callers:
 *
 *   - {@link #stripCodes(String)}  -> the plain, code-free string. This is
 *     the ONLY string that may ever reach skin/cape/profile/UUID lookups,
 *     cache keys, or network identifiers (SkinCache, Mojang API calls,
 *     tab-list UUID resolution, the chat/death-screen substring-replace,
 *     etc). It must never contain '&', '§', or a "#RRGGBB" fragment.
 *
 *   - {@link #parse(String)} -> a styled {@link Text} for rendering only
 *     (nametags, tab list, GUI list rows, scoreboard/team display names,
 *     chat replacement, profile previews, etc). Never used for lookups.
 *
 * Recognized codes (case-insensitive), matching vanilla Formatting:
 *   &0-&9, &a-&f   -> standard 16 colors
 *   &#RRGGBB       -> full RGB hex color (6 hex digits, case-insensitive)
 *   &k  -> obfuscated
 *   &l  -> bold
 *   &m  -> strikethrough
 *   &n  -> underline
 *   &o  -> italic
 *   &r  -> reset (clears all active formatting, including color)
 *
 * Exactly like vanilla '§' formatting, a new color code (standard or hex)
 * resets bold/italic/underline/strikethrough/obfuscated back to false —
 * only "&r" and color codes reset; formatting codes stack.
 *
 * Unknown/invalid sequences (e.g. "&z", a bare trailing '&', or
 * "&#GGGGGG" with non-hex digits) are left untouched as literal text
 * (including the '&'), so normal names that happen to contain an '&' for
 * some other reason don't lose characters. This also means plain names
 * with no codes at all behave exactly as before (full backward
 * compatibility).
 */
public final class ColorCodeParser {

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{6}");

    private ColorCodeParser() {
    }

    /**
     * Returns the plain text with every valid "&<code>" and "&#RRGGBB"
     * sequence removed. Safe to feed into skin/cape/profile/UUID lookups,
     * cache keys, and any other backend/network-facing string field.
     */
    public static String stripCodes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder(input.length());
        int i = 0;
        int len = input.length();
        while (i < len) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < len) {
                char next = input.charAt(i + 1);
                if (next == '#') {
                    Matcher m = HEX_PATTERN.matcher(input.substring(i + 2));
                    if (m.find()) {
                        i += 2 + 6;
                        continue;
                    }
                } else if (isValidLegacyCode(next)) {
                    i += 2;
                    continue;
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    /**
     * Parses the raw input into a styled {@link Text} for display. Color
     * and formatting codes apply to all subsequent characters until the
     * next code or a "&r" reset, exactly like vanilla '§' formatting.
     * Only plain characters are ever included in the rendered output; the
     * '&...' markers themselves are never part of the result.
     */
    public static Text parse(String input) {
        if (input == null || input.isEmpty()) {
            return Text.empty();
        }

        MutableText result = Text.empty();
        StringBuilder segment = new StringBuilder();
        TextColor color = null;
        boolean bold = false, italic = false, underline = false, strikethrough = false, obfuscated = false;

        int i = 0;
        int len = input.length();
        while (i < len) {
            char c = input.charAt(i);

            if (c == '&' && i + 1 < len && input.charAt(i + 1) == '#') {
                Matcher m = HEX_PATTERN.matcher(input.substring(i + 2));
                if (m.find()) {
                    flush(result, segment, color, bold, italic, underline, strikethrough, obfuscated);
                    String hex = input.substring(i + 2, i + 8);
                    color = TextColor.fromRgb(Integer.parseInt(hex, 16));
                    bold = italic = underline = strikethrough = obfuscated = false;
                    i += 8;
                    continue;
                }
            }

            if (c == '&' && i + 1 < len && isValidLegacyCode(input.charAt(i + 1))) {
                flush(result, segment, color, bold, italic, underline, strikethrough, obfuscated);
                char code = Character.toLowerCase(input.charAt(i + 1));
                if (code == 'r') {
                    color = null;
                    bold = italic = underline = strikethrough = obfuscated = false;
                } else {
                    Formatting formatting = Formatting.byCode(code);
                    if (formatting != null) {
                        if (formatting.isColor()) {
                            color = TextColor.fromFormatting(formatting);
                            bold = italic = underline = strikethrough = obfuscated = false;
                        } else {
                            switch (code) {
                                case 'l' -> bold = true;
                                case 'o' -> italic = true;
                                case 'n' -> underline = true;
                                case 'm' -> strikethrough = true;
                                case 'k' -> obfuscated = true;
                                default -> {
                                }
                            }
                        }
                    }
                }
                i += 2;
                continue;
            }

            segment.append(c);
            i++;
        }
        flush(result, segment, color, bold, italic, underline, strikethrough, obfuscated);
        return result;
    }

    private static void flush(MutableText result, StringBuilder segment, TextColor color,
                               boolean bold, boolean italic, boolean underline,
                               boolean strikethrough, boolean obfuscated) {
        if (segment.length() == 0) {
            return;
        }
        result.append(Text.literal(segment.toString()).styled(style -> style
                .withColor(color)
                .withBold(bold)
                .withItalic(italic)
                .withUnderline(underline)
                .withStrikethrough(strikethrough)
                .withObfuscated(obfuscated)));
        segment.setLength(0);
    }

    private static boolean isValidLegacyCode(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9')
                || (lower >= 'a' && lower <= 'f')
                || lower == 'k' || lower == 'l' || lower == 'm'
                || lower == 'n' || lower == 'o' || lower == 'r';
    }
}
