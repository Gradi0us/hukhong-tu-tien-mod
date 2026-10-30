package com.hukhong.client.screen.profile;

import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.passive.PassiveMath;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapMath;
import com.hukhong.phaptu.ThiemBoMath;
import com.hukhong.phaptu.beam.PhapBeamMath;
import com.hukhong.phaptu.linhtruong.LinhTruongMath;
import com.hukhong.skill.BanMenhSkill;
import com.hukhong.skill.Skill;
import com.hukhong.skill.SkillPages;
import com.hukhong.skill.SkillEffects;
import com.hukhong.skillbook.BookSkill;
import com.hukhong.skillbook.SkillBook;
import com.hukhong.skillbook.SkillBookData;
import com.hukhong.skillbook.SkillBookMath;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tab "Kỹ Năng" của màn K: liệt kê mọi kỹ năng người chơi đang có theo phím Q/E/R/T cho từng trang đạo, kèm chi phí,
 * hồi chiêu và sát thương (công thức + số thật theo chỉ số hiện tại). Thuần dữ liệu (không đọc Minecraft.player), số liệu
 * lấy từ đúng hằng số của code gây sát thương: Skill.*_COEF, SkillEffects, PhapMath.coef, PhapBeamMath, BanMenhSkill,
 * BookSkill.coef, LinhTruongMath, SkillBookMath.
 */
public final class SkillEntries {
    private SkillEntries() {}

    public static final int DESC = 0xFFCCCCCC, META = 0xFF8FB8FF, DMG = 0xFF8FE08F, WARN = 0xFFFF8855, NAME = 0xFFFFD34F;
    /** Cửa sổ Cường Hóa kỹ năng nền (SkillBookRuntime.windowTicks mặc định): nhịp đến muộn hơn thì không nhân hệ số. */
    static final int DEFAULT_WINDOW = 12;
    private static final String[] KEYS = {"Q", "E", "R", "T"};

    /** Đầu vào đã chụp từ client. stage = giai đoạn pháp khí/Linh Bảo theo Path.ordinal() (đã kẹp trần cảnh giới). */
    public record Ctx(int level, Set<Path> paths, Map<Path, Aptitude> apt, SkillBookData book, PhapElement element,
                      double ap, double sp, double maxHp, int resMax, int costMult, int[] stage, int bones) {
        Aptitude aptOf(Path p) { return apt.getOrDefault(p, Aptitude.B); }
        int stageOf(Path p) { return stage == null || p.ordinal() >= stage.length ? 0 : stage[p.ordinal()]; }
    }

    public record Line(Component text, int argb) {}

    public record Entry(String key, Component name, List<Line> lines) {}

    public record Page(Path path, Component header, List<Entry> basic, List<Entry> keys) {}

    private static Component t(String key, String vi, Object... a) { return Component.translatableWithFallback(key, vi, a); }

    private static String fmt(double v) { return PassiveEntries.fmt(v); }

    private static String pc(double v) { return PassiveEntries.pc(v); }

    /** Số sát thương làm tròn gọn: >=100 không lẻ, <100 một số lẻ. */
    public static String num(double v) {
        if (Math.abs(v) >= 100) return String.valueOf(Math.round(v));
        return fmt(Math.round(v * 10) / 10.0);
    }

    // ------------------------------------------------------------------ công thức thuần (test)

    /** Sát thương MỘT nhát của kỹ năng nền không phải Pháp (0 nếu kỹ năng không gây sát thương). */
    public static double baseHit(Skill s, double ap, double sp, int skillLevel) {
        double m = SkillBookMath.dmgMult(skillLevel);
        return switch (s) {
            case KIEM_1 -> Skill.KIEM_1_COEF * ap * m;
            case KIEM_2 -> Skill.KIEM_2_COEF * ap * m;
            case KIEM_ULT -> Skill.KIEM_ULT_COEF * ap * m;
            case MA_1 -> Skill.MA_1_COEF * ap * m;
            case THE_1 -> Skill.THE_1_COEF * ap * m;
            case DAO_1 -> Skill.DAO_1_COEF * PassiveMath.DAO_SKILL_MULT * sp * m;
            default -> 0;
        };
    }

    /** Hệ số nhân Cường Hóa của nhịp đến sau `delay` tick (ngoài cửa sổ thì x1). */
    static double winM(int delay, double m) { return delay <= DEFAULT_WINDOW ? m : 1.0; }

    /** Tổng sát thương lên MỘT mục tiêu của kỹ năng Pháp đánh n nhịp chia đều, nhịp đầu sau `first` tick, cách nhau `step`. */
    public static double phapPulses(double totalCoef, double power, int n, int first, int step, int skillLevel) {
        double m = SkillBookMath.dmgMult(skillLevel), sum = 0;
        for (int i = 0; i < n; i++) sum += totalCoef / n * power * winM(first + i * step, m);
        return sum;
    }

