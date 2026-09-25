package com.tierspoofer.mixin;

import com.tierspoofer.FakeNameSuggestions;
import net.minecraft.client.network.ClientCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

// names offered when you press tab in normal chat
@Mixin(ClientCommandSource.class)
public class MixinClientCommandSource {
    @Inject(method = "getChatSuggestions", at = @At("RETURN"), cancellable = true)
    private void tierspoofer$fakeNames(CallbackInfoReturnable<Collection<String>> cir) {
        cir.setReturnValue(FakeNameSuggestions.withFakeNames(cir.getReturnValue()));
    }
}
