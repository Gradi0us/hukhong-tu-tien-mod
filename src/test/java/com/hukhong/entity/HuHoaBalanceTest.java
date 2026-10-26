package com.hukhong.entity;

import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.skill.Skill;
import com.hukhong.stats.StatService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mô phỏng TTK (R5-balance-p0): người chơi Đạo/Thể vs Hư Hóa cùng cấp. In bảng kết quả ra stdout. */
class HuHoaBalanceTest {
    // {tên, hp gốc vanilla, hpMul, sát thương gốc, dmgMul}
    record Mob(String name, double hp, double hpMul, double atk, double dmgMul) {}
    static final Mob[] MOBS = {
            new Mob("HuThi", 20, 1.45, 3, 1.0),
            new Mob("HuCot", 20, 1.5, 2, 1.0),
            new Mob("HuChu", 16, 1.8, 2, 1.5),
    };
    static final int[] LEVELS = {1, 10, 20, 30};

    static double expectedHit(Path p, int lv) {
        var s = StatService.compute(lv, Set.of(p), Map.of(p, Aptitude.B));
        double base = p == Path.DAO ? Skill.DAO_1_COEF * s.spell() : Skill.THE_1_COEF * s.attack();
        return base * (1 + s.crit() * (s.critDamage() - 1));
    }

    @Test
    void hitsToKillWithinRangeAndNoOneShot() {
        StringBuilder out = new StringBuilder("path lv mob hits mobMaxHit playerHp\n");
        for (Path p : new Path[]{Path.DAO, Path.THE}) {
            for (int lv : LEVELS) {
                double hit = expectedHit(p, lv);
                var s = StatService.compute(lv, Set.of(p), Map.of(p, Aptitude.B));
                double php = StatService.maxHealth(lv, Set.of(p), Map.of(p, Aptitude.B));
                for (Mob m : MOBS) {
                    double hp = m.hp() * m.hpMul() * HuHoa.hpScale(lv);
                    int hits = (int) Math.ceil(hp / hit);
                    double melee = m.atk() * m.dmgMul() * HuHoa.dmgScale(lv);
                    double max = Math.max(melee, HuHoa.skillDamage(lv, 1.5));
                    double taken = StatService.mitigate((float) max, s.defense());
                    out.append(String.format("%s %d %s %d %.1f %.1f%n", p, lv, m.name(), hits, taken, php));
                    // Bội số: Đạo_1 là đòn spam (rẻ), Thể_1 là đòn hồi chiêu dài -> mục tiêu 6..10.
                    assertTrue(hits >= 6 && hits <= 10, out.toString());
                    assertTrue(taken < php * 0.5, "one-shot risk\n" + out);
                }
            }
        }
        System.out.println(out);
    }
}
