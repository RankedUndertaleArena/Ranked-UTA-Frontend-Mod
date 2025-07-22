package org.rankeduta.commands.handlers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;

import static org.rankeduta.RankedUTA.SERVER_UUID;

public class SendStorageHandler {
    public static class executeSendStorage implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();

            NbtCompound storage = NbtCompoundArgumentType.getNbtCompound(context, "storage");
            String path = StringArgumentType.getString(context, "path");

            if (!path.isEmpty()) storage = storage.getCompoundOrEmpty(path);
            if (storage == null) {
                source.sendFeedback(() -> Text.literal("Storage 不存在"), false);
                return 0;
            }

            // NBT 轉 JSON
            String body = new JSONObject()
                .put("server", SERVER_UUID.toString())
                .put("data", nbtToJson(storage))
                .toString();
            HTTPClient.post("/game/end", body);
            source.sendFeedback(() -> Text.literal("已將 Storage 傳送至伺服器"), false);
            return 1;
        }

        // NBT 轉 JSON 方法
        private static JSONObject nbtToJson(NbtCompound nbt) {
            JSONObject json = new JSONObject();
            for (String key : nbt.getKeys()) {
                NbtElement element = nbt.get(key);
                if (element instanceof NbtCompound compound) {
                    json.put(key, nbtToJson(compound));
                } else if (element instanceof NbtList list) {
                    JSONArray jsonArray = new JSONArray();
                    for (NbtElement item : list) {
                        if (item instanceof NbtCompound itemCompound) {
                            jsonArray.put(nbtToJson(itemCompound));
                        } else {
                            jsonArray.put(item.toString());
                        }
                    }
                    json.put(key, jsonArray);
                } else {
                    if (element == null) {
                        json.put(key, JSONObject.NULL);
                        continue;
                    }
                    switch (element.getType()) {
                        case NbtElement.BYTE_TYPE -> json.put(key, element.asByte());
                        case NbtElement.SHORT_TYPE -> json.put(key, element.asShort());
                        case NbtElement.INT_TYPE -> json.put(key, element.asInt());
                        case NbtElement.LONG_TYPE -> json.put(key, element.asLong());
                        case NbtElement.FLOAT_TYPE -> json.put(key, element.asFloat());
                        case NbtElement.DOUBLE_TYPE -> json.put(key, element.asDouble());
                        case NbtElement.STRING_TYPE -> json.put(key, element.asString());
                        default -> throw new IllegalStateException("Unexpected value: " + element.getType());
                    }
                }
            }
            return json;
        }
    }

    public static class executeSendBack implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            MinecraftServer server = source.getServer();

            String body = new JSONObject()
                .put("server", SERVER_UUID.toString())
                .toString();

            HTTPClient.post("/game/return", body);
            return 1;
        }
    }
}
