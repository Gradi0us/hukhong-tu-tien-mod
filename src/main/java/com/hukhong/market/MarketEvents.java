package com.hukhong.market;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.city.MarketDistrict;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Dung Khu Cho lazily: khi nguoi choi o gan mot thanh da dung (chunk quanh do da nap) ma thanh chua co khu cho thi dung
 * (the gioi cu cung vay) va don thuong nhan cu quanh An Hon Dai. Moi lan kiem toi da 1 khu cho moi.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MarketEvents {
    private MarketEvents() {}

    /** Khoang (block, Chebyshev) toi tam thanh de bat dau dung/duy tri khu cho (market toi da 70 + 16). */
    public static final int NEAR = 110;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int t = server.getTickCount();
        if (t % 100 != 37 || t < 300) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        for (CityDef d : CityDef.ALL) {
            if (!MarketDistrict.eligible(d)) continue;
            BlockPos c = CityService.builtCenter(ow, d);
            if (c == null) continue;
            boolean near = false;
            for (ServerPlayer p : ow.players())
                if (Math.abs(p.getX() - c.getX()) <= NEAR && Math.abs(p.getZ() - c.getZ()) <= NEAR) { near = true; break; }
            if (!near) continue;
            boolean wasBuilt = com.hukhong.city.MarketDistricts.get(ow).built(d.id()) != null;
            try {
                if (MarketDistrict.ensure(ow, d) && !wasBuilt) return; // moi lan toi da 1 khu cho moi dung
            } catch (RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().error("Market ensure failed for {}: {}", d.id(), ex.toString(), ex);
            }
        }
    }
}
