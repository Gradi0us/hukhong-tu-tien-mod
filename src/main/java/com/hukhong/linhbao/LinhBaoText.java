package com.hukhong.linhbao;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Văn bản mô tả Linh Bảo (tên, phẩm, bonus thật theo chỉ số hiện tại). Dùng chung cho lệnh /hukhong linhbao và tooltip màn Hồ Sơ
 * (ProfileScreen.artifactTooltip có thể nối thêm lines(...) bằng 1 dòng).
 */
public final class LinhBaoText {
    private LinhBaoText() {}

    public static Component name(LinhBaoType t) {
        return Component.translatableWithFallback("hukhong.linhbao.name." + t.id, switch (t) {
            case DAO_LINH -> "Đạo Linh";
            case LINH_THE -> "Linh Thể";
            case MA_CONG -> "Ma Công Bá Thể Quyết";
        });
    }

    public static List<Component> lines(LinhBaoType t, int stage, double attackPower, double spellPower) {
        List<Component> l = new ArrayList<>();
        l.add(Component.translatableWithFallback("hukhong.linhbao.tip.stage", "Phẩm: %s (x%s)",
                Component.translatable("hukhong.banmenh.stage." + stage), String.format("%.2f", LinhBaoMath.quality(stage))).withStyle(ChatFormatting.GOLD));
        switch (t) {
            case DAO_LINH -> {
                l.add(Component.translatableWithFallback("hukhong.linhbao.tip.dao_linh.bolt",
                        "Trong giao tranh, mỗi %s giây bắn 1 tia thức thần: %s sát thương.",
                        String.format("%.1f", LinhBaoMath.DAO_BOLT_INTERVAL_TICKS / 20.0),
                        String.format("%.0f", LinhBaoMath.daoBolt(spellPower, stage))).withStyle(ChatFormatting.YELLOW));
                if (stage >= LinhBaoMath.DAO_HEAL_MIN_STAGE)
                    l.add(Component.translatableWithFallback("hukhong.linhbao.tip.dao_linh.heal",
                            "Cứ %s tia, 1 tia chữa đồng đội thấp máu nhất gần đó %s máu.", LinhBaoMath.DAO_HEAL_EVERY,
                            String.format("%.0f", LinhBaoMath.daoHeal(spellPower, stage))).withStyle(ChatFormatting.GREEN));
            }
            case LINH_THE -> l.add(Component.translatableWithFallback("hukhong.linhbao.tip.linh_the.hit",
                    "Mỗi đòn tay cộng %s sát thương.", String.format("%.0f", LinhBaoMath.theHit(attackPower, stage))).withStyle(ChatFormatting.YELLOW));
            case MA_CONG -> {
                l.add(Component.translatableWithFallback("hukhong.linhbao.tip.ma_cong.hit",
                        "Mỗi đòn thường cộng %s sát thương.", String.format("%.0f", LinhBaoMath.maHit(attackPower, stage))).withStyle(ChatFormatting.YELLOW));
                l.add(Component.translatableWithFallback("hukhong.linhbao.tip.ma_cong.ls",
                        "Hút máu %s%% sát thương cộng thêm (tắt khi Mất Kiểm Soát).",
                        String.format("%.0f", LinhBaoMath.maLifesteal(stage) * 100)).withStyle(ChatFormatting.RED));
            }
        }
        return l;
    }
}
