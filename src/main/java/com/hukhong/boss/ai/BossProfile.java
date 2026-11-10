package com.hukhong.boss.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Hồ sơ chiến đấu của một boss cho {@link BossBrain} (PLAN-v1 §1.3). Giá trị theo pha 1..3 (pha thiếu = pha trước).
 * Boss bật brain bằng cách trả về profile khác null từ {@code PhasedBoss.brainProfile()}.
 */
public final class BossProfile {
    /** Thông số một pha. Khoảng cách tính bằng block, tốc độ block/tick, xoay độ/tick, thời gian tick. */
    public record PhaseVals(double prefMin, double prefMax, double strafeSpeed, float turnRate,
                            double evadeChance, int evadeCd, int restMin, int restMax) {}

    /** Kiểu né. */
    public enum Evade { BACKSTEP, SIDESTEP, BACKFLIP }

    /** Một nhát trong chuỗi. lunge = số block lao theo hướng mặt trong lúc vung; recoil = block lùi lúc thu đòn. */
    public record Hit(int windup, int active, int recovery, double reach, double arcDeg, double dmgMult,
                      double lunge, double recoil, boolean delayed, boolean heavy, boolean unblockable) {
        public int total(int delayExtra) { return windup + (delayed ? delayExtra : 0) + active + recovery; }
    }

    /** Điều kiện rẽ nhánh sau nhát {@code afterHit} (0-based): người chơi lùi xa thêm > dist, hoặc đi ngang > deg. */
    public enum Cond { RETREAT, STRAFE }
    public record Branch(int afterHit, Cond cond, double threshold, String toCombo) {}

    /** Chuỗi đòn thường 2-4 nhát; chọn theo tầm [rangeMin, rangeMax], trọng số, pha mở khóa. */
    public record Combo(String id, List<Hit> hits, double rangeMin, double rangeMax, double weight, int minPhase,
                        List<Branch> branches, boolean branchOnly) {}

    final PhaseVals[] phases = new PhaseVals[3];
    final List<Combo> combos = new ArrayList<>();
    double weight = 1.0;          // trọng lượng: 0.5 nhẹ .. 3 rất nặng (hit-stop, bụi, âm bước)
    int accelTicks = 4, brakeTicks = 3;
    double moveSpeed = 0.28;      // tốc độ đi/đuổi tối đa
    GapClose gapClose = GapClose.DASH;
    double gapCloseMin = 9;       // xa hơn mức này mới áp sát
    int delayExtra = 10;          // số tick "trễ cố ý" của nhát delayed
    boolean punishHeal;
    String punishCombo;
    int counterHits, counterWindow;  // 0 = không phản đòn
    String counterCombo;
    int bigMoveEvery = 3;         // sau mỗi N chuỗi, thử chiêu lớn riêng của boss
    double difficulty = 1.0;      // hệ số độ khó: nhân sát thương, chia thời gian nghỉ
    final Evade[][] evadeKinds = new Evade[3][];   // null = mặc định (backstep/sidestep, pha >= 2 thêm backflip)
    Evade counterEvade = Evade.SIDESTEP;           // null = phản đòn tại chỗ, không né
    final java.util.Map<String, Integer> maxPhase = new java.util.HashMap<>();

    private BossProfile() {}

    public static Builder builder() { return new Builder(); }

    public PhaseVals phase(int p) {
        for (int i = Math.max(0, Math.min(2, p - 1)); i >= 0; i--) if (phases[i] != null) return phases[i];
        throw new IllegalStateException("BossProfile: chưa khai báo pha 1");
    }
    public List<Combo> combos() { return combos; }
    public Combo combo(String id) { for (Combo c : combos) if (c.id().equals(id)) return c; return null; }
    public double weight() { return weight; }
    public int accelTicks() { return accelTicks; }
    public int brakeTicks() { return brakeTicks; }
    public double moveSpeed() { return moveSpeed; }
    public GapClose gapClose() { return gapClose; }
    public double gapCloseMin() { return gapCloseMin; }
    public int delayExtra() { return delayExtra; }
    public boolean punishHeal() { return punishHeal; }
    public String punishCombo() { return punishCombo; }
    public int counterHits() { return counterHits; }
    public int counterWindow() { return counterWindow; }
    public String counterCombo() { return counterCombo; }
    public int bigMoveEvery() { return bigMoveEvery; }
    public double difficulty() { return difficulty; }
    /** Kiểu né riêng của pha (null = mặc định). */
    public Evade[] evadeKinds(int ph) {
        for (int i = Math.max(0, Math.min(2, ph - 1)); i >= 0; i--) if (evadeKinds[i] != null) return evadeKinds[i];
        return null;
    }
    public Evade counterEvade() { return counterEvade; }
    /** Pha cuối combo còn được tự chọn (mặc định 3). */
    public int comboMaxPhase(String id) { return maxPhase.getOrDefault(id, 3); }

    public static final class Builder {
        private final BossProfile p = new BossProfile();
        public Builder phase(int ph, PhaseVals v) { p.phases[ph - 1] = v; return this; }
        public Builder weight(double w) { p.weight = w; return this; }
        public Builder accel(int accelTicks, int brakeTicks) { p.accelTicks = Math.max(1, accelTicks); p.brakeTicks = Math.max(1, brakeTicks); return this; }
        public Builder moveSpeed(double s) { p.moveSpeed = s; return this; }
        public Builder gapClose(GapClose g, double minDist) { p.gapClose = g; p.gapCloseMin = minDist; return this; }
        public Builder delayExtra(int t) { p.delayExtra = t; return this; }
        public Builder combo(Combo c) { p.combos.add(c); return this; }
        public Builder punishHeal(String comboId) { p.punishHeal = true; p.punishCombo = comboId; return this; }
        public Builder counter(int hits, int windowTicks, String comboId) { p.counterHits = hits; p.counterWindow = windowTicks; p.counterCombo = comboId; return this; }
        public Builder bigMoveEvery(int n) { p.bigMoveEvery = Math.max(1, n); return this; }
        public Builder difficulty(double d) { p.difficulty = d; return this; }
        /** Kiểu né của pha {@code ph} (và các pha sau nếu không khai báo). */
        public Builder evades(int ph, Evade... kinds) { p.evadeKinds[ph - 1] = kinds; return this; }
        /** Kiểu né trước phản đòn; null = gồng phản đòn tại chỗ. */
        public Builder counterEvade(Evade e) { p.counterEvade = e; return this; }
        /** Combo chỉ tự được chọn tới pha {@code ph} (vẫn dùng được làm nhánh). */
        public Builder comboUntil(String id, int ph) { p.maxPhase.put(id, ph); return this; }
        public BossProfile build() {
            if (p.phases[0] == null) throw new IllegalStateException("BossProfile: cần phase(1, ...)");
            return p;
        }
    }

    /** Tiện ích tạo nhát chém. */
    public static Hit hit(int windup, int active, int recovery, double reach, double arc, double dmg, double lunge) {
        return new Hit(windup, active, recovery, reach, arc, dmg, lunge, 0, false, false, false);
    }
}
