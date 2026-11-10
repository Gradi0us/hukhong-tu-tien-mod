package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.SuDo;
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

/** Sứ Đồ kiểu Margit: chiêu riêng (sd_*) được director giao cho boss tự thi triển và đều kết thúc. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class SuDoGameTests {
    private SuDoGameTests() {}

    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    @GameTest(template = "empty", timeoutTicks = 1000, batch = "sudo_moves")
    public static void suDoEveryPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-sudo"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 pv = h.absoluteVec(new Vec3(2, 1, 6));
        p.setPos(pv.x, pv.y, pv.z);
        SuDo d = StoryRegistry.SU_DO_KIEM.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Su Do"); return; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("Su Do phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("Su Do bien mat giua chieu " + d.currentMove());
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            if (d.currentMove() != null) {
                if (wait.incrementAndGet() > 150) h.fail("chieu " + d.currentMove() + " khong ket thuc (t=" + d.moveTick() + ")");
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
            if (!m.id().startsWith("sd_")) h.fail("chieu chung lot vao bo Su Do: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": Su Do khong nhan chieu rieng");
        });
    }
}
