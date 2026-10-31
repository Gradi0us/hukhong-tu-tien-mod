package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/** DEV ONLY (chi chay khi -Dhukhong.autotest=1, tuc ./gradlew runClient -Pautotest=<world>): tu danh quai + hoi mau + chup anh de kiem chung HUD. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevAutoTest {
    private DevAutoTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.autotest") != null;
    private static int t;
    private static Mob dummy;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (mc.isPaused()) return;
        t++;
        var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) return;
        if (t == 200) server.execute(() -> {
            sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            Vec3 look = sp.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 at = sp.position().add(look.scale(4));
            dummy = EntityType.ZOMBIE.create(sp.serverLevel());
            dummy.moveTo(at.x, sp.getY(), at.z);
            dummy.setNoAi(true);
            dummy.setPersistenceRequired();
            dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
            dummy.setHealth(200);
            sp.serverLevel().addFreshEntity(dummy);
            sp.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, dummy.position().add(0, 1, 0));
            LOG.info("AUTOTEST dummy spawned at {}", at);
        });
        if (t == 236 || t == 246 || t == 276) server.execute(() -> {
            if (dummy == null) return;
            sp.setNoGravity(true); sp.setDeltaMovement(Vec3.ZERO); sp.hurtMarked = true;
            Vec3 look = sp.getLookAngle();
            Vec3 at = sp.getEyePosition().add(look.scale(4)).add(0, -1.6, 0);
            dummy.setNoGravity(true); dummy.setDeltaMovement(Vec3.ZERO);
            dummy.moveTo(at.x, at.y, at.z);
            sp.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, dummy.position().add(0, 1, 0));
        });
        if (t == 240 || t == 250) server.execute(() -> {
            if (dummy != null) dummy.hurt(sp.damageSources().playerAttack(sp), 17f);
            LOG.info("AUTOTEST hit sent");
        });
        if (t == 244 || t == 254) shot("hit" + t);
        if (t == 280) server.execute(() -> { sp.setHealth(3f); sp.heal(8f); LOG.info("AUTOTEST heal sent"); });
        if (t == 284) shot("heal");
        if (t == 290 || t == 294 || t == 298 || t == 330) server.execute(() -> { // Thể Tu tay không vung 3 cái liền + 1 cái sau cửa sổ
            sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            sp.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        });
        if (t == 292) bcCheck();
        String kiem = System.getenv("HUKHONG_KIEM"); // dev: HUKHONG_KIEM=1 -> Kiếm Tu cầm Bản Mệnh Kiếm, 8 đòn đánh thường lên bù nhìn (4 ngôi thứ ba + 4 ngôi thứ nhất), chụp ảnh giữa nhát chém
        if (kiem != null && !kiem.isEmpty()) {
            kiemHarness(mc, server, sp);
            return;
        }
        String skills = System.getenv("HUKHONG_SKILLS"); // dev: HUKHONG_SKILLS=QUANG|LOI|HOA|THUY|THO|KIM -> thi triển Q,E,R,T lần lượt lên bù nhìn và chụp ảnh từng chiêu
        if (skills != null && !skills.isEmpty()) {
            skillsHarness(mc, server, sp, skills);
            return;
        }
        String beam = System.getenv("HUKHONG_BEAM"); // dev: HUKHONG_BEAM=QUANG|LOI|HOA|THUY -> nạp, bắn Tinh Hà Quang Trụ và chụp ảnh
        if (beam != null && !beam.isEmpty()) {
            if (t == 400) server.execute(() -> {
                var d = sp.getData(com.hukhong.core.ModAttachments.CULTIVATION);
                sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, new com.hukhong.core.CultivationData(60, 0, 1000, 1000,
                        java.util.Set.of(com.hukhong.core.Path.PHAP), java.util.Map.of(com.hukhong.core.Path.PHAP, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
                com.hukhong.phaptu.PhapTu.setElement(sp, com.hukhong.phaptu.PhapElement.valueOf(beam));
                com.hukhong.stats.StatService.refresh(sp);
                sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, sp.getData(com.hukhong.core.ModAttachments.CULTIVATION).withResource(sp.getData(com.hukhong.core.ModAttachments.CULTIVATION).resourceMax()));
                com.hukhong.skill.SkillService.state(sp).stance = true;
                com.hukhong.skill.SkillService.state(sp).cooldownEnd[3] = 0;
                LOG.info("AUTOTEST beam chargeStart={}", com.hukhong.phaptu.beam.PhapBeamService.chargeStart(sp));
            });
            if (t == 415) shot("beam_charge_" + beam);
            if (t == 440) shot("beam_ready_" + beam);
            if (t == 445) server.execute(() -> LOG.info("AUTOTEST beam fireStart={}", com.hukhong.phaptu.beam.PhapBeamService.fireStart(sp)));
            if (t == 455 || t == 480) shot("beam_fire_" + beam + t);
            if (t == 540) { LOG.info("AUTOTEST done"); mc.stop(); }
            return;
        }
        if (t == 340) { LOG.info("AUTOTEST done"); mc.stop(); }
    }

    /** Kiếm Tu + Bản Mệnh Kiếm: 8 đòn đánh thường, mỗi đòn chụp ở +3/+5/+7/+10 tick sau khi gửi yêu cầu. Ảnh -> HUKHONG_SHOTDIR. */
    private static void kiemHarness(Minecraft mc, net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerPlayer sp) {
        final int start = 400, gap = 34, n = 8;
        if (t == start) server.execute(() -> {
            var d = sp.getData(com.hukhong.core.ModAttachments.CULTIVATION);
            sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, new com.hukhong.core.CultivationData(60, 0, 1000, 1000,
                    java.util.Set.of(com.hukhong.core.Path.KIEM), java.util.Map.of(com.hukhong.core.Path.KIEM, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
            com.hukhong.stats.StatService.refresh(sp);
            com.hukhong.banmenh.BanMenhService.grant(sp);
            var bm = sp.getData(com.hukhong.banmenh.BanMenhAttachments.BAN_MENH.get());
            sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, com.hukhong.banmenh.BanMenhService.create(bm.withStage(4)));
            var lv = sp.serverLevel(); // sân phẳng trên cao, ban ngày: không vướng công trình / bóng đổ
            int cx = (int) sp.getX(), cz = (int) sp.getZ(), cy = 230;
            for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++)
                lv.setBlockAndUpdate(new net.minecraft.core.BlockPos(cx + x, cy, cz + z), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
            lv.setDayTime(13200); // chạng vạng: thấy rõ hiệu ứng cộng sáng
            sp.teleportTo(cx + 0.5, cy + 1, cz + 0.5);
            sp.setYRot(0); sp.setXRot(8);
            sp.setInvulnerable(true);
            LOG.info("AUTOTEST kiem hand={} stage={}", sp.getMainHandItem(), com.hukhong.banmenh.BanMenhKiemItem.stageOf(sp.getMainHandItem()));
        });
        if (t == start) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        if (t == start + 34 + n / 2 * gap - 10) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        String dir = System.getenv("HUKHONG_SHOTDIR");
        for (int k = 0; k < n; k++) {
            final int t0 = start + 40 + k * gap;
            if (t == t0 - 3) server.execute(() -> { // bù nhìn mới (bất tử) ngay trước mặt trên sân phẳng, người nhìn ngang
                {
                    if (dummy != null) dummy.discard();
                    dummy = EntityType.ZOMBIE.create(sp.serverLevel());
                    dummy.setNoAi(true); dummy.setPersistenceRequired(); dummy.setInvulnerable(true);
                    sp.serverLevel().addFreshEntity(dummy);
                }
                sp.setNoGravity(true); sp.setDeltaMovement(Vec3.ZERO);
                sp.teleportTo(sp.serverLevel(), -34.5, 231, -321.5, java.util.Set.of(), 0f, 8f);
                dummy.clearFire(); dummy.setDeltaMovement(Vec3.ZERO); dummy.setNoGravity(true);
                dummy.moveTo(-34.5, 231, -321.5 + 4.5, 180f, 0f);
            });
            if (t == t0) server.execute(() -> {
                if (dummy == null) return;
                com.hukhong.banmenh.BanMenhStrike.request(sp, dummy.getId());
                LOG.info("AUTOTEST kiem strike id={} alive={} dummy={} player={} rot={},{} hand={} type={} owner={}", dummy.getId(), dummy.isAlive(), dummy.position(), sp.getEyePosition(), sp.getYRot(), sp.getXRot(), sp.getMainHandItem().getItem(), com.hukhong.banmenh.BanMenhKiemItem.typeOf(sp.getMainHandItem()), com.hukhong.banmenh.BanMenhKiemItem.isOwner(sp.getMainHandItem(), sp));
            });
            int[] offs = {6, 8, 10, 12, 14, 17};
            for (int o : offs) if (t == t0 + o) shotTo(dir, "kiem_" + (k < n / 2 ? "3p" : "1p") + "_atk" + (k % 4) + "_t" + o);
        }
        if (t == start + 40 + n * gap + 20) { LOG.info("AUTOTEST done"); mc.stop(); }
    }

    private static void shotTo(String dir, String name) {
        Minecraft mc = Minecraft.getInstance();
        try (var img = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            java.io.File f = new java.io.File(dir == null || dir.isEmpty() ? "D:/mc_mod/docs/vfx-review/kiem" : dir, name + ".png");
            f.getParentFile().mkdirs();
            img.writeToFile(f);
            LOG.info("AUTOTEST screenshot {}", f);
        } catch (Exception ex) {
            LOG.error("AUTOTEST screenshot failed", ex);
        }
    }

    /** Mỗi phím cách 70 tick: cast ở t0, chụp ảnh ở t0+3 / +8 / +16. Phím T của Quang/Lôi/Hỏa/Thủy là tia kênh (dùng HUKHONG_BEAM). */
    private static void skillsHarness(Minecraft mc, net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerPlayer sp, String el) {
        final int start = 400, gap = 70;
        if (t == start) server.execute(() -> {
            var d = sp.getData(com.hukhong.core.ModAttachments.CULTIVATION);
            sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, new com.hukhong.core.CultivationData(60, 0, 1000, 1000,
                    java.util.Set.of(com.hukhong.core.Path.PHAP), java.util.Map.of(com.hukhong.core.Path.PHAP, com.hukhong.core.Aptitude.SSS), false, d.origin(), d.roll(), d.milestoneUnlocked()));
            com.hukhong.phaptu.PhapTu.setElement(sp, com.hukhong.phaptu.PhapElement.valueOf(el));
            com.hukhong.stats.StatService.refresh(sp);
            sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, sp.getData(com.hukhong.core.ModAttachments.CULTIVATION).withResource(sp.getData(com.hukhong.core.ModAttachments.CULTIVATION).resourceMax()));
            sp.setInvulnerable(true); // dev: không chết giữa chừng khi chụp
            com.hukhong.skill.SkillService.state(sp).stance = true;
            com.hukhong.skill.SkillService.state(sp).page = 0;
            LOG.info("AUTOTEST skills element={} beamT={}", el, com.hukhong.phaptu.beam.PhapBeamService.locksT(sp));
        });
        for (int slot = 0; slot < 4; slot++) {
            final int sl = slot, t0 = start + 40 + slot * gap;
            if (t == t0 - 2) server.execute(() -> { // dựng lại bù nhìn + hồi đầy, đặt người nhìn thẳng vào nó
                if (dummy != null) { dummy.setHealth(200); dummy.setDeltaMovement(Vec3.ZERO); }
                Vec3 look = sp.getLookAngle();
                Vec3 at = sp.getEyePosition().add(look.scale(5)).add(0, -1.6, 0);
                if (dummy != null) dummy.moveTo(at.x, at.y, at.z);
                var cd = sp.getData(com.hukhong.core.ModAttachments.CULTIVATION);
                sp.setData(com.hukhong.core.ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
                java.util.Arrays.fill(com.hukhong.skill.SkillService.state(sp).cooldownEnd, 0L);
            });
            if (t == t0) server.execute(() -> {
                if (sl == 3 && com.hukhong.phaptu.beam.PhapBeamService.locksT(sp)) { LOG.info("AUTOTEST skip T (beam) for {}", el); return; }
                com.hukhong.skill.SkillService.cast(sp, sl);
                LOG.info("AUTOTEST cast slot {} cd={}", sl, com.hukhong.skill.SkillService.state(sp).cooldownEnd[sl]);
            });
            if (t == t0 + 3 || t == t0 + 8 || t == t0 + 16 || (sl == 3 && (t == t0 + 12 || t == t0 + 18 || t == t0 + 24))) shot("skill_" + el + "_" + sl + "_" + (t - t0));
        }
        if (t == start + 40 + 4 * gap + 10) {
            server.execute(() -> LOG.info("AUTOTEST turrets alive near player = {}", sp.serverLevel().getEntitiesOfClass(com.hukhong.phaptu.PhapTurret.class, sp.getBoundingBox().inflate(40)).size()));
        }
        if (t == start + 40 + 4 * gap + 30) { LOG.info("AUTOTEST done"); mc.stop(); }
    }

    private static void bcCheck() {
        if (!net.neoforged.fml.ModList.get().isLoaded("bettercombat")) { LOG.info("AUTOTEST BC not loaded"); return; }
        LOG.info("AUTOTEST BC attributes for ban_menh_kiem = {}", BcProbe.attrs());
    }

    private static final class BcProbe {
        static String attrs() {
            var st = new net.minecraft.world.item.ItemStack(com.hukhong.item.ModItems.BAN_MENH_KIEM.get());
            var a = net.bettercombat.logic.WeaponRegistry.getAttributes(st);
            return a == null ? "NULL" : "attacks=" + a.attacks().length + " category=" + a.category();
        }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), c -> LOG.info("AUTOTEST screenshot {}: {}", name, c.getString()));
    }
}
