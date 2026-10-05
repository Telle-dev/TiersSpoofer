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

            String realName = context.computeOnClient(c -> c.player.getGameProfile().name());
            UUID uuid = context.computeOnClient(c -> c.player.getUuid());
            resetConfig();

            // singleplayer has no other players, and vanilla doesn't draw the tab list for one
            Map<UUID, String> others = new LinkedHashMap<>();
            UUID notch = UUID.randomUUID();
            UUID dream = UUID.randomUUID();
            others.put(notch, "Notch");
            others.put(dream, "Dream");
            for (String name : new String[]{"Pixelcraft", "Cobblestone", "Mango", "Sniper77", "Lumi"}) {
                others.put(UUID.randomUUID(), name);
            }
            context.runOnClient(client -> {
                try {
                    addTabEntries(client, others);
                } catch (Throwable t) {
                    System.out.println("[showcase] tab entries failed: " + t);
                }
            });

            // before: the real name in tab and chat
            TierSpoofer.getConfig().setEnabled(false);
            context.runOnClient(client -> client.inGameHud.getChatHud().addMessage(Text.literal("<" + realName + "> new tier just dropped")));
            context.waitTicks(3);
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
            addExtra(notch, "Notch", "Alex", "LT2", TierList.MCTIERS, "Pot");
            addExtra(dream, "Dream", "Bob", "HT3", TierList.PVPTIERS, "Sword");
            context.waitTicks(5);

            hold(context, GLFW.GLFW_KEY_TAB);
            context.takeScreenshot("showcase_tab_after");
            release(context, GLFW.GLFW_KEY_TAB);

            context.runOnClient(client -> client.inGameHud.getChatHud().addMessage(Text.literal("<" + realName + "> new tier just dropped")));
            context.waitTicks(3);
            context.takeScreenshot("showcase_chat");

            // third person from the front, own nametag and the swapped skin
            try {
                context.waitFor(c -> c.player.getSkin().toString().contains("tierspoofer"), 600);
            } catch (Throwable t) {
                System.out.println("[showcase] skin not loaded: " + t);
            }
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

    // the tab key, set directly so it works without a real key event
    private static void hold(ClientGameTestContext context, int key) {
        context.runOnClient(client -> client.options.playerListKey.setPressed(true));
        context.waitTicks(4);
    }

    private static void release(ClientGameTestContext context, int key) {
        context.runOnClient(client -> client.options.playerListKey.setPressed(false));
        context.waitTicks(2);
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

    private static void addExtra(UUID id, String real, String fake, String tier, TierList list, String mode) {
        SpoofedPlayer p = new SpoofedPlayer(id, real);
        p.setSpoofedName(fake);
        p.setTier(list, tier, list.getMode(mode).key());
        TierSpoofer.getSpoofedPlayers().put(id, p);
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
