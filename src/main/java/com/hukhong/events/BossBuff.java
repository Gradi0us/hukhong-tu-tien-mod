package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.chg.ChapHanhGia;
import com.hukhong.endgame.DaKhuyet;
import com.hukhong.endgame.NguyenThuyTichDiet;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuVuong;
import com.hukhong.entity.WorldBoss;
import com.hukhong.story.SuDo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Buff chung cho moi boss: danh nhieu hon ~30% (hoi chieu / 1.3) va +30% sat thuong.
 * Boss Bi Canh la quai Hu Hoa thuong co tag phat sang (BiCanhManager.spawn boss) -> nhan dien qua do.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class BossBuff {
    private BossBuff() {}

    public static final double DMG_MUL = 1.3;
    /** Danh ~30% thuong xuyen hon: hoi chieu chia 1.3. */
    public static final double CD_DIV = 1.3;

    /** Hoi chieu (tick) sau buff, toi thieu 1. */
    public static int cd(int ticks) { return Math.max(1, (int) Math.round(ticks / CD_DIV)); }

    public static boolean isBoss(Entity e) {
        if (e instanceof HuVuong || e instanceof SuDo || e instanceof ChapHanhGia || e instanceof NguyenThuyTichDiet
                || e instanceof DaKhuyet || e instanceof WorldBoss) return true;
        // Boss Bi Canh / mini-boss tap kich: quai Hu co tag phat sang + ten hien
        return e instanceof Mob m && e instanceof HuMob && m.hasCustomName() && m.hasGlowingTag();
    }

    /** Hoi chieu cho entity: boss duoc rut ngan, quai thuong giu nguyen. */
    public static int cdFor(Entity e, int ticks) { return isBoss(e) ? cd(ticks) : ticks; }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide) return;
        Entity src = e.getSource().getEntity();
        if (src == null || !isBoss(src)) return;
        LivingEntity victim = e.getEntity();
        if (isBoss(victim) || victim instanceof HuMob) return;
        if (src.level().dimension() == com.hukhong.bicanh.BiCanhRegistry.DIM) return; // Bi Canh co he so boss rieng (BiCanhManager.BOSS_DMG_MUL), khong cong don
        e.setAmount((float) (e.getAmount() * DMG_MUL));
    }
}
