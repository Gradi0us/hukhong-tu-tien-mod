package com.hukhong.guild;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import com.hukhong.entity.HuMob;
import com.hukhong.events.CorruptionData;
import com.hukhong.skillbook.SkillBookRewards;
import com.hukhong.skillbook.SkillGrade;
import com.hukhong.society.Herbs;
import com.hukhong.society.SectService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

/** Logic Hiep Hoi Mao Hiem Gia (chi chay tren server): lam moi ngay, nhan / nop nhiem vu, thu thach thang bac, tien do. */
public final class GuildService {
    private GuildService() {}

    public static final long DAY_TICKS = 24000L;

    public static GuildData get(ServerPlayer p) { return p.getData(GuildRegistry.DATA.get()); }

    public static void set(ServerPlayer p, GuildData d) { p.setData(GuildRegistry.DATA.get(), d); }

    public static long today(ServerLevel l) { return l.getGameTime() / DAY_TICKS; }

    public static int level(ServerPlayer p) { return CultivationService.get(p).level(); }

    public static boolean sectMember(ServerPlayer p) { return SectService.get(p).member(); }

    /** Sang ngay moi thi lam moi danh sach (huy courier dang chay). */
    public static GuildData refresh(ServerPlayer p) {
        GuildData d = get(p);
        long day = today(p.serverLevel());
        if (d.day() != day) {
            GuildCourier.cancel(p.getUUID());
            d = d.refreshed(day, level(p));
            set(p, d);
        }
        return d;
    }

    public static List<GuildMath.Quest> quests(GuildData d, java.util.UUID id) {
        return GuildMath.daily(d.rank(), d.day(), d.genLevel(), id.getLeastSignificantBits());
    }

