package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.charcreate.CharCreateService;
import com.hukhong.charcreate.TienThien;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.flight.FlightMode;
import com.hukhong.flight.FlightService;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.story.flow.FlowSteps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Bay theo cấp/nguyên tố (Pháp Quang/Lôi cấp 1, Ma 20, Thể/Đạo 40) và Tiên Thiên đa đạo. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class FlightTienThienGameTests {
    private FlightTienThienGameTests() {}

    private static ServerPlayer player(GameTestHelper h, Path path, int level, PhapElement el) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "fl-" + path + level));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 2000, 2000, Set.of(path), Map.of(path, Aptitude.B), false)
                .withMilestoneUnlocked(true));
        PhapTu.setElement(p, el);
        var v = h.absoluteVec(new net.minecraft.world.phys.Vec3(2, 1, 2)); // gần test, xa boss thế giới của batch khác
        p.setPos(v.x, v.y, v.z);
        p.setHealth(p.getMaxHealth());
        return p;
    }

    private static boolean tryFly(ServerPlayer p) {
        FlightService.toggle(p);
        boolean on = FlightService.isActive(p);
        if (on) FlightService.stop(p, null, false);
        return on;
    }

    @GameTest(template = "empty")
    public static void phapQuangLoiBayCap1(GameTestHelper h) {
        if (!tryFly(player(h, Path.PHAP, 1, PhapElement.QUANG))) h.fail("Phap Quang lv1 khong bay duoc");
        if (!tryFly(player(h, Path.PHAP, 1, PhapElement.LOI))) h.fail("Phap Loi lv1 khong bay duoc");
        if (tryFly(player(h, Path.PHAP, 1, PhapElement.HOA))) h.fail("Phap Hoa lv1 bay duoc");
        if (tryFly(player(h, Path.PHAP, 59, PhapElement.THUY))) h.fail("Phap Thuy lv59 bay duoc");
        if (!tryFly(player(h, Path.PHAP, 60, PhapElement.HOA))) h.fail("Phap Hoa lv60 khong bay duoc");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tranDanBayCap40(GameTestHelper h) {
        if (FlightMode.TRAN.minLevelFor(null) != 40 || FlightMode.DAN.minLevelFor(null) != 40) h.fail("Tran/Dan minLevel != 40");
        if (tryFly(player(h, Path.TRAN, 39, null))) h.fail("Tran lv39 bay duoc");
        if (!tryFly(player(h, Path.TRAN, 40, null))) h.fail("Tran lv40 khong bay duoc");
        if (tryFly(player(h, Path.DAN, 39, null))) h.fail("Dan lv39 bay duoc");
        if (!tryFly(player(h, Path.DAN, 40, null))) h.fail("Dan lv40 khong bay duoc");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void maBayCap20(GameTestHelper h) {
        if (tryFly(player(h, Path.MA, 19, null))) h.fail("Ma lv19 bay duoc");
        ServerPlayer p = player(h, Path.MA, 20, null);
        FlightService.toggle(p);
        if (!FlightService.isActive(p) || FlightService.modeFor(p) != FlightMode.MA) h.fail("Ma lv20 khong bay");
        FlightService.stop(p, null, false);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theDaoBayCap40(GameTestHelper h) {
        for (Path path : new Path[]{Path.THE, Path.DAO}) {
            if (tryFly(player(h, path, 39, null))) h.fail(path + " lv39 bay duoc");
            if (!tryFly(player(h, path, 40, null))) h.fail(path + " lv40 khong bay duoc");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tienThienCongThuc(GameTestHelper h) {
        if (TienThien.multiPathLevel(List.of(Aptitude.SSS, Aptitude.SSS)) != 20) h.fail("2xSSS != 20");
        if (TienThien.multiPathLevel(List.of(Aptitude.SSS)) != 0) h.fail("1 dao != 0");
        if (TienThien.multiPathLevel(List.of(Aptitude.SS, Aptitude.A)) != 10) h.fail("SS+A != 10");
        if (TienThien.multiPathLevel(List.of(Aptitude.D, Aptitude.D)) != 0) h.fail("D+D != 0");
        if (TienThien.multiPathLevel(List.of(Aptitude.SSS, Aptitude.SSS, Aptitude.SSS)) != 20) h.fail("tam nang khong cap 20");
        if (TienThien.multiPathLevel(List.of(Aptitude.SSS, Aptitude.S, Aptitude.D)) != 15) h.fail("tam nang chi tinh 2 tot nhat");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nhanVatMoiBatDauTienThienKhongKet(GameTestHelper h) {
        Map<Path, Aptitude> apt = Map.of(Path.KIEM, Aptitude.SSS, Path.MA, Aptitude.SSS);
        CultivationData fresh = new CultivationData(1, 0, 10, 10, Set.of(), Map.of(), false)
                .withCharacter(Set.of(Path.KIEM, Path.MA), apt, "", null);
        int lv = TienThien.startLevel(apt.values(), 12345L);
        CultivationData nd = CharCreateService.applyStartLevel(fresh, lv);
        // Tien Thien 20 dung o moc 10 (khoa, chon loi xong moi len tiep)
        if (nd.level() != 10 || !nd.milestoneLocked()) h.fail("level=" + nd.level() + " locked=" + nd.milestoneLocked());
        if (TienThien.firstStop(10, 20) != 20 || TienThien.firstStop(10, 15) != 15 || TienThien.firstStop(1, 7) != 7) h.fail("firstStop sai");
        // don dao khong thuong da dao: khoi dau theo Tien Thien cu (1..10)
        int single = TienThien.startLevel(List.of(Aptitude.SSS), 1L);
        if (single != 10) h.fail("don dao SSS=" + single);
        // cap 15: dung o 10 cho chon loi
        CultivationData n15 = CharCreateService.applyStartLevel(fresh, 15);
        if (n15.level() != 10 || !n15.milestoneLocked()) h.fail("cap 15 sai");
        h.succeed();
    }
}
