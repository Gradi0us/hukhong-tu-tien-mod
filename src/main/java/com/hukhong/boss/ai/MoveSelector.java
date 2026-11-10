package com.hukhong.boss.ai;

import java.util.ArrayList;
import java.util.List;

/** Chọn đòn theo tầm + trọng số + chống lặp (không dùng một đòn quá 2 lần liên tiếp). Thuần Java để unit test. */
public final class MoveSelector {
    private MoveSelector() {}

    public record Option(String id, double rangeMin, double rangeMax, double weight, int minPhase) {}

    /** @param history các đòn đã dùng, mới nhất ở cuối. @param roll số ngẫu nhiên [0,1). @return null nếu không có đòn hợp lệ. */
    public static Option pick(List<Option> opts, double dist, int phase, List<String> history, double roll) {
        String banned = null;
        int n = history.size();
        if (n >= 2 && history.get(n - 1).equals(history.get(n - 2))) banned = history.get(n - 1);
        List<Option> ok = new ArrayList<>();
        double sum = 0;
        for (Option o : opts) {
            if (o.weight() <= 0 || phase < o.minPhase() || dist < o.rangeMin() || dist > o.rangeMax() || o.id().equals(banned)) continue;
            ok.add(o);
            sum += o.weight();
        }
        if (ok.isEmpty()) return null;
        double x = roll * sum;
        for (Option o : ok) { x -= o.weight(); if (x < 0) return o; }
        return ok.get(ok.size() - 1);
    }

    /** Thêm vào lịch sử, giữ tối đa 4 mục. */
    public static void push(List<String> history, String id) {
        history.add(id);
        while (history.size() > 4) history.remove(0);
    }

    /** Có né không: roll < chance khi đã hết hồi né. */
    public static boolean evadeRoll(double chance, int cdLeft, double roll) { return cdLeft <= 0 && roll < chance; }
}
