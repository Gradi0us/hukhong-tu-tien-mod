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
import com.hukhong.entity.ModEntities;
import com.hukhong.entity.TamMa;
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
        final UUID player; Type type; final boolean chain; Vec3 center; final int level;
        boolean inside; int fade; Vec3 home; net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> homeDim; float homeYaw;
        final List<UUID> ghosts = new ArrayList<>(); int cd, tele, cycle, shownAt; List<Vec3> pend;
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
        if (t == Type.TAM_MA) {
            s.fade = 60;
            PacketDistributor.sendToPlayer(p, new com.hukhong.network.TamThucPayload(0, 60));
        } else s.gap = 40;
    }

    // ---------------------------------------------------------------- Tam Ma

    private static void spawnClone(ServerPlayer p, Session s) {
        ServerLevel lv = s.inside ? p.server.getLevel(TAM_THUC) : p.serverLevel();
        TamMa h = ModEntities.TAM_MA.get().create(lv);
        if (h != null) h.setOwnerId(p.getUUID());
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
        if (s.inside) { vanish(s, h); s.cd = 50; }
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
            if (s != null) {
                if (s.inside) { e.setCanceled(true); p.setHealth(1f); } // chet trong Tam Thuc = that bai, khong chet that
                fail(p, s);
            }
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
            if (s.fade > 0) { if (--s.fade == 0) enter(p, s); continue; }
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
        if (s.inside && s.ticks % 120 == 0)
            p.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.9f, 0.6f + p.getRandom().nextFloat() * 0.3f);
        if (s.ticks % 5 == 0 && c != null && !c.isInvisible())
            lv.sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(), c.getX(), c.getY() + 1, c.getZ(), 4, 0.3, 0.7, 0.3, 0.02);
        if (c instanceof TamMa h && s.inside) tickBoss(p, s, lv, h);
        else if (c instanceof TamMa h && h.getTarget() == null) h.setTarget(p);
        if (c == null || !c.isAlive()) { finish(p, s); return; }
        if (s.ticks > TAM_MA_TIMEOUT) fail(p, s);
    }

    // ---------------------------------------------------------------- Loi Kiep

    private static void tickLoi(ServerPlayer p, Session s) {
        ServerLevel lv = p.serverLevel();
        for (Iterator<Strike> it = s.strikes.iterator(); it.hasNext(); ) {
            Strike k = it.next();
            if (--k.left > 0) {
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
            com.hukhong.vfx.Fx.play(lv, com.hukhong.vfx.FxType.TELEGRAPH, pos, Vec3.ZERO, 2.5f, 0xA050FF, tele + 2);
        }
        if (s.wave == 0 || s.wave % 2 == 0)
            com.hukhong.vfx.Fx.play(lv, com.hukhong.vfx.FxType.STORM_CLOUD, p.position(), Vec3.ZERO, 16f, 0x1C1C2C, tele + 60);
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
        com.hukhong.vfx.Fx.play(lv, com.hukhong.vfx.FxType.LIGHTNING_BOLT, k.pos, Vec3.ZERO, 1.2f, 0xB8D8FF);
        com.hukhong.vfx.Fx.play(lv, com.hukhong.vfx.FxType.GROUND_CRACK, k.pos, Vec3.ZERO, 3.2f, 0xB8D8FF, 30);
        lv.sendParticles(com.hukhong.vfx.ModParticles.THANH_QUANG.get(), k.pos.x, k.pos.y + 0.5, k.pos.z, 20, 0.8, 0.4, 0.8, 0.1);
        PacketDistributor.sendToPlayer(p, new BreakthroughPayload(8, 0.35f)); // rung/flash: ClientShake ton trong screenEffectsOff (R26)
        double dx = p.getX() - k.pos.x, dz = p.getZ() - k.pos.z;
        if (dx * dx + dz * dz <= 2.5 * 2.5) p.hurt(p.damageSources().lightningBolt(), k.dmg);
    }

    // ---------------------------------------------------------------- The Gioi Tam Thuc

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> TAM_THUC =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tam_thuc"));
    static final String RET_TAG = "hukhong_tamthuc_ret";
    static final int ARENA_FLOOR = 64, ARENA_FLOOR_R = 12;

    /** Het man den: dua nguoi choi sang chieu Tam Thuc, dung san dau, sinh Tam Ma. */
    private static void enter(ServerPlayer p, Session s) {
        ServerLevel tl = p.server.getLevel(TAM_THUC);
        if (tl == null || p.serverLevel() == tl) { // khong co chieu -> che do cu (ban sao ngay tai cho)
            PacketDistributor.sendToPlayer(p, new com.hukhong.network.TamThucPayload(1, 30));
            spawnClone(p, s);
            return;
        }
        buildArena(tl);
        s.home = p.position(); s.homeDim = p.level().dimension(); s.homeYaw = p.getYRot();
        var t = new net.minecraft.nbt.CompoundTag();
        t.putString("dim", s.homeDim.location().toString());
        t.putDouble("x", s.home.x); t.putDouble("y", s.home.y); t.putDouble("z", s.home.z); t.putFloat("yaw", s.homeYaw);
        p.getPersistentData().put(RET_TAG, t);
        s.center = new Vec3(0.5, ARENA_FLOOR + 1, 0.5);
        p.teleportTo(tl, s.center.x, s.center.y, s.center.z, 0f, 0f);
        s.inside = true;
        PacketDistributor.sendToPlayer(p, new com.hukhong.network.TamThucPayload(1, 40));
        com.hukhong.audio.BossMusic.start(p, "tam_ma");
        spawnClone(p, s);
    }

    /** Roi Tam Thuc ve cho cu (thang/thua/huy). */
    private static void leave(ServerPlayer p, Session s) {
        if (!s.inside) return;
        s.inside = false;
        com.hukhong.audio.BossMusic.stop(p);
        ServerLevel hl = p.server.getLevel(s.homeDim);
        if (hl == null) hl = p.server.overworld();
        p.getPersistentData().remove(RET_TAG);
        p.teleportTo(hl, s.home.x, s.home.y, s.home.z, s.homeYaw, 0f);
        s.center = s.home;
        PacketDistributor.sendToPlayer(p, new com.hukhong.network.TamThucPayload(1, 40));
    }

    /** Dang nhap lai khi dang o Tam Thuc (mat ket noi / server tat giua chung): tra ve vi tri cu. */
    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var data = p.getPersistentData();
        if (!data.contains(RET_TAG)) return;
        var t = data.getCompound(RET_TAG);
        data.remove(RET_TAG);
        Session old = SESSIONS.remove(p.getUUID());
        if (old != null) discardClone(p.server, old);
        ServerLevel hl = null;
        var rl = net.minecraft.resources.ResourceLocation.tryParse(t.getString("dim"));
        if (rl != null) hl = p.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, rl));
        if (hl == null) hl = p.server.overworld();
        p.teleportTo(hl, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), 0f);
        if (p.getHealth() < 1f) p.setHealth(1f);
        p.sendSystemMessage(Component.translatable("hukhong.trial.tam_thuc.returned"));
    }

    private static void buildArena(ServerLevel tl) {
        tl.getChunk(0, 0);
        var floor = net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES.defaultBlockState();
        var crack = net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN.defaultBlockState();
        var wall = net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = -13; x <= 13; x++) for (int z = -13; z <= 13; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d <= ARENA_FLOOR_R + 0.5) {
                boolean c = Math.floorMod(x * 31 + z * 17 + x * z, 11) == 0; // vet nut phat sang thua
                tl.setBlock(pos.set(x, ARENA_FLOOR, z), c ? crack : floor, 2);
            } else if (d <= 13.5) {
                for (int y = ARENA_FLOOR; y <= ARENA_FLOOR + 6; y++) tl.setBlock(pos.set(x, y, z), wall, 2);
            }
        }
    }

    // --- AI Tam Ma: bien mat vao bong toi, hien ra sau/ben canh, kem bong phu ngan ngui; bao truoc bang tieng tham (~1.2s).

    private static void vanish(Session s, TamMa h) {
        h.setInvisible(true); h.setNoAi(true); h.setInvulnerable(true);
        h.setTarget(null);
        h.teleportTo(s.center.x, s.center.y + 30, s.center.z);
    }

    private static Vec3 around(ServerPlayer p, Session s, double deg, double dist) {
        double a = Math.toRadians(p.getYRot() + deg);
        double x = p.getX() - Math.sin(a) * dist, z = p.getZ() + Math.cos(a) * dist;
        double dx = x - s.center.x, dz = z - s.center.z, r = Math.sqrt(dx * dx + dz * dz), max = ARENA_FLOOR_R - 1.5;
        if (r > max) { x = s.center.x + dx / r * max; z = s.center.z + dz / r * max; }
        return new Vec3(x, s.center.y, z);
    }

    private static void tickBoss(ServerPlayer p, Session s, ServerLevel lv, TamMa h) {
        if (s.pend != null) {
            if (--s.tele > 0) return;
            List<Vec3> ps = s.pend; s.pend = null;
            Vec3 b = ps.get(0);
            h.teleportTo(b.x, b.y, b.z);
            h.setInvisible(false); h.setNoAi(false); h.setInvulnerable(false); h.setTarget(p);
            s.shownAt = s.ticks;
            lv.sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(), b.x, b.y + 1, b.z, 12, 0.3, 0.7, 0.3, 0.03);
            double base = h.getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (int i = 1; i < ps.size(); i++) {
                TamMa g = ModEntities.TAM_MA.get().create(lv);
                if (g == null) continue;
                Vec3 q = ps.get(i);
                g.setPos(q.x, q.y, q.z);
                g.setOwnerId(p.getUUID());
                g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4);
                g.setHealth(4f);
                g.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(1.0, base * 0.5));
                g.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
                g.ghostLife = 40;
                g.addTag(CLONE_TAG);
                g.setPersistenceRequired();
                g.setTarget(p);
                lv.addFreshEntity(g);
                s.ghosts.add(g.getUUID());
                lv.sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(), q.x, q.y + 1, q.z, 8, 0.3, 0.7, 0.3, 0.03);
            }
            s.cd = 90;
            return;
        }
        if (!h.isInvisible() && s.ticks - s.shownAt > 70) { vanish(s, h); s.cd = 30; return; }
        if (h.isInvisible() && --s.cd <= 0) {
            List<Vec3> ps = new ArrayList<>();
            double[] opts = {180, 150, -150, 90, -90};
            double ang = opts[p.getRandom().nextInt(opts.length)] + (p.getRandom().nextDouble() - 0.5) * 30;
            ps.add(around(p, s, ang, 2.5));
            if (s.cycle++ % 2 == 1) {
                int n = 2 + p.getRandom().nextInt(2);
                double a0 = ang + 70 + p.getRandom().nextDouble() * 40;
                for (int i = 0; i < n; i++) ps.add(around(p, s, a0 + i * (360.0 / (n + 1)), 3.5));
            }
            s.pend = ps; s.tele = 24;
            for (Vec3 q : ps) {
                lv.playSound(null, q.x, q.y, q.z, SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.HOSTILE, 0.8f, 1.5f);
                lv.sendParticles(ParticleTypes.SOUL, q.x, q.y + 1, q.z, 6, 0.3, 0.6, 0.3, 0.01);
            }
        }
    }

    // ---------------------------------------------------------------- ket thuc

    private static void discardClone(MinecraftServer srv, Session s) {
        for (ServerLevel l : srv.getAllLevels()) {
            if (s.clone != null) { var c = l.getEntity(s.clone); if (c != null) c.discard(); }
            for (UUID g : s.ghosts) { var c = l.getEntity(g); if (c != null) c.discard(); }
        }
        s.ghosts.clear();
    }

    private static void finish(ServerPlayer p, Session s) {
        discardClone(p.server, s);
        leave(p, s);
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
        leave(p, s);
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
