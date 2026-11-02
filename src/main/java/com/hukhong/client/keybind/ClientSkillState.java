package com.hukhong.client.keybind;

import com.hukhong.network.SkillSyncPayload;
import com.hukhong.network.TranSyncPayload;

/** Bản sao client: thế tu luyện + hồi chiêu (đếm lùi cục bộ mỗi tick). */
public final class ClientSkillState {
    private ClientSkillState() {}

    private static volatile boolean stance = false;
    private static volatile int page = 0;
    private static volatile boolean domain = false;
    private static final int[] REMAINING = new int[4];
    private static final int[] TOTAL = {1, 1, 1, 1};

    public static void update(SkillSyncPayload p) {
        stance = p.stance();
        page = p.page();
        domain = p.domain();
        for (int i = 0; i < 4 && i < p.remaining().size(); i++) {
            REMAINING[i] = p.remaining().get(i);
            TOTAL[i] = Math.max(1, p.total().get(i));
        }
    }

    private static volatile int tranStance = 0, tranArmed = 0, tranStacks = 0, tranExhaust = 0;

    /** Trận Tu: thế (ordinal TranStance), tick còn lại của chọn thế, Kiệt Lực, tick còn lại của Kiệt Sức. */
    public static void updateTran(TranSyncPayload p) {
        tranStance = p.stance();
        tranArmed = p.armedTicks();
        tranStacks = p.stacks();
        tranExhaust = p.exhaustTicks();
    }

    public static int tranStance() { return tranStance; }
    public static int tranArmed() { return tranArmed; }
    public static int tranStacks() { return tranStacks; }
    public static int tranExhaust() { return tranExhaust; }

    public static void tick() {
        if (tranArmed > 0) tranArmed--;
        if (tranExhaust > 0) tranExhaust--;
        for (int i = 0; i < 4; i++) if (REMAINING[i] > 0) REMAINING[i]--;
    }

    public static int page() { return page; }

    public static boolean stance() { return stance; }

    /** Kiếm Vực (kênh) đang bật. */
    public static boolean domain() { return domain; }

    public static int remaining(int slot) { return REMAINING[slot]; }

    public static int total(int slot) { return TOTAL[slot]; }
}
