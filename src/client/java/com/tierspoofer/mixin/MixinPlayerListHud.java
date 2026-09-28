package com.tierspoofer.mixin;

import com.mojang.authlib.GameProfile;
import com.tierspoofer.TierSpoofer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerListHud.class, priority = 2000)
public class MixinPlayerListHud {
    // "Welcome Steve" style tab header/footer
    @ModifyVariable(method = "setHeader", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text tierspoofer$header(Text header) {
        return TierSpoofer.replaceNamesInText(header);
    }

    @ModifyVariable(method = "setFooter", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text tierspoofer$footer(Text footer) {
        return TierSpoofer.replaceNamesInText(footer);
    }

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        try {
            if (!TierSpoofer.getConfig().isEnabled()) return;
            GameProfile profile = entry.getProfile();
            Text modified = TierSpoofer.getTabName(profile.id(), profile.name(), cir.getReturnValue());
            if (modified != null) {
                cir.setReturnValue(modified);
            }
        } catch (Exception ignored) {
        }
    }
}
