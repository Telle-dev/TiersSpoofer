package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DeathScreen.class)
public class MixinDeathScreen {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Text modifyDeathMessage(Text message) {
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