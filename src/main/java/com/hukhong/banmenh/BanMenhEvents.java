package com.hukhong.banmenh;

import com.hukhong.gates.Resources;
import com.hukhong.item.ModItems;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ràng buộc chủ, không rơi khi chết, hiệu ứng theo loại + giai đoạn. */
@EventBusSubscriber(modid = com.hukhong.HuKhong.MODID)
public final class BanMenhEvents {
    private BanMenhEvents() {}

    private static boolean reentry = false;
    private static final Map<UUID, Integer> HIT_COUNT = new HashMap<>();
    private static final Map<UUID, Long> FREEZE_CD = new HashMap<>();

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent e) {
        if (reentry) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p) return;
        ItemStack stack = p.getMainHandItem();
        if (!stack.is(ModItems.BAN_MENH_KIEM.get())) return;
        BanMenhType type = BanMenhKiemItem.typeOf(stack);
        if (type == null) return;
        if (!BanMenhKiemItem.isOwner(stack, p)) { // không phải chủ: không sát thương
            e.setCanceled(true);
            p.displayClientMessage(Component.translatable("hukhong.banmenh.msg.not_owner"), true);
            return;
        }
        LivingEntity target = e.getEntity();
        int st = BanMenhKiemItem.stageOf(stack);
        RandomSource rng = p.getRandom();
        double ap = StatService.attackPower(p);
        float base = (float) (ap * BanMenhService.QUALITY[st]);
        float amt = e.getAmount() + base;
        long now = p.level().getGameTime();

        switch (type) {
            case THANH_PHONG -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
                if (st >= 2) for (LivingEntity o : near(target, p, 3, 1)) extra(p, o, amt * 0.5f);
                if (st >= 4) p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false));
            }
            case LIET_DIEM -> {
                if (rng.nextFloat() < 0.20f) { target.igniteForSeconds(3); amt += base * 0.20f; }
                if (st >= 2 && rng.nextFloat() < 0.25f) for (LivingEntity o : near(target, p, 3, 99)) {
                    extra(p, o, amt * 0.3f); if (st >= 4) o.igniteForSeconds(3);
                }
            }
            case HAN_BANG -> {
                if (rng.nextFloat() < 0.15f) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                if (st >= 2) {
                    Long cd = FREEZE_CD.get(p.getUUID());
                    if (cd == null || now - cd >= 160) {
                        FREEZE_CD.put(p.getUUID(), now);
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 6));
                        target.setTicksFrozen(target.getTicksRequiredToFreeze() + 20);
                    }
                }
                if (st >= 4 && rng.nextFloat() < 0.25f) for (LivingEntity o : near(target, p, 5, 99))
                    o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            case LOI_DINH -> {
                if (rng.nextFloat() < 0.10f) {
                    amt += (float) (ap * 0.6);
                    bolt(target);
                    if (st >= 2) for (LivingEntity o : near(target, p, 6, 3)) extra(p, o, (float) (ap * 0.6));
                    if (st >= 4) for (LivingEntity o : near(target, p, 4, 99)) extra(p, o, (float) (ap * 0.4));
                }
            }
            case HUYET_ANH -> {
                float pct = st >= 4 ? 0.09f : st >= 2 ? 0.06f : 0.03f;
                p.heal(amt * pct);
                if (st >= 4 && p instanceof ServerPlayer sp) Resources.add(sp, Math.round(amt * 0.02f));
            }
            case TRONG_NHAC -> {
                amt *= 1.12f;
                if (st >= 2 && rng.nextDouble() < p.getAttributeValue(ModAttributes.holder(ModAttributes.CRIT_CHANCE)))
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6));
                if (st >= 4) amt += e.getAmount() * 0.15f; // phá giáp 15% (xấp xỉ: cộng thêm sát thương gốc)
            }
            case NGOC_TAM -> Resources.add(p, 1 + st);
            case TINH_THAN -> {
                if (rng.nextFloat() < 0.06f) amt *= 1.75f;
                if (st >= 4 && p.level().isNight()) amt *= 1.25f;
                if (st >= 2) {
                    int c = HIT_COUNT.merge(p.getUUID(), 1, Integer::sum);
                    if (c >= 5) { HIT_COUNT.put(p.getUUID(), 0); amt += (float) (ap * 2.0); }
                }
            }
        }
        e.setAmount(amt);
    }

    private static List<LivingEntity> near(LivingEntity target, Player owner, double r, int max) {
        List<LivingEntity> l = target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(r),
                o -> o != target && o != owner && o.isAlive() && !(o instanceof Player));
        return l.size() > max ? l.subList(0, max) : l;
    }

    static void extra(ServerPlayer p, LivingEntity o, float dmg) {
        reentry = true;
        try { o.hurt(p.damageSources().playerAttack(p), dmg); } finally { reentry = false; }
    }

    private static void bolt(LivingEntity target) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(target.level());
        if (b == null) return;
        b.moveTo(target.position());
        b.setVisualOnly(true);
        target.level().addFreshEntity(b);
    }

    /** Không rơi khi chết (canon §3): loại khỏi drops; respawn sẽ trả lại. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (e.getEntity() instanceof ServerPlayer)
            e.getDrops().removeIf(ie -> ie.getItem().is(ModItems.BAN_MENH_KIEM.get()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        HIT_COUNT.remove(id);
        FREEZE_CD.remove(id);
        BanMenhService.forget(id);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !e.isEndConquered()) BanMenhService.restore(p);
    }

    /** Người khác không nhặt được kiếm của chủ. */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack s = e.getItemEntity().getItem();
        if (s.is(ModItems.BAN_MENH_KIEM.get()) && !BanMenhKiemItem.isOwner(s, e.getPlayer()))
            e.setCanPickup(TriState.FALSE);
    }

    /** Tôi Kiếm trên đe: Shift + chuột phải đe khi cầm Bản Mệnh Kiếm (Hồn Tinh lấy trong túi). */
    @SubscribeEvent
    public static void onAnvil(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        Player pl = e.getEntity();
        ItemStack s = pl.getMainHandItem();
        if (!pl.isShiftKeyDown() || !s.is(ModItems.BAN_MENH_KIEM.get())) return;
        if (!pl.level().getBlockState(e.getPos()).is(net.minecraft.tags.BlockTags.ANVIL)) return;
        e.setCanceled(true);
        e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (pl instanceof ServerPlayer sp && !pl.level().isClientSide) BanMenhService.tryEvolve(sp, s, false);
    }

    // ------------------------------------------------------------------ không vứt / không cất vào vật chứa

    private static boolean isSword(ItemStack s) { return s.is(ModItems.BAN_MENH_KIEM.get()); }

    /** Phím Q / kéo ra ngoài GUI: hủy việc vứt, trả kiếm vào túi (ưu tiên đúng ô đang chọn). */
    @SubscribeEvent
    public static void onToss(net.neoforged.neoforge.event.entity.item.ItemTossEvent e) {
        ItemStack s = e.getEntity().getItem();
        if (!isSword(s) || !(e.getPlayer() instanceof ServerPlayer p) || !p.isAlive()) return;
        ItemStack copy = s.copy();
        var inv = p.getInventory();
        e.setCanceled(true);
        if (inv.getItem(inv.selected).isEmpty()) inv.setItem(inv.selected, copy);
        else if (!inv.add(copy)) {
            if (p.containerMenu.getCarried().isEmpty()) p.containerMenu.setCarried(copy);
            else e.setCanceled(false); // hết chỗ hoàn toàn: để rơi, /banmenh summon dọn bản rơi
        }
        p.displayClientMessage(Component.translatableWithFallback("hukhong.banmenh.msg.no_drop",
                "Bản Mệnh Kiếm gắn liền với chủ nhân, không thể vứt bỏ."), true);
    }

    /** Khung vật phẩm / giá giáp / Allay không nhận Bản Mệnh Kiếm. */
    @SubscribeEvent
    public static void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
        if (!isSword(e.getItemStack())) return;
        var t = e.getTarget();
        if (t instanceof net.minecraft.world.entity.decoration.ItemFrame || t instanceof net.minecraft.world.entity.animal.allay.Allay) {
            e.setCanceled(true);
            e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e) {
        if (isSword(e.getItemStack()) && e.getTarget() instanceof net.minecraft.world.entity.decoration.ArmorStand) {
            e.setCanceled(true);
            e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    /** Bình trang trí nhận mọi vật phẩm: chặn. */
    @SubscribeEvent
    public static void onPotInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (!isSword(e.getItemStack())) return;
        if (e.getLevel().getBlockState(e.getPos()).getBlock() instanceof net.minecraft.world.level.block.DecoratedPotBlock) {
            e.setCanceled(true);
            e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    /**
     * Lưới an toàn cho mọi đường vào vật chứa (Shift-click, phím số, kéo thả): mỗi 2 tick quét ô không thuộc túi
     * người chơi, kiếm lạc vào đó bị trả về túi.
     */
    @SubscribeEvent
    public static void onContainerScan(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 2 != 0 || p.isCreative()) return;
        var menu = p.containerMenu;
        for (var slot : menu.slots) {
            if (slot.container == p.getInventory() || !isSword(slot.getItem())) continue;
            ItemStack s = slot.getItem().copy();
            if (p.getInventory().add(s)) {
                slot.set(ItemStack.EMPTY);
                slot.setChanged();
            } else if (menu.getCarried().isEmpty()) {
                slot.set(ItemStack.EMPTY);
                menu.setCarried(s);
            }
        }
    }
}
