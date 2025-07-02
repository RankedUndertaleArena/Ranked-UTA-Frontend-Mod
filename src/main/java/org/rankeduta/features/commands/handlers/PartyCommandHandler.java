package org.rankeduta.features.commands.handlers;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.rankeduta.defines.Party;
import org.rankeduta.features.services.PartyService;

import java.util.UUID;

public class PartyCommandHandler {
    public static void invite(ServerPlayerEntity sender, ServerPlayerEntity target, PartyService partyService) {
        // 檢查是否為 leader，如果不是則返回錯誤訊息
        Party party = partyService.getPartyByMember(sender.getUuidAsString());
        boolean isTargetHaveParty = partyService.getPartyByMember(target.getUuidAsString()) != null;

        if (sender == target) {
            sender.sendMessage(Text.literal("你不能邀請自己！").withColor(0xFF5555));
            return;
        }
        if (isTargetHaveParty) {
            sender.sendMessage(Text.literal("該玩家已在其他隊伍中！").withColor(0xFF5555));
            return;
        }
        if (party == null) {
            party = new Party(sender.getUuidAsString());
            partyService.saveParty(party);
        } else if (!party.getLeader().equals(sender.getUuidAsString())) {
            sender.sendMessage(Text.literal("只有隊長可以邀請成員！").withColor(0xFF5555));
            return;
        }
        if (!sender.getServer().getPlayerManager().getPlayerList().contains(target)) {
            sender.sendMessage(Text.literal("找不到玩家或是玩家不在伺服器中！").withColor(0xFF5555));
            return;
        }
        if (party.getMembers().contains(target.getUuidAsString())) {
            sender.sendMessage(Text.literal("該玩家已在隊伍中！").withColor(0xFF5555));
            return;
        }

        long expireAt = System.currentTimeMillis() + 30000;
        party.getInvites().put(target.getUuidAsString(), expireAt);
        partyService.saveParty(party);
        sender.sendMessage(Text.literal("已邀請 " + target.getName().getString() + " 進入隊伍，他們有 30 秒的時間加入隊伍！").withColor(0x55FF55));
        target.sendMessage(Text.literal(sender.getName().getString() + " 已邀請你進入隊伍，你有 30 秒的時間加入隊伍！").withColor(0x55FF55));
    }
    public static void accept(ServerPlayerEntity sender, ServerPlayerEntity inviter, PartyService partyService) {
        MinecraftServer server = sender.getServer();
        boolean isSenderHaveParty = partyService.getPartyByMember(sender.getUuidAsString()) != null;

        if (inviter == null) {
            sender.sendMessage(Text.literal("找不到來自該玩家的邀請！").withColor(0xFF5555));
            return;
        }
        Party party = partyService.getPartyByLeader(inviter.getUuidAsString());
        if (isSenderHaveParty) {
            party.getInvites().remove(sender.getUuidAsString());
            sender.sendMessage(Text.literal("你已經在其他隊伍裡！").withColor(0xFF5555));
            return;
        }
        if (party == null) {
            sender.sendMessage(Text.literal("該隊伍不存在！").withColor(0xFF5555));
            return;
        }
        party.getInvites().remove(sender.getUuidAsString());
        party.getMembers().add(sender.getUuidAsString());
        partyService.saveParty(party);
        sender.sendMessage(Text.literal("你已加入隊伍！").withColor(0x55FF55));
        for (String memberUUID : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(memberUUID));
            if (member != null && !member.equals(sender)) {
                member.sendMessage(Text.literal(sender.getName().getString() + " 已加入隊伍！").withColor(0x55FF55));
            }
        }
    }
    public static void kick(ServerPlayerEntity sender, ServerPlayerEntity target, PartyService partyService) {
        // 檢查是否為 leader，移除成員
        MinecraftServer server = sender.getServer();
        Party party = partyService.getPartyByMember(sender.getUuidAsString());

        if (sender == target) {
            sender.sendMessage(Text.literal("你不能踢出自己！").withColor(0xFF5555));
            return;
        }

        if (party == null) {
            sender.sendMessage(Text.literal("你目前不在隊伍裡！").withColor(0xFF5555));
            return;
        } else if (!party.getLeader().equals(sender.getUuidAsString())) {
            sender.sendMessage(Text.literal("只有隊長可以踢出成員！").withColor(0xFF5555));
            return;
        }

        party.getMembers().remove(target.getUuidAsString());
        partyService.saveParty(party);
        target.sendMessage(Text.literal(sender.getName().getString() + "已把你踢出隊伍！").withColor(0x55FF55));

        for (String memberUUID : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(memberUUID));
            if (member != null) {
                if (!member.equals(sender)) {
                    member.sendMessage(Text.literal("已把 " + target.getName().getString() + " 踢出隊伍！").withColor(0x55FF55));
                }
            }
        }
    }
    public static void transfer(ServerPlayerEntity sender, ServerPlayerEntity target, PartyService partyService) {
        // 檢查是否為 leader，轉移 leader
        MinecraftServer server = sender.getServer();
        Party party = partyService.getPartyByMember(sender.getUuidAsString());

        if (sender == target) {
            sender.sendMessage(Text.literal("你已經是隊長了！").withColor(0xFF5555));
            return;
        }

        if (party == null) {
            sender.sendMessage(Text.literal("你目前不在隊伍裡！").withColor(0xFF5555));
            return;
        } else if (!party.getLeader().equals(sender.getUuidAsString())) {
            sender.sendMessage(Text.literal("你並不是隊長！").withColor(0xFF5555));
            return;
        }

        party.setLeader(target.getUuidAsString());
        partyService.saveParty(party);
        sender.sendMessage(Text.literal("你已把隊長轉交給 " + target.getName().getString() + "！").withColor(0x55FF55));
        target.sendMessage(Text.literal(sender.getName().getString() + " 已把隊長轉交給你！").withColor(0x55FF55));
        for (String memberUUID : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(memberUUID));
            if (member != null) {
                if (!member.equals(sender) || !member.equals(target)) {
                    member.sendMessage(Text.literal(sender.getName().getString() + " 已把隊長轉交給 " + target.getName().getString() + "！").withColor(0x55FF55));
                }
            }
        }
    }
    public static void list(ServerPlayerEntity sender, PartyService partyService) {
        // 顯示隊伍成員
        MinecraftServer server = sender.getServer();
        Party party = partyService.getPartyByMember(sender.getUuidAsString());
        int offlineCount = 0;
        if (party == null) {
            sender.sendMessage(Text.literal("你目前不在隊伍裡！").withColor(0xFF5555));
            return;
        }
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(UUID.fromString(party.getLeader()));
        sender.sendMessage(Text.literal("隊伍成員：").withColor(0x55FF55));
        if (sender.equals(leader)) {
            sender.sendMessage(Text.literal("- " + leader.getName().getString()).setStyle(Style.EMPTY.withBold(true).withColor(0xFFFF55)));
        } else {
            sender.sendMessage(Text.literal("- " + leader.getName().getString()).withColor(0xFFFF55));
        }
        for (String memberUUID : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(memberUUID));
            if (member != null) {
                if (!member.equals(leader) && sender.equals(member)) {
                    sender.sendMessage(Text.literal("- " + member.getName().getString()).setStyle(Style.EMPTY.withBold(true).withColor(0x55FF55)));
                } else {
                    sender.sendMessage(Text.literal("- " + member.getName().getString()).withColor(0x55FF55));
                }
            } else {
                offlineCount++;
            }
        }
        if (offlineCount > 0) {
            sender.sendMessage(Text.literal("- " + offlineCount + " 位成員離線。").withColor(0x555555));
        }
    }
    public static void leave(ServerPlayerEntity sender, PartyService partyService) {
        // 非 leader 離開隊伍
        MinecraftServer server = sender.getServer();
        Party party = partyService.getPartyByMember(sender.getUuidAsString());

        if (party == null) {
            sender.sendMessage(Text.literal("你目前不在隊伍裡！").withColor(0xFF5555));
            return;
        }

        party.getMembers().remove(sender.getUuidAsString());
        partyService.saveParty(party);

        sender.sendMessage(Text.literal("你已退出隊伍！"));
        for (String memberUUID : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(memberUUID));
            if (member != null) {
                if (!member.equals(sender)) {
                    member.sendMessage(Text.literal(sender.getName().getString() + " 已退出隊伍！").withColor(0x55FF55));
                }
            }
        }
    }
    public static void disband(ServerPlayerEntity sender, PartyService partyService) {
        // leader 解散隊伍
        MinecraftServer server = sender.getServer();
        Party party = partyService.getPartyByMember(sender.getUuidAsString());

        if (party == null) {
            sender.sendMessage(Text.literal("你目前不在隊伍裡！").withColor(0xFF5555));
            return;
        } else if (!party.getLeader().equals(sender.getUuidAsString())) {
            sender.sendMessage(Text.literal("你並不是隊長！").withColor(0xFF5555));
            return;
        }

        sender.sendMessage(Text.literal("你已解散隊伍！").withColor(0x55FF55));
        for (String memberUUID : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(memberUUID));
            if (member != null) {
                if (!member.equals(sender)) {
                    member.sendMessage(Text.literal(sender.getName().getString() + " 已解散隊伍！").withColor(0x55FF55));
                }
            }
        }

        partyService.deleteParty(party.getLeader());
    }
}
