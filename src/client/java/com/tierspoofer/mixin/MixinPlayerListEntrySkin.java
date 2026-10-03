// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.mojang.authlib.GameProfile;
import com.tierspoofer.SkinSwap;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntrySkin {
    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$skin(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            GameProfile profile = getProfile();
            if (profile == null) return;
            SkinTextures swapped = SkinSwap.apply(profile.id(), profile.name(), cir.getReturnValue());
            if (swapped != cir.getReturnValue()) {
                cir.setReturnValue(swapped);
            }
        } catch (Exception ignored) {
        }
    }
}
