package com.tierspoofer.model;

import com.tierspoofer.ColorCodeParser;
import com.tierspoofer.NameColor;

import java.util.UUID;

public class SpoofedPlayer {
    private UUID uuid;
    private String originalName;

    /**
     * Raw user input for the fake name, exactly as typed in the config
     * screen's "Fake Name" field — may contain '&'-style formatting codes
     * (e.g. "&6K1&8RBE"). This is what's persisted to config and what's
     * shown back in the text field when editing an existing entry.
     *
     * IMPORTANT: never pass this field directly to skin/cape/profile/UUID
     * lookups. Use {@link #getSkinTargetName()} (always plain) for that.
     */
    private String spoofedName;

    private String maceTier;
    private String crystalTier;
    private String displayTier;
    private String gamemode;

    /**
     * Which tier list's icons/colors to render this entry with — a
     * {@link TierList#id} ("mctiers", "pvptiers", "subtiers"). Null in
     * configs saved before this existed, which is treated as MCTiers.
     */
    private String tierList;

    /**
     * Custom name color as typed in the config screen: "#RRGGBB", a gradient
     * like "#FF0000-#0000FF", or "rainbow". Null/empty = no custom color.
     * See {@link com.tierspoofer.NameColor}.
     */
    private String nameColor;

    /**
     * Plain, code-free username used for ALL skin/cape/profile/UUID
     * resolution (Mojang API lookups, SkinCache, tab-list name matching,
     * the chat/death-screen substring replace). Derived automatically from
     * {@link #spoofedName} via {@link ColorCodeParser#stripCodes(String)}
     * whenever the spoofed name is set — never set this independently to
     * something containing formatting codes.
     */
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

    /** Returns the raw, possibly '&'-coded fake name exactly as entered. */
    public String getSpoofedName() { return spoofedName; }

    /**
     * Sets the fake name from raw user input (may contain '&'-codes).
     * Automatically (re-)derives {@link #skinTargetName} as the stripped,
     * plain-text equivalent — callers never need to call
     * {@link #setSkinTargetName(String)} themselves after this.
     */
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

    /** True if this entry changes how the name itself looks (fake name and/or custom color). */
    public boolean changesName() {
        return hasSpoofedName() || NameColor.parse(nameColor) != null;
    }

    public boolean isUseMaceTier() { return useMaceTier; }
    public void setUseMaceTier(boolean useMaceTier) {
        this.useMaceTier = useMaceTier;
        this.displayTier = useMaceTier ? maceTier : crystalTier;
    }

    /**
     * Plain effective name for anywhere a plain Java String is needed
     * (e.g. as a fallback before formatting is considered). Prefer
     * {@link com.tierspoofer.TierSpoofer#getDisplayName} for actual
     * rendering, since that applies the parsed color/formatting.
     */
    public String getEffectiveName() {
        if (skinTargetName != null && !skinTargetName.isEmpty()) return skinTargetName;
        return originalName;
    }

    /**
     * Always returns the plain, code-free username — safe for skin/cape/
     * profile/UUID lookups. Kept as an explicit setter only for config
     * deserialization (GSON) and edge cases; prefer letting
     * {@link #setSpoofedName(String)} derive this automatically.
     */
    public String getSkinTargetName() { return skinTargetName; }
    public void setSkinTargetName(String skinTargetName) { this.skinTargetName = skinTargetName; }
}
