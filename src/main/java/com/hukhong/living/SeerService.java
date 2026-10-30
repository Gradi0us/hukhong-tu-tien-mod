package com.hukhong.living;

import com.hukhong.auction.AuctionConfig;
import com.hukhong.auction.AuctionMath;
import com.hukhong.auction.Tx;
import com.hukhong.story.world.StoryHooks;
import com.hukhong.world.calendar.CalendarScheduler;
import com.hukhong.world.calendar.LichMath;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Thay Boi (D7): xem tuong, bao truoc cac su kien Lich sap toi (lan cot truyen chi hien neu da toi chuong) + gio Dau Gia Hoi. */
public final class SeerService {
    private SeerService() {}

    /** Cac dong bao truoc (toi da maxLines). */
    public static List<Component> forecast(ServerPlayer sp, int maxLines) {
        List<Component> out = new ArrayList<>();
        ServerLevel ow = sp.server.overworld();
        long now = ow.getGameTime();
        for (CalendarScheduler.Occ o : CalendarScheduler.occurrences(now, now + CalendarScheduler.HORIZON)) {
            if (o.start() <= now) continue;
            if (o.def().story() && !StoryHooks.allowed(sp, o.def().lore())) continue;
            long mins = Math.max(1, (o.start() - now) / 1200);
            Component name = Component.translatable(o.def().nameKey());
            out.add(o.def().story()
                    ? Tx.t("hukhong.living.seer.story", "• Thiên cơ: %s - còn khoảng %s phút thực.", "• Fate: %s - in about %s real minutes.", name, mins)
                    : Tx.t("hukhong.living.seer.world", "• %s - còn khoảng %s phút thực.", "• %s - in about %s real minutes.", name, mins));
            if (out.size() >= maxLines) break;
        }
        return out;
    }

    /** Gio Dau Gia Hoi ke tiep (gio may chu), hoac null. */
    public static LocalDateTime nextAuction() {
        AuctionConfig c = AuctionConfig.get();
        return AuctionMath.nextSlot(LocalDateTime.now(), c.days(), c.liveHour, c.liveMinute);
    }

    public static void consult(ServerPlayer sp) {
        sp.sendSystemMessage(Tx.t("hukhong.living.seer.open", "Thầy Bói nheo mắt nhìn quẻ: \"Thiên cơ sắp tới...\"", "The Seer squints at the omens: \"What fate has in store...\""));
        List<Component> lines = forecast(sp, 5);
        if (lines.isEmpty()) sp.sendSystemMessage(Tx.t("hukhong.living.seer.none", "Quẻ yên ắng, chưa thấy biến động lớn.", "The omens are calm; nothing large approaches."));
        for (Component l : lines) sp.sendSystemMessage(l);
        LocalDateTime next = nextAuction();
        if (next != null)
            sp.sendSystemMessage(Tx.t("hukhong.living.seer.auction", "• Đấu Giá Hội: %s (giờ máy chủ).", "• Grand Auction: %s (server time).", next.format(DateTimeFormatter.ofPattern("EEEE dd/MM HH:mm", java.util.Locale.ENGLISH))));
        sp.sendSystemMessage(Tx.t("hukhong.living.seer.lich", "(Một ngày Lịch dài %s phút thực.)", "(One Lich day lasts %s real minutes.)", LichMath.DAY / 1200));
    }
}
