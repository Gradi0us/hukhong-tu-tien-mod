package com.hukhong.profession.phu;

import com.hukhong.craft.CraftQuality;
import com.hukhong.craft.CraftRegistry;
import com.hukhong.craft.RecipeBook;
import com.hukhong.phapkhi.PhapKhiKind;
import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.ProfessionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Phu An (Phu Van Su): ve phu bang minigame (server cham lai) va Ve nhanh (cap >= 12, Trung pham, tốn 1.5x).
 * Slot 0-2 = Phu Chi, Linh Mac, nguyen to; slot 3 = ket qua. Chi dung API Container nen khong phu thuoc BE.
 */
public final class PhuDraw {
    private PhuDraw() {}

    public static final int OUT = 3, QUICK_LEVEL = 12;
    /** Toa do chap nhan cho mau (ngoai khung mot chut van hop le, de client kep vao gioi han). */
    private static final int COORD_MIN = -400, COORD_MAX = 700;
    private static final int MAX_FLAT = PhuPattern.MAX_SAMPLES * 3;

    /** Mau da giai ma: x, y, t (ms tu luc bat dau). */
    record Trace(int[] x, int[] y, int[] t) {}

    /** Ket qua cham: acc = ty le trong ong x do phu net; fail = lech ong > 1 s. */
    record Eval(double acc, boolean fail) {}

    // ------------------------------------------------------------------ minigame

    /** Cham mot lan ve. trace = flat (x,y,t) x n; durMs = thoi gian client bao cao. */
    public static void submit(ServerPlayer sp, Container box, List<Integer> flat, int durMs) {
        if (!box.getItem(OUT).isEmpty()) { say(sp, "hukhong.station.out_full"); return; }
        ProfessionRecipes.Recipe r = find(box);
        if (r == null) { say(sp, "hukhong.station.no_recipe"); return; }
        if (!precheck(sp, r)) return;
        PhapKhiKind kind = PhuPattern.kindOf(r.id());
        if (kind == null) { say(sp, "hukhong.station.no_recipe"); return; }

        int limit = PhuPattern.limitMs(r.tier());
        Trace tr = decode(flat, durMs, limit);
        if (tr == null) { say(sp, "hukhong.phu.bad_trace"); return; }

        int lv = ProfessionService.level(sp, Profession.PHU_VAN);
        Eval ev = evaluate(tr, PhuPattern.path(kind.id, r.tier()), PhuPattern.radius(lv));
        int q = ev.fail() ? -1 : quality(ev.acc());
        if (q >= 0 && thuongChi(box)) q = Math.min(CraftQuality.CUC, q + 1);

        List<ItemStack> in = inputs(box);
        if (q < 0) {
            takeOne(in, r.needs().get(0));
            takeOne(in, r.needs().get(1));
            ProfessionService.addCraftXp(sp, Profession.PHU_VAN, 1);
            say(sp, "hukhong.phu.fail");
            box.setChanged();
            return;
        }
        ProfessionRecipes.consume(r, in);
        ItemStack made = r.out().make(lv, false);
        CraftQuality.stamp(made, q, sp.getName().getString());
        box.setItem(OUT, made);
        box.setChanged();
        ProfessionService.addCraftXp(sp, Profession.PHU_VAN, Math.round(4.0 * r.tier() * (1 + 0.25 * q)));
        sp.displayClientMessage(Component.translatable("hukhong.phu.done",
                Component.translatable("hukhong.craft.quality." + CraftQuality.name(q))), true);
    }

    /** Phẩm theo do chinh xac. -1 = hong (duoi 35%). */
    static int quality(double acc) {
        if (acc >= 0.90) return CraftQuality.CUC;
        if (acc >= 0.75) return CraftQuality.THUONG;
        if (acc >= 0.55) return CraftQuality.TRUNG;
        if (acc >= 0.35) return CraftQuality.HA;
        return -1;
    }

