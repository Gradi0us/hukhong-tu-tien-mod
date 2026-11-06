package com.hukhong.story.kyngo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Một Kỳ Ngộ đang diễn ra: token, người chơi, vị trí, thực thể đã sinh và khối đã đặt (để khôi phục). Chỉ dùng trên server. */
public final class KyNgoInstance {
    public final String token;
    public final KyNgoType type;
    public final ServerPlayer owner;
    public final ServerLevel level;
    public final BlockPos origin;
    public final long startTick;
    /** Mọi thực thể cần dọn khi kết thúc (đã gắn tag). */
    public final List<UUID> entities = new ArrayList<>();
    /** Quái canh giữ (Động Phủ): hết chết thì mở rương thưởng. */
    public final List<UUID> guards = new ArrayList<>();
    /** Chỉ các ô từng là không khí và do ta đặt; dọn thì trả về không khí nếu vẫn đúng khối đó. */
    public final Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
    /** Thực thể chính: Lão Nhân / Linh Thú / Thi Thể (armor stand). */
    public UUID main;
    public boolean rewarded;
    /** Đã có lựa chọn: không nhận lựa chọn nữa, chờ hết hạn để dọn. */
    public boolean resolved;

    KyNgoInstance(String token, KyNgoType type, ServerPlayer owner, ServerLevel level, BlockPos origin, long startTick) {
        this.token = token;
        this.type = type;
        this.owner = owner;
        this.level = level;
        this.origin = origin;
        this.startTick = startTick;
    }
}