    /** Sát thương một nhịp Tinh Hà Quang Trụ đã gồm Cường Hóa. */
    public static double beamPulse(PhapElement e, Aptitude a, double sp, int skillLevel) {
        return PhapBeamMath.pulseDamage(e, a, sp) * SkillBookMath.dmgMult(skillLevel);
    }

    // ------------------------------------------------------------------ dựng

    public static List<Page> build(Ctx x) {
        List<Page> out = new ArrayList<>();
        for (Path p : SkillPages.owned(x.paths())) {
            Component header = Component.translatable(SkillPages.pathLangKey(p)).append(" - ")
                    .append(Component.translatable("hukhong.aptitude." + x.aptOf(p).name().toLowerCase(java.util.Locale.ROOT)));
            if (p == Path.PHAP && x.element() != null) header = header.copy().append("  ").append(Component.translatable(x.element().langKey()));
            List<Entry> basic = new ArrayList<>(), keys = new ArrayList<>();
            Entry b = basicAttack(x, p);
            if (b != null) basic.add(b);
            for (int slot = 0; slot < 4; slot++) {
                Entry e = forSlot(x, p, slot);
                if (e != null) keys.add(e);
            }
            out.add(new Page(p, header, basic, keys));
        }
        return out;
    }

    /** Bí tịch đã học nhưng không nằm trên phím nào ở bất kỳ trang nào đang sở hữu. */
    public static List<Entry> unassigned(Ctx x) {
        List<Entry> out = new ArrayList<>();
        for (BookSkill b : BookSkill.values()) {
            if (!x.book().learned(b.id) || !usableAnywhere(x, b)) continue;
            boolean placed = false;
            for (Path p : SkillPages.owned(x.paths())) {
                for (int s = 0; s < 4 && !placed; s++) {
                    SkillBook.Resolved r = SkillBook.resolve(x.book(), p, s, x.level(), lockT(x, p));
                    if (r != null && r.book() == b) placed = true;
                }
            }
            if (!placed) {
                Path pg = b.path != null ? b.path : SkillPages.owned(x.paths()).isEmpty() ? null : SkillPages.owned(x.paths()).get(0);
                if (pg != null) out.add(bookEntry(x, pg, "-", b));
            }
        }
        return out;
    }

    private static boolean usableAnywhere(Ctx x, BookSkill b) {
        for (Path p : SkillPages.owned(x.paths())) if (b.usableOn(p)) return true;
        return false;
    }

    private static int lockT(Ctx x, Path p) { return p == Path.PHAP ? PhapMath.lockMask(x.element()) : 0; } // mặt nạ phím cố định (Thổ E+T)

    private static Entry forSlot(Ctx x, Path page, int slot) {
        SkillBook.Resolved r = SkillBook.resolve(x.book(), page, slot, x.level(), lockT(x, page));
        if (r == null) return slot == 2 ? rEntry(x, page) : null;
        if (r.base() != null) return baseEntry(x, page, KEYS[slot], r.base());
        return bookEntry(x, page, KEYS[slot], r.book());
    }

    // ------------------------------------------------------------------ chung

    private static Line l(Component c, int argb) { return new Line(c, argb); }

    private static Component nameWithLevel(Component base, int lvl) {
        return lvl > 1 ? base.copy().append(" Lv" + lvl) : base;
    }

    private static Component resName(Path page) { return Component.translatable(page.resourceKey); }

    private static Line meta(Ctx x, Path page, int baseCost, int cdTicks) {
        return l(t("hukhong.skills.meta", "Tốn %s %s · Hồi chiêu %ss", baseCost * x.costMult(), resName(page), fmt(cdTicks / 20.0)), META);
    }

    private static int cdTicks(Ctx x, Path page, String id, int baseCd) {
        double mult = page == Path.PHAP && x.element() != null ? PhapMath.cooldownMult(x.element()) : 1.0;
        return (int) Math.round(SkillBookMath.cooldown(baseCd, x.book().level(id)) * mult);
    }

    /** Dòng sát thương: "Sát thương/mục tiêu: 50% Công x1.08 = 54". */
    private static Line dmg(boolean spell, double coef, double mult, double power, int hits) {
        String stat = (spell ? t("hukhong.profile.stat.spell_power", "Thần Thức") : t("hukhong.profile.stat.attack_power", "Công")).getString();
        String formula = pc(coef) + " " + stat + (mult > 1.0001 ? " x" + fmt(mult) : "");
        double v = coef * mult * power;
        if (hits <= 1) return l(t("hukhong.skills.dmg", "Sát thương/mục tiêu: %s = %s", formula, num(v)), DMG);
        return l(t("hukhong.skills.dmg_multi", "Sát thương/mục tiêu: %s = %s x %s đòn = %s", formula, num(v), hits, num(v * hits)), DMG);
    }

    private static Line note(String key, String vi, Object... a) { return l(t(key, vi, a), DESC); }

    // ------------------------------------------------------------------ đòn thường (Linh Trượng / Linh Bảo)

