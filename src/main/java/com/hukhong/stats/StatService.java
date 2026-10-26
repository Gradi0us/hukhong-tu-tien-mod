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
            case DAO -> new double[]{2, 0.8, 6, 2.0, 0, 0.30, 0.03, 0.0015, 0, 0, 120, 6};
            case THE -> new double[]{3, 1.3, 1, 0.2, 2, 0.85, 0.03, 0.001, 0, 0, 100, 0};
            case MA -> new double[]{3.5, 1.2, 3, 1.0, 1, 0.50, 0.05, 0.002, 0.06, 0.0004, 100, 3};
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
                ls * off, 1.0, Math.round(res));
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
        CoreService.applyAll(p); // gỡ + áp lại bị động Lõi (transient)
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

    private static void set(ServerPlayer p, String name, double v) {
        AttributeInstance inst = p.getAttribute(ModAttributes.holder(name));
        if (inst != null && inst.getBaseValue() != v) inst.setBaseValue(v);
    }

    private static void syncResourceMax(ServerPlayer p) {
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
        return Objects.hash(d.level(), d.paths(), d.aptitude(), CoreService.get(p).picked());
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
