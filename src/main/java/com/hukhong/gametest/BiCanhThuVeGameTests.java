package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.bicanh.BiCanhRegistry;
import com.hukhong.bicanh.BiCanhThuVe;
import com.hukhong.bicanh.BiCanhTier;
import com.hukhong.bicanh.ThuVeMoves;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Bí Cảnh Thủ Vệ: bộ chiêu riêng tv_* đều kết thúc; khiên chặn đòn trực diện;  */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class BiCanhThuVeGameTests {
    private BiCanhThuVeGameTests() {}

    private static void forceArea(ServerLevel l, net.minecraft.core.BlockPos c, boolean on) {
        for (int dx = -32; dx <= 32; dx += 16)
            for (int dz = -32; dz <= 32; dz += 16)
                l.setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    private static ServerPlayer tank(ServerLevel l, String name) {
        ServerPlayer p = FakePlayerFactory.get(l, new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        return p;
    }

    @GameTest(template = "empty", timeoutTicks = 1400, batch = "thuve_moves")
    public static void thuVeEveryPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        forceArea(h.getLevel(), c, true);
        ServerPlayer p = tank(h.getLevel(), "hk-thuve");
        Vec3 pv = h.absoluteVec(new Vec3(2, 1, 6));
        p.setPos(pv.x, pv.y, pv.z);
        BiCanhThuVe d = BiCanhRegistry.THU_VE.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Thu Ve"); return; }
        d.setTier(BiCanhTier.HAC);
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("Thu Ve phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("Thu Ve bien mat giua chieu " + d.currentMove());
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            if (d.currentMove() != null) {
                if (wait.incrementAndGet() > 160) h.fail("chieu " + d.currentMove() + " khong ket thuc (t=" + d.moveTick() + ")");
                return;
            }
            int i = idx.incrementAndGet();
            if (i >= moves.size()) {
                boolean grav = d.isNoGravity();
                d.discard();
                forceArea(h.getLevel(), c, false);
                if (grav) h.fail("ket thuc chieu phai tra lai trong luc");
                h.succeed();
                return;
            }
            Move m = moves.get(i);
            if (!m.id().startsWith("tv_") || !ThuVeMoves.handles(m.id())) h.fail("chieu chung lot vao bo Thu Ve: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": Thu Ve khong nhan chieu rieng");
        });
    }

    /** Khiên: khi đang thủ, đòn trực diện chỉ còn một phần, đòn sau lưng nguyên vẹn. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void thuVeShieldBlocksFrontalOnly(GameTestHelper h) {
        ServerPlayer p = tank(h.getLevel(), "hk-thuve-shield");
        BiCanhThuVe d = BiCanhRegistry.THU_VE.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Thu Ve"); return; }
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(4, 1, 4));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        d.setTarget(p);
        h.runAfterDelay(5, () -> {
            if (!d.guarding()) { h.fail("Thu Ve phai thu khien khi co muc tieu va dang ranh"); return; }
            double yaw = Math.toRadians(d.yBodyRot);
            Vec3 face = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 at = d.position();
            p.setPos(at.x + face.x * 3, at.y, at.z + face.z * 3);
            float front = d.blockFactor(d.damageSources().playerAttack(p));
            p.setPos(at.x - face.x * 3, at.y, at.z - face.z * 3);
            float back = d.blockFactor(d.damageSources().playerAttack(p));
            d.discard();
            if (front >= 1f || back != 1f) h.fail("khien sai: truoc=" + front + " sau=" + back);
            else h.succeed();
        });
    }
}
