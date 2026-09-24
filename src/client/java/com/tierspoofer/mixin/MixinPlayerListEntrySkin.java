package com.tierspoofer.mixin;

import com.mojang.authlib.GameProfile;
import com.tierspoofer.SkinCache;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.model.SpoofedPlayer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntrySkin {
    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            TierSpooferConfig config = TierSpoofer.getConfig();
            if (config == null || !config.isEnabled() || !config.isSkinEnabled()) {
                return;
            }
            GameProfile profile = getProfile();
            if (profile == null) {
                return;
            }
            UUID uuid = profile.getId();
            SpoofedPlayer spoofed = TierSpoofer.getSpoofedPlayer(uuid);
            if (spoofed != null && spoofed.getSkinTargetName() != null && !spoofed.getSkinTargetName().isEmpty()) {
                String targetName = spoofed.getSkinTargetName();
                UUID targetUuid = SkinCache.getUuidForUsername(targetName);
                if (targetUuid != null && SkinCache.hasCachedSkin(targetUuid)) {
                    Identifier skinId = SkinCache.getCachedSkin(targetUuid);
                    if (skinId != null) {
                        SkinTextures original = cir.getReturnValue();
                        if (original == null) {
                            return;
                        }
                        Identifier capeId = config.isCapeEnabled()
                                ? SkinCache.getCachedCape(targetUuid)
                                : original.capeTexture();
                        SkinTextures newTextures = new SkinTextures(
                                skinId,
                                original.textureUrl(),
                                capeId,
                                original.elytraTexture(),
                                original.model(),
                                original.secure()
                        );
                        cir.setReturnValue(newTextures);
                    }
                } else if (!SkinCache.isLoading(targetUuid != null ? targetUuid : UUID.randomUUID())) {
                    SkinCache.prefetchSkin(targetName);
                }
            }
        } catch (Exception ignored) {
        }
    }
}