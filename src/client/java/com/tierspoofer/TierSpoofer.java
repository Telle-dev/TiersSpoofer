package com.tierspoofer;

import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.config.TierSpooferConfigScreen;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TierSpoofer implements ClientModInitializer {
    public static final String MOD_ID = "tierspoofer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<UUID, SpoofedPlayer> spoofedPlayers = new ConcurrentHashMap<>();
    private static TierSpooferConfig config = new TierSpooferConfig();
    private static KeyBinding openConfigKey;
    private static volatile int changeCount;
    private static boolean cmdsHintShown;

    @Override
    public void onInitializeClient() {
        config = TierSpooferConfig.load();
        for (SpoofedPlayer player : config.getSpoofedPlayers()) {
            if (player.getUuid() != null) spoofedPlayers.put(player.getUuid(), player);
        }

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tierspoofer.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                "key.categories.tierspoofer"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new TierSpooferConfigScreen(null));
            }
        });
    }

    public static TierSpooferConfig getConfig() {
        return config;
    }

    /** Bumped on every save, so cached text (holograms) knows to refresh. */
    public static int getChangeCount() {
        return changeCount;
    }

    public static void saveConfig() {
        changeCount++;
        config.getSpoofedPlayers().clear();
        config.getSpoofedPlayers().addAll(spoofedPlayers.values());
        config.save();
    }

    public static Map<UUID, SpoofedPlayer> getSpoofedPlayers() {
        return spoofedPlayers;
    }

    public static void addSpoofedPlayer(SpoofedPlayer player) {
        if (player.getUuid() == null) return;
        spoofedPlayers.put(player.getUuid(), player);
        saveConfig();
    }

    public static void removeSpoofedPlayer(UUID uuid) {
        spoofedPlayers.remove(uuid);
        saveConfig();
    }

    public static SpoofedPlayer getSpoofedPlayer(UUID uuid) {
        return spoofedPlayers.get(uuid);
    }

    /**
     * Finds an entry by UUID, or by name as a fallback. Entries added while the
     * player was offline have a made-up UUID, which gets swapped for the real
     * one the first time we see them.
     */
    public static SpoofedPlayer findSpoofedPlayer(UUID uuid, String username) {
        SpoofedPlayer byUuid = uuid == null ? null : spoofedPlayers.get(uuid);
        if (byUuid != null) {
            // they changed their name since they were added, text still shows the new one
            if (username != null && !username.isEmpty() && !username.equals(byUuid.getOriginalName())) {
                byUuid.setOriginalName(username);
                saveConfig();
            }
            return byUuid;
        }
        if (username == null || username.isEmpty()) return null;
        for (SpoofedPlayer player : spoofedPlayers.values()) {
            if (!username.equalsIgnoreCase(player.getOriginalName())) continue;
            if (uuid != null && uuid.version() == 4 && player.getUuid() != null
                    && player.getUuid().version() != 4 && !spoofedPlayers.containsKey(uuid)) {
                spoofedPlayers.remove(player.getUuid());
                player.setUuid(uuid);
                spoofedPlayers.put(uuid, player);
                saveConfig();
            }
            return player;
        }
        return null;
    }

    public static Text createTierText(String tier, TierList list, String gamemode, boolean showIcon) {
        if (tier == null || tier.isEmpty()) return Text.empty();
        if (list == null) list = TierList.MCTIERS;

        MutableText result = Text.empty();
        if (showIcon && gamemode != null && !gamemode.isEmpty()) {
            result.append(Text.literal(TierList.iconFor(list, gamemode) + " ").styled(s -> s.withColor(0xFFFFFF)));
        }
        int color = list.getTierColor(tier);
        result.append(Text.literal(tier).styled(s -> s.withColor(color)));
        return result;
    }

    /** "[icon] HT1 | name" */
    private static Text withTierTag(String tier, TierList list, String gamemode, Text name) {
        MutableText result = Text.empty();
        result.append(createTierText(tier, list, gamemode, config.isShowIcons()));
        result.append(Text.literal(" | ").styled(s -> s.withColor(0xAAAAAA)));
        result.append(name);
        return result;
    }

    /** The spoofed player's name as it should look: fake name (with & codes) or real name, plus their color. */
    public static Text buildStyledName(SpoofedPlayer player) {
        Text base = player.hasSpoofedName()
                ? ColorCodeParser.parse(player.getSpoofedName())
                : Text.literal(player.getOriginalName() == null ? "" : player.getOriginalName());
        NameColor color = NameColor.parse(player.getNameColor());
        return color != null ? color.apply(base) : base;
    }

    // Name above the head. Only the username part is swapped, ranks stay.
    public static Text getDisplayName(UUID uuid, String username, Text originalName) {
        return getDisplayName(uuid, username, originalName, true, true);
    }

    // Tab list: fake tier only for the people you added (if Tab is on), no real tiers.
    public static Text getTabName(UUID uuid, String username, Text originalName) {
        return getDisplayName(uuid, username, originalName, config.isShowInTabList(), false);
    }

    private static Text getDisplayName(UUID uuid, String username, Text originalName, boolean showTier, boolean showReal) {
        if (config == null || !config.isEnabled() || originalName == null) return originalName;

        SpoofedPlayer spoofed = findSpoofedPlayer(uuid, username);
        if (spoofed == null) return showReal ? getRealTierDisplayName(uuid, username, originalName) : originalName;

        String realName = username != null ? username : spoofed.getOriginalName();
        String tier = showTier ? spoofed.getDisplayTier() : null;
        // drop the real tier other tier mods put on this name, ours replaces it
        if (tier != null) originalName = NameReplacer.stripTierTags(originalName, realName);

        Text name = originalName;
        if (spoofed.changesName()) {
            Text styled = buildStyledName(spoofed);
            Text replaced = realName == null || realName.isEmpty()
                    ? originalName
                    : NameReplacer.replace(originalName, Map.of(realName, styled));
            // no username in there at all (nick plugins etc), so just show the styled name
            name = replaced != originalName ? replaced : styled;
        }

        return tier == null ? name : withTierTag(tier, spoofed.getTierList(), spoofed.getGamemode(), name);
    }

    private static Text getRealTierDisplayName(UUID uuid, String username, Text originalName) {
        TierList list = config.getRealTierList();
        if (list == null) return originalName;
        RealTierCache.RealTier real = RealTierCache.get(uuid, list, config.getRealTierMode());
        if (real == null) return originalName;
        Text name = username == null ? originalName : NameReplacer.stripTierTags(originalName, username);
        return withTierTag(real.tier(), list, real.gamemode(), name);
    }

    /**
     * Turns fake names back into real ones in a command before it's sent, so
     * "/tpa k1rbe" reaches the server as "/tpa Steve".
     */
    public static String toRealNames(String command) {
        if (config == null || !config.isEnabled() || command == null) return command;
        String real = swapFakeNames(command);
        if (config.isCommandNames()) return real;
        if (!real.equals(command) && !cmdsHintShown) {
            // easy to miss that Cmds is off, so say it once instead of the command just failing
            cmdsHintShown = true;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.execute(() -> client.inGameHud.getChatHud().addMessage(
                    Text.literal("[TierSpoofer] Turn on \"Cmds\" in the menu (=) to use fake names in commands.")
                            .formatted(Formatting.YELLOW)));
        }
        return command;
    }

    private static String swapFakeNames(String command) {
        Map<String, String> realByFake = new HashMap<>();
        for (SpoofedPlayer player : spoofedPlayers.values()) {
            String fake = player.getSkinTargetName();
            String real = player.getOriginalName();
            if (fake != null && !fake.isEmpty() && real != null && !fake.equalsIgnoreCase(real)) {
                realByFake.put(fake.toLowerCase(), real);
            }
        }
        if (realByFake.isEmpty()) return command;

        // one pass, so swaps can't chain into each other
        StringBuilder names = new StringBuilder();
        for (String fake : realByFake.keySet()) {
            if (names.length() > 0) names.append('|');
            names.append(Pattern.quote(fake));
        }
        Matcher m = Pattern.compile("(?i)(?<![A-Za-z0-9_])(" + names + ")(?![A-Za-z0-9_])").matcher(command);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(realByFake.get(m.group(1).toLowerCase())));
        }
        m.appendTail(out);
        return out.toString();
    }

    public static Text replaceNamesInText(Text text) {
        return replaceNamesInText(text, false);
    }

    /**
     * For shared classes (scoreboard, teams) that a singleplayer server also uses:
     * only swap on the client thread, so the fake name never ends up in the world save.
     */
    public static Text replaceNamesOnClient(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isOnThread()) return text;
        return replaceNamesInText(text, false);
    }

    /**
     * Swaps spoofed players' names anywhere in a text (chat, death messages,
     * holograms). withTier also puts the tier in front, for server-made nametags.
     */
    public static Text replaceNamesInText(Text text, boolean withTier) {
        if (config == null || !config.isEnabled() || text == null) return text;
        try {
            Map<String, Text> replacements = new HashMap<>();
            for (SpoofedPlayer player : spoofedPlayers.values()) {
                String original = player.getOriginalName();
                if (original == null || original.isEmpty()) continue;
                String tier = withTier ? player.getDisplayTier() : null;
                if (!player.changesName() && tier == null) continue;

                Text name = player.changesName() ? buildStyledName(player) : Text.literal(original);
                if (tier != null) {
                    text = NameReplacer.stripTierTags(text, original);
                    name = withTierTag(tier, player.getTierList(), player.getGamemode(), name);
                }
                replacements.put(original, name);
            }
            return NameReplacer.replace(text, replacements);
        } catch (Exception e) {
            LOGGER.debug("Failed to replace names in text", e);
            return text;
        }
    }
}
