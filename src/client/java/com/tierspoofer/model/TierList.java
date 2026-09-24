package com.tierspoofer.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The tier lists TierSpoofer knows how to display (and look up for real).
 *
 * Each list has its own gamemode set, icon glyphs and tier colors. The
 * glyphs are private-use characters mapped to bundled PNGs by
 * assets/minecraft/font/default.json:
 *   \ue701-\ue708  MCTiers icons   (same code points TierTagger uses)
 *   \ue801-\ue812  SubTiers icons  (same code points TierTagger uses)
 *   \uea01-\uea08  PvPTiers icons
 */
public enum TierList {
    MCTIERS("mctiers", "MCTiers", "https://mctiers.com/api/v2/profile/", true),
    PVPTIERS("pvptiers", "PvPTiers", "https://pvptiers.com/api/profile/", false),
    SUBTIERS("subtiers", "SubTiers", "https://subtiers.net/api/v2/profile/", true);

    public final String id;
    public final String displayName;
    private final String profileUrl;
    private final boolean dashedUuid;

    /** gamemode key (as used by the API's "rankings" object) -> Mode info, in display order. */
    private final Map<String, Mode> modes = new LinkedHashMap<>();

    TierList(String id, String displayName, String profileUrl, boolean dashedUuid) {
        this.id = id;
        this.displayName = displayName;
        this.profileUrl = profileUrl;
        this.dashedUuid = dashedUuid;
    }

    static {
        MCTIERS.mode("vanilla", "Vanilla", '\ue708', 0xE267FF)
                .mode("sword", "Sword", '\ue706', 0x02BCCE)
                .mode("axe", "Axe", '\ue701', 0x8E561B)
                .mode("pot", "Pot", '\ue704', 0xA51141)
                .mode("nethop", "NethOP", '\ue703', 0x492C72)
                .mode("uhc", "UHC", '\ue707', 0xF10003)
                .mode("smp", "SMP", '\ue705', 0x11574F)
                .mode("mace", "Mace", '\ue702', 0x656F84);

        PVPTIERS.mode("crystal", "Crystal", '\uea01', 0xE267FF)
                .mode("sword", "Sword", '\uea02', 0x02BCCE)
                .mode("uhc", "UHC", '\uea03', 0xF10003)
                .mode("pot", "Pot", '\uea04', 0xA51141)
                .mode("neth_pot", "NethPot", '\uea05', 0x492C72)
                .mode("smp", "SMP", '\uea06', 0x11574F)
                .mode("axe", "Axe", '\uea07', 0x8E561B)
                .mode("mace", "Mace", '\uea08', 0x656F84);

        SUBTIERS.mode("minecart", "Minecart", '\ue809', 0xDB441A)
                .mode("dia_crystal", "DiaCrystal", '\ue805', 0x66C4FF)
                .mode("debuff", "Debuff", '\ue804', 0xE3B136)
                .mode("elytra", "Elytra", '\ue807', 0x8B8CC8)
                .mode("speed", "Speed", '\ue811', 0x6DC4CD)
                .mode("creeper", "Creeper", '\ue803', 0x89DF89)
                .mode("manhunt", "Manhunt", '\ue808', 0x424242)
                .mode("dia_smp", "DiaSMP", '\ue806', 0x8E658C)
                .mode("bow", "Bow", '\ue802', 0x91705C)
                .mode("bed", "Bed", '\ue801', 0xB12F28)
                .mode("og_vanilla", "OGVanilla", '\ue810', 0xE9B750)
                .mode("trident", "Trident", '\ue812', 0x42957E);
    }

    private TierList mode(String key, String label, char icon, int color) {
        modes.put(key, new Mode(key, label, icon, color));
        return this;
    }

    public record Mode(String key, String label, char icon, int color) {
    }

    public Map<String, Mode> getModes() {
        return modes;
    }

    /** Looks up a gamemode by API key or label, case-insensitively. */
    public Mode getMode(String keyOrLabel) {
        if (keyOrLabel == null) return null;
        String k = keyOrLabel.toLowerCase();
        Mode m = modes.get(k);
        if (m != null) return m;
        for (Mode mode : modes.values()) {
            if (mode.label().equalsIgnoreCase(keyOrLabel)) return mode;
        }
        // Aliases so a gamemode picked on one list still maps sensibly on another.
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

    /**
     * Tier colors. MCTiers/SubTiers use TierTagger's palette; PvPTiers uses
     * the palette from the official PvPTiers "Tiers" mod.
     */
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
