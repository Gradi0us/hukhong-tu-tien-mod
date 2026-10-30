package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.chg.ChgKit;
import com.hukhong.endgame.EndgameService;
import com.hukhong.endgame.SieuDo;
import com.hukhong.endgame.SieuDoKit;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.chain.CityArcKit;
import com.hukhong.story.chain.CityArcKit.Ctx;
import com.hukhong.story.chain.CityArcs;
import com.hukhong.story.chain.QuestChains;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.world.HuAnh;
import com.hukhong.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** GameTest Dot 6: 7 cung truyen thanh (buoc theo co, mo phong ngu canh), dieu kien ending_truth, chuoi pha Tam Chien. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class CityArcGameTests {
    private CityArcGameTests() {}

    private static ServerPlayer player(GameTestHelper h, String name, int chapter) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
        p.setData(StoryRegistry.STORY.get(), StoryData.DEFAULT.withMode(StoryData.MODE_STORY));
        DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(chapter));
        return p;
    }

    private static int stage(ServerPlayer p, String chain) { return QuestChains.stageOf(DialogueService.quest(p).flags(), chain); }

    private static boolean has(ServerPlayer p, String f) { return DialogueService.quest(p).has(f); }

    /** Mo phong tung buoc qua Ctx; kiem co buoc, co done, Van Kien thuong, co sau khi xong. */
    @GameTest(template = "empty")
    public static void everyArcProgressesByFlagsAndRewards(GameTestHelper h) {
        Ctx[][] walks = {
                {Ctx.NONE.withNight(true).withNearCity(true), Ctx.NONE.withReached(true), Ctx.NONE.withCleared(true)},
                {Ctx.NONE.withCleared(true), Ctx.NONE.withPearls(5), null},
                {Ctx.NONE.withScene(true), Ctx.NONE.withReached(true), Ctx.NONE.withCleared(true)},
                {Ctx.NONE.withReached(true), Ctx.NONE.withStorm(true), null},
                {Ctx.NONE.withCleared(true), Ctx.NONE.withCleared(true), Ctx.NONE.withRival(true)},
                {Ctx.NONE.withReached(true), Ctx.NONE.withCleared(true), Ctx.NONE.withModao(true)},
                {Ctx.NONE.withNearCity(true), Ctx.NONE.withRetaken(true), null}};
        for (int i = 0; i < CityArcKit.ARCS.size(); i++) {
            CityArcKit.Arc a = CityArcKit.ARCS.get(i);
            ServerPlayer p = player(h, "arc-" + a.id(), a.chMin());
            if (stage(p, a.id()) != 0) h.fail(a.id() + " da co buoc");
            QuestChains.setStage(p, a.id(), 1); // nhan tu NPC (action qc)
            if (stage(p, a.id()) != 1) h.fail(a.id() + " khong vao buoc 1");
            if (CityArcs.simulate(p, a.id(), Ctx.NONE) != 1) h.fail(a.id() + " tu chuyen buoc khi chua du dieu kien");
            for (int s = 0; s < 3; s++) {
                int cur = stage(p, a.id());
                if (walks[i][s] == null) { // buoc 3 chot bang thoai
                    if (cur != 3) h.fail(a.id() + " phai o buoc 3, dang " + cur);
                    QuestChains.setStage(p, a.id(), 9);
                    break;
                }
                int nx = CityArcs.simulate(p, a.id(), walks[i][s]);
                if (nx != (cur == 3 ? 9 : cur + 1)) h.fail(a.id() + " buoc " + cur + " -> " + nx);
            }
            if (!has(p, a.doneFlag()) || stage(p, a.id()) != 0) h.fail(a.id() + " chua done");
            for (String d : a.docs()) if (!p.getData(StoryRegistry.STORY.get()).has(d)) h.fail(a.id() + " thieu Van Kien " + d);
            for (String f : a.setsFlags()) if (!has(p, f)) h.fail(a.id() + " thieu co " + f);
            if (!a.modaoKey().equals(com.hukhong.story.world.StoryHooks.pendingModaoLine(p))) h.fail(a.id() + " Mo Dao chua xep loi");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void endingTruthAvailability(GameTestHelper h) {
        ServerPlayer p = player(h, "truth-req", 10);
        var ow = h.getLevel().getServer().overworld();
        int[] hu = new int[10];
        for (int i = 0; i < 10; i++) { hu[i] = HuAnh.get(ow, i); HuAnh.add(ow, i, -100); }
        var zones = ZoneData.get(ow).zones();
        boolean[] lib = new boolean[zones.size()];
        for (int i = 0; i < lib.length; i++) { lib[i] = zones.get(i).liberated; zones.get(i).liberated = true; }
        StoryData full = StoryData.DEFAULT.withMode(StoryData.MODE_STORY);
        for (int i = 1; i <= 10; i++) full = full.withDoc("c" + i);
        for (int i = 1; i <= ChgKit.COUNT; i++) full = full.withLair(ChgKit.defeatedBit(i));
        try {
            p.setData(StoryRegistry.STORY.get(), full);
            if (!EndgameService.truthAvailable(p)) h.fail("du dieu kien nhung khong mo: " + EndgameService.truthStatus(p));
            // thieu 1 Di thu C
            StoryData lack = StoryData.DEFAULT.withMode(StoryData.MODE_STORY);
            for (int i = 1; i <= 9; i++) lack = lack.withDoc("c" + i);
            for (int i = 1; i <= ChgKit.COUNT; i++) lack = lack.withLair(ChgKit.defeatedBit(i));
            p.setData(StoryRegistry.STORY.get(), lack);
            if (EndgameService.truthStatus(p).missing() != SieuDoKit.M_DOCS) h.fail("thieu c10 phai bao M_DOCS: " + EndgameService.truthStatus(p));
            // thieu CHG
            StoryData noChg = StoryData.DEFAULT.withMode(StoryData.MODE_STORY);
            for (int i = 1; i <= 10; i++) noChg = noChg.withDoc("c" + i);
            p.setData(StoryRegistry.STORY.get(), noChg.withLair(ChgKit.defeatedBit(1)));
            if ((EndgameService.truthStatus(p).missing() & SieuDoKit.M_CHG) == 0) h.fail("thieu CHG phai bao M_CHG");
            // Hu Anh cao
            p.setData(StoryRegistry.STORY.get(), full);
            for (int i = 0; i < 10; i++) HuAnh.add(ow, i, 60);
            if ((EndgameService.truthStatus(p).missing() & SieuDoKit.M_HU_ANH) == 0) h.fail("Hu Anh 60 phai bao M_HU_ANH");
            for (int i = 0; i < 10; i++) HuAnh.add(ow, i, -100);
            // thieu vung giai phong
            if (!zones.isEmpty()) {
                for (var z : zones) z.liberated = false;
                if ((EndgameService.truthStatus(p).missing() & SieuDoKit.M_ZONES) == 0) h.fail("0 vung giai phong phai bao M_ZONES");
            }
        } finally {
            for (int i = 0; i < lib.length; i++) zones.get(i).liberated = lib[i];
            for (int i = 0; i < 10; i++) { HuAnh.add(ow, i, -100); HuAnh.add(ow, i, hu[i]); }
        }
        h.succeed();
    }

    /** Intro, Tam Chien (4 tan anh), giu 4 Manh 60s (Da Khuyet bat tu), man ket; roi arena = that bai. Dung o man ket de khong doi The Gioi. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void tamChienPhaseSequence(GameTestHelper h) {
        ServerPlayer p = player(h, "sieu-do", 10);
        BlockPos center = h.absolutePos(new BlockPos(2, 2, 2));
        p.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        p.setInvulnerable(true);
        try {
            if (!SieuDo.begin(p, h.getLevel(), center)) h.fail("begin that bai");
            if (SieuDo.phase() != SieuDoKit.Phase.INTRO) h.fail("phai o INTRO");
            var dak = SieuDo.daKhuyet();
            if (dak == null || !dak.isInvulnerable() || dak.hurt(h.getLevel().damageSources().generic(), 50f)) h.fail("Da Khuyet phai bat tu");
            for (int i = 0; i < SieuDoKit.INTRO_TICKS + 2; i++) SieuDo.tickSession();
            if (SieuDo.phase() != SieuDoKit.Phase.TAM_CHIEN) h.fail("phai o TAM_CHIEN, dang " + SieuDo.phase());
            if (SieuDo.shadesAliveNow() != SieuDoKit.SHADES) h.fail("tan anh = " + SieuDo.shadesAliveNow());
            for (var e : h.getLevel().getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(center).inflate(40),
                    x -> x.getTags().contains(SieuDo.TAG_SHADE))) e.discard();
            for (int i = 0; i < 3; i++) SieuDo.tickSession();
            if (SieuDo.phase() != SieuDoKit.Phase.GIU_MANH) h.fail("phai o GIU_MANH, dang " + SieuDo.phase());
            for (int i = 0; i < SieuDoKit.HOLD_TICKS + 2 && SieuDo.phase() == SieuDoKit.Phase.GIU_MANH; i++) { p.setHealth(p.getMaxHealth()); SieuDo.tickSession(); }
            if (SieuDo.phase() != SieuDoKit.Phase.OUTRO) h.fail("phai o OUTRO, dang " + SieuDo.phase() + " hold=" + SieuDo.holdTicks());
            if (dak.getHealth() < dak.getMaxHealth()) h.fail("Da Khuyet bi mat mau");
        } finally {
            SieuDo.abort();
            p.setInvulnerable(false);
        }
        // roi arena giua tran -> that bai, phien ket thuc, nguoi choi giu co cho chon lai
        ServerPlayer q = player(h, "sieu-do-flee", 10);
        q.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        if (!SieuDo.begin(q, h.getLevel(), center)) h.fail("begin 2 that bai");
        q.setPos(center.getX() + 500.5, center.getY(), center.getZ() + 0.5);
        SieuDo.tickSession();
        if (SieuDo.active()) { SieuDo.abort(); h.fail("roi arena phai that bai"); }
        h.succeed();
    }
}
