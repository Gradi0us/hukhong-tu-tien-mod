package com.hukhong.banmenh;

import com.hukhong.HuKhong;
import com.hukhong.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Bản Mệnh Kiếm: không độ bền, không thể vứt (Q / Ctrl+Q / GUI / ItemEntity), tự khôi phục khi mất. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class BanMenhGameTests {
    private BanMenhGameTests() {}

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-bm"));
        p.getInventory().clearContent();
        BanMenhData d = BanMenhData.EMPTY.withType(BanMenhType.values()[2].id(), p.getUUID()).withStage(3);
        p.setData(BanMenhAttachments.BAN_MENH.get(), d);
        return p;
    }

    private static int swords(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(ModItems.BAN_MENH_KIEM.get())) n++;
        return n;
    }

    private static int entities(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(64),
                e -> e.getItem().is(ModItems.BAN_MENH_KIEM.get())).size();
    }

    @GameTest(template = "empty")
    public static void kiemKhongDoBen(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack s = BanMenhService.create(p.getData(BanMenhAttachments.BAN_MENH.get()));
        if (s.isDamageableItem() || s.getMaxDamage() != 0 || s.isBarVisible()) h.fail("kiem con do ben");
        s.set(DataComponents.DAMAGE, 5); // save cu
        s.hurtAndBreak(500, h.getLevel(), p, i -> h.fail("kiem bi vo"));
        if (s.isEmpty()) h.fail("kiem bi pha huy");
        p.getInventory().setItem(0, s);
        s.getItem().inventoryTick(s, h.getLevel(), p, 0, true);
        if (s.has(DataComponents.DAMAGE)) h.fail("DAMAGE khong bi go");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void khongVutDuocQvaCtrlQ(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack s = BanMenhService.create(p.getData(BanMenhAttachments.BAN_MENH.get()));
        p.getInventory().setItem(0, s);
        p.getInventory().selected = 0;
        p.drop(false);
        if (!p.getInventory().getItem(0).is(ModItems.BAN_MENH_KIEM.get())) h.fail("Q lam mat kiem");
        p.drop(true);
        if (!p.getInventory().getItem(0).is(ModItems.BAN_MENH_KIEM.get())) h.fail("Ctrl+Q lam mat kiem");
        // vut qua duong GUI (THROW tren o hotbar) va drop(ItemStack) truc tiep (keo ra ngoai / mod khac)
        var menu = p.inventoryMenu;
        menu.clicked(36, 0, ClickType.THROW, p);
        menu.clicked(36, 1, ClickType.THROW, p);
        p.drop(p.getInventory().removeItem(0, 1), true);
        if (swords(p) != 1) h.fail("sau THROW/drop: so kiem = " + swords(p));
        if (entities(h) != 0) h.fail("ItemEntity cua kiem ton tai");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void itemEntityBiHuyVaTraChu(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack s = BanMenhService.create(p.getData(BanMenhAttachments.BAN_MENH.get()));
        h.getLevel().getServer().getPlayerList().getPlayers(); // FakePlayer khong nam trong danh sach: chi kiem tra huy
        var pos = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
        h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(), pos.x, pos.y, pos.z, s));
        if (entities(h) != 0) h.fail("ItemEntity kiem van ton tai");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mat_kiem_thi_tai_tao(GameTestHelper h) {
        ServerPlayer p = player(h);
        if (BanMenhService.hasSword(p)) h.fail("khong ro rang hasSword");
        BanMenhService.restore(p);
        ItemStack s = null;
        for (ItemStack x : p.getInventory().items) if (x.is(ModItems.BAN_MENH_KIEM.get())) s = x;
        if (s == null) { h.fail("khong tai tao"); return; }
        if (BanMenhKiemItem.typeOf(s) != BanMenhType.values()[2] || BanMenhKiemItem.stageOf(s) != 3 && BanMenhKiemItem.stageOf(s) < 0)
            h.fail("loai/giai doan sai");
        if (!BanMenhKiemItem.isOwner(s, p)) h.fail("sai chu");
        BanMenhService.restore(p);
        if (swords(p) != 1) h.fail("nhan doi kiem: " + swords(p));
        h.succeed();
    }
}
