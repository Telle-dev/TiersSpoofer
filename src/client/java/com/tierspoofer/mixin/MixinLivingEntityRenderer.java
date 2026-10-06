// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void tierspoofer$ownLabel(LivingEntity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        TierSpooferConfig config = TierSpoofer.getConfig();
        if (!config.isEnabled() || !config.isShowOwnNametag() || !config.isShowInWorld()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || entity != client.player) return;
        if (client.options.getPerspective().isFirstPerson() || client.options.hudHidden) return;
        if (entity.isInvisible() || entity.hasPassengers()) return;
        cir.setReturnValue(true);
    }
}
