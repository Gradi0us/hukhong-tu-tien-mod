package com.hukhong.creature;

import com.hukhong.HuKhong;
import com.hukhong.charcreate.CharCreateService;
import com.hukhong.charcreate.CharRoll;
import com.hukhong.charcreate.Origin;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.world.TreeFell;
import com.hukhong.world.yeulam.DaiSamLam;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 2026-11-06: rừng Hư Không xâm thực, quật đổ cây, Pháp Tu tự chọn nguyên tố. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class VoidForestGameTests {
    private VoidForestGameTests() {}

    @GameTest(template = "empty")
    public static void corruptedPlantIsStronger(GameTestHelper h) {
        var sl = h.getLevel();
        LinhQuai a = DaiSamLam.spawnBeast(sl, CreatureKind.THUC_NHAN_HOA, h.absolutePos(new BlockPos(1, 2, 1)), YeuThuAge.Y100, MobSpawnType.COMMAND);
        LinhQuai b = DaiSamLam.spawnBeast(sl, CreatureKind.THUC_NHAN_HOA, h.absolutePos(new BlockPos(4, 2, 1)), YeuThuAge.Y100, MobSpawnType.COMMAND);
        a.setCorrupted(false);
        b.setCorrupted(true);
        if (!b.corrupted()) { h.fail("khong danh dau xam thuc"); return; }
        if (b.getMaxHealth() * b.toughness() <= a.getMaxHealth() * a.toughness() * 1.5) { h.fail("quai xam thuc khong nhieu mau hon"); return; }
        double atkA = a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double atkB = b.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (atkB <= atkA) { h.fail("quai xam thuc khong manh hon"); return; }
        int bad = 0, total = 0;
        for (int gx = -6; gx <= 6; gx++) for (int gz = -6; gz <= 6; gz++) {
            var r = DaiSamLam.cell(12345L, gx, gz);
            if (r == null) continue;
            total++;
            if (DaiSamLam.corrupted(12345L, r)) bad++;
        }
        if (bad == 0 || bad == total) { h.fail("ti le rung xam thuc sai: " + bad + "/" + total); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void treeFellsButNotHouses(GameTestHelper h) {
        var sl = h.getLevel();
        // cây tự nhiên: thân 4 khối + lá tự mọc
        for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(2, y, 2), Blocks.OAK_LOG);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            h.setBlock(new BlockPos(2 + dx, 5, 2 + dz), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false));
        // "nhà": cột gỗ + lá người chơi đặt (persistent)
        for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(6, y, 2), Blocks.OAK_LOG);
        h.setBlock(new BlockPos(6, 4, 2), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        int n = TreeFell.fellIn(sl, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(0, 1, 0))).inflate(0, 0, 0)
                .expandTowards(8, 3, 4), 10);
        if (!sl.getBlockState(h.absolutePos(new BlockPos(2, 2, 2))).isAir()) { h.fail("cay khong do"); return; }
        if (!sl.getBlockState(h.absolutePos(new BlockPos(6, 2, 2))).is(Blocks.OAK_LOG)) { h.fail("pha ca nha go"); return; }
        if (n != 1) { h.fail("so cay do = " + n); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void phapElementIsChosen(GameTestHelper h) {
        CharRoll r0 = CharRoll.roll(4242L);
        CharRoll roll = new CharRoll(r0.seed(), 1, r0.aptitude(), Origin.VILLAGE, r0.startAge());
        for (PhapElement want : new PhapElement[]{PhapElement.THUY, PhapElement.KIM}) {
            ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-phap-el"));
            p.setData(ModAttachments.CULTIVATION, new CultivationData(1, 0, 100, 100, Set.of(), Map.of(), false, "", roll, false));
            CharCreateService.confirm(p, (1 << Path.PHAP.ordinal()) | ((want.ordinal() + 1) << CharCreateService.ELEMENT_SHIFT));
            if (!p.getData(ModAttachments.CULTIVATION).paths().contains(Path.PHAP)) { h.fail("khong nhan dao Phap"); return; }
            if (PhapTu.element(p) != want) { h.fail("nguyen to " + PhapTu.element(p) + " != " + want); return; }
        }
        ServerPlayer q = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-phap-none"));
        q.setData(ModAttachments.CULTIVATION, new CultivationData(1, 0, 100, 100, Set.of(), Map.of(), false, "", roll, false));
        CharCreateService.confirm(q, 1 << Path.PHAP.ordinal()); // chưa chọn nguyên tố: bị từ chối
        if (!q.getData(ModAttachments.CULTIVATION).paths().isEmpty()) { h.fail("chap nhan Phap khong co nguyen to"); return; }
        h.succeed();
    }
}
