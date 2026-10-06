// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TagSide;
import com.tierspoofer.model.TierList;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private boolean skinEnabled = true;
    private boolean capeEnabled = true;
    private boolean showPlayerList = false;
    private boolean showOwnNametag = true;
    private boolean commandNames = false;
    private boolean spoofForMods = true;

    private boolean realTiers = false;
    private Map<String, TagSide> tagSides = new LinkedHashMap<>();

    private String realTierList;
    private List<SpoofedPlayer> spoofedPlayers = new ArrayList<>();

    public static TierSpooferConfig load() {
        Path configPath = getConfigPath();
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                TierSpooferConfig config = GSON.fromJson(json, TierSpooferConfig.class);
                if (config != null) {
                    config.cleanUp();
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

    private void cleanUp() {
        if (spoofedPlayers == null) spoofedPlayers = new ArrayList<>();
        spoofedPlayers.removeIf(Objects::isNull);
        for (SpoofedPlayer player : spoofedPlayers) {
            player.setSpoofedName(player.getSpoofedName());
            player.upgradeOldConfig();
        }
        if (tagSides == null) tagSides = new LinkedHashMap<>();
        tagSides.values().removeIf(Objects::isNull);
        if (realTierList != null && !realTierList.equalsIgnoreCase("off")) realTiers = true;
        realTierList = null;
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

    public boolean isShowIcons() { return showIcons; }
    public void setShowIcons(boolean showIcons) { this.showIcons = showIcons; }

    public boolean isRealTiers() { return realTiers; }
    public void setRealTiers(boolean realTiers) { this.realTiers = realTiers; }

    public TagSide getSide(TierList list) { return tagSides.getOrDefault(list.id, TagSide.LEFT); }
    public void setSide(TierList list, TagSide side) { tagSides.put(list.id, side); }

    public List<SpoofedPlayer> getSpoofedPlayers() { return spoofedPlayers; }

    public boolean isSkinEnabled() { return skinEnabled; }
    public void setSkinEnabled(boolean skinEnabled) { this.skinEnabled = skinEnabled; }

    public boolean isCapeEnabled() { return capeEnabled; }
    public void setCapeEnabled(boolean capeEnabled) { this.capeEnabled = capeEnabled; }

    public boolean isShowOwnNametag() { return showOwnNametag; }
    public void setShowOwnNametag(boolean showOwnNametag) { this.showOwnNametag = showOwnNametag; }

    public boolean isCommandNames() { return commandNames; }
    public void setCommandNames(boolean commandNames) { this.commandNames = commandNames; }

    public boolean isSpoofForMods() { return spoofForMods; }
    public void setSpoofForMods(boolean spoofForMods) { this.spoofForMods = spoofForMods; }

    public boolean isShowPlayerList() { return showPlayerList; }
    public void setShowPlayerList(boolean showPlayerList) { this.showPlayerList = showPlayerList; }
}
