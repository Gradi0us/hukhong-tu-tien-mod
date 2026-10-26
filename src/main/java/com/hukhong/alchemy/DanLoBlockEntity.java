package com.hukhong.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Đan Lô: ô 0-2 thảo dược, 3 nhiên liệu, 4 Dị Hỏa Châu, 5 thành phẩm.
 * Minigame: giữ nhiệt trong dải mục tiêu suốt 400 tick; nút 1 = tăng lửa (cần đang cháy), nút 2 = giảm lửa, nút 0 = bắt đầu.
 * Quá nhiệt kéo dài = nổ nhỏ (không phá block); quá nguội kéo dài hoặc thời gian trong dải quá thấp = phế liệu.
 */
public class DanLoBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SLOTS = 6, HERB0 = 0, FUEL = 3, DI_HOA = 4, OUT = 5, DATA_SIZE = 8;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private boolean brewing, diHoa;
    private int heat, tick, inBand, hot, cold, burnLeft, cooldown, recipeIdx;

    public DanLoBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyRegistry.DAN_LO_BE.get(), pos, state);
    }

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> heat;
                case 1 -> tick;
                case 2 -> brewing ? 1 : 0;
                case 3 -> AlchemyMath.BAND_CENTER - half();
                case 4 -> AlchemyMath.BAND_CENTER + half();
                case 5 -> burnLeft;
                case 6 -> AlchemyMath.BREW_TICKS;
                default -> inBand;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return DATA_SIZE; }
    };

    private int half() {
        int tier = brewing ? AlchemyRecipes.ALL.get(recipeIdx).tier() : 3;
        return AlchemyMath.bandHalf(tier, diHoa);
    }

    // ---- nút từ menu ----
    public void button(int id, net.minecraft.world.entity.player.Player player) {
        if (id == 0) { start(player); return; }
        if (!brewing || cooldown > 0) return;
        if (id == 1 && burnLeft > 0) heat = AlchemyMath.clampHeat(heat + AlchemyMath.PRESS);
        else if (id == 2) heat = AlchemyMath.clampHeat(heat - AlchemyMath.PRESS);
        cooldown = 2;
        setChanged();
    }

    private void start(net.minecraft.world.entity.player.Player player) {
        if (brewing) return;
        if (!items.get(OUT).isEmpty()) { say(player, "hukhong.alchemy.out_full"); return; }
        List<ItemStack> herbs = new ArrayList<>(items.subList(HERB0, HERB0 + 3));
        var r = AlchemyRecipes.match(herbs);
        if (r == null) { say(player, "hukhong.alchemy.no_recipe"); return; }
        if (items.get(FUEL).getBurnTime(RecipeType.SMELTING) <= 0) { say(player, "hukhong.alchemy.no_fuel"); return; }
        for (var nd : r.needs()) {
            int left = nd.count();
            for (int i = HERB0; i < HERB0 + 3 && left > 0; i++) {
                ItemStack s = items.get(i);
                if (s.is(nd.item().get())) { int t = Math.min(left, s.getCount()); s.shrink(t); left -= t; }
            }
        }
        diHoa = items.get(DI_HOA).is(AlchemyRegistry.DI_HOA_CHAU.get());
        if (diHoa) items.get(DI_HOA).shrink(1);
        recipeIdx = AlchemyRecipes.ALL.indexOf(r);
        brewing = true;
        heat = 40; tick = 0; inBand = 0; hot = 0; cold = 0; burnLeft = 0; cooldown = 0;
        setChanged();
    }

    private void say(net.minecraft.world.entity.player.Player p, String key) {
        if (p != null) p.displayClientMessage(Component.translatable(key), true);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DanLoBlockEntity be) {
        if (!be.brewing) return;
        be.tick++;
        if (be.cooldown > 0) be.cooldown--;
        if (be.burnLeft <= 0) {
            ItemStack f = be.items.get(FUEL);
            int bt = f.getBurnTime(RecipeType.SMELTING);
            if (bt > 0) {
                be.burnLeft = Math.min(bt, 1600);
                f.shrink(1); // lò than: vật phẩm thùng (xô dung nham) không cần xử lý đặc biệt
            }
        }
        boolean burning = be.burnLeft > 0;
        if (burning) be.burnLeft--;
        be.heat = AlchemyMath.passive(be.heat, be.tick, be.diHoa);
        if (!burning && be.tick % 4 == 0) be.heat = AlchemyMath.clampHeat(be.heat - 1);
        if (AlchemyMath.inBand(be.heat, be.half())) be.inBand++;
        be.hot = be.heat >= 95 ? be.hot + 1 : Math.max(0, be.hot - 1);
        be.cold = be.heat <= 5 ? be.cold + 1 : 0;
        if (be.tick % 20 == 0) level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 1f);
        if (be.hot > 30) { be.fail(level, pos, true); return; }
        if (be.cold > 100) { be.fail(level, pos, false); return; }
        if (be.tick >= AlchemyMath.BREW_TICKS) be.finish(level, pos);
        be.setChanged();
    }

    private void fail(Level level, BlockPos pos, boolean explode) {
        brewing = false;
        setChanged();
        if (explode) level.explode(null, pos.getX() + .5, pos.getY() + 1.0, pos.getZ() + .5, 1.5f, Level.ExplosionInteraction.NONE);
        notifyNear(level, pos, explode ? "hukhong.alchemy.exploded" : "hukhong.alchemy.wasted");
    }

    private void finish(Level level, BlockPos pos) {
        brewing = false;
        var q = AlchemyMath.qualityFor(inBand / (double) AlchemyMath.BREW_TICKS);
        if (q == null) { notifyNear(level, pos, "hukhong.alchemy.wasted"); return; }
        var r = AlchemyRecipes.ALL.get(recipeIdx);
        ItemStack out = new ItemStack(r.result().get(), r.yield());
        out.set(AlchemyRegistry.PILL_QUALITY.get(), q.ordinal());
        items.set(OUT, out);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1f, 1f);
        notifyNear(level, pos, "hukhong.alchemy.done." + q.name().toLowerCase());
    }

    private void notifyNear(Level level, BlockPos pos, String key) {
        var p = level.getNearestPlayer(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 8, false);
        say(p, key);
    }

    // ---- container ----
    @Override protected Component getDefaultName() { return Component.translatable("hukhong.alchemy.title"); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> list) { items = list; }
    @Override public int getContainerSize() { return SLOTS; }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new DanLoMenu(id, inv, this, data); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putBoolean("brewing", brewing);
        tag.putBoolean("diHoa", diHoa);
        tag.putInt("heat", heat); tag.putInt("tick", tick); tag.putInt("inBand", inBand);
        tag.putInt("burnLeft", burnLeft); tag.putInt("recipe", recipeIdx);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        brewing = tag.getBoolean("brewing");
        diHoa = tag.getBoolean("diHoa");
        heat = tag.getInt("heat"); tick = tag.getInt("tick"); inBand = tag.getInt("inBand");
        burnLeft = tag.getInt("burnLeft");
        recipeIdx = Math.max(0, Math.min(AlchemyRecipes.ALL.size() - 1, tag.getInt("recipe")));
    }
}
