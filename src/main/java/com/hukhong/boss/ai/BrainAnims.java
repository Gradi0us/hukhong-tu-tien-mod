package com.hukhong.boss.ai;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Controller GeckoLib "brain" cho các animation BossBrain gọi qua {@code playBrainAnim}. Tên chưa có trong file animation của
 * boss được ánh xạ về animation sẵn có (combo_* -> attack, còn lại -> walk) để boss chưa có animation mới vẫn chạy.
 * transitionTicks (M5) làm mượt chuyển giữa đi -> đứng -> đánh.
 */
public final class BrainAnims {
    private BrainAnims() {}
    public static final String CONTROLLER = "brain";
    public static final List<String> MOVE_NAMES = List.of("strafe_l", "strafe_r", "backstep", "sidestep_l", "sidestep_r", "backflip", "dash", "leap", "hover", "blink");

    /** Tất cả tên animation brain có thể phát với profile này. */
    public static List<String> names(BossProfile p) {
        List<String> out = new ArrayList<>(MOVE_NAMES);
        if (p != null) for (BossProfile.Combo c : p.combos()) for (int i = 1; i <= c.hits().size(); i++) out.add("combo_" + c.id() + "_" + i);
        return out;
    }

    /**
     * @param prefix ví dụ "animation.su_do_kiem."
     * @param available tên animation brain mà file animation thật sự có
     * @param attack / walk tên animation dự phòng (không có tiền tố)
     */
    public static <T extends GeoAnimatable> AnimationController<T> controller(T a, BossProfile p, String prefix, Set<String> available,
                                                                             String attack, String walk, int transitionTicks) {
        AnimationController<T> c = new AnimationController<>(a, CONTROLLER, transitionTicks, s -> PlayState.STOP);
        for (String n : names(p)) {
            String real = available.contains(n) ? n : n.startsWith("combo_") ? attack : walk;
            c.triggerableAnim(n, RawAnimation.begin().then(prefix + real, Animation.LoopType.PLAY_ONCE));
        }
        return c;
    }
}
