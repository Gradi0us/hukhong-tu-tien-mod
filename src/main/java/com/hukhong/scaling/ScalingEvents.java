package com.hukhong.scaling;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.WorldBoss;
import com.hukhong.gates.RewardGate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Logic máy chủ: Kiếp Số tăng/giảm, Huyết Ma, rơi Tâm Tinh/Linh Tinh, áp bonus vĩnh viễn. */
public final class ScalingEvents {
    private ScalingEvents() {}

    static final String ELITE = "hk_huyet_ma";
    private static final ResourceLocation HEART_ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tam_tinh_hp");
    private static final ResourceLocation POWER_ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "linh_tinh_atk");

    // ------------------------------------------------------------------ Kiếp Số theo người chơi

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !ScalingConfig.on()) return;
        if (p.tickCount % 20 != 0 || p.isSpectator() || !p.isAlive()) return;
        double lv = CultivationService.get(p).level();
        Difficulty.setKiep(p, Difficulty.kiep(p) + Difficulty.perSecond((int) lv));
        if (p.tickCount % 200 == 0) { applyCrystals(p); sync(p); } // re-apply: an toan sau StatService.refresh
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !ScalingConfig.on()) return;
        Difficulty.setKiep(p, Difficulty.kiep(p) * ScalingConfig.DEATH_KEEP.get());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        var o = e.getOriginal().getPersistentData();
        var n = e.getEntity().getPersistentData();
        for (String k : new String[]{Difficulty.KEY, Difficulty.HEART, Difficulty.POWER})
            if (o.contains(k)) n.putDouble(k, o.getDouble(k));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { join(e.getEntity()); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { join(e.getEntity()); }

    private static void join(net.minecraft.world.entity.player.Player pl) {
        if (pl instanceof ServerPlayer p) {
            applyCrystals(p);
            sync(p);
        }
    }

    static void sync(ServerPlayer p) {
        if (p.connection == null) return;
        double k = Difficulty.kiep(p);
        PacketDistributor.sendToPlayer(p, new KiepPayload((float) k, (float) Difficulty.mult(p.serverLevel(), p.blockPosition())));
    }

    // ------------------------------------------------------------------ bonus vĩnh viễn (Tâm Tinh / Linh Tinh)

    /** Áp lại modifier vĩnh viễn từ dữ liệu đã lưu (idempotent). */
    static void applyCrystals(ServerPlayer p) {
        put(p, Attributes.MAX_HEALTH, HEART_ID, p.getPersistentData().getDouble(Difficulty.HEART));
        put(p, com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.ATTACK_POWER), POWER_ID,
                p.getPersistentData().getDouble(Difficulty.POWER));
    }

    private static void put(ServerPlayer p, Holder<Attribute> a, ResourceLocation id, double amount) {
        var inst = p.getAttribute(a);
        if (inst == null) return;
        var cur = inst.getModifier(id);
        if (cur != null && cur.amount() == amount) return;
        inst.removeModifier(id);
        if (amount > 0) inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    // ------------------------------------------------------------------ Huyết Ma

    public static boolean isElite(Mob m) { return m.getPersistentData().getBoolean(ELITE); }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent e) {
        Mob m = e.getEntity();
        if (!ScalingConfig.on() || !(m instanceof HuMob) || m instanceof WorldBoss) return;
        MobSpawnType t = e.getSpawnType();
        if (t == MobSpawnType.COMMAND || t == MobSpawnType.SPAWN_EGG || t == MobSpawnType.DISPENSER) return;
        if (!(m.level() instanceof ServerLevel sl)) return;
        double chance = Math.min(ScalingConfig.HUYET_MAX.get(),
                ScalingConfig.HUYET_BASE.get() + ScalingConfig.HUYET_PER_KIEP.get() * Difficulty.avgKiep(sl, m.blockPosition()));
        if (m.getRandom().nextDouble() >= chance) return;
        makeElite(m);
    }

    public static void makeElite(Mob m) {
        m.getPersistentData().putBoolean(ELITE, true);
        mod(m, Attributes.MAX_HEALTH, "huyet_ma_hp", ScalingConfig.HUYET_HP.get() - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(m, Attributes.ATTACK_DAMAGE, "huyet_ma_atk", ScalingConfig.HUYET_DMG.get() - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(m, Attributes.ARMOR, "huyet_ma_armor", 4, AttributeModifier.Operation.ADD_VALUE);
        mod(m, Attributes.MOVEMENT_SPEED, "huyet_ma_speed", 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        m.setHealth(m.getMaxHealth());
        m.setGlowingTag(true);
        m.setCustomName(Component.translatable("hukhong.scaling.huyet_ma", m.getType().getDescription()).withStyle(ChatFormatting.DARK_RED));
        m.setCustomNameVisible(true);
    }

    private static void mod(Mob m, Holder<Attribute> a, String id, double amount, AttributeModifier.Operation op) {
        var inst = m.getAttribute(a);
        if (inst == null) return;
        var rl = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id);
        inst.removeModifier(rl);
        inst.addPermanentModifier(new AttributeModifier(rl, amount, op));
    }

    // ------------------------------------------------------------------ thưởng

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof Mob m) || !(m instanceof HuMob h) || m.level().isClientSide || !isElite(m)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !RewardGate.canReward(m)) return;
        long base = HuHoa.exp(h.huWeight(), h.huLevel(), CultivationService.get(p).level());
        long extra = Math.round(base * (ScalingConfig.HUYET_EXP.get() - 1));
        if (extra > 0) CultivationService.addExp(p, extra);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof Mob m) || !(m instanceof HuMob) || m.level().isClientSide || !ScalingConfig.on()) return;
        if (!RewardGate.canReward(m)) return;
        boolean elite = isElite(m);
        roll(e, m, elite ? ScalingConfig.DROP_HEART_ELITE.get() : ScalingConfig.DROP_HEART.get(), new ItemStack(ScalingMod.TAM_TINH.get()));
        roll(e, m, elite ? ScalingConfig.DROP_POWER_ELITE.get() : ScalingConfig.DROP_POWER.get(), new ItemStack(ScalingMod.LINH_TINH.get()));
        if (elite) {
            var hon = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "hon_tinh"));
            if (hon != net.minecraft.world.item.Items.AIR) {
                ItemStack s = new ItemStack(hon, 2 + m.getRandom().nextInt(3));
                e.getDrops().add(new ItemEntity(m.level(), m.getX(), m.getY(), m.getZ(), s));
            }
        }
    }

    private static void roll(LivingDropsEvent e, Mob m, double chance, ItemStack s) {
        if (m.getRandom().nextDouble() < chance)
            e.getDrops().add(new ItemEntity(m.level(), m.getX(), m.getY(), m.getZ(), s));
    }
}
