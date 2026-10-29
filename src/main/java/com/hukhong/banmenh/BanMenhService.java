package com.hukhong.banmenh;

import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Cấp / triệu hồi / khôi phục Bản Mệnh Kiếm. */
public final class BanMenhService {
    private BanMenhService() {}

    public static final long SUMMON_COOLDOWN_TICKS = 1200; // 60s
    /** Cấp tối thiểu cho từng giai đoạn: Phàm, Linh, Huyền, Địa, Thiên, Tiên, Thần. */
    public static final int[] STAGE_LEVELS = BanMenhMath.STAGE_LEVELS;
    /** Hệ số Công theo phẩm (S2 §2.1). */
    public static final float[] QUALITY = {1.0f, 1.15f, 1.35f, 1.6f, 1.9f, 2.3f, 3.0f};

    private static final Map<UUID, Long> SUMMON_CD = new HashMap<>();

    public static int stageForLevel(int level) { return BanMenhMath.stageForLevel(level); }

    /** CHỮ KÝ CỐ ĐỊNH. Roll (seed lưu trước, không reroll) + trao kiếm nếu chưa có trong túi. */
    public static void grant(ServerPlayer p) {
        BanMenhData d = p.getData(BanMenhAttachments.BAN_MENH.get());
        if (d.seed() == 0L) {
            d = d.withSeed(p.getRandom().nextLong() | 1L);
            p.setData(BanMenhAttachments.BAN_MENH.get(), d); // lưu seed TRƯỚC khi roll
        }
        if (!d.hasType()) {
            BanMenhType t = roll(d.seed(), p);
            d = d.withType(t.id(), p.getUUID()).withStage(0);
            p.setData(BanMenhAttachments.BAN_MENH.get(), d);
        } else if (!p.getUUID().equals(d.owner())) {
            d = d.withType(d.type(), p.getUUID());
            p.setData(BanMenhAttachments.BAN_MENH.get(), d);
        }
        if (!hasSword(p)) give(p, d);
    }

    static BanMenhType roll(long seed, ServerPlayer p) {
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        Aptitude apt = cd.aptitude().get(Path.KIEM);
        double shift = apt == null ? 0 : switch (apt) { case S -> 0.3; case SS -> 0.6; case SSS -> 1.0; default -> 0; };
        BanMenhType[] all = BanMenhType.values();
        double[] w = new double[all.length];
        double sum = 0;
        for (int i = 0; i < all.length; i++) { w[i] = all[i].weight * (all[i].rare ? 1 + shift : 1); sum += w[i]; }
        double r = new Random(seed).nextDouble() * sum;
        for (int i = 0; i < all.length; i++) { r -= w[i]; if (r < 0) return all[i]; }
        return all[0];
    }

