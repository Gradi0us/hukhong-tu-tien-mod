package com.hukhong.city;

import com.hukhong.HuKhong;
import com.hukhong.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Sinh thành 1 lần, đăng ký Cư Dân, cột sáng particle trên Ấn Hồn Đài. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CityEvents {
    private CityEvents() {}

    /** Dựng thành nếu chưa có (hoặc force = dựng lại, trạng thái về NORMAL). */
    public static BlockPos ensureBuilt(MinecraftServer server, boolean force) {
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return null;
        CitySavedData d = CitySavedData.get(ow);
        if (d.isBuilt() && !force) return d.center();
        BlockPos site = d.isBuilt() ? d.center() : CityBuilder.chooseSite(ow);
        BlockPos c = CityBuilder.build(ow, site);
        d.markBuilt(c);
        com.mojang.logging.LogUtils.getLogger().info("Luu Ly Cang built at {}", c);
        return c;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int t = server.getTickCount();
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        if (t % 20 == 0) {
            CitySavedData d = CitySavedData.get(ow);
            if (!d.isBuilt()) {
                if (t >= 40) ensureBuilt(server, false);
                return;
            }
            for (ServerPlayer p : ow.players()) {
                if (d.inside(p.getX(), p.getZ()) && d.addResident(p.getUUID()))
                    p.sendSystemMessage(Component.translatable("hukhong.city.resident_joined"));
            }
        }
        if (t % 10 == 0) {
            CitySavedData d = CitySavedData.get(ow);
            if (!d.isBuilt()) return;
            BlockPos a = d.altarPos();
            var type = d.state() == CitySavedData.State.RUIN ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD;
            for (ServerPlayer p : ow.players()) {
                if (p.distanceToSqr(a.getX(), a.getY(), a.getZ()) > 128 * 128) continue;
                for (int i = 0; i < 24; i++)
                    ow.sendParticles(p, type, true, a.getX() + 0.5, a.getY() + 7 + i * 1.5, a.getZ() + 0.5, 1, 0.1, 0, 0.1, 0.0);
            }
        }
    }

    /** Bấm Ấn Hồn Đài lần đầu → đăng ký Cư Dân (không chặn tương tác). */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!e.getLevel().getBlockState(e.getPos()).is(ModBlocks.AN_HON_DAI.get())) return;
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        if (CitySavedData.get(ow).addResident(p.getUUID()))
            p.sendSystemMessage(Component.translatable("hukhong.city.resident_joined"));
    }
}
