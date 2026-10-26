package com.hukhong.stats;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.cores.CoreService;
import com.hukhong.effect.ModEffects;
import com.hukhong.network.SyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Hệ chỉ số (S2 §1): base theo cấp x đạo x tư chất -> attribute hukhong:*; cấp 100 = bộ chỉ số Thần. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class StatService {
    private StatService() {}

    /** Hồi tài nguyên: resource_regen = 1 + REGEN_PER_LEVEL x (cấp-1) (Bước 27: cũ 1.0 cố định). */
    public static final double REGEN_PER_LEVEL = 0.03;
    public static final int THAN_LEVEL = 100;
    public static final double THAN_BONUS = 0.5;
    public static final long BAT_DIET_COOLDOWN = 12000; // 10 phút
    public static final float BAT_DIET_HEAL = 0.30f;
    public static final double DEFENSE_CAP = 0.80;      // S2 T3 (solo)
    private static final String BAT_DIET_KEY = "hukhong_bat_diet";

    /** Chỉ số tính thuần (chưa gồm Lõi/Thần). crit/lifesteal là phân số. */
    public record Stats(double attack, double spell, double defense, double crit, double critDamage,
                        double lifesteal, double regen, double maxResource) {}

    // {atkBase, atkGrow, spBase, spGrow, defBase, defGrow, critBase, critGrow, lsBase, lsGrow, resBase, resGrow}
    private static double[] table(Path p) {
        return switch (p) {
            case KIEM -> new double[]{4, 1.6, 2, 0.6, 0, 0.35, 0.05, 0.003, 0, 0, 100, 5};
            case DAO -> new double[]{2, 0.8, 8, 2.0, 0, 0.30, 0.03, 0.0015, 0, 0, 120, 6};
            case THE -> new double[]{3.9, 1.55, 1, 0.2, 2, 0.85, 0.03, 0.0035, 0, 0, 100, 0};
            case MA -> new double[]{3.5, 1.2, 3, 1.0, 1, 0.50, 0.05, 0.003, 0.06, 0.0004, 100, 3};
        };
    }

    /** Hệ số tư chất cho chỉ số khởi đầu và tăng trưởng (S2 §3): D -10% ... SSS +20%. */
    public static double aptFactor(Aptitude a) {
        return switch (a) {
            case D -> 0.90; case C -> 0.95; case B -> 1.0; case A -> 1.05;
            case S -> 1.10; case SS -> 1.15; case SSS -> 1.20;
        };
    }

    public static Stats compute(int level, Set<Path> paths, Map<Path, Aptitude> apt) {
        double atk = 0, sp = 0, def = 0, crit = 0, ls = 0, res = 100;
        int g = Math.max(0, level - 1);
        boolean any = false;
        for (Path p : paths) {
            double[] t = table(p);
            double f = aptFactor(apt.getOrDefault(p, Aptitude.B));
            atk = Math.max(atk, (t[0] + t[1] * g) * f);
            sp = Math.max(sp, (t[2] + t[3] * g) * f);
            def = Math.max(def, (t[4] + t[5] * g) * f);
            crit = Math.max(crit, t[6] + t[7] * g);
            ls = Math.max(ls, (t[8] + t[9] * g) * f);
            double r = t[10] + t[11] * g;
            res = any ? Math.max(res, r) : r;
            any = true;
        }
        // Phân Tâm (S2 3.1): song x0.8 / x0.9, tam x0.7 / x0.85
        int n = paths.size();
        double off = n >= 3 ? 0.70 : n == 2 ? 0.80 : 1.0, tank = n >= 3 ? 0.85 : n == 2 ? 0.90 : 1.0;
        return new Stats(atk * off, sp * off, def * tank, Math.min(1, crit), ModAttributes.DEFAULT_CRIT_DAMAGE,
                ls * off, 1.0 + REGEN_PER_LEVEL * g, Math.round(res));
    }

    // ---- Sinh Lực + Tốc (S2 §1): {hpBase, hpGrow, speedBase(1.0), speedGrow(phân số/cấp)}
    private static double[] vitals(Path p) {
        return switch (p) {
            case KIEM -> new double[]{20, 4.5, 0.0010};
            case DAO -> new double[]{20, 3.5, 0.0005};
            case THE -> new double[]{24, 7.0, 0.0002};
            case MA -> new double[]{22, 5.0, 0.0007};
        };
    }

    public static final double VANILLA_HP = 20.0;
    public static final double MAX_HP_CAP = 1024.0;     // trần RangedAttribute vanilla max_health
    public static final double SPEED_CAP = 0.25;        // tối đa +25% movement_speed

    /** Sinh Lực tối đa mục tiêu (tư chất theo cả base lẫn tăng trưởng; Phân Tâm giảm theo hệ số Phòng). 20 nếu chưa có đạo. */
    public static double maxHealth(int level, Set<Path> paths, Map<Path, Aptitude> apt) {
        int g = Math.max(0, level - 1);
        double hp = VANILLA_HP;
        for (Path p : paths) {
            double[] t = vitals(p);
            hp = Math.max(hp, (t[0] + t[1] * g) * aptFactor(apt.getOrDefault(p, Aptitude.B)));
        }
        int n = paths.size();
        double tank = n >= 3 ? 0.85 : n == 2 ? 0.90 : 1.0;
        return Math.min(MAX_HP_CAP, Math.max(VANILLA_HP, hp * tank));
    }

    /** Tốc cộng thêm (phân số so vanilla, vd 0.10 = +10%), trần SPEED_CAP. */
    public static double speedBonus(int level, Set<Path> paths, Map<Path, Aptitude> apt) {
        int g = Math.max(0, level - 1);
        double b = 0;
        for (Path p : paths) b = Math.max(b, vitals(p)[2] * g * aptFactor(apt.getOrDefault(p, Aptitude.B)));
        return Math.min(SPEED_CAP, b);
    }

    // ------------------------------------------------------------------ truy cập

    public static double value(ServerPlayer p, String attr) { return p.getAttributeValue(ModAttributes.holder(attr)); }

    public static double attackPower(ServerPlayer p) { return value(p, ModAttributes.ATTACK_POWER); }

    public static double spellPower(ServerPlayer p) { return value(p, ModAttributes.SPELL_POWER); }

    /** Hệ số hồi tài nguyên = attribute resource_regen x hiệu ứng Hư Hóa (0.5). */
    public static double regen(ServerPlayer p) { return value(p, ModAttributes.RESOURCE_REGEN) * ModEffects.regenMultiplier(p); }

    public static boolean isThan(CultivationData d) { return d.level() >= THAN_LEVEL && !d.paths().isEmpty(); }

    /** Phòng giảm sát thương nhận: def/(def+100), trần 80%. */
    public static float mitigate(float amount, double defense) {
        if (defense <= 0 || amount <= 0) return amount;
        return (float) (amount * (1.0 - Math.min(DEFENSE_CAP, defense / (defense + 100.0))));
    }

    // ------------------------------------------------------------------ áp dụng

    /** Đặt base attribute theo cấp/đạo/tư chất, áp lại Lõi + bộ Thần, đồng bộ tài nguyên tối đa. */
    public static void refresh(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        Stats s = compute(d.level(), d.paths(), d.aptitude());
        if (d.paths().isEmpty()) s = new Stats(0, 0, 0, 0, ModAttributes.DEFAULT_CRIT_DAMAGE, 0, 1.0, d.resourceMax());
        set(p, ModAttributes.ATTACK_POWER, s.attack());
        set(p, ModAttributes.SPELL_POWER, s.spell());
        set(p, ModAttributes.DEFENSE, s.defense());
        set(p, ModAttributes.CRIT_CHANCE, s.crit());
        set(p, ModAttributes.CRIT_DAMAGE, s.critDamage());
        set(p, ModAttributes.LIFESTEAL, s.lifesteal());
        set(p, ModAttributes.RESOURCE_REGEN, s.regen());
        set(p, ModAttributes.MAX_RESOURCE, s.maxResource());
        applyVitals(p, d);
        CoreService.applyAll(p); // gỡ + áp lại bị động Lõi (transient)
        com.hukhong.honcot.HonCotService.apply(p); // Hồn Cốt
        com.hukhong.banmenh.BanMenhService.applyPassives(p); // Ngọc Tâm / Trọng Nhạc
        boolean than = isThan(d);
        var mid = ModAttributes.id("than");
        for (String name : ModAttributes.names()) {
            AttributeInstance inst = p.getAttribute(ModAttributes.holder(name));
            if (inst == null) continue;
            if (than) inst.addOrUpdateTransientModifier(
                    new AttributeModifier(mid, THAN_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else inst.removeModifier(mid);
        }
        syncResourceMax(p);
        SIG.put(p.getUUID(), signature(p));
    }

    /** max_health / movement_speed vanilla qua AttributeModifier transient (gọi lại sau login/respawn qua refresh). */
    private static void applyVitals(ServerPlayer p, CultivationData d) {
        var hp = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        var spd = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var hid = ModAttributes.id("stat/max_health");
        var sid = ModAttributes.id("stat/speed");
        double extra = d.paths().isEmpty() ? 0 : maxHealth(d.level(), d.paths(), d.aptitude()) - VANILLA_HP;
        double sb = d.paths().isEmpty() ? 0 : speedBonus(d.level(), d.paths(), d.aptitude());
        if (hp != null) {
            if (extra > 0) hp.addOrUpdateTransientModifier(new AttributeModifier(hid, extra, AttributeModifier.Operation.ADD_VALUE));
            else hp.removeModifier(hid);
        }
        if (spd != null) {
            if (sb > 0) spd.addOrUpdateTransientModifier(new AttributeModifier(sid, sb, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            else spd.removeModifier(sid);
        }
    }

    private static void set(ServerPlayer p, String name, double v) {
        AttributeInstance inst = p.getAttribute(ModAttributes.holder(name));
        if (inst != null && inst.getBaseValue() != v) inst.setBaseValue(v);
    }

    public static void syncResourceMax(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        int max = (int) Math.max(1, Math.round(value(p, ModAttributes.MAX_RESOURCE)));
        if (max == d.resourceMax()) return;
        p.setData(ModAttachments.CULTIVATION, d.withResourceMax(max));
        SyncPayload.send(p);
    }

    // ------------------------------------------------------------------ sự kiện

    private static final Map<UUID, Integer> SIG = new ConcurrentHashMap<>();

    private static int signature(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        var bm = p.getData(com.hukhong.banmenh.BanMenhAttachments.BAN_MENH.get());
        return Objects.hash(d.level(), d.paths(), d.aptitude(), CoreService.get(p).picked(), bm.type(), bm.stage());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) refresh(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) refresh(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { SIG.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        long now = p.level().getGameTime();
        if (now % 20 == 0) {
            Integer old = SIG.get(p.getUUID());
            if (old == null || old != signature(p)) refresh(p); // đổi cấp/đạo/Lõi/tư chất
        }
        if (now % 10 == 0 && isThan(p.getData(ModAttachments.CULTIVATION))) purgeHarmful(p);
    }

    private static void purgeHarmful(ServerPlayer p) {
        for (MobEffectInstance mi : new ArrayList<>(p.getActiveEffects())) {
            if (mi.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(mi.getEffect());
        }
    }

    /** "Bất Diệt": cấp 100, 1 lần / 10 phút, chết -> hồi 30% máu. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isThan(p.getData(ModAttachments.CULTIVATION))) return;
        long now = p.level().getGameTime();
        var tag = p.getPersistentData();
        if (tag.contains(BAT_DIET_KEY) && now - tag.getLong(BAT_DIET_KEY) < BAT_DIET_COOLDOWN) return;
        tag.putLong(BAT_DIET_KEY, now);
        e.setCanceled(true);
        p.setHealth(Math.max(1f, p.getMaxHealth() * BAT_DIET_HEAL));
        p.removeAllEffects();
        p.displayClientMessage(Component.translatable("hukhong.than.bat_diet"), true);
    }
}
