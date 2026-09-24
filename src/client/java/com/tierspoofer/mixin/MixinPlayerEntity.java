package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
        try {
            PlayerEntity self = (PlayerEntity) (Object) this;
            TierSpooferConfig config = TierSpoofer.getConfig();
            if (config != null && config.isEnabled() && config.isShowInWorld()) {
                Text modified = TierSpoofer.getDisplayName(self.getUuid(), cir.getReturnValue());
                if (modified != null) {
                    cir.setReturnValue(modified);
                }
            }
        } catch (Exception ignored) {
        }
    }
}