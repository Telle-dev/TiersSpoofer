package com.tierspoofer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TierSpooferConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("tierspoofer/tierspoofer.json");
    }

    private boolean enabled = true;
    private boolean showInTabList = true;
    private boolean showInWorld = true;
    private boolean showIcons = true;
    private boolean autoFetchSkins = true;
    private boolean skinEnabled = true;
    private boolean capeEnabled = true;
    private boolean showPlayerList = false;
    private boolean showOwnNametag = true;

    private String realTierList = "off";
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
        } catch (Exception e) {
            TierSpoofer.LOGGER.error("Couldn't read {}, starting with a fresh config", configPath, e);
            try {
                Files.move(configPath, configPath.resolveSibling("tierspoofer.json.broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        }
        return new TierSpooferConfig();
    }

    private static void normalizeSkinTargets(TierSpooferConfig config) {
        if (config.spoofedPlayers == null) config.spoofedPlayers = new ArrayList<>();
        config.spoofedPlayers.removeIf(Objects::isNull);
        for (SpoofedPlayer player : config.spoofedPlayers) {
            // gson skips the setter, so rebuild the skin name
            player.setSpoofedName(player.getSpoofedName());
        }
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            if (Files.notExists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            Path tmp = configPath.resolveSibling("tierspoofer.json.tmp");
            Files.writeString(tmp, GSON.toJson(this));
            try {
                Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
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

    public boolean isShowOwnNametag() { return showOwnNametag; }
    public void setShowOwnNametag(boolean showOwnNametag) { this.showOwnNametag = showOwnNametag; }

    public boolean isShowPlayerList() { return showPlayerList; }
    public void setShowPlayerList(boolean showPlayerList) { this.showPlayerList = showPlayerList; }
}
