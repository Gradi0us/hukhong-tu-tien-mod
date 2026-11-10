package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.chg.ChapHanhGia;
import com.hukhong.chg.ChgOneMoves;
import com.hukhong.chg.ChgRegistry;
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

/** CHG I (Thạch Hống, kiểu Godfrey/Hoarah Loux): director giao chiêu phase cho boss tự thi triển; AI pha 1 dùng chiêu riêng thay ChgKit. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ChgOneGameTests {
    private ChgOneGameTests() {}

    private static final String B = "chg1";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-chg1"));
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

    private static ChapHanhGia spawn(GameTestHelper h) {
        ChapHanhGia d = ChgRegistry.type(1).create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc CHG I"); return null; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest(template = "empty", timeoutTicks = 1200, batch = B + "_moves")
    public static void everyPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 2, 6);
        ChapHanhGia d = spawn(h);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("CHG I phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("CHG I bien mat giua chieu " + d.currentMove());
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
            if (!m.id().startsWith("chg1_") || !ChgOneMoves.handles(m.id())) h.fail("chieu chung lot vao bo CHG I: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": CHG I khong nhan chieu rieng");
        });
    }

    /** Pha 1: AI tự chọn chiêu riêng (chg1_*), không dùng khung windup ChgKit. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = B)
    public static void phaseOneAiUsesOwnMoves(GameTestHelper h) {
        ServerPlayer p = target(h, 2, 6);
        ChapHanhGia d = spawn(h);
        h.onEachTick(() -> {
            d.setTarget(p);
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            String mv = d.currentMove();
            if (mv == null) return;
            d.discard();
            if (!ChgOneMoves.handles(mv)) h.fail("AI chon chieu la: " + mv);
            if (d.phase() != 1) h.fail("phai o pha 1");
            h.succeed();
        });
    }
}
