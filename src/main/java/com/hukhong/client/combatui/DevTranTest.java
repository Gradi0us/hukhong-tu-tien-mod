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
 * hợp thể, bắn T/Q/R, tự huỷ; chụp ảnh vào docs/vfx-review/tran_vk và ghi log kiểm chứng rồi thoát.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevTranTest {
    private DevTranTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.trantest") != null;
    private static final String DIR = "D:/mc_mod/docs/vfx-review/tran_vk";
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
            String name = "hk-kiem-test-" + System.currentTimeMillis();
            LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            LOG.info("TRANTEST creating fresh world {}", name);
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
                    java.util.Set.of(com.hukhong.core.Path.TRAN), java.util.Map.of(com.hukhong.core.Path.TRAN, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
            com.hukhong.stats.StatService.refresh(sp);
            sp.setGameMode(GameType.SURVIVAL);
            sp.setInvulnerable(true);
            com.hukhong.skill.SkillService.state(sp).stance = true;
            com.hukhong.skill.SkillService.state(sp).page = 0;
            lv.setDayTime(18000);
            lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            sp.teleportTo(0.5, sp.getY(), 0.5);
            Vec3 c = sp.position();
            for (int i = 0; i < 7; i++) {
                Zombie z = EntityType.ZOMBIE.create(lv);
                z.moveTo(c.x - 4 + (i % 4) * 2.7, c.y, c.z + 5 + (i / 4) * 3);
                z.setNoAi(true);
                z.setPersistenceRequired();
                z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(2000);
                z.setHealth(2000);
                lv.addFreshEntity(z);
                mobs.add(z);
            }
        });
        if (t == 110) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (t == 112) server.execute(() -> { sp.setYRot(0); sp.setXRot(40); sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), 0, 40); });
        if (t == 120) server.execute(() -> {
            sp.setYRot(0); sp.setXRot(25); sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), 0, 25);
            com.hukhong.skill.SkillService.cast(sp, 2); // R Hộ Thân Trận (thế Cơ bản)
            LOG.info("TRANTEST hoThan={} cdR={}", com.hukhong.tran.TranRuntime.hasHoThan(sp), com.hukhong.skill.SkillService.state(sp).cooldownEnd[2] - lv.getGameTime());
        });
        if (t == 128) shot("01_hothan");
        if (t == 130) server.execute(() -> {
            com.hukhong.skill.SkillService.cast(sp, 3); // T chọn thế
            com.hukhong.skill.SkillService.cast(sp, 1); // E -> thế Sát Thương
            LOG.info("TRANTEST stance={} cdR(after switch)={}", com.hukhong.tran.TranRuntime.stance(sp), com.hukhong.skill.SkillService.state(sp).cooldownEnd[2] - lv.getGameTime());
            hpMark();
            com.hukhong.skill.SkillService.cast(sp, 1); // E Vạn Kiếm Trận
        });
        if (t == 145) shot("02_blades");
        if (t == 175) server.execute(() -> {
            int hit = 0;
            for (Zombie z : mobs) if (z.getHealth() < z.getMaxHealth()) hit++;
            LOG.info("TRANTEST blades dmg={} targetsHit={} hoThan={}", hpDelta(), hit, com.hukhong.tran.TranRuntime.hasHoThan(sp));
            sp.connection.teleport(sp.getX() + 4, sp.getY(), sp.getZ() + 2, 0, 35);
        });
        if (t == 190) shot("03_hothan_follow");
        if (t == 200) { LOG.info("TRANTEST done"); mc.stop(); }
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
            LOG.info("TRANTEST screenshot {}", f);
        } catch (Exception ex) {
            LOG.error("TRANTEST screenshot failed", ex);
        }
    }
}
