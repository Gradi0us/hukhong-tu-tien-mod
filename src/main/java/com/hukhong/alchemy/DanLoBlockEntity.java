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
    /** Nguoi bat dau me dan + cap Luyen Dan Su luc bat dau (nghe phu). */
    private java.util.UUID brewer;
    private int brewLevel;

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
        int lv = player == null ? 0 : com.hukhong.profession.ProfessionService.level(player, com.hukhong.profession.Profession.LUYEN_DAN);
        if (lv <= 0) { say(player, "hukhong.profession.need.luyen_dan"); return; }
        if (!items.get(OUT).isEmpty()) { say(player, "hukhong.alchemy.out_full"); return; }
        List<ItemStack> herbs = new ArrayList<>(items.subList(HERB0, HERB0 + 3));
        var r = AlchemyRecipes.match(herbs);
        if (r == null) { say(player, "hukhong.alchemy.no_recipe"); return; }
        if (player == null || !com.hukhong.craft.RecipeBook.knows(player, com.hukhong.craft.RecipeBook.DAN + r.id())) {
            say(player, "hukhong.recipe.unknown"); return;
        }
        if (r.tier() > lv / 2 + 1) { say(player, "hukhong.profession.tier_locked", Math.max(1, 2 * (r.tier() - 1))); return; }
        if (items.get(FUEL).getBurnTime(RecipeType.SMELTING) <= 0) { say(player, "hukhong.alchemy.no_fuel"); return; }
        brewer = player.getUUID();
        brewLevel = lv;
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

    /** Xep nguyen lieu tu tui vao 3 o thao duoc (nut "Xep nguyen lieu" trong so cong thuc). Khong lam khi dang luyen. */
    public void arrange(net.minecraft.server.level.ServerPlayer sp, String recipeId) {
        if (brewing) { say(sp, "hukhong.recipe.brewing"); return; }
        com.hukhong.craft.RecipeBook.arrange(sp, this, new int[]{HERB0, HERB0 + 1, HERB0 + 2}, recipeId,
                com.hukhong.profession.Profession.LUYEN_DAN);
    }

    private void say(net.minecraft.world.entity.player.Player p, String key, Object... args) {
        if (p != null) p.displayClientMessage(Component.translatable(key, args), true);
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
        double ratio = inBand / (double) AlchemyMath.BREW_TICKS;
        if (level.getNearestPlayer(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 8, false) instanceof net.minecraft.server.level.ServerPlayer sp)
            ratio = Math.min(1.0, ratio + com.hukhong.society.SectBuffs.alchemyBonus(sp)); // buff Đan Đỉnh Tông
        var q = AlchemyMath.qualityFor(ratio);
        if (q == null) { notifyNear(level, pos, "hukhong.alchemy.wasted"); return; }
        // Đan Tâm: Đan Tu đứng gần lò được +1 bậc phẩm chất (Quality.of kẹp trong [HA, CUC])
        if (level.getNearestPlayer(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 8, false) instanceof net.minecraft.server.level.ServerPlayer brewer
                && brewer.getData(com.hukhong.core.ModAttachments.CULTIVATION).paths().contains(com.hukhong.core.Path.DAN)) {
            q = AlchemyMath.Quality.of(q.ordinal() + com.hukhong.dan.DanMath.DAN_LO_QUALITY_BONUS);
        }
        var r = AlchemyRecipes.ALL.get(recipeIdx);
        int yield = r.yield();
        // Luyen Dan Su: cap cang cao, cang co hoi them 1 vien (3%/cap)
        if (level.getRandom().nextDouble() < brewLevel * com.hukhong.profession.ProfessionMath.PILL_BONUS_PER_LEVEL) yield++;
        ItemStack out = new ItemStack(r.result().get(), yield);
        out.set(AlchemyRegistry.PILL_QUALITY.get(), q.ordinal());
        String crafterName = brewer != null && level.getServer() != null
                && level.getServer().getPlayerList().getPlayer(brewer) instanceof net.minecraft.server.level.ServerPlayer nm
                ? nm.getName().getString() : null;
        com.hukhong.craft.CraftQuality.stamp(out, q.ordinal(), crafterName);
        items.set(OUT, out);
        if (brewer != null && level.getServer() != null
                && level.getServer().getPlayerList().getPlayer(brewer) instanceof net.minecraft.server.level.ServerPlayer bp)
            com.hukhong.profession.ProfessionService.addCraftXp(bp, com.hukhong.profession.Profession.LUYEN_DAN, 5L * r.tier());
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
        if (brewer != null) tag.putUUID("brewer", brewer);
        tag.putInt("brewLevel", brewLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        brewing = tag.getBoolean("brewing");
        diHoa = tag.getBoolean("diHoa");
        heat = tag.getInt("heat"); tick = tag.getInt("tick"); inBand = tag.getInt("inBand");
        burnLeft = tag.getInt("burnLeft");
        recipeIdx = Math.max(0, Math.min(AlchemyRecipes.ALL.size() - 1, tag.getInt("recipe")));
        brewer = tag.hasUUID("brewer") ? tag.getUUID("brewer") : null;
        brewLevel = tag.getInt("brewLevel");
    }
}
