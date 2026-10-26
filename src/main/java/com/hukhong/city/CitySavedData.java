package com.hukhong.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Trạng thái thành Lưu Ly Cảng (lưu theo thế giới, Overworld). */
public class CitySavedData extends SavedData {
    public enum State { NORMAL, RUIN }

    private static final String NAME = "hukhong_city";

    private boolean built;
    private State state = State.NORMAL;
    private BlockPos center = BlockPos.ZERO; // y = mặt nền; Ấn Hồn Đài ở center.above()
    private final Set<UUID> residents = new HashSet<>();
    private int invasionsWon;

    public static CitySavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CitySavedData::new, CitySavedData::load, null), NAME);
    }

    public static CitySavedData load(CompoundTag tag, HolderLookup.Provider reg) {
        CitySavedData d = new CitySavedData();
        d.built = tag.getBoolean("built");
        d.state = "RUIN".equals(tag.getString("state")) ? State.RUIN : State.NORMAL;
        d.center = new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz"));
        d.invasionsWon = tag.getInt("won");
        for (Tag t : tag.getList("residents", Tag.TAG_INT_ARRAY)) d.residents.add(NbtUtils.loadUUID(t));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putBoolean("built", built);
        tag.putString("state", state.name());
        tag.putInt("cx", center.getX());
        tag.putInt("cy", center.getY());
        tag.putInt("cz", center.getZ());
        tag.putInt("won", invasionsWon);
        ListTag l = new ListTag();
        for (UUID u : residents) l.add(NbtUtils.createUUID(u));
        tag.put("residents", l);
        return tag;
    }

    public boolean isBuilt() { return built; }
    public State state() { return state; }
    public BlockPos center() { return center; }
    public BlockPos altarPos() { return center.above(); }
    public int invasionsWon() { return invasionsWon; }
    public boolean isResident(UUID u) { return residents.contains(u); }
    public int residentCount() { return residents.size(); }

    public void markBuilt(BlockPos c) { built = true; center = c; state = State.NORMAL; setDirty(); }
    public void setState(State s) { state = s; setDirty(); }
    public void addWin() { invasionsWon++; setDirty(); }

    /** @return true nếu mới đăng ký. */
    public boolean addResident(UUID u) {
        boolean r = residents.add(u);
        if (r) setDirty();
        return r;
    }

    /** Hộp thành (XZ) để kiểm tra người chơi bước vào. */
    public boolean inside(double x, double z) {
        return built && Math.abs(x - center.getX()) <= CityBuilder.HALF + 1 && Math.abs(z - center.getZ()) <= CityBuilder.HALF + 1;
    }
}
