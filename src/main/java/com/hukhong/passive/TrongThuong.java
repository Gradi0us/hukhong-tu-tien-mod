package com.hukhong.passive;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.effect.ModEffects;
import com.hukhong.events.BossBuff;
import com.hukhong.gates.DamageRules;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

/**
 * Thể Tu nội tại "Trọng Thương": mỗi đòn thường (không phải kỹ năng) trúng kẻ địch cộng 1 tầng (tối đa 3, 6s, làm mới mỗi đòn).
 * Mục tiêu có tầng nhận thêm X% sát thương mỗi tầng từ MỌI nguồn (X theo tư chất Thể của người đánh); dưới Y% máu tối đa thì bị xử tử.
 * Boss không bị xử tử, và bonus sát thương bị chặn 1/3. Người chơi không bị ảnh hưởng (PvP).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TrongThuong {
    private TrongThuong() {}

    /** Lưu trên mục tiêu: X (% mỗi tầng) và Y (% xử tử) của người áp dụng gần nhất. */
    static final String K_AMP = "hukhong_tt_amp", K_EXEC = "hukhong_tt_exec";
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.05f), 1.0f);

    public static boolean isBoss(LivingEntity e) {
        return PassiveService.isBoss(e) || BossBuff.isBoss(e) || e instanceof com.hukhong.entity.TamMa;
    }

    public static int stacks(LivingEntity e) {
        MobEffectInstance i = e.getEffect(ModEffects.TRONG_THUONG);
        return i == null ? 0 : PassiveMath.ttClamp(i.getAmplifier() + 1);
    }

    /** Sát thương nhận thêm từ mọi nguồn khi có tầng. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || e.getAmount() >= 1.0e8f) return; // bỏ qua đòn kill
        int n = stacks(t);
        if (n <= 0) return;
        double amp = t.getPersistentData().getDouble(K_AMP);
        if (amp > 0) e.setAmount((float) (e.getAmount() * PassiveMath.ttDamageMult(amp, n, isBoss(t))));
    }

    /** Sau khi đòn thường trúng: cộng tầng rồi xét xử tử. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer atk) || e.getNewDamage() <= 0f) return;
        LivingEntity t = e.getEntity();
        if (t == atk || t instanceof Player || t.level().isClientSide) return;
        if (DamageRules.isSkill() || e.getSource().getDirectEntity() != atk) return;
        CultivationData d = atk.getData(ModAttachments.CULTIVATION);
        if (!d.paths().contains(Path.THE)) return;
        Aptitude a = d.aptitude().getOrDefault(Path.THE, Aptitude.D);
        huyetKhi(atk, a, e.getNewDamage());
        apply(atk, t, PassiveMath.ttAmpPerStack(a), PassiveMath.ttExecPct(a));
    }

    /** Huyết Khí: đủ 3 đòn thường trúng -> hồi % tổng sát thương 3 đòn đó; hồi chiêu theo tư chất. */
    private static final java.util.Map<java.util.UUID, double[]> HK = new java.util.concurrent.ConcurrentHashMap<>(); // {hits, dmg, readyTick}

    static void huyetKhi(ServerPlayer p, Aptitude a, float dmg) {
        long now = p.serverLevel().getGameTime();
        double[] st = HK.computeIfAbsent(p.getUUID(), k -> new double[3]);
        if (now < st[2]) return;
        st[0]++; st[1] += dmg;
        if (st[0] < PassiveMath.HK_HITS) return;
        float heal = (float) (st[1] * PassiveMath.hkHealPct(a));
        st[0] = 0; st[1] = 0; st[2] = now + PassiveMath.hkCooldown(a);
        if (heal <= 0) return;
        p.heal(heal);
        Vec3 c = p.position().add(0, p.getBbHeight() * 0.6, 0);
        p.serverLevel().sendParticles(BLOOD, c.x, c.y, c.z, 14, 0.35, 0.5, 0.35, 0.02);
        p.serverLevel().sendParticles(ParticleTypes.HEART, c.x, c.y + 0.5, c.z, 2, 0.3, 0.2, 0.3, 0.0);
        p.serverLevel().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    /** Công khai để GameTest gọi: cộng 1 tầng + kiểm tra xử tử (đã gồm hiệu ứng). */
    public static void apply(ServerPlayer atk, LivingEntity t, double ampPerStack, double execPct) {
        if (!t.isAlive()) return;
        int n = Math.min(PassiveMath.TT_MAX_STACKS, stacks(t) + 1);
        t.getPersistentData().putDouble(K_AMP, ampPerStack);
        t.getPersistentData().putDouble(K_EXEC, execPct);
        t.addEffect(new MobEffectInstance(ModEffects.TRONG_THUONG, PassiveMath.TT_TICKS, n - 1, false, true, true));
        if (atk.level() instanceof ServerLevel lv) markFx(lv, atk, t, n);
        boolean boss = isBoss(t);
        if (PassiveMath.ttExecutes(n, t.getHealth(), t.getMaxHealth(), execPct, boss, false)) execute(atk, t);
    }

    private static void markFx(ServerLevel lv, ServerPlayer atk, LivingEntity t, int n) {
        Vec3 c = t.position().add(0, t.getBbHeight() * 0.55, 0);
        Fx.groundCrack(lv, atk, t.position().add(0, 0.05, 0), 0.5f + 0.15f * n, 0x8B0000, 2 + n, 24);
        lv.sendParticles(BLOOD, c.x, c.y, c.z, 6 + 4 * n, t.getBbWidth() * 0.4, t.getBbHeight() * 0.3, t.getBbWidth() * 0.4, 0.0);
    }

    private static void execute(ServerPlayer atk, LivingEntity t) {
        if (atk.level() instanceof ServerLevel lv) {
            Vec3 c = t.position().add(0, t.getBbHeight() * 0.5, 0);
            Fx.slash(lv, atk, 60, 3.2, 0, 7, 2.4f);
            lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 24, 0.4, 0.5, 0.4, 0.4);
            lv.sendParticles(BLOOD, c.x, c.y, c.z, 30, 0.5, 0.6, 0.5, 0.0);
            lv.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.6f);
            lv.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 1.0f, 0.5f);
            lv.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.5f, 0.7f);
        }
        t.removeEffect(ModEffects.TRONG_THUONG);
        t.hurt(t.damageSources().genericKill(), Float.MAX_VALUE);
    }
}
