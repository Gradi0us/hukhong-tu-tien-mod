package com.hukhong.phaptu.linhtruong;

import com.hukhong.HuKhong;
import com.hukhong.artifact.ArtifactAttachments;
import com.hukhong.artifact.ArtifactData;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Linh Trượng: được trao, không vứt được, tia phép gây sát thương theo Phép lực x COEF x QUALITY của giai đoạn. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class LinhTruongGameTests {
    private LinhTruongGameTests() {}

    private static ServerPlayer player(GameTestHelper h, int level, int phapStage) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-lt"));
        p.getInventory().clearContent();
        CultivationData d = new CultivationData(level, 0, 100, 100, Set.of(Path.PHAP), Map.of(Path.PHAP, Aptitude.B), false, "", null, false);
        p.setData(ModAttachments.CULTIVATION, d);
        PhapTu.setElement(p, PhapElement.HOA);
        p.setData(ArtifactAttachments.ARTIFACT.get(), new ArtifactData(0, 0, 0, phapStage));
        StatService.refresh(p);
        return p;
    }

    private static int staffs(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(LinhTruongRegistry.LINH_TRUONG.get())) n++;
        return n;
    }

    @GameTest(template = "empty")
    public static void truongDuocTraoVaGanChu(GameTestHelper h) {
        ServerPlayer p = player(h, 60, 3);
        if (!LinhTruongService.grant(p)) h.fail("grant tra false");
        if (staffs(p) != 1) h.fail("so truong = " + staffs(p));
        LinhTruongService.grant(p);
        if (staffs(p) != 1) h.fail("nhan doi: " + staffs(p));
        ItemStack s = null;
        for (ItemStack x : p.getInventory().items) if (x.is(LinhTruongRegistry.LINH_TRUONG.get())) s = x;
        if (!LinhTruongItem.isOwner(s, p)) h.fail("sai chu");
        if (LinhTruongItem.elementOf(s) != PhapElement.HOA) h.fail("sai nguyen to");
        if (LinhTruongItem.stageOf(s) != 3) h.fail("sai giai doan " + LinhTruongItem.stageOf(s));
        if (s.isDamageableItem() || s.getMaxDamage() != 0) h.fail("co do ben");
        // mat truong -> tai tao (nhu recovery tick)
        p.getInventory().clearContent();
        LinhTruongService.grant(p);
        if (staffs(p) != 1) h.fail("khong tai tao");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void truongKhongVutDuoc(GameTestHelper h) {
        ServerPlayer p = player(h, 60, 3);
        p.getInventory().setItem(0, LinhTruongService.create(p));
        p.getInventory().selected = 0;
        p.drop(false);
        p.drop(true);
        p.inventoryMenu.clicked(36, 0, ClickType.THROW, p);
        p.inventoryMenu.clicked(36, 1, ClickType.THROW, p);
        p.drop(p.getInventory().removeItem(0, 1), true);
        if (staffs(p) != 1) h.fail("sau khi vut so truong = " + staffs(p));
        int ents = h.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(h.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(64),
                e -> e.getItem().is(LinhTruongRegistry.LINH_TRUONG.get())).size();
        if (ents != 0) h.fail("ItemEntity ton tai: " + ents);
        h.succeed();
    }

    /** Bắn tia vào bò đứng yên 2 block phía trước (tường khung test chắn ở ~3,5 block); trả sát thương (đã chuẩn hóa nếu bạo kích). */
    private static double shoot(GameTestHelper h, ServerPlayer p, double expected) {
        Vec3 base = h.absoluteVec(new Vec3(1.5, 1, 1.5));
        p.setPos(base);
        p.moveTo(base.x, base.y, base.z, 0f, 0f); // nhìn +Z
        p.getInventory().setItem(0, LinhTruongService.create(p));
        p.getInventory().selected = 0;
        Cow cow = EntityType.COW.create(h.getLevel());
        cow.moveTo(base.x, base.y, base.z + 2, 0f, 0f);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        cow.setHealth(1024);
        h.getLevel().addFreshEntity(cow);
        LinhTruongService.forget(p.getUUID());
        if (!LinhTruongService.fire(p)) h.fail("fire tra false");
        double dealt = 1024 - cow.getHealth();
        if (LinhTruongService.fire(p)) h.fail("hoi chieu khong chan");
        cow.discard();
        double r = dealt / expected;
        if (Math.abs(r - 1.0) < 0.02) return 1.0;
        if (Math.abs(r - 1.5) < 0.03) return 1.5;   // bao kich (crit damage x1.5)
        if (dealt >= 1023.9 && expected * 1.5 >= 1024) return 1.5; // bao kich vuot tran 1024 mau cua bo -> chet han
        h.fail("sat thuong " + dealt + " khong khop ky vong " + expected + " (ty le " + r + ")");
        return r;
    }

    @GameTest(template = "empty")
    public static void tiaPhepTheoGiaiDoan(GameTestHelper h) {
        double[] dmg = new double[2];
        int[] stages = {0, 6};
        for (int i = 0; i < 2; i++) {
            ServerPlayer p = player(h, 100, stages[i]);
            double sp = StatService.spellPower(p);
            double expected = LinhTruongMath.boltDamage(sp, stages[i]);
            if (LinhTruongService.stage(p) != stages[i]) h.fail("stage hieu luc " + LinhTruongService.stage(p));
            shoot(h, p, expected); // that bai neu sat thuong thuc te khong khop ky vong
            dmg[i] = expected;
        }
        // Phep luc cung cap -> ti le sat thuong = (COEF6 x Q6) / (COEF0 x Q0) ~ 2.0 x 1.12 (Pháp Khí cộng +2% Phép lực mỗi giai đoạn)
        double ratio = dmg[1] / dmg[0];
        if (ratio < 2.0 || ratio > 2.4) h.fail("ti le giai doan " + ratio);
        h.succeed();
    }
}
