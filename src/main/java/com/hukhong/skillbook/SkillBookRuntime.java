package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import com.hukhong.gates.DamageRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hệ số Cường Hóa cho kỹ năng NỀN (SkillEffects tính sát thương bằng hằng số): khi cast, mở cửa sổ nhân sát thương kỹ năng
 * của người cast trong vài tick; {@link DamageRules#hurtBySkill} nhân qua {@link #scale}. Kỹ năng bí tịch tự nhân hệ số
 * và gọi {@link #hurt} (bỏ qua cửa sổ) để không bị nhân đôi. Chỉ chạy trên luồng server.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SkillBookRuntime {
    private SkillBookRuntime() {}

    private record Win(long until, double mult) {}

    private static final Map<UUID, Win> WIN = new HashMap<>();
    private static boolean bypass = false;

    /** Mở cửa sổ nhân mult cho p trong ticks tick (mult <= 1 thì đóng cửa sổ). */
    public static void begin(ServerPlayer p, double mult, int ticks) {
        if (mult <= 1.0001) WIN.remove(p.getUUID());
        else WIN.put(p.getUUID(), new Win(p.level().getGameTime() + ticks, mult));
    }

    public static void clear(ServerPlayer p) { WIN.remove(p.getUUID()); }

    /** Gọi từ DamageRules.hurtBySkill. */
    public static float scale(ServerPlayer caster, float amount) {
        if (bypass || WIN.isEmpty()) return amount;
        Win w = WIN.get(caster.getUUID());
        if (w == null) return amount;
        if (caster.level().getGameTime() > w.until()) {
            WIN.remove(caster.getUUID());
            return amount;
        }
        return (float) (amount * w.mult());
    }

    /** Gây sát thương kỹ năng đã tự nhân hệ số, không chịu cửa sổ. */
    public static void hurt(ServerPlayer caster, LivingEntity target, float amount) {
        bypass = true;
        try {
            DamageRules.hurtBySkill(caster, target, amount);
        } finally {
            bypass = false;
        }
    }

    /** Thời gian cửa sổ theo kỹ năng nền: vòng chém/tối thượng kéo dài hơn. */
    public static int windowTicks(com.hukhong.skill.Skill s) {
        return switch (s) {
            case KIEM_2 -> 30;
            case KIEM_ULT -> 100;
            case DAO_ULT, THE_ULT, MA_ULT -> 40;
            default -> 12;
        };
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { WIN.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        WIN.clear();
        bypass = false;
    }
}
