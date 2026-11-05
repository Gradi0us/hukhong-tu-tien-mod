package com.hukhong.phapkhi;

import com.hukhong.HuKhong;
import com.hukhong.guild.Bosses;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Random;

/** Nguồn rơi Pháp Khí: boss (theo cấp boss), rương Bí Cảnh (theo cấp Bí Cảnh), quà phụ Hiệp Hội (Phàm/Linh). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhapKhiDrops {
    private PhapKhiDrops() {}

    public static final double BOSS_CHANCE = 0.30;
    public static final double GUILD_BONUS_CHANCE = 0.08;

    /** Xác suất rương Bí Cảnh có Pháp Khí: Bạch 10% ... Hồng 30%. */
    public static double biCanhChance(int tierOrdinal) { return 0.10 + 0.05 * Math.max(0, tierOrdinal); }

    /** Phẩm tối đa theo cấp: cấp 1-19 Phàm ... 80+ Thiên. */
    static int baseTier(int level) { return Math.max(0, Math.min(4, level / 20)); }

    /** Phẩm rơi quanh mốc base: 15% lên một bậc (trần Thiên), 35% xuống một bậc. */
    public static PhapKhiTier rollTier(Random r, int base) {
        double u = r.nextDouble();
        int t = base;
        if (u < 0.15) t = Math.min(4, base + 1); else if (u > 0.65) t = Math.max(0, base - 1);
        return PhapKhiTier.of(t);
    }

    public static ItemStack roll(Random r, int baseTier) {
        PhapKhiKind[] ks = PhapKhiKind.values();
        return PhapKhiRegistry.stack(ks[r.nextInt(ks.length)], rollTier(r, baseTier));
    }

    /** Bí Cảnh: tierOrdinal = BiCanhTier.ordinal() (0 Bạch .. 4 Hồng); trả ItemStack.EMPTY nếu không rơi. */
    public static ItemStack biCanhDrop(int tierOrdinal, Random r) {
        if (r.nextDouble() >= biCanhChance(tierOrdinal)) return ItemStack.EMPTY;
        return roll(r, tierOrdinal);
    }

    /** Quà phụ nhiệm vụ Hiệp Hội: hiếm, chỉ Phàm / Linh. */
    public static ItemStack guildBonus(Random r) {
        if (r.nextDouble() >= GUILD_BONUS_CHANCE) return ItemStack.EMPTY;
        PhapKhiKind[] ks = PhapKhiKind.values();
        return PhapKhiRegistry.stack(ks[r.nextInt(ks.length)], r.nextDouble() < 0.8 ? PhapKhiTier.PHAM : PhapKhiTier.LINH);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        var v = e.getEntity();
        if (v.level().isClientSide || !Bosses.isBoss(v)) return;
        Random r = new Random(v.getRandom().nextLong());
        if (r.nextDouble() >= BOSS_CHANCE) return;
        ItemStack s = roll(r, baseTier(Bosses.levelOf(v)));
        e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), s));
    }
}
