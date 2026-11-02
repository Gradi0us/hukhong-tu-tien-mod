package com.hukhong.dan;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.skill.SkillService;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Đan Tu: độc theo tầng, Hồi Xuân, Đan Hỏa, Cửu Chuyển Kim Đan. Mỗi test có batch riêng để vùng hiệu ứng không chồng nhau. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class DanGameTests {
    private DanGameTests() {}

    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-dan"));
        p.getInventory().clearContent();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(60, 0, 1000, 1000, Set.of(Path.DAN), Map.of(Path.DAN, Aptitude.SSS), false, "", null, false));
        StatService.refresh(p);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
        SkillService.state(p).stance = true;
        Vec3 b = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        p.moveTo(b.x, b.y, b.z, 0f, 0f); // yaw 0 = nhìn +Z
        return p;
    }

    /** Zombie đứng yên, máu {@code hp} (đủ lớn để không chết, đủ nhỏ để float không làm tròn sát thương), cách {@code ahead} block phía trước, lệch {@code side}. */
    static Zombie zombie(GameTestHelper h, ServerPlayer p, double ahead, double side, double hp) {
        Zombie z = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 at = p.position().add(side, 0.7, ahead);
        z.moveTo(at.x, at.y, at.z);
        z.setNoGravity(true);
        z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        z.setHealth((float) hp);
        z.setPersistenceRequired();
        h.getLevel().addFreshEntity(z);
        return z;
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "dan_poison_stack")
    public static void poisonStacks(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie z = zombie(h, p, 3, 0, 10000);
        for (int i = 0; i < 3; i++) DanRuntime.basicHit(p, z); // 3 đòn thường, mỗi viên nổ +1 tầng
        h.runAfterDelay(20, () -> {
            int st = DanRuntime.stacks(z);
            if (st != 3) h.fail("3 Bạo Đan phải cho 3 tầng độc, được " + st);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = "dan_poison_cap")
    public static void poisonCaps10(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie z = zombie(h, p, 3, 0, 10000);
        for (int i = 0; i < 4; i++) DanRuntime.addPoison(p, z, 3); // 12 tầng cộng dồn
        int st = DanRuntime.stacks(z);
        if (st != DanMath.MAX_STACKS) h.fail("Độc phải kẹp ở " + DanMath.MAX_STACKS + " tầng, được " + st);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "dan_heal")
    public static void hoiXuanHealsAndCleanses(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setHealth(5f);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        float before = p.getHealth();
        SkillService.cast(p, 1); // E Hồi Xuân Đan
        if (p.hasEffect(MobEffects.POISON)) h.fail("Hồi Xuân Đan không xóa hiệu ứng xấu (Đầu độc)");
        if (p.getHealth() <= before) h.fail("Hồi Xuân Đan không hồi máu: " + before + " -> " + p.getHealth());
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "dan_fire")
    public static void phanThienScales(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie full = zombie(h, p, 4, 0, 10000);     // 5 tầng độc: hệ số 1 + 0,15 x 5 = 1,75
        Zombie none = zombie(h, p, 4, 1.5, 10000);   // không có độc: hệ số 1 (vẫn trong nón)
        com.hukhong.gametest.GameTestSupport.noCrit(p); // bạo kích ngẫu nhiên làm lệch tỉ lệ
        DanRuntime.addPoison(p, full, 5);
        float hf = full.getHealth(), hn = none.getHealth();
        SkillService.cast(p, 2); // R Đan Hỏa Phần Thiên
        float lossFull = hf - full.getHealth(), lossNone = hn - none.getHealth();
        if (lossNone <= 0f) h.fail("Đan Hỏa không gây sát thương lên mục tiêu không độc (mất " + lossNone + ")");
        if (lossFull <= lossNone * 1.7f) h.fail("Đan Hỏa 5 tầng phải > 1,7 lần 0 tầng: " + lossFull + " vs " + lossNone);
        if (DanRuntime.stacks(full) != 0) h.fail("Đan Hỏa phải tiêu hao độc, còn " + DanRuntime.stacks(full) + " tầng");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "dan_kim")
    public static void kimDanFree(GameTestHelper h) {
        ServerPlayer p = player(h);
        SkillService.cast(p, 3); // T Cửu Chuyển Kim Đan (trả phí bình thường)
        SkillService.state(p).cooldownEnd[0] = 0;
        int before = p.getData(ModAttachments.CULTIVATION).resource();
        SkillService.cast(p, 0); // Q trong cửa sổ Kim Đan
        int after = p.getData(ModAttachments.CULTIVATION).resource();
        if (SkillService.state(p).cooldownEnd[0] <= h.getLevel().getGameTime()) h.fail("Q không thi triển trong Kim Đan (không đặt hồi chiêu)");
        if (after != before) h.fail("Trong Kim Đan Q vẫn tốn tài nguyên: " + before + " -> " + after);
        h.succeed();
    }
}
