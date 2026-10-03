// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.tierspoofer.model.SpoofedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Adds fake names next to the real ones in command tab-complete, so both can be picked. Sending a
// command turns the fake name back into the real one (see TierSpoofer.toRealNames).
public final class FakeNameSuggestions {
    // "/tpa " -> real names the server offered right after it. Lets "/tpa k1" suggest k1rbe even
    // though the server itself only knows (and only filters by) the real name.
    private static final Map<String, Set<String>> PLAYER_SLOTS = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
            return size() > 100;
        }
    };

    private FakeNameSuggestions() {
    }

    // Real names plus the fake name of everyone spoofed, for tab in normal chat.
    public static Collection<String> withFakeNames(Collection<String> names) {
        if (names == null || !TierSpoofer.getConfig().isEnabled() || !TierSpoofer.getConfig().isCommandNames()) return names;
        Map<String, String> fakeByReal = fakeNamesByReal();
        if (fakeByReal.isEmpty()) return names;
        List<String> out = new ArrayList<>(names);
        Set<String> seen = new HashSet<>();
        for (String name : names) seen.add(name.toLowerCase(Locale.ROOT));
        for (String name : names) {
            String fake = fakeByReal.get(name.toLowerCase(Locale.ROOT));
            if (fake != null && seen.add(fake.toLowerCase(Locale.ROOT))) out.add(fake);
        }
        return out;
    }

    private static Map<String, String> fakeNamesByReal() {
        Map<String, String> fakeByReal = new HashMap<>();
        for (SpoofedPlayer player : TierSpoofer.getSpoofedPlayers().values()) {
            String real = player.getOriginalName();
            String fake = player.getSkinTargetName();
            if (real != null && fake != null && !fake.isEmpty() && !fake.equalsIgnoreCase(real)) {
                fakeByReal.put(real.toLowerCase(Locale.ROOT), fake);
            }
        }
        return fakeByReal;
    }

    public static Suggestions add(Suggestions suggestions, String text, int cursor) {
        if (suggestions == null || text == null) return suggestions;
        cursor = Math.max(0, Math.min(cursor, text.length()));
        int wordStart = text.lastIndexOf(' ', cursor - 1) + 1;
        if (wordStart <= 0) return suggestions; // still typing the command itself

        Map<String, String> fakeByReal = fakeNamesByReal();
        if (fakeByReal.isEmpty()) return suggestions;

        String before = text.substring(0, wordStart).toLowerCase(Locale.ROOT);
        String word = text.substring(wordStart, cursor).toLowerCase(Locale.ROOT);
        StringRange wordRange = StringRange.between(wordStart, cursor);

        List<Suggestion> out = new ArrayList<>();
        Set<String> shown = new HashSet<>();
        Set<String> realsHere = new HashSet<>();
        for (Suggestion s : suggestions.getList()) {
            out.add(s);
            shown.add(s.getText().toLowerCase(Locale.ROOT));
        }
        for (Suggestion s : suggestions.getList()) {
            String real = s.getText().toLowerCase(Locale.ROOT);
            String fake = fakeByReal.get(real);
            if (fake == null) continue;
            realsHere.add(real);
            if (fake.toLowerCase(Locale.ROOT).startsWith(word) && shown.add(fake.toLowerCase(Locale.ROOT))) {
                out.add(new Suggestion(s.getRange(), fake, s.getTooltip()));
            }
        }

        synchronized (PLAYER_SLOTS) {
            if (!realsHere.isEmpty()) PLAYER_SLOTS.computeIfAbsent(before, k -> new HashSet<>()).addAll(realsHere);
            if (!word.isEmpty()) {
                Set<String> known = PLAYER_SLOTS.getOrDefault(before, Set.of());
                for (String real : known) {
                    String fake = fakeByReal.get(real);
                    if (fake != null && fake.toLowerCase(Locale.ROOT).startsWith(word) && shown.add(fake.toLowerCase(Locale.ROOT))) {
                        out.add(new Suggestion(wordRange, fake));
                    }
                }
            }
        }

        if (out.size() == suggestions.getList().size()) return suggestions;
        return new Suggestions(suggestions.getList().isEmpty() ? wordRange : suggestions.getRange(), out);
    }
}
