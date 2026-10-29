package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.skill.SkillService;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pháp Tu: Thổ (E dựng trụ qua SkillService.cast thật, Thạch Giáp hấp thụ, Thiên Sơn Áp Đỉnh gây sát thương + choáng, trụ bắn/khiêu khích)
 * và các bị động Quang/Lôi/Hỏa/Thủy/Kim thực sự kích hoạt trong giao chiến.
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class PhapThoGameTests {
    private PhapThoGameTests() {}

    static ServerPlayer player(GameTestHelper h, PhapElement el, Aptitude apt) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-" + el.id()));
        p.getInventory().clearContent();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(60, 0, 1000, 1000, Set.of(Path.PHAP), Map.of(Path.PHAP, apt), false, "", null, false));
        PhapTu.setElement(p, el);
        StatService.refresh(p);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
        SkillService.state(p).stance = true;
        PhapRuntime.reset(p);
        Vec3 b = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        p.moveTo(b.x, b.y, b.z, 0f, 0f); // yaw 0 = nhìn +Z
        return p;
    }

    static long turrets(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(PhapTurret.class, new AABB(h.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(64)).stream()
                .filter(PhapTurret::isAlive).count();
    }

    static java.util.List<PhapTurret> turretsOf(GameTestHelper h, ServerPlayer p) {
        return h.getLevel().getEntitiesOfClass(PhapTurret.class, new AABB(p.blockPosition()).inflate(40)).stream()
                .filter(t -> t.isAlive() && p.getUUID().equals(t.ownerId())).toList();
    }

    static Zombie zombie(GameTestHelper h, ServerPlayer p, double ahead) {
        Zombie z = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 at = p.position().add(0, 0.7, ahead); // ngang tia nhin (aim() bat muc tieu doc tia), khong roi xuong dat
        z.moveTo(at.x, at.y, at.z);
        z.setNoGravity(true);
        z.setNoAi(true);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0E8);
        z.setHealth(1.0E8f);
        z.setPersistenceRequired();
        h.getLevel().addFreshEntity(z);
        return z;
    }

    private static void hitSkill(ServerPlayer p, Zombie z, float dmg) {
        z.invulnerableTime = 0;
        DamageRules.hurtBySkill(p, z, dmg);
    }

    // ------------------------------------------------------------------ Thổ

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tho_e_qua_skill_service_dung_thap(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        long before = SkillService.state(p).cooldownEnd[1];
        SkillService.cast(p, 1); // đường thật: SkillService.cast -> SkillEffects -> PhapSkills (ground() từng ném NPE ở đây)
        if (SkillService.state(p).cooldownEnd[1] == before) h.fail("cast E khong dat cooldown (cast bi nem exception?)");
        h.runAfterDelay(40, () -> {
            if (turrets(h) < 1) h.fail("E Tho qua SkillService.cast khong co thap nao song sau 40 tick");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void tho_e_thach_giap_hap_thu_va_mien_day_lui(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        SkillService.cast(p, 1);
        float abs = p.getAbsorptionAmount();
        if (abs < 1f) h.fail("khong co khien hap thu: " + abs);
        if (!PhapRuntime.thoArmored(p)) h.fail("thoArmored false");
        if (p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 0.999) h.fail("khong mien day lui: " + p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        float expect = PhapMath.thoShield(StatService.spellPower(p), p.getMaxHealth());
        if (Math.abs(abs - expect) > 0.01f) h.fail("khien " + abs + " khac cong thuc " + expect);
        // NguoiChoiGia (FakePlayer) bat tu moi sat thuong nen khong the ban don that; hap thu do co che absorption cua vanilla (MAX_ABSORPTION da duoc nang)
        if (p.getAttributeValue(Attributes.MAX_ABSORPTION) < expect - 0.01) h.fail("tran hap thu chua duoc nang: " + p.getAttributeValue(Attributes.MAX_ABSORPTION));
        // het han: khien va mien day lui duoc go
        h.runAfterDelay(PhapMath.THO_SHIELD_TICKS + 5, () -> {
            PhapRuntime.tickThoForTest(p);
            if (PhapRuntime.thoArmored(p)) h.fail("khien chua het han");
            if (p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) > 0.001) h.fail("mien day lui chua go");
            if (p.getAttributeValue(Attributes.MAX_ABSORPTION) > 0.001) h.fail("tran hap thu chua go");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void tho_t_thien_son_gay_sat_thuong_va_choang(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        Zombie z = zombie(h, p, 6);
        float before = z.getHealth();
        SkillService.cast(p, 3);
        h.runAfterDelay(8, () -> { if (before - z.getHealth() > 20) h.fail("sat thuong den qua som (phai co telegraph 1s): " + (before - z.getHealth())); });
        h.runAfterDelay(26, () -> {
            if (before - z.getHealth() < 50) h.fail("Thien Son khong gay sat thuong lon: " + (before - z.getHealth()));
            if (turretsOf(h, p).size() < PhapMath.thoMaxTurrets(Aptitude.SSS)) h.fail("T khong lap day tru: " + turretsOf(h, p).size());
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void tho_t_choang_mob_trong_ban_kinh(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        Zombie z = zombie(h, p, 6);
        z.setNoAi(false); // Zombie co AI de choang co hieu luc
        SkillService.cast(p, 3);
        h.runAfterDelay(24, () -> {
            if (!z.isNoAi()) h.fail("mob trong ban kinh khong bi choang (NoAI) sau khi nui roi");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void tho_tru_ban_ke_dich_va_khieu_khich(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        Zombie z = zombie(h, p, 6);
        z.setNoAi(false);
        Zombie target = z;
        float before = z.getHealth();
        SkillService.cast(p, 1);
        h.runAfterDelay(100, () -> {
            if (turrets(h) < 1) h.fail("tru bien mat");
            if (!(target.getHealth() < before)) h.fail("tru khong ban trung ke dich: " + target.getHealth());
            if (!(target.getTarget() instanceof PhapTurret)) h.fail("tru khong khieu khich: target=" + target.getTarget());
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_t_khuech_dai_tru_x2(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        SkillService.cast(p, 3);
        h.runAfterDelay(24, () -> {
            var ts = turretsOf(h, p);
            if (ts.isEmpty()) h.fail("khong co tru");
            if (!ts.stream().allMatch(PhapTurret::isEmpowered)) h.fail("tru chua duoc khuech dai");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ bị động kích hoạt thật

    @GameTest(template = "empty")
    public static void bi_dong_hoa_thieu_nhien(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.HOA, Aptitude.SSS);
        Zombie z = zombie(h, p, 4);
        hitSkill(p, z, 5f);
        hitSkill(p, z, 5f);
        if (PhapRuntime.burnStacks(z) < 2) h.fail("Thieu Nhien khong cong tang: " + PhapRuntime.burnStacks(z));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bi_dong_loi_an_no_choang(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.LOI, Aptitude.SSS);
        Zombie z = zombie(h, p, 4);
        z.setNoAi(false);
        for (int i = 0; i < PhapMath.LOI_STACKS; i++) hitSkill(p, z, 5f);
        if (!z.isNoAi()) h.fail("Loi An 5 tang khong gay choang");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bi_dong_quang_mang_no_va_mien_phi(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.QUANG, Aptitude.SSS);
        Zombie z = zombie(h, p, 4);
        for (int i = 0; i < PhapMath.QUANG_STACKS; i++) hitSkill(p, z, 5f);
        if (PhapRuntime.quangFreeMask(p) == 0) h.fail("Quang Mang 15 tang khong no / khong mo vong mien phi");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bi_dong_thuy_an_tang_khi_hoi_mau_va_danh(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THUY, Aptitude.SSS);
        Zombie z = zombie(h, p, 4);
        hitSkill(p, z, 5f);
        if (PhapRuntime.thuyStacks(p) < 1) h.fail("Thuy An khong tang khi danh: " + PhapRuntime.thuyStacks(p));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bi_dong_kim_cuong_tang_phep_luc(GameTestHelper h) {
        ServerPlayer kim = player(h, PhapElement.KIM, Aptitude.SSS);
        double withKim = StatService.spellPower(kim);
        PhapTu.setElement(kim, PhapElement.QUANG);
        StatService.refresh(kim);
        double without = StatService.spellPower(kim);
        if (!(withKim > without * 1.2)) h.fail("Kim Cuong khong cong Phep luc: " + withKim + " vs " + without);
        h.succeed();
    }
}
