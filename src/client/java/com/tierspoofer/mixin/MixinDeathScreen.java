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

    // Rewrites the stored death message once the constructor has finished
    // (injecting at HEAD of a constructor, before super(), isn't allowed).
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void onInit(CallbackInfo ci) {
        try {
            if (this.message != null) {
                this.message = TierSpoofer.replaceNamesInText(this.message);
            }
        } catch (Exception ignored) {
        }
    }
}
