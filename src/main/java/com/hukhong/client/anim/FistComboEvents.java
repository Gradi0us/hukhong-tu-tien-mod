package com.hukhong.client.anim;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server: Thể Tu tay không (tay chính trống) vung tay -> phát combo đấm (JAB -> CROSS -> HOOK -> ...) qua lớp
 * playerAnimator "cast" cho chính chủ và người xung quanh. Better Combat chỉ hoạt động theo thuộc tính vũ khí nên
 * tay không không có combo của nó; lớp này bù chỗ đó. Cửa sổ nối đòn 20 tick (1 giây). Chỉ phát tư thế, không đổi sát thương.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class FistComboEvents {
    private FistComboEvents() {}

    public static final int COMBO_WINDOW = 20;
    private static final CastAnim[] CYCLE = {CastAnim.FIST_JAB, CastAnim.FIST_CROSS, CastAnim.FIST_HOOK};
    private static final class St { long last = -1_000_000L; int idx; }
    private static final Map<UUID, St> STATE = new HashMap<>();

    /** Tick đầu tiên của một cú vung tay (swinging && swingTime == 0). */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.swinging || p.swingTime != 0) return;
        if (!p.getMainHandItem().isEmpty() || p.isSpectator()) return;
        if (!p.getData(ModAttachments.CULTIVATION).paths().contains(Path.THE)) return;
        CastAnim a = next(p.getUUID(), p.level().getGameTime());
        CastAnimHook.send(p, a);
    }

    /** Chọn đòn kế tiếp: trong cửa sổ thì nối tiếp vòng JAB/CROSS/HOOK, quá cửa sổ thì quay về JAB. */
    static CastAnim next(UUID id, long now) {
        St s = STATE.computeIfAbsent(id, k -> new St());
        s.idx = now - s.last <= COMBO_WINDOW ? (s.idx + 1) % CYCLE.length : 0;
        s.last = now;
        return CYCLE[s.idx];
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        STATE.remove(e.getEntity().getUUID());
    }
}
