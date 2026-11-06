package com.hukhong.memory;

import com.hukhong.auction.Tx;
import com.hukhong.core.CultivationService;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Nghiệp vụ Ký Ức: ghi nhận, nhập bằng chuột phải, tặng ký ức chưa thấy, thưởng khi gom đủ tám. */
public final class MemoryService {
    private MemoryService() {}

    /** Kết quả ghi nhận dữ liệu (không tiêu item, không diễn cảnh). */
    public enum Outcome { NEW, NEW_COMPLETE, DUP, INVALID }

    /** Kết quả chuột phải. */
    public enum Result { OK, ALREADY, BUSY, INVALID }

    /** Thưởng khi gom đủ tám: tu vi lớn (x1.5 mức lên cấp hiện tại). */
    public static final double COMPLETE_EXP_MUL = 1.5;

    public static MemoryData data(ServerPlayer p) { return p.getData(MemoryRegistry.DATA.get()); }

    private static void save(ServerPlayer p, MemoryData d) { p.setData(MemoryRegistry.DATA.get(), d); }

    /** Ghi nhận ký ức id vào hồ sơ. Trùng = DUP (không tính thêm). Gom đủ tám = NEW_COMPLETE (một lần). */
    public static Outcome record(ServerPlayer p, int id) {
        if (MemoryLore.byId(id) == null) return Outcome.INVALID;
        MemoryData d = data(p);
        if (d.has(id)) return Outcome.DUP;
        d = d.with(id);
        boolean complete = d.count() == MemoryLore.COUNT && !d.completed();
        if (complete) d = d.done();
        save(p, d);
        return complete ? Outcome.NEW_COMPLETE : Outcome.NEW;
    }

    /** Thưởng tu vi + thông báo danh hiệu khi gom đủ tám (gọi sau record = NEW_COMPLETE). */
    public static void reward(ServerPlayer p) {
        long next = CultivationService.expToNext(Math.max(1, CultivationService.get(p).level()));
        long amount = Math.max(200L, Math.round(next * COMPLETE_EXP_MUL));
        CultivationService.addExp(p, amount);
        p.sendSystemMessage(Tx.t("hukhong.memory.complete.exp", "Linh khí tràn vào kinh mạch: +%s tu vi.", "Spiritual energy floods your meridians: +%s cultivation.", amount)
                .withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Tx.t("hukhong.memory.complete.title", "Danh hiệu mới: Người Giữ Ký Ức.", "New title earned: Keeper of Memories.")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    /** Chuột phải Mảnh Ký Ức: ghi nhận, tiêu item, diễn cảnh hồi ức (cảnh cuối nếu vừa gom đủ). */
    public static Result use(ServerPlayer p, ItemStack st) {
        int id = MemoryRegistry.idOf(st);
        if (MemoryLore.byId(id) == null) {
            p.displayClientMessage(Tx.t("hukhong.memory.invalid", "Mảnh ký ức bị vỡ, không đọc được.", "This shard is corrupted and unreadable."), true);
            return Result.INVALID;
        }
        if (data(p).has(id)) {
            p.displayClientMessage(Tx.t("hukhong.memory.already", "Ký ức này đã được nhập từ trước.", "This memory is already recorded."), true);
            return Result.ALREADY;
        }
        if (MemoryScene.active(p)) {
            p.displayClientMessage(Tx.t("hukhong.memory.busy", "Đang trong một ký ức khác, chưa thể nhập thêm.", "Another memory is still unfolding."), true);
            return Result.BUSY;
        }
        st.shrink(1);
        Outcome o = record(p, id);
        if (o == Outcome.NEW_COMPLETE) reward(p);
        MemoryScene.start(p, id, o == Outcome.NEW_COMPLETE ? () -> MemoryScene.start(p, 0, null) : null);
        return Result.OK;
    }

    /** Đánh dấu loại địa điểm đã cho ký ức lần đầu. */
    public static void markLooted(ServerPlayer p, String kind) { save(p, data(p).withLooted(kind)); }

    /**
     * Tặng một Mảnh Ký Ức chưa thấy (chưa nhập, chưa có trong túi). Ưu tiên preferred nếu còn trống.
     * @return id đã tặng, 0 nếu không còn ký ức nào để tặng
     */
    public static int grantUnseen(ServerPlayer p, int preferred) {
        MemoryData d = data(p);
        Set<Integer> held = new HashSet<>();
        for (ItemStack s : p.getInventory().items) {
            int hid = MemoryRegistry.idOf(s);
            if (hid > 0) held.add(hid);
        }
        List<Integer> free = new ArrayList<>();
        for (int id = 1; id <= MemoryLore.COUNT; id++) if (!d.has(id) && !held.contains(id)) free.add(id);
        if (free.isEmpty()) return 0;
        int pick = free.contains(preferred) ? preferred : free.get(p.getRandom().nextInt(free.size()));
        give(p, pick);
        p.displayClientMessage(Tx.t("hukhong.memory.drop", "Một mảnh ký ức thoáng hiện: %s", "A memory shard surfaces: %s",
                MemoryLore.byId(pick).name().tr()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return pick;
    }

    /** Đưa Mảnh Ký Ức id vào túi (rơi xuống đất nếu đầy). */
    public static void give(ServerPlayer p, int id) {
        ItemStack st = MemoryRegistry.shard(id);
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    /** Xóa toàn bộ hồ sơ ký ức (lệnh thử nghiệm). */
    public static void reset(ServerPlayer p) {
        MemoryScene.cancel(p);
        save(p, MemoryData.EMPTY);
    }
}
