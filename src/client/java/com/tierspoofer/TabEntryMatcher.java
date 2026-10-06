// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer;

import com.tierspoofer.model.SpoofedPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TabEntryMatcher {
    private static final Pattern LEGACY_CODE = Pattern.compile("§.");
    private static final Pattern INVISIBLE = Pattern.compile("[\\p{Cf}\\p{Cc}]");
    private static final int MIN_LENGTH = 3;

    private static volatile Map<UUID, SpoofedPlayer> entryName = Map.of();
    private static volatile Map<UUID, SpoofedPlayer> entrySkin = Map.of();

    private TabEntryMatcher() {
    }

    public static SpoofedPlayer forName(UUID entryUuid) {
        return entryUuid == null ? null : entryName.get(entryUuid);
    }

    public static SpoofedPlayer forSkin(UUID entryUuid) {
        return entryUuid == null ? null : entrySkin.get(entryUuid);
    }

    public static void update(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (!TierSpoofer.getConfig().isEnabled() || handler == null || TierSpoofer.getSpoofedPlayers().isEmpty()) {
            clear();
            return;
        }

        Map<UUID, String> rows = new HashMap<>();
        for (PlayerListEntry entry : handler.getPlayerList()) {
            rows.put(entry.getProfile().getId(), clean(rowText(entry)));
        }

        Map<UUID, SpoofedPlayer> names = new HashMap<>();
        Map<UUID, SpoofedPlayer> skins = new HashMap<>();
        for (SpoofedPlayer player : TierSpoofer.getSpoofedPlayers().values()) {
            List<String> candidates = candidates(player, rows);
            if (candidates.isEmpty()) continue;
            for (Map.Entry<UUID, String> row : rows.entrySet()) {
                if (row.getKey().equals(player.getUuid()) || TierSpoofer.getSpoofedPlayer(row.getKey()) != null) continue;
                if (!containsAny(row.getValue(), candidates)) continue;
                names.putIfAbsent(row.getKey(), player);
                if (player.getSkinTargetName() != null) skins.putIfAbsent(row.getKey(), player);
            }
        }
        entryName = names;
        entrySkin = skins;
    }

    private static void clear() {
        if (!entryName.isEmpty()) entryName = Map.of();
        if (!entrySkin.isEmpty()) entrySkin = Map.of();
    }

    private static List<String> candidates(SpoofedPlayer player, Map<UUID, String> rows) {
        List<String> out = new ArrayList<>();
        addCandidate(out, clean(player.getOriginalName()));

        addCandidate(out, rows.get(player.getUuid()));
        return out;
    }

    private static void addCandidate(List<String> out, String name) {
        if (name != null && name.length() >= MIN_LENGTH && !out.contains(name)) out.add(name);
    }

    private static boolean containsAny(String row, List<String> candidates) {
        for (String candidate : candidates) {
            if (matchesName(row, candidate)) return true;
        }
        return false;
    }

    public static boolean matchesName(String row, String name) {
        int from = 0;
        while ((from = row.indexOf(name, from)) >= 0) {
            int end = from + name.length();
            boolean startsWord = from == 0 || !isNameChar(row.charAt(from - 1));
            boolean endsWord = end == row.length() || !isNameChar(row.charAt(end));
            if (startsWord && endsWord) return true;
            from++;
        }
        return false;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String rowText(PlayerListEntry entry) {
        Text shown = entry.getDisplayName();
        return shown != null ? shown.getString() : TierSpoofer.realProfile(entry).getName();
    }

    public static String clean(String text) {
        if (text == null) return null;
        String plain = LEGACY_CODE.matcher(text).replaceAll("");
        return INVISIBLE.matcher(plain).replaceAll("").strip().toLowerCase(Locale.ROOT);
    }
}
