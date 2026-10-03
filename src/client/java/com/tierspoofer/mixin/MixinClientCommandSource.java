// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.tierspoofer.FakeNameSuggestions;
import com.tierspoofer.TierSpoofer;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

// names offered when you press tab in normal chat
@Mixin(ClientCommandSource.class)
public class MixinClientCommandSource {
    @Inject(method = "getChatSuggestions", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$fakeNames(CallbackInfoReturnable<Collection<String>> cir) {
        cir.setReturnValue(FakeNameSuggestions.withFakeNames(cir.getReturnValue()));
    }

    // commands need real names, the fake ones get added above if Cmds is on
    @WrapOperation(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/PlayerListEntry;getProfile()Lcom/mojang/authlib/GameProfile;"), require = 0)
    private GameProfile tierspoofer$realProfile(PlayerListEntry entry, Operation<GameProfile> original) {
        return TierSpoofer.realProfile(entry);
    }
}
