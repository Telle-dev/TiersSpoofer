package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// servers that draw nametags with text displays instead of the vanilla one
@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public class MixinTextDisplayRenderer {
    @ModifyVariable(method = "getLines", at = @At("HEAD"), argsOnly = true)
    private Text tierspoofer$replaceNames(Text text) {
        try {
            return TierSpoofer.replaceNamesInText(text, true);
        } catch (Exception e) {
            return text;
        }
    }
}
