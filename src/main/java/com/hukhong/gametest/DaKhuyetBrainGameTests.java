package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.arena.BossArena;
import com.hukhong.boss.ai.BossBrain;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.endgame.DaKhuyet;
import com.hukhong.endgame.EndgameRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * BossBrain của Dạ Khuyết (PLAN-v1 §2): đi vòng, né theo tỉ lệ pha 1, Tam Nguyệt rẽ nhánh (lùi -> Đâm Xuyên, né ngang -> Quét),
 * Phản Nguyệt sau 3 nhát / 2 giây, đòn thường hút máu, áp sát xong trả trọng lực / hiện hình, ở trong sân.
 * Dùng phân thân (không director/đấu trường endgame), mục tiêu là dân làng NoAI máu lớn. Mỗi test một batch (ép chunk riêng).
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class DaKhuyetBrainGameTests {
    private DaKhuyetBrainGameTests() {}

    record Rig(DaKhuyet boss, Villager target) {
        BossBrain brain() { return BossBrain.get(boss); }
        double dist() { return Math.hypot(boss.getX() - target.getX(), boss.getZ() - target.getZ()); }
        double angle() { return Math.toDegrees(Math.atan2(boss.getZ() - target.getZ(), boss.getX() - target.getX())); }
    }

    private static void forceArea(GameTestHelper h, boolean on) {
        BlockPos c = h.absolutePos(new BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static Rig rig(GameTestHelper h, Vec3 bossRel, Vec3 targetRel, int phase) {
        forceArea(h, true);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        for (BlockPos q : BlockPos.betweenClosed(o.offset(-2, -1, -2), o.offset(7, 7, 7)))
            if (h.getLevel().getBlockState(q).is(Blocks.BARRIER)) h.getLevel().setBlock(q, Blocks.AIR.defaultBlockState(), 2);
        DaKhuyet d = EndgameRegistry.DA_KHUYET.get().create(h.getLevel());
        Villager v = EntityType.VILLAGER.create(h.getLevel());
        if (d == null || v == null) throw new IllegalStateException("khong tao duoc thuc the");
        d.markAvatar();
        d.addTag(BossPhaseDirector.INTRO_TAG);
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

    /** Đi vòng ngân kiếm: góc quanh mục tiêu đổi >= 90° trong 5 giây. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "dk_brain_strafe")
    public static void dkBrainStrafes(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 6.5), 1);
        Vec3 tv = h.absoluteVec(new Vec3(2, 1, 6.5));
        Double[] start = {null};
        double[] best = {0};
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            r.target().teleportTo(tv.x, r.target().getY(), tv.z);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowAttacks = false; b.allowEvade = false;
            if (b.state() != BossBrain.State.STRAFE && start[0] == null) return;
            if (start[0] == null) start[0] = r.angle();
            best[0] = Math.max(best[0], Math.abs(Mth.wrapDegrees(r.angle() - start[0])));
            if (best[0] >= 90) done(h, r);
            else if (t.incrementAndGet() > 100) h.fail("Da Khuyet di vong chi " + (int) best[0] + " do; d=" + r.dist() + " state=" + b.state());
        });
    }

    /** Né: tỉ lệ ≈ 0.30 (pha 1) ±0.1. */
    @GameTest(template = "empty", timeoutTicks = 2600, batch = "dk_brain_evade")
    public static void dkBrainEvadesAtPhaseChance(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 6), 1);
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowAttacks = false;
            b.onThreat(BossBrain.Threat.MELEE);
            if (t.incrementAndGet() >= 2500) {
                double rate = b.evades / (double) Math.max(1, b.evadeRolls);
                if (b.evadeRolls < 80) h.fail("qua it lan tung: " + b.evadeRolls);
                else if (Math.abs(rate - 0.30) > 0.1) h.fail("ti le ne " + rate + " (" + b.evades + "/" + b.evadeRolls + ") lech 0.30");
                else done(h, r);
            }
        });
    }

    private static void branchTest(GameTestHelper h, boolean retreat) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 4), 1);
        AtomicInteger t = new AtomicInteger();
        boolean[] moved = {false};
        String want = retreat ? "tam_nguyet->dam_xuyen" : "tam_nguyet->quet";
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            if (i == 5) b.startCombo(b.profile().combo("tam_nguyet"), r.target());
            if (i < 5) return;
            if (!moved[0] && "tam_nguyet".equals(b.comboId()) && b.hitIndex() == 1 && b.hitStage() == 2) { // nhát 2 đang thu đòn
                moved[0] = true;
                Vec3 rel = r.target().position().subtract(r.boss().position()).multiply(1, 0, 1);
                Vec3 to = retreat ? r.boss().position().add(rel.normalize().scale(rel.length() + 6))
                        : r.boss().position().add(new Vec3(-rel.z, 0, rel.x)); // 90° sang ngang, cùng khoảng cách
                r.target().teleportTo(to.x, r.target().getY(), to.z);
            }
            if (want.equals(b.lastBranch) && b.combosCompleted >= 1) done(h, r);
            else if (i > 280) h.fail("khong re nhanh " + want + ": branch=" + b.lastBranch + " done=" + b.combosCompleted);
        });
    }

    /** Tam Nguyệt: sau nhát 2 mục tiêu lùi > 4 ô -> nối Đâm Xuyên. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "dk_brain_branch_a")
    public static void dkComboBranchesToThrustOnRetreat(GameTestHelper h) { branchTest(h, true); }

    /** Tam Nguyệt: sau nhát 2 mục tiêu né ngang -> nối Quét 270°. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "dk_brain_branch_b")
    public static void dkComboBranchesToSweepOnStrafe(GameTestHelper h) { branchTest(h, false); }

    /** Phản Nguyệt: bị đánh 3 lần trong 2 giây -> lướt ngang rồi chuỗi phan_nguyet. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "dk_brain_counter")
    public static void dkCounterAfterThreeHits(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 5), 1);
        AtomicInteger t = new AtomicInteger(), hits = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            if (b.reacts == 0) {
                b.allowAttacks = false;
                if (i > 10 && i % 12 == 0 && hits.get() < 3) {
                    b.onHurt(r.target()); // = BrainEvents.onDamaged (sát thương từ mob bị bộ lọc boss của mod chặn)
                    hits.incrementAndGet();
                    if (hits.get() < 3 && b.reacts > 0) h.fail("phan don som sau " + hits.get() + " nhat");
                }
            } else b.allowAttacks = true;
            if ("phan_nguyet".equals(b.comboId())) done(h, r);
            else if (i > 180) h.fail("khong phan don: hits=" + hits.get() + " reacts=" + b.reacts + " state=" + b.state());
        });
    }

    /** Đòn thường (Kiếm Vũ) trúng -> Dạ Khuyết hồi máu. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "dk_brain_drain")
    public static void dkBasicHitsDrainLife(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 3.8), 1);
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            r.target().setHealth(r.target().getMaxHealth());
            r.boss().setTarget(r.target());
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            if (i == 5) { r.boss().setHealth(1000); b.startCombo(b.profile().combo("kiem_vu"), r.target()); }
            if (i < 5) return;
            if (r.boss().getHealth() > 1000) done(h, r);
            else if (i > 150) h.fail("don thuong trung khong hut mau (hp=" + r.boss().getHealth() + ")");
        });
    }

    /** Áp sát (pha 3: lướt / bay lượn / lướt máu) đưa boss lại gần, xong thì có trọng lực + hiện hình, rồi vào chuỗi đòn. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "dk_brain_gap")
    public static void dkGapCloseEndsCleanly(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 16), 3);
        AtomicInteger t = new AtomicInteger(), gaps = new AtomicInteger();
        BossBrain.State[] prev = {null};
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            BossBrain.State s = b.state();
            if (prev[0] == BossBrain.State.GAP_CLOSE && s != BossBrain.State.GAP_CLOSE) {
                if (r.boss().isNoGravity() || r.boss().isInvisible()) h.fail("ap sat xong van bay/tang hinh");
                if (r.dist() > 6) h.fail("ap sat xong van xa " + r.dist());
                gaps.incrementAndGet();
            }
            prev[0] = s;
            if (gaps.get() >= 1 && s == BossBrain.State.BASIC_COMBO) done(h, r);
            else if (i > 380) h.fail("khong ap sat: gaps=" + gaps.get() + " state=" + s + " d=" + r.dist());
        });
    }

    /** Ở trong sân: mục tiêu sát vành, 15 giây đi vòng/lùi/né vẫn trong bán kính và không kẹt. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "dk_brain_arena")
    public static void dkStaysInsideArena(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 13), 2);
        BossArena.attach(r.boss(), 14, null, false, null);
        AtomicInteger t = new AtomicInteger();
        double[] travel = {0};
        Vec3[] last = {null};
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            double[] a = BossArena.bounds(r.boss());
            if (b == null || a == null) { if (t.incrementAndGet() > 40) h.fail("khong co brain/dau truong"); return; }
            b.allowAttacks = false;
            if (t.get() % 20 == 0) b.onThreat(BossBrain.Threat.MELEE);
            double d = Math.hypot(r.boss().getX() - a[0], r.boss().getZ() - a[1]);
            if (d > a[2] - 0.5) h.fail("Da Khuyet ra ngoai san: " + d + " / " + a[2]);
            if (last[0] != null) travel[0] += r.boss().position().distanceTo(last[0]);
            last[0] = r.boss().position();
            if (t.incrementAndGet() > 300) {
                if (travel[0] < 6) h.fail("Da Khuyet ket (di duoc " + travel[0] + ")");
                else done(h, r);
            }
        });
    }
}
