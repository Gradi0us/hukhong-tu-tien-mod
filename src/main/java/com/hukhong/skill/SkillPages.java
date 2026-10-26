package com.hukhong.skill;

import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Logic thuần: trang kỹ năng đa đạo (phím V). Thứ tự cố định Kiếm, Đạo, Thể, Ma. */
public final class SkillPages {
    private SkillPages() {}

    /** Phân Tâm: hồi chiêu chung khi đổi trang (3 giây). */
    public static final int SWITCH_COOLDOWN_TICKS = 60;

    private static final Path[] ORDER = {Path.KIEM, Path.DAO, Path.THE, Path.MA};

    public static List<Path> owned(Set<Path> paths) {
        List<Path> l = new ArrayList<>(4);
        for (Path p : ORDER) if (paths.contains(p)) l.add(p);
        return l;
    }

    public static String pathLangKey(Path p) {
        return switch (p) {
            case KIEM -> "hukhong.path.sword";
            case DAO -> "hukhong.path.dao";
            case THE -> "hukhong.path.body";
            case MA -> "hukhong.path.demon";
        };
    }

    public static int next(int page, int count) { return count <= 0 ? 0 : (Math.floorMod(page, count) + 1) % count; }

    public static Path active(Set<Path> paths, int page) {
        List<Path> l = owned(paths);
        return l.isEmpty() ? null : l.get(Math.floorMod(page, l.size()));
    }

    public static Path active(CultivationData d, int page) { return active(d.paths(), page); }
}
