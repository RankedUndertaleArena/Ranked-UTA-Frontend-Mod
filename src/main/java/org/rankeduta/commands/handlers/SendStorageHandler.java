package org.rankeduta.commands.handlers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.json.JSONObject;

public class SendStorageHandler {
    public static class executeSendStorage implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            MinecraftServer server = source.getServer();

            String storageName = StringArgumentType.getString(context, "storage");
            String path = StringArgumentType.getString(context, "path");
            Identifier storage = null;

            if (storageName.isEmpty()) {
                source.sendError(Text.literal("Storage 名稱或路徑不能為空"));
                return 0;
            }
            if (path.isEmpty()) storage = Identifier.of(storageName);
            else storage = Identifier.of(storageName, path);

            // 取得世界資料
            NbtCompound nbt = server.getDataCommandStorage().get(storage);
            if (nbt == null) {
                source.sendFeedback(() -> Text.literal("Storage 不存在：" + storageName), false);
                return 0;
            }

            // NBT 轉 Map
            JSONObject json = new JSONObject(NbtHelper.toFormattedString(nbt));
            source.sendFeedback(() -> Text.literal(json.toString()), false);
            return 1;
        }
    }
}
