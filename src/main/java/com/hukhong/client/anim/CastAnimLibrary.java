package com.hukhong.client.anim;

import dev.kosmx.playerAnim.core.data.AnimationFormat;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Dựng keyframe cho từng {@link CastAnim} bằng code (không cần file JSON). Chỉ chạm lõi playerAnimator
 * (không chạm lớp Minecraft client) nên dựng/kiểm tra được trong unit test.
 * Quy ước (radian, hệ trục mô hình vanilla): tay phải/trái pitch -1,57 = giơ ngang về trước, -3,1 = giơ thẳng lên;
 * tay phải roll + = dang ra ngoài (tay trái roll - = ra ngoài); yaw tay phải - / tay trái + = khép vào giữa;
 * body pitch + = cúi về trước; chân pitch - = nhấc về trước.
 */
public final class CastAnimLibrary {
    private CastAnimLibrary() {}

    private static final Map<CastAnim, KeyframeAnimation> CACHE = new EnumMap<>(CastAnim.class);

    public static synchronized KeyframeAnimation get(CastAnim a) {
        return CACHE.computeIfAbsent(a, CastAnimLibrary::build);
    }

    static final String R = "rightArm", L = "leftArm", RL = "rightLeg", LL = "leftLeg", BODY = "body";
    static final float HALF = 1.57f, UP = 3.0f;
    private static final Ease IO = Ease.INOUTSINE, OUT = Ease.OUTCUBIC, SNAP = Ease.INQUAD;

    /** Trình dựng nhỏ: mỗi {@code at} đặt pitch/yaw/roll cho 1 bộ phận ở 1 tick; tự thêm tư thế nghỉ ở tick 0 và tick cuối. */
    private static final class B {
        final KeyframeAnimation.AnimationBuilder b = new KeyframeAnimation.AnimationBuilder(AnimationFormat.JSON_EMOTECRAFT);
        final Set<String> used = new LinkedHashSet<>();
        final int end;

        B(CastAnim a) {
            end = a.ticks;
            b.setName(a.name().toLowerCase()).setAuthor("hukhong");
            b.endTick = end;
            b.stopTick = end + 4; // 4 tick cuối để mờ dần về tư thế thường
            b.isLooped = false;
        }

        B at(int tick, Ease e, String part, float pitch, float yaw, float roll) {
            KeyframeAnimation.StateCollection c = b.getPart(part);
            if (used.add(part)) {
                c.fullyEnablePart(false);
                rest(c, 0);
            }
            c.pitch.addKeyFrame(tick, pitch, e);
            c.yaw.addKeyFrame(tick, yaw, e);
            c.roll.addKeyFrame(tick, roll, e);
            return this;
        }

        private static void rest(KeyframeAnimation.StateCollection c, int tick) {
            c.pitch.addKeyFrame(tick, 0f, Ease.LINEAR);
            c.yaw.addKeyFrame(tick, 0f, Ease.LINEAR);
            c.roll.addKeyFrame(tick, 0f, Ease.LINEAR);
        }

        KeyframeAnimation done() {
            for (String part : used) rest(b.getPart(part), end);
            return b.build();
        }
    }

