package com.tierspoofer.test;

import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfigScreen;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs inside a real client: adds the local player through the config screen
 * exactly like a user would, then checks every place the spoof should show up.
 */
public class TierSpooferClientGameTest implements FabricClientGameTest {
    private static final String FAKE = "Notch";
    private final List<String> failures = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();

            String realName = context.computeOnClient(c -> c.player.getGameProfile().name());
            log("local player: " + realName + " / " + context.computeOnClient(c -> c.player.getUuid()));
            log("config enabled: " + TierSpoofer.getConfig().isEnabled());

            // --- fill in the GUI and press Add, like a user
            context.setScreen(() -> new TierSpooferConfigScreen(null));
            context.waitTick();
            context.runOnClient(client -> {
                Object screen = client.currentScreen;
                check("config screen open", screen instanceof TierSpooferConfigScreen, String.valueOf(screen));
                ((TextFieldWidget) get(screen, "nameField")).setText(realName);
                ((TextFieldWidget) get(screen, "spoofNameField")).setText(FAKE);
                ((TextFieldWidget) get(screen, "colorField")).setText("#FF0000");
                set(screen, "selectedTier", "HT1");
                set(screen, "selectedList", TierList.PVPTIERS);
                set(screen, "selectedMode", "Sword");
                call(screen, "addPlayer");
            });
            context.setScreen(() -> null);
            context.waitTicks(5);

            context.runOnClient(client -> {
                log("spoofed entries: " + TierSpoofer.getSpoofedPlayers().size());
                for (SpoofedPlayer p : TierSpoofer.getSpoofedPlayers().values()) {
                    log("  entry uuid=" + p.getUuid() + " name=" + p.getOriginalName() + " fake=" + p.getSpoofedName()
                            + " tier=" + p.getDisplayTier() + " list=" + p.getTierList() + " mode=" + p.getGamemode()
                            + " color=" + p.getNameColor());
                }
                check("entry added for local player",
                        TierSpoofer.findSpoofedPlayer(client.player.getUuid(), realName) != null, "");

                // nametag text (what's drawn above the head)
                String display = client.player.getDisplayName().getString();
                check("nametag text has tier + fake name", display.contains("HT1") && display.contains(FAKE), display);

                // tab list
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                String tab = entry == null ? "<no tab entry>"
                        : client.inGameHud.getPlayerListHud().getPlayerName(entry).getString();
                check("tab list has tier + fake name", tab.contains("HT1") && tab.contains(FAKE), tab);

                // chat
                String chat = TierSpoofer.replaceNamesInText(Text.literal("<" + realName + "> hello")).getString();
                check("chat name replaced", chat.contains(FAKE) && !chat.contains(realName), chat);
            });

            // --- server-made nametags: a text display and an armor stand showing the name
            world.getServer().runCommand("execute at @a run summon text_display ~ ~2.5 ~2 {text:\"" + realName + "\",billboard:\"center\"}");
            world.getServer().runCommand("execute at @a run summon armor_stand ~ ~ ~2 {CustomName:\"" + realName + "\",CustomNameVisible:1b,NoGravity:1b,Invisible:1b}");
            context.waitTicks(20);
            context.runOnClient(client -> {
                String hologram = null, stand = null;
                for (Entity e : client.world.getEntities()) {
                    try {
                        EntityRenderState st = client.getEntityRenderDispatcher().getRenderer(e).getAndUpdateRenderState(e, 1.0f);
                        if (e instanceof DisplayEntity.TextDisplayEntity) {
                            hologram = linesToString(((TextDisplayEntityRenderState) st).textLines);
                        } else if (e instanceof ArmorStandEntity) {
                            stand = st.displayName == null ? "<no label>" : st.displayName.getString();
                        }
                    } catch (Throwable t) {
                        log("render state error for " + e + ": " + t);
                    }
                }
                check("text display hologram swapped", hologram != null && hologram.contains(FAKE) && hologram.contains("HT1"), String.valueOf(hologram));
                check("armor stand hologram swapped", stand != null && stand.contains(FAKE) && stand.contains("HT1"), String.valueOf(stand));
            });

            // --- own nametag in F5
            context.runOnClient(client -> client.options.setPerspective(Perspective.THIRD_PERSON_BACK));
            context.waitTicks(5);
            context.runOnClient(client -> {
                try {
                    EntityRenderer<? super net.minecraft.client.network.ClientPlayerEntity, ?> renderer =
                            client.getEntityRenderDispatcher().getRenderer(client.player);
                    EntityRenderState state = renderer.getAndUpdateRenderState(client.player, 1.0f);
                    String label = state.displayName == null ? "<no label>" : state.displayName.getString();
                    check("own nametag shown in F5", state.displayName != null && label.contains(FAKE), label);
                } catch (Throwable t) {
                    check("own nametag shown in F5", false, t.toString());
                }
            });
            context.takeScreenshot("tierspoofer_f5");

            // --- skin (downloaded from Mojang for the fake name)
            boolean skinOk;
            try {
                context.waitFor(c -> c.player.getSkin().toString().contains("tierspoofer"), 400);
                skinOk = true;
            } catch (Throwable t) {
                skinOk = false;
            }
            String skin = context.computeOnClient(c -> c.player.getSkin().toString());
            check("own skin swapped to fake name's skin", skinOk, skin);
            context.takeScreenshot("tierspoofer_f5_skin");
            context.runOnClient(client -> client.options.setPerspective(Perspective.FIRST_PERSON));
        }

        log(failures.isEmpty() ? "ALL CHECKS PASSED" : failures.size() + " CHECK(S) FAILED: " + failures);
        if (!failures.isEmpty()) {
            throw new AssertionError("TierSpoofer checks failed: " + failures);
        }
    }

    /** Reads the rendered lines of a text display (records, so read by component type). */
    private static String linesToString(Object textLines) throws Exception {
        if (textLines == null) return "<no lines>";
        StringBuilder sb = new StringBuilder();
        for (RecordComponent rc : textLines.getClass().getRecordComponents()) {
            Object v = rc.getAccessor().invoke(textLines);
            if (!(v instanceof List<?> list)) continue;
            for (Object line : list) {
                for (RecordComponent lc : line.getClass().getRecordComponents()) {
                    Object o = lc.getAccessor().invoke(line);
                    if (o instanceof OrderedText ordered) {
                        ordered.accept((index, style, cp) -> {
                            sb.appendCodePoint(cp);
                            return true;
                        });
                        sb.append(' ');
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    private void check(String name, boolean ok, String detail) {
        log((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : "  ->  " + detail));
        if (!ok) failures.add(name);
    }

    private static void log(String msg) {
        System.out.println("[TierSpooferTest] " + msg);
    }

    private static Object get(Object o, String field) {
        try {
            Field f = o.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(Object o, String field, Object value) {
        try {
            Field f = o.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(o, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void call(Object o, String method) {
        try {
            Method m = o.getClass().getDeclaredMethod(method);
            m.setAccessible(true);
            m.invoke(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
