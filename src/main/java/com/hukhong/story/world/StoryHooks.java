package com.hukhong.story.world;

import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.StoryService;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Điểm nối duy nhất giữa thế giới (Lịch, địa điểm, đấu giá, nhiệm vụ) và cốt truyện. PLAN-v2 §2.2.
 * Mộ Dao nhắc 1 lần: complete() đặt cờ "wl.say:KEY"; {@link DialogueService#start} phát câu đó khi người chơi nói chuyện với Mộ Dao
 * rồi đổi thành "qc.told.KEY".
 */
public final class StoryHooks {
    private StoryHooks() {}

    public static final String SAY_PREFIX = "wl.say:";
    public static final String TOLD_PREFIX = "qc.told.";

    public static int chapter(ServerPlayer p) { return DialogueService.quest(p).current(); }

    /** Chế độ Tự do (không phải Tuyến truyện) bỏ qua lọc chương. */
    public static boolean allowed(ServerPlayer p, Lore lore) {
        if (lore == null) return true;
        QuestData q = DialogueService.quest(p);
        if (p.getData(StoryRegistry.STORY.get()).mode() != StoryData.MODE_FREE) {
            int ch = q.current();
            if (ch < lore.chapterMin() || ch > lore.chapterMax()) return false;
        }
        for (String f : lore.requiresFlags()) if (!q.has(f)) return false;
        return true;
    }

    /** Đặt cờ, trao manh mối (Văn Kiện), xếp lời Mộ Dao (một lần). */
    public static void complete(ServerPlayer p, Lore lore) {
        if (lore == null) return;
        QuestData q = DialogueService.quest(p);
        QuestData n = q;
        for (String f : lore.setsFlags()) n = n.withFlag(f);
        String say = lore.modaoLine();
        if (say != null && !n.has(TOLD_PREFIX + say) && !n.has(SAY_PREFIX + say)) n = n.withFlag(SAY_PREFIX + say);
        if (n != q) DialogueService.setQuest(p, n);
        String clue = lore.clue();
        if (clue != null && !p.getData(StoryRegistry.STORY.get()).has(clue)) StoryService.collect(p, clue);
    }

    /** Câu Mộ Dao đang chờ nói (key lang) hoặc null. */
    public static String pendingModaoLine(ServerPlayer p) {
        String k = DialogueService.quest(p).valueOf(SAY_PREFIX);
        return k.isEmpty() ? null : k;
    }

    /** Đánh dấu đã nói: bỏ cờ chờ, đặt "qc.told.KEY". */
    public static void markTold(ServerPlayer p, String key) {
        DialogueService.setQuest(p, DialogueService.quest(p).withoutFlag(SAY_PREFIX + key).withFlag(TOLD_PREFIX + key));
    }
}