    private static KeyframeAnimation build(CastAnim a) {
        B x = new B(a);
        switch (a) {
            case HAND_SEAL -> x
                    .at(6, OUT, R, -1.25f, -0.65f, 0f).at(6, OUT, L, -1.25f, 0.65f, 0f).at(6, OUT, BODY, 0.1f, 0f, 0f)
                    .at(15, OUT, R, -1.45f, -0.2f, 0f).at(15, OUT, L, -1.45f, 0.2f, 0f).at(15, OUT, BODY, 0.05f, 0f, 0f)
                    .at(20, IO, R, -1.45f, -0.2f, 0f).at(20, IO, L, -1.45f, 0.2f, 0f).at(20, IO, BODY, 0.05f, 0f, 0f);
            case SWORD_THRUST -> x
                    .at(5, IO, R, -0.9f, -0.7f, 0.3f).at(5, IO, L, -0.4f, 0.2f, -0.5f).at(5, IO, BODY, 0f, 0.35f, 0f)
                    .at(5, IO, RL, 0.35f, 0f, 0f).at(5, IO, LL, -0.35f, 0f, 0f)
                    .at(8, SNAP, R, -HALF, 0f, 0f).at(8, SNAP, L, -0.2f, 0.1f, -0.6f).at(8, SNAP, BODY, 0.1f, -0.3f, 0f)
                    .at(8, SNAP, RL, 0.35f, 0f, 0f).at(8, SNAP, LL, -0.5f, 0f, 0f)
                    .at(12, IO, R, -HALF, 0f, 0f).at(12, IO, L, -0.2f, 0.1f, -0.6f).at(12, IO, BODY, 0.1f, -0.3f, 0f)
                    .at(12, IO, RL, 0.35f, 0f, 0f).at(12, IO, LL, -0.5f, 0f, 0f);
            case SWORD_OVERHEAD -> x
                    .at(7, OUT, R, -UP, 0f, 0.1f).at(7, OUT, L, -0.5f, 0.3f, -0.5f).at(7, OUT, BODY, -0.2f, 0f, 0f)
                    .at(10, SNAP, R, -0.35f, 0f, 0f).at(10, SNAP, L, -0.5f, 0.3f, -0.5f).at(10, SNAP, BODY, 0.4f, 0f, 0f)
                    .at(10, SNAP, RL, -0.3f, 0f, 0f).at(10, SNAP, LL, 0.3f, 0f, 0f)
                    .at(16, IO, R, -0.35f, 0f, 0f).at(16, IO, L, -0.5f, 0.3f, -0.5f).at(16, IO, BODY, 0.4f, 0f, 0f)
                    .at(16, IO, RL, -0.3f, 0f, 0f).at(16, IO, LL, 0.3f, 0f, 0f);
            case SWORD_SPIN -> x
                    .at(4, IO, R, -0.3f, 0f, 1.5f).at(4, IO, L, -0.3f, 0f, -1.5f).at(4, IO, BODY, 0f, 0f, 0f)
                    .at(9, Ease.LINEAR, R, -0.3f, 0f, 1.5f).at(9, Ease.LINEAR, L, -0.3f, 0f, -1.5f).at(9, Ease.LINEAR, BODY, 0f, -3.1f, 0f)
                    .at(14, Ease.LINEAR, R, -0.3f, 0f, 1.5f).at(14, Ease.LINEAR, L, -0.3f, 0f, -1.5f).at(14, Ease.LINEAR, BODY, 0f, -6.2f, 0f)
                    .at(17, IO, R, -0.3f, 0f, 1.5f).at(17, IO, L, -0.3f, 0f, -1.5f).at(17, IO, BODY, 0f, -6.28f, 0f);
            case PUNCH_HEAVY -> x
                    .at(6, IO, R, 0.5f, 0f, 0.2f).at(6, IO, L, -1.1f, 0.7f, 0f).at(6, IO, BODY, 0.05f, 0.5f, 0f)
                    .at(9, SNAP, R, -HALF, 0f, 0f).at(9, SNAP, L, -1.1f, 0.7f, 0f).at(9, SNAP, BODY, 0.1f, -0.45f, 0f)
                    .at(9, SNAP, RL, 0.3f, 0f, 0f).at(9, SNAP, LL, -0.4f, 0f, 0f)
                    .at(13, IO, R, -HALF, 0f, 0f).at(13, IO, L, -1.1f, 0.7f, 0f).at(13, IO, BODY, 0.1f, -0.45f, 0f)
                    .at(13, IO, RL, 0.3f, 0f, 0f).at(13, IO, LL, -0.4f, 0f, 0f);
            case STOMP -> x
                    .at(7, OUT, RL, -1.3f, 0f, 0.1f).at(7, OUT, R, -2.4f, 0f, 0.4f).at(7, OUT, L, -2.4f, 0f, -0.4f).at(7, OUT, BODY, -0.15f, 0f, 0f)
                    .at(10, SNAP, RL, 0.05f, 0f, 0f).at(10, SNAP, R, -0.3f, 0f, 0.35f).at(10, SNAP, L, -0.3f, 0f, -0.35f).at(10, SNAP, BODY, 0.45f, 0f, 0f)
                    .at(17, IO, RL, 0.05f, 0f, 0f).at(17, IO, R, -0.3f, 0f, 0.35f).at(17, IO, L, -0.3f, 0f, -0.35f).at(17, IO, BODY, 0.45f, 0f, 0f);
            case UPPERCUT -> x
                    .at(5, IO, R, 0.6f, 0f, 0.25f).at(5, IO, L, -1.1f, 0.7f, 0f).at(5, IO, BODY, 0.3f, 0.2f, 0f)
                    .at(5, IO, RL, 0.4f, 0f, 0f).at(5, IO, LL, -0.3f, 0f, 0f)
                    .at(9, SNAP, R, -2.7f, 0f, 0.1f).at(9, SNAP, L, -1.1f, 0.7f, 0f).at(9, SNAP, BODY, -0.2f, -0.3f, 0f)
                    .at(9, SNAP, RL, 0.1f, 0f, 0f).at(9, SNAP, LL, -0.1f, 0f, 0f)
                    .at(13, IO, R, -2.7f, 0f, 0.1f).at(13, IO, L, -1.1f, 0.7f, 0f).at(13, IO, BODY, -0.2f, -0.3f, 0f);
            case FIST_JAB -> x
                    .at(3, SNAP, R, -HALF, 0f, 0f).at(3, SNAP, L, -1.1f, 0.7f, 0f).at(3, SNAP, BODY, 0.05f, -0.3f, 0f)
                    .at(6, IO, R, -HALF, 0f, 0f).at(6, IO, L, -1.1f, 0.7f, 0f).at(6, IO, BODY, 0.05f, -0.3f, 0f);
            case FIST_CROSS -> x
                    .at(3, SNAP, L, -HALF, 0f, 0f).at(3, SNAP, R, -1.1f, -0.7f, 0f).at(3, SNAP, BODY, 0.05f, 0.45f, 0f)
                    .at(6, IO, L, -HALF, 0f, 0f).at(6, IO, R, -1.1f, -0.7f, 0f).at(6, IO, BODY, 0.05f, 0.45f, 0f);
            case FIST_HOOK -> x
                    .at(3, IO, R, -1.3f, 0.9f, 0.9f).at(3, IO, L, -1.1f, 0.7f, 0f).at(3, IO, BODY, 0.05f, 0.5f, 0f)
                    .at(6, SNAP, R, -1.3f, -1.0f, 0.2f).at(6, SNAP, L, -1.1f, 0.7f, 0f).at(6, SNAP, BODY, 0.1f, -0.6f, 0f)
                    .at(8, IO, R, -1.3f, -1.0f, 0.2f).at(8, IO, L, -1.1f, 0.7f, 0f).at(8, IO, BODY, 0.1f, -0.6f, 0f);
            case CLAW_SWIPE -> x
                    .at(4, OUT, R, -2.0f, 0f, 0.9f).at(4, OUT, L, -0.6f, 0f, -0.4f).at(4, OUT, BODY, 0.1f, 0.3f, 0f)
                    .at(8, SNAP, R, -1.4f, -1.0f, 0f).at(8, SNAP, L, -0.6f, 0f, -0.4f).at(8, SNAP, BODY, 0.2f, -0.2f, 0f)
                    .at(10, OUT, R, -1.4f, -1.0f, 0f).at(10, OUT, L, -2.0f, 0f, -0.9f).at(10, OUT, BODY, 0.1f, -0.3f, 0f)
                    .at(14, SNAP, R, -1.2f, -0.6f, 0f).at(14, SNAP, L, -1.4f, 1.0f, 0f).at(14, SNAP, BODY, 0.2f, 0.2f, 0f);
            case ROAR -> x
                    .at(7, OUT, R, -0.4f, 0f, 2.1f).at(7, OUT, L, -0.4f, 0f, -2.1f).at(7, OUT, BODY, -0.25f, 0f, 0f)
                    .at(7, OUT, RL, 0f, 0f, 0.2f).at(7, OUT, LL, 0f, 0f, -0.2f)
                    .at(20, IO, R, -0.5f, 0f, 2.2f).at(20, IO, L, -0.5f, 0f, -2.2f).at(20, IO, BODY, -0.2f, 0f, 0f)
                    .at(20, IO, RL, 0f, 0f, 0.2f).at(20, IO, LL, 0f, 0f, -0.2f);
            case CHANNEL -> x
                    .at(6, OUT, R, -1.5f, -0.3f, 0.15f).at(6, OUT, L, -1.5f, 0.3f, -0.15f)
                    .at(6, OUT, RL, 0f, 0f, 0.15f).at(6, OUT, LL, 0f, 0f, -0.15f)
                    .at(13, IO, R, -1.6f, -0.2f, 0.25f).at(13, IO, L, -1.6f, 0.2f, -0.25f)
                    .at(13, IO, RL, 0f, 0f, 0.15f).at(13, IO, LL, 0f, 0f, -0.15f)
                    .at(19, IO, R, -1.5f, -0.3f, 0.15f).at(19, IO, L, -1.5f, 0.3f, -0.15f)
                    .at(19, IO, RL, 0f, 0f, 0.15f).at(19, IO, LL, 0f, 0f, -0.15f);
            case ULT_RAISE -> x
                    .at(9, OUT, R, -UP, 0f, 0.35f).at(9, OUT, L, -UP, 0f, -0.35f).at(9, OUT, BODY, -0.18f, 0f, 0f)
                    .at(18, IO, R, -UP, 0f, 0.35f).at(18, IO, L, -UP, 0f, -0.35f).at(18, IO, BODY, -0.18f, 0f, 0f);
            case DASH_LUNGE -> x
                    .at(3, OUT, R, 0.9f, 0f, 0.2f).at(3, OUT, L, 0.9f, 0f, -0.2f).at(3, OUT, BODY, 0.5f, 0f, 0f)
                    .at(3, OUT, RL, -0.6f, 0f, 0f).at(3, OUT, LL, 0.6f, 0f, 0f)
                    .at(8, IO, R, 0.9f, 0f, 0.2f).at(8, IO, L, 0.9f, 0f, -0.2f).at(8, IO, BODY, 0.5f, 0f, 0f)
                    .at(8, IO, RL, -0.6f, 0f, 0f).at(8, IO, LL, 0.6f, 0f, 0f);
            case GUARD -> x
                    .at(4, OUT, R, -1.2f, -1.1f, 0f).at(4, OUT, L, -1.35f, 1.1f, 0f).at(4, OUT, BODY, 0.1f, 0f, 0f)
                    .at(11, IO, R, -1.2f, -1.1f, 0f).at(11, IO, L, -1.35f, 1.1f, 0f).at(11, IO, BODY, 0.1f, 0f, 0f);
            case OPEN_PALMS -> x
                    .at(7, OUT, R, -0.9f, 0f, 0.6f).at(7, OUT, L, -0.9f, 0f, -0.6f).at(7, OUT, BODY, -0.08f, 0f, 0f)
                    .at(18, IO, R, -1.0f, 0f, 0.7f).at(18, IO, L, -1.0f, 0f, -0.7f).at(18, IO, BODY, -0.1f, 0f, 0f);
        }
        return x.done();
    }
}
