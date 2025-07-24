package org.rankeduta.command.handler;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.json.JSONObject;
import org.rankeduta.command.Command;
import org.rankeduta.service.BackendService;

import static org.rankeduta.RankedUTA.SERVER_UUID;

public class SendDataHandler implements Command.IHandler {
    @Override
    public int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        String type = StringArgumentType.getString(context, "type");
        NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");
        String storage = null , key = null;
        try {
            storage = StringArgumentType.getString(context, "storage");
        } catch (IllegalArgumentException ignored) {}
        try {
            key = StringArgumentType.getString(context, "key");
        } catch (IllegalArgumentException ignored) {}

        if (type == null || type.isEmpty()) {
            source.sendError(Text.literal("資料類型不可為空"));
            return 0;
        }

        if (storage != null && !storage.isEmpty()) {
            if (key != null && !key.isEmpty())
                nbt = source.getServer().getDataCommandStorage().get(Identifier.of(storage)).getCompound(key).orElse(new NbtCompound());
            else
                nbt = source.getServer().getDataCommandStorage().get(Identifier.of(storage));
        }

        if (nbt == null) {
            source.sendError(Text.literal("NBT 資料不可為空"));
            return 0;
        }

        String body = new JSONObject()
            .put("server", SERVER_UUID.toString())
            .put("data", nbt.toString())
            .toString();
        switch (type) {
            case "player_setting" -> {
                if (player == null) {
                    source.sendError(Text.literal("請在遊戲中使用此指令"));
                    return 0;
                }
                player.sendMessage(Text.literal("正在將玩家設定傳送至伺服器：" + nbt).setStyle(Style.EMPTY.withColor(0xFFFF55)));
                return 1;
            }
            case "game_stats" -> {
                JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/game/end", body));
                if (jsonResponse == null) {
                    source.sendError(Text.literal("無法連接到後端伺服器，請檢查伺服器是否已啟動"));
                    return 0;
                }
                source.sendFeedback(() -> Text.literal("已將 Storage 傳送至伺服器"), false);
                return 1;
            }
            default -> source.sendError(Text.literal("未知的資料類型：" + type));
        }
        return 0;
    }
}
