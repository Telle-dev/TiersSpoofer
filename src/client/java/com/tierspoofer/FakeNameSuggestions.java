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
    // "/tpa " -> real names offered right after it. Lets "/tpa k1" suggest k1rbe even
    // though the server itself only knows (and only filters by) the real name.
    private static final Map<String, Set<String>> PLAYER_SLOTS = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
            return size() > 100;
        }
    };

    private FakeNameSuggestions() {
    }

    // A spoofed player's two names. The tab list can hand out either one, depending on the Mods setting.
    private record Names(String real, String fake) {
        String other(String name) {
            return name.equalsIgnoreCase(real) ? fake : real;
        }
    }

    // lower-case real or fake name -> both names
    private static Map<String, Names> namesByEither() {
        Map<String, Names> names = new HashMap<>();
        for (SpoofedPlayer player : TierSpoofer.getSpoofedPlayers().values()) {
            String real = player.getOriginalName();
            String fake = player.getSkinTargetName();
            if (real == null || fake == null || fake.isEmpty() || fake.equalsIgnoreCase(real)) continue;
            Names pair = new Names(real, fake);
            names.put(real.toLowerCase(Locale.ROOT), pair);
            names.put(fake.toLowerCase(Locale.ROOT), pair);
        }
        return names;
    }

    // Every name plus the other name of anyone spoofed, for tab in normal chat.
    public static Collection<String> withFakeNames(Collection<String> names) {
        if (names == null || !TierSpoofer.getConfig().isEnabled() || !TierSpoofer.getConfig().isCommandNames()) return names;
        Map<String, Names> pairs = namesByEither();
        if (pairs.isEmpty()) return names;
        List<String> out = new ArrayList<>(names);
        Set<String> seen = new HashSet<>();
        for (String name : names) seen.add(name.toLowerCase(Locale.ROOT));
        for (String name : names) {
            Names pair = pairs.get(name.toLowerCase(Locale.ROOT));
            if (pair == null) continue;
            String other = pair.other(name);
            if (seen.add(other.toLowerCase(Locale.ROOT))) out.add(other);
        }
        return out;
    }

    public static Suggestions add(Suggestions suggestions, String text, int cursor) {
        if (suggestions == null || text == null) return suggestions;
        cursor = Math.max(0, Math.min(cursor, text.length()));
        int wordStart = text.lastIndexOf(' ', cursor - 1) + 1;
        if (wordStart <= 0) return suggestions; // still typing the command itself

        Map<String, Names> pairs = namesByEither();
        if (pairs.isEmpty()) return suggestions;

        String before = text.substring(0, wordStart).toLowerCase(Locale.ROOT);
        String word = text.substring(wordStart, cursor).toLowerCase(Locale.ROOT);
        StringRange wordRange = StringRange.between(wordStart, cursor);

        List<Suggestion> out = new ArrayList<>(suggestions.getList());
        Set<String> shown = new HashSet<>();
        for (Suggestion s : suggestions.getList()) shown.add(s.getText().toLowerCase(Locale.ROOT));

        Set<String> playersHere = new HashSet<>();
        for (Suggestion s : suggestions.getList()) {
            Names pair = pairs.get(s.getText().toLowerCase(Locale.ROOT));
            if (pair == null) continue;
            playersHere.add(pair.real().toLowerCase(Locale.ROOT));
            String other = pair.other(s.getText());
            if (other.toLowerCase(Locale.ROOT).startsWith(word) && shown.add(other.toLowerCase(Locale.ROOT))) {
                out.add(new Suggestion(s.getRange(), other, s.getTooltip()));
            }
        }

        synchronized (PLAYER_SLOTS) {
            if (!playersHere.isEmpty()) PLAYER_SLOTS.computeIfAbsent(before, k -> new HashSet<>()).addAll(playersHere);
            if (!word.isEmpty()) {
                for (String real : PLAYER_SLOTS.getOrDefault(before, Set.of())) {
                    Names pair = pairs.get(real);
                    if (pair == null) continue;
                    for (String name : new String[]{pair.real(), pair.fake()}) {
                        if (name.toLowerCase(Locale.ROOT).startsWith(word) && shown.add(name.toLowerCase(Locale.ROOT))) {
                            out.add(new Suggestion(wordRange, name));
                        }
                    }
                }
            }
        }

        if (out.size() == suggestions.getList().size()) return suggestions;
        return new Suggestions(suggestions.getList().isEmpty() ? wordRange : suggestions.getRange(), out);
    }
}
