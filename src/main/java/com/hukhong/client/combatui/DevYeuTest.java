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
            for (int dx = -24; dx <= 24; dx++) for (int dz = -6; dz <= 18; dz++)
                lv.setBlock(new BlockPos(x0 + dx, y0 - 1, z0 + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            YeuThuAge[] ages = YeuThuAge.values();
            for (int i = 0; i < 5; i++) {
                LinhQuai a = DaiSamLam.spawnBeast(lv, CreatureKind.BEASTS.get(i), new BlockPos(x0 - 20 + i * 10, y0, z0 + 8), YeuThuAge.Y1000, MobSpawnType.COMMAND);
                LinhQuai b = DaiSamLam.spawnBeast(lv, CreatureKind.XICH_VAN_HO, new BlockPos(x0 - 20 + i * 10, y0, z0), ages[i], MobSpawnType.COMMAND);
                for (LinhQuai m : new LinhQuai[]{a, b}) { m.setNoAi(true); m.setYRot(180); m.setYHeadRot(180); }
                LOG.info("YEUTEST {} hp={} tough={} scale={} | {} hp={}", b.getDisplayName().getString(), b.getMaxHealth(), b.toughness(), b.getScale(),
                        a.getDisplayName().getString(), a.getMaxHealth());
            }
            sp.connection.teleport(x0 + 0.5, y0 + 7, z0 - 18.5, 0, 20);
        });
        if (t == 700) shot("03_ages_and_kinds");
        if (t == 705) server.execute(() -> sp.connection.teleport(reg.cx() + 0.5, 233, reg.cz() + 297.5, 0, 30));
        if (t == 760) shot("04_ages_close");
        if (t == 780) { LOG.info("YEUTEST done"); mc.stop(); }
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