    public static void open(ServerPlayer p) {
        refresh(p);
        var pos = p.blockPosition();
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GuildMenu(id, inv, pos), Component.translatable("hukhong.guild.title")),
                buf -> buf.writeBlockPos(pos));
    }

    private static void msg(ServerPlayer p, String key, Object... args) { p.displayClientMessage(Component.translatable(key, args), true); }

    // ---- nut bam ----

    public static boolean handleButton(ServerPlayer p, int id) {
        GuildData d = refresh(p);
        if (id >= GuildMenu.B_ABANDON && id < GuildMenu.B_ABANDON + GuildMath.SLOTS) return abandon(p, d, id - GuildMenu.B_ABANDON);
        if (id == GuildMenu.B_TRIAL) return trialButton(p, d);
        if (id < 0 || id >= GuildMath.SLOTS) return false;
        List<GuildMath.Quest> qs = quests(d, p.getUUID());
        GuildMath.Quest q = qs.get(id);
        int prog = d.progOf(id);
        if (prog == GuildData.DONE) return false;
        if (prog == GuildData.NONE) return accept(p, d, id, q);
        if (q.kind() == GuildMath.K_HERB && prog < q.amount()) {
            if (Herbs.count(p.getInventory()) < q.amount()) { msg(p, "hukhong.guild.need_herbs", q.amount()); return false; }
            Herbs.take(p.getInventory(), q.amount());
            prog = q.amount();
            set(p, d.withProg(id, prog));
            d = get(p);
        }
        if (prog < q.amount()) { msg(p, "hukhong.guild.not_done", prog, q.amount()); return false; }
        complete(p, d, id, q);
        return true;
    }

    private static boolean accept(ServerPlayer p, GuildData d, int slot, GuildMath.Quest q) {
        if (d.activeCount() >= GuildMath.MAX_ACTIVE) { msg(p, "hukhong.guild.too_many", GuildMath.MAX_ACTIVE); return false; }
        if (q.kind() == GuildMath.K_CLEANSE) {
            ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
            if (ow == null || CorruptionData.get(ow).size() == 0) { msg(p, "hukhong.guild.no_corruption"); return false; }
        }
        if (q.kind() == GuildMath.K_COURIER && !GuildCourier.start(p, slot, q.tier())) { msg(p, "hukhong.guild.courier_fail"); return false; }
        set(p, d.withProg(slot, 0));
        msg(p, "hukhong.guild.accepted");
        return true;
    }

    private static boolean abandon(ServerPlayer p, GuildData d, int slot) {
        if (d.progOf(slot) < 0) return false;
        if (quests(d, p.getUUID()).get(slot).kind() == GuildMath.K_COURIER) GuildCourier.cancel(p.getUUID());
        set(p, d.withProg(slot, GuildData.NONE));
        return true;
    }

    private static void complete(ServerPlayer p, GuildData d, int slot, GuildMath.Quest q) {
        boolean sect = sectMember(p);
        long ha = GuildMath.scale(q.ha(), sect);
        int pts = (int) GuildMath.scale(q.points(), sect);
        Wallet.give(p, ha);
        GuildData n = d.withProg(slot, GuildData.DONE).withPoints(d.points() + pts).withDone(d.done() + 1);
        set(p, n);
        if (q.bonus()) bonus(p, slot, d.day());
        p.sendSystemMessage(Component.translatable("hukhong.guild.done", TuTinhText.of(ha), pts));
    }

    private static void bonus(ServerPlayer p, int slot, long day) {
        ItemStack st = ((slot + day) & 1) == 0 ? new ItemStack(AlchemyRegistry.HOI_XUAN_DAN.get(), 2)
                : SkillBookRewards.stack(new Random(p.getRandom().nextLong()), SkillGrade.PHAM, null);
        SkillBookRewards.give(p, st);
    }

    private static boolean trialButton(ServerPlayer p, GuildData d) {
        GuildMath.Trial t = GuildMath.trial(d.rank() + 1);
        if (d.trial() == GuildData.NONE) {
            if (!GuildMath.canTrial(d.rank(), d.points(), level(p))) { msg(p, "hukhong.guild.trial_locked"); return false; }
            set(p, d.withTrial(0));
            msg(p, "hukhong.guild.trial_started");
            return true;
        }
        if (d.trial() < t.amount()) { msg(p, "hukhong.guild.not_done", d.trial(), t.amount()); return false; }
        GuildData n = d.withRank(d.rank() + 1);
        set(p, n);
        p.sendSystemMessage(Component.translatable("hukhong.guild.promoted", Component.translatable("hukhong.guild.rank." + n.rank())));
        return true;
    }

    // ---- tien do (goi tu GuildEvents / BiCanhManager / GuildCourier) ----

    private static void bump(ServerPlayer p, GuildData d, int slot, GuildMath.Quest q, boolean trial) {
        int amount = trial ? GuildMath.trial(d.rank() + 1).amount() : q.amount();
        int cur = trial ? d.trial() : d.progOf(slot);
        if (cur < 0 || cur >= amount) return;
        set(p, trial ? d.withTrial(cur + 1) : d.withProg(slot, cur + 1));
        msg(p, "hukhong.guild.progress", cur + 1, amount);
    }

    /** Nguoi choi ha victim hop le (da qua RewardGate). */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        boolean mob = victim instanceof HuMob, boss = Bosses.isBoss(victim);
        if (!mob && !boss) return;
        GuildData d = refresh(p);
        List<GuildMath.Quest> qs = quests(d, p.getUUID());
        int lv = Bosses.levelOf(victim);
        for (int i = 0; i < qs.size(); i++) {
            GuildMath.Quest q = qs.get(i);
            if (d.progOf(i) < 0) continue;
            switch (q.kind()) {
                case GuildMath.K_KILL -> { if (mob && lv >= q.lvLo() && lv <= q.lvHi()) bump(p, d, i, q, false); }
                case GuildMath.K_HUNT -> { if (boss) bump(p, d, i, q, false); }
                case GuildMath.K_CLEANSE -> { if (mob && cleanse(p, victim)) bump(p, d, i, q, false); }
                default -> { }
            }
            d = get(p);
        }
        if (boss && d.trial() >= 0 && GuildMath.trial(d.rank() + 1).kind() == GuildMath.K_HUNT) bump(p, d, 0, null, true);
    }

    /** Ha quai trong chunk bi nhiem: tay 1 chunk gan nhat (CorruptionData). */
    private static boolean cleanse(ServerPlayer p, LivingEntity victim) {
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        if (ow == null || victim.level() != ow) return false;
        CorruptionData cd = CorruptionData.get(ow);
        if (!cd.isCorrupted(victim.blockPosition())) return false;
        cd.cleanseNear(victim.getBlockX(), victim.getBlockZ(), 1);
        return true;
    }

    /** Clear Bi Canh bac tierIdx (0..4). */
    public static void onBiCanhClear(ServerPlayer p, int tierIdx) {
        GuildData d = refresh(p);
        List<GuildMath.Quest> qs = quests(d, p.getUUID());
        for (int i = 0; i < qs.size(); i++) {
            GuildMath.Quest q = qs.get(i);
            if (q.kind() == GuildMath.K_BICANH && d.progOf(i) >= 0 && tierIdx >= q.tier()) { bump(p, d, i, q, false); d = get(p); }
        }
        GuildMath.Trial t = GuildMath.trial(d.rank() + 1);
        if (d.trial() >= 0 && t.kind() == GuildMath.K_BICANH && tierIdx >= t.tier()) bump(p, d, 0, null, true);
    }

    /** Tin su toi noi: tien do nhiem vu giao hang. */
    public static void onCourierArrived(ServerPlayer p, int slot) {
        GuildData d = get(p);
        if (d.progOf(slot) == 0) {
            set(p, d.withProg(slot, 1));
            p.sendSystemMessage(Component.translatable("hukhong.guild.courier_arrived"));
        }
    }

    /** Tin su chet / het gio. */
    public static void onCourierLost(ServerPlayer p, int slot) {
        GuildData d = get(p);
        if (d.progOf(slot) >= 0) set(p, d.withProg(slot, GuildData.NONE));
        p.sendSystemMessage(Component.translatable("hukhong.guild.courier_lost"));
    }
}
