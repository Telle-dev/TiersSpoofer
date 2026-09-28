package com.tierspoofer.test;

import com.tierspoofer.SkinCache;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfigScreen;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.ScreenHandlerType;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Runs inside a real client: adds the local player through the config screen
 * exactly like a user would, then checks every place the spoof should show up.
 */
public class TierSpooferClientGameTest implements FabricClientGameTest {
    private static final String FAKE = "Notch";
    private final List<String> failures = new ArrayList<>();
    private static volatile String receivedName;

    @Override
    public void runTest(ClientGameTestContext context) {
        // "/tstarget <name>" stores what the server got, no op needed
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                CommandManager.literal("tstarget").then(CommandManager.argument("name", StringArgumentType.word())
                        // server-side suggestions, like /tpa from a plugin
                        .suggests((c, b) -> CommandSource.suggestMatching(c.getSource().getServer().getPlayerNames(), b))
                        .executes(c -> {
                            receivedName = StringArgumentType.getString(c, "name");
                            return 1;
                        }))));

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();

            String realName = context.computeOnClient(c -> c.player.getGameProfile().getName());
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

                // with the real TierTagger installed its tag lands on top of ours; it has to be dropped
                FakeTierTagger.enabled = true;
                String taggedTab = entry == null ? "<no tab entry>"
                        : client.inGameHud.getPlayerListHud().getPlayerName(entry).getString();
                String taggedName = client.player.getDisplayName().getString();
                FakeTierTagger.enabled = false;
                check("TierTagger tag dropped in tab", taggedTab.contains("HT1") && taggedTab.contains(FAKE) && !taggedTab.contains("HT3"), taggedTab);
                check("TierTagger tag dropped on nametag", taggedName.contains("HT1") && taggedName.contains(FAKE) && !taggedName.contains("HT3"), taggedName);

                // chat
                String chat = TierSpoofer.replaceNamesInText(Text.literal("<" + realName + "> hello")).getString();
                String untouched = TierSpoofer.toRealNames("tpa " + FAKE);
                check("commands untouched while Cmds is off", untouched.equals("tpa " + FAKE), untouched);
                TierSpoofer.getConfig().setCommandNames(true);
                String cmd = TierSpoofer.toRealNames("tpa " + FAKE.toLowerCase());
                check("fake name in commands sent as real name", cmd.equals("tpa " + realName), cmd);

                try {
                    SkinCache.prefetchSkin("K1 RBE");
                    check("fake name with a space doesn't crash the skin lookup", true, "");
                } catch (Throwable t) {
                    check("fake name with a space doesn't crash the skin lookup", false, t.toString());
                }

                // what TierTagger / PvPTiers' Tiers mod make of the name before we see it
                java.util.UUID id = client.player.getUuid();
                Text tierTagger = Text.literal("\uE706").append(Text.literal("HT3")).append(Text.literal(" | ")).append(Text.literal(realName));
                String withTt = TierSpoofer.getDisplayName(id, realName, tierTagger).getString();
                check("no double tag with TierTagger", withTt.contains("HT1 | " + FAKE) && !withTt.contains("HT3"), withTt);
                Text tiersMod = Text.empty().append(Text.literal("\uF005 HT3 EU | ")).append(Text.literal(realName)).append(Text.literal(" | EU HT3 \uF005"));
                String withTiers = TierSpoofer.getDisplayName(id, realName, tiersMod).getString();
                check("no double tag with PvPTiers' Tiers mod", withTiers.contains("HT1 | " + FAKE) && !withTiers.contains("HT3"), withTiers);

                // the real hooks: chat HUD and death screen
                client.inGameHud.getChatHud().addMessage(Text.literal("<" + realName + "> gg"));
                String chatLine = safe(() -> newestChatLine(client.inGameHud.getChatHud()));
                check("chat hud shows the fake name", chatLine.contains(FAKE) && !chatLine.contains(realName), chatLine);
                Object death = new DeathScreen(Text.literal(realName + " was slain"), false);
                String deathMsg = safe(() -> firstTextField(death));
                check("death screen shows the fake name", deathMsg.contains(FAKE) && !deathMsg.contains(realName), deathMsg);

                check("chat name replaced", chat.contains(FAKE) && !chat.contains(realName), chat);

                // servers that color names with old-style codes: "§aName", a color per letter, §x hex
                StringBuilder perLetter = new StringBuilder();
                for (int i = 0; i < realName.length(); i++) perLetter.append('\u00A7').append("c6eab9".charAt(i % 6)).append(realName.charAt(i));
                String legacy = TierSpoofer.replaceNamesInText(Text.literal("\u00A77Rank \u00A7a" + realName + "\u00A7r | " + perLetter
                        + " | \u00A7x\u00A7f\u00A7f\u00A75\u00A75\u00A75\u00A75" + realName)).getString();
                check("color-coded names swapped", legacy.equals("Rank " + FAKE + " | " + FAKE + " | " + FAKE), legacy);
            });

            // --- a command typed with the fake name reaches the server with the real one
            context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("tstarget " + FAKE.toLowerCase()));
            context.waitTicks(10);
            check("typed fake name sent to the server as the real name", realName.equals(receivedName), String.valueOf(receivedName));

            // --- tab-complete offers the real name and the fake name, and the fake one also by its own first letters
            context.setScreen(() -> new ChatScreen("/msg "));
            context.waitTicks(20);
            String suggested = context.computeOnClient(client -> safe(() -> suggestionsOf(client.currentScreen)));
            check("tab-complete shows real and fake name", suggested.contains(FAKE) && suggested.contains(realName), suggested);
            String fakePrefix = FAKE.substring(0, 2).toLowerCase();
            context.setScreen(() -> new ChatScreen("/msg " + fakePrefix));
            context.waitTicks(20);
            String byPrefix = context.computeOnClient(client -> safe(() -> suggestionsOf(client.currentScreen)));
            context.setScreen(() -> null);
            check("tab-complete finds the fake name by its first letters", byPrefix.contains(FAKE), byPrefix);

            // tab in normal chat
            String chatNames = context.computeOnClient(client -> client.getNetworkHandler().getCommandSource().getChatSuggestions().toString());
            check("chat tab-complete has real and fake name", chatNames.contains(FAKE) && chatNames.contains(realName), chatNames);

            // same with names the server suggests (plugin commands like /tpa)
            context.setScreen(() -> new ChatScreen("/tstarget "));
            context.waitTicks(20);
            String fromServer = context.computeOnClient(client -> safe(() -> suggestionsOf(client.currentScreen)));
            check("server tab-complete shows real and fake name", fromServer.contains(FAKE) && fromServer.contains(realName), fromServer);
            context.setScreen(() -> new ChatScreen("/tstarget " + fakePrefix));
            context.waitTicks(20);
            String fromServerPrefix = context.computeOnClient(client -> safe(() -> suggestionsOf(client.currentScreen)));
            context.setScreen(() -> null);
            check("server tab-complete finds the fake name by its first letters", fromServerPrefix.contains(FAKE), fromServerPrefix);

            // --- typed in the chat box like a player: fake and real name both reach the server as the real one
            for (String typed : new String[]{FAKE.toLowerCase(), realName.toLowerCase(), realName}) {
                receivedName = null;
                context.setScreen(() -> new ChatScreen(""));
                context.waitTick();
                context.getInput().typeChars("/tstarget " + typed);
                context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
                context.waitTicks(10);
                check("chat box '/tstarget " + typed + "' reaches the server as the real name",
                        realName.equalsIgnoreCase(String.valueOf(receivedName)), String.valueOf(receivedName));
            }
            context.setScreen(() -> null);

            // --- other places servers put your name
            world.getServer().runCommand("bossbar add tierspoofer:test \"" + realName + " vs Herobrine\"");
            world.getServer().runCommand("bossbar set tierspoofer:test players @a");
            world.getServer().runCommand("scoreboard objectives add tsside dummy \"" + realName + "'s stats\"");
            world.getServer().runCommand("scoreboard objectives setdisplay sidebar tsside");
            world.getServer().runCommand("scoreboard players set " + realName + " tsside 7");
            world.getServer().runCommand("team add tsteam");
            world.getServer().runCommand("team modify tsteam prefix \"Hi " + realName + " \"");
            world.getServer().runCommand("title @a subtitle \"GG " + realName + "\"");
            world.getServer().runCommand("title @a title \"" + realName + "\"");
            world.getServer().runCommand("title @a actionbar \"" + realName + " joined\"");
            context.waitTicks(10);
            context.runOnClient(client -> {
                String hud = safe(() -> textFields(client.inGameHud));
                check("title, subtitle and action bar", hud.contains(FAKE) && !hud.contains(realName), hud);
                String bars = safe(() -> bossBarNames(client.inGameHud.getBossBarHud()));
                check("boss bar", bars.contains(FAKE) && !bars.contains(realName), bars);

                Scoreboard scoreboard = client.world.getScoreboard();
                ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                String side = sidebar == null ? "<no sidebar>" : sidebar.getDisplayName().getString() + " "
                        + scoreboard.getScoreboardEntries(sidebar).stream().map(e -> e.name().getString()).toList();
                check("scoreboard sidebar", side.contains(FAKE) && !side.contains(realName), side);
                Team team = scoreboard.getTeam("tsteam");
                String prefix = team == null ? "<no team>" : team.getPrefix().getString();
                check("team prefix", prefix.contains(FAKE) && !prefix.contains(realName), prefix);
                String decorated = team == null ? "<no team>" : Team.decorateName(team, Text.literal("line")).getString();
                check("team prefix as the sidebar draws it", decorated.contains(FAKE) && !decorated.contains(realName), decorated);
                client.inGameHud.getPlayerListHud().setHeader(Text.literal("Welcome " + realName));
                String header = safe(() -> textFields(client.inGameHud.getPlayerListHud()));
                check("tab header", header.contains(FAKE) && !header.contains(realName), header);

                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponentTypes.CUSTOM_NAME, Text.literal(realName + "'s Profile"));
                head.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Owner: " + realName))));
                String tooltip = Screen.getTooltipFromItem(client, head).stream().map(Text::getString).toList().toString();
                check("item name and lore", tooltip.contains(FAKE) && !tooltip.contains(realName), tooltip);

                HandledScreens.open(ScreenHandlerType.GENERIC_9X3, client, 99, Text.literal(realName + "'s Profile"));
                String menu = client.currentScreen == null ? "<no screen>" : client.currentScreen.getTitle().getString();
                check("menu title", menu.contains(FAKE) && !menu.contains(realName), menu);
            });
            context.setScreen(() -> null);

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
                context.waitFor(c -> c.player.getSkinTextures().toString().contains("tierspoofer"), 400);
                skinOk = true;
            } catch (Throwable t) {
                skinOk = false;
            }
            String skin = context.computeOnClient(c -> c.player.getSkinTextures().toString());
            if (!skinOk && SkinCache.getUuidForUsername(FAKE) == null) {
                // the Mojang API is shared by all CI runners and often rate limits them
                log("SKIP own skin swap: Mojang API didn't answer the name lookup");
            } else {
                check("own skin swapped to fake name's skin", skinOk, skin);
            }
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

    /** Suggestions currently shown in a chat screen (fields found by type, names differ in production). */
    private static String suggestionsOf(Object chatScreen) throws Exception {
        Object suggestor = fieldOfType(chatScreen, ChatInputSuggestor.class);
        if (suggestor == null) return "<no suggestor>";
        Object pending = fieldOfType(suggestor, CompletableFuture.class);
        if (!(pending instanceof CompletableFuture<?> future) || !future.isDone()) return "<no suggestions yet>";
        Object result = future.getNow(null);
        if (!(result instanceof Suggestions suggestions)) return "<none>";
        List<String> texts = new ArrayList<>();
        for (Suggestion suggestion : suggestions.getList()) texts.add(suggestion.getText());
        return texts.toString();
    }

    /** Text of the newest chat line (lists of line records, found by type). */
    private static String newestChatLine(Object chatHud) throws Exception {
        for (Field f : chatHud.getClass().getDeclaredFields()) {
            if (!List.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            if (!(f.get(chatHud) instanceof List<?> list) || list.isEmpty()) continue;
            Object line = list.get(0);
            if (!line.getClass().isRecord()) continue;
            for (RecordComponent rc : line.getClass().getRecordComponents()) {
                if (rc.getAccessor().invoke(line) instanceof Text t) return t.getString();
            }
        }
        return "<no chat line>";
    }

    private static String firstTextField(Object owner) throws Exception {
        for (Field f : owner.getClass().getDeclaredFields()) {
            if (Text.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object v = f.get(owner);
                return v == null ? "<null>" : ((Text) v).getString();
            }
        }
        return "<no text field>";
    }

    /** Every Text field of an object, e.g. the HUD's title, subtitle and action bar. */
    private static String textFields(Object owner) throws Exception {
        List<String> texts = new ArrayList<>();
        for (Field f : owner.getClass().getDeclaredFields()) {
            if (!Text.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            if (f.get(owner) instanceof Text t) texts.add(t.getString());
        }
        return texts.toString();
    }

    private static String bossBarNames(Object bossBarHud) throws Exception {
        if (!(fieldOfType(bossBarHud, Map.class) instanceof Map<?, ?> bars)) return "<no boss bars>";
        List<String> names = new ArrayList<>();
        for (Object bar : bars.values()) names.add(((BossBar) bar).getName().getString());
        return names.toString();
    }

    private static Object fieldOfType(Object owner, Class<?> type) throws Exception {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f.get(owner);
                }
            }
        }
        return null;
    }

    /** Lambdas passed to the client can't throw checked exceptions, so report them as text. */
    private static String safe(Callable<String> reader) {
        try {
            return String.valueOf(reader.call());
        } catch (Exception e) {
            return "<error: " + e + ">";
        }
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
