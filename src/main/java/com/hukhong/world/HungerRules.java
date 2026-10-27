package com.hukhong.world;

import com.hukhong.HuKhong;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Đói chậm hơn 5 lần: chỉ giữ 20% phần mệt mỏi (exhaustion) tăng thêm mỗi tick. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HungerRules {
    private HungerRules() {}
    public static final float KEEP = 0.2f;
    private static final Map<Player, Float> LAST = new WeakHashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        FoodData f = p.getFoodData();
        float now = f.getExhaustionLevel();
        Float last = LAST.get(p);
        if (last != null && now > last) {
            now = last + (now - last) * KEEP;
            f.setExhaustion(now);
        }
        LAST.put(p, now);
    }
}
