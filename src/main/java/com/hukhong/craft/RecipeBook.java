package com.hukhong.craft;

import com.hukhong.alchemy.AlchemyRecipes;
import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.ProfessionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * API so cong thuc (server). Id thong nhat: "dan:<id>" (Dan Lo, AlchemyRecipes), "phu:<id>" (Phu An, ProfessionRecipes.PHU),
 * "khi:<id>" (Doan Tao Dai, ProfessionRecipes.FORGE).
 *
 * <pre>
 *   RecipeBook.knows(p, id)              // da biet?
 *   RecipeBook.learn(sp, id)             // hoc (dong bo client)
 *   RecipeBook.autoLearnBasics(sp, prof) // hoc moi cong thuc tier <= level/4+1 cua nghe
 *   RecipeBook.entries(prof)             // danh sach cong thuc cua mot nghe (hien thi / so sanh)
 *   RecipeBook.find(id)                  // metadata 1 cong thuc
 * </pre>
 * Them cong thuc vao so: xem docs/GUIDE-v1-crafting-base.md.
 */
public final class RecipeBook {
    private RecipeBook() {}

    public static final String DAN = "dan:", PHU = "phu:", KHI = "khi:";

    /** Nguyen lieu: item + so luong. */
    public record Ing(java.util.function.Supplier<? extends Item> item, int count) {}

    /**
     * Metadata 1 cong thuc. display = stack mau (icon + ten). requiredLevel = cap nghe can de craft / de thay sang.
     */
    public record Entry(String id, Profession prof, int tier, int minLevel,
                        java.util.function.Supplier<ItemStack> display, List<Ing> ingredients) {
        /** Cap nghe can: max(minLevel, 4*(tier-1)) - khop quy tac tu hoc (tier <= level/4+1). */
        public int requiredLevel() { return Math.max(minLevel, 4 * (tier - 1)); }

        public Component name() { return display.get().getHoverName(); }
    }

    /** Tat ca cong thuc cua 3 tram (dong, vi FORGE co the duoc them khi khoi dong). */
    public static List<Entry> all() {
        List<Entry> l = new ArrayList<>();
        for (AlchemyRecipes.Recipe r : AlchemyRecipes.ALL) {
            List<Ing> ings = new ArrayList<>();
            for (AlchemyRecipes.Need n : r.needs()) ings.add(new Ing(n.item(), n.count()));
            l.add(new Entry(DAN + r.id(), Profession.LUYEN_DAN, r.tier(), 1,
                    () -> new ItemStack(r.result().get()), List.copyOf(ings)));
        }
        for (ProfessionRecipes.Recipe r : ProfessionRecipes.PHU) l.add(profEntry(PHU, r, Profession.PHU_VAN));
        for (ProfessionRecipes.Recipe r : ProfessionRecipes.FORGE) l.add(profEntry(KHI, r, Profession.DOAN_TAO));
        return l;
    }

    private static Entry profEntry(String prefix, ProfessionRecipes.Recipe r, Profession prof) {
        List<Ing> ings = new ArrayList<>();
        for (ProfessionRecipes.Need n : r.needs()) ings.add(new Ing(n.item(), n.count()));
        return new Entry(prefix + r.id(), prof, r.tier(), r.minLevel(), () -> r.out().make(1, false), List.copyOf(ings));
    }

    /** Cong thuc cua mot nghe. */
    public static List<Entry> entries(Profession prof) {
        List<Entry> l = new ArrayList<>();
        for (Entry e : all()) if (e.prof() == prof) l.add(e);
        return l;
    }

    public static Entry find(String id) {
        for (Entry e : all()) if (e.id().equals(id)) return e;
        return null;
    }

    /** Id cong thuc cua tram {@code prof}: Đan = dan:, Phù = phu:, Khí = khi:. */
    public static String prefixOf(Profession prof) {
        return switch (prof) {
            case LUYEN_DAN -> DAN;
            case PHU_VAN -> PHU;
            case DOAN_TAO -> KHI;
            default -> "";
        };
    }

    // ---- so cong thuc (server) ----

    /** Attachment hien tai cua nguoi choi. */
    public static Set<String> ids(Player p) { return p.getData(CraftRegistry.KNOWN.get()).ids(); }

    public static boolean knows(Player p, String id) { return ids(p).contains(id); }

    /** Hoc cong thuc. false neu da biet. Dong bo client. */
    public static boolean learn(ServerPlayer p, String id) {
        if (!addQuiet(p, id)) return false;
        sendTo(p);
        return true;
    }

    private static boolean addQuiet(ServerPlayer p, String id) {
        Set<String> cur = ids(p);
        if (cur.contains(id)) return false;
        Set<String> next = new LinkedHashSet<>(cur);
        next.add(id);
        p.setData(CraftRegistry.KNOWN.get(), new KnownRecipes(Set.copyOf(next)));
        return true;
    }

    /**
     * Tu hoc moi cong thuc cua nghe {@code prof} co tier <= cap/4+1 (cap = cap nghe hien tai). Goi khi chon nghe,
     * khi len cap va khi dang nhap (migration nguoi choi cu). Idempotent.
     */
    public static void autoLearnBasics(ServerPlayer p, Profession prof) {
        int lv = ProfessionService.level(p, prof);
        if (lv <= 0) return;
        int limit = lv / 4 + 1;
        boolean changed = false;
        for (Entry e : entries(prof)) if (e.tier() <= limit && addQuiet(p, e.id())) changed = true;
        if (changed) sendTo(p);
    }

