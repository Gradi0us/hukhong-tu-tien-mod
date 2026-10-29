package com.hukhong.zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Thuan (khong phu thuoc Minecraft): bo tri vung xam thuc, cap theo ban kinh, hang boss, ten. */
public final class ZoneMath {
    private ZoneMath() {}

    public static final int MIN_RADIUS = 120, MAX_RADIUS = 700, MIN_ZONES = 8, MAX_ZONES = 14, MAX_LEVEL = 85;
    /** Khoang cach toi thieu tu mep vung toi spawn / tam thanh / vung khac. */
    public static final int SPAWN_CLEAR = 500, CITY_CLEAR = 400, ZONE_GAP = 60;
    public static final int MIN_DIST = 650, MAX_DIST = 12500, BORDER = 14800;

    public record Spec(int id, int cx, int cz, int radius, int level, String name) {}

    /** 120-250 -> 20-40, 250-450 -> 40-65, 450-700 -> 65-85 (noi suy tuyen tinh trong tung bang). */
    public static int levelForRadius(int r) {
        r = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, r));
        double lv;
        if (r <= 250) lv = 20 + (r - 120) * 20.0 / 130.0;
        else if (r <= 450) lv = 40 + (r - 250) * 25.0 / 200.0;
        else lv = 65 + (r - 450) * 20.0 / 250.0;
        return Math.min(MAX_LEVEL, (int) Math.round(lv));
    }

    /** 0 = khong danh hieu, 1 = Tinh Anh, 2 = Co. */
    public static int tier(int r) { return r < 250 ? 0 : r < 450 ? 1 : 2; }

    public static String tierLabel(int tier) { return tier == 1 ? "Tinh Anh" : tier >= 2 ? "Cổ" : ""; }

    /** Di thu C goi y cho vung lon (>=450), chon tat dinh theo id; vung nho -> null. */
    public static String clueFor(int zoneId, int radius) {
        return radius >= 450 ? "c" + (1 + Math.floorMod(zoneId * 7 + 3, 10)) : null;
    }

    public static long honTinhReward(int level) { return 20L + level; }

    /** Ban kinh mong muon: xa spawn thi to hon (ngau nhien +-). */
    public static int desiredRadius(double dist, double rnd) {
        double t = Math.max(0, Math.min(1, (dist - MIN_DIST) / (MAX_DIST - MIN_DIST)));
        double r = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * Math.pow(t, 0.8) * (0.7 + 0.5 * rnd);
        return (int) Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, r));
    }

    /** Bo tri tat dinh theo seed. cityXZ = cac tam thanh {x,z}; spawn (sx,sz). */
    public static List<Spec> place(long seed, int sx, int sz, int[][] cityXZ) {
        Random rnd = new Random(seed ^ 0x5A4F4E45L);
        int target = MIN_ZONES + rnd.nextInt(MAX_ZONES - MIN_ZONES + 1);
        List<Spec> out = new ArrayList<>();
        for (int attempt = 0; attempt < 4000 && out.size() < target; attempt++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = MIN_DIST + (MAX_DIST - MIN_DIST) * Math.sqrt(rnd.nextDouble());
            int cx = sx + (int) Math.round(Math.cos(ang) * dist), cz = sz + (int) Math.round(Math.sin(ang) * dist);
            int fit = (int) Math.min(desiredRadius(dist, rnd.nextDouble()), maxFit(cx, cz, sx, sz, cityXZ, out));
            if (fit < MIN_RADIUS) continue;
            int id = out.size() + 1;
            out.add(new Spec(id, cx, cz, fit, levelForRadius(fit), name(rnd)));
        }
        return out;
    }

    /** Ban kinh lon nhat dat duoc tai (cx,cz) khong de len spawn/thanh/vung da co/bien the gioi. */
    public static double maxFit(int cx, int cz, int sx, int sz, int[][] cityXZ, List<Spec> others) {
        double d = Math.hypot(cx - sx, cz - sz);
        double fit = Math.min(d - SPAWN_CLEAR, BORDER - d);
        for (int[] c : cityXZ) fit = Math.min(fit, Math.hypot(cx - c[0], cz - c[1]) - CITY_CLEAR);
        for (Spec o : others) fit = Math.min(fit, Math.hypot(cx - o.cx(), cz - o.cz()) - o.radius() - ZONE_GAP);
        return fit;
    }

    private static final String[] A = {"Hắc", "Huyết", "Tịch", "Vọng", "Cô", "Tàn", "U", "Hàn", "Xích", "Minh", "Phế", "Thương", "Diệt", "Ảo", "Tử", "Lạc"};
    private static final String[] B = {"Phong", "Sa", "Mộc", "Hải", "Cốt", "Vân", "Nguyệt", "Uyên", "Linh", "Dạ", "Thạch", "Tuyết", "Độc", "Huyễn", "Âm", "Hỏa"};
    private static final String[] C = {"Nguyên", "Cốc", "Lĩnh", "Đầm", "Lâm", "Sơn", "Hoang Địa", "Vực", "Bình Nguyên", "Trủng"};

    public static String name(Random r) { return A[r.nextInt(A.length)] + " " + B[r.nextInt(B.length)] + " " + C[r.nextInt(C.length)]; }

    /** Trong ban kinh va chua tay sach: khoang cach (x,z) toi tam nam trong [cleansed, radius]. */
    public static boolean covers(double x, double z, int cx, int cz, int radius, int cleansed) {
        double d = Math.hypot(x - cx, z - cz);
        return d <= radius && d >= cleansed;
    }
}
