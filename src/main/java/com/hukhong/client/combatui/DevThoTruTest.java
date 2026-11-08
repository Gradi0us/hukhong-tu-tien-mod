package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.hukhong.phaptu.PhapTurret;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
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

/** Dev: ./gradlew runClient -Pthotrutest -> thế giới phẳng mới, dựng 3 cấp Thổ Trụ, chụp ảnh ngầm (cửa sổ ngoài màn hình) vào docs/vfx-review/thotru. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevThoTruTest {
    private DevThoTruTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.thotrutest") != null;
    /** -Pthotrudemo: mở cửa sổ thật cho người chơi xem, dùng lại map cố định (không tạo thế giới mới mỗi lần), không tự tắt. */
    private static final boolean DEMO = System.getProperty("hukhong.thotrudemo") != null;
    private static final String WORLD = "hk-thotru-demo";
    private static final String DIR = "D:/mc_mod/docs/vfx-review/thotru";
    private static boolean created, moved;
    private static int t;
    private static PhapTurret t1, t2, t3;
    private static Vec3 base;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created && mc.level == null && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
            created = true;
            if (new java.io.File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").exists()) {
                mc.createWorldOpenFlows().openWorld(WORLD, () -> {});
                return;
            }
            String name = WORLD;
            LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) return;
        mc.options.pauseOnLostFocus = false;
        long w = mc.getWindow().getWindow();
        if (DEMO) { demo(mc, server); return; }
        if (!moved) {
            moved = true;
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(w, -6000, -6000);
            org.lwjgl.glfw.GLFW.glfwSetWindowAttrib(w, org.lwjgl.glfw.GLFW.GLFW_FOCUS_ON_SHOW, org.lwjgl.glfw.GLFW.GLFW_FALSE);
        }
        if (mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();
        org.lwjgl.glfw.GLFW.glfwSetInputMode(w, org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
        if (mc.screen != null) mc.setScreen(null);
        if (mc.isPaused()) return;
        t++;
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) return;
        var lv = sp.serverLevel();
        if (t == 60) server.execute(() -> {
            lv.setDayTime(6000);
            lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            sp.setInvulnerable(true);
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            var d = sp.getData(com.hukhong.core.ModAttachments.CULTIVATION);
            sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, new com.hukhong.core.CultivationData(60, 0, 1000, 1000,
                    java.util.Set.of(com.hukhong.core.Path.PHAP), java.util.Map.of(com.hukhong.core.Path.PHAP, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
            com.hukhong.phaptu.PhapTu.setElement(sp, com.hukhong.phaptu.PhapElement.THO);
            base = sp.position();
            t1 = spawn(lv, sp, base.add(-10, 0, 12));
            t2 = spawn(lv, sp, base.add(0, 0, 12));
            t2.absorb(1000, 20, base.add(0, 0, 12));
            t3 = spawn(lv, sp, base.add(16, 0, 22));
            t3.makeTitan(200, 100000);
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        if (base == null) return;
        // (x, y, z, yaw, pitch, tick, tên)
        Object[][] views = {
                {2.0, 5.0, -8.0, 0f, 12f, 120, "00_all"},
                {-10.0, 2.2, 6.5, 0f, 8f, 150, "01_lv1_front"},
                {-14.0, 3.0, 8.5, -40f, 12f, 170, "02_lv1_34"},
                {-6.5, 2.5, 15.5, 135f, 10f, 190, "03_lv1_back"},
                {0.0, 2.8, 4.0, 0f, 6f, 210, "04_lv2_front"},
                {-6.0, 4.5, 6.0, -40f, 18f, 230, "05_lv2_34"},
                {16.0, 6.0, 4.0, 0f, 8f, 260, "06_lv3_front"},
                {2.0, 12.0, 6.0, -40f, 22f, 280, "07_lv3_34"},
                {16.0, 26.0, 16.0, 0f, 75f, 300, "08_lv3_top"},
                {30.0, 8.0, 34.0, 135f, 10f, 320, "09_lv3_back"},
                {2.0, 12.0, 6.0, -40f, 22f, 350, "10_lv3_ctl"},
        };
        for (Object[] v : views) {
            int at = (int) v[5];
            if (t == at - 12) {
                double x = base.x + (double) v[0], y = base.y + (double) v[1], z = base.z + (double) v[2];
                float yaw = (float) v[3], pitch = (float) v[4];
                server.execute(() -> sp.connection.teleport(x, y, z, yaw, pitch));
            }
            if (t == at) shot((String) v[6]);
        }
        if (t == 335) server.execute(() -> t3.setControlled(true));
        if (t == 370) { LOG.info("THOTRUTEST done"); mc.stop(); }
    }

    private static void demo(Minecraft mc, net.minecraft.server.MinecraftServer server) {
        if (mc.isPaused() && mc.screen == null) return;
        t++;
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null || t != 40) return;
        server.execute(() -> {
            var lv = sp.serverLevel();
            lv.setDayTime(6000);
            lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            sp.setInvulnerable(true);
            var d = sp.getData(com.hukhong.core.ModAttachments.CULTIVATION);
            sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, new com.hukhong.core.CultivationData(60, 0, 1000, 1000,
                    java.util.Set.of(com.hukhong.core.Path.PHAP), java.util.Map.of(com.hukhong.core.Path.PHAP, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
            com.hukhong.phaptu.PhapTu.setElement(sp, com.hukhong.phaptu.PhapElement.THO);
            for (var old : lv.getEntitiesOfClass(PhapTurret.class, sp.getBoundingBox().inflate(80))) old.discard();
            Vec3 b0 = sp.position();
            PhapTurret a = spawn(lv, sp, b0.add(-10, 0, 12));
            PhapTurret m = spawn(lv, sp, b0.add(0, 0, 12));
            m.absorb(1000, 20, b0.add(0, 0, 12));
            PhapTurret ti = spawn(lv, sp, b0.add(16, 0, 22));
            ti.makeTitan(200, 10_000_000);
            a.setLife(10_000_000); m.setLife(10_000_000);
            sp.connection.teleport(b0.x + 2, b0.y, b0.z - 8, 0, 5);
            var boss = com.hukhong.entity.V3Entities.SA_HAT_VUONG.get().create(lv); // boss cấp 30 để xem trụ đánh
            if (boss != null) {
                boss.moveTo(b0.x + 2, b0.y, b0.z + 34, 180, 0);
                lv.addFreshEntity(boss);
                boss.initBoss(lv, 30);
                boss.setPersistenceRequired();
            }
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6Thổ Trụ: trái = cấp 1, giữa = cấp 2 (Hợp Thể), phải = cấp 3 (Titan). Bay lại gần để xem."));
        });
    }

    private static PhapTurret spawn(net.minecraft.server.level.ServerLevel lv, ServerPlayer sp, Vec3 p) {
        PhapTurret tu = com.hukhong.phaptu.PhapTuRegistry.TURRET.get().create(lv);
        tu.moveTo(p.x, p.y, p.z, 180, 0);
        tu.setup(sp, 200, 1, 40);
        lv.addFreshEntity(tu);
        tu.setYRot(180); tu.setYHeadRot(180); tu.yBodyRot = 180; tu.yBodyRotO = 180;
        return tu;
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        try (var img = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            java.io.File f = new java.io.File(DIR, name + ".png");
            f.getParentFile().mkdirs();
            img.writeToFile(f);
            LOG.info("THOTRUTEST screenshot {}", f);
        } catch (Exception ex) {
            LOG.error("THOTRUTEST screenshot failed", ex);
        }
    }
}
