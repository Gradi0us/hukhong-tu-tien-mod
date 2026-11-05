package com.hukhong.item;

import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Nguyên Không Tinh: hấp thụ để nhận pct% kinh nghiệm của cấp hiện tại (mua bằng linh thạch). */
public class NguyenKhongTinhItem extends Item {
    public final double pct;

    public NguyenKhongTinhItem(double pct, Properties p) { super(p); this.pct = pct; }

    /** Lượng EXP nhận được ở cấp hiện tại. */
    public long amount(CultivationData d) {
        return Math.max(1, Math.round(CultivationService.expToNext(Math.max(1, d.level())) * pct));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResultHolder.success(s);
        CultivationData d = CultivationService.get(sp);
        if (d.paths().isEmpty()) { sp.displayClientMessage(Component.translatable("hukhong.tiem_nang.no_path"), true); return InteractionResultHolder.fail(s); }
        long amt = amount(d);
        CultivationService.addExp(sp, amt);
        sp.displayClientMessage(Component.translatable("hukhong.nguyen_khong.absorb", amt).withColor(0x8FD6FF), true);
        sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1, sp.getZ(), 16, 0.4, 0.6, 0.4, 0.05);
        sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.3f);
        if (!sp.getAbilities().instabuild) s.shrink(1);
        return InteractionResultHolder.consume(s);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("hukhong.nguyen_khong.tip", String.valueOf((int) Math.round(pct * 100))).withColor(0x9FB8D0));
    }
}
