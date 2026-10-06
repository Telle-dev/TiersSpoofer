// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.test.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tierspoofer.test.FakeTierTagger;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListHud.class)
public class FakeTierTaggerTab {
    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text fakeTierTagger(Text original, PlayerListEntry entry) {
        return FakeTierTagger.prepend(original);
    }
}
