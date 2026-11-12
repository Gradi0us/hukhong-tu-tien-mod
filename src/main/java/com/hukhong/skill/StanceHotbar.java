package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.phapkhi.PhapKhiItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Thế Tu Luyện: thanh nhanh chỉ còn vũ khí bản mệnh (ô 0) + đan/phù (ô 1..8).
 * 9 ô gốc được cất vào attachment {@link Stash} (không rơi, còn lại qua logout).
 * Vật phẩm dùng cho thế được "mượn" từ túi hoặc từ ô gốc đã cất; mỗi vật phẩm nhớ nguồn
 * ({@code origin}) để trả về đúng chỗ. Không nhân đôi, không xoá: mọi di chuyển là chuyển tham chiếu ItemStack.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class StanceHotbar {
    private StanceHotbar() {}

    /** origin = -1: không rõ nguồn (nhặt thêm trong thế) -> ra túi khi thoát. */
    static final int NONE = -1;
    /** origin 100+i: mượn từ ô gốc i đã cất. origin 9..35: mượn từ ô túi. */
    static final int STASH_BASE = 100;
    static final int MAIN_START = StanceHands.HOTBAR;
    static final int INV_SIZE = 36;

    /** Tag đan dược dùng cho thế (ô 1..8). Phù nhận theo class PhapKhiItem. */
    static final TagKey<Item> CONSUMABLE_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "stance_consumable"));

    /** Attachment cất thanh nhanh gốc. Lưu qua logout; đăng nhập lại mà còn active thì trả về. */
    public static final class Stash {
        public static final Codec<Stash> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("home").forGetter(s -> s.home),
                Codec.INT.listOf().fieldOf("origin").forGetter(s -> s.origin),
                Codec.STRING.listOf().fieldOf("type").forGetter(s -> s.type),
                Codec.BOOL.fieldOf("active").forGetter(s -> s.active)
        ).apply(i, Stash::new));

        /** Ô gốc i (đang cất; rỗng nếu đã mượn vào thế). */
        final List<ItemStack> home = new ArrayList<>();
        /** Nguồn của vật phẩm đang nằm ở ô thế i. */
        final List<Integer> origin = new ArrayList<>();
        /** Loại (item id) đã gắn cho ô đan/phù i; "" = chưa gắn. Ô đã gắn chỉ nạp lại cùng loại. */
        final List<String> type = new ArrayList<>();
        boolean active;

        public Stash() { this(List.of(), List.of(), List.of(), false); }

        Stash(List<ItemStack> h, List<Integer> o, List<String> t, boolean a) {
            for (int k = 0; k < StanceHands.HOTBAR; k++) {
                home.add(k < h.size() ? h.get(k).copy() : ItemStack.EMPTY);
                origin.add(k < o.size() ? o.get(k) : NONE);
                type.add(k < t.size() ? t.get(k) : "");
            }
            active = a;
        }
    }

    static boolean isPill(ItemStack s) { return !s.isEmpty() && s.is(CONSUMABLE_TAG); }

    static boolean isTalisman(ItemStack s) { return !s.isEmpty() && s.getItem() instanceof PhapKhiItem; }

    static boolean isConsumable(ItemStack s) { return isPill(s) || isTalisman(s); }

    static String itemId(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).toString(); }

    /** Vào thế: cất 9 ô gốc, rồi lấp thanh nhanh theo đạo hiện tại. */
    public static void enter(ServerPlayer p, Path path) {
        Stash st = p.getData(SkillAttachments.STASH);
        if (st.active) exit(p);
        Inventory inv = p.getInventory();
        for (int i = 0; i < StanceHands.HOTBAR; i++) {
            st.home.set(i, inv.items.get(i));
            inv.items.set(i, ItemStack.EMPTY);
            st.origin.set(i, NONE);
            st.type.set(i, "");
        }
        st.active = true;
        reconcile(p, path);
        inv.selected = 0;
        p.connection.send(new ClientboundSetCarriedItemPacket(0));
        p.inventoryMenu.broadcastChanges();
    }

    /** Ra thế: trả đồ thế về đúng nguồn, khôi phục nguyên 9 ô gốc. Không có chỗ thì rơi dưới chân. */
    public static void exit(ServerPlayer p) {
        Stash st = p.getData(SkillAttachments.STASH);
        if (!st.active) return;
        Inventory inv = p.getInventory();
        List<ItemStack> loose = new ArrayList<>();
        List<Integer> from = new ArrayList<>();
        for (int i = 0; i < StanceHands.HOTBAR; i++) {
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty()) { loose.add(s); from.add(st.origin.get(i)); inv.items.set(i, ItemStack.EMPTY); }
        }
        for (int i = 0; i < StanceHands.HOTBAR; i++) { // khôi phục nguyên thanh nhanh gốc
            ItemStack h = st.home.get(i);
            if (!h.isEmpty()) { inv.items.set(i, h); st.home.set(i, ItemStack.EMPTY); }
        }
        for (int k = 0; k < loose.size(); k++) {
            ItemStack s = loose.get(k);
            int o = from.get(k), dst = NONE;
            if (o >= STASH_BASE && inv.items.get(o - STASH_BASE).isEmpty()) dst = o - STASH_BASE;
            else if (o >= MAIN_START && o < INV_SIZE && inv.items.get(o).isEmpty()) dst = o;
            if (dst == NONE) dst = freeSlot(inv, MAIN_START, INV_SIZE);
            if (dst == NONE) dst = freeSlot(inv, 0, INV_SIZE);
            if (dst != NONE) inv.items.set(dst, s);
            else p.drop(s, false);
        }
        for (int i = 0; i < StanceHands.HOTBAR; i++) { st.origin.set(i, NONE); st.type.set(i, ""); }
        st.active = false;
        p.connection.send(new ClientboundSetCarriedItemPacket(inv.selected));
        p.inventoryMenu.broadcastChanges();
    }

    /** Cân lại thanh nhanh trong thế: đồ lạ ra túi, ô trống lấp lại (hết loại thì để trống). */
    static void reconcile(ServerPlayer p, Path path) {
        Stash st = p.getData(SkillAttachments.STASH);
        Inventory inv = p.getInventory();
        for (int i = 0; i < StanceHands.HOTBAR; i++) { // 1. đồ lạ trên thanh -> túi (không có chỗ thì rơi)
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) continue;
            boolean ok = i == 0 ? StanceHands.isPathWeapon(p, path, s) : isConsumable(s);
            if (ok) continue;
            inv.items.set(i, ItemStack.EMPTY);
            st.origin.set(i, NONE);
            int dst = freeSlot(inv, MAIN_START, INV_SIZE);
            if (dst != NONE) inv.items.set(dst, s);
            else p.drop(s, false);
        }
        if (inv.items.get(0).isEmpty()) { // 2. vũ khí bản mệnh -> ô 0
            int c = find(inv, st, s -> StanceHands.isPathWeapon(p, path, s));
            if (c != NONE) { inv.items.set(0, take(inv, st, c)); st.origin.set(0, c); }
        }
        for (int i = 1; i < StanceHands.HOTBAR; i++) { // 3. đan trước, phù sau -> ô 1..8
            if (!inv.items.get(i).isEmpty()) continue;
            String t = st.type.get(i);
            int c;
            if (!t.isEmpty()) {
                c = find(inv, st, s -> itemId(s).equals(t)); // đã gắn loại: chỉ nạp lại cùng loại
            } else {
                c = find(inv, st, StanceHotbar::isPill);
                if (c == NONE) c = find(inv, st, StanceHotbar::isTalisman);
            }
            if (c == NONE) continue;
            ItemStack s = take(inv, st, c);
            inv.items.set(i, s);
            st.origin.set(i, c);
            st.type.set(i, itemId(s));
        }
    }

    /** Tìm vật phẩm thoả điều kiện trong túi (9..35) trước, rồi ô gốc đã cất. Trả mã nguồn hoặc NONE. */
    static int find(Inventory inv, Stash st, Predicate<ItemStack> ok) {
        for (int i = MAIN_START; i < INV_SIZE; i++) if (ok.test(inv.items.get(i))) return i;
        for (int i = 0; i < StanceHands.HOTBAR; i++) if (ok.test(st.home.get(i))) return STASH_BASE + i;
        return NONE;
    }

    static ItemStack take(Inventory inv, Stash st, int code) {
        if (code >= STASH_BASE) {
            ItemStack s = st.home.get(code - STASH_BASE);
            st.home.set(code - STASH_BASE, ItemStack.EMPTY);
            return s;
        }
        ItemStack s = inv.items.get(code);
        inv.items.set(code, ItemStack.EMPTY);
        return s;
    }

    static int freeSlot(Inventory inv, int from, int to) {
        for (int i = from; i < to; i++) if (inv.items.get(i).isEmpty()) return i;
        return NONE;
    }

    /** Mỗi 5 tick khi thế bật: giữ thanh nhanh đúng bố cục (nhặt thêm, dùng hết, đổi đồ). */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0 || !p.isAlive()) return;
        SkillState ss = SkillService.state(p);
        if (!ss.stance || !p.getData(SkillAttachments.STASH).active) return;
        if (p.containerMenu != p.inventoryMenu) return; // đang mở rương/màn hình: không xáo đồ
        Path path = Skill.skillPath(p.getData(ModAttachments.CULTIVATION), ss.page);
        if (path != null) reconcile(p, path);
    }

    /** Chết: trả thanh nhanh TRƯỚC khi rơi đồ (HIGHEST) để túi rơi đúng đồ thật. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SkillService.forceStanceOff(p);
    }

    /** Đăng nhập lại: thế không lưu runtime, nên nếu còn cất thì trả đồ về ngay. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getData(SkillAttachments.STASH).active) exit(p);
    }
}
