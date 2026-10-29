package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.flight.FlightBans;
import com.hukhong.passive.TrongThuong;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Trọng Thương: tối đa 3 tầng, xử tử dưới ngưỡng máu, boss không bị xử tử. Đòn thật qua hurt() -> PathRules/DamageRules/TrongThuong. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class TrongThuongGameTests {
    private TrongThuongGameTests() {}

    private static ServerPlayer the(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-tt"));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(60, 0, 0, 2000, Set.of(Path.THE), Map.of(Path.THE, Aptitude.SSS), false));
        StatService.refresh(p);
        GameTestSupport.noCrit(p);
        var ap = p.getAttribute(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.ATTACK_POWER));
        if (ap != null) ap.setBaseValue(0); // don thuong ~1 sat thuong: xu tu moi la thu giet
        p.setInvulnerable(false);
        return p;
    }

    private static Zombie zombie(GameTestHelper h, double maxHp, double hp) {
        Zombie z = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        z.moveTo(v.x, v.y, v.z, 0f, 0f);
        z.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(z.blockPosition()), MobSpawnType.COMMAND, null);
        z.setPersistenceRequired();
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);
        GameTestSupport.deterministic(z);
        z.setHealth((float) hp);
        h.getLevel().addFreshEntity(z);
        return z;
    }

    private static void basicHit(GameTestHelper h, ServerPlayer p, Zombie z) {
        z.invulnerableTime = 0;
        z.hurt(h.getLevel().damageSources().playerAttack(p), 1f);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void trongThuongStacksCapAtThree(GameTestHelper h) {
        ServerPlayer p = the(h);
        Zombie z = zombie(h, 1000, 1000);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) { basicHit(h, p, z); sb.append(TrongThuong.stacks(z)).append('/').append(z.getHealth()).append(' '); }
        int n = TrongThuong.stacks(z);
        z.discard();
        if (n != 3) h.fail("tang Trong Thuong phai = 3 (toi da), thuc te " + n + " " + sb);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void trongThuongExecutesBelowThreshold(GameTestHelper h) {
        ServerPlayer p = the(h);
        // SSS: xu tu duoi 20% = 200; HP 150 -> mot don thuong (vai chuc sat thuong) khong the tu giet
        Zombie z = zombie(h, 1000, 150);
        basicHit(h, p, z);
        boolean dead = !z.isAlive() || z.getHealth() <= 0f;
        float hp = z.getHealth();
        z.discard();
        if (!dead) h.fail("phai bi xu tu (HP con " + hp + " < 20% max)");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void trongThuongDoesNotExecuteAboveThresholdOrBoss(GameTestHelper h) {
        ServerPlayer p = the(h);
        Zombie hi = zombie(h, 1000, 600);
        basicHit(h, p, hi);
        boolean hiAlive = hi.isAlive();
        hi.discard();
        Zombie boss = zombie(h, 1000, 150);
        boss.addTag(FlightBans.WORLD_BOSS_TAG);
        basicHit(h, p, boss);
        boolean bossAlive = boss.isAlive();
        boss.discard();
        if (!hiAlive) h.fail("tren nguong khong duoc bi xu tu");
        if (!bossAlive) h.fail("boss khong duoc bi xu tu");
        h.succeed();
    }
}
