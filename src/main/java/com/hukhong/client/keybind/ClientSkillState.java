package com.hukhong.client.keybind;

import com.hukhong.network.SkillSyncPayload;

/** Bản sao client: thế tu luyện + hồi chiêu (đếm lùi cục bộ mỗi tick). */
public final class ClientSkillState {
    private ClientSkillState() {}

    private static volatile boolean stance = false;
    private static volatile int page = 0;
    private static final int[] REMAINING = new int[4];
    private static final int[] TOTAL = {1, 1, 1, 1};

    public static void update(SkillSyncPayload p) {
        stance = p.stance();
        page = p.page();
        for (int i = 0; i < 4 && i < p.remaining().size(); i++) {
            REMAINING[i] = p.remaining().get(i);
            TOTAL[i] = Math.max(1, p.total().get(i));
        }
    }

    public static void tick() {
        for (int i = 0; i < 4; i++) if (REMAINING[i] > 0) REMAINING[i]--;
    }

    public static int page() { return page; }

    public static boolean stance() { return stance; }

    public static int remaining(int slot) { return REMAINING[slot]; }

    public static int total(int slot) { return TOTAL[slot]; }
}
