package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.endgame.DaKhuyet;
import com.hukhong.endgame.EndgameRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Dạ Khuyết kiểu Malenia: chiêu riêng được director giao cho boss tự thi triển, hút máu khi trúng, Huyết Nguyệt Khai Hoa để lại vũng máu. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class DaKhuyetGameTests {
    private DaKhuyetGameTests() {}

    private static final String B = "dakhuyet";

    private static ServerPlayer target(GameTestHelper h, double x, double z) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-dakhuyet"));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    /**
     * Chiêu lướt/lao đưa boss xa tới ~16 block: ngoài khuôn test chunk có thể không tick thực thể (boss "đóng băng" giữa chiêu).
     * Ép nạp các chunk quanh khuôn trong lúc test, bỏ ép khi xong. Hai test dùng forceArea nằm ở batch riêng
     * (test cùng batch chạy song song, khuôn sát nhau => test này bỏ ép sẽ làm chunk của test kia ngừng tick).
     */
    private static void forceArea(GameTestHelper h, boolean on) {
        net.minecraft.core.BlockPos c = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (int dx = -24; dx <= 24; dx += 16)
            for (int dz = -24; dz <= 24; dz += 16)
                h.getLevel().setChunkForced((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4, on);
    }

    /** Phân thân (avatar) để không dựng đấu trường endgame; đánh dấu intro đã xong để không bị cutscene khoá. */
    private static DaKhuyet spawn(GameTestHelper h) {
        DaKhuyet d = EndgameRegistry.DA_KHUYET.get().create(h.getLevel());
        if (d == null) { h.fail("khong tao duoc Da Khuyet"); return null; }
        d.markAvatar();
        d.addTag(BossPhaseDirector.INTRO_TAG);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        d.moveTo(v.x, v.y, v.z, 0, 0);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest(template = "empty", timeoutTicks = 1000, batch = B + "_moves")
    public static void everyPhaseMoveIsOwnAndFinishes(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 2, 6);
        DaKhuyet d = spawn(h);
        d.setCurrentPhase(3);
        List<Move> moves = d.movesForPhase(3);
        if (moves.size() != 8) h.fail("Da Khuyet phai co 8 chieu phase, thuc te " + moves.size());
        AtomicInteger idx = new AtomicInteger(-1), wait = new AtomicInteger();
        h.onEachTick(() -> {
            if (!d.isAlive()) h.fail("Da Khuyet bien mat giua chieu " + d.currentMove() + " (removed=" + d.getRemovalReason() + ", hp=" + d.getHealth() + ", pos=" + d.position() + ")");
            d.setHealth(d.getMaxHealth());
            p.setHealth(p.getMaxHealth());
            if (d.currentMove() != null) {
                if (wait.incrementAndGet() > 200) h.fail("chieu " + d.currentMove() + " khong ket thuc (t=" + d.moveTick() + ", tickCount=" + d.tickCount + ", gt=" + h.getTick() + ", ticking=" + h.getLevel().isPositionEntityTicking(d.blockPosition()) + ", noAi=" + d.isNoAi() + ", pos=" + d.position() + ", removed=" + d.getRemovalReason() + ")");
                return;
            }
            int i = idx.incrementAndGet();
            if (i >= moves.size()) {
                boolean grav = d.isNoGravity();
                d.discard();
                forceArea(h, false);
                if (grav) h.fail("ket thuc chieu phai tra lai trong luc");
                h.succeed();
                return;
            }
            Move m = moves.get(i);
            if (!m.id().startsWith("dk_")) h.fail("chieu chung lot vao bo Da Khuyet: " + m.id());
            wait.set(0);
            d.director().start(h.getLevel(), m, p);
            if (d.director().currentRun() != null) h.fail(m.id() + ": director khong duoc chay PhaseMoves chung");
            if (d.currentMove() == null || d.idleForMove()) h.fail(m.id() + ": Da Khuyet khong nhan chieu rieng");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void hitsHealDaKhuyet(GameTestHelper h) {
        // FakePlayer bat tu voi moi sat thuong => dung mot sinh vat thuong lam nan nhan
        net.minecraft.world.entity.animal.Pig p = h.spawn(net.minecraft.world.entity.EntityType.PIG, new net.minecraft.core.BlockPos(2, 1, 4));
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        p.setHealth(1000);
        DaKhuyet d = spawn(h);
        h.runAfterDelay(2, () -> {
            d.setHealth(1000);
            boolean ok = d.strike(p, 40f);
            float hp = d.getHealth();
            d.discard();
            p.discard();
            if (!ok) h.fail("don khong trung");
            // luong hoi thuc te con qua cac bo dieu chinh hoi mau chung cua mod (LivingHealEvent) => chi kiem tra co hoi
            if (hp <= 1000) h.fail("trung don phai hoi mau (hp=" + hp + ")");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = B + "_bloom")
    public static void bloomLeavesDamagingPool(GameTestHelper h) {
        forceArea(h, true);
        ServerPlayer p = target(h, 4, 4);
        DaKhuyet d = spawn(h);
        d.setCurrentPhase(3);
        h.runAfterDelay(2, () -> {
            if (!d.startMove(h.getLevel(), "dk_huyet_hoa", p)) h.fail("khong thi trien duoc Huyet Nguyet Khai Hoa");
        });
        h.runAfterDelay(110, () -> {
            int pools = d.poolCount();
            double off = d.position().multiply(1, 0, 1).distanceTo(p.position().multiply(1, 0, 1));
            boolean grav = d.isNoGravity();
            d.discard();
            forceArea(h, false);
            if (pools == 0) h.fail("hoa no phai de lai vung mau");
            if (off > 1.5) h.fail("phai lao xuong dung diem danh dau (lech " + off + ")");
            if (grav) h.fail("xong chieu phai tra lai trong luc");
            h.succeed();
        });
    }
}
