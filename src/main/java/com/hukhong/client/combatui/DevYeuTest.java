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
        if (!movedAway) { // chạy ngầm: đẩy cửa sổ ra ngoài màn hình, không che máy người dùng
            movedAway = true;
            long w = mc.getWindow().getWindow();
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(w, -6000, -6000);
        }
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
        // 2026-11-06: kiểm tra hình ảnh Thiên Kiếp + Kỳ Ngộ
        final int y0 = 230, x0 = reg == null ? 0 : reg.cx(), z0 = reg == null ? 0 : reg.cz() + 300;
        if (t == 480) server.execute(() -> {
            for (int dx = -45; dx <= 45; dx++) for (int dz = -30; dz <= 45; dz++) {
                lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int dy = 0; dy < 30; dy++) lv.setBlock(new BlockPos(x0 + dx, y0 + dy, z0 + dz), Blocks.AIR.defaultBlockState(), 2);
            }
            sp.setGameMode(GameType.CREATIVE);
            sp.getAbilities().flying = false; sp.onUpdateAbilities();
            sp.connection.teleport(x0 + 0.5, y0, z0 + 0.5, 0, 0);
        });
        if (t == 520 && RUN_KIEP) server.execute(() -> com.hukhong.breakthrough.ThienKiep.begin(sp, 3));
        if (t == 545) shot("10_kiep_intro_a");
        if (t == 590) shot("11_kiep_intro_b");
        if (t == 616) shot("12_kiep_intro_end");
        if (t == 640) server.execute(() -> sp.connection.teleport(x0 + 0.5, y0 + 4, z0 - 6.5, 0, 35));
        if (t >= 660 && t <= 900 && t % 60 == 0) shot("13_kiep_fight_" + t);
        if (t >= 905 && t <= 1000 && t % 30 == 5) shot("14_kiep_demons_" + t);
        if (t >= 1200 && t <= 1290 && t % 15 == 0) shot("15_kiep_final_" + t);
        if (t == 1300) shot("16_kiep_after");
        if (t == 1310 && RUN_KYNGO) server.execute(() -> {
            com.hukhong.currency.Wallet.give(sp, 500);
            sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 4));
            var T = com.hukhong.story.kyngo.KyNgoType.values();
            for (int i = 0; i < T.length; i++) KYNGO[i] = com.hukhong.story.kyngo.KyNgoService.startAt(sp, T[i], new BlockPos(x0 - 24 + i * 16, y0, z0 + 30));
            sp.connection.teleport(x0 + 0.5, y0 + 6, z0 - 10.5, 0, 12);
        });
        if (t == 1360) shot("20_kyngo_columns");
        for (int i = 0; i < 4; i++) {
            final int k = i;
            int base = 1380 + i * 140;
            if (t == base && KYNGO[k] != null) server.execute(() -> {
                var o = KYNGO[k].origin;
                sp.connection.teleport(o.getX() + 0.5, y0, o.getZ() - 6.5, 0, 15);
            });
            if (t == base + 30) shot("2" + (k + 1) + "_reveal_" + k);
            if (t == base + 85 && KYNGO[k] != null) server.execute(() -> {
                var o = KYNGO[k].origin;
                sp.connection.teleport(o.getX() + 0.5, y0 + 1.5, o.getZ() - 5.5, 0, 18);
            });
            if (t == base + 95) shot("3" + (k + 1) + "_scene_" + k);
            if (t == base + 100 && KYNGO[k] != null) server.execute(() -> {
                var in = KYNGO[k];
                switch (in.type) {
                    case LAO_NHAN -> com.hukhong.story.kyngo.KyNgoService.resolve(sp, in.token, "help");
                    case DONG_PHU -> { for (var u : in.guards) { if (lv.getEntity(u) instanceof net.minecraft.world.entity.LivingEntity le) le.kill(); } }
                    case THI_THE -> com.hukhong.story.kyngo.KyNgoService.resolve(sp, in.token, "search");
                    case LINH_THU -> com.hukhong.story.kyngo.KyNgoService.resolve(sp, in.token, "free");
                }
            });
            if (t == base + 106) shot("4" + (k + 1) + "_outcome_" + k);
        }
        // 2026-11-06: Bảo Rương 4 cấp + cột sáng đồ rơi
        if (t == 1970 && false) server.execute(() -> {
            for (int i = 0; i < 4; i++) com.hukhong.treasure.TreasureFx.place(lv, new BlockPos(x0 - 6 + i * 4, y0, z0 + 8), i, net.minecraft.core.Direction.NORTH);
            net.minecraft.world.item.ItemStack[] drops = {new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND),
                    new net.minecraft.world.item.ItemStack(com.hukhong.resource.ResourceRegistry.LINH_DUOC.values().iterator().next().get()),
                    new net.minecraft.world.item.ItemStack(com.hukhong.currency.CurrencyRegistry.class.getName().isEmpty() ? net.minecraft.world.item.Items.STONE : net.minecraft.world.item.Items.NETHER_STAR),
                    new net.minecraft.world.item.ItemStack(com.hukhong.chg.ChgRegistry.CORES.get(0).get())};
            for (int i = 0; i < drops.length; i++) {
                var ie = new net.minecraft.world.entity.item.ItemEntity(lv, x0 - 6 + i * 4 + 0.5, y0 + 0.2, z0 + 3.5, drops[i], 0, 0, 0);
                ie.setNoPickUpDelay(); ie.setPickUpDelay(32767); ie.setUnlimitedLifetime();
                lv.addFreshEntity(ie);
            }
            sp.connection.teleport(x0 + 0.5, y0 + 2.2, z0 - 3.5, 0, 22);
        });
        if (t == 2005) server.execute(() -> sp.connection.teleport(x0 + 2.5, y0 + 1.4, z0 + 5.0, 0, 20));
        if (t == 2012) shot("50_treasure_closed");
        if (t == 2015) server.execute(() -> {
            for (int i = 0; i < 4; i++) {
                BlockPos cp = new BlockPos(x0 - 6 + i * 4, y0, z0 + 8);
                var st = lv.getBlockState(cp);
                lv.getBlockState(cp).useWithoutItem(lv, sp, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(cp), net.minecraft.core.Direction.UP, cp, false));
                sp.closeContainer();
                if (lv.getBlockEntity(cp) instanceof com.hukhong.treasure.BaoRuongBE be) be.startOpen(sp);
            }
        });
        if (t == 2022) shot("51_treasure_opening");
        if (t == 2030) for (int i = 0; i < 4; i++) { var cp = new BlockPos(x0 - 6 + i * 4, y0, z0 + 8); var cb = mc.level.getBlockEntity(cp); var sb = lv.getBlockEntity(cp); LOG.info("YEUTEST chest {} client={} open={} server={} state={}", i, cb, cb instanceof com.hukhong.treasure.BaoRuongBE b ? b.isOpen() : null, sb instanceof com.hukhong.treasure.BaoRuongBE b2 ? b2.isOpen() : null, mc.level.getBlockState(cp)); }
        if (t == 2045) shot("52_treasure_open");
        if (t == 2050) server.execute(() -> sp.connection.teleport(x0 - 1.5, y0 + 2.0, z0 + 3.0, 30, 25));
        if (t == 2058) shot("52b_treasure_side");
        if (t == 2060) server.execute(() -> sp.connection.teleport(x0 + 0.5, y0 + 1.6, z0 - 1.5, 0, 30));
        if (t == 2065) shot("53_loot_beams");
        if (t == 2075) server.execute(() -> { lv.setDayTime(18000); });
        if (t == 2100) shot("54_loot_beams_night");
        // 2026-11-06: công trình địa điểm làm lại
        if (t == 2110) server.execute(() -> {
            var data = com.hukhong.poi.PoiSavedData.get(lv);
            for (int i = 0; i < POIS.length; i++) {
                int px = x0 - 100 + (i % 4) * 66, pz = z0 + 140 + (i / 4) * 66;
                for (int dx = -26; dx <= 26; dx++) for (int dz = -26; dz <= 26; dz++) {
                    lv.setBlock(new BlockPos(px + dx, y0 - 1, pz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int dy = 0; dy < 24; dy++) lv.setBlock(new BlockPos(px + dx, y0 + dy, pz + dz), Blocks.AIR.defaultBlockState(), 2);
                }
                String want = POIS[i];
                var def = com.hukhong.poi.PoiDefs.all().stream().filter(d -> d.builder().equals(want)).findFirst().orElse(null);
                if (def == null) { LOG.info("YEUTEST poi missing {}", want); continue; }
                com.hukhong.poi.PoiPlacer.place(lv, data, def, px, pz);
            }
            lv.setDayTime(6000);
        });
        for (int i = 0; i < POIS.length; i++) {
            int base = 2160 + i * 50, px = x0 - 100 + (i % 4) * 66, pz = z0 + 140 + (i / 4) * 66;
            final int fi = i;
            if (t == base) server.execute(() -> sp.connection.teleport(px + 0.5, y0 + 20, pz + 30.5, 180, 32));
            if (t == base + 18) shot("6" + fi + "_poi_" + POIS[fi] + "_air");
            if (t == base + 20) server.execute(() -> sp.connection.teleport(px + 0.5, y0 + 2.5, pz + 15.5, 180, 4));
            if (t == base + 40) shot("6" + fi + "_poi_" + POIS[fi] + "_ground");
        }
        if (t == 2160 + POIS.length * 50 + 5) { LOG.info("YEUTEST done"); mc.stop(); }
    }

    private static final com.hukhong.story.kyngo.KyNgoInstance[] KYNGO = new com.hukhong.story.kyngo.KyNgoInstance[4];
    private static boolean movedAway;
    static final String[] POIS = {"battlefield", "grave", "sword_mound", "ruin", "shrine", "dao_quan", "wuxing", "abyss", "cave", "arena"};
    static final boolean RUN_KIEP = false, RUN_KYNGO = false;
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
