package com.hukhong.story.npc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.hukhong.HuKhong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Nạp bảng thoại từ datapack (thư mục hukhong/dialogue). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DialogueManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, Dialogue> byNpc = Map.of();

    private DialogueManager() { super(new Gson(), "hukhong/dialogue"); }

    public static Dialogue get(String npc) { return byNpc.get(npc); }

    public static Iterable<Dialogue> all() { return byNpc.values(); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        Map<String, Dialogue> m = new LinkedHashMap<>();
        for (var e : in.entrySet()) {
            try {
                Dialogue d = Dialogue.parse(e.getValue().getAsJsonObject());
                m.put(d.npc(), d);
            } catch (Exception ex) {
                com.mojang.logging.LogUtils.getLogger().error("Bad dialogue {}: {}", e.getKey(), ex.toString());
            }
        }
        byNpc = m;
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) { e.addListener(new DialogueManager()); }
}
