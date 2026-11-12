package com.hukhong.profession.khi;

import com.hukhong.HuKhong;
import com.hukhong.craft.CraftQuality;
import com.hukhong.craft.CraftRegistry;
import com.hukhong.craft.RecipeBook;
import com.hukhong.phapkhi.PhapKhiTier;
import com.hukhong.phapkhi.hothe.HoTheArmorItem;
import com.hukhong.phapkhi.hothe.HoTheKind;
import com.hukhong.phapkhi.hothe.HoTheRegistry;
import com.hukhong.phapkhi.hothe.HoTheStats;
import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.ProfessionRegistry;
import com.hukhong.profession.ProfessionService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Doan Tao Dai (server): Nung -> Ron -> Toi (PLAN-v1 §3). Phien dang chay giu theo nguoi choi; nguyen lieu nam
 * trong 3 o cho toi khi xong (kiem tra chu ky o moi thao tac). Tinh luyen / sua / phan ra la thao tac don le.
 * Thao tac qua StationMenu.forge -> handle(). Moi lan handle xong gui State cho client.
 */
public final class ForgeService {
    private ForgeService() {}

    public static final int OP_SYNC = 0, OP_START = 1, OP_PULL = 2, OP_STRIKE = 3,
            OP_Q_WATER = 4, OP_Q_TOI = 5, OP_Q_DAU = 6, OP_CANCEL = 7,
            OP_REFINE = 8, OP_REPAIR = 9, OP_DISMANTLE = 10;

    public static final int PH_IDLE = 0, PH_HEAT = 1, PH_HAMMER = 2, PH_QUENCH = 3;

    /** Ti le thanh cong tinh luyen theo cap hien tai +0..+4 -> +1..+5. */
    private static final double[] REFINE_CHANCE = {1.0, 0.85, 0.70, 0.55, 0.40};

    private static final int OUT = 3;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private static final class Session {
        final ProfessionRecipes.Recipe recipe;
        final int tier, lv;
        final long seed, startTick;
        final List<String> snapshot;
        int phase = PH_HEAT, lastTick, strikes, heatPts, hammerSum;

        Session(ProfessionRecipes.Recipe recipe, int tier, int lv, long seed, long startTick, List<String> snapshot) {
            this.recipe = recipe;
            this.tier = tier;
            this.lv = lv;
            this.seed = seed;
            this.startTick = startTick;
            this.snapshot = snapshot;
        }
    }

    /** Diem vao. box = Container tram (o 0..2 nguyen lieu, o 3 ket qua). */
    public static void handle(ServerPlayer sp, Container box, int op, long tick) {
        if (op == OP_SYNC) { sendState(sp); return; }
        Session s = SESSIONS.get(sp.getUUID());
        switch (op) {
            case OP_START -> start(sp, box);
            case OP_PULL, OP_STRIKE -> press(sp, box, s, op, tick);
            case OP_Q_WATER, OP_Q_TOI, OP_Q_DAU -> quench(sp, box, s, op);
            case OP_CANCEL -> {
                if (s != null) { SESSIONS.remove(sp.getUUID()); say(sp, "hukhong.forge.cancelled"); }
            }
            case OP_REFINE, OP_REPAIR, OP_DISMANTLE -> {
                if (s != null) say(sp, "hukhong.forge.busy");
                else armorOp(sp, box, op);
            }
            default -> { }
        }
        sendState(sp);
    }

    // ---- 3.1 Nung ----

