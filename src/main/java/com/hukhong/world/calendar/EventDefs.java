package com.hukhong.world.calendar;

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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Nap su kien lich tu datapack (thu muc hukhong_events). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class EventDefs extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, EventDef> defs = Map.of();

    private EventDefs() { super(new Gson(), "hukhong_events"); }

    public static EventDef get(String id) { return defs.get(id); }
    public static Collection<EventDef> all() { return defs.values(); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        Map<String, EventDef> m = new LinkedHashMap<>();
        for (var e : in.entrySet()) {
            try {
                String id = e.getKey().getPath();
                m.put(id, EventDef.parse(id, e.getValue().getAsJsonObject()));
            } catch (Exception ex) {
                com.mojang.logging.LogUtils.getLogger().error("Bad calendar event {}: {}", e.getKey(), ex.toString());
            }
        }
        defs = m;
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) { e.addListener(new EventDefs()); }
}
