package com.hukhong.siege;

import com.hukhong.HuKhong;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.city.ambient.AmbientRegistry;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Man thanh bi vay: linh khoa muc tieu Hu Hoa that, bat tu voi nguoi choi nhung khong voi quai; dan chay vao nha va hien lai. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class SiegeGameTests {
    private SiegeGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 300, batch = "siegeA")
    public static void guardsTargetVoidMobWhenSiegeStarts(GameTestHelper h) {
        SiegeDirector.abort();
        AmbientNpc guard = h.spawn(AmbientRegistry.AMBIENT.get(), 2, 2, 2);
        guard.setup(AmbientNpc.GUARD, 0, guard.blockPosition(), 40);
        guard.setPersistenceRequired();
        Mob hu = h.spawn(ModEntities.HU_THI.get(), 7, 2, 2);
        hu.setPersistenceRequired();
        hu.setNoAi(true); // dung yen: chi kiem tra linh chu dong khoa dich
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-siege"));
        SiegeDirector.begin(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)), 8, false, false);
        h.succeedWhen(() -> {
            h.assertTrue(guard.siegeMode() == AmbientNpc.SIEGE_FIGHT, "guard did not enter fight mode");
            h.assertTrue(guard.getTarget() instanceof HuMob, "guard target must be a Hu Hoa mob, got " + guard.getTarget());
            h.assertTrue(guard.isInvulnerableTo(h.getLevel().damageSources().playerAttack(p)), "guard must stay invulnerable to players");
            h.assertFalse(guard.isInvulnerableTo(h.getLevel().damageSources().mobAttack(hu)), "guard must be hurtable by Hu Hoa mobs");
            SiegeDirector.abort();
            h.assertTrue(guard.siegeMode() == AmbientNpc.SIEGE_NONE, "abort must restore the guard");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "siegeB")
    public static void citizensShelterThenReturn(GameTestHelper h) {
        SiegeDirector.abort();
        AmbientNpc c = h.spawn(AmbientRegistry.AMBIENT.get(), 2, 2, 2);
        c.setup(AmbientNpc.CITIZEN, 1, c.blockPosition(), 40);
        c.setPersistenceRequired();
        SiegeDirector.begin(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)), 8, false, false);
        h.runAfterDelay(260, () -> {
            h.assertTrue(c.hiddenInside() && c.isInvisible(), "citizen must be sheltered: mode=" + c.siegeMode() + " hidden=" + c.hiddenInside() + " invis=" + c.isInvisible() + " alive=" + c.isAlive() + " removed=" + c.isRemoved() + " running=" + SiegeDirector.running());
            SiegeDirector.abort();
            h.runAfterDelay(5, () -> {
                h.assertTrue(!c.hiddenInside() && !c.isInvisible() && c.siegeMode() == AmbientNpc.SIEGE_NONE, "citizen must return visible after the siege");
                h.succeed();
            });
        });
    }
}
