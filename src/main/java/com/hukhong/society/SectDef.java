package com.hukhong.society;

import com.hukhong.city.Tier;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;

import java.util.List;
import java.util.Set;

/** 7 tông (canon §7, S6 §2.2). cả 7 đều dựng được sơn môn; Huyết Ảnh là tông ẩn (chỉ mời, Ma tu có Ác Danh). */
public record SectDef(String id, Tier tier, int ax, int az, int minLevel, Aptitude minApt, int maxAge,
                      Set<Path> paths, boolean multiPath, boolean banMa, boolean buildable, boolean hidden) {

    public static final List<SectDef> ALL = List.of(
            new SectDef("ba_the_mon", Tier.NHAN, 400, 2200, 10, Aptitude.C, 55, Set.of(Path.THE), false, false, true, false),
            new SectDef("dan_dinh_tong", Tier.NHAN, 3400, 800, 10, Aptitude.D, 55, Set.of(), false, false, true, false),
            new SectDef("thien_cong_khi_cac", Tier.NHAN, -3000, 3400, 10, Aptitude.D, 55, Set.of(), false, false, true, false),
            new SectDef("thien_kiem_tong", Tier.DIA, -3000, -6400, 20, Aptitude.B, 45, Set.of(Path.KIEM), false, false, true, false),
            new SectDef("quang_minh_dao_cung", Tier.DIA, -6800, 1800, 20, Aptitude.B, 45, Set.of(Path.DAO), false, true, true, false),
            new SectDef("van_tuong_thanh_tong", Tier.THIEN, -6000, 7200, 30, Aptitude.A, 40, Set.of(), true, false, true, false),
            new SectDef("huyet_anh_ma_mon", Tier.NHAN, -3200, -3000, 10, Aptitude.C, 55, Set.of(Path.MA), false, false, true, true));

    /** The loai cau truc: data/hukhong/structure/sect/ hoac config/hukhong/structures/sect/. */
    public String category() { return "sect"; }

    public String nameKey() { return "hukhong.sect.n." + id; }

    public static SectDef byId(String id) {
        for (SectDef d : ALL) if (d.id.equals(id)) return d;
        return null;
    }
}
