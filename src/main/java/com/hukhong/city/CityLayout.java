package com.hukhong.city;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bo cuc thuan (khong dung the gioi) cho Khu Cho + Hiep Hoi Mao Hiem Gia cua moi thanh: moi cong trinh chiem mot O RIENG
 * (plot) khong de len tuong/nha/dien dan/o khac, dia hinh phang (chenh cao &lt;= {@link #MAX_VAR}), khong nuoc.
 * Thu tu: (1) ben trong thanh neu con cho trong tu nhien (cau truc import lon), (2) ngay ngoai cong, noi voi cong bang loi di
 * (thu theo huong uu tien cua the loai, lech ngang, ra xa dan). Thanh nui chon o PHANG NHAT. Ket qua duoc luu mot lan (CityLayouts).
 */
public final class CityLayout {
    private CityLayout() {}

    /** Nua canh o Khu Cho: HALF(16) + vien co(1) + le dem(2). */
    public static final int MARKET_HALF = MarketDistrict.HALF + 3;
    /** Hiep Hoi: ngang +-GUILD_U, sau GUILD_BACK, mat tien GUILD_FRONT (khop GuildBuilder: W+2, D+2, D+4). */
    public static final int GUILD_U = 9, GUILD_BACK = 7, GUILD_FRONT = 9;
    /** Le an toan quanh ranh thanh (bien ten, den cong, thap) va khoang dem loi di. */
    public static final int EXCL = 5, PATH_GAP = 4;
    /** Chenh cao toi da cho phep trong vet o. */
    public static final int MAX_VAR = 3;
    /** Khoang cach toi thieu tu moi quay hang den Ấn Hồn Đài (luat "khong thuong nhan trong 32 block"). */
    public static final int ALTAR_MIN = MarketDistrict.ALTAR_CLEAR + 1;
    /** Nua canh vung cam quanh altar khi tim cho BEN TRONG thanh (dien dan lon toi da T=21 + le). */
    public static final int ALTAR_KEEPOUT = 25;
    private static final int[] SHIFTS = {0, 12, -12, 24, -24};
    private static final int[] EXTRA = {0, 16, 32};
    /** Huong ra ngoai: 0=N(-z) 1=E(+x) 2=S(+z) 3=W(-x). */
    private static final Direction[] OUT = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public record Rect(int minX, int minZ, int maxX, int maxZ) {
        public boolean intersects(Rect o) { return minX <= o.maxX && o.minX <= maxX && minZ <= o.maxZ && o.minZ <= maxZ; }
        public Rect inflate(int n) { return new Rect(minX - n, minZ - n, maxX + n, maxZ + n); }
        public boolean contains(int x, int z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }
        public int cx() { return (minX + maxX) / 2; }
        public int cz() { return (minZ + maxZ) / 2; }
        public int[] asClip() { return new int[]{minX, minZ, maxX, maxZ}; }
    }

    /** O dat: tam (cx,cz), y = mat nen (block tren cung cua dia hinh), face = Direction.get2DDataValue() huong mat tien (ve phia thanh). */
    public record Plot(int cx, int cz, int y, int face) {
        public Direction dir() { return Direction.from2DDataValue(face); }
    }

    /** market / guild co the null (khong tim duoc cho hop le -> dung duong cu). */
    public record Layout(Plot market, Plot guild) {}

    public interface Terrain {
        /** y cua block tren cung cua dia hinh tu nhien tai (x,z). */
        int height(int x, int z);
        boolean wet(int x, int z);
    }

    // ---------------------------------------------------------------- hinh hoc

    public static Rect marketRect(Plot p) { return new Rect(p.cx() - MARKET_HALF, p.cz() - MARKET_HALF, p.cx() + MARKET_HALF, p.cz() + MARKET_HALF); }

    public static Rect guildRect(Plot p) {
        Direction out = p.dir(), side = out.getClockWise();
        int[] xs = new int[4], zs = new int[4];
        int i = 0;
        for (int u : new int[]{-GUILD_U, GUILD_U}) for (int v : new int[]{-GUILD_BACK, GUILD_FRONT}) {
            xs[i] = p.cx() + side.getStepX() * u + out.getStepX() * v;
            zs[i] = p.cz() + side.getStepZ() * u + out.getStepZ() * v;
            i++;
        }
        return new Rect(min(xs), min(zs), max(xs), max(zs));
    }

    private static int min(int[] a) { int m = a[0]; for (int v : a) m = Math.min(m, v); return m; }
    private static int max(int[] a) { int m = a[0]; for (int v : a) m = Math.max(m, v); return m; }

    /** Toa do the gioi cua cac quay hang cua mot Khu Cho tam (cx,cz) mat tien g. */
    public static List<int[]> stallXZ(int cx, int cz, Direction g) {
        List<int[]> out = new ArrayList<>();
        for (int[] s : MarketDistrict.STALLS)
            out.add(new int[]{cx + g.getStepX() * s[1] + g.getClockWise().getStepX() * s[0], cz + g.getStepZ() * s[1] + g.getClockWise().getStepZ() * s[0]});
        return out;
    }

    /** Khao sat dia hinh trong vet o (bo le dem): {min, max} hoac null neu co nuoc. */
    public static int[] survey(Terrain t, Rect r) {
        Rect c = r.inflate(-2);
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int x = c.minX(); ; x = Math.min(x + 4, c.maxX())) {
            for (int z = c.minZ(); ; z = Math.min(z + 4, c.maxZ())) {
                if (t.wet(x, z)) return null;
                int h = t.height(x, z);
                lo = Math.min(lo, h);
                hi = Math.max(hi, h);
                if (z == c.maxZ()) break;
            }
            if (x == c.maxX()) break;
        }
        return new int[]{lo, hi};
    }

    // ---------------------------------------------------------------- lap ke hoach

    private static int[] order(String category, boolean market) {
        return switch (category) {
            case "port" -> market ? new int[]{1, 3, 0, 2} : new int[]{2, 3, 1, 0};   // cho: ben phai/trai (bo bien); hiep hoi: phia cang (nam)
            default -> market ? new int[]{2, 1, 3, 0} : new int[]{1, 3, 0, 2};
        };
    }

    /**
     * Lap ke hoach cho mot thanh. city = ranh thanh; (ax,az) = Ấn Hồn Đài; avoid = cac hop khong duoc cham (thanh khac, o khac, tong mon...);
     * insideOk = kiem tra "vung nay trong ben trong thanh la dat trong tu nhien" (null = bo qua buoc trong thanh).
     */
    public static Layout plan(String category, Rect city, int ax, int az, Terrain t, List<Rect> avoid, Predicate<Rect> insideOk) {
        boolean flattest = category.equals("city_mountain");
        Plot market = pick(true, category, city, ax, az, t, avoid, insideOk, flattest);
        List<Rect> avoid2 = new ArrayList<>(avoid);
        if (market != null) avoid2.add(marketRect(market));
        Plot guild = pick(false, category, city, ax, az, t, avoid2, insideOk, flattest);
        return new Layout(market, guild);
    }

    private static Plot pick(boolean market, String category, Rect city, int ax, int az, Terrain t, List<Rect> avoid, Predicate<Rect> insideOk, boolean flattest) {
        Plot best = null;
        int bestVar = Integer.MAX_VALUE;
        for (Plot cand : candidates(market, category, city, ax, az, insideOk != null)) {
            Rect r = market ? marketRect(cand) : guildRect(cand);
            boolean inside = city.contains(r.minX(), r.minZ()) && city.contains(r.maxX(), r.maxZ());
            if (inside && !insideOk.test(r)) continue;
            if (!inside && r.intersects(city.inflate(EXCL))) continue;
            if (inside && r.intersects(new Rect(ax - ALTAR_KEEPOUT, az - ALTAR_KEEPOUT, ax + ALTAR_KEEPOUT, az + ALTAR_KEEPOUT))) continue;
            boolean clash = false;
            for (Rect a : avoid) if (r.intersects(a.inflate(2))) { clash = true; break; }
            if (clash) continue;
            if (market) {
                for (int[] s : stallXZ(cand.cx(), cand.cz(), cand.dir())) {
                    double dx = s[0] - ax, dz = s[1] - az;
                    if (dx * dx + dz * dz < (double) ALTAR_MIN * ALTAR_MIN) { clash = true; break; }
                }
                if (clash) continue;
            }
            int[] sv = survey(t, r);
            if (sv == null || sv[1] - sv[0] > MAX_VAR) continue;
            Plot p = new Plot(cand.cx(), cand.cz(), sv[1], cand.face());
            if (!flattest) return p;
            if (sv[1] - sv[0] < bestVar) { bestVar = sv[1] - sv[0]; best = p; }
        }
        return best;
    }

    /** Ung vien theo thu tu uu tien (y chua biet: de 0). */
    static List<Plot> candidates(boolean market, String category, Rect city, int ax, int az, boolean withInside) {
        List<Plot> out = new ArrayList<>();
        int half = market ? MARKET_HALF : Math.max(GUILD_U, GUILD_FRONT);
        // 1) ben trong thanh: luoi 5 o, cach tuong >= 6, uu tien gan cac canh theo thu tu huong
        if (withInside) {
            int in = EXCL + 1;
            for (int side : order(category, market)) {
                for (int x = city.minX() + in + half; x <= city.maxX() - in - half; x += 5)
                    for (int z = city.minZ() + in + half; z <= city.maxZ() - in - half; z += 5) {
                        // chi nhan o thuoc nua thanh ve phia 'side'
                        boolean ok = switch (side) { case 0 -> z < az; case 1 -> x > ax; case 2 -> z > az; default -> x < ax; };
                        if (!ok) continue;
                        Direction g = Math.abs(ax - x) >= Math.abs(az - z) ? (ax > x ? Direction.EAST : Direction.WEST) : (az > z ? Direction.SOUTH : Direction.NORTH);
                        out.add(new Plot(x, z, 0, g.get2DDataValue()));
                    }
            }
        }
        // 2) ngay ngoai cong: truc cong thang hang voi tam thanh
        int cxm = city.cx(), czm = city.cz();
        for (int extra : EXTRA) for (int side : order(category, market)) for (int shift : SHIFTS) {
            Direction o = OUT[side];
            int edge = switch (side) { case 0 -> city.minZ(); case 1 -> city.maxX(); case 2 -> city.maxZ(); default -> city.minX(); };
            int a = EXCL + PATH_GAP + (market ? MARKET_HALF : GUILD_FRONT) + extra;
            int x, z;
            if (o.getAxis() == Direction.Axis.Z) { x = cxm + shift; z = edge + o.getStepZ() * a; }
            else { z = czm + shift; x = edge + o.getStepX() * a; }
            out.add(new Plot(x, z, 0, o.getOpposite().get2DDataValue()));
        }
        return out;
    }

    /** Diem cong cua thanh o phia 'side' (de noi loi di): toa do (x,z) tren ranh, tai truc giua. */
    public static int[] gatePoint(Rect city, Direction out) {
        return switch (out) {
            case NORTH -> new int[]{city.cx(), city.minZ()};
            case SOUTH -> new int[]{city.cx(), city.maxZ()};
            case EAST -> new int[]{city.maxX(), city.cz()};
            default -> new int[]{city.minX(), city.cz()};
        };
    }
}
