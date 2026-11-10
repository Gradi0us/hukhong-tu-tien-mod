package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.entity.DocMau;
import com.hukhong.entity.DocMauMoves;
import com.hukhong.entity.HuTrung;
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

/** Độc Mẫu: chiêu riêng dm_* do director giao cho boss tự thi triển; ấu trùng biến mất khi mẹ chết. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class DocMauGameTests {
    private DocMauGameTests() {}

    private static final String B = "docmau";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-docmau"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    private static DocMau spawn(GameTestHelper h) {
        DocMau d = V3Entities.DOC_MAU.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Doc Mau"); return null; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    private static int broodNear(GameTestHelper h, DocMau d) {
        return h.getLevel().getEntitiesOfClass(HuTrung.class, d.getBoundingBox().inflate(48), e -> e.getTags().contains(DocMauMoves.BROOD_TAG)).size();
    }

    @GameTest(template = "empty", timeoutTicks = 1200, batch = B + "_moves")
    public static void docMauEveryPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        ServerPlayer p = target(h, 2, 6);
        DocMau d = spawn(h);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("Doc Mau phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("Doc Mau bien mat giua chieu " + d.currentMove());
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
                if (grav) h.fail("ket thuc chieu phai tra lai trong luc");
                h.succeed();
                return;
            }
            Move m = moves.get(i);
            if (!m.id().startsWith("dm_")) h.fail("chieu chung lot vao bo Doc Mau: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": Doc Mau khong nhan chieu rieng");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void docMauBroodDespawnsOnDeath(GameTestHelper h) {
        ServerPlayer p = target(h, 6, 6);
        DocMau d = spawn(h);
        d.setCurrentPhase(2);
        h.runAfterDelay(2, () -> { if (!d.startMove(h.getLevel(), "dm_au_trung", p)) h.fail("khong thi trien duoc Au Trung"); });
        h.runAfterDelay(45, () -> {
            if (d.broodCount() == 0 || broodNear(h, d) == 0) { d.discard(); h.fail("Au Trung phai sinh dan con"); return; }
            if (!d.startMove(h.getLevel(), "dm_diep_hu", p)) h.fail("khong thi trien duoc Diep Hu");
        });
        h.runAfterDelay(80, () -> {
            if (d.mothCount() == 0) h.fail("Diep Hu phai tha buom hu");
            d.kill();
        });
        h.runAfterDelay(90, () -> {
            int left = broodNear(h, d);
            d.discard();
            if (left != 0) h.fail("dan con phai bien mat khi me chet, con " + left);
            h.succeed();
        });
    }
}
