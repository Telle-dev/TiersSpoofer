package com.tierspoofer.test;

import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.config.TierSpooferConfigScreen;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TagSide;
import com.tierspoofer.model.TierList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.Perspective;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Takes the screenshots for the Modrinth page. Not a test, nothing is checked here.
public class ShowcaseClientGameTest implements FabricClientGameTest {
    private static final String FAKE = "Herobrine";

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();

            try {
                Object input = context.getInput();
                input.getClass().getMethod("resizeWindow", int.class, int.class).invoke(input, 1920, 1080);
            } catch (Throwable t) {
                System.out.println("[showcase] resize not available: " + t);
            }
            context.waitTicks(10);
            context.runOnClient(client -> {
                client.options.getGuiScale().setValue(3);
                client.onResolutionChanged();
                System.out.println("[showcase] window " + client.getWindow().getWidth() + "x" + client.getWindow().getHeight()
                        + " scale " + client.getWindow().getScaleFactor());
            });
            context.waitTicks(5);

            buildScene(world, context);

            String realName = context.computeOnClient(c -> c.player.getGameProfile().name());
            UUID uuid = context.computeOnClient(c -> c.player.getUuid());
            resetConfig();

            // singleplayer has no other players, and vanilla doesn't draw the tab list for one
            Map<UUID, String> others = new LinkedHashMap<>();
            UUID mango = UUID.randomUUID();
            UUID sniper = UUID.randomUUID();
            others.put(mango, "Mango");
            others.put(sniper, "Sniper77");
            for (String name : new String[]{"Pixelcraft", "Cobblestone", "Lumi"}) {
                others.put(UUID.randomUUID(), name);
            }
            context.runOnClient(client -> {
                try {
                    addTabEntries(client, others);
                } catch (Throwable t) {
                    System.out.println("[showcase] tab entries failed: " + t);
                }
            });

            context.runOnClient(client -> client.options.setPerspective(Perspective.THIRD_PERSON_BACK));

            // before: the real name in tab and chat
            TierSpoofer.getConfig().setEnabled(false);
            chatLines(context, realName);
            hold(context, GLFW.GLFW_KEY_TAB);
            context.takeScreenshot("showcase_tab_before");
            release(context, GLFW.GLFW_KEY_TAB);

            // after
            TierSpoofer.getConfig().setEnabled(true);
            SpoofedPlayer player = new SpoofedPlayer(uuid, realName);
            player.setSpoofedName(FAKE);
            player.setNameColor("#FF5555-#FFAA00");
            player.setTier(TierList.MCTIERS, "HT1", TierList.MCTIERS.getMode("Sword").key());
            player.setTier(TierList.PVPTIERS, "HT3", TierList.PVPTIERS.getMode("Crystal").key());
            TierSpoofer.addSpoofedPlayer(player);
            SpoofedPlayer frostbite = addExtra(mango, "Mango", "Frostbite", "HT1", TierList.MCTIERS, "Sword");
            frostbite.setNameColor("#00C6FF-#0072FF");
            SpoofedPlayer ember = addExtra(sniper, "Sniper77", "Ember", "HT2", TierList.PVPTIERS, "Crystal");
            ember.setTier(TierList.MCTIERS, "LT1", TierList.MCTIERS.getMode("Pot").key());
            ember.setNameColor("#FF5555-#FFAA00");
            context.waitTicks(5);

            hold(context, GLFW.GLFW_KEY_TAB);
            context.takeScreenshot("showcase_tab_after");
            release(context, GLFW.GLFW_KEY_TAB);

            chatLines(context, realName);
            context.takeScreenshot("showcase_chat");

            // the other players, nametags only
            context.runOnClient(client -> client.options.setPerspective(Perspective.FIRST_PERSON));
            context.waitTicks(10);
            context.takeScreenshot("showcase_players");

            // third person from the front, own nametag and the swapped skin
            try {
                context.waitFor(c -> c.player.getSkin().toString().contains("tierspoofer"), 600);
            } catch (Throwable t) {
                System.out.println("[showcase] skin not loaded: " + t);
            }
            world.getServer().runCommand("execute as @p at @s run tp @s ~-3.5 ~ ~-1 160 6");
            context.runOnClient(client -> client.options.setPerspective(Perspective.THIRD_PERSON_FRONT));
            context.waitTicks(10);
            context.takeScreenshot("showcase_nametag");
            context.runOnClient(client -> client.options.setPerspective(Perspective.FIRST_PERSON));

