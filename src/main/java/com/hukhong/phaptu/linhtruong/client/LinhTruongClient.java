package com.hukhong.phaptu.linhtruong.client;

import com.hukhong.HuKhong;
import com.hukhong.artifact.ArtifactAttachments;
import com.hukhong.client.ClientCache;
import com.hukhong.client.ClientConfig;
import com.hukhong.client.render.orbit.OrbitClientSetup;
import com.hukhong.core.Path;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.phaptu.linhtruong.LinhTruongBoltPayload;
import com.hukhong.phaptu.linhtruong.LinhTruongItem;
import com.hukhong.phaptu.linhtruong.LinhTruongMath;
import com.hukhong.phaptu.linhtruong.LinhTruongRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/** Phía client của Linh Trượng: chuột trái = tia phép (giữ chuột bắn liên tục theo hồi chiêu), hạt chờ quanh đầu trượng, property model. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class LinhTruongClient {
    private LinhTruongClient() {}

    private static long nextFire = 0;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> ItemProperties.register(LinhTruongRegistry.LINH_TRUONG.get(), OrbitClientSetup.STAGE_PROP,
                (stack, level, entity, seed) -> LinhTruongItem.stageOf(stack)));
    }

    private static boolean holdingOwn(Minecraft mc) {
        var p = mc.player;
        if (p == null || mc.level == null || mc.screen != null || p.isSpectator()) return false;
        ItemStack s = p.getMainHandItem();
        return s.is(LinhTruongRegistry.LINH_TRUONG.get()) && LinhTruongItem.isOwner(s, p)
                && ClientCache.cultivation().paths().contains(Path.PHAP);
    }

    private static void fire(Minecraft mc) {
        long now = mc.level.getGameTime();
        if (now < nextFire) return;
        nextFire = now + LinhTruongMath.COOLDOWN_TICKS;
        mc.player.swing(InteractionHand.MAIN_HAND);
        PacketDistributor.sendToServer(new LinhTruongBoltPayload());
    }

    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        if (!holdingOwn(mc)) return;
        e.setCanceled(true);
        fire(mc);
    }

    /** Giữ chuột trái: bắn liên tục theo hồi chiêu. */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (holdingOwn(mc) && mc.options.keyAttack.isDown()) fire(mc);
        idle(mc);
    }

    // ------------------------------------------------------------------ hạt chờ

    private static void idle(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) return;
        long t = level.getGameTime();
        boolean low = ClientConfig.particles() == ClientConfig.ParticleLevel.LOW;
        if (t % (low ? 4 : 2) != 0) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        RandomSource r = level.random;
        for (AbstractClientPlayer p : level.players()) {
            if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(cam) > 40 * 40) continue;
            ItemStack s = p.getMainHandItem();
            if (!s.is(LinhTruongRegistry.LINH_TRUONG.get())) {
                s = p.getOffhandItem();
                if (!s.is(LinhTruongRegistry.LINH_TRUONG.get())) continue;
            }
            PhapElement el = LinhTruongItem.elementOf(s);
            if (el == null) el = PhapTu.rawElement(p);
            if (el == null) continue;
            int stage = Math.max(LinhTruongItem.stageOf(s), p.getData(ArtifactAttachments.ARTIFACT.get()).phap());
            Vec3 tip = LinhTruongMath.tip(p.getEyePosition(), p.getYRot(), p.getXRot());
            Vec3 look = p.getLookAngle();
            double yaw = Math.toRadians(p.getYRot());
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            Vec3 up = right.cross(look).normalize();
            var dust = new DustParticleOptions(new Vector3f(((el.rgb >> 16) & 255) / 255f, ((el.rgb >> 8) & 255) / 255f, (el.rgb & 255) / 255f), 0.8f);
            // quầng sáng nhỏ ở đầu trượng
            level.addParticle(dust, tip.x + (r.nextDouble() - 0.5) * 0.06, tip.y + (r.nextDouble() - 0.5) * 0.06,
                    tip.z + (r.nextDouble() - 0.5) * 0.06, 0, 0.004, 0);
            // hạt quay quanh đầu trượng
            int n = LinhTruongMath.motes(stage);
            double base = t * 0.22, rad = 0.17 + 0.012 * stage;
            for (int i = 0; i < n; i++) {
                double a = base + i * (Math.PI * 2 / n);
                Vec3 o = tip.add(right.scale(Math.cos(a) * rad)).add(up.scale(Math.sin(a) * rad))
                        .add(look.scale(Math.sin(a * 2) * 0.05));
                level.addParticle(dust, o.x, o.y, o.z, 0, 0, 0);
            }
            if (r.nextInt(low ? 6 : 3) == 0) level.addParticle(ParticleTypes.END_ROD, tip.x, tip.y, tip.z,
                    (r.nextDouble() - 0.5) * 0.02, 0.012, (r.nextDouble() - 0.5) * 0.02);
        }
    }
}
