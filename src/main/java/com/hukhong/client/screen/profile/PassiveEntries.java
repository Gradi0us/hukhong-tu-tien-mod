package com.hukhong.client.screen.profile;

import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.passive.PassiveMath;
import com.hukhong.pathrules.MaRules;
import com.hukhong.pathrules.PathRules;
import com.hukhong.pathrules.TheBody;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapMath;
import com.hukhong.phaptu.linhtruong.LinhTruongMath;
import com.hukhong.stats.StatService;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mô tả nội tại từng đạo (tab Nội Tại màn K). Thuần dữ liệu: số lấy từ PassiveMath / PhapMath / MaRules... theo tư chất và cấp. */
public final class PassiveEntries {
    private PassiveEntries() {}

    private static Component t(String key, String vi, Object... a) { return Component.translatableWithFallback(key, vi, a); }

    static String fmt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    static String pc(double v) { return fmt(v * 100) + "%"; }

    private static Component[] e(Component name, Component desc) { return new Component[]{name, desc}; }

    /** element chỉ dùng cho Pháp (null -> chỉ nội tại chung). */
    public static List<Component[]> of(Path p, Aptitude a, int lv, PhapElement element) {
        List<Component[]> l = new ArrayList<>();
        switch (p) {
            case THE -> {
                l.add(e(t("hukhong.passive.kg.name", "Kiên Giáp"),
                        t("hukhong.passive.kg.desc", "Mỗi %s đòn trúng cộng 1 bậc: +%s Giáp, +%s Sinh Lực tối đa mỗi bậc; tối đa %s bậc (%s Giáp, %s Sinh Lực). Mất sau %ss không đánh.",
                                PassiveMath.THE_PER_TIER, fmt(PassiveMath.theArmor(a, 3)), fmt(PassiveMath.theMaxHp(a, 3)),
                                PassiveMath.theTiers(PassiveMath.THE_STACK_CAP), fmt(PassiveMath.theArmor(a, PassiveMath.THE_STACK_CAP)),
                                fmt(PassiveMath.theMaxHp(a, PassiveMath.THE_STACK_CAP)), PassiveMath.THE_HOLD_TICKS / 20)));
                l.add(e(t("hukhong.passive.tt.name", "Trọng Thương"),
                        t("hukhong.passive.tt.desc", "Mỗi đòn đánh thường (không phải kỹ năng) cộng 1 tầng lên kẻ địch, tối đa %s tầng, %ss. Mỗi tầng: kẻ địch nhận thêm %s sát thương từ mọi nguồn. Kẻ địch có tầng mà còn dưới %s máu tối đa bị xử tử ngay. Boss không bị xử tử, bonus sát thương chỉ còn 1/3. Không tác dụng lên người chơi.",
                                PassiveMath.TT_MAX_STACKS, PassiveMath.TT_TICKS / 20, pc(PassiveMath.ttAmpPerStack(a)), pc(PassiveMath.ttExecPct(a)))));
                l.add(e(t("hukhong.passive.hk.name", "Huyết Khí"),
                        t("hukhong.passive.hk.desc", "Đánh thường trúng %s đòn: hồi máu bằng %s tổng sát thương của %s đòn đó. Hồi chiêu %ss.",
                                PassiveMath.HK_HITS, pc(PassiveMath.hkHealPct(a)), PassiveMath.HK_HITS, PassiveMath.hkCooldown(a) / 20)));
                l.add(e(t("hukhong.passive.baky.name", "Bá Khí Quyền"),
                        t("hukhong.passive.baky.desc", "Tay không: sát thương = Công x (1 + %s x Bá Khí%%); cầm vũ khí thì đòn thường = 0; không dùng được cung/nỏ. Bá Khí tự hồi %s/s, +%s mỗi đòn trúng, +%s/+%s khi bị đánh (nhẹ/nặng).",
                                fmt(com.hukhong.gates.DamageRules.FIST_AURA_COEF), PathRules.THE_PASSIVE_REGEN, PathRules.THE_MELEE_GAIN,
                                PathRules.THE_HIT_SMALL, PathRules.THE_HIT_BIG)));
                l.add(e(t("hukhong.passive.body.name", "Thân Thể Thể Tu"),
                        t("hukhong.passive.body.desc", "Đi trên mặt nước (giữ Shift để lặn). Nín thở lâu gấp %s lần. Chạy nhanh +%s, nhảy cao +%s (theo cấp %s). Chịu độ cao rơi thêm %s khối.",
                                TheBody.BREATH_MULT, pc(TheBody.speedBonus(lv)), pc(TheBody.jumpBonus(lv)), lv, fmt(TheBody.SAFE_FALL))));
            }
            case KIEM -> {
                l.add(e(t("hukhong.passive.kiem.name", "Bản Mệnh Kiếm"),
                        t("hukhong.passive.kiem.desc", "Mỗi %s đòn trúng, kiếm giáng từ trời gây sát thương bạo kích chắc chắn và hồi %s sát thương gây ra.",
                                PassiveMath.KIEM_EVERY, pc(PassiveMath.kiemHealPct(a)))));
                l.add(e(t("hukhong.passive.kiem2.name", "Kiếm Quét"),
                        t("hukhong.passive.kiem2.desc", "Tầm quét của Bản Mệnh Kiếm +%s; kẻ địch trong cung quét nhận 50%% đòn gốc. Mỗi đòn trúng hồi %s Linh Lực tối đa (tối đa 1 lần / 2s).",
                                pc(PassiveMath.kiemSweepBonus(a)), pc(PathRules.KIEM_HIT_MANA_PCT))));
                l.add(e(t("hukhong.passive.kiem3.name", "Kiếm Ý Hồi Linh"),
                        t("hukhong.passive.kiem3.desc", "Linh Lực tự hồi %s/s khi giao chiến, %s/s khi nghỉ (nhân tốc độ hồi).",
                                fmt(PathRules.KIEM_REGEN_COMBAT), fmt(PathRules.KIEM_REGEN_IDLE))));
            }
            case DAO -> {
                l.add(e(t("hukhong.passive.dao.name", "Đạo Vận"),
                        t("hukhong.passive.dao.desc", "Mỗi %s đòn: hồi %s Sinh Lực tối đa và gây thêm %s máu tối đa của mục tiêu (boss chỉ 1/3). Kỹ năng gây sát thương x%s.",
                                PassiveMath.DAO_EVERY, pc(PassiveMath.daoPct(a)), pc(PassiveMath.daoPct(a)), fmt(PassiveMath.DAO_SKILL_MULT))));
                l.add(e(t("hukhong.passive.dao2.name", "Tịch Cốc"),
                        t("hukhong.passive.dao2.desc", "Không bao giờ đói. Thần Thức tự hồi %s/s, thiền (Shift đứng yên) %s/s. Đòn thường lên quái = 0, chỉ kỹ năng gây sát thương.",
                                PathRules.DAO_REGEN, PathRules.DAO_MEDITATE_REGEN)));
            }
            case MA -> {
                l.add(e(t("hukhong.passive.ma.name", "Tia Ma"),
                        t("hukhong.passive.ma.desc", "Tay không bắn tia ma (tầm %s block), nổ hình quạt: làm chậm và cộng 1 tầng. Đủ %s tầng kích nổ %s máu tối đa (boss chỉ 1/3). Làm chậm tối đa %s.",
                                fmt(PassiveMath.MA_RANGE), PassiveMath.MA_EVERY, pc(PassiveMath.maPct(a)), pc(PassiveMath.MA_SLOW_MAX))));
                double ls = StatService.compute(lv, java.util.EnumSet.of(Path.MA), new EnumMap<>(Map.of(Path.MA, a))).lifesteal();
                l.add(e(t("hukhong.passive.ma2.name", "Hấp Huyết"),
                        t("hukhong.passive.ma2.desc", "Hút máu %s sát thương gây ra ở cấp %s (hồi tối đa 4%% Sinh Lực mỗi đòn). Mỗi đòn trúng +1 Ô Uế Lực (tối đa +10/s); Ô Uế Lực tan -2/s ngoài giao chiến.",
                                pc(ls), lv)));
                l.add(e(t("hukhong.passive.ma3.name", "Huyết Tế & Ác Tính"),
                        t("hukhong.passive.ma3.desc", "Lộ Hấp Thu: giết sinh vật cộng Huyết Tế (+%s Công, +%s hút máu mỗi điểm, EXP x1,45) nhưng cộng Ác Tính. Ác Tính 30/60/80: +%s/+%s/+%s Công và +%s/+%s/+%s hút máu. Từ 80 có thể Mất Kiểm Soát (+25%% Công, mất hút máu, tự đánh); 95 đếm ngược 8s rồi Tẩu Hỏa Nhập Ma (phế tu). Trần cộng thêm: Công %s (đa đạo %s), hút máu %s.",
                                pc(0.005), pc(0.001), pc(MaRules.stateAtk(30)), pc(MaRules.stateAtk(60)), pc(MaRules.stateAtk(80)),
                                pc(MaRules.stateHh(30)), pc(MaRules.stateHh(60)), pc(MaRules.stateHh(80)),
                                pc(MaRules.ATK_CAP_SOLO), pc(MaRules.ATK_CAP_MULTI), pc(MaRules.HH_BONUS_CAP))));
            }
            case PHAP -> {
                l.add(e(t("hukhong.passive.phap.name", "Pháp Lực Tinh Thuần"),
                        t("hukhong.passive.phap.desc", "Pháp Lực tự hồi %s/s, thiền %s/s. Mọi kỹ năng Q/E/R/T tính theo Phép lực. Linh Trượng bản mệnh: chuột trái bắn tia phép (tầm %s block, hồi %ss) gây %s Phép lực ở Phàm phẩm, tối đa %s ở Thần phẩm; kỹ năng phát ra từ đầu trượng.",
                                PathRules.PHAP_REGEN, PathRules.PHAP_MEDITATE_REGEN, fmt(LinhTruongMath.RANGE),
                                fmt(LinhTruongMath.COOLDOWN_TICKS / 20.0), pc(LinhTruongMath.boltDamage(1, 0)),
                                pc(LinhTruongMath.boltDamage(1, LinhTruongMath.MAX_STAGE)))));
                if (element != null) l.add(element(element, a));
                else l.add(e(t("hukhong.passive.phap.noel", "Nguyên tố"), t("hukhong.passive.phap.noel.desc", "Chưa có nguyên tố (sẽ gán khi vào thế giới).")));
            }
        }
        return l;
    }

