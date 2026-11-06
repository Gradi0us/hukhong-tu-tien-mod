package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.LinhQuai;
import com.hukhong.creature.YeuThuAge;
import com.hukhong.world.yeulam.DaiSamLam;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * DEV ONLY (./gradlew runClient -Pyeutest): tạo thế giới THƯỜNG mới (seed 12345, không đụng save đang chơi), bay tới Đại Sâm Lâm
 * gần nhất, chụp rừng cổ thụ sinh tự nhiên; dựng sân đá trên trời, thả 5 Yêu Thú ở 5 mức năm tuổi; chụp vào docs/vfx-review/yeu.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevYeuTest {
    private DevYeuTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.yeutest") != null;
    private static final String DIR = "D:/mc_mod/docs/vfx-review/yeu";
    private static boolean created;
    private static int t;
    private static DaiSamLam.Region reg;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created && mc.level == null && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
            created = true;
            String name = "hk-yeu-test-" + System.currentTimeMillis();
            LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(), mc.screen);
            LOG.info("YEUTEST creating fresh world {}", name);
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
        if (t == 40) server.execute(() -> {
            sp.setGameMode(GameType.SPECTATOR);
            sp.getAbilities().flying = true; sp.onUpdateAbilities();
            lv.setDayTime(5000);
            lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            lv.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            reg = DaiSamLam.nearest(lv.getSeed(), 0, 0);
            LOG.info("YEUTEST nearest forest {}", reg);
            sp.connection.teleport(reg.cx() + 0.5, 190, reg.cz() + 110.5, 180, 30);
        });
        if (t == 500) shot("01_forest_air");
        if (t == 505) server.execute(() -> {
            int logs = 0;
            BlockPos c = new BlockPos(reg.cx(), 0, reg.cz());
            for (int dx = -40; dx <= 40; dx += 2) for (int dz = -40; dz <= 40; dz += 2) {
                int y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX() + dx, c.getZ() + dz);
                if (lv.getBlockState(new BlockPos(c.getX() + dx, y - 1, c.getZ() + dz)).is(BlockTags.LOGS)) logs++;
            }
            LOG.info("YEUTEST log columns near center = {} / 1681, biome={}", logs, lv.getBiome(c.atY(80)).getRegisteredName());
            int y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, reg.cx() + 12, reg.cz() + 12);
            sp.connection.teleport(reg.cx() + 12.5, y + 2, reg.cz() + 12.5, 135, -15);
        });
        if (t == 620) shot("02_forest_ground");
        if (t == 625) server.execute(() -> {
            int y0 = 230, x0 = reg.cx(), z0 = reg.cz() + 300;
            for (int dx = -40; dx <= 40; dx++) for (int dz = -10; dz <= 40; dz++)
                lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            // 2026-11-06: kích thước theo năm - thú <= 7 khối, Yêu Thụ 16-20 khối
            // 2026-11-06: bản thường / bản Hư Không xâm thực cạnh nhau
            Object[][] show = {{CreatureKind.THU_YEU, YeuThuAge.Y1000, false}, {CreatureKind.THU_YEU, YeuThuAge.Y1000, true},
                    {CreatureKind.THUC_NHAN_HOA, YeuThuAge.Y1000, true}, {CreatureKind.VAN_NIEN_THU_VUONG, YeuThuAge.Y1000, true}, {CreatureKind.QUY_CO, YeuThuAge.Y1000, true}};
            int[] xs = {-30, -18, -6, 10, 28};
            for (int i = 0; i < show.length; i++) {
                LinhQuai a = DaiSamLam.spawnBeast(lv, (CreatureKind) show[i][0], new BlockPos(x0 + xs[i], y0, z0 + 10), (YeuThuAge) show[i][1], MobSpawnType.COMMAND);
                a.setNoAi(true); a.setYRot(180); a.setYHeadRot(180); a.yBodyRot = 180;
                a.setCorrupted((Boolean) show[i][2]);
                LOG.info("YEUTEST {} scale={} height={}", a.getDisplayName().getString(), a.getScale(), a.getBbHeight());
            }
            sp.connection.teleport(x0 + 0.5, y0 + 9, z0 - 24.5, 0, 14);
        });
        if (t == 748) server.execute(() -> {
            for (var q : lv.getEntitiesOfClass(LinhQuai.class, sp.getBoundingBox().inflate(80)))
                LOG.info("YEUTEST at-shot {} pos={} alive={} hu={}", q.getType().toShortString(), q.blockPosition(), q.isAlive(), q.corrupted());
            for (var q : mc.level.entitiesForRendering()) if (q instanceof LinhQuai lq) LOG.info("YEUTEST client {} pos={} hu={}", lq.getType().toShortString(), lq.blockPosition(), lq.corrupted());
        });
        if (t == 750) shot("03_giant_sizes");
        // 2026-11-06: test cây đổ - Thụ Yêu 10000 năm đi xuyên hàng sồi, boss Thụ Vương giữa cụm cây
        if (t == 760) server.execute(() -> {
            int y0 = 230, x0 = reg.cx(), z0 = reg.cz() + 400;
            for (int dx = -40; dx <= 40; dx++) for (int dz = -10; dz <= 50; dz++)
                lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            var oak = lv.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                    .getHolderOrThrow(net.minecraft.data.worldgen.features.TreeFeatures.OAK).value();
            int planted = 0;
            for (int i = 0; i < 6; i++) if (oak.place(lv, lv.getChunkSource().getGenerator(), lv.random, new BlockPos(x0 - 12 + (i % 2) * 3, y0, z0 + 8 + i * 4))) planted++;
            for (int i = 0; i < 10; i++) {
                double an = i * Math.PI * 2 / 10;
                if (oak.place(lv, lv.getChunkSource().getGenerator(), lv.random, new BlockPos(x0 + 22 + (int) (Math.cos(an) * 7), y0, z0 + 25 + (int) (Math.sin(an) * 7)))) planted++;
            }
            LOG.info("YEUTEST fell planted={} logs={}", planted, countLogs(lv, x0, y0, z0));
            sp.connection.teleport(x0 + 12.5, y0 + 13, z0 - 10.5, -15, 22);
        });
        if (t == 800) shot("04_trees_before");
        if (t == 900) server.execute(() -> {
            int y0 = 230, x0 = reg.cx(), z0 = reg.cz() + 400;
            LinhQuai w = DaiSamLam.spawnBeast(lv, CreatureKind.THU_YEU, new BlockPos(x0 - 11, y0, z0 - 2), YeuThuAge.Y10000, MobSpawnType.COMMAND);
            var z = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(lv);
            z.moveTo(x0 - 11.5, y0, z0 + 40.5); lv.addFreshEntity(z);
            w.getNavigation().moveTo(x0 - 11.5, y0, z0 + 40.5, 1.4);
            fellWalker = w;
            LinhQuai boss = DaiSamLam.spawnBeast(lv, CreatureKind.VAN_NIEN_THU_VUONG, new BlockPos(x0 + 22, y0, z0 + 25), YeuThuAge.Y1000, MobSpawnType.COMMAND);
            boss.setNoAi(true);
            spawnedAt = System.currentTimeMillis();
        });
        if (spawnedAt > 0 && shotIdx < FALL_SHOTS.length && System.currentTimeMillis() - spawnedAt >= FALL_SHOTS[shotIdx]) shot("05_fall_" + FALL_SHOTS[shotIdx++] + "ms");
        if (t > 900 && t < 1100 && t % 10 == 0 && fellWalker != null) server.execute(() -> {
            int y0 = 230, x0 = reg.cx(), z0 = reg.cz() + 400;
            if (fellWalker.getNavigation().isDone()) fellWalker.getNavigation().moveTo(x0 - 11.5, y0, z0 + 40.5, 1.4);
        });
        if (t == 1100) server.execute(() -> {
            int y0 = 230, x0 = reg.cx(), z0 = reg.cz() + 400;
            LOG.info("YEUTEST fell after logs={} walkerZ={}", countLogs(lv, x0, y0, z0), fellWalker == null ? -1 : fellWalker.getZ() - z0);
        });
        if (t == 1105) shot("06_trees_after");
        if (t == 1130) { LOG.info("YEUTEST done"); mc.stop(); }
    }

    private static LinhQuai fellWalker;
    private static volatile long spawnedAt;
    private static int shotIdx;
    private static final int[] FALL_SHOTS = {200, 500, 800, 1100, 1500, 2200};

    private static int countLogs(net.minecraft.server.level.ServerLevel lv, int x0, int y0, int z0) {
        int n = 0;
        for (int x = x0 - 40; x <= x0 + 40; x++) for (int z = z0 - 10; z <= z0 + 50; z++) for (int y = y0; y <= y0 + 10; y++)
            if (lv.getBlockState(new BlockPos(x, y, z)).is(net.minecraft.tags.BlockTags.LOGS)) n++;
        return n;
    }


    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        try (var img = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            java.io.File f = new java.io.File(DIR, name + ".png");
            f.getParentFile().mkdirs();
            img.writeToFile(f);
            LOG.info("YEUTEST screenshot {}", f);
        } catch (Exception ex) {
            LOG.error("YEUTEST screenshot failed", ex);
        }
    }
}
