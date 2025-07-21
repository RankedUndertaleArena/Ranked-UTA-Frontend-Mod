package org.rankeduta.commands.handlers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.rankeduta.services.PartyService;

public class PartyHandler {
    public static class executeInvite implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                String targetName = StringArgumentType.getString(context, "player");
                ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);

                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                if (target == null) {
                    source.sendError(Text.literal("找不到玩家：" + targetName));
                    return 0;
                }
                return PartyService.invite(source, sender, target);
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }

    public static class executeAccept implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            PlayerManager playerManager = source.getServer().getPlayerManager();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                String targetName = StringArgumentType.getString(context, "player");
                ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);

                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                if (target == null) {
                    source.sendError(Text.literal("找不到玩家：" + targetName));
                    return 0;
                }
                return PartyService.accept(source, sender, target, playerManager);
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }

    public static class executeKick implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            PlayerManager playerManager = source.getServer().getPlayerManager();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                String targetName = StringArgumentType.getString(context, "player");
                ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);

                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                if (target == null) {
                    source.sendError(Text.literal("找不到玩家：" + targetName));
                    return 0;
                }
                return PartyService.kick(source, sender, target, playerManager);
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }

    public static class executeTransfer implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            PlayerManager playerManager = source.getServer().getPlayerManager();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                String targetName = StringArgumentType.getString(context, "player");
                ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);

                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                if (target == null) {
                    source.sendError(Text.literal("找不到玩家：" + targetName));
                    return 0;
                }
                return PartyService.transfer(source, sender, target, playerManager);
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }

    public static class executeLeave implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            PlayerManager playerManager = source.getServer().getPlayerManager();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                return PartyService.leave(source, sender, playerManager);
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }

    public static class executeList implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            PlayerManager playerManager = source.getServer().getPlayerManager();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                return PartyService.list(source, sender, playerManager);
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }

    public static class executeDisband implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            PlayerManager playerManager = source.getServer().getPlayerManager();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                return PartyService.disband(source, sender, playerManager);
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }
}
