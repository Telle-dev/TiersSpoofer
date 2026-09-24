package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// text displays cache their lines, clear that when spoofs change so holograms update
@Mixin(DisplayEntity.TextDisplayEntity.class)
public class MixinTextDisplayEntity {
    @Shadow
    private DisplayEntity.TextDisplayEntity.TextLines textLines;

    @Unique
    private int tierspoofer$seenChange = -1;

    @Inject(method = "splitLines", at = @At("HEAD"))
    private void tierspoofer$refresh(DisplayEntity.TextDisplayEntity.LineSplitter splitter,
                                     CallbackInfoReturnable<DisplayEntity.TextDisplayEntity.TextLines> cir) {
        int now = TierSpoofer.getChangeCount();
        if (tierspoofer$seenChange != now) {
            tierspoofer$seenChange = now;
            this.textLines = null;
        }
    }
}
