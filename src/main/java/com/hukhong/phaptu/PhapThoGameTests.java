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

    /** Quay thời gian của ThoService n tick (người chơi giả không nhận PlayerTickEvent). */
    private static void spin(ServerPlayer p, int n) {
        for (int i = 0; i < n; i++) ThoService.tick(p);
    }

    /** Zombie có mũ (không cháy nắng); MAX_HEALTH vanilla bị kẹp 1024 nên hp chỉ mang tính "tối đa". */
    private static Zombie zombieHp(GameTestHelper h, ServerPlayer p, double ahead, float hp) {
        Zombie z = zombie(h, p, ahead);
        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        z.setHealth(hp);
        return z;
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_t_ba_dot_roi_roi_giu_T_dan_no_rut_99_mana(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        Zombie z = zombieHp(h, p, 6, 1.0E6f);
        float h0 = z.getHealth();
        SkillService.cast(p, 3);
        long cdLeft = SkillService.state(p).cooldownEnd[3] - p.level().getGameTime();
        if (cdLeft != PhapMath.THO_T_COOLDOWN_TICKS) h.fail("cooldown T Tho phai " + PhapMath.THO_T_COOLDOWN_TICKS + " tick (45s), thuc te " + cdLeft);
        if (!ThoService.tActive(p)) h.fail("T khong khoi dong dot roi");
        spin(p, PhapMath.THO_T_FALL_DELAY + 1);
        if (ThoService.wavesLanded(p) != 1) h.fail("sau dot 1 phai co 1 dot da roi: " + ThoService.wavesLanded(p));
        float d1 = h0 - z.getHealth();
        if (d1 <= 0) h.fail("dot 1 khong gay sat thuong");
        spin(p, 3 * PhapMath.THO_T_WAVE_GAP);
        if (ThoService.wavesLanded(p) != PhapMath.THO_T_WAVES) h.fail("chua du 3 dot: " + ThoService.wavesLanded(p));
        if (!ThoService.blastWindow(p)) h.fail("sau 3 dot phai mo cua so dan no");
        float dWaves = h0 - z.getHealth();
        if (dWaves < d1 * 2f) h.fail("3 dot phai gay ~3x dot 1: dot1=" + d1 + " tong=" + dWaves);
        double expectWaves = PhapMath.coef(PhapElement.THO, 3) * StatService.spellPower(p) * PhapMath.THO_T_WAVE_FRAC * PhapMath.THO_T_WAVES;
        if (dWaves > expectWaves * 3 || dWaves < expectWaves * 0.3) h.fail("tong 3 dot " + dWaves + " lech xa cong thuc " + expectWaves);
        // chua giu T: khong tu no
        spin(p, 10);
        if (!ThoService.blastWindow(p)) h.fail("cua so dan no tu dong dong khi chua het han");
        int mana = p.getData(ModAttachments.CULTIVATION).resource();
        z.setHealth(z.getMaxHealth()); // MAX_HEALTH bị kẹp 1024: hồi đầy để không giết quá tay trước lúc nổ
        float beforeBlast = z.getHealth();
        ThoService.tKey(p, true);
        spin(p, PhapMath.THO_T_HOLD_TICKS - 2);
        if (!ThoService.blastWindow(p)) h.fail("no qua som (chua giu du lau)");
        spin(p, 4);
        if (ThoService.blastWindow(p)) h.fail("giu T du lau nhung chua dan no");
        float dBlast = beforeBlast - z.getHealth();
        if (dBlast < d1 * 1.5f) h.fail("sat thuong dan no phai ~2,9x dot 1: dot1=" + d1 + " no=" + dBlast);
        int left = p.getData(ModAttachments.CULTIVATION).resource();
        if (left > Math.ceil(mana * 0.01) + 1) h.fail("dan no phai rut 99% Phap Luc: truoc=" + mana + " sau=" + left);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_t_het_han_cua_so_khong_dan_no(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        Zombie z = zombieHp(h, p, 6, 1.0E6f);
        SkillService.cast(p, 3);
        spin(p, 3 * PhapMath.THO_T_WAVE_GAP);
        if (!ThoService.blastWindow(p)) h.fail("cua so khong mo");
        float hp = z.getHealth();
        int mana = p.getData(ModAttachments.CULTIVATION).resource();
        ThoService.tKey(p, true); // giu T roi nha truoc khi du lau
        spin(p, PhapMath.THO_T_HOLD_TICKS - 3);
        ThoService.tKey(p, false);
        spin(p, PhapMath.THO_T_WINDOW_TICKS + 5);
        if (ThoService.blastWindow(p)) h.fail("cua so phai het han sau ~6s");
        if (z.getHealth() < hp) h.fail("het han ma van gay sat thuong no");
        if (p.getData(ModAttachments.CULTIVATION).resource() != mana) h.fail("het han khong duoc rut mana");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_t_nhan_giu_tu_luc_cast_khong_tu_no(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        SkillService.cast(p, 3);
        ThoService.tKey(p, true); // dang giu T tu luc cast -> phai nhan T MOI sau khi cua so mo
        spin(p, 3 * PhapMath.THO_T_WAVE_GAP + PhapMath.THO_T_HOLD_TICKS + 10);
        if (!ThoService.blastWindow(p)) h.fail("giu T tu luc cast khong duoc tu dan no");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_t_choang_mob_trong_ban_kinh(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        Zombie z = zombie(h, p, 6);
        z.setNoAi(false); // Zombie co AI de choang co hieu luc
        SkillService.cast(p, 3);
        spin(p, PhapMath.THO_T_FALL_DELAY + 1);
        if (!z.isNoAi()) h.fail("mob trong ban kinh khong bi choang (NoAI) sau khi nui roi");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_e_va_t_khong_thao_duoc_khoi_thanh_ky_nang(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        int ord = Path.PHAP.ordinal();
        com.hukhong.skillbook.SkillBookService.assign(p, ord, 1, null);       // thao E
        com.hukhong.skillbook.SkillBookService.assign(p, ord, 1, "phap_1");   // thay E
        com.hukhong.skillbook.SkillBookService.assign(p, ord, 3, "phap_1");   // thay T
        com.hukhong.skillbook.SkillBookService.assign(p, ord, 0, "phap_2");   // dua E sang phim khac
        com.hukhong.skillbook.SkillBookService.assign(p, ord, 0, "phap_ult"); // dua T sang phim khac
        var d = com.hukhong.skillbook.SkillBookService.data(p);
        if (d.slot(Path.PHAP, 1) != null || d.slot(Path.PHAP, 3) != null) h.fail("gan de len phim E/T cua Tho khong bi chan: E=" + d.slot(Path.PHAP, 1) + " T=" + d.slot(Path.PHAP, 3));
        if ("phap_2".equals(d.slot(Path.PHAP, 0)) || "phap_ult".equals(d.slot(Path.PHAP, 0))) h.fail("chuyen E/T sang phim khac khong bi chan");
        int mask = ThoService.lockMask(p);
        if (mask != (2 | 8)) h.fail("lockMask Tho phai = E|T (10): " + mask);
        // du du lieu luu co gan bay, resolve van ep ve mac dinh
        var bad = d.withSlot(Path.PHAP, 1, "phap_1").withSlot(Path.PHAP, 3, "phap_1").withSlot(Path.PHAP, 0, "phap_2");
        var e = com.hukhong.skillbook.SkillBook.resolve(bad, Path.PHAP, 1, 60, mask);
        var t = com.hukhong.skillbook.SkillBook.resolve(bad, Path.PHAP, 3, 60, mask);
        var q = com.hukhong.skillbook.SkillBook.resolve(bad, Path.PHAP, 0, 60, mask);
        if (e == null || e.base() != com.hukhong.skill.Skill.PHAP_2) h.fail("phim E Tho khong co dinh la Tho Thap");
        if (t == null || t.base() != com.hukhong.skill.Skill.PHAP_ULT) h.fail("phim T Tho khong co dinh la Thien Son");
        if (q == null || q.base() != com.hukhong.skill.Skill.PHAP_1) h.fail("phap_2 gan len Q van lot qua khoa: " + (q == null ? null : q.base()));
        if (PhapMath.lockMask(PhapElement.KIM) != 0 || PhapMath.lockMask(PhapElement.QUANG) != 8) h.fail("lockMask nguyen to khac sai");
        h.succeed();
    }

    // ------------------------------------------------------------------ Hợp Thể + laser

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_hai_tru_trong_5_block_hop_the(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        Vec3 a = p.position().add(2, 0, 0), b = p.position().add(2, 0, 4.2); // cach nhau 4,2 < 5
        PhapTurret t1 = PhapRuntime.summonTurret(h.getLevel(), p, a);
        PhapTurret t2 = PhapRuntime.summonTurret(h.getLevel(), p, b);
        if (t1 == null || t2 == null) h.fail("khong dung duoc tru");
        h.runAfterDelay(2, () -> {
            var ts = turretsOf(h, p);
            if (ts.size() != 1) h.fail("2 tru cach < 5 block phai hop thanh 1, con " + ts.size());
            PhapTurret m = ts.get(0);
            if (!m.isMerged()) h.fail("tru khong o trang thai Hop The");
            if (m.lifeLeft() < PhapMath.THO_LIFE_TICKS * 2 - 20) h.fail("thoi gian song phai cong don: " + m.lifeLeft());
            if (m.getMaxHealth() < PhapMath.thoTurretHp(Aptitude.SSS, p.getMaxHealth()) * 1.99f) h.fail("mau phai cong don: " + m.getMaxHealth());
            if (m.getBbWidth() < 0.9f * PhapMath.THO_MERGED_SCALE - 0.01f) h.fail("mo hinh/hitbox chua to hon: " + m.getBbWidth());
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_hai_tru_cach_xa_hon_5_block_khong_hop_the(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        PhapRuntime.summonTurret(h.getLevel(), p, p.position().add(-3, 0, 0));
        PhapRuntime.summonTurret(h.getLevel(), p, p.position().add(3, 0, 0)); // 6 block
        h.runAfterDelay(2, () -> {
            var ts = turretsOf(h, p);
            if (ts.size() != 2) h.fail("2 tru cach 6 block phai o rieng: " + ts.size());
            if (ts.stream().anyMatch(PhapTurret::isMerged)) h.fail("tru cach xa khong duoc hop the");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void tho_tru_hop_the_laser_tang_dan_don_muc_tieu_va_reset(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        PhapRuntime.summonTurret(h.getLevel(), p, p.position().add(0, 0, -3));
        PhapTurret m = PhapRuntime.summonTurret(h.getLevel(), p, p.position().add(1, 0, -3));
        Zombie near = zombieHp(h, p, 0, 1.0E6f);
        near.setPos(m.getX() + 5, m.getY() + 0.7, m.getZ());
        Zombie far = zombieHp(h, p, 0, 1.0E6f);
        far.setPos(m.getX() + 1, m.getY() + 0.7, m.getZ() + 9); // 9 block (xa hơn near 5 block)
        float[] hp = {near.getMaxHealth()};
        float[] early = {0};
        h.runAfterDelay(26, () -> { early[0] = hp[0] - near.getHealth(); hp[0] = near.getHealth(); });
        h.runAfterDelay(150, () -> { near.setHealth(near.getMaxHealth()); hp[0] = near.getHealth(); });
        h.runAfterDelay(170, () -> {
            float late = hp[0] - near.getHealth();
            if (early[0] <= 0) h.fail("laser khong gay sat thuong luc dau");
            if (late < early[0] * 2.5f) h.fail("laser phai tang dan: 26 tick dau=" + early[0] + " 20 tick sau 6s=" + late);
            if (m.lockTicks() < PhapMath.THO_LASER_RAMP_TICKS) h.fail("chua dat nac toi da: " + m.lockTicks());
            if (m.rampFraction() < 0.99f) h.fail("rampFraction chua day: " + m.rampFraction());
            if (far.getHealth() < far.getMaxHealth() - 0.01f) h.fail("laser chi duoc ban 1 muc tieu, muc tieu xa bi trung: far=" + far.getHealth() + " near=" + near.getHealth() + " early=" + early[0] + " late=" + late + " sp=" + StatService.spellPower(p) + " lock=" + m.lockTicks());
            // doi muc tieu: dua muc tieu hien tai ra khoi tam -> reset
            near.setPos(m.getX() + 40, m.getY() + 0.7, m.getZ());
        });
        h.runAfterDelay(190, () -> {
            if (m.lockTicks() > 30) h.fail("doi muc tieu phai reset nac laser: " + m.lockTicks());
            if (m.rampFraction() > 0.3f) h.fail("rampFraction phai ve thap sau khi doi muc tieu: " + m.rampFraction());
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ E giữ: xuyên tường

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void tho_e_bam_nhanh_dung_thap_giu_thi_xuyen_tuong(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        ThiemBoService.tick(p);
        ThiemBoService.tick(p);
        ThiemBoService.action(p, ThiemBoPayload.RELEASE); // bam nhanh
        if (ThoService.phasing(p)) h.fail("bam nhanh khong duoc xuyen tuong");
        if (SkillService.state(p).cooldownEnd[1] <= p.level().getGameTime()) h.fail("bam nhanh phai cast E (dung thap)");
        h.runAfterDelay(5, () -> {
            if (turrets(h) < 1) h.fail("bam nhanh khong dung thap");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tho_e_giu_xuyen_tuong_5s_roi_day_ra_cho_trong(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        var lv = h.getLevel();
        var base = h.absolutePos(new net.minecraft.core.BlockPos(0, 3, 0));
        for (int x = 0; x <= 6; x++) for (int y = 0; y <= 6; y++) for (int z = 0; z <= 6; z++)
            lv.setBlock(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        for (int i = 0; i < ThiemBoMath.HOLD_TICKS + 1; i++) ThiemBoService.tick(p);
        if (!ThoService.phasing(p)) h.fail("giu E khong bat dau xuyen tuong");
        if (!p.noPhysics) h.fail("noPhysics phai bat khi xuyen tuong");
        if (ThoService.phaseLeft(p) < PhapMath.THO_PHASE_TICKS - 3) h.fail("thoi luong xuyen tuong sai: " + ThoService.phaseLeft(p));
        if (SkillService.state(p).cooldownEnd[1] <= p.level().getGameTime()) h.fail("xuyen tuong phai dat hoi chieu E");
        p.setPos(base.getX() + 3.5, base.getY() + 3, base.getZ() + 3.5); // giua khoi da
        ThoService.tick(p);
        if (!ThoService.phasing(p)) h.fail("dang trong da phai van xuyen tuong");
        spin(p, PhapMath.THO_PHASE_TICKS + 2);
        if (ThoService.phasing(p)) h.fail("het 5s phai ket thuc xuyen tuong");
        if (p.noPhysics) h.fail("noPhysics phai tat sau khi het");
        boolean free = lv.noCollision(p, p.getBoundingBox());
        Vec3 end = p.position();
        for (int x = 0; x <= 6; x++) for (int y = 0; y <= 6; y++) for (int z = 0; z <= 6; z++)
            lv.setBlock(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        if (!free) h.fail("het xuyen tuong van ket trong khoi: " + end);
        if (end.distanceTo(new Vec3(base.getX() + 3.5, base.getY() + 3, base.getZ() + 3.5)) > 7) h.fail("bi day qua xa: " + end);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tho_xuyen_tuong_khong_xuyen_bedrock(GameTestHelper h) {
        ServerPlayer p = player(h, PhapElement.THO, Aptitude.SSS);
        var lv = h.getLevel();
        var bed = h.absolutePos(new net.minecraft.core.BlockPos(8, 3, 2));
        lv.setBlock(bed, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 3);
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        for (int i = 0; i < ThiemBoMath.HOLD_TICKS + 1; i++) ThiemBoService.tick(p);
        if (!ThoService.phasing(p)) h.fail("khong bat dau xuyen tuong");
        Vec3 safe = p.position();
        ThoService.tick(p); // ghi vi tri an toan
        p.setPos(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5); // vao trong bedrock
        ThoService.tick(p);
        boolean inside = ThoPhase.blocked(lv, p.getBoundingBox());
        Vec3 now = p.position();
        lv.setBlock(bed, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        ThoService.reset(p);
        if (inside) h.fail("van o trong bedrock sau tick: " + now);
        if (now.distanceTo(safe) > 2) h.fail("khong bi keo ve cho an toan: " + now + " vs " + safe);
        h.succeed();
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
