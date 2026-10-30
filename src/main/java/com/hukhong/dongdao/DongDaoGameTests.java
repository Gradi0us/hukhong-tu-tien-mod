package com.hukhong.dongdao;

import com.hukhong.HuKhong;
import com.hukhong.travel.TravelRules;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Dong Dao: dich chuyen thanh cong giua 2 nguoi choi gia, hoi chieu 30s, chan khi chien dau. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class DongDaoGameTests {
    private DongDaoGameTests() {}

    private static ServerPlayer fake(GameTestHelper h, String name) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
        DongDao.resetCooldown(p.getUUID());
        return p;
    }

    @GameTest(template = "empty")
    public static void dongDaoCombatRefused(GameTestHelper h) {
        ServerPlayer a = fake(h, "hk-dd1"), b = fake(h, "hk-dd2");
        String ok = DongDao.validate(a, b);
        if (ok != null) { h.fail("fresh players must be allowed, got " + ok); return; }
        TravelRules.markCombat(a);
        if (!"hukhong.cal.err_combat".equals(DongDao.validate(a, b))) { h.fail("combat must refuse"); return; }
        if (!"hukhong.dongdao.err_target".equals(DongDao.validate(a, a))) { h.fail("self target must be refused"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dongDaoTeleportAndCooldown(GameTestHelper h) {
        ServerPlayer a = fake(h, "hk-dd3"), b = fake(h, "hk-dd4");
        var at = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        b.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        a.setPos(at.getX() + 100.5, at.getY(), at.getZ() + 0.5);
        String err = DongDao.go(a, b);
        if (err != null) { h.fail("teleport refused: " + err); return; }
        if (a.position().distanceTo(b.position()) > 14) { h.fail("did not land near target: " + a.position()); return; }
        if (!"hukhong.dongdao.err_cooldown".equals(DongDao.validate(a, b))) { h.fail("30s cooldown must apply after success"); return; }
        DongDao.resetCooldown(a.getUUID());
        if (DongDao.validate(a, b) != null) { h.fail("after cooldown reset must be allowed"); return; }
        h.succeed();
    }
}