    /** Giai ma va kiem tra mau; null neu sai dinh dang / vuot gioi han. */
    static Trace decode(List<Integer> flat, int durMs, int limit) {
        if (flat == null || flat.size() % 3 != 0 || flat.size() < 6 || flat.size() > MAX_FLAT) return null;
        int n = flat.size() / 3;
        int[] x = new int[n], y = new int[n], t = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = flat.get(3 * i);
            y[i] = flat.get(3 * i + 1);
            t[i] = flat.get(3 * i + 2);
            if (x[i] < COORD_MIN || x[i] > COORD_MAX || y[i] < COORD_MIN || y[i] > COORD_MAX) return null;
            if (t[i] < 0 || (i > 0 && t[i] < t[i - 1])) return null;
        }
        int cap = limit + PhuPattern.TIME_TOL_MS;
        if (t[n - 1] > cap || durMs < t[n - 1] || durMs > cap) return null;
        return new Trace(x, y, t);
    }

    /**
     * Do chinh xac = (quang duong mau nam trong ong / tong quang duong mau) x (ty le net duoc phu trong ong).
     * Lech ong lien tuc > 1 s = hong.
     */
    static Eval evaluate(Trace tr, List<double[]> path, double radius) {
        int n = tr.t().length;
        int[] x = tr.x(), y = tr.y(), t = tr.t();
        boolean[] in = new boolean[n];
        for (int i = 0; i < n; i++) in[i] = PhuPattern.distToPath(x[i], y[i], path) <= radius;

        boolean fail = false;
        long runStart = -1;
        for (int i = 0; i < n; i++) {
            if (in[i]) { runStart = -1; continue; }
            if (runStart < 0) runStart = t[i];
            if (t[i] - runStart > PhuPattern.OUT_LIMIT_MS) fail = true;
        }

        double total = 0, inside = 0;
        for (int i = 0; i + 1 < n; i++) {
            double seg = Math.hypot(x[i + 1] - x[i], y[i + 1] - y[i]);
            total += seg;
            if (in[i] && in[i + 1]) inside += seg;
        }
        double ratio = total > 0 ? inside / total : 0;

        // do phu net: lay mau doc net moi 4 px, dem diem nam trong ong cua mot trong cac doan mau
        int covered = 0, samples = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            double[] a = path.get(i), b = path.get(i + 1);
            double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            int steps = Math.max(1, (int) Math.ceil(len / 4));
            for (int s = 0; s < steps; s++) {
                double px = a[0] + (b[0] - a[0]) * s / steps, py = a[1] + (b[1] - a[1]) * s / steps;
                samples++;
                if (PhuPattern.distToTrace(px, py, x, y) <= radius) covered++;
            }
        }
        double cover = samples > 0 ? (double) covered / samples : 0;
        return new Eval(ratio * cover, fail);
    }

    // ------------------------------------------------------------------ ve nhanh

    /** Ve nhanh: cap >= 12, khong minigame, Trung pham, moi nguyen lieu tốn ceil(1.5x). */
    public static void quick(ServerPlayer sp, Container box) {
        if (!box.getItem(OUT).isEmpty()) { say(sp, "hukhong.station.out_full"); return; }
        ProfessionRecipes.Recipe r = find(box);
        if (r == null) { say(sp, "hukhong.station.no_recipe"); return; }
        if (!precheck(sp, r)) return;
        int lv = ProfessionService.level(sp, Profession.PHU_VAN);
        if (lv < QUICK_LEVEL) { say(sp, "hukhong.phu.quick_low"); return; }

        List<ItemStack> in = inputs(box);
        int[] slot = new int[r.needs().size()];
        int[] cost = new int[r.needs().size()];
        boolean[] used = new boolean[in.size()];
        for (int n = 0; n < slot.length; n++) {
            ProfessionRecipes.Need need = r.needs().get(n);
            cost[n] = (need.count() * 3 + 1) / 2; // lam tron len: 1 -> 2
            slot[n] = -1;
            for (int i = 0; i < in.size() && slot[n] < 0; i++) {
                if (!used[i] && need.accepts(in.get(i)) && in.get(i).getCount() >= cost[n]) slot[n] = i;
            }
            if (slot[n] < 0) { say(sp, "hukhong.phu.quick_need", cost[n]); return; }
            used[slot[n]] = true;
        }
        for (int n = 0; n < slot.length; n++) in.get(slot[n]).shrink(cost[n]);
        ItemStack made = r.out().make(lv, false);
        CraftQuality.stamp(made, CraftQuality.TRUNG, sp.getName().getString());
        box.setItem(OUT, made);
        box.setChanged();
        ProfessionService.addCraftXp(sp, Profession.PHU_VAN, Math.round(4.0 * r.tier() * (1 + 0.25 * CraftQuality.TRUNG)));
        say(sp, "hukhong.phu.quick_done");
    }

    // ------------------------------------------------------------------ helpers

    private static ProfessionRecipes.Recipe find(Container box) {
        return ProfessionRecipes.find(ProfessionRecipes.PHU, inputs(box));
    }

    /** Cac o nguyen lieu (tham chieu song: shrink() tac dong len container). */
    private static List<ItemStack> inputs(Container box) {
        return List.of(box.getItem(0), box.getItem(1), box.getItem(2));
    }

    /** Phu Chi Thuong trong o (tang 1 bac pham). */
    private static boolean thuongChi(Container box) {
        for (int i = 0; i < 3; i++) if (box.getItem(i).is(CraftRegistry.PHU_CHI_THUONG.get())) return true;
        return false;
    }

    private static void takeOne(List<ItemStack> in, ProfessionRecipes.Need need) {
        for (ItemStack s : in) if (!s.isEmpty() && need.accepts(s)) { s.shrink(1); return; }
    }

    private static boolean precheck(ServerPlayer sp, ProfessionRecipes.Recipe r) {
        if (!RecipeBook.knows(sp, RecipeBook.prefixOf(r.prof()) + r.id())) { say(sp, "hukhong.recipe.unknown"); return false; }
        int lv = ProfessionService.level(sp, r.prof());
        if (lv <= 0) { say(sp, r.prof().needKey()); return false; }
        if (lv < r.minLevel()) {
            sp.displayClientMessage(Component.translatable("hukhong.station.level_low", r.minLevel()), true);
            return false;
        }
        return true;
    }

    private static void say(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
    }

    private static void say(ServerPlayer p, String key, Object arg) {
        p.displayClientMessage(Component.translatable(key, arg), true);
    }
}
