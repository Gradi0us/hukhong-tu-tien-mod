package com.hukhong.alchemy;

import com.hukhong.HuKhong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Đan Độc (0–100): cộng mỗi viên, tự giảm 1/ngày game, cao thì giảm hiệu quả + buồn nôn. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DanDoc {
    private DanDoc() {}

    public static double get(ServerPlayer p) { return p.getData(AlchemyRegistry.DAN_DOC.get()); }

    public static void set(ServerPlayer p, double v) { p.setData(AlchemyRegistry.DAN_DOC.get(), Math.max(0, Math.min(100, v))); }

    /** false nếu Bùng Phát (100): không dùng được đan. */
    public static boolean canUse(ServerPlayer p) {
        if (get(p) >= 100) {
            p.displayClientMessage(Component.translatable("hukhong.dandoc.burst"), true);
            return false;
        }
        return true;
    }

    /** Gọi sau khi uống thành công: cộng độc, báo trạng thái. */
    public static void consume(ServerPlayer p, double groupCoef, int tier, AlchemyMath.Quality q) {
        double t = AlchemyMath.addToxin(get(p), AlchemyMath.toxinAdd(groupCoef, tier, q));
        set(p, t);
        if (t > 85) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
        p.sendSystemMessage(Component.translatable("hukhong.dandoc.status", (int) t));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        double t = get(p);
        if (t > 0) set(p, AlchemyMath.decay(t, 20));
    }
}
