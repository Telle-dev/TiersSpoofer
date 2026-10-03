// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.tierspoofer.SkinSwap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void tierspoofer$skin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        cir.setReturnValue(SkinSwap.apply(self.getUuid(), self.getGameProfile().name(), cir.getReturnValue()));
    }
}
