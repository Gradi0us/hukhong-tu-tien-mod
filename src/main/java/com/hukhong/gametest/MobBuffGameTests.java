package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.entity.BangNhan;
import com.hukhong.entity.DocMau;
import com.hukhong.entity.HuAnh;
import com.hukhong.entity.HuCot;
import com.hukhong.entity.HuGiap;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuThi;
import com.hukhong.entity.HuTrieuSu;
import com.hukhong.entity.HuTrung;
import com.hukhong.entity.ModEntities;
import com.hukhong.entity.PoisonSpit;
import com.hukhong.entity.SaHatVuong;
import com.hukhong.entity.V3Entities;
import com.hukhong.entity.WorldBoss;
import com.hukhong.events.BossBuff;
import com.hukhong.events.CorruptBuff;
import com.hukhong.events.CorruptionData;
import com.hukhong.events.VoidRaid;
import com.hukhong.events.WorldBossManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Quai/boss moi (v3): sinh khong loi, chay het ky nang, buff boss +30%, dot kich Hu Khong, buff vung nhiem. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class MobBuffGameTests {
    private MobBuffGameTests() {}

    private static final String B = "mobbuff";

    private static ServerPlayer target(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-mobbuff"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        p.setInvulnerable(false);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 5));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    private static <T extends Mob> T put(GameTestHelper h, EntityType<T> type, Vec3 rel) {
        T m = type.create(h.getLevel());
        Vec3 v = h.absoluteVec(rel);
        m.moveTo(v.x, v.y, v.z, 0f, 0f);
        m.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
        m.setPersistenceRequired();
        h.getLevel().addFreshEntity(m);
        return m;
    }

    /** Sinh quai, cho no co muc tieu ~5 s: moi ky nang/goal chay, khong exception, cap >= 1. */
    private static void mobRuns(GameTestHelper h, EntityType<? extends Mob> type) {
        ServerPlayer p = target(h);
        Mob m = put(h, type, new Vec3(2, 1, 1));
        if (!(m instanceof HuMob hm) || hm.huLevel() < 1) h.fail("thieu cap Hu: " + type);
        h.onEachTick(() -> { if (m.isAlive()) m.setTarget(p); });
        h.runAfterDelay(110, () -> {
            boolean ok = m.isAlive() || m.isRemoved();
            m.discard();
            if (!ok) h.fail("quai o trang thai la");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void huAnhSpawnsAndRuns(GameTestHelper h) { mobRuns(h, V3Entities.HU_ANH.get()); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void huGiapSpawnsAndRuns(GameTestHelper h) { mobRuns(h, V3Entities.HU_GIAP.get()); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void huTrungSpawnsAndRuns(GameTestHelper h) { mobRuns(h, V3Entities.HU_TRUNG.get()); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void huTrieuSuSpawnsAndRuns(GameTestHelper h) { mobRuns(h, V3Entities.HU_TRIEU_SU.get()); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void huThiNewSkillsRun(GameTestHelper h) { mobRuns(h, ModEntities.HU_THI.get()); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void huChuNewSkillsRun(GameTestHelper h) { mobRuns(h, ModEntities.HU_CHU.get()); }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void huCotSpitsInsteadOfArrows(GameTestHelper h) {
        HuCot c = put(h, ModEntities.HU_COT.get(), new Vec3(2, 1, 1));
        if (!c.getMainHandItem().isEmpty()) h.fail("Hu Cot khong duoc cam cung: " + c.getMainHandItem());
        boolean bow = c.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof RangedBowAttackGoal<?>);
        if (bow) h.fail("Hu Cot con goal ban cung");
        ServerPlayer p = target(h);
        PoisonSpit s = PoisonSpit.launch(c, p, 4f, 2.5);
        if (s.damage() <= 0 || s.isRemoved()) h.fail("nuoc bot doc khong phong ra");
        h.runAfterDelay(60, () -> {
            c.discard();
            s.discard();
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void huGiapShieldBlocksFrontOnly(GameTestHelper h) {
        HuGiap g = put(h, V3Entities.HU_GIAP.get(), new Vec3(2, 1, 2));
        g.setYRot(0f);
        g.setYHeadRot(0f);
        Vec3 here = g.position();
        if (!g.blocks(here.add(0, 0, 5))) h.fail("khien phai chan don tu phia truoc");
        if (g.blocks(here.add(0, 0, -5))) h.fail("khien khong duoc chan don tu phia sau");
        g.discard();
        h.succeed();
    }

    /** Boss the gioi: ep vao pha 2 roi chay tung ky nang qua windup -> fire, khong exception, pha 2 duoc kich hoat. */
    private static void bossRunsAllSkills(GameTestHelper h, EntityType<? extends WorldBoss> type, int kind, int skills) {
        ServerPlayer p = target(h);
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 1));
        WorldBoss b = WorldBossManager.spawn(h.getLevel(), kind, at);
        if (b == null || b.getType() != type) h.fail("khong sinh duoc boss kind=" + kind);
        b.setPersistenceRequired();
        b.setHealth(b.getMaxHealth() * 0.4f);
        h.onEachTick(() -> { if (b.isAlive()) b.setTarget(p); });
        for (int i = 0; i < skills; i++) {
            int sk = i;
            h.runAfterDelay(30 + i * 70L, () -> b.begin(h.getLevel(), sk, p));
        }
        h.runAfterDelay(30 + skills * 70L + 20, () -> {
            boolean alive = b.isAlive();
            int phase = b.phase();
            b.discard();
            if (!alive) h.fail("boss chet bat thuong");
            if (phase != 2) h.fail("boss chua vao pha 2 (phase=" + phase + ")");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500, batch = B)
    public static void saHatVuongRunsAllSkills(GameTestHelper h) { bossRunsAllSkills(h, V3Entities.SA_HAT_VUONG.get(), WorldBossManager.DESERT, 4); }

    @GameTest(template = "empty", timeoutTicks = 500, batch = B)
    public static void bangNhanRunsAllSkills(GameTestHelper h) { bossRunsAllSkills(h, V3Entities.BANG_NHAN.get(), WorldBossManager.SNOW, 4); }

    @GameTest(template = "empty", timeoutTicks = 500, batch = B)
    public static void docMauRunsAllSkills(GameTestHelper h) { bossRunsAllSkills(h, V3Entities.DOC_MAU.get(), WorldBossManager.SWAMP, 4); }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void bossBuffMathAndDetection(GameTestHelper h) {
        if (BossBuff.cd(100) != 77) h.fail("cd(100) phai = 77 (~30% thuong xuyen hon), thuc te " + BossBuff.cd(100));
        if (BossBuff.cd(1) < 1) h.fail("cd toi thieu 1");
        if (BossBuff.DMG_MUL != 1.3) h.fail("DMG_MUL");
        WorldBoss wb = put(h, V3Entities.DOC_MAU.get(), new Vec3(2, 1, 2));
        HuGiap g = put(h, V3Entities.HU_GIAP.get(), new Vec3(4, 1, 2));
        if (!BossBuff.isBoss(wb)) h.fail("WorldBoss phai la boss");
        if (BossBuff.isBoss(g)) h.fail("Hu Giap thuong khong phai boss");
        VoidRaid.makeMiniBoss(g);
        if (!BossBuff.isBoss(g)) h.fail("mini-boss dot kich phai duoc nhan la boss");
        if (BossBuff.cdFor(g, 100) != 77 || BossBuff.cdFor(put(h, ModEntities.HU_THI.get(), new Vec3(6, 1, 2)), 100) != 100)
            h.fail("hoi chieu: boss 77, quai thuong 100");
        wb.discard();
        g.discard();
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void raidRulesPure(GameTestHelper h) {
        if (VoidRaid.chance(false, false) != 0.45) h.fail("co ban 45%");
        if (VoidRaid.chance(false, true) != 0.20) h.fail("rung 20%");
        if (VoidRaid.chance(true, true) != 0.70 || VoidRaid.chance(true, false) != 0.70) h.fail("vung nhiem 70%");
        if (!VoidRaid.isNight(14000) || VoidRaid.isNight(1000) || VoidRaid.isNight(12000)) h.fail("isNight");
        if (WorldBossManager.kindOf("desert") != 0 || WorldBossManager.kindOf("eroded_badlands") != 0) h.fail("kind sa mac");
        if (WorldBossManager.kindOf("snowy_slopes") != 1 || WorldBossManager.kindOf("frozen_peaks") != 1) h.fail("kind tuyet");
        if (WorldBossManager.kindOf("mangrove_swamp") != 2 || WorldBossManager.kindOf("jungle") != 2) h.fail("kind dam lay");
        if (WorldBossManager.kindOf("plains") != -1) h.fail("kind khac");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void raidStartsAndCleansUp(GameTestHelper h) {
        ServerPlayer p = target(h);
        VoidRaid.clear();
        if (!VoidRaid.start(p)) h.fail("start dot kich that bai");
        if (!VoidRaid.active(p.getUUID())) h.fail("dot kich khong active");
        if (VoidRaid.start(p)) h.fail("khong duoc bat dau dot thu hai cung luc");
        h.runAfterDelay(40, () -> {
            VoidRaid.clear();
            if (VoidRaid.activeCount() != 0) h.fail("clear khong don het");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void corruptedChunkBuffsMobs(GameTestHelper h) {
        if (CorruptBuff.level(true, 10) != 13 || CorruptBuff.level(false, 10) != 10) h.fail("cap +30%");
        if (CorruptBuff.mul(true) != 1.3 || CorruptBuff.mul(false) != 1.0) h.fail("mul");
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        CorruptionData cd = CorruptionData.get(h.getLevel().getServer().overworld());
        long key = ChunkPos.asLong(at.getX() >> 4, at.getZ() >> 4);
        boolean had = cd.chunks().contains(key);
        try {
            cd.chunks().add(key);
            if (!CorruptBuff.at(h.getLevel(), at)) h.fail("chunk nhiem phai bi nhan dien");
            HuThi z = put(h, ModEntities.HU_THI.get(), new Vec3(2, 1, 2));
            // HP thiet ke = base * hpMul * hpScale(cap) * 1.3 (nhiem) -> phai cao hon cung cap khong nhiem
            double expect = 20.0 * 1.45 * 1.3 * HuHoa.hpScale(z.huLevel());
            double design = HuHoa.designHp(z);
            z.discard();
            if (design < expect * 0.99) h.fail("HP thiet ke " + design + " < ky vong nhiem " + expect);
        } finally {
            if (!had) cd.chunks().remove(key);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void raidWavesSpawnWithMiniBoss(GameTestHelper h) {
        ServerPlayer p = target(h);
        for (int stage = 1; stage <= VoidRaid.WAVES; stage++) {
            java.util.List<Mob> mobs = VoidRaid.debugWave(p, stage);
            if (mobs.isEmpty()) h.fail("dot " + stage + " khong sinh quai nao");
            boolean boss = mobs.stream().anyMatch(BossBuff::isBoss);
            if (stage == VoidRaid.WAVES && !boss) h.fail("dot cuoi phai co mini-boss");
            if (stage < VoidRaid.WAVES && boss) h.fail("dot " + stage + " khong duoc co mini-boss");
            mobs.forEach(Mob::discard);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B)
    public static void poisonSpitHitsAndPoisons(GameTestHelper h) {
        HuCot c = put(h, ModEntities.HU_COT.get(), new Vec3(2, 1, 1));
        net.minecraft.world.entity.animal.Cow cow = put(h, EntityType.COW, new Vec3(2, 1, 6));
        cow.setNoAi(true);
        PoisonSpit.launch(c, cow, 1f, 2.5);
        h.runAfterDelay(80, () -> {
            boolean poisoned = cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON);
            c.discard();
            cow.discard();
            if (!poisoned) h.fail("nuoc bot doc khong gay doc");
            h.succeed();
        });
    }
}
