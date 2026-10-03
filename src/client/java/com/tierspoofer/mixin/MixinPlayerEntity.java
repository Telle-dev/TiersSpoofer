// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerEntity.class, priority = 2000)
public class MixinPlayerEntity {
    // same kind of hook TierTagger uses, so with the higher priority ours runs after theirs and drops their tag
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text tierspoofer$displayName(Text original) {
        try {
            PlayerEntity self = (PlayerEntity) (Object) this;
            TierSpooferConfig config = TierSpoofer.getConfig();
            if (config != null && config.isEnabled() && config.isShowInWorld()) {
                Text modified = TierSpoofer.getDisplayName(self.getUuid(), self.getGameProfile().name(), original);
                if (modified != null) return modified;
            }
        } catch (Exception ignored) {
        }
        return original;
    }

    // only players in our world, the singleplayer server keeps real names
    @ModifyReturnValue(method = "getName", at = @At("RETURN"), require = 0)
    private Text tierspoofer$name(Text original) {
        if (!((Object) this instanceof AbstractClientPlayerEntity self)) return original;
        try {
            return TierSpoofer.spoofEntityName(self.getUuid(), original);
        } catch (Exception e) {
            return original;
        }
    }
}
