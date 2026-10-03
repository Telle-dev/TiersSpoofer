package com.tierspoofer.test.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tierspoofer.test.FakeTierTagger;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListHud.class)
public class FakeTierTaggerTab {
    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text fakeTierTagger(Text original, PlayerListEntry entry) {
        return FakeTierTagger.prepend(original);
    }
}