    private static Entry basicAttack(Ctx x, Path p) {
        List<Line> ls = new ArrayList<>();
        int stage = x.stageOf(p);
        switch (p) {
            case PHAP -> {
                float bolt = LinhTruongMath.boltDamage(x.sp(), stage);
                ls.add(note("hukhong.skills.truong.desc", "Cầm Linh Trượng, bấm chuột trái: bắn tia phép tầm %s block, hồi %ss.",
                        fmt(LinhTruongMath.RANGE), fmt(LinhTruongMath.COOLDOWN_TICKS / 20.0)));
                ls.add(l(t("hukhong.skills.truong.dmg", "Sát thương: %s Thần Thức x %s (hệ số) x %s (phẩm) = %s mỗi tia, ~%s mỗi giây",
                        num(x.sp()), fmt(LinhTruongMath.coef(stage)), fmt(LinhTruongMath.quality(stage)), num(bolt),
                        num(bolt * 20.0 / LinhTruongMath.COOLDOWN_TICKS)), DMG));
                return new Entry("", t("hukhong.skills.truong.name", "Linh Trượng (đòn thường)"), ls);
            }
            case DAO, THE, MA -> {
                var lb = com.hukhong.linhbao.LinhBaoType.of(p);
                if (lb == null) return null;
                for (Component c : com.hukhong.linhbao.LinhBaoText.lines(lb, stage, x.ap(), x.sp())) ls.add(l(c, DMG));
                return new Entry("", com.hukhong.linhbao.LinhBaoText.name(lb), ls);
            }
            default -> {
                return null;
            }
        }
    }

    // ------------------------------------------------------------------ kỹ năng nền

    private static Entry baseEntry(Ctx x, Path page, String key, Skill s) {
        if (s.path == Path.PHAP) return phapEntry(x, key, s);
        int lvl = x.book().level(s.icon);
        double m = SkillBookMath.dmgMult(lvl);
        List<Line> ls = new ArrayList<>();
        int cd = cdTicks(x, page, s.icon, s.cooldownTicks);
        Component name = nameWithLevel(Component.translatable(s.langKey()), lvl);
        switch (s) {
            case KIEM_1 -> {
                ls.add(note("hukhong.skills.kiem_1.desc", "Kiếm khí xuyên tầm 8 block (Kiếm Ý Cốt +25%), trúng mọi mục tiêu trên đường."));
                ls.add(meta(x, page, s.cost, cd));
                ls.add(dmg(false, Skill.KIEM_1_COEF, m, x.ap(), 1));
            }
            case KIEM_2 -> {
                ls.add(note("hukhong.skills.kiem_2.desc", "3 nhát chém quạt tầm 6 block, cách nhau 0,3 giây; mỗi nhát trúng mọi mục tiêu trong quạt."));
                ls.add(meta(x, page, s.cost, cd));
                ls.add(dmg(false, Skill.KIEM_2_COEF, m, x.ap(), 3));
            }
            case KIEM_ULT -> {
                ls.add(note("hukhong.skills.kiem_ult.desc", "Bật/tắt (phím T): lĩnh vực bán kính 5 block quanh bạn, mỗi %s giây chém mọi kẻ địch trong vùng và rơi mưa kiếm.",
                        fmt(SkillEffects.DOMAIN_PULSE_TICKS / 20.0)));
                ls.add(l(t("hukhong.skills.kiem_ult.meta", "Tốn %s%% %s tối đa mỗi giây (~%s/s) · khóa bấm %ss",
                        fmt(SkillEffects.DOMAIN_DRAIN_PCT * 100), resName(page), Math.max(1, (int) Math.round(x.resMax() * SkillEffects.DOMAIN_DRAIN_PCT)),
                        fmt(SkillEffects.DOMAIN_LOCK_TICKS / 20.0)), META));
                Line one = dmg(false, Skill.KIEM_ULT_COEF, m, x.ap(), 1);
                ls.add(one);
                double per = Skill.KIEM_ULT_COEF * m * x.ap();
                ls.add(l(t("hukhong.skills.kiem_ult.dps", "Mỗi giây: %s nhịp = %s",
                        20 / SkillEffects.DOMAIN_PULSE_TICKS, num(per * 20.0 / SkillEffects.DOMAIN_PULSE_TICKS)), DMG));
            }
            case MA_1 -> {
                ls.add(note("hukhong.skills.ma_1.desc", "3 vệt móng cận chiến tầm 4 block (quạt); Hút Huyết nhân đôi trên đòn này."));
                ls.add(meta(x, page, s.cost, cd));
                ls.add(dmg(false, Skill.MA_1_COEF, m, x.ap(), 1));
            }
            case MA_2 -> {
                ls.add(note("hukhong.skills.ma_2.desc", "Lá chắn bằng 15%% máu tối đa trong 6 giây (chỉ bản thân): %s.", num(x.maxHp() * 0.15)));
                ls.add(meta(x, page, s.cost, cd));
            }
            case MA_ULT -> {
                ls.add(note("hukhong.skills.ma_ult.desc", "12 giây: +40%% Công (= +%s), +4%% Hút Huyết, Ác Tính +15.", num(x.ap() * 0.40)));
                ls.add(meta(x, page, s.cost, cd));
            }
            case DAO_1 -> {
                ls.add(note("hukhong.skills.dao_1.desc", "Tia sáng tầm 14 block đánh 1 mục tiêu (nội tại Đạo Tu x%s).", fmt(PassiveMath.DAO_SKILL_MULT)));
                ls.add(meta(x, page, s.cost, cd));
                ls.add(l(t("hukhong.skills.dmg", "Sát thương/mục tiêu: %s = %s",
                        pc(Skill.DAO_1_COEF) + " x" + fmt(PassiveMath.DAO_SKILL_MULT) + " "
                                + t("hukhong.profile.stat.spell_power", "Thần Thức").getString() + (m > 1.0001 ? " x" + fmt(m) : ""),
                        num(baseHit(s, x.ap(), x.sp(), lvl))), DMG));
            }
            case DAO_2 -> {
                ls.add(note("hukhong.skills.dao_2.desc", "Đồng đội gần nhất trong 20 block nhận Sức Mạnh, Tốc Độ, Hấp Thụ II 20 giây; không có thì bản thân nhận Tốc Độ + Hấp Thụ."));
                ls.add(meta(x, page, s.cost, cd));
            }
            case DAO_ULT -> {
                ls.add(note("hukhong.skills.dao_ult.desc", "Vùng 12 block: người chơi được hồi %s máu (bản thân %s), Hồi Phục II 10 giây, Hấp Thụ 30 giây; đồng đội thêm Sức Mạnh + Tốc Độ.",
                        num(Math.max(4, x.sp() * 0.6)), num(Math.max(4, x.sp() * 0.8))));
                ls.add(meta(x, page, s.cost, cd));
            }
            case THE_1 -> {
                ls.add(note("hukhong.skills.the_1.desc", "Dậm đất bán kính 4 block, hất tung kẻ địch."));
                ls.add(meta(x, page, s.cost, cd));
                ls.add(dmg(false, Skill.THE_1_COEF, m, x.ap(), 1));
            }
            case THE_2 -> {
                ls.add(note("hukhong.skills.the_2.desc", "Giảm ~20%% sát thương nhận trong 6 giây."));
                ls.add(meta(x, page, s.cost, cd));
            }
            case THE_ULT -> {
                ls.add(note("hukhong.skills.the_ult.desc", "8 giây: giảm ~40%% sát thương nhận, khiên Hấp Thụ III, gỡ Làm Chậm và Suy Yếu."));
                ls.add(meta(x, page, s.cost, cd));
            }
            default -> ls.add(meta(x, page, s.cost, cd));
        }
        return new Entry(key, name, ls);
    }