    private static void start(ServerPlayer sp, Container box) {
        if (SESSIONS.containsKey(sp.getUUID())) { say(sp, "hukhong.forge.busy"); return; }
        if (!box.getItem(OUT).isEmpty()) { say(sp, "hukhong.station.out_full"); return; }
        ProfessionRecipes.Recipe r = ProfessionRecipes.find(ProfessionRecipes.FORGE, inputs(box));
        if (r == null) { say(sp, "hukhong.station.no_recipe"); return; }
        if (!RecipeBook.knows(sp, RecipeBook.KHI + r.id())) { say(sp, "hukhong.recipe.unknown"); return; }
        int lv = ProfessionService.level(sp, Profession.DOAN_TAO);
        if (lv <= 0) { say(sp, Profession.DOAN_TAO.needKey()); return; }
        if (lv < r.minLevel()) {
            sp.displayClientMessage(Component.translatable("hukhong.station.level_low", r.minLevel()), true);
            return;
        }
        if (!take(sp, CraftRegistry.LINH_THAN.get())) { say(sp, "hukhong.forge.need_fuel"); return; }
        SESSIONS.put(sp.getUUID(), new Session(r, ForgeMath.clampTier(r.tier()), lv, sp.getRandom().nextLong(),
                sp.level().getGameTime(), signature(box)));
        say(sp, "hukhong.forge.heat_start");
    }

    // ---- 3.1 Rut phoi / 3.2 Dap ----

    private static void press(ServerPlayer sp, Container box, Session s, int op, long tick) {
        if (s == null) return;
        if (!signature(box).equals(s.snapshot)) { changed(sp); return; }
        long now = sp.level().getGameTime() - s.startTick;
        if (tick < 0 || Math.abs(tick - now) > ForgeMath.TIME_TOL || tick < s.lastTick + ForgeMath.MIN_GAP) {
            say(sp, "hukhong.forge.timing");
            return;
        }
        int t = (int) tick;
        if (op == OP_PULL) {
            if (s.phase != PH_HEAT) return;
            int sc = ForgeMath.heatScore(s.seed, s.tier, t);
            s.heatPts = sc;
            s.lastTick = t;
            s.phase = PH_HAMMER;
            say(sp, "hukhong.forge.pulled", sc);
        } else {
            if (s.phase != PH_HAMMER) return;
            int sc = ForgeMath.strikeScore(s.seed, s.tier, s.lv, s.strikes, t);
            s.hammerSum += sc;
            s.lastTick = t;
            s.strikes++;
            say(sp, "hukhong.forge.hit." + sc);
            if (s.strikes >= ForgeMath.strikes(s.tier)) s.phase = PH_QUENCH;
        }
    }

    // ---- 3.3 Toi + 3.4 ket qua ----

    private static void quench(ServerPlayer sp, Container box, Session s, int op) {
        if (s == null || s.phase != PH_QUENCH) return;
        if (!signature(box).equals(s.snapshot)) { changed(sp); return; }
        if (!box.getItem(OUT).isEmpty()) { say(sp, "hukhong.station.out_full"); return; }
        int quenchPts = 0;
        boolean cracked = false;
        if (op == OP_Q_TOI) {
            if (!take(sp, CraftRegistry.TOI_DICH.get())) { say(sp, "hukhong.forge.need_toi"); return; }
            quenchPts = 1;
        } else if (op == OP_Q_DAU) {
            if (!take(sp, CraftRegistry.DAU_THU.get())) { say(sp, "hukhong.forge.need_dau"); return; }
            float roll = sp.getRandom().nextFloat();
            if (roll < 0.20f) cracked = true;
            else if (roll < 0.50f) quenchPts = 2;
        }
        int total = s.heatPts + ForgeMath.hammerPts(s.hammerSum, ForgeMath.strikes(s.tier)) + quenchPts + materialPts(box);
        int q = ForgeMath.quality(total);
        if (cracked) q = Math.max(CraftQuality.HA, q - 1);
        boolean up = ProfessionService.rollTierBonus(sp, sp.getRandom()) > 0;
        ItemStack made = s.recipe.out().make(s.lv, up);
        CraftQuality.stamp(made, q, sp.getName().getString());
        HoTheStats.refresh(made);

        ProfessionRecipes.consume(s.recipe, inputs(box));
        clearEmpty(box);
        box.setItem(OUT, made);
        box.setChanged();
        long xp = Math.round((20 + 15L * s.tier) * (1 + 0.25 * q));
        ProfessionService.addCraftXp(sp, Profession.DOAN_TAO, xp);
        SESSIONS.remove(sp.getUUID());
        say(sp, cracked ? "hukhong.forge.done_cracked" : "hukhong.forge.done",
                made.getHoverName(), CraftQuality.tooltip(q, null));
    }

