package com.tierspoofer.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.model.SpoofedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.screen.ChatInputSuggestor$SuggestionWindow")
public class MixinSuggestionWindow {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Suggestions modifySuggestions(Suggestions suggestions) {
        if (!TierSpoofer.getConfig().isEnabled() || suggestions == null) {
            return suggestions;
        }
        try {
            List<Suggestion> modifiedList = new ArrayList<>();
            boolean anyModified = false;
            for (Suggestion suggestion : suggestions.getList()) {
                String text = suggestion.getText();
                String modifiedText = text;
                for (SpoofedPlayer player : TierSpoofer.getSpoofedPlayers().values()) {
                    if (player.getSpoofedName() == null || player.getSpoofedName().isEmpty()
                            || !text.equalsIgnoreCase(player.getOriginalName())) continue;
                    // Same rule as MixinChatInputSuggestor: this becomes the
                    // literal text inserted into the chat box if the
                    // suggestion is accepted, so it must be the plain,
                    // code-free name, never the raw '&'-coded spoofedName.
                    modifiedText = player.getSkinTargetName() != null
                            ? player.getSkinTargetName()
                            : player.getSpoofedName();
                    anyModified = true;
                    break;
                }
                modifiedList.add(new Suggestion(suggestion.getRange(), modifiedText, suggestion.getTooltip()));
            }
            if (anyModified) {
                return new Suggestions(suggestions.getRange(), modifiedList);
            }
        } catch (Exception ignored) {
        }
        return suggestions;
    }
}