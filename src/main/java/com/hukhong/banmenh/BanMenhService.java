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
    public static final int[] STAGE_LEVELS = {1, 20, 40, 60, 80, 90, 100};
    /** Hệ số Công theo phẩm (S2 §2.1). */
    public static final float[] QUALITY = {1.0f, 1.15f, 1.35f, 1.6f, 1.9f, 2.3f, 3.0f};

    private static final Map<UUID, Long> SUMMON_CD = new HashMap<>();

    public static int stageForLevel(int level) {
        int s = 0;
        for (int i = 0; i < STAGE_LEVELS.length; i++) if (level >= STAGE_LEVELS[i]) s = i;
        return s;
    }

    /** CHỮ KÝ CỐ ĐỊNH. Roll (seed lưu trước, không reroll) + trao kiếm nếu chưa có trong túi. */
    public static void grant(ServerPlayer p) {
        BanMenhData d = p.getData(BanMenhAttachments.BAN_MENH.get());
        if (d.seed() == 0L) {
            d = d.withSeed(p.getRandom().nextLong() | 1L);
            p.setData(BanMenhAttachments.BAN_MENH.get(), d); // lưu seed TRƯỚC khi roll
        }
        if (!d.hasType()) {
            BanMenhType t = roll(d.seed(), p);
            d = d.withType(t.id(), p.getUUID()).withStage(stageNow(p));
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

    static int stageNow(ServerPlayer p) {
        return stageForLevel(p.getData(ModAttachments.CULTIVATION).level());
    }

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
        return isSword(p.containerMenu.getCarried(), p.getUUID());
    }

    static boolean isSword(ItemStack s, UUID owner) {
        return s.is(ModItems.BAN_MENH_KIEM.get()) && owner.equals(s.get(ModItems.BOUND_TO.get()));
    }

    private static void give(ServerPlayer p, BanMenhData d) {
        ItemStack s = create(d.withStage(stageNow(p)));
        if (!p.getInventory().add(s)) p.drop(s, false);
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
        give(p, d);
        p.sendSystemMessage(Component.translatable("hukhong.banmenh.msg.summoned"));
        return true;
    }
}