    // ---- 3.5 Tinh luyen / 3.6 Sua / Phan ra ----

    private static void armorOp(ServerPlayer sp, Container box, int op) {
        ItemStack a = box.getItem(0);
        if (!(a.getItem() instanceof HoTheArmorItem h)) { say(sp, "hukhong.forge.need_armor"); return; }
        switch (op) {
            case OP_REFINE -> refine(sp, box, a, h);
            case OP_REPAIR -> repair(sp, box, a);
            case OP_DISMANTLE -> dismantle(sp, box, h);
            default -> { }
        }
    }

    /** Tinh luyen: giap + 2 phoi khung cung bac (o 1-2). Thanh cong +1; tu +3 co the tut 1 khi that bai. */
    private static void refine(ServerPlayer sp, Container box, ItemStack a, HoTheArmorItem h) {
        if (ProfessionService.level(sp, Profession.DOAN_TAO) <= 0) { say(sp, Profession.DOAN_TAO.needKey()); return; }
        if (!box.getItem(OUT).isEmpty()) { say(sp, "hukhong.station.out_full"); return; }
        int r = HoTheStats.refine(a);
        if (r >= HoTheStats.REFINE_MAX) { say(sp, "hukhong.forge.refine_max"); return; }
        Item frame = frameOf(h.kind, h.tier);
        if (frame == null || countSlots(box, frame, 1, 2) < 2 || !onlySlots(box, frame, 1, 2)) {
            say(sp, "hukhong.forge.need_frame");
            return;
        }
        takeFromSlots(box, frame, 2, 1, 2);
        boolean ok = sp.getRandom().nextFloat() < REFINE_CHANCE[r];
        int nr = r;
        if (ok) nr = r + 1;
        else if (r >= 3 && sp.getRandom().nextBoolean()) nr = r - 1;
        box.setItem(0, ItemStack.EMPTY);
        HoTheStats.setRefine(a, nr);
        HoTheStats.refresh(a);
        box.setItem(OUT, a);
        box.setChanged();
        if (ok) {
            ProfessionService.addCraftXp(sp, Profession.DOAN_TAO, 10L * nr);
            say(sp, "hukhong.forge.refine_ok", nr);
        } else if (nr < r) say(sp, "hukhong.forge.refine_drop", nr);
        else say(sp, "hukhong.forge.refine_fail");
    }

    /** Sua: 1 Linh Thiet Phoi (o 1) hoi 25% do ben toi da. */
    private static void repair(ServerPlayer sp, Container box, ItemStack a) {
        if (!a.isDamageableItem() || a.getDamageValue() <= 0) { say(sp, "hukhong.forge.need_damaged"); return; }
        ItemStack ph = box.getItem(1);
        if (!ph.is(ProfessionRegistry.LINH_THIET_PHOI.get())) { say(sp, "hukhong.forge.need_phoi"); return; }
        if (!box.getItem(OUT).isEmpty()) { say(sp, "hukhong.station.out_full"); return; }
        ph.shrink(1);
        if (ph.isEmpty()) box.setItem(1, ItemStack.EMPTY);
        a.setDamageValue(Math.max(0, a.getDamageValue() - a.getMaxDamage() / 4));
        box.setItem(0, ItemStack.EMPTY);
        box.setItem(OUT, a);
        box.setChanged();
        say(sp, "hukhong.forge.repaired");
    }

    /** Phan ra: tra 50% (lam tron xuong) nguyen lieu cong thuc vao tui nguoi choi. Mat tinh luyen. */
    private static void dismantle(ServerPlayer sp, Container box, HoTheArmorItem h) {
        ProfessionRecipes.Recipe rec = hotheRecipe(h.kind, h.tier);
        if (rec == null) return;
        box.setItem(0, ItemStack.EMPTY);
        box.setChanged();
        for (ProfessionRecipes.Need n : rec.needs()) {
            int c = n.count() / 2;
            if (c <= 0) continue;
            ItemStack back = new ItemStack(n.item().get(), c);
            if (!sp.getInventory().add(back)) sp.drop(back, false);
        }
        say(sp, "hukhong.forge.dismantled");
    }

