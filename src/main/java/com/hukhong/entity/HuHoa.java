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
    static final double R1 = 1500, R2 = 3500;

    public static int levelFor(ServerLevelAccessor lvl, BlockPos pos, net.minecraft.util.RandomSource r) {
        BlockPos sp = lvl.getLevel().getSharedSpawnPos();
        double dx = pos.getX() - sp.getX(), dz = pos.getZ() - sp.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        int base = d < R1 ? 1 + (int) (14 * d / R1) : 15 + (int) Math.min(15, 15 * (d - R1) / (R2 - R1));
        return Math.max(1, Math.min(30, base + r.nextInt(3) - 1));
    }

    /** He so cap: hp x(1+0.38(L-1)), sat thuong x(1+0.10(L-1)), giap +0.25/cap (toi da 20), toc do +0.5%/cap (toi da +30%). */
    public static double hpScale(int level) { return 1 + HP_PER_LEVEL * (level - 1); }
    /** Chỉnh ở Bước 9 (R5-balance-p0): 0.12 -> 0.38 để người cùng cấp cần 6-10 đòn kỹ năng. */
    public static final double HP_PER_LEVEL = 0.38;
    public static double dmgScale(int level) { return 1 + 0.10 * (level - 1); }

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

    /** w * E(L)/200, E(L)=40*L^2.15; x0.1 neu quai thap hon nguoi choi >= 10 cap. */
    public static long exp(double w, int level, int playerLevel) {
        double e = 40 * Math.pow(level, 2.15) / 200.0;
        double f = level <= playerLevel - 10 ? 0.1 : 1.0;
        return Math.round(w * e * f);
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
