package com.tierspoofer.model;

import com.tierspoofer.ColorCodeParser;
import com.tierspoofer.NameColor;

import java.util.UUID;

public class SpoofedPlayer {
    private UUID uuid;
    private String originalName;

    private String spoofedName;

    private String maceTier;
    private String crystalTier;
    private String displayTier;
    private String gamemode;

    private String tierList;

    private String nameColor;

    private String skinTargetName;

    private boolean useMaceTier = true;

    public static final String[] GAMEMODES = new String[]{
            "mace", "crystal", "sword", "vanilla", "axe", "pot", "uhc", "smp", "nethop"
    };
    public static final String[] TIERS = new String[]{
            "HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5", ""
    };

    public SpoofedPlayer() {
        this.gamemode = "vanilla";
    }

    public SpoofedPlayer(UUID uuid, String originalName) {
        this.uuid = uuid;
        this.originalName = originalName;
        this.gamemode = "vanilla";
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getSpoofedName() { return spoofedName; }

    public void setSpoofedName(String spoofedName) {
        this.spoofedName = spoofedName;
        if (spoofedName == null || spoofedName.isEmpty()) {
            this.skinTargetName = null;
        } else {
            this.skinTargetName = ColorCodeParser.stripCodes(spoofedName);
        }
    }

    public String getMaceTier() { return maceTier; }
    public void setMaceTier(String maceTier) {
        this.maceTier = maceTier;
        if (useMaceTier) this.displayTier = maceTier;
    }

    public String getCrystalTier() { return crystalTier; }
    public void setCrystalTier(String crystalTier) {
        this.crystalTier = crystalTier;
        if (!useMaceTier) this.displayTier = crystalTier;
    }

    public String getDisplayTier() {
        if (displayTier != null && !displayTier.isEmpty()) return displayTier;
        if (useMaceTier && maceTier != null) return maceTier;
        if (!useMaceTier && crystalTier != null) return crystalTier;
        return null;
    }
    public void setDisplayTier(String displayTier) { this.displayTier = displayTier; }

    public String getGamemode() { return gamemode != null ? gamemode : "vanilla"; }
    public void setGamemode(String gamemode) { this.gamemode = gamemode; }

    public TierList getTierList() { return TierList.byId(tierList); }
    public void setTierList(TierList tierList) { this.tierList = tierList == null ? null : tierList.id; }

    public String getNameColor() { return nameColor; }
    public void setNameColor(String nameColor) {
        this.nameColor = nameColor == null || nameColor.isBlank() ? null : nameColor.trim();
    }

    public boolean hasSpoofedName() { return spoofedName != null && !spoofedName.isEmpty(); }

    public boolean changesName() {
        return hasSpoofedName() || NameColor.parse(nameColor) != null;
    }

    public boolean isUseMaceTier() { return useMaceTier; }
    public void setUseMaceTier(boolean useMaceTier) {
        this.useMaceTier = useMaceTier;
        this.displayTier = useMaceTier ? maceTier : crystalTier;
    }

    public String getEffectiveName() {
        if (skinTargetName != null && !skinTargetName.isEmpty()) return skinTargetName;
        return originalName;
    }

    public String getSkinTargetName() { return skinTargetName; }
    public void setSkinTargetName(String skinTargetName) { this.skinTargetName = skinTargetName; }
}
