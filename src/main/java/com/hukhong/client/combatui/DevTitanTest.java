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
 * hợp thể, bắn T/Q/R, tự huỷ; chụp ảnh vào docs/vfx-review/titan và ghi log kiểm chứng rồi thoát.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevTitanTest {
    private DevTitanTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.titantest") != null;
    private static final String DIR = "D:/mc_mod/docs/vfx-review/titan";
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
            String name = "hk-titan-test-" + System.currentTimeMillis();
            LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            LOG.info("TITANTEST creating fresh world {}", name);
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
                    java.util.Set.of(com.hukhong.core.Path.PHAP), java.util.Map.of(com.hukhong.core.Path.PHAP, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
            com.hukhong.phaptu.PhapTu.setElement(sp, com.hukhong.phaptu.PhapElement.THO);
            com.hukhong.stats.StatService.refresh(sp);
            sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, sp.getData(com.hukhong.core.ModAttachments.CULTIVATION).withResource(sp.getData(com.hukhong.core.ModAttachments.CULTIVATION).resourceMax()));
            sp.setGameMode(GameType.SURVIVAL);
            sp.setInvulnerable(true);
            com.hukhong.skill.SkillService.state(sp).stance = true;
            com.hukhong.skill.SkillService.state(sp).page = 0;
            lv.setDayTime(18000);
            lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            sp.teleportTo(0.5, sp.getY(), 0.5);
            sp.setYRot(0); sp.setXRot(10);
            LOG.info("TITANTEST setup player at {}", sp.position());
        });
        if (t == 120) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (t == 130) server.execute(() -> {
            Vec3 c = sp.position();
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 10, r = 9 + (i % 3) * 3;
                Zombie z = EntityType.ZOMBIE.create(lv);
                z.moveTo(c.x + Math.cos(a) * r, c.y, c.z + 14 + Math.sin(a) * r);
                z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(2000);
                z.setHealth(2000);
                z.setPersistenceRequired();
                lv.addFreshEntity(z);
                mobs.add(z);
            }
            com.hukhong.phaptu.PhapRuntime.summonTurret(lv, sp, c.add(0, 0, 6));
            com.hukhong.phaptu.PhapRuntime.summonTurret(lv, sp, c.add(1.5, 0, 7));
            LOG.info("TITANTEST zombies {} turrets {}", mobs.size(), com.hukhong.phaptu.PhapRuntime.turretsOf(lv, sp).size());
        });
        if (t == 150) server.execute(() -> {
            boolean ok = com.hukhong.phaptu.ThoTitan.onE(lv, sp);
            titan = com.hukhong.phaptu.ThoTitan.titan(sp);
            LOG.info("TITANTEST create titan ok={} titan={} life={} mana={}", ok, titan != null, titan == null ? -1 : titan.lifeLeft(),
                    sp.getData(com.hukhong.core.ModAttachments.CULTIVATION).resource());
        });
        if (t == 165 || t == 200) shot("01_auto_" + t);
        if (t == 210) server.execute(() -> LOG.info("TITANTEST auto dmg dealt = {}", dealt()));
        if (t == 215) server.execute(() -> { // lại gần trụ rồi hợp thể (vẫn trong 5s? -> dùng khoảng còn lại)
            if (titan != null) sp.teleportTo(titan.getX() + 3.0, titan.getY(), titan.getZ());
        });
        if (t == 220) server.execute(() -> {
            com.hukhong.phaptu.ThoTitan.onTap(sp);
            LOG.info("TITANTEST enter controlling={} life={} seat={} player={}", com.hukhong.phaptu.ThoTitan.controlling(sp),
                    titan == null ? -1 : titan.lifeLeft(), titan == null ? null : titan.position(), sp.position());
        });
        if (t == 230) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (t == 235) server.execute(() -> aimAt(sp, mobs.get(0)));
        if (t == 240) shot("02_pilot_view");
        if (t == 245) server.execute(() -> {
            hpMark();
            com.hukhong.skill.SkillService.cast(sp, 3);
            LOG.info("TITANTEST T laser active={}", com.hukhong.phaptu.ThoTitan.laserActive(sp));
        });
        if (t > 245 && t < 300 && t % 5 == 0) server.execute(() -> aimAt(sp, firstAlive()));
        if (t == 255 || t == 270 || t == 290) shot("03_laser_" + t);
        if (t == 300) server.execute(() -> {
            com.hukhong.skill.SkillService.cast(sp, 3); // tạm dừng
            LOG.info("TITANTEST T paused active={} laserDmg={}", com.hukhong.phaptu.ThoTitan.laserActive(sp), hpDelta());
        });
        if (t == 310) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (t == 315) server.execute(() -> { hpMark(); com.hukhong.skill.SkillService.cast(sp, 0); });
        if (t == 330 || t == 350) shot("04_q_" + t);
        if (t == 380) server.execute(() -> { LOG.info("TITANTEST Q dmg={}", hpDelta()); com.hukhong.skill.SkillService.cast(sp, 2); });
        if (t == 390 || t == 410) shot("05_r_" + t);
        if (t == 412) server.execute(() -> LOG.info("TITANTEST spikes={}", lv.getEntitiesOfClass(com.hukhong.phaptu.PhapTurret.class,
                sp.getBoundingBox().inflate(30), com.hukhong.phaptu.PhapTurret::isSpike).size()));
        if (t == 420) server.execute(() -> { hpMark(); com.hukhong.phaptu.ThoTitan.selfDestruct(sp);
            LOG.info("TITANTEST destruct started={}", com.hukhong.phaptu.ThoTitan.selfDestructing(sp)); });
        if (t == 480 || t == 540) shot("06_destruct_" + t);
        if (t == 562 || t == 568 || t == 580 || t == 600) shot("07_nuke_" + t);
        if (t == 605) server.execute(() -> LOG.info("TITANTEST after nuke titanAlive={} phasing={} immune={} visible={} nukeDmg={} player={}",
                titan != null && titan.isAlive(), com.hukhong.phaptu.ThoService.phasing(sp), com.hukhong.phaptu.ThiemBoService.immune(sp),
                !sp.isInvisible(), hpDelta(), sp.position()));
        if (t == 620) { LOG.info("TITANTEST done"); mc.stop(); }
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
            LOG.info("TITANTEST screenshot {}", f);
        } catch (Exception ex) {
            LOG.error("TITANTEST screenshot failed", ex);
        }
    }
}