    private static Component[] element(PhapElement el, Aptitude a) {
        Component name = t("hukhong.passive.el." + el.id() + ".name", switch (el) {
            case QUANG -> "Quang Mang";
            case LOI -> "Lôi Ấn";
            case HOA -> "Thiêu Nhiên";
            case THUY -> "Thủy Ấn";
            case THO -> "Thổ Tháp";
            case KIM -> "Kim Cương";
        }).copy().withColor(el.rgb);
        Component desc = switch (el) {
            case QUANG -> t("hukhong.passive.el.quang.desc", "Mỗi đòn trúng cộng 1 tầng (tối đa %s, mất sau 8s không đánh). Đủ tầng: nổ %s tổng sát thương đã gây trong các đòn đó lên kẻ địch quanh mục tiêu (4 block), rồi %ss tới mỗi phím Q/E/R/T dùng được 1 lần miễn tiêu hao.",
                    PhapMath.QUANG_STACKS, pc(PhapMath.quangExplode(a)), PhapMath.QUANG_FREE_WINDOW_TICKS / 20);
            case LOI -> t("hukhong.passive.el.loi.desc", "Mỗi đòn trúng cộng 1 tầng (tối đa %s, mất sau 6s). Đủ tầng: nổ sét %s máu tối đa mục tiêu (trần %sx Phép lực) và làm choáng %ss (boss thế giới chỉ nửa thời gian).",
                    PhapMath.LOI_STACKS, pc(PhapMath.loiHpPct(a)), fmt(PhapMath.LOI_DMG_CAP_MULT), fmt(PhapMath.loiStunSec(a)));
            case HOA -> t("hukhong.passive.el.hoa.desc", "Mỗi đòn trúng cộng 1 tầng cháy (tối đa %s, %ss): %s Phép lực/giây mỗi tầng. Đủ %s tầng: mỗi %ss lây 2 tầng cho tối đa 3 kẻ địch trong 4 block.",
                    PhapMath.HOA_STACKS, PhapMath.HOA_DURATION_TICKS / 20, fmt(PhapMath.hoaBurnPerSecond(a, 1, 1.0) * 100) + "%",
                    PhapMath.HOA_STACKS, PhapMath.HOA_SPREAD_TICKS / 20);
            case THUY -> t("hukhong.passive.el.thuy.desc", "Đánh trúng hoặc được hồi máu cộng tầng (tối đa %s). Mỗi 5 tầng miễn thương thêm (tối đa %s ở 15 tầng). Khi giao chiến, xung nước mỗi %ss hồi nhẹ đồng đội; đủ %s tầng: bùng nổ %s Sinh Lực tối đa của bạn (trần %sx Phép lực) trong %s block.",
                    PhapMath.THUY_MAX_STACKS, pc(PhapMath.thuyImmunity(a, PhapMath.THUY_MAX_STACKS)), fmt(PhapMath.THUY_PULSE_TICKS / 20.0),
                    PhapMath.THUY_MAX_STACKS, pc(PhapMath.THUY_HP_PCT_MAX * PhapMath.frac(a)), fmt(PhapMath.THUY_DMG_CAP_MULT), fmt(PhapMath.THUY_RADIUS));
            case THO -> t("hukhong.passive.el.tho.desc", "Tháp Thổ tối đa %s tháp cùng lúc, mỗi tháp tồn tại %ss, gây %s sát thương của bạn, có %s Sinh Lực tối đa của bạn và thu hút kẻ địch trong %s block.",
                    PhapMath.thoMaxTurrets(a), PhapMath.THO_LIFE_TICKS / 20, pc(PhapMath.thoDamagePct(a)), pc(PhapMath.thoHpPct(a)), PhapMath.THO_TAUNT_RANGE);
            case KIM -> t("hukhong.passive.el.kim.desc", "Công, Phép lực và Phòng +%s. Đổi lại hồi chiêu kỹ năng x%s và mọi buff tốc độ (trừ chạy bộ) chỉ còn %s hiệu lực.",
                    pc(PhapMath.kimBonus(a)), fmt(PhapMath.KIM_COOLDOWN_MULT), pc(PhapMath.KIM_SPEED_EFFECT));
        };
        return e(name, desc);
    }
}
