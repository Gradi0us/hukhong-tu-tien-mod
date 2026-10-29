package com.hukhong.boss.phase;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload khung boss (gọi từ SkillNet.register). */
public final class BossPhaseNet {
    private BossPhaseNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(CutscenePayload.TYPE, CutscenePayload.STREAM_CODEC, CutscenePayload::handle);
        r.playToClient(BossSkyPayload.TYPE, BossSkyPayload.STREAM_CODEC, BossSkyPayload::handle);
    }
}
