// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public enum TierList {
    MCTIERS("mctiers", "MCTiers", "https://mctiers.com/api/v2/profile/", true),
    PVPTIERS("pvptiers", "PvPTiers", "https://pvptiers.com/api/profile/", false),
    SUBTIERS("subtiers", "SubTiers", "https://subtiers.net/api/v2/profile/", true);

    public final String id;
    public final String displayName;
    private final String profileUrl;
    private final boolean dashedUuid;

    private final Map<String, Mode> modes = new LinkedHashMap<>();

    TierList(String id, String displayName, String profileUrl, boolean dashedUuid) {
        this.id = id;
        this.displayName = displayName;
        this.profileUrl = profileUrl;
        this.dashedUuid = dashedUuid;
    }

    static {
        MCTIERS.mode("vanilla", "Vanilla", '\ue708')
                .mode("sword", "Sword", '\ue706')
                .mode("axe", "Axe", '\ue701')
                .mode("pot", "Pot", '\ue704')
                .mode("nethop", "NethOP", '\ue703')
                .mode("uhc", "UHC", '\ue707')
                .mode("smp", "SMP", '\ue705')
                .mode("mace", "Mace", '\ue702');

        PVPTIERS.mode("crystal", "Crystal", '\uea01')
                .mode("sword", "Sword", '\uea02')
                .mode("uhc", "UHC", '\uea03')
                .mode("pot", "Pot", '\uea04')
                .mode("neth_pot", "NethPot", '\uea05')
                .mode("smp", "SMP", '\uea06')
                .mode("axe", "Axe", '\uea07')
                .mode("mace", "Mace", '\uea08');

        SUBTIERS.mode("minecart", "Minecart", '\ue809')
                .mode("dia_crystal", "DiaCrystal", '\ue805')
                .mode("debuff", "Debuff", '\ue804')
                .mode("elytra", "Elytra", '\ue807')
                .mode("speed", "Speed", '\ue811')
                .mode("creeper", "Creeper", '\ue803')
                .mode("manhunt", "Manhunt", '\ue808')
                .mode("dia_smp", "DiaSMP", '\ue806')
                .mode("bow", "Bow", '\ue802')
                .mode("bed", "Bed", '\ue801')
                .mode("og_vanilla", "OGVanilla", '\ue810')
                .mode("trident", "Trident", '\ue812');
    }

    private TierList mode(String key, String label, char icon) {
        modes.put(key, new Mode(key, label, icon));
        return this;
    }

    public record Mode(String key, String label, char icon) {
    }

    public Map<String, Mode> getModes() {
        return modes;
    }

    public Mode getMode(String keyOrLabel) {
        if (keyOrLabel == null) return null;
        String k = keyOrLabel.toLowerCase(java.util.Locale.ROOT);
        Mode m = modes.get(k);
        if (m != null) return m;
        for (Mode mode : modes.values()) {
            if (mode.label().equalsIgnoreCase(keyOrLabel)) return mode;
        }
        return switch (k) {
            case "neth_pot", "nethpot" -> modes.get("nethop");
            case "nethop" -> modes.get("neth_pot");
            default -> null;
        };
    }

    public String profileUrl(UUID uuid) {
        String id = dashedUuid ? uuid.toString() : uuid.toString().replace("-", "");
        return profileUrl + id;
    }

    public int getTierColor(String tier) {
        if (tier == null) return 0xFFFFFF;
        String t = tier.toUpperCase();
        if (this == PVPTIERS) {
            if (t.startsWith("R")) return 0x656565;
            return switch (t) {
                case "HT1", "LT1" -> 0xF5CE4D;
                case "HT2", "LT2" -> 0xBFCDD6;
                case "HT3", "LT3" -> 0xB06453;
                case "HT4", "LT4" -> 0xA0323D;
                case "HT5", "LT5" -> 0xBCBBC1;
                default -> 0xFFFFFF;
            };
        }
        return switch (t) {
            case "HT1" -> 0xE8BA3A;
            case "LT1" -> 0xD5B355;
            case "HT2" -> 0xC4D3E7;
            case "LT2" -> 0xA0A7B2;
            case "HT3" -> 0xF89F5A;
            case "LT3" -> 0xC67B42;
            case "HT4" -> 0x81749A;
            case "LT4" -> 0x655B79;
            case "HT5" -> 0x8F82A8;
            case "LT5" -> 0x655B79;
            case "RHT1" -> 0xB0944D;
            case "RLT1" -> 0xA8956E;
            case "RHT2" -> 0x9AA5B5;
            case "RLT2" -> 0x7E8690;
            case "RHT3" -> 0xC08560;
            case "RLT3" -> 0x9A6850;
            case "RHT4" -> 0x6A6280;
            case "RLT4" -> 0x524B63;
            case "RHT5" -> 0x746A88;
            case "RLT5" -> 0x524B63;
            default -> 0xFFFFFF;
        };
    }

    // Icon for a gamemode; falls back to other lists (older configs mixed them) and then a dot.
    public static char iconFor(TierList list, String gamemode) {
        Mode mode = list.getMode(gamemode);
        if (mode != null) return mode.icon();
        for (TierList other : values()) {
            mode = other.getMode(gamemode);
            if (mode != null) return mode.icon();
        }
        return '\u2022';
    }

    public TierList next() {
        TierList[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static TierList byId(String id) {
        if (id != null) {
            for (TierList list : values()) {
                if (list.id.equalsIgnoreCase(id)) return list;
            }
        }
        return MCTIERS;
    }
}
