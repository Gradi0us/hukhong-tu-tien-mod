package com.hukhong.living;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.story.world.Faction;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Su kien NPC song: Ma Tu Doi Lot xuat hien ngau nhien, phat thien cam khi danh dan, lenh /nhiemvu. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LivingEvents {
    private LivingEvents() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (e.getTarget() instanceof AmbientNpc n && e.getEntity() instanceof ServerPlayer p && n.role() != AmbientNpc.MA_TU) {
            AffectionService.change(p, n.getUUID(), NpcLife.factionOf(n), -6);
            p.displayClientMessage(Tx.t("hukhong.living.hit", "Dân cư bị bạn làm đau, thiện cảm giảm.", "You hurt a local; affection drops."), true);
        }
    }

    /** Moi 30 giay: nguoi choi o gan thanh/cang va da toi chuong Da Khuyet co 10% cho 1 Ma Tu xuat hien (toi da 1 trong 100 o). */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % 600 != 211) return;
        ServerLevel ow = s.overworld();
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator() || !StoryHooks.allowed(p, MaTu.LORE) || ow.random.nextInt(100) >= 10) continue;
            if (!ow.getEntitiesOfClass(AmbientNpc.class, new AABB(p.blockPosition()).inflate(100, 40, 100), n -> n.role() == AmbientNpc.MA_TU).isEmpty()) continue;
            if (ow.getEntitiesOfClass(AmbientNpc.class, new AABB(p.blockPosition()).inflate(70, 30, 70), n -> n.role() != AmbientNpc.MA_TU).size() < 4) continue; // chi trong khu dan cu
            double ang = ow.random.nextDouble() * Math.PI * 2, d = 25 + ow.random.nextInt(20);
            int x = p.getBlockX() + (int) (Math.cos(ang) * d), z = p.getBlockZ() + (int) (Math.sin(ang) * d);
            if (!ow.hasChunk(x >> 4, z >> 4)) continue;
            MaTu.spawnDisguised(ow, new BlockPos(x, ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z), null, null);
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("nhiemvu")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var qs = QuestService.quests(p);
                    if (qs.isEmpty()) p.sendSystemMessage(Tx.t("hukhong.living.q.none", "Bạn chưa nhận nhiệm vụ nào. Hãy trò chuyện với dân cư quen thuộc.", "No active quests. Chat with a friendly local."));
                    for (Quest q : qs) p.sendSystemMessage(QuestService.describe(q, p));
                    p.sendSystemMessage(Tx.t("hukhong.living.aff", "Thiện cảm Thành: %s - Tông Môn: %s", "City affection: %s - Sect: %s", AffectionService.faction(p, Faction.THANH), AffectionService.faction(p, Faction.TONG_MON)));
                    return qs.size();
                })
                .then(Commands.literal("huy").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (Quest q : new java.util.ArrayList<>(QuestService.quests(p))) QuestService.fail(p, q, "bỏ dở");
                    return 1;
                })));
    }
}
