package com.hukhong.liberation;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.ModEntities;
import com.hukhong.events.CorruptBuff;
import com.hukhong.events.RaidLevel;
import com.hukhong.events.VillagerGuard;
import com.hukhong.events.VoidRaid;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class LiberationGameTests {
    @GameTest(template = "empty")
    public static void tapKichXacSuatVaCap(GameTestHelper h) {
        if (VoidRaid.chance(false, false) != 0.45 || VoidRaid.chance(false, true) != 0.20 || VoidRaid.chance(true, false) != 0.70) { h.fail("xac suat tap kich"); return; }
        if (RaidLevel.fromAverage(40.4, false) != 40) { h.fail("cap tb"); return; }
        if (RaidLevel.fromAverage(40.0, true) != CorruptBuff.level(true, 40) || CorruptBuff.level(true, 40) != 52) { h.fail("vung nhiem +30%"); return; }
        if (RaidLevel.fromAverage(0, false) != 1) { h.fail("cap toi thieu"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void quaiTapKichDoiCapTheoTrungBinh(GameTestHelper h) {
        var l = h.getLevel();
        var m = ModEntities.HU_THI.get().create(l);
        BlockPos p = h.absolutePos(new BlockPos(2, 2, 2));
        m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        m.finalizeSpawn(l, l.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
        double hp0 = m.getMaxHealth();
        int lv0 = ((HuMob) m).huLevel();
        int target = lv0 > 50 ? 10 : lv0 + 25;
        RaidLevel.apply(m, target, false);
        if (((HuMob) m).huLevel() != target) { h.fail("cap khong doi: " + ((HuMob) m).huLevel()); return; }
        if ((target > lv0) != (m.getMaxHealth() > hp0) || m.getMaxHealth() == hp0) { h.fail("HP khong tang theo cap: " + hp0 + " -> " + m.getMaxHealth()); return; }
        m.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void danLangKhongThanhZombieVillager(GameTestHelper h) {
        Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        if (VillagerGuard.blocksConversion(v, EntityType.ZOMBIE_VILLAGER)) { h.fail("khong co quai ma van chan"); return; }
        Zombie vanilla = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        v.setLastHurtByMob(vanilla);
        if (VillagerGuard.blocksConversion(v, EntityType.ZOMBIE_VILLAGER)) { h.fail("zombie vanilla bi chan nham"); return; }
        var hu = ModEntities.HU_THI.get().create(h.getLevel());
        BlockPos p = h.absolutePos(new BlockPos(3, 2, 3));
        hu.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        h.getLevel().addFreshEntity(hu);
        v.setLastHurtByMob(hu);
        if (!VillagerGuard.blocksConversion(v, EntityType.ZOMBIE_VILLAGER)) { h.fail("Hu Thi giet dan lang van chuyen doi"); return; }
        if (VillagerGuard.blocksConversion(v, EntityType.PIG)) { h.fail("chan nham ket qua khac"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void damTamHuHoaTatDinhVaCapBoss(GameTestHelper h) {
        BlockPos a = new BlockPos(1234, 0, -567);
        var c1 = HeartArena.cracks(a);
        var c2 = HeartArena.cracks(a);
        if (c1.isEmpty() || c1.size() != c2.size()) { h.fail("khe nut khong tat dinh"); return; }
        for (int i = 0; i < c1.size(); i++) if (c1.get(i)[0] != c2.get(i)[0] || c1.get(i)[1] != c2.get(i)[1]) { h.fail("khe nut lech"); return; }
        for (int[] c : c1) if (Math.hypot(c[0], c[1]) > HeartArena.RADIUS) { h.fail("khe nut ngoai dau truong"); return; }
        if (HeartArena.pillars().size() != HeartArena.SPOKES) { h.fail("so tru"); return; }
        if (LiberationManager.bossMul(30) != 1.0 || LiberationManager.bossMul(100) != 100 / 30.0 || LiberationManager.bossMul(300) != 3.5 || LiberationManager.bossMul(1) != 0.6) { h.fail("he so boss"); return; }
        if (!LiberationManager.inRegion(a, 1234 + 100, -567) || LiberationManager.inRegion(a, 1234 + 200, -567)) { h.fail("pham vi vung"); return; }
        h.succeed();
    }
}
