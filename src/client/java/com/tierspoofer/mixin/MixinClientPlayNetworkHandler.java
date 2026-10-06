// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.tierspoofer.TierSpoofer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @ModifyVariable(method = "sendChatCommand", at = @At("HEAD"), argsOnly = true)
    private String tierspoofer$chatCommand(String command) {
        return TierSpoofer.toRealNames(command);
    }

    @ModifyVariable(method = "sendCommand", at = @At("HEAD"), argsOnly = true, require = 0)
    private String tierspoofer$command(String command) {
        return TierSpoofer.toRealNames(command);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/PlayerListEntry;getProfile()Lcom/mojang/authlib/GameProfile;"), require = 0)
    private GameProfile tierspoofer$realProfile(PlayerListEntry entry, Operation<GameProfile> original) {
        return TierSpoofer.realProfile(entry);
    }
}
