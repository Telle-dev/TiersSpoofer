package com.tierspoofer;

import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.config.TierSpooferConfigScreen;
import com.tierspoofer.model.SpoofedPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TierSpoofer implements ClientModInitializer {
    public static final String MOD_ID = "tierspoofer";
    public static final Logger LOGGER = LoggerFactory.getLogger("tierspoofer");

    private static TierSpooferConfig config = new TierSpooferConfig();
    private static final Map<UUID, SpoofedPlayer> spoofedPlayers = new ConcurrentHashMap<>();
    private static KeyBinding openConfigKey;

    // --- 1.21.11 CHANGE ---------------------------------------------------
    // KeyBinding.Category replaces the plain-String category parameter
    // (breaking change introduced in 1.21.9, still in effect in 1.21.11).
    // Create exactly once, store in a static field, pass the Category
    // object (not a String) into the KeyBinding constructor below.
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
    // ------------------------------------------------------------------------

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

        // --- 1.21.11 CHANGE: pass CATEGORY (KeyBinding.Category), not a
        // translation-key String, as the 4th constructor argument. ----------
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tierspoofer.open_config",
                InputUtil.Type.KEYSYM,
                61, // '=' key
                CATEGORY
        ));
        // ------------------------------------------------------------------

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
        if (tier == null) return 0xFFFFFF;
        tier = tier.toUpperCase();
        switch (tier) {
            case "HT1":  return 0xE8BA3A;
            case "LT1":  return 0xD5B355;
            case "HT2":  return 0xC4D3E7;
            case "LT2":  return 0xA0A7B2;
            case "HT3":  return 0xF89F5A;
            case "LT3":  return 0xC67B42;
            case "HT4":  return 0x81749A;
            case "LT4":  return 0x655B79;
            case "HT5":  return 0x8F82A8;
            case "LT5":  return 0x655B79;
            case "RHT1": return 0xB0944D;
            case "RLT1": return 0xA8956E;
            case "RHT2": return 0x9AA5B5;
            case "RLT2": return 0x7E8690;
            case "RHT3": return 0xC08560;
            case "RLT3": return 0x9A6850;
            case "RHT4": return 0x6A6280;
            case "RLT4": return 0x524B63;
            case "RHT5": return 0x746A88;
            case "RLT5": return 0x524B63;
            default:     return 0xFFFFFF;
        }
    }

    public static Text createTierText(String tier, String gamemode, boolean showIcon) {
        if (tier == null || tier.isEmpty()) {
            return Text.empty();
        }
        int tierColor = getTierColor(tier);
        MutableText result = Text.empty().copy();
        if (showIcon && gamemode != null && !gamemode.isEmpty()) {
            char icon = getGamemodeIcon(gamemode);
            result.append(Text.literal(String.valueOf(icon)).styled(s -> s.withColor(0xFFFFFF)));
        }
        result.append(Text.literal(tier).styled(s -> s.withColor(tierColor)));
        return result;
    }

    public static Text getDisplayName(UUID uuid, Text originalName) {
        if (config == null || !config.isEnabled()) {
            return originalName;
        }
        SpoofedPlayer spoofed = spoofedPlayers.get(uuid);
        if (spoofed == null) {
            return originalName;
        }
        String tier = spoofed.getDisplayTier();
        String gamemode = spoofed.getGamemode();
        boolean hasSpoofedName = spoofed.getSpoofedName() != null && !spoofed.getSpoofedName().isEmpty();

        if (tier == null || tier.isEmpty()) {
            if (hasSpoofedName) {
                // ColorCodeParser.parse turns '&'-coded input into a styled
                // Text; plain names without codes come out as plain Text
                // (backward compatible with names that never used '&').
                return ColorCodeParser.parse(spoofed.getSpoofedName());
            }
            return originalName;
        }

        Text nameToShow;
        if (hasSpoofedName) {
            nameToShow = ColorCodeParser.parse(spoofed.getSpoofedName());
        } else {
            String originalString = originalName.getString();
            nameToShow = Text.literal(stripExistingTier(originalString));
        }

        MutableText result = Text.empty().copy();
        boolean showIcon = config.isShowIcons();
        result.append(createTierText(tier, gamemode, showIcon));
        result.append(Text.literal(" | ").styled(s -> s.withColor(0xAAAAAA)));
        result.append(nameToShow);
        return result;
    }

    private static String stripExistingTier(String name) {
        if (name == null) return "";
        int sepIndex = name.indexOf(" | ");
        if (sepIndex >= 0 && sepIndex + 3 < name.length()) {
            return name.substring(sepIndex + 3);
        }
        sepIndex = name.indexOf("|");
        if (sepIndex >= 0 && sepIndex + 1 < name.length()) {
            return name.substring(sepIndex + 1).trim();
        }
        return name;
    }

    public static Text replaceNamesInText(Text originalText) {
        if (config == null || !config.isEnabled() || originalText == null) {
            return originalText;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null) {
                return originalText;
            }
            DynamicRegistryManager registry = client.world.getRegistryManager();
            String originalJson = Text.Serialization.toJsonString(originalText, registry);
            String json = originalJson;
            boolean changed = false;
            for (SpoofedPlayer player : spoofedPlayers.values()) {
                if (player.getSpoofedName() == null || player.getSpoofedName().isEmpty()) continue;
                String original = player.getOriginalName();
                if (original == null || original.isEmpty()) continue;
                String quotedOriginal = "\"" + jsonEscape(original) + "\"";
                if (!json.contains(quotedOriginal)) continue;

                // Build the colored replacement as its own little Text tree,
                // then re-encode just that fragment to a JSON array so the
                // formatting (color/bold/etc.) survives the splice instead
                // of being flattened into a plain string. This keeps the
                // original mod's "raw JSON substring replace" approach (which
                // is robust against arbitrary message shapes) while still
                // rendering the spoofed name's '&'-codes as real colors.
                Text colored = ColorCodeParser.parse(player.getSpoofedName());
                String coloredJson = Text.Serialization.toJsonString(colored, registry);
                json = json.replace(quotedOriginal, coloredJson);
                changed = true;
            }
            if (!changed) {
                return originalText;
            }
            try {
                return Text.Serialization.fromJson(json, registry);
            } catch (Exception malformed) {
                // The colored splice produced invalid JSON for this
                // particular message shape (e.g. the name also matched a
                // non-text-value position). Fall back to the original
                // mod's plain, uncolored substring replace so the message
                // still gets renamed correctly even if not colored.
                String plainJson = originalJson;
                for (SpoofedPlayer player : spoofedPlayers.values()) {
                    if (player.getSpoofedName() == null || player.getSpoofedName().isEmpty()) continue;
                    String original = player.getOriginalName();
                    String plain = ColorCodeParser.stripCodes(player.getSpoofedName());
                    if (original == null || original.isEmpty() || !plainJson.contains(original)) continue;
                    plainJson = plainJson.replace(original, plain);
                }
                return Text.Serialization.fromJson(plainJson, registry);
            }
        } catch (Exception ignored) {
            // matches original mod's blunt try/catch-and-ignore behaviour
        }
        return originalText;
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}