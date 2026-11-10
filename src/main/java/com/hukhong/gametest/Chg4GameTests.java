package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.chg.ChapHanhGia;
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

/** CHG IV kiểu Mohg: chiêu riêng (c4_*) được director giao cho boss tự thi triển; Hư Vô đánh dấu người chơi. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class Chg4GameTests {
    private Chg4GameTests() {}

    private static final String B = "chg4";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-chg4"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    /** Chiêu lướt/lao đưa boss xa khuôn test: ép nạp chunk quanh khuôn (batch riêng, xem DaKhuyetGameTests). */
    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static ChapHanhGia spawn(GameTestHelper h) {
        ChapHanhGia d = ChgRegistry.type(4).create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc CHG IV"); return null; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest(template = "empty", timeoutTicks = 1200, batch = B + "_moves")
    public static void chg4EveryPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 2, 6);
        ChapHanhGia d = spawn(h);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("CHG IV phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("CHG IV bien mat giua chieu " + d.currentMove());
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
            if (!m.id().startsWith("c4_")) h.fail("chieu chung lot vao bo CHG IV: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": CHG IV khong nhan chieu rieng");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B + "_nihil")
    public static void chg4NihilMarksPlayers(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 6, 6);
        ChapHanhGia d = spawn(h);
        d.setCurrentPhase(3);
        h.runAfterDelay(2, () -> {
            if (!d.startMove(h.getLevel(), "c4_nihil", p)) h.fail("khong thi trien duoc Hu Vo");
        });
        h.runAfterDelay(50, () -> {
            int marked = d.markedCount();
            if (marked == 0) { d.discard(); forceArea(h, false); h.fail("Hu Vo phai danh dau nguoi choi"); }
        });
        h.runAfterDelay(150, () -> {
            String mv = d.currentMove();
            boolean grav = d.isNoGravity();
            d.discard();
            forceArea(h, false);
            if (mv != null) h.fail("Hu Vo phai ket thuc");
            if (grav) h.fail("xong chieu phai tra lai trong luc");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void chg4OtherChgKeepsGenericMoves(GameTestHelper h) {
        ChapHanhGia c1 = ChgRegistry.type(1).create(h.getLevel());
        if (c1 == null) { h.fail("khong tao duoc CHG I"); return; }
        if (c1.startMove(h.getLevel(), "c4_quet", null)) h.fail("CHG I khong duoc dung chieu CHG IV");
        for (Move m : c1.movesForPhase(3)) if (m.id().startsWith("c4_")) h.fail("CHG I lot chieu c4: " + m.id());
        c1.discard();
        h.succeed();
    }
}
