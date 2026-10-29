package com.hukhong.linhbao;

import com.hukhong.artifact.ArtifactService;
import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.core.ModAttachments;
import net.minecraft.world.entity.player.Player;

/**
 * Khung dùng chung của Linh Bảo. Không có kho dữ liệu riêng: giai đoạn nằm ở ArtifactData (đồng bộ, copyOnDeath, tiến hóa bằng
 * Hồn Tinh như Bản Mệnh Kiếm: ArtifactService.tryUpgrade + nút ở màn Hồ Sơ + Shift-chuột-phải Hồn Tinh). Linh Bảo của một đạo
 * "được cấp" ngay khi nhập đạo đó (suy ra từ danh sách đạo => không thể mất, người chơi cũ tự có khi đăng nhập).
 */
public final class LinhBao {
    private LinhBao() {}

    public static boolean has(Player p, LinhBaoType t) {
        return p.getData(ModAttachments.CULTIVATION).paths().contains(t.path);
    }

    /** Giai đoạn hiệu lực 0..6 (trần theo cấp); 0 nếu chưa nhập đạo. */
    public static int stage(Player p, LinhBaoType t) {
        if (!has(p, t)) return 0;
        return BanMenhMath.effectiveStage(ArtifactService.get(p).get(t.path), p.getData(ModAttachments.CULTIVATION).level());
    }
}
