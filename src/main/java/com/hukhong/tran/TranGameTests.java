package com.hukhong.tran;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.skill.Skill;
import com.hukhong.skill.SkillService;
import com.hukhong.skillbook.SkillBook;
import com.hukhong.skillbook.SkillBookService;
import com.hukhong.stats.StatService;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Trận Tu: thế (T + Q/E/R), khoá 4 phím, Giam Cầm, Kiệt Sức, Hồi Linh; qua đúng đường SkillService.cast. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class TranGameTests {
    private TranGameTests() {}

    /** Người chơi giả: giữ tham chiếu (không nằm trong PlayerList), nhìn +Z. */
    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-tran"));
        p.getInventory().clearContent();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(60, 0, 1000, 1000, Set.of(Path.TRAN), Map.of(Path.TRAN, Aptitude.SSS), false, "", null, false));
        StatService.refresh(p);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
        SkillService.state(p).stance = true;
        Vec3 b = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        p.moveTo(b.x, b.y, b.z, 0f, 0f);
        return p;
    }

    /** Quái đứng yên, không AI di chuyển, máu rất lớn, cách người chơi {@code ahead} block phía trước. */
    static Zombie zombie(GameTestHelper h, ServerPlayer p, double ahead) {
        Zombie z = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 at = p.position().add(0, 0.7, ahead);
        z.moveTo(at.x, at.y, at.z);
        z.setNoGravity(true);
        z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0E8);
        z.setHealth(1.0E8f);
        z.setPersistenceRequired();
        h.getLevel().addFreshEntity(z);
        return z;
    }

    /** Vào thế: T (chọn thế) rồi phím đúng. Trả về thế sau khi bấm. */
    static void enterStance(ServerPlayer p, int slot) {
        SkillService.cast(p, 3);
        SkillService.cast(p, slot);
    }

    /** Hồi chiêu được reset giữa các lần bấm để test không phụ thuộc thời gian. */
    static void castReset(ServerPlayer p, int slot) {
        SkillService.state(p).cooldownEnd[slot] = 0;
        SkillService.cast(p, slot);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void stanceEnterAndReset(GameTestHelper h) {
        ServerPlayer p = player(h);
        SkillService.cast(p, 3); // T: chọn thế
        if (TranRuntime.stance(p) != TranStance.BASE) h.fail("T khong duoc doi thanh chon the, dang o " + TranRuntime.stance(p));
        SkillService.cast(p, 0); // Q khi đang chọn thế: vào Khống Chế (không cast Khốn Trận)
        if (TranRuntime.stance(p) != TranStance.KHONG_CHE) h.fail("Q khong vao the Khong Che: " + TranRuntime.stance(p));
        if (SkillService.state(p).cooldownEnd[0] > h.getLevel().getGameTime()) h.fail("Q chon the khong duoc cast ngay: hoi chieu bi dat");
        SkillService.cast(p, 3); // T trong thế: về BASE
        if (TranRuntime.stance(p) != TranStance.BASE) h.fail("T trong the khong ve BASE: " + TranRuntime.stance(p));
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void stanceResetPreventsImmediateRearm(GameTestHelper h) {
        ServerPlayer p = player(h);
        enterStance(p, 2); // PHONG_NGU
        SkillService.cast(p, 3); // về BASE, bắt đầu hồi 1 giây
        SkillService.cast(p, 3); // bấm lại ngay: chưa được chọn thế
        if (TranRuntime.stance(p) != TranStance.BASE) h.fail("T lap tuc sau khi ve BASE van chon the");
        if (TranRuntime.st(p).armedUntil >= h.getLevel().getGameTime()) h.fail("the duoc chon khi chua het hoi 1 giay");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void tranSlotsLockedAgainstBooks(GameTestHelper h) {
        ServerPlayer p = player(h);
        var bd = SkillBookService.data(p);
        for (int slot = 0; slot < 4; slot++) {
            SkillBook.Resolved r = SkillBook.resolve(bd, Path.TRAN, slot, 60, TranMath.LOCK_MASK);
            Skill want = Skill.of(Path.TRAN, slot);
            if (r == null || r.base() != want) h.fail("Phim " + slot + " Tran khong bi khoa: " + (r == null ? "null" : r.id()) + " ky vong " + (want == null ? null : want.icon));
        }
        if (SkillBook.fixedId(Path.TRAN, TranMath.LOCK_MASK, Skill.TRAN_CAGE.icon)) {
            // kỹ năng của thế không phải phím cố định của trang: không được coi là khoá gán
            h.fail("kỹ năng thế không được coi là phím cố định của trang");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "tran_pull")
    public static void khonTranRoots(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie z = zombie(h, p, 6);
        SkillService.cast(p, 0); // Q (BASE): Khốn Trận
        if (SkillService.state(p).cooldownEnd[0] <= h.getLevel().getGameTime()) h.fail("Q khong thuc thi (khong dat hoi chieu)");
        h.runAfterDelay(45, () -> {
            if (z.getHealth() >= 1.0E8f) h.fail("Khon Tran khong gay sat thuong");
            if (!z.isNoAi()) h.fail("Khon Tran khong choang muc tieu sau 45 tick (hp=" + z.getHealth() + ")");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hoThanReduces(GameTestHelper h) {
        ServerPlayer p = player(h);
        SkillService.cast(p, 2); // R (BASE): Hộ Thân Trận quanh người chơi
        var holder = h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC);
        // FakePlayer.hurt luôn bị chặn nên phát sự kiện đầu vào trực tiếp
        var ev = new LivingIncomingDamageEvent(p, new DamageContainer(new DamageSource(holder), 10f));
        NeoForge.EVENT_BUS.post(ev);
        float out = ev.getAmount();
        if (Math.abs(out - 7f) > 0.05f) h.fail("Ho Than Tran: 10 sat thuong con " + out + ", ky vong ~7 (-30%)");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "tran_cage")
    public static void cageKeepsMobInside(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie z = zombie(h, p, 6);
        enterStance(p, 0); // KHONG_CHE
        SkillService.cast(p, 0); // Giam Cầm Trận tại quái
        h.runAfterDelay(5, () -> {
            if (TranRuntime.countOwned(TranRuntime.Kind.CAGE, p) != 1) h.fail("Giam Cam Tran khong duoc tao");
            Vec3 c = z.position();
            z.setPos(c.x + TranMath.CAGE_RADIUS + 3, c.y, c.z); // thử đẩy quái ra ngoài lồng
        });
        h.runAfterDelay(12, () -> {
            var cage = TranRuntime.ownedZone(TranRuntime.Kind.CAGE, p);
            if (cage == null) h.fail("Long vo qua som");
            double d = Math.hypot(z.getX() - cage.c.x, z.getZ() - cage.c.z);
            if (d > TranMath.CAGE_RADIUS + 0.01) h.fail("Quai thoat long: khoang cach " + d);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "tran_cage")
    public static void cageBreaksAfterHits(GameTestHelper h) {
        ServerPlayer p = player(h);
        zombie(h, p, 6);
        zombie(h, p, 6.5);
        enterStance(p, 0);
        SkillService.cast(p, 0); // lồng với 2 quái: giới hạn đập = 6 + 2*2 = 10, mỗi giây 2 đập -> vỡ ~100 tick
        h.runAfterDelay(60, () -> {
            if (TranRuntime.countOwned(TranRuntime.Kind.CAGE, p) != 1) h.fail("Long vo qua som (60 tick)");
        });
        h.runAfterDelay(130, () -> {
            if (TranRuntime.countOwned(TranRuntime.Kind.CAGE, p) != 0) h.fail("Long khong vo sau 10 lan dap (con " + TranRuntime.countOwned(TranRuntime.Kind.CAGE, p) + ")");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void kietSucAtFiveCasts(GameTestHelper h) {
        ServerPlayer p = player(h);
        enterStance(p, 1); // SAT_THUONG
        for (int i = 0; i < 4; i++) castReset(p, 1); // 4 Kiệt Lực (E Vạn Kiếm): chưa Kiệt Sức; Q là Sát Trận giữ-để-nạp
        if (TranRuntime.exhausted(p)) h.fail("Kiet Suc qua som (4 lan)");
        castReset(p, 1); // lần thứ 5
        if (!TranRuntime.exhausted(p)) h.fail("Chua Kiet Suc sau 5 lan cast");
        if (!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || !p.hasEffect(MobEffects.WEAKNESS)) h.fail("Thieu Slowness/Weakness khi Kiet Suc");
        if (TranRuntime.st(p).stacks != 0) h.fail("Kiet Luc khong reset sau Kiet Suc: " + TranRuntime.st(p).stacks);
        h.succeed();
    }

    /** Sát Trận giữ-để-nạp: quái trong vòng tụ lực bị làm chậm; giữ 3 giây gây nhiều hơn nhả ngay; thả xong Kiệt Sức. */
    @GameTest(template = "empty", timeoutTicks = 160, batch = "tran_charge")
    public static void chargeFormation(GameTestHelper h) {
        ServerPlayer p = player(h);
        enterStance(p, 1); // SAT_THUONG
        Zombie quick = zombie(h, p, 6);
        TranRuntime.chargeStart(p);
        h.runAfterDelay(2, () -> {
            TranRuntime.chargeRelease(p); // thả gần như ngay: cột sáng nhỏ nhất
            double shortDmg = 1.0E8 - quick.getHealth();
            if (shortDmg <= 0) h.fail("Tu hoa ngan khong gay sat thuong");
            quick.discard();
            SkillService.state(p).cooldownEnd[0] = 0; // bỏ hồi chiêu để kiểm lần giữ thứ hai
            Zombie held = zombie(h, p, 6);
            TranRuntime.chargeStart(p);
            h.runAfterDelay(60, () -> {
                if (!held.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) h.fail("Quai trong vong tu luc khong bi lam cham");
                TranRuntime.chargeRelease(p); // giữ đủ 3 giây: cột sáng tối đa
                double longDmg = 1.0E8 - held.getHealth();
                if (longDmg <= shortDmg) h.fail("Giu 3 giay khong gay hon nha ngay: " + longDmg + " <= " + shortDmg);
                if (!TranRuntime.exhausted(p)) h.fail("Chua Kiet Suc sau cot sang Sat Tran");
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "tran_heal")
    public static void healingFormationHeals(GameTestHelper h) {
        ServerPlayer p = player(h);
        enterStance(p, 2); // PHONG_NGU
        p.setHealth(p.getMaxHealth() / 2);
        float before = p.getHealth();
        castReset(p, 1); // E: Hồi Linh Trận
        h.runAfterDelay(25, () -> {
            if (p.getHealth() <= before) h.fail("Hoi Linh Tran khong hoi mau: " + before + " -> " + p.getHealth());
            h.succeed();
        });
    }

    /** 2026-11-06: mỗi thế hồi chiêu riêng - dùng Q ở Cơ bản rồi đổi sang Khống Chế thì Q dùng được ngay; quay về thì hồi chiêu cũ còn nguyên. */
    @GameTest(template = "empty")
    public static void tranStanceCooldownsIndependent(GameTestHelper h) {
        ServerPlayer p = player(h);
        long now = h.getLevel().getGameTime();
        var st = SkillService.state(p);
        st.cooldownEnd[0] = now + 500;
        TranRuntime.onKey(p, 3); // T: chọn thế
        TranRuntime.onKey(p, 0); // Q: vào Khống Chế
        if (TranRuntime.stance(p) != TranStance.KHONG_CHE) { h.fail("khong vao duoc the Khong Che"); return; }
        if (st.cooldownEnd[0] > now) { h.fail("Q the Khong Che bi dinh hoi chieu cua the Co ban"); return; }
        if (TranRuntime.storedCooldown(p, TranStance.BASE, 0) != now + 500) { h.fail("khong cat hoi chieu the Co ban"); return; }
        st.cooldownEnd[1] = now + 300; // dùng E ở Khống Chế
        TranRuntime.onKey(p, 3); // về Cơ bản
        if (st.cooldownEnd[0] != now + 500 || st.cooldownEnd[1] > now) { h.fail("ve Co ban khong nap lai hoi chieu cu: " + st.cooldownEnd[0] + " / " + st.cooldownEnd[1]); return; }
        if (TranRuntime.storedCooldown(p, TranStance.KHONG_CHE, 1) != now + 300) { h.fail("khong cat hoi chieu Khong Che"); return; }
        h.succeed();
    }

    /** Hộ Thân Trận (R cơ bản): đi theo chân, còn sau khi đổi thế, gây sát thương mỗi giây cho kẻ địch trong vùng. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void tranHoThanFollowsAndDamages(GameTestHelper h) {
        ServerPlayer p = player(h);
        TranSkills.run(com.hukhong.skill.Skill.TRAN_3, p);
        if (!TranRuntime.hasHoThan(p)) { h.fail("khong dung Ho Than Tran"); return; }
        Zombie z = zombie(h, p, 2);
        float hp = z.getHealth();
        TranRuntime.onKey(p, 3);
        TranRuntime.onKey(p, 1); // sang Sát Thương
        Vec3 moved = p.position().add(1.5, 0, 0);
        p.moveTo(moved.x, moved.y, moved.z);
        h.runAfterDelay(45, () -> {
            if (!TranRuntime.hasHoThan(p)) { h.fail("doi the lam mat Ho Than Tran"); return; }
            if (z.getHealth() >= hp) { h.fail("Ho Than Tran khong gay sat thuong theo thoi gian"); return; }
            h.succeed();
        });
    }

    /** Vạn Kiếm Trận (E Sát Thương): kiếm chia cho nhiều mục tiêu; 1 mục tiêu chịu tối đa 60%. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void tranBladesSplitTargets(GameTestHelper h) {
        if (Math.abs(TranMath.bladeMult(12, 12) - 0.6) > 1e-9 || Math.abs(TranMath.bladeMult(6, 12) - 0.8) > 1e-9) { h.fail("he so giam sai"); return; }
        ServerPlayer p = player(h);
        p.setXRot(30);
        Zombie a = zombie(h, p, 4), b = zombie(h, p, 5);
        b.moveTo(b.getX() + 1.5, b.getY(), b.getZ());
        float ha = a.getHealth(), hb = b.getHealth();
        TranSkills.blades(h.getLevel(), p, StatService.spellPower(p));
        h.runAfterDelay(50, () -> {
            if (a.getHealth() >= ha || b.getHealth() >= hb) { h.fail("kiem khong chia du 2 muc tieu"); return; }
            h.succeed();
        });
    }
}
