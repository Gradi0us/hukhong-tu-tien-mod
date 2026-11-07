package com.hukhong.story.nemesis;

import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.story.StoryService;
import com.hukhong.story.nemesis.NemesisService.Session;
import com.hukhong.story.nemesis.NemesisService.Timeline;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Kịch bản các lần xuất hiện của Phản Thần Dạ Khuyết: đối thoại dài nhiều người (Dạ Khuyết / Bạn / Mộ Dao truyền âm),
 * phụ đề trong cutscene (SubLine, key lang có sẵn tên người nói), máy quay lượn chậm suốt cảnh, hiệu ứng nhịp theo lời.
 * Độ dài cảnh = tổng thời lượng lời thoại (mỗi câu ~2.5 s + thời gian đọc).
 */
final class NemesisScenes {
    private NemesisScenes() {}

    // ------------------------------------------------------------------ người nói + lời
    enum Spk {
        DK("Dạ Khuyết", ChatFormatting.DARK_PURPLE), ME("Bạn", ChatFormatting.AQUA), MD("Mộ Dao (truyền âm)", ChatFormatting.GOLD);
        final String name; final ChatFormatting color;
        Spk(String n, ChatFormatting c) { name = n; color = c; }
    }

    /** Một câu thoại: key lang (hukhong.nemesis.d.*), người nói, bản Việt/Anh (bản lang có thêm tiền tố tên). */
    record L(String key, Spk spk, String vi, String en) {
        int dur() { return 50 + (int) Math.round(vi.length() * 1.15); }
    }

    static L L(String key, Spk spk, String vi, String en) { return new L(key, spk, vi, en); }

    /** Đặt lời thoại từ tick {@code start}: phụ đề cutscene + dòng chat đúng lúc. Trả tick kết thúc. */
    static int talk(Timeline tl, List<SubLine> subs, ServerPlayer p, int start, L... lines) {
        int t = start;
        for (L l : lines) {
            int d = l.dur();
            subs.add(new SubLine(t, t + d - 6, "hukhong.nemesis.d." + l.key()));
            tl.at(t, x -> NemesisFx.chat(p, l.spk().name, l.spk().color, com.hukhong.auction.Tx.t("hukhong.nemesis.c." + l.key(), l.vi(), l.en())));
            t += d;
        }
        return t;
    }

    /** Máy quay lượn chậm quanh neo: mỗi ~90 tick đổi góc, đẩy vào dần. */
    static List<CamPath.Key> orbit(int total, float r0, float r1, float h0, float h1, float look, float swing) {
        List<CamPath.Key> ks = new ArrayList<>();
        int n = Math.max(2, total / 90);
        for (int i = 0; i <= n; i++) {
            float f = i / (float) n;
            double ang = Math.toRadians(Math.sin(f * Math.PI * 1.5) * swing);
            float r = r0 + (r1 - r0) * f;
            ks.add(new CamPath.Key(Math.round(total * f), (float) (Math.sin(ang) * r), h0 + (h1 - h0) * f, (float) (Math.cos(ang) * r), 0, 0, 62 - 8 * f, look));
        }
        return ks;
    }

