package com.tierspoofer.mixin;

import com.tierspoofer.SkinSwap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The skin the player model is drawn with (your own model in F5 / the
 * inventory, and other players). Covers players that have no tab-list entry
 * the PlayerListEntry hook could catch.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetSkin(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
            SkinTextures swapped = SkinSwap.apply(self.getUuid(), self.getGameProfile().name(), cir.getReturnValue());
            if (swapped != cir.getReturnValue()) {
                cir.setReturnValue(swapped);
            }
        } catch (Exception ignored) {
        }
    }
}
