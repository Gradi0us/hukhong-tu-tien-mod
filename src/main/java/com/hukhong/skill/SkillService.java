package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.Resources;
import com.hukhong.skillbook.BookSkillEffects;
import com.hukhong.skillbook.SkillBook;
import com.hukhong.skillbook.SkillBookData;
import com.hukhong.skillbook.SkillBookMath;
import com.hukhong.skillbook.SkillBookRuntime;
import com.hukhong.skillbook.SkillBookService;
import com.hukhong.network.SkillSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Xác thực + thực thi cast kỹ năng phía server. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SkillService {
    private SkillService() {}

    public static SkillState state(ServerPlayer p) { return p.getData(SkillAttachments.STATE); }

    public static void toggleStance(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        SkillState st = state(p);
        if (Skill.skillPath(d, st.page) == null) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_skill_path"), true);
            return;
        }
        if (st.stance) stanceOff(p, st);
        else { st.stance = true; StanceHotbar.enter(p, Skill.skillPath(d, st.page)); }
        p.displayClientMessage(Component.translatable(st.stance ? "hukhong.stance.on" : "hukhong.stance.off"), true);
        sync(p);
    }

    /** Tắt thế (chết, ...): dừng kênh, trả thanh nhanh gốc. An toàn khi thế đã tắt. */
    public static void forceStanceOff(ServerPlayer p) {
        SkillState st = state(p);
        if (st.stance) stanceOff(p, st);
        else StanceHotbar.exit(p);
        sync(p);
    }

    private static void stanceOff(ServerPlayer p, SkillState st) {
        st.stance = false;
        SkillEffects.kiemVucStop(p, st, true);
        com.hukhong.phaptu.beam.PhapBeamService.cancel(p);
        com.hukhong.phaptu.ThiemBoService.cancel(p);
        StanceHotbar.exit(p);
    }

    /** Phím V: đổi trang giữa các đạo đang sở hữu; Phân Tâm = hồi chiêu chung 3s. */
    public static void cyclePage(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        int n = SkillPages.owned(d.paths()).size();
        if (n < 2 || !p.isAlive() || p.isSpectator()) return;
        SkillState st = state(p);
        long now = p.level().getGameTime();
        if (st.lastSwitch + SkillPages.SWITCH_COOLDOWN_TICKS > now) return;
        st.page = SkillPages.next(st.page, n);
        SkillEffects.kiemVucStop(p, st, true);
        com.hukhong.phaptu.beam.PhapBeamService.cancel(p);
        com.hukhong.phaptu.ThiemBoService.cancel(p);
        st.lastSwitch = now;
        for (int i = 0; i < 4; i++) st.cooldownEnd[i] = Math.max(st.cooldownEnd[i], now + SkillPages.SWITCH_COOLDOWN_TICKS);
        p.displayClientMessage(Component.translatable("hukhong.msg.page_switched",
                Component.translatable(SkillPages.pathLangKey(SkillPages.active(d, st.page)))), true);
        if (st.stance) { StanceHotbar.exit(p); StanceHotbar.enter(p, Skill.skillPath(d, st.page)); }
        sync(p);
    }

    public static void cast(ServerPlayer p, int slot) {
        if (slot < 0 || slot > 3 || !p.isAlive() || p.isSpectator()) return;
        SkillState st = state(p);
        if (!st.stance) return; // server chỉ nhận cast khi thế bật
        if (p.level() instanceof net.minecraft.server.level.ServerLevel bl) com.hukhong.boss.ai.BossBrain.notifySkillCast(bl, p.position(), 12); // boss có brain có thể né
        if (com.hukhong.phaptu.beam.PhapBeamService.busy(p)) return; // đang nạp / sẵn sàng / bắn Tinh Hà Quang Trụ
        if (com.hukhong.phaptu.ThoTitan.interceptCast(p, slot)) return; // đang hợp thể trụ cấp 3: Q/R/T là kỹ năng trụ
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        Path page = Skill.skillPath(d, st.page);
        SkillBookData bd = SkillBookService.data(p);
        boolean lockT = page == Path.PHAP && com.hukhong.phaptu.beam.PhapBeamService.locksT(p);
        if (lockT && slot == 3) return; // phím T của Quang/Lôi/Hỏa/Thủy đi qua PhapBeamService (nạp -> bắn), không cast tức thời
        if (page == Path.MA && slot == 2 && MaDomain.active(p) && p.level() instanceof net.minecraft.server.level.ServerLevel ml) { // Ma Vực: R lần 2 = nổ
            MaDomain.detonate(ml, p);
            sync(p);
            return;
        }
        if (page == Path.TRAN) { // Trận Tu: 4 phím cố định; T đổi thế, Q/E/R theo thế (TranSkills)
            com.hukhong.tran.TranSkills.cast(p, st, slot);
            sync(p);
            return;
        }
        SkillBook.Resolved r = SkillBook.resolve(bd, page, slot, d.level(), page == Path.PHAP ? com.hukhong.phaptu.ThoService.lockMask(p) : 0); // Thổ khoá E + T
        // phím R: Bản Mệnh / Vạn Cốt Cộng Minh, trừ khi có kỹ năng gán thủ công và chưa mở Bản Mệnh
        if (slot == 2 && r == null) {
            SkillBookRuntime.clear(p);
            BanMenhSkill animSig = BanMenhSkillEffects.useResonance(p, st) ? null : BanMenhSkillEffects.signature(p, st);
            long rBefore = st.cooldownEnd[2];
            BanMenhSkillEffects.castR(p, st);
            if (st.cooldownEnd[2] != rBefore) com.hukhong.client.anim.CastAnimHook.playBanMenh(p, animSig); // cast thành công -> tư thế
            if (st.cooldownEnd[2] != rBefore && page == Path.KIEM && p.level() instanceof net.minecraft.server.level.ServerLevel kl)
                KiemRend.detonate(kl, p); // Kiếm Tu: R Bản Mệnh kích nổ Xé Toạc
            sync(p);
            return;
        }
        if (r == null) return;
        if (com.hukhong.pathrules.MaRules.isLost(p)) { // Mất Kiểm Soát: khóa phím kỹ năng
            p.displayClientMessage(Component.translatable("hukhong.ma.lost_control"), true);
            return;
        }
        int lvl = bd.level(r.id());
        if (r.book() != null && d.level() < r.book().minLevel()) return;
        long now = p.level().getGameTime();
        if (st.cooldownEnd[slot] > now) return;
        if (r.base() == Skill.KIEM_ULT) { // Kiếm Vực: kênh bật/tắt, không hồi chiêu (chỉ khoá 1s chống bấm liên tục)
            SkillBookRuntime.clear(p);
            boolean ok;
            if (st.domainOn) { SkillEffects.kiemVucStop(p, st, false); ok = true; }
            else { ok = SkillEffects.kiemVucStart(p, st, slot); if (ok) { com.hukhong.client.anim.CastAnimHook.play(p, r.base(), null, slot); SkillEffects.kiemVucTick(p, st); } } // nhịp đầu ngay lập tức
            if (ok) { st.cooldownEnd[slot] = now + SkillEffects.DOMAIN_LOCK_TICKS; st.lastCombat = now; }
            sync(p);
            return;
        }
        var before = p.getData(com.hukhong.core.ModAttachments.CULTIVATION);
        if (!com.hukhong.phaptu.PhapRuntime.consumeFree(p, page, slot)
                && !com.hukhong.dan.DanRuntime.freeCast(p, page)
                && !Resources.tryConsume(p, r.cost(), com.hukhong.banmenh.BanMenhService.skillCostFactor(p) * com.hukhong.phaptu.QuangLoiFlow.costMult(p, page, slot))) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
            return;
        }
        com.hukhong.phaptu.QuangLoiFlow.afterCast(p, page, slot, com.hukhong.phaptu.QuangLoiFlow.spent(before, p)); // Quang/Lôi: hoàn mana khi trúng
        com.hukhong.skill.DaoThanhQuang.onCast(p); // Đạo: 5 kỹ năng = 1 tầng Thánh Quang
        com.hukhong.client.anim.CastAnimHook.play(p, r.base(), r.book(), slot); // tư thế thi triển (client)
        if (r.base() != null) {
            SkillBookRuntime.begin(p, SkillBookMath.dmgMult(lvl), SkillBookRuntime.windowTicks(r.base()));
            SkillEffects.run(r.base(), p);
        } else {
            BookSkillEffects.run(r.book(), p, lvl);
        }
        int tbCd = r.base() == Skill.PHAP_2 ? com.hukhong.phaptu.ThiemBoService.cooldownOverride(p, page, slot) : -1;
        if (tbCd < 0 && r.base() == Skill.PHAP_ULT) tbCd = com.hukhong.phaptu.ThoService.cooldownOverride(p, page, slot); // Thiên Sơn: 45s
        st.cooldownEnd[slot] = now + (tbCd >= 0 ? tbCd : Math.round(SkillBookMath.cooldown(r.baseCooldown(), lvl) * com.hukhong.phaptu.PhapRuntime.cooldownMult(p, page)
                * com.hukhong.phaptu.QuangLoiFlow.cdMult(p, page, slot)));
        st.lastCombat = now;
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        SkillState st = state(p);
        long now = p.level().getGameTime();
        Path page = Skill.skillPath(p.getData(ModAttachments.CULTIVATION), st.page);
        SkillBookData bd = SkillBookService.data(p);
        int plv = p.getData(ModAttachments.CULTIVATION).level();
        List<Integer> rem = new ArrayList<>(4), tot = new ArrayList<>(4);
        boolean lockT = page == Path.PHAP && com.hukhong.phaptu.beam.PhapBeamService.locksT(p);
        for (int i = 0; i < 4; i++) {
            if (page == Path.TRAN) { // Trận Tu: hồi chiêu theo thế hiện tại
                rem.add((int) Math.max(0, st.cooldownEnd[i] - now));
                tot.add(Math.max(1, com.hukhong.tran.TranStance.skillOf(com.hukhong.tran.TranRuntime.stance(p), i).cooldownTicks));
                continue;
            }
            SkillBook.Resolved r = SkillBook.resolve(bd, page, i, plv, page == Path.PHAP ? com.hukhong.phaptu.ThoService.lockMask(p) : 0);
            rem.add((int) Math.max(0, st.cooldownEnd[i] - now));
            tot.add(i == 2 && r == null ? Math.max(1, st.rTotal) : r == null ? 1
                    : r.base() == Skill.KIEM_ULT ? SkillEffects.DOMAIN_LOCK_TICKS
                    : r.base() == Skill.PHAP_ULT && com.hukhong.phaptu.ThoService.cooldownOverride(p, page, i) >= 0 ? com.hukhong.phaptu.ThoService.cooldownOverride(p, page, i)
                    : r.base() == Skill.PHAP_2 && com.hukhong.phaptu.ThiemBoService.cooldownOverride(p, page, i) >= 0 ? com.hukhong.phaptu.ThiemBoService.cooldownOverride(p, page, i)
                    : (int) Math.round(SkillBookMath.cooldown(r.baseCooldown(), bd.level(r.id())) * com.hukhong.phaptu.PhapRuntime.cooldownMult(p, page)));
        }
        PacketDistributor.sendToPlayer(p, new SkillSyncPayload(st.stance, rem, tot, st.page, st.domainOn));
        if (page == Path.TRAN) com.hukhong.tran.TranRuntime.sendSync(p);
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            SkillState st = state(p);
            if (st.domainOn) SkillEffects.kiemVucTick(p, st);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) state(p).domainOn = false;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }
}
