package com.hukhong.siege;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Khe nut Hu Khong: dia xoay doc (nhieu canh xoan oc tim + vanh lua xanh) mo ra tren mat dat, nut dat toa ra xung quanh.
 * Chi la hieu ung: khong dat/pha block. Quai xuat hien tai {@link #spawnPoint}. Do {@link SiegeDirector} tick.
 */
public final class Rift {
    public static final int OPEN_TICKS = 30, READY_TICKS = 40, CLOSE_TICKS = 20;

    final ServerLevel level;
    final Vec3 base;
    final double radius;
    private final double tx, tz;      // tiep tuyen nam ngang cua mat dia
    private final double[][][] cracks; // [vet][diem] = {dx, dz}
    int age, closeAge = -1;

    Rift(ServerLevel level, Vec3 base, Vec3 facing, double radius) {
        this.level = level;
        this.base = base;
        this.radius = radius;
        Vec3 f = new Vec3(facing.x, 0, facing.z);
        f = f.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : f.normalize();
        tx = -f.z;
        tz = f.x;
        var rnd = level.random;
        cracks = new double[7][4][2];
        for (int i = 0; i < cracks.length; i++) {
            double a = i * Math.PI * 2 / cracks.length + rnd.nextDouble() * 0.5;
            double len = 3 + rnd.nextDouble() * 3.5;
            double cx = 0, cz = 0;
            for (int k = 0; k < 4; k++) {
                a += (rnd.nextDouble() - 0.5) * 0.7; // gap khuc nhu vet nut
                cx += Math.cos(a) * len / 4;
                cz += Math.sin(a) * len / 4;
                cracks[i][k][0] = cx;
                cracks[i][k][1] = cz;
            }
        }
        // am thanh "nhac hieu" ky la khi khe nut mo
        Vec3 c = center();
        SiegeFx.sound(level, c, SoundEvents.PORTAL_TRIGGER, 0.55f, 0.55f);
        SiegeFx.sound(level, c, SoundEvents.WARDEN_NEARBY_CLOSER, 0.9f, 0.7f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, net.minecraft.sounds.SoundSource.HOSTILE, 0.5f, 0.55f);
    }

    public Vec3 center() { return base.add(0, radius * 1.1 + 0.2, 0); }
    public boolean ready() { return closeAge < 0 && age >= READY_TICKS; }
    public boolean closing() { return closeAge >= 0; }
    public void close() { if (closeAge < 0) closeAge = 0; }

    /** Diem quai tro ra: quanh chan khe nut. */
    public Vec3 spawnPoint() {
        var r = level.random;
        double a = r.nextDouble() * Math.PI * 2, d = 0.8 + r.nextDouble() * 2.2;
        return new Vec3(base.x + Math.cos(a) * d, base.y, base.z + Math.sin(a) * d);
    }

    /** @return true khi da khep han. */
    boolean tick() {
        age++;
        double s = Math.min(1.0, age / (double) OPEN_TICKS);
        s = 1 - (1 - s) * (1 - s);
        if (closeAge >= 0) {
            if (closeAge == 0) {
                Vec3 c = center();
                SiegeFx.sound(level, c, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.9f, 0.7f);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 30, 0.3, 0.5, 0.3, 0.25);
            }
            closeAge++;
            s *= Math.max(0, 1 - closeAge / (double) CLOSE_TICKS);
            if (closeAge >= CLOSE_TICKS) {
                Vec3 c = center();
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 14, 0.2, 0.4, 0.2, 0.12);
                return true;
            }
        }
        if (s <= 0.02) return false;
        Vec3 c = center();
        double R = radius * s;
        long t = age;
        if ((t & 1) == 0) {
            // 3 canh xoan oc, quay cham, hut vao tam
            for (int arm = 0; arm < 3; arm++) {
                for (int k = 0; k < 4; k++) {
                    double rr = R * (0.2 + 0.8 * (k + 1) / 4.0);
                    double a = t * 0.16 + arm * Math.PI * 2 / 3 + k * 0.85;
                    put(ParticleTypes.REVERSE_PORTAL, c, a, rr, 0);
                }
            }
            // vanh vien lua ma troi
            for (int i = 0; i < 8; i++) put(ParticleTypes.SOUL_FIRE_FLAME, c, -t * 0.07 + i * Math.PI / 4, R, 0.02);
            level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 6, R * 0.3, R * 0.3, R * 0.3, 0.6);
        }
        if (t % 5 == 0) put(SiegeFx.VOID_DUST, c, level.random.nextDouble() * 6.283, R * (0.4 + level.random.nextDouble() * 0.6), 0);
        // vet nut dat: moc dan ra trong ~30 tick, ve lai moi 6 tick
        if (t % 6 == 0) {
            int shown = (int) Math.ceil(4 * s);
            for (double[][] cr : cracks)
                for (int k = 0; k < shown; k++)
                    level.sendParticles(SiegeFx.VOID_DUST, base.x + cr[k][0] * s, base.y + 0.12, base.z + cr[k][1] * s, 1, 0.04, 0, 0.04, 0);
            if (t % 12 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, base.x, base.y + 0.2, base.z, 3, 1.2, 0.05, 1.2, 0.1);
        }
        if (t % 50 == 0 && closeAge < 0) SiegeFx.ambientSound(level, c, SoundEvents.PORTAL_AMBIENT, 0.5f, 0.6f);
        return false;
    }

    private void put(net.minecraft.core.particles.ParticleOptions o, Vec3 c, double a, double rr, double speed) {
        double u = Math.cos(a) * rr, v = Math.sin(a) * rr * 1.2;
        level.sendParticles(o, c.x + tx * u, c.y + v, c.z + tz * u, 1, 0, 0, 0, speed);
    }
}
