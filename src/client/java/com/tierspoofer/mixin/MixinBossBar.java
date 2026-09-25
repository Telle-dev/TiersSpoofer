package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BossBar.class)
public class MixinBossBar {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$name(CallbackInfoReturnable<Text> cir) {
        // only the bars we draw, not the ones a singleplayer server keeps
        if ((Object) this instanceof ClientBossBar) {
            cir.setReturnValue(TierSpoofer.replaceNamesInText(cir.getReturnValue()));
        }
    }
}
