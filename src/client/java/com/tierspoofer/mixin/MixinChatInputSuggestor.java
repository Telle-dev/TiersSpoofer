package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.suggestion.Suggestions;
import com.tierspoofer.FakeNameSuggestions;
import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class MixinChatInputSuggestor {
    @Shadow
    @Final
    TextFieldWidget textField;

    // ModifyExpressionValue instead of Redirect, so other chat mods hooking the same call don't clash
    @ModifyExpressionValue(
            method = "refresh",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private CompletableFuture<Suggestions> tierspoofer$fakeNameSuggestions(CompletableFuture<Suggestions> original) {
        if (!TierSpoofer.getConfig().isEnabled() || !TierSpoofer.getConfig().isCommandNames()) return original;
        String text = this.textField.getText();
        int cursor = this.textField.getCursor();
        return original.thenApply(suggestions -> {
            try {
                return FakeNameSuggestions.add(suggestions, text, cursor);
            } catch (Exception e) {
                return suggestions;
            }
        });
    }
}
