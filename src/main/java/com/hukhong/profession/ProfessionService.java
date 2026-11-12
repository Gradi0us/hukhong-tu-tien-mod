package com.hukhong.profession;

import com.hukhong.core.ModAttachments;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * API nghe phu. Doc (server + client): get / profession / level / cap / kimBonus.
 * Ghi (ServerPlayer): choose / addXp / addCraftXp. Moi thay doi deu dong bo client.
 */
public final class ProfessionService {
    private ProfessionService() {}

    public static ProfessionData get(Player p) { return p.getData(ProfessionRegistry.DATA.get()); }

    public static Profession profession(Player p) { return get(p).profession(); }

    /** Cap nghe {@code prof} cua nguoi choi, 0 neu khong mang nghe do. */
    public static int level(Player p, Profession prof) {
        ProfessionData d = get(p);
        return prof != Profession.NONE && d.profession() == prof ? d.level() : 0;
    }

    /** Tran cap nghe theo canh gioi tu luyen hien tai. */
    public static int cap(Player p) { return ProfessionMath.cap(p.getData(ModAttachments.CULTIVATION).realmIndex()); }

    /** Chon nghe (mot lan, vinh vien). false neu da co nghe hoac prof == NONE. */
    public static boolean choose(ServerPlayer p, Profession prof) {
        if (prof == Profession.NONE) return false;
        if (get(p).profession() != Profession.NONE) {
            p.displayClientMessage(Component.translatable("hukhong.profession.already"), true);
            return false;
        }
        put(p, new ProfessionData(prof, 1, 0L));
        p.displayClientMessage(Component.translatable("hukhong.profession.chosen", Component.translatable(prof.nameKey())), false);
        return true;
    }

    /** Cong xp nghe. Chi co tac dung neu nguoi choi dang mang dung nghe {@code prof}. Len cap: thong bao + am thanh. */
    public static void addXp(ServerPlayer p, Profession prof, long amount) {
        ProfessionData d = get(p);
        if (prof == Profession.NONE || d.profession() != prof || amount <= 0) return;
        int cap = cap(p), lv = d.level();
        long xp = d.xp() + amount;
        boolean up = false;
        while (lv < cap && xp >= ProfessionMath.need(lv)) {
            xp -= ProfessionMath.need(lv);
            lv++;
            up = true;
        }
        if (lv >= cap) xp = Math.min(xp, ProfessionMath.need(lv));
        put(p, new ProfessionData(prof, lv, xp));
        if (up) {
            p.displayClientMessage(Component.translatable("hukhong.profession.levelup",
                    Component.translatable(prof.nameKey()), lv), false);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    /** Xp che tao: Doan Tao Su co Kim Phap Tu duoc +50%. */
    public static void addCraftXp(ServerPlayer p, Profession prof, long base) {
        long amt = prof == Profession.DOAN_TAO && kimBonus(p) ? Math.round(base * ProfessionMath.KIM_XP_MULT) : base;
        addXp(p, prof, amt);
    }

    /** true = Doan Tao Su dong thoi la Phap Tu he Kim (bonus ran). */
    public static boolean kimBonus(Player p) {
        return get(p).profession() == Profession.DOAN_TAO && PhapTu.element(p) == PhapElement.KIM;
    }

    /** 0 hoac 1: co duoc +1 bac pham cho lan ran nay khong (chi khi kimBonus). */
    public static int rollTierBonus(Player p, RandomSource r) {
        return kimBonus(p) && r.nextFloat() < ProfessionMath.KIM_TIER_CHANCE ? 1 : 0;
    }

    private static void put(ServerPlayer p, ProfessionData d) {
        p.setData(ProfessionRegistry.DATA.get(), d);
        ProfessionNet.send(p);
    }
}
