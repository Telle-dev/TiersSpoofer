package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Older servers put the nametag on an invisible armor stand (or other entity) above
// the player. Players themselves are handled in MixinPlayerEntity.
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$replaceHologramName(Entity entity, CallbackInfoReturnable<Text> cir) {
        try {
            if (entity instanceof PlayerEntity || cir.getReturnValue() == null) return;
            Text replaced = TierSpoofer.replaceNamesInText(cir.getReturnValue(), true);
            if (replaced != cir.getReturnValue()) cir.setReturnValue(replaced);
        } catch (Exception ignored) {
        }
    }
}
