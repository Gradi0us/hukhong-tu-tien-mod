package com.hukhong.creature;

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

/** Nap sinh vat tu datapack (hukhong_creatures). Thieu def -> dung mac dinh trong {@link CreatureKind}. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CreatureDefs extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, CreatureDef> defs = Map.of();

    private CreatureDefs() { super(new Gson(), "hukhong_creatures"); }

    public static CreatureDef get(String id) { return defs.get(id); }
    public static Collection<CreatureDef> all() { return defs.values(); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        Map<String, CreatureDef> m = new LinkedHashMap<>();
        for (var e : in.entrySet()) {
            try {
                String id = e.getKey().getPath();
                m.put(id, CreatureDef.parse(id, e.getValue().getAsJsonObject()));
            } catch (Exception ex) {
                com.mojang.logging.LogUtils.getLogger().error("Bad creature {}: {}", e.getKey(), ex.toString());
            }
        }
        defs = m;
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) { e.addListener(new CreatureDefs()); }
}
