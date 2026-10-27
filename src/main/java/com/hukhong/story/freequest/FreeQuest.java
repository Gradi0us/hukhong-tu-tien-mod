package com.hukhong.story.freequest;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.item.ModItems;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.objective.Objectives;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/** Quest phụ "Đột Phá" cho chế độ Tự do: tới Đan Sư ở chợ thành gần nhất lấy đan phá Bình Cảnh. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class FreeQuest {
    private FreeQuest() {}

    public static final String OBJ_PREFIX = "freequest.dotpha.";
    public static final String OBJ_FIND = OBJ_PREFIX + "1", OBJ_USE = OBJ_PREFIX + "2";
    private static final String PILL_TRADER_KEY = "hukhong.trade.pill";

    /** Đan cần cho cấp Bình Cảnh; null nếu chưa có đan tương ứng. */
    public static @Nullable Item pillFor(int level) {
        return switch (level) {
            case 94 -> ModItems.TIEU_KINH_DAN_V.get();
            case 98 -> ModItems.TIEU_BAN_THAN_DAN.get();
            default -> switch (FreeQuestMath.pillTier(level)) {
                case 1 -> ModItems.KHAI_MACH_DAN.get();
                case 2 -> ModItems.NGUNG_NGUYEN_DAN.get();
                case 3 -> AlchemyRegistry.THONG_HUYEN_DAN.get();
                case 4 -> AlchemyRegistry.TU_VUONG_DAN.get();
                case 5 -> AlchemyRegistry.HOANG_CUC_DAN.get();
                case 6 -> AlchemyRegistry.DE_HOAN_DAN.get();
                default -> null;
            };
        };
    }

    private static Item herbFor(int tier) {
        return FreeQuestMath.herbIsHuyetTham(tier) ? AlchemyRegistry.HUYET_THAM.get() : AlchemyRegistry.LINH_THAO.get();
    }

    public static FreeQuestData data(ServerPlayer p) { return p.getData(FreeQuestAttachments.DATA.get()); }

    private static boolean isFree(ServerPlayer p) {
        return p.getData(StoryRegistry.STORY.get()).mode() == StoryData.MODE_FREE;
    }

    /** Đang ở Bình Cảnh (chính hoặc nhẹ). */
    private static boolean locked(CultivationData d) {
        return d.inBottleneck() || CultivationService.inLightBottleneck(d);
    }

    private static boolean ownObjective(ServerPlayer p) {
        String id = Objectives.currentId(p);
        return id != null && id.startsWith(OBJ_PREFIX);
    }

    private static boolean has(ServerPlayer p, Item item) {
        for (ItemStack s : p.getInventory().items) if (s.is(item)) return true;
        return false;
    }

    private static Component pillName(Item pill) { return Component.translatable(pill.getDescriptionId()); }

    /** Gọi định kỳ (mỗi 20 tick) và từ test. */
    public static void tick(ServerPlayer p) {
        if (!isFree(p)) return;
        CultivationData cd = CultivationService.get(p);
        FreeQuestData q = data(p);
        int lv = cd.level();
        if (q.active() != 0 && (q.active() != lv || !locked(cd))) { // đã phá xong (hoặc đổi cấp)
            if (ownObjective(p)) {
                Objectives.complete(p, OBJ_USE);
                if (ownObjective(p)) Objectives.clear(p);
            }
            p.setData(FreeQuestAttachments.DATA.get(), q.withActive(0));
            q = data(p);
        }
        if (!locked(cd) || !FreeQuestMath.isQuestLevel(lv)) return;
        Item pill = pillFor(lv);
        if (pill == null) return;
        String cur = Objectives.currentId(p);
        if (q.active() == 0) {
            if (cur != null && !cur.startsWith(OBJ_PREFIX)) return; // không đè mục tiêu khác
            q = q.withActive(lv);
            if (lv == 9 && !q.hinted()) {
                q = q.withHinted();
                p.sendSystemMessage(Component.translatable("hukhong.fq.hint9", pillName(pill)));
            }
            p.setData(FreeQuestAttachments.DATA.get(), q);
        }
        if (has(p, pill)) {
            if (!OBJ_USE.equals(cur) && (cur == null || cur.startsWith(OBJ_PREFIX)))
                Objectives.set(p, OBJ_USE, Component.translatable("hukhong.fq.step2", pillName(pill)), null, null);
        } else if (cur == null || (cur.startsWith(OBJ_PREFIX) && !OBJ_FIND.equals(cur))) {
            setFind(p, pill);
        }
    }

    private static void setFind(ServerPlayer p, Item pill) {
        BlockPos target = null;
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        if (ow != null) {
            CityDef c = CityService.nearest(ow, p.getX(), p.getZ(), 1.0e7);
            if (c != null) target = CityService.builtCenter(ow, c);
        }
        Objectives.set(p, OBJ_FIND, Component.translatable("hukhong.fq.step1", pillName(pill)), target,
                target == null ? null : Level.OVERWORLD);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) tick(p);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof com.hukhong.entity.HuMob m) || !(e.getSource().getEntity() instanceof ServerPlayer p) || !isFree(p)) return;
        FreeQuestData q = data(p);
        if (q.active() == 0 || m.huLevel() < FreeQuestMath.minMobLevel(q.active())) return;
        p.setData(FreeQuestAttachments.DATA.get(), q.withKills(q.kills() + 1));
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || e.getLevel().isClientSide()) return;
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getTarget() instanceof WanderingTrader t) || p.isShiftKeyDown()) return;
        if (!isPillTrader(t) || !isFree(p)) return;
        FreeQuestData q = data(p);
        if (q.active() == 0) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        talk(p, q);
    }

    private static boolean isPillTrader(WanderingTrader t) {
        return t.getCustomName() != null && t.getCustomName().getContents() instanceof TranslatableContents tc
                && PILL_TRADER_KEY.equals(tc.getKey());
    }

    /** Đối thoại Đan Sư: nộp thảo dược hoặc đủ Hư Hóa là nhận đan (1 lần / mốc). Shift+chuột phải = giao dịch thường. */
    static void talk(ServerPlayer p, FreeQuestData q) {
        int lv = q.active();
        Item pill = pillFor(lv);
        if (pill == null) return;
        if (q.wasRewarded(lv)) { p.sendSystemMessage(Component.translatable("hukhong.fq.npc.done")); return; }
        int tier = FreeQuestMath.pillTier(lv);
        Item herb = herbFor(tier);
        int need = FreeQuestMath.herbCount(tier), kills = FreeQuestMath.killCount(tier);
        int have = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(herb)) have += s.getCount();
        if (have >= need) {
            int left = need;
            for (ItemStack s : p.getInventory().items) {
                if (left <= 0) break;
                if (s.is(herb)) { int n = Math.min(left, s.getCount()); s.shrink(n); left -= n; }
            }
        } else if (q.kills() < kills) {
            p.sendSystemMessage(Component.translatable("hukhong.fq.npc.ask", pillName(pill), need,
                    Component.translatable(herb.getDescriptionId()), kills, q.kills()));
            return;
        }
        p.setData(FreeQuestAttachments.DATA.get(), q.withRewarded(lv));
        ItemStack out = new ItemStack(pill);
        if (!p.getInventory().add(out)) p.drop(out, false);
        p.sendSystemMessage(Component.translatable("hukhong.fq.npc.give", pillName(pill)));
        tick(p);
    }
}
