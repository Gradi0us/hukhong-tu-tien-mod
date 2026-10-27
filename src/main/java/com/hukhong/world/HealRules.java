package com.hukhong.world;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import com.hukhong.skill.SkillAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Hoi phuc: (1) tat hoi mau tu nhien theo do doi (gamerule naturalRegeneration); (2) ngoai chien dau (5s khong gay/nhan sat thuong)
 * hoi 3% HP toi da moi giay; (3) giet quai: Kiem/The/Dao hoi 5% HP, 8% tai nguyen, +2 do an.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HealRules {
    private HealRules() {}

    /** Ngoai chien dau sau 5 giay (100 tick). */
    public static final long OUT_OF_COMBAT_TICKS = 100;
    /** Hoi ngoai chien: 3% HP toi da moi giay, tinh theo tick (1/20 giay). */
    public static final float OUT_OF_COMBAT_HEAL_PER_SEC = 0.03f;
    public static final float KILL_HEAL_PCT = 0.05f, KILL_RESOURCE_PCT = 0.08f;
    public static final int KILL_FOOD = 2;

    public static boolean outOfCombat(long now, long lastCombat) { return now - lastCombat > OUT_OF_COMBAT_TICKS; }

    /** Luong hoi moi tick. */
    public static float healPerTick(float maxHp) { return maxHp * OUT_OF_COMBAT_HEAL_PER_SEC / 20f; }

    /** Tai nguyen hoi khi giet quai (lam tron len, toi thieu 1). */
    public static int killResource(int resourceMax) { return Math.max(1, (int) Math.ceil(resourceMax * KILL_RESOURCE_PCT)); }

    /** Dat gamerule naturalRegeneration = false (hoi mau theo do doi vanilla). */
    static void disableNaturalRegen(MinecraftServer s) {
        GameRules.BooleanValue r = s.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
        if (r.get()) r.set(false, s);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) { disableNaturalRegen(e.getServer()); }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 200 == 0) disableNaturalRegen(e.getServer()); // chong /gamerule bat lai
    }

    /** Dau/nhan sat thuong = dang chien dau (cap nhat thoi diem chien dau dung chung voi SkillState). */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || e.getNewDamage() <= 0f) return;
        long now = v.level().getGameTime();
        if (v instanceof ServerPlayer tp) tp.getData(SkillAttachments.STATE).lastCombat = now;
        if (e.getSource().getEntity() instanceof ServerPlayer ap && ap != v) ap.getData(SkillAttachments.STATE).lastCombat = now;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        if (!outOfCombat(p.level().getGameTime(), p.getData(SkillAttachments.STATE).lastCombat)) return;
        if (p.isDeadOrDying() || p.getHealth() >= p.getMaxHealth()) return;
        p.heal(healPerTick(p.getMaxHealth()));
    }

    /** Giet quai (khong phai nguoi choi): Kiem / The / Dao hoi HP, tai nguyen va do an. Ma tu khong nhan. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v instanceof Player || v.level().isClientSide) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        if (!(d.paths().contains(Path.KIEM) || d.paths().contains(Path.THE) || d.paths().contains(Path.DAO))) return;
        p.heal(p.getMaxHealth() * KILL_HEAL_PCT);
        Resources.add(p, killResource(d.resourceMax()));
        p.getFoodData().eat(KILL_FOOD, 0f);
    }
}
