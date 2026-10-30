package com.hukhong.boss.phase;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Mô hình cấu hình pha boss (thuần Java, không phụ thuộc MC): ngưỡng HP, cutscene, chiêu, đấu trường theo pha. */
public final class BossPhaseConfig {
    private BossPhaseConfig() {}

    /** Bảng màu chủ đề (hào quang, telegraph, hạt). */
    public enum Pal {
        VOID(0xA050FF), FROST(0x9FE8FF), TOXIC(0x7BD04A), SAND(0xE0B060), HOLY(0xFFE9A0), BLOOD(0xC01028), MOON(0x8FA8FF),
        /** 8 Hu Vuong dang: go / xuong / bien. */
        WOOD(0x4FA83A), BONE(0xE8E0C8), SEA(0x2E8FD8);
        public final int rgb;
        Pal(int rgb) { this.rgb = rgb; }
    }

    /** Hình dạng chiêu phase (dùng chung, tham số hoá bằng {@link Move}). */
    public enum Shape {
        /** Cột nổi dưới chân từng người chơi (+ vài điểm ngẫu nhiên). */
        PILLARS,
        /** Vệt chém thẳng từ boss về phía mục tiêu. */
        LINE,
        /** Mưa thiên thạch quanh người chơi. */
        METEOR,
        /** Sóng xung vòng tròn: nhảy lên để né (đứng đất thì trúng). */
        SHOCK,
        /** Tối thượng: hút vào tâm trong lúc gồng, rồi nổ cả vòng; chạy ra ngoài vòng cảnh báo. */
        PULL_BURST,
        /** Tối thượng: vòng nhẫn nguy hiểm, an toàn ở tâm và ngoài rìa. */
        DONUT
    }

    public enum Hit { NONE, KNOCK_UP, SLOW, BLIND, WEAK, POISON, FREEZE, NAUSEA }

    public enum ArenaEffect { PURPLE_SKY, BLOOD_RAIN, CRACKED_FLOOR, VOID_PILLARS }

    /** Một chiêu: id (khoá lang hukhong.boss.move.<id>), hình, bán kính/chiều dài, hệ số sát thương, thời gian gồng. */
    public record Move(String id, Shape shape, Pal pal, double radius, double dmg, int windup, Hit hit, boolean ultimate) {}

    /** Dòng phụ đề: hiện trong [from, to) tick của cutscene. */
    public record SubLine(int from, int to, String key) {}

    /**
     * Một pha. {@code below} = tỉ lệ HP mà dưới đó vào pha này; {@code transition} = số tick bất tử + cutscene;
     * {@code reveal} = tick (trong chuyển pha) đổi ngoại hình; {@code scale} = cỡ cơ thể (cũng là hitbox, qua thuộc tính SCALE).
     */
    public record Phase(int phase, float below, float scale, int transition, int reveal,
                        List<CamPath.Key> cam, List<SubLine> subs, List<Move> moves, Set<ArenaEffect> arena,
                        String music, float cdMul) {}

    public record Config(String id, Pal aura, double size, List<Phase> phases) {
        public Phase phase(int p) { return phases.get(Math.max(1, Math.min(phases.size(), p)) - 1); }
        public int count() { return phases.size(); }

        /** Pha mong muốn theo tỉ lệ HP (độc lập pha hiện tại). */
        public int phaseFor(float hpFrac) {
            int want = 1;
            for (Phase p : phases) if (hpFrac <= p.below && p.phase > want) want = p.phase;
            return want;
        }

        /** Mọi chiêu phase đã mở tới pha {@code p} (cộng dồn). */
        public List<Move> movesUpTo(int p) {
            List<Move> out = new ArrayList<>();
            for (Phase ph : phases) if (ph.phase <= p) out.addAll(ph.moves);
            return out;
        }

        public Set<ArenaEffect> arenaFor(int p) {
            Set<ArenaEffect> s = EnumSet.noneOf(ArenaEffect.class);
            for (Phase ph : phases) if (ph.phase <= p) s.addAll(ph.arena);
            return s;
        }
    }
}