    static int cultivationLevel(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).level(); }

    public static ItemStack create(BanMenhData d) {
        BanMenhType t = BanMenhType.byId(d.type());
        ItemStack s = new ItemStack(ModItems.BAN_MENH_KIEM.get());
        if (t == null) return s;
        s.set(ModItems.BAN_MENH_TYPE.get(), t.id());
        s.set(ModItems.BAN_MENH_STAGE.get(), d.stage());
        if (d.owner() != null) s.set(ModItems.BOUND_TO.get(), d.owner());
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(t.modelIndex()));
        return s;
    }

    public static boolean hasSword(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
            if (isSword(inv.getItem(i), p.getUUID())) return true;
        if (isSword(p.containerMenu.getCarried(), p.getUUID())) return true;
        for (var slot : p.containerMenu.slots) // kiếm đang tạm nằm ở ô menu khác (đe, bàn chế tạo...)
            if (isSword(slot.getItem(), p.getUUID())) return true;
        return false;
    }

    static boolean isSword(ItemStack s, UUID owner) {
        return s.is(ModItems.BAN_MENH_KIEM.get()) && owner.equals(s.get(ModItems.BOUND_TO.get()));
    }

    /** Không bao giờ vứt ra thế giới: túi đầy thì chờ (vòng kiểm tra định kỳ thử lại). */
    private static boolean give(ServerPlayer p, BanMenhData d) {
        ItemStack s = create(d.withStage(BanMenhMath.effectiveStage(d.stage(), cultivationLevel(p))));
        return p.getInventory().add(s);
    }

    public static void forget(UUID id) { SUMMON_CD.remove(id); }

    /** Respawn: trả kiếm miễn phí nếu thiếu. */
    public static void restore(ServerPlayer p) {
        BanMenhData d = p.getData(BanMenhAttachments.BAN_MENH.get());
        if (d.hasType() && !hasSword(p)) give(p, d);
    }

    /** Lệnh triệu hồi lại (cooldown 60s). Trả true nếu thành công. */
    public static boolean summon(ServerPlayer p) {
        BanMenhData d = p.getData(BanMenhAttachments.BAN_MENH.get());
        if (!d.hasType()) { p.sendSystemMessage(Component.translatable("hukhong.banmenh.msg.none")); return false; }
        if (hasSword(p)) { p.sendSystemMessage(Component.translatable("hukhong.banmenh.msg.have")); return false; }
        long now = p.serverLevel().getGameTime();
        Long last = SUMMON_CD.get(p.getUUID());
        if (last != null && now - last < SUMMON_COOLDOWN_TICKS) {
            long left = (SUMMON_COOLDOWN_TICKS - (now - last) + 19) / 20;
            p.sendSystemMessage(Component.translatable("hukhong.banmenh.msg.cooldown", left));
            return false;
        }
        SUMMON_CD.put(p.getUUID(), now);
        // dọn bản đã vứt trong thế giới để không nhân đôi
        for (ItemEntity ie : p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(128),
                e -> isSword(e.getItem(), p.getUUID()))) ie.discard();
        if (!give(p, d)) {
            SUMMON_CD.remove(p.getUUID());
            p.sendSystemMessage(Component.translatableWithFallback("hukhong.banmenh.msg.full", "Túi đầy, hãy chừa một ô trống."));
            return false;
        }
        p.sendSystemMessage(Component.translatable("hukhong.banmenh.msg.summoned"));
        return true;
    }

    // ------------------------------------------------------------------ tiến hóa bằng Hồn Tinh

    /** Hồn Tinh trong ví (offhandOnly giữ lại cho tương thích chữ ký; ví không phân biệt tay). */
    public static int countTinh(ServerPlayer p, boolean offhandOnly) {
        return (int) Math.min(Integer.MAX_VALUE, com.hukhong.currency.Wallet.honTinh(p));
    }

    public static void takeTinh(ServerPlayer p, int n, boolean offhandOnly) {
        com.hukhong.currency.Wallet.payHonTinh(p, n);
    }

    /**
     * Tôi Kiếm: tiến hóa 1 giai đoạn, tốn Hồn Tinh, trần theo cảnh giới. offhandOnly = dùng kiếm với Hồn Tinh ở tay phụ;
     * false = đập kiếm trên đe (lấy Hồn Tinh trong cả túi). Trả true nếu tiến hóa.
     */
    public static boolean tryEvolve(ServerPlayer p, ItemStack sword, boolean offhandOnly) {
        if (!sword.is(ModItems.BAN_MENH_KIEM.get()) || !BanMenhKiemItem.isOwner(sword, p)) return false;
        BanMenhData d = p.getData(BanMenhAttachments.BAN_MENH.get());
        int cur = BanMenhKiemItem.stageOf(sword);
        int have = countTinh(p, offhandOnly);
        int code = BanMenhMath.check(cur, cultivationLevel(p), have);
        switch (code) {
            case BanMenhMath.MAXED -> { msg(p, "hukhong.banmenh.evolve.max", "Kiếm đã đạt Thần phẩm."); return false; }
            case BanMenhMath.REALM_CAP -> { msg(p, "hukhong.banmenh.evolve.realm", "Cảnh giới chưa đủ để tiến hóa tiếp (trần theo cấp)."); return false; }
            case BanMenhMath.NEED_TINH -> {
                p.displayClientMessage(Component.translatableWithFallback("hukhong.banmenh.evolve.need",
                        "Cần %s Hồn Tinh (hiện có %s).", BanMenhMath.tinhCost(cur + 1), have), true);
                return false;
            }
            default -> { }
        }
        takeTinh(p, BanMenhMath.tinhCost(cur + 1), offhandOnly);
        int ns = cur + 1;
        sword.set(ModItems.BAN_MENH_STAGE.get(), ns);
        p.setData(BanMenhAttachments.BAN_MENH.get(), d.withStage(ns));
        com.hukhong.stats.StatService.refresh(p);
        if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.6, 0.5, 0.5);
            sl.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
        }
        p.sendSystemMessage(Component.translatableWithFallback("hukhong.banmenh.evolve.done", "Bản Mệnh Kiếm tiến hóa: %s",
                Component.translatable("hukhong.banmenh.stage." + ns)));
        return true;
    }

    private static void msg(ServerPlayer p, String key, String vi) {
        p.displayClientMessage(Component.translatableWithFallback(key, vi), true);
    }

    // ------------------------------------------------------------------ hiệu ứng loại (Ngọc Tâm / Trọng Nhạc)

    private static final net.minecraft.resources.ResourceLocation PASSIVE_ID =
            com.hukhong.stats.ModAttributes.id("banmenh/passive");

    private static BanMenhType typeOf(ServerPlayer p) {
        return BanMenhType.byId(p.getData(BanMenhAttachments.BAN_MENH.get()).type());
    }

    private static int stageOf(ServerPlayer p) {
        return BanMenhMath.effectiveStage(p.getData(BanMenhAttachments.BAN_MENH.get()).stage(), cultivationLevel(p));
    }

    /** Hệ số chi phí kỹ năng (Ngọc Tâm Lv40: 0.9). */
    public static double skillCostFactor(ServerPlayer p) { return BanMenhMath.skillCostFactor(typeOf(p), stageOf(p)); }

    /** Ngự kiếm miễn phí (Ngọc Tâm Lv80, trong combat). */
    public static boolean freeFlight(ServerPlayer p, boolean inCombat) {
        return BanMenhMath.freeFlight(typeOf(p), stageOf(p), inCombat);
    }

    /** Gỡ + áp lại modifier transient: Ngọc Tâm (hồi/tối đa tài nguyên), Trọng Nhạc (tốc -6%). Gọi từ StatService.refresh. */
    public static void applyPassives(ServerPlayer p) {
        BanMenhType t = typeOf(p);
        passive(p, com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.RESOURCE_REGEN), BanMenhMath.regenBonus(t));
        passive(p, com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.MAX_RESOURCE), BanMenhMath.maxResourceBonus(t));
        passive(p, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, BanMenhMath.speedBonus(t));
    }

    private static void passive(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double v) {
        var inst = p.getAttribute(attr);
        if (inst == null) return;
        if (v == 0) { inst.removeModifier(PASSIVE_ID); return; }
        inst.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                PASSIVE_ID, v, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
}
