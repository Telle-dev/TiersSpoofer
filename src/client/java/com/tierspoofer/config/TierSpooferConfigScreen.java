package com.tierspoofer.config;

import com.tierspoofer.NameColor;
import com.tierspoofer.SkinCache;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class TierSpooferConfigScreen extends Screen {
    private static final String[] TIERS = {
            "None", "HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5",
            "RHT1", "RLT1", "RHT2", "RLT2", "RHT3", "RLT3", "RHT4", "RLT4", "RHT5", "RLT5"
    };
    private static final int MODE_DROPDOWN_WIDTH = 80;

    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget spoofNameField;
    private TextFieldWidget colorField;
    private int swatchX;
    private int swatchY;
    private int previewY;

    private static final String[] SWATCHES = {
            "#FF5555", "#FFAA00", "#FFFF55", "#55FF55", "#55FFFF", "#5555FF", "#FF55FF", "#FFFFFF",
            "#AAAAAA", "#FF0000-#FFAA00", "#00C6FF-#0072FF", "#F953C6-#B91D73", "rainbow"
    };
    private static final int SWATCH_SIZE = 12;
    private static final int SWATCH_GAP = 3;
    private String selectedTier = "None";
    private String selectedMode = "None";
    private TierList selectedList = TierList.PVPTIERS;
    private UUID selectedPlayerUuid;

    private boolean tierDropdownOpen = false;
    private boolean modeDropdownOpen = false;
    private int tierDropdownScroll = 0;
    private int modeDropdownScroll = 0;

    private int listScroll = 0;
    private int listY;

    private ButtonWidget tierDropdownButton;
    private ButtonWidget modeDropdownButton;
    private ButtonWidget listButton;
    private ButtonWidget realModeButton;

    public TierSpooferConfigScreen(Screen parent) {
        super(Text.literal("TierSpoofer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TierSpooferConfig config = TierSpoofer.getConfig();
        int centerX = this.width / 2;
        int y = 30;

        tip(toggle("Mod", config::isEnabled, config::setEnabled, centerX - 160, y, 70), "Turns the whole mod on or off.");
        tip(toggle("List", config::isShowPlayerList, config::setShowPlayerList, centerX - 85, y, 70), "Shows everyone you added below. Click someone to load them into the boxes.");

        // Real: OFF -> MCTiers -> PvPTiers -> SubTiers -> OFF
        this.addDrawableChild(ButtonWidget.builder(
                realListLabel(config.getRealTierList()),
                btn -> {
                    TierList current = config.getRealTierList();
                    TierList next = current == null ? TierList.values()[0]
                            : (current.ordinal() == TierList.values().length - 1 ? null : current.next());
                    config.setRealTierList(next);
                    if (next == null || next.getMode(config.getRealTierMode()) == null) {
                        config.setRealTierMode("highest");
                    }
                    btn.setMessage(realListLabel(next));
                    realModeButton.setMessage(realModeLabel());
                    TierSpoofer.saveConfig();
                }
        ).dimensions(centerX - 10, y, 90, 20).tooltip(Tooltip.of(Text.literal("Shows everyone's real tier from this list. People you added keep their fake one."))).build());
        tip(toggle("Own Tag", config::isShowOwnNametag, config::setShowOwnNametag, centerX + 85, y, 75), "Shows your own nametag in F5.");

        y += 24;
        tip(toggle("Skin", config::isSkinEnabled, config::setSkinEnabled, centerX - 160, y, 55), "Gives people the skin of their fake name.");
        tip(toggle("Cape", config::isCapeEnabled, config::setCapeEnabled, centerX - 102, y, 55), "Takes the fake name's cape too. Off keeps their own cape.");
        tip(toggle("Icons", config::isShowIcons, config::setShowIcons, centerX - 44, y, 58), "Shows the gamemode icon in front of the tier.");
        tip(toggle("Cmds", config::isCommandNames, config::setCommandNames, centerX + 17, y, 65),
                "Fake names show up when you tab-complete commands, and get swapped back to the real name when you send it. /tpa k1rbe goes out as /tpa Steve.");

        realModeButton = ButtonWidget.builder(realModeLabel(), btn -> {
            TierList list = config.getRealTierList();
            if (list == null) return;
            List<String> keys = new ArrayList<>();
            keys.add("highest");
            keys.addAll(list.getModes().keySet());
            int idx = keys.indexOf(config.getRealTierMode().toLowerCase());
            config.setRealTierMode(keys.get((idx + 1) % keys.size()));
            btn.setMessage(realModeLabel());
            TierSpoofer.saveConfig();
        }).dimensions(centerX + 85, y, 75, 20).tooltip(Tooltip.of(Text.literal("Which gamemode the real tier is from. Best uses their highest one."))).build();
        this.addDrawableChild(realModeButton);

        y += 26;
        nameField = new TextFieldWidget(this.textRenderer, centerX - 160, y, 100, 18, Text.literal("Player Name"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Player Name"));
        tip(nameField, "Real name of the player you want to spoof.");
        this.addDrawableChild(nameField);

        tierDropdownButton = ButtonWidget.builder(
                Text.literal(selectedTier),
                btn -> { tierDropdownOpen = !tierDropdownOpen; modeDropdownOpen = false; }
        ).dimensions(centerX - 55, y, 50, 18).tooltip(Tooltip.of(Text.literal("The fake tier."))).build();
        this.addDrawableChild(tierDropdownButton);

        modeDropdownButton = ButtonWidget.builder(
                Text.literal(selectedMode.equals("None") ? "Mode" : selectedMode),
                btn -> { modeDropdownOpen = !modeDropdownOpen; tierDropdownOpen = false; }
        ).dimensions(centerX, y, 50, 18).tooltip(Tooltip.of(Text.literal("Gamemode, decides the icon in front of the tier."))).build();
        this.addDrawableChild(modeDropdownButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> addPlayer())
                .dimensions(centerX + 55, y, 40, 18).tooltip(Tooltip.of(Text.literal("Adds the player with what's set here."))).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(centerX + 100, y, 40, 18).tooltip(Tooltip.of(Text.literal("Closes the menu. Everything is saved already."))).build());

        y += 22;
        spoofNameField = new TextFieldWidget(this.textRenderer, centerX - 160, y, 100, 18, Text.literal("Fake Name"));
        spoofNameField.setMaxLength(64);
        spoofNameField.setPlaceholder(Text.literal("Fake Name (supports &codes)"));
        tip(spoofNameField, "Name shown instead of their real one. Color codes like &c or &#FF5555 work. The skin comes from this name too.");
        this.addDrawableChild(spoofNameField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), btn -> editPlayer())
                .dimensions(centerX - 55, y, 40, 18).tooltip(Tooltip.of(Text.literal("Puts what's in the boxes on the player you clicked in the list."))).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Del"), btn -> deletePlayer())
                .dimensions(centerX - 10, y, 35, 18).tooltip(Tooltip.of(Text.literal("Removes the player you clicked in the list."))).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), btn -> clearAll())
                .dimensions(centerX + 30, y, 40, 18).tooltip(Tooltip.of(Text.literal("Removes everyone."))).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("All"), btn -> addAllOnline())
                .dimensions(centerX + 75, y, 35, 18).tooltip(Tooltip.of(Text.literal("Adds everyone who's online right now."))).build());

        listButton = ButtonWidget.builder(Text.literal(selectedList.displayName), btn -> {
            selectedList = selectedList.next();
            btn.setMessage(Text.literal(selectedList.displayName));
            TierList.Mode mode = selectedList.getMode(selectedMode);
            selectedMode = mode != null ? mode.label() : "None";
            modeDropdownButton.setMessage(Text.literal(selectedMode.equals("None") ? "Mode" : selectedMode));
            modeDropdownScroll = 0;
        }).dimensions(centerX + 115, y, 60, 18).tooltip(Tooltip.of(Text.literal("Which tier list the fake tier is from. Changes the colors and icons."))).build();
        this.addDrawableChild(listButton);

        y += 22;
        colorField = new TextFieldWidget(this.textRenderer, centerX - 160, y, 100, 18, Text.literal("Name Color"));
        colorField.setMaxLength(64);
        colorField.setPlaceholder(Text.literal("Name Color (#hex)"));
        colorField.setChangedListener(text -> colorField.setEditableColor(
                text.isBlank() || NameColor.isValid(text) ? 0xFFE0E0E0 : 0xFFFF5555));
        tip(colorField, "Name color: #FF5555, a gradient like #FF0000-#0000FF, or rainbow. Or click a color on the right.");
        this.addDrawableChild(colorField);
        swatchX = centerX - 55;
        swatchY = y + 3;

        y += 24;
        previewY = y;

        y += 18;
        listY = y;
    }

    private static <T extends ClickableWidget> T tip(T widget, String text) {
        widget.setTooltip(Tooltip.of(Text.literal(text)));
        return widget;
    }

    private ButtonWidget toggle(String label, BooleanSupplier get, Consumer<Boolean> set, int x, int y, int width) {
        return this.addDrawableChild(ButtonWidget.builder(onOff(label, get.getAsBoolean()), btn -> {
            set.accept(!get.getAsBoolean());
            btn.setMessage(onOff(label, get.getAsBoolean()));
            TierSpoofer.saveConfig();
        }).dimensions(x, y, width, 20).build());
    }

    private static Text onOff(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    private static Text realListLabel(TierList list) {
        return Text.literal("Real: " + (list == null ? "OFF" : list.displayName));
    }

    private Text realModeLabel() {
        TierSpooferConfig config = TierSpoofer.getConfig();
        TierList list = config.getRealTierList();
        String mode = config.getRealTierMode();
        if (list == null || mode.equalsIgnoreCase("highest")) {
            return Text.literal("Best");
        }
        TierList.Mode m = list.getMode(mode);
        return m == null ? Text.literal("Best") : Text.literal(m.icon() + " " + m.label());
    }

    private static List<SpoofedPlayer> sortedPlayers() {
        List<SpoofedPlayer> players = new ArrayList<>(TierSpoofer.getSpoofedPlayers().values());
        players.sort(Comparator.comparing(p -> p.getOriginalName() == null ? "" : p.getOriginalName().toLowerCase()));
        return players;
    }

    private String[] modeOptions() {
        List<String> options = new ArrayList<>();
        options.add("None");
        for (TierList.Mode mode : selectedList.getModes().values()) {
            options.add(mode.label());
        }
        return options.toArray(new String[0]);
    }

    private void addPlayer() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        UUID uuid = resolveUuid(name);
        // same name again = update it
        SpoofedPlayer existing = TierSpoofer.findSpoofedPlayer(uuid, name);
        if (existing != null) {
            applyFormToPlayer(existing);
            TierSpoofer.saveConfig();
            selectedPlayerUuid = existing.getUuid();
            return;
        }
        SpoofedPlayer player = new SpoofedPlayer(uuid, name);
        applyFormToPlayer(player);
        TierSpoofer.addSpoofedPlayer(player);
        selectedPlayerUuid = uuid;
    }

    private void editPlayer() {
        if (selectedPlayerUuid == null) return;
        SpoofedPlayer player = TierSpoofer.getSpoofedPlayer(selectedPlayerUuid);
        if (player == null) return;
        applyFormToPlayer(player);
        TierSpoofer.saveConfig();
    }

    private void applyFormToPlayer(SpoofedPlayer player) {
        player.setDisplayTier(selectedTier.equals("None") ? null : selectedTier);
        player.setTierList(selectedList);
        String color = colorField.getText().trim();
        player.setNameColor(NameColor.isValid(color) ? color : null);
        TierList.Mode mode = selectedList.getMode(selectedMode);
        String fallback = selectedList == TierList.MCTIERS
                ? "vanilla" : selectedList.getModes().keySet().iterator().next();
        player.setGamemode(mode != null ? mode.key() : fallback);
        String spoofName = spoofNameField.getText().trim();
        if (!spoofName.isEmpty()) {
            player.setSpoofedName(spoofName);
            SkinCache.prefetchSkin(player.getSkinTargetName());
        } else {
            player.setSpoofedName(null);
        }
    }

    private void deletePlayer() {
        if (selectedPlayerUuid == null) return;
        TierSpoofer.removeSpoofedPlayer(selectedPlayerUuid);
        selectedPlayerUuid = null;
    }

    private void clearAll() {
        TierSpoofer.getSpoofedPlayers().clear();
        TierSpoofer.saveConfig();
        selectedPlayerUuid = null;
    }

    private void addAllOnline() {
        if (this.client == null || this.client.getNetworkHandler() == null) return;
        for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            UUID uuid = entry.getProfile().getId();
            TierSpoofer.getSpoofedPlayers().putIfAbsent(uuid, new SpoofedPlayer(uuid, entry.getProfile().getName()));
        }
        TierSpoofer.saveConfig();
    }

    private UUID resolveUuid(String name) {
        if (this.client != null && this.client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile().getName().equalsIgnoreCase(name)) {
                    return entry.getProfile().getId();
                }
            }
        }
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // background is drawn by renderWithTooltip already, calling it again crashes (blur)
        super.render(context, mouseX, mouseY, delta);
        renderSwatches(context, mouseX, mouseY);
        renderPreview(context);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§b§lTier§f§lSpoofer"), this.width / 2, 10, 0xFFFFFFFF);

        TierSpooferConfig config = TierSpoofer.getConfig();
        if (config.isShowPlayerList()) {
            renderPlayerList(context, mouseX, mouseY);
        }

        // push dropdowns in front of everything else
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);
        if (tierDropdownOpen) {
            renderDropdown(context, tierDropdownButton, TIERS, tierDropdownScroll, mouseX, mouseY, true);
        }
        if (modeDropdownOpen) {
            renderDropdown(context, modeDropdownButton, modeOptions(), modeDropdownScroll, mouseX, mouseY, false);
        }
        context.getMatrices().pop();
    }

    private void renderSwatches(DrawContext context, int mouseX, int mouseY) {
        for (int i = 0; i < SWATCHES.length; i++) {
            int x = swatchX + i * (SWATCH_SIZE + SWATCH_GAP);
            NameColor color = NameColor.parse(SWATCHES[i]);
            boolean hovered = mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= swatchY && mouseY < swatchY + SWATCH_SIZE;
            boolean chosen = SWATCHES[i].equalsIgnoreCase(colorField.getText().trim());
            context.fill(x - 1, swatchY - 1, x + SWATCH_SIZE + 1, swatchY + SWATCH_SIZE + 1,
                    chosen ? 0xFFFFFFFF : (hovered ? 0xFFAAAAAA : 0xFF000000));
            for (int px = 0; px < SWATCH_SIZE; px++) {
                int rgb = color.colorAt(px, SWATCH_SIZE);
                context.fill(x + px, swatchY, x + px + 1, swatchY + SWATCH_SIZE, 0xFF000000 | rgb);
            }
        }
    }

    private boolean handleSwatchClick(double mouseX, double mouseY) {
        if (mouseY < swatchY || mouseY >= swatchY + SWATCH_SIZE || mouseX < swatchX) return false;
        int i = (int) ((mouseX - swatchX) / (SWATCH_SIZE + SWATCH_GAP));
        int x = swatchX + i * (SWATCH_SIZE + SWATCH_GAP);
        if (i < 0 || i >= SWATCHES.length || mouseX >= x + SWATCH_SIZE) return false;
        colorField.setText(SWATCHES[i]);
        return true;
    }

    private void renderPreview(DrawContext context) {
        String realName = nameField.getText().trim();
        SpoofedPlayer preview = new SpoofedPlayer(null, realName.isEmpty() ? "Player" : realName);
        preview.setTierList(selectedList);
        String fake = spoofNameField.getText().trim();
        preview.setSpoofedName(fake.isEmpty() ? null : fake);
        String color = colorField.getText().trim();
        preview.setNameColor(NameColor.isValid(color) ? color : null);

        MutableText line = Text.literal("Preview: ").styled(st -> st.withColor(0x888888));
        if (!selectedTier.equals("None")) {
            TierList.Mode mode = selectedList.getMode(selectedMode);
            String gamemode = mode != null ? mode.key() : selectedList.getModes().keySet().iterator().next();
            line.append(TierSpoofer.createTierText(selectedTier, selectedList, gamemode,
                    TierSpoofer.getConfig().isShowIcons()));
            line.append(Text.literal(" | ").styled(st -> st.withColor(0xAAAAAA)));
        }
        line.append(TierSpoofer.buildStyledName(preview));
        if (!color.isEmpty() && !NameColor.isValid(color)) {
            line.append(Text.literal("  (bad color: use #RRGGBB)").styled(st -> st.withColor(0xFF5555)));
        }
        context.drawTextWithShadow(this.textRenderer, line, this.width / 2 - 160, previewY, 0xFFFFFFFF);
    }

    private void renderPlayerList(DrawContext context, int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int left = centerX - 160;
        int rowHeight = 14;
        int visibleRows = Math.max(1, (this.height - listY - 10) / rowHeight);
        List<SpoofedPlayer> players = sortedPlayers();

        int index = 0;
        for (SpoofedPlayer player : players) {
            if (index < listScroll) { index++; continue; }
            int rowIndex = index - listScroll;
            if (rowIndex >= visibleRows) break;
            int rowY = listY + rowIndex * rowHeight;

            boolean selected = player.getUuid().equals(selectedPlayerUuid);
            boolean hasTier = player.getDisplayTier() != null && !player.getDisplayTier().isEmpty();
            int bgColor = selected ? 0xAA1E1E2E : (hasTier ? 0x80101820 : 0x80101010);
            context.fill(left, rowY, left + 320, rowY + rowHeight, bgColor);

            Text line;
            if (hasTier) {
                Text tierText = TierSpoofer.createTierText(player.getDisplayTier(), player.getTierList(),
                        player.getGamemode(), TierSpoofer.getConfig().isShowIcons());
                line = tierText.copy().append(Text.literal(" " + player.getOriginalName()));
            } else {
                line = Text.literal(player.getOriginalName());
            }
            if (player.changesName()) {
                line = line.copy().append(Text.literal(" \u2192 ").styled(st -> st.withColor(0xAAAAAA)))
                        .append(TierSpoofer.buildStyledName(player));
            }
            context.drawTextWithShadow(this.textRenderer, line, left + 4, rowY + 3, 0xFFFFFFFF);
            index++;
        }
    }

    private void renderDropdown(DrawContext context, ButtonWidget anchor, String[] options,
                                 int scroll, int mouseX, int mouseY, boolean isTierDropdown) {
        int x = anchor.getX();
        int y = anchor.getY() + anchor.getHeight();
        int width = isTierDropdown ? anchor.getWidth() : MODE_DROPDOWN_WIDTH;
        int rowHeight = 16;
        int maxVisible = 8;
        int visible = Math.min(maxVisible, options.length);

        context.fill(x, y, x + width, y + visible * rowHeight, 0xF0101010);
        for (int i = 0; i < visible; i++) {
            int optIndex = i + scroll;
            if (optIndex >= options.length) break;
            String option = options[optIndex];
            int rowY = y + i * rowHeight;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + rowHeight;
            if (hovered) {
                context.fill(x, rowY, x + width, rowY + rowHeight, 0x80FFFFFF);
            }
            int color = isTierDropdown ? (selectedList.getTierColor(option) | 0xFF000000) : 0xFFFFFFFF;
            Text label = Text.literal(option);
            TierList.Mode mode = isTierDropdown ? null : selectedList.getMode(option);
            if (mode != null) {
                label = Text.literal(mode.icon() + " ").append(Text.literal(option));
            }
            context.drawTextWithShadow(this.textRenderer, label, x + 2, rowY + 4, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (tierDropdownOpen) {
            if (handleDropdownClick(tierDropdownButton, TIERS, tierDropdownScroll, mouseX, mouseY, true)) {
                tierDropdownOpen = false;
                return true;
            }
            tierDropdownOpen = false;
        }
        if (modeDropdownOpen) {
            if (handleDropdownClick(modeDropdownButton, modeOptions(), modeDropdownScroll, mouseX, mouseY, false)) {
                modeDropdownOpen = false;
                return true;
            }
            modeDropdownOpen = false;
        }

        if (handleSwatchClick(mouseX, mouseY)) {
            return true;
        }

        if (TierSpoofer.getConfig().isShowPlayerList() && handlePlayerListClick(mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleDropdownClick(ButtonWidget anchor, String[] options, int scroll,
                                         double mouseX, double mouseY, boolean isTierDropdown) {
        int x = anchor.getX();
        int y = anchor.getY() + anchor.getHeight();
        int width = isTierDropdown ? anchor.getWidth() : MODE_DROPDOWN_WIDTH;
        int rowHeight = 16;
        int visible = Math.min(8, options.length);
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + visible * rowHeight) {
            return false;
        }
        int row = (int) ((mouseY - y) / rowHeight);
        int optIndex = row + scroll;
        if (optIndex < 0 || optIndex >= options.length) return false;
        String chosen = options[optIndex];
        if (isTierDropdown) {
            selectedTier = chosen;
            tierDropdownButton.setMessage(Text.literal(chosen));
        } else {
            selectedMode = chosen;
            modeDropdownButton.setMessage(Text.literal(chosen.equals("None") ? "Mode" : chosen));
        }
        return true;
    }

    private boolean handlePlayerListClick(double mouseX, double mouseY) {
        int centerX = this.width / 2;
        int left = centerX - 160;
        int rowHeight = 14;
        int visibleRows = Math.max(1, (this.height - listY - 10) / rowHeight);
        if (mouseX < left || mouseX > left + 320 || mouseY < listY) return false;

        List<SpoofedPlayer> players = sortedPlayers();
        int row = (int) ((mouseY - listY) / rowHeight);
        int index = row + listScroll;
        if (row < 0 || row >= visibleRows || index >= players.size()) return false;

        SpoofedPlayer player = players.get(index);
        selectedPlayerUuid = player.getUuid();
        nameField.setText(player.getOriginalName());
        spoofNameField.setText(player.getSpoofedName() != null ? player.getSpoofedName() : "");
        colorField.setText(player.getNameColor() != null ? player.getNameColor() : "");
        selectedTier = player.getDisplayTier() != null ? player.getDisplayTier() : "None";
        tierDropdownButton.setMessage(Text.literal(selectedTier));
        selectedList = player.getTierList();
        listButton.setMessage(Text.literal(selectedList.displayName));
        TierList.Mode mode = selectedList.getMode(player.getGamemode());
        selectedMode = mode != null ? mode.label() : "None";
        modeDropdownButton.setMessage(Text.literal(selectedMode.equals("None") ? "Mode" : selectedMode));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tierDropdownOpen) {
            tierDropdownScroll = Math.max(0, Math.min(TIERS.length - 8,
                    tierDropdownScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        if (modeDropdownOpen) {
            modeDropdownScroll = Math.max(0, Math.min(modeOptions().length - 8,
                    modeDropdownScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        if (TierSpoofer.getConfig().isShowPlayerList()) {
            int total = TierSpoofer.getSpoofedPlayers().size();
            int rowHeight = 14;
            int visibleRows = Math.max(1, (this.height - listY - 10) / rowHeight);
            listScroll = Math.max(0, Math.min(Math.max(0, total - visibleRows),
                    listScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
