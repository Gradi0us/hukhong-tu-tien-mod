package com.hukhong.phapkhi;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.stats.StatService;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
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

/** Pháp Khí: chủ giữ theo tham chiếu (FakePlayer không nằm trong PlayerList); quái đứng yên, di chuyển bằng setPos. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class PhapKhiGameTests {
    private PhapKhiGameTests() {}

    /** Máu vanilla tối đa 1024 (RangedAttribute): quái 'máu vô hạn' thực chất là 1024. */
    static final float HP = 1024f;

    /** Người dùng là Trận Tu: Pháp Khí công/hộ/trận không phụ thuộc đạo. */
    static ServerPlayer player(GameTestHelper h, int resource) { return player(h, resource, 60); }

    static ServerPlayer player(GameTestHelper h, int resource, int level) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-phapkhi"));
        p.getInventory().clearContent();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 1000, 1000, Set.of(Path.TRAN), Map.of(Path.TRAN, Aptitude.SSS), false, "", null, false));
        StatService.refresh(p);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(Math.min(resource, cd.resourceMax())));
        Vec3 b = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        p.moveTo(b.x, b.y, b.z, 0f, 0f);
        return p;
    }

    static Zombie zombie(GameTestHelper h, ServerPlayer p, double ahead) {
        Zombie z = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 at = p.position().add(0, 0.7, ahead);
        z.moveTo(at.x, at.y, at.z);
        z.setNoGravity(true);
        z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0E6);
        z.setHealth(HP);
        z.setPersistenceRequired();
        h.getLevel().addFreshEntity(z);
        return z;
    }

    static ItemStack hold(ServerPlayer p, PhapKhiKind k, PhapKhiTier t) {
        ItemStack s = PhapKhiRegistry.stack(k, t);
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        return s;
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "phapkhi_attack")
    public static void attackTalismanDamagesAndBreaks(GameTestHelper h) {
        ServerPlayer p = player(h, 1000);
        Zombie z = zombie(h, p, 4);
        ItemStack s = hold(p, PhapKhiKind.KIEM, PhapKhiTier.PHAM);
        s.set(PhapKhiRegistry.USES.get(), 2);
        if (!PhapKhiItem.activate(p, s)) h.fail("activate that bai");
        if (PhapKhiItem.usesLeft(s) != 1) h.fail("khong tru 1 lan: con " + PhapKhiItem.usesLeft(s));
        if (z.getHealth() >= HP) h.fail("phu Cong khong gay sat thuong");
        if (!PhapKhiItem.activate(p, s)) h.fail("activate lan 2 that bai");
        if (!s.isEmpty()) h.fail("phu het lan nhung khong vo (count=" + s.getCount() + ")");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "phapkhi_tier")
    public static void thienStrikeBeatsPham(GameTestHelper h) {
        ServerPlayer p = player(h, 1000, 5);
        Zombie z = zombie(h, p, 4);
        PhapKhiItem.activate(p, hold(p, PhapKhiKind.KIEM, PhapKhiTier.PHAM));
        float dPham = HP - z.getHealth();
        z.setHealth(HP);
        StringBuilder seen = new StringBuilder();
        for (PhapKhiTier t : PhapKhiTier.values()) {
            z.setHealth(HP);
            PhapKhiItem.activate(p, hold(p, PhapKhiKind.KIEM, t));
            seen.append(t.id).append('=').append(HP - z.getHealth()).append(' ');
        }
        z.setHealth(HP);
        PhapKhiItem.activate(p, hold(p, PhapKhiKind.KIEM, PhapKhiTier.THIEN));
        float dThien = HP - z.getHealth();
        if (dPham <= 0) h.fail("Pham khong gay sat thuong");
        if (dThien <= dPham * 2) h.fail("Thien (" + dThien + ") khong vuot Pham (" + dPham + ") ro ret; power=" + PhapKhiAttack.power(p) + " alive=" + z.isAlive() + " hp=" + z.getHealth() + " tiers=" + seen);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "phapkhi_defense")
    public static void hoThanGivesAbsorption(GameTestHelper h) {
        ServerPlayer p = player(h, 1000);
        if (p.getAbsorptionAmount() > 0) h.fail("absorption ban dau phai 0");
        if (!PhapKhiItem.activate(p, hold(p, PhapKhiKind.HO_THAN, PhapKhiTier.PHAM))) h.fail("activate that bai");
        float want = (float) (p.getMaxHealth() * PhapKhiDefense.SHIELD_PCT[0]);
        if (p.getAbsorptionAmount() < want - 0.01f) h.fail("absorption " + p.getAbsorptionAmount() + " < " + want);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "phapkhi_defense")
    public static void kimCangReducesDamage(GameTestHelper h) {
        ServerPlayer p = player(h, 1000);
        PhapKhiItem.activate(p, hold(p, PhapKhiKind.KIM_CANG, PhapKhiTier.PHAM));
        var holder = h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC);
        var ev = new LivingIncomingDamageEvent(p, new DamageContainer(new DamageSource(holder), 10f));
        NeoForge.EVENT_BUS.post(ev);
        if (Math.abs(ev.getAmount() - 7f) > 0.05f) h.fail("Kim Cang Pham: 10 con " + ev.getAmount() + ", ky vong 7");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "phapkhi_defense")
    public static void donKhongMovesPlayer(GameTestHelper h) {
        ServerPlayer p = player(h, 1000);
        Vec3 before = p.position();
        if (!PhapKhiItem.activate(p, hold(p, PhapKhiKind.DON_KHONG, PhapKhiTier.PHAM))) h.fail("activate that bai");
        // template empty la hop kin ~5 block: tia nhin chan o ~2.5 block nen chi kiem tra chop theo huong nhin va dung truoc vat can
        double moved = p.position().distanceTo(before);
        if (moved < 1.5) h.fail("Don Khong chi di " + moved + " block");
        if (p.position().z <= before.z) h.fail("khong di ve huong nhin (+Z)");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80, batch = "phapkhi_khon")
    public static void khonTranSlowsZombie(GameTestHelper h) {
        ServerPlayer p = player(h, 1000);
        Zombie z = zombie(h, p, 2);
        PhapKhiItem.activate(p, hold(p, PhapKhiKind.KHON, PhapKhiTier.PHAM));
        if (PhapKhiZones.count() < 1) h.fail("Khon Tran Ban khong tao vung");
        h.runAfterDelay(15, () -> {
            if (!PhapKhiZones.isSlowed(z)) h.fail("zombie trong tran khong bi cham");
            if (!z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) h.fail("zombie khong bi troi khi vao tran");
            z.setPos(z.getX() + 30, z.getY(), z.getZ()); // ra khoi tran: het cham
        });
        h.runAfterDelay(30, () -> {
            if (PhapKhiZones.isSlowed(z)) h.fail("zombie ra khoi tran van bi cham");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80, batch = "phapkhi_tulinh")
    public static void tuLinhRegensResource(GameTestHelper h) {
        ServerPlayer p = player(h, 100);
        int before = p.getData(ModAttachments.CULTIVATION).resource();
        float hp = p.getMaxHealth();
        p.setHealth(hp * 0.5f);
        PhapKhiItem.activate(p, hold(p, PhapKhiKind.TU_LINH, PhapKhiTier.PHAM));
        h.runAfterDelay(45, () -> {
            int after = p.getData(ModAttachments.CULTIVATION).resource();
            if (after <= before) h.fail("Tu Linh khong hoi linh luc: " + before + " -> " + after + " " + PhapKhiZones.debug(p));
            if (p.getHealth() <= hp * 0.5f) h.fail("Tu Linh khong hoi mau");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80, batch = "phapkhi_sat")
    public static void satTranDamagesZombie(GameTestHelper h) {
        ServerPlayer p = player(h, 1000);
        Zombie z = zombie(h, p, 2);
        PhapKhiItem.activate(p, hold(p, PhapKhiKind.SAT, PhapKhiTier.PHAM));
        h.runAfterDelay(30, () -> {
            if (z.getHealth() >= HP) h.fail("Sat Tran khong gay sat thuong");
            h.succeed();
        });
    }

    /** Mọi phù Công x 2 chế độ x 5 phẩm thi triển được không lỗi (VFX + sát thương + việc hẹn giờ), và gây sát thương lên quái đứng trước. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "phapkhi_all")
    public static void allAttackKindsBothModesRun(GameTestHelper h) {
        ServerPlayer p = player(h, 1000, 5);
        Zombie z = zombie(h, p, 2);
        for (PhapKhiKind k : PhapKhiKind.values()) {
            if (k.cat != PhapKhiKind.Cat.ATTACK) continue;
            for (boolean skill : new boolean[]{false, true}) {
                for (PhapKhiTier t : PhapKhiTier.values()) {
                    z.setHealth(HP);
                    z.invulnerableTime = 0;
                    ItemStack s = hold(p, k, t);
                    s.set(PhapKhiRegistry.SKILL_MODE.get(), skill);
                    if (!PhapKhiItem.activate(p, s)) h.fail(k + " " + t + " skill=" + skill + " khong thi trien duoc");
                }
            }
        }
        h.runAfterDelay(30, () -> h.succeed());
    }

    /** Hộ / Trận: mọi loại x 5 phẩm thi triển được. */
    @GameTest(template = "empty", timeoutTicks = 60, batch = "phapkhi_all")
    public static void allSupportKindsRun(GameTestHelper h) {
        ServerPlayer p = player(h, 1000, 5);
        net.minecraft.world.phys.Vec3 home = p.position();
        for (PhapKhiKind k : PhapKhiKind.values()) {
            if (k.cat == PhapKhiKind.Cat.ATTACK) continue;
            for (PhapKhiTier t : PhapKhiTier.values()) {
                p.moveTo(home.x, home.y, home.z, p.getYRot(), p.getXRot()); // Độn Không lần trước dời người sát tường hộp test
                p.getCooldowns().removeCooldown(PhapKhiRegistry.item(k, t));
                if (!PhapKhiItem.activate(p, hold(p, k, t))) h.fail(k + " " + t + " khong thi trien duoc");
            }
        }
        h.succeed();
    }
}