    /** Goi tu ProfessionService.addXp khi len cap: tu hoc basics moi + thuong quyen pho moi 4 cap. */
    public static void onLevelUp(ServerPlayer p, Profession prof, int from, int to) {
        autoLearnBasics(p, prof);
        for (int lv = from + 1; lv <= to; lv++) {
            if (lv % 4 != 0) continue;
            ItemStack s = randomScroll(p.getRandom(), prof, 2, Math.min(9, lv / 4 + 2), p);
            if (s.isEmpty()) continue;
            if (!p.getInventory().add(s)) p.drop(s, false);
            say(p, "hukhong.recipe.reward", s.getHoverName());
        }
    }

    /**
     * Quyen pho ngau nhien cua nghe {@code prof} co tier trong [minTier, maxTier]. Neu {@code forPlayer} != null
     * chi chon cong thuc chua biet. Rong neu khong con ung vien.
     */
    public static ItemStack randomScroll(RandomSource r, Profession prof, int minTier, int maxTier, ServerPlayer forPlayer) {
        List<Entry> cand = new ArrayList<>();
        for (Entry e : entries(prof)) {
            if (e.tier() < minTier || e.tier() > maxTier) continue;
            if (forPlayer != null && knows(forPlayer, e.id())) continue;
            cand.add(e);
        }
        if (cand.isEmpty()) return ItemStack.EMPTY;
        Entry pick = cand.get(r.nextInt(cand.size()));
        return CraftRegistry.scroll(kindOf(prof), pick.id());
    }

    public static RecipeScrollItem.Kind kindOf(Profession prof) {
        return switch (prof) {
            case PHU_VAN -> RecipeScrollItem.Kind.PHU;
            case DOAN_TAO -> RecipeScrollItem.Kind.KHI;
            default -> RecipeScrollItem.Kind.DAN;
        };
    }

    /** Dong bo sổ cong thuc cho client. */
    public static void sendTo(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new RecipeBookNet.Sync(List.copyOf(ids(p))));
    }

    static void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }

    // ---- xep nguyen lieu tu tui vao o tram (nut "Xep nguyen lieu") ----

    /**
     * Chuyen nguyen lieu cong thuc {@code id} tu tui vao cac o {@code slots} cua {@code box}. Chi lam khi du nguyen lieu
     * (tui + o hien co). Do dong khong thuoc cong thuc o cac o nay se tra ve tui.
     */
    public static void arrange(ServerPlayer sp, Container box, int[] slots, String id, Profession expect) {
        Entry e = find(id);
        if (e == null || e.prof() != expect || !knows(sp, id)) { say(sp, "hukhong.recipe.unknown"); return; }
        Inventory inv = sp.getInventory();
        List<ItemStack> pool = new ArrayList<>(inv.items);
        pool.addAll(inv.offhand);
        List<Ing> ings = e.ingredients();
        for (Ing g : ings) {
            int have = count(pool, g.item().get());
            for (int s : slots) if (box.getItem(s).is(g.item().get())) have += box.getItem(s).getCount();
            if (have < g.count()) { say(sp, "hukhong.recipe.missing"); return; }
        }
        // do het mon khong thuoc cong thuc ra tui
        for (int s : slots) {
            ItemStack st = box.getItem(s);
            if (st.isEmpty() || ings.stream().anyMatch(g -> st.is(g.item().get()))) continue;
            box.setItem(s, ItemStack.EMPTY);
            giveBack(sp, st);
        }
        // gan moi nguyen lieu vao 1 o (uu tien o cung mon, roi o trong)
        int[] target = new int[ings.size()];
        boolean[] taken = new boolean[slots.length];
        for (int i = 0; i < ings.size(); i++) {
            Item it = ings.get(i).item().get();
            int pick = -1;
            for (int j = 0; j < slots.length && pick < 0; j++) if (!taken[j] && box.getItem(slots[j]).is(it)) pick = j;
            for (int j = 0; j < slots.length && pick < 0; j++) if (!taken[j] && box.getItem(slots[j]).isEmpty()) pick = j;
            if (pick < 0) { say(sp, "hukhong.recipe.no_slot"); return; }
            taken[pick] = true;
            target[i] = slots[pick];
        }
        for (int i = 0; i < ings.size(); i++) {
            Ing g = ings.get(i);
            Item it = g.item().get();
            ItemStack cur = box.getItem(target[i]);
            int need = g.count() - (cur.is(it) ? cur.getCount() : 0);
            while (need > 0) {
                ItemStack src = null;
                for (ItemStack st : pool) if (!st.isEmpty() && st.is(it)) { src = st; break; }
                if (src == null) break; // khong xay ra: da kiem tra o tren
                ItemStack moved = src.split(need);
                need -= moved.getCount();
                ItemStack now = box.getItem(target[i]);
                if (now.isEmpty()) box.setItem(target[i], moved); else now.grow(moved.getCount());
            }
        }
        box.setChanged();
        say(sp, "hukhong.recipe.arranged");
    }

    private static int count(List<ItemStack> pool, Item it) {
        int c = 0;
        for (ItemStack st : pool) if (st.is(it)) c += st.getCount();
        return c;
    }

    private static void giveBack(ServerPlayer sp, ItemStack st) {
        sp.getInventory().add(st);
        if (!st.isEmpty()) sp.drop(st, false);
    }
}
