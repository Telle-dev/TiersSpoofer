package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// servers that draw nametags with text displays instead of the vanilla one.
// priority 2000 so this runs after other tier mods touched the text
@Mixin(value = DisplayEntityRenderer.TextDisplayEntityRenderer.class, priority = 2000)
public class MixinTextDisplayRenderer {
    @ModifyArg(method = "getLines", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;"))
    private StringVisitable tierspoofer$replaceNames(StringVisitable text) {
        try {
            return text instanceof Text t ? TierSpoofer.replaceNamesInText(t, true) : text;
        } catch (Exception e) {
            return text;
        }
    }
}
