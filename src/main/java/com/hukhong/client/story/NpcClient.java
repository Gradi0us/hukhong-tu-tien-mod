package com.hukhong.client.story;

import com.hukhong.story.npc.NpcNet;
import com.hukhong.story.npc.QuestData;
import net.minecraft.client.Minecraft;

/** Phía client: bản sao QuestData + mở/đóng hộp thoại NPC. */
public final class NpcClient {
    private NpcClient() {}

    private static volatile QuestData data = QuestData.DEFAULT;

    public static QuestData data() { return data; }

    public static void update(QuestData d) { data = d; }

    public static void view(NpcNet.View v) {
        Minecraft mc = Minecraft.getInstance();
        if (v.pages().isEmpty()) {
            if (mc.screen instanceof DialogueScreen) mc.setScreen(null);
            return;
        }
        if (mc.screen instanceof DialogueScreen ds) ds.show(v);
        else mc.setScreen(new DialogueScreen(v));
    }
}