    // ================================================================== c1: VISION - lần đầu gặp mặt
    static Timeline vision(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        List<SubLine> subs = new ArrayList<>();
        Timeline tl = new Timeline(1, NemesisService::finish);
        int end = talk(tl, subs, p, 20,
                L("v1", Spk.DK, "...Cuối cùng cũng có kẻ nghe thấy tiếng gọi của trăng khuyết.", "...At last, someone hears the call of the waning moon."),
                L("v2", Spk.ME, "Ngươi là ai? Sao lại đứng giữa cột sáng đó?", "Who are you? Why do you stand in that light?"),
                L("v3", Spk.DK, "Ta là kẻ từng canh giữ Thiên Môn. Người đời gọi ta là Dạ Khuyết.", "I once guarded the Heaven Gate. Mortals call me Da Khuyet."),
                L("v4", Spk.DK, "Còn ngươi... vừa mở mắt tu hành đã dám bước vào đất Hư Không.", "And you... barely awakened, yet you walk the Void's land."),
                L("v5", Spk.ME, "Hư Thi tràn khắp nơi. Là ngươi gây ra?", "The Hollow Dead are everywhere. Your doing?"),
                L("v6", Spk.DK, "Ta chỉ mở cửa. Những gì bước qua có ý chí của riêng chúng.", "I only opened a door. What came through has its own will."),
                L("v7", Spk.DK, "Lão Mộ Dao hẳn chưa kể, vì sao lão ở lại Lưu Ly Cảng suốt ba trăm năm.", "Old Mo Dao never told you why he stayed in Luu Ly for three hundred years."),
                L("v8", Spk.ME, "Lão ấy không liên quan gì đến ngươi.", "He has nothing to do with you."),
                L("v9", Spk.DK, "Mọi thứ dưới ánh trăng này đều liên quan đến ta.", "Everything beneath this moon has to do with me."),
                L("v10", Spk.DK, "Tu luyện đi. Mạnh lên. Ta cần một kẻ đủ sức đứng trước cánh cửa ấy.", "Cultivate. Grow strong. I need someone fit to stand before that door."),
                L("v11", Spk.DK, "Đêm nay trăng khuyết. Hẹn gặp lại, kẻ đi trong bóng đêm.", "The moon wanes tonight. Until we meet again, walker in the dark."));
        int total = end + 40;
        tl = retime(tl, total);
        Timeline T = tl;
        T.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 22, p.getRandom().nextDouble() * 300 - 150);
            x.anchor = c;
            x.avatar = NemesisService.spawnPhantom(p, c);
            NemesisFx.darkness(p, 60);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), total, orbit(total, 18f, 6.5f, 3f, 2.2f, 2.2f, 35f), subs);
            NemesisFx.pillar(lv, c, NemesisFx.VOID_RGB, 120);
            NemesisFx.moonRune(lv, p, c, 7f, total);
            NemesisFx.growl(lv, c);
        });
        for (int t = 160; t < end; t += 160) T.at(t, x -> NemesisFx.voidRing(lv, p, x.anchor, 3.5f, 60));
        for (int t = end; t <= end + 24; t++) { int k = t; T.at(t, x -> NemesisFx.converge(lv, x.anchor, 6.0 * (end + 24 - k) / 24.0, 10)); }
        T.at(end + 26, x -> { NemesisFx.implode(p, lv, x.anchor); NemesisService.discardAvatar(x); });
        return T;
    }

    // ================================================================== c4: TAUNT - bóng sau lưng, bốn bàn tay
    static Timeline taunt(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        List<SubLine> subs = new ArrayList<>();
        Timeline tl = new Timeline(1, NemesisService::finish);
        boolean docs = !StoryService.get(p).docs().isEmpty();
        int end = talk(tl, subs, p, 14,
                L("t1", Spk.DK, "Đừng quay lại. Cứ đứng yên mà nghe.", "Don't turn around. Stand still and listen."),
                L("t2", Spk.ME, "...Dạ Khuyết.", "...Da Khuyet."),
                docs ? L("t3a", Spk.DK, "Từng mảnh Văn Kiện ngươi gom... đều mang tên ta.", "Every dossier page you gathered... bears my name.")
                     : L("t3b", Spk.DK, "Chưa có nổi một mảnh Văn Kiện, mà dám lần theo ta?", "Not one dossier page, and you dare trace me?"),
                L("t4", Spk.DK, "Bốn Chấp Hành Giả là bốn bàn tay của ta. Mỗi kẻ giữ một mảnh Thiên Môn.", "The four Executors are my four hands. Each holds a shard of the Heaven Gate."),
                L("t5", Spk.DK, "Thạch Hống giữ Thể. Bạch Từ Âm giữ Đạo. Lăng Uyên giữ Kiếm.", "Thach Hong holds the Body. Bach Tu Am, the Way. Lang Uyen, the Sword."),
                L("t6", Spk.DK, "Còn Cơ Vô Tận... đến ta cũng không chắc hắn còn là người.", "And Co Vo Tan... even I am unsure he is still a man."),
                L("t7", Spk.ME, "Sao ngươi lại nói cho ta biết?", "Why tell me this?"),
                L("t8", Spk.DK, "Vì ta muốn ngươi giết chúng. Từng kẻ một.", "Because I want you to kill them. One by one."),
                L("t9", Spk.ME, "Ngươi muốn dùng ta như một thanh kiếm?", "You would wield me like a sword?"),
                L("t10", Spk.DK, "Kiếm tốt thì không hỏi tay ai cầm. Ánh sáng của ngươi... ta mượn một chút.", "A good sword never asks whose hand holds it. Your light... I'll borrow a little."),
                L("t11", Spk.DK, "Lần sau, ngươi sẽ không còn được thấy mặt ta.", "Next time, you will not see my face."));
        int total = end + 40;
        Timeline T = retime(tl, total);
        T.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 4, 180); // sau lưng
            x.anchor = c;
            x.avatar = NemesisService.spawnPhantom(p, c);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), total, List.of(
                    new CamPath.Key(0, 6f, 2.6f, 2.5f, 0, 0, 62, 2.2f),
                    new CamPath.Key(total / 3, 4.5f, 2.2f, -3f, 0, 0, 58, 2.4f),
                    new CamPath.Key(total * 2 / 3, -5f, 2.8f, 1f, 0, 0, 56, 2.2f),
                    new CamPath.Key(total, -3.5f, 2.1f, 4f, 0, 0, 52, 2.0f)), subs);
            NemesisFx.voidRing(lv, p, c, 3f, 40);
        });
        T.at(10, x -> NemesisFx.voidRing(lv, p, x.anchor, 6f, 50));
        int steal = Math.max(40, end - 200);
        for (int t = steal; t <= steal + 40; t += 2) T.at(t, x -> NemesisFx.drain(lv, p.position().add(0, 1.6, 0), x.anchor.add(0, 1.4, 0)));
        T.at(steal + 20, x -> { NemesisFx.blindness(p); NemesisFx.growl(lv, x.anchor); });
        T.at(steal + 60, x -> {
            Fx.play(lv, FxType.SHOCKWAVE, x.anchor.add(0, 0.1, 0), Vec3.ZERO, 3f, NemesisFx.VOID_RGB, 16);
            NemesisFx.voidRing(lv, p, x.anchor, 8f, 50);
            Fx.shake(lv, x.anchor, 0.5f, 12, 16f);
        });
        for (int t = end; t <= end + 24; t++) { int k = t; T.at(t, x -> NemesisFx.converge(lv, x.anchor, 2.5 * (end + 24 - k) / 24.0 + 0.2, 8)); }
        T.at(end + 26, x -> { NemesisFx.implode(p, lv, x.anchor); NemesisService.discardAvatar(x); });
        return T;
    }

    // ================================================================== c6: FIGHT - mở đầu
    static Timeline fightIntro(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        List<SubLine> subs = new ArrayList<>();
        Timeline tl = new Timeline(1, NemesisService::startFight);
        int end = talk(tl, subs, p, 24,
                L("f1", Spk.MD, "Tiểu tử, lùi lại! Đó là Ảnh của hắn, không phải bóng ma tầm thường!", "Child, step back! That is his Shade, no common ghost!"),
                L("f2", Spk.DK, "Mộ Dao... ba trăm năm rồi, giọng ngươi vẫn run như thế.", "Mo Dao... three hundred years, and your voice still trembles."),
                L("f3", Spk.ME, "Ta không lùi.", "I won't step back."),
                L("f4", Spk.DK, "Tốt. Để ta xem ngươi được bao nhiêu.", "Good. Let me see what you are worth."),
                L("f5", Spk.DK, "Ta chỉ là một cái bóng... nhưng bóng cũng biết cắn.", "I am only a shadow... but shadows bite too."));
        int total = end + 10;
        Timeline T = retime(tl, total);
        T.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 9, 0);
            x.anchor = c;
            x.dim = lv.dimension();
            x.avatar = NemesisService.spawnAvatar(p, c);
            NemesisFx.darkness(p, 60);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), total, orbit(total, 9f, 5.5f, 2.6f, 3.2f, 2.4f, 40f), subs);
            NemesisFx.pillar(lv, c, NemesisFx.VOID_RGB, 90);
            NemesisFx.moonRune(lv, p, c, 7f, total);
            NemesisFx.growl(lv, c);
        });
        T.at(end - 30, x -> NemesisFx.voidRing(lv, p, x.anchor, 3f, 30));
        return T;
    }

    // ================================================================== c6: FIGHT - rút lui (≤35% máu): lộ quá khứ với Mộ Dao
    static Timeline retreat(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        List<SubLine> subs = new ArrayList<>();
        Timeline tl = new Timeline(1, NemesisService::finish);
        int end = talk(tl, subs, p, 6,
                L("r1", Spk.DK, "...Chưa phải lúc.", "...Not yet."),
                L("r2", Spk.DK, "Ngươi mạnh hơn ta tưởng. Mộ Dao dạy dỗ khá lắm.", "Stronger than I thought. Mo Dao taught you well."),
                L("r3", Spk.ME, "Lão ấy biết ngươi?", "He knows you?"),
                L("r4", Spk.DK, "Biết? Chính lão là kẻ đã chém đứt nửa thân ta dưới chân Thiên Môn.", "Knows? He is the one who cut away half my body beneath the Heaven Gate."),
                L("r5", Spk.MD, "...Đừng nghe hắn.", "...Do not listen to him."),
                L("r6", Spk.DK, "Hỏi lão đi. Đêm ấy lão chọn phong ấn ta... hay chọn bỏ mặc cả một tòa thành?", "Ask him. That night, did he choose to seal me... or to abandon a whole city?"),
                L("r7", Spk.DK, "Ta sẽ quay lại, khi bóng đêm đủ dày.", "I will return when the darkness is thick enough."));
        int total = end + 30;
        Timeline T = retime(tl, total);
        T.at(0, x -> {
            Mob a = x.avatar;
            Vec3 c = a != null && a.isAlive() ? a.position() : x.anchor;
            x.anchor = c;
            if (a != null) { a.setNoAi(true); a.setTarget(null); a.setInvulnerable(true); }
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), total, orbit(total, 7f, 4.5f, 2.4f, 2.8f, 2.4f, 50f), subs);
        });
        for (int t = 60; t < end; t += 120) T.at(t, x -> NemesisFx.voidRing(lv, p, x.anchor, 3f, 50));
        for (int t = end - 30; t <= end; t++) { int k = t; T.at(t, x -> NemesisFx.converge(lv, x.anchor, 3.0 * (end - k) / 30.0 + 0.1, 8)); }
        T.at(end + 2, x -> { NemesisFx.implode(p, lv, x.anchor); NemesisService.discardAvatar(x); });
        return T;
    }

    // ================================================================== c9: REVELATION - cánh cửa và lựa chọn
    static Timeline revelation(Session s) {
        ServerPlayer p = s.p;
        ServerLevel lv = p.serverLevel();
        List<SubLine> subs = new ArrayList<>();
        Timeline tl = new Timeline(1, NemesisService::finish);
        int end = talk(tl, subs, p, 20,
                L("e1", Spk.DK, "Ngươi đã đi xa thật. Ba Chấp Hành Giả ngã xuống dưới tay ngươi.", "You have come far. Three Executors fell by your hand."),
                L("e2", Spk.ME, "Còn lại ngươi. Và cánh cửa.", "What remains is you. And the door."),
                L("e3", Spk.DK, "Cánh cửa đó... chính tay ta đã mở ra.", "That door... I opened it with my own hand."),
                L("e4", Spk.DK, "Chư thần cho rằng giới hạn là để bảo vệ. Ta thì thấy nó là xiềng xích.", "The gods called limits a shelter. I saw only chains."),
                L("e5", Spk.DK, "Ta muốn biết bên kia có gì. Và bên kia đã trả lời.", "I wanted to know what lay beyond. And beyond answered."),
                L("e6", Spk.ME, "Nguyên Thủy Tịch Diệt.", "The Primordial Annihilation."),
                L("e7", Spk.DK, "Nó không phải chủ ta. Nó là một cơn đói không có tên.", "It is not my master. It is a hunger with no name."),
                L("e8", Spk.DK, "Ta tưởng mình thuần hóa được nó. Ta đã sai... nửa thân ta là cái giá.", "I thought I could tame it. I was wrong... half my body was the price."),
                L("e9", Spk.MD, "Vậy nên ngươi muốn đứa nhỏ này sửa sai thay ngươi?", "So you want this child to mend your mistake?"),
                L("e10", Spk.DK, "Không. Ta muốn nó đứng trước đúng lựa chọn mà ta từng đứng.", "No. I want it to face the very choice I once faced."),
                L("e11", Spk.DK, "Phong ấn, hay hấp thụ. Đóng cửa lại... hoặc trở thành chính cánh cửa.", "Seal, or absorb. Close the door... or become the door."),
                L("e12", Spk.ME, "Ta sẽ không giống ngươi.", "I will not become you."),
                L("e13", Spk.DK, "Ai cũng nói vậy. Đến ngày cuối, ta sẽ đích thân đón ngươi.", "They all say that. On the last day, I will receive you myself."));
        int total = end + 40;
        Timeline T = retime(tl, total);
        T.at(0, x -> {
            Vec3 c = NemesisFx.around(lv, p, 14, 0);
            x.anchor = c;
            x.avatar = NemesisService.spawnPhantom(p, c);
            NemesisFx.darkness(p, 60);
            NemesisFx.cam(p, c, NemesisFx.yawFromTo(c, p.position()), total, orbit(total, 6f, 20f, 2.2f, 9f, 3f, 45f), subs);
            riftAt(lv, p, c, total / 2);
            NemesisFx.growl(lv, c);
        });
        T.at(total / 2, x -> riftAt(lv, p, x.anchor, total / 2));
        for (int t = 200; t < end; t += 200) T.at(t, x -> { NemesisFx.voidRing(lv, p, x.anchor, 5f, 60); Fx.shake(lv, x.anchor, 0.3f, 10, 20f); });
        for (int t = end; t <= end + 30; t++) { int k = t; T.at(t, x -> NemesisFx.converge(lv, x.anchor, 3.0 * (end + 30 - k) / 30.0 + 0.1, 8)); }
        T.at(end + 32, x -> { NemesisFx.implode(p, lv, x.anchor); NemesisService.discardAvatar(x); });
        return T;
    }

    /** Dòng thời gian mới đúng độ dài, chép các hành động đã đặt (lời thoại). */
    private static Timeline retime(Timeline src, int total) {
        Timeline t = new Timeline(total, src.after);
        src.acts.forEach((k, v) -> v.forEach(a -> t.at(k, a)));
        return t;
    }

    /** Vết rách Hư Không mở trước mặt (vuông góc với hướng nhìn của người chơi). */
    private static void riftAt(ServerLevel lv, ServerPlayer p, Vec3 c, int life) {
        Vec3 d = p.position().subtract(c);
        Vec3 perp = new Vec3(-d.z, 0, d.x);
        perp = perp.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : perp.normalize().scale(6);
        Vec3 back = d.lengthSqr() < 1e-6 ? Vec3.ZERO : new Vec3(-d.x, 0, -d.z).normalize().scale(3.5); // sau lưng Dạ Khuyết
        Fx.rift(lv, p, c.add(back).add(0, 3.6, 0), perp.scale(1.3), 3.4f, NemesisFx.RIFT_RGB, life);
    }
}
