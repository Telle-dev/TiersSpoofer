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

import java.lang.reflect.Field;
import java.util.UUID;

// Takes the screenshots for the Modrinth page. Not a test, nothing is checked here.
public class ShowcaseClientGameTest implements FabricClientGameTest {
    private static final String FAKE = "Herobrine";

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();

            context.runOnClient(client -> {
                try {
                    GLFW.glfwSetWindowSize(client.getWindow().getHandle(), 1920, 1080);
                } catch (Throwable t) {
                    System.out.println("[showcase] resize failed: " + t);
                }
            });
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
            context.runOnClient(client -> {
                client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                client.options.hudHidden = true;
            });
            context.waitTicks(10);
            context.takeScreenshot("showcase_nametag");
            context.runOnClient(client -> {
                client.options.setPerspective(Perspective.FIRST_PERSON);
                client.options.hudHidden = false;
            });

            // the menu with a few players in the list
            addExtra("Notch", "Alex", "LT2", TierList.MCTIERS, "Pot");
            addExtra("Dream", "Bob", "HT3", TierList.PVPTIERS, "Sword");
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

    private static void addExtra(String real, String fake, String tier, TierList list, String mode) {
        UUID id = UUID.randomUUID();
        SpoofedPlayer p = new SpoofedPlayer(id, real);
        p.setSpoofedName(fake);
        p.setTier(list, tier, list.getMode(mode).key());
        TierSpoofer.getSpoofedPlayers().put(id, p);
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
