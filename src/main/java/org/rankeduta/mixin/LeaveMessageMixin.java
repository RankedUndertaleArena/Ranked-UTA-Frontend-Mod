package org.rankeduta.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.rankeduta.RankedUTA;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayNetworkHandler.class)
public class LeaveMessageMixin {
    @Redirect(method = "cleanUp", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    public void cleanUp(PlayerManager instance, Text message, boolean overlay) {
        RankedUTA.LOGGER.debug("玩家斷線，消息被阻止：{}", message.getString());
    }
}