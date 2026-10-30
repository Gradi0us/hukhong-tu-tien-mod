package com.hukhong.poi;

/**
 * Toan thuan cua dat dia diem. The gioi chia o vuong CELL x CELL; moi (dinh nghia, o) co toi da 1 ung vien, xuat hien voi xac suat rarity,
 * vi tri tat dinh theo seed -> chunk nap lan nao cung ra cung ket qua (khong can quet truoc, khong chan tao the gioi).
 */
public final class PoiMath {
    private PoiMath() {}

    public static final int CELL = 320, MARGIN = 40;

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long hash(long seed, String id, int cx, int cz) {
        long h = mix(seed ^ 0x504F49L);
        h = mix(h ^ id.hashCode() * 0x9E3779B97F4A7C15L);
        h = mix(h ^ ((long) cx << 32 | (cz & 0xFFFFFFFFL)));
        return h;
    }

    /** {x,z} cua ung vien trong o (cx,cz) hoac null neu khong xuat hien. */
    public static int[] candidate(long seed, String id, double rarity, int cx, int cz) {
        long h = hash(seed, id, cx, cz);
        double r = ((h >>> 11) & 0x1FFFFFFFFFFFFFL) / (double) (1L << 53);
        if (r >= rarity) return null;
        long h2 = mix(h);
        int span = CELL - 2 * MARGIN;
        int ox = (int) Math.floorMod(h2 >>> 8, (long) span), oz = (int) Math.floorMod(mix(h2) >>> 8, (long) span);
        return new int[]{cx * CELL + MARGIN + ox, cz * CELL + MARGIN + oz};
    }

    public static int cellOf(int block) { return Math.floorDiv(block, CELL); }

    /** Ung vien nam dung trong chunk (chunkX,chunkZ), hoac null. */
    public static int[] candidateInChunk(long seed, String id, double rarity, int chunkX, int chunkZ) {
        int[] c = candidate(seed, id, rarity, cellOf(chunkX * 16), cellOf(chunkZ * 16));
        return c != null && (c[0] >> 4) == chunkX && (c[1] >> 4) == chunkZ ? c : null;
    }

    public static boolean farEnough(int x, int z, int ox, int oz, int minDist) {
        long dx = x - ox, dz = z - oz;
        return dx * dx + dz * dz >= (long) minDist * minDist;
    }

    public static double dist(double x, double z, double ox, double oz) { return Math.sqrt((x - ox) * (x - ox) + (z - oz) * (z - oz)); }

    /** 8 huong theo chieu kim dong ho: 0=Bac(-z), 1=Dong-Bac, 2=Dong ... */
    public static int compass8(double dx, double dz) {
        double ang = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = bac, + = dong
        return Math.floorMod((int) Math.round(ang / 45.0), 8);
    }
}
