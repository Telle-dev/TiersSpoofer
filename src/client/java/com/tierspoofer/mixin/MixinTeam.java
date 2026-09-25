package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// sidebar plugins put most of their lines (and often your name) in team prefixes/suffixes
@Mixin(Team.class)
public class MixinTeam {
    @Inject(method = "getPrefix", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$prefix(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(TierSpoofer.replaceNamesOnClient(cir.getReturnValue()));
    }

    @Inject(method = "getSuffix", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$suffix(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(TierSpoofer.replaceNamesOnClient(cir.getReturnValue()));
    }
}
