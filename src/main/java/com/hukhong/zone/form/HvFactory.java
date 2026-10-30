package com.hukhong.zone.form;

import com.hukhong.entity.HuHoa;
import com.hukhong.entity.ModEntities;
import com.hukhong.liberation.LiberationManager;
import com.hukhong.zone.ZoneBossFactory;
import com.hukhong.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/** Nha may boss vung: sinh Hu Vuong dang {@code zone.form} o cap {@code zone.level}, ten rieng theo vung. */
public final class HvFactory {
    private HvFactory() {}

    public static final ZoneBossFactory INSTANCE = HvFactory::spawn;

    public static Mob spawn(ServerLevel ow, ZoneData.Zone zone, BlockPos pos) {
        HuVuongForm b = ModEntities.HU_VUONG_FORM.get().create(ow);
        if (b == null) return ZoneBossFactory.DEFAULT.spawn(ow, zone, pos);
        HvForm form = HvForm.byId(zone.form);
        b.setForm(form);
        b.setZoneInfo(zone.id, zone.level, zone.tier());
        b.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ow.random.nextFloat() * 360f, 0f);
        // Chi so: cung duong cong boss vung (bossMul theo cap vung) nhung di qua HuHoa.applyStats de dong bo giap/toc do/cap hien thi
        double mul = LiberationManager.bossMul(zone.level);
        HuHoa.applyStats(b, zone.level, mul * form.hpMul / HuHoa.hpScale(zone.level), mul / HuHoa.dmgScale(zone.level));
        b.getPersistentData().putInt("hk_zone_level", zone.level);
        b.setCustomName(Component.literal(zone.bossTitle()).withStyle(Style.EMPTY.withColor(form.pal.rgb).withBold(true)));
        b.setCustomNameVisible(true);
        b.finalizeSpawn(ow, ow.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        ow.addFreshEntity(b);
        b.initScaling(ow);
        for (ServerPlayer p : ow.players())
            if (p.distanceToSqr(b) < 128 * 128) p.displayClientMessage(Component.translatable("hukhong.boss.appear"), false);
        return b;
    }
}
