package com.hukhong.poi;

import com.hukhong.core.CultivationService;
import com.hukhong.core.Path;
import com.hukhong.currency.Wallet;
import com.hukhong.honcot.Bone;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.honcot.HonCotRegistry;
import com.hukhong.skillbook.SkillBookRewards;
import com.hukhong.skillbook.SkillGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** Phan thuong cua dia diem tu danh sach "loot" trong JSON: tien te (Tu Tinh / Hon Tinh) tra thang vao vi; vat pham, sach ky nang, Hon Cot vao tui / ruong. */
public final class PoiRewards {
    private PoiRewards() {}

    public static boolean isCurrency(String item) { return item.equals("@tutinh") || item.equals("@hontinh"); }

    private static int amount(PoiDef.Loot l, RandomSource r) { return l.max() <= l.min() ? l.min() : l.min() + r.nextInt(l.max() - l.min() + 1); }

    public static ItemStack bone(RandomSource r) {
        Bone[] all = Bone.values();
        return HonCotRegistry.stack(all[r.nextInt(all.length)], HonCotMath.TIER_TU, 30 + r.nextInt(40));
    }

    /** Vat pham (khong gom tien te) theo xac suat; paths de ua tien sach ky nang hop dao. */
    public static List<ItemStack> itemStacks(PoiDef d, RandomSource r, Set<Path> paths) {
        List<ItemStack> out = new ArrayList<>();
        for (PoiDef.Loot l : d.loot()) {
            if (isCurrency(l.item()) || r.nextDouble() >= l.chance()) continue;
            if (l.item().startsWith("@skillbook:")) {
                try {
                    SkillGrade g = SkillGrade.valueOf(l.item().substring(11).toUpperCase(Locale.ROOT));
                    out.add(SkillBookRewards.stack(new Random(r.nextLong()), g, paths));
                } catch (IllegalArgumentException ignored) { }
            } else if (l.item().equals("@bone")) {
                out.add(bone(r));
            } else {
                ResourceLocation id = ResourceLocation.tryParse(l.item());
                Item it = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (it != null) out.add(new ItemStack(it, Math.max(1, amount(l, r))));
            }
        }
        return out;
    }

    public static void currencies(ServerPlayer p, PoiDef d, RandomSource r) {
        for (PoiDef.Loot l : d.loot()) {
            if (!isCurrency(l.item()) || r.nextDouble() >= l.chance()) continue;
            int n = amount(l, r);
            if (n <= 0) continue;
            if (l.item().equals("@hontinh")) Wallet.grantHonTinh(p, n);
            else Wallet.give(p, n);
        }
    }

    public static void giveStacks(ServerPlayer p, List<ItemStack> stacks) {
        for (ItemStack st : stacks) SkillBookRewards.give(p, st);
    }

    /** Tien te + vat pham thang vao nguoi choi. */
    public static void giveAll(ServerPlayer p, PoiDef d, RandomSource r) {
        currencies(p, d, r);
        giveStacks(p, itemStacks(d, r, CultivationService.get(p).paths()));
    }

    /** Do day ruong tai pos bang vat pham trong loot (xoa cu). */
    public static void fillChest(ServerLevel l, BlockPos pos, PoiDef d, RandomSource r) {
        if (!(l.getBlockEntity(pos) instanceof Container c)) return;
        c.clearContent();
        List<ItemStack> st = itemStacks(d, r, null);
        for (int i = 0; i < st.size() && i < c.getContainerSize(); i++) c.setItem(i, st.get(i));
        c.setChanged();
    }

    public static void emptyChest(ServerLevel l, BlockPos pos) {
        if (l.getBlockEntity(pos) instanceof Container c) { c.clearContent(); c.setChanged(); }
    }
}
