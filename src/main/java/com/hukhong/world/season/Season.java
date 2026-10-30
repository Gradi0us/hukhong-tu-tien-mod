package com.hukhong.world.season;

import com.hukhong.world.calendar.LichMath;
import net.minecraft.world.level.Level;

/**
 * 4 mua cua Lich Thien Co (moi mua = 7 ngay Lich, xem {@link LichMath#season}).
 * Mua suy ra tu game time (gameTime la toan cuc moi chieu khong gian) nen client doc cung gia tri voi server:
 * game time duoc dong bo xuong client dinh ky, khong can goi tin rieng.
 */
public enum Season {
    XUAN(0x8CE05A), HA(0x3FA83A), THU(0xE0902A), DONG(0xBFD8E0);

    /** Mau la (tint) cua mua, dung cho hook mau la cay. */
    public final int foliage;

    Season(int foliage) { this.foliage = foliage; }

    public int mask() { return 1 << ordinal(); }

    public String langKey() { return "hukhong.season." + name().toLowerCase(java.util.Locale.ROOT); }

    public static Season of(long gameTime) { return values()[LichMath.season(gameTime)]; }

    /** Mua hien tai cua the gioi (server hay client deu duoc). */
    public static Season current(Level level) { return of(level.getGameTime()); }

    /** Hook mau la: tron mau goc voi mau mua (60%). */
    public static int tint(int rgb, Season s) {
        int r = ((rgb >> 16 & 255) * 2 + (s.foliage >> 16 & 255) * 3) / 5;
        int g = ((rgb >> 8 & 255) * 2 + (s.foliage >> 8 & 255) * 3) / 5;
        int b = ((rgb & 255) * 2 + (s.foliage & 255) * 3) / 5;
        return r << 16 | g << 8 | b;
    }
}
