package com.hukhong.skillbook;

import com.hukhong.core.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Bảng rơi bí tịch / công pháp: Bí Cảnh, boss (Hư Vương, Chấp Hành Giả, Sứ Đồ), nhiệm vụ tông môn, thưởng NPC cốt truyện.
 * Phần chọn là thuần Java (Random + trọng số) để test; phần tạo ItemStack/trao thưởng ở cuối.
 */
public final class SkillBookRewards {
    private SkillBookRewards() {}

    // trọng số phẩm {Phàm, Huyền, Địa, Thiên}
    private static final double[][] BI_CANH = {{85, 15, 0, 0}, {60, 35, 5, 0}, {25, 50, 22, 3}, {5, 35, 50, 10}, {0, 15, 50, 35}};
    private static final double[][] CHG = {{20, 60, 20, 0}, {5, 50, 40, 5}, {0, 30, 50, 20}, {0, 10, 50, 40}};
    private static final double[][] SU_DO = {{70, 30, 0, 0}, {30, 55, 15, 0}, {5, 40, 45, 10}};
    private static final double[] HU_VUONG = {55, 40, 5, 0};
    private static final double[][] SECT = {{70, 30, 0, 0}, {35, 50, 15, 0}, {10, 40, 40, 10}};

    /** Chọn phẩm theo trọng số, x thuộc [0,1). */
    public static SkillGrade gradeAt(double[] w, double x) {
        double sum = 0;
        for (double v : w) sum += v;
        double t = Math.max(0, Math.min(0.999999, x)) * sum, acc = 0;
        for (int i = 0; i < w.length; i++) {
            acc += w[i];
            if (t < acc && w[i] > 0) return SkillGrade.of(i);
        }
        for (int i = w.length - 1; i >= 0; i--) if (w[i] > 0) return SkillGrade.of(i);
        return SkillGrade.PHAM;
    }

    /** Chọn ngẫu nhiên một kỹ năng phẩm g; nếu có `paths` thì 75% ưu tiên kỹ năng thuộc đạo của người nhận (hoặc chung). */
    public static BookSkill pick(Random r, SkillGrade g, Set<Path> paths) {
        List<BookSkill> all = BookSkill.ofGrade(g);
        if (paths != null && !paths.isEmpty() && r.nextDouble() < 0.75) {
            List<BookSkill> mine = new ArrayList<>();
            for (BookSkill s : all) if (s.path == null || paths.contains(s.path)) mine.add(s);
            if (!mine.isEmpty()) return mine.get(r.nextInt(mine.size()));
        }
        return all.get(r.nextInt(all.size()));
    }

    public static BookSkill pick(Random r, SkillGrade g) { return pick(r, g, null); }

    // ------------------------------------------------------------------ xác suất theo nguồn
    /** Bí Cảnh: xác suất rương có bí tịch theo cấp (0..4): 10% -> 34%. */
    public static double biCanhChance(int tier) { return 0.10 + 0.06 * Math.max(0, Math.min(4, tier)); }

    public static SkillGrade biCanhGrade(int tier, double x) { return gradeAt(BI_CANH[Math.max(0, Math.min(4, tier))], x); }

    public static SkillGrade huVuongGrade(double x) { return gradeAt(HU_VUONG, x); }
    public static final double HU_VUONG_CHANCE = 0.60;

    /** Chấp Hành Giả idx 1..4: luôn rơi 1 bí tịch. */
    public static SkillGrade chgGrade(int idx, double x) { return gradeAt(CHG[Math.max(1, Math.min(4, idx)) - 1], x); }

    /** Sứ Đồ: tỉ lệ nhỏ mỗi lần hạ, phẩm theo cấp quái. */
    public static double suDoChance(int level) { return 0.03 + 0.0004 * Math.max(0, Math.min(100, level)); }
    public static SkillGrade suDoGrade(int level, double x) { return gradeAt(SU_DO[level < 30 ? 0 : level < 60 ? 1 : 2], x); }

    /** Nhiệm vụ tông môn: thường 4% + 2%/bậc tông; chuỗi hoàn tất 100%. tier = 0 Nhân, 1 Địa, 2 Thiên. */
    public static double questChance(int tier, boolean chainFinal) {
        return chainFinal ? 1.0 : 0.04 + 0.02 * Math.max(0, Math.min(2, tier));
    }
    public static SkillGrade questGrade(int tier, boolean chainFinal, double x) {
        return gradeAt(SECT[Math.max(0, Math.min(2, tier))], chainFinal ? Math.min(0.999, x * 0.6 + 0.4) : x);
    }

    // ------------------------------------------------------------------ vật phẩm
    public static ItemStack stack(Random r, SkillGrade g, Set<Path> paths) {
        return SkillBookRegistry.stack(pick(r, g, paths));
    }

    /** Đạo của người hạ boss (để ưu tiên bí tịch hợp đạo); null nếu không phải người chơi. */
    public static Set<Path> killerPaths(net.minecraft.world.damagesource.DamageSource src) {
        return src != null && src.getEntity() instanceof ServerPlayer p ? com.hukhong.core.CultivationService.get(p).paths() : null;
    }

    /** Trao vào túi (rơi xuống đất nếu đầy). */
    public static void give(ServerPlayer p, ItemStack st) {
        if (st.isEmpty()) return;
        if (!p.getInventory().add(st.copy())) p.drop(st.copy(), false);
        p.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("hukhong.skillbook.msg.received",
                "Nhận được: %s", st.getHoverName()), false);
    }

    /** Thưởng NPC cốt truyện: spec = id kỹ năng, hoặc "random:PHAM|HUYEN|DIA|THIEN" (ưu tiên kỹ năng hợp đạo của người nhận). */
    public static void giveStory(ServerPlayer p, String spec) {
        if (spec == null) return;
        BookSkill s = BookSkill.byId(spec);
        if (s == null && spec.startsWith("random:")) {
            try {
                SkillGrade g = SkillGrade.valueOf(spec.substring(7).toUpperCase(java.util.Locale.ROOT));
                s = pick(new Random(p.getRandom().nextLong()), g, com.hukhong.core.CultivationService.get(p).paths());
            } catch (IllegalArgumentException ignored) { }
        }
        if (s != null) give(p, SkillBookRegistry.stack(s));
    }

    /** Thưởng nhiệm vụ tông môn (rỗng nếu không trúng). */
    public static ItemStack questReward(ServerPlayer p, int tier, boolean chainFinal) {
        var r = p.getRandom();
        if (r.nextDouble() >= questChance(tier, chainFinal)) return ItemStack.EMPTY;
        SkillGrade g = questGrade(tier, chainFinal, r.nextDouble());
        return stack(new Random(r.nextLong()), g, com.hukhong.core.CultivationService.get(p).paths());
    }
}
