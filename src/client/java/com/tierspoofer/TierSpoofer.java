package com.tierspoofer;

import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.config.TierSpooferConfigScreen;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TierSpoofer implements ClientModInitializer {
    public static final String MOD_ID = "tierspoofer";
    public static final Logger LOGGER = LoggerFactory.getLogger("tierspoofer");

    private static TierSpooferConfig config = new TierSpooferConfig();
    private static final Map<UUID, SpoofedPlayer> spoofedPlayers = new ConcurrentHashMap<>();
    private static KeyBinding openConfigKey;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    public static final char ICON_AXE        = '\ue701';
    public static final char ICON_MACE       = '\ue702';
    public static final char ICON_NETHPOT    = '\ue703';
    public static final char ICON_POT        = '\ue704';
    public static final char ICON_SMP        = '\ue705';
    public static final char ICON_SWORD      = '\ue706';
    public static final char ICON_UHC        = '\ue707';
    public static final char ICON_VANILLA    = '\ue708';
    public static final char ICON_BED        = '\ue801';
    public static final char ICON_BOW        = '\ue802';
    public static final char ICON_CREEPER    = '\ue803';
    public static final char ICON_DEBUFF     = '\ue804';
    public static final char ICON_CRYSTAL    = '\ue805';
    public static final char ICON_DIA_SMP    = '\ue806';
    public static final char ICON_ELYTRA     = '\ue807';
    public static final char ICON_MANHUNT    = '\ue808';
    public static final char ICON_MINECART   = '\ue809';
    public static final char ICON_OG_VANILLA = '\ue810';
    public static final char ICON_SPEED      = '\ue811';
    public static final char ICON_TRIDENT    = '\ue812';
    public static final char ICON_DEFAULT    = '\u2022';

    @Override
    public void onInitializeClient() {
        LOGGER.info("[TierSpoofer] Loaded!");
        config = TierSpooferConfig.load();
        for (SpoofedPlayer player : config.getSpoofedPlayers()) {
            if (player.getUuid() == null) continue;
            spoofedPlayers.put(player.getUuid(), player);
        }

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tierspoofer.open_config",
                InputUtil.Type.KEYSYM,
                61, // '=' key
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                if (client.currentScreen != null) continue;
                client.setScreen(new TierSpooferConfigScreen(null));
            }
        });
    }

    public static TierSpooferConfig getConfig() {
        return config;
    }

    public static void saveConfig() {
        config.getSpoofedPlayers().clear();
        config.getSpoofedPlayers().addAll(spoofedPlayers.values());
        config.save();
    }

    public static Map<UUID, SpoofedPlayer> getSpoofedPlayers() {
        return spoofedPlayers;
    }

    public static void addSpoofedPlayer(SpoofedPlayer player) {
        if (player.getUuid() != null) {
            spoofedPlayers.put(player.getUuid(), player);
            saveConfig();
        }
    }

    public static void removeSpoofedPlayer(UUID uuid) {
        spoofedPlayers.remove(uuid);
        saveConfig();
    }

    public static SpoofedPlayer getSpoofedPlayer(UUID uuid) {
        return spoofedPlayers.get(uuid);
    }

    public static boolean isPlayerSpoofed(UUID uuid) {
        return spoofedPlayers.containsKey(uuid);
    }

    public static char getGamemodeIcon(TierList list, String gamemode) {
        TierList.Mode mode = list == null ? null : list.getMode(gamemode);
        return mode != null ? mode.icon() : getGamemodeIcon(gamemode);
    }

    public static char getGamemodeIcon(String gamemode) {
        if (gamemode == null) return ICON_DEFAULT;
        gamemode = gamemode.toLowerCase();
        switch (gamemode) {
            case "axe":         return ICON_AXE;
            case "mace":        return ICON_MACE;
            case "nethop":      return ICON_NETHPOT;
            case "neth_pot":    return ICON_NETHPOT;
            case "pot":         return ICON_POT;
            case "smp":         return ICON_SMP;
            case "sword":       return ICON_SWORD;
            case "uhc":         return ICON_UHC;
            case "vanilla":     return ICON_VANILLA;
            case "bed":         return ICON_BED;
            case "bow":         return ICON_BOW;
            case "creeper":     return ICON_CREEPER;
            case "debuff":      return ICON_DEBUFF;
            case "crystal":     return ICON_CRYSTAL;
            case "dia_crystal": return ICON_CRYSTAL;
            case "dia_smp":     return ICON_DIA_SMP;
            case "elytra":      return ICON_ELYTRA;
            case "manhunt":     return ICON_MANHUNT;
            case "minecart":    return ICON_MINECART;
            case "og_vanilla":  return ICON_OG_VANILLA;
            case "speed":       return ICON_SPEED;
            case "trident":     return ICON_TRIDENT;
            default:            return ICON_DEFAULT;
        }
    }

    public static int getGamemodeColor(String gamemode) {
        if (gamemode == null) return 0xFFFFFF;
        gamemode = gamemode.toLowerCase();
        switch (gamemode) {
            case "axe":         return 0x55FF55;
            case "mace":        return 0xAAAAAA;
            case "nethop":      return 0x7D4A40;
            case "neth_pot":    return 0x7D4A40;
            case "pot":         return 0xFF0000;
            case "smp":         return 0xECCB45;
            case "sword":       return 0xA4FDF0;
            case "uhc":         return 0xFF5555;
            case "vanilla":     return 0xFF55FF;
            case "bed":         return 0xFF0000;
            case "bow":         return 0x663D10;
            case "creeper":     return 0x55FF55;
            case "debuff":      return 0x555555;
            case "crystal":     return 0x55FFFF;
            case "dia_crystal": return 0x55FFFF;
            case "dia_smp":     return 0x8C668B;
            case "elytra":      return 0x8D8DB1;
            case "manhunt":     return 0xFF5555;
            case "minecart":    return 0xAAAAAA;
            case "og_vanilla":  return 0xFFAA00;
            case "speed":       return 0x43A9D1;
            case "trident":     return 0x579B8C;
            default:            return 0xFFFFFF;
        }
    }

    public static int getTierColor(String tier) {
        return TierList.MCTIERS.getTierColor(tier);
    }

    public static Text createTierText(String tier, String gamemode, boolean showIcon) {
        return createTierText(tier, TierList.MCTIERS, gamemode, showIcon);
    }

    public static Text createTierText(String tier, TierList list, String gamemode, boolean showIcon) {
        if (tier == null || tier.isEmpty()) {
            return Text.empty();
        }
        if (list == null) list = TierList.MCTIERS;
        int tierColor = list.getTierColor(tier);
        MutableText result = Text.empty().copy();
        if (showIcon && gamemode != null && !gamemode.isEmpty()) {
            char icon = getGamemodeIcon(list, gamemode);
            result.append(Text.literal(icon + " ").styled(s -> s.withColor(0xFFFFFF)));
        }
        result.append(Text.literal(tier).styled(s -> s.withColor(tierColor)));
        return result;
    }

    public static Text getDisplayName(UUID uuid, Text originalName) {
        return getDisplayName(uuid, null, originalName);
    }

    public static Text getDisplayName(UUID uuid, String username, Text originalName) {
        if (config == null || !config.isEnabled() || originalName == null) {
            return originalName;
        }
        SpoofedPlayer spoofed = findSpoofedPlayer(uuid, username);
        if (spoofed == null) {
            return getRealTierDisplayName(uuid, originalName);
        }

        Text nameToShow = originalName;
        if (spoofed.changesName()) {
            Text styled = buildStyledName(spoofed);
            String realName = username != null ? username : spoofed.getOriginalName();
            Text replaced = realName == null || realName.isEmpty()
                    ? originalName
                    : NameReplacer.replace(originalName, Map.of(realName, styled));
            // If the server shows a name that doesn't contain the username at all
            // (e.g. a nick plugin), replace the whole thing.
            nameToShow = replaced != originalName ? replaced : styled;
        }

        String tier = spoofed.getDisplayTier();
        if (tier == null || tier.isEmpty()) {
            return nameToShow;
        }
        MutableText result = Text.empty();
        result.append(createTierText(tier, spoofed.getTierList(), spoofed.getGamemode(), config.isShowIcons()));
        result.append(Text.literal(" | ").styled(s -> s.withColor(0xAAAAAA)));
        result.append(nameToShow);
        return result;
    }

    public static Text buildStyledName(SpoofedPlayer player) {
        Text base = player.hasSpoofedName()
                ? ColorCodeParser.parse(player.getSpoofedName())
                : Text.literal(player.getOriginalName() == null ? "" : player.getOriginalName());
        NameColor color = NameColor.parse(player.getNameColor());
        return color != null ? color.apply(base) : base;
    }

    public static SpoofedPlayer findSpoofedPlayer(UUID uuid, String username) {
        SpoofedPlayer byUuid = uuid == null ? null : spoofedPlayers.get(uuid);
        if (byUuid != null || username == null || username.isEmpty()) {
            return byUuid;
        }
        for (SpoofedPlayer player : spoofedPlayers.values()) {
            if (!username.equalsIgnoreCase(player.getOriginalName())) continue;
            if (uuid != null && uuid.version() == 4 && player.getUuid() != null
                    && player.getUuid().version() != 4 && !spoofedPlayers.containsKey(uuid)) {
                // Placeholder (offline-style) UUID: adopt the real one.
                spoofedPlayers.remove(player.getUuid());
                player.setUuid(uuid);
                spoofedPlayers.put(uuid, player);
                saveConfig();
            }
            return player;
        }
        return null;
    }

    private static Text getRealTierDisplayName(UUID uuid, Text originalName) {
        TierList list = config.getRealTierList();
        if (list == null || originalName == null) {
            return originalName;
        }
        RealTierCache.RealTier real = RealTierCache.get(uuid, list, config.getRealTierMode());
        if (real == null) {
            return originalName;
        }
        MutableText result = Text.empty();
        result.append(createTierText(real.tier(), list, real.gamemode(), config.isShowIcons()));
        result.append(Text.literal(" | ").styled(s -> s.withColor(0xAAAAAA)));
        result.append(originalName);
        return result;
    }

    public static Text replaceNamesInText(Text originalText) {
        if (config == null || !config.isEnabled() || originalText == null) {
            return originalText;
        }
        try {
            Map<String, Text> replacements = new HashMap<>();
            for (SpoofedPlayer player : spoofedPlayers.values()) {
                String original = player.getOriginalName();
                if (original == null || original.isEmpty() || !player.changesName()) continue;
                replacements.put(original, buildStyledName(player));
            }
            return NameReplacer.replace(originalText, replacements);
        } catch (Exception e) {
            LOGGER.debug("[TierSpoofer] Failed to replace names in text", e);
            return originalText;
        }
    }
}
