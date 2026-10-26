package com.hukhong.client.story;

import com.hukhong.story.StoryData;
import net.minecraft.client.Minecraft;

/** Bản sao phía client của StoryData + mở màn chọn mode. */
public final class StoryClient {
    private StoryClient() {}

    private static volatile StoryData data = StoryData.DEFAULT;

    public static StoryData data() { return data; }

    public static void update(StoryData d) { data = d; }

    public static void openModeScreen() { Minecraft.getInstance().setScreen(new StoryModeScreen()); }
}
