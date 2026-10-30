package com.hukhong.living;

import com.hukhong.auction.AuctionNet;
import com.hukhong.auction.LiveAuction;
import com.hukhong.auction.Tx;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.story.world.Faction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.Random;

/**
 * Cau noi AmbientNpc -> he thong NPC song: lich sinh hoat, AI day du chi trong 64 o quanh nguoi choi, thien cam, qua tang,
 * de nghi nhiem vu, Dau Gia Su, Thay Boi, Ma Tu.
 */
public final class NpcLife {
    private NpcLife() {}

    public static final int AI_RANGE = 64, TALK_CD = 2400, OFFER_CD = 6000, GIFT_CD = 6000;

    public static Faction factionOf(AmbientNpc n) { return n.role() == AmbientNpc.DISCIPLE ? Faction.TONG_MON : Faction.THANH; }

    /** Moi tick may chu cua NPC (AmbientNpc.tick goi). */
    public static void tick(AmbientNpc n) {
        if (n.tickCount % 20 != 0) return;
        ServerLevel sl = (ServerLevel) n.level();
        // vay thanh (tru ne/chien dau/an nap) la hanh vi chinh: luon bat AI du khong co nguoi choi gan
        boolean near = sl.getNearestPlayer(n, AI_RANGE) != null || n.siegeMode() != AmbientNpc.SIEGE_NONE || n.hiddenInside();
        if (n.isNoAi() == near) n.setNoAi(!near); // xa nguoi choi -> tat AI de nhe may chu
        if (!near) return;
        int act = NpcSchedule.activity(n.role(), sl.getDayTime());
        if (act != n.activity()) n.setActivity(act);
        if (n.role() == AmbientNpc.MA_TU) MaTu.tick(n);
    }

    /** Chuot phai vao dan cu. */
    public static InteractionResult interact(AmbientNpc n, Player pl, InteractionHand hand) {
        if (n.level().isClientSide || !(pl instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) return InteractionResult.sidedSuccess(n.level().isClientSide);
        switch (n.role()) {
            case AmbientNpc.AUCTIONEER -> {
                if (!LiveAuction.interact(sp)) AuctionNet.open(sp);
                return InteractionResult.CONSUME;
            }
            case AmbientNpc.SEER -> {
                SeerService.consult(sp);
                touch(sp, n, 1);
                return InteractionResult.CONSUME;
            }
            default -> { }
        }
        if (QuestService.interact(sp, n)) return InteractionResult.CONSUME;
        ItemStack held = sp.getMainHandItem();
        if (sp.isShiftKeyDown() && !held.isEmpty()) { gift(sp, n, held); return InteractionResult.CONSUME; }
        talk(sp, n);
        return InteractionResult.CONSUME;
    }

    static void touch(ServerPlayer sp, AmbientNpc n, int delta) {
        long now = sp.level().getGameTime();
        String k = "t:" + n.getUUID();
        if (AffectionService.onCooldown(sp, k, now, TALK_CD)) return;
        AffectionService.stamp(sp, k, now);
        AffectionService.change(sp, n.getUUID(), factionOf(n), delta);
    }

    static void talk(ServerPlayer sp, AmbientNpc n) {
        long now = sp.level().getGameTime();
        String nk = AffectionService.npcKey(n.getUUID());
        if (n.role() == AmbientNpc.MA_TU) {
            sp.sendSystemMessage(Tx.t("hukhong.living.matu.talk", "Người này nói năng ấp úng, mắt cứ liếc vào bóng tối...", "This person mumbles and keeps glancing into the dark..."));
            return;
        }
        touch(sp, n, 1);
        int aff = AffectionService.get(sp, nk);
        int tier = AffectionMath.tier(aff);
        sp.sendSystemMessage(greeting(tier, aff));
        // de nghi nhiem vu
        if (!AffectionMath.offersQuest(aff) || n.role() == AmbientNpc.CHILD) return;
        String qk = "q:" + n.getUUID();
        if (AffectionService.onCooldown(sp, qk, now, OFFER_CD)) return;
        if (QuestService.quests(sp).size() >= LivingData.MAX_QUESTS) {
            sp.sendSystemMessage(Tx.t("hukhong.living.q.full", "\"Bạn đang bận nhiều việc rồi, lần sau nhé.\"", "\"You have your hands full; next time.\""));
            return;
        }
        boolean special = AffectionMath.offersSpecial(aff) && sp.getRandom().nextBoolean();
        Quest q = QuestService.generate(sp, new Random(sp.getRandom().nextLong()), n.blockPosition(), n.getUUID(), special, null);
        if (QuestService.accept(sp, q)) {
            AffectionService.stamp(sp, qk, now);
            if (special) sp.sendSystemMessage(Tx.t("hukhong.living.q.special", "\"Chỉ tin bạn mới dám nhờ việc này...\" (nhiệm vụ đặc biệt)", "\"I only trust you with this...\" (special quest)"));
            sp.sendSystemMessage(QuestService.describe(q, sp));
        }
    }

    static net.minecraft.network.chat.Component greeting(int tier, int aff) {
        return switch (tier) {
            case 0 -> Tx.t("hukhong.living.talk.0", "\"Đừng bén mảng đến gần ta!\" (thiện cảm %s)", "\"Stay away from me!\" (affection %s)", aff);
            case 1 -> Tx.t("hukhong.living.talk.1", "\"Lại là ngươi...\" (thiện cảm %s)", "\"You again...\" (affection %s)", aff);
            case 2 -> Tx.t("hukhong.living.talk.2", "\"Chào đạo hữu.\" (thiện cảm %s)", "\"Greetings, friend.\" (affection %s)", aff);
            case 3 -> Tx.t("hukhong.living.talk.3", "\"Ồ, người quen! Có việc gì không?\" (thiện cảm %s)", "\"Oh, a familiar face! Anything I can do?\" (affection %s)", aff);
            case 4 -> Tx.t("hukhong.living.talk.4", "\"Bạn hiền đến chơi, thật vui!\" (thiện cảm %s)", "\"A dear friend visits, what joy!\" (affection %s)", aff);
            default -> Tx.t("hukhong.living.talk.5", "\"Tri kỷ! Ta sẵn lòng giúp bạn mọi việc.\" (thiện cảm %s)", "\"Soulmate! I will gladly help you in anything.\" (affection %s)", aff);
        };
    }

    static void gift(ServerPlayer sp, AmbientNpc n, ItemStack held) {
        long now = sp.level().getGameTime();
        String k = "g:" + n.getUUID();
        if (AffectionService.onCooldown(sp, k, now, GIFT_CD)) {
            sp.sendSystemMessage(Tx.t("hukhong.living.gift.cd", "\"Cảm ơn, nhưng tôi vừa nhận quà của bạn rồi.\"", "\"Thanks, but you just gave me a gift.\""));
            return;
        }
        boolean mod = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).getNamespace().equals("hukhong");
        boolean rare = held.getRarity() != Rarity.COMMON;
        int d = AffectionMath.giftDelta(mod, rare);
        net.minecraft.network.chat.Component name = held.getHoverName();
        held.shrink(1);
        AffectionService.stamp(sp, k, now);
        AffectionService.change(sp, n.getUUID(), factionOf(n), d);
        sp.sendSystemMessage(Tx.t("hukhong.living.gift.ok", "Bạn tặng %s. Thiện cảm +%s.", "You gift %s. Affection +%s.", name, d));
    }
}
