package com.tierspoofer;

import com.tierspoofer.config.TierSpooferConfig;
import com.tierspoofer.model.SpoofedPlayer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

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
            SkinCache.prefetchSkin(targetName);
            return original;
        }
        Identifier skin = SkinCache.getCachedSkin(targetUuid);
        if (skin == null) {
            return original;
        }
        SkinTextures.Model model = SkinTextures.Model.fromName(SkinCache.isSlim(targetUuid) ? "slim" : "default");
        Identifier cape = config.isCapeEnabled() ? SkinCache.getCachedCape(targetUuid) : original.capeTexture();
        Identifier elytra = config.isCapeEnabled() ? null : original.elytraTexture();
        return new SkinTextures(skin, null, cape, elytra, model, original.secure());
    }
}
