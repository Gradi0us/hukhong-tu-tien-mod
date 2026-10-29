package com.hukhong.currency;

import com.hukhong.HuKhong;
import com.hukhong.artifact.ArtifactService;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.entity.ModEntities;
import com.hukhong.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Hon Tinh la tien trong vi: boss chet cong vi (khong roi vat pham), nang cap tru vi, nhat / trong tui tu doi vao vi. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class HonTinhGameTests {
    private HonTinhGameTests() {}

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-ht"));
        p.getInventory().clearContent();
        p.setPos(h.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        Wallet.setHonTinh(p, 0);
        return p;
    }

    private static int honTinhItems(GameTestHelper h) {
        int n = 0;
        for (ItemEntity ie : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(64)))
            if (ie.getItem().is(ModItems.HON_TINH.get())) n += ie.getItem().getCount();
        return n;
    }

    @GameTest(template = "empty")
    public static void bossChetCongViKhongRoiVatPham(GameTestHelper h) {
        ServerPlayer p = player(h);
        var boss = h.spawn(ModEntities.HU_VUONG.get(), new BlockPos(4, 2, 2));
        boss.die(h.getLevel().damageSources().playerAttack(p));
        if (Wallet.honTinh(p) < 8) h.fail("vi Hon Tinh khong tang: " + Wallet.honTinh(p));
        if (honTinhItems(h) != 0) h.fail("van con vat pham Hon Tinh roi ra dat: " + honTinhItems(h));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nangCapTruVi(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setData(ModAttachments.CULTIVATION, new CultivationData(100, 0, 4000, 4000, Set.of(Path.DAO), Map.<Path, Aptitude>of(), false));
        int cost = com.hukhong.artifact.ArtifactMath.tinhCost(1);
        if (ArtifactService.tryUpgrade(p, Path.DAO)) h.fail("nang cap duoc khi vi rong");
        Wallet.setHonTinh(p, 100);
        p.getInventory().add(new ItemStack(ModItems.HON_TINH.get(), 50)); // vat pham KHONG duoc tinh
        if (!ArtifactService.tryUpgrade(p, Path.DAO)) h.fail("khong nang cap duoc du vi co 100");
        if (Wallet.honTinh(p) != 100 - cost) h.fail("vi con " + Wallet.honTinh(p) + " != " + (100 - cost));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nhatVaTuiTuDoiVaoVi(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemEntity ie = new ItemEntity(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(ModItems.HON_TINH.get(), 7));
        ie.setNoPickUpDelay();
        h.getLevel().addFreshEntity(ie);
        ie.playerTouch(p);
        if (Wallet.honTinh(p) != 7) h.fail("nhat item khong vao vi: " + Wallet.honTinh(p));
        if (!ie.isRemoved()) h.fail("item chua bi huy sau khi nhat");
        p.getInventory().add(new ItemStack(ModItems.HON_TINH.get(), 5)); // save cu / ruong
        if (Wallet.convertHonTinhInventory(p) != 5 || Wallet.honTinh(p) != 12) h.fail("quet tui sai: " + Wallet.honTinh(p));
        if (p.getInventory().contains(new ItemStack(ModItems.HON_TINH.get()))) h.fail("tui van con Hon Tinh");
        h.succeed();
    }
}
