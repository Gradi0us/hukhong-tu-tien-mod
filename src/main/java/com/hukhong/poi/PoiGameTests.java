package com.hukhong.poi;

import com.hukhong.HuKhong;
import com.hukhong.network.MapMarkersPayload;
import com.hukhong.travel.TravelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Dia diem: luat dat, danh dau ban do khi kham pha, chi dan tin don, giai do Dong Phu. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class PoiGameTests {
    private PoiGameTests() {}

    private static ServerPlayer player(GameTestHelper h, String name) {
        return FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
    }

    private static ServerLevel ow(ServerPlayer p) { return p.getServer().overworld(); }

    @GameTest(template = "empty")
    public static void poiPlacementRules(GameTestHelper h) {
        ServerLevel ow = h.getLevel().getServer().overworld();
        PoiSavedData data = PoiSavedData.get(ow);
        PoiDef def = PoiDefs.get("bia_da");
        if (def == null) { h.fail("bia_da definition must be loaded from the datapack"); return; }
        BlockPos sp = ow.getSharedSpawnPos();
        if (!"spawn".equals(PoiPlacer.evaluate(ow, def, sp.getX() + 20, sp.getZ() + 20, data))) { h.fail("next to world spawn must be rejected as 'spawn'"); return; }
        // bioma khong ton tai -> 'biome' (xa spawn, khong thanh, khong vung)
        PoiDef odd = PoiDef.parse("odd", jsonWithBiome("minecraft:no_such_biome"));
        String why = PoiPlacer.evaluate(ow, odd, sp.getX() + 30000, sp.getZ() + 30000, data);
        if (!"biome".equals(why) && !"zone".equals(why) && !"city".equals(why)) { h.fail("impossible biome must be rejected, got " + why); return; }
        // khoang cach toi thieu cung loai + qua gan bat ky dia diem nao
        int bx = sp.getX() + 41000, bz = sp.getZ() + 41000;
        PoiInstance a = new PoiInstance("bia_da", bx, 70, bz, 5);
        data.add(a);
        if (!data.anyWithin("bia_da", bx + def.minSpacing() - 10, bz, def.minSpacing())) { h.fail("same-def location within min_spacing must be detected"); return; }
        if (data.anyWithin("bia_da", bx + def.minSpacing() + 10, bz, def.minSpacing())) { h.fail("same-def location beyond min_spacing must be free"); return; }
        if (!data.anyWithin(null, bx + 30, bz, PoiPlacer.CROWD) || data.anyWithin(null, bx + PoiPlacer.CROWD + 5, bz, PoiPlacer.CROWD)) { h.fail("crowding radius wrong"); return; }
        if (!data.known(a.key) || data.known(PoiInstance.keyOf("bia_da", bx + 1, bz))) { h.fail("known() must cover built instances only"); return; }
        data.markSkipped("bia_da@1,1");
        if (!data.known("bia_da@1,1")) { h.fail("skipped candidates must be remembered"); return; }
        // ung vien tat dinh: cung seed -> cung vi tri
        int[] c1 = PoiMath.candidate(ow.getSeed(), "bia_da", 1.0, 3, 4), c2 = PoiMath.candidate(ow.getSeed(), "bia_da", 1.0, 3, 4);
        if (c1[0] != c2[0] || c1[1] != c2[1]) { h.fail("candidate not deterministic"); return; }
        h.succeed();
    }

    private static com.google.gson.JsonObject jsonWithBiome(String biome) {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("builder", "stele");
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        arr.add(biome);
        o.add("biomes", arr);
        return o;
    }

    @GameTest(template = "empty")
    public static void poiDiscoveryAddsMapMarkerAndTravelPoint(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-poi1");
        ServerLevel ow = ow(p);
        PoiSavedData data = PoiSavedData.get(ow);
        PoiInstance spring = new PoiInstance("linh_tuyen", 52000, 70, 52000, 3), inn = new PoiInstance("khach_dien", 52100, 70, 52000, 4);
        data.add(spring); data.add(inn);
        p.setPos(52000, 70, 52000);
        if (hasMarker(p, spring.key)) { h.fail("undiscovered location must not be on the map"); return; }
        if (!PoiService.discover(p, spring)) { h.fail("first discovery must return true"); return; }
        if (PoiService.discover(p, spring)) { h.fail("second discovery must be a no-op"); return; }
        if (!hasMarker(p, spring.key)) { h.fail("discovered location must produce a POI map marker"); return; }
        if (!PoiService.takeMarkerDirty(p.getUUID())) { h.fail("discovery must flag the player's markers for resend"); return; }
        MapMarkersPayload.Marker m = PoiService.markersFor(p).get(0);
        if (!"hukhong.poi.n.linh_tuyen".equals(m.nameKey()) || m.x() != 52000 || m.kind() != MapMarkersPayload.POI) { h.fail("marker fields wrong: " + m); return; }
        if (p.getData(TravelRegistry.TRAVEL.get()).has("p:" + spring.key)) { h.fail("a spring is not a travel point"); return; }
        PoiService.discover(p, inn);
        if (!p.getData(TravelRegistry.TRAVEL.get()).has("p:" + inn.key)) { h.fail("inn must become a travel point"); return; }
        if (!"hukhong.poi.n.khach_dien".equals(com.hukhong.poi.PoiService.markersFor(p).stream().filter(x -> x.id().equals(inn.key)).findFirst().orElseThrow().nameKey())) { h.fail("inn marker name"); return; }
        ServerPlayer other = player(h, "hk-poi1b");
        if (hasMarker(other, spring.key)) { h.fail("discovery is per player"); return; }
        h.succeed();
    }

    private static boolean hasMarker(ServerPlayer p, String key) {
        for (MapMarkersPayload.Marker m : PoiService.markersFor(p)) if (m.id().equals(key)) return true;
        return false;
    }

    @GameTest(template = "empty")
    public static void poiInnkeeperRumorPointsToNearestUndiscoveredPoi(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-poi2");
        ServerLevel ow = ow(p);
        PoiSavedData data = PoiSavedData.get(ow);
        int ox = 61000, oz = 61000;
        PoiInstance inn = new PoiInstance("khach_dien", ox, 70, oz, 8), far = new PoiInstance("bia_da", ox + 900, 70, oz, 1), near = new PoiInstance("tho_dia_mieu", ox - 120, 70, oz + 30, 2);
        data.add(inn); data.add(far); data.add(near);
        p.setPos(ox, 70, oz);
        PoiRumor.Rumor r1 = PoiRumor.pick(p, data, inn); // luot chan -> dia diem gan nhat chua kham pha
        if (r1.target() != near) { h.fail("first rumor must point to the nearest undiscovered location, got " + r1.target()); return; }
        PoiRumor.Rumor r2 = PoiRumor.pick(p, data, inn);
        if (r2.target() != null) { h.fail("odd rumor must be the story line"); return; }
        PoiRumor.Rumor r3 = PoiRumor.pick(p, data, inn); // chan lai: near van chua kham pha -> van tro near
        if (r3.target() != near) { h.fail("rumor must keep pointing at the undiscovered one, got " + r3.target()); return; }
        PoiService.discover(p, near);
        PoiActions.talk(p, ow, data, inn);              // luot le
        PoiRumor.Rumor r4 = PoiRumor.pick(p, data, inn);
        if (r4.target() != far) { h.fail("after discovering the near one the rumor must move on to the next, got " + r4.target()); return; }
        if (!data.hasFound(p.getUUID(), inn.key)) { h.fail("talking to the innkeeper must discover the inn"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void poiCavePuzzleOpensVaultOnlyInOrder(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-poi3");
        ServerLevel ow = h.getLevel();
        PoiSavedData data = PoiSavedData.get(ow);
        BlockPos o = h.absolutePos(new BlockPos(1, 1, 1));
        PoiInstance cave = new PoiInstance("dong_phu", o.getX(), o.getY(), o.getZ(), 11);
        for (int s = 0; s < 4; s++) {
            BlockPos lp = h.absolutePos(new BlockPos(s, 2, 0));
            ow.setBlock(lp, Blocks.LEVER.defaultBlockState(), 3);
            cave.acts.add(new PoiInstance.Act("lever:" + s, lp.getX(), lp.getY(), lp.getZ()));
        }
        BlockPos vp = h.absolutePos(new BlockPos(2, 2, 3));
        cave.acts.add(new PoiInstance.Act("vault", vp.getX(), vp.getY(), vp.getZ()));
        BlockPos sealA = new BlockPos(vp.getX(), vp.getY(), vp.getZ() + 2), sealB = sealA.above();
        ow.setBlock(sealA, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        ow.setBlock(sealB, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        data.add(cave);
        int[] order = PoiPuzzle.slotOrder(cave.seed, 4);
        PoiInstance.Act first = cave.firstAct("lever:" + order[0]);
        if (data.actAt(first.pos()) != first || data.ownerOf(first.pos()) != cave) { h.fail("act index must resolve lever blocks to their location"); return; }
        // sai thu tu: bam can sai truoc -> khong tien trien, moi can ve tat
        int wrong = order[2]; // khong phai buoc ke tiep (order[1])
        if (!PoiActions.useAct(p, ow, data, cave, cave.firstAct("lever:" + order[0]))) { h.fail("lever use must be handled"); return; }
        PoiActions.useAct(p, ow, data, cave, cave.firstAct("lever:" + wrong));
        if (cave.progress != 0 || cave.solved) { h.fail("wrong lever must reset progress, progress=" + cave.progress); return; }
        if (ow.getBlockState(first.pos()).getValue(LeverBlock.POWERED)) { h.fail("reset must switch levers off"); return; }
        if (!ow.getBlockState(sealA).is(Blocks.CRYING_OBSIDIAN)) { h.fail("seal must stay closed while unsolved"); return; }
        // dung thu tu
        for (int k = 0; k < 4; k++) {
            if (cave.solved) { h.fail("solved too early at step " + k); return; }
            PoiActions.useAct(p, ow, data, cave, cave.firstAct("lever:" + order[k]));
        }
        if (!cave.solved) { h.fail("correct sequence must solve the puzzle"); return; }
        if (!ow.getBlockState(sealA).isAir() || !ow.getBlockState(sealB).isAir()) { h.fail("seal must open when solved"); return; }
        // vault: da giai nhung chua dung dao -> khong nhan (bao ve), khong nem
        if (!PoiActions.useAct(p, ow, data, cave, cave.firstAct("vault"))) { h.fail("vault click must be handled"); return; }
        if (cave.extra.getBoolean("claimed:" + p.getUUID())) { h.fail("a player without the sealed path must not claim the vault"); return; }
        PoiService.resetPuzzle(ow, data, cave);
        if (cave.solved || cave.progress != 0 || !ow.getBlockState(sealA).is(Blocks.CRYING_OBSIDIAN)) { h.fail("puzzle reset must re-close the seal"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void poiSteleLeadsToUndefeatedChgOrDocSource(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-poi4");
        ServerLevel ow = ow(p);
        PoiSavedData data = PoiSavedData.get(ow);
        PoiInstance stele = new PoiInstance("bia_da", 70000, 70, 70000, 4), dong = new PoiInstance("dong_phu", 70200, 70, 70000, 1);
        data.add(stele); data.add(dong);
        p.setPos(70000, 70, 70000);
        PoiRumor.Lead l = PoiRumor.lead(p, ow, data, stele); // seed chan -> hang CHG neu con
        if (l.kind() != 1 || l.chgIdx() < 1) { h.fail("even seed must point at an undefeated CHG lair, got " + l); return; }
        PoiInstance stele2 = new PoiInstance("bia_da", 70050, 70, 70000, 5);
        data.add(stele2);
        PoiRumor.Lead l2 = PoiRumor.lead(p, ow, data, stele2); // seed le -> nguon Di thu (dong_phu: c5 hoac bia khac) neu con
        if (l2.kind() != 2 || l2.poi() == null || l2.poi() == stele2) { h.fail("odd seed must point at a Di thu source, got " + l2); return; }
        PoiRumor.showLead(p, ow, data, l2);
        if (!data.hasFound(p.getUUID(), l2.poi().key)) { h.fail("pointing at a source must reveal it on the map"); return; }
        h.succeed();
    }
}
