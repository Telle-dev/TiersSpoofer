package com.tierspoofer.mixin;

import com.mojang.authlib.GameProfile;
import com.tierspoofer.SkinCache;
import com.tierspoofer.TierSpoofer;
import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.model.SpoofedPlayer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * Gives a spoofed player the skin (and cape) of the account named in their
 * Fake Name. Used for the tab list heads and the player model in the world.
 */
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
            SkinTextures original = cir.getReturnValue();
            if (profile == null || original == null) {
                return;
            }
            SpoofedPlayer spoofed = TierSpoofer.findSpoofedPlayer(profile.id(), profile.name());
            if (spoofed == null || spoofed.getSkinTargetName() == null || spoofed.getSkinTargetName().isEmpty()) {
                return;
            }
            String targetName = spoofed.getSkinTargetName();
            UUID targetUuid = SkinCache.getUuidForUsername(targetName);
            if (targetUuid == null || !SkinCache.hasCachedSkin(targetUuid)) {
                // Not downloaded yet; SkinCache ignores repeat requests while loading.
                SkinCache.prefetchSkin(targetName);
                return;
            }
            Identifier skinId = SkinCache.getCachedSkin(targetUuid);
            if (skinId == null) {
                return;
            }
            // The texture is registered under skinId itself, so id and texture path are the same.
            AssetInfo.TextureAssetInfo body = new AssetInfo.TextureAssetInfo(skinId, skinId);
            PlayerSkinType model = PlayerSkinType.byModelMetadata(SkinCache.isSlim(targetUuid) ? "slim" : "default");

            if (config.isCapeEnabled()) {
                // Take the target's cape too (or no cape if they don't have one).
                Identifier capeId = SkinCache.getCachedCape(targetUuid);
                AssetInfo.TextureAssetInfo cape = capeId == null ? null : new AssetInfo.TextureAssetInfo(capeId, capeId);
                cir.setReturnValue(SkinTextures.create(body, cape, null, model));
            } else {
                // Only the skin; keep the player's own cape/elytra.
                cir.setReturnValue(original.withOverride(SkinTextures.SkinOverride.create(
                        Optional.of(body), Optional.empty(), Optional.empty(), Optional.of(model))));
            }
        } catch (Exception ignored) {
        }
    }
}
