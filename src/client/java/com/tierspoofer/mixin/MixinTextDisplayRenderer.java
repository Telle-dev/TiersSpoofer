package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Many PvP servers hide the vanilla nametag and draw their own with a text display
// entity riding the player. Swap the name (and add the tier) in those too.
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
