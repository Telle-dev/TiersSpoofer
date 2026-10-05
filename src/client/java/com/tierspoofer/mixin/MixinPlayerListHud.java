// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = PlayerListHud.class, priority = 2000)
public class MixinPlayerListHud {
    // "Welcome Steve" style tab header/footer
    @ModifyVariable(method = "setHeader", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text tierspoofer$header(Text header) {
        return TierSpoofer.replaceNamesInText(header);
    }

    @ModifyVariable(method = "setFooter", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text tierspoofer$footer(Text footer) {
        return TierSpoofer.replaceNamesInText(footer);
    }

    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text tierspoofer$tabName(Text original, PlayerListEntry entry) {
        GameProfile profile = TierSpoofer.realProfile(entry);
        return TierSpoofer.getTabName(profile.getId(), profile.getName(), original);
    }
}
