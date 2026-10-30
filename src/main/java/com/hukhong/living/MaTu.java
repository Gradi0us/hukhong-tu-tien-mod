package com.hukhong.living;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.city.ambient.AmbientRegistry;
import com.hukhong.core.CultivationService;
import com.hukhong.story.StoryDocs;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.StoryService;
import com.hukhong.story.SuDo;
import com.hukhong.story.objective.Objectives;
import com.hukhong.story.world.Faction;
import com.hukhong.story.world.Lore;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ma Tu Doi Lot (D9 = Su Do cai trang dan thuong). Ban dem (hoac khi bi truy duoi) lo mat that: hoa thanh Su Do, ha duoc roi Van Kien A.
 * Dung chung cho (1) ma tu ngau nhien trong thanh, (2) ke mua bi an cua Dau Gia Hoi (owner = nguoi choi truy duoi, loot = lo da mua).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MaTu {
    private MaTu() {}

    public static final String TAG = "hk_matu", PURSUIT_ID = "auction.pursuit";
    public static final Lore LORE = new Lore(Faction.DA_KHUYET, 4, 99, List.of(), List.of(), null, null, null);
    public static final Lore PURSUIT_DONE = new Lore(Faction.DA_KHUYET, 4, 99, List.of(), List.of("wl.auction_pursuit_done"), null, null, null);

    /** Sinh Ma Tu trong lot dan thuong. owner/loot co the null/rong. */
    public static AmbientNpc spawnDisguised(ServerLevel sl, BlockPos pos, UUID owner, ItemStack loot) {
        AmbientNpc n = AmbientRegistry.AMBIENT.get().create(sl);
        if (n == null) return null;
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        n.setup(AmbientNpc.MA_TU, sl.random.nextInt(AmbientRegistry.SKINS), pos, 20);
        n.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        n.addTag(TAG);
        CompoundTag pd = n.getPersistentData();
        if (owner != null) { pd.putUUID("hk_owner", owner); n.setPersistenceRequired(); }
        if (loot != null && !loot.isEmpty()) pd.put("hk_loot", loot.save(sl.registryAccess()));
        sl.addFreshEntity(n);
        return n;
    }

    /** Moi giay (goi tu NpcLife.tick): ma tu rieng bi lo khi nguoi choi o gan 8 o; ma tu thuong lo ban dem khi nguoi choi o gan 12 o. */
    public static void tick(AmbientNpc n) {
        if (!(n.level() instanceof ServerLevel sl)) return;
        CompoundTag pd = n.getPersistentData();
        boolean owned = pd.hasUUID("hk_owner");
        Player near = sl.getNearestPlayer(n, owned ? 8 : 12);
        if (!(near instanceof ServerPlayer p)) return;
        if (owned && !p.getUUID().equals(pd.getUUID("hk_owner"))) return;
        if (!owned && !NpcSchedule.isNight(sl.getDayTime())) return;
        reveal(n, p);
    }

    /** Lo mat that: thay bang Su Do (bien the Da Kiem), giu owner/loot trong persistent data. */
    public static void reveal(AmbientNpc n, ServerPlayer target) {
        ServerLevel sl = (ServerLevel) n.level();
        int lv = Math.max(10, Math.min(StoryService.MAX_LEVEL, CultivationService.get(target).level()));
        SuDo su = StoryService.spawn(sl, n.blockPosition(), 0, lv);
        if (su == null) return;
        su.addTag(TAG);
        su.setPersistenceRequired();
        su.getPersistentData().merge(n.getPersistentData().copy());
        su.setTarget(target);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, n.getX(), n.getY() + 1, n.getZ(), 25, 0.4, 0.8, 0.4, 0.05);
        target.sendSystemMessage(Tx.t("hukhong.living.matu.reveal", "Lớp cải trang tuột xuống - đó là Sứ Đồ Dạ Khuyết!", "The disguise falls away - a Night-Rift envoy!"));
        n.discard();
    }

    /** Tung Van Kien chuoi A cho Su Do: chon ngau nhien trong cac tai lieu A hop bac. */
    public static StoryDocs.Doc pickDoc(net.minecraft.util.RandomSource r, int level) {
        int tier = StoryDocs.tierOf(level);
        List<StoryDocs.Doc> ok = new ArrayList<>();
        for (StoryDocs.Doc d : StoryDocs.ALL) if (d.chain() == 'a' && tier >= d.minTier() && tier <= d.maxTier()) ok.add(d);
        return ok.isEmpty() ? StoryDocs.get("a1") : ok.get(r.nextInt(ok.size()));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof SuDo su) || !su.getTags().contains(TAG) || su.level().isClientSide) return;
        ServerLevel sl = (ServerLevel) su.level();
        StoryDocs.Doc d = pickDoc(su.getRandom(), su.suLevel());
        e.getDrops().add(new ItemEntity(sl, su.getX(), su.getY() + 0.5, su.getZ(), new ItemStack(StoryRegistry.DOC_ITEMS.get(d.id()).get())));
        CompoundTag pd = su.getPersistentData();
        if (pd.contains("hk_loot")) {
            ItemStack loot = ItemStack.parse(sl.registryAccess(), pd.getCompound("hk_loot")).orElse(ItemStack.EMPTY);
            if (!loot.isEmpty()) e.getDrops().add(new ItemEntity(sl, su.getX(), su.getY() + 0.5, su.getZ(), loot));
        }
        if (pd.hasUUID("hk_owner") && sl.getServer().getPlayerList().getPlayer(pd.getUUID("hk_owner")) instanceof ServerPlayer owner) {
            Objectives.complete(owner, PURSUIT_ID);
            StoryHooks.complete(owner, PURSUIT_DONE);
            owner.sendSystemMessage(Tx.t("hukhong.living.matu.recovered", "Bạn đã hạ kẻ mua bí ẩn và lấy lại bảo vật.", "You downed the masked buyer and recovered the prize."));
        }
    }
}
