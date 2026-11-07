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
        if (!movedAway && !DEMO) { // chạy ngầm: đẩy cửa sổ ra ngoài màn hình, không che máy người dùng
            movedAway = true;
            long w = mc.getWindow().getWindow();
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(w, -6000, -6000);
            org.lwjgl.glfw.GLFW.glfwSetWindowAttrib(w, org.lwjgl.glfw.GLFW.GLFW_FOCUS_ON_SHOW, org.lwjgl.glfw.GLFW.GLFW_FALSE);
            org.lwjgl.glfw.GLFW.glfwSetWindowAttrib(w, org.lwjgl.glfw.GLFW.GLFW_FLOATING, org.lwjgl.glfw.GLFW.GLFW_FALSE);
        }
        // không bao giờ giữ chuột của người dùng
        if (!DEMO && mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();
        if (!DEMO) org.lwjgl.glfw.GLFW.glfwSetInputMode(mc.getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
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
        // 2026-11-06 (b): Dạ Khuyết, Ký Ức, Động Phủ, Linh Thú
        if (t == 560 && !RUN_POI) t = 2999;
        if (!RUN_POI && t >= 3000) { scenes2(server, sp, lv, x0, y0, z0); return; }
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
    static final boolean DEMO = System.getProperty("hukhong.yeudemo") != null;
    static final String[] POIS = {"battlefield", "grave", "sword_mound", "ruin", "shrine", "dao_quan", "wuxing", "abyss", "cave", "arena"};
    static final boolean RUN_KIEP = false, RUN_KYNGO = false, RUN_POI = false;

    static void cmd(net.minecraft.server.MinecraftServer server, ServerPlayer sp, String c) {
        server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c);
    }

    static double lastY;
    static void scenes2(net.minecraft.server.MinecraftServer server, ServerPlayer sp, net.minecraft.server.level.ServerLevel lv, int x0, int y0, int z0) {
        int u = t - 3000;
        if (DEMO) { demo(server, sp, lv, x0, y0, z0, u); return; }
        if (NPCS_ONLY) {
            String[] ids = {"mo_dao", "hua_co", "su_phu", "hang_chieu", "tong_chu_thanh_van", "hoang_toc_sot", "kiem_si", "lao_gia_bi_an", "hai_tuong", "dau_gia_su", "chu_cho_den", "thuong_nhan"};
            if (u == 0) server.execute(() -> {
                lv.setDayTime(6000);
                for (int dx = -30; dx <= 30; dx++) for (int dz = -30; dz <= 30; dz++) {
                    lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int dy = 0; dy < 20; dy++) lv.setBlock(new BlockPos(x0 + dx, y0 + dy, z0 + dz), Blocks.AIR.defaultBlockState(), 2);
                }
                sp.setGameMode(GameType.SPECTATOR);
                for (int i = 0; i < ids.length; i++) {
                    var n = com.hukhong.story.npc.StoryNpcRegistry.STORY_NPC.get().create(lv);
                    if (n == null) continue;
                    n.moveTo(x0 - 11 + (i % 6) * 4 + 0.5, y0, z0 + (i / 6) * 6 + 0.5, 180, 0);
                    n.setNpcId(ids[i]); n.setNoAi(true); n.setYBodyRot(180); n.setYHeadRot(180);
                    lv.addFreshEntity(n);
                }
                var dk = com.hukhong.endgame.EndgameRegistry.DA_KHUYET.get().create(lv);
                if (dk != null) { dk.markAvatar(); dk.moveTo(x0 + 16.5, y0, z0 + 3.5, 180, 0); dk.setNoAi(true); dk.setYBodyRot(180); dk.setYHeadRot(180); lv.addFreshEntity(dk); }
            });
            if (u == 40) server.execute(() -> sp.connection.teleport(x0 - 2.5, y0 + 2, z0 - 7.5, 0, 6));
            if (u == 60) shot("90_npcs_row");
            if (u == 70) server.execute(() -> sp.connection.teleport(x0 - 8.5, y0 + 1.6, z0 - 3.5, 0, 4));
            if (u == 90) shot("91_npcs_close_a");
            if (u == 100) server.execute(() -> sp.connection.teleport(x0 + 3.5, y0 + 1.6, z0 - 3.5, 0, 4));
            if (u == 120) shot("92_npcs_close_b");
            if (u == 130) server.execute(() -> sp.connection.teleport(x0 + 16.5, y0 + 1.7, z0 - 0.6, 0, -6));
            if (u == 150) shot("93_da_khuyet_front");
            if (u == 155) server.execute(() -> sp.connection.teleport(x0 + 13.6, y0 + 1.7, z0 + 0.4, -43, -6));
            if (u == 175) shot("96_da_khuyet_34");
            if (u == 180) server.execute(() -> sp.connection.teleport(x0 + 16.5, y0 + 1.8, z0 + 7.8, 180, -6));
            if (u == 188) shot("94_da_khuyet_back");
            if (u == 190) server.execute(() -> sp.connection.teleport(x0 - 2.5, y0 + 2, z0 + 14.5, 180, 6));
            if (u == 210) shot("95_npcs_back");
            if (u == 220) { LOG.info("YEUTEST done"); mc().stop(); }
            return;
        }
        if (PETS_ONLY) {
            if (u == 0) server.execute(() -> {
                lv.setDayTime(6000);
                for (int dx = -30; dx <= 30; dx++) for (int dz = -30; dz <= 30; dz++) {
                    lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int dy = 0; dy < 20; dy++) lv.setBlock(new BlockPos(x0 + dx, y0 + dy, z0 + dz), Blocks.AIR.defaultBlockState(), 2);
                }
                sp.setGameMode(GameType.SPECTATOR);
                var K = com.hukhong.creature.CreatureKind.class;
                com.hukhong.creature.CreatureKind[][] rows = {
                        {com.hukhong.creature.CreatureKind.THIET_BOI_HUNG_CON, com.hukhong.creature.CreatureKind.THIET_BOI_HUNG_CON, com.hukhong.creature.CreatureKind.THIET_BOI_HUNG},
                        {com.hukhong.creature.CreatureKind.LINH_HO, com.hukhong.creature.CreatureKind.LINH_HO, com.hukhong.creature.CreatureKind.LINH_HO},
                        {com.hukhong.creature.CreatureKind.NGOC_THO, com.hukhong.creature.CreatureKind.NGOC_THO, com.hukhong.creature.CreatureKind.NGOC_THO}};
                int[] grow = {0, 60, 100};
                for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
                    var e = com.hukhong.creature.CreatureRegistry.type(rows[r][c]).create(lv);
                    if (!(e instanceof com.hukhong.creature.LinhThu lt)) continue;
                    lt.moveTo(x0 - 9 + c * 9 + 0.5, y0, z0 + r * 14 + 0.5, 90, 0);
                    lt.tame(sp);
                    lt.setGrowth(grow[c]);
                    com.hukhong.creature.pet.PetGrowth.applyScale(lt);
                    lt.setOrderedToSit(true);
                    lt.setNoAi(true); lt.setYBodyRot(90); lt.setYHeadRot(90);
                    lv.addFreshEntity(lt);
                }
            });
            for (int r = 0; r < 3; r++) {
                int rr = r;
                if (u == 40 + r * 40) server.execute(() -> sp.connection.teleport(x0 + 0.5, y0 + 3.5, z0 + rr * 14 - 9.5, 0, 14));
                if (u == 60 + r * 40) shot("86_pets_row" + r + "_front");
            }
            for (int r = 0; r < 3; r++) {
                int rr = r;
                if (u == 160 + r * 40) server.execute(() -> sp.connection.teleport(x0 + 18.5, y0 + 4, z0 + rr * 14 + 0.5, 90, 12));
                if (u == 180 + r * 40) shot("87_pets_row" + r + "_side");
            }
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
                int rr = r, cc = c, at = 300 + (r * 3 + c) * 30;
                double sc = c == 2 ? 11 : c == 1 ? 6 : 3.5;
                if (u == at) server.execute(() -> sp.connection.teleport(x0 - 9 + cc * 9 + 0.5 - sc * 0.45, y0 + sc * 0.4, z0 + rr * 14 + 0.5 - sc * 0.9, -27, 20));
                if (u == at + 20) shot("88_pet_r" + r + "_g" + c);
            }
            if (u == 300 + 9 * 30 + 10) { LOG.info("YEUTEST done"); mc().stop(); }
            return;
        }
        if (RIFT_ONLY) {
            if (u == 0) server.execute(() -> { lv.setDayTime(18000); sp.setGameMode(GameType.CREATIVE); sp.getAbilities().flying = true; sp.onUpdateAbilities(); sp.connection.teleport(x0 + 0.5, y0, z0 + 0.5, 0, 0); });
            if (u == 40) server.execute(() -> com.hukhong.story.nemesis.NemesisService.start(sp, com.hukhong.story.flow.FlowSteps.get("c9_nemesis")));
            for (int k : new int[]{70, 130, 250, 450, 700}) if (u == k) shot("80_rift_" + k);
            if (u == 720) { LOG.info("YEUTEST done"); mc().stop(); }
            return;
        }
        if (Math.abs(sp.getY() - lastY) > 2) LOG.info("YEUTEST ydrop u={} {} -> {}", u, lastY, sp.position());
        lastY = sp.getY();
        if (u == 0) server.execute(() -> {
            for (int dx = -48; dx <= 48; dx++) for (int dz = -48; dz <= 48; dz++) {
                lv.setBlock(new BlockPos(x0 + dx, y0 - 2, z0 + dz), Blocks.STONE.defaultBlockState(), 2);
                lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int dy = 0; dy < 30; dy++) lv.setBlock(new BlockPos(x0 + dx, y0 + dy, z0 + dz), Blocks.AIR.defaultBlockState(), 2);
            }
            lv.setDayTime(18000);
            com.hukhong.core.CultivationService.setLevel(sp, 65);
            sp.setGameMode(GameType.CREATIVE);
            sp.getAbilities().flying = true; sp.onUpdateAbilities();
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 99999, 4, false, false));
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 99999, 4, false, false));
            sp.connection.teleport(x0 + 0.5, y0, z0 + 0.5, 0, 0);
        });
        if (u == 20) server.execute(() -> com.hukhong.story.nemesis.NemesisService.start(sp, com.hukhong.story.flow.FlowSteps.get("c1_nemesis")));
        for (int k : new int[]{50, 90, 130, 170, 210}) if (u == k) shot("70_nemesis_vision_" + k);
        if (u == 300) server.execute(() -> com.hukhong.story.nemesis.NemesisService.start(sp, com.hukhong.story.flow.FlowSteps.get("c4_nemesis")));
        for (int k : new int[]{320, 350, 390, 430}) if (u == k) shot("71_nemesis_taunt_" + k);
        if (u == 515) server.execute(() -> { sp.setGameMode(GameType.SURVIVAL); sp.connection.teleport(x0 + 0.5, y0, z0 + 0.5, 0, 0); });
        if (u == 850) server.execute(() -> { sp.setGameMode(GameType.CREATIVE); sp.getAbilities().flying = true; sp.onUpdateAbilities(); sp.connection.teleport(x0 + 0.5, y0, z0 + 0.5, 0, 0); });
        if (u == 520) server.execute(() -> com.hukhong.story.nemesis.NemesisService.start(sp, com.hukhong.story.flow.FlowSteps.get("c6_nemesis")));
        for (int k : new int[]{540, 580, 620, 680}) if (u == k) shot("72_nemesis_fight_" + k);
        if (u == 700) server.execute(() -> { var a = com.hukhong.story.nemesis.NemesisService.avatar(sp); if (a != null) { a.setHealth(a.getMaxHealth() * 0.3f); a.hurt(sp.damageSources().playerAttack(sp), 1f); } });
        for (int k : new int[]{705, 720, 750, 790}) if (u == k) shot("73_nemesis_retreat_" + k);
        if (u == 860) server.execute(() -> com.hukhong.story.nemesis.NemesisService.start(sp, com.hukhong.story.flow.FlowSteps.get("c9_nemesis")));
        for (int k : new int[]{890, 950, 1010, 1070}) if (u == k) shot("74_nemesis_reveal_" + k);
        if (u == 1200) server.execute(() -> cmd(server, sp, "hkkyuc play 4"));
        for (int k : new int[]{1230, 1290, 1350, 1430}) if (u == k) shot("75_memory4_" + k);
        if (u == 1550) server.execute(() -> cmd(server, sp, "hkkyuc play 6"));
        for (int k : new int[]{1580, 1640, 1700, 1780}) if (u == k) shot("76_memory6_" + k);
        if (u == 1900) server.execute(() -> {
            lv.setDayTime(13500);
            BlockPos bp = new BlockPos(x0 + 6, y0, z0 + 6);
            lv.setBlock(bp, com.hukhong.dongphu.DongPhuRegistry.TRAN_BAN.get().defaultBlockState(), 3);
            com.hukhong.dongphu.DongPhuService.claim(sp, lv, bp);
            sp.connection.teleport(x0 + 6.5, y0 + 1, z0 + 1.5, 0, 20);
        });
        for (int k : new int[]{1910, 1930, 1960}) if (u == k) shot("77_dongphu_claim_" + k);
        if (u == 2000) server.execute(() -> sp.connection.teleport(x0 + 6.5, y0 + 22, z0 - 14.5, 0, 45));
        if (u == 2020) shot("77_dongphu_air");
        if (u == 2040) server.execute(() -> sp.connection.teleport(x0 + 6.5, y0 + 1.2, z0 + 3.5, 0, 25));
        if (u == 2060) shot("77_dongphu_close");
        if (u == 2100) server.execute(() -> {
            lv.setDayTime(6000);
            var et = com.hukhong.creature.CreatureRegistry.type(com.hukhong.creature.CreatureKind.THIET_BOI_HUNG_CON);
            var e = et.create(lv);
            if (e instanceof com.hukhong.creature.LinhThu lt) {
                lt.moveTo(x0 - 3.5, y0, z0 + 7.5, 180, 0);
                lv.addFreshEntity(lt);
                lt.setHealth(1f);
                LOG.info("YEUTEST tame {}", com.hukhong.creature.pet.PetService.tryTame(sp, lt));
                lt.setHealth(lt.getMaxHealth());
            }
            sp.connection.teleport(x0 - 1.5, y0, z0 - 1.5, 0, 12);
            cmd(server, sp, "hkpet bond 80");
            cmd(server, sp, "give @s hukhong:huyet_yeu_tinh_huyet 4");
        });
        if (u == 2130) shot("78_pet_hud");
        if (u == 2140) server.execute(() -> cmd(server, sp, "hkpet evolve huyet_sat"));
        for (int k : new int[]{2150, 2170, 2200, 2240}) if (u == k) shot("79_pet_evo1_" + k);
        if (u == 2300) server.execute(() -> { var pt = com.hukhong.creature.pet.PetService.loaded(sp, "battle"); if (pt != null) pt.setHealth(pt.getMaxHealth() * 0.4f); });
        if (u == 2320) shot("79_pet_hud_dmg");
        if (u == 2340) server.execute(() -> cmd(server, sp, "hkpet evolve huyet_sat"));
        for (int k : new int[]{2350, 2380, 2420}) if (u == k) shot("79_pet_evo2_" + k);
        if (u == 2480 && !DEMO) { LOG.info("YEUTEST done"); mc().stop(); }
    }

    static com.hukhong.creature.LinhThu demoPet;

    /** Bản xem cho người dùng: cảnh Dạ Khuyết chương 1, rồi linh thú ấu thể lớn dần tới trưởng thành. */
    static void demo(net.minecraft.server.MinecraftServer server, ServerPlayer sp, net.minecraft.server.level.ServerLevel lv, int x0, int y0, int z0, int u) {
        if (u == 0) server.execute(() -> {
            for (int dx = -48; dx <= 48; dx++) for (int dz = -48; dz <= 48; dz++) {
                lv.setBlock(new BlockPos(x0 + dx, y0 - 2, z0 + dz), Blocks.STONE.defaultBlockState(), 2);
                lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int dy = 0; dy < 30; dy++) lv.setBlock(new BlockPos(x0 + dx, y0 + dy, z0 + dz), Blocks.AIR.defaultBlockState(), 2);
            }
            lv.setDayTime(18000);
            com.hukhong.core.CultivationService.setLevel(sp, 30);
            sp.setGameMode(GameType.CREATIVE);
            sp.getAbilities().flying = true; sp.onUpdateAbilities();
            sp.connection.teleport(x0 + 0.5, y0, z0 + 0.5, 0, 0);
        });
        // 4 cảnh Dạ Khuyết nối nhau (bắt đầu cảnh sau khi cảnh trước xong)
        String[] sc = {"c1_nemesis", "c4_nemesis", "c6_nemesis", "c9_nemesis"};
        if (u >= 60 && demoIdx < sc.length && u % 20 == 0) server.execute(() -> {
            boolean busy = com.hukhong.story.nemesis.NemesisService.busy(sp);
            if (busy) return;
            if (demoIdx == 2 && demoFightStarted && com.hukhong.story.nemesis.NemesisService.fighting(sp)) return;
            demoIdx++;
            if (demoIdx <= sc.length) {
                sp.connection.teleport(x0 + 0.5, y0, z0 + 0.5, 0, 0);
                com.hukhong.story.nemesis.NemesisService.start(sp, com.hukhong.story.flow.FlowSteps.get(sc[demoIdx - 1]));
                demoFightStarted = demoIdx == 3;
            }
        });
        // trận chương 6: đợi 10 s rồi hạ Ảnh xuống 35% để xem cảnh rút lui
        if (demoFightStarted && com.hukhong.story.nemesis.NemesisService.fighting(sp) && demoFightT++ == 200) server.execute(() -> {
            var a = com.hukhong.story.nemesis.NemesisService.avatar(sp);
            if (a != null) { a.setHealth(a.getMaxHealth() * 0.3f); a.hurt(sp.damageSources().playerAttack(sp), 1f); }
        });
        if (demoIdx == sc.length + 1 && demoPetAt < 0) demoPetAt = u + 40;
        if (demoPetAt > 0) pets(server, sp, lv, x0, y0, z0, u - demoPetAt);
    }

    static final boolean RIFT_ONLY = false, PETS_ONLY = false, NPCS_ONLY = false;
    static int demoIdx, demoFightT, demoPetAt = -1;
    static boolean demoFightStarted;
    static final java.util.List<com.hukhong.creature.LinhThu> DEMO_PETS = new java.util.ArrayList<>();

    /** Ba linh thú ấu thể đứng cạnh nhau, lớn dần qua thiếu niên tới trưởng thành. */
    static void pets(net.minecraft.server.MinecraftServer server, ServerPlayer sp, net.minecraft.server.level.ServerLevel lv, int x0, int y0, int z0, int v) {
        if (v == 0) server.execute(() -> {
            lv.setDayTime(6000);
            sp.setGameMode(GameType.CREATIVE);
            sp.getAbilities().flying = false; sp.onUpdateAbilities();
            sp.connection.teleport(x0 + 0.5, y0 + 1.5, z0 - 9.5, 0, 14);
            com.hukhong.creature.CreatureKind[] ks = {com.hukhong.creature.CreatureKind.NGOC_THO, com.hukhong.creature.CreatureKind.THIET_BOI_HUNG_CON, com.hukhong.creature.CreatureKind.LINH_HO};
            com.hukhong.creature.pet.PetData none = com.hukhong.creature.pet.PetData.EMPTY;
            for (int i = 0; i < ks.length; i++) {
                sp.setData(com.hukhong.creature.CreatureRegistry.PETS.get(), none); // demo: cho thuần cả 3
                var e = com.hukhong.creature.CreatureRegistry.type(ks[i]).create(lv);
                if (e instanceof com.hukhong.creature.LinhThu lt) {
                    lt.moveTo(x0 - 8 + i * 8 + 0.5, y0, z0 + 0.5, 180, 0);
                    lv.addFreshEntity(lt);
                    lt.setHealth(1f);
                    com.hukhong.creature.pet.PetService.tryTame(sp, lt);
                    lt.setHealth(lt.getMaxHealth());
                    lt.setOrderedToSit(true);
                    DEMO_PETS.add(lt);
                }
            }
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Demo] Ngọc Thố · Thiết Bối Hùng Con · Linh Hồ — ấu thể → thiếu niên → trưởng thành"));
        });
        for (int k = 1; k <= 10; k++) if (v == 40 + k * 60) server.execute(() -> {
            for (var lt : DEMO_PETS) if (lt.isAlive()) com.hukhong.creature.pet.PetGrowth.add(lt, 10);
            // gấu con đã đổi thành gấu lớn: lấy lại thực thể mới gần đó
            DEMO_PETS.removeIf(x -> !x.isAlive());
            for (var e : lv.getEntitiesOfClass(com.hukhong.creature.LinhThu.class, sp.getBoundingBox().inflate(40)))
                if (e.isTame() && !DEMO_PETS.contains(e)) { e.setOrderedToSit(true); DEMO_PETS.add(e); }
        });
        if (v == 760) server.execute(() -> sp.connection.teleport(x0 + 0.5, y0 + 4, z0 - 20.5, 0, 12));
    }

    static net.minecraft.client.Minecraft mc() { return net.minecraft.client.Minecraft.getInstance(); }
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
