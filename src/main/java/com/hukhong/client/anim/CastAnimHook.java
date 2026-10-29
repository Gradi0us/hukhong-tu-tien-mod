package com.hukhong.client.anim;

import com.hukhong.core.Path;
import com.hukhong.skill.BanMenhSkill;
import com.hukhong.skill.Skill;
import com.hukhong.skillbook.BookSkill;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Hook phía server (không nạp lớp client): gọi 1 dòng ở điểm cast thành công để phát tư thế cho mọi người xung quanh. */
public final class CastAnimHook {
    private CastAnimHook() {}

    /** Kỹ năng đã resolve từ SkillService.cast (base hoặc book). */
    public static void play(ServerPlayer p, Skill base, BookSkill book, int slot) {
        CastAnim a = base != null ? (base.path == Path.PHAP ? CastAnimMap.phap(slot) : CastAnimMap.of(base)) : CastAnimMap.of(book);
        send(p, a);
    }

    /** Phím R: tuyệt kỹ Bản Mệnh (sig) hoặc Vạn Cốt Cộng Minh (sig == null). */
    public static void playBanMenh(ServerPlayer p, BanMenhSkill sig) {
        send(p, sig == null ? CastAnimMap.RESONANCE : CastAnimMap.of(sig));
    }

    public static void send(ServerPlayer p, CastAnim a) {
        if (a == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new CastAnimPayload(p.getId(), a.ordinal()));
    }
}
