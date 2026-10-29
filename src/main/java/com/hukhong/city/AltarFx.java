package com.hukhong.city;

import com.hukhong.HuKhong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Vong sang xoay tren Quang Truong An Hon lon: 16 diem lua hon + anh sang xoay cham quanh tam, o do cao 11 so voi san altar
 * (hat, khong phai khoi, nen "xoay" that su). Chi chay khi co nguoi choi trong 80 block cua dien dan.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class AltarFx {
    private AltarFx() {}

    public static final int POINTS = 16, RANGE = 80;

    /** Ban kinh vong theo vet T: R - 1 = T - 4. */
    public static double ringRadius(int plazaT) { return Math.max(4, plazaT - 4); }

    /** Toa do diem i cua vong tai thoi diem t (tick): xoay 1 vong / 30 giay. Thuan. */
    public static double[] point(double cx, double cz, double radius, int i, long t) {
        double ang = (t * 2 * Math.PI / 600.0) + i * 2 * Math.PI / POINTS;
        return new double[]{cx + Math.cos(ang) * radius, cz + Math.sin(ang) * radius};
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        long t = server.getTickCount();
        if (t % 4 != 0) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        MarketDistricts md = MarketDistricts.get(ow);
        for (var en : md.allPlaza().entrySet()) {
            if (en.getValue() < AltarShrine.MIN_GRAND + 3) continue; // chi dien dan lon moi co vong sang
            CityDef d = CityDef.byId(en.getKey());
            BlockPos c = d == null ? null : CityService.builtCenter(ow, d);
            if (c == null) continue;
            double r = ringRadius(en.getValue());
            double y = c.getY() + 12.5;
            for (ServerPlayer p : ow.players()) {
                if (Math.abs(p.getX() - c.getX()) > RANGE || Math.abs(p.getZ() - c.getZ()) > RANGE) continue;
                for (int i = 0; i < POINTS; i++) {
                    double[] pt = point(c.getX() + 0.5, c.getZ() + 0.5, r, i, t);
                    ow.sendParticles(p, i % 4 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD, false, pt[0], y, pt[1], 1, 0, 0, 0, 0);
                }
            }
        }
    }
}
