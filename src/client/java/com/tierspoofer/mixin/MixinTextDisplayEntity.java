// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DisplayEntity.TextDisplayEntity.class)
public class MixinTextDisplayEntity {
    @Shadow
    private DisplayEntity.TextDisplayEntity.TextLines textLines;

    @Unique
    private int tierspoofer$seenChange = -1;

    @Inject(method = "splitLines", at = @At("HEAD"))
    private void tierspoofer$refresh(DisplayEntity.TextDisplayEntity.LineSplitter splitter,
                                     CallbackInfoReturnable<DisplayEntity.TextDisplayEntity.TextLines> cir) {
        int now = TierSpoofer.getChangeCount();
        if (tierspoofer$seenChange != now) {
            tierspoofer$seenChange = now;
            this.textLines = null;
        }
    }
}
