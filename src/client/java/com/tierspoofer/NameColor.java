package com.tierspoofer;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NameColor {
    private static final int[] RAINBOW = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF};

    private final int[] stops;

    private NameColor(int[] stops) {
        this.stops = stops;
    }

    public static NameColor parse(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        if (s.equalsIgnoreCase("rainbow")) return new NameColor(RAINBOW);

        String[] parts = s.split("[\\s,\\-]+");
        List<Integer> colors = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            Integer rgb = parseHex(part);
            if (rgb == null) return null;
            colors.add(rgb);
        }
        if (colors.isEmpty()) return null;
        return new NameColor(colors.stream().mapToInt(Integer::intValue).toArray());
    }

    public static boolean isValid(String input) {
        return parse(input) != null;
    }

    private static Integer parseHex(String part) {
        String hex = part.startsWith("#") ? part.substring(1) : part;
        if (hex.length() == 3) {
            // #F5A -> #FF55AA
            hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        }
        if (hex.length() != 6) return null;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int colorAt(int index, int length) {
        if (stops.length == 1 || length <= 1) return stops[0];
        float t = (float) index / (length - 1) * (stops.length - 1);
        int seg = Math.min((int) t, stops.length - 2);
        float f = t - seg;
        int a = stops[seg], b = stops[seg + 1];
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * f);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * f);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * f);
        return (r << 16) | (g << 8) | bl;
    }

    public int primary() {
        return stops[0];
    }

    public Text apply(Text text) {
        List<String> parts = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        text.visit((style, string) -> {
            parts.add(string);
            styles.add(style);
            return Optional.empty();
        }, Style.EMPTY);

        int length = 0;
        for (String part : parts) length += part.codePointCount(0, part.length());

        MutableText out = Text.empty();
        int index = 0;
        for (int p = 0; p < parts.size(); p++) {
            String part = parts.get(p);
            Style style = styles.get(p);
            if (style.getColor() != null) {
                out.append(Text.literal(part).setStyle(style));
                index += part.codePointCount(0, part.length());
                continue;
            }
            if (stops.length == 1) {
                out.append(Text.literal(part).setStyle(style.withColor(TextColor.fromRgb(stops[0]))));
                index += part.codePointCount(0, part.length());
                continue;
            }
            int i = 0;
            while (i < part.length()) {
                int cp = part.codePointAt(i);
                int rgb = colorAt(index++, length);
                out.append(Text.literal(new String(Character.toChars(cp)))
                        .setStyle(style.withColor(TextColor.fromRgb(rgb))));
                i += Character.charCount(cp);
            }
        }
        return out;
    }
}
