// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = DisplayEntityRenderer.TextDisplayEntityRenderer.class, priority = 2000)
public class MixinTextDisplayRenderer {
    @ModifyArg(method = "getLines", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;"))
    private StringVisitable tierspoofer$replaceNames(StringVisitable text) {
        return text instanceof Text t ? TierSpoofer.replaceNamesInText(t, true) : text;
    }
}
