package com.hukhong.honcot;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 6 ô Hồn Cốt đã mang (ItemStack, rỗng = trống). Bất biến: đổi qua with(). */
public record BoneSlots(List<ItemStack> items) {
    public static final BoneSlots EMPTY = new BoneSlots(blank());

    public static final Codec<BoneSlots> CODEC = ItemStack.OPTIONAL_CODEC.listOf().xmap(BoneSlots::normalize, BoneSlots::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, BoneSlots> STREAM_CODEC =
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(BoneSlots::normalize, BoneSlots::items);

    private static List<ItemStack> blank() {
        List<ItemStack> l = new ArrayList<>(BoneSlot.COUNT);
        for (int i = 0; i < BoneSlot.COUNT; i++) l.add(ItemStack.EMPTY);
        return l;
    }

    /** Luôn đủ 6 phần tử. */
    private static BoneSlots normalize(List<ItemStack> in) {
        List<ItemStack> l = blank();
        for (int i = 0; i < Math.min(in.size(), BoneSlot.COUNT); i++) l.set(i, in.get(i).copy());
        return new BoneSlots(l);
    }

    public ItemStack get(BoneSlot s) { return items.get(s.ordinal()); }

    public BoneSlots with(BoneSlot s, ItemStack st) {
        List<ItemStack> l = new ArrayList<>(items);
        l.set(s.ordinal(), st.copy());
        return new BoneSlots(l);
    }

    public int count() {
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n++;
        return n;
    }

    /** Bone của ô (null nếu trống / không phải Hồn Cốt). */
    public static Bone boneOf(ItemStack s) { return s.getItem() instanceof HonCotItem h ? h.bone : null; }
}
