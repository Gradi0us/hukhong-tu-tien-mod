package com.hukhong.linhbao;

import com.hukhong.core.Path;

/** 3 Linh Bảo: bản mệnh không cầm tay của Đạo / Thể / Ma (Kiếm dùng Bản Mệnh Kiếm, Pháp dùng Linh Trượng). id = hậu tố lang. */
public enum LinhBaoType {
    DAO_LINH(Path.DAO, "dao_linh"),
    LINH_THE(Path.THE, "linh_the"),
    MA_CONG(Path.MA, "ma_cong");

    public final Path path;
    public final String id;

    LinhBaoType(Path path, String id) { this.path = path; this.id = id; }

    public static LinhBaoType of(Path p) {
        for (LinhBaoType t : values()) if (t.path == p) return t;
        return null;
    }
}