    // ------------------------------------------------------------------ Bản Mệnh (phím R)

    private static Entry rEntry(Ctx x, Path page) {
        BanMenhSkill bm = BanMenhSkill.of(page, x.level());
        List<Line> ls = new ArrayList<>();
        if (bm != null) {
            double c = bm.coef(x.level()), range = bm.range(x.level());
            Component name = Component.translatable(bm.langKey());
            switch (bm) {
                case NHAT_KIEM -> {
                    ls.add(note("hukhong.skills.bm_kiem.desc", "Lướt thẳng %s block về phía trước, chém mọi mục tiêu trên đường.", fmt(range)));
                    ls.add(meta(x, page, bm.cost, bm.cooldownTicks));
                    ls.add(dmg(false, c, 1, x.ap(), 1));
                }
                case PHAN_XET -> {
                    ls.add(note("hukhong.skills.bm_dao.desc", "Tia phán quyết tầm %s block vào 1 mục tiêu; gấp đôi sát thương lên Hư Hóa.", fmt(range)));
                    ls.add(meta(x, page, bm.cost, bm.cooldownTicks));
                    ls.add(dmg(true, c, 1, x.sp(), 1));
                    ls.add(l(t("hukhong.skills.bm_dao.huhoa", "Lên Hư Hóa: %s", num(c * x.sp() * 2)), DMG));
                }
                case DIA_LIET -> {
                    ls.add(note("hukhong.skills.bm_the.desc", "Dậm đất bán kính %s block: hất tung và làm chậm mạnh %s giây.", fmt(range),
                            fmt(Math.min(80, 30 + (x.level() - BanMenhSkill.UNLOCK_LEVEL)) / 20.0)));
                    ls.add(meta(x, page, bm.cost, bm.cooldownTicks));
                    ls.add(dmg(false, c, 1, x.ap(), 1));
                }
                case HUYET_HAI -> {
                    ls.add(note("hukhong.skills.bm_ma.desc", "Vùng huyết bán kính %s block, 6 nhịp mỗi giây; hồi máu bằng 50%% sát thương (tối đa 5%% máu tối đa mỗi nhịp).", fmt(range)));
                    ls.add(meta(x, page, bm.cost, bm.cooldownTicks));
                    ls.add(dmg(false, c, 1, x.ap(), 6));
                }
            }
            return new Entry("R", name, ls);
        }
        if (x.bones() > 0) return resonance(x, page, "R");
        return null;
    }

