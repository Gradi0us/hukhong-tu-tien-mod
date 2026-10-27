package com.hukhong.client.render.orbit;

import com.hukhong.HuKhong;
import com.hukhong.banmenh.BanMenhKiemItem;
import com.hukhong.client.ClientConfig;
import com.hukhong.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Kiếm tu: khi Bản Mệnh Kiếm được chọn thì tay không cầm gì; một thanh kiếm treo nhẹ sau vai (không hạt).
 * Khi tấn công kiếm phóng từ sau vai theo cung ease-in-out, mũi theo hướng bay, đâm mục tiêu rồi quay về.
 * Phi kiếm kỹ năng (FxPayload SWORD_FLY) dùng cùng bộ vẽ. Vệt = 2-3 bóng kiếm mờ, không hạt, không chớp màn hình.
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

    /** Vị trí kiếm treo sau vai phải (có dao động nhẹ). */
    static Vec3 shoulder(Player pl, float partial, double time) {
        double yaw = Math.toRadians(Mth.rotLerp(partial, pl.yBodyRotO, pl.yBodyRot));
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        double h = (pl.isCrouching() ? 1.3 : 1.55) + OrbitMath.bob(time, pl.getId());
        return pl.getPosition(partial).add(right.scale(0.6)).subtract(fwd.scale(0.45)).add(0, h, 0);
    }

    /** Vị trí kiếm tại time; null nếu chưa xuất hiện. */
    static Vec3 pos(ClientLevel level, OrbitState.Flight f, double time, float partial) {
        if (f.slash) return slashPos(level, f, time, partial);
        double t = time - f.start;
        Vec3 a;
        if (f.owner >= 0) {
            if (!(level.getEntity(f.owner) instanceof Player o)) return null;
            a = shoulder(o, partial, time);
            if (f.target >= 0) {
                Entity te = level.getEntity(f.target);
                if (te != null) f.to = te.getPosition(partial).add(0, te.getBbHeight() * 0.55, 0);
            }
            if (t <= 0) return a;
        } else {
            if (t < 0) return null;
            a = f.from;
        }
        Vec3 b = f.to;
        Vec3 d = b.subtract(a);
        double len = d.length();
        Vec3 perp = d.cross(new Vec3(0, 1, 0));
        perp = perp.lengthSqr() < 1e-6 ? Vec3.ZERO : perp.normalize();
        Vec3 bulge = perp.scale(0.16 * f.side).add(0, 0.1, 0).scale(len);
        if (t < f.flight) {
            double e = OrbitMath.easeInOut(t / f.flight);
            return a.add(d.scale(e)).add(bulge.scale(Math.sin(Math.PI * e)));
        }
        if (f.owner >= 0 && t < f.flight + f.ret) {
            double u = OrbitMath.easeInOut((t - f.flight) / f.ret);
            return b.add(a.subtract(b).scale(u)).add(bulge.scale(0.5 * Math.sin(Math.PI * u)));
        }
        return b; // kiếm kỹ năng cắm lại ở đích
    }

    /** Tâm xoay của cung chém: ngang ngực chủ. */
    static Vec3 pivot(Player o, float partial) { return o.getEyePosition(partial).subtract(0, 0.55, 0); }

    /** Vị trí kiếm khi chém: vai -> điểm đầu cung (WIND) -> quét cung trước mặt -> về vai. */
    static Vec3 slashPos(ClientLevel level, OrbitState.Flight f, double time, float partial) {
        if (!(level.getEntity(f.owner) instanceof Player o)) return null;
        double t = time - f.start;
        Vec3 sh = shoulder(o, partial, time);
        if (t <= 0) return sh;
        Vec3 piv = pivot(o, partial);
        Vec3 look = o.getViewVector(partial);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (f.target >= 0 && level.getEntity(f.target) instanceof Entity te) {
            Vec3 d = te.getPosition(partial).subtract(o.getPosition(partial));
            if (d.x * d.x + d.z * d.z > 0.04) flat = new Vec3(d.x, 0, d.z);
        }
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 fwd = new Vec3(flat.x, Mth.clamp(look.y, -0.5, 0.5) * 0.5, flat.z).normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        double sign = f.roll < 0 ? -1 : 1, tilt = Math.toRadians(Math.abs(f.roll));
        Vec3 w = right.scale(Math.cos(tilt) * sign).add(0, Math.sin(tilt), 0); // phía bắt đầu: cao; phía kết thúc: thấp
        double A = 1.15, R = f.radius;
        Vec3 pa = pointOnArc(piv, fwd, w, R, A), pb = pointOnArc(piv, fwd, w, R, -A);
        double wind = OrbitState.Flight.WIND;
        if (t < wind) return sh.add(pa.subtract(sh).scale(OrbitMath.easeInOut(t / wind)));
        if (t < wind + f.flight) {
            double e = OrbitMath.easeInOut((t - wind) / f.flight);
            return pointOnArc(piv, fwd, w, R, A - 2 * A * e);
        }
        if (t < wind + f.flight + f.ret) {
            double u = OrbitMath.easeInOut((t - wind - f.flight) / f.ret);
            return pb.add(sh.subtract(pb).scale(u));
        }
        return sh;
    }

    /** Điểm trên cung góc a (a>0 phía đầu cung). */
    private static Vec3 pointOnArc(Vec3 piv, Vec3 fwd, Vec3 w, double R, double a) {
        return piv.add(fwd.scale(Math.cos(a) * R)).add(w.scale(Math.sin(a) * R));
    }

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

    /** Bọc buffer để vẽ model item bán trong suốt (bóng vệt / mờ dần). */
    private static final class Fade implements MultiBufferSource {
        private final MultiBufferSource.BufferSource src;
        private final float alpha;
        Fade(MultiBufferSource.BufferSource src, float alpha) { this.src = src; this.alpha = alpha; }
        @Override public VertexConsumer getBuffer(RenderType rt) {
            return new Alpha(src.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)), alpha);
        }
    }

    private static final class Alpha implements VertexConsumer {
        private final VertexConsumer d;
        private final float a;
        Alpha(VertexConsumer d, float a) { this.d = d; this.a = a; }
        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int al) { d.setColor(r, g, b, (int) (al * a)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
    }

    private static void draw(Minecraft mc, ClientLevel level, PoseStack ps, MultiBufferSource.BufferSource buf, ItemStack st,
                             Vec3 pos, Vec3 dir, Vec3 cam, float scale, float alpha, float roll, int seed, boolean local) {
        Vec3 toCam = cam.subtract(pos);
        if (local && toCam.lengthSqr() < (scale > 1.6f ? 9.0 : 0.36)) return; // quá sát mắt: bỏ để khỏi che tầm nhìn
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(pos)); // không còn FULL_BRIGHT
        MultiBufferSource src = alpha >= 0.98f ? buf : new Fade(buf, alpha);
        ps.pushPose();
        ps.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        ps.mulPose(orient(dir, toCam));
        ps.mulPose(Axis.YP.rotationDegrees(roll));
        ps.mulPose(Axis.ZP.rotationDegrees(45f)); // mũi kiếm trên texture nằm chéo phải-trên
        ps.scale(scale, scale, scale);
        mc.getItemRenderer().renderStatic(st, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, src, level, seed);
        ps.popPose();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = e.getPartialTick().getGameTimeDeltaPartialTick(true);
        double time = level.getGameTime() + partial;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        OrbitState.prune(level.getGameTime() + 1.0E-3);
        boolean any = false;
        int ghosts = switch (ClientConfig.fxQuality()) { case LOW -> 0; case MED -> 2; default -> 3; };

        // kiếm treo sau vai
        for (AbstractClientPlayer pl : level.players()) {
            if (pl.distanceToSqr(cam) > VIEW_RANGE * VIEW_RANGE || !visible(pl)) continue;
            ItemStack sword = heldSword(pl);
            if (sword.isEmpty() || OrbitState.busy(pl.getId(), time)) continue;
            int stage = BanMenhKiemItem.stageOf(sword);
            Vec3 pos = shoulder(pl, partial, time);
            double yaw = Math.toRadians(Mth.rotLerp(partial, pl.yBodyRotO, pl.yBodyRot));
            Vec3 dir = new Vec3(Math.sin(yaw) * 0.3, 1, -Math.cos(yaw) * 0.3); // mũi hướng lên, hơi ngả ra sau
            draw(mc, level, ps, buf, sword.copyWithCount(1), pos, dir, cam, 0.4f + 0.05f * stage, 1f, 0f, pl.getId(), pl == mc.player);
            any = true;
        }

        // phi kiếm đang bay
        for (OrbitState.Flight f : OrbitState.all()) {
            ItemStack st;
            Player owner = null;
            if (f.owner >= 0) {
                if (!(level.getEntity(f.owner) instanceof Player o) || !visible(o)) continue;
                owner = o;
                st = heldSword(o);
                if (st.isEmpty()) {
                    if (!f.slash) continue;
                    st = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD); // kỹ năng chém khi chưa cầm Bản Mệnh Kiếm
                }
                st = st.copyWithCount(1);
            } else st = f.stack;
            Vec3 pos = pos(level, f, time, partial);
            if (pos == null || pos.distanceToSqr(cam) > VIEW_RANGE * VIEW_RANGE) continue;
            double t = time - f.start;
            if (f.slash && t <= 0) continue;
            Vec3 ahead = pos(level, f, time + 0.6, partial);
            Vec3 behind = pos(level, f, time - 0.6, partial);
            Vec3 v = ahead == null || behind == null ? Vec3.ZERO : ahead.subtract(behind);
            if (v.lengthSqr() < 1e-5) v = f.to.subtract(owner != null ? shoulder(owner, partial, time) : f.from);
            Vec3 piv = f.slash && owner != null ? pivot(owner, partial) : null;
            if (piv != null) v = pos.subtract(piv); // kiếm chém: thân kiếm hướng ra ngoài từ người chủ
            int stage = owner == null ? 3 : (st.is(ModItems.BAN_MENH_KIEM.get()) ? BanMenhKiemItem.stageOf(st) : 0);
            float scale = (0.4f + 0.05f * stage) * f.size;
            float alpha = f.owner >= 0 ? (f.giant ? 0.8f : 1f) : Mth.clamp((float) (f.flight + f.hold - t) / (f.hold > 10 ? 8f : 3f), 0f, 1f);
            float roll = (float) (Math.sin(time * 0.35 + f.start) * 10);
            boolean moving = t > 0 && (f.slash ? t < OrbitState.Flight.WIND + f.flight + f.ret : (t < f.flight || (f.owner >= 0 && t < f.flight + f.ret)));
            if (moving) for (int k = ghosts; k >= 1; k--) {
                double tg = time - k * 0.85;
                Vec3 gp = pos(level, f, tg, partial);
                if (gp == null) continue;
                Vec3 gv = pos(level, f, tg + 0.6, partial);
                Vec3 gb = pos(level, f, tg - 0.6, partial);
                Vec3 gd = gv == null || gb == null ? v : gv.subtract(gb);
                if (gd.lengthSqr() < 1e-5) gd = v;
                if (piv != null) gd = gp.subtract(piv);
                draw(mc, level, ps, buf, st, gp, gd, cam, scale, 0.2f * (ghosts - k + 1) / ghosts, roll, f.owner + k, false);
            }
            draw(mc, level, ps, buf, st, pos, v, cam, scale, alpha, roll, f.owner, owner != null && owner == mc.player);
            any = true;
        }
        if (any) buf.endBatch();
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) {
        OrbitState.clear();
    }
}
