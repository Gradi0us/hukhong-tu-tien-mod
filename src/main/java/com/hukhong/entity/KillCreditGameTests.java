package com.hukhong.entity;

import com.hukhong.HuKhong;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.LinhQuai;
import com.hukhong.creature.YeuThuAge;
import com.hukhong.world.yeulam.DaiSamLam;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 2026-11-06: chia EXP - một mình / hỗ trợ / lính thủ vệ kết liễu. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class KillCreditGameTests {
    private KillCreditGameTests() {}

    static ServerPlayer pl(GameTestHelper h, String n) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), n));
        p.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        return p;
    }

    static LinhQuai mob(GameTestHelper h) {
        return DaiSamLam.spawnBeast(h.getLevel(), CreatureKind.COMMON_BEASTS.get(0), h.absolutePos(new BlockPos(3, 2, 3)), YeuThuAge.Y10, MobSpawnType.COMMAND);
    }

    @GameTest(template = "empty")
    public static void soloAssistAndGuardKill(GameTestHelper h) {
        ServerPlayer a = pl(h, "kc-a"), b = pl(h, "kc-b"), c = pl(h, "kc-c");
        LinhQuai m1 = mob(h); // một mình
        KillCredit.record(m1, a, 50);
        var s1 = KillCredit.shares(m1, a);
        if (s1.size() != 1 || s1.get(a) != 1.0) { h.fail("solo sai: " + s1); return; }
        LinhQuai m2 = mob(h); // a giết, b hỗ trợ 40%, c chỉ quẹt 2%
        KillCredit.record(m2, a, 58); KillCredit.record(m2, b, 40); KillCredit.record(m2, c, 2);
        var s2 = KillCredit.shares(m2, a);
        if (s2.get(a) != 1.0 || s2.get(b) == null || s2.get(b) < 0.45 || s2.get(b) > 0.55 || s2.containsKey(c)) { h.fail("ho tro sai: " + s2); return; }
        LinhQuai m3 = mob(h); // lính thủ vệ kết liễu: b gây nhiều nhất
        KillCredit.record(m3, a, 20); KillCredit.record(m3, b, 70);
        var s3 = KillCredit.shares(m3, null);
        if (s3.get(b) == null || s3.get(b) != 1.0 || s3.get(a) == null || s3.get(a) >= 1.0) { h.fail("thu ve ks sai: " + s3); return; }
        LinhQuai m4 = mob(h); // không người chơi nào đánh: không ai nhận
        if (!KillCredit.shares(m4, null).isEmpty()) { h.fail("khong ai danh ma van co exp"); return; }
        for (LinhQuai m : new LinhQuai[]{m1, m2, m3, m4}) m.discard();
        h.succeed();
    }
}