    private static Entry resonance(Ctx x, Path page, String key) {
        List<Line> ls = new ArrayList<>();
        int n = x.bones();
        double coef = HonCotMath.resonanceCoef(n), pow = Math.max(x.ap(), x.sp());
        ls.add(note("hukhong.skills.bm_bone.desc", "Bộc phát bán kính %s block theo số Hồn Cốt đang mang (%s); Shift+R khi đã có Bản Mệnh.", 5 + n, n));
        ls.add(meta(x, page, 50, 900));
        ls.add(l(t("hukhong.skills.bm_bone.dmg", "Sát thương/mục tiêu: %s x max(Công, Thần Thức) = %s", fmt(coef), num(coef * pow)), DMG));
        return new Entry(key, Component.translatable("hukhong.skill.bm_bone"), ls);
    }

    // ------------------------------------------------------------------ bí tịch / công pháp

    private static Entry bookEntry(Ctx x, Path page, String key, BookSkill b) {
        int lvl = x.book().level(b.id);
        double m = SkillBookMath.dmgMult(lvl);
        double power = b.spell() ? x.sp() : x.ap();
        List<Line> ls = new ArrayList<>();
        ls.add(l(Component.translatableWithFallback(b.descKey(), b.viDesc), DESC));
        ls.add(meta(x, page, b.cost, cdTicks(x, page, b.id, b.cooldownTicks)));
        if (x.level() < b.minLevel()) ls.add(l(t("hukhong.skills.need_level", "Cần cấp %s để dùng (hiện cấp %s).", b.minLevel(), x.level()), WARN));
        int hits = switch (b) {
            case PHI_KIEM_TRUY_HON -> 4;
            case KIEM_VU_HOA -> 6;
            case QUANG_TRAN -> 8;
            case HUYET_TE_DAI -> 10;
            default -> 1;
        };
        switch (b) {
            case THANH_TAM_CHU -> ls.add(l(t("hukhong.skills.heal", "Hồi: %s", num(Math.max(4, b.coef * m * x.sp()))), DMG));
            case LANG_BA_VI_BO -> { }
            case HO_THAN_CUONG_KHI -> ls.add(l(t("hukhong.skills.shield", "Lá chắn: %s", num(x.maxHp() * b.coef * m)), DMG));
            case HOI_XUAN_THUAT -> ls.add(l(t("hukhong.skills.heal", "Hồi: %s", num(x.maxHp() * b.coef * m)), DMG));
            default -> {
                ls.add(dmg(b.spell(), b.coef, m, power, hits));
                if (b == BookSkill.QUANG_TRAN)
                    ls.add(l(t("hukhong.skills.heal_pulse", "Hồi mỗi nhịp: %s", num(Math.max(1, 0.15 * m * x.sp()))), DMG));
                if (b == BookSkill.HUYET_TE_CHU)
                    ls.add(l(t("hukhong.skills.heal_cap", "Hồi tối đa: %s", num(Math.min(0.30 * b.coef * m * power, x.maxHp() * 0.05))), DMG));
                if (b == BookSkill.HUYET_TE_DAI)
                    ls.add(l(t("hukhong.skills.heal_cap", "Hồi tối đa: %s", num(Math.min(0.30 * b.coef * m * power * hits, x.maxHp() * 0.03))), DMG));
                if (b == BookSkill.THON_THIEN_MA_CONG)
                    ls.add(l(t("hukhong.skills.heal", "Hồi: %s", num(x.maxHp() * 0.12 * Math.min(m, 1.5))), DMG));
            }
        }
        return new Entry(key, nameWithLevel(Component.translatableWithFallback(b.langKey(), b.viName), lvl), ls);
    }

    // ------------------------------------------------------------------ Pháp Tu

    private static final String[][] PHAP_NAMES = {
            {"Quang Tiễn", "Thiểm Bộ", "Quang Vũ", "Tinh Hà Quang Trụ"},
            {"Lôi Châu", "Lôi Trận", "Thiên Lôi Liên", "Tinh Hà Quang Trụ"},
            {"Hỏa Cầu", "Liệt Diễm Hoàn", "Hỏa Long", "Tinh Hà Quang Trụ"},
            {"Thủy Tiễn", "Cam Lộ", "Thủy Bích", "Tinh Hà Quang Trụ"},
            {"Thạch Mâu", "Thổ Tháp", "Địa Chấn", "Thiên Sơn Áp Đỉnh"},
            {"Kim Nhận", "Kim Giáp", "Kim Quang Phá", "Thiên Kim Trảm"},
    };

