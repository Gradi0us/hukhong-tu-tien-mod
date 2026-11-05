package com.hukhong.society;

import com.hukhong.HuKhong;
import com.hukhong.city.WorldBuildData;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Tông môn bản lớn (>= 100 ô): đứng trong Linh Khí Hồ (trên cầu / bờ hồ / dưới nước) hoặc trên Ấn Hồn Đài thì mỗi 10s
 * được cộng một phần trăm kinh nghiệm của cấp hiện tại. Vị trí lấy theo tâm tông môn (WorldBuildData) + hằng số SectBuilder.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SectZones {
    private SectZones() {}

    public static final int PERIOD = 200;
    /** % EXP cần để lên cấp, mỗi 10s. */
    public static final double LAKE_PCT = 0.005, ALTAR_PCT = 0.01;
    public static final int MIN_SIZE = 100;

    public enum Zone { NONE, LAKE, ALTAR }

    /** Vùng tông môn mà người chơi đang đứng. */
    public static Zone zoneOf(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return Zone.NONE;
        for (WorldBuildData.Settlement s : WorldBuildData.get(sl).settlements().values()) {
            if (!"sect".equals(s.kind()) || s.maxX() - s.minX() < MIN_SIZE) continue;
            if (p.getX() < s.minX() || p.getX() > s.maxX() + 1 || p.getZ() < s.minZ() || p.getZ() > s.maxZ() + 1) continue;
            double dx = p.getX() - (s.ax() + 0.5), dz = p.getZ() - (s.az() + 0.5), dy = p.getY() - s.y();
            if (Math.abs(dy) > 8) continue;
            if (Math.hypot(dx, dz - SectBuilder.ALTAR_Z) <= SectBuilder.ALTAR_R + 2.5 && dy >= 1) return Zone.ALTAR;
            if (Math.abs(dx) <= SectBuilder.LAKE_HALF_X + 1.5 && dz >= SectBuilder.LAKE_Z0 - 1.5 && dz <= SectBuilder.LAKE_Z1 + 1.5) return Zone.LAKE;
        }
        return Zone.NONE;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % PERIOD == 0) tick(p);
    }

    /** Cộng EXP nếu đang ở vùng; trả lượng đã cộng (test gọi tay). */
    public static long tick(ServerPlayer p) {
        Zone z = zoneOf(p);
        if (z == Zone.NONE || !(p.level() instanceof ServerLevel sl)) return 0;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        long amt = Math.max(1, Math.round(CultivationService.expToNext(Math.max(1, d.level())) * (z == Zone.ALTAR ? ALTAR_PCT : LAKE_PCT)));
        CultivationService.addExp(p, amt);
        p.displayClientMessage(Component.translatable(z == Zone.ALTAR ? "hukhong.sect.zone.altar" : "hukhong.sect.zone.lake",
                z == Zone.ALTAR ? "1" : "0.5").withColor(z == Zone.ALTAR ? 0x8FE6FF : 0x7FE0C0), true);
        sl.sendParticles(z == Zone.ALTAR ? ParticleTypes.SOUL : ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 8, 0.4, 0.6, 0.4, 0.02);
        return amt;
    }
}
