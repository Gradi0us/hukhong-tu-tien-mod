package com.hukhong.item;

import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.network.SyncPayload;
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
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Đá Tiềm Năng: dùng để nâng Tư chất của 1 đạo lên 1 bậc (đạo đầu tiên chưa SSS theo thứ tự đạo).
 * Tỉ lệ giảm dần theo bậc; thất bại vẫn mất đá.
 */
public class TiemNangThachItem extends Item {
    /** Tỉ lệ thành công khi đang ở D, C, B, A, S, SS. */
    public static final double[] CHANCE = {1.0, 0.9, 0.7, 0.5, 0.3, 0.15};

    public TiemNangThachItem(Properties p) { super(p); }

    /** Đạo sẽ được nâng, null nếu không còn đạo nào nâng được. */
    public static Path target(CultivationData d) {
        return d.paths().stream().sorted()
                .filter(p -> d.aptitude().getOrDefault(p, Aptitude.D) != Aptitude.SSS).findFirst().orElse(null);
    }

    public enum Result { NO_PATH, MAXED, SUCCESS, FAIL }

    /** Thử nâng; r = số ngẫu nhiên [0,1). Không tiêu đá (người gọi lo). */
    public static Result apply(ServerPlayer p, double r) {
        CultivationData d = CultivationService.get(p);
        if (d.paths().isEmpty()) return Result.NO_PATH;
        Path t = target(d);
        if (t == null) return Result.MAXED;
        Aptitude cur = d.aptitude().getOrDefault(t, Aptitude.D);
        if (r >= CHANCE[cur.ordinal()]) return Result.FAIL;
        Map<Path, Aptitude> apt = new EnumMap<>(Path.class);
        apt.putAll(d.aptitude());
        apt.put(t, Aptitude.values()[cur.ordinal() + 1]);
        p.setData(ModAttachments.CULTIVATION, d.withCharacter(d.paths(), apt, d.origin(), d.roll()));
        SyncPayload.send(p);
        com.hukhong.stats.StatService.refresh(p);
        return Result.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResultHolder.success(s);
        CultivationData d = CultivationService.get(sp);
        Path t = target(d);
        Aptitude before = t == null ? null : d.aptitude().getOrDefault(t, Aptitude.D);
        Result res = apply(sp, new Random().nextDouble());
        switch (res) {
            case NO_PATH -> { sp.displayClientMessage(Component.translatable("hukhong.tiem_nang.no_path"), true); return InteractionResultHolder.fail(s); }
            case MAXED -> { sp.displayClientMessage(Component.translatable("hukhong.tiem_nang.maxed"), true); return InteractionResultHolder.fail(s); }
            case SUCCESS -> {
                sp.displayClientMessage(Component.translatable("hukhong.tiem_nang.success", Component.translatable(pathLang(t)),
                        before.name(), Aptitude.values()[before.ordinal() + 1].name()).withColor(0xFFD34F), false);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
                sl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
            }
            case FAIL -> {
                sp.displayClientMessage(Component.translatable("hukhong.tiem_nang.fail", Component.translatable(pathLang(t)),
                        (int) Math.round(CHANCE[before.ordinal()] * 100)).withColor(0xB0B0B0), true);
                sl.sendParticles(ParticleTypes.SMOKE, sp.getX(), sp.getY() + 1, sp.getZ(), 20, 0.4, 0.5, 0.4, 0.02);
                sl.playSound(null, sp.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
            }
        }
        if (!sp.getAbilities().instabuild) s.shrink(1);
        return InteractionResultHolder.consume(s);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, java.util.List<Component> tip, net.minecraft.world.item.TooltipFlag f) {
        tip.add(Component.translatable("hukhong.tiem_nang.tip").withColor(0xC9A8FF));
    }

    public static String pathLang(Path p) {
        return switch (p) {
            case KIEM -> "hukhong.path.sword";
            case DAO -> "hukhong.path.dao";
            case THE -> "hukhong.path.body";
            case MA -> "hukhong.path.demon";
            case PHAP -> "hukhong.path.phap";
            case TRAN -> "hukhong.path.tran";
            case DAN -> "hukhong.path.dan";
        };
    }
}
