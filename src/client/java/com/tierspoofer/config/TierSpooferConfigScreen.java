package com.tierspoofer.config;

import com.tierspoofer.NameColor;
import com.tierspoofer.SkinCache;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.model.SpoofedPlayer;
import com.tierspoofer.model.TierList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    /** Quick-pick colors under the Name Color field. */
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

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Mod: " + (config.isEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setEnabled(!config.isEnabled());
                    btn.setMessage(Text.literal("Mod: " + (config.isEnabled() ? "ON" : "OFF")));
                    TierSpoofer.saveConfig();
                }
        ).dimensions(centerX - 125, y, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("List: " + (config.isShowPlayerList() ? "ON" : "OFF")),
                btn -> {
                    config.setShowPlayerList(!config.isShowPlayerList());
                    btn.setMessage(Text.literal("List: " + (config.isShowPlayerList() ? "ON" : "OFF")));
                    TierSpoofer.saveConfig();
                }
        ).dimensions(centerX - 40, y, 80, 20).build());

        // TierTagger-style real tier lookups: Off -> MCTiers -> PvPTiers -> SubTiers -> Off
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
        ).dimensions(centerX + 45, y, 80, 20).build());

        y += 24;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Skin: " + (config.isSkinEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setSkinEnabled(!config.isSkinEnabled());
                    btn.setMessage(Text.literal("Skin: " + (config.isSkinEnabled() ? "ON" : "OFF")));
                    TierSpoofer.saveConfig();
                }
        ).dimensions(centerX - 130, y, 60, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cape: " + (config.isCapeEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setCapeEnabled(!config.isCapeEnabled());
                    btn.setMessage(Text.literal("Cape: " + (config.isCapeEnabled() ? "ON" : "OFF")));
                    TierSpoofer.saveConfig();
                }
        ).dimensions(centerX - 65, y, 60, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Icons: " + (config.isShowIcons() ? "ON" : "OFF")),
                btn -> {
                    config.setShowIcons(!config.isShowIcons());
                    btn.setMessage(Text.literal("Icons: " + (config.isShowIcons() ? "ON" : "OFF")));
                    TierSpoofer.saveConfig();
                }
        ).dimensions(centerX, y, 60, 20).build());

        // Which gamemode's real tier to show: Highest, or a specific mode of the real list.
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
        }).dimensions(centerX + 65, y, 70, 20).build();
        this.addDrawableChild(realModeButton);

        y += 26;
        nameField = new TextFieldWidget(this.textRenderer, centerX - 160, y, 100, 18, Text.literal("Player Name"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Player Name"));
        this.addDrawableChild(nameField);

        tierDropdownButton = ButtonWidget.builder(
                Text.literal(selectedTier),
                btn -> { tierDropdownOpen = !tierDropdownOpen; modeDropdownOpen = false; }
        ).dimensions(centerX - 55, y, 50, 18).build();
        this.addDrawableChild(tierDropdownButton);

        modeDropdownButton = ButtonWidget.builder(
                Text.literal(selectedMode.equals("None") ? "Mode" : selectedMode),
                btn -> { modeDropdownOpen = !modeDropdownOpen; tierDropdownOpen = false; }
        ).dimensions(centerX, y, 50, 18).build();
        this.addDrawableChild(modeDropdownButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> addPlayer())
                .dimensions(centerX + 55, y, 40, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(centerX + 100, y, 40, 18).build());

        y += 22;
        spoofNameField = new TextFieldWidget(this.textRenderer, centerX - 160, y, 100, 18, Text.literal("Fake Name"));
        // 64 instead of 16: the field now stores raw '&'-coded input
        // (e.g. "&#ffaa00K1&#555555RBE"), which can be much longer than the
        // 16-character visible name once color/format codes are included.
        spoofNameField.setMaxLength(64);
        spoofNameField.setPlaceholder(Text.literal("Fake Name (supports &codes)"));
        this.addDrawableChild(spoofNameField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), btn -> editPlayer())
                .dimensions(centerX - 55, y, 40, 18).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Del"), btn -> deletePlayer())
                .dimensions(centerX - 10, y, 35, 18).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), btn -> clearAll())
                .dimensions(centerX + 30, y, 40, 18).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("All"), btn -> addAllOnline())
                .dimensions(centerX + 75, y, 35, 18).build());

        // Tier list the new/edited entry is displayed as (icons + colors).
        listButton = ButtonWidget.builder(Text.literal(selectedList.displayName), btn -> {
            selectedList = selectedList.next();
            btn.setMessage(Text.literal(selectedList.displayName));
            // Keep the chosen gamemode if the new list has an equivalent, else reset it.
            TierList.Mode mode = selectedList.getMode(selectedMode);
            selectedMode = mode != null ? mode.label() : "None";
            modeDropdownButton.setMessage(Text.literal(selectedMode.equals("None") ? "Mode" : selectedMode));
            modeDropdownScroll = 0;
        }).dimensions(centerX + 115, y, 60, 18).build();
        this.addDrawableChild(listButton);

        y += 22;
        colorField = new TextFieldWidget(this.textRenderer, centerX - 160, y, 100, 18, Text.literal("Name Color"));
        colorField.setMaxLength(64);
        colorField.setPlaceholder(Text.literal("Name Color (#hex)"));
        colorField.setChangedListener(text -> colorField.setEditableColor(
                text.isBlank() || NameColor.isValid(text) ? 0xFFE0E0E0 : 0xFFFF5555));
        this.addDrawableChild(colorField);
        swatchX = centerX - 55;
        swatchY = y + 3;

        y += 24;
        previewY = y;

        y += 18;
        listY = y;
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
        // Adding a name that's already in the list updates that entry instead of duplicating it.
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
        // "None" keeps the old default of vanilla (no matching icon on PvPTiers/SubTiers,
        // so those fall back to the list's first mode instead).
        String fallback = selectedList == TierList.MCTIERS
                ? "vanilla" : selectedList.getModes().keySet().iterator().next();
        player.setGamemode(mode != null ? mode.key() : fallback);
        String spoofName = spoofNameField.getText().trim();
        if (!spoofName.isEmpty()) {
            // setSpoofedName stores the raw (possibly '&'-coded) input and
            // automatically derives the plain skinTargetName from it — do
            // not call setSkinTargetName separately here, and never pass
            // the raw spoofName to SkinCache.
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
        for (UUID uuid : new ArrayList<>(TierSpoofer.getSpoofedPlayers().keySet())) {
            TierSpoofer.removeSpoofedPlayer(uuid);
        }
        selectedPlayerUuid = null;
    }

    private void addAllOnline() {
        if (this.client == null || this.client.getNetworkHandler() == null) return;
        for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            UUID uuid = entry.getProfile().id();
            if (!TierSpoofer.isPlayerSpoofed(uuid)) {
                TierSpoofer.addSpoofedPlayer(new SpoofedPlayer(uuid, entry.getProfile().name()));
            }
        }
    }

    private UUID resolveUuid(String name) {
        if (this.client != null && this.client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile().name().equalsIgnoreCase(name)) {
                    return entry.getProfile().id();
                }
            }
        }
        // Not online right now: use a placeholder. The entry is matched by name and
        // switched to the real UUID as soon as the player shows up (see
        // TierSpoofer.findSpoofedPlayer).
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // The background (with blur) is already drawn by Screen#renderWithTooltip
        // on 1.21.2+; drawing it again here throws "Can only blur once per frame".
        super.render(context, mouseX, mouseY, delta);
        renderSwatches(context, mouseX, mouseY);
        renderPreview(context);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§b§lTier§f§lSpoofer"), this.width / 2, 10, 0xFFFFFFFF);

        TierSpooferConfig config = TierSpoofer.getConfig();
        if (config.isShowPlayerList()) {
            renderPlayerList(context, mouseX, mouseY);
        }

        if (tierDropdownOpen || modeDropdownOpen) {
            // Draw dropdowns on a new layer so they cover the text underneath.
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
            // Gradients are drawn as vertical stripes.
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

    /** Live preview of how the entry being edited will look in tab / above the head. */
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
        List<SpoofedPlayer> players = new ArrayList<>(TierSpoofer.getSpoofedPlayers().values());

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

        List<SpoofedPlayer> players = new ArrayList<>(TierSpoofer.getSpoofedPlayers().values());
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

    /**
     * 1.21.11 helper: Click no longer carries raw doubles in the same way the
     * old (double,double,int) triple did. We derive screen-space coordinates
     * from the live mouse handler, which mirrors what vanilla widgets do
     * internally and avoids depending on an unverified Click accessor name.
     */
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
    public boolean shouldPause() {
        return false;
    }
}