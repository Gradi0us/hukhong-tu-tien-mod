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

    /** 10A: mở cả 4 đạo (Kiếm, Đạo, Thể, Ma). */
    public static final Set<Path> UNLOCKED = Set.of(Path.KIEM, Path.DAO, Path.THE, Path.MA);

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
        boolean extraBits = (mask >>> Path.values().length) != 0;
        if (extraBits || set.size() != requiredPicks(roll) || !UNLOCKED.containsAll(set)) {
            open(p, roll); // yêu cầu không hợp lệ: mở lại
            return;
        }
        Map<Path, Aptitude> apt = new EnumMap<>(Path.class);
        for (Path x : set) apt.put(x, roll.aptitude().get(x));
        CultivationData nd = d.withCharacter(set, apt, roll.origin().id, roll);
        // Tiên Thiên: cấp khởi đầu 1..10 theo tư chất tốt nhất (đặt trực tiếp, không qua EXP -> không kích Bình Cảnh/khóa mốc nhầm).
        int tt = TienThien.roll(apt.values(), new java.util.Random(roll.seed() ^ 0x5454L));
        if (tt > nd.level()) nd = nd.withLevel(tt).withCharacter(set, apt, roll.origin().id, roll);
        p.setData(ModAttachments.CULTIVATION, nd);
        SyncPayload.send(p);
        com.hukhong.stats.StatService.refresh(p);
        try { com.hukhong.world.OriginSpawn.apply(p, roll.origin(), set, roll.seed()); }
        catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().warn("OriginSpawn failed", ex); }
        if (set.contains(Path.KIEM)) com.hukhong.banmenh.BanMenhService.grant(p); // Kiếm Tu nhận Bản Mệnh Kiếm
        com.hukhong.story.StoryService.promptIfNeeded(p); // Bước 19: chọn Truyện/Tự do
    }
}
