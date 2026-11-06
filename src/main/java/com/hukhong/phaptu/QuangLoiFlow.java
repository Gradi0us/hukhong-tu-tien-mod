package com.hukhong.phaptu;

import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quang / Lôi — Linh Lưu (2026-11-06): spam kỹ năng ít tốn mana.
 * Q/E rẻ hơn 40%, hồi nhanh hơn 25%; kỹ năng (Q/E/R) trúng kẻ địch trong 2 giây sau khi tung thì hoàn 50% mana vừa tốn (1 lần mỗi lần tung).
 */
public final class QuangLoiFlow {
    private QuangLoiFlow() {}

    public static final double QE_COST_MULT = 0.60, QE_CD_MULT = 0.75, REFUND = 0.50;
    public static final int REFUND_WINDOW = 40;

    private record Pending(int amount, long until) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    public static boolean applies(ServerPlayer p, Path page) {
        if (page != Path.PHAP) return false;
        PhapElement e = PhapTu.element(p);
        return e == PhapElement.QUANG || e == PhapElement.LOI;
    }

    /** Hệ số mana cho phím slot (Q=0, E=1). */
    public static double costMult(ServerPlayer p, Path page, int slot) { return applies(p, page) && slot <= 1 ? QE_COST_MULT : 1.0; }

    public static double cdMult(ServerPlayer p, Path page, int slot) { return applies(p, page) && slot <= 1 ? QE_CD_MULT : 1.0; }

    /** Gọi sau khi đã trừ mana: ghi nhận phần hoàn nếu trúng. slot T (3) không hoàn (tia T đã có giảm riêng). */
    public static void afterCast(ServerPlayer p, Path page, int slot, int spent) {
        if (!applies(p, page) || slot > 2 || spent <= 0) return;
        PENDING.put(p.getUUID(), new Pending((int) Math.round(spent * REFUND), p.level().getGameTime() + REFUND_WINDOW));
    }

    /** Kỹ năng của p vừa trúng mục tiêu: hoàn mana (tối đa 1 lần mỗi lần tung). Trả lượng hoàn. */
    public static int onSkillHit(ServerPlayer p) {
        Pending pd = PENDING.get(p.getUUID());
        if (pd == null) return 0;
        PENDING.remove(p.getUUID());
        if (p.level().getGameTime() > pd.until) return 0;
        Resources.add(p, pd.amount);
        if (p.level() instanceof ServerLevel lv)
            lv.sendParticles(PhapTu.element(p) == PhapElement.LOI ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                    p.getX(), p.getY() + 1.1, p.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
        return pd.amount;
    }

    /** Mana thật đã trừ cho lần tung (để biết hoàn bao nhiêu). */
    public static int spent(CultivationData before, ServerPlayer p) {
        return Math.max(0, before.resource() - p.getData(ModAttachments.CULTIVATION).resource());
    }
}
