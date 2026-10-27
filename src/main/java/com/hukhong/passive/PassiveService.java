package com.hukhong.passive;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.flight.FlightBans;
import com.hukhong.gates.DamageRules;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nội tại 4 đạo (server-authoritative): Kiếm (Bản Mệnh Kiếm giáng từ trời mỗi 4 đòn), Đạo (5 đòn: hồi máu + sát thương % máu tối đa),
 * Thể (Kiên Giáp), Ma (tia ma tay không + tầng "nổ"). Đòn đánh thường và kỹ năng cùng cộng tầng (mỗi tick tính 1 lần để AoE không spam).
 * Sát thương nội tại tự gây ra đi qua hàng đợi (tick sau, xóa khung bất tử) và không tính lại thành tầng (cờ internal).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PassiveService {
    private PassiveService() {}

    private static final Map<UUID, PassiveState> STATES = new HashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID, MaMark> MARKS = new HashMap<>();
    private static boolean internal = false;

    private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.45f, 0.1f, 0.7f), 1.1f);
    private static final ResourceLocation MA_SLOW_ID = ModAttributes.id("passive/ma_slow");
    private static final ResourceLocation THE_ARMOR_ID = ModAttributes.id("passive/the_armor");
    private static final ResourceLocation THE_HP_ID = ModAttributes.id("passive/the_hp");

    private record Pending(ServerPlayer owner, LivingEntity target, long due, float amount, float healPct) {}

    private static final class MaMark {
        final LivingEntity target;
        UUID owner;
        int stacks;
        long expire;
        MaMark(LivingEntity t) { target = t; }
    }

    // ------------------------------------------------------------------ tiện ích

    static PassiveState st(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new PassiveState()); }

    private static Aptitude apt(CultivationData d, Path path) { return d.aptitude().getOrDefault(path, Aptitude.D); }

    public static boolean isBoss(LivingEntity e) {
        return e.getTags().contains(FlightBans.WORLD_BOSS_TAG) || e.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static boolean validTarget(ServerPlayer p, LivingEntity e) {
        return e != p && !(e instanceof Player) && e.isAlive() && !e.isSpectator()
                && !(e instanceof net.minecraft.world.entity.npc.Npc)
                && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p));
    }

    private static void queue(ServerPlayer owner, LivingEntity target, int delay, float amount, float healPct) {
        PENDING.add(new Pending(owner, target, owner.level().getGameTime() + delay, amount, healPct));
    }

    // ------------------------------------------------------------------ đếm đòn

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (internal || !(e.getSource().getEntity() instanceof ServerPlayer atk) || e.getEntity() == atk) return;
        CultivationData d = atk.getData(ModAttachments.CULTIVATION);
        if (d.paths().isEmpty()) return;
        boolean skill = DamageRules.isSkill();
        boolean melee = !skill && e.getSource().getDirectEntity() == atk;
        if (!skill && !melee) return;
        LivingEntity target = e.getEntity();
        long now = atk.level().getGameTime();
        PassiveState s = st(atk);
        if (skill ? s.lastSkillTick == now : s.lastBasicTick == now) return; // 1 lần / tick / loại (AoE không spam tầng)
        if (skill) s.lastSkillTick = now; else s.lastBasicTick = now;

        if (melee && d.paths().contains(Path.KIEM)) kiemHit(atk, target, s, d, e.getAmount());
        if (d.paths().contains(Path.DAO)) daoHit(atk, target, s, d);
        if (d.paths().contains(Path.THE)) theHit(atk, target, s, d, melee, now);
        sync(atk, s);
    }

    private static void kiemHit(ServerPlayer atk, LivingEntity target, PassiveState s, CultivationData d, float amount) {
        if (++s.kiemHits < PassiveMath.KIEM_EVERY) return;
        s.kiemHits = 0;
        float base = amount > 0f ? amount : (float) StatService.attackPower(atk);
        float dmg = (float) (base * StatService.value(atk, ModAttributes.CRIT_DAMAGE)); // chắc chắn bạo kích
        queue(atk, target, 8, dmg, (float) PassiveMath.kiemHealPct(apt(d, Path.KIEM)));
        if (atk.level() instanceof ServerLevel lv) {
            Vec3 to = target.position().add(0, 0.1, 0);
            Fx.swordStab(lv, to.add(0, 9, 0), to, 8, 0, 1.8f);
        }
    }

    private static void daoHit(ServerPlayer atk, LivingEntity target, PassiveState s, CultivationData d) {
        if (++s.daoStacks < PassiveMath.DAO_EVERY) return;
        s.daoStacks = 0;
        double pct = PassiveMath.daoPct(apt(d, Path.DAO));
        atk.heal((float) (atk.getMaxHealth() * pct));
        float bonus = (float) (target.getMaxHealth() * PassiveMath.pctOfMaxHp(pct, isBoss(target)));
        queue(atk, target, 1, bonus, 0f);
        if (atk.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.END_ROD, atk.getX(), atk.getY() + 1, atk.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
            lv.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 12, 0.3, 0.4, 0.3, 0.12);
        }
    }

    private static void theHit(ServerPlayer atk, LivingEntity target, PassiveState s, CultivationData d, boolean melee, long now) {
        s.theStacks = Math.min(PassiveMath.THE_STACK_CAP, s.theStacks + 1);
        s.theLast = now;
        applyThe(atk, s, d);
        if (melee && !DamageRules.isWeapon(atk.getMainHandItem()) && atk.level() instanceof ServerLevel lv) fistFx(lv, atk, target);
    }

    /** Đấm Thể Tu: sóng xung kích mềm màu cam nhạt, sống lâu hơn (đọc rõ hơn) + khói nhẹ. */
    private static void fistFx(ServerLevel lv, ServerPlayer p, LivingEntity t) {
        Vec3 hit = t.position().add(0, t.getBbHeight() * 0.5, 0);
        Vec3 dir = hit.subtract(p.position()).multiply(1, 0, 1);
        Vec3 n = dir.lengthSqr() < 1e-4 ? Vec3.ZERO : dir.normalize();
        Fx.play(lv, FxType.SHOCKWAVE, new Vec3(hit.x, t.getY() + 0.2, hit.z), Vec3.ZERO, 1.6f, 0xFFD8A8, 16);
        lv.sendParticles(ParticleTypes.CLOUD, hit.x, hit.y, hit.z, 6, n.x * 0.15 + 0.1, 0.1, n.z * 0.15 + 0.1, 0.03);
        lv.playSound(null, hit.x, hit.y, hit.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.5f, 0.8f);
    }

    // ------------------------------------------------------------------ Kiên Giáp

    private static void applyThe(ServerPlayer p, PassiveState s, CultivationData d) {
        int tiers = PassiveMath.theTiers(s.theStacks);
        if (tiers == s.appliedTiers) return;
        s.appliedTiers = tiers;
        Aptitude a = apt(d, Path.THE);
        setMod(p.getAttribute(Attributes.ARMOR), THE_ARMOR_ID, PassiveMath.theArmor(a, s.theStacks));
        setMod(p.getAttribute(Attributes.MAX_HEALTH), THE_HP_ID, PassiveMath.theMaxHp(a, s.theStacks));
    }

    private static void setMod(AttributeInstance inst, ResourceLocation id, double v) {
        if (inst == null) return;
        if (v > 0) inst.addOrUpdateTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_VALUE));
        else inst.removeModifier(id);
    }

    private static void clearThe(ServerPlayer p, PassiveState s) {
        s.theStacks = 0;
        s.appliedTiers = 0;
        setMod(p.getAttribute(Attributes.ARMOR), THE_ARMOR_ID, 0);
        setMod(p.getAttribute(Attributes.MAX_HEALTH), THE_HP_ID, 0);
    }

    // ------------------------------------------------------------------ Ma: tia ma tay không

    public static void maBolt(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || !(p.level() instanceof ServerLevel lv)) return;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        if (!d.paths().contains(Path.MA) || !p.getMainHandItem().isEmpty()) return;
        PassiveState s = st(p);
        long now = lv.getGameTime();
        if (now - s.maCd < PassiveMath.MA_COOLDOWN_TICKS) return;
        s.maCd = now;
        double range = PassiveMath.MA_RANGE;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult bh = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(lv, p, eye, end, box,
                en -> en instanceof LivingEntity le && validTarget(p, le));
        LivingEntity first = null;
        if (eh != null) { end = eh.getLocation(); first = (LivingEntity) eh.getEntity(); }
        Vec3 from = eye.add(look.scale(0.7)).subtract(0, 0.25, 0);
        double len = from.distanceTo(end);
        for (double t = 0; t <= len; t += 0.5) {
            Vec3 q = from.add(look.scale(t));
            lv.sendParticles(DARK, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0);
            if (((int) (t * 2)) % 3 == 0) lv.sendParticles(ParticleTypes.SQUID_INK, q.x, q.y, q.z, 1, 0.04, 0.04, 0.04, 0.005);
        }
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5f, 1.4f);
        if (first == null) return;
        // đòn trúng đầu: sát thương tay không (DamageRules.resolve -> 2 cố định)
        first.hurt(p.damageSources().playerAttack(p), DamageRules.UNARMED_FIXED_DAMAGE);
        explode(lv, p, d, s, end, look, now);
    }

    /** Nổ hình quạt tại điểm trúng: làm chậm + cộng tầng "nổ"; đủ 5 tầng thì kích nổ % máu tối đa. */
    private static void explode(ServerLevel lv, ServerPlayer p, CultivationData d, PassiveState s, Vec3 at, Vec3 dir, long now) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        final Vec3 f = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        final Vec3 origin = at.subtract(f.scale(1.0));
        final double reach = 4.5;
        List<LivingEntity> list = lv.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(reach, 2, reach), e -> {
            if (!validTarget(p, e)) return false;
            Vec3 c = e.getBoundingBox().getCenter().subtract(origin);
            double h = Math.sqrt(c.x * c.x + c.z * c.z);
            return h <= reach + e.getBbWidth() / 2 && (h < 0.8 || (c.x * f.x + c.z * f.z) / h >= 0.5);
        });
        for (int i = -6; i <= 6; i++) { // cung nổ (hạt) +-60 độ
            double a = i * (Math.PI / 3) / 6;
            double cx = f.x * Math.cos(a) - f.z * Math.sin(a), cz = f.x * Math.sin(a) + f.z * Math.cos(a);
            for (double r = 1.0; r <= reach; r += 1.0)
                lv.sendParticles(DARK, origin.x + cx * r, at.y + 0.3, origin.z + cz * r, 1, 0.05, 0.1, 0.05, 0);
        }
        lv.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.3, at.z, 14, 0.5, 0.3, 0.5, 0.04);
        lv.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.4f, 1.6f);
        double pct = PassiveMath.maPct(apt(d, Path.MA));
        int shown = 0;
        for (LivingEntity e : list) {
            MaMark m = MARKS.get(e.getUUID());
            if (m == null || !p.getUUID().equals(m.owner) || m.expire < now) {
                if (m != null) removeSlow(m);
                m = new MaMark(e);
                m.owner = p.getUUID();
                MARKS.put(e.getUUID(), m);
            }
            m.stacks++;
            m.expire = now + PassiveMath.MA_MARK_TICKS;
            if (m.stacks >= PassiveMath.MA_EVERY) {
                queue(p, e, 1, (float) (e.getMaxHealth() * PassiveMath.pctOfMaxHp(pct, isBoss(e))), 0f);
                lv.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 1, 0, 0, 0, 0);
                removeSlow(m);
                MARKS.remove(e.getUUID());
                shown = 0;
            } else {
                applySlow(m);
                shown = m.stacks;
            }
        }
        s.maShown = shown;
        s.maShownUntil = now + PassiveMath.MA_MARK_TICKS;
        sync(p, s);
    }

    private static void applySlow(MaMark m) {
        AttributeInstance inst = m.target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) inst.addOrUpdateTransientModifier(new AttributeModifier(MA_SLOW_ID,
                -PassiveMath.maSlow(m.stacks), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeSlow(MaMark m) {
        AttributeInstance inst = m.target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) inst.removeModifier(MA_SLOW_ID);
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        long now = e.getServer().overworld().getGameTime();
        if (!PENDING.isEmpty()) {
            for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
                Pending pd = it.next();
                if (pd.due() > now) continue;
                it.remove();
                if (!pd.owner().isAlive() || pd.owner().hasDisconnected() || !pd.target().isAlive() || pd.target().isRemoved()) continue;
                internal = true;
                try {
                    pd.target().invulnerableTime = 0;
                    DamageRules.hurtBySkill(pd.owner(), pd.target(), pd.amount());
                } finally { internal = false; }
                if (pd.healPct() > 0f) pd.owner().heal(pd.amount() * pd.healPct());
            }
        }
        if (!MARKS.isEmpty() && now % 5 == 0) {
            for (Iterator<MaMark> it = MARKS.values().iterator(); it.hasNext(); ) {
                MaMark m = it.next();
                if (m.expire < now || !m.target.isAlive() || m.target.isRemoved()) { removeSlow(m); it.remove(); }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PassiveState s = STATES.get(p.getUUID());
        if (s == null) return;
        long now = p.level().getGameTime();
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        if (s.theStacks > 0 && (!d.paths().contains(Path.THE) || now - s.theLast > PassiveMath.THE_HOLD_TICKS)) clearThe(p, s);
        if (!d.paths().contains(Path.KIEM)) s.kiemHits = 0;
        if (!d.paths().contains(Path.DAO)) s.daoStacks = 0;
        if (s.maShown > 0 && now > s.maShownUntil) s.maShown = 0;
        sync(p, s);
    }

    private static void sync(ServerPlayer p, PassiveState s) {
        if (s.sKiem == s.kiemHits && s.sDao == s.daoStacks && s.sThe == s.theStacks && s.sMa == s.maShown) return;
        s.sKiem = s.kiemHits; s.sDao = s.daoStacks; s.sThe = s.theStacks; s.sMa = s.maShown;
        PacketDistributor.sendToPlayer(p, new PassiveSyncPayload(s.kiemHits, s.daoStacks, s.theStacks, s.maShown));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            PassiveState s = STATES.remove(p.getUUID());
            if (s != null) clearThe(p, s);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            PassiveState s = STATES.remove(p.getUUID());
            if (s != null) clearThe(p, s);
            PacketDistributor.sendToPlayer(p, new PassiveSyncPayload(0, 0, 0, 0));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            PassiveState s = STATES.get(p.getUUID());
            if (s != null) { clearThe(p, s); s.kiemHits = 0; s.daoStacks = 0; s.maShown = 0; sync(p, s); }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        STATES.clear();
        PENDING.clear();
        MARKS.clear();
    }
}
