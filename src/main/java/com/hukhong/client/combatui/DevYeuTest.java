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
            var kinds = CreatureKind.BEASTS;
            for (int i = 0; i < kinds.size(); i++) {
                LinhQuai a = DaiSamLam.spawnBeast(lv, kinds.get(i), new BlockPos(x0 - 32 + i * 8, y0, z0 + 6), YeuThuAge.Y1000, MobSpawnType.COMMAND);
                a.setNoAi(true); a.setYRot(180); a.setYHeadRot(180); a.yBodyRot = 180;
                LOG.info("YEUTEST {} scale={} bb={}", a.getDisplayName().getString(), a.getScale(), a.getBbWidth());
            }
            sp.connection.teleport(x0 + 0.5, y0 + 9, z0 - 22.5, 0, 18);
        });
        if (t == 720) shot("03_nine_beasts");
        if (t == 760) server.execute(() -> {
            int y0 = 230, x0 = reg.cx(), z0 = reg.cz() + 320;
            for (var old : lv.getEntitiesOfClass(LinhQuai.class, new net.minecraft.world.phys.AABB(x0 - 60, y0 - 5, z0 - 40, x0 + 60, y0 + 20, z0 + 40))) old.discard();
            CreatureKind[] demo = {CreatureKind.XICH_VAN_HO, CreatureKind.BAO_VIEM_TICH, CreatureKind.THIET_GIAP_TRI_CHU, CreatureKind.BICH_NHAN_THIEM};
            for (int i = 0; i < demo.length; i++) {
                int x = x0 - 24 + i * 16;
                LinhQuai m = DaiSamLam.spawnBeast(lv, demo[i], new BlockPos(x, y0, z0), YeuThuAge.Y10000, MobSpawnType.COMMAND);
                var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(lv);
                z.moveTo(x + 0.5, y0, z0 + 7.5, 180, 0);
                z.setNoAi(true); z.setPersistenceRequired(); z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000); z.setHealth(1000);
                lv.addFreshEntity(z);
                m.setTarget(z);
                com.hukhong.creature.YeuThuSkills.start(lv, m, z, YeuThuAge.Y10000);
            }
            sp.connection.teleport(x0 + 0.5, y0 + 10, z0 - 16.5, 0, 25);
        });
        if (t == 790) shot("04_skill_windup");
        if (t == 812) shot("05_skill_cast");
        if (t == 830) shot("06_skill_after");
        if (t == 860) { LOG.info("YEUTEST done"); mc.stop(); }
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
