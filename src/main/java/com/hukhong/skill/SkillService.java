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
        st.stance = !st.stance;
        p.displayClientMessage(Component.translatable(st.stance ? "hukhong.stance.on" : "hukhong.stance.off"), true);
        sync(p);
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
        st.lastSwitch = now;
        for (int i = 0; i < 4; i++) st.cooldownEnd[i] = Math.max(st.cooldownEnd[i], now + SkillPages.SWITCH_COOLDOWN_TICKS);
        p.displayClientMessage(Component.translatable("hukhong.msg.page_switched",
                Component.translatable(SkillPages.pathLangKey(SkillPages.active(d, st.page)))), true);
        sync(p);
    }

    public static void cast(ServerPlayer p, int slot) {
        if (slot < 0 || slot > 3 || !p.isAlive() || p.isSpectator()) return;
        SkillState st = state(p);
        if (!st.stance) return; // server chỉ nhận cast khi thế bật
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        Path page = Skill.skillPath(d, st.page);
        SkillBookData bd = SkillBookService.data(p);
        SkillBook.Resolved r = SkillBook.resolve(bd, page, slot, d.level());
        // phím R: Bản Mệnh / Vạn Cốt Cộng Minh, trừ khi có kỹ năng gán thủ công và chưa mở Bản Mệnh
        if (slot == 2 && r == null) {
            SkillBookRuntime.clear(p);
            BanMenhSkillEffects.castR(p, st);
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
        if (!Resources.tryConsume(p, r.cost(), com.hukhong.banmenh.BanMenhService.skillCostFactor(p))) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
            return;
        }
        if (r.base() != null) {
            SkillBookRuntime.begin(p, SkillBookMath.dmgMult(lvl), SkillBookRuntime.windowTicks(r.base()));
            SkillEffects.run(r.base(), p);
        } else {
            BookSkillEffects.run(r.book(), p, lvl);
        }
        st.cooldownEnd[slot] = now + SkillBookMath.cooldown(r.baseCooldown(), lvl);
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
        for (int i = 0; i < 4; i++) {
            SkillBook.Resolved r = SkillBook.resolve(bd, page, i, plv);
            rem.add((int) Math.max(0, st.cooldownEnd[i] - now));
            tot.add(i == 2 && r == null ? Math.max(1, st.rTotal) : r == null ? 1
                    : SkillBookMath.cooldown(r.baseCooldown(), bd.level(r.id())));
        }
        PacketDistributor.sendToPlayer(p, new SkillSyncPayload(st.stance, rem, tot, st.page));
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
