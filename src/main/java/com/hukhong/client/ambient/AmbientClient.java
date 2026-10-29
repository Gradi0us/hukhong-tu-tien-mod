package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.client.minimap.MinimapState;
import com.hukhong.network.MapMarkersPayload;
import com.hukhong.world.SafeZones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Lõi khí quyển phía client: đọc dữ liệu thô ({@link AmbientState}, marker thành/tông, chiều không gian), làm mượt
 * mọi trọng số (~6-8s, không giật) và điều phối emitter/âm thanh/thẻ tên. Mọi hiệu ứng khác chỉ đọc các getter ở đây.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class AmbientClient {
    private AmbientClient() {}

    public static final ResourceKey<Level> BICANH = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "bicanh"));
    public static final ResourceKey<Level> TAM_THUC = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tam_thuc"));
    public static final ResourceKey<Level> BAN_NGUYEN = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "hu_khong_ban_nguyen"));

    public static final int Z_NONE = 0, Z_CITY = 1, Z_PORT = 2, Z_SECT = 3, Z_GUILD = 4;

    private static final float STEP_SLOW = 1f / (7f * 20f), STEP_MID = 1f / (4.5f * 20f);

    private static float corr, corrO, war, warO, peace, peaceO, dust, dustO, master, masterO;
    private static int zoneKind;
    private static MapMarkersPayload.Marker zone;
    private static float night, rain;
    private static boolean overworld, bicanh, tamThuc;
    private static long tick;
    private static boolean selfTested;

    // ---------------------------------------------------------------- getters (đã nội suy partial tick)
    /** Hệ số cường độ tổng (config × bật/tắt, làm mượt). */
    public static float master(float pt) { return Mth.lerp(pt, masterO, master); }
    /** Mức nhiễm Hư Hóa 0..1 (đã nhân cường độ). */
    public static float corruption(float pt) { return Mth.lerp(pt, corrO, corr) * master(pt); }
    /** Mức chiến sự (xâm lăng/đột kích) 0..1. */
    public static float war(float pt) { return Mth.lerp(pt, warO, war) * master(pt); }
    /** Mức yên bình trong thành/cảng/tông. */
    public static float peace(float pt) { return Mth.lerp(pt, peaceO, peace) * master(pt); }
    /** Mức bụi/ánh sáng Bí Cảnh/Tâm Thức. */
    public static float dust(float pt) { return Mth.lerp(pt, dustO, dust) * master(pt); }
    public static float rawCorruption() { return corr; }
    public static float rawWar() { return war; }
    public static float rawPeace() { return peace; }
    public static int zoneKind() { return zoneKind; }
    public static MapMarkersPayload.Marker zone() { return zone; }
    public static float night() { return night; }
    public static float rain() { return rain; }
    public static boolean overworld() { return overworld; }
    public static boolean inBiCanh() { return bicanh; }
    public static boolean inTamThuc() { return tamThuc; }
    public static long ticks() { return tick; }
    public static boolean active() { return master > 0.01f; }

    static float step(float cur, float target, float s) {
        return cur < target ? Math.min(target, cur + s) : Math.max(target, cur - s * 1.4f);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        corrO = corr; warO = war; peaceO = peace; dustO = dust; masterO = master;
        if (!selfTested && System.getProperty("hukhong.ambient.selftest") != null && mc.getOverlay() == null) {
            selfTested = true;
            AmbientPost.selfTest();
        }
        if (lv == null || mc.player == null) {
            corr = war = peace = dust = 0; master = 0;
            zone = null; zoneKind = Z_NONE;
            AmbientSounds.clear();
            return;
        }
        if (mc.isPaused()) return;
        tick++;
        overworld = lv.dimension() == Level.OVERWORLD;
        bicanh = lv.dimension() == BICANH;
        tamThuc = lv.dimension() == TAM_THUC;
        float masterT = ClientConfig.ambientVfx() ? ClientConfig.ambientIntensity() : 0f;
        master = step(master, masterT, 1f / 40f);
        double px = mc.player.getX(), pz = mc.player.getZ();

        float corrT = overworld ? Math.max(AmbientState.corruptionAt(px, pz), com.hukhong.client.boss.BossSkyFx.corr()) : 0f;
        float warT = overworld ? Math.max(AmbientState.warAt(px, pz), com.hukhong.client.boss.BossSkyFx.war()) : 0f;
        corr = step(corr, corrT, STEP_SLOW);
        war = step(war, warT, STEP_SLOW);

        // yên bình: gần thành/cảng/tông/hiệp hội; bị lấn át khi nhiễm hoặc chiến sự
        zone = null; zoneKind = Z_NONE;
        float peaceT = 0f;
        if (overworld) {
            double best = 1e9;
            for (MapMarkersPayload.Marker m : MinimapState.markers()) {
                if (!m.built()) continue;
                int half = halfOf(m);
                double edge = Math.max(Math.abs(px - m.x()), Math.abs(pz - m.z())) - half;
                if (edge < best) { best = edge; zone = m; }
            }
            if (zone != null) {
                peaceT = (float) (1.0 - Mth.smoothstep(best / 44.0));
                zoneKind = kindOf(zone);
                if (best > 44) { zone = null; zoneKind = Z_NONE; }
            }
        }
        peaceT *= (1f - Math.max(corr, war));
        peace = step(peace, peaceT, STEP_MID);

        float dustT = 0f;
        if (bicanh || tamThuc) dustT = 1f;
        dust = step(dust, dustT, STEP_MID);

        // thời gian
        float tod = lv.getTimeOfDay(1f);
        float day = Mth.clamp(Mth.cos(tod * Mth.TWO_PI) * 2f + 0.5f, 0f, 1f);
        night = overworld ? 1f - day : 0f;
        rain = lv.getRainLevel(1f);

        if (master > 0.01f) {
            AmbientEmitters.tick(mc, lv);
            ZoneTitle.tick(mc, zone, zoneKind, tick);
        }
        AmbientSounds.tick(mc, lv);
        MoteSystem.tick(mc, lv);
        AmbientSky.tick(mc, lv);
    }

    static int halfOf(MapMarkersPayload.Marker m) {
        return switch (m.kind()) {
            case MapMarkersPayload.GUILD -> SafeZones.GUILD_HALF;
            case MapMarkersPayload.SECT -> com.hukhong.society.SectBuilder.HALF;
            default -> MinimapState.isPort(m) ? SafeZones.PORT_HALF : com.hukhong.city.CityBuilder.HALF;
        };
    }

    static int kindOf(MapMarkersPayload.Marker m) {
        return switch (m.kind()) {
            case MapMarkersPayload.GUILD -> Z_GUILD;
            case MapMarkersPayload.SECT -> Z_SECT;
            default -> MinimapState.isPort(m) ? Z_PORT : Z_CITY;
        };
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        AmbientState.reset();
        MoteSystem.clear();
        AmbientSounds.clear();
        corr = war = peace = dust = master = 0;
        corrO = warO = peaceO = dustO = masterO = 0;
    }
}
