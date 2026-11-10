package com.hukhong.dodge;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.effect.ModEffects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
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

/**
 * Lách Né: tiêu Thể Lực, i-frame chặn sát thương trong cửa sổ (không phải sau), Né Hoàn Hảo cho buff, Thể Lực hồi.
 * FakePlayer miễn mọi sát thương ở isInvulnerableTo nên đòn được mô phỏng bằng cách post LivingIncomingDamageEvent
 * lên bus thật (đi qua đúng handler đã đăng ký trong DodgeMod).
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class DodgeGameTests {
    private DodgeGameTests() {}

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-dodge"));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(ModAttachments.CULTIVATION, new CultivationData(15, 0, 0, 100, Set.of(Path.THE), Map.of(Path.THE, Aptitude.B), false));
        p.setData(ModAttachments.DODGE, DodgeData.DEFAULT);
        Vec3 v = h.absoluteVec(new Vec3(2, 2, 2));
        p.moveTo(v.x, v.y, v.z, 0f, 0f);
        p.removeAllEffects();
        return p;
    }

    private static Zombie zombie(GameTestHelper h) {
        Zombie z = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 v = h.absoluteVec(new Vec3(4, 2, 4));
        z.moveTo(v.x, v.y, v.z, 0f, 0f);
        return z; // không add vào thế giới: chỉ làm nguồn sát thương
    }

    /** Trả về true nếu đòn bị hủy. */
    private static boolean hit(ServerPlayer p, DamageSource s) {
        return NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(p, new DamageContainer(s, 5f))).isCanceled();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dodge_tieu_the_luc_va_lao(GameTestHelper h) {
        ServerPlayer p = player(h);
        float max = DodgeService.max(p);
        if (!DodgeService.tryDodge(p, 1f, 0f)) { h.fail("lach ne that bai khi day the luc"); return; }
        float after = DodgeService.stamina(p);
        float cost = StaminaMath.dodgeCost(15);
        if (Math.abs(max - cost - after) > 1e-3) { h.fail("the luc " + max + " -> " + after + ", mong doi tru " + cost); return; }
        if (!(p.getDeltaMovement().z > 0.5)) { h.fail("khong lao toi (+Z): " + p.getDeltaMovement()); return; }
        if (DodgeService.tryDodge(p, 1f, 0f)) { h.fail("hoi chieu khong chan lach lien tiep"); return; }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dodge_khong_du_the_luc(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setData(ModAttachments.DODGE, new DodgeData(5f, -1000, -1000, 0, true));
        if (DodgeService.tryDodge(p, 0f, 0f)) h.fail("lach duoc khi the luc 5");
        else h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void dodge_iframe_chi_trong_cua_so(GameTestHelper h) {
        ServerPlayer p = player(h);
        Zombie z = zombie(h);
        DamageSource src = h.getLevel().damageSources().mobAttack(z);
        if (hit(p, src)) { h.fail("don bi huy khi chua lach"); return; }
        DodgeService.tryDodge(p, 0f, 0f);
        long start = h.getLevel().getGameTime();
        // Bỏ qua cửa sổ hoàn hảo để chỉ kiểm i-frame thường
        h.runAtTickTime(h.getTick() + StaminaMath.PERFECT_WINDOW + 1, () -> {
            long dt = h.getLevel().getGameTime() - start;
            if (dt >= StaminaMath.IFRAMES) { h.fail("tick lech, dt=" + dt); return; }
            if (!hit(p, src)) { h.fail("don khong bi huy trong i-frame (dt=" + dt + ")"); return; }
            if (p.hasEffect(ModEffects.THAN_PHAP)) { h.fail("ngoai cua so hoan hao van co Than Phap"); return; }
            if (hit(p, h.getLevel().damageSources().fellOutOfWorld())) { h.fail("void phai xuyen i-frame"); return; }
        });
        h.runAtTickTime(h.getTick() + StaminaMath.IFRAMES + 2, () -> {
            if (hit(p, src)) h.fail("don van bi huy sau khi het i-frame");
            else h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dodge_ne_hoan_hao_cho_than_phap(GameTestHelper h) {
        ServerPlayer p = player(h);
        DodgeService.tryDodge(p, 0f, 1f);
        if (!hit(p, h.getLevel().damageSources().mobAttack(zombie(h)))) { h.fail("don khong bi huy"); return; }
        if (!p.hasEffect(ModEffects.THAN_PHAP)) { h.fail("khong co buff Than Phap"); return; }
        if (!DodgeService.data(p).perfectUsed()) { h.fail("perfectUsed chua dat"); return; }
        p.removeEffect(ModEffects.THAN_PHAP);
        hit(p, h.getLevel().damageSources().mobAttack(zombie(h)));
        if (p.hasEffect(ModEffects.THAN_PHAP)) { h.fail("mot lan lach cho buff 2 lan"); return; }
        // môi trường (rơi) bị né nhưng không tính hoàn hảo
        ServerPlayer q = player(h);
        DodgeService.tryDodge(q, 0f, 0f);
        hit(q, h.getLevel().damageSources().fall());
        if (q.hasEffect(ModEffects.THAN_PHAP)) { h.fail("sat thuong roi khong duoc tinh ne hoan hao"); return; }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void the_luc_hoi_sau_khi_tieu(GameTestHelper h) {
        ServerPlayer p = player(h);
        DodgeService.tryDodge(p, 0f, 0f);
        float low = DodgeService.stamina(p);
        DodgeService.tickPlayer(p);
        if (DodgeService.stamina(p) != low) { h.fail("hoi ngay khi vua tieu (phai dung ~1s)"); return; }
        h.runAtTickTime(h.getTick() + StaminaMath.REGEN_DELAY + 1, () -> {
            for (int i = 0; i < 5; i++) DodgeService.tickPlayer(p);
            float now = DodgeService.stamina(p);
            if (!(now > low)) { h.fail("the luc khong hoi: " + low + " -> " + now); return; }
            p.setSprinting(true);
            DodgeService.tickPlayer(p);
            if (!(DodgeService.stamina(p) < now)) { h.fail("chay nhanh khong tieu the luc"); return; }
            h.succeed();
        });
    }
}
