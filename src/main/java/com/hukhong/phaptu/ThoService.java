package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import com.hukhong.skill.SkillService;
import com.hukhong.skill.SkillState;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxSfx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thổ: E giữ = xuyên tường 5s (server là nguồn sự thật, client dự đoán chuyển động) và T "Thiên Sơn Áp Đỉnh"
 * (3 đợt núi rơi -> cửa sổ giữ T dẫn nổ). Bộ đếm tick là bộ đếm riêng của trạng thái (mỗi {@link #tick} +1) nên GameTest có thể tự "quay" thời gian.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ThoService {
    private ThoService() {}

    private static final int COL = PhapElement.THO.rgb, DUST = 0xC49A5C;
    private static final BlockParticleOption STONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());

    static final class St {
        // xuyên tường
        int phaseLeft;
        Vec3 lastSafe;
        // T
        int tAge = -1;                 // >= 0: đang trong các đợt rơi
        int waveTelegraphed, waveLanded;
        Vec3 center;
        double dmg;
        boolean window;
        int windowLeft, tHeld;
        boolean tPressed;
        final List<List<Vec3>> impacts = new ArrayList<>();
    }

    private static final Map<UUID, St> STATES = new ConcurrentHashMap<>();

    private static St st(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new St()); }

    public static void register(PayloadRegistrar r) {
        r.playToClient(ThoStatePayload.TYPE, ThoStatePayload.STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.fx.ThoClient.update(pl)));
    }

    // ------------------------------------------------------------------ khoá phím

    public static boolean isTho(ServerPlayer p) { return PhapTu.element(p) == PhapElement.THO; }

    /** Mặt nạ phím cố định của Pháp Tu: Thổ khoá E + T, Quang/Lôi/Hỏa/Thủy khoá T (Tinh Hà Quang Trụ), còn lại 0. */
    public static int lockMask(ServerPlayer p) {
        if (isTho(p)) return com.hukhong.skillbook.SkillBook.LOCK_E | com.hukhong.skillbook.SkillBook.LOCK_T;
        return PhapMath.lockMask(PhapTu.element(p));
    }

    /** Hồi chiêu T Thổ (tick, đã nhân hệ số nguyên tố) hoặc -1. */
    public static int cooldownOverride(ServerPlayer p, Path page, int slot) {
        if (page != Path.PHAP || slot != 3 || !isTho(p)) return -1;
        return (int) Math.round(PhapMath.THO_T_COOLDOWN_TICKS * PhapRuntime.cooldownMult(p, page));
    }

    // ================================================================== E giữ: xuyên tường

    public static boolean phasing(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.phaseLeft > 0;
    }

    public static int phaseLeft(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s == null ? 0 : s.phaseLeft;
    }

    /** Giữ E đủ lâu (gọi từ ThiemBoService). Trả true nếu bắt đầu xuyên tường. */
    public static boolean startPhase(ServerPlayer p) {
        if (phasing(p) || !(p.level() instanceof ServerLevel lv)) return false;
        if (com.hukhong.bicanh.BiCanhManager.inRun(p) || com.hukhong.arena.BossArena.isEngaged(p)) {
            p.displayClientMessage(Component.translatable("hukhong.phap.tho.phase.denied").withColor(COL), true);
            return false;
        }
        SkillState sk = SkillService.state(p);
        long now = lv.getGameTime();
        if (sk.cooldownEnd[1] > now) return false;
        if (!Resources.tryConsume(p, PhapMath.THO_PHASE_COST, com.hukhong.banmenh.BanMenhService.skillCostFactor(p))) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
            return false;
        }
        sk.cooldownEnd[1] = now + Math.round(PhapMath.THO_PHASE_COOLDOWN_TICKS * PhapRuntime.cooldownMult(p, Path.PHAP));
        sk.lastCombat = now;
        SkillService.sync(p);
        St s = st(p);
        s.phaseLeft = PhapMath.THO_PHASE_TICKS;
        s.lastSafe = ThoPhase.allowed(lv, p.getBoundingBox()) ? p.position() : null;
        p.noPhysics = true;
        p.fallDistance = 0;
        send(p, ThoStatePayload.PHASE_ON, s.phaseLeft);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.phase.on").withColor(COL), true);
        Fx.shell(lv, p, p, 1.25f, COL, PhapMath.THO_PHASE_TICKS);
        Fx.groundRing(lv, p, p.position().add(0, 0.06, 0), 2.4f, COL, 18);
        lv.sendParticles(STONE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
        PhapSkills.sound(lv, p.position(), SoundEvents.DEEPSLATE_BREAK, 1f, 0.6f);
        return true;
    }

    /** Kết thúc xuyên tường: đẩy ra chỗ trống gần nhất, chống rơi. */
    public static void endPhase(ServerPlayer p, boolean notify) {
        St s = STATES.get(p.getUUID());
        if (s == null || s.phaseLeft <= 0) return;
        s.phaseLeft = 0;
        p.noPhysics = false;
        if (p.level() instanceof ServerLevel lv) {
            AABB bb = p.getBoundingBox();
            if (!lv.noCollision(p, bb) || ThoPhase.blocked(lv, bb) || ThoPhase.yOutOfBounds(lv, p.getY())) {
                Vec3 to = freeSpot(lv, p, s.lastSafe);
                if (to != null) {
                    safeTeleport(p, to);
                    Fx.groundRing(lv, p, to.add(0, 0.06, 0), 2.0f, DUST, 14);
                    lv.sendParticles(STONE, to.x, to.y + 1, to.z, 24, 0.4, 0.6, 0.4, 0.1);
                }
            }
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        }
        p.fallDistance = 0;
        s.lastSafe = null;
        send(p, ThoStatePayload.PHASE_OFF, 0);
        if (notify) p.displayClientMessage(Component.translatable("hukhong.phap.tho.phase.off").withColor(COL), true);
    }

    /** teleportTo qua mạng; nếu kết nối không áp dụng (người chơi giả) thì đặt thẳng vị trí. */
    private static void safeTeleport(ServerPlayer p, Vec3 to) {
        p.teleportTo(to.x, to.y, to.z);
        if (p.position().distanceToSqr(to) > 0.01) p.setPos(to.x, to.y, to.z);
    }

    /** Chỗ trống gần nhất quanh người chơi (ưu tiên có nền đỡ chân); fallback = vị trí an toàn cuối. */
    static Vec3 freeSpot(ServerLevel lv, ServerPlayer p, Vec3 fallback) {
        AABB bb = p.getBoundingBox();
        Vec3 base = p.position();
        final int R = 7;
        List<int[]> offs = new ArrayList<>();
        for (int dx = -R; dx <= R; dx++) for (int dy = -R; dy <= R; dy++) for (int dz = -R; dz <= R; dz++) offs.add(new int[]{dx, dy, dz});
        offs.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1] + a[2] * a[2], b[0] * b[0] + b[1] * b[1] + b[2] * b[2]));
        double bx = Math.floor(base.x) + 0.5, bz = Math.floor(base.z) + 0.5, by = Math.floor(base.y);
        for (int pass = 0; pass < 2; pass++) {
            for (int[] o : offs) {
                AABB c = bb.move(bx + o[0] - base.x, by + o[1] - base.y, bz + o[2] - base.z);
                if (!ThoPhase.allowed(lv, c) || !lv.noCollision(p, c)) continue;
                if (pass == 0 && lv.noCollision(p, c.move(0, -1.0, 0))) continue; // pass 0: cần nền đỡ
                return new Vec3(bx + o[0], by + o[1], bz + o[2]);
            }
        }
        return fallback;
    }

    private static void tickPhase(ServerPlayer p, St s, ServerLevel lv) {
        if (s.phaseLeft <= 0) return;
        boolean bad = !p.isAlive() || p.isSpectator() || com.hukhong.bicanh.BiCanhManager.inRun(p) || com.hukhong.arena.BossArena.isEngaged(p);
        if (bad) { endPhase(p, false); return; }
        s.phaseLeft--;
        p.noPhysics = true;
        p.fallDistance = 0;
        AABB bb = p.getBoundingBox();
        if (!ThoPhase.allowed(lv, bb)) { // chạm khối bất hoại / ra ngoài chiều cao thế giới: kéo về chỗ an toàn cuối
            Vec3 back = s.lastSafe != null ? s.lastSafe : freeSpot(lv, p, null);
            if (back != null) {
                safeTeleport(p, back);
                p.setDeltaMovement(Vec3.ZERO);
                p.hurtMarked = true;
            }
        } else s.lastSafe = p.position();
        if (s.phaseLeft % 4 == 0) lv.sendParticles(STONE, p.getX(), p.getY() + 1, p.getZ(), 4, 0.4, 0.7, 0.4, 0.05);
        if (s.phaseLeft % 20 == 0 && s.phaseLeft > 0)
            p.displayClientMessage(Component.translatable("hukhong.phap.tho.phase.left", s.phaseLeft / 20).withColor(COL), true);
        if (s.phaseLeft <= 0) { s.phaseLeft = 1; endPhase(p, true); } // endPhase bỏ qua khi phaseLeft <= 0
    }

    // ================================================================== T: Thiên Sơn Áp Đỉnh

    public static boolean tActive(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && (s.tAge >= 0 || s.window);
    }

    public static boolean blastWindow(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.window;
    }

    public static int wavesLanded(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s == null ? 0 : s.waveLanded;
    }

    /** Khởi động T tại center với sát thương T hiện tại dmg (= hệ số x Phép lực). Gọi từ PhapSkills. */
    public static void startT(ServerLevel lv, ServerPlayer p, Vec3 center, double dmg) {
        St s = st(p);
        s.tAge = 0;
        s.waveTelegraphed = 0;
        s.waveLanded = 0;
        s.center = center;
        s.dmg = dmg;
        s.window = false;
        s.windowLeft = 0;
        s.tHeld = 0;
        s.tPressed = false;
        s.impacts.clear();
        double base = p.getYRot() * 0.0174;
        for (int w = 0; w < PhapMath.THO_T_WAVES; w++) {
            List<Vec3> pts = new ArrayList<>();
            pts.add(w == 0 ? center : center.add(Math.cos(base + w * 2.1) * 1.6, 0, Math.sin(base + w * 2.1) * 1.6)); // luôn có một điểm gần tâm
            for (int i = 1; i < PhapMath.THO_T_IMPACTS; i++) {
                double a = base + w * 0.9 + 2 * Math.PI * i / (PhapMath.THO_T_IMPACTS - 1);
                double r = PhapMath.THO_T_AREA * (0.55 + 0.35 * (((w + i) % 3) / 2.0));
                pts.add(PhapSkills.ground(lv, center.add(Math.cos(a) * r, 2, Math.sin(a) * r)));
            }
            s.impacts.add(pts);
        }
        Fx.rune(lv, p, center.add(0, 0.08, 0), (float) PhapMath.THO_T_AREA, COL, 40, 10);
        Fx.telegraphCircle(lv, p, center.add(0, 0.05, 0), (float) PhapMath.THO_T_AREA, 20, COL);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.waves").withColor(COL), true);
    }

    private static void telegraphWave(ServerLevel lv, ServerPlayer p, St s, int w) {
        for (Vec3 pt : s.impacts.get(w)) {
            Fx.telegraphCircle(lv, p, pt.add(0, 0.05, 0), (float) PhapMath.THO_T_IMPACT_RADIUS, PhapMath.THO_T_FALL_DELAY, COL);
            Fx.elem(lv, p, Fx.E_BOULDER, pt, Vec3.ZERO, 3.2f, COL, PhapMath.THO_T_FALL_DELAY + 8);
        }
        PhapSkills.sound(lv, s.center, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 0.5f);
    }

    private static void landWave(ServerLevel lv, ServerPlayer p, St s, int w) {
        Set<LivingEntity> hit = java.util.Collections.newSetFromMap(new IdentityHashMap<>()); // mỗi mục tiêu chỉ ăn MỘT lần mỗi đợt
        double amount = s.dmg * PhapMath.THO_T_WAVE_FRAC;
        for (Vec3 pt : s.impacts.get(w)) {
            for (LivingEntity e : PhapSkills.around(lv, p, pt, PhapMath.THO_T_IMPACT_RADIUS)) {
                if (!hit.add(e)) continue;
                PhapSkills.hit(p, e, amount);
                PhapRuntime.stun(e, PhapMath.THO_MOUNTAIN_STUN);
                PhapSkills.knock(e, pt, 0.4, 0.3);
            }
            Fx.groundCrack(lv, p, pt.add(0, 0.06, 0), (float) PhapMath.THO_T_IMPACT_RADIUS + 0.8f, COL, 6, 50);
            Fx.groundRing(lv, p, pt.add(0, 0.06, 0), (float) PhapMath.THO_T_IMPACT_RADIUS, DUST, 16);
            Fx.elem(lv, p, Fx.E_SPIKES, pt, Vec3.ZERO, (float) PhapMath.THO_T_IMPACT_RADIUS * 0.8f, COL, 26);
            lv.sendParticles(STONE, pt.x, pt.y + 0.6, pt.z, 24, 1.6, 0.5, 1.6, 0.2);
        }
        Fx.shake(lv, s.center, 0.9f + 0.3f * w, 12, 36f);
        FxSfx.cast(lv, s.center, 1.3f, 0.5f, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.ANVIL_LAND, SoundEvents.DEEPSLATE_BREAK);
        s.waveLanded = w + 1;
    }

    private static void openWindow(ServerLevel lv, ServerPlayer p, St s) {
        s.window = true;
        s.windowLeft = PhapMath.THO_T_WINDOW_TICKS;
        s.tHeld = 0;
        s.tPressed = false; // phải nhấn T MỚI sau khi cửa sổ mở (không để lần giữ T lúc cast tự dẫn nổ)
        send(p, ThoStatePayload.BLAST_OPEN, s.windowLeft);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.window").withColor(COL), true);
        Fx.telegraphCircle(lv, p, s.center.add(0, 0.05, 0), (float) PhapMath.THO_T_BLAST_RADIUS, 24, COL);
        PhapSkills.sound(lv, s.center, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.5f);
    }

    private static void closeWindow(ServerPlayer p, St s, boolean expired) {
        s.window = false;
        s.tAge = -1;
        s.tHeld = 0;
        s.tPressed = false;
        send(p, ThoStatePayload.BLAST_CLOSED, 0);
        if (expired) p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.expired").withColor(0x999999), true);
    }

    /** Nhấn / nhả T (Thổ). Chỉ đếm khi cửa sổ dẫn nổ đang mở. */
    public static void tKey(ServerPlayer p, boolean down) {
        St s = st(p);
        s.tPressed = down && s.window;
        s.tHeld = 0;
    }

    /** Dẫn nổ: sát thương 1,75 x T hiện tại, rút 99% Pháp Lực hiện có. Trả true nếu nổ. */
    public static boolean detonate(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || !s.window || !(p.level() instanceof ServerLevel lv)) return false;
        Vec3 c = s.center;
        double amount = s.dmg * PhapMath.THO_T_BLAST_MULT;
        for (LivingEntity e : PhapSkills.around(lv, p, c, PhapMath.THO_T_BLAST_RADIUS)) {
            PhapSkills.hit(p, e, amount);
            PhapRuntime.stun(e, PhapMath.THO_MOUNTAIN_STUN * 1.5);
            PhapSkills.knock(e, c, 1.1, 0.5);
        }
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        Resources.add(p, -(int) Math.floor(d.resource() * PhapMath.THO_T_MANA_DRAIN));
        float r = (float) PhapMath.THO_T_BLAST_RADIUS;
        Fx.groundCrack(lv, p, c.add(0, 0.06, 0), r + 2f, COL, 12, 90);
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), r, 0xFFFFFF, 20);
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), r * 0.6f, DUST, 26);
        Fx.rune(lv, p, c.add(0, 0.08, 0), r * 0.8f, COL, 34, 12);
        Fx.elem(lv, p, Fx.E_SPIKES, c, Vec3.ZERO, r * 0.9f, COL, 44);
        Fx.elem(lv, p, Fx.E_BOULDER, c, Vec3.ZERO, 7f, COL, 24);
        Fx.shake(lv, c, 2.6f, 28, 60f);
        Fx.play(lv, FxType.SCREEN_TINT, c, Vec3.ZERO, 0.35f, 0xFFE9B0, 12);
        lv.sendParticles(STONE, c.x, c.y + 1, c.z, 160, r * 0.4, 1.2, r * 0.4, 0.45);
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 3, 2.5, 0.5, 2.5, 0);
        FxSfx.cast(lv, c, 2f, 0.4f, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.ANVIL_LAND, SoundEvents.DEEPSLATE_BREAK);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.boom").withColor(COL), true);
        closeWindow(p, s, false);
        return true;
    }

    private static void tickT(ServerPlayer p, St s, ServerLevel lv) {
        if (!p.isAlive()) { clearT(p, s); return; }
        if (s.tAge >= 0 && !s.window) {
            int gap = PhapMath.THO_T_WAVE_GAP, fall = PhapMath.THO_T_FALL_DELAY;
            if (s.waveTelegraphed < PhapMath.THO_T_WAVES && s.tAge >= s.waveTelegraphed * gap) telegraphWave(lv, p, s, s.waveTelegraphed++);
            if (s.waveLanded < s.waveTelegraphed && s.tAge >= s.waveLanded * gap + fall) landWave(lv, p, s, s.waveLanded);
            if (s.waveLanded >= PhapMath.THO_T_WAVES) openWindow(lv, p, s);
            s.tAge++;
            return;
        }
        if (!s.window) return;
        if (--s.windowLeft <= 0) { closeWindow(p, s, true); return; }
        if (s.windowLeft % 20 == 0) { // vòng cảnh báo tầm nổ
            Fx.telegraphCircle(lv, p, s.center.add(0, 0.05, 0), (float) PhapMath.THO_T_BLAST_RADIUS, 20, COL);
            p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.window_left", s.windowLeft / 20 + 1).withColor(COL), true);
        }
        if (s.tPressed) {
            s.tHeld++;
            if (s.tHeld % 2 == 0) { // nạp: vòng đất co lại + rung nhẹ
                float k = 1f - s.tHeld / (float) PhapMath.THO_T_HOLD_TICKS;
                Fx.groundRing(lv, p, s.center.add(0, 0.06, 0), (float) PhapMath.THO_T_BLAST_RADIUS * Math.max(0.2f, k), COL, 6);
                Fx.shake(lv, s.center, 0.5f, 4, 40f);
            }
            if (s.tHeld >= PhapMath.THO_T_HOLD_TICKS) detonate(p);
        }
    }

    private static void clearT(ServerPlayer p, St s) {
        boolean was = s.window;
        s.tAge = -1;
        s.window = false;
        s.tHeld = 0;
        s.tPressed = false;
        if (was) send(p, ThoStatePayload.BLAST_CLOSED, 0);
    }

    // ================================================================== chung

    /** Mỗi tick người chơi (PlayerTickEvent.Post; GameTest gọi tay vì người chơi giả không nhận sự kiện). */
    public static void tick(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || !(p.level() instanceof ServerLevel lv)) return;
        tickPhase(p, s, lv);
        tickT(p, s, lv);
    }

    /** Tắt thế / đổi trang: chỉ kết thúc xuyên tường (T đã tung vẫn chạy nốt). */
    public static void cancelPhase(ServerPlayer p) { endPhase(p, false); }

    /** Dọn toàn bộ (chết, thoát, test). */
    public static void reset(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null) return;
        endPhase(p, false);
        clearT(p, s);
        STATES.remove(p.getUUID());
    }

    private static void send(ServerPlayer p, int kind, int ticks) {
        if (p.connection != null) PacketDistributor.sendToPlayer(p, new ThoStatePayload(kind, ticks));
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) tick(p);
    }

    /** Xuyên tường: Player.tick đặt noPhysics=false ở đầu tick nên có thể bị ngạt tường (IN_WALL) -> huỷ. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && phasing(p) && e.getSource().is(DamageTypes.IN_WALL)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) reset(p);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) reset(p);
    }
}
