// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.model;

import com.tierspoofer.ColorCodeParser;
import com.tierspoofer.NameColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SpoofedPlayer {
    private UUID uuid;
    private String originalName;
    private String spoofedName;
    private String skinTargetName; // spoofedName without & codes, used for the skin lookup
    private String nameColor;
    // tier list id -> fake tier on that list
    private Map<String, FakeTier> tiers = new LinkedHashMap<>();

    // only read from configs saved before every list could have its own tier
    private String displayTier;
    private String gamemode;
    private String tierList;

    public static class FakeTier {
        private String tier;
        private String gamemode;

        public FakeTier(String tier, String gamemode) {
            this.tier = tier;
            this.gamemode = gamemode;
        }

        public String tier() {
            return tier;
        }

        public String gamemode() {
            return gamemode;
        }
    }

    public SpoofedPlayer() {
    }

    public SpoofedPlayer(UUID uuid, String originalName) {
        this.uuid = uuid;
        this.originalName = originalName;
    }

    // Moves the single tier of an old config into the per-list map.
    public void upgradeOldConfig() {
        if (tiers == null) tiers = new LinkedHashMap<>();
        tiers.values().removeIf(t -> t == null || t.tier == null || t.tier.isEmpty());
        if (displayTier != null && !displayTier.isEmpty() && tiers.isEmpty()) {
            tiers.put(TierList.byId(tierList).id, new FakeTier(displayTier, gamemode));
        }
        displayTier = null;
        gamemode = null;
        tierList = null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getSpoofedName() {
        return spoofedName;
    }

    public void setSpoofedName(String spoofedName) {
        this.spoofedName = spoofedName;
        this.skinTargetName = spoofedName == null || spoofedName.isEmpty() ? null : ColorCodeParser.stripCodes(spoofedName);
    }

    public boolean hasSpoofedName() {
        return spoofedName != null && !spoofedName.isEmpty();
    }

    public String getSkinTargetName() {
        return skinTargetName;
    }

    public FakeTier getTier(TierList list) {
        return tiers == null ? null : tiers.get(list.id);
    }

    public void setTier(TierList list, String tier, String gamemode) {
        if (tiers == null) tiers = new LinkedHashMap<>();
        if (tier == null || tier.isEmpty()) {
            tiers.remove(list.id);
        } else {
            tiers.put(list.id, new FakeTier(tier, gamemode));
        }
    }

    public boolean hasFakeTier() {
        return tiers != null && !tiers.isEmpty();
    }

    public String getNameColor() {
        return nameColor;
    }

    public void setNameColor(String nameColor) {
        this.nameColor = nameColor == null || nameColor.isBlank() ? null : nameColor.trim();
    }

    // True if the name itself looks different (fake name or custom color).
    public boolean changesName() {
        return hasSpoofedName() || NameColor.parse(nameColor) != null;
    }
}
