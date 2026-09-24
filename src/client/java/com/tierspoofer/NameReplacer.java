package com.tierspoofer;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Replaces usernames inside an arbitrary {@link Text} (chat lines, death
 * messages, tab-list entries, nametags) while keeping everything around them
 * intact: rank prefixes, team colors, hover/click events.
 *
 * The text is flattened into styled runs, every whole-word occurrence of a
 * username is swapped for its replacement Text (which inherits the style of
 * the character it replaces, then applies its own colors on top), and the
 * result is rebuilt. All names are matched in one pass, so a replacement is
 * never itself replaced again (A->B and B->C won't turn A into C).
 */
public final class NameReplacer {
    private NameReplacer() {
    }

    /**
     * @param text         the original text
     * @param replacements username (matched case-insensitively, whole word) -> replacement
     * @return the rewritten text, or {@code text} itself when nothing matched
     */
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
        String lower = plain.toLowerCase();

        // Per-character style lookup.
        Style[] charStyles = new Style[plain.length()];
        int pos = 0;
        for (int p = 0; p < parts.size(); p++) {
            for (int i = 0; i < parts.get(p).length(); i++) charStyles[pos++] = styles.get(p);
        }

        // Find non-overlapping matches, earliest first, longest name on ties.
        List<int[]> matches = new ArrayList<>(); // {start, end}
        List<Text> matchTexts = new ArrayList<>();
        int i = 0;
        while (i < plain.length()) {
            String bestName = null;
            for (String name : replacements.keySet()) {
                if (name == null || name.isEmpty()) continue;
                if (lower.startsWith(name.toLowerCase(), i) && isBoundary(plain, i - 1)
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
            // Wrap so the replacement inherits e.g. bold/hover of the original name
            // but its own colors win.
            out.append(Text.empty().setStyle(charStyles[start]).append(matchTexts.get(m)));
            cursor = end;
        }
        appendRuns(out, plain, charStyles, cursor, plain.length());
        return out;
    }

    /** Appends plain[from, to) as runs of identical style. */
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

    /** Minecraft usernames are [A-Za-z0-9_]; a match must not touch another such character. */
    private static boolean isBoundary(String s, int index) {
        if (index < 0 || index >= s.length()) return true;
        char c = s.charAt(index);
        return !(Character.isLetterOrDigit(c) || c == '_');
    }
}
