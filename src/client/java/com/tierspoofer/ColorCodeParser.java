package com.tierspoofer;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorCodeParser {
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{6}");

    private ColorCodeParser() {
    }

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
