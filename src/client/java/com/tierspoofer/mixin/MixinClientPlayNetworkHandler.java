package com.tierspoofer.mixin;

import com.tierspoofer.TierSpoofer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// the server only knows real names, so undo the spoof in commands we send
@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @ModifyVariable(method = "sendChatCommand", at = @At("HEAD"), argsOnly = true)
    private String tierspoofer$realNamesInCommand(String command) {
        return TierSpoofer.toRealNames(command);
    }

    @ModifyVariable(method = "sendCommand", at = @At("HEAD"), argsOnly = true, require = 0)
    private String tierspoofer$realNamesInCommand2(String command) {
        return TierSpoofer.toRealNames(command);
    }
}
