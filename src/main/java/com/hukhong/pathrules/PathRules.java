package com.hukhong.pathrules;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.gates.Resources;
import com.hukhong.skill.SkillAttachments;
import com.hukhong.skill.SkillEffects;
import com.hukhong.skill.SkillState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Luật cứng theo đạo (Đạo Tu, Thể Tu) + hồi tài nguyên + thiền. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PathRules {
    /** Hồi tài nguyên mỗi giây (x resource_regen). Bước 27: BalanceSimTest dùng các hằng này. */
    public static final int DAO_REGEN = 5, DAO_MEDITATE_REGEN = 14, KIEM_REGEN_COMBAT = 9, KIEM_REGEN_IDLE = 8;
    public static final int THE_HIT_SMALL = 1, THE_HIT_BIG = 3, THE_MELEE_GAIN = 2, MA_HIT_GAIN = 1;

    private PathRules() {}

    private static boolean has(ServerPlayer p, Path path) {
        return p.getData(ModAttachments.CULTIVATION).paths().contains(path);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        var src = e.getSource();
        if (src.getEntity() instanceof ServerPlayer atk && atk != target) {
            CultivationData d = atk.getData(ModAttachments.CULTIVATION);
            if (!d.paths().isEmpty()) {
                float before = e.getAmount();
                float after = DamageRules.resolve(atk, target, src, before);
                if (after != before) e.setAmount(after);
                SkillState st = atk.getData(SkillAttachments.STATE);
                st.lastCombat = atk.level().getGameTime();
                if (after <= 0f && before > 0f && has(atk, Path.DAO) && !(target instanceof Player)) {
                    atk.displayClientMessage(Component.translatable("hukhong.msg.void"), true);
                }
                if (after > 0f && has(atk, Path.MA)) MaRules.onHit(atk);
                if (after > 0f && !DamageRules.isSkill() && src.getDirectEntity() == atk && has(atk, Path.THE)) {
                    Resources.add(atk, THE_MELEE_GAIN);
                }
            }
        }
        if (target instanceof ServerPlayer tp0 && e.getAmount() > 0f
                && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            double def = com.hukhong.stats.StatService.value(tp0, com.hukhong.stats.ModAttributes.DEFENSE);
            if (def > 0) e.setAmount(com.hukhong.stats.StatService.mitigate(e.getAmount(), def));
        }
        if (target instanceof ServerPlayer tp && has(tp, Path.THE) && e.getAmount() > 0f) {
            tp.getData(SkillAttachments.STATE).lastCombat = tp.level().getGameTime();
            Resources.add(tp, e.getAmount() < tp.getMaxHealth() * 0.03f ? THE_HIT_SMALL : THE_HIT_BIG);
        }
    }

    /** Thể Tu không dùng cung/nỏ. */
    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof ServerPlayer p && has(p, Path.THE)
                && e.getItem().getItem() instanceof ProjectileWeaponItem) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lv)) return;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        if (d.paths().isEmpty()) return;
        SkillState st = p.getData(SkillAttachments.STATE);
        long now = lv.getGameTime();

        // Thiền: sneak + đứng yên >= 3s
        Vec3 pos = p.position();
        if (p.isShiftKeyDown() && pos.distanceToSqr(st.lx, st.ly, st.lz) < 0.0004) st.still++;
        else st.still = 0;
        st.lx = pos.x; st.ly = pos.y; st.lz = pos.z;
        boolean med = st.meditating();

        boolean dao = d.paths().contains(Path.DAO), the = d.paths().contains(Path.THE);

        if (dao && p.getFoodData().getFoodLevel() < 20) p.getFoodData().setFoodLevel(20); // không đói

        if (now % 20 == 0) {
            double rg = com.hukhong.stats.StatService.regen(p);
            if (dao) Resources.add(p, (int) Math.round((med ? DAO_MEDITATE_REGEN : DAO_REGEN) * rg));
            if (d.paths().contains(Path.KIEM)) Resources.add(p, (int) Math.round((now - st.lastCombat > 60 ? KIEM_REGEN_IDLE : KIEM_REGEN_COMBAT) * rg));
            if (the) {
                if (med) {
                    if (d.resource() < 60) Resources.add(p, Math.min((int) Math.round(5 * rg), 60 - d.resource()));
                } else if (now - st.lastCombat > 100) {
                    Resources.add(p, -4);
                }
            }
        }
        if (med && now % 4 == 0) {
            // tụ khí: ENCHANT bay từ vòng ngoài vào người
            double a = (now * 0.35) % (2 * Math.PI);
            for (int i = 0; i < 3; i++) {
                double ang = a + i * 2.094;
                lv.sendParticles(com.hukhong.vfx.ModParticles.LINH_KHI.get(), p.getX(), p.getY() + 1.0, p.getZ(), 0,
                        Math.cos(ang) * 1.6, 0.8 + 0.3 * i, Math.sin(ang) * 1.6, 1.0);
            }
        }
        if (med && now % 100 == 0) com.hukhong.vfx.ModSounds.play(lv, p, com.hukhong.vfx.ModSounds.MEDITATE, 0.5f, 1.0f);
        if (the && now % 10 == 0) {
            double ratio = (double) d.resource() / d.resourceMax();
            if (d.resource() >= 25) { // vòng hào quang Bá Khí
                SkillEffects.ring(lv, p.position().add(0, 0.1, 0), 0.9, 10,
                        com.hukhong.vfx.ModParticles.BA_KHI.get());
            }
        }
    }
}
