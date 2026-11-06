package com.hukhong.memory;

import com.hukhong.HuKhong;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Ký Ức: nhập bằng chuột phải tiêu item và ghi id, trùng không cộng thêm, đủ tám thì có cờ hoàn thành. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class KyUcGameTests {
    private KyUcGameTests() {}

    private static ServerPlayer player(GameTestHelper h, String name) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        BlockPos abs = h.absolutePos(new BlockPos(2, 2, 2));
        p.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    @GameTest(template = "empty")
    public static void giveAndUseRecordsIdAndConsumes(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-kyuc-use");
        ItemStack st = MemoryRegistry.shard(1);
        if (MemoryRegistry.idOf(st) != 1) { h.fail("mat id tren manh ky uc"); return; }
        if (MemoryService.use(p, st) != MemoryService.Result.OK) { h.fail("chuot phai khong nhap duoc ky uc 1"); return; }
        if (!MemoryService.data(p).has(1)) { h.fail("id 1 chua duoc ghi nhan"); return; }
        if (!st.isEmpty()) { h.fail("manh ky uc chua bi tieu"); return; }
        MemoryScene.cancel(p);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void duplicateNotDoubleCounted(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-kyuc-dup");
        if (MemoryService.record(p, 2) != MemoryService.Outcome.NEW) { h.fail("lan dau phai NEW"); return; }
        if (MemoryService.record(p, 2) != MemoryService.Outcome.DUP) { h.fail("trung phai DUP"); return; }
        if (MemoryService.data(p).count() != 1) { h.fail("trung bi cong don"); return; }
        ItemStack dup = MemoryRegistry.shard(2);
        if (MemoryService.use(p, dup) != MemoryService.Result.ALREADY) { h.fail("nhap lai ky uc da co phai bi tu choi"); return; }
        if (dup.isEmpty()) { h.fail("ky uc trung khong duoc giu lai"); return; }
        MemoryScene.cancel(p);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void eightMemoriesSetCompletionFlag(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-kyuc-full");
        for (int id = 1; id < MemoryLore.COUNT; id++) {
            if (MemoryService.record(p, id) != MemoryService.Outcome.NEW) { h.fail("ky uc " + id + " phai NEW"); return; }
        }
        if (MemoryService.data(p).completed()) { h.fail("chua du 8 ma da hoan thanh"); return; }
        if (MemoryService.record(p, MemoryLore.COUNT) != MemoryService.Outcome.NEW_COMPLETE) { h.fail("ky uc thu 8 phai NEW_COMPLETE"); return; }
        if (!MemoryService.data(p).completed() || MemoryService.data(p).count() != MemoryLore.COUNT) { h.fail("co hoan thanh khong dung"); return; }
        if (MemoryService.record(p, 3) != MemoryService.Outcome.DUP) { h.fail("sau hoan thanh van cong them"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void grantSkipsCollectedAndHeld(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-kyuc-grant");
        MemoryService.record(p, 1);
        MemoryService.give(p, 2); // đang nằm trong túi
        int got = MemoryService.grantUnseen(p, 1); // 1 đã thu thập: không được tặng lại
        if (got == 1 || got == 2 || got == 0) { h.fail("tang trung ky uc da co: " + got); return; }
        MemoryScene.cancel(p);
        h.succeed();
    }
}
