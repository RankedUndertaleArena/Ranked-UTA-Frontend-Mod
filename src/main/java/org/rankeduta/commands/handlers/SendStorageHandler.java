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
            /*
            String path = StringArgumentType.getString(context, "path");

            if (!path.isEmpty()) storage = storage.getCompoundOrEmpty(path);
            if (storage == null) {
                source.sendFeedback(() -> Text.literal("Storage 不存在"), false);
                return 0;
            }
            */

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
                        } else if (item instanceof NbtList innerList) {
                            // 递归处理嵌套 List
                            jsonArray.put(nbtToJsonList(innerList));
                        } else {
                            switch (item.getType()) {
                                case NbtElement.BYTE_TYPE -> jsonArray.put(item.asByte());
                                case NbtElement.SHORT_TYPE -> jsonArray.put(item.asShort());
                                case NbtElement.INT_TYPE -> jsonArray.put(item.asInt());
                                case NbtElement.LONG_TYPE -> jsonArray.put(item.asLong());
                                case NbtElement.FLOAT_TYPE -> jsonArray.put(item.asFloat());
                                case NbtElement.DOUBLE_TYPE -> jsonArray.put(item.asDouble());
                                case NbtElement.STRING_TYPE -> jsonArray.put(item.asString());
                                case NbtElement.BYTE_ARRAY_TYPE -> jsonArray.put(item.asByteArray());
                                case NbtElement.INT_ARRAY_TYPE -> jsonArray.put(item.asIntArray());
                                case NbtElement.LONG_ARRAY_TYPE -> jsonArray.put(item.asLongArray());
                                default -> jsonArray.put(item.toString());
                            }
                        }
                    }
                    json.put(key, jsonArray);
                } else {
                    if (element == null) {
                        json.put(key, JSONObject.NULL);
                        continue;
                    }
                    switch (element.getType()) {
                        case NbtElement.BYTE_TYPE -> json.put(key, element.asByte().orElse((byte) 0));
                        case NbtElement.SHORT_TYPE -> json.put(key, element.asShort().orElse((short) 0));
                        case NbtElement.INT_TYPE -> json.put(key, element.asInt().orElse(0));
                        case NbtElement.LONG_TYPE -> json.put(key, element.asLong().orElse((long) 0));
                        case NbtElement.FLOAT_TYPE -> json.put(key, element.asFloat().orElse((float) 0));
                        case NbtElement.DOUBLE_TYPE -> json.put(key, element.asDouble().orElse((double) 0));
                        case NbtElement.STRING_TYPE -> json.put(key, element.asString().orElse(""));
                        case NbtElement.BYTE_ARRAY_TYPE -> json.put(key, element.asByteArray().orElse(new byte[0]));
                        case NbtElement.INT_ARRAY_TYPE -> json.put(key, element.asIntArray().orElse(new int[0]));
                        case NbtElement.LONG_ARRAY_TYPE -> json.put(key, element.asLongArray().orElse(new long[0]));
                        default -> throw new IllegalStateException("Unexpected value: " + element.getType());
                    }
                }
            }
            return json;
        }

        // 辅助方法：处理 NbtList 转 JSONArray
        private static JSONArray nbtToJsonList(NbtList list) {
            JSONArray jsonArray = new JSONArray();
            for (NbtElement item : list) {
                if (item instanceof NbtCompound itemCompound) {
                    jsonArray.put(nbtToJson(itemCompound));
                } else if (item instanceof NbtList innerList) {
                    jsonArray.put(nbtToJsonList(innerList));
                } else {
                    switch (item.getType()) {
                        case NbtElement.BYTE_TYPE -> jsonArray.put(item.asByte().orElse((byte) 0));
                        case NbtElement.SHORT_TYPE -> jsonArray.put(item.asShort().orElse((short) 0));
                        case NbtElement.INT_TYPE -> jsonArray.put(item.asInt().orElse(0));
                        case NbtElement.LONG_TYPE -> jsonArray.put(item.asLong().orElse((long) 0));
                        case NbtElement.FLOAT_TYPE -> jsonArray.put(item.asFloat().orElse((float) 0));
                        case NbtElement.DOUBLE_TYPE -> jsonArray.put(item.asDouble().orElse((double) 0));
                        case NbtElement.STRING_TYPE -> jsonArray.put(item.asString().orElse(""));
                        case NbtElement.BYTE_ARRAY_TYPE -> jsonArray.put(item.asByteArray().orElse(new byte[0]));
                        case NbtElement.INT_ARRAY_TYPE -> jsonArray.put(item.asIntArray().orElse(new int[0]));
                        case NbtElement.LONG_ARRAY_TYPE -> jsonArray.put(item.asLongArray().orElse(new long[0]));
                        default -> jsonArray.put(item.toString());
                    }
                }
            }
            return jsonArray;
        }
    }

    public static class executeSendBack implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            String body = new JSONObject()
                .put("server", SERVER_UUID.toString())
                .toString();

            HTTPClient.post("/game/return", body);
            return 1;
        }
    }
}
