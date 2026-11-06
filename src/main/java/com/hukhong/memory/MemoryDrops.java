package com.hukhong.memory;

import com.hukhong.HuKhong;
import com.hukhong.story.SuDo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Nguồn rơi Ký Ức:
 * - onPoiLoot: lần đầu người chơi nhận vật phẩm từ một loại địa điểm -> tặng một ký ức chưa thấy (ưu tiên ký ức gắn loại đó).
 * - Quái tinh anh Hư Không và Sứ Đồ: 2% mỗi lần hạ, tặng một ký ức chưa thấy.
 * Tinh anh = quái thường mang tên + phát sáng (quy ước của mod: BiCanhManager, QuestService).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MemoryDrops {
    private MemoryDrops() {}

    public static final double ELITE_CHANCE = 0.02;

    /** Gọi từ PoiRewards.giveAll và PoiService.clear (mỗi người chơi trong vùng). Chỉ tác dụng lần đầu cho mỗi loại. */
    public static void onPoiLoot(ServerPlayer p, String kind) {
        if (MemoryService.data(p).looted(kind)) return;
        MemoryService.markLooted(p, kind);
        MemoryService.grantUnseen(p, MemoryLore.preferredFor(kind));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        LivingEntity v = e.getEntity();
        boolean elite = v instanceof SuDo || (v instanceof Monster && v.isCurrentlyGlowing() && v.hasCustomName());
        if (!elite || p.getRandom().nextDouble() >= ELITE_CHANCE) return;
        MemoryService.grantUnseen(p, 0);
    }
}
