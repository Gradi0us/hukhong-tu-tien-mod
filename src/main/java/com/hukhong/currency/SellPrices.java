package com.hukhong.currency;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.creature.CreatureRegistry;
import com.hukhong.honcot.HonCotItem;
import com.hukhong.item.ModItems;
import com.hukhong.resource.ResourceRegistry;
import com.hukhong.skillbook.BookSkill;
import com.hukhong.skillbook.SkillBookRegistry;
import com.hukhong.skillbook.SkillScrollItem;
import com.hukhong.skillbook.SkillUpgradeScrollItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gia thu mua cua thuong nhan Khu Cho (Ha). Mot ham {@link #of(ItemStack)} (0 = khong thu mua).
 * <p>Quy tac: (1) mon thuong nhan nao dang ban -> 40% gia mua goc (don vi nho nhat); (2) mon khac -> bang gia tri {@link #VALUE}
 * (Ha/cai): thao duoc 8 loai con lai 10..30, linh thach/khoang san 20..700, nguyen lieu sinh vat 15..80, dan khong ban 400..120000,
 * Tu Linh Dan 400, Di Hoa Chau 1500; (3) Hon Cot = Hon Cot gia * 40% * (80%..120% theo roll); bi tich = 40% gia ban theo pham
 * (Pham/Huyen/Dia/Thien 1200/2400/4800/30000), Truc Nang Cap 40% x 12000. Pham chat (thao duoc / dan) nhan 75/100/125/150%.
 * Loai tru: tien te, Hon Tinh, vat da bind (Ban Menh, Pha Canh Dan da bind...), Van Kien, Linh Truong, trung sinh vat, khoi.
 * Moi gia luon &lt;= 60% gia mua cua cung mon (xem test) nen khong the mua roi ban lai kiem loi.
 */
public final class SellPrices {
    private SellPrices() {}

    public static final int BUYBACK_PCT = 40;
    /** Uy Danh nhan duoc = 50% gia tri ban (mua van 100%). */
    public static final int REP_PCT = 50;
    private static final int[] QUALITY_PCT = {75, 100, 125, 150};
    private static final long[] BOOK_BUY = {1_200L, 2_400L, 4_800L, 30_000L};

    /** Bang gia tri (Ha / cai) cho mon khong co thuong nhan nao ban. Khoa = path vat pham. */
    private static final Map<String, Long> VALUE = new HashMap<>();
    static {
        // 8 thao duoc chua co thuong nhan (4 loai kia lay tu ShopCatalog)
        v(10, "bang_lien"); v(14, "hoa_linh_chi"); v(16, "tu_linh_thao"); v(20, "ngung_hon_hoa");
        v(24, "kim_tinh_thao");
        v(60, "thien_son_tuyet_lien"); v(40, "long_huyet_thao"); v(40, "u_minh_hoa"); v(32, "xich_duong_chi"); v(28, "am_duong_thao");
        v(40, "ngoc_tuy_chi"); v(90, "van_nien_linh_chi"); v(18, "bich_hai_thao"); v(45, "tinh_than_hoa"); v(10, "hoang_tinh");
        v(16, "ha_thu_o"); v(36, "loi_kich_moc");
        v(120, "hoi_linh_dan"); v(900, "dai_hoi_linh_dan"); v(600, "thanh_tam_dan"); v(2_000, "tay_tuy_dan"); v(300, "kim_cuong_dan");
        v(500, "bao_huyet_dan"); v(300, "phong_hanh_dan"); v(4_000, "van_nien_linh_dich"); v(12, "thuy_tam_thao"); v(14, "phong_linh_diep"); v(30, "dia_long_can");
        // linh thach / khoang san (+ khoi quang cung gia nguyen lieu no roi ra)
        v(20, "linh_thach_ha", "linh_thach_ha_ore"); v(120, "linh_thach_trung", "linh_thach_trung_ore");
        v(700, "linh_thach_thuong", "linh_thach_thuong_ore"); v(300, "huyen_thiet", "huyen_thiet_ore");
        v(150, "tinh_than_sa", "tinh_than_sa_ore");
        // nguyen lieu sinh vat
        v(40, "linh_loc_giac"); v(25, "hac_tien_vu"); v(20, "ngoc_tho_mao"); v(45, "hoa_huynh_quang"); v(15, "linh_ngu_lan");
        v(50, "linh_ho_vi"); v(35, "thiet_boi_hung_trao"); v(30, "doc_van_xa_nha"); v(25, "sa_trung_giap"); v(55, "bang_hon_tinh");
        v(60, "moc_yeu_loi"); v(30, "doc_thiem_noc"); v(80, "quan_hon_manh"); v(50, "hu_lang_nanh"); v(70, "thach_quy_tam"); v(60, "am_hon_phu"); v(55, "hoa_tinh_hach"); v(600, "lang_vuong_doc_nha"); v(800, "cu_nhan_thach_tam"); v(700, "huyet_yeu_tinh_huyet"); v(10000, "tiem_nang_thach"); v(60, "huyet_nha_tru_nha"); v(70, "thu_yeu_tam"); v(60, "thuc_nhan_hoa_nhuy"); v(50, "dang_xa_dang"); v(55, "quy_co_bao_tu"); v(900, "thu_vuong_linh_qua"); v(70, "u_anh_bao_trao"); v(55, "tri_chu_thiet_ti"); v(65, "bao_viem_tich_hach"); v(60, "xich_van_ho_bi"); v(75, "hac_giap_te_giac"); v(70, "cuong_luc_vien_tam"); v(55, "bich_nhan_thiem_nang"); v(65, "ngan_lan_mang_lan");
        // dan / vat dung khong ban o cho
        v(3_000, "hoang_cuc_dan"); v(8_000, "de_hoan_dan"); v(20_000, "huyen_ton_dan"); v(50_000, "thanh_co_dan");
        v(120_000, "than_dan"); v(400, "tu_linh_dan"); v(1_500, "di_hoa_chau");
    }

    private static void v(long ha, String... ids) { for (String id : ids) VALUE.put(id, ha); }

    private static volatile Map<Item, Long> catalogUnit;

    /** Gia mua goc nho nhat / 1 cai cua cac mon thuong nhan ban (khong thanh, khong giam gia), lazily. */
    private static Map<Item, Long> catalog() {
        Map<Item, Long> m = catalogUnit;
        if (m == null) {
            m = new HashMap<>();
            for (int k : new int[]{0, 1, 4, ShopCatalog.K_EVO})
                for (ShopCatalog.Offer o : ShopCatalog.buys(0L, k, null, 0L))
                    m.merge(o.stack().getItem(), Math.max(1L, o.price() / Math.max(1, o.stack().getCount())), Math::min);
            catalogUnit = m;
        }
        return m;
    }

    private static int qualityPct(ItemStack s) {
        Integer q = s.get(AlchemyRegistry.PILL_QUALITY.get());
        if (q == null) q = s.get(ResourceRegistry.HERB_QUALITY.get());
        return q == null ? 100 : QUALITY_PCT[Math.max(0, Math.min(3, q))];
    }

    /** Gia thu mua 1 cai (Ha), 0 neu khong thu mua. */
    public static long unit(ItemStack s) {
        if (s.isEmpty() || s.has(ModItems.BOUND_TO.get())) return 0;
        Item it = s.getItem();
        if (it == CurrencyRegistry.HA.get() || it == CurrencyRegistry.TRUNG.get() || it == CurrencyRegistry.THUONG.get()) return 0;
        if (it == ModItems.HON_TINH.get()) return HON_TINH_SELL;
        var key = BuiltInRegistries.ITEM.getKey(it);
        if (!HuKhong.MODID.equals(key.getNamespace())) return 0;
        if (it instanceof HonCotItem) {
            long full = ShopCatalog.BONE_PRICE[HonCotItem.tierOf(s)];
            return Math.max(1, full * BUYBACK_PCT * (80 + HonCotItem.rollOf(s) * 40 / 100) / 10_000);
        }
        if (it instanceof SkillScrollItem) {
            BookSkill b = SkillBookRegistry.skillOf(s);
            return b == null ? 0 : BOOK_BUY[b.grade.ordinal()] * BUYBACK_PCT / 100;
        }
        if (it instanceof SkillUpgradeScrollItem) return ShopCatalog.SCROLL_PRICE * BUYBACK_PCT / 100;
        if (it instanceof com.hukhong.phapkhi.PhapKhiItem pk) { // Phap Khi: thu mua theo so lan con lai
            return Math.max(1, pk.kind.price(pk.tier) * BUYBACK_PCT / 100 * com.hukhong.phapkhi.PhapKhiItem.usesLeft(s) / Math.max(1, pk.maxUses()));
        }
        Long cat = catalog().get(it);
        if (cat != null) return Math.max(1, cat * BUYBACK_PCT * qualityPct(s) / 10_000);
        Long val = VALUE.get(key.getPath());
        return val == null ? 0 : Math.max(1, val * qualityPct(s) / 100);
    }

    /** Hon Tinh (trong vi) ban lai = 50% gia mua 12 Ha. */
    public static final long HON_TINH_SELL = 6;

    public static boolean isHonTinh(ItemStack s) { return s.is(ModItems.HON_TINH.get()); }

    /** Gia thu mua ca chong (Ha); 0 neu khong thu mua. */
    public static long of(ItemStack s) { return unit(s) * Math.max(0, s.getCount()); }

    /** Mot dong "Ban": cac chong giong nhau gop lai (proto = 1 cai). */
    public record Row(ItemStack proto, int count, long unit) {
        public long total() { return unit * count; }
    }

    /** Cac mon thu mua duoc trong tui (gop chong, sap theo ten vat pham), thu tu on dinh giua client va server. */
    public static List<Row> rows(Player p) {
        List<Row> out = new ArrayList<>();
        long hon = Wallet.honTinh(p);
        if (hon > 0) out.add(new Row(new ItemStack(ModItems.HON_TINH.get()), (int) Math.min(Integer.MAX_VALUE, hon), HON_TINH_SELL));
        for (ItemStack s : p.getInventory().items) {
            long u = isHonTinh(s) ? 0 : unit(s);
            if (u <= 0) continue;
            int idx = -1;
            for (int i = 0; i < out.size(); i++) if (ItemStack.isSameItemSameComponents(out.get(i).proto(), s)) { idx = i; break; }
            if (idx >= 0) { Row r = out.get(idx); out.set(idx, new Row(r.proto(), r.count() + s.getCount(), r.unit())); }
            else out.add(new Row(s.copyWithCount(1), s.getCount(), u));
        }
        out.sort(Comparator.<Row, String>comparing(r -> BuiltInRegistries.ITEM.getKey(r.proto().getItem()).toString())
                .thenComparingLong(Row::unit).thenComparing(r -> r.proto().getHoverName().getString()));
        return out;
    }

    /** Lay toi da want cai giong proto khoi tui; tra so cai lay duoc. */
    public static int take(Player p, ItemStack proto, int want) {
        if (isHonTinh(proto)) { // Hon Tinh nam trong vi
            int n = (int) Math.min(want, Wallet.honTinh(p));
            if (n > 0) Wallet.setHonTinh(p, Wallet.honTinh(p) - n);
            return n;
        }
        int got = 0;
        for (ItemStack s : p.getInventory().items) {
            if (got >= want) break;
            if (!ItemStack.isSameItemSameComponents(s, proto)) continue;
            int t = Math.min(want - got, s.getCount());
            s.shrink(t);
            got += t;
        }
        return got;
    }
}
