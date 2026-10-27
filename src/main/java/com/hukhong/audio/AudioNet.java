package com.hukhong.audio;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Dang ky payload am thanh (goi tu SkillNet.register). */
public final class AudioNet {
    private AudioNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(BossMusicPayload.TYPE, BossMusicPayload.STREAM_CODEC, BossMusicPayload::handle);
    }
}
