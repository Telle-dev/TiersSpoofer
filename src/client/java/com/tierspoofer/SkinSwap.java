package com.tierspoofer;

import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.model.SpoofedPlayer;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;

public final class SkinSwap {
    private SkinSwap() {
    }

    public static SkinTextures apply(UUID uuid, String username, SkinTextures original) {
        TierSpooferConfig config = TierSpoofer.getConfig();
        if (original == null || config == null || !config.isEnabled() || !config.isSkinEnabled()) {
            return original;
        }
        SpoofedPlayer spoofed = TierSpoofer.findSpoofedPlayer(uuid, username);
        if (spoofed == null || spoofed.getSkinTargetName() == null || spoofed.getSkinTargetName().isEmpty()) {
            return original;
        }
        String targetName = spoofed.getSkinTargetName();
        UUID targetUuid = SkinCache.getUuidForUsername(targetName);
        if (targetUuid == null || !SkinCache.hasCachedSkin(targetUuid)) {
            // Not downloaded yet; SkinCache ignores repeat requests while loading.
            SkinCache.prefetchSkin(targetName);
            return original;
        }
        Identifier skinId = SkinCache.getCachedSkin(targetUuid);
        if (skinId == null) {
            return original;
        }
        // The texture is registered under skinId itself, so id and texture path are the same.
        AssetInfo.TextureAssetInfo body = new AssetInfo.TextureAssetInfo(skinId, skinId);
        PlayerSkinType model = PlayerSkinType.byModelMetadata(SkinCache.isSlim(targetUuid) ? "slim" : "default");

        if (config.isCapeEnabled()) {
            // Take the target's cape too (or no cape if they don't have one).
            Identifier capeId = SkinCache.getCachedCape(targetUuid);
            AssetInfo.TextureAssetInfo cape = capeId == null ? null : new AssetInfo.TextureAssetInfo(capeId, capeId);
            return SkinTextures.create(body, cape, null, model);
        }
        // Only the skin; keep the player's own cape/elytra.
        return original.withOverride(SkinTextures.SkinOverride.create(
                Optional.of(body), Optional.empty(), Optional.empty(), Optional.of(model)));
    }
}
