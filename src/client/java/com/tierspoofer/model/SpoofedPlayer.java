package com.tierspoofer.model;

import com.tierspoofer.ColorCodeParser;
import com.tierspoofer.NameColor;

import java.util.UUID;

public class SpoofedPlayer {
    private UUID uuid;
    private String originalName;
    private String spoofedName;
    private String skinTargetName; // spoofedName without & codes, used for the skin lookup
    private String displayTier;
    private String gamemode = "vanilla";
    private String tierList;
    private String nameColor;

    public SpoofedPlayer() {
    }

    public SpoofedPlayer(UUID uuid, String originalName) {
        this.uuid = uuid;
        this.originalName = originalName;
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

    public String getDisplayTier() {
        return displayTier == null || displayTier.isEmpty() ? null : displayTier;
    }

    public void setDisplayTier(String displayTier) {
        this.displayTier = displayTier;
    }

    public String getGamemode() {
        return gamemode != null ? gamemode : "vanilla";
    }

    public void setGamemode(String gamemode) {
        this.gamemode = gamemode;
    }

    public TierList getTierList() {
        return TierList.byId(tierList);
    }

    public void setTierList(TierList tierList) {
        this.tierList = tierList == null ? null : tierList.id;
    }

    public String getNameColor() {
        return nameColor;
    }

    public void setNameColor(String nameColor) {
        this.nameColor = nameColor == null || nameColor.isBlank() ? null : nameColor.trim();
    }

    /** True if the name itself looks different (fake name or custom color). */
    public boolean changesName() {
        return hasSpoofedName() || NameColor.parse(nameColor) != null;
    }
}