    private static Entry phapEntry(Ctx x, String key, Skill s) {
        PhapElement el = x.element();
        if (el == null) { // chưa biết nguyên tố (chưa đồng bộ): chỉ nêu phím
            return new Entry(key, Component.translatable(s.langKey()), List.of(meta(x, Path.PHAP, s.cost, s.cooldownTicks)));
        }
        int slot = s.slot;
        int lvl = x.book().level(s.icon);
        double m = SkillBookMath.dmgMult(lvl);
        double sp = x.sp(), coef = PhapMath.coef(el, slot);
        Aptitude apt = x.aptOf(Path.PHAP);
        int cd = cdTicks(x, Path.PHAP, s.icon, s.cooldownTicks);
        Component name = nameWithLevel(t("hukhong.phap.cast." + el.id() + "." + slot, PHAP_NAMES[el.ordinal()][slot]), lvl);
        List<Line> ls = new ArrayList<>();
        String ek = "hukhong.skills.phap." + el.id() + "." + slot;
        if (slot == 3 && PhapBeamMath.locksT(el)) {
            double pulse = beamPulse(el, apt, sp, lvl);
            ls.add(note("hukhong.skills.beam.desc", "Giữ T nạp %ss, rồi giữ chuột phải để kênh tia: tầm %s block, rộng %s, tối đa %ss. Nạp làm chậm %s%%, kênh làm chậm %s%%.",
                    fmt(PhapBeamMath.CHARGE_TICKS / 20.0), fmt(PhapBeamMath.RANGE), fmt(PhapBeamMath.RADIUS * 2),
                    fmt(PhapBeamMath.FIRE_MAX_TICKS / 20.0), fmt(-PhapBeamMath.CHARGE_SLOW * 100), fmt(-PhapBeamMath.FIRE_SLOW * 100)));
            int up = PhapBeamMath.upfrontCost(x.resMax(), 1.0);
            ls.add(l(t("hukhong.skills.beam.meta", "Tốn %s (%s%% %s tối đa) khi bắn + %s/s (%s%%/s) · Hồi chiêu %ss",
                    up, fmt(PhapBeamMath.UPFRONT_PCT * 100), resName(Path.PHAP), num(PhapBeamMath.drainPerTick(x.resMax(), 1.0) * 20),
                    fmt(PhapBeamMath.DRAIN_PCT_PER_SEC * 100),
                    fmt(Math.round(SkillBookMath.cooldown(PhapBeamMath.COOLDOWN_TICKS, lvl) * PhapMath.cooldownMult(el)) / 20.0)), META));
            double ratio = PhapBeamMath.pulseCoef(el) * PhapBeamMath.aptMult(apt);
            int n = PhapBeamMath.pulses(PhapBeamMath.FIRE_MAX_TICKS);
            ls.add(l(t("hukhong.skills.beam.dmg", "Sát thương/mục tiêu mỗi nhịp (%ss): %s%% Thần Thức%s = %s; kênh đủ %s nhịp = %s",
                    fmt(PhapBeamMath.PULSE_TICKS / 20.0), fmt(Math.round(ratio * 10000) / 100.0), m > 1.0001 ? " x" + fmt(m) : "",
                    num(pulse), n, num(pulse * n)), DMG));
            return new Entry(key, name, ls);
        }
        ls.add(note(ek + ".desc", PHAP_DESC[el.ordinal()][slot]));
        if (el == PhapElement.QUANG && slot == 1) { // Thiểm Bộ (giữ E: chạy / bấm nhanh: lùi + Bá Thể)
            ls.add(l(t("hukhong.skills.thiem_bo.meta", "Giữ E: +%s%% tốc độ, tốn %s%% %s tối đa/s (~%s/s), cần >= %s%% để bắt đầu. Bấm nhanh: tốn %s, hồi chiêu %ss.",
                    fmt(ThiemBoMath.SPEED_BONUS * 100), fmt(ThiemBoMath.DRAIN_PCT * 100), resName(Path.PHAP),
                    num(ThiemBoMath.drainPerTick(x.resMax(), 1.0) * 20), fmt(ThiemBoMath.MIN_START_PCT * 100),
                    ThiemBoMath.COST * x.costMult(), fmt(ThiemBoMath.COOLDOWN / 20.0)), META));
            ls.add(l(t("hukhong.skills.thiem_bo.dash", "Bấm nhanh: lùi tối đa %s block, rồi Bá Thể miễn nhiễm sát thương %s giây. Không gây sát thương.",
                    fmt(ThiemBoMath.DASH_DIST), fmt(ThiemBoMath.IMMUNE_TICKS / 20.0)), DMG));
            return new Entry(key, name, ls);
        }
        if (el == PhapElement.THO && slot == 1) { // Thổ Tháp: không sát thương trực tiếp; bấm = dựng trụ, giữ = xuyên tường, hai trụ gần nhau hợp thể
            ls.add(meta(x, Path.PHAP, s.cost, cd));
            appendTurret(x, ls, apt);
            ls.add(l(t("hukhong.skills.tho.merge", "Hợp Thể: 2 trụ cùng chủ cách <= %s block gộp thành 1 trụ lớn x%s (máu + thời gian sống cộng dồn), bắn laser 1 mục tiêu: %s%% sát thương trụ thường, tăng tuyến tính tới %s%% sau %ss khoá cùng mục tiêu (đổi mục tiêu thì về lại)",
                    fmt(PhapMath.THO_MERGE_RADIUS), fmt(PhapMath.THO_MERGED_SCALE), fmt(PhapMath.THO_LASER_MIN * 100), fmt(PhapMath.THO_LASER_MAX * 100),
                    fmt(PhapMath.THO_LASER_RAMP_TICKS / 20.0)), DMG));
            ls.add(l(t("hukhong.skills.tho.phase", "Giữ E: xuyên tường %ss (tốn %s, hồi chiêu %ss); hết giờ bị đẩy ra chỗ trống gần nhất. Không dùng được ở Bí Cảnh / đấu trường boss, không xuyên bedrock và rào chắn.",
                    fmt(PhapMath.THO_PHASE_TICKS / 20.0), PhapMath.THO_PHASE_COST * x.costMult(), fmt(PhapMath.THO_PHASE_COOLDOWN_TICKS / 20.0)), META));
            return new Entry(key, name, ls);
        }
        if (el == PhapElement.THO && slot == 3) { // Thiên Sơn Áp Đỉnh: 3 đợt núi rơi + giữ T dẫn nổ
            ls.add(meta(x, Path.PHAP, s.cost, (int) Math.round(PhapMath.THO_T_COOLDOWN_TICKS * PhapMath.cooldownMult(el))));
            double c3 = coef * sp * m;
            ls.add(l(t("hukhong.skills.tho.t.waves", "%s đợt núi rơi (cách %ss), mỗi đợt %s%% sát thương T = %s/mục tiêu, vùng bán kính %s, choáng %ss",
                    PhapMath.THO_T_WAVES, fmt(PhapMath.THO_T_WAVE_GAP / 20.0), fmt(PhapMath.THO_T_WAVE_FRAC * 100), num(c3 * PhapMath.THO_T_WAVE_FRAC),
                    fmt(PhapMath.THO_T_AREA), fmt(PhapMath.THO_MOUNTAIN_STUN)), DMG));
            ls.add(l(t("hukhong.skills.tho.t.blast", "Sau đợt 3, giữ T trong %ss để dẫn nổ: %s%% sát thương T = %s, bán kính %s, rút %s%% Pháp Lực hiện có. Quá %ss không dẫn nổ thì mất.",
                    fmt(PhapMath.THO_T_HOLD_TICKS / 20.0), fmt(PhapMath.THO_T_BLAST_MULT * 100), num(c3 * PhapMath.THO_T_BLAST_MULT),
                    fmt(PhapMath.THO_T_BLAST_RADIUS), fmt(PhapMath.THO_T_MANA_DRAIN * 100), fmt(PhapMath.THO_T_WINDOW_TICKS / 20.0)), DMG));
            ls.add(l(t("hukhong.skills.tho.t.total", "Tổng nếu trúng đủ 3 đợt + nổ: %s%% sát thương T = %s. Phím E và T của Pháp Thổ cố định, không thể thay.",
                    fmt(PhapMath.THO_T_TOTAL * 100), num(c3 * PhapMath.THO_T_TOTAL)), META));
            return new Entry(key, name, ls);
        }
        ls.add(meta(x, Path.PHAP, s.cost, cd));
        double c = coef * sp;
        switch (el) {
            case LOI -> {
                if (slot == 0) {
                    ls.add(dmg(true, coef, m, sp, 1));
                    ls.add(l(t("hukhong.skills.loi.0.side", "Mục tiêu phụ (tối đa 2): %s", num(c * 0.5 * m)), DMG));
                } else if (slot == 1) {
                    ls.add(pulsesLine(coef, sp, 3, 4, 6, lvl));
                } else {
                    double sum = 0, mul = 1;
                    for (int i = 0; i < 6; i++) { sum += c * mul * m; mul *= 0.85; }
                    ls.add(l(t("hukhong.skills.loi.2.dmg", "Sát thương: mục tiêu đầu %s, mỗi bước còn 85%%; chuỗi 6 mục tiêu = %s tổng",
                            num(c * m), num(sum)), DMG));
                }
            }
            case HOA -> {
                ls.add(dmg(true, coef, m, sp, 1));
                if (slot == 0) ls.add(l(t("hukhong.skills.hoa.0.side", "Mục tiêu xung quanh: 60%% = %s", num(c * 0.6 * m)), DMG));
            }
            case KIM -> ls.add(dmg(true, coef, slot == 3 ? winM(14, m) : m, sp, 1)); // Thiên Kim Trảm: nhát đến sau 14 tick (ngoài cửa sổ Cường Hóa)
            case THO -> {
                ls.add(dmg(true, coef, m, sp, 1));

            }
            default -> ls.add(dmg(true, coef, m, sp, 1));
        }
        if (el == PhapElement.THUY && slot == 0) ls.add(l(t("hukhong.skills.heal", "Hồi: %s", num(x.maxHp() * 0.04)), DMG));
        if (el == PhapElement.THUY && slot == 1) ls.add(l(t("hukhong.skills.heal", "Hồi: %s", num(x.maxHp() * 0.25)), DMG));
        return new Entry(key, name, ls);
    }

