package com.hukhong.story.nemesis;

import com.hukhong.boss.phase.CamPath;
import com.hukhong.story.StoryService;
import com.hukhong.story.nemesis.NemesisService.Session;
import com.hukhong.story.nemesis.NemesisService.Timeline;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.hukhong.story.nemesis.NemesisService.INTRO_TICKS;
import static com.hukhong.story.nemesis.NemesisService.REVEAL_TICKS;
import static com.hukhong.story.nemesis.NemesisService.RETREAT_TICKS;
import static com.hukhong.story.nemesis.NemesisService.TAUNT_TICKS;
import static com.hukhong.story.nemesis.NemesisService.VISION_TICKS;

/**
 * Kịch bản từng lần xuất hiện của Phản Thần Dạ Khuyết (dòng thời gian tick + máy quay + lời thoại).
 * Lời thoại viết inline qua Tx.t (có bản dịch tiếng Anh dự phòng), không sửa file lang.
 */
final class NemesisScenes {
    private NemesisScenes() {}

    /** Lời thoại có bản dự phòng tiếng Việt/Anh (Tx.t); args thay cho %s trong bản dự phòng. */
    private static Component c(String key, String vi, String en, Object... args) {
        return com.hukhong.auction.Tx.t("hukhong.nemesis." + key, vi, en, args);
    }

