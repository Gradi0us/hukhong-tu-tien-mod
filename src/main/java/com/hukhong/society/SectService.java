package com.hukhong.society;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Logic tông môn phía server: xét tuyển, mời, nhiệm vụ, CH, mở bảng. */
public final class SectService {
    private SectService() {}

    public static final long CYCLE_TICKS = 7L * 24000L;
    /** Bảng Tông Môn thuộc sơn môn gần nhất trong bán kính này. */
    public static final int BOARD_RADIUS = 40;

    public static SectData get(ServerPlayer p) { return p.getData(SocietyRegistry.SECT); }

    private static void set(ServerPlayer p, SectData d) { p.setData(SocietyRegistry.SECT, d); }

    public static long cycleOf(ServerLevel l) { return l.getGameTime() / CYCLE_TICKS; }

    public static SectMath.Rank rankOf(ServerPlayer p, SectData d) {
        SectDef def = SectDef.byId(d.sect());
        if (def == null) return SectMath.Rank.NGOAI;
        return SectMath.rankFor(def.tier(), CultivationService.get(p).level(), d.totalCh());
    }

    /** Mã xét tuyển của người chơi vào tông def (invited = có thư mời hợp lệ). */
    public static int checkApply(ServerPlayer p, SectDef def, boolean invited) {
        CultivationData c = CultivationService.get(p);
        int age = p.getData(ModAttachments.AGE).ageYears();
        return SectMath.check(def, c.level(), SectMath.best(c.aptitude().values()), age, c.paths(), invited);
    }

    // ---- mở bảng ----

