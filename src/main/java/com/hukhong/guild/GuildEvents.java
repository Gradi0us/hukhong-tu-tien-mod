package com.hukhong.guild;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.gates.RewardGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Su kien Hiep Hoi: tien do diet quai, le tan, tin su, dung nha lazy cho thanh da co san. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class GuildEvents {
    private GuildEvents() {}

    /** Ban kinh (block) quanh tam thanh de dung / kiem tra Hiep Hoi khi co nguoi choi. */
    private static final int NEAR = 90;

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (!RewardGate.canReward(e.getEntity())) return; // spawner / thi luyen khong tinh
        GuildService.onKill(p, e.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getTarget() instanceof Villager v) || !v.getTags().contains(GuildBuilder.NPC_TAG)) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (!e.getLevel().isClientSide() && e.getEntity() instanceof ServerPlayer p) GuildService.open(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        GuildData d = GuildService.refresh(p);
        var qs = GuildService.quests(d, p.getUUID());
        for (int i = 0; i < qs.size(); i++) // tin su khong song qua dang xuat
            if (qs.get(i).kind() == GuildMath.K_COURIER && d.progOf(i) == 0 && !GuildCourier.active(p.getUUID())) GuildService.set(p, d.withProg(i, GuildData.NONE));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        GuildCourier.cancel(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent e) { GuildCourier.reset(); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int t = server.getTickCount();
        if (t % 20 == 3) GuildCourier.tick(server);
        if (t % 100 != 51 || t < 200) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        GuildHalls halls = GuildHalls.get(ow);
        for (ServerPlayer p : ow.players()) {
            for (CityDef d : CityDef.ALL) {
                BlockPos c = CityService.builtCenter(ow, d);
                if (c == null || !GuildBuilder.eligible(d)) continue;
                if (Math.abs(p.getX() - c.getX()) > NEAR || Math.abs(p.getZ() - c.getZ()) > NEAR) continue;
                if (!halls.has(d.id()) || !halls.isCurrent(d.id())) { if (GuildBuilder.ensure(ow, d)) return; } // dung moi / dung lai hall cu sai cho; toi da 1 nha moi lan kiem
                else GuildBuilder.ensureNpc(ow, halls.get(d.id()));
            }
        }
    }
}
