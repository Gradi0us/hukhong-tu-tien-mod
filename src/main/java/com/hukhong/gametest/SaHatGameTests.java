package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.entity.SaHatMoves;
import com.hukhong.entity.SaHatVuong;
import com.hukhong.entity.V3Entities;
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

/** Sa Hat Vuong kieu Radahn: chieu rieng (sh_*) duoc director giao cho boss tu thi trien va ket thuc; thien thach giang dung diem. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class SaHatGameTests {
    private SaHatGameTests() {}

    private static final String B = "sahat";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-sahat"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -32; dx <= 32; dx += 16)
            for (int dz = -32; dz <= 32; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static SaHatVuong spawn(GameTestHelper h) {
        SaHatVuong d = V3Entities.SA_HAT_VUONG.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Sa Hat Vuong"); return null; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest(template = "empty", timeoutTicks = 1400, batch = B + "_moves")
    public static void sahatEveryPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 2, 8);
        SaHatVuong d = spawn(h);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("Sa Hat Vuong phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("Sa Hat Vuong bien mat giua chieu " + d.currentMove());
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            if (d.currentMove() != null) {
                if (wait.incrementAndGet() > 200) h.fail("chieu " + d.currentMove() + " khong ket thuc (t=" + d.moveTick() + ")");
                return;
            }
            int i = idx.incrementAndGet();
            if (i >= moves.size()) {
                boolean grav = d.isNoGravity(), inv = d.isInvisible();
                d.discard();
                forceArea(h, false);
                if (grav) h.fail("ket thuc chieu phai tra lai trong luc");
                if (inv) h.fail("ket thuc chieu phai hien hinh lai");
                h.succeed();
                return;
            }
            Move m = moves.get(i);
            if (!m.id().startsWith("sh_")) h.fail("chieu chung lot vao bo Sa Hat Vuong: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": Sa Hat Vuong khong nhan chieu rieng");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B + "_meteor")
    public static void sahatMeteorLandsOnTargetAndSpawnsOrbs(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 9, 9);
        SaHatVuong d = spawn(h);
        d.setCurrentPhase(3);
        h.runAfterDelay(2, () -> { if (!d.startMove(h.getLevel(), SaHatMoves.ULT, p)) h.fail("khong thi trien duoc Tinh Van"); });
        h.runAfterDelay(2 + SaHatMoves.MET_MARK + 6, () -> {
            if (d.getY() < p.getY() + 20) h.fail("Tinh Van: boss phai bay len troi");
        });
        h.runAfterDelay(2 + SaHatMoves.MET_HIT + 2, () -> {
            double dx = d.getX() - p.getX(), dz = d.getZ() - p.getZ();
            if (dx * dx + dz * dz > 4) h.fail("thien thach phai giang xuong vi tri muc tieu");
            if (d.orbCount() == 0) h.fail("du chan phai sinh cau trong luc");
        });
        h.runAfterDelay(2 + SaHatMoves.MET_END + 4, () -> {
            String mv = d.currentMove();
            boolean grav = d.isNoGravity();
            d.discard();
            forceArea(h, false);
            if (mv != null) h.fail("Tinh Van phai ket thuc");
            if (grav) h.fail("xong chieu phai tra lai trong luc");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160, batch = B)
    public static void sahatQuicksandLeavesPools(GameTestHelper h) {
        ServerPlayer p = target(h, 4, 6);
        SaHatVuong d = spawn(h);
        h.runAfterDelay(2, () -> { if (!d.startMove(h.getLevel(), "sh_luu_sa", p)) h.fail("khong thi trien duoc Luu Sa"); });
        h.runAfterDelay(26, () -> {
            if (d.sandCount() == 0) { d.discard(); h.fail("Luu Sa phai de lai cat lun"); }
        });
        h.runAfterDelay(40, () -> {
            String mv = d.currentMove();
            d.discard();
            if (mv != null) h.fail("Luu Sa phai ket thuc");
            h.succeed();
        });
    }
}
