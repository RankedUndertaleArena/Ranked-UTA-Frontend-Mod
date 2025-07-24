package org.rankeduta.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.rankeduta.RankedUTA;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerManager.class)
public class JoinMessageMixin {
	@Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
	private void redirectSendToAll(PlayerManager instance, Text message, boolean overlay) {
		RankedUTA.LOGGER.debug("玩家連接，消息被阻止：{}", message.getString());
	}
}