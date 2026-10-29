package com.hukhong.world.calendar;

import java.util.ArrayList;
import java.util.List;

/** Thuan Java: thoi gian Lich Thien Co. 1 ngay Lich = 4 ngay Minecraft = 96000 tick; tuan = 7 ngay Lich; mua = 7 ngay Lich (4 mua). */
public final class LichMath {
    private LichMath() {}

    public static final long DAY = 96000L;
    public static final int WEEK_DAYS = 7, SEASON_DAYS = 7, SEASONS = 4;

    public static long day(long t) { return Math.floorDiv(t, DAY); }
    public static long tickOfDay(long t) { return Math.floorMod(t, DAY); }
    /** 0..6 */
    public static int dayOfWeek(long t) { return (int) Math.floorMod(day(t), (long) WEEK_DAYS); }
    /** 0..6 (ngay trong mua) */
    public static int dayOfSeason(long t) { return (int) Math.floorMod(day(t), (long) SEASON_DAYS); }
    /** 0..3 */
    public static int season(long t) { return (int) Math.floorMod(Math.floorDiv(day(t), (long) SEASON_DAYS), (long) SEASONS); }
    /** Gio trong ngay Lich (0..23) */
    public static int hour(long t) { return (int) (tickOfDay(t) * 24 / DAY); }
    public static int minute(long t) { return (int) (tickOfDay(t) * 24 * 60 / DAY % 60); }

    /** Gia tri tat dinh trong [0,1) tu (id, so nguyen). */
    public static double unit(String id, long n) {
        long h = id.hashCode() * 0x9E3779B97F4A7C15L + n * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 30; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 27; h *= 0x94D049BB133111EBL; h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Chi so (0..n-1) tat dinh cho mot lan xuat hien. */
    public static int pickIndex(String id, long start, int n) { return (int) (unit(id + "#loc", start) * n); }

    public enum Kind { EVERY, WEEKLY, RANDOM }

    /** Chu ky: EVERY (moi {@code n} ngay Lich), WEEKLY (ngay {@code weekDay} 0..6), RANDOM (xac suat {@code chance} moi ngay). offset = tick trong ngay. */
    public record Recurrence(Kind kind, int n, int weekDay, double chance, long offset) {
        /** Cac thoi diem bat dau trong [from, to), tang dan. */
        public List<Long> starts(String id, long from, long to) {
            List<Long> out = new ArrayList<>();
            if (to <= from) return out;
            long d0 = Math.floorDiv(from - offset, DAY), d1 = Math.floorDiv(to - 1 - offset, DAY);
            for (long d = d0; d <= d1; d++) {
                boolean hit = switch (kind) {
                    case EVERY -> d >= 0 && Math.floorMod(d, (long) Math.max(1, n)) == 0;
                    case WEEKLY -> Math.floorMod(d, (long) WEEK_DAYS) == weekDay;
                    case RANDOM -> unit(id, d) < chance;
                };
                long s = d * DAY + offset;
                if (hit && s >= from && s < to) out.add(s);
            }
            return out;
        }
    }
}
