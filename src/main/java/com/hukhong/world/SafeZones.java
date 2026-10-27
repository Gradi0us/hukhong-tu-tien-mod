package com.hukhong.world;

import com.hukhong.city.CityBuilder;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.city.WorldBuildData;
import com.hukhong.guild.GuildHalls;
import com.hukhong.society.SectBuilder;
import com.hukhong.society.SectDef;
import com.hukhong.society.SectEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Vung an toan (khong quai thu dich sinh tu nhien): thanh, cang, son mon, tru so Hiep Hoi (+ Linh Khi Dai nam trong son mon).
 * Chi doc, dung API cong khai cua city/society/guild. Ban kinh = nua canh cong trinh + {@link #MARGIN}.
 */
public final class SafeZones {
    private SafeZones() {}

    /** Le them ngoai ranh cong trinh (block). */
    public static final int MARGIN = 48;
    /** Nua canh uoc tinh cua tru so Hiep Hoi (GuildBuilder ~ 9x7 + le). */
    public static final int GUILD_HALF = 10;
    /** Nua canh uoc tinh cua khu cang (ben tau) quanh diem cang cua thanh. */
    public static final int PORT_HALF = 24;

    /** Thuan: (x,z) nam trong hinh vuong tam c ban kinh half+margin. */
    public static boolean inBox(double x, double z, int cx, int cz, int half) {
        return Math.abs(x - cx) <= half + MARGIN && Math.abs(z - cz) <= half + MARGIN;
    }

    public static boolean isSafe(ServerLevel l, BlockPos p) {
        if (l.dimension() != Level.OVERWORLD) return false;
        for (CityDef d : CityDef.ALL) {
            BlockPos c = CityService.builtCenter(l, d);
            if (c == null) { if (d.index() == 0) continue; c = CityService.site(l, d); } // site(0) chua dung tra cuu bioma (nang)
            if (inBox(p.getX(), p.getZ(), c.getX(), c.getZ(), CityBuilder.HALF)) return true;
            int[] port = WorldBuildData.get(l).portSite(d.id());
            if (port != null && port.length == 2 && inBox(p.getX(), p.getZ(), port[0], port[1], PORT_HALF)) return true;
        }
        for (SectDef d : SectDef.ALL) {
            BlockPos c = SectEvents.site(l, d);
            if (inBox(p.getX(), p.getZ(), c.getX(), c.getZ(), SectBuilder.HALF)) return true;
        }
        for (BlockPos h : GuildHalls.get(l).all().values()) {
            if (inBox(p.getX(), p.getZ(), h.getX(), h.getZ(), GUILD_HALF)) return true;
        }
        return false;
    }
}
