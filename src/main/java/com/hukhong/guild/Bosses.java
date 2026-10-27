package com.hukhong.guild;

import com.hukhong.chg.ChapHanhGia;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuVuong;
import com.hukhong.story.SuDo;
import net.minecraft.world.entity.Entity;

/** Nhan dien boss cho Tu Tinh / nhiem vu san: Hu Vuong, Su Do, Chap Hanh Gia. */
public final class Bosses {
    private Bosses() {}

    public static boolean isBoss(Entity e) { return e instanceof HuVuong || e instanceof SuDo || e instanceof ChapHanhGia; }

    public static int levelOf(Entity e) {
        if (e instanceof SuDo s) return s.suLevel();
        if (e instanceof ChapHanhGia) return 60;
        return e instanceof HuMob m ? m.huLevel() : 1;
    }

    /** He so tien: Chap Hanh Gia x2. */
    public static int coinMult(Entity e) { return e instanceof ChapHanhGia ? 2 : 1; }
}
