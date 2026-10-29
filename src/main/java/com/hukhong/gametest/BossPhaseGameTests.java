package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhases;
import com.hukhong.boss.phase.PhasedBoss;
import com.hukhong.entity.WorldBoss;
import com.hukhong.events.WorldBossManager;
import net.minecraft.core.BlockPos;
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

/**
 * Khung pha boss (Dot 4b): boss qua du 3 pha (scale 1 -> 1.25 -> 1.5), bat tu trong luc chuyen pha, chieu phase chi mo tu pha 2,
 * cau hinh du cho 9 boss. Dung Sa Hat Vuong (WorldBoss, khong dung san BossArena) lam dai dien.
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class BossPhaseGameTests {
    private BossPhaseGameTests() {}

    private static final String B = "bossphase";
    private static final List<String> IDS = List.of("hu_vuong", "sa_hat_vuong", "bang_nhan", "doc_mau", "chg_1", "chg_2", "chg_3", "chg_4", "da_khuyet");

    private static ServerPlayer target(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-bossphase"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 6));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    private static WorldBoss spawn(GameTestHelper h) {
        WorldBoss b = WorldBossManager.spawn(h.getLevel(), WorldBossManager.DESERT, h.absolutePos(new BlockPos(2, 1, 1)));
        if (b == null) h.fail("khong sinh duoc Sa Hat Vuong");
        b.setPersistenceRequired();
        return b;
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void configCoversNineBosses(GameTestHelper h) {
        for (String id : IDS) {
            if (!BossPhases.has(id)) h.fail("thieu cau hinh pha cho " + id);
            BossPhaseConfig.Config c = BossPhases.get(id);
            if (!c.movesUpTo(1).isEmpty()) h.fail(id + ": pha 1 khong duoc co chieu phase");
            if (c.movesUpTo(2).size() != 2) h.fail(id + ": pha 2 phai them dung 2 chieu");
            List<Move> p3 = c.movesUpTo(3);
            if (p3.size() != 3 || !p3.get(2).ultimate() || p3.get(2).windup() < 80) h.fail(id + ": pha 3 phai them 1 chieu toi thuong co vong canh bao");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 500, batch = B)
    public static void bossPassesAllPhasesAndGrows(GameTestHelper h) {
        ServerPlayer p = target(h);
        WorldBoss b = spawn(h);
        h.onEachTick(() -> { if (b.isAlive()) b.setTarget(p); });
        float base = b.director().currentScale();
        if (Math.abs(base - 1f) > 1e-3) h.fail("scale ban dau phai 1.0, thuc te " + base);
        h.runAfterDelay(5, () -> b.setHealth(b.getMaxHealth() * 0.6f));
        h.runAfterDelay(150, () -> {
            if (b.phase() != 2) h.fail("sau chuyen pha 1->2 phai o pha 2, thuc te " + b.phase());
            if (b.director().transitioning()) h.fail("chuyen pha 2 chua ket thuc sau 150 tick");
            if (Math.abs(b.director().currentScale() - 1.25f) > 0.02f) h.fail("scale pha 2 phai 1.25, thuc te " + b.director().currentScale());
            if (b.getBbHeight() < 1e-3) h.fail("hitbox loi");
            b.setHealth(b.getMaxHealth() * 0.25f);
        });
        h.runAfterDelay(320, () -> {
            boolean ok = b.phase() == 3 && !b.director().transitioning() && Math.abs(b.director().currentScale() - 1.5f) < 0.02f;
            int ph = b.phase();
            float sc = b.director().currentScale();
            b.discard();
            if (!ok) h.fail("phai o pha 3 voi scale 1.5 (pha=" + ph + ", scale=" + sc + ")");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = B)
    public static void bossInvulnerableDuringTransition(GameTestHelper h) {
        ServerPlayer p = target(h);
        WorldBoss b = spawn(h);
        h.onEachTick(() -> { if (b.isAlive()) b.setTarget(p); });
        h.runAfterDelay(5, () -> b.setHealth(b.getMaxHealth() * 0.6f));
        AtomicInteger sawFrozen = new AtomicInteger();
        h.runAfterDelay(20, () -> {
            if (!b.director().transitioning()) h.fail("HP < 66% phai bat dau chuyen pha");
            if (!b.director().invulnerable() || !b.isImmobile()) h.fail("dang chuyen pha: boss phai bat tu + AI bi khoa");
            float before = b.getHealth();
            boolean hit = b.hurt(h.getLevel().damageSources().generic(), 50f);
            if (hit || b.getHealth() < before) h.fail("boss bi mat mau trong luc chuyen pha (bat tu)");
            sawFrozen.incrementAndGet();
        });
        h.runAfterDelay(160, () -> {
            if (b.director().transitioning()) h.fail("chuyen pha khong ket thuc");
            if (b.director().invulnerable()) h.fail("het chuyen pha phai het bat tu");
            float before = b.getHealth();
            b.hurt(h.getLevel().damageSources().generic(), 5f);
            boolean lost = b.getHealth() < before;
            b.discard();
            if (!lost) h.fail("sau chuyen pha boss phai nhan sat thuong lai");
            if (sawFrozen.get() != 1) h.fail("khong kiem tra duoc trang thai bat tu");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = B)
    public static void phaseMovesUnlockOnlyFromPhase2(GameTestHelper h) {
        ServerPlayer p = target(h);
        WorldBoss b = spawn(h);
        PhasedBoss pb = b;
        if (!pb.movesForPhase(1).isEmpty()) h.fail("pha 1 khong co chieu phase");
        if (pb.movesForPhase(2).size() != 2 || pb.movesForPhase(3).size() != 3) h.fail("so chieu theo pha sai");
        h.onEachTick(() -> { if (b.isAlive()) b.setTarget(p); });
        AtomicInteger p1Runs = new AtomicInteger(), p2Runs = new AtomicInteger();
        h.onEachTick(() -> {
            if (b.director().currentRun() == null) return;
            (b.phase() == 1 ? p1Runs : p2Runs).incrementAndGet();
            Move m = b.director().currentRun().move;
            if (b.phase() >= 2 && !pb.movesForPhase(2).contains(m)) h.fail("chieu khong thuoc bo pha 2: " + m.id());
        });
        h.runAfterDelay(140, () -> {
            if (p1Runs.get() != 0) h.fail("pha 1 da dung chieu phase (" + p1Runs.get() + " tick)");
            b.setCurrentPhase(2);  // ep sang pha 2 (khong cutscene)
            b.setHealth(b.getMaxHealth() * 0.5f);
        });
        h.runAfterDelay(380, () -> {
            int runs = p2Runs.get();
            b.discard();
            if (runs == 0) h.fail("sang pha 2 nhung director khong thi trien chieu phase nao");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void forcedMoveRunsAndClears(GameTestHelper h) {
        ServerPlayer p = target(h);
        WorldBoss b = spawn(h);
        b.setCurrentPhase(2);
        b.setTarget(p);
        Move m = b.director().moveById("cat_lun");
        if (m == null) h.fail("khong tim thay cat_lun");
        b.director().start(h.getLevel(), m, p);
        if (b.director().currentRun() == null || !b.isImmobile()) h.fail("dang gong chieu: boss phai bi khoa AI");
        h.runAfterDelay(m.windup() + 10, () -> {
            boolean cleared = b.director().currentRun() == null;
            b.discard();
            if (!cleared) h.fail("chieu khong ket thuc sau windup");
            h.succeed();
        });
    }
}
