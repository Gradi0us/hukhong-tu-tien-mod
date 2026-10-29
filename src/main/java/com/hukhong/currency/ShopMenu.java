package com.hukhong.currency;

import com.hukhong.guild.GuildRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu cua hang Tu Tinh (khong co o do). Client nhan danh sach hang (anh chup) khi mo; nut: id 0..N-1 mua 1,
 * 1000+i ban 1, 2000+i ban het. Server tinh lai danh sach va kiem tra moi thu.
 */
public class ShopMenu extends AbstractContainerMenu {
    public static final int B_SELL1 = 1000, B_SELLALL = 2000;

    public record Entry(ItemStack stack, long price, int minRank) {}

    /** rank: bac Hiep Hoi (cu) hoac bac Uy Danh Cho (marketLocks=true); discountPct = % giam gia hien thi. */
    public record Snapshot(int entityId, int kind, int rank, List<Entry> buys, List<Entry> sells, int discountPct, boolean marketLocks) {
        public Snapshot(int entityId, int kind, int rank, List<Entry> buys, List<Entry> sells) { this(entityId, kind, rank, buys, sells, 5 * rank, false); }

        public void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(entityId);
            b.writeVarInt(kind);
            b.writeVarInt(rank);
            writeList(b, buys);
            writeList(b, sells);
            b.writeVarInt(discountPct);
            b.writeBoolean(marketLocks);
        }

        private static void writeList(RegistryFriendlyByteBuf b, List<Entry> l) {
            b.writeVarInt(l.size());
            for (Entry e : l) {
                ItemStack.STREAM_CODEC.encode(b, e.stack());
                b.writeVarLong(e.price());
                b.writeVarInt(e.minRank());
            }
        }

        private static List<Entry> readList(RegistryFriendlyByteBuf b) {
            int n = b.readVarInt();
            List<Entry> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(new Entry(ItemStack.STREAM_CODEC.decode(b), b.readVarLong(), b.readVarInt()));
            return l;
        }

        public static Snapshot read(RegistryFriendlyByteBuf b) {
            return new Snapshot(b.readVarInt(), b.readVarInt(), b.readVarInt(), readList(b), readList(b), b.readVarInt(), b.readBoolean());
        }
    }

    public final Snapshot s;

    public ShopMenu(int id, Inventory inv, Snapshot s) {
        super(GuildRegistry.SHOP_MENU.get(), id);
        this.s = s;
    }

    public ShopMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, Snapshot.read(buf)); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || !stillValid(player)) return false;
        Entity e = sp.level().getEntity(s.entityId());
        return e != null && ShopService.handleButton(sp, e, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        Entity e = player.level().getEntity(s.entityId());
        return e != null && e.isAlive() && player.distanceToSqr(e) < 64;
    }
}
