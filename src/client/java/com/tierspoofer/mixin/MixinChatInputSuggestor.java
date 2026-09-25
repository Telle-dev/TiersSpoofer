package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.model.SpoofedPlayer;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class MixinChatInputSuggestor {
    // ModifyExpressionValue instead of Redirect, so other chat mods hooking the same call don't clash
    @ModifyExpressionValue(
            method = "refresh",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private CompletableFuture<Suggestions> tierspoofer$fakeNameSuggestions(CompletableFuture<Suggestions> original) {
        return original.thenApply(suggestions -> {
            if (!TierSpoofer.getConfig().isEnabled() || !TierSpoofer.getConfig().isCommandNames() || suggestions == null) {
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
