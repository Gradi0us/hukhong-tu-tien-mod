package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.client.compat.ShaderCompat;
import com.hukhong.client.keybind.SkillKeys;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.phaptu.beam.PhapBeamActionPayload;
import com.hukhong.phaptu.beam.PhapBeamMath;
import com.hukhong.phaptu.beam.PhapBeamStatePayload;
import com.hukhong.phaptu.linhtruong.LinhTruongMath;
import com.hukhong.phaptu.linhtruong.LinhTruongRegistry;
import com.hukhong.vfx.FxType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Tinh Hà Quang Trụ phía client: giữ trạng thái tia của mọi người chơi (từ gói server), xử lý phím T / chuột phải,
 * sinh hạt (hội tụ, dọc tia, điểm chạm) và vẽ HUD trên ô T. Phần dựng hình học nằm ở {@link PhapBeamRender}.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class PhapBeamClient {
    private PhapBeamClient() {}

    public static final int IDLE = 0, CHARGING = 1, READY = 2, FIRING = 3, FADE = 4;
    static final int FADE_TICKS = 8;

    static final class Entry {
        int phase;
        PhapElement el;
        long start;       // game time lúc vào pha
        int ticks;        // độ dài tối đa của pha (từ server)
        long fadeStart;
    }

    static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static final Random RND = new Random();
    private static boolean prevT, prevR;

    // ------------------------------------------------------------------ gói từ server

    public static void update(PhapBeamStatePayload pl) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        Entry old = ENTRIES.get(pl.entityId());
        Entity en = mc.level.getEntity(pl.entityId());
        PhapElement el = pl.element() >= 0 && pl.element() < PhapElement.values().length ? PhapElement.values()[pl.element()] : null;
        if (pl.phase() == IDLE) {
            if (old != null && old.phase == FIRING) { // kết thúc: tan dần + bùng nổ cuối
                old.phase = FADE;
                old.fadeStart = now;
                if (en != null && old.el != null) burst(en, old.el, 1.4f, false);
            } else ENTRIES.remove(pl.entityId());
            return;
        }
        Entry e = new Entry();
        e.phase = pl.phase();
        e.el = el != null ? el : old != null ? old.el : PhapElement.QUANG;
        e.start = now;
        e.ticks = pl.ticks();
        ENTRIES.put(pl.entityId(), e);
        if (en != null && (e.phase == READY || e.phase == FIRING)) burst(en, e.el, e.phase == FIRING ? 2f : 1f, true);
    }

    /** Nháy sáng + sóng chấn + hạt bùng ở đầu trượng / tay. */
    private static void burst(Entity en, PhapElement el, float scale, boolean flash) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 tip = origin(en, 1f);
        FxClient.add(FxType.SHOCKWAVE, tip, Vec3.ZERO, 3.2f * scale, el.rgb, 14, 0);
        FxClient.add(FxType.SHOCKWAVE, tip, Vec3.ZERO, 2.0f * scale, 0xFFFFFF, 10, 2);
        if (flash && en == mc.player) FxClient.add(FxType.SCREEN_TINT, tip, Vec3.ZERO, 0.28f * scale, 0xFFFFFF, 10, 0);
        int n = (int) (36 * ClientConfig.fxDensity() * scale * (en == mc.player ? 1f : 0.5f));
        for (int i = 0; i < n; i++) {
            double a = RND.nextDouble() * Math.PI * 2, b = RND.nextDouble() * Math.PI - Math.PI / 2, sp = 0.15 + RND.nextDouble() * 0.35;
            FxClient.puff(i % 3 == 0 ? FxParticle.STAR : FxParticle.GLOW, tip.x, tip.y, tip.z,
                    Math.cos(a) * Math.cos(b) * sp, Math.sin(b) * sp, Math.sin(a) * Math.cos(b) * sp,
                    i % 4 == 0 ? 0xFFFFFF : el.rgb, 0.3f, 24, 0f, true);
        }
    }

    // ------------------------------------------------------------------ truy vấn cho HUD / phím

    public static int localPhase() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return IDLE;
        Entry e = ENTRIES.get(mc.player.getId());
        return e == null ? IDLE : e.phase;
    }

    /** Người chơi cục bộ có phím T bị thay bằng tia không. */
    public static boolean lockedT(Minecraft mc) {
        // client: CULTIVATION khong dong bo -> dung ClientCache + nguyen to (ELEMENT co dong bo)
        return mc.player != null && com.hukhong.client.ClientCache.cultivation() != null
                && com.hukhong.client.ClientCache.cultivation().paths().contains(com.hukhong.core.Path.PHAP)
                && PhapBeamMath.locksT(PhapTu.rawElement(mc.player));
    }

    /** Đầu Linh Trượng nếu đang cầm, không thì vị trí bàn tay (xấp xỉ). */
    static Vec3 origin(Entity en, float pt) {
        Vec3 eye = en.getEyePosition(pt);
        return LinhTruongMath.tip(eye, en.getViewYRot(pt), en.getViewXRot(pt));
    }

    static boolean holdsStaff(Entity en) {
        return en instanceof LivingEntity le && le.getMainHandItem().is(LinhTruongRegistry.LINH_TRUONG.get());
    }

    // ------------------------------------------------------------------ phím

    public static boolean physicalDown(Minecraft mc, KeyMapping k) {
        long w = mc.getWindow().getWindow();
        InputConstants.Key key = k.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(w, key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM) return InputConstants.isKeyDown(w, key.getValue());
        return k.isDown();
    }

    private static void send(int action) { PacketDistributor.sendToServer(new PhapBeamActionPayload(action)); }

    /**
     * Gọi mỗi tick từ SkillKeyHandler (trước handleKeybinds của vanilla). active = thế tu luyện bật, không mở màn hình, và phím T bị thay bằng tia.
     * Giữ T = nạp; nhả = huỷ. Sẵn sàng/đang bắn: nuốt chuột phải của vanilla (không đặt khối / dùng đồ) và dùng nó làm nút bắn.
     */
    public static void tickKeys(Minecraft mc, boolean active) {
        boolean t = active && mc.screen == null && physicalDown(mc, SkillKeys.SKILL[3]); // doc phim that: T trung phim chat vanilla
        if (t && !prevT) send(PhapBeamActionPayload.CHARGE_START);
        if (!t && prevT) send(PhapBeamActionPayload.CHARGE_STOP);
        prevT = t;
        int ph = localPhase();
        boolean swallow = active && (ph == CHARGING || ph == READY || ph == FIRING);
        if (swallow) {
            while (mc.options.keyUse.consumeClick()) { }
            mc.options.keyUse.setDown(false);
        }
        boolean r = active && (ph == READY || ph == FIRING) && physicalDown(mc, mc.options.keyUse);
        if (r && !prevR) send(PhapBeamActionPayload.FIRE_START);
        if (!r && prevR) send(PhapBeamActionPayload.FIRE_STOP);
        prevR = r;
    }

    // ------------------------------------------------------------------ tick: hạt + dọn

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ENTRIES.clear(); prevT = false; prevR = false; return; }
        if (mc.isPaused() || ENTRIES.isEmpty()) return;
        long now = mc.level.getGameTime();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Iterator<Map.Entry<Integer, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            var me = it.next();
            Entry en = me.getValue();
            Entity ent = mc.level.getEntity(me.getKey());
            if (ent == null || (en.phase == FADE && now - en.fadeStart > FADE_TICKS)) { it.remove(); continue; }
            if (ent.position().distanceToSqr(cam) > 100 * 100) continue;
            float q = ClientConfig.fxDensity() * (ent == mc.player ? 1f : 0.5f);
            if (ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) continue;
            switch (en.phase) {
                case CHARGING, READY -> converge(ent, en, q, now);
                case FIRING -> beamParticles(mc, ent, en, q);
                default -> { }
            }
        }
    }

    private static void converge(Entity ent, Entry en, float q, long now) {
        Vec3 tip = origin(ent, 1f);
        float k = en.phase == READY ? 1f : Mth.clamp((now - en.start) / (float) PhapBeamMath.CHARGE_TICKS, 0f, 1f);
        int n = Math.max(1, Math.round((1 + 4 * k) * q));
        for (int i = 0; i < n; i++) {
            double a = RND.nextDouble() * Math.PI * 2, b = RND.nextDouble() * Math.PI - Math.PI / 2, r = 2.2 + RND.nextDouble() * 1.4;
            Vec3 p = tip.add(Math.cos(a) * Math.cos(b) * r, Math.sin(b) * r, Math.sin(a) * Math.cos(b) * r);
            Vec3 v = tip.subtract(p).scale(1 / 7.0);
            FxClient.puff(i % 2 == 0 ? FxParticle.GLOW : FxParticle.STAR, p.x, p.y, p.z, v.x, v.y, v.z,
                    i % 3 == 0 ? 0xFFFFFF : en.el.rgb, 0.16f + 0.1f * k, 11, 0f, false);
        }
    }

    private static void beamParticles(Minecraft mc, Entity ent, Entry en, float q) {
        Vec3 eye = ent.getEyePosition(1f), look = ent.getViewVector(1f);
        Vec3 org = origin(ent, 1f);
        Vec3 dir = eye.add(look.scale(PhapBeamMath.RANGE)).subtract(org).normalize();
        Vec3 u = PhapBeamRender.perp(dir), v = dir.cross(u);
        boolean shaders = ShaderCompat.shadersActive(); // shader có thể ẩn hình học cộng sáng: bù bằng hạt
        int n = Math.max(1, Math.round(6 * q * (shaders ? 2.5f : 1f)));
        int rgb = en.el.rgb;
        for (int i = 0; i < n; i++) {
            double s = 1.5 + RND.nextDouble() * (PhapBeamMath.RANGE - 2), a = RND.nextDouble() * Math.PI * 2;
            double r = (0.3 + RND.nextDouble() * 1.2) * PhapBeamRender.profile(s);
            Vec3 p = org.add(dir.scale(s)).add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r));
            double ox = Math.cos(a) * 0.04, oz = Math.sin(a) * 0.04;
            switch (en.el) {
                case HOA -> FxClient.puff(FxParticle.GLOW, p.x, p.y, p.z, ox, 0.06 + RND.nextDouble() * 0.06, oz, i % 2 == 0 ? 0xFF7A20 : 0xFFD070, 0.22f, 22, -0.002f, true);
                case THUY -> FxClient.puff(FxParticle.GLOW, p.x, p.y, p.z, ox, 0.02, oz, i % 2 == 0 ? 0x4FF0E0 : 0xFFFFFF, 0.2f, 18, 0.002f, true);
                case LOI -> {
                    FxClient.puff(FxParticle.STAR, p.x, p.y, p.z, ox * 3, 0.0, oz * 3, i % 2 == 0 ? 0xB8A8FF : 0xFFFFFF, 0.2f, 8, 0f, true);
                    if (i == 0 && mc.level != null) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, ox * 6, 0.05, oz * 6);
                }
                default -> FxClient.puff(i % 2 == 0 ? FxParticle.STAR : FxParticle.GLOW, p.x, p.y, p.z, ox, 0.03, oz, i % 3 == 0 ? 0xFFFFFF : rgb, 0.2f, 20, 0f, true);
            }
        }
        // đầu trượng
        FxClient.puff(FxParticle.GLOW, org.x, org.y, org.z, (RND.nextDouble() - 0.5) * 0.2, (RND.nextDouble() - 0.5) * 0.2, (RND.nextDouble() - 0.5) * 0.2, 0xFFFFFF, 0.4f, 8, 0f, true);
        // điểm chạm
        BlockHitResult hit = clip(mc, ent, org, org.add(dir.scale(PhapBeamMath.RANGE)));
        if (hit != null) {
            Vec3 h = hit.getLocation(), nrm = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
            int m = Math.max(1, Math.round(5 * q * (shaders ? 2f : 1f)));
            for (int i = 0; i < m; i++) {
                double a = RND.nextDouble() * Math.PI * 2, sp = 0.1 + RND.nextDouble() * 0.3;
                double vy = 0.08 + RND.nextDouble() * 0.25;
                FxClient.puff(i % 2 == 0 ? FxParticle.GLOW : FxParticle.SHARD, h.x + nrm.x * 0.2, h.y + nrm.y * 0.2, h.z + nrm.z * 0.2,
                        Math.cos(a) * sp + nrm.x * 0.1, vy * Math.max(0.3, Math.abs(nrm.y)) + nrm.y * 0.1, Math.sin(a) * sp + nrm.z * 0.1,
                        i % 3 == 0 ? 0xFFFFFF : rgb, 0.3f, 22, en.el == PhapElement.HOA ? -0.003f : 0.02f, true);
            }
            if (mc.level != null && mc.level.getGameTime() % 3 == 0) {
                switch (en.el) {
                    case HOA -> mc.level.addParticle(ParticleTypes.FLAME, h.x, h.y + 0.2, h.z, (RND.nextDouble() - 0.5) * 0.2, 0.15, (RND.nextDouble() - 0.5) * 0.2);
                    case THUY -> mc.level.addParticle(ParticleTypes.SPLASH, h.x, h.y + 0.2, h.z, (RND.nextDouble() - 0.5) * 0.4, 0.2, (RND.nextDouble() - 0.5) * 0.4);
                    case LOI -> mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, h.x, h.y + 0.2, h.z, (RND.nextDouble() - 0.5) * 0.5, 0.2, (RND.nextDouble() - 0.5) * 0.5);
                    default -> mc.level.addParticle(ParticleTypes.END_ROD, h.x, h.y + 0.2, h.z, (RND.nextDouble() - 0.5) * 0.3, 0.2, (RND.nextDouble() - 0.5) * 0.3);
                }
            }
        }
    }

    /** Điểm chạm khối đầu tiên trên đoạn, hoặc null. */
    static BlockHitResult clip(Minecraft mc, Entity ent, Vec3 a, Vec3 b) {
        if (mc.level == null) return null;
        BlockHitResult r = mc.level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ent));
        return r.getType() == HitResult.Type.MISS ? null : r;
    }

    // ------------------------------------------------------------------ HUD ô T

    public static void drawHud(GuiGraphics g, Minecraft mc, int x, int y, int size) {
        if (mc.player == null || mc.level == null) return;
        Entry en = ENTRIES.get(mc.player.getId());
        if (en == null) return;
        long now = mc.level.getGameTime();
        float age = now - en.start;
        int rgb = (en.el != null ? en.el.rgb : 0xFFFFFF) & 0xFFFFFF;
        int bc = 0xFF000000 | rgb;
        switch (en.phase) {
            case CHARGING -> {
                float k = Mth.clamp(age / PhapBeamMath.CHARGE_TICKS, 0f, 1f);
                int h = Math.max(1, Math.round(16 * k));
                g.fill(x + 2, y + 18 - h, x + 18, y + 18, 0xAA000000 | rgb);
                String s = Math.round(k * 100) + "%";
                g.drawString(mc.font, s, x + (size - mc.font.width(s)) / 2, y + 6, 0xFFFFFFFF, true);
            }
            case READY -> {
                boolean on = (now / 4) % 2 == 0;
                int c = on ? bc : 0xFFFFFFFF;
                g.fill(x, y, x + size, y + 1, c); g.fill(x, y + size - 1, x + size, y + size, c);
                g.fill(x, y, x + 1, y + size, c); g.fill(x + size - 1, y, x + size, y + size, c);
                g.fill(x + 2, y + 2, x + 18, y + 18, 0x44000000 | rgb);
                String s = Component.translatable("hukhong.phap.beam.hud.ready").getString();
                g.drawString(mc.font, s, x + (size - mc.font.width(s)) / 2, y + 2, 0xFFFFFFFF, true);
                hint(g, mc, "hukhong.phap.beam.hint", Math.max(0, en.ticks - (int) age + 19) / 20, rgb);
            }
            case FIRING -> {
                float rem = Mth.clamp(1f - age / PhapBeamMath.FIRE_MAX_TICKS, 0f, 1f);
                int h = Math.max(1, Math.round(16 * rem));
                g.fill(x + 2, y + 18 - h, x + 18, y + 18, 0xCC000000 | rgb);
                int c = 0xFFFFFFFF;
                g.fill(x, y, x + size, y + 1, c); g.fill(x, y + size - 1, x + size, y + size, c);
                g.fill(x, y, x + 1, y + size, c); g.fill(x + size - 1, y, x + size, y + size, c);
                String s = Component.translatable("hukhong.phap.beam.hud.fire").getString();
                g.drawString(mc.font, s, x + (size - mc.font.width(s)) / 2, y + 2, 0xFFFFFFFF, true);
            }
            default -> { }
        }
    }

    private static void hint(GuiGraphics g, Minecraft mc, String key, int sec, int rgb) {
        Component c = Component.translatable(key, sec);
        int w = mc.font.width(c);
        g.drawString(mc.font, c, (g.guiWidth() - w) / 2, g.guiHeight() - 62, 0xFF000000 | rgb, true);
    }
}
