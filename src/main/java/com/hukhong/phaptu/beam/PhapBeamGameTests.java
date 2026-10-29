package com.hukhong.phaptu.beam;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.skill.SkillService;
import com.hukhong.skillbook.SkillBookService;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tinh Hà Quang Trụ: nạp -> sẵn sàng -> bắn gây sát thương, trừ Pháp Lực, đặt hồi chiêu; hủy sớm; gán phím T bị từ chối với Quang. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class PhapBeamGameTests {
    private PhapBeamGameTests() {}

    private static ServerPlayer player(GameTestHelper h, PhapElement el, Aptitude apt, int level) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-beam"));
        p.getInventory().clearContent();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 1000, 1000, Set.of(Path.PHAP), Map.of(Path.PHAP, apt), false, "", null, false));
        PhapTu.setElement(p, el);
        StatService.refresh(p);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
        SkillService.state(p).stance = true;
        Vec3 b = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        p.moveTo(b.x, b.y, b.z, 0f, 0f); // yaw 0 = nhìn +Z
        return p;
    }

    private static Cow target(GameTestHelper h, ServerPlayer p, double dz) {
        Cow c = EntityType.COW.create(h.getLevel());
        c.moveTo(p.getX(), p.getY(), p.getZ() + dz, 0f, 0f);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0e7);
        c.setHealth(1.0e7f);
        h.getLevel().addFreshEntity(c);
        return c;
    }

    private static int max(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).resourceMax(); }

    private static int res(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).resource(); }

    @GameTest(template = "empty")
    public static void nap_ban_gay_sat_thuong(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.QUANG, Aptitude.SSS, 60);
        Cow cow = target(h, p, 6);
        long now = h.getLevel().getGameTime();

        if (!PhapBeamService.chargeStart(p)) h.fail("chargeStart tra false");
        if (PhapBeamService.phase(p) != PhapBeamService.Phase.CHARGING) h.fail("khong o CHARGING");
        if (PhapBeamService.fireStart(p)) h.fail("ban duoc khi chua nap xong");
        for (int i = 0; i < PhapBeamMath.CHARGE_TICKS - 1; i++) PhapBeamService.tick(p);
        if (PhapBeamService.phase(p) != PhapBeamService.Phase.CHARGING) h.fail("len READY som");
        PhapBeamService.tick(p);
        if (PhapBeamService.phase(p) != PhapBeamService.Phase.READY) h.fail("khong READY sau nap: " + PhapBeamService.phase(p));
        if (res(p) != max(p)) h.fail("nap lam mat mana: " + res(p));

        if (!PhapBeamService.fireStart(p)) h.fail("fireStart tra false");
        if (res(p) != max(p) - PhapBeamMath.upfrontCost(max(p), com.hukhong.banmenh.BanMenhService.skillCostFactor(p))) h.fail("tru truoc sai: " + res(p));
        double cz = cow.getZ(), dealt = 0;
        for (int i = 0; i < PhapBeamMath.FIRE_MAX_TICKS + 5; i++) { // giữ bia đứng yên + hồi đầy máu mỗi tick (máu tối đa bị kẹp 1024)
            cow.setDeltaMovement(Vec3.ZERO);
            cow.moveTo(cow.getX(), cow.getY(), cz, 0f, 0f);
            cow.setHealth(cow.getMaxHealth());
            float hb = cow.getHealth();
            PhapBeamService.tick(p);
            dealt += hb - cow.getHealth();
        }
        if (PhapBeamService.phase(p) != PhapBeamService.Phase.IDLE) h.fail("khong ve IDLE: " + PhapBeamService.phase(p));

        double expected = PhapBeamMath.fullChannelDamage(PhapElement.QUANG, Aptitude.SSS, StatService.spellPower(p));
        if (dealt <= 0) h.fail("khong gay sat thuong");
        if (dealt < expected * 0.5) h.fail("sat thuong qua thap " + dealt + " / ky vong " + expected);
        if (res(p) > max(p) / 10) h.fail("mana con qua nhieu sau 4s kenh: " + res(p));
        long cd = SkillService.state(p).cooldownEnd[3] - now;
        if (cd < PhapBeamMath.COOLDOWN_TICKS - 5) h.fail("hoi chieu qua ngan: " + cd);
        if (PhapBeamService.chargeStart(p)) h.fail("nap lai duoc khi dang hoi chieu");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nha_T_som_huy_khong_ton_mana(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.LOI, Aptitude.B, 30);
        if (!PhapBeamService.chargeStart(p)) h.fail("chargeStart false");
        for (int i = 0; i < 10; i++) PhapBeamService.tick(p);
        PhapBeamService.chargeStop(p);
        if (PhapBeamService.phase(p) != PhapBeamService.Phase.IDLE) h.fail("khong huy");
        if (res(p) != max(p)) h.fail("mat mana khi huy: " + res(p));
        long lock = SkillService.state(p).cooldownEnd[3] - h.getLevel().getGameTime();
        if (lock < 1 || lock > 100) h.fail("khoa nhe sai: " + lock);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void het_mana_giua_chung_tia_tat(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.HOA, Aptitude.S, 40);
        PhapBeamService.chargeStart(p);
        for (int i = 0; i < PhapBeamMath.CHARGE_TICKS; i++) PhapBeamService.tick(p);
        // chỉ đủ trả trước, hết ngay khi kênh
        p.setData(ModAttachments.CULTIVATION, p.getData(ModAttachments.CULTIVATION).withResource(PhapBeamMath.upfrontCost(max(p), 1.0) + 10));
        if (!PhapBeamService.fireStart(p)) h.fail("fireStart false");
        for (int i = 0; i < 20; i++) PhapBeamService.tick(p);
        if (PhapBeamService.phase(p) != PhapBeamService.Phase.IDLE) h.fail("tia khong tat khi het mana");
        if (res(p) != 0) h.fail("mana con " + res(p));
        if (SkillService.state(p).cooldownEnd[3] <= h.getLevel().getGameTime()) h.fail("khong dat hoi chieu");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void gan_phim_T_bi_tu_choi_voi_Quang(GameTestHelper h) {
        for (PhapElement el : new PhapElement[]{PhapElement.QUANG, PhapElement.LOI, PhapElement.HOA, PhapElement.THUY}) {
            ServerPlayer p = player(h, el, Aptitude.B, 30);
            SkillBookService.assign(p, Path.PHAP.ordinal(), 3, "phap_1");
            if (SkillBookService.data(p).slot(Path.PHAP, 3) != null) h.fail("gan duoc phim T voi " + el);
            SkillBookService.assign(p, Path.PHAP.ordinal(), 0, "phap_ult");
            if (SkillBookService.data(p).slot(Path.PHAP, 0) != null) h.fail("gan phap_ult sang Q voi " + el);
        }
        // Thổ giữ hành vi cũ
        ServerPlayer tho = player(h, PhapElement.THO, Aptitude.B, 30);
        SkillBookService.assign(tho, Path.PHAP.ordinal(), 3, "phap_1");
        if (!"phap_1".equals(SkillBookService.data(tho).slot(Path.PHAP, 3))) h.fail("Tho khong gan duoc phim T");
        h.succeed();
    }
}
