package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.chg.ChapHanhGia;
import com.hukhong.chg.ChgRegistry;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.endgame.NguyenThuyTichDiet;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuVuong;
import com.hukhong.entity.ModEntities;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mot nguoi choi (khong phai FakePlayer bat tu) gay sat thuong that len boss: HP phai giam. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class BossDamageGameTests {
    private BossDamageGameTests() {}

    private static ServerPlayer attacker(GameTestHelper h, Path path) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-boss-hit"));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(60, 0, 2000, 2000, Set.of(path), Map.<Path, Aptitude>of(), false));
        StatService.refresh(p);
        p.setHealth(p.getMaxHealth());
        p.setInvulnerable(false);
        return p;
    }

    private static <T extends Mob> T put(GameTestHelper h, T m) {
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        m.moveTo(v.x, v.y, v.z, 0f, 0f);
        m.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
        m.setPersistenceRequired();
        h.getLevel().addFreshEntity(m);
        return m;
    }

    private static String dbg(LivingEntity b) {
        StringBuilder sb = new StringBuilder("removed=" + b.getRemovalReason() + " pos=" + b.position() + " tick=" + b.tickCount + " invT=" + b.invulnerableTime + " inv=" + b.isInvulnerable());
        for (String f : new String[]{"intro", "invulTicks", "windup", "pending"}) {
            try { var fl = b.getClass().getDeclaredField(f); fl.setAccessible(true); sb.append(' ').append(f).append('=').append(fl.get(b)); } catch (Exception ex) { }
        }
        return sb.toString();
    }

    /** Doi {@code wait} tick (qua mo man/giang lam), danh 10 dong, kiem HP giam. */
    private static void hitCheck(GameTestHelper h, LivingEntity boss, int wait, String label) {
        ServerPlayer p = attacker(h, Path.KIEM);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 4));
        p.setPos(v.x, v.y, v.z);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        h.runAfterDelay(wait, () -> {
            float before = boss.getHealth();
            for (int i = 0; i < 10; i++) {
                boss.invulnerableTime = 0;
                boss.hurt(h.getLevel().damageSources().playerAttack(p), 200f);
            }
            float after = boss.getHealth();
            String info = dbg(boss);
            boss.discard();
            if (!(after < before)) h.fail(label + ": HP khong giam " + before + " -> " + after + " max=" + boss.getMaxHealth()
                    + " design=" + HuHoa.designHp(boss) + " ehp=" + HuHoa.ehpScale(boss) + " dbg=" + info);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "bossdmg")
    public static void playerDamagesNguyenThuyTichDiet(GameTestHelper h) {
        NguyenThuyTichDiet b = put(h, EndgameRegistry.BOSS.get().create(h.getLevel()));
        b.begin(h.getLevel());
        hitCheck(h, b, com.hukhong.endgame.EndgameKit.INTRO_TICKS + 40, "NTD");
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "bossdmg")
    public static void playerDamagesHuVuong(GameTestHelper h) {
        HuVuong b = put(h, ModEntities.HU_VUONG.get().create(h.getLevel()));
        hitCheck(h, b, 20, "HuVuong");
    }

    @GameTest(template = "empty", timeoutTicks = 600, batch = "bossdmg")
    public static void playerDamagesChg(GameTestHelper h) {
        ChapHanhGia b = put(h, ChgRegistry.type(1).create(h.getLevel()));
        b.beginDescent();
        hitCheck(h, b, 200, "CHG1");
    }

    // ------------------------------------------------------------ ky nang khong ket windup (isImmobile vs customServerAiStep)

    private static int intField(Object o, String name) {
        try { var f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.getInt(o); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** Boss co muc tieu: phai vao windup (pending >= 0) roi tha chieu (pending ve -1). Truoc day windup ket vinh vien. */
    private static void skillCompletes(GameTestHelper h, Mob boss, String label) {
        ServerPlayer p = attacker(h, Path.KIEM);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 4));
        p.setPos(v.x, v.y, v.z);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        boolean[] seen = {false};
        h.onEachTick(() -> {
            boss.setTarget(p);
            int pending = intField(boss, "pending");
            if (pending >= 0) seen[0] = true;
            else if (seen[0]) { boss.discard(); h.succeed(); }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 700, batch = "bossdmg")
    public static void nguyenThuyTichDietSkillCompletes(GameTestHelper h) {
        NguyenThuyTichDiet b = put(h, EndgameRegistry.BOSS.get().create(h.getLevel()));
        b.begin(h.getLevel());
        skillCompletes(h, b, "NTD");
    }

    @GameTest(template = "empty", timeoutTicks = 700, batch = "bossdmg")
    public static void chgSkillCompletes(GameTestHelper h) {
        ChapHanhGia b = put(h, ChgRegistry.type(1).create(h.getLevel()));
        b.beginDescent();
        skillCompletes(h, b, "CHG1");
    }
    /**
     * NTTD 2 dang: spawn qua luong that (EndgameService.spawnBoss) phai ra DANG 1; danh het mau dang 1 -> khong chet ma bien hinh;
     * sau cutscene phai o DANG 2, pha 1, day mau (lon hon mau dang 1).
     */
    @GameTest(template = "empty", timeoutTicks = 700, batch = "bossdmg")
    public static void nguyenThuyTichDietReachesForm2(GameTestHelper h) {
        NguyenThuyTichDiet b = com.hukhong.endgame.EndgameService.spawnBoss(h.getLevel(), h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)));
        if (b == null) { h.fail("spawnBoss null"); return; }
        if (b.form() != 1) h.fail("spawn khong ra dang 1: form=" + b.form());
        double f1Max = HuHoa.designHp(b);
        h.runAfterDelay(com.hukhong.endgame.EndgameKit.INTRO_TICKS + 10, () -> {
            for (int i = 0; i < 200 && !b.morphing() && b.isAlive(); i++) {
                b.invulnerableTime = 0;
                b.hurt(h.getLevel().damageSources().magic(), 1.0e7f);
            }
            if (!b.isAlive()) { h.fail("dang 1 chet that thay vi bien hinh"); return; }
            if (!b.morphing()) { h.fail("khong bat dau bien hinh, hp=" + b.getHealth() + " dbg=" + dbg(b)); return; }
            h.runAfterDelay(NguyenThuyTichDiet.MORPH_TICKS + 10, () -> {
                String info = "form=" + b.form() + " phase=" + b.phase() + " hp=" + b.getHealth() + "/" + b.getMaxHealth() + " design=" + HuHoa.designHp(b) + " f1Design=" + f1Max + " alive=" + b.isAlive();
                boolean ok = b.isAlive() && b.form() == 2 && b.phase() == 1 && !b.morphing()
                        && HuHoa.designHp(b) > f1Max * 2 && b.getHealth() >= b.getMaxHealth() * 0.99f && b.getBbHeight() > 5f;
                b.discard();
                if (!ok) h.fail("khong vao dang 2: " + info);
                h.succeed();
            });
        });
    }
}
