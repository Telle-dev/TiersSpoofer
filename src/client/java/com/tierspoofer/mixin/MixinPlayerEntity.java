// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tierspoofer.TierSpoofer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerEntity.class, priority = 2000)
public class MixinPlayerEntity {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text tierspoofer$displayName(Text original) {
        if (!TierSpoofer.getConfig().isShowInWorld()) return original;
        PlayerEntity self = (PlayerEntity) (Object) this;
        return TierSpoofer.getDisplayName(self.getUuid(), self.getGameProfile().getName(), original);
    }

    @ModifyReturnValue(method = "getName", at = @At("RETURN"), require = 0)
    private Text tierspoofer$name(Text original) {
        if (!((Object) this instanceof AbstractClientPlayerEntity self)) return original;
        return TierSpoofer.spoofEntityName(self.getUuid(), original);
    }
}
