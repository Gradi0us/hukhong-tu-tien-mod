package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import com.hukhong.chg.ChgKit;
import com.hukhong.chg.ChgService;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.Wallet;
import com.hukhong.story.StoryService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thien Kiep Loi Vu (CHG): bao set quanh hang o Chap Hanh Gia chua bi ha (doc ChgService.lairXZ). Song sot du lau thi nhan EXP + Hon Tinh.
 * Set danh la hieu ung + sat thuong vua (khong gay chet thang neu mau day); chet trong bao thi khong duoc thuong. Cho phep tim hang tu nhien theo tia set.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LoiVuHandler {
    private LoiVuHandler() {}

    public static final int STORM_RADIUS = 28, ZONE_RADIUS = 48, MIN_STAY = 400, STRIKE_EVERY = 30;
    public static final String TYPE = "loi_vu";

    private static final class Storm {
        final int cx, cz, lair;
        final Map<UUID, Integer> stay = new HashMap<>();
        final Set<UUID> died = new HashSet<>();
        Storm(int cx, int cz, int lair) { this.cx = cx; this.cz = cz; this.lair = lair; }
    }

    private static final Map<String, Storm> STORMS = new HashMap<>();

    /** Thuan: hang (1..ChgKit.COUNT) duoc chon cho mot lan xuat hien. */
    public static int lairFor(long start) { return 1 + LichMath.pickIndex(TYPE, start, ChgKit.COUNT); }

    /** Su kien lich lane truyen co dinh hang (vd arc_loi_vu = Chap Hanh Gia I, cung Lac Nhat Thanh); cac su kien khac chon theo lan xuat hien. */
    public static int lairFor(String defId, long start) { return "arc_loi_vu".equals(defId) ? 1 : lairFor(start); }

    /** Thuan: sat thuong set len muc tieu co maxHealth: 12% mau toi da, toi thieu 3. */
    public static float strikeDamage(float maxHealth) { return Math.max(3f, maxHealth * 0.12f); }

    /** Thuan: EXP thuong = 1/5 thanh kinh nghiem len cap, toi thieu 1. */
    public static long rewardExp(long expToNext) { return Math.max(1, expToNext / 5); }

    /** Vi tri su kien cho lich (nut "Den"): ngay tai hang. */
    public static BlockPos locate(ServerLevel ow, long start) { return locate(ow, TYPE, start); }

    public static BlockPos locate(ServerLevel ow, String defId, long start) {
        int[] xz = ChgService.lairXZ(ow, lairFor(defId, start));
        return new BlockPos(xz[0], 64, xz[1]);
    }

    public static void begin(ServerLevel ow, CalendarData.Act a) {
        int lair = lairFor(a.defId(), a.start());
        boolean any = ow.players().isEmpty();
        for (ServerPlayer p : ow.players()) if (!ChgKit.defeated(StoryService.get(p).lair(), lair)) { any = true; break; }
        if (!any) return; // moi nguoi da ha CHG nay: khong co bao
        int[] xz = ChgService.lairXZ(ow, lair);
        STORMS.put(a.key(), new Storm(xz[0], xz[1], lair));
        for (ServerPlayer p : ow.players())
            p.sendSystemMessage(Component.translatable("hukhong.cal.loi_vu.began", Component.translatable("entity.hukhong.chg_" + lair)));
    }

    public static void end(ServerLevel ow, CalendarData.Act a) {
        Storm s = STORMS.remove(a.key());
        if (s == null) return;
        for (ServerPlayer p : ow.players()) {
            Integer t = s.stay.get(p.getUUID());
            if (t == null || t < MIN_STAY || s.died.contains(p.getUUID()) || !p.isAlive()) continue;
            CultivationService.addExp(p, rewardExp(CultivationService.expToNext(CultivationService.get(p).level())));
            Wallet.grantHonTinh(p, 2);
            p.sendSystemMessage(Component.translatable("hukhong.cal.loi_vu.survived"));
            com.hukhong.story.chain.CityArcs.onLoiVuSurvived(p, s.lair); // cung Lac Nhat Thanh "Loi Vu Viem Son"

        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) { STORMS.clear(); }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (STORMS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        for (Storm s : STORMS.values()) {
            double dx = p.getX() - s.cx, dz = p.getZ() - s.cz;
            if (dx * dx + dz * dz <= (double) ZONE_RADIUS * ZONE_RADIUS) s.died.add(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (STORMS.isEmpty()) return;
        ServerLevel ow = e.getServer().overworld();
        if (ow == null) return;
        long t = ow.getGameTime();
        if (t % 10 != 0) return;
        for (Storm s : STORMS.values()) {
            for (ServerPlayer p : ow.players()) {
                double dx = p.getX() - s.cx, dz = p.getZ() - s.cz;
                if (!p.isAlive() || p.isSpectator() || dx * dx + dz * dz > (double) ZONE_RADIUS * ZONE_RADIUS) continue;
                s.stay.merge(p.getUUID(), 10, Integer::sum);
            }
            if (!ow.hasChunk(s.cx >> 4, s.cz >> 4)) continue;
            if (t % STRIKE_EVERY == 0) strike(ow, s);
        }
    }

    private static void strike(ServerLevel ow, Storm s) {
        double ang = ow.random.nextDouble() * Math.PI * 2, rad = ow.random.nextDouble() * STORM_RADIUS;
        double x = s.cx + Math.cos(ang) * rad, z = s.cz + Math.sin(ang) * rad;
        // 40%: roi gan mot nguoi choi dang o trong vung (lech 2..6 block, tranh ban trung tuc thi)
        for (ServerPlayer p : ow.players()) {
            double dx = p.getX() - s.cx, dz = p.getZ() - s.cz;
            if (dx * dx + dz * dz <= (double) ZONE_RADIUS * ZONE_RADIUS && ow.random.nextDouble() < 0.4) {
                double a2 = ow.random.nextDouble() * Math.PI * 2, r2 = 2 + ow.random.nextDouble() * 4;
                x = p.getX() + Math.cos(a2) * r2; z = p.getZ() + Math.sin(a2) * r2;
                break;
            }
        }
        if (!ow.hasChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4)) return;
        int y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(ow);
        if (bolt == null) return;
        bolt.moveTo(x, y, z);
        bolt.setVisualOnly(true);
        ow.addFreshEntity(bolt);
        ow.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1, z, 20, 1.2, 1, 1.2, 0.2);
        for (ServerPlayer p : ow.players()) {
            if (!p.isAlive() || p.isSpectator() || p.distanceToSqr(x, y, z) > 3.5 * 3.5) continue;
            float dmg = Math.min(strikeDamage(p.getMaxHealth()), p.getHealth() - 1f); // khong gay chet truc tiep
            if (dmg > 0f) p.hurt(ow.damageSources().lightningBolt(), dmg);
        }
    }
}
