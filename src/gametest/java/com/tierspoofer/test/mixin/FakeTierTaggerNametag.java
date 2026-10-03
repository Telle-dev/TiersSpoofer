package com.tierspoofer.test.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tierspoofer.test.FakeTierTagger;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public class FakeTierTaggerNametag {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text fakeTierTagger(Text original) {
        return FakeTierTagger.prepend(original);
    }
}
