package com.hukhong.charcreate;

import com.hukhong.HuKhong;
import com.hukhong.age.AgeData;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.network.OpenCharCreatePayload;
import com.hukhong.network.SyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Server: roll lần đầu, mở màn tạo nhân vật, xác thực lựa chọn. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CharCreateService {
    private CharCreateService() {}

    /** Mở cả 7 đạo (Kiếm, Đạo, Thể, Ma, Pháp, Trận, Đan). */
    public static final Set<Path> UNLOCKED = Set.of(Path.KIEM, Path.DAO, Path.THE, Path.MA, Path.PHAP, Path.TRAN, Path.DAN);

    /** Bit cờ trong mask gói xác nhận: Pháp Tu đã dùng lượt reroll nguyên tố duy nhất. */
    public static final int REROLL_BIT = 1 << 8;
    /** 2026-11-06: Pháp Tu tự chọn nguyên tố - 3 bit (ordinal + 1, 0 = chưa chọn) bắt đầu từ bit 9. */
    public static final int ELEMENT_SHIFT = 9, ELEMENT_MASK = 7 << ELEMENT_SHIFT;
    private static final int PATH_MASK = (1 << Path.values().length) - 1;

    public static int requiredPicks(CharRoll roll) { return Math.min(roll.slots(), UNLOCKED.size()); }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CultivationData d = CultivationService.get(p);
        if (!d.paths().isEmpty()) return;
        if (d.roll() == null) reroll(p); else open(p, d.roll());
    }

    /** Roll mới (lần đầu hoặc /hukhong reroll): xóa đạo, ghi tuổi khởi đầu, mở màn. */
    public static void reroll(ServerPlayer p) {
        CharRoll roll = CharRoll.roll(p.getRandom().nextLong());
        com.hukhong.phaptu.PhapTu.setElement(p, null);
        CultivationData d = CultivationService.get(p);
        p.setData(ModAttachments.CULTIVATION, d.withCharacter(Set.of(), Map.of(), roll.origin().id, roll));
        AgeData a = p.getData(ModAttachments.AGE);
        p.setData(ModAttachments.AGE, new AgeData(a.onlineTicks(), roll.startAge()));
        SyncPayload.send(p);
        open(p, roll);
    }

    private static void open(ServerPlayer p, CharRoll roll) {
        PacketDistributor.sendToPlayer(p, new OpenCharCreatePayload(roll));
    }

    /** Xác thực lựa chọn từ client (mask bit = Path.ordinal). */
    public static void confirm(ServerPlayer p, int mask) {
        CultivationData d = CultivationService.get(p);
        CharRoll roll = d.roll();
        if (roll == null || !d.paths().isEmpty()) return;
        Set<Path> set = EnumSet.noneOf(Path.class);
        for (Path x : Path.values()) if ((mask & (1 << x.ordinal())) != 0) set.add(x);
        boolean rerolled = (mask & REROLL_BIT) != 0;
        boolean extraBits = (mask & ~(PATH_MASK | REROLL_BIT | ELEMENT_MASK)) != 0;
        int el = (mask & ELEMENT_MASK) >> ELEMENT_SHIFT;
        com.hukhong.phaptu.PhapElement chosen = el >= 1 && el <= com.hukhong.phaptu.PhapElement.values().length
                ? com.hukhong.phaptu.PhapElement.values()[el - 1] : null;
        if (extraBits || set.size() != requiredPicks(roll) || !UNLOCKED.containsAll(set) || (set.contains(Path.PHAP) && chosen == null)) {
            open(p, roll); // yêu cầu không hợp lệ: mở lại
            return;
        }
        Map<Path, Aptitude> apt = new EnumMap<>(Path.class);
        for (Path x : set) apt.put(x, roll.aptitude().get(x));
        com.hukhong.phaptu.PhapTu.setElement(p, set.contains(Path.PHAP) ? chosen : null); // tự chọn (trước: roll ngẫu nhiên)
        CultivationData nd = d.withCharacter(set, apt, roll.origin().id, roll);
        // Tiên Thiên: cấp khởi đầu (đơn đạo 1..10; đa đạo tới 20). Đặt trực tiếp, không qua EXP.
        nd = applyStartLevel(nd, TienThien.startLevel(apt.values(), roll.seed()));
        p.setData(ModAttachments.CULTIVATION, nd);
        SyncPayload.send(p);
        com.hukhong.stats.StatService.refresh(p);
        try { com.hukhong.world.OriginSpawn.apply(p, roll.origin(), set, roll.seed()); }
        catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().warn("OriginSpawn failed", ex); }
        if (set.contains(Path.KIEM)) com.hukhong.banmenh.BanMenhService.grant(p); // Kiếm Tu nhận Bản Mệnh Kiếm
        if (set.contains(Path.PHAP)) com.hukhong.phaptu.linhtruong.LinhTruongService.grant(p); // Pháp Tu nhận Linh Trượng
        com.hukhong.onboarding.GuideBook.give(p, set); // sách hướng dẫn theo đạo đã chọn
        com.hukhong.story.StoryService.promptIfNeeded(p); // Bước 19: chọn Truyện/Tự do
    }

    /**
     * Đặt cấp khởi đầu. Nếu rơi đúng mốc khóa (10/20) thì mở khóa luôn: nhân vật mới không bị kẹt chờ Ấn Hồn Đài
     * (Lõi của các mốc đã vượt không được cấp -- đánh đổi có chủ đích, tránh soft-lock).
     * Truyện: chapterFor(level, unlocked) cho chương đúng.
     */
    public static CultivationData applyStartLevel(CultivationData nd, int level) {
        if (level <= nd.level()) return nd;
        // Tiên Thiên vượt mốc 10: dừng ở mốc đầu tiên (khóa, phải chọn lõi); chọn lõi xong mới lên tiếp (CultivationService.unlockMilestone).
        int stop = TienThien.firstStop(nd.level(), level);
        CultivationData r = nd.withLevel(stop);
        return stop == level && CultivationData.isMilestoneLevel(level) && level < 10 ? r.withMilestoneUnlocked(true) : r;
    }
}
