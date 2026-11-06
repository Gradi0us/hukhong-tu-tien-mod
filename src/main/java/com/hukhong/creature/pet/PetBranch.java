package com.hukhong.creature.pet;

import com.hukhong.auction.Tx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 4 nhanh tien hoa cua linh thu. Tang 1 chon o cap 30 (xuc tac cua nhanh), tang 2 o cap 60 (can than mat >= 50, cung xuc tac).
 * Xuc tac dung vat pham mod san co (nguyen lieu sinh vat / Nguyen Khong Tinh Trung cho nhanh hiem).
 */
public enum PetBranch {
    HUYET_SAT("huyet_sat", "Huyết Sát", "Blood Slayer", 0xE03A3A, "huyet_yeu_tinh_huyet", 0.00, 0.35, 0.05, 0, 240, false),
    HUYEN_GIAP("huyen_giap", "Huyền Giáp", "Mystic Armor", 0x4A7FD0, "thach_quy_tam", 0.40, 0.00, 0.00, 6, 300, false),
    LINH_PHONG("linh_phong", "Linh Phong", "Spirit Wind", 0x50D070, "linh_loc_giac", -0.10, 0.10, 0.20, 0, 280, false),
    HU_KHONG("hu_khong", "Hư Không", "Void", 0x9050E0, "nguyen_khong_tinh_trung", 0.15, 0.25, 0.10, 2, 200, true);

    public final String id, vi, en;
    public final int rgb;
    public final String catalyst;
    /** Cong them cho chi so o tang 1 (tang 2 nhan 1.6). */
    public final double hp, dmg, spd, armor;
    /** Hoi chieu ky nang chu dong (tick, tang 2 giam 25%). */
    public final int cooldown;
    public final boolean rare;

    PetBranch(String id, String vi, String en, int rgb, String catalyst, double hp, double dmg, double spd, double armor, int cooldown, boolean rare) {
        this.id = id; this.vi = vi; this.en = en; this.rgb = rgb; this.catalyst = catalyst;
        this.hp = hp; this.dmg = dmg; this.spd = spd; this.armor = armor; this.cooldown = cooldown; this.rare = rare;
    }

    public String catalystId() { return "hukhong:" + catalyst; }

    public boolean isCatalyst(ItemStack s) {
        return !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).equals(ResourceLocation.parse(catalystId()));
    }

    public Component displayName() { return Tx.t("hukhong.pet.branch." + id, vi, en); }

    public Component formName(int stage) {
        return stage >= 2 ? Tx.t("hukhong.pet.form." + id + ".2", vi + " Chân Thân", en + " True Form")
                : Tx.t("hukhong.pet.form." + id + ".1", vi, en);
    }

    public static PetBranch byId(String id) {
        for (PetBranch b : values()) if (b.id.equals(id)) return b;
        return null;
    }

    public static PetBranch byCatalyst(ItemStack s) {
        for (PetBranch b : values()) if (b.isCatalyst(s)) return b;
        return null;
    }
}
