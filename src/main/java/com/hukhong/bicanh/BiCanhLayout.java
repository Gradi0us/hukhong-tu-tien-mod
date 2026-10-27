package com.hukhong.bicanh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Chuỗi phòng của một instance (toạ độ tương đối so với origin; thuần Java). */
public final class BiCanhLayout {
    private BiCanhLayout() {}

    public enum Kind { ENTRY, WAVE, BOSS }

    public static final int ROOM_LEN = 25, HALF_Z = 11, HEIGHT = 10, CORRIDOR = 7, PITCH = ROOM_LEN + CORRIDOR;
    /** Khoảng cách X giữa 2 slot instance (đủ cho 7 phòng + dư). */
    public static final int SLOT_SPACING = 256;

    /** Loai bo cuc phong (0 vong nghi le, 1 hanh lang tru, 2 dai cao, 3 ho goc, 4 hom ben). Phong boss luon la dai cao (2). */
    public static int layoutOf(long seed, Room r) {
        if (r.kind() == Kind.BOSS) return 2;
        if (r.kind() == Kind.ENTRY) return 0;
        return new Random(seed * 1_000_003L + r.index() * 7919L).nextInt(5);
    }

    public record Room(int index, Kind kind, int x0) {}

    public static List<Room> rooms(BiCanhTier t) {
        List<Room> l = new ArrayList<>();
        l.add(new Room(0, Kind.ENTRY, 0));
        for (int i = 1; i <= t.waves; i++) l.add(new Room(i, Kind.WAVE, i * PITCH));
        l.add(new Room(t.waves + 1, Kind.BOSS, (t.waves + 1) * PITCH));
        return l;
    }

    /** X tương đối của khối tường cuối (đông) của chuỗi. */
    public static int endX(List<Room> rooms) { return rooms.get(rooms.size() - 1).x0() + ROOM_LEN; }

    /** Phòng chứa relX (vùng trong x0+1..x0+ROOM_LEN-2), -1 nếu không. */
    public static int roomAt(List<Room> rooms, int relX) {
        for (Room r : rooms) if (relX >= r.x0() + 1 && relX <= r.x0() + ROOM_LEN - 2) return r.index();
        return -1;
    }

    /** Cột trụ trong phòng: [relX, relZ]; tránh hàng cửa |z| &lt;= 1 và tâm phòng boss. */
    public static List<int[]> pillars(long seed, Room room) {
        List<int[]> out = new ArrayList<>();
        if (room.kind() == Kind.ENTRY) return out;
        Random r = new Random(seed * 31 + room.index() * 7919L);
        Set<Integer> used = new HashSet<>();
        for (int n = 0; n < 5; n++) {
            int x = room.x0() + 3 + r.nextInt(ROOM_LEN - 7);
            int z = -HALF_Z + 1 + r.nextInt(2 * HALF_Z - 1);
            if (Math.abs(z) <= 1) continue;
            if (room.kind() == Kind.BOSS && x >= room.x0() + ROOM_LEN / 2 - 4 && x <= room.x0() + ROOM_LEN / 2 + 4 && Math.abs(z) <= 5) continue;
            if (used.add(x * 100 + z)) out.add(new int[]{x, z});
        }
        return out;
    }
}
