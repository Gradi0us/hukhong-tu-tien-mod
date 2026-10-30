package com.hukhong.living;

/**
 * Lich sinh hoat ngay/dem cua dan cu (thuan, kiem thu duoc). dayTime = gio trong ngay Minecraft (0 = sang som, 6000 = trua, 12000 = hoang hon,
 * 13000-23000 = dem). Vai tro theo AmbientNpc: 0 dan, 1 linh, 2 khuan vac, 3 tho cang, 4 de tu, 5 tre em, 6 Dau Gia Su, 7 Thay Boi, 8 Ma Tu.
 */
public final class NpcSchedule {
    private NpcSchedule() {}

    public static final int WORK = 0, EAT = 1, SLEEP = 2;

    private static boolean in(int t, int a, int b) { return t >= a && t < b; }

    public static int activity(int role, long dayTime) {
        int t = (int) Math.floorMod(dayTime, 24000L);
        switch (role) {
            case 1: // linh: canh dem, chi nghi an trua
                return in(t, 6000, 6600) ? EAT : WORK;
            case 6: case 7: case 8: // Dau Gia Su, Thay Boi, Ma Tu: luon tai vi tri
                return WORK;
            case 5: // tre em ngu som
                if (in(t, 12500, 23500)) return SLEEP;
                return in(t, 6000, 6800) ? EAT : WORK;
            case 4: // de tu day som
                if (in(t, 14000, 22500)) return SLEEP;
                return in(t, 6000, 6800) || in(t, 11800, 12600) ? EAT : WORK;
            default: // dan thuong, khuan vac, tho cang
                if (in(t, 14500, 23000)) return SLEEP;
                return in(t, 6000, 6800) || in(t, 11800, 12600) ? EAT : WORK;
        }
    }

    public static boolean isNight(long dayTime) {
        int t = (int) Math.floorMod(dayTime, 24000L);
        return t >= 13000 && t < 23000;
    }
}
