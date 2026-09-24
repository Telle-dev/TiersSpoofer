package com.tierspoofer.mixin;

import com.mojang.authlib.GameProfile;
import com.tierspoofer.SkinSwap;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tab-list heads (and players whose model reads the skin from their tab entry). */
@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntrySkin {
    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            GameProfile profile = getProfile();
            if (profile == null) return;
            SkinTextures swapped = SkinSwap.apply(profile.id(), profile.name(), cir.getReturnValue());
            if (swapped != cir.getReturnValue()) {
                cir.setReturnValue(swapped);
            }
        } catch (Exception ignored) {
        }
    }
}
