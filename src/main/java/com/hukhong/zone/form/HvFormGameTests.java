package com.hukhong.zone.form;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhases;
import com.hukhong.entity.ModEntities;
import com.hukhong.honcot.HonCotItem;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.zone.ZoneBossFactory;
import com.hukhong.zone.ZoneData;
import com.hukhong.zone.ZoneMath;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 8 dang Hu Vuong (PLAN-v2 12.6): moi dang chay du chieu rieng, qua du 3 pha (scale 1 -> 1.25 -> 1.5), roi dung 1 Hon Cot / nguoi gay >= 10%
 * (dung khe + pham); ten rieng tat dinh; nha may vung sinh dung dang o dung cap.
 */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class HvFormGameTests {
    private HvFormGameTests() {}

    private static final String B = "hvform";

    private static ServerPlayer player(GameTestHelper h, String name, Vec3 at) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        Vec3 v = h.absoluteVec(at);
        p.setPos(v.x, v.y, v.z);
        return p;
    }

    private static HuVuongForm spawn(GameTestHelper h, HvForm f, int level, int tier) {
        HuVuongForm b = ModEntities.HU_VUONG_FORM.get().create(h.getLevel());
        if (b == null) { h.fail("khong tao duoc HuVuongForm"); throw new IllegalStateException(); }
        b.setForm(f);
        b.setZoneInfo(901, level, tier);
        Vec3 v = h.absoluteVec(new Vec3(2, 1, 2));
        b.moveTo(v.x, v.y, v.z, 0f, 0f);
        b.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(b.blockPosition()), MobSpawnType.COMMAND, null);
        b.setPersistenceRequired();
        h.getLevel().addFreshEntity(b);
        return b;
    }

    // ------------------------------------------------------------------ chay du chieu
    private static void runsAllSkills(GameTestHelper h, HvForm f) {
        ServerPlayer p = player(h, "hv-skill", new Vec3(2, 1, 7));
        HuVuongForm b = spawn(h, f, 60, 1);
        b.setHealth(b.getMaxHealth() * 0.9f);
        List<HvSkill> all = f.allSkills();
        h.onEachTick(() -> { if (b.isAlive()) { b.setTarget(p); if (!b.casting()) b.holdSkills(60); } });
        for (int i = 0; i < all.size(); i++) {
            HvSkill sk = all.get(i);
            h.runAfterDelay(20 + i * 70L, () -> { if (b.isAlive()) b.startSkill(h.getLevel(), sk, p); });
        }
        h.runAfterDelay(20 + all.size() * 70L + 20, () -> {
            boolean alive = b.isAlive();
            int fired = b.skillsFired(), burrow = b.burrowing() ? 1 : 0;
            b.discard();
            if (!alive) h.fail(f + " chet bat thuong khi chay chieu");
            if (fired != all.size()) h.fail(f + " chi no " + fired + "/" + all.size() + " chieu");
            if (burrow != 0) h.fail(f + " con ket trang thai chui dat");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ qua du pha
    private static void passesPhases(GameTestHelper h, HvForm f) {
        ServerPlayer p = player(h, "hv-phase", new Vec3(2, 1, 7));
        HuVuongForm b = spawn(h, f, 60, 1);
        h.onEachTick(() -> { if (b.isAlive()) b.setTarget(p); });
        h.runAfterDelay(5, () -> b.setHealth(b.getMaxHealth() * 0.6f));
        h.runAfterDelay(150, () -> {
            if (b.phase() != 2 || b.director().transitioning()) h.fail(f + ": phai o pha 2 (pha=" + b.phase() + ")");
            if (Math.abs(b.director().currentScale() - 1.25f) > 0.02f) h.fail(f + ": scale pha 2 = " + b.director().currentScale());
            if (b.movesForPhase(2).size() != 2) h.fail(f + ": pha 2 phai co 2 chieu phase");
            if (b.form().skills(2).isEmpty()) h.fail(f + ": khong co chieu rieng pha 2");
            b.setHealth(b.getMaxHealth() * 0.25f);
        });
        h.runAfterDelay(320, () -> {
            boolean ok = b.phase() == 3 && !b.director().transitioning() && Math.abs(b.director().currentScale() - 1.5f) < 0.02f;
            int ph = b.phase();
            boolean ult = b.movesForPhase(3).size() == 3 && b.movesForPhase(3).get(2).ultimate();
            b.discard();
            if (!ok) h.fail(f + ": phai o pha 3, scale 1.5 (pha=" + ph + ")");
            if (!ult) h.fail(f + ": pha 3 phai co chieu toi thuong");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ roi Hon Cot
    private static void dropsBones(GameTestHelper h, HvForm f, int zoneTier, int level) {
        ServerPlayer a = player(h, "hv-a", new Vec3(2, 1, 6)), c = player(h, "hv-b", new Vec3(3, 1, 6)), d = player(h, "hv-c", new Vec3(4, 1, 6));
        HuVuongForm b = spawn(h, f, level, zoneTier);
        b.recordDamage(a, 70);
        b.recordDamage(c, 25);
        b.recordDamage(d, 5);  // < 10% -> khong du dieu kien
        b.kill();
        h.runAfterDelay(5, () -> {
            List<ItemStack> ia = bones(a), ib = bones(c), ic = bones(d);
            b.discard();
            if (b.isAlive()) h.fail(f + " chua chet");
            int expectTier = HvDrops.tier(f, zoneTier);
            if (ia.isEmpty() || ia.size() > (f.multiFace() ? 2 : 1)) h.fail(f + ": nguoi 70% phai nhan " + (f.multiFace() ? "1-2" : "dung 1") + " Hon Cot, co " + ia.size());
            if (ib.isEmpty() || ib.size() > (f.multiFace() ? 2 : 1)) h.fail(f + ": nguoi 25% phai nhan " + (f.multiFace() ? "1-2" : "dung 1") + " Hon Cot, co " + ib.size());
            if (!ic.isEmpty()) h.fail(f + ": nguoi 5% (<10%) khong duoc nhan Hon Cot, co " + ic.size());
            for (ItemStack st : List.of(ia.get(0), ib.get(0))) {
                HonCotItem it = (HonCotItem) st.getItem();
                if (it.bone != f.bone) h.fail(f + ": Hon Cot dau phai la " + f.bone + ", thuc te " + it.bone);
                if (it.bone.slot != f.bone.slot) h.fail(f + ": sai khe");
                if (HonCotItem.tierOf(st) != expectTier) h.fail(f + ": pham " + HonCotItem.tierOf(st) + " != " + expectTier);
                if (HonCotItem.rollOf(st) != HvDrops.roll(level)) h.fail(f + ": roll " + HonCotItem.rollOf(st));
            }
            if (f.multiFace() && HonCotItem.tierOf(ia.get(0)) != HonCotMath.TIER_HONG) h.fail("Thien Dien luon pham cao nhat");
            h.succeed();
        });
    }

    private static List<ItemStack> bones(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof HonCotItem) out.add(s);
        return out;
    }

    // ------------------------------------------------------------------ 8 dang x 3 kiem tra
    @GameTest(template = "empty", timeoutTicks = 900, batch = B) public static void huyetMaRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.HUYET_MA); }
    @GameTest(template = "empty", timeoutTicks = 900, batch = B) public static void cotLongRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.COT_LONG); }
    @GameTest(template = "empty", timeoutTicks = 900, batch = B) public static void mocYeuRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.MOC_YEU); }
    @GameTest(template = "empty", timeoutTicks = 900, batch = B) public static void bangPhachRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.BANG_PHACH); }
    @GameTest(template = "empty", timeoutTicks = 900, batch = B) public static void saTranRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.SA_TRAN); }
    @GameTest(template = "empty", timeoutTicks = 900, batch = B) public static void docChuongRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.DOC_CHUONG); }
    @GameTest(template = "empty", timeoutTicks = 900, batch = B) public static void haiUyenRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.HAI_UYEN); }
    @GameTest(template = "empty", timeoutTicks = 1200, batch = B) public static void thienDienRunsAllSkills(GameTestHelper h) { runsAllSkills(h, HvForm.THIEN_DIEN); }

    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void huyetMaPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.HUYET_MA); }
    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void cotLongPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.COT_LONG); }
    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void mocYeuPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.MOC_YEU); }
    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void bangPhachPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.BANG_PHACH); }
    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void saTranPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.SA_TRAN); }
    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void docChuongPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.DOC_CHUONG); }
    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void haiUyenPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.HAI_UYEN); }
    @GameTest(template = "empty", timeoutTicks = 500, batch = B) public static void thienDienPassesPhases(GameTestHelper h) { passesPhases(h, HvForm.THIEN_DIEN); }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void huyetMaDropsBones(GameTestHelper h) { dropsBones(h, HvForm.HUYET_MA, 0, 22); }
    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void cotLongDropsBones(GameTestHelper h) { dropsBones(h, HvForm.COT_LONG, 1, 55); }
    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void mocYeuDropsBones(GameTestHelper h) { dropsBones(h, HvForm.MOC_YEU, 2, 80); }
    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void bangPhachDropsBones(GameTestHelper h) { dropsBones(h, HvForm.BANG_PHACH, 1, 45); }
    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void saTranDropsBones(GameTestHelper h) { dropsBones(h, HvForm.SA_TRAN, 0, 30); }
    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void docChuongDropsBones(GameTestHelper h) { dropsBones(h, HvForm.DOC_CHUONG, 1, 50); }
    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void haiUyenDropsBones(GameTestHelper h) { dropsBones(h, HvForm.HAI_UYEN, 2, 70); }
    @GameTest(template = "empty", timeoutTicks = 100, batch = B) public static void thienDienDropsBones(GameTestHelper h) { dropsBones(h, HvForm.THIEN_DIEN, 0, 85); }

    // ------------------------------------------------------------------ thuan + nha may
    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void configAndRulesPure(GameTestHelper h) {
        Set<String> names = new HashSet<>();
        for (HvForm f : HvForm.values()) {
            if (!BossPhases.has(f.phaseId())) h.fail("thieu cau hinh pha " + f.phaseId());
            var cfg = BossPhases.get(f.phaseId());
            if (cfg.movesUpTo(2).size() != 2) h.fail(f + ": pha 2 them 2 chieu");
            List<com.hukhong.boss.phase.BossPhaseConfig.Move> m3 = cfg.movesUpTo(3);
            if (m3.size() != 3 || !m3.get(2).ultimate()) h.fail(f + ": pha 3 them 1 chieu toi thuong");
            if (f.allSkills().size() < 3 || f.skills(1).size() < 2) h.fail(f + ": 2-3 chieu rieng");
            if (f.multiFace() && f.allSkills().size() != 9) h.fail("Thien Dien: 3 mat x 3 chieu");
            if (!names.add(f.vi)) h.fail("trung ten dang " + f.vi);
        }
        // khung xuong: 8 dang tren 4 khung
        Set<HvForm.Skel> sk = new HashSet<>();
        for (HvForm f : HvForm.values()) sk.add(f.skel);
        if (sk.size() != 4) h.fail("phai du 4 khung xuong, co " + sk.size());
        // ten: 40 ten, tat dinh, doi theo vung
        if (HvNames.PERSONAL.length != 40) h.fail("40 ten rieng, co " + HvNames.PERSONAL.length);
        if (!HvNames.personal(3, 1000, -2000).equals(HvNames.personal(3, 1000, -2000))) h.fail("ten phai tat dinh");
        Set<String> seen = new HashSet<>();
        Random r = new Random(5);
        for (int i = 0; i < 200; i++) seen.add(HvNames.personal(i, r.nextInt(30000) - 15000, r.nextInt(30000) - 15000));
        if (seen.size() < 25) h.fail("ten rieng it da dang: " + seen.size());
        if (!HvNames.full(HvForm.COT_LONG, 2, "Mạc Trầm").equals("Cổ Cốt Long Hư Vương — Mạc Trầm")) h.fail("dinh dang ten: " + HvNames.full(HvForm.COT_LONG, 2, "Mạc Trầm"));
        if (!HvNames.full(HvForm.HUYET_MA, 0, "Tô Hàn").equals("Huyết Ma Hư Vương — Tô Hàn")) h.fail("ten khong danh hieu");
        if (!HvNames.full(HvForm.BANG_PHACH, 1, "X").startsWith("Tinh Anh Băng Phách")) h.fail("Tinh Anh");
        ZoneData.Zone z = new ZoneData.Zone(7, 40000, -12000, 500, 70, 1, "Hắc Phong Lĩnh");
        if (!z.bossTitle().startsWith("Cổ Cốt Long Hư Vương — ") || !z.bossTitle().endsWith(z.bossName())) h.fail("Zone.bossTitle: " + z.bossTitle());
        // Hon Cot: khe + pham + roll
        if (HvDrops.tier(HvForm.COT_LONG, 0) != HonCotMath.TIER_TU || HvDrops.tier(HvForm.COT_LONG, 1) != HonCotMath.TIER_HAC
                || HvDrops.tier(HvForm.COT_LONG, 2) != HonCotMath.TIER_HONG || HvDrops.tier(HvForm.THIEN_DIEN, 0) != HonCotMath.TIER_HONG) h.fail("pham theo hang vung");
        if (HvDrops.roll(20) != 0 || HvDrops.roll(85) != 100 || HvDrops.roll(60) <= HvDrops.roll(40)) h.fail("roll theo cap vung");
        if (!HvDrops.qualifies(10, 100) || HvDrops.qualifies(9.9, 100) || HvDrops.qualifies(0, 0)) h.fail("nguong 10%");
        int second = 0;
        Random q = new Random(11);
        for (int i = 0; i < 4000; i++) if (HvDrops.second(HvForm.THIEN_DIEN, q) != null) second++;
        if (second < 1000 || second > 1400) h.fail("Thien Dien 30% hon cot thu hai, thuc te " + second + "/4000");
        if (HvDrops.second(HvForm.HUYET_MA, new Random(1)) != null) h.fail("dang thuong khong co hon cot thu hai");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = B)
    public static void zoneFactorySpawnsMatchingForm(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        List<Mob> spawned = new ArrayList<>();
        for (int form = 0; form < HvForm.COUNT; form++) {
            ZoneData.Zone z = new ZoneData.Zone(910 + form, at.getX(), at.getZ(), form == 7 ? 650 : 300, ZoneMath.levelForRadius(form == 7 ? 650 : 300), form, "Test");
            Mob m = ZoneBossFactory.Holder.current.spawn(h.getLevel(), z, at);
            if (!(m instanceof HuVuongForm hv)) { h.fail("nha may phai sinh HuVuongForm cho dang " + form); return; }
            spawned.add(m);
            if (hv.form().ordinal() != form) h.fail("sai dang: " + hv.form() + " != " + form);
            if (hv.huLevel() != z.level) h.fail("sai cap: " + hv.huLevel());
            if (hv.getCustomName() == null || !hv.getCustomName().getString().equals(z.bossTitle())) h.fail("sai ten: " + hv.getCustomName());
            if (hv.getMaxHealth() < 1000) h.fail("khong co mau");
        }
        for (Mob m : spawned) m.discard();
        h.succeed();
    }
}