    // ---- helpers ----

    /** Cong thuc giap (id hothe_<loai>_<phẩm>) trong bang FORGE. */
    static ProfessionRecipes.Recipe hotheRecipe(HoTheKind k, PhapKhiTier t) {
        String id = "hothe_" + HoTheRegistry.id(k, t);
        for (ProfessionRecipes.Recipe r : ProfessionRecipes.FORGE) if (r.id().equals(id)) return r;
        return null;
    }

    /** Phoi khung cua bac giap = nguyen lieu dau tien cua cong thuc giap. */
    private static Item frameOf(HoTheKind k, PhapKhiTier t) {
        ProfessionRecipes.Recipe r = hotheRecipe(k, t);
        return r == null ? null : r.needs().get(0).item().get();
    }

    private static List<ItemStack> inputs(Container box) {
        List<ItemStack> in = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) in.add(box.getItem(i));
        return in;
    }

    /** Chu ky 3 o nguyen lieu (id, phẩm, so luong) de phat hien o bi doi giua phien. */
    private static List<String> signature(Container box) {
        List<String> sig = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            ItemStack s = box.getItem(i);
            sig.add(s.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(s.getItem()) + "|"
                    + CraftQuality.ordinal(s) + "|" + s.getCount());
        }
        return sig;
    }

    /** Phẩm trung binh nguyen lieu lam tron (0..3). */
    private static int materialPts(Container box) {
        int sum = 0, n = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack s = box.getItem(i);
            if (s.isEmpty()) continue;
            sum += CraftQuality.ordinal(s);
            n++;
        }
        return n == 0 ? 0 : (int) Math.round((double) sum / n);
    }

    private static void clearEmpty(Container box) {
        for (int i = 0; i < 3; i++) if (box.getItem(i).isEmpty()) box.setItem(i, ItemStack.EMPTY);
    }

    private static int countSlots(Container box, Item item, int from, int to) {
        int c = 0;
        for (int i = from; i <= to; i++) if (box.getItem(i).is(item)) c += box.getItem(i).getCount();
        return c;
    }

    private static boolean onlySlots(Container box, Item item, int from, int to) {
        for (int i = from; i <= to; i++) if (!box.getItem(i).isEmpty() && !box.getItem(i).is(item)) return false;
        return true;
    }

    private static void takeFromSlots(Container box, Item item, int amount, int from, int to) {
        int left = amount;
        for (int i = from; i <= to && left > 0; i++) {
            ItemStack s = box.getItem(i);
            if (!s.is(item)) continue;
            int t = Math.min(left, s.getCount());
            s.shrink(t);
            left -= t;
            if (s.isEmpty()) box.setItem(i, ItemStack.EMPTY);
        }
        box.setChanged();
    }

    /** Lay 1 mon tu tui nguoi choi. false neu khong co. */
    private static boolean take(ServerPlayer sp, Item item) {
        Predicate<ItemStack> p = s -> s.is(item);
        if (sp.getInventory().clearOrCountMatchingItems(p, 0, sp.getInventory()) < 1) return false;
        sp.getInventory().clearOrCountMatchingItems(p, 1, sp.getInventory());
        return true;
    }

    private static void changed(ServerPlayer sp) {
        SESSIONS.remove(sp.getUUID());
        say(sp, "hukhong.forge.changed");
    }

    private static void sendState(ServerPlayer sp) {
        Session s = SESSIONS.get(sp.getUUID());
        ForgeNet.State st = s == null ? ForgeNet.State.IDLE
                : new ForgeNet.State(s.phase, s.startTick, s.seed, s.tier, s.lv, s.strikes, s.heatPts, s.hammerSum);
        ForgeNet.sendTo(sp, st);
    }

    private static void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }

    /** Huy phien khi nguoi choi thoat (nguyen lieu van nam trong o, nhien lieu khong hoan lai). */
    @EventBusSubscriber(modid = HuKhong.MODID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
            SESSIONS.remove(e.getEntity().getUUID());
        }
    }
}
