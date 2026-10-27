package com.hukhong.client.render.orbit;

import com.hukhong.HuKhong;
import com.hukhong.banmenh.BanMenhKiemItem;
import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.banmenh.BanMenhType;
import com.hukhong.client.ClientConfig;
import com.hukhong.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Kiếm tu: khi Bản Mệnh Kiếm nằm trong tay (ô hotbar đang chọn) thì KHÔNG vẽ kiếm trong tay (model ẩn ở các display
 * tay) mà vẽ các thanh kiếm bay quanh người + vệt hạt theo màu loại kiếm. Vẽ theo không gian thế giới ở
 * AFTER_ENTITIES nên thấy được cả góc nhìn thứ nhất lẫn người chơi khác.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class OrbitRenderer {
    private OrbitRenderer() {}

    private static final double VIEW_RANGE = 64;

    /** Kiếm hợp lệ trong tay chính hoặc tay phụ, hoặc rỗng. */
    public static ItemStack heldSword(Player p) {
        ItemStack s = p.getMainHandItem();
        if (!s.is(ModItems.BAN_MENH_KIEM.get())) s = p.getOffhandItem();
        if (!s.is(ModItems.BAN_MENH_KIEM.get()) || BanMenhKiemItem.typeOf(s) == null) return ItemStack.EMPTY;
        return s;
    }

    private static boolean visible(Player p) {
        return p.isAlive() && !p.isInvisible() && !p.isSpectator();
    }

    /** Vị trí thế giới của kiếm idx tại time = gameTime + partial (đã gồm đoạn phi kiếm nếu có). */
    static Vec3 swordPos(ClientLevel level, Player pl, int idx, int count, double time, float partial,
                         OrbitState.Strike st) {
        double[] o = new double[3];
        OrbitMath.orbit(idx, count, time, o);
        Vec3 pos = pl.getPosition(partial).add(o[0], o[1], o[2]);
        if (st != null && st.slot() % count == idx) {
            double k = OrbitMath.strikeBlend(time - st.start(), BanMenhMath.STRIKE_DELAY_TICKS, BanMenhMath.STRIKE_RETURN_TICKS);
            if (k >= 0) {
                Entity t = level.getEntity(st.targetId());
                if (t != null) pos = pos.lerp(t.getPosition(partial).add(0, t.getBbHeight() * 0.55, 0), k);
            }
        }
        return pos;
    }

    /** Quaternion: mũi kiếm (+Y sau khi xoay 45 độ) hướng theo dir, mặt phẳng kiếm quay về phía camera. */
    private static Quaternionf orient(Vec3 dir, Vec3 toCam) {
        Vector3f u = dir.lengthSqr() < 1.0E-8 ? new Vector3f(0, 1, 0) : new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize();
        Vector3f c = new Vector3f((float) toCam.x, (float) toCam.y, (float) toCam.z);
        Vector3f n = c.sub(new Vector3f(u).mul(c.dot(u)));
        if (n.lengthSquared() < 1.0E-6f) {
            n = u.cross(0, 1, 0, new Vector3f());
            if (n.lengthSquared() < 1.0E-6f) n = u.cross(1, 0, 0, new Vector3f());
        }
        n.normalize();
        Vector3f x = u.cross(n, new Vector3f());
        return new Quaternionf().setFromNormalized(new Matrix3f(x, u, n));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.players().isEmpty()) return;
        float partial = e.getPartialTick().getGameTimeDeltaPartialTick(true);
        double time = level.getGameTime() + partial;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        boolean any = false;
        for (AbstractClientPlayer pl : level.players()) {
            if (pl.distanceToSqr(cam) > VIEW_RANGE * VIEW_RANGE || !visible(pl)) continue;
            ItemStack sword = heldSword(pl);
            if (sword.isEmpty()) continue;
            int stage = BanMenhKiemItem.stageOf(sword);
            int count = BanMenhMath.orbitCount(stage);
            ItemStack one = sword.copyWithCount(1);
            int light = stage >= 4 ? LightTexture.FULL_BRIGHT
                    : LevelRenderer.getLightColor(level, BlockPos.containing(pl.getX(), pl.getEyeY(), pl.getZ()));
            float scale = 0.45f + 0.07f * stage;
            OrbitState.Strike st = OrbitState.get(pl.getId(), time);
            for (int i = 0; i < count; i++) {
                Vec3 pos = swordPos(level, pl, i, count, time, partial, st);
                Vec3 dir = swordPos(level, pl, i, count, time + 0.5, partial, st)
                        .subtract(swordPos(level, pl, i, count, time - 0.5, partial, st));
                Vec3 toCam = cam.subtract(pos);
                if (pl == mc.player && toCam.lengthSqr() < 0.36) continue; // quá sát mắt: bỏ để khỏi che tầm nhìn
                ps.pushPose();
                ps.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
                ps.mulPose(orient(dir, toCam));
                ps.mulPose(Axis.ZP.rotationDegrees(45f)); // mũi kiếm trên texture nằm chéo phải-trên
                ps.scale(scale, scale, scale);
                mc.getItemRenderer().renderStatic(one, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                        ps, buf, level, pl.getId() + i);
                ps.popPose();
                any = true;
            }
        }
        if (any) buf.endBatch();
    }

    // ------------------------------------------------------------------ vệt hạt

    private static Vector3f color(BanMenhType t) {
        return switch (t) {
            case THANH_PHONG -> new Vector3f(0.35f, 1.0f, 0.75f);
            case LIET_DIEM -> new Vector3f(1.0f, 0.45f, 0.08f);
            case HAN_BANG -> new Vector3f(0.6f, 0.9f, 1.0f);
            case LOI_DINH -> new Vector3f(1.0f, 0.93f, 0.3f);
            case HUYET_ANH -> new Vector3f(0.85f, 0.04f, 0.1f);
            case TRONG_NHAC -> new Vector3f(0.75f, 0.62f, 0.4f);
            case NGOC_TAM -> new Vector3f(0.25f, 0.95f, 0.85f);
            case TINH_THAN -> new Vector3f(0.7f, 0.5f, 1.0f);
        };
    }

    private static ParticleOptions accent(BanMenhType t) {
        return switch (t) {
            case THANH_PHONG -> ParticleTypes.CLOUD;
            case LIET_DIEM -> ParticleTypes.FLAME;
            case HAN_BANG -> ParticleTypes.SNOWFLAKE;
            case LOI_DINH -> ParticleTypes.ELECTRIC_SPARK;
            case HUYET_ANH -> ParticleTypes.DAMAGE_INDICATOR;
            case TRONG_NHAC -> ParticleTypes.ASH;
            case NGOC_TAM -> ParticleTypes.END_ROD;
            case TINH_THAN -> ParticleTypes.GLOW;
        };
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) return;
        boolean low = ClientConfig.particles() == ClientConfig.ParticleLevel.LOW;
        double time = level.getGameTime();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (AbstractClientPlayer pl : level.players()) {
            if (pl.distanceToSqr(cam) > 40 * 40 || !visible(pl)) continue;
            ItemStack sword = heldSword(pl);
            if (sword.isEmpty()) continue;
            BanMenhType type = BanMenhKiemItem.typeOf(sword);
            int stage = BanMenhKiemItem.stageOf(sword);
            int count = BanMenhMath.orbitCount(stage);
            OrbitState.Strike st = OrbitState.get(pl.getId(), time);
            DustParticleOptions dust = new DustParticleOptions(color(type), 0.7f + 0.08f * stage);
            for (int i = 0; i < count; i++) {
                if (low && level.random.nextInt(3) != 0) continue;
                Vec3 p = swordPos(level, pl, i, count, time, 0f, st);
                Vec3 prev = swordPos(level, pl, i, count, time - 1, 0f, st);
                // hai hạt dọc đường đi trong tick vừa rồi -> vệt liền
                level.addParticle(dust, p.x, p.y, p.z, 0, 0, 0);
                level.addParticle(dust, (p.x + prev.x) / 2, (p.y + prev.y) / 2, (p.z + prev.z) / 2, 0, 0, 0);
                if (level.random.nextInt(stage >= 4 ? 3 : 6) == 0)
                    level.addParticle(accent(type), p.x, p.y, p.z, (level.random.nextDouble() - 0.5) * 0.02,
                            (level.random.nextDouble() - 0.5) * 0.02, (level.random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) {
        OrbitState.clear();
    }
}
