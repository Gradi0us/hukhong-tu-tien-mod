package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.skill.SkillService;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Quang E Thiểm Bộ: bấm nhanh = lùi + Bá Thể 3s (hurt không mất máu); giữ = tốc độ + hao Pháp Lực; nhả = gỡ modifier. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ThiemBoGameTests {
    private ThiemBoGameTests() {}

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-tb"));
        p.getInventory().clearContent();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(60, 0, 1000, 1000, Set.of(Path.PHAP), Map.of(Path.PHAP, Aptitude.SSS), false, "", null, false));
        PhapTu.setElement(p, PhapElement.QUANG);
        StatService.refresh(p);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
        SkillService.state(p).stance = true;
        Vec3 b = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        p.moveTo(b.x, b.y, b.z, 0f, 0f); // yaw 0 = nhìn +Z
        return p;
    }

    private static int res0max(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).resourceMax(); }

    private static int res(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).resource(); }

    @GameTest(template = "empty")
    public static void bam_nhanh_lui_va_ba_the(GameTestHelper h) {
        ServerPlayer p = player(h);
        int before = res(p);
        long now = h.getLevel().getGameTime();
        p.setYRot(180f); // nhìn -Z (tường barrier phía sau ở -Z bị tránh: lùi về +Z)
        p.setXRot(0f);
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        ThiemBoService.tick(p);
        ThiemBoService.action(p, ThiemBoPayload.RELEASE); // 1 tick < HOLD_TICKS => bấm nhanh
        if (ThiemBoService.sprinting(p)) h.fail("tap khong duoc kich hoat sprint");
        Vec3 dm = p.getDeltaMovement();
        if (!(dm.z > 0.5)) h.fail("khong lui ve sau (nhin -Z): " + dm);
        if (Math.abs(dm.x) > 1e-6) h.fail("lech ngang: " + dm);
        if (res(p) >= before) h.fail("khong tru mana: " + res(p));
        if (SkillService.state(p).cooldownEnd[1] - now < ThiemBoMath.COOLDOWN - 1) h.fail("cooldown khong phai " + ThiemBoMath.COOLDOWN);
        if (!ThiemBoService.immune(p)) h.fail("khong co Ba The");
        float hp = p.getHealth();
        p.invulnerableTime = 0;
        p.hurt(h.getLevel().damageSources().generic(), 8f);
        if (p.getHealth() != hp) h.fail("van mat mau khi Ba The: " + hp + " -> " + p.getHealth());
        if (ThiemBoService.immune(p, now + ThiemBoMath.IMMUNE_TICKS + 1)) h.fail("Ba The khong het sau 3s");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void giu_chay_hao_mana_nha_go_modifier(GameTestHelper h) {
        ServerPlayer p = player(h);
        int before = res(p);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        double base = speed.getValue();
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        for (int i = 0; i < ThiemBoMath.HOLD_TICKS + 20; i++) ThiemBoService.tick(p);
        if (!ThiemBoService.sprinting(p)) h.fail("khong vao sprint");
        if (speed.getValue() < base * (1 + ThiemBoMath.SPEED_BONUS) - 1e-6) h.fail("toc do chua tang: " + base + " -> " + speed.getValue());
        if (!p.isSprinting()) h.fail("khong ep sprint");
        int drained = before - res(p);
        double exp = res0max(p) * ThiemBoMath.DRAIN_PCT;
        if (Math.abs(drained - exp) > 2) h.fail("hao mana ~" + exp + "/s mong doi, thuc te " + drained + " sau 20 tick");
        if (SkillService.state(p).cooldownEnd[1] > h.getLevel().getGameTime()) h.fail("giu E khong duoc dat cooldown");
        if (ThiemBoService.immune(p)) h.fail("giu E khong duoc cap Ba The");
        ThiemBoService.action(p, ThiemBoPayload.RELEASE);
        if (ThiemBoService.sprinting(p)) h.fail("nha ma van sprint");
        if (ThiemBoService.hasSpeedModifier(p) || Math.abs(speed.getValue() - base) > 1e-9) h.fail("modifier chua duoc go: " + speed.getValue());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void het_mana_dung_va_tat_the_dung(GameTestHelper h) {
        ServerPlayer p = player(h);
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(ThiemBoMath.minStartMana(cd.resourceMax()) + 20));
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        for (int i = 0; i < 80 && (i < ThiemBoMath.HOLD_TICKS + 1 || ThiemBoService.sprinting(p)); i++) ThiemBoService.tick(p);
        if (ThiemBoService.sprinting(p)) h.fail("het mana ma van chay");
        if (ThiemBoService.hasSpeedModifier(p)) h.fail("modifier con sot");
        ThiemBoService.action(p, ThiemBoPayload.RELEASE);
        // mana thấp hơn mức tối thiểu: không bắt đầu được
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        for (int i = 0; i < 10; i++) ThiemBoService.tick(p);
        if (ThiemBoService.sprinting(p)) h.fail("bat dau duoc khi thieu mana toi thieu");
        ThiemBoService.action(p, ThiemBoPayload.RELEASE);
        // tắt thế khi đang chạy
        cd = p.getData(ModAttachments.CULTIVATION);
        p.setData(ModAttachments.CULTIVATION, cd.withResource(cd.resourceMax()));
        ThiemBoService.action(p, ThiemBoPayload.PRESS);
        for (int i = 0; i < 8; i++) ThiemBoService.tick(p);
        if (!ThiemBoService.sprinting(p)) h.fail("khong sprint lai");
        SkillService.toggleStance(p);
        if (ThiemBoService.sprinting(p)) h.fail("tat the ma van chay");
        h.succeed();
    }
}
