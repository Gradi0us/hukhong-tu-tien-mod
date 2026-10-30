package com.hukhong.client.endgame;

import com.hukhong.endgame.EndgameNet;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Điểm vào phía client của Bước 26 (gọi từ payload). */
public final class EndgameClient {
    private EndgameClient() {}

    public static void openChoice(EndgameNet.OpenChoice c) { Minecraft.getInstance().setScreen(new EndingChoiceScreen(c)); }

    public static void playCutscene(int ending) { Minecraft.getInstance().setScreen(new CutsceneScreen(ending)); }

    /** Cutscene + credits xong (hoặc bỏ qua): báo server đưa về thế giới. */
    public static void finish() {
        Minecraft.getInstance().setScreen(null);
        PacketDistributor.sendToServer(new EndgameNet.Finish());
    }
}
