package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.arena.BossArena;
import com.hukhong.boss.ai.BossBrain;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.chg.ChapHanhGia;
import com.hukhong.chg.ChgRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * BossBrain cho 4 Chấp Hành Giả (PLAN-v1 §4, B4): CHG I không đi vòng/không né + dậm đất phản đòn; CHG II pha 1 đi vòng + né,
 * pha 2 hắc kiếm; CHG III né kèm ném dao, gậy 2 nhát trễ; CHG IV giữ tầm 5-9, pha 1 không né, áp sát -> quét 360°.
 * Mục tiêu là dân làng NoAI máu lớn; chiêu riêng tắt (ownMovesEnabled=false) để chỉ đo brain.
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ChgBrainGameTests {
    private ChgBrainGameTests() {}

    record Rig(ChapHanhGia boss, Villager target) {
        BossBrain brain() { return BossBrain.get(boss); }
        double dist() { return Math.hypot(boss.getX() - target.getX(), boss.getZ() - target.getZ()); }
        double angle() { return Math.toDegrees(Math.atan2(boss.getZ() - target.getZ(), boss.getX() - target.getX())); }
    }

    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static Rig rig(GameTestHelper h, int idx, int phase, Vec3 bossRel, Vec3 targetRel) {
        forceArea(h, true);
        net.minecraft.core.BlockPos o = h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(o.offset(-2, -1, -2), o.offset(7, 7, 7)))
            if (h.getLevel().getBlockState(q).is(net.minecraft.world.level.block.Blocks.BARRIER))
                h.getLevel().setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        ChapHanhGia d = ChgRegistry.type(idx).create(h.getLevel());
        Villager v = EntityType.VILLAGER.create(h.getLevel());
        if (d == null || v == null) throw new IllegalStateException("khong tao duoc thuc the");
        d.addTag(BossPhaseDirector.INTRO_TAG);
        d.ownMovesEnabled = false;
        d.setCurrentPhase(phase);
        Vec3 b = h.absoluteVec(bossRel), t = h.absoluteVec(targetRel);
        d.moveTo(b.x, b.y, b.z, 0, 0);
        v.moveTo(t.x, t.y, t.z, 0, 0);
        v.setNoAi(true);
        v.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);
        v.setHealth(v.getMaxHealth());
        h.getLevel().addFreshEntity(d);
        h.getLevel().addFreshEntity(v);
        return new Rig(d, v);
    }

    private static void keep(Rig r) {
        r.boss().setHealth(r.boss().getMaxHealth());
        r.target().setHealth(r.target().getMaxHealth());
        r.boss().setTarget(r.target());
    }

    private static void done(GameTestHelper h, Rig r) { r.boss().discard(); r.target().discard(); forceArea(h, false); h.succeed(); }

    private static void pin(GameTestHelper h, Rig r, Vec3 rel) {
        Vec3 a = h.absoluteVec(rel);
        r.target().teleportTo(a.x, r.target().getY(), a.z);
    }

    // ------------------------------------------------------------------ đi vòng hay không (theo tính cách)
    /** Đo góc quay quanh mục tiêu trong 120 tick (không đánh, không né). */
    private static void strafeTest(GameTestHelper h, int idx, int phase, Vec3 targetRel, boolean expectStrafe) {
        Rig r = rig(h, idx, phase, new Vec3(2, 1, 2), targetRel);
        AtomicInteger t = new AtomicInteger();
        Double[] start = {null};
        double[] best = {0};
        h.onEachTick(() -> {
            keep(r);
            pin(h, r, targetRel);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowAttacks = false; b.allowEvade = false;
            if (b.state() != BossBrain.State.STRAFE && start[0] == null) return;
            if (start[0] == null) start[0] = r.angle();
            best[0] = Math.max(best[0], Math.abs(Mth.wrapDegrees(r.angle() - start[0])));
            if (expectStrafe && best[0] >= 60) done(h, r);
            if (t.incrementAndGet() >= 120) {
                if (expectStrafe) h.fail("CHG " + idx + " pha " + phase + " khong di vong: " + (int) best[0] + " do");
                else if (best[0] > 30) h.fail("CHG " + idx + " khong duoc di vong nhieu: " + (int) best[0] + " do");
                else done(h, r);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "chg_brain_1")
    public static void chg1BarelyStrafes(GameTestHelper h) { strafeTest(h, 1, 1, new Vec3(2, 1, 6), false); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "chg_brain_1")
    public static void chg2PhaseOneStrafes(GameTestHelper h) { strafeTest(h, 2, 1, new Vec3(2, 1, 7), true); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "chg_brain_2")
    public static void chg3Strafes(GameTestHelper h) { strafeTest(h, 3, 1, new Vec3(2, 1, 7.5), true); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "chg_brain_2")
    public static void chg4StrafesAtRange(GameTestHelper h) { strafeTest(h, 4, 1, new Vec3(2, 1, 9), true); }

    // ------------------------------------------------------------------ né hay không
    private static void evadeTest(GameTestHelper h, int idx, int phase, boolean expectEvade) {
        Rig r = rig(h, idx, phase, new Vec3(2, 1, 2), new Vec3(2, 1, 8));
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowAttacks = false;
            b.onThreat(BossBrain.Threat.MELEE);
            if (expectEvade && b.evades >= 3) {
                if (idx == 3 && r.boss().daggersThrown < 1) h.fail("CHG III ne ma khong nem dao");
                else done(h, r);
            }
            if (t.incrementAndGet() >= 400) {
                if (expectEvade) h.fail("CHG " + idx + " pha " + phase + " khong ne: " + b.evades + "/" + b.evadeRolls);
                else if (b.evades > 0) h.fail("CHG " + idx + " pha " + phase + " khong duoc ne nhung ne " + b.evades + " lan");
                else done(h, r);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "chg_brain_3")
    public static void chg1NeverEvades(GameTestHelper h) { evadeTest(h, 1, 3, false); }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "chg_brain_3")
    public static void chg2Evades(GameTestHelper h) { evadeTest(h, 2, 1, true); }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "chg_brain_4")
    public static void chg3EvadesAndThrowsDagger(GameTestHelper h) { evadeTest(h, 3, 1, true); }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "chg_brain_4")
    public static void chg4PhaseOneNeverEvades(GameTestHelper h) { evadeTest(h, 4, 1, false); }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "chg_brain_5")
    public static void chg4PhaseTwoEvades(GameTestHelper h) { evadeTest(h, 4, 2, true); }

    // ------------------------------------------------------------------ chuỗi đòn chạy hết
    private static void comboTest(GameTestHelper h, int idx, int phase, String combo, double range) {
        Vec3 tRel = new Vec3(2, 1, 2 + range);
        Rig r = rig(h, idx, phase, new Vec3(2, 1, 2), tRel);
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            pin(h, r, tRel);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            if (i == 5) {
                if (b.profile().combo(combo) == null) { h.fail("khong co combo " + combo); return; }
                b.startCombo(b.profile().combo(combo), r.target());
            }
            if (i < 5) return;
            if (b.combosCompleted >= 1) {
                if (b.branchesTaken != 0) h.fail("re nhanh khi muc tieu dung yen: " + b.lastBranch);
                else done(h, r);
            } else if (i > 280) h.fail("CHG " + idx + " chuoi " + combo + " khong ket thuc, state=" + b.state() + " combo=" + b.comboId());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "chg_brain_5")
    public static void chg1AxeComboCompletes(GameTestHelper h) { comboTest(h, 1, 1, "riu", 2.5); }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "chg_brain_6")
    public static void chg1GrabPunchComboCompletes(GameTestHelper h) { comboTest(h, 1, 2, "vo", 3); }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "chg_brain_6")
    public static void chg2ClawComboCompletes(GameTestHelper h) { comboTest(h, 2, 1, "cao", 2.5); }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "chg_brain_7")
    public static void chg2BladeComboCompletes(GameTestHelper h) { comboTest(h, 2, 2, "hac", 2.5); }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "chg_brain_7")
    public static void chg3CaneComboCompletes(GameTestHelper h) { comboTest(h, 3, 1, "gay", 2.5); }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "chg_brain_8")
    public static void chg4TridentComboCompletes(GameTestHelper h) { comboTest(h, 4, 1, "dinh_ba", 4); }

    // ------------------------------------------------------------------ phản ứng riêng
    /** CHG I: bị đánh 3 lần -> dậm đất tại chỗ (không né). */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "chg_brain_8")
    public static void chg1CounterStompsWithoutEvade(GameTestHelper h) {
        Rig r = rig(h, 1, 1, new Vec3(2, 1, 2), new Vec3(2, 1, 4.5));
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            int i = t.incrementAndGet();
            if (i == 5) { b.allowAttacks = true; for (int k = 0; k < 3; k++) b.onHurt(r.target()); }
            if (i < 5) return;
            if (b.evades > 0) h.fail("CHG I khong duoc ne khi phan don");
            if ("dam".equals(b.comboId())) done(h, r);
            else if (i > 150) h.fail("khong dam dat: reacts=" + b.reacts + " state=" + b.state() + " combo=" + b.comboId());
        });
    }

    /** CHG IV: mục tiêu áp sát (1.5 ô) -> chọn quét 360°. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "chg_brain_9")
    public static void chg4SweepsWhenCrowded(GameTestHelper h) {
        Vec3 tRel = new Vec3(2, 1, 3.5);
        Rig r = rig(h, 4, 1, new Vec3(2, 1, 2), tRel);
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            Vec3 at = r.boss().position().add(r.boss().getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
            r.target().teleportTo(at.x, r.target().getY(), at.z);
            if (b.comboId() != null) {
                if ("xoay".equals(b.comboId())) done(h, r);
                else h.fail("ap sat ma chon " + b.comboId());
            } else if (t.incrementAndGet() > 150) h.fail("khong ra don khi bi ap sat, state=" + b.state());
        });
    }

    // ------------------------------------------------------------------ ở trong sân
    private static void arenaTest(GameTestHelper h, int idx) {
        Rig r = rig(h, idx, 2, new Vec3(2, 1, 2), new Vec3(2, 1, 15));
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            double[] a = BossArena.bounds(r.boss());
            if (b == null || a == null) { if (t.incrementAndGet() > 40) h.fail("khong co brain/dau truong"); return; }
            b.allowAttacks = false;
            b.onThreat(BossBrain.Threat.MELEE);
            double d = Math.hypot(r.boss().getX() - a[0], r.boss().getZ() - a[1]);
            if (d > a[2] - 0.5) h.fail("CHG " + idx + " ra ngoai san: " + d + " / " + a[2]);
            if (t.incrementAndGet() > 300) done(h, r);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "chg_brain_9")
    public static void chg1StaysInArena(GameTestHelper h) { arenaTest(h, 1); }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "chg_brain_10")
    public static void chg2StaysInArena(GameTestHelper h) { arenaTest(h, 2); }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "chg_brain_10")
    public static void chg3StaysInArena(GameTestHelper h) { arenaTest(h, 3); }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "chg_brain_11")
    public static void chg4StaysInArena(GameTestHelper h) { arenaTest(h, 4); }
}
