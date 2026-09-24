package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class MixinChatHud {
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text modifyChatMessage(Text message) {
        try {
            if (message == null) {
                return message;
            }
            return TierSpoofer.replaceNamesInText(message);
        } catch (Exception e) {
            return message;
        }
    }
}