package com.hukhong.boss.phase;

import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Cutscene giải phóng vùng: camera lùi ra rồi ngẩng lên trời, trời tím tan dần, ánh sáng trở lại. Chỉ người trong 60 block xem; ai cũng bỏ qua được. */
public final class LiberationCutscene {
    private LiberationCutscene() {}

    public static final int TICKS = 150;
    public static final List<CamPath.Key> KEYS = List.of(
            new CamPath.Key(0, 0, 3, 7, 0, 0, 62, 2.5f),
            new CamPath.Key(50, -4, 8, 12, 0, 4, 68, 4f),
            new CamPath.Key(105, 0, 22, 28, 0, -12, 76, 12f),
            new CamPath.Key(TICKS, 0, 30, 34, 0, -24, 80, 20f));
    public static final List<SubLine> SUBS = List.of(
            new SubLine(8, 70, "hukhong.boss.sub.liberation.1"),
            new SubLine(76, 140, "hukhong.boss.sub.liberation.2"));

    /** {@code surface} = mặt đất ngay trên Tâm vừa bị phá. */
    public static void play(ServerLevel ow, BlockPos surface) {
        CutscenePayload cs = new CutscenePayload(-1, surface.getX() + 0.5, surface.getY() + 1.5, surface.getZ() + 0.5, 0f, TICKS, KEYS, SUBS);
        BossSkyPayload sky = new BossSkyPayload(1f, 0f, 0, 70, 90);
        for (ServerPlayer p : ow.players()) {
            if (Math.hypot(p.getX() - surface.getX(), p.getZ() - surface.getZ()) > BossPhaseDirector.CUTSCENE_RANGE) continue;
            try {
                PacketDistributor.sendToPlayer(p, cs);
                PacketDistributor.sendToPlayer(p, sky);
            } catch (RuntimeException ignored) { }
        }
    }
}
