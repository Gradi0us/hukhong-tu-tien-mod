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
 * hợp thể, bắn T/Q/R, tự huỷ; chụp ảnh vào docs/vfx-review/ma và ghi log kiểm chứng rồi thoát.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevMaTest {
    private DevMaTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.matest") != null;
    private static final String DIR = "D:/mc_mod/docs/vfx-review/ma";
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
            String name = "hk-ma-test-" + System.currentTimeMillis();
            LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            LOG.info("MATEST creating fresh world {}", name);
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
        if (t == 100) server.execute(() -> {
            var d = sp.getData(com.hukhong.core.ModAttachments.CULTIVATION);
            sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, new com.hukhong.core.CultivationData(60, 0, 1000, 1000,
                    java.util.Set.of(com.hukhong.core.Path.MA), java.util.Map.of(com.hukhong.core.Path.MA, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
            com.hukhong.stats.StatService.refresh(sp);
            sp.setGameMode(GameType.SURVIVAL);
            sp.setInvulnerable(true);
            com.hukhong.skill.SkillService.state(sp).stance = true;
            com.hukhong.skill.SkillService.state(sp).page = 0;
            lv.setDayTime(18000);
            lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            sp.teleportTo(0.5, sp.getY(), 0.5);
            Vec3 c = sp.position();
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Zombie z = EntityType.ZOMBIE.create(lv);
                z.moveTo(c.x + Math.cos(a) * 4, c.y, c.z + Math.sin(a) * 4);
                z.setNoAi(true);
                z.setPersistenceRequired();
                lv.addFreshEntity(z);
                mobs.add(z);
            }
        });
        if (t == 110) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (t == 130) server.execute(() -> { hpMark(); com.hukhong.skill.SkillService.cast(sp, 2);
            LOG.info("MATEST domain active={} openDmg={}", com.hukhong.skill.MaDomain.active(sp), hpDelta()); });
        if (t == 140 || t == 175) shot("01_front_" + t);
        if (t == 180) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (t == 195) shot("02_back");
        if (t == 196) server.execute(() -> LOG.info("MATEST hover y={} hp={}/{}", sp.getY(), sp.getHealth(), sp.getMaxHealth()));
        if (t == 200) server.execute(() -> { hpMark(); com.hukhong.skill.SkillService.cast(sp, 2);
            LOG.info("MATEST detonate active={} dmg={}", com.hukhong.skill.MaDomain.active(sp), hpDelta()); });
        if (t == 203 || t == 212) shot("03_boom_" + t);
        if (t == 240) { LOG.info("MATEST done"); mc.stop(); }
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
            LOG.info("MATEST screenshot {}", f);
        } catch (Exception ex) {
            LOG.error("MATEST screenshot failed", ex);
        }
    }
}
