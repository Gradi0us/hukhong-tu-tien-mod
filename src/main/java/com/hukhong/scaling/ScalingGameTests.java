package com.hukhong.scaling;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;

@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ScalingGameTests {
    private ScalingGameTests() {}

    @GameTest(template = "empty")
    public static void linhTinhTangAttackPower(GameTestHelper h) {
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "hukhong-sc"));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(1, 0, 100, 100,
                Set.of(com.hukhong.core.Path.KIEM), Map.of(), false));
        StatService.refresh(p);
        double before = StatService.attackPower(p);
        p.getPersistentData().putDouble(Difficulty.POWER, 6.0);
        ScalingEvents.applyCrystals(p);
        double after = StatService.attackPower(p);
        if (Math.abs(after - before - 6.0) > 1e-6) h.fail("attack_power " + before + " -> " + after + " (ky vong +6)");
        StatService.refresh(p); // refresh khong duoc xoa bonus
        if (Math.abs(StatService.attackPower(p) - before - 6.0) > 1e-6) h.fail("refresh lam mat bonus Linh Tinh");
        h.succeed();
    }
}
