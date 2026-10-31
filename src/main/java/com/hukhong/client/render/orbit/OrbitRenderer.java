package com.hukhong.client.render.orbit;

import com.hukhong.HuKhong;
import com.hukhong.banmenh.BanMenhKiemItem;
import com.hukhong.client.ClientConfig;
import com.hukhong.client.fx.FxSword;
import com.hukhong.client.compat.ShaderCompat;
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

    /** Chiều dài thân kiếm (block) khi vẽ: ~1,6 block (Phàm) -> ~1,95 (Thần), gần bằng chiều cao người; kỹ năng cỡ lớn nhỉnh hơn. */
    static double bladeLen(int stage, float size) {
        double k = size <= 1f ? 1.0 : Math.min(1.45, 1.0 + (size - 1f) * 0.3);
        return (1.6 + 0.06 * stage) * k;
    }

    /** Tỉ lệ scale item FIXED (0,5 block/đơn vị, chéo 45 độ, thân kiếm ~90% đường chéo) -> chiều dài thân kiếm. */
    static final double LEN_PER_SCALE = 1.1;

    /** Màu hào quang theo loại Bản Mệnh Kiếm. */
    public static int colorOf(ItemStack s) {
        com.hukhong.banmenh.BanMenhType t = s.isEmpty() ? null : BanMenhKiemItem.typeOf(s);
        if (t == null) return 0x9FE8FF;
        return switch (t) {
            case THANH_PHONG -> 0x7FFFD0; case LIET_DIEM -> 0xFF8A3A; case HAN_BANG -> 0x9FE8FF; case LOI_DINH -> 0xC8A8FF;
            case HUYET_ANH -> 0xFF4060; case TRONG_NHAC -> 0xE0C070; case NGOC_TAM -> 0x70FFC8; case TINH_THAN -> 0xFFE9A0;
        };
    }

    /** Vị trí kiếm treo sau vai phải (lơ lửng, nhấp nhô nhẹ). Đặt xa thân/lùi sau để không chạm camera góc nhìn thứ nhất. */
    static Vec3 shoulder(Player pl, float partial, double time) {
        double yaw = Math.toRadians(Mth.rotLerp(partial, pl.yBodyRotO, pl.yBodyRot));
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        double h = (pl.isCrouching() ? 1.3 : 1.5) + OrbitMath.bob(time, pl.getId()) * 2.2;
        return pl.getPosition(partial).add(right.scale(1.0)).subtract(fwd.scale(0.7)).add(0, h, 0);
    }

    /** Hướng thân kiếm khi treo: mũi chếch lên, ra ngoài và hơi ra sau (như cánh), lắc nhẹ. */
    static Vec3 idleDir(Player pl, float partial, double time) {
        double yaw = Math.toRadians(Mth.rotLerp(partial, pl.yBodyRotO, pl.yBodyRot));
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        double sway = Math.sin(time * 0.1 + pl.getId());
        return new Vec3(0, 1, 0).add(right.scale(0.5 + 0.08 * sway)).add(fwd.scale(0.12)).normalize();
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

    /** Hệ toạ độ cung chém: piv (tâm), fwd (hướng chém), w (trục quét: +w là đầu cung), nrm bỏ qua. */
    record Frame(Vec3 piv, Vec3 fwd, Vec3 w) {}

    static Frame frame(ClientLevel level, OrbitState.Flight f, Player o, float partial) {
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
        double sign = f.roll < 0 ? -1 : 1;
        double tilt = Math.toRadians(Double.isNaN(f.tiltDeg) ? Math.abs(f.roll) : f.tiltDeg);
        Vec3 w = right.scale(Math.cos(tilt) * sign).add(0, Math.sin(tilt), 0); // phía bắt đầu: cao (tilt>0); phía kết thúc: thấp
        return new Frame(piv, fwd, w);
    }

    /** Vị trí kiếm khi chém: vai -> điểm đầu cung (WIND) -> quét cung trước mặt -> về vai. */
    static Vec3 slashPos(ClientLevel level, OrbitState.Flight f, double time, float partial) {
        if (!(level.getEntity(f.owner) instanceof Player o)) return null;
        double t = time - f.start;
        Vec3 sh = shoulder(o, partial, time);
        if (t <= 0) return sh;
        Frame fr = frame(level, f, o, partial);
        double A = 1.15, R = f.radius;
        Vec3 pa = pointOnArc(fr.piv, fr.fwd, fr.w, R, A), pb = pointOnArc(fr.piv, fr.fwd, fr.w, R, -A);
        double wind = OrbitState.Flight.WIND;
        if (t < wind) return sh.add(pa.subtract(sh).scale(OrbitMath.easeInOut(t / wind)));
        if (t < wind + f.flight) {
            double e = OrbitMath.easeInOut((t - wind) / f.flight);
            return pointOnArc(fr.piv, fr.fwd, fr.w, R, A - 2 * A * e);
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
        if (local && toCam.lengthSqr() < 1.0) return; // quá sát mắt (góc nhìn thứ nhất): bỏ để khỏi che tầm nhìn
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

    /** Hướng thân kiếm tại thời điểm tm (đơn vị). */
    private static Vec3 dirAt(ClientLevel level, OrbitState.Flight f, Player owner, double tm, float partial, Vec3 p) {
        if (f.slash && owner != null) {
            if (tm - f.start <= 0) return idleDir(owner, partial, tm);
            Vec3 d = p.subtract(pivot(owner, partial)); // kiếm chém: thân kiếm hướng ra ngoài từ người chủ
            return d.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : d.normalize();
        }
        Vec3 a = pos(level, f, tm + 0.6, partial), b = pos(level, f, tm - 0.6, partial);
        Vec3 d = a == null || b == null ? Vec3.ZERO : a.subtract(b);
        if (d.lengthSqr() < 1e-5) d = f.to.subtract(owner != null ? shoulder(owner, partial, tm) : f.from);
        return d.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : d.normalize();
    }

    private static long lastSparkTick = Long.MIN_VALUE;

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
        PoseStack fx = new PoseStack(); // ngăn xếp mới, toạ độ tương đối camera: dùng cho hình học cộng sáng
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        OrbitState.prune(level.getGameTime() + 1.0E-3);
        boolean any = false;
        boolean sparkNow = level.getGameTime() != lastSparkTick;
        if (sparkNow) lastSparkTick = level.getGameTime();
        float glowGain = ShaderCompat.shadersActive() ? 1.4f : 1f; // shader làm mờ hình học cộng sáng: bù độ đậm
        VertexConsumer glow = null;
        int samples = switch (ClientConfig.fxQuality()) { case LOW -> 0; case MED -> 5; default -> 8; };

        // kiếm treo sau vai
        for (AbstractClientPlayer pl : level.players()) {
            if (pl.distanceToSqr(cam) > VIEW_RANGE * VIEW_RANGE || !visible(pl)) continue;
            ItemStack sword = heldSword(pl);
            if (sword.isEmpty() || OrbitState.busy(pl.getId(), time)) continue;
            int stage = BanMenhKiemItem.stageOf(sword);
            Vec3 pos = shoulder(pl, partial, time);
            Vec3 dir = idleDir(pl, partial, time);
            double len = bladeLen(stage, 1f);
            float roll = (float) (Math.sin(time * 0.09 + pl.getId()) * 6);
            if (glow == null) glow = buf.getBuffer(RenderType.lightning());
            if (!(pl == mc.player && cam.distanceToSqr(pos) < 1.0))
                FxSword.blade(glow, fx.last().pose(), cam, pos, dir, len * 1.05, colorOf(sword),
                        (0.55f + 0.12f * (float) Math.sin(time * 0.2 + pl.getId())) * glowGain);
            draw(mc, level, ps, buf, sword.copyWithCount(1), pos, dir, cam, (float) (len / LEN_PER_SCALE), 1f, roll, pl.getId(), pl == mc.player);
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
            Vec3 v = dirAt(level, f, owner, time, partial, pos);
            int stage = owner == null ? 3 : (st.is(ModItems.BAN_MENH_KIEM.get()) ? BanMenhKiemItem.stageOf(st) : 0);
            float scale;
            double len;
            if (owner != null && !f.giant) {
                len = bladeLen(stage, f.size);
                scale = (float) (len / LEN_PER_SCALE);
            } else { // phi kiếm kỹ năng / kiếm khổng lồ: giữ cỡ cũ
                scale = (0.4f + 0.05f * stage) * f.size;
                len = scale * LEN_PER_SCALE;
            }
            int rgb = owner != null ? (f.arc ? f.color : colorOf(st)) : 0x9FE8FF;
            float alpha = f.owner >= 0 ? (f.giant ? 0.8f : 1f) : Mth.clamp((float) (f.flight + f.hold - t) / (f.hold > 10 ? 8f : 3f), 0f, 1f);
            float roll = (float) (Math.sin(time * 0.35 + f.start) * 10);
            boolean moving = t > 0 && (f.slash ? t < OrbitState.Flight.WIND + f.flight + f.ret : (t < f.flight || (f.owner >= 0 && t < f.flight + f.ret)));
            if (glow == null) glow = buf.getBuffer(RenderType.lightning());

            if (f.arc && !f.arcFired && owner != null && t >= OrbitState.Flight.WIND + 1) { // vung tới: tung cung kiếm khí
                f.arcFired = true;
                Frame fr = frame(level, f, owner, partial);
                FxSword.arc(fr.piv(), fr.fwd(), fr.w(), f.reach, f.color);
            }

            // dải sáng sau thân kiếm (thay các bóng mờ cũ)
            if (moving && samples > 0 && owner != null) {
                Vec3 prevTip = null, prevBase = null;
                float prevA = 0;
                for (int k = 0; k <= samples; k++) {
                    double tg = time - k * 0.4;
                    if (tg < f.start + 0.5) break; // chưa vung: không nối dải sáng về vai
                    Vec3 gp = k == 0 ? pos : pos(level, f, tg, partial);
                    if (gp == null) break;
                    Vec3 gd = k == 0 ? v : dirAt(level, f, owner, tg, partial, gp);
                    Vec3 tip = gp.add(gd.scale(len * 0.5)), base = gp.subtract(gd.scale(len * 0.15));
                    float a = (1f - k / (float) (samples + 1)) * 0.9f * glowGain * (alpha < 0.98f ? alpha : 1f);
                    if (prevTip != null) FxSword.sheet(glow, fx.last().pose(), cam, prevTip, prevBase, tip, base, rgb, prevA, a);
                    prevTip = tip; prevBase = base; prevA = a;
                }
            }
            if (moving && sparkNow && owner != null && ClientConfig.particles() != ClientConfig.ParticleLevel.OFF) {
                Vec3 tip = pos.add(v.scale(len * (0.2 + 0.3 * level.random.nextDouble())));
                FxSword.spark(tip, new Vec3((level.random.nextDouble() - 0.5) * 0.04, 0.02, (level.random.nextDouble() - 0.5) * 0.04), rgb, 0.13f, 12);
            }
            if (owner != null) FxSword.blade(glow, fx.last().pose(), cam, pos, v, len * 1.05, rgb,
                    (moving ? 0.95f : 0.6f) * glowGain * (alpha < 0.98f ? alpha : 1f));
            draw(mc, level, ps, buf, st, pos, v, cam, scale, alpha, roll, f.owner, owner != null && owner == mc.player);
            any = true;
        }
        if (any) {
            buf.endBatch();
            buf.endBatch(RenderType.lightning());
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) {
        OrbitState.clear();
    }
}
