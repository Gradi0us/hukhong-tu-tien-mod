package com.hukhong.society;

import com.hukhong.HuKhong;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Buff tông theo bậc (S6 §2.2, rút gọn thành 1 buff/tông): Bá Thể +15% HP, Thiên Kiếm +15% sát thương,
 * Thiên Công +2 hiệu suất đào, Đan Đỉnh +10% tỉ lệ thành đan (đọc ở DanLoBlockEntity),
 * Quang Minh +6 giáp hấp thụ, Vạn Tượng +2 may mắn, Huyết Ảnh +12% tốc độ di chuyển. Nhân hệ số bậc 25/50/100/100%.
 */
public final class SectBuffs {
    private SectBuffs() {}

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "sect_buff");
    private static final Holder<Attribute>[] ALL = holders();

    @SuppressWarnings("unchecked")
    private static Holder<Attribute>[] holders() {
        return new Holder[]{Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MINING_EFFICIENCY,
                Attributes.MAX_ABSORPTION, Attributes.LUCK, Attributes.MOVEMENT_SPEED};
    }

    public static double factor(ServerPlayer p) {
        SectData d = SectService.get(p);
        return d.member() ? SectMath.buffFactor(SectService.rankOf(p, d)) : 0;
    }

    /** Cộng thêm vào tỉ lệ đúng-nhiệt khi luyện đan (thành viên Đan Đỉnh Tông). */
    public static double alchemyBonus(ServerPlayer p) {
        return "dan_dinh_tong".equals(SectService.get(p).sect()) ? 0.10 * factor(p) : 0;
    }

    /** Gỡ buff cũ rồi gắn lại theo tông + bậc hiện tại. */
    public static void refresh(ServerPlayer p) {
        for (Holder<Attribute> h : ALL) {
            AttributeInstance a = p.getAttribute(h);
            if (a != null) a.removeModifier(ID);
        }
        String sect = SectService.get(p).sect();
        double f = factor(p);
        if (f <= 0) return;
        switch (sect) {
            case "ba_the_mon" -> add(p, Attributes.MAX_HEALTH, 0.15 * f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "thien_kiem_tong" -> add(p, Attributes.ATTACK_DAMAGE, 0.15 * f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case "thien_cong_khi_cac" -> add(p, Attributes.MINING_EFFICIENCY, 2.0 * f, AttributeModifier.Operation.ADD_VALUE);
            case "quang_minh_dao_cung" -> add(p, Attributes.MAX_ABSORPTION, 6.0 * f, AttributeModifier.Operation.ADD_VALUE);
            case "van_tuong_thanh_tong" -> add(p, Attributes.LUCK, 2.0 * f, AttributeModifier.Operation.ADD_VALUE);
            case "huyet_anh_ma_mon" -> add(p, Attributes.MOVEMENT_SPEED, 0.12 * f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            default -> { }
        }
    }

    private static void add(ServerPlayer p, Holder<Attribute> h, double v, AttributeModifier.Operation op) {
        AttributeInstance a = p.getAttribute(h);
        if (a != null) a.addTransientModifier(new AttributeModifier(ID, v, op));
    }
}
