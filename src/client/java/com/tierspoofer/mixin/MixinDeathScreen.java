package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class MixinDeathScreen {
    @Shadow
    @Final
    @Mutable
    private Text message;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void tierspoofer$message(CallbackInfo ci) {
        this.message = TierSpoofer.replaceNamesInText(this.message);
    }
}
