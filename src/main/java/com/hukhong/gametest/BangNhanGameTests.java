package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.entity.BangNhan;
import com.hukhong.entity.BangNhanMoves;
import com.hukhong.entity.V3Entities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Băng Nhãn (kiểu Astel + Rennala): chiêu phase bn_* do boss tự thi triển (không PhaseMoves chung) và đều kết thúc. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class BangNhanGameTests {
    private BangNhanGameTests() {}

    private static final String B = "bangnhan";

    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -32; dx <= 32; dx += 16)
            for (int dz = -32; dz <= 32; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    @GameTest(template = "empty", timeoutTicks = 2000, batch = B + "_moves")
    public static void bangNhanEveryMoveIsOwnAndFinishes(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-bangnhan"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 pv = h.absoluteVec(new Vec3(2, 1, 8));
        p.setPos(pv.x, pv.y, pv.z);
        BangNhan d = V3Entities.BANG_NHAN.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Bang Nhan"); return; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("Bang Nhan phai co 8 chieu phase, thuc te " + moves.size());
        // 8 chieu phase (qua director) + chieu chi dung o pha 1 (goi truc tiep)
        List<String> extra = new ArrayList<>();
        for (String id : BangNhanMoves.IDS) if (moves.stream().noneMatch(m -> m.id().equals(id))) extra.add(id);
        int total = moves.size() + extra.size();
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("Bang Nhan bien mat giua chieu " + d.currentMove());
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            if (d.currentMove() != null) {
                if (wait.incrementAndGet() > 200) h.fail("chieu " + d.currentMove() + " khong ket thuc (t=" + d.moveTick() + ")");
                return;
            }
            int i = idx.incrementAndGet();
            if (i >= total) {
                boolean grav = d.isNoGravity();
                d.discard();
                forceArea(h, false);
                if (grav) h.fail("ket thuc chieu phai tra lai trong luc");
                h.succeed();
                return;
            }
            wait.set(0);
            if (i < moves.size()) {
                Move m = moves.get(i);
                if (!m.id().startsWith("bn_")) h.fail("chieu chung lot vao bo Bang Nhan: " + m.id());
                d.director().start(h.getLevel(), m, p);
                if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
                if (!m.id().equals(d.currentMove()) || d.idleForMove()) h.fail(m.id() + ": Bang Nhan khong nhan chieu rieng");
            } else {
                String id = extra.get(i - moves.size());
                if (!d.startMove(h.getLevel(), id, p)) h.fail("khong thi trien duoc " + id);
            }
        });
    }
}
