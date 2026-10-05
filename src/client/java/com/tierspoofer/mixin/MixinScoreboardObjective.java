// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// sidebar / below name title
@Mixin(ScoreboardObjective.class)
public class MixinScoreboardObjective {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$title(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(TierSpoofer.replaceNamesOnClient(cir.getReturnValue()));
    }
}
