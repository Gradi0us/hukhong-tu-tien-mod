package com.hukhong.flight.client;

import com.hukhong.HuKhong;
import com.hukhong.flight.FlightClientState;
import com.hukhong.flight.FlightJumpPayload;
import com.hukhong.flight.FlightMode;
import com.hukhong.flight.FlightTogglePayload;
import com.hukhong.vfx.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client: gửi phím Z / nhảy bậc không, sinh particle vệt Kiếm Khí và Ma Vân. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class FlightClient {
    private FlightClient() {}

    private static boolean prevJump;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            while (FlightKeys.TOGGLE.consumeClick()) { }
            prevJump = false;
            return;
        }
        while (FlightKeys.TOGGLE.consumeClick()) {
            if (mc.screen == null) PacketDistributor.sendToServer(new FlightTogglePayload());
        }
        var mine = FlightClientState.get(mc.player.getId());
        boolean down = mc.options.keyJump.isDown() && mc.screen == null;
        if (down && !prevJump && mine != null && mine.mode() == FlightMode.THE
                && !mc.player.onGround() && !mc.player.getAbilities().flying) {
            PacketDistributor.sendToServer(new FlightJumpPayload());
        }
        prevJump = down;

        var it = FlightClientState.all().entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            if (!(mc.level.getEntity(en.getKey()) instanceof AbstractClientPlayer p)) { it.remove(); continue; }
            FlightMode m = en.getValue().mode();
            var rnd = mc.level.random;
            if (m == FlightMode.KIEM) {
                mc.level.addParticle(ModParticles.KIEM_KHI.get(), p.getX() + (rnd.nextDouble() - 0.5) * 0.4,
                        p.getY() + 0.1, p.getZ() + (rnd.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
            } else if (m == FlightMode.MA) {
                mc.level.addParticle(ParticleTypes.LARGE_SMOKE, p.getX() + (rnd.nextDouble() - 0.5) * 0.9,
                        p.getY() + 0.05, p.getZ() + (rnd.nextDouble() - 0.5) * 0.9, 0, 0.005, 0);
                if (rnd.nextInt(3) == 0) {
                    mc.level.addParticle(ParticleTypes.SQUID_INK, p.getX() + (rnd.nextDouble() - 0.5) * 0.6,
                            p.getY() + 0.2, p.getZ() + (rnd.nextDouble() - 0.5) * 0.6, 0, -0.01, 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { FlightClientState.clear(); }
}
