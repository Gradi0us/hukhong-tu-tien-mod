package com.hukhong.auction;

import com.hukhong.HuKhong;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/** Phi Kiem Truyen Thu: gui thu cho nguoi choi (online -> giao ngay, offline -> giao khi dang nhap). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MailService {
    private MailService() {}

    /** Gui tien (ha) va/hoac vat pham. Nguoi nhan dang online thi giao ngay. */
    public static void send(MinecraftServer server, UUID to, String subjectKey, String arg, long ha, ItemStack item) {
        if (to == null || Lot.NIL.equals(to) || (ha <= 0 && item.isEmpty())) return;
        MailData d = MailData.get(server);
        d.box(to).add(new MailData.Mail(subjectKey, arg, Math.max(0, ha), item.copy(), System.currentTimeMillis()));
        d.trim(to);
        d.setDirty();
        ServerPlayer p = server.getPlayerList().getPlayer(to);
        if (p != null) deliver(p);
    }

    /** Giao tat ca thu dang cho: tien vao vi, vat pham vao tui (neu day thi con lai trong hom thu). Tra so thu da giao xong. */
    public static int deliver(ServerPlayer p) {
        MailData d = MailData.get(p.server);
        int done = 0, left = 0;
        for (MailData.Mail m : d.peek(p.getUUID())) {
            if (!m.pending()) continue;
            if (m.ha > 0) {
                Wallet.give(p, m.ha);
                p.displayClientMessage(Tx.t("hukhong.mail.got_ha", "Phi Kiếm Truyền Thư: nhận %s", "Flying-sword mail: received %s", TuTinhText.of(m.ha)), false);
                m.ha = 0;
            }
            if (!m.item.isEmpty()) {
                ItemStack rest = m.item.copy();
                p.getInventory().add(rest);
                if (rest.isEmpty()) {
                    p.displayClientMessage(Tx.t("hukhong.mail.got_item", "Phi Kiếm Truyền Thư: nhận %s", "Flying-sword mail: received %s", m.item.getHoverName()), false);
                    m.item = ItemStack.EMPTY;
                } else {
                    m.item = rest;
                }
            }
            if (m.pending()) left++; else { m.claimed = true; done++; }
        }
        d.setDirty();
        if (left > 0) p.displayClientMessage(Tx.t("hukhong.mail.left", "Hòm thư còn %s thư chưa nhận (túi đầy). Dùng /thu để mở.", "%s mails still waiting (inventory full). Use /thu to open.", left), false);
        return done;
    }

    /** Tieu de thu (khoa co dinh de client/lang). */
    public static net.minecraft.network.chat.Component subject(String key, String arg) {
        return switch (key) {
            case "hukhong.mail.sub.sold" -> Tx.t("hukhong.mail.sub.sold", "Đã bán: %s", "Sold: %s", arg);
            case "hukhong.mail.sub.won" -> Tx.t("hukhong.mail.sub.won", "Thắng đấu giá: %s", "Auction won: %s", arg);
            case "hukhong.mail.sub.refund" -> Tx.t("hukhong.mail.sub.refund", "Hoàn tiền đặt giá: %s", "Bid refunded: %s", arg);
            case "hukhong.mail.sub.returned" -> Tx.t("hukhong.mail.sub.returned", "Trả lại vật phẩm không bán được: %s", "Unsold item returned: %s", arg);
            case "hukhong.mail.sub.buyback" -> Tx.t("hukhong.mail.sub.buyback", "Thương hội mua lại giá sàn: %s", "Merchant guild bought back at floor: %s", arg);
            case "hukhong.mail.sub.live_won" -> Tx.t("hukhong.mail.sub.live_won", "Đấu Giá Hội - lô thắng: %s", "Grand Auction - lot won: %s", arg);
            default -> Component.literal(key + " " + arg);
        };
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !p.level().isClientSide) deliver(p);
    }
}
