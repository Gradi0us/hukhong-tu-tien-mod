package com.hukhong.entity;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientCache;
import com.hukhong.core.CultivationService;
import com.hukhong.gates.RewardGate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Logic chung cho quai Hu Hoa: cap theo khoang cach spawn, ten mau, thuong. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HuHoa {
    private HuHoa() {}

    public static final String TAG_LEVEL = "HuLevel";
    /** Vong 1: 0..R1 block -> cap 1-15; vong 2: R1..R2 -> cap 15-30. */
    static final double R1 = 1500, R2 = 3500, R3 = 12000;
    /** Vòng 3 (Bước 27): R2..R3 -> cấp 30-100 để người chơi cấp cao còn quái cùng cấp (trước đây trần 30). */
    public static final int MAX_MOB_LEVEL = 100;

    /**
     * Cap quai = vong khoang cach (1..100) + thuong theo tuoi the gioi, nhieu +-1.
     * Tuoi the gioi: +1 cap moi {@link #DAYS_PER_LEVEL} ngay game ke tu luc tao the gioi (gameTime/24000), toi da +{@link #AGE_BONUS_CAP}.
     */
    public static int levelFor(ServerLevelAccessor lvl, BlockPos pos, net.minecraft.util.RandomSource r) {
        BlockPos sp = lvl.getLevel().getSharedSpawnPos();
        double dx = pos.getX() - sp.getX(), dz = pos.getZ() - sp.getZ();
        return levelFor(Math.sqrt(dx * dx + dz * dz), ageBonus(lvl.getLevel().getGameTime() / 24000L), r.nextInt(3) - 1);
    }

    public static final int DAYS_PER_LEVEL = 3, AGE_BONUS_CAP = 30;

    /** Thuong cap theo so ngay game the gioi da song: floor(days / DAYS_PER_LEVEL), kep [0, AGE_BONUS_CAP]. */
    public static int ageBonus(long days) {
        return (int) Math.max(0, Math.min(AGE_BONUS_CAP, days / DAYS_PER_LEVEL));
    }

    /** Thuan: d = khoang cach toi spawn, jitter in -1..1. */
    public static int levelFor(double d, int ageBonus, int jitter) {
        int base = d < R1 ? 1 + (int) (14 * d / R1)
                : d < R2 ? 15 + (int) (15 * (d - R1) / (R2 - R1))
                : 30 + (int) Math.min(MAX_MOB_LEVEL - 30, (MAX_MOB_LEVEL - 30) * (d - R2) / (R3 - R2));
        return Math.max(1, Math.min(MAX_MOB_LEVEL, base + jitter + ageBonus));
    }

    /** He so cap: hp x(1+0.38(L-1)), sat thuong x(1+0.10(L-1)), giap +0.25/cap (toi da 20), toc do +0.5%/cap (toi da +30%). */
    public static double hpScale(int level) {
        int g = Math.max(0, level - 1);
        double s = HP_BASE + HP_LIN * g + HP_QUAD * g * g + HP_CUBE * g * g * g;
        return level >= com.hukhong.stats.StatService.THAN_LEVEL ? s * THAN_HP_MUL : s;
    }
    /** Bước 27 (R5 mở rộng): đường cong HP quái theo cấp, khớp sát thương người chơi (BalanceSimTest). THAN_HP_MUL: người cấp 100 có bộ chỉ số Thần. */
    public static final double HP_BASE = 2.48, HP_LIN = 0.713, HP_QUAD = -0.003817, HP_CUBE = 0.0000929, THAN_HP_MUL = 2.0;
    public static double dmgScale(int level) {
        int g = Math.max(0, level - 1);
        return 1 + 0.10 * g + DMG_QUAD * g * g;
    }
    /** Bước 27: thành phần bậc hai để quái cấp cao còn đe dọa (BalanceSimTest: 8-70 đòn hạ người). */
    public static final double DMG_QUAD = 0.0017;

    /** Sat thuong ky nang: 3 x dmgScale x mult. */
    public static double skillDamage(int level, double mult) { return 3.0 * dmgScale(level) * mult; }

    private static void mod(Mob m, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a,
                            String id, double amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation op) {
        var inst = m.getAttribute(a);
        if (inst == null) return;
        var rl = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id);
        inst.removeModifier(rl);
        inst.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl, amount, op));
    }

    public static void applyStats(Mob m, int level, double hpMul, double dmgMul) {
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        var add = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        mod(m, Attributes.MAX_HEALTH, "hu_lv_hp", hpMul * com.hukhong.endgame.WorldTier.hpMul() * hpScale(level) - 1, mul);
        mod(m, Attributes.ATTACK_DAMAGE, "hu_lv_atk", dmgMul * com.hukhong.endgame.WorldTier.dmgMul() * dmgScale(level) - 1, mul);
        mod(m, Attributes.ARMOR, "hu_lv_armor", Math.min(20, 0.25 * (level - 1)), add);
        mod(m, Attributes.MOVEMENT_SPEED, "hu_lv_speed", Math.min(0.30, 0.005 * (level - 1)), mul);
        m.setHealth(m.getMaxHealth());
    }

    public static Component name(int level, Component base) {
        MutableComponent c = Component.literal("[Lv." + level + "] ").append(base);
        return c.withStyle(color(level));
    }

    private static ChatFormatting color(int mobLevel) {
        int diff = mobLevel - ClientCache.cultivation().level();
        if (diff >= 10) return ChatFormatting.DARK_RED;
        if (diff >= 3) return ChatFormatting.RED;
        if (diff >= -2) return ChatFormatting.YELLOW;
        if (diff > -10) return ChatFormatting.GREEN;
        return ChatFormatting.GRAY;
    }

    public static void particles(Mob m) {
        if (m.level().isClientSide && m.tickCount % 3 == 0)
            m.level().addParticle(com.hukhong.vfx.ModParticles.HU_KHONG.get(),
                    m.getRandomX(0.6), m.getRandomY(), m.getRandomZ(0.6), 0, 0.05, 0);
    }

    /**
     * EXP mot quai = w * E(L) * levelExpMult(L), E(L) = yeu cau EXP cua cap L (CultivationService.expToNext).
     * Quai cung cap cho ~3% yeu cau cap o cap 1-20 (~30 quai/cap), giam dan xuong ~1.6% o cap cao.
     * x0.1 neu quai thap hon nguoi choi >= 10 cap.
     */
    public static long exp(double w, int level, int playerLevel) {
        double e = com.hukhong.core.CultivationService.expToNext(level) * levelExpMult(level);
        double f = level <= playerLevel - 10 ? 0.1 : 1.0;
        double raw = w * e * f;
        // ROOT CAUSE "giet quai khong co EXP": quai cap thap cho < 0.5 EXP -> Math.round = 0.
        // Quai ngang/cao hon nguoi choi (f == 1) luon cho >= 1 EXP.
        return f >= 1.0 && w > 0 ? Math.max(1, Math.round(raw)) : Math.round(raw);
    }

    /** Ty le yeu cau cap moi quai (tren 1 don vi trong so w): 0.030 cho cap 1-20, giam tuyen tinh toi 0.016 o cap 100. */
    public static double levelExpMult(int level) {
        if (level <= 20) return 0.030;
        double t = Math.min(1.0, (level - 20) / 80.0);
        return 0.030 - t * (0.030 - 0.016);
    }

    /** Trần max_health vanilla (RangedAttribute). HP thiết kế vượt trần được quy đổi thành giảm sát thương nhận. */
    public static final double VANILLA_HP_CAP = 1024.0;

    /** HP thiết kế (chưa bị kẹp 1024) = base cộng ADD_VALUE, nhân ADD_MULTIPLIED_BASE rồi TOTAL. */
    public static double designHp(LivingEntity e) {
        var inst = e.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return e.getMaxHealth();
        double base = inst.getBaseValue(), v = base;
        for (var m : inst.getModifiers()) if (m.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) v += m.amount();
        double sum = 0;
        for (var m : inst.getModifiers()) if (m.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE) sum += m.amount();
        v += base * sum;
        for (var m : inst.getModifiers()) if (m.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) v *= 1 + m.amount();
        return v;
    }

    /** Hệ số (<= 1) nhân sát thương nhận / lượng hồi tuyệt đối: HP thiết kế / HP thực (kẹp 1024) = số lần "dày" hơn. */
    public static float ehpScale(LivingEntity e) {
        double real = e.getMaxHealth();
        double design = designHp(e);
        return design <= real || real <= 0 ? 1f : (float) (real / design);
    }

    private static boolean scaledByDesignHp(LivingEntity e) {
        return e instanceof HuMob || e instanceof com.hukhong.chg.ChapHanhGia || e instanceof com.hukhong.endgame.NguyenThuyTichDiet;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !scaledByDesignHp(v)) return;
        float s = ehpScale(v);
        if (s < 1f) e.setAmount(e.getAmount() * s);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof HuMob h) || e.getEntity().level().isClientSide) return;
        LivingEntity v = e.getEntity();
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        float mult = RewardGate.reward(v);
        if (mult <= 0f) return;
        long exp = Math.round(exp(h.huWeight(), h.huLevel(), CultivationService.get(p).level()) * mult * com.hukhong.endgame.WorldTier.rewardMul());
        if (exp > 0) CultivationService.addExp(p, exp);
        if (v.getRandom().nextFloat() < h.honTinhChance() * mult) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "hon_tinh"));
            if (item != net.minecraft.world.item.Items.AIR)
                v.level().addFreshEntity(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), new ItemStack(item)));
        }
    }
}