    /** n đợt chia đều hệ số; đợt đến muộn hơn cửa sổ Cường Hóa (12 tick) không được nhân hệ số. */
    private static Line pulsesLine(double coef, double sp, int n, int first, int step, int lvl) {
        double total = phapPulses(coef, sp, n, first, step, lvl), m = SkillBookMath.dmgMult(lvl);
        int inWin = 0;
        for (int i = 0; i < n; i++) if (first + i * step <= DEFAULT_WINDOW) inWin++;
        String each = pc(coef / n);
        if (m > 1.0001 && inWin < n)
            return l(t("hukhong.skills.pulses_win", "Sát thương/mục tiêu: %s đợt x %s Thần Thức; tổng = %s (Cường Hóa chỉ nhân %s đợt đầu)", n, each, num(total), inWin), DMG);
        return l(t("hukhong.skills.pulses", "Sát thương/mục tiêu: %s đợt x %s Thần Thức; tổng = %s", n, each, num(total)), DMG);
    }

    private static void appendTurret(Ctx x, List<Line> ls, Aptitude apt) {
        int max = PhapMath.thoMaxTurrets(apt);
        ls.add(l(t("hukhong.skills.tho.turret", "Tháp: tối đa %s, %s máu, bắn mỗi %ss %s sát thương (%s%% x %s Thần Thức), tồn tại %ss",
                max, num(PhapMath.thoTurretHp(apt, x.maxHp())), fmt(PhapMath.THO_SHOT_TICKS / 20.0),
                num(PhapMath.thoShotDamage(apt, x.sp())), fmt(PhapMath.thoDamagePct(apt) * 100), fmt(PhapMath.THO_SHOT_COEF),
                PhapMath.THO_LIFE_TICKS / 20), DMG));
    }

