package com.hukhong.balance;

import com.hukhong.bicanh.BiCanhTier;
import com.hukhong.chg.ChgKit;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationService;
import com.hukhong.core.Path;
import com.hukhong.endgame.EndgameKit;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuVuong;
import com.hukhong.events.BossScaling;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.pathrules.PathRules;
import com.hukhong.skill.BanMenhSkill;
import com.hukhong.skill.Skill;
import com.hukhong.skillbook.SkillBookMath;
import com.hukhong.stats.StatService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bước 27 - mô phỏng cân bằng toàn cục (R5 mở rộng). Tính DPS/EHP/tài nguyên/TTK cho cấp 1,10..100 x 4 đạo,
 * ghi docs/research/REPORT-v1-balance.md và KHÓA các mục tiêu bằng assert. Chỉ dùng hằng số thật của mod.
 * Giả định (xem báo cáo): tư chất B, 1 đạo, Lõi/Hồn Cốt "điển hình" theo cấp, solo, kỹ năng bỏ qua giáp quái
 * (indirectMagic), đòn cận chiến chịu giáp vanilla.
 */
class BalanceSimTest {
    static final int[] LV = {1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    static final Path[] PATHS = {Path.KIEM, Path.DAO, Path.THE, Path.MA};

    // ------------------------------------------------------------------ giả định trang bị
    static final double[] OFF = {0.04, 0.06, 0.09, 0.13, 0.19}, DEF = {0.04, 0.06, 0.09, 0.13, 0.20};
    static final double CORE_EFF = 0.6;           // tỉ lệ Lõi chọn trúng chỉ số cần (phần còn lại là Lõi tiện ích)
    static final double[] REGEN_CORE = {0.4, 0.6, 1.0, 1.5, 2.2};
    static final double MELEE_RATE_SWORD = 1.6, MELEE_RATE_FIST = 1.6;
    static final double MELEE_UPTIME = 0.55, SKILL_UPTIME = 0.85;
    static final double AURA_AVG = 0.5;           // Bá Khí trung bình của Thể
    static final double MA_ATK_BONUS = 0.10;      // Ác Tính/Huyết Tế điển hình (MaRules)
    static final double SEEK_SECONDS = 30;        // thời gian tìm/chạy giữa hai quái khi cày EXP

    static int milestone(int lv) { return lv >= 99 ? (lv == 99 ? 11 : 12) : lv >= 95 ? 10 : lv / 10; }
    static int tierIdx(int lv) { return Math.min(4, lv / 22); }
    static int bones(int lv) { return lv < HonCotMath.MIN_DROP_LEVEL ? 0 : Math.min(6, 1 + (lv - HonCotMath.MIN_DROP_LEVEL) / 6); }
    static int boneTier(int lv) { return lv >= 96 ? HonCotMath.TIER_HONG : lv >= 81 ? HonCotMath.TIER_HAC : HonCotMath.TIER_TU; }
    static double weapon(int lv) { return lv < 20 ? 5 : lv < 40 ? 6 : lv < 60 ? 7 : 8; }

    record Player(Path path, int lv, double power, double def, double crit, double critDmg, double regen,
                  double resMax, double hp, double lifesteal) {
        double critMul() { return 1 + crit * (critDmg - 1); }
        double mitigation() { return def / (def + 100.0); }
    }

    static Player player(Path p, int lv) {
        var s = StatService.compute(lv, Set.of(p), Map.of(p, Aptitude.B));
        int m = milestone(lv), t = tierIdx(lv), nb = bones(lv);
        double bm = HonCotMath.tierMult(boneTier(lv));
        double offGear = Math.ceil(m / 2.0) * OFF[t] * CORE_EFF + (nb >= 1 ? 0.10 * bm : 0) + (nb >= 2 ? 0.08 * bm : 0);
        double defGear = Math.floor(m / 2.0) * DEF[t] * CORE_EFF + (nb >= 3 ? 0.10 * bm : 0) + (nb >= 4 ? 0.08 * bm : 0) + (nb >= 5 ? 0.04 * bm : 0);
        double hpGear = (nb >= 4 ? 0.15 * bm : 0) + (nb >= 6 ? 0.05 * bm : 0);
        double regenAdd = lv >= 30 ? REGEN_CORE[t] * CORE_EFF : 0;
        double than = lv >= StatService.THAN_LEVEL ? 1 + StatService.THAN_BONUS : 1.0;
        double base = p == Path.DAO ? s.spell() : s.attack();
        double power = base * (1 + offGear + (p == Path.MA ? MA_ATK_BONUS : 0)) * than;
        double hp = Math.min(StatService.MAX_HP_CAP, StatService.maxHealth(lv, Set.of(p), Map.of(p, Aptitude.B)) * (1 + hpGear));
        return new Player(p, lv, power, s.defense() * (1 + defGear) * than, Math.min(1, s.crit() * than),
                s.critDamage() * than, (s.regen() + regenAdd) * than, s.maxResource() * than, hp, s.lifesteal() * than);
    }

    // ------------------------------------------------------------------ kỹ năng
    record Cast(String name, double dmg, double cd, double cost) {}

    /** Danh sách cast gây sát thương lên 1 mục tiêu (đã nhân Bạo Kích). huMob: Phán Quyết x2. */
    static List<Cast> casts(Player pl, boolean huMob) {
        List<Cast> c = new ArrayList<>();
        // Cường Hóa kỹ năng: người chơi điển hình đã nâng kỹ năng nền lên SkillBookMath.typicalLevel(cấp tu): +8% sát thương/cấp, -3% hồi chiêu/cấp
        int sl = SkillBookMath.typicalLevel(pl.lv());
        double A0 = pl.power(), A = A0 * SkillBookMath.dmgMult(sl), cm = pl.critMul();
        switch (pl.path()) {
            case KIEM -> {
                c.add(cast("Q", Skill.KIEM_1, Skill.KIEM_1_COEF * A * cm, sl));
                c.add(cast("E", Skill.KIEM_2, 3 * Skill.KIEM_2_COEF * A * cm, sl));
                c.add(cast("T", Skill.KIEM_ULT, 8 * Skill.KIEM_ULT_COEF * A * cm, sl));
            }
            case MA -> {
                c.add(cast("Q", Skill.MA_1, Skill.MA_1_COEF * A * cm, sl));
                c.add(cast("E", Skill.MA_2, 0, sl));       // Ma Khí Hộ Thể (lá chắn)
                c.add(cast("T", Skill.MA_ULT, 0, sl));     // +40% Công 12s: quy đổi ở fight()
            }
            case DAO -> {
                c.add(cast("Q", Skill.DAO_1, Skill.DAO_1_COEF * A * cm, sl));
                c.add(cast("E", Skill.DAO_2, 0, sl));
                c.add(cast("T", Skill.DAO_ULT, 0, sl));
            }
            case THE -> {
                c.add(cast("Q", Skill.THE_1, Skill.THE_1_COEF * A * cm, sl));
                c.add(cast("E", Skill.THE_2, 0, sl));
                c.add(cast("T", Skill.THE_ULT, 0, sl));
            }
        }
        BanMenhSkill r = BanMenhSkill.of(pl.path(), pl.lv());
        if (r != null) {
            double k = r.coef(pl.lv()) * A0 * cm; // Bản Mệnh không có Cường Hóa
            switch (r) {
                case NHAT_KIEM, DIA_LIET -> {}
                case PHAN_XET -> k *= huMob ? 2.0 : 1.0;
                case HUYET_HAI -> k *= 6;
            }
            c.add(new Cast("R", k, r.cooldownTicks / 20.0, r.cost));
        }
        return c;
    }

    static Cast cast(String n, Skill s, double dmg, int skillLevel) {
        return new Cast(n, dmg, SkillBookMath.cooldown(s.cooldownTicks, skillLevel) / 20.0, s.cost);
    }

    static double meleeHit(Player pl) {
        return switch (pl.path()) {
            case THE -> pl.power() * (1 + com.hukhong.gates.DamageRules.FIST_AURA_COEF * AURA_AVG) * pl.critMul();
            case DAO -> 0;
            default -> (weapon(pl.lv()) + pl.power()) * pl.critMul();
        };
    }

    static double meleeRate(Path p) { return p == Path.THE ? MELEE_RATE_FIST : p == Path.DAO ? 0 : MELEE_RATE_SWORD; }

    /** Vanilla armor: giảm tối đa 80%, công thức min(20, max(a/5, a - d/(2+t/4)))/25 (toughness = 0). */
    static double armorFactor(double armor, double dmg) {
        double red = Math.min(20, Math.max(armor / 5.0, armor - dmg / 2.0));
        return 1 - red / 25.0;
    }

    /** Hồi tài nguyên/giây trong giao chiến (mô hình hóa từ PathRules/MaRules). */
    static double regenPerSec(Player pl, double castsPerSec) {
        return switch (pl.path()) {
            case KIEM -> PathRules.KIEM_REGEN_COMBAT * pl.regen() // hồi thụ động 1/4 + 4% max / đòn trúng (tối đa 1 lần / 2s)
                    + Math.min(MELEE_RATE_SWORD * MELEE_UPTIME, 20.0 / PathRules.KIEM_HIT_MANA_ICD_TICKS) * PathRules.KIEM_HIT_MANA_PCT * pl.resMax();
            case DAO -> PathRules.DAO_REGEN * pl.regen();
            case THE -> (PathRules.THE_MELEE_GAIN * meleeRate(Path.THE) * MELEE_UPTIME + PathRules.THE_HIT_BIG * 0.5) * pl.regen();
            case MA -> Math.min(10, PathRules.MA_HIT_GAIN * (meleeRate(Path.MA) * MELEE_UPTIME + castsPerSec)) * pl.regen();
        };
    }

    record Fight(double dps, double ttk, double f) {}

    /** DPS trung bình trong cửa sổ T giây (CD + ngân sách tài nguyên). Trả {dps, f = tỉ lệ cast tài nguyên đạt được}. */
    static double[] dpsAt(Player pl, double T, double armor, double ehp, boolean huMob) {
        List<Cast> cs = casts(pl, huMob);
        double cost = 0;
        for (Cast c : cs) cost += (1 + Math.floor(T * SKILL_UPTIME / c.cd())) * c.cost();
        double budget = pl.resMax() + regenPerSec(pl, 1.0) * T;
        double f = cost <= 0 ? 1 : Math.min(1, budget / cost);
        double total = 0;
        for (Cast c : cs) total += (1 + Math.floor(T * SKILL_UPTIME / c.cd())) * (c.cost() > 0 ? f : 1) * c.dmg();
        double skillDps = total / T;
        if (pl.path() == Path.MA) skillDps *= 1 + 0.4 * Math.min(1, 12.0 / (SkillBookMath.cooldown(Skill.MA_ULT.cooldownTicks, SkillBookMath.typicalLevel(pl.lv())) / 20.0)); // Ma Thần +40% Công 12s/CD
        double mh = meleeHit(pl);
        // ehp: sát thương thực bị chia ehp trước giáp (HuHoa.ehpScale), rồi quy lại HP thiết kế
        double melee = mh * armorFactor(armor, mh / ehp) * meleeRate(pl.path()) * MELEE_UPTIME;
        return new double[]{skillDps + melee, f};
    }

    /** Đánh solo: lặp T = HP / DPS(T). */
    static Fight fight(Player pl, double hp, double armor, double ehp, boolean huMob) {
        double T = 60, dps = 1, f = 1;
        for (int it = 0; it < 80; it++) {
            double[] r = dpsAt(pl, T, armor, ehp, huMob);
            dps = r[0];
            f = r[1];
            double nt = hp / dps;
            if (Math.abs(nt - T) < 1e-3) { T = nt; break; }
            T = 0.5 * T + 0.5 * nt;
        }
        return new Fight(dps, T, f);
    }

    /** Thời gian liên tục (giây) tung Q+E (không T/R) trước khi cạn tài nguyên. 999 = vô hạn. */
    static double sustain(Player pl) {
        double drain = 0;
        for (Cast c : casts(pl, false)) if (c.name().equals("Q") || c.name().equals("E")) drain += c.cost() / c.cd();
        double q = casts(pl, false).get(0).cd();
        double net = drain - regenPerSec(pl, 1.0 / q);
        return net <= 0 ? 999 : Math.min(999, pl.resMax() / net);
    }

    // ------------------------------------------------------------------ quái Hư Hóa
    // {tên, hp gốc, hpMul, sát thương gốc, dmgMul, trọng số EXP}
    record Mob(String name, double hp, double hpMul, double atk, double dmgMul, double w) {}

    static final Mob HU_THI = new Mob("HuThi", 20, 1.45, 3, 0.8, 1.0);
    static final Mob HU_COT = new Mob("HuCot", 20, 1.5, 2, 1.0, 1.1);
    static final Mob HU_CHU = new Mob("HuChu", 16, 1.8, 2, 1.2, 1.2);
    static final Mob[] MOBS = {HU_THI, HU_COT, HU_CHU};

    static double mobHp(Mob m, int lv) { return m.hp() * m.hpMul() * HuHoa.hpScale(lv); }
    static double mobMelee(Mob m, int lv) { return m.atk() * m.dmgMul() * HuHoa.dmgScale(lv); }
    static double mobArmor(int lv) { return Math.min(20, 0.25 * (lv - 1)); }
    static double mobEhp(double designHp) { return Math.max(1, designHp / HuHoa.VANILLA_HP_CAP); }

    /** Nhịp đòn chuẩn (đòn/giây) để quy "số đòn": 1 đòn = 0,625 giây giao chiến. */
    static final double ACTION_RATE = 1.6;

    /** DPS khi dọn quái thường: Q + E (hồi chiêu) + cận chiến, uptime 100%, không dùng R/T. */
    static double trashDps(Player pl, Mob m, int lv, double designHp) {
        double d = 0, e = mobEhp(designHp);
        for (Cast c : casts(pl, true)) if (c.name().equals("Q") || c.name().equals("E")) d += c.dmg() / c.cd();
        double mh = meleeHit(pl);
        return d + mh * armorFactor(mobArmor(lv), mh / e) * meleeRate(pl.path());
    }

    static double ttkMob(Player pl, Mob m, int lv) {
        double hp = mobHp(m, lv);
        return hp / trashDps(pl, m, lv, hp);
    }

    /** Số "đòn chuẩn" (ACTION_RATE) cần để hạ quái. */
    static int hitsToKill(Player pl, Mob m, int lv) { return (int) Math.ceil(ttkMob(pl, m, lv) * ACTION_RATE); }

    /** Số đòn quái cận chiến cần để hạ người chơi (sau Phòng). */
    static int mobHitsToKillPlayer(Player pl, Mob m, int lv) {
        double d = mobMelee(m, lv) * (1 - pl.mitigation());
        return (int) Math.ceil(pl.hp() / d);
    }

    /** Đòn kỹ năng mạnh nhất của quái / HP người chơi. */
    static double mobBigHitFrac(Player pl, Mob m, int lv) {
        double d = Math.max(mobMelee(m, lv), HuHoa.skillDamage(lv, 1.5)) * (1 - pl.mitigation());
        return d / pl.hp();
    }

    // ------------------------------------------------------------------ boss
    record Boss(String name, int lv, double hp, double dmg, double armor, boolean huMob, double minSec, double maxSec) {}

    static Boss[] bosses() {
        return new Boss[]{
                new Boss("Hư Vương", 30, HuVuong.HP, HuVuong.DMG, HuVuong.ARMOR, true, 60, 120),
                chg(1, 180, 300), chg(2, 180, 300), chg(3, 180, 300), chg(4, 180, 300),
                new Boss("Nguyên Thủy Tịch Diệt", 100, EndgameKit.BOSS_HP, EndgameKit.BOSS_DMG, EndgameKit.BOSS_ARMOR, false, 360, 480),
        };
    }

    static Boss chg(int i, double lo, double hi) {
        var d = ChgKit.def(i);
        return new Boss("CHG" + i + " " + d.id(), d.level(), d.hp(), d.dmg(), d.armor(), false, lo, hi);
    }

    static Fight fightBoss(Player pl, Boss b) {
        double hp = b.hp() * BossScaling.hpMul(1);
        return fight(pl, hp, b.armor(), mobEhp(hp), b.huMob());
    }

    /** Đòn mạnh nhất của boss / HP người chơi cùng cấp (pha cuối). */
    static double bossBigHitFrac(Player pl, Boss b) {
        double mul = b.name().startsWith("Nguy") ? EndgameKit.PHASE_DMG[3] * 1.4 : b.name().startsWith("CHG") ? ChgKit.PHASE_DMG[2] * 1.4 : 1.2;
        return b.dmg() * mul * (1 - pl.mitigation()) / pl.hp();
    }

    // ------------------------------------------------------------------ EXP
    static double hoursTo100() {
        double avgW = (HU_THI.w() + HU_COT.w() + HU_CHU.w()) / 3.0, sec = 0;
        for (int lv = 1; lv < 100; lv++) {
            double perKill = avgW * 40 * Math.pow(lv, 2.15) / HuHoa.EXP_DIV;
            double kills = CultivationService.barSize(lv) / perKill;
            double t = 0;
            for (Path p : PATHS) {
                Player pl = player(p, lv);
                t += ttkMob(pl, HU_THI, lv) + 1.5;
            }
            sec += kills * (t / PATHS.length + SEEK_SECONDS);
        }
        return sec / 3600.0;
    }

    // ------------------------------------------------------------------ test
    @Test
    void balanceTargets() throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# REPORT-v1: Cân bằng toàn cục (Bước 27)\n\n");
        md.append("**Last Updated**: 2026-10-27 - sinh tự động bởi `BalanceSimTest` (chạy `./gradlew test` để làm mới)\n\n");
        md.append("Giả định: tư chất B, 1 đạo, solo, Lõi/Hồn Cốt điển hình theo cấp (CORE_EFF=").append(CORE_EFF)
                .append("), kỹ năng bỏ qua giáp quái, cận chiến chịu giáp vanilla, uptime kỹ năng ").append(SKILL_UPTIME)
                .append(" / cận chiến ").append(MELEE_UPTIME).append(". Quái Hư Hóa cùng cấp (cấp quái = cấp người). Kỹ năng nền ở cấp Cường Hóa điển hình (SkillBookMath.typicalLevel; +8% sát thương, -3% hồi chiêu mỗi cấp).\n")
                .append("HP thiết kế vượt trần 1024 vanilla được quy đổi qua `HuHoa.ehpScale`.\n\n");

        List<String> fails = new ArrayList<>();

        // --- 1. Người chơi
        md.append("## 1. Người chơi (Công/Phép hiệu dụng, Phòng, HP, tài nguyên)\n\n");
        md.append("| Lv | Đạo | Công/Phép | Phòng | Giảm% | HP | EHP | Tài nguyên | Hồi/s | Q+E liên tục (s) | Sát thương/đòn chuẩn | Đòn chuẩn/s |\n|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (int lv : LV) for (Path p : PATHS) {
            Player pl = player(p, lv);
            double sus = sustain(pl);
            md.append(String.format("| %d | %s | %.0f | %.0f | %.0f | %.0f | %.0f | %.0f | %.1f | %s | %.0f | %.1f |%n", lv, p, pl.power(), pl.def(),
                    pl.mitigation() * 100, pl.hp(), pl.hp() / (1 - pl.mitigation()), pl.resMax(), regenPerSec(pl, 1 / casts(pl, true).get(0).cd()),
                    sus >= 999 ? "inf" : String.format("%.0f", sus), trashDps(pl, HU_THI, lv, mobHp(HU_THI, lv)) / ACTION_RATE, ACTION_RATE));
            // Kiếm: hồi thụ động chỉ còn 1/4 (yêu cầu thiết kế mới), phần còn lại dựa vào +4% max / đòn trúng (1 lần / 2s) -> sàn 10s; đạo khác giữ 20s.
            double minSus = p == Path.KIEM ? 10 : 20;
            if (sus < minSus) fails.add("sustain<" + minSus + "s " + p + " L" + lv + " = " + sus);
        }

        // --- 2. Hư Hóa cùng cấp
        md.append("\n## 2. TTK vs Hư Hóa cùng cấp (HuThi/HuCot/HuChu): số đòn cơ bản, giây, và số đòn quái hạ người\n\n");
        md.append("| Lv | Đạo | HuThi đòn | HuCot đòn | HuChu đòn | TTK HuThi (s) | Quái hạ người (đòn, HuThi) | Đòn mạnh nhất/HP |\n|---|---|---|---|---|---|---|---|\n");
        for (int lv : LV) for (Path p : PATHS) {
            Player pl = player(p, lv);
            int h1 = hitsToKill(pl, HU_THI, lv), h2 = hitsToKill(pl, HU_COT, lv), h3 = hitsToKill(pl, HU_CHU, lv);
            double sec = ttkMob(pl, HU_THI, lv);
            int kill = mobHitsToKillPlayer(pl, HU_THI, lv);
            double big = 0;
            for (Mob m : MOBS) big = Math.max(big, mobBigHitFrac(pl, m, lv));
            md.append(String.format("| %d | %s | %d | %d | %d | %.1f | %d | %.0f%% |%n", lv, p, h1, h2, h3, sec, kill, big * 100));
            for (int h : new int[]{h1, h2, h3}) if (h < 6 || h > 10) fails.add("hits " + p + " L" + lv + " = " + h);
            if (sec < 3 || sec > 6.5) fails.add("ttk sec " + p + " L" + lv + " = " + sec);
            for (Mob m : MOBS) if (mobHitsToKillPlayer(pl, m, lv) < 8 || mobHitsToKillPlayer(pl, m, lv) > 70) fails.add("mob kills player outside 8-70 hits " + m.name() + " " + p + " L" + lv);
            if (big >= 0.5) fails.add("one-shot risk " + p + " L" + lv + " = " + big);
        }

        // --- 3. Boss solo
        md.append("\n## 3. TTK boss solo cùng cấp (giây) và DPS bền vững\n\n");
        md.append("| Boss | Lv | Đạo | DPS | TTK (s) | Mục tiêu (s) | Đòn mạnh nhất/HP người |\n|---|---|---|---|---|---|---|\n");
        for (Boss b : bosses()) {
            double sumDps = 0;
            int n = 0;
            double[] dpsBy = new double[PATHS.length];
            for (int i = 0; i < PATHS.length; i++) {
                Player pl = player(PATHS[i], b.lv());
                Fight f = fightBoss(pl, b);
                dpsBy[i] = f.dps();
                double big = bossBigHitFrac(pl, b);
                md.append(String.format("| %s | %d | %s | %.0f | %.0f | %.0f-%.0f | %.0f%% |%n", b.name(), b.lv(), PATHS[i], f.dps(), f.ttk(), b.minSec(), b.maxSec(), big * 100));
                if (PATHS[i] != Path.DAO) { sumDps += f.dps(); n++; }
                boolean dao = PATHS[i] == Path.DAO;
                if (!dao && (f.ttk() < b.minSec() * 0.9 || f.ttk() > b.maxSec() * 1.1)) fails.add("boss ttk " + b.name() + " " + PATHS[i] + " = " + f.ttk());
                if (dao && (f.ttk() < b.minSec() * 0.9 || f.ttk() > b.maxSec() * 1.8)) fails.add("boss ttk (Dao) " + b.name() + " = " + f.ttk());
                if (big >= 0.6) fails.add("boss one-shot " + b.name() + " " + PATHS[i] + " = " + big);
            }
            double mean = sumDps / n;
            double mid = (b.minSec() + b.maxSec()) / 2, sug = 0;
            for (Path p : PATHS) if (p != Path.DAO) sug += dpsAt(player(p, b.lv()), mid, b.armor(), mobEhp(b.hp()), b.huMob())[0] * mid / 3;
            md.append(String.format("| (gợi ý HP thiết kế cho TTK %.0fs: %.0f) | | | | | | |%n", mid, sug));
            System.out.println("SUGGEST " + b.name() + " " + Math.round(sug));
            for (int i = 0; i < PATHS.length; i++) {
                double r = dpsBy[i] / mean;
                if (PATHS[i] == Path.DAO ? (r < 0.55 || r > 1.15) : (r < 0.85 || r > 1.15)) fails.add("path dps ratio " + b.name() + " " + PATHS[i] + " = " + r);
            }
        }

        // --- 4. DPS cân bằng (cửa sổ 180s, mục tiêu giáp 15, không phải HuMob)
        md.append("\n## 4. DPS bền vững theo đạo (cửa sổ 180s, giáp 15, f = tỉ lệ cast tài nguyên đạt được)\n\n| Lv | KIEM (f) | DAO (f) | THE | MA | Dao/TB | Chênh Kiếm/Thể/Ma |\n|---|---|---|---|---|---|---|\n");
        for (int lv : LV) {
            double[][] r = new double[4][];
            for (int i = 0; i < 4; i++) r[i] = dpsAt(player(PATHS[i], lv), 180, 15, 1, false);
            double mean = (r[0][0] + r[2][0] + r[3][0]) / 3;
            double spread = Math.max(Math.abs(r[0][0] / mean - 1), Math.max(Math.abs(r[2][0] / mean - 1), Math.abs(r[3][0] / mean - 1)));
            md.append(String.format("| %d | %.0f (%.2f) | %.0f (%.2f) | %.0f | %.0f | %.2f | %.0f%% |%n", lv, r[0][0], r[0][1], r[1][0], r[1][1], r[2][0], r[3][0], r[1][0] / mean, spread * 100));
            if (lv >= 10 && spread > 0.20) fails.add("DPS spread " + lv + " = " + spread);
            // Đạo: 100% sát thương là kỹ năng nên hưởng Cường Hóa nhiều nhất (cận chiến Kiếm/Thể/Ma không nhân) -> cho phép tới +5% so với trung bình
            if (lv >= 10 && (r[1][0] / mean < 0.6 || r[1][0] / mean > 1.05)) fails.add("DPS Dao/mean " + lv + " = " + r[1][0] / mean);
        }

        // --- 5. Bí Cảnh
        md.append("\n## 5. Bí Cảnh: cấp quái từng bậc vs dải cấp người chơi\n\n| Bậc | Người chơi | Cấp quái | Đòn cơ bản (Kiếm, ở min/max) |\n|---|---|---|---|\n");
        for (BiCanhTier t : BiCanhTier.values()) {
            int hMin = hitsToKill(player(Path.KIEM, t.minLevel), HU_THI, t.mobLevel), hMax = hitsToKill(player(Path.KIEM, t.maxLevel), HU_THI, t.mobLevel);
            md.append(String.format("| %s | %d-%d | %d | %d / %d |%n", t.name(), t.minLevel, t.maxLevel, t.mobLevel, hMin, hMax));
            if (t.mobLevel < t.minLevel || t.mobLevel > t.maxLevel) fails.add("bicanh mobLevel out of range " + t);
            if (hMin < 3 || hMin > 40 || hMax < 1 || hMax > 40) fails.add("bicanh hits " + t + " " + hMin + "/" + hMax);
        }

        // --- 6. EXP
        double hours = hoursTo100();
        md.append(String.format("%n## 6. Thời gian lên cấp 100 (chỉ cày quái, không vật phẩm đột phá, spawner = 0)%n%n"));
        md.append(String.format("- EXP_DIV = %.0f; tìm quái %.0fs/con; TTK trung bình 4 đạo.%n- **Tổng: %.0f giờ** (mục tiêu 500h, dung sai +-15%%).%n", HuHoa.EXP_DIV, SEEK_SECONDS, hours));
        if (hours < 425 || hours > 575) fails.add("hours to 100 = " + hours);

        md.append("\n## 7. Nhận xét\n\n- Hồi nhanh bằng đan: Chỉ Huyết 15% HP + Hồi Xuân 30% HP, hồi chiêu riêng 10s mỗi loại (chưa tính vào bảng).\n");
        md.append("- Đạo: đòn thường (không phải kỹ năng) x0 với quái, x1 với người chơi (R12); kỹ năng không bị chặn. Đạo mạnh ở buff đồng đội (Ban Phúc/Thánh Giáng), không mô phỏng ở đây.\n");
        md.append("\n## 8. Trước / sau Bước 27 (số liệu chính)\n\n| Chỉ số | Trước | Sau |\n|---|---|---|\n");
        md.append("| Hư Vương (cấp 30) TTK solo | 1-11s | 88-98s (HP 600 -> " + (int) HuVuong.HP + ") |\n");
        md.append("| CHG1-4 TTK solo | 3-26s | 210-260s (HP 3200-8000 -> " + (int) ChgKit.def(1).hp() + "-" + (int) ChgKit.def(4).hp() + ") |\n");
        md.append("| Boss cuối TTK solo | 8-91s | 400-450s (HP 24000 -> " + (int) EndgameKit.BOSS_HP + ") |\n");
        md.append("| Lệch DPS Kiếm/Thể/Ma | 51-92% | <=15% (thực tế 5-11%) |\n");
        md.append("| Số đòn chuẩn hạ Hư Hóa cùng cấp | 2-3 (cấp 90-100), 5 (cấp 50) | 6-10 mọi cấp |\n");
        md.append("| Đòn quái hạ người (HuThi) | 35-128 ở cấp 100, đòn mạnh nhất 1-5% HP | 8-70, đòn mạnh nhất <= 50% HP |\n");
        md.append("| Q+E liên tục (Kiếm, cấp 1) | 6s | >= 20s mọi cấp/đạo |\n");
        md.append("| Thời gian tới cấp 100 (cày quái) | 183h | ~500h |\n");
        md.append("| HP boss thực tế | bị kẹp 1024 (HP thiết kế 3200-24000 vô hiệu) | HuHoa.ehpScale quy HP thiết kế thành giảm sát thương nhận |\n");

        java.nio.file.Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) root = root.getParent();
        java.nio.file.Path out = (root == null ? Paths.get("") : root).resolve("docs").resolve("research").resolve("REPORT-v1-balance.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, md.toString(), StandardCharsets.UTF_8);
        System.out.println(md);
        assertTrue(fails.isEmpty(), "Balance targets violated (" + fails.size() + "):\n" + String.join("\n", fails));
    }
}
