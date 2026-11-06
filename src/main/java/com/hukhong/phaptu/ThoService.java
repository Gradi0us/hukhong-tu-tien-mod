package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import com.hukhong.skill.Skill;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thổ: R "Địa Thứ Lĩnh Vực" (vùng gai đất: giữ R nở bán kính, hao mana theo bán kính, làm chậm, kéo kẻ bay xuống, chảy máu), E giữ = xuyên tường 5s (server là nguồn sự thật, client dự đoán chuyển động) và T "Thiên Sơn Áp Đỉnh"
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
        double tDealt;                 // tổng sát thương T đã tung (trước giảm trừ) - dùng cho test cân bằng
        // R: Địa Thứ Lĩnh Vực
        boolean fieldOn, rHeld;
        int fAge, fHeld;
        double fRadius, fDrainAcc;
        Vec3 fCenter;
        final Set<UUID> slowed = new HashSet<>();
        final Map<UUID, Integer> grabbed = new HashMap<>();   // kẻ bay đang bị kéo -> tick hiệu ứng kế tiếp
        final Map<UUID, Bleed> bleeds = new HashMap<>();
        int clock;                     // bộ đếm tick riêng (mỗi tick() +1)
    }

    /** Chảy máu của một mục tiêu: số tầng, tick hết hạn, tick gây sát thương kế tiếp (theo St.clock). */
    static final class Bleed {
        int stacks, expire, next;
    }

    private static final ResourceLocation SLOW_ID = ModAttributes.id("phap/tho_r_slow");

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
    /** Xuyên tường miễn phí ticks tick (trụ cấp 3 vỡ khi đang nhập). */
    public static void freePhase(ServerPlayer p, int ticks) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        St s = st(p);
        s.phaseLeft = ticks;
        s.lastSafe = ThoPhase.allowed(lv, p.getBoundingBox()) ? p.position() : null;
        p.noPhysics = true;
        p.fallDistance = 0;
        send(p, ThoStatePayload.PHASE_ON, s.phaseLeft);
        Fx.shell(lv, p, p, 1.25f, COL, ticks);
        lv.sendParticles(STONE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
    }

    public static boolean startPhase(ServerPlayer p) {
        if (phasing(p) || !(p.level() instanceof ServerLevel lv) || ThoTitan.controlling(p)) return false;
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
        s.tDealt = 0;
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
        ThoFx.tStart(lv, p, center, (float) PhapMath.THO_T_AREA);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.waves").withColor(COL), true);
    }

    private static void telegraphWave(ServerLevel lv, ServerPlayer p, St s, int w) {
        for (Vec3 pt : s.impacts.get(w)) ThoFx.tTelegraph(lv, p, s.center, pt, (float) PhapMath.THO_T_IMPACT_RADIUS, PhapMath.THO_T_FALL_DELAY);
    }

    private static void landWave(ServerLevel lv, ServerPlayer p, St s, int w) {
        Set<LivingEntity> hit = java.util.Collections.newSetFromMap(new IdentityHashMap<>()); // mỗi mục tiêu chỉ ăn MỘT lần mỗi đợt
        double amount = s.dmg * PhapMath.THO_T_WAVE_FRAC;
        for (Vec3 pt : s.impacts.get(w)) {
            for (LivingEntity e : PhapSkills.around(lv, p, pt, PhapMath.THO_T_IMPACT_RADIUS)) {
                if (!hit.add(e)) continue;
                PhapSkills.hit(p, e, amount);
                s.tDealt += amount;
                PhapRuntime.stun(e, PhapMath.THO_MOUNTAIN_STUN);
                PhapSkills.knock(e, pt, 0.4, 0.3);
            }
            ThoFx.tLand(lv, p, pt, (float) PhapMath.THO_T_IMPACT_RADIUS, w);
            com.hukhong.world.TreeFell.fellRadius(lv, pt, PhapMath.THO_T_IMPACT_RADIUS, 6, 4); // núi đá giáng xuống quật đổ cây
        }
        ThoFx.tWaveShake(lv, s.center, w);
        s.waveLanded = w + 1;
    }

    private static void openWindow(ServerLevel lv, ServerPlayer p, St s) {
        s.window = true;
        s.windowLeft = PhapMath.THO_T_WINDOW_TICKS;
        s.tHeld = 0;
        s.tPressed = false; // phải nhấn T MỚI sau khi cửa sổ mở (không để lần giữ T lúc cast tự dẫn nổ)
        send(p, ThoStatePayload.BLAST_OPEN, s.windowLeft);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.window").withColor(COL), true);
        ThoFx.tWindowOpen(lv, p, s.center, (float) PhapMath.THO_T_BLAST_RADIUS);
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

    /** Dẫn nổ: sát thương = T hiện tại x (3..8 theo % Pháp Lực lúc nổ), rút 99% Pháp Lực hiện có. Trả true nếu nổ. */
    public static boolean detonate(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || !s.window || !(p.level() instanceof ServerLevel lv)) return false;
        Vec3 c = s.center;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        double frac = d.resourceMax() <= 0 ? 0 : d.resource() / (double) d.resourceMax();
        double amount = s.dmg * PhapMath.thoTBlastMult(frac);
        com.hukhong.world.TreeFell.fellRadius(lv, c, PhapMath.THO_T_BLAST_RADIUS * 0.7, 8, 12); // kích nổ núi đá
        for (LivingEntity e : PhapSkills.around(lv, p, c, PhapMath.THO_T_BLAST_RADIUS)) {
            PhapSkills.hit(p, e, amount);
            s.tDealt += amount;
            PhapRuntime.stun(e, PhapMath.THO_MOUNTAIN_STUN * 1.5);
            PhapSkills.knock(e, c, 1.1, 0.5);
        }
        Resources.add(p, -(int) Math.floor(d.resource() * PhapMath.THO_T_MANA_DRAIN));
        ThoFx.tDetonate(lv, p, c, (float) PhapMath.THO_T_BLAST_RADIUS);
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.t.boom").withColor(COL), true);
        closeWindow(p, s, false);
        return true;
    }

    /** Tổng sát thương T đã tung (trước giảm trừ, cộng mọi mục tiêu) kể từ lần cast gần nhất - cho test cân bằng. */
    public static double tDamageDealt(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s == null ? 0 : s.tDealt;
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
            if (s.tHeld % 2 == 0) { // nạp: vòng đất co lại + rung + đá lăn
                float k = 1f - s.tHeld / (float) PhapMath.THO_T_HOLD_TICKS;
                ThoFx.tCharge(lv, p, s.center, (float) PhapMath.THO_T_BLAST_RADIUS * Math.max(0.2f, k));
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

    // ================================================================== R: Địa Thứ Lĩnh Vực

    public static boolean fieldActive(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s != null && s.fieldOn;
    }

    public static double fieldRadius(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s == null || !s.fieldOn ? 0 : s.fRadius;
    }

    public static Vec3 fieldCenter(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        return s == null ? null : s.fCenter;
    }

    /** Số tầng chảy máu của e do chủ p gây ra (0 nếu không). */
    public static int bleedStacks(ServerPlayer p, LivingEntity e) {
        St s = STATES.get(p.getUUID());
        Bleed b = s == null ? null : s.bleeds.get(e.getUUID());
        return b == null ? 0 : b.stacks;
    }

    /** e đang bị làm chậm 50% bởi vùng gai của p không. */
    public static boolean slowedByField(ServerPlayer p, LivingEntity e) {
        St s = STATES.get(p.getUUID());
        return s != null && s.slowed.contains(e.getUUID());
    }

    /**
     * Nhấn / nhả R (Thổ), đến từ ThiemBoPayload qua ThiemBoService.action. Nhấn: nếu vùng đang có thì kết thúc, không thì cast R qua
     * SkillService (chi phí + hồi chiêu + khoá như mọi kỹ năng) -> PhapSkills -> {@link #startField}; giữ R tiếp thì vùng nở. Nhả: ngừng nở.
     */
    public static void rKey(ServerPlayer p, boolean down) {
        if (!isTho(p)) return;
        St s = st(p);
        if (!down) { s.rHeld = false; return; }
        if (s.fieldOn) { endField(p, s, true); return; }
        SkillState sk = SkillService.state(p);
        if (!sk.stance || Skill.skillPath(p.getData(ModAttachments.CULTIVATION), sk.page) != Path.PHAP) return;
        SkillService.cast(p, 2);
    }

    /** Khởi động vùng gai tại center (gọi từ PhapSkills sau khi SkillService đã trừ mana; hồi chiêu đặt ngay sau). Người cast đang giữ R nên vùng sẽ nở. */
    public static void startField(ServerLevel lv, ServerPlayer p, Vec3 center) {
        St s = st(p);
        endField(p, s, false);
        s.fieldOn = true;
        s.rHeld = true;
        s.fAge = 0;
        s.fHeld = 0;
        s.fDrainAcc = 0;
        s.fRadius = PhapMath.THO_R_RADIUS_MIN;
        s.fCenter = PhapSkills.ground(lv, center.add(0, 1, 0));
        ThoFx.fieldStart(lv, p, s.fCenter, (float) s.fRadius);
        com.hukhong.world.TreeFell.fellRadius(lv, s.fCenter, s.fRadius, 5, 6); // gai đất trồi lên làm đổ cây trong vùng
        p.displayClientMessage(Component.translatable("hukhong.phap.tho.r.start").withColor(COL), true);
    }

    private static void endField(ServerPlayer p, St s, boolean notify) {
        if (!s.fieldOn) return;
        s.fieldOn = false;
        s.rHeld = false;
        if (p.level() instanceof ServerLevel lv) {
            clearSlow(lv, s, Set.of());
            ThoFx.fieldEnd(lv, p, s.fCenter, (float) s.fRadius);
        }
        s.grabbed.clear();
        if (notify) p.displayClientMessage(Component.translatable("hukhong.phap.tho.r.end").withColor(0x999999), true);
    }

    /** Gỡ làm chậm khỏi mọi mục tiêu không còn trong keep. */
    private static void clearSlow(ServerLevel lv, St s, Set<UUID> keep) {
        var it = s.slowed.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (keep.contains(id)) continue;
            it.remove();
            if (lv.getEntity(id) instanceof LivingEntity e) {
                AttributeInstance a = e.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a != null) a.removeModifier(SLOW_ID);
            }
        }
    }

    /** Mục tiêu "bay": quái bay / không trọng lực / đang lơ lửng quá THO_R_FLY_HEIGHT trên mặt đất. */
    private static boolean isFlyer(LivingEntity e, double gap) {
        return e instanceof net.minecraft.world.entity.FlyingMob || e instanceof net.minecraft.world.entity.animal.FlyingAnimal
                || e instanceof net.minecraft.world.entity.ambient.Bat || e.isNoGravity()
                || (!e.onGround() && gap > PhapMath.THO_R_FLY_HEIGHT);
    }

    private static void tickField(ServerPlayer p, St s, ServerLevel lv) {
        if (!s.fieldOn) return;
        if (!p.isAlive() || p.isSpectator()) { endField(p, s, false); return; }
        s.fAge++;
        if (s.rHeld && s.fHeld < PhapMath.THO_R_GROW_TICKS) s.fHeld++;
        s.fRadius = PhapMath.thoRRadius(s.fHeld);
        // hao Pháp Lực theo bán kính
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        s.fDrainAcc += PhapMath.thoRDrainPerSecond(s.fRadius, d.resourceMax()) / 20.0 * com.hukhong.banmenh.BanMenhService.skillCostFactor(p);
        int take = (int) s.fDrainAcc;
        if (take > 0) {
            s.fDrainAcc -= take;
            if (d.resource() <= take) {
                Resources.add(p, -d.resource());
                endField(p, s, false);
                p.displayClientMessage(Component.translatable("hukhong.phap.tho.r.nomana").withColor(0x999999), true);
                return;
            }
            Resources.add(p, -take);
        }
        if (s.fAge >= PhapMath.THO_R_MAX_TICKS) {
            endField(p, s, false);
            p.displayClientMessage(Component.translatable("hukhong.phap.tho.r.expired").withColor(0x999999), true);
            return;
        }
        Vec3 c = s.fCenter;
        float r = (float) s.fRadius;
        double rr = s.fRadius;
        var inside = lv.getEntitiesOfClass(LivingEntity.class,
                new AABB(c.x - rr - 2, c.y - PhapMath.THO_R_REACH_DOWN, c.z - rr - 2, c.x + rr + 2, c.y + PhapMath.THO_R_REACH_UP, c.z + rr + 2),
                e -> PhapRuntime.valid(p, e) && Math.pow(e.getX() - c.x, 2) + Math.pow(e.getZ() - c.z, 2) <= Math.pow(rr + e.getBbWidth() / 2, 2));
        Set<UUID> now = new HashSet<>();
        for (LivingEntity e : inside) {
            now.add(e.getUUID());
            AttributeInstance sp = e.getAttribute(Attributes.MOVEMENT_SPEED);
            if (sp != null) sp.addOrUpdateTransientModifier(new AttributeModifier(SLOW_ID, -PhapMath.THO_R_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            s.slowed.add(e.getUUID());
            // kẻ bay bị kéo xuống đất (setPos từng tick: setDeltaMovement không đủ tin cậy vì AI bay ghi đè)
            double gy = PhapSkills.ground(lv, new Vec3(e.getX(), e.getY() + 0.2, e.getZ())).y;
            double gap = e.getY() - gy;
            if (gap > 0.3 && isFlyer(e, gap)) {
                e.setPos(e.getX(), Math.max(gy, e.getY() - PhapMath.THO_R_PULL_STEP), e.getZ());
                e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 0, 0.2));
                e.fallDistance = 0;
                e.hurtMarked = true;
                Integer nextFx = s.grabbed.get(e.getUUID());
                if (nextFx == null || s.fAge >= nextFx) {
                    ThoFx.grab(lv, p, e, gy, nextFx == null);
                    s.grabbed.put(e.getUUID(), s.fAge + 8);
                }
            } else s.grabbed.remove(e.getUUID());
        }
        clearSlow(lv, s, now);
        s.grabbed.keySet().retainAll(now);
        if (s.fAge % 12 == 1) ThoFx.fieldEdge(lv, p, c, r, lv.random); // viền + vết nứt
        if ((s.fAge - 1) % PhapMath.THO_R_PULSE_TICKS == 0) {          // nhịp gai
            double dmg = PhapMath.THO_R_PULSE_COEF * StatService.spellPower(p);
            ThoFx.fieldPulse(lv, p, c, r, lv.random);
            for (LivingEntity e : inside) {
                PhapSkills.hit(p, e, dmg);
                addBleed(s, e);
                ThoFx.spikeHit(lv, p, e);
            }
        }
    }

    private static void addBleed(St s, LivingEntity e) {
        Bleed b = s.bleeds.computeIfAbsent(e.getUUID(), k -> new Bleed());
        if (b.stacks == 0) b.next = s.clock + PhapMath.THO_R_BLEED_TICK;
        b.stacks = Math.min(PhapMath.THO_R_BLEED_MAX_STACKS, b.stacks + 1);
        b.expire = s.clock + PhapMath.THO_R_BLEED_TICKS;
    }

    /** Chảy máu: mỗi THO_R_BLEED_TICK tick gây THO_R_BLEED_COEF x Phép lực x số tầng (qua hurtBySkill), máu nhỏ giọt, hết hạn thì dọn. */
    private static void tickBleed(ServerPlayer p, St s, ServerLevel lv) {
        if (s.bleeds.isEmpty()) return;
        double sp = StatService.spellPower(p);
        var it = s.bleeds.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Bleed b = en.getValue();
            if (!(lv.getEntity(en.getKey()) instanceof LivingEntity e) || !e.isAlive() || s.clock >= b.expire) { it.remove(); continue; }
            if (s.clock % 4 == 0) ThoFx.bleedDrip(lv, e);
            if (s.clock >= b.next) {
                b.next += PhapMath.THO_R_BLEED_TICK;
                PhapSkills.hit(p, e, PhapMath.THO_R_BLEED_COEF * sp * b.stacks);
            }
        }
    }

    // ================================================================== chung

    /** Mỗi tick người chơi (PlayerTickEvent.Post; GameTest gọi tay vì người chơi giả không nhận sự kiện). */
    public static void tick(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null || !(p.level() instanceof ServerLevel lv)) return;
        s.clock++;
        tickPhase(p, s, lv);
        tickT(p, s, lv);
        tickField(p, s, lv);
        tickBleed(p, s, lv);
    }

    /** Tắt thế / đổi trang: kết thúc xuyên tường và vùng gai R (T đã tung vẫn chạy nốt). */
    public static void cancelPhase(ServerPlayer p) {
        endPhase(p, false);
        St s = STATES.get(p.getUUID());
        if (s != null) endField(p, s, false);
    }

    /** Dọn toàn bộ (chết, thoát, test). */
    public static void reset(ServerPlayer p) {
        St s = STATES.get(p.getUUID());
        if (s == null) return;
        endPhase(p, false);
        endField(p, s, false);
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
