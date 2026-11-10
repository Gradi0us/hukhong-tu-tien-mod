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

/** CHG III Lăng Uyên kiểu Morgott: chiêu riêng c3_* được director giao cho boss tự thi triển; Huyết Kiếm để lại vũng huyết uyên. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class Chg3GameTests {
    private Chg3GameTests() {}

    private static final String B = "chg3";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-chg3"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    /**
     * Chiêu lướt/lao đưa boss xa tới ~16 block: ngoài khuôn test chunk có thể không tick thực thể (boss "đóng băng" giữa chiêu).
     * Ép nạp các chunk quanh khuôn trong lúc test, bỏ ép khi xong. Hai test dùng forceArea nằm ở batch riêng
     * (test cùng batch chạy song song, khuôn sát nhau => test này bỏ ép sẽ làm chunk của test kia ngừng tick).
     */
    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    /** Đánh dấu intro đã xong để không bị cutscene khoá. */
    private static ChapHanhGia spawn(GameTestHelper h) {
        ChapHanhGia d = ChgRegistry.type(3).create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc CHG III"); return null; }
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
        if (moves.size() != 8) h.fail("CHG III phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("CHG III bien mat giua chieu " + d.currentMove());
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
            if (!m.id().startsWith("c3_")) h.fail("chieu chung lot vao bo CHG III: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": CHG III khong nhan chieu rieng");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B + "_pool")
    public static void bloodSwordLeavesPool(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 2, 8);
        ChapHanhGia d = spawn(h);
        d.setCurrentPhase(3);
        h.runAfterDelay(2, () -> {
            if (!d.startMove(h.getLevel(), "c3_huyet_kiem", p)) h.fail("khong thi trien duoc Huyet Kiem Uyen Lang");
        });
        h.runAfterDelay(95, () -> {
            int pools = d.poolCount();
            d.discard();
            forceArea(h, false);
            if (pools == 0) h.fail("nhat kiem cuoi phai de lai vung huyet uyen");
            h.succeed();
        });
    }
}
