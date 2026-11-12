package com.hukhong.trungsinh;

import com.hukhong.charcreate.CharCreateService;
import com.hukhong.charcreate.CharRoll;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.network.SyncPayload;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.vfx.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.Set;

/** Trùng Sinh: xóa đạo, roll lại tư chất (không vượt mức cũ), giữ xuất thân và tuổi, mở lại màn chọn đạo; xác nhận sẽ đặt cấp 10. */
public final class TrungSinhService {
    private TrungSinhService() {}

    /** Cửa sổ xác nhận: 10 giây = 200 tick. */
    public static final long CONFIRM_WINDOW_TICKS = 200L;
    public static final int START_LEVEL = 10;

    public static TrungSinhData data(ServerPlayer p) { return p.getData(ModAttachments.TRUNG_SINH); }

    private static void put(ServerPlayer p, TrungSinhData d) { p.setData(ModAttachments.TRUNG_SINH, d); }

    public static boolean isPending(ServerPlayer p) { return data(p).rebirthPending(); }

    /** Lần dùng đầu: báo cảnh báo và ghi mốc; lần dùng trong cửa sổ: cho phép bắt đầu (ăn 32 tick). */
    public static boolean begin(ServerPlayer p) {
        TrungSinhData t = data(p);
        if (t.used()) {
            p.sendSystemMessage(Component.translatable("hukhong.trung_sinh.used"));
            return false;
        }
        CultivationData d = CultivationService.get(p);
        if (d.paths().isEmpty() || d.roll() == null) {
            p.sendSystemMessage(Component.translatable("hukhong.trung_sinh.need_path"));
            return false;
        }
        long now = p.level().getGameTime();
        if (t.confirmTick() >= 0 && now - t.confirmTick() <= CONFIRM_WINDOW_TICKS) return true;
        put(p, t.withConfirmTick(now));
        p.sendSystemMessage(Component.translatable("hukhong.trung_sinh.warn"));
        return false;
    }

    /** Hoàn tất (sau 32 tick): đánh dấu đã dùng, reset đạo, roll lại tư chất có trần, mở màn chọn đạo. */
    public static boolean rebirth(ServerPlayer p) {
        TrungSinhData t = data(p);
        if (t.used()) {
            p.sendSystemMessage(Component.translatable("hukhong.trung_sinh.used"));
            return false;
        }
        CultivationData d = CultivationService.get(p);
        CharRoll old = d.roll();
        if (old == null || d.paths().isEmpty()) {
            p.sendSystemMessage(Component.translatable("hukhong.trung_sinh.need_path"));
            return false;
        }
        // Trần = tư chất cao nhất trong các đạo đã chọn.
        Aptitude cap = Aptitude.D;
        for (Path x : d.paths()) {
            Aptitude a = d.aptitude().getOrDefault(x, Aptitude.D);
            if (a.ordinal() > cap.ordinal()) cap = a;
        }
        // Roll mới, kẹp trần; giữ XUẤT THÂN và tuổi khởi đầu cũ (vị trí spawn không đổi).
        CharRoll fresh = CharRoll.roll(p.getRandom().nextLong()).withAptitudeCap(cap);
        CharRoll roll = new CharRoll(fresh.seed(), fresh.slots(), fresh.aptitude(), old.origin(), old.startAge());

        put(p, t.withUsed().withRebirthPending(true)); // ghi "đã dùng" trước mọi bước khác
        PhapTu.setElement(p, null);
        p.setData(ModAttachments.CULTIVATION, d.withCharacter(Set.of(), Map.of(), roll.origin().id, roll));
        SyncPayload.send(p);
        CharCreateService.open(p, roll);
        return true;
    }

    /** Cấp sau xác nhận: đúng 10 và mở khóa mốc 10 (không để kẹt). */
    public static CultivationData rebirthLevel(CultivationData nd) {
        return nd.withLevel(START_LEVEL).withMilestoneUnlocked(true);
    }

    /** Sau khi xác nhận chọn đạo: tắt cờ chờ, thông báo và hiệu ứng. */
    public static void completeRebirth(ServerPlayer p) {
        put(p, data(p).withRebirthPending(false));
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("hukhong.trung_sinh.title")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("hukhong.trung_sinh.subtitle")));
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ModParticles.THANH_QUANG.get(), p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.6, 1.0, 0.6, 0.1);
            sl.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}
