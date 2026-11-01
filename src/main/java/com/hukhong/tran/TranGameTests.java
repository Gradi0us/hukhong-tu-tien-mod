package com.hukhong.tran;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.skill.SkillService;
import com.hukhong.stats.StatService;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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

/** Trận Tu: Q khống chế, E giảm sát thương, T kéo + hồi chiêu, qua đúng đường SkillService.cast (phím thật). */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class TranGameTests {
    private TranGameTests() {}

    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-tran"));
        p.getInventory().clearContent();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(60, 0, 1000, 1000, Set.of(Path.TRAN), Map.of(Path.TRAN, Aptitude.SSS), false, "", null, false));
        StatService.refresh(p);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
        SkillService.state(p).stance = true;
        Vec3 b = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        p.moveTo(b.x, b.y, b.z, 0f, 0f); // yaw 0 = nhìn +Z
        return p;
    }

    /** Zombie đứng yên (tốc độ 0, không AI) cách người chơi {@code ahead} block phía trước. */
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

    @GameTest(template = "empty", timeoutTicks = 100, batch = "tran_pull")
    public static void khonTranRoots(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie z = zombie(h, p, 6);
        SkillService.cast(p, 0); // Q
        if (SkillService.state(p).cooldownEnd[0] <= h.getLevel().getGameTime()) h.fail("Q khong thuc thi (khong dat hoi chieu)");
        h.runAfterDelay(45, () -> {
            if (z.getHealth() >= 1.0E8f) h.fail("Khon Tran khong gay sat thuong");
            if (!z.isNoAi()) h.fail("Khon Tran khong choang muc tieu sau 45 tick (hp=" + z.getHealth() + ", pos=" + z.position() + ", ky vong gan " + p.position().add(0, 0.7, 6) + ")");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hoThanReduces(GameTestHelper h) {
        ServerPlayer p = player(h);
        SkillService.cast(p, 1); // E: lập Hộ Thân Trận quanh người chơi
        var holder = h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC);
        // FakePlayer.hurt luôn bị chặn nên phát sự kiện đầu vào trực tiếp (cùng cách DongDaoGameTests)
        var ev = new LivingIncomingDamageEvent(p, new DamageContainer(new DamageSource(holder), 10f));
        NeoForge.EVENT_BUS.post(ev);
        float out = ev.getAmount();
        if (Math.abs(out - 7f) > 0.05f) h.fail("Ho Than Tran: 10 sat thuong con " + out + ", ky vong ~7 (-30%)");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "tran_pull")
    public static void daiTranPulls(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie z = zombie(h, p, 10);
        double d0 = z.position().distanceTo(p.position());
        SkillService.cast(p, 3); // T: Đại Trận, kéo quái về tâm
        int zones = TranRuntime.zoneCount();
        h.runAfterDelay(6, () -> {
            if (TranRuntime.zoneCount() == 0) h.fail("Dai Tran khong duoc tao (zones=" + zones + ")");
        });
        h.runAfterDelay(40, () -> {
            double d1 = z.position().distanceTo(p.position());
            if (d0 - d1 < 1.5) h.fail("Dai Tran khong keo quai: khoang cach " + d0 + " -> " + d1 + ", zones=" + TranRuntime.zoneCount() + ", pulls=" + TranRuntime.pullCount() + ", vel=" + z.getDeltaMovement() + ", hp=" + z.getHealth());
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void daiTranCooldownHalf(GameTestHelper h) {
        ServerPlayer p = player(h);
        long now = h.getLevel().getGameTime();
        SkillService.cast(p, 0); // Q ngoài Đại Trận
        long outside = SkillService.state(p).cooldownEnd[0] - now;
        SkillService.state(p).cooldownEnd[0] = 0;
        SkillService.cast(p, 3); // T: lập Đại Trận quanh người chơi
        SkillService.state(p).cooldownEnd[0] = 0;
        SkillService.cast(p, 0); // Q trong Đại Trận
        long inside = SkillService.state(p).cooldownEnd[0] - now;
        if (outside <= 0) h.fail("Q ngoai tran khong co hoi chieu: " + outside);
        if (inside >= outside || inside > outside * 0.5 + 1) h.fail("Hoi chieu trong Dai Tran = " + inside + ", ngoai = " + outside + " (ky vong x0,5)");
        h.succeed();
    }
}