    // ================================================================== c1: VISION - hiện ra xa
    static Timeline vision(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        Timeline tl = new Timeline(VISION_TICKS, NemesisService::finish);
        tl.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 25, p.getRandom().nextDouble() * 300 - 150);
            x.anchor = c;
            x.avatar = NemesisService.spawnPhantom(p, c);
            NemesisFx.darkness(p, 90);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), VISION_TICKS, List.of(
                    new CamPath.Key(0, 0f, 3f, 20f, 0, 0, 62, 2.5f),
                    new CamPath.Key(VISION_TICKS / 2, 2.5f, 2.6f, 13f, 0, 0, 56, 2f),
                    new CamPath.Key(VISION_TICKS, -2f, 2.2f, 8f, 0, 0, 50, 2f)), List.of(new com.hukhong.boss.phase.BossPhaseConfig.SubLine(20, 58, "hukhong.nemesis.vision.1"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(60, 98, "hukhong.nemesis.vision.2"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(100, 138, "hukhong.nemesis.vision.3"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(140, 192, "hukhong.nemesis.vision.4")));
            NemesisFx.pillar(lv, c, NemesisFx.VOID_RGB, 90);
            NemesisFx.moonRune(lv, p, c, 7f, 150);
            NemesisFx.silhouette(lv, p, c, 1.8f, 150);
            NemesisFx.growl(lv, c);
        });
        tl.at(20, x -> NemesisFx.say(p, c("vision.1", "...Ngươi đã tìm thấy ta rồi sao?", "...You have found me already?")));
        tl.at(60, x -> {
            NemesisFx.say(p, c("vision.2", "Đêm nay trăng khuyết. Hãy nhớ lấy điều đó.", "Tonight the moon wanes. Remember that."));
            NemesisFx.silhouette(lv, p, x.anchor, 1.8f, 60);
        });
        tl.at(100, x -> NemesisFx.say(p, c("vision.3", "Ta đã dõi theo ngươi từ bước chân đầu tiên.", "I have watched every step you have taken since the first.")));
        tl.at(140, x -> NemesisFx.say(p, c("vision.4", "Hẹn gặp lại, kẻ đi trong bóng đêm.", "Until we meet again, walker in the dark.")));
        for (int t = 170; t <= 194; t++) {
            int k = t;
            tl.at(t, x -> NemesisFx.converge(lv, x.anchor, 6.0 * (194 - k) / 24.0, 10));
        }
        tl.at(196, x -> { NemesisFx.implode(p, lv, x.anchor); NemesisService.discardAvatar(x); });
        return tl;
    }

    // ================================================================== c4: TAUNT - bóng sau lưng, hút ánh sáng
    static Timeline taunt(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        Timeline tl = new Timeline(TAUNT_TICKS, NemesisService::finish);
        tl.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 4, 180); // sau lưng người chơi
            x.anchor = c;
            x.avatar = NemesisService.spawnPhantom(p, c);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), TAUNT_TICKS, List.of(
                    new CamPath.Key(0, 6f, 2.6f, 2.5f, 0, 0, 62, 2.2f),
                    new CamPath.Key(TAUNT_TICKS / 2, 5f, 2.3f, 3.5f, 0, 0, 58, 2.2f),
                    new CamPath.Key(TAUNT_TICKS, 3.8f, 2.1f, 4.2f, 0, 0, 54, 2.2f)), List.of(new com.hukhong.boss.phase.BossPhaseConfig.SubLine(12, 42, "hukhong.nemesis.taunt.1"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(44, 78, StoryService.get(p).docs().isEmpty() ? "hukhong.nemesis.taunt.2b" : "hukhong.nemesis.taunt.2a"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(80, 148, "hukhong.nemesis.taunt.3"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(150, 196, "hukhong.nemesis.taunt.4")));
            NemesisFx.silhouette(lv, p, c, 1.5f, 130);
            NemesisFx.voidRing(lv, p, c, 3f, 40);
        });
        tl.at(8, x -> NemesisFx.voidRing(lv, p, x.anchor, 5f, 40));
        tl.at(16, x -> NemesisFx.voidRing(lv, p, x.anchor, 7f, 40));
        tl.at(12, x -> NemesisFx.say(p, c("taunt.1", "Ngươi đang lần theo dấu vết của ta à?", "Following my traces, are you?")));
        for (int t = 30; t <= 56; t += 2) {
            tl.at(t, x -> NemesisFx.drain(lv, p.position().add(0, 1.6, 0), x.anchor.add(0, 1.4, 0)));
        }
        tl.at(40, x -> {
            NemesisFx.blindness(p);
            NemesisFx.growl(lv, x.anchor);
        });
        tl.at(44, x -> {
            int n = StoryService.get(p).docs().size();
            if (n > 0) NemesisFx.say(p, c("taunt.2a", "%s mảnh Văn Kiện... mảnh nào cũng mang tên ta.",
                    "You gathered %s dossier pages. Every one bears my name.", n));
            else NemesisFx.say(p, c("taunt.2b", "Chưa có nổi một mảnh Văn Kiện, mà dám lần theo ta?",
                    "You have not gathered a single dossier page, yet you dare to trace me."));
        });
        tl.at(80, x -> {
            NemesisFx.say(p, c("taunt.3", "Ánh sáng của ngươi... ta sẽ lấy đi một chút.", "Your light... I shall take a little of it."));
            NemesisFx.silhouette(lv, p, x.anchor, 1.5f, 60);
        });
        tl.at(120, x -> {
            Fx.play(lv, FxType.SHOCKWAVE, x.anchor.add(0, 0.1, 0), Vec3.ZERO, 3f, NemesisFx.VOID_RGB, 16);
            NemesisFx.voidRing(lv, p, x.anchor, 8f, 50);
            Fx.shake(lv, x.anchor, 0.5f, 12, 16f);
        });
        tl.at(150, x -> NemesisFx.say(p, c("taunt.4", "Lần sau, ngươi sẽ không còn được thấy mặt ta.", "Next time, you will not see my face.")));
        for (int t = 168; t <= 196; t++) {
            int k = t;
            tl.at(t, x -> NemesisFx.converge(lv, x.anchor, 2.5 * (196 - k) / 28.0 + 0.2, 8));
        }
        tl.at(198, x -> { NemesisFx.implode(p, lv, x.anchor); NemesisService.discardAvatar(x); });
        return tl;
    }

    // ================================================================== c6: FIGHT - mở đầu (Ảnh xuất hiện, trận bắt đầu sau đó)
    static Timeline fightIntro(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        Timeline tl = new Timeline(INTRO_TICKS, NemesisService::startFight);
        tl.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 9, 0);
            x.anchor = c;
            x.dim = lv.dimension();
            x.avatar = NemesisService.spawnAvatar(p, c);
            NemesisFx.darkness(p, 80);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), INTRO_TICKS, List.of(
                    new CamPath.Key(0, -2f, 2.5f, 9f, 0, 0, 62, 2.2f),
                    new CamPath.Key(INTRO_TICKS / 2, 2f, 3f, 7f, 0, 0, 58, 2.4f),
                    new CamPath.Key(INTRO_TICKS, -1f, 3.4f, 6f, 0, 0, 56, 2.6f)), List.of(new com.hukhong.boss.phase.BossPhaseConfig.SubLine(26, 50, "hukhong.nemesis.fight.intro1"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(52, INTRO_TICKS, "hukhong.nemesis.fight.intro2")));
            NemesisFx.pillar(lv, c, NemesisFx.VOID_RGB, 90);
            NemesisFx.moonRune(lv, p, c, 7f, 90);
            NemesisFx.growl(lv, c);
        });
        tl.at(14, x -> NemesisFx.silhouette(lv, p, x.anchor, 1.6f, 60));
        tl.at(26, x -> NemesisFx.say(p, c("fight.intro1", "Ngươi đã tới. Để ta xem ngươi được bao nhiêu.", "You came. Let me see how far you can go.")));
        tl.at(52, x -> NemesisFx.say(p, c("fight.intro2", "Ta chỉ là một cái bóng... nhưng bóng cũng biết cắn.", "I am only a shadow... but shadows bite too.")));
        tl.at(68, x -> NemesisFx.voidRing(lv, p, x.anchor, 3f, 30));
        return tl;
    }

    // ================================================================== c6: FIGHT - rút lui (≤35% máu)
    static Timeline retreat(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        Timeline tl = new Timeline(RETREAT_TICKS, NemesisService::finish);
        tl.at(0, x -> {
            Mob a = x.avatar;
            Vec3 c = a != null && a.isAlive() ? a.position().add(0, 0.5, 0) : x.anchor;
            x.anchor = c;
            if (a != null) { a.setNoAi(true); a.setTarget(null); }
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), RETREAT_TICKS, List.of(
                    new CamPath.Key(0, -1f, 2.2f, 6f, 0, 0, 64, 2f),
                    new CamPath.Key(RETREAT_TICKS / 2, 3f, 3.4f, 5f, 0, 0, 60, 2.4f),
                    new CamPath.Key(RETREAT_TICKS, 0.5f, 2.8f, 4.5f, 0, 0, 56, 2.6f)), List.of(new com.hukhong.boss.phase.BossPhaseConfig.SubLine(0, 44, "hukhong.nemesis.retreat.1"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(46, RETREAT_TICKS, "hukhong.nemesis.retreat.2")));
            NemesisFx.say(p, c("retreat.1", "...Chưa phải lúc.", "...Not yet."));
        });
        for (int t = 10; t <= 40; t++) {
            int k = t;
            tl.at(t, x -> NemesisFx.converge(lv, x.anchor, 3.0 * (40 - k) / 30.0 + 0.1, 8));
        }
        tl.at(40, x -> {
            NemesisFx.implode(p, lv, x.anchor);
            NemesisService.discardAvatar(x);
        });
        tl.at(46, x -> NemesisFx.say(p, c("retreat.2", "Ta sẽ quay lại, khi bóng đêm đủ dày.", "I will return when the darkness is thick enough.")));
        return tl;
    }

    // ================================================================== c9: REVELATION - tiết lộ cánh cửa Hư Không
    static Timeline revelation(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        Timeline tl = new Timeline(REVEAL_TICKS, NemesisService::finish);
        tl.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 16, 0);
            x.anchor = c;
            x.avatar = NemesisService.spawnPhantom(p, c);
            NemesisFx.darkness(p, 60);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), REVEAL_TICKS, List.of(
                    new CamPath.Key(0, 0.5f, 2.0f, 5f, 0, 0, 58, 1.5f),
                    new CamPath.Key(120, -3f, 4.5f, 12f, 0, 0, 66, 3f),
                    new CamPath.Key(REVEAL_TICKS, 2f, 9f, 20f, 0, 0, 74, 5f)), List.of(new com.hukhong.boss.phase.BossPhaseConfig.SubLine(15, 58, "hukhong.nemesis.reveal.1"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(60, 108, "hukhong.nemesis.reveal.2"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(110, 158, "hukhong.nemesis.reveal.3"), new com.hukhong.boss.phase.BossPhaseConfig.SubLine(160, 220, "hukhong.nemesis.reveal.4")));
            riftAt(lv, p, c, 170);
            NemesisFx.silhouette(lv, p, c, 2f, 170);
            NemesisFx.pillar(lv, c, NemesisFx.RIFT_RGB, 170);
            NemesisFx.growl(lv, c);
        });
        tl.at(15, x -> NemesisFx.say(p, c("reveal.1", "Cánh cửa đó... chính tay ta đã mở ra.", "That door... I opened it with my own hand.")));
        tl.at(60, x -> NemesisFx.say(p, c("reveal.2", "Hư Không đã bước qua từ lâu, chỉ chờ ngươi tới.", "The Void crossed it long ago, waiting for you to draw near.")));
        tl.at(110, x -> {
            NemesisFx.say(p, c("reveal.3", "Ngươi tưởng đang săn ta? Không. Ngươi đang về phía cánh cửa.", "You think you hunt me? No. You walk toward the door."));
            riftAt(lv, p, x.anchor, 90);
        });
        tl.at(160, x -> NemesisFx.say(p, c("reveal.4", "Đến ngày cuối, ta sẽ đích thân đón ngươi.", "On the last day, I will receive you myself.")));
        for (int t = 190; t <= 224; t++) {
            int k = t;
            tl.at(t, x -> NemesisFx.converge(lv, x.anchor, 3.0 * (224 - k) / 34.0 + 0.1, 8));
        }
        tl.at(228, x -> { NemesisFx.implode(p, lv, x.anchor); NemesisService.discardAvatar(x); });
        return tl;
    }

    /** Vết rách Hư Không mở trước mặt (vuông góc với hướng nhìn của người chơi). */
    private static void riftAt(ServerLevel lv, ServerPlayer p, Vec3 c, int life) {
        Vec3 d = p.position().subtract(c);
        Vec3 perp = new Vec3(-d.z, 0, d.x);
        perp = perp.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : perp.normalize().scale(6);
        Fx.rift(lv, p, c.add(0, 3, 0), perp, 3f, NemesisFx.RIFT_RGB, life);
    }
}
