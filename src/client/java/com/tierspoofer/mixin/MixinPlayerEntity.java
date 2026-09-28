package com.tierspoofer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerEntity.class, priority = 2000)
public class MixinPlayerEntity {
    // same kind of hook TierTagger uses, so with the higher priority ours runs after theirs and drops their tag
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text tierspoofer$displayName(Text original) {
        try {
            PlayerEntity self = (PlayerEntity) (Object) this;
            TierSpooferConfig config = TierSpoofer.getConfig();
            if (config != null && config.isEnabled() && config.isShowInWorld()) {
                Text modified = TierSpoofer.getDisplayName(self.getUuid(), self.getGameProfile().getName(), original);
                if (modified != null) return modified;
            }
        } catch (Exception ignored) {
        }
        return original;
    }
}
