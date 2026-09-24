package com.tierspoofer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TierSpooferConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("tierspoofer/tierspoofer.json");
    }

    private boolean enabled = false;
    private boolean showInTabList = true;
    private boolean showInWorld = true;
    private boolean showIcons = true;
    private boolean autoFetchSkins = true;
    private boolean skinEnabled = true;
    private boolean capeEnabled = true;
    private boolean showPlayerList = false;

    /**
     * TierTagger-style real tiers: tier list id ("mctiers", "pvptiers",
     * "subtiers") to look up every non-spoofed player's real tier from, or
     * "off". Spoofed entries always take priority over real tiers.
     */
    private String realTierList = "off";
    /** Gamemode key to show for real tiers, or "highest" for each player's best one. */
    private String realTierMode = "highest";
    private List<SpoofedPlayer> spoofedPlayers = new ArrayList<>();

    public static TierSpooferConfig load() {
        Path configPath = getConfigPath();
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                TierSpooferConfig config = GSON.fromJson(json, TierSpooferConfig.class);
                if (config != null) {
                    normalizeSkinTargets(config);
                    return config;
                }
            }
        } catch (IOException e) {
            TierSpoofer.LOGGER.error("Failed to load config", e);
        }
        return new TierSpooferConfig();
    }

    /**
     * GSON sets fields directly via reflection, bypassing
     * {@code SpoofedPlayer.setSpoofedName}'s automatic derivation of the
     * plain {@code skinTargetName}. This re-derives it for every loaded
     * entry so a hand-edited or older config file (or any future config
     * migration) can never end up with a {@code skinTargetName} that still
     * contains '&'-codes — skin/cape/profile lookups must always see a
     * plain username.
     */
    private static void normalizeSkinTargets(TierSpooferConfig config) {
        if (config.spoofedPlayers == null) return;
        for (SpoofedPlayer player : config.spoofedPlayers) {
            if (player == null) continue;
            // Re-invoking the setter with the already-persisted raw value
            // re-runs the strip-and-derive logic unconditionally.
            player.setSpoofedName(player.getSpoofedName());
        }
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            if (Files.notExists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            TierSpoofer.LOGGER.error("Failed to save config", e);
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isShowInTabList() { return showInTabList; }
    public void setShowInTabList(boolean showInTabList) { this.showInTabList = showInTabList; }

    public boolean isShowInWorld() { return showInWorld; }
    public void setShowInWorld(boolean showInWorld) { this.showInWorld = showInWorld; }

    public boolean isShowIcons() { return showIcons; }
    public void setShowIcons(boolean showIcons) { this.showIcons = showIcons; }

    public boolean isAutoFetchSkins() { return autoFetchSkins; }
    public void setAutoFetchSkins(boolean autoFetchSkins) { this.autoFetchSkins = autoFetchSkins; }

    /** The tier list to fetch real tiers from, or null when real tiers are off. */
    public TierList getRealTierList() {
        if (realTierList == null || realTierList.equalsIgnoreCase("off")) return null;
        return TierList.byId(realTierList);
    }
    public void setRealTierList(TierList list) { this.realTierList = list == null ? "off" : list.id; }

    public String getRealTierMode() { return realTierMode == null ? "highest" : realTierMode; }
    public void setRealTierMode(String realTierMode) { this.realTierMode = realTierMode; }

    public List<SpoofedPlayer> getSpoofedPlayers() { return spoofedPlayers; }
    public void setSpoofedPlayers(List<SpoofedPlayer> spoofedPlayers) { this.spoofedPlayers = spoofedPlayers; }

    public boolean isSkinEnabled() { return skinEnabled; }
    public void setSkinEnabled(boolean skinEnabled) { this.skinEnabled = skinEnabled; }

    public boolean isCapeEnabled() { return capeEnabled; }
    public void setCapeEnabled(boolean capeEnabled) { this.capeEnabled = capeEnabled; }

    public boolean isShowPlayerList() { return showPlayerList; }
    public void setShowPlayerList(boolean showPlayerList) { this.showPlayerList = showPlayerList; }
}