package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// names on the sidebar
@Mixin(ScoreboardEntry.class)
public class MixinScoreboardEntry {
    @Inject(method = "name", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$name(CallbackInfoReturnable<MutableText> cir) {
        Text name = TierSpoofer.replaceNamesOnClient(cir.getReturnValue());
        cir.setReturnValue(name instanceof MutableText mutable ? mutable : name.copy());
    }

    // custom line text servers send for a score (1.20.3+), in case something reads it directly
    @Inject(method = "display", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$display(CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() != null) cir.setReturnValue(TierSpoofer.replaceNamesOnClient(cir.getReturnValue()));
    }
}
