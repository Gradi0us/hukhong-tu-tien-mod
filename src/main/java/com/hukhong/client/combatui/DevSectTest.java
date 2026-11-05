package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * DEV ONLY (./gradlew runClient -Ptitantest): tự tạo một thế giới phẳng MỚI (không đụng save đang chơi), dựng trụ cấp 3 Thổ,
 * hợp thể, bắn T/Q/R, tự huỷ; chụp ảnh vào docs/vfx-review/sect và ghi log kiểm chứng rồi thoát.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevSectTest {
    private DevSectTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.secttest") != null;
    private static final String DIR = "D:/mc_mod/docs/vfx-review/sect";
    private static boolean created;
    private static int t;
    private static final List<Zombie> mobs = new ArrayList<>();
    private static com.hukhong.phaptu.PhapTurret titan;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created && mc.level == null && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
            created = true;
            String name = "hk-sect-test-" + System.currentTimeMillis();
            LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            LOG.info("SECTTEST creating fresh world {}", name);
            return;
        }
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) mc.setScreen(null);
        if (mc.isPaused()) return;
        t++;
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) return;
        var lv = sp.serverLevel();
        if (t == 60) server.execute(() -> {
            sp.setGameMode(GameType.CREATIVE);
            lv.setDayTime(6000);
            lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            long t0 = System.currentTimeMillis();
            var c = com.hukhong.society.SectBuilder.buildProcedural(lv, new net.minecraft.core.BlockPos(0, sp.getBlockY(), -80),
                    com.hukhong.society.SectDef.ALL.get(0));
            LOG.info("SECTTEST built at {} in {} ms", c, System.currentTimeMillis() - t0);
        });
        if (t == 118) server.execute(() -> { sp.getAbilities().flying = true; sp.onUpdateAbilities(); });
        if (t == 120) server.execute(() -> sp.connection.teleport(0.5, 63 + 62, -80 + 92, 180, 36));
        if (t == 150) shot("01_overview");
        if (t == 155) server.execute(() -> sp.connection.teleport(0.5, 63 + 7, -80 + 70, 180, 8));
        if (t == 180) shot("02_gate");
        if (t == 185) server.execute(() -> sp.connection.teleport(26.5, 63 + 14, -80 + 46, 150, 22));
        if (t == 210) shot("03_lake_pagoda");
        if (t == 215) server.execute(() -> sp.connection.teleport(0.5, 63 + 9, -80 - 2, 180, 12));
        if (t == 240) shot("04_altar_hall");
        if (t == 245) server.execute(() -> {
            sp.connection.teleport(10.5, 64, -80 + 37, 0, 30);
            LOG.info("SECTTEST zone at lake = {}", com.hukhong.society.SectZones.zoneOf(sp));
        });
        if (t == 250) server.execute(() -> {
            String[] ids = {"hu_lang_vuong", "thach_cu_nhan", "huyet_yeu_vuong", "hu_lang", "thach_quy", "am_hon_su", "hoa_tinh"};
            for (int i = 0; i < ids.length; i++) {
                var k = com.hukhong.creature.CreatureKind.byId(ids[i]);
                var m = (net.minecraft.world.entity.Mob) com.hukhong.creature.CreatureRegistry.type(k).create(lv);
                m.moveTo(-18 + i * 6, -60, 140, 180, 0);
                m.setNoAi(true);
                m.setPersistenceRequired();
                lv.addFreshEntity(m);
            }
            sp.connection.teleport(0.5, -55, 122, 0, 12);
        });
        if (t == 280) shot("05_mobs");
        if (t == 300) { LOG.info("SECTTEST done"); mc.stop(); }
    }

    private static final java.util.Map<Zombie, Float> mark = new java.util.HashMap<>();

    private static void hpMark() { mark.clear(); for (Zombie z : mobs) mark.put(z, z.isAlive() ? z.getHealth() : 0f); }

    private static float hpDelta() {
        float s = 0;
        for (var en : mark.entrySet()) s += en.getValue() - (en.getKey().isAlive() ? en.getKey().getHealth() : 0f);
        return s;
    }

    private static float dealt() {
        float s = 0;
        for (Zombie z : mobs) s += 2000 - (z.isAlive() ? z.getHealth() : 0);
        return s;
    }

    private static Zombie firstAlive() {
        for (Zombie z : mobs) if (z.isAlive()) return z;
        return mobs.get(0);
    }

    private static void aimAt(ServerPlayer sp, Zombie z) {
        if (z == null) return;
        Vec3 d = z.getBoundingBox().getCenter().subtract(sp.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        sp.setYRot(yaw); sp.setXRot(pitch); sp.setYHeadRot(yaw);
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), yaw, pitch);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        try (var img = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            java.io.File f = new java.io.File(DIR, name + ".png");
            f.getParentFile().mkdirs();
            img.writeToFile(f);
            LOG.info("SECTTEST screenshot {}", f);
        } catch (Exception ex) {
            LOG.error("SECTTEST screenshot failed", ex);
        }
    }
}
