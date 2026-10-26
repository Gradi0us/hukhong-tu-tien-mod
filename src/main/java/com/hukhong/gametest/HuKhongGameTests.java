package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;
import com.hukhong.gates.SpawnerMarks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;

/** GameTest P0: luật Đạo tu đánh = 0, mob từ spawner không cho EXP. Chạy bằng gradle runGameTestServer. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class HuKhongGameTests {
    private HuKhongGameTests() {}

    private static ServerPlayer player(GameTestHelper h, Set<Path> paths) {
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "hukhong-test"));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(1, 0, 100, 100, paths, Map.of(), false));
        return p;
    }

    @GameTest(template = "empty")
    public static void daoDanhZombieBangKhong(GameTestHelper h) {
        ServerPlayer p = player(h, Set.of(Path.DAO));
        Zombie z = h.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        float before = z.getHealth();
        p.attack(z);
        h.runAfterDelay(2, () -> {
            if (z.getHealth() < before) h.fail("Dao tu danh zombie gay sat thuong: " + before + " -> " + z.getHealth());
            h.succeed();
        });
    }

    private static long killBy(GameTestHelper h, ServerPlayer p, boolean spawnerMarked) {
        Zombie z = h.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        if (spawnerMarked) z.setData(SpawnerMarks.MARK, true);
        long before = CultivationService.get(p).exp();
        z.hurt(h.getLevel().damageSources().playerAttack(p), 1000f);
        return CultivationService.get(p).exp() - before;
    }

    @GameTest(template = "empty")
    public static void spawnerMobKhongChoExp(GameTestHelper h) {
        ServerPlayer p = player(h, Set.of());
        long gained = killBy(h, p, true);
        if (gained != 0) h.fail("mob spawner cho EXP: " + gained);
        long control = killBy(h, p, false);
        if (control <= 0) h.fail("doi chung: mob thuong phai cho EXP, nhan " + control);
        h.succeed();
    }
}
