package com.hukhong.phaptu;

import com.hukhong.charcreate.CharRoll;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import net.minecraft.world.entity.player.Player;

/** Truy cập nhanh dữ liệu Pháp Tu của người chơi (dùng được cả server lẫn client). */
public final class PhapTu {
    private PhapTu() {}

    /** Nguyên tố hiện tại, hoặc null nếu không phải Pháp Tu / chưa có nguyên tố. */
    public static PhapElement element(Player p) {
        if (!p.getData(ModAttachments.CULTIVATION).paths().contains(Path.PHAP)) return null;
        return PhapElement.byId(p.getData(PhapTuRegistry.ELEMENT.get()));
    }

    /** Nguyên tố không kiểm tra đạo (client vẽ người chơi khác không có CultivationData đồng bộ). */
    public static PhapElement rawElement(Player p) { return PhapElement.byId(p.getData(PhapTuRegistry.ELEMENT.get())); }

    public static void setElement(Player p, PhapElement e) { p.setData(PhapTuRegistry.ELEMENT.get(), e == null ? "" : e.id()); }

    public static Aptitude aptitude(Player p) {
        return p.getData(ModAttachments.CULTIVATION).aptitude().getOrDefault(Path.PHAP, Aptitude.B);
    }

    /** Nguyên tố mặc định theo seed roll (dùng khi chưa có: lệnh admin, nhân vật cũ). */
    public static PhapElement defaultElement(CultivationData d, Player p) {
        CharRoll r = d.roll();
        return PhapElement.roll(r != null ? r.seed() : p.getUUID().getLeastSignificantBits(), false);
    }
}
