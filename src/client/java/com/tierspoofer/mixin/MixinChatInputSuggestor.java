package com.tierspoofer.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.model.SpoofedPlayer;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class MixinChatInputSuggestor {
    @Redirect(
            method = "refresh",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private CompletableFuture<Suggestions> redirectGetCompletionSuggestions(
            CommandDispatcher dispatcher, ParseResults parse, int cursor) {
        CompletableFuture<Suggestions> original = dispatcher.getCompletionSuggestions(parse, cursor);
        return original.thenApply(suggestions -> {
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
                        // Brigadier suggestion text becomes the literal
                        // characters typed into the chat box when accepted —
                        // it must always be the plain, code-free name, never
                        // the raw '&'-coded spoofedName.
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
        });
    }
}