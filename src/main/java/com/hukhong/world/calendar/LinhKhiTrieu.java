package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Linh Khi Trieu: vung suong ban kinh 64 quanh diem su kien, EXP tu luyen x1.5 ben trong. Khong giu trang thai: doc thang CalendarData.active(). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LinhKhiTrieu {
    private LinhKhiTrieu() {}

    public static final int RADIUS = 64;
    public static final double MULT = 1.5;
    public static final String TYPE = "linh_khi_trieu";
    private static final Set<UUID> INSIDE = new HashSet<>();

    /** Thuan: EXP sau khi nhan he so (lam tron xuong, toi thieu bang goc). */
    public static long boosted(long amount) { return amount <= 0 ? amount : Math.max(amount, (long) Math.floor(amount * MULT)); }

    /** Thuan: (x,z) co nam trong vung ban kinh RADIUS quanh (cx,cz)? */
    public static boolean inZone(double x, double z, double cx, double cz) {
        double dx = x - cx, dz = z - cz;
        return dx * dx + dz * dz <= (double) RADIUS * RADIUS;
    }

    public static boolean inside(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD || p.getServer() == null) return false;
        ServerLevel ow = p.getServer().overworld();
        for (CalendarData.Act a : CalendarData.get(ow).active().values())
            if (TYPE.equals(a.defId()) && inZone(p.getX(), p.getZ(), a.x(), a.z())) return true;
        return false;
    }

    /** Goi tu CultivationService.addExp. */
    public static long boost(ServerPlayer p, long amount) { return inside(p) ? boosted(amount) : amount; }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().overworld();
        if (ow == null || ow.getGameTime() % 10 != 0) return;
        boolean any = false;
        for (CalendarData.Act a : CalendarData.get(ow).active().values()) if (TYPE.equals(a.defId())) { any = true; break; }
        if (!any) { INSIDE.clear(); return; }
        DustParticleOptions mist = new DustParticleOptions(new Vector3f(0.45f, 0.95f, 0.9f), 1.6f);
        for (ServerPlayer p : ow.players()) {
            if (!inside(p)) { INSIDE.remove(p.getUUID()); continue; }
            if (INSIDE.add(p.getUUID())) p.displayClientMessage(Component.translatable("hukhong.cal.linh_khi.enter"), true);
            ow.sendParticles(mist, p.getX(), p.getY() + 1.0, p.getZ(), 10, 6, 1.5, 6, 0.0);
            ow.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.5, p.getZ(), 2, 8, 2, 8, 0.01);
        }
    }
}
