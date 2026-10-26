package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.gates.Resources;
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
        if (Skill.skillPath(d) == null) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_skill_path"), true);
            return;
        }
        st.stance = !st.stance;
        p.displayClientMessage(Component.translatable(st.stance ? "hukhong.stance.on" : "hukhong.stance.off"), true);
        sync(p);
    }

    public static void cast(ServerPlayer p, int slot) {
        if (slot < 0 || slot > 3 || !p.isAlive() || p.isSpectator()) return;
        SkillState st = state(p);
        if (!st.stance) return; // server chỉ nhận cast khi thế bật
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        Skill s = Skill.of(Skill.skillPath(d), slot);
        if (s == null) return;
        if (com.hukhong.pathrules.MaRules.isLost(p)) { // Mất Kiểm Soát: khóa phím kỹ năng
            p.displayClientMessage(Component.translatable("hukhong.ma.lost_control"), true);
            return;
        }
        long now = p.level().getGameTime();
        if (st.cooldownEnd[slot] > now) return;
        if (!Resources.tryConsume(p, s.cost)) {
            p.displayClientMessage(Component.translatable("hukhong.msg.no_resource"), true);
            return;
        }
        SkillEffects.run(s, p);
        st.cooldownEnd[slot] = now + s.cooldownTicks;
        st.lastCombat = now;
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        SkillState st = state(p);
        long now = p.level().getGameTime();
        List<Integer> rem = new ArrayList<>(4), tot = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            Skill s = Skill.of(Skill.skillPath(p.getData(ModAttachments.CULTIVATION)), i);
            rem.add((int) Math.max(0, st.cooldownEnd[i] - now));
            tot.add(s == null ? 1 : s.cooldownTicks);
        }
        PacketDistributor.sendToPlayer(p, new SkillSyncPayload(st.stance, rem, tot));
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
