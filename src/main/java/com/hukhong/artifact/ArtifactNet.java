package com.hukhong.artifact;

import com.hukhong.core.Path;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload pháp khí (gọi từ SkillNet.register để dùng chung registrar). */
public final class ArtifactNet {
    private ArtifactNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToServer(ArtifactUpgradePayload.TYPE, ArtifactUpgradePayload.STREAM_CODEC, (pl, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            Path[] all = Path.values();
            if (pl.path() < 0 || pl.path() >= all.length) return;
            Path path = all[pl.path()];
            ctx.enqueueWork(() -> ArtifactService.tryUpgrade(sp, path));
        });
    }
}
