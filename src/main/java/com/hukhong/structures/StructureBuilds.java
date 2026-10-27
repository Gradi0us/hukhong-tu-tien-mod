package com.hukhong.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Consumer;

/** Cau noi giua CityDef/SectDef va he thong import: chon file -&gt; xep hang dat -&gt; tra tam (hoac null = dung builder thu tuc). */
public final class StructureBuilds {
    private StructureBuilds() {}

    /** Vi tri neo gameplay (Ấn Hồn Đài / Linh Khí Đài) du kien cua 1 cau truc dat o (cx,cz) voi mat nen y0. */
    public static BlockPos anchorOf(LoadedStructure st, String anchorMarker, int cx, int cz, int y0) {
        BlockPos origin = new BlockPos(cx - st.sx / 2, y0, cz - st.sz / 2);
        LoadedStructure.Marker m = st.marker(anchorMarker);
        return m != null ? origin.offset(m.x(), m.y(), m.z()) : new BlockPos(cx, y0 + 1, cz);
    }

    /**
     * Thu dung bang file. Neu co file hop le: xep hang dat bat dong bo va tra TAM (y = mat nen, neo = tam.above()).
     * {@code gameplay} chay sau khi dat xong. Neu khong co file: tra null (caller dung builder thu tuc).
     */
    public static BlockPos tryBuild(ServerLevel l, String category, String key, BlockPos site, String anchorMarker,
                                    Consumer<StructurePlacer.Placed> gameplay) {
        return tryBuild(l, category, key, site, anchorMarker, null, gameplay);
    }

    /** Nhu tren; originY != null = ep mat nen (vd cang: muc bien - 1 de lop nuoc cua cau truc khop mat bien). */
    public static BlockPos tryBuild(ServerLevel l, String category, String key, BlockPos site, String anchorMarker,
                                    Integer originY, Consumer<StructurePlacer.Placed> gameplay) {
        StructureLibrary.ensureUserDirs();
        LoadedStructure st = StructureLibrary.pick(l.getServer(), category, key, l.getSeed());
        if (st == null) return null;
        int cx = site.getX(), cz = site.getZ();
        int y0 = originY != null ? originY : StructurePlacer.surfaceY(l, cx, cz);
        BlockPos origin = new BlockPos(cx - st.sx / 2, y0, cz - st.sz / 2);
        BlockPos anchor = anchorOf(st, anchorMarker, cx, cz, y0);
        StructurePlacer.enqueue(l, st, origin, gameplay);
        return anchor.below();
    }
}
