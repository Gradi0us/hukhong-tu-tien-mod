package com.hukhong.zone;

import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuVuong;
import com.hukhong.events.CorruptionManager;
import com.hukhong.liberation.LiberationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Diem cam tao boss trung tam vung. Mac dinh = HuVuong hien tai scale theo cap vung.
 * Agent lam 8 dang Hu Vuong: gan {@code ZoneBossFactory.Holder.current = (ow, zone, pos) -> ...} (zone.form = goi y dang 0..7).
 * Factory chi can tao + addFreshEntity va tra ve Mob; ZoneManager tu gan the vung + theo doi chet.
 */
@FunctionalInterface
public interface ZoneBossFactory {
    Mob spawn(ServerLevel ow, ZoneData.Zone zone, BlockPos pos);

    ResourceLocation ZONE_MOD = ResourceLocation.fromNamespaceAndPath("hukhong", "zone_boss_level");

    ZoneBossFactory DEFAULT = (ow, zone, pos) -> {
        HuVuong b = CorruptionManager.spawnBoss(ow, pos);
        if (b == null) return null;
        double mul = LiberationManager.bossMul(zone.level) - 1;
        for (var a : java.util.List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE)) {
            var inst = b.getAttribute(a);
            if (inst == null) continue;
            inst.removeModifier(ZONE_MOD);
            inst.addPermanentModifier(new AttributeModifier(ZONE_MOD, mul, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        b.setHealth(b.getMaxHealth());
        b.getPersistentData().putInt("hk_zone_level", zone.level);
        String tier = ZoneMath.tierLabel(zone.tier());
        b.setCustomName(HuHoa.name(zone.level, Component.literal((tier.isEmpty() ? "" : tier + " ") + "Hư Vương")));
        b.setCustomNameVisible(true);
        return b;
    };

    /** Nha may dang dung. */
    final class Holder {
        private Holder() {}
        public static volatile ZoneBossFactory current = com.hukhong.zone.form.HvFactory.INSTANCE;
    }
}
