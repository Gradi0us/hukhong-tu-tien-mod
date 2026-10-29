package com.hukhong.story.world;

import com.hukhong.city.CityDef;
import com.hukhong.city.CityRegistryData;
import com.hukhong.events.CorruptionData;
import com.hukhong.story.StoryNet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Thế Lực Hư Ảnh: thanh 0..100 theo thành (PLAN-v2 §2.3), lưu trong CorruptionData, đồng bộ xuống client (ClientHuAnh). */
@EventBusSubscriber(modid = com.hukhong.HuKhong.MODID)
public final class HuAnh {
    private HuAnh() {}

    public static int get(ServerLevel ow, int cityIdx) { return CorruptionData.get(ow.getServer().overworld()).huAnh(cityIdx); }

    /** Cộng/trừ (kẹp 0..100) và đồng bộ cho mọi người chơi. Trả giá trị mới. */
    public static int add(ServerLevel ow, int cityIdx, int delta) {
        ServerLevel o = ow.getServer().overworld();
        CorruptionData cd = CorruptionData.get(o);
        if (cd.setHuAnh(cityIdx, cd.huAnh(cityIdx) + delta)) syncAll(o);
        return cd.huAnh(cityIdx);
    }

    /** Chỉ số thành gần vị trí nhất (tâm thành đã xây, chưa xây thì neo tương đối spawn). */
    public static int nearestCity(ServerLevel ow, BlockPos pos) {
        ServerLevel o = ow.getServer().overworld();
        CityRegistryData reg = CityRegistryData.get(o);
        BlockPos sp = o.getSharedSpawnPos();
        int best = 0; double bd = Double.MAX_VALUE;
        for (CityDef c : CityDef.ALL) {
            BlockPos ctr = reg.center(c.id());
            double cx = ctr != null ? ctr.getX() : sp.getX() + c.ax(), cz = ctr != null ? ctr.getZ() : sp.getZ() + c.az();
            double d = (cx - pos.getX()) * (cx - pos.getX()) + (cz - pos.getZ()) * (cz - pos.getZ());
            if (d < bd) { bd = d; best = c.index(); }
        }
        return best;
    }

    public static int average(ServerLevel ow) {
        int[] v = CorruptionData.get(ow.getServer().overworld()).huAnhAll();
        int s = 0; for (int x : v) s += x;
        return v.length == 0 ? 0 : s / v.length;
    }

    /** Tiện: đổi Hư Ảnh của thành gần {@code pos}. */
    public static void addNear(ServerLevel ow, BlockPos pos, int delta) { add(ow, nearestCity(ow, pos), delta); }

    static void syncAll(ServerLevel o) {
        PacketDistributor.sendToAllPlayers(new StoryNet.HuAnhSync(CorruptionData.get(o).huAnhAll()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel o = p.getServer().getLevel(Level.OVERWORLD);
        if (o != null) PacketDistributor.sendToPlayer(p, new StoryNet.HuAnhSync(CorruptionData.get(o).huAnhAll()));
    }
}