            // the menu with a few players in the list
            TierSpoofer.getConfig().setShowPlayerList(true);
            context.setScreen(() -> new TierSpooferConfigScreen(null));
            context.waitTicks(3);
            context.runOnClient(client -> {
                Object screen = client.currentScreen;
                text(screen, "nameField", realName);
                text(screen, "spoofNameField", FAKE);
                text(screen, "colorField", "#FF5555-#FFAA00");
                set(screen, "selectedList", TierList.MCTIERS);
                set(screen, "selectedTier", "HT1");
                set(screen, "selectedMode", "Sword");
            });
            context.waitTicks(3);
            context.takeScreenshot("showcase_menu");
            context.setScreen(() -> null);
        }
    }

    private static void chatLines(ClientGameTestContext context, String realName) {
        context.runOnClient(client -> {
            var chat = client.inGameHud.getChatHud();
            chat.addMessage(Text.literal("<" + realName + "> new tier just dropped"));
            chat.addMessage(Text.literal("<Mango> anyone up for a duel?"));
            chat.addMessage(Text.literal("<Sniper77> sure, 1v1 at spawn"));
            chat.addMessage(Text.literal("Mango was slain by Sniper77"));
        });
        context.waitTicks(3);
    }

    // the tab key, set directly so it works without a real key event
    private static void hold(ClientGameTestContext context, int key) {
        context.runOnClient(client -> client.options.playerListKey.setPressed(true));
        context.waitTicks(4);
    }

    private static void release(ClientGameTestContext context, int key) {
        context.runOnClient(client -> client.options.playerListKey.setPressed(false));
        context.waitTicks(2);
    }

    // a small plaza at golden hour, three mannequins in front of the player with their name as label
    private static void buildScene(TestSingleplayerContext world, ClientGameTestContext context) {
        String[] commands = {
                "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false",
                "weather clear",
                "time set 11800",
                "execute at @p run fill ~-9 ~-1 ~-7 ~9 ~-1 ~10 minecraft:stone_bricks",
                "execute at @p run fill ~-8 ~-1 ~-6 ~8 ~-1 ~9 minecraft:smooth_quartz",
                "execute at @p run fill ~-3 ~-1 ~1 ~3 ~-1 ~7 minecraft:polished_andesite",
                "execute at @p run fill ~-7 ~ ~8 ~-7 ~3 ~8 minecraft:quartz_pillar",
                "execute at @p run fill ~7 ~ ~8 ~7 ~3 ~8 minecraft:quartz_pillar",
                "execute at @p run fill ~-7 ~ ~-5 ~-7 ~3 ~-5 minecraft:quartz_pillar",
                "execute at @p run fill ~7 ~ ~-5 ~7 ~3 ~-5 minecraft:quartz_pillar",
                "execute at @p run setblock ~-7 ~4 ~8 minecraft:sea_lantern",
                "execute at @p run setblock ~7 ~4 ~8 minecraft:sea_lantern",
                "execute at @p run setblock ~-7 ~4 ~-5 minecraft:sea_lantern",
                "execute at @p run setblock ~7 ~4 ~-5 minecraft:sea_lantern",
                "execute at @p run summon minecraft:mannequin ~-2.5 ~ ~5 {CustomName:\"Pixelcraft\",CustomNameVisible:1b,hide_description:1b,Rotation:[180f,0f]}",
                "execute at @p run summon minecraft:mannequin ~ ~ ~5 {CustomName:\"Mango\",CustomNameVisible:1b,hide_description:1b,Rotation:[180f,0f]}",
                "execute at @p run summon minecraft:mannequin ~2.5 ~ ~5 {CustomName:\"Sniper77\",CustomNameVisible:1b,hide_description:1b,Rotation:[180f,0f]}",
                "execute as @p at @s run tp @s ~3.5 ~ ~1 41 4",
        };
        for (String command : commands) {
            world.getServer().runCommand(command);
        }
        context.waitTicks(60);
    }

    private static void resetConfig() {
        TierSpooferConfig config = TierSpoofer.getConfig();
        TierSpoofer.getSpoofedPlayers().clear();
        config.setEnabled(true);
        config.setShowInTabList(true);
        config.setShowIcons(true);
        config.setSkinEnabled(true);
        config.setCapeEnabled(true);
        config.setRealTiers(false);
        config.setShowOwnNametag(true);
        config.setSpoofForMods(true);
        config.setCommandNames(false);
        config.setShowPlayerList(false);
        config.setSide(TierList.MCTIERS, TagSide.LEFT);
        config.setSide(TierList.PVPTIERS, TagSide.RIGHT);
        config.setSide(TierList.SUBTIERS, TagSide.OFF);
        FakeTierTagger.enabled = false;
    }

    private static SpoofedPlayer addExtra(UUID id, String real, String fake, String tier, TierList list, String mode) {
        SpoofedPlayer p = new SpoofedPlayer(id, real);
        p.setSpoofedName(fake);
        p.setTier(list, tier, list.getMode(mode).key());
        TierSpoofer.getSpoofedPlayers().put(id, p);
        return p;
    }

    // looks the two fields up by type, their names differ between mappings
    @SuppressWarnings("unchecked")
    private static void addTabEntries(MinecraftClient client, Map<UUID, String> players) throws Exception {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        Map<UUID, PlayerListEntry> all = null;
        Collection<PlayerListEntry> listed = null;
        for (Field f : ClientPlayNetworkHandler.class.getDeclaredFields()) {
            if (!(f.getGenericType() instanceof ParameterizedType type)) continue;
            Type[] args = type.getActualTypeArguments();
            f.setAccessible(true);
            if (Map.class.isAssignableFrom(f.getType()) && args.length == 2 && args[0] == UUID.class && args[1] == PlayerListEntry.class) {
                all = (Map<UUID, PlayerListEntry>) f.get(handler);
            } else if (Collection.class.isAssignableFrom(f.getType()) && args.length == 1 && args[0] == PlayerListEntry.class) {
                listed = (Collection<PlayerListEntry>) f.get(handler);
            }
        }
        if (all == null || listed == null) throw new IllegalStateException("player list fields not found");

        Constructor<PlayerListEntry> constructor = PlayerListEntry.class.getDeclaredConstructor(GameProfile.class, boolean.class);
        constructor.setAccessible(true);
        for (Map.Entry<UUID, String> player : players.entrySet()) {
            GameProfile profile = new GameProfile(player.getKey(), player.getValue(), client.player.getGameProfile().properties());
            PlayerListEntry entry = constructor.newInstance(profile, false);
            all.put(player.getKey(), entry);
            listed.add(entry);
        }
    }

    private static void text(Object owner, String field, String value) {
        ((TextFieldWidget) get(owner, field)).setText(value);
    }

    private static Object get(Object owner, String name) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(Object owner, String name, Object value) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(owner, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
