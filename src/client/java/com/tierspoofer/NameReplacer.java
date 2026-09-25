package com.tierspoofer;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NameReplacer {
    private NameReplacer() {
    }

    public static Text replace(Text text, Map<String, Text> replacements) {
        if (text == null || replacements.isEmpty()) return text;

        List<String> parts = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        text.visit((style, string) -> {
            if (!string.isEmpty()) {
                parts.add(string);
                styles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);

        StringBuilder full = new StringBuilder();
        for (String part : parts) full.append(part);
        String plain = full.toString();

        Style[] charStyles = new Style[plain.length()];
        int pos = 0;
        for (int p = 0; p < parts.size(); p++) {
            for (int i = 0; i < parts.get(p).length(); i++) charStyles[pos++] = styles.get(p);
        }

        List<int[]> matches = new ArrayList<>();
        List<Text> matchTexts = new ArrayList<>();
        int i = 0;
        while (i < plain.length()) {
            String bestName = null;
            for (String name : replacements.keySet()) {
                if (name == null || name.isEmpty()) continue;
                if (plain.regionMatches(true, i, name, 0, name.length()) && isBoundary(plain, i - 1)
                        && isBoundary(plain, i + name.length())
                        && (bestName == null || name.length() > bestName.length())) {
                    bestName = name;
                }
            }
            if (bestName != null) {
                matches.add(new int[]{i, i + bestName.length()});
                matchTexts.add(replacements.get(bestName));
                i += bestName.length();
            } else {
                i++;
            }
        }
        if (matches.isEmpty()) return text;

        MutableText out = Text.empty();
        int cursor = 0;
        for (int m = 0; m < matches.size(); m++) {
            int start = matches.get(m)[0], end = matches.get(m)[1];
            appendRuns(out, plain, charStyles, cursor, start);
            // keeps hover/bold etc from the original, its own colors win
            out.append(Text.empty().setStyle(charStyles[start]).append(matchTexts.get(m)));
            cursor = end;
        }
        appendRuns(out, plain, charStyles, cursor, plain.length());
        return out;
    }

    // Tier tags other mods put around a name. TierTagger: "<icon>HT3 (peak: HT2) | Name",
    // PvPTiers' Tiers mod: "<icon> HT3^ EU | Name" and/or "Name | EU ^HT3 <icon>".
    private static final String ICON = "[\\uE000-\\uF8FF]";
    private static final String TIER = "(?<![A-Za-z0-9])\\^?R?[HL]T[1-5]\\^?";
    private static final String EXTRA = "(?: \\(peak: \\^?R?[HL]T[1-5]\\))?(?: [A-Za-z]{2,7})?";
    private static final Pattern TAG_AT_START = Pattern.compile(
            "^\u200C?(?:" + ICON + " ?)?" + TIER + EXTRA + " \\| ");
    private static final Pattern TAG_BEFORE_NAME = Pattern.compile(
            "(?:" + ICON + " ?)?" + TIER + EXTRA + " \\| $");
    private static final Pattern TAG_AFTER_NAME = Pattern.compile(
            "^ \\| (?:[A-Za-z]{2,7} )?\\^?R?[HL]T[1-5](?: ?" + ICON + ")?(?![A-Za-z0-9])");
    private static final Pattern TAG_AT_END = Pattern.compile(
            " \\| (?:[A-Za-z]{2,7} )?\\^?R?[HL]T[1-5](?: ?" + ICON + ")?\u200C?$");

    /**
     * Removes tier tags that other tier mods (TierTagger, PvPTiers' Tiers) added
     * around {@code name}, so ours doesn't show up next to theirs.
     */
    public static Text stripTierTags(Text text, String name) {
        if (text == null || name == null || name.isEmpty()) return text;

        List<String> parts = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        text.visit((style, string) -> {
            if (!string.isEmpty()) {
                parts.add(string);
                styles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);
        StringBuilder sb = new StringBuilder();
        for (String part : parts) sb.append(part);
        String plain = sb.toString();
        if (!plain.contains(" | ")) return text;

        Style[] charStyles = new Style[plain.length()];
        int pos = 0;
        for (int p = 0; p < parts.size(); p++) {
            for (int i = 0; i < parts.get(p).length(); i++) charStyles[pos++] = styles.get(p);
        }
        boolean[] drop = new boolean[plain.length()];
        boolean changed = false;

        // a text can carry a tag from each mod, so keep going until nothing matches
        for (int round = 0; round < 4; round++) {
            String kept = keptString(plain, drop);
            int[] map = keptIndexMap(drop);
            int nameAt = indexOfName(kept, name);
            if (nameAt < 0) break;
            int[] range = null;

            Matcher m = TAG_AT_START.matcher(kept);
            if (m.find() && m.end() <= nameAt) range = new int[]{m.start(), m.end()};
            if (range == null) {
                m = TAG_BEFORE_NAME.matcher(kept.substring(0, nameAt));
                if (m.find()) range = new int[]{m.start(), m.end()};
            }
            if (range == null) {
                int afterName = nameAt + name.length();
                m = TAG_AFTER_NAME.matcher(kept.substring(afterName));
                if (m.find()) range = new int[]{afterName + m.start(), afterName + m.end()};
            }
            if (range == null) {
                m = TAG_AT_END.matcher(kept);
                if (m.find() && m.start() >= nameAt + name.length()) range = new int[]{m.start(), m.end()};
            }
            if (range == null) break;

            for (int i = range[0]; i < range[1]; i++) {
                if (kept.charAt(i) != '\u200C') drop[map[i]] = true;
            }
            changed = true;
        }
        if (!changed) return text;

        MutableText out = Text.empty();
        int runStart = -1;
        for (int i = 0; i <= plain.length(); i++) {
            boolean keep = i < plain.length() && !drop[i];
            if (keep && runStart >= 0 && charStyles[i].equals(charStyles[runStart])) continue;
            if (runStart >= 0) out.append(Text.literal(plain.substring(runStart, i)).setStyle(charStyles[runStart]));
            runStart = keep ? i : -1;
        }
        return out;
    }

    private static String keptString(String plain, boolean[] drop) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plain.length(); i++) if (!drop[i]) sb.append(plain.charAt(i));
        return sb.toString();
    }

    private static int[] keptIndexMap(boolean[] drop) {
        int count = 0;
        for (boolean d : drop) if (!d) count++;
        int[] map = new int[count];
        int k = 0;
        for (int i = 0; i < drop.length; i++) if (!drop[i]) map[k++] = i;
        return map;
    }

    private static int indexOfName(String s, String name) {
        for (int i = 0; i + name.length() <= s.length(); i++) {
            if (s.regionMatches(true, i, name, 0, name.length())
                    && isBoundary(s, i - 1) && isBoundary(s, i + name.length())) {
                return i;
            }
        }
        return -1;
    }

    private static void appendRuns(MutableText out, String plain, Style[] charStyles, int from, int to) {
        int runStart = from;
        for (int i = from + 1; i <= to; i++) {
            if (i == to || !charStyles[i].equals(charStyles[runStart])) {
                if (i > runStart) {
                    out.append(Text.literal(plain.substring(runStart, i)).setStyle(charStyles[runStart]));
                }
                runStart = i;
            }
        }
    }

    private static boolean isBoundary(String s, int index) {
        if (index < 0 || index >= s.length()) return true;
        char c = s.charAt(index);
        return !(Character.isLetterOrDigit(c) || c == '_');
    }
}
