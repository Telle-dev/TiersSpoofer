// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// titles of server menus, like "Steve's Profile"
@Mixin(HandledScreens.class)
public class MixinHandledScreens {
    @ModifyVariable(method = "open", at = @At("HEAD"), argsOnly = true)
    private static Text tierspoofer$title(Text title) {
        return TierSpoofer.replaceNamesInText(title);
    }
}
