// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.config;

import com.mojang.authlib.GameProfile;
import com.tierspoofer.NameColor;
import com.tierspoofer.SkinCache;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.minecraft.client.gui.Click;
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
    private static final int FORM_WIDTH = 320;
    private static final int BUTTON_HEIGHT = 18;
    private static final int ROW = 20;
    private static final int GAP = 4;
    private static final int LIST_ROW_HEIGHT = 14;

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

    public TierSpooferConfigScreen(Screen parent) {
        super(Text.literal("TierSpoofer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TierSpooferConfig config = TierSpoofer.getConfig();
        int left = this.width / 2 - FORM_WIDTH / 2;
        int y = 22;

        // four equal columns for the toggles
        int col = (FORM_WIDTH - 3 * GAP) / 4;
        tip(toggle("Mod", config::isEnabled, config::setEnabled, left, y, col), "Turns the whole mod on or off.");
        tip(toggle("Tab", config::isShowInTabList, config::setShowInTabList, left + (col + GAP), y, col),
                "Shows the fake tier of the people you added in the tab list. Their fake name shows either way.");
        tip(toggle("Skin", config::isSkinEnabled, config::setSkinEnabled, left + 2 * (col + GAP), y, col), "Gives people the skin of their fake name.");
        tip(toggle("Cape", config::isCapeEnabled, config::setCapeEnabled, left + 3 * (col + GAP), y, col), "Takes the fake name's cape too. Off keeps their own cape.");

        y += ROW;
        tip(toggle("Cmds", config::isCommandNames, config::setCommandNames, left, y, col),
                "Fake names show up when you tab-complete commands, and get swapped back to the real name when you send it. /tpa k1rbe goes out as /tpa Steve.");
        tip(toggle("Mods", config::isSpoofForMods, config::setSpoofForMods, left + (col + GAP), y, col),
                "Other client mods (tab mods, HUDs, minimaps) get the fake name too. Turn it off if one of them acts weird.");
        tip(toggle("Icons", config::isShowIcons, config::setShowIcons, left + 2 * (col + GAP), y, col), "Shows the gamemode icon in front of the tier.");
        tip(toggle("Own Tag", config::isShowOwnNametag, config::setShowOwnNametag, left + 3 * (col + GAP), y, col), "Shows your own nametag in F5.");

        y += ROW;
        int realWidth = 62;
        int sideWidth = (FORM_WIDTH - realWidth - 3 * GAP) / 3;
        tip(toggle("Real", config::isRealTiers, config::setRealTiers, left, y, realWidth),
                "Shows everyone's best real tier above their head, from every list that isn't Off. People you added keep their fake ones.");
        int sideX = left + realWidth + GAP;
        for (TierList list : TierList.values()) {
            tip(this.addDrawableChild(ButtonWidget.builder(sideLabel(list), btn -> {
                config.setSide(list, config.getSide(list).next());
                btn.setMessage(sideLabel(list));
                TierSpoofer.saveConfig();
            }).dimensions(sideX, y, sideWidth, BUTTON_HEIGHT).build()),
                    "Where " + list.displayName + " tags go: left of the name, right of it, or hidden.");
            sideX += sideWidth + GAP;
        }

        y += ROW + 4;
        int field = (FORM_WIDTH - 2 * GAP) / 3;
        nameField = new TextFieldWidget(this.textRenderer, left, y, field, BUTTON_HEIGHT, Text.literal("Player Name"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Player"));
        tip(nameField, "Real name of the player you want to spoof.");
        this.addDrawableChild(nameField);

        spoofNameField = new TextFieldWidget(this.textRenderer, left + field + GAP, y, field, BUTTON_HEIGHT, Text.literal("Fake Name"));
        spoofNameField.setMaxLength(64);
        spoofNameField.setPlaceholder(Text.literal("Fake name"));
        tip(spoofNameField, "Name shown instead of their real one. Color codes like &c or &#FF5555 work. The skin comes from this name too.");
        this.addDrawableChild(spoofNameField);

        colorField = new TextFieldWidget(this.textRenderer, left + 2 * (field + GAP), y, field, BUTTON_HEIGHT, Text.literal("Name Color"));
        colorField.setMaxLength(64);
        colorField.setPlaceholder(Text.literal("Color (#hex)"));
        colorField.setChangedListener(text -> colorField.setEditableColor(
                text.isBlank() || NameColor.isValid(text) ? 0xFFE0E0E0 : 0xFFFF5555));
        tip(colorField, "Name color: #FF5555, a gradient like #FF0000-#0000FF, or rainbow. Or click a color below.");
        this.addDrawableChild(colorField);

        y += ROW;
        swatchX = left;
        swatchY = y;

        y += SWATCH_SIZE + 6;
        listButton = ButtonWidget.builder(Text.literal(selectedList.displayName), btn -> {
            selectedList = selectedList.next();
            btn.setMessage(Text.literal(selectedList.displayName));
            SpoofedPlayer selected = selectedPlayerUuid == null ? null : TierSpoofer.getSpoofedPlayer(selectedPlayerUuid);
            if (selected != null) {
                loadTier(selected.getTier(selectedList));
            } else {
                TierList.Mode mode = selectedList.getMode(selectedMode);
                setMode(mode != null ? mode.label() : "None");
            }
            modeDropdownScroll = 0;
        }).dimensions(left, y, 76, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Which tier list you're setting a tier for. Each list can have its own fake tier."))).build();
        this.addDrawableChild(listButton);

        tierDropdownButton = ButtonWidget.builder(
                Text.literal(selectedTier),
                btn -> { tierDropdownOpen = !tierDropdownOpen; modeDropdownOpen = false; }
        ).dimensions(left + 80, y, 46, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("The fake tier on that list. None removes it."))).build();
        this.addDrawableChild(tierDropdownButton);

        modeDropdownButton = ButtonWidget.builder(
                Text.literal(selectedMode.equals("None") ? "Mode" : selectedMode),
                btn -> { modeDropdownOpen = !modeDropdownOpen; tierDropdownOpen = false; }
        ).dimensions(left + 130, y, 66, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Gamemode, decides the icon in front of the tier."))).build();
        this.addDrawableChild(modeDropdownButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> addPlayer())
                .dimensions(left + 200, y, 64, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Adds the player with what's set here. Adding the same name again updates it."))).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), btn -> deletePlayer())
                .dimensions(left + 268, y, FORM_WIDTH - 268, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Removes the player you clicked in the list."))).build());

        y += BUTTON_HEIGHT + 4;
        previewY = y;

        y += 14;
        int third = (FORM_WIDTH - 2 * GAP) / 3;
        tip(toggle("List", config::isShowPlayerList, config::setShowPlayerList, left, y, third),
                "Shows everyone you added below. Click someone to load them into the boxes.");
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add online"), btn -> addAllOnline())
                .dimensions(left + third + GAP, y, third, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Adds everyone who's online right now."))).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), btn -> clearAll())
                .dimensions(left + 2 * (third + GAP), y, third, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Removes everyone."))).build());

        y += ROW + 2;
        listY = y;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 24, 100, 20)
                .tooltip(Tooltip.of(Text.literal("Closes the menu. Everything is saved already."))).build());
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
        }).dimensions(x, y, width, BUTTON_HEIGHT).build());
    }

    private static Text onOff(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    // short names, the full ones don't fit three to a row
    private static Text sideLabel(TierList list) {
        String name = switch (list) {
            case MCTIERS -> "MC";
            case PVPTIERS -> "PvP";
            case SUBTIERS -> "Sub";
        };
        return Text.literal(name + ": " + TierSpoofer.getConfig().getSide(list).label);
    }

    private void loadTier(SpoofedPlayer.FakeTier fake) {
        selectedTier = fake != null ? fake.tier() : "None";
        tierDropdownButton.setMessage(Text.literal(selectedTier));
        TierList.Mode mode = fake == null ? null : selectedList.getMode(fake.gamemode());
        setMode(mode != null ? mode.label() : "None");
    }

    private void setMode(String mode) {
        selectedMode = mode;
        modeDropdownButton.setMessage(Text.literal(mode.equals("None") ? "Mode" : mode));
    }

    private String formGamemode() {
        TierList.Mode mode = selectedList.getMode(selectedMode);
        if (mode != null) return mode.key();
        return selectedList == TierList.MCTIERS ? "vanilla" : selectedList.getModes().keySet().iterator().next();
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

    private void applyFormToPlayer(SpoofedPlayer player) {
        player.setTier(selectedList, selectedTier.equals("None") ? null : selectedTier, formGamemode());
        String color = colorField.getText().trim();
        player.setNameColor(NameColor.isValid(color) ? color : null);
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
            GameProfile profile = TierSpoofer.realProfile(entry);
            TierSpoofer.getSpoofedPlayers().putIfAbsent(profile.id(), new SpoofedPlayer(profile.id(), profile.name()));
        }
        TierSpoofer.saveConfig();
    }

    private UUID resolveUuid(String name) {
        if (this.client != null && this.client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
                GameProfile profile = TierSpoofer.realProfile(entry);
                if (profile.name().equalsIgnoreCase(name)) {
                    return profile.id();
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
                Text.literal("TierSpoofer"), this.width / 2, 8, 0xFFFFFFFF);

        TierSpooferConfig config = TierSpoofer.getConfig();
        if (config.isShowPlayerList()) {
            renderPlayerList(context, mouseX, mouseY);
        }

        if (tierDropdownOpen || modeDropdownOpen) {
            context.createNewRootLayer();
        }
        if (tierDropdownOpen) {
            renderDropdown(context, tierDropdownButton, TIERS, tierDropdownScroll, mouseX, mouseY, true);
        }
        if (modeDropdownOpen) {
            renderDropdown(context, modeDropdownButton, modeOptions(), modeDropdownScroll, mouseX, mouseY, false);
        }
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
        // the tiers they already have on other lists, plus what's in the form
        SpoofedPlayer selected = selectedPlayerUuid == null ? null : TierSpoofer.getSpoofedPlayer(selectedPlayerUuid);
        if (selected != null && realName.equalsIgnoreCase(selected.getOriginalName())) {
            for (TierList list : TierList.values()) {
                SpoofedPlayer.FakeTier fake = selected.getTier(list);
                if (fake != null) preview.setTier(list, fake.tier(), fake.gamemode());
            }
        }
        preview.setTier(selectedList, selectedTier.equals("None") ? null : selectedTier, formGamemode());
        String fake = spoofNameField.getText().trim();
        preview.setSpoofedName(fake.isEmpty() ? null : fake);
        String color = colorField.getText().trim();
        preview.setNameColor(NameColor.isValid(color) ? color : null);

        MutableText line = Text.literal("Preview: ").styled(st -> st.withColor(0x888888));
        line.append(TierSpoofer.withFakeTags(preview, TierSpoofer.buildStyledName(preview)));
        if (!color.isEmpty() && !NameColor.isValid(color)) {
            line.append(Text.literal("  (bad color: use #RRGGBB)").styled(st -> st.withColor(0xFF5555)));
        }
        context.drawTextWithShadow(this.textRenderer, line, this.width / 2 - FORM_WIDTH / 2, previewY, 0xFFFFFFFF);
    }

    // rows that fit between the form and the Done button
    private int visibleListRows() {
        return Math.max(0, (this.height - 28 - listY) / LIST_ROW_HEIGHT);
    }

    private void renderPlayerList(DrawContext context, int mouseX, int mouseY) {
        int left = this.width / 2 - FORM_WIDTH / 2;
        int rowHeight = LIST_ROW_HEIGHT;
        int visibleRows = visibleListRows();
        List<SpoofedPlayer> players = sortedPlayers();

        int index = 0;
        for (SpoofedPlayer player : players) {
            if (index < listScroll) { index++; continue; }
            int rowIndex = index - listScroll;
            if (rowIndex >= visibleRows) break;
            int rowY = listY + rowIndex * rowHeight;

            boolean selected = player.getUuid().equals(selectedPlayerUuid);
            int bgColor = selected ? 0xAA1E1E2E : (player.hasFakeTier() ? 0x80101820 : 0x80101010);
            context.fill(left, rowY, left + FORM_WIDTH, rowY + rowHeight, bgColor);

            Text line = TierSpoofer.withFakeTags(player, Text.literal(player.getOriginalName()));
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
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = clickX(click);
        double mouseY = clickY(click);

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

        return super.mouseClicked(click, doubled);
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
            setMode(chosen);
        }
        return true;
    }

    private boolean handlePlayerListClick(double mouseX, double mouseY) {
        int left = this.width / 2 - FORM_WIDTH / 2;
        int rowHeight = LIST_ROW_HEIGHT;
        int visibleRows = visibleListRows();
        if (mouseX < left || mouseX > left + FORM_WIDTH || mouseY < listY) return false;

        List<SpoofedPlayer> players = sortedPlayers();
        int row = (int) ((mouseY - listY) / rowHeight);
        int index = row + listScroll;
        if (row < 0 || row >= visibleRows || index >= players.size()) return false;

        SpoofedPlayer player = players.get(index);
        selectedPlayerUuid = player.getUuid();
        nameField.setText(player.getOriginalName());
        spoofNameField.setText(player.getSpoofedName() != null ? player.getSpoofedName() : "");
        colorField.setText(player.getNameColor() != null ? player.getNameColor() : "");
        // stay on the current list if they have a tier there, otherwise jump to one they have
        if (player.getTier(selectedList) == null) {
            for (TierList list : TierList.values()) {
                if (player.getTier(list) != null) {
                    selectedList = list;
                    break;
                }
            }
        }
        listButton.setMessage(Text.literal(selectedList.displayName));
        loadTier(player.getTier(selectedList));
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
            int visibleRows = visibleListRows();
            listScroll = Math.max(0, Math.min(Math.max(0, total - visibleRows),
                    listScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private double clickX(Click click) {
        if (this.client == null) return 0;
        double scale = (double) this.client.getWindow().getScaledWidth() / this.client.getWindow().getWidth();
        return this.client.mouse.getX() * scale;
    }

    private double clickY(Click click) {
        if (this.client == null) return 0;
        double scale = (double) this.client.getWindow().getScaledHeight() / this.client.getWindow().getHeight();
        return this.client.mouse.getY() * scale;
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
