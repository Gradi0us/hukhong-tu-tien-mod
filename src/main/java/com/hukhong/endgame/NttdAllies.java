package com.hukhong.endgame;

import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Mộ Dao trợ chiến pha 4 trận Nguyên Thủy Tịch Diệt ({@link MoDaoCombat} bất tử, gắn MO_TAG, tự đánh theo moveset riêng). */
public final class NttdAllies {
    private NttdAllies() {}

    public static final String MO_TAG = "hk_nttd_mo_ally";
    public static final int MO_COLOR = 0x9FE8FF;

    public static StoryNpc findMo(ServerLevel sl, Vec3 around) {
        for (StoryNpc n : sl.getEntitiesOfClass(StoryNpc.class, new AABB(around, around).inflate(160), e -> e.isAlive() && e.getTags().contains(MO_TAG)))
            return n;
        return null;
    }

    /** Sinh Mộ Dao chiến đấu ({@link MoDaoCombat}, hoặc mang tên Tàn Hồn Mộ Dao nếu người chơi quanh đây đã chứng kiến nàng ngã xuống); có rồi thì chỉ dời chỗ. */
    public static StoryNpc spawnMo(ServerLevel sl, Vec3 at, Vec3 face) {
        StoryNpc mo = findMo(sl, at);
        if (mo != null && !(mo instanceof MoDaoCombat)) { mo.discard(); mo = null; } // bản StoryNpc cũ (save trước rework) -> thay
        if (mo == null) {
            MoDaoCombat c = MoDaoCombat.create(sl, at, MoDaoCombat.yawTo(at, face), false);
            if (c == null) return null;
            c.addTag(MO_TAG);
            boolean soul = false;
            for (ServerPlayer p : sl.players())
                if (p.distanceToSqr(at) < 160 * 160 && DialogueService.quest(p).has("mo_dao_died")) soul = true;
            if (soul) c.setCustomName(Component.translatable("hukhong.npc.mo_dao_soul"));
            mo = c;
        }
        place(mo, at, face);
        sl.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1.2, at.z, 60, 0.6, 1.2, 0.6, 0.4);
        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 30, 0.5, 1.0, 0.5, 0.05);
        Fx.groundRing(sl, null, at, 3f, MO_COLOR, 30);
        return mo;
    }

    public static void place(StoryNpc mo, Vec3 at, Vec3 face) {
        Vec3 d = face.subtract(at);
        float yaw = (float) (Math.atan2(-d.x, d.z) * 180 / Math.PI);
        mo.moveTo(at.x, at.y, at.z, yaw, 0f);
        mo.setYBodyRot(yaw);
        mo.setYHeadRot(yaw);
        mo.setDeltaMovement(Vec3.ZERO);
    }

    public static void dismissMo(ServerLevel sl, Vec3 around) {
        for (StoryNpc mo : sl.getEntitiesOfClass(StoryNpc.class, new AABB(around, around).inflate(160), e -> e.getTags().contains(MO_TAG))) {
            sl.sendParticles(ParticleTypes.END_ROD, mo.getX(), mo.getY() + 1.2, mo.getZ(), 40, 0.5, 1.2, 0.5, 0.08);
            mo.discard();
        }
    }

    /**
     * Giám sát Mộ Dao pha 4 (NTTD gọi mỗi 3.5s). Chiêu thức do máy trạng thái của {@link MoDaoCombat} tự lo;
     * ở đây chỉ trao mục tiêu và kéo nàng về nếu lạc xa (> 40 block).
     */
    static void moTick(ServerLevel sl, NguyenThuyTichDiet boss, StoryNpc mo, int beat) {
        if (!(mo instanceof MoDaoCombat c)) return;
        c.setScripted(false);
        c.setTargetHint(boss);
        if (c.distanceTo(boss) <= 40) return;
        Vec3 bp = boss.position();
        Vec3 cen = Vec3.ZERO;
        int n = 0;
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(boss) < 50 * 50) { cen = cen.add(p.position()); n++; }
        Vec3 dir = n == 0 ? new Vec3(1, 0, 0) : cen.scale(1.0 / n).subtract(bp).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-3) dir = new Vec3(1, 0, 0);
        Vec3 to = bp.add(dir.normalize().scale(9));
        to = new Vec3(to.x, c.groundAt(to.x, bp.y + 2, to.z), to.z);
        sl.sendParticles(ParticleTypes.END_ROD, c.getX(), c.getY() + 1, c.getZ(), 20, 0.4, 1, 0.4, 0.05);
        place(c, to, bp);
        Fx.groundRing(sl, null, to, 2f, MO_COLOR, 20);
    }

    static void say(ServerLevel sl, net.minecraft.world.entity.Entity near, String key) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(near) < 100 * 100) p.displayClientMessage(Component.translatable(key), false);
    }
}