    public static void openBoard(ServerPlayer sp, BlockPos pos) {
        ServerLevel ow = sp.server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        String board = SectSavedData.get(ow).nearest(pos.getX(), pos.getZ(), BOARD_RADIUS);
        if (board == null) { sp.displayClientMessage(Component.translatable("hukhong.sect.board_orphan"), true); return; }
        SectData d = get(sp);
        SectDef boardDef = SectDef.byId(board);
        int code = d.member() ? SectMath.ALREADY : checkApply(sp, boardDef, board.equals(d.invite()));
        SectDef mine = SectDef.byId(d.sect());
        int level = CultivationService.get(sp).level();
        int need = mine == null ? 0 : SectMath.questNeed(d.quest(), mine.tier());
        int capLeft = mine == null ? 0 : SectMath.cycleCap(mine.tier()) - d.earnedIn(cycleOf(ow));
        var snap = new SectMenu.Snapshot(pos, board, d.sect(), rankOf(sp, d).ordinal(), d.totalCh(), d.quest(), d.progress(),
                need, d.invite(), code, Math.max(0, capLeft), level);
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SectMenu(id, inv, snap), Component.translatable(boardDef.nameKey())),
                snap::write);
    }

    // ---- nút ----

    public static boolean handleButton(ServerPlayer sp, BlockPos pos, int id) {
        ServerLevel ow = sp.server.getLevel(Level.OVERWORLD);
        if (ow == null) return false;
        String board = SectSavedData.get(ow).nearest(pos.getX(), pos.getZ(), BOARD_RADIUS);
        SectDef boardDef = board == null ? null : SectDef.byId(board);
        if (boardDef == null) return false;
        SectData d = get(sp);
        boolean ok = switch (id) {
            case SectMenu.B_APPLY -> !d.member() && join(sp, boardDef, false);
            case SectMenu.B_ACCEPT -> !d.member() && board.equals(d.invite()) && join(sp, boardDef, true);
            case SectMenu.B_LEAVE -> d.member() && leave(sp, d);
            case SectMenu.B_KILL -> d.sect().equals(board) && d.quest().isEmpty() && startQuest(sp, d, SectMath.Q_KILL);
            case SectMenu.B_HERB -> d.sect().equals(board) && d.quest().isEmpty() && startQuest(sp, d, SectMath.Q_HERB);
            case SectMenu.B_SUBMIT -> d.sect().equals(board) && SectMath.Q_HERB.equals(d.quest()) && submitHerbs(sp, d, boardDef);
            case SectMenu.B_CLAIM -> d.sect().equals(board) && claim(sp, ow, d, boardDef);
            case SectMenu.B_ABANDON -> d.sect().equals(board) && !d.quest().isEmpty() && startQuest(sp, d, "");
            default -> false;
        };
        openBoard(sp, pos);
        return ok;
    }

    private static boolean join(ServerPlayer sp, SectDef def, boolean invited) {
        int code = checkApply(sp, def, invited);
        if (code != SectMath.OK) {
            sp.displayClientMessage(Component.translatable("hukhong.sect.apply_fail." + code), true);
            return false;
        }
        set(sp, get(sp).withSect(def.id()));
        sp.sendSystemMessage(Component.translatable("hukhong.sect.joined", Component.translatable(def.nameKey())));
        SectBuffs.refresh(sp);
        return true;
    }

    private static boolean leave(ServerPlayer sp, SectData d) {
        set(sp, SectData.DEFAULT);
        SectBuffs.refresh(sp);
        sp.sendSystemMessage(Component.translatable("hukhong.sect.left"));
        return true;
    }

    private static boolean startQuest(ServerPlayer sp, SectData d, String q) {
        set(sp, d.withQuest(q, 0));
        return true;
    }

    private static boolean submitHerbs(ServerPlayer sp, SectData d, SectDef def) {
        int need = SectMath.questNeed(SectMath.Q_HERB, def.tier()) - d.progress();
        if (need <= 0) return false;
        int took = Herbs.take(sp.getInventory(), need);
        if (took <= 0) { sp.displayClientMessage(Component.translatable("hukhong.sect.no_herbs"), true); return false; }
        set(sp, d.withProgress(d.progress() + took));
        return true;
    }

    private static boolean claim(ServerPlayer sp, ServerLevel ow, SectData d, SectDef def) {
        int need = SectMath.questNeed(d.quest(), def.tier());
        if (need <= 0 || d.progress() < need) return false;
        String q = d.quest();
        long cyc = cycleOf(ow);
        int ch = SectMath.allowedCh(d.earnedIn(cyc), SectMath.questCh(q, def.tier()), def.tier());
        SectMath.Rank before = rankOf(sp, d);
        SectData n = d.withEarned(cyc, ch).withQuest("", 0);
        set(sp, n);
        CultivationService.addExp(sp, Math.round(CultivationService.expToNext(CultivationService.get(sp).level()) * 0.03));
        ItemStack reward = SectMath.Q_KILL.equals(q) ? new ItemStack(AlchemyRegistry.CHI_HUYET_DAN.get(), 2)
                : new ItemStack(AlchemyRegistry.HOI_XUAN_DAN.get());
        if (!sp.getInventory().add(reward)) sp.drop(reward, false);
        sp.sendSystemMessage(Component.translatable(ch > 0 ? "hukhong.sect.quest_done" : "hukhong.sect.quest_done_capped", ch));
        SectMath.Rank after = rankOf(sp, n);
        if (after.ordinal() > before.ordinal())
            sp.sendSystemMessage(Component.translatable("hukhong.sect.promoted", Component.translatable(after.key())));
        SectBuffs.refresh(sp);
        return true;
    }

    /** Hạ quái Hư Hóa hợp lệ: tiến độ nhiệm vụ diệt. */
    public static void onHuKill(ServerPlayer sp) {
        SectData d = get(sp);
        SectDef def = SectDef.byId(d.sect());
        if (def == null || !SectMath.Q_KILL.equals(d.quest())) return;
        int need = SectMath.questNeed(SectMath.Q_KILL, def.tier());
        if (d.progress() >= need) return;
        set(sp, d.withProgress(d.progress() + 1));
        sp.displayClientMessage(Component.translatable("hukhong.sect.quest_progress", d.progress() + 1, need), true);
    }
}
