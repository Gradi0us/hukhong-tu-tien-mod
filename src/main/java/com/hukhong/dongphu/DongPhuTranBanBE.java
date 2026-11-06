package com.hukhong.dongphu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Block entity của Động Phủ Trận Bàn: chỉ giữ hoạt ảnh idle (đĩa ngọc xoay, hai vòng rune ngược chiều, lõi đập nhẹ). */
public class DongPhuTranBanBE extends BlockEntity implements GeoBlockEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.dong_phu_tran_ban.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DongPhuTranBanBE(BlockPos pos, BlockState s) { super(DongPhuRegistry.TRAN_BAN_BE.get(), pos, s); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "idle", 0, st -> st.setAndContinue(IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
