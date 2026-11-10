package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.entity.HuVuong;
import com.hukhong.entity.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Hư Vương kiểu Rykard + Godrick: chiêu riêng (hvg_*) được director giao cho boss tự thi triển và đều kết thúc. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class HuVuongGameTests {
    private HuVuongGameTests() {}

    private static final String B = "huvuong";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-huvuong"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static HuVuong spawn(GameTestHelper h) {
        HuVuong d = ModEntities.HU_VUONG.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Hu Vuong"); return null; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest(template = "empty", timeoutTicks = 1400, batch = B + "_moves")
    public static void huVuongEveryPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 2, 6);
        HuVuong d = spawn(h);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("Hu Vuong phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("Hu Vuong bien mat giua chieu " + d.currentMove());
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            if (d.currentMove() != null) {
                if (wait.incrementAndGet() > 200) h.fail("chieu " + d.currentMove() + " khong ket thuc (t=" + d.moveTick() + ")");
                return;
            }
            int i = idx.incrementAndGet();
            if (i >= moves.size()) {
                boolean grav = d.isNoGravity();
                d.discard();
                forceArea(h, false);
                if (grav) h.fail("ket thuc chieu phai tra lai trong luc");
                h.succeed();
                return;
            }
            Move m = moves.get(i);
            if (!m.id().startsWith("hvg_")) h.fail("chieu chung lot vao bo Hu Vuong: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": Hu Vuong khong nhan chieu rieng");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B + "_summon")
    public static void huVuongAiOnlyMovesFinish(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 2, 6);
        HuVuong d = spawn(h);
        h.runAfterDelay(2, () -> { if (!d.startMove(h.getLevel(), "hvg_trieu_hoi", p)) h.fail("khong thi trien duoc Trieu Hon"); });
        h.runAfterDelay(60, () -> {
            String mv = d.currentMove();
            d.discard();
            h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, d.getBoundingBox().inflate(16)).forEach(net.minecraft.world.entity.Entity::discard);
            forceArea(h, false);
            if (mv != null) h.fail("Trieu Hon phai ket thuc");
            h.succeed();
        });
    }
}
