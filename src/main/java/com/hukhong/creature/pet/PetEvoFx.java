package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.hukhong.creature.LinhThu;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Canh tien hoa (5 giay, phia server, nhieu goi Fx): ken anh sang bao quanh thu, vong phap trien moc len, cot sang mau nhanh,
 * song chan, rung man hinh, chu "Tien Hoa: <form>". Chay theo tick qua ServerTickEvent.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PetEvoFx {
    private PetEvoFx() {}

    static final int DURATION = 100;
    private static final List<Run> RUNS = new ArrayList<>();

    private static final class Run {
        final ServerPlayer owner; final UUID pet; final PetBranch branch; final int stage; final int rgb;
        Vec3 at; int t;
        Run(ServerPlayer owner, UUID pet, PetBranch branch, int stage, Vec3 at) {
            this.owner = owner; this.pet = pet; this.branch = branch; this.stage = stage; this.rgb = branch.rgb; this.at = at;
        }
    }

    public static void start(ServerPlayer owner, LinhThu pet, PetBranch b, int stage) {
        Run r = new Run(owner, pet.getUUID(), b, stage, pet.position());
        RUNS.add(r);
        step(r);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (RUNS.isEmpty()) return;
        Iterator<Run> it = RUNS.iterator();
        while (it.hasNext()) {
            Run r = it.next();
            if (r.owner.isRemoved()) { it.remove(); continue; }
            r.t++;
            step(r);
            if (r.t >= DURATION) it.remove();
        }
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), size);
    }

    private static void step(Run r) {
        ServerLevel lv = r.owner.serverLevel();
        Entity e = lv.getEntity(r.pet);
        if (e != null) r.at = e.position();
        Vec3 c = r.at;
        int t = r.t, rgb = r.rgb;
        if (t == 0) {
            lv.playSound(null, BlockPos.containing(c), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.9f);
            Fx.rune(lv, r.owner, c.add(0, 0.05, 0), 3.2f, rgb, DURATION, 6);
            Fx.play(lv, FxType.LIGHT_PILLAR, c, Vec3.ZERO, 2.2f, rgb, 60);
            Fx.shake(lv, c, 0.35f, 30, 10f);
        }
        if (t <= 40 && t % 2 == 0) cocoon(lv, c, t, rgb);
        if (t == 20 || t == 60) {
            Fx.groundRing(lv, r.owner, c.add(0, 0.05, 0), t == 20 ? 4.5f : 7f, rgb, 30);
            Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.1, 0), Vec3.ZERO, t == 20 ? 3.5f : 6f, rgb, 20);
        }
        if (t >= 20 && t <= 60 && t % 10 == 0) Fx.rune(lv, r.owner, c.add(0, 0.6 + (t - 20) * 0.05, 0), 2.6f, rgb, 30, 5);
        if (t == 40) {
            lv.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
            lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 1, c.z, 60, 0.6, 0.9, 0.6, 0.5);
            Fx.shake(lv, c, 1.0f, 25, 14f);
            lv.playSound(null, BlockPos.containing(c), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2f, 0.8f);
            title(r.owner, r.branch, r.stage);
        }
        if (t >= 40 && t % 3 == 0) ascend(lv, c, t, rgb);
        if (t == 60) lv.playSound(null, BlockPos.containing(c), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9f, 1.2f);
    }

    /** Kén ánh sáng: 12 hạt xoay quanh thú, bán kính co dần. */
    private static void cocoon(ServerLevel lv, Vec3 c, int t, int rgb) {
        double rad = 2.4 - t * 0.035;
        DustParticleOptions d = dust(rgb, 1.1f);
        for (int i = 0; i < 12; i++) {
            double a = t * 0.25 + i * Math.PI * 2 / 12;
            double y = c.y + 0.9 + Math.sin(a * 0.5 + i) * 0.8;
            lv.sendParticles(d, c.x + Math.cos(a) * rad, y, c.z + Math.sin(a) * rad, 1, 0, 0, 0, 0);
        }
        lv.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 3, 0.3, 0.6, 0.3, 0.02);
    }

    /** Cột hạt bay lên sau khi thú thoát kén. */
    private static void ascend(ServerLevel lv, Vec3 c, int t, int rgb) {
        double y = c.y + (t - 40) * 0.06;
        for (int k = 0; k < 4; k++) {
            double a = t * 0.2 + k * Math.PI / 2;
            lv.sendParticles(dust(rgb, 1.3f), c.x + Math.cos(a) * 0.8, y, c.z + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
        }
    }

    private static void title(ServerPlayer p, PetBranch b, int stage) {
        if (p.connection == null) return;
        try {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Tx.t("hukhong.pet.evo.sub", "%s · tầng %s", "%s · stage %s", b.displayName(), stage).withColor(0xE0E0E0)));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Tx.t("hukhong.pet.evo.title", "Tiến Hóa: %s", "Evolution: %s", b.formName(stage)).withColor(b.rgb)));
        } catch (RuntimeException ignored) { }
    }
}
