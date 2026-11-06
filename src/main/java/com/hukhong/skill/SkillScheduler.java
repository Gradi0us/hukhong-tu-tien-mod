package com.hukhong.skill;

import com.hukhong.HuKhong;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Hàng đợi tác vụ trễ phía server (liên hoàn chém, vùng kiếm). Chỉ chạy trên luồng server. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SkillScheduler {
    private SkillScheduler() {}

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("hukhong/scheduler");

    private record Task(int[] left, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> INCOMING = new ArrayList<>();

    public static void later(int ticks, Runnable r) { INCOMING.add(new Task(new int[]{Math.max(1, ticks)}, r)); }

    /** TEST: chạy action rồi thực thi ngay (theo thứ tự hẹn) mọi việc mà CHÍNH action đó hẹn giờ; không đụng việc của nơi khác. */
    public static void runNow(Runnable action) {
        int n0 = INCOMING.size();
        action.run();
        for (int guard = 0; guard < 8 && INCOMING.size() > n0; guard++) {
            List<Task> mine = new ArrayList<>(INCOMING.subList(n0, INCOMING.size()));
            INCOMING.subList(n0, INCOMING.size()).clear();
            mine.sort(java.util.Comparator.comparingInt(t -> t.left[0]));
            for (Task t : mine) {
                try { t.run.run(); } catch (RuntimeException ex) { LOG.warn("SkillScheduler task failed", ex); }
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        TASKS.addAll(INCOMING);
        INCOMING.clear();
        for (int i = TASKS.size() - 1; i >= 0; i--) {
            Task t = TASKS.get(i);
            if (--t.left[0] > 0) continue;
            TASKS.remove(i);
            try { t.run.run(); } catch (RuntimeException ex) { LOG.warn("SkillScheduler task failed", ex); }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { TASKS.clear(); INCOMING.clear(); }
}
