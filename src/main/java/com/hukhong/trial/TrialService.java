package com.hukhong.trial;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.core.Path;
import com.hukhong.network.BreakthroughPayload;
import com.hukhong.pathrules.MaRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Khao Hach moc 90+ (S3): Tam Ma (bong nhan ban) roi Loi Kiep (cac dot set co vong bao truoc).
 * Bat buoc: CultivationService.unlockMilestone chi mo khoa moc khi ca hai qua. Phien chi nam trong RAM;
 * mat ket noi/chet = that bai, tu thu lai sau thoi gian phat.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TrialService {
    private TrialService() {}

    public enum Type { TAM_MA, LOI_KIEP }

    static final String CLONE_TAG = "hukhong_tam_ma";
    static final int ARENA_R = 14, TAM_MA_TIMEOUT = 2400;

    private static final class Strike {
        final Vec3 pos; int left; final float dmg;
        Strike(Vec3 p, int l, float d) { pos = p; left = l; dmg = d; }
    }

    private static final class Session {
        final UUID player; Type type; final boolean chain; final Vec3 center; final int level;
        UUID clone; int ticks; int wave = -1; int gap; final List<Strike> strikes = new ArrayList<>();
        Session(UUID p, Type t, boolean chain, Vec3 c, int level) { player = p; type = t; this.chain = chain; center = c; this.level = level; }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> RETRY_AT = new HashMap<>();
    private static final Map<UUID, Integer> FAILS = new HashMap<>();

    public static boolean active(ServerPlayer p) { return SESSIONS.containsKey(p.getUUID()); }

    /** Moc tu CultivationService.unlockMilestone: bat dau chuoi Khao Hach (Tam Ma -> Loi Kiep) roi moi mo khoa. */
    public static void begin(ServerPlayer p) {
        if (active(p) || RETRY_AT.containsKey(p.getUUID())) return;
        start(p, Type.TAM_MA, true);
    }

    /** Lenh thu: chay rieng mot thu thach, khong mo khoa moc. */
    public static void test(ServerPlayer p, Type t) {
        Session old = SESSIONS.remove(p.getUUID());
        if (old != null) discardClone(p.server, old);
        start(p, t, false);
    }

    private static void start(ServerPlayer p, Type t, boolean chain) {
        int lvl = Math.max(TrialMath.MIN_LEVEL, CultivationService.get(p).level());
        Session s = new Session(p.getUUID(), t, chain, p.position(), lvl);
        SESSIONS.put(p.getUUID(), s);
        p.sendSystemMessage(Component.translatable("hukhong.trial.start." + (t == Type.TAM_MA ? "tam_ma" : "loi_kiep")));
        if (t == Type.TAM_MA) spawnClone(p, s);
        else s.gap = 40;
    }

    // ---------------------------------------------------------------- Tam Ma

    private static void spawnClone(ServerPlayer p, Session s) {
        ServerLevel lv = p.serverLevel();
        Husk h = EntityType.HUSK.create(lv);
        if (h == null) { finish(p, s); return; }
        double sc = TrialMath.cloneScale(s.level);
        h.setPos(s.center.x + 5, s.center.y, s.center.z);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                h.setItemSlot(slot, p.getItemBySlot(slot).copy());
                h.setDropChance(slot, 0f);
            }
        }
        double hp = p.getMaxHealth() * sc;
        h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        h.setHealth((float) hp);
        h.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(2.0, p.getAttributeValue(Attributes.ATTACK_DAMAGE) * sc));
        h.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        h.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
        h.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false)); // vien sang = tint toi
        h.setCustomName(Component.translatable("hukhong.trial.clone_name", p.getName()));
        h.setPersistenceRequired();
        h.addTag(CLONE_TAG);
        h.setTarget(p);
        lv.addFreshEntity(h);
        s.clone = h.getUUID();
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (e.getEntity().getTags().contains(CLONE_TAG)) e.setCanceled(true);
    }

    /** Bản sao Tâm Ma lưu trong chunk (server tắt giữa chừng) -> xóa khi nạp lại, tránh quái mồ côi. */
    @SubscribeEvent
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() && e.getEntity().getTags().contains(CLONE_TAG)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Session s = SESSIONS.get(p.getUUID());
            if (s != null) fail(p, s);
        }
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer srv = e.getServer();
        if (!RETRY_AT.isEmpty()) {
            long now = srv.overworld().getGameTime();
            for (Iterator<Map.Entry<UUID, Long>> it = RETRY_AT.entrySet().iterator(); it.hasNext(); ) {
                var en = it.next();
                if (now < en.getValue()) continue;
                ServerPlayer p = srv.getPlayerList().getPlayer(en.getKey());
                if (p == null) continue; // cho dang nhap lai
                it.remove();
                if (CultivationService.isMilestoneLocked(p)) start(p, Type.TAM_MA, true);
            }
        }
        if (SESSIONS.isEmpty()) return;
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            ServerPlayer p = srv.getPlayerList().getPlayer(s.player);
            if (p == null) { // rot mang = that bai am tham, thu lai sau
                SESSIONS.remove(s.player);
                discardClone(srv, s);
                if (s.chain) RETRY_AT.put(s.player, srv.overworld().getGameTime() + 600);
                continue;
            }
            if (!p.isAlive()) continue;
            s.ticks++;
            if (p.position().distanceTo(s.center) > ARENA_R) {
                p.teleportTo(s.center.x, s.center.y, s.center.z);
                p.sendSystemMessage(Component.translatable("hukhong.trial.arena"));
            }
            if (s.type == Type.TAM_MA) tickTamMa(p, s); else tickLoi(p, s);
        }
    }

    private static void tickTamMa(ServerPlayer p, Session s) {
        ServerLevel lv = p.serverLevel();
        var c = s.clone == null ? null : lv.getEntity(s.clone);
        if (s.ticks % 5 == 0 && c != null)
            lv.sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(), c.getX(), c.getY() + 1, c.getZ(), 4, 0.3, 0.7, 0.3, 0.02);
        if (c instanceof Husk h && h.getTarget() == null) h.setTarget(p);
        if (c == null || !c.isAlive()) { finish(p, s); return; }
        if (s.ticks > TAM_MA_TIMEOUT) fail(p, s);
    }

    // ---------------------------------------------------------------- Loi Kiep

    private static void tickLoi(ServerPlayer p, Session s) {
        ServerLevel lv = p.serverLevel();
        for (Iterator<Strike> it = s.strikes.iterator(); it.hasNext(); ) {
            Strike k = it.next();
            if (--k.left > 0) {
                if (k.left % 4 == 0) ring(lv, k.pos, 2.5, 24);
                continue;
            }
            it.remove();
            strike(lv, p, k);
        }
        if (!s.strikes.isEmpty()) return;
        if (s.gap > 0) { s.gap--; return; }
        s.wave++;
        if (s.wave >= TrialMath.waves(s.level)) { finish(p, s); return; }
        int n = TrialMath.strikesInWave(s.level, s.wave);
        int tele = TrialMath.telegraphTicks(s.wave);
        float dmg = TrialMath.damage(s.level, s.wave);
        for (int i = 0; i < n; i++) {
            Vec3 pos = i == 0 ? p.position()
                    : p.position().add((p.getRandom().nextDouble() - 0.5) * 12, 0, (p.getRandom().nextDouble() - 0.5) * 12);
            s.strikes.add(new Strike(pos, tele, dmg));
        }
        s.gap = 20;
        p.displayClientMessage(Component.translatable("hukhong.trial.wave", s.wave + 1, TrialMath.waves(s.level)), true);
        lv.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.4f, 1.6f);
    }

    private static void ring(ServerLevel lv, Vec3 c, double r, int pts) {
        for (int i = 0; i < pts; i++) {
            double a = i * Math.PI * 2 / pts;
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static void strike(ServerLevel lv, ServerPlayer p, Strike k) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(lv);
        if (b != null) { b.moveTo(k.pos); b.setVisualOnly(true); lv.addFreshEntity(b); }
        for (int i = 0; i < 20; i++) // tia sang doc
            lv.sendParticles(ParticleTypes.END_ROD, k.pos.x, k.pos.y + i * 0.8, k.pos.z, 3, 0.15, 0.1, 0.15, 0.02);
        lv.sendParticles(com.hukhong.vfx.ModParticles.THANH_QUANG.get(), k.pos.x, k.pos.y + 0.5, k.pos.z, 20, 0.8, 0.4, 0.8, 0.1);
        PacketDistributor.sendToPlayer(p, new BreakthroughPayload(8, 0.35f)); // rung/flash: ClientShake ton trong screenEffectsOff (R26)
        double dx = p.getX() - k.pos.x, dz = p.getZ() - k.pos.z;
        if (dx * dx + dz * dz <= 2.5 * 2.5) p.hurt(p.damageSources().lightningBolt(), k.dmg);
    }

    // ---------------------------------------------------------------- ket thuc

    private static void discardClone(MinecraftServer srv, Session s) {
        if (s.clone == null) return;
        for (ServerLevel l : srv.getAllLevels()) {
            var c = l.getEntity(s.clone);
            if (c != null) c.discard();
        }
    }

    private static void finish(ServerPlayer p, Session s) {
        discardClone(p.server, s);
        String key = s.type == Type.TAM_MA ? "tam_ma" : "loi_kiep";
        p.sendSystemMessage(Component.translatable("hukhong.trial.pass." + key));
        if (s.type == Type.TAM_MA && s.chain) {
            s.type = Type.LOI_KIEP; s.ticks = 0; s.clone = null; s.gap = 40;
            return;
        }
        SESSIONS.remove(s.player);
        FAILS.remove(s.player);
        if (s.chain) CultivationService.forceUnlockMilestone(p);
    }

    private static void fail(ServerPlayer p, Session s) {
        discardClone(p.server, s);
        SESSIONS.remove(s.player);
        p.sendSystemMessage(Component.translatable("hukhong.trial.fail"));
        if (!s.chain) return;
        int n = FAILS.merge(s.player, 1, Integer::sum);
        if (s.type == Type.TAM_MA) {
            boolean ma = CultivationService.get(p).paths().contains(Path.MA);
            double ac = MaRules.data(p).acTinh;
            if (p.getRandom().nextDouble() < TrialMath.tauHoaChance(ma, ac)) {
                FAILS.remove(s.player);
                MaRules.tauHoa(p);
                return;
            }
        }
        RETRY_AT.put(s.player, p.serverLevel().getGameTime() + TrialMath.penaltyTicks(n));
        p.sendSystemMessage(Component.translatable("hukhong.trial.extended", TrialMath.penaltyTicks(n) / 20));
    }
}
