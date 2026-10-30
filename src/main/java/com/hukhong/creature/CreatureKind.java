package com.hukhong.creature;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.List;

/**
 * 14 sinh vat F4 (khong co thu cuoi). Cau truc (lop, kich thuoc, thuoc tinh goc, bay...) co dinh trong code vi phai co luc
 * dang ky; con so tinh chinh / do roi / sinh san / thuan phuc nam trong JSON (hukhong_creatures).
 */
public enum CreatureKind {
    LINH_LOC("linh_loc", Impl.THU, MobCategory.CREATURE, 0.9f, 1.5f, 14, 2, 0.28, false, null, null),
    HAC_TIEN("hac_tien", Impl.THU, MobCategory.CREATURE, 0.6f, 1.3f, 10, 1, 0.25, true, null, null),
    NGOC_THO("ngoc_tho", Impl.THU, MobCategory.CREATURE, 0.5f, 0.6f, 6, 1, 0.30, false, "spirit", "hukhong:linh_thao"),
    HOA_HUYNH("hoa_huynh", Impl.THU, MobCategory.AMBIENT, 0.3f, 0.3f, 2, 0, 0.12, true, null, null),
    LINH_NGU("linh_ngu", Impl.NGU, MobCategory.WATER_AMBIENT, 0.5f, 0.4f, 4, 0, 1.0, false, null, null),
    LINH_HO("linh_ho", Impl.THU, MobCategory.CREATURE, 0.6f, 0.85f, 16, 3, 0.32, false, "spirit", "hukhong:hoa_lien"),
    THIET_BOI_HUNG("thiet_boi_hung", Impl.THU, MobCategory.CREATURE, 1.5f, 1.7f, 60, 8, 0.22, false, null, null),
    THIET_BOI_HUNG_CON("thiet_boi_hung_con", Impl.THU, MobCategory.CREATURE, 0.8f, 0.9f, 24, 4, 0.28, false, "battle", "hukhong:huyet_tham"),
    DOC_VAN_XA("doc_van_xa", Impl.QUAI, MobCategory.MONSTER, 0.7f, 0.5f, 20, 4, 0.30, false, null, null),
    SA_TRUNG("sa_trung", Impl.QUAI, MobCategory.MONSTER, 1.1f, 0.9f, 30, 6, 0.26, false, null, null),
    BANG_HON("bang_hon", Impl.QUAI, MobCategory.MONSTER, 0.7f, 1.9f, 24, 5, 0.24, false, null, null),
    MOC_YEU("moc_yeu", Impl.QUAI, MobCategory.MONSTER, 0.9f, 2.4f, 40, 7, 0.20, false, null, null),
    DOC_THIEM("doc_thiem", Impl.QUAI, MobCategory.MONSTER, 0.9f, 0.8f, 22, 3, 0.26, false, null, null),
    QUAN_HON("quan_hon", Impl.QUAI, MobCategory.MONSTER, 0.6f, 1.95f, 26, 5, 0.26, false, null, null);

    public enum Impl { THU, QUAI, NGU }

    public final String id;
    public final Impl impl;
    public final MobCategory cat;
    public final float w, h;
    public final double hp, dmg, speed;
    public final boolean fly;
    /** "spirit" (linh sung) | "battle" (thu chien dau) | null = khong thuan phuc duoc. */
    public final String tameSlot;
    public final String tameItem;

    CreatureKind(String id, Impl impl, MobCategory cat, float w, float h, double hp, double dmg, double speed, boolean fly,
                 String tameSlot, String tameItem) {
        this.id = id; this.impl = impl; this.cat = cat; this.w = w; this.h = h; this.hp = hp; this.dmg = dmg;
        this.speed = speed; this.fly = fly; this.tameSlot = tameSlot; this.tameItem = tameItem;
    }

    public CreatureDef def() { return CreatureDefs.get(id); }

    public boolean hostile() { return impl == Impl.QUAI; }

    public static final List<CreatureKind> ALL = List.of(values());

    /** Nguyen lieu roi (vat pham mod): moi sinh vat 1 loai (tru thu chien dau con). */
    public static final List<String> MATERIALS = List.of("linh_loc_giac", "hac_tien_vu", "ngoc_tho_mao", "hoa_huynh_quang", "linh_ngu_lan",
            "linh_ho_vi", "thiet_boi_hung_trao", "doc_van_xa_nha", "sa_trung_giap", "bang_hon_tinh", "moc_yeu_loi",
            "doc_thiem_noc", "quan_hon_manh");

    public static CreatureKind byId(String id) {
        for (CreatureKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }

    public static CreatureKind of(EntityType<?> t) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(t);
        return "hukhong".equals(key.getNamespace()) ? byId(key.getPath()) : null;
    }
}
