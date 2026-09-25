package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// item names and lore in menus (profile heads, stats items...)
@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$tooltip(CallbackInfoReturnable<List<Text>> cir) {
        List<Text> lines = cir.getReturnValue();
        if (lines == null || lines.isEmpty()) return;
        List<Text> out = new ArrayList<>(lines.size());
        for (Text line : lines) out.add(TierSpoofer.replaceNamesOnClient(line));
        cir.setReturnValue(out);
    }
}
