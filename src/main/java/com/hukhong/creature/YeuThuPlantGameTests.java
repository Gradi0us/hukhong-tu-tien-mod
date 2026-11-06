package com.hukhong.creature;

import com.hukhong.HuKhong;
import com.hukhong.world.yeulam.DaiSamLam;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** 2026-11-06: Yêu Thú thực vật - hồn kỹ trúng mục tiêu, boss Thụ Vương ít nhất 1000 năm, boss không lẫn vào bầy thường. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class YeuThuPlantGameTests {
    private YeuThuPlantGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void plantSkillsHit1(GameTestHelper h) { hit(h, List.of(CreatureKind.THU_YEU, CreatureKind.THUC_NHAN_HOA)); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void plantSkillsHit2(GameTestHelper h) { hit(h, List.of(CreatureKind.DANG_XA, CreatureKind.QUY_CO)); }

    private static void hit(GameTestHelper h, List<CreatureKind> kinds) {
        var sl = h.getLevel();
        StringBuilder bad = new StringBuilder();
        java.util.List<Object[]> checks = new java.util.ArrayList<>();
        int x = 3;
        for (CreatureKind k : kinds) {
            LinhQuai m = DaiSamLam.spawnBeast(sl, k, h.absolutePos(new BlockPos(x, 2, 3)), YeuThuAge.Y1000, MobSpawnType.COMMAND);
            m.setNoAi(true);
            Zombie z = EntityType.ZOMBIE.create(sl);
            z.moveTo(h.absolutePos(new BlockPos(x, 2, 6)).getCenter().subtract(0, 0.5, 0));
            z.setNoAi(true);
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            z.setHealth(1000);
            sl.addFreshEntity(z);
            m.setTarget(z);
            YeuThuSkills.cast(sl, m, z, YeuThuAge.Y1000);
            if (m.age() == null) bad.append(k).append(" khong co nam tuoi; ");
            checks.add(new Object[]{k, z});
            x += 8;
        }
        h.runAfterDelay(20, () -> {
            for (Object[] c : checks) if (((Zombie) c[1]).getHealth() >= 1000f) bad.append(c[0]).append(" chieu khong trung; ");
            if (bad.length() > 0) h.fail(bad.toString()); else h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void treeKingIsAgedBoss(GameTestHelper h) {
        if (!CreatureKind.VAN_NIEN_THU_VUONG.beast() || !CreatureKind.VAN_NIEN_THU_VUONG.mini()) { h.fail("Thu Vuong phai la boss Yeu Thu"); return; }
        if (CreatureKind.COMMON_BEASTS.contains(CreatureKind.VAN_NIEN_THU_VUONG)) { h.fail("boss lan vao bay thuong"); return; }
        var sl = h.getLevel();
        if (!(CreatureRegistry.type(CreatureKind.VAN_NIEN_THU_VUONG).create(sl) instanceof LinhQuai m)) { h.fail("khong tao duoc"); return; }
        m.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
        if (m.age() == null || m.age().ordinal() < YeuThuAge.Y1000.ordinal()) { h.fail("Thu Vuong duoi 1000 nam: " + m.age()); return; }
        if (m.age().boneChance <= 0) { h.fail("Thu Vuong khong roi Hon Cot"); return; }
        h.succeed();
    }
}
