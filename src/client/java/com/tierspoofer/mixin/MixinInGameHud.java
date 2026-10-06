// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private Text tierspoofer$title(Text title) {
        return TierSpoofer.replaceNamesInText(title);
    }

    @ModifyVariable(method = "setSubtitle", at = @At("HEAD"), argsOnly = true)
    private Text tierspoofer$subtitle(Text subtitle) {
        return TierSpoofer.replaceNamesInText(subtitle);
    }

    @ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true)
    private Text tierspoofer$actionBar(Text message) {
        return TierSpoofer.replaceNamesInText(message);
    }
}
