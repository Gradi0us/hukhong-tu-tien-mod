package com.hukhong.endgame;

import com.hukhong.story.flow.RequiredNpcs;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.vfx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Mộ Dao trợ chiến pha 4 trận Nguyên Thủy Tịch Diệt (StoryNpc bất tử, gắn MO_TAG, AI tắt; NTTD điều khiển qua tick()). */
public final class NttdAllies {
    private NttdAllies() {}

    public static final String MO_TAG = "hk_nttd_mo_ally";
    public static final int MO_COLOR = 0x9FE8FF;

    public static StoryNpc findMo(ServerLevel sl, Vec3 around) {
        for (StoryNpc n : sl.getEntitiesOfClass(StoryNpc.class, new AABB(around, around).inflate(160), e -> e.isAlive() && e.getTags().contains(MO_TAG)))
            return n;
        return null;
    }

    /** Sinh Mộ Dao (hoặc Tàn Hồn Mộ Dao nếu người chơi quanh đây đã chứng kiến nàng ngã xuống); có rồi thì chỉ dời chỗ. */
    public static StoryNpc spawnMo(ServerLevel sl, Vec3 at, Vec3 face) {
        StoryNpc mo = findMo(sl, at);
        if (mo == null) {
            mo = RequiredNpcs.create(sl, "mo_dao", BlockPos.containing(at));
            if (mo == null) return null;
            mo.addTag(MO_TAG);
            mo.setNoAi(true);
            boolean soul = false;
            for (ServerPlayer p : sl.players())
                if (p.distanceToSqr(at) < 160 * 160 && DialogueService.quest(p).has("mo_dao_died")) soul = true;
            if (soul) mo.setCustomName(Component.translatable("hukhong.npc.mo_dao_soul"));
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

    /** Một nhịp Mộ Dao trong pha 4: giữ vị trí, kiếm khí đánh boss (0.4% máu tối đa) hoặc hộ thể / hồi máu người chơi. */
    static void moTick(ServerLevel sl, NguyenThuyTichDiet boss, StoryNpc mo, int beat) {
        Vec3 bp = boss.position();
        double dist = mo.distanceTo(boss);
        if (dist > 22 || dist < 6) {
            Vec3 c = Vec3.ZERO;
            int n = 0;
            for (ServerPlayer p : sl.players()) if (p.distanceToSqr(boss) < 50 * 50) { c = c.add(p.position()); n++; }
            Vec3 dir = n == 0 ? new Vec3(1, 0, 0) : c.scale(1.0 / n).subtract(bp).multiply(1, 0, 1);
            if (dir.lengthSqr() < 1e-3) dir = new Vec3(1, 0, 0);
            Vec3 to = bp.add(dir.normalize().scale(11));
            BlockPos g = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(to));
            to = new Vec3(to.x, Math.min(g.getY(), bp.y + 6), to.z);
            sl.sendParticles(ParticleTypes.END_ROD, mo.getX(), mo.getY() + 1, mo.getZ(), 20, 0.4, 1, 0.4, 0.05);
            place(mo, to, bp);
        } else place(mo, mo.position(), bp);
        if (beat % 4 == 3) { // Thanh Tâm Hộ Thể: hồi máu + khiên
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(40), q -> !q.isSpectator())) {
                p.heal(p.getMaxHealth() * 0.12f);
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 2));
                Fx.shell(sl, null, p, 1.2f, MO_COLOR, 40);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 10, 0.5, 0.8, 0.5, 0.05);
            }
            Fx.rune(sl, null, mo.position().add(0, 0.1, 0), 3f, MO_COLOR, 40, 6);
            sl.playSound(null, mo.getX(), mo.getY(), mo.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 3f, 1.3f);
            if (beat % 8 == 7) say(sl, boss, "hukhong.endgame.modao.heal");
        } else { // Thanh Minh kiếm khí
            Vec3 a = mo.position().add(0, 1.4, 0), b = bp.add(0, boss.getBbHeight() * 0.5, 0);
            Fx.ribbon(sl, null, a, b, 1.2, 8, 0, 0.3f, MO_COLOR);
            Fx.qi(sl, a, b.subtract(a), 1.6f, MO_COLOR, 20, 10);
            boss.hurt(sl.damageSources().mobAttack(mo), boss.getMaxHealth() * 0.004f);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, b.x, b.y, b.z, 3, 0.6, 0.6, 0.6, 0);
            sl.playSound(null, mo.getX(), mo.getY(), mo.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.5f, 1.2f);
        }
    }

    static void say(ServerLevel sl, net.minecraft.world.entity.Entity near, String key) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(near) < 100 * 100) p.displayClientMessage(Component.translatable(key), false);
    }
}
