// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

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
        if (original == null || !config.isEnabled() || !config.isSkinEnabled()) return original;
        SpoofedPlayer spoofed = TierSpoofer.findSpoofedPlayer(uuid, username);
        if (spoofed == null) spoofed = TabEntryMatcher.forSkin(uuid);
        String targetName = spoofed == null ? null : spoofed.getSkinTargetName();
        if (targetName == null || targetName.isEmpty()) return original;

        UUID targetUuid = SkinCache.getUuidForUsername(targetName);
        if (targetUuid == null || !SkinCache.hasCachedSkin(targetUuid)) {
            SkinCache.prefetchSkin(targetName);
            return original;
        }
        Identifier skinId = SkinCache.getCachedSkin(targetUuid);
        if (skinId == null) return original;

        SkinTextures.Model model = SkinTextures.Model.fromName(SkinCache.isSlim(targetUuid) ? "slim" : "default");
        Identifier cape = config.isCapeEnabled() ? SkinCache.getCachedCape(targetUuid) : original.capeTexture();
        Identifier elytra = config.isCapeEnabled() ? null : original.elytraTexture();
        return new SkinTextures(skinId, null, cape, elytra, model, original.secure());
    }
}
