package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.chg.ChapHanhGia;
import com.hukhong.chg.ChgMaliketh;
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

/** CHG II (Bạch Từ Âm, kiểu Maliketh): director giao chiêu riêng chg2_* cho boss tự thi triển; Tử Mệnh giảm hồi máu. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ChgMalikethGameTests {
    private ChgMalikethGameTests() {}

    private static final String B = "chg2";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-chg2"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    /** Chiêu bổ/nhảy đưa boss xa ~16 block: ép nạp chunk quanh khuôn để boss không "đóng băng" (xem DaKhuyetGameTests). */
    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static ChapHanhGia spawn(GameTestHelper h) {
        ChapHanhGia d = ChgRegistry.type(2).create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc CHG II"); return null; }
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
        if (moves.size() != 8) h.fail("CHG II phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("CHG II bien mat giua chieu " + d.currentOwnMove());
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            if (d.currentOwnMove() != null) {
                if (wait.incrementAndGet() > 200) h.fail("chieu " + d.currentOwnMove() + " khong ket thuc");
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
            if (!m.id().startsWith("chg2_")) h.fail("chieu chung lot vao bo CHG II: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentOwnMove() == null || d.idleForMove()) h.fail(m.id() + ": CHG II khong nhan chieu rieng");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void otherChgHasNoOwnMoves(GameTestHelper h) {
        ChapHanhGia c = ChgRegistry.type(3).create(h.getLevel());
        if (c == null) { h.fail("khong tao duoc CHG III"); return; }
        if (c.startOwn(h.getLevel(), "chg2_trao", null)) h.fail("CHG III khong duoc dung chieu cua CHG II");
        c.discard();
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void destinedDeathReducesHealing(GameTestHelper h) {
        net.minecraft.world.entity.animal.Pig p = h.spawn(net.minecraft.world.entity.EntityType.PIG, new net.minecraft.core.BlockPos(2, 1, 2));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        p.setHealth(100);
        p.heal(100);
        float normal = p.getHealth() - 100;
        p.setHealth(100);
        ChgMaliketh.doom(h.getLevel(), p, 100);
        if (!ChgMaliketh.doomed(p)) h.fail("phai bi Tu Menh");
        p.heal(100);
        float doomed = p.getHealth() - 100;
        p.discard();
        if (normal <= 0) h.fail("hoi mau thuong phai > 0");
        if (doomed >= normal * 0.6f) h.fail("Tu Menh phai giam hoi mau (thuong " + normal + ", bi Tu Menh " + doomed + ")");
        h.succeed();
    }
}
