package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.arena.BossArena;
import com.hukhong.boss.ai.BossBrain;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.SuDo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BossBrain (PLAN-v1 §7) trên boss mẫu Sứ Đồ Dạ Kiếm: lùi khi bị áp sát, đi vòng, né theo tỉ lệ, ở trong sân,
 * khóa hướng khi vung, chuỗi đòn chạy hết và rẽ nhánh. Mục tiêu là dân làng NoAI máu lớn (fake player không nằm trong level).
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class BossBrainGameTests {
    private BossBrainGameTests() {}

    record Rig(SuDo boss, Villager target) {
        BossBrain brain() { return BossBrain.get(boss); }
        double dist() { return Math.hypot(boss.getX() - target.getX(), boss.getZ() - target.getZ()); }
        double angle() { return Math.toDegrees(Math.atan2(boss.getZ() - target.getZ(), boss.getX() - target.getX())); }
    }

    /** Không có người chơi thật => phải ép chunk để boss tick đều mỗi tick. */
    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static Rig rig(GameTestHelper h, Vec3 bossRel, Vec3 targetRel) {
        forceArea(h, true);
        // khuôn "empty" 5x5 bị bao tường barrier: gỡ để boss di chuyển tự do trong sân
        net.minecraft.core.BlockPos o = h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(o.offset(-2, -1, -2), o.offset(7, 7, 7)))
            if (h.getLevel().getBlockState(q).is(net.minecraft.world.level.block.Blocks.BARRIER))
                h.getLevel().setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        SuDo d = StoryRegistry.SU_DO_KIEM.get().create(h.getLevel());
        Villager v = EntityType.VILLAGER.create(h.getLevel());
        if (d == null || v == null) throw new IllegalStateException("khong tao duoc thuc the");
        d.addTag(BossPhaseDirector.INTRO_TAG);
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

    /** Bị áp sát (1.5 ô) khi đang đi vòng -> lùi ra ít nhất 3 ô (khoảng cách ưa thích 3-6). */
    @GameTest(template = "empty", timeoutTicks = 140, batch = "boss_brain_1")
    public static void brainBacksOffWhenAdjacent(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(3.5, 1, 2));
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowAttacks = false; b.allowEvade = false;
            if (t.incrementAndGet() > 10 && r.dist() >= 2.9) done(h, r);
            else if (t.get() > 120) h.fail("boss khong lui: d=" + r.dist() + " state=" + b.state());
        });
    }

    /** Đi vòng: góc quanh mục tiêu đổi >= 90° trong 5 giây. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "boss_brain_2")
    public static void brainStrafesAroundTarget(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 6.5));
        AtomicReference<Double> start = new AtomicReference<>();
        AtomicInteger t = new AtomicInteger();
        double[] best = {0}, moved = {0};
        Vec3[] prev = {null};
        h.onEachTick(() -> {
            keep(r);
            r.target().teleportTo(h.absoluteVec(new Vec3(2, 1, 6.5)).x, r.target().getY(), h.absoluteVec(new Vec3(2, 1, 6.5)).z);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowAttacks = false; b.allowEvade = false;
            if (b.state() != BossBrain.State.STRAFE && start.get() == null) return;
            if (start.get() == null) start.set(r.angle());
            if (prev[0] != null) moved[0] += r.boss().position().subtract(prev[0]).horizontalDistance();
            prev[0] = r.boss().position();
            best[0] = Math.max(best[0], Math.abs(Mth.wrapDegrees(r.angle() - start.get())));
            if (best[0] >= 90) done(h, r);
            else if (t.incrementAndGet() > 100) h.fail("di vong chi duoc " + (int) best[0] + " do trong 5s; v=" + b.movement().velocity() + " d=" + r.dist() + " dm=" + r.boss().getDeltaMovement() + " goc=" + (int) r.angle() + " start=" + start.get().intValue() + " moved=" + moved[0] + " below=" + h.getLevel().getBlockState(r.boss().blockPosition().below()) + " flips=" + b.strafeFlips + " pos=" + h.relativeVec(r.boss().position()));
        });
    }

    /** Né: tỉ lệ né khi bị đe dọa ≈ evadeChance pha 1 (0.25) ±0.1; đòn đánh của người chơi (AttackEntityEvent) kích hoạt né. */
    @GameTest(template = "empty", timeoutTicks = 3100, batch = "boss_brain_3")
    public static void brainEvadesAtConfiguredChance(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 6));
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "hk-brain"));
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowAttacks = false;
            int i = t.incrementAndGet();
            if (i < 40) { if (i % 3 == 0) net.neoforged.neoforge.common.CommonHooks.onPlayerAttackTarget(p, r.boss()); return; } // AttackEntityEvent -> BrainEvents
            if (i == 40) { if (b.evadeRolls == 0) h.fail("AttackEntityEvent khong kich hoat ne"); b.evadeRolls = 0; b.evades = 0; }
            b.onThreat(BossBrain.Threat.MELEE);
            if (i >= 3000) {
                double rate = b.evades / (double) Math.max(1, b.evadeRolls);
                if (b.evadeRolls < 100) h.fail("qua it lan tung: " + b.evadeRolls);
                else if (Math.abs(rate - 0.25) > 0.1) h.fail("ti le ne " + rate + " (" + b.evades + "/" + b.evadeRolls + ") lech 0.25");
                else done(h, r);
            }
        });
    }

    /** Ở trong sân: mục tiêu sát vành, boss đi vòng/lùi 15 giây vẫn trong bán kính và vẫn di chuyển (không kẹt). */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "boss_brain_4")
    public static void brainStaysInsideArena(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 15));
        AtomicInteger t = new AtomicInteger();
        double[] travel = {0};
        Vec3[] last = {null};
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            double[] a = BossArena.bounds(r.boss());
            if (b == null || a == null) { if (t.incrementAndGet() > 40) h.fail("khong co brain/dau truong"); return; }
            b.allowAttacks = false;
            double d = Math.hypot(r.boss().getX() - a[0], r.boss().getZ() - a[1]);
            if (d > a[2] - 0.5) h.fail("boss ra ngoai san: " + d + " / " + a[2]);
            if (last[0] != null) travel[0] += r.boss().position().distanceTo(last[0]);
            last[0] = r.boss().position();
            if (t.incrementAndGet() > 300) {
                if (travel[0] < 6) h.fail("boss ket (di duoc " + travel[0] + ")");
                else done(h, r);
            }
        });
    }

    /** M3: trong lúc vung (stage 1) hướng mặt khóa dù mục tiêu chạy vòng quanh. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "boss_brain_5")
    public static void brainFacingLocksDuringSwing(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 4));
        AtomicInteger t = new AtomicInteger(), active = new AtomicInteger();
        float[] locked = {Float.NaN};
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            if (i == 5) b.startCombo(b.profile().combo("truong"), r.target());
            if (i < 5) return;
            if (b.hitStage() == 1) {
                float y = b.movement().yaw();
                if (Float.isNaN(locked[0])) locked[0] = y;
                else if (Math.abs(Mth.wrapDegrees(y - locked[0])) > 0.01f) h.fail("xoay trong luc vung: " + locked[0] + " -> " + y);
                active.incrementAndGet();
                double ang = i * 0.6; // mục tiêu chạy vòng 34°/tick quanh boss
                r.target().teleportTo(r.boss().getX() + Math.cos(ang) * 2, r.target().getY(), r.boss().getZ() + Math.sin(ang) * 2);
            } else locked[0] = Float.NaN;
            if (active.get() >= 6) done(h, r);
            else if (i > 280) h.fail("khong thay nhat vung nao");
        });
    }

    /** Chuỗi đòn đứng yên: chạy hết 3 nhát, không rẽ nhánh, nhát nặng cuối gây hit-stop. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "boss_brain_6")
    public static void brainComboCompletes(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 4));
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            Vec3 tv = h.absoluteVec(new Vec3(2, 1, 4));
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            if (i == 5) { r.target().teleportTo(tv.x, tv.y, tv.z); b.startCombo(b.profile().combo("truong"), r.target()); }
            if (i < 5) return;
            if (b.combosCompleted >= 1) {
                if (b.branchesTaken != 0) h.fail("re nhanh khi muc tieu dung yen: " + b.lastBranch);
                else if (b.hitStops < 1) h.fail("nhat nang khong gay hit-stop");
                else done(h, r);
            } else if (i > 280) h.fail("chuoi don khong ket thuc, state=" + b.state() + " combo=" + b.comboId());
        });
    }

    /** Rẽ nhánh M7: sau nhát 1 mục tiêu lùi xa -> nối chuỗi Đâm Lướt, và chuỗi nhánh cũng chạy hết. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "boss_brain_7")
    public static void brainComboBranchesOnRetreat(GameTestHelper h) {
        Rig r = rig(h, new Vec3(2, 1, 2), new Vec3(2, 1, 4));
        AtomicInteger t = new AtomicInteger();
        h.onEachTick(() -> {
            keep(r);
            BossBrain b = r.brain();
            if (b == null) return;
            b.allowEvade = false;
            int i = t.incrementAndGet();
            if (i == 5) b.startCombo(b.profile().combo("truong"), r.target());
            if (i < 5) return;
            if ("truong".equals(b.comboId()) && b.hitStage() == 2) { // nhát 1 đang thu đòn: mục tiêu lùi 6 ô
                Vec3 away = r.target().position().subtract(r.boss().position()).multiply(1, 0, 1).normalize();
                Vec3 to = r.boss().position().add(away.scale(7.5));
                r.target().teleportTo(to.x, r.target().getY(), to.z);
            }
            if ("truong->dam".equals(b.lastBranch) && b.combosCompleted >= 1) done(h, r);
            else if (i > 280) h.fail("khong re nhanh: branch=" + b.lastBranch + " done=" + b.combosCompleted);
        });
    }
}
