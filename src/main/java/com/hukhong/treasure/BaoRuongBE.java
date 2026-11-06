package com.hukhong.treasure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Dữ liệu Bảo Rương: 27 ô như rương thường; số người đang mở đồng bộ qua blockEvent để nắp bật/đóng có hoạt ảnh. */
public class BaoRuongBE extends RandomizableContainerBlockEntity implements GeoBlockEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.bao_ruong.idle");
    static final RawAnimation OPEN = RawAnimation.begin().thenPlay("animation.bao_ruong.open").thenLoop("animation.bao_ruong.opened");

    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int openers;

    public BaoRuongBE(BlockPos pos, BlockState s) { super(TreasureRegistry.BAO_RUONG_BE.get(), pos, s); }

    public int tier() { return getBlockState().getValue(BaoRuongBlock.TIER); }

    public boolean isOpen() { return openers > 0; }

    @Override protected NonNullList<ItemStack> getItems() { return items; }

    @Override protected void setItems(NonNullList<ItemStack> l) { items = l; }

    @Override public int getContainerSize() { return 27; }

    @Override protected Component getDefaultName() { return Component.translatable("block.hukhong.bao_ruong.tier" + tier()); }

    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return ChestMenu.threeRows(id, inv, this); }

    @Override
    public void startOpen(Player p) {
        if (p.isSpectator() || level == null) return;
        if (openers++ == 0) level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 0.7f + 0.1f * tier());
        sync();
    }

    @Override
    public void stopOpen(Player p) {
        if (p.isSpectator() || level == null) return;
        openers = Math.max(0, openers - 1);
        if (openers == 0) level.playSound(null, worldPosition, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.8f, 0.7f);
        sync();
    }

    /** Đồng bộ số người đang mở xuống client (gói dữ liệu khối) để nắp bật/đóng. */
    private void sync() {
        if (level == null || level.isClientSide) return;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        t.putInt("Openers", openers);
        return t;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) { openers = param; return true; }
        return super.triggerEvent(id, param);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, reg);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("Openers")) { openers = tag.getInt("Openers"); return; } // gói đồng bộ client
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items, reg);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "lid", 3, st -> st.setAndContinue(isOpen() || getBlockState().getValue(BaoRuongBlock.LOOTED) && openers > 0 ? OPEN : IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
