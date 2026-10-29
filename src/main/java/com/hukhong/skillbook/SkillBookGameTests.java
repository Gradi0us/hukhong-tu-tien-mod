package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import com.hukhong.skill.SkillService;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tích hợp Công Pháp: học bí tịch (item) -> gán phím -> cast qua SkillService -> sát thương thật; Cường Hóa trừ Hồn Tinh và
 * nhân sát thương kỹ năng nền. Chạy bằng gradle runGameTestServer.
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class SkillBookGameTests {
    private SkillBookGameTests() {}

    private static ServerPlayer player(GameTestHelper h, Path path, int level) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-book"));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 4000, 4000, Set.of(path), Map.<Path, Aptitude>of(), false));
        StatService.refresh(p);
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1.5));
        p.moveTo(at.x, at.y, at.z, 0f, 0f); // nhìn về +z
        SkillService.state(p).stance = true;
        return p;
    }

    private static Zombie dummy(GameTestHelper h, double z) {
        Zombie zb = h.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, (int) z);
        zb.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000);
        zb.setHealth(4000f);
        com.hukhong.gametest.GameTestSupport.deterministic(zb);
        return zb;
    }

    @GameTest(template = "empty")
    public static void hocGanVaCastBiTich(GameTestHelper h) {
        ServerPlayer p = player(h, Path.KIEM, 30);
        // chưa học: không gán được, không cast được
        SkillBookService.assign(p, Path.KIEM.ordinal(), 1, BookSkill.PHA_KHONG_TRAM.id);
        if (SkillBookService.data(p).slot(Path.KIEM, 1) != null) h.fail("gan duoc bi tich chua hoc");
        // dùng item để học
        ItemStack scroll = SkillBookRegistry.stack(BookSkill.PHA_KHONG_TRAM);
        if (SkillBookRegistry.skillOf(scroll) != BookSkill.PHA_KHONG_TRAM) h.fail("item khong mang ky nang");
        if (!SkillBookService.learn(p, BookSkill.PHA_KHONG_TRAM)) h.fail("hoc that bai");
        if (!SkillBookService.data(p).learned("pha_khong_tram")) h.fail("data khong ghi da hoc");
        if (SkillBookService.learn(p, BookSkill.PHA_KHONG_TRAM)) h.fail("hoc lan 2 phai bi tu choi (giu lam vat lieu)");
        // sai đạo: Đạo-skill trên Kiếm tu
        if (SkillBookService.learn(p, BookSkill.THANH_TAM_CHU)) h.fail("hoc ky nang dao khac");
        SkillBookService.assign(p, Path.KIEM.ordinal(), 1, BookSkill.PHA_KHONG_TRAM.id);
        if (!BookSkill.PHA_KHONG_TRAM.id.equals(SkillBookService.data(p).slot(Path.KIEM, 1))) h.fail("gan phim E that bai");
        Zombie z = dummy(h, 5);
        float before = z.getHealth();
        SkillService.cast(p, 1);
        if (z.getHealth() >= before) h.fail("Pha Khong Tram khong gay sat thuong: " + before + " -> " + z.getHealth());
        // hồi chiêu: cast lại ngay không gây thêm sát thương
        float mid = z.getHealth();
        SkillService.cast(p, 1);
        if (z.getHealth() < mid) h.fail("cast lai khi dang hoi chieu van gay sat thuong");
        h.succeed();
    }

    private static float castDamage(GameTestHelper h, ServerPlayer p, int slot) {
        Zombie z = dummy(h, 3);
        SkillService.state(p).cooldownEnd[slot] = 0;
        com.hukhong.gametest.GameTestSupport.noCrit(p);
        float before = z.getHealth();
        SkillService.cast(p, slot);
        float d = before - z.getHealth();
        z.discard();
        return d;
    }

    @GameTest(template = "empty")
    public static void cuongHoaTruHonTinhVaNhanSatThuong(GameTestHelper h) {
        ServerPlayer p = player(h, Path.KIEM, 30); // cấp tu 30 -> trần cấp kỹ năng 4
        float base = castDamage(h, p, 0);
        if (base <= 0) h.fail("Tram Khi cap 1 khong gay sat thuong");
        // thiếu Hồn Tinh: không nâng
        SkillBookService.upgrade(p, "kiem_1", SkillBookMath.Mode.HON_TINH.ordinal());
        if (SkillBookService.data(p).level("kiem_1") != 1) h.fail("nang cap khi thieu Hon Tinh");
        com.hukhong.currency.Wallet.setHonTinh(p, 100); // Hon Tinh la tien trong vi
        SkillBookService.upgrade(p, "kiem_1", SkillBookMath.Mode.HON_TINH.ordinal());
        if (SkillBookService.data(p).level("kiem_1") != 2) h.fail("khong len cap 2");
        int expectLeft = 100 - SkillBookMath.cost(1, SkillBookService.gradeOf("kiem_1"), SkillBookMath.Mode.HON_TINH).honTinh();
        if (SkillBookService.honTinh(p) != expectLeft) h.fail("Hon Tinh con " + SkillBookService.honTinh(p) + " != " + expectLeft);
        for (int i = 0; i < 12; i++) SkillBookService.upgrade(p, "kiem_1", SkillBookMath.Mode.HON_TINH.ordinal());
        int lv = SkillBookService.data(p).level("kiem_1");
        if (lv != SkillBookMath.levelCap(30)) h.fail("cap cap theo cap tu luyen: " + lv + " != " + SkillBookMath.levelCap(30));
        // CH ví: không có tông môn -> từ chối, không mất Hồn Tinh
        int ht = SkillBookService.honTinh(p);
        CultivationData orig = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, new CultivationData(100, 0, 4000, 4000, Set.of(Path.KIEM), Map.<Path, Aptitude>of(), false));
        SkillBookService.upgrade(p, "kiem_1", SkillBookMath.Mode.CONTRIBUTION.ordinal());
        if (SkillBookService.data(p).level("kiem_1") != lv || SkillBookService.honTinh(p) != ht) h.fail("CH khong tong mon van tru phi");
        p.setData(ModAttachments.CULTIVATION, orig); // trả về cấp 30 để so sánh công bằng
        com.hukhong.stats.StatService.refresh(p);
        // sát thương kỹ năng nền tăng theo cấp
        float up = castDamage(h, p, 0);
        double ratio = up / base;
        double want = SkillBookMath.dmgMult(lv);
        if (Math.abs(ratio - want) > 0.05 * want) h.fail("ti le sat thuong " + ratio + " != " + want);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ganKyNangNenSangPhimKhacVaPhimR(GameTestHelper h) {
        ServerPlayer p = player(h, Path.THE, 20); // chưa mở Bản Mệnh (cấp < 40)
        BookSkill b = BookSkill.LONG_HO_QUYEN;
        SkillBookService.learn(p, b);
        SkillBookService.assign(p, Path.THE.ordinal(), 2, b.id); // R
        Zombie z = dummy(h, 2);
        float before = z.getHealth();
        SkillService.cast(p, 2);
        if (z.getHealth() >= before) h.fail("ky nang gan phim R khong chay khi chua mo Ban Menh");
        h.succeed();
    }
}