    private static final String[][] PHAP_DESC = {
            {"Tia sáng xuyên tầm 16 block, trúng mọi mục tiêu trên đường.",
                    "Giữ E: chạy tia sáng (tốc độ lớn, vệt sét phía sau, tiêu hao Pháp Lực mỗi giây). Bấm nhanh E: lùi về sau rồi nhận Bá Thể (miễn nhiễm sát thương).",
                    "5 tia sáng quạt (lệch 11 độ) tầm 14 block; mỗi mục tiêu chỉ trúng một lần.",
                    ""},
            {"Giáng sét xuống điểm ngắm (tầm 18); lan sang tối đa 2 mục tiêu quanh đó (5 block).",
                    "Vùng sét bán kính 5 tại điểm ngắm (tầm 14): 3 đợt, choáng 0,5 giây ở đợt đầu.",
                    "Sét nhảy chuỗi tối đa 6 mục tiêu (mỗi bước trong 6 block), choáng 0,4 giây mỗi mục tiêu.",
                    ""},
            {"Cầu lửa nổ tại điểm ngắm (tầm 18), bán kính 2,6; thiêu 3 giây.",
                    "Vòng lửa bán kính 6 quanh bạn: thiêu 4 giây, đẩy lùi.",
                    "Rồng lửa xoắn dài 14 block, thiêu 4 giây.",
                    ""},
            {"Tia nước 16 block vào mục tiêu đầu tiên, hồi cho bản thân 4% máu tối đa.",
                    "Hồi 25% máu tối đa cho người chơi trong 8 block (kèm Hồi Phục II 5 giây); gây sát thương kẻ địch bán kính 6.",
                    "Khiên nước cho người chơi trong 8 block (Hấp Thụ II 8 giây, giảm sát thương 6 giây); đẩy lùi và gây sát thương kẻ địch bán kính 5.",
                    ""},
            {"Gai đá dọc đường dài 10 block.",
                    "Bấm: dựng tháp, 2 tháp gần nhau hợp thể thành trụ laser. Giữ: xuyên tường 5 giây.",
                    "Địa chấn bán kính 7: làm chậm 3 giây, hất lên.",
                    "Mưa núi đá 3 đợt (mỗi đợt 60% sát thương T), rồi giữ T dẫn nổ 175% và rút 99% Pháp Lực."},
            {"Lưỡi kim bay 14 block, trúng mọi mục tiêu trên đường.",
                    "Giáp Kim 8 giây (giảm sát thương + Hấp Thụ II); lưỡi phản chấn bán kính 4.",
                    "Chém quạt lớn tầm 6,5 block.",
                    "Sau 0,7 giây, lưỡi kiếm khổng lồ giáng xuống điểm ngắm (tầm 14), bán kính 6; giảm sát thương 4 giây."},
    };
}
