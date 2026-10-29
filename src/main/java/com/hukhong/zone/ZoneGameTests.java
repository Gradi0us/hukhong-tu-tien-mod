package com.hukhong.zone;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.story.objective.ObjectiveAttachments;
import com.hukhong.story.world.HuAnh;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** GameTest vung xam thuc dat san: bo tri tranh thanh, cap theo ban kinh, vao vung -> muc tieu, boss chet -> giai phong + Hu Anh. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ZoneGameTests {
    private ZoneGameTests() {}

    @GameTest(template = "empty")
    public static void zonePlacementAvoidsCitiesAndSpawn(GameTestHelper h) {
        ServerLevel ow = h.getLevel().getServer().overworld();
        BlockPos sp = ow.getSharedSpawnPos();
        int[][] cities = new int[CityDef.ALL.size()][];
        for (CityDef c : CityDef.ALL) cities[c.index()] = new int[]{sp.getX() + c.ax(), sp.getZ() + c.az()};
        List<ZoneMath.Spec> zs = ZoneMath.place(ow.getSeed(), sp.getX(), sp.getZ(), cities);
        if (zs.size() < 8 || zs.size() > 14) { h.fail("so vung " + zs.size()); return; }
        for (ZoneMath.Spec z : zs) {
            if (Math.hypot(z.cx() - sp.getX(), z.cz() - sp.getZ()) < z.radius() + ZoneMath.SPAWN_CLEAR) { h.fail("de len spawn: " + z); return; }
            for (int[] c : cities) if (Math.hypot(z.cx() - c[0], z.cz() - c[1]) < z.radius() + ZoneMath.CITY_CLEAR) { h.fail("de len thanh: " + z); return; }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void zoneLevelByRadiusAndMobLevel(GameTestHelper h) {
        if (ZoneMath.levelForRadius(120) != 20 || ZoneMath.levelForRadius(250) != 40 || ZoneMath.levelForRadius(450) != 65
                || ZoneMath.levelForRadius(700) != 85) { h.fail("moc cap"); return; }
        ZoneData.Zone z = new ZoneData.Zone(1, 100000, 100000, 300, ZoneMath.levelForRadius(300), 0, "Test");
        if (!z.covers(100100, 100000) || z.covers(100400, 100000)) { h.fail("pham vi"); return; }
        if (z.tier() != 1) { h.fail("hang"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void enteringZoneSetsLiberationObjective(GameTestHelper h) {
        ServerLevel ow = h.getLevel().getServer().overworld();
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "zone-enter"));
        ZoneData.Zone z = new ZoneData.Zone(901, 100000, 100000, 200, 30, 0, "Hắc Phong Lĩnh");
        ZoneManager.enter(ow, p, z, 10, 0);
        var od = p.getData(ObjectiveAttachments.OBJECTIVE.get());
        if (!ZoneManager.OBJECTIVE_ID.equals(od.id())) { h.fail("khong co nhiem vu: " + od.id()); return; }
        if (od.target().isEmpty() || od.target().get().getX() != 100000 || od.target().get().getZ() != 100000) { h.fail("muc tieu khong tro tam"); return; }
        if (!od.text().getString().contains("Hắc Phong Lĩnh")) { h.fail("thieu ten vung: " + od.text().getString()); return; }
        ZoneManager.leave(p, z);
        if (!p.getData(ObjectiveAttachments.OBJECTIVE.get()).isEmpty()) { h.fail("roi vung phai xoa muc tieu"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bossKillLiberatesZoneAndDropsHuAnh(GameTestHelper h) {
        ServerLevel ow = h.getLevel().getServer().overworld();
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        int city = HuAnh.nearestCity(ow, at);
        HuAnh.add(ow, city, 60);
        int before = HuAnh.get(ow, city);
        ZoneData zd = new ZoneData();
        ZoneData.Zone z = new ZoneData.Zone(902, at.getX(), at.getZ(), 500, 70, 0, "Tử Cốt Trủng");
        zd.add(z);
        if (!ZoneManager.liberate(ow, zd, z, Vec3.atCenterOf(at), null)) { h.fail("liberate false"); return; }
        if (!z.liberated) { h.fail("chua danh dau giai phong"); return; }
        int after = HuAnh.get(ow, city);
        if (after != Math.max(0, before - 25)) { h.fail("Hu Anh " + before + " -> " + after); return; }
        if (ZoneManager.liberate(ow, zd, z, Vec3.atCenterOf(at), null)) { h.fail("giai phong 2 lan"); return; }
        ZoneManager.ensureGuardian(ow, z, 100000L);
        if (!ow.getEntitiesOfClass(Mob.class, new AABB(at).inflate(96), m -> m.getPersistentData().getInt(ZoneManager.TAG_ZONE) == 902).isEmpty()) {
            h.fail("vung da giai phong van sinh boss"); return;
        }
        h.succeed();
    }
}
