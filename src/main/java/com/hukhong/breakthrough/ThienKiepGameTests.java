package com.hukhong.breakthrough;

import com.hukhong.HuKhong;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Thiên Kiếp: bắt đầu kiếp, thất bại không ghi cờ, thành công ghi cờ qua mốc. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ThienKiepGameTests {
    private ThienKiepGameTests() {}

    private static ServerPlayer fake(GameTestHelper h, String name) {
        return FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
    }

    @GameTest(template = "empty")
    public static void kiepStartsAndFailsWithoutPass(GameTestHelper h) {
        ServerPlayer p = fake(h, "hk-kiep-fail");
        if (!ThienKiep.begin(p, 1)) { h.fail("khong bat dau duoc kiep"); return; }
        if (!ThienKiep.active(p)) { h.fail("khong co phien kiep dang chay"); return; }
        if (ThienKiep.begin(p, 1)) { h.fail("mo 2 kiep cung luc"); return; }
        ThienKiep.fail(p, "hukhong.kiep.reason.death", false);
        if (ThienKiep.active(p)) { h.fail("kiep khong ket thuc sau khi that bai"); return; }
        if (ThienKiep.isPassed(p, 1)) { h.fail("that bai nhung van ghi co qua kiep"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void kiepSuccessSetsPassFlag(GameTestHelper h) {
        ServerPlayer p = fake(h, "hk-kiep-pass");
        if (ThienKiep.isPassed(p, 1)) { h.fail("co co qua kiep truoc khi thu"); return; }
        if (!ThienKiep.begin(p, 1)) { h.fail("khong bat dau duoc kiep"); return; }
        ThienKiep.succeed(p);
        if (!ThienKiep.isPassed(p, 1)) { h.fail("thanh cong nhung khong ghi co"); return; }
        if (ThienKiep.active(p)) { h.fail("kiep van chay sau khi qua"); return; }
        if (ThienKiep.isPassed(p, 2)) { h.fail("qua mốc 1 ma tinh la qua mốc 2"); return; }
        h.succeed();
    }
}
