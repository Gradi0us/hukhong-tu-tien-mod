package com.hukhong.vfx;

import com.hukhong.skill.SkillScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * F7 (bản không có file âm riêng): xếp 3 lớp âm vanilla cho một kỹ năng - đầu (tức thì, sắc), thân (trầm hơn, vol thấp hơn),
 * đuôi (trễ vài tick, rất trầm, mờ dần). Pitch gốc riêng cho từng kỹ năng để chiêu nghe khác nhau.
 */
public final class FxSfx {
    private FxSfx() {}

    public static void cast(ServerLevel lv, Vec3 at, float vol, float pitch, SoundEvent head, SoundEvent body, SoundEvent tail) {
        play(lv, at, head, vol, pitch);
        if (body != null) play(lv, at, body, vol * 0.7f, pitch * 0.82f);
        if (tail != null) SkillScheduler.later(4, () -> play(lv, at, tail, vol * 0.5f, pitch * 0.6f));
    }

    /** Một lớp trễ delay tick (dùng cho nhịp bùng của chiêu có tụ lực). */
    public static void later(ServerLevel lv, Vec3 at, int delay, float vol, float pitch, SoundEvent s) {
        SkillScheduler.later(delay, () -> play(lv, at, s, vol, pitch));
    }

    private static void play(ServerLevel lv, Vec3 at, SoundEvent s, float vol, float pitch) {
        lv.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, Math.max(0.5f, Math.min(2f, pitch)));
    }
}
