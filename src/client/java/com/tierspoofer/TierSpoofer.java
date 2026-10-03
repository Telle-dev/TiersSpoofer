// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer;

import com.mojang.authlib.GameProfile;
import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.config.TierSpooferConfigScreen;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TagSide;
import com.tierspoofer.model.TierList;
import com.tierspoofer.mixin.PlayerListEntryAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
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
    private static final Map<UUID, GameProfile> fakeProfiles = new ConcurrentHashMap<>();
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
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))
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

    // Bumped on every save, so cached text (holograms) knows to refresh.
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

    // Finds an entry by UUID, or by name as a fallback. Entries added while the player was offline
    // have a made-up UUID, which gets swapped for the real one the first time we see them.
    public static SpoofedPlayer findSpoofedPlayer(UUID uuid, String username) {
        SpoofedPlayer byUuid = uuid == null ? null : spoofedPlayers.get(uuid);
        if (byUuid != null) {
            // they changed their name since they were added, text still shows the new one
            if (username != null && !username.isEmpty() && !username.equals(byUuid.getOriginalName())
                    && !username.equalsIgnoreCase(byUuid.getSkinTargetName())) {
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

    // The tab list profile with the fake name in it. Other mods read names from there by UUID.
    public static GameProfile spoofProfile(GameProfile real) {
        if (real == null || config == null || !config.isEnabled() || !config.isSpoofForMods()) return real;
        SpoofedPlayer player = spoofedPlayers.get(real.id());
        String fake = player == null ? null : player.getSkinTargetName();
        if (fake == null || fake.isEmpty() || fake.equals(real.name())) return real;

        GameProfile cached = fakeProfiles.get(real.id());
        if (cached != null && cached.name().equals(fake) && cached.properties() == real.properties()) return cached;
        GameProfile spoofed = new GameProfile(real.id(), fake, real.properties());
        fakeProfiles.put(real.id(), spoofed);
        return spoofed;
    }

    public static GameProfile realProfile(PlayerListEntry entry) {
        return ((PlayerListEntryAccessor) entry).tierspoofer$getRealProfile();
    }

    // Name of a player entity as other mods read it (player.getName()).
    public static Text spoofEntityName(UUID uuid, Text name) {
        if (config == null || !config.isEnabled() || !config.isSpoofForMods() || name == null) return name;
        SpoofedPlayer player = spoofedPlayers.get(uuid);
        String fake = player == null ? null : player.getSkinTargetName();
        return fake == null || fake.isEmpty() ? name : Text.literal(fake);
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

    // Puts each list's tag on its side of the name: "HT1 LT2 | name | HT3".
    private static Text withTags(Map<TierList, Text> tags, Text name) {
        MutableText left = Text.empty();
        MutableText right = Text.empty();
        boolean anyLeft = false;
        boolean anyRight = false;
        for (Map.Entry<TierList, Text> tag : tags.entrySet()) {
            TagSide side = config.getSide(tag.getKey());
            if (side == TagSide.LEFT) {
                if (anyLeft) left.append(" ");
                left.append(tag.getValue());
                anyLeft = true;
            } else if (side == TagSide.RIGHT) {
                if (anyRight) right.append(" ");
                right.append(tag.getValue());
                anyRight = true;
            }
        }
        if (!anyLeft && !anyRight) return name;

        MutableText result = Text.empty();
        if (anyLeft) result.append(left).append(separator());
        result.append(name);
        if (anyRight) result.append(separator()).append(right);
        return result;
    }

    private static Text separator() {
        return Text.literal(" | ").styled(s -> s.withColor(0xAAAAAA));
    }

    private static Map<TierList, Text> fakeTags(SpoofedPlayer player) {
        Map<TierList, Text> tags = new EnumMap<>(TierList.class);
        for (TierList list : TierList.values()) {
            SpoofedPlayer.FakeTier fake = player.getTier(list);
            if (fake != null) tags.put(list, createTierText(fake.tier(), list, fake.gamemode(), config.isShowIcons()));
        }
        return tags;
    }

    private static Map<TierList, Text> realTags(UUID uuid) {
        Map<TierList, Text> tags = new EnumMap<>(TierList.class);
        for (TierList list : TierList.values()) {
            if (config.getSide(list) == TagSide.OFF) continue; // don't look up lists nobody sees
            RealTierCache.RealTier real = RealTierCache.get(uuid, list);
            if (real != null) tags.put(list, createTierText(real.tier(), list, real.gamemode(), config.isShowIcons()));
        }
        return tags;
    }

    // A spoofed player's name with their fake tags, the way the nametag shows it.
    public static Text withFakeTags(SpoofedPlayer player, Text name) {
        return withTags(fakeTags(player), name);
    }

    // The spoofed player's name as it should look: fake name (with & codes) or real name, plus
    // their color.
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

    // Tab list: fake tags only for the people you added (if Tab is on), never real ones.
    public static Text getTabName(UUID uuid, String username, Text originalName) {
        return getDisplayName(uuid, username, originalName, config.isShowInTabList(), false);
    }

    private static Text getDisplayName(UUID uuid, String username, Text originalName, boolean showTier, boolean showReal) {
        if (config == null || !config.isEnabled() || originalName == null) return originalName;
        try {
            return spoofName(uuid, username, originalName, showTier, showReal);
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to spoof name of {}", username, e);
            return originalName;
        }
    }

    private static Text spoofName(UUID uuid, String username, Text originalName, boolean showTier, boolean showReal) {

        SpoofedPlayer spoofed = findSpoofedPlayer(uuid, username);
        if (spoofed == null) {
            if (showReal) return getRealTierDisplayName(uuid, username, originalName);
            // tab plugins often fill the list with placeholder entries (own UUID, names like " 01")
            // and put the player's name only in the text, so swap whatever names are in there
            return replaceNamesInText(originalName, showTier);
        }

        String realName = username != null ? username : spoofed.getOriginalName();
        // with Mods on the text can already hold the plain fake name (from player.getName())
        String fakeName = config.isSpoofForMods() ? spoofed.getSkinTargetName() : null;
        if (fakeName != null && (fakeName.isEmpty() || fakeName.equalsIgnoreCase(realName))) fakeName = null;

        // drop the real tags other tier mods put on this name, the fake ones replace them
        if (spoofed.hasFakeTier()) {
            originalName = NameReplacer.stripTierTags(originalName, realName);
            if (fakeName != null) originalName = NameReplacer.stripTierTags(originalName, fakeName);
        }

        Text name = originalName;
        if (spoofed.changesName()) {
            Text styled = buildStyledName(spoofed);
            Map<String, Text> swaps = new HashMap<>();
            if (realName != null && !realName.isEmpty()) swaps.put(realName, styled);
            if (fakeName != null) swaps.put(fakeName, styled);
            Text replaced = swaps.isEmpty() ? originalName : NameReplacer.replace(originalName, swaps);
            // no username in there at all (nick plugins etc), so show the styled name in the server's color
            name = replaced != originalName ? replaced
                    : Text.empty().setStyle(NameReplacer.colorAtEnd(originalName)).append(styled);
        }

        return showTier ? withFakeTags(spoofed, name) : name;
    }

    private static Text getRealTierDisplayName(UUID uuid, String username, Text originalName) {
        if (!config.isRealTiers()) return originalName;
        Map<TierList, Text> tags = realTags(uuid);
        if (tags.isEmpty()) return originalName;
        Text name = username == null ? originalName : NameReplacer.stripTierTags(originalName, username);
        return withTags(tags, name);
    }

    // Turns fake names back into real ones in a command before it's sent, so "/tpa k1rbe" reaches
    // the server as "/tpa Steve".
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

    // Hovering a name in chat shows that player's name and UUID. Point both at the fake account
    // (its UUID is known once its skin was looked up).
    private static Style spoofHover(Style style) {
        HoverEvent hover = style.getHoverEvent();
        if (hover instanceof HoverEvent.ShowEntity show) {
            HoverEvent.EntityContent entity = show.entity();
            SpoofedPlayer player = getSpoofedPlayer(entity.uuid);
            if (player == null || !player.changesName()) return style;
            UUID fakeUuid = player.getSkinTargetName() == null ? null : SkinCache.getUuidForUsername(player.getSkinTargetName());
            return style.withHoverEvent(new HoverEvent.ShowEntity(new HoverEvent.EntityContent(
                    entity.entityType, fakeUuid != null ? fakeUuid : entity.uuid, buildStyledName(player))));
        }
        if (hover instanceof HoverEvent.ShowText show) {
            Text swapped = replaceNamesInText(show.value(), false);
            return swapped == show.value() ? style : style.withHoverEvent(new HoverEvent.ShowText(swapped));
        }
        return style;
    }

    public static Text replaceNamesInText(Text text) {
        return replaceNamesInText(text, false);
    }

    // For shared classes (scoreboard, teams) that a singleplayer server also uses: only swap on the
    // client thread, so the fake name never ends up in the world save.
    public static Text replaceNamesOnClient(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isOnThread()) return text;
        return replaceNamesInText(text, false);
    }

    // Swaps spoofed players' names anywhere in a text (chat, death messages, holograms). withTier
    // also adds their fake tags, for nametags the server draws itself.
    public static Text replaceNamesInText(Text text, boolean withTier) {
        if (config == null || !config.isEnabled() || text == null) return text;
        try {
            Map<String, Text> replacements = new HashMap<>();
            for (SpoofedPlayer player : spoofedPlayers.values()) {
                String original = player.getOriginalName();
                if (original == null || original.isEmpty()) continue;
                boolean tagged = withTier && player.hasFakeTier();
                if (!player.changesName() && !tagged) continue;

                Text name = player.changesName() ? buildStyledName(player) : Text.literal(original);
                if (tagged) {
                    text = NameReplacer.stripTierTags(text, original);
                    name = withFakeTags(player, name);
                }
                replacements.put(original, name);
            }
            if (replacements.isEmpty()) return text;
            return NameReplacer.mapStyles(NameReplacer.replace(text, replacements), TierSpoofer::spoofHover);
        } catch (Exception e) {
            LOGGER.debug("Failed to replace names in text", e);
            return text;
        }
    }
}
