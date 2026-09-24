package com.tierspoofer.mixin;

import com.tierspoofer.SkinSwap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetSkin(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
            SkinTextures swapped = SkinSwap.apply(self.getUuid(), self.getGameProfile().getName(), cir.getReturnValue());
            if (swapped != cir.getReturnValue()) {
                cir.setReturnValue(swapped);
            }
        } catch (Exception ignored) {
        }
    }
}
