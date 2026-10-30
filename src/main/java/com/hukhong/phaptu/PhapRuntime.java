package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.flight.FlightBans;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.pathrules.PathRules;
import com.hukhong.skill.SkillAttachments;
import com.hukhong.skill.SkillEffects;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bị động + trạng thái chạy của Pháp Tu (server). Toán thuần nằm ở {@link PhapMath}. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhapRuntime {
    private PhapRuntime() {}

    static final class St {
        int quang, loi, thuy;
        double quangAcc;
        long lastQuang, lastLoi, lastThuyGain, lastThuyDecay, nextPulse;
        int freeMask;
        long freeUntil;
        long lastKimFx, shieldEnd, lastMove;
        float shieldAbs;
        Vec3 lastPos = Vec3.ZERO;
        final List<UUID> turrets = new ArrayList<>();
    }

    private static final class Burn {
        int stacks;
        long expire, nextSpread;
        UUID owner;
        ResourceKey<Level> dim;
    }

    private static final class Stun {
        long end;
        boolean wasNoAi;
        ResourceKey<Level> dim;
    }

    private static final Map<UUID, St> STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Burn> BURN = new HashMap<>();
    private static final Map<UUID, Stun> STUN = new HashMap<>();
    private static boolean busy = false;
    /** Tinh Hà Quang Trụ: nhịp tia này không cộng tầng bị động (giới hạn 1 tầng / 0,5s / mục tiêu). */
    public static boolean noStack = false;

    private static final ResourceLocation KIM_BASE = ModAttributes.id("phap/kim_speed_base"), KIM_TOTAL = ModAttributes.id("phap/kim_speed_total");
    private static final ResourceLocation SPRINT = ResourceLocation.withDefaultNamespace("sprinting");

    static St st(Player p) { return STATE.computeIfAbsent(p.getUUID(), k -> new St()); }

    private static boolean isPhap(Player p) { return PhapTu.element(p) != null; }

    // ------------------------------------------------------------------ chi phí / hồi chiêu (SkillService gọi)

    /** Quang: vòng kỹ năng miễn phí sau khi nổ 15 tầng. True nếu phím này được miễn phí (và đã tiêu 1 lượt miễn phí). */
    public static boolean consumeFree(ServerPlayer p, Path page, int slot) {
        if (page != Path.PHAP || PhapTu.element(p) != PhapElement.QUANG) return false;
        St s = st(p);
        if (s.freeMask == 0 || p.level().getGameTime() > s.freeUntil || (s.freeMask & (1 << slot)) == 0) return false;
        s.freeMask &= ~(1 << slot);
        return true;
    }

    /** Hệ số hồi chiêu (Kim chậm hơn). */
    public static double cooldownMult(ServerPlayer p, Path page) {
        if (page != Path.PHAP) return 1.0;
        PhapElement e = PhapTu.element(p);
        return e == null ? 1.0 : PhapMath.cooldownMult(e);
    }

    // ------------------------------------------------------------------ chỉ số (StatService.refresh gọi)

    private static void mod(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                            String id, double v) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation rl = ModAttributes.id("phap/" + id);
        if (v == 0) inst.removeModifier(rl);
        else inst.addOrUpdateTransientModifier(new AttributeModifier(rl, v, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** Kim Cương: +% Công/Phép/Phòng. Gọi từ StatService.refresh. */
    public static void applyPassives(ServerPlayer p) {
        double v = PhapTu.element(p) == PhapElement.KIM ? PhapMath.kimBonus(PhapTu.aptitude(p)) : 0;
        mod(p, ModAttributes.holder(ModAttributes.ATTACK_POWER), "kim_attack", v);
        mod(p, ModAttributes.holder(ModAttributes.SPELL_POWER), "kim_spell", v);
        mod(p, ModAttributes.holder(ModAttributes.DEFENSE), "kim_defense", v);
        if (v == 0) {
            AttributeInstance spd = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) { spd.removeModifier(KIM_BASE); spd.removeModifier(KIM_TOTAL); }
        }
    }

    /** Kim: mọi buff tốc độ (trừ chạy bộ vanilla) chỉ còn 25% hiệu lực. */
    private static void kimSpeed(ServerPlayer p) {
        AttributeInstance spd = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd == null) return;
        double sumBase = 0, prodTotal = 1;
        for (AttributeModifier m : spd.getModifiers()) {
            if (m.id().equals(KIM_BASE) || m.id().equals(KIM_TOTAL) || m.id().equals(SPRINT) || m.amount() <= 0) continue;
            if (m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) sumBase += m.amount();
            else if (m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) prodTotal *= 1 + m.amount();
        }
        setMod(spd, KIM_BASE, -sumBase * (1 - PhapMath.KIM_SPEED_EFFECT), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        double kept = 1 + PhapMath.kimSpeed(prodTotal - 1);
        setMod(spd, KIM_TOTAL, kept / prodTotal - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void setMod(AttributeInstance inst, ResourceLocation id, double v, AttributeModifier.Operation op) {
        AttributeModifier cur = inst.getModifier(id);
        if (Math.abs(v) < 1e-6) { if (cur != null) inst.removeModifier(id); return; }
        if (cur == null || Math.abs(cur.amount() - v) > 1e-6) inst.addOrUpdateTransientModifier(new AttributeModifier(id, v, op));
    }

    // ------------------------------------------------------------------ đòn trúng -> tầng bị động

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post e) {
        if (busy || noStack || !(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p) return;
        PhapElement el = PhapTu.element(p);
        if (el == null || e.getNewDamage() <= 0f || !(p.level() instanceof ServerLevel sl)) return;
        LivingEntity t = e.getEntity();
        Aptitude apt = PhapTu.aptitude(p);
        St s = st(p);
        long now = sl.getGameTime();
        busy = true;
        try {
            switch (el) {
                case QUANG -> {
                    if (now - s.lastQuang > 160) { s.quang = 0; s.quangAcc = 0; }
                    s.lastQuang = now;
                    s.quang++;
                    s.quangAcc += e.getNewDamage();
                    if (s.quang >= PhapMath.QUANG_STACKS) quangExplode(sl, p, t, s, apt);
                    else bar(p, "hukhong.phap.stack.quang", s.quang, PhapMath.QUANG_STACKS);
                }
                case LOI -> {
                    if (now - s.lastLoi > 120) s.loi = 0;
                    s.lastLoi = now;
                    s.loi++;
                    if (s.loi >= PhapMath.LOI_STACKS) {
                        s.loi = 0;
                        float d = PhapMath.loiExplodeDamage(apt, t.getMaxHealth(), StatService.spellPower(p));
                        t.invulnerableTime = 0;
                        DamageRules.hurtBySkill(p, t, d);
                        stun(t, PhapMath.loiStunSec(apt));
                        Fx.play(sl, FxType.LIGHTNING_BOLT, t.position(), Vec3.ZERO, 1.2f, PhapElement.LOI.rgb);
                        Fx.elem(sl, p, Fx.E_ARC, t.position().add(0, t.getBbHeight() * 0.5, 0), Vec3.ZERO, 2.4f, PhapElement.LOI.rgb, 9); // Lôi Ấn nổ: sét toả tròn
                    } else bar(p, "hukhong.phap.stack.loi", s.loi, PhapMath.LOI_STACKS);
                }
                case HOA -> addBurn(sl, p, t, 1);
                case THUY -> gainThuy(p, s, now, 1, 4);
                case KIM -> {
                    if (now - s.lastKimFx >= 12) { // Kim Cương: chớp phi kim bạc ở mục tiêu khi được cộng sức mạnh
                        s.lastKimFx = now;
                        Fx.elem(sl, p, Fx.E_NEEDLES, t.position().add(0, t.getBbHeight() * 0.6, 0), Vec3.ZERO, 0.7f, PhapElement.KIM.rgb, 8);
                    }
                }
                default -> { }
            }
        } finally {
            busy = false;
        }
    }

    private static void bar(ServerPlayer p, String key, int n, int max) {
        p.displayClientMessage(Component.translatable(key, n, max), true);
    }

    private static void quangExplode(ServerLevel sl, ServerPlayer p, LivingEntity t, St s, Aptitude apt) {
        float dmg = (float) (PhapMath.quangExplode(apt) * s.quangAcc);
        s.quang = 0;
        s.quangAcc = 0;
        s.freeMask = 0b1111;
        s.freeUntil = sl.getGameTime() + PhapMath.QUANG_FREE_WINDOW_TICKS;
        Vec3 c = t.position();
        for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(4), x -> valid(p, x))) {
            v.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, v, dmg);
        }
        Fx.elem(sl, p, Fx.E_HALOS, c, Vec3.ZERO, 3.6f, PhapElement.QUANG.rgb, 22); // Quang Mang nổ: vòng hào quang + sao lăng kính
        Fx.play(sl, FxType.LIGHT_PILLAR, c, Vec3.ZERO, 0.9f, PhapElement.QUANG.rgb, 24);
        p.displayClientMessage(Component.translatable("hukhong.phap.quang.free"), true);
    }

    public static boolean valid(ServerPlayer p, LivingEntity e) {
        return e != p && !(e instanceof Player) && e.isAlive() && !e.isSpectator()
                && !(e instanceof PhapTurret) && !(e instanceof net.minecraft.world.entity.npc.Npc)
                && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p));
    }

    // ------------------------------------------------------------------ Hỏa: Thiêu Nhiên

    static void addBurn(ServerLevel sl, ServerPlayer p, LivingEntity t, int n) {
        Burn b = BURN.computeIfAbsent(t.getUUID(), k -> new Burn());
        b.stacks = Math.min(PhapMath.HOA_STACKS, b.stacks + n);
        b.expire = sl.getGameTime() + PhapMath.HOA_DURATION_TICKS;
        b.owner = p.getUUID();
        b.dim = sl.dimension();
        if (b.nextSpread == 0) b.nextSpread = sl.getGameTime() + PhapMath.HOA_SPREAD_TICKS;
    }

    private static void tickBurn(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Burn>> it = BURN.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Burn b = en.getValue();
            ServerLevel sl = server.getLevel(b.dim);
            ServerPlayer o = server.getPlayerList().getPlayer(b.owner);
            LivingEntity t = sl != null && sl.getEntity(en.getKey()) instanceof LivingEntity le ? le : null;
            long now = sl == null ? 0 : sl.getGameTime();
            if (sl == null || o == null || t == null || !t.isAlive() || now > b.expire || PhapTu.element(o) != PhapElement.HOA) {
                it.remove();
                continue;
            }
            float dmg = (float) (PhapMath.hoaBurnPerSecond(PhapTu.aptitude(o), b.stacks, StatService.spellPower(o))
                    * PhapMath.HOA_TICK / 20.0);
            t.invulnerableTime = 0;
            boolean was = busy;
            busy = true;
            try { DamageRules.hurtBySkill(o, t, dmg); } finally { busy = was; }
            sl.sendParticles(ParticleTypes.FLAME, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 2 + b.stacks, 0.25, 0.4, 0.25, 0.02);
            if (now % 20 == 0) Fx.elem(sl, o, Fx.E_TONGUES, t.position(), Vec3.ZERO, 0.5f + 0.12f * b.stacks, PhapElement.HOA.rgb, 16); // lưỡi lửa bốc quanh kẻ đang cháy
            if (b.stacks >= PhapMath.HOA_STACKS && now % 20 == 0)
                sl.sendParticles(ParticleTypes.LAVA, t.getX(), t.getY() + 1, t.getZ(), 3, 0.6, 0.4, 0.6, 0.1);
        }
    }

    /** Lây lửa an toàn ngoài vòng duyệt (gọi trước tickBurn). */
    private static void spreadBurn(MinecraftServer server) {
        List<Object[]> pend = new ArrayList<>();
        for (var en : BURN.entrySet()) {
            Burn b = en.getValue();
            if (b.stacks < PhapMath.HOA_STACKS) continue;
            ServerLevel sl = server.getLevel(b.dim);
            ServerPlayer o = server.getPlayerList().getPlayer(b.owner);
            if (sl == null || o == null || sl.getGameTime() < b.nextSpread || !(sl.getEntity(en.getKey()) instanceof LivingEntity t)) continue;
            b.nextSpread = sl.getGameTime() + PhapMath.HOA_SPREAD_TICKS;
            int n = 0;
            for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(4), x -> x != t && valid(o, x))) {
                if (n++ >= 3) break;
                pend.add(new Object[]{sl, o, v});
            }
        }
        for (Object[] a : pend) addBurn((ServerLevel) a[0], (ServerPlayer) a[1], (LivingEntity) a[2], 2);
    }

    // ------------------------------------------------------------------ Lôi: choáng

    /** Choáng điện: toàn phần với quái nhỏ và boss thường, nửa thời gian với boss thế giới. */
    public static void stun(LivingEntity t, double seconds) {
        if (!(t.level() instanceof ServerLevel sl) || !(t instanceof Mob m)) return;
        double sec = PhapMath.stunFor(seconds, t.getTags().contains(FlightBans.WORLD_BOSS_TAG));
        long end = sl.getGameTime() + Math.max(1, Math.round(sec * 20));
        Stun s = STUN.get(t.getUUID());
        if (s == null) {
            s = new Stun();
            s.wasNoAi = m.isNoAi();
            s.dim = sl.dimension();
            STUN.put(t.getUUID(), s);
            m.setNoAi(true);
        }
        s.end = Math.max(s.end, end);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 12, 0.3, 0.5, 0.3, 0.2);
    }

    private static void tickStun(MinecraftServer server, boolean releaseAll) {
        Iterator<Map.Entry<UUID, Stun>> it = STUN.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Stun s = en.getValue();
            ServerLevel sl = server.getLevel(s.dim);
            Mob m = sl != null && sl.getEntity(en.getKey()) instanceof Mob mm ? mm : null;
            if (m == null) { it.remove(); continue; }
            if (releaseAll || sl.getGameTime() >= s.end || !m.isAlive()) {
                m.setNoAi(s.wasNoAi);
                it.remove();
            } else {
                if (sl.getGameTime() % 6 == 0)
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 2, 0.3, 0.4, 0.3, 0.1);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer srv = e.getServer();
        long t = srv.getTickCount();
        if (!STUN.isEmpty()) tickStun(srv, false);
        if (!BURN.isEmpty() && t % PhapMath.HOA_TICK == 0) {
            spreadBurn(srv);
            tickBurn(srv);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        tickStun(e.getServer(), true); // trả NoAI trước khi lưu thế giới
        BURN.clear();
        STATE.clear();
    }

    // ------------------------------------------------------------------ Thủy: Thủy Ấn

    private static void gainThuy(ServerPlayer p, St s, long now, int n, int icd) {
        if (now - s.lastThuyGain < icd) return;
        s.lastThuyGain = now;
        s.lastThuyDecay = now;
        int old = s.thuy;
        s.thuy = Math.min(PhapMath.THUY_MAX_STACKS, s.thuy + n);
        if (s.thuy != old) bar(p, "hukhong.phap.stack.thuy", s.thuy, PhapMath.THUY_MAX_STACKS);
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0f || PhapTu.element(p) != PhapElement.THUY) return;
        gainThuy(p, st(p), p.level().getGameTime(), 1, 10);
    }

    /** Thủy: miễn thương % theo 3 tầng (5/10/15 tầng). */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PhapElement el = PhapTu.element(p);
        if (el == PhapElement.THUY) {
            double imm = PhapMath.thuyImmunity(PhapTu.aptitude(p), st(p).thuy);
            if (imm > 0) e.setAmount((float) (e.getAmount() * (1 - imm)));
        } else if (el == PhapElement.THO && p.level() instanceof ServerLevel sl) { // Kiên Cố
            St s = st(p);
            boolean still = sl.getGameTime() - s.lastMove >= PhapMath.THO_STILL_TICKS;
            double dr = PhapMath.thoReduction(PhapTu.aptitude(p), still, turretCount(sl, p));
            if (dr > 0) e.setAmount((float) (e.getAmount() * (1 - dr)));
        }
    }

    private static void thuyPulse(ServerLevel sl, ServerPlayer p, St s, long now) {
        Aptitude apt = PhapTu.aptitude(p);
        var cs = com.hukhong.skill.SkillAttachments.STATE;
        boolean combat = now - p.getData(cs).lastCombat < 200;
        if (!combat) {
            if (s.thuy > 0 && now - s.lastThuyDecay >= 40) { s.lastThuyDecay = now; s.thuy--; }
            return;
        }
        double r = 2.0 + s.thuy * 0.25;
        Vec3 c = p.position().add(0, 0.2, 0);
        Fx.groundRing(sl, p, c, (float) r, PhapElement.THUY.rgb, 14); // xung Thủy: vòng nước nở trên đất
        sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.5, c.z, 10 + s.thuy, r * 0.5, 0.3, r * 0.5, 0.1);
        if (s.thuy >= PhapMath.THUY_MAX_STACKS) {
            float dmg = PhapMath.thuyBurstDamage(apt, p.getMaxHealth(), StatService.spellPower(p));
            boolean was = busy;
            busy = true;
            try {
                for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(PhapMath.THUY_RADIUS, 2, PhapMath.THUY_RADIUS),
                        x -> valid(p, x) && x.distanceToSqr(p) <= PhapMath.THUY_RADIUS * PhapMath.THUY_RADIUS)) {
                    v.invulnerableTime = 0;
                    DamageRules.hurtBySkill(p, v, dmg);
                }
            } finally { busy = was; }
            Fx.groundRing(sl, p, c, (float) PhapMath.THUY_RADIUS, 0xFFFFFF, 20);
            Fx.groundRing(sl, p, c, (float) PhapMath.THUY_RADIUS * 0.6f, PhapElement.THUY.rgb, 22);
            Fx.rune(sl, p, c, (float) PhapMath.THUY_RADIUS * 0.8f, PhapElement.THUY.rgb, 30, 6);
            s.thuy = 5;
            return;
        }
        s.thuy++;
        // xung hồi nhẹ cho đồng đội trong vòng
        for (ServerPlayer a : sl.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r, 2, r), x -> x.distanceToSqr(p) <= r * r)) {
            a.heal((float) (a.getMaxHealth() * (0.003 + 0.0015 * s.thuy)));
        }
    }

    // ------------------------------------------------------------------ Thổ: tháp

    /** Triệu hồi 1 tháp tại pos; vượt giới hạn thì thay tháp cũ nhất. Trả tháp mới hoặc null. */
    public static PhapTurret summonTurret(ServerLevel sl, ServerPlayer p, Vec3 pos) {
        St s = st(p);
        Aptitude apt = PhapTu.aptitude(p);
        s.turrets.removeIf(id -> !(sl.getEntity(id) instanceof PhapTurret t) || !t.isAlive());
        // Hợp Thể: trụ cùng chủ (chưa hợp thể) cách <= 5 block -> gộp thành MỘT trụ laser (to hơn, sống = tổng, máu = tổng)
        PhapTurret partner = null;
        for (UUID id : s.turrets) {
            if (sl.getEntity(id) instanceof PhapTurret o && o.isAlive() && !o.isMerged() && o.position().distanceTo(pos) <= PhapMath.THO_MERGE_RADIUS
                    && (partner == null || o.position().distanceToSqr(pos) < partner.position().distanceToSqr(pos))) partner = o;
        }
        if (partner != null) {
            Vec3 mid = partner.position().add(pos).scale(0.5);
            partner.absorb(PhapMath.THO_LIFE_TICKS, PhapMath.thoTurretHp(apt, p.getMaxHealth()), mid);
            Fx.elem(sl, p, Fx.E_SPIKES, mid, Vec3.ZERO, 2.2f, PhapElement.THO.rgb, 34);
            Fx.groundCrack(sl, p, mid.add(0, 0.06, 0), 3.4f, PhapElement.THO.rgb, 8, 40);
            Fx.groundRing(sl, p, mid.add(0, 0.06, 0), 3.0f, 0xFFE9A0, 18);
            Fx.shake(sl, mid, 0.6f, 8, 20f);
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()),
                    mid.x, mid.y + 1.2, mid.z, 30, 0.6, 0.8, 0.6, 0.15);
            sl.playSound(null, mid.x, mid.y, mid.z, net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.4f);
            return partner;
        }
        int max = PhapMath.thoMaxTurrets(apt);
        while (s.turrets.size() >= max) {
            if (sl.getEntity(s.turrets.remove(0)) instanceof PhapTurret old) old.discard();
        }
        PhapTurret t = PhapTuRegistry.TURRET.get().create(sl);
        if (t == null) return null;
        t.moveTo(pos.x, pos.y, pos.z, p.getYRot(), 0);
        t.setup(p, PhapMath.thoTurretHp(apt, p.getMaxHealth()), PhapMath.thoShotDamage(apt, StatService.spellPower(p)), PhapMath.thoShotTicks(max));
        sl.addFreshEntity(t);
        s.turrets.add(t.getUUID());
        Fx.elem(sl, p, Fx.E_SPIKES, pos, Vec3.ZERO, 0.9f, PhapElement.THO.rgb, 28); // gai đá trồi quanh chân trụ
        Fx.groundCrack(sl, p, pos.add(0, 0.06, 0), 1.8f, PhapElement.THO.rgb, 6, 30);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()),
                pos.x, pos.y + 0.3, pos.z, 20, 0.4, 0.3, 0.4, 0.1);
        return t;
    }

    /** Thạch Giáp: khiên hấp thụ shield trong ticks + miễn đẩy lùi. */
    public static void thoArmor(ServerPlayer p, float shield, int ticks) {
        St s = st(p);
        long now = p.level().getGameTime();
        s.shieldAbs = Math.max(s.shieldAbs, shield);
        s.shieldEnd = now + ticks;
        // 1.21: setAbsorptionAmount bị kẹp bởi thuộc tính MAX_ABSORPTION (mặc định 0) -> phải nâng trần trước, nếu không khiên = 0
        AttributeInstance mx = p.getAttribute(Attributes.MAX_ABSORPTION);
        if (mx != null) mx.addOrUpdateTransientModifier(new AttributeModifier(THO_ABS, s.shieldAbs, AttributeModifier.Operation.ADD_VALUE));
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), shield));
        AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.addOrUpdateTransientModifier(new AttributeModifier(THO_KB, 1.0, AttributeModifier.Operation.ADD_VALUE));
    }

    /** GameTest: người chơi giả không nhận PlayerTickEvent, gọi tay phần hết hạn khiên. */
    public static void tickThoForTest(ServerPlayer p) { tickThoArmor(p, st(p), p.level().getGameTime()); }

    public static boolean thoArmored(ServerPlayer p) { return st(p).shieldEnd > p.level().getGameTime(); }

    private static final ResourceLocation THO_KB = ModAttributes.id("phap/tho_kb"), THO_ABS = ModAttributes.id("phap/tho_abs");

    private static void tickThoArmor(ServerPlayer p, St s, long now) {
        if (s.shieldEnd != 0 && now >= s.shieldEnd) {
            s.shieldEnd = 0;
            if (p.getAbsorptionAmount() <= s.shieldAbs) p.setAbsorptionAmount(0);
            s.shieldAbs = 0;
            AttributeInstance mx = p.getAttribute(Attributes.MAX_ABSORPTION);
            if (mx != null) mx.removeModifier(THO_ABS);
            AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.removeModifier(THO_KB);
        }
        if (p.position().distanceToSqr(s.lastPos) > 0.0016) { s.lastPos = p.position(); s.lastMove = now; }
    }

    public static int turretCount(ServerLevel sl, ServerPlayer p) {
        St s = st(p);
        s.turrets.removeIf(id -> !(sl.getEntity(id) instanceof PhapTurret t) || !t.isAlive());
        return s.turrets.size();
    }

    public static void empowerTurrets(ServerLevel sl, ServerPlayer p, int ticks) {
        for (UUID id : st(p).turrets) if (sl.getEntity(id) instanceof PhapTurret t) t.empower(ticks);
    }

    // ------------------------------------------------------------------ tick người chơi

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel sl)) return;
        var d = p.getData(ModAttachments.CULTIVATION);
        if (!d.paths().contains(Path.PHAP)) return;
        long now = sl.getGameTime();
        PhapElement el = PhapTu.element(p);
        if (el == null) {
            if (now % 20 == 0) { PhapTu.setElement(p, PhapTu.defaultElement(d, p)); StatService.refresh(p); }
            return;
        }
        St s = st(p);
        if (now % 20 == 0) {
            var sk = p.getData(SkillAttachments.STATE);
            double rg = StatService.regen(p);
            double mm = sk.meditating() ? com.hukhong.age.AgeMath.meditationMult(com.hukhong.age.AgeService.stageOf(p)) : 1.0;
            Resources.add(p, (int) Math.max(1, Math.round((sk.meditating() ? PathRules.PHAP_MEDITATE_REGEN * mm : PathRules.PHAP_REGEN) * rg)));
        }
        switch (el) {
            case THUY -> { if (now % PhapMath.THUY_PULSE_TICKS == 0) thuyPulse(sl, p, s, now); }
            case KIM -> { if (now % 10 == 0) kimSpeed(p); }
            case QUANG -> {
                if (s.quang > 0 && now - s.lastQuang > 160) { s.quang = 0; s.quangAcc = 0; }
                if (now % 4 == 0 && s.quang > 0)
                    sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.4, 0.6, 0.4, 0.01);
            }
            case THO -> {
                tickThoArmor(p, s, now);
                if (now % 20 == 0) turretCount(sl, p);
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        STATE.remove(e.getEntity().getUUID());
    }

    // ---- đọc trạng thái bị động (GameTest / HUD debug)
    public static int burnStacks(LivingEntity e) { Burn b = BURN.get(e.getUUID()); return b == null ? 0 : b.stacks; }
    public static int quangFreeMask(Player p) { return st(p).freeMask; }
    public static int quangStacks(Player p) { return st(p).quang; }
    public static int thuyStacks(Player p) { return st(p).thuy; }
    public static float thoShieldLeft(Player p) { return st(p).shieldAbs; }

    /** Test GameTest/lệnh: xóa mọi trạng thái runtime. */
    public static void reset(Player p) { STATE.remove(p.getUUID()); }

    /** Danh sách kẻ địch trong bán kính (kỹ năng dùng). */
    static boolean isEnemyLike(LivingEntity e) { return e instanceof Enemy; }
}
