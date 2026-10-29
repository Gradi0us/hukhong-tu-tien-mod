package com.hukhong.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Di chuyen NPC ho tong theo tuyen da tinh san (waypoint cach nhau ~STEP block), toc do ~di bo cua nguoi choi,
 * dung cho khi nguoi choi tut qua WAIT_DIST, khong bao gio di lang thang. Tinh moi tick (setPos + bam mat dat), AI cua NPC tat.
 */
public final class EscortMover {
    public static final double SPEED = 0.215;      // block/tick ~ 4.3 block/s = toc do di bo cua nguoi choi
    public static final double WAIT_DIST = 12.0;   // xa hon -> dung doi
    public static final double RESUME_DIST = 8.0;  // gan hon -> di tiep (tranh nhap nhay)
    public static final int STEP = 4;              // khoang cach waypoint
    public static final double ARRIVE = 2.5;

    /** Tuyen: danh sach waypoint (x,z) tu diem xuat phat toi dich, doan cuoi = dich. Thuan, kiem thu duoc. */
    public static List<int[]> plan(int sx, int sz, int dx, int dz) {
        List<int[]> pts = new ArrayList<>();
        double len = Math.hypot(dx - sx, dz - sz);
        int n = Math.max(1, (int) Math.ceil(len / STEP));
        for (int i = 1; i < n; i++) pts.add(new int[]{(int) Math.round(sx + (dx - sx) * (double) i / n), (int) Math.round(sz + (dz - sz) * (double) i / n)});
        pts.add(new int[]{dx, dz});
        return pts;
    }

    /** Hysteresis cho viec doi nguoi choi: dang doi thi chi di tiep khi nguoi choi lai gan, dang di thi dung khi qua xa. */
    public static boolean shouldWait(boolean waiting, double playerDist) {
        return waiting ? playerDist > RESUME_DIST : playerDist > WAIT_DIST;
    }

    /** Trang thai di chuyen cua mot escort. */
    public static final class Walker {
        private final List<int[]> route;
        private int idx;
        private boolean waiting;
        private final BlockPos dest;
        public boolean halted; // dung ngoai y (vd phuc kich)

        public Walker(ServerLevel l, Vec3 from, BlockPos dest) {
            this.dest = dest;
            this.route = plan((int) Math.floor(from.x), (int) Math.floor(from.z), dest.getX(), dest.getZ());
        }

        public boolean waiting() { return waiting; }
        public int index() { return idx; }
        public int size() { return route.size(); }
        public double remaining() { return route.size() - idx; }

        /** Goi moi tick. Tra true khi da toi dich. p co the null (khong ai de doi). */
        public boolean tick(Mob e, ServerPlayer p) {
            if (!(e.level() instanceof ServerLevel l)) return false;
            e.setNoAi(true); // chan hanh vi mac dinh (dan lang di bo bua)
            e.getNavigation().stop();
            double dxd = e.getX() - (dest.getX() + 0.5), dzd = e.getZ() - (dest.getZ() + 0.5);
            if (dxd * dxd + dzd * dzd < ARRIVE * ARRIVE || idx >= route.size()) return true;
            if (halted) return false;
            if (p != null) waiting = shouldWait(waiting, Math.sqrt(p.distanceToSqr(e)));
            if (waiting) { face(e, p.getX(), p.getZ()); return false; }
            int[] w = route.get(idx);
            double tx = w[0] + 0.5, tz = w[1] + 0.5;
            double dx = tx - e.getX(), dz = tz - e.getZ(), d = Math.hypot(dx, dz);
            if (d <= SPEED * 1.5) { idx++; return false; }
            double nx = e.getX() + dx / d * SPEED, nz = e.getZ() + dz / d * SPEED;
            int bx = (int) Math.floor(nx), bz = (int) Math.floor(nz);
            if (!l.hasChunk(bx >> 4, bz >> 4)) return false; // chunk chua nap: doi
            double ny = groundY(l, bx, bz, e.getY());
            e.setPos(nx, ny, nz);
            e.setYRot((float) (Math.toDegrees(Math.atan2(-dx, dz))));
            e.setYHeadRot(e.getYRot());
            e.yBodyRot = e.getYRot();
            e.setOnGround(true);
            e.fallDistance = 0;
            e.walkAnimation.update(0.9f, 0.4f);
            return false;
        }

        private static void face(Mob e, double x, double z) {
            float yaw = (float) Math.toDegrees(Math.atan2(-(x - e.getX()), z - e.getZ()));
            e.setYRot(yaw); e.setYHeadRot(yaw);
            e.walkAnimation.update(0f, 0.4f);
        }
    }

    /** Be mat di bo tai cot (bx,bz): mat dat cao nhat khong tinh la (mat nuoc = di tren mat nuoc, tranh chim chet duoi); bo qua neu lech > 24 so voi cur. */
    static double groundY(ServerLevel l, int bx, int bz, double cur) {
        int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return Math.abs(top - cur) > 24 ? cur : top;
    }
}
