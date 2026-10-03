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

// armor stand holograms and other named entities (players go through MixinPlayerEntity)
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$replaceHologramName(Entity entity, CallbackInfoReturnable<Text> cir) {
        if (entity instanceof PlayerEntity) return;
        cir.setReturnValue(TierSpoofer.replaceNamesInText(cir.getReturnValue(), true));
    }
}
