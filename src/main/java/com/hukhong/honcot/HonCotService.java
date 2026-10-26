package com.hukhong.honcot;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

/** Server: mang / tháo Hồn Cốt, áp chỉ số (modifier transient qua StatService.refresh), đồng bộ client. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HonCotService {
    private HonCotService() {}

    public static BoneSlots get(ServerPlayer p) { return p.getData(HonCotRegistry.BONE_SLOTS.get()); }

    private static ResourceLocation modId(String path) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, path); }

    public static boolean equip(ServerPlayer p, ItemStack stack) {
        Bone b = BoneSlots.boneOf(stack);
        if (b == null || stack.isEmpty()) return false;
        int tier = HonCotItem.tierOf(stack);
        int lv = CultivationService.get(p).level();
        if (!HonCotMath.canEquip(lv, tier)) {
            p.displayClientMessage(Component.translatable("hukhong.honcot.msg.level", HonCotMath.requiredLevel(tier)), true);
            return false;
        }
        BoneSlots s = get(p);
        ItemStack old = s.get(b.slot);
        p.setData(HonCotRegistry.BONE_SLOTS.get(), s.with(b.slot, stack));
        if (!old.isEmpty() && !p.getInventory().add(old.copy())) p.drop(old.copy(), false);
        p.displayClientMessage(Component.translatable("hukhong.honcot.msg.equipped", stack.getHoverName()), true);
        changed(p);
        return true;
    }

    public static boolean unequip(ServerPlayer p, int slotIdx) {
        BoneSlot slot = BoneSlot.of(slotIdx);
        if (slot == null) return false;
        BoneSlots s = get(p);
        ItemStack old = s.get(slot);
        if (old.isEmpty()) return false;
        p.setData(HonCotRegistry.BONE_SLOTS.get(), s.with(slot, ItemStack.EMPTY));
        if (!p.getInventory().add(old.copy())) p.drop(old.copy(), false);
        changed(p);
        return true;
    }

    private static void changed(ServerPlayer p) {
        com.hukhong.stats.StatService.refresh(p);
        sync(p);
    }

    public static void sync(ServerPlayer p) { PacketDistributor.sendToPlayer(p, new BoneSyncPayload(get(p))); }

    /** Hồn Cốt đang có hiệu lực của kỹ năng fx (cấp đủ yêu cầu phẩm) hoặc null. */
    public static ItemStack active(ServerPlayer p, Bone.Fx fx) {
        int lv = CultivationService.get(p).level();
        for (ItemStack st : get(p).items()) {
            Bone b = BoneSlots.boneOf(st);
            if (b != null && b.fx == fx && HonCotMath.canEquip(lv, HonCotItem.tierOf(st))) return st;
        }
        return null;
    }

    /** Hệ số mạnh của kỹ năng (phẩm x roll) hoặc 0 nếu không mang. */
    public static double power(ServerPlayer p, Bone.Fx fx) {
        ItemStack st = active(p, fx);
        return st == null ? 0 : HonCotMath.tierMult(HonCotItem.tierOf(st)) * HonCotMath.rollMult(HonCotItem.rollOf(st));
    }

    /** Đang mang và đủ cấp dùng Hồn Cốt b. */
    public static boolean has(ServerPlayer p, Bone b) {
        int lv = CultivationService.get(p).level();
        for (ItemStack st : get(p).items()) {
            if (BoneSlots.boneOf(st) == b && HonCotMath.canEquip(lv, HonCotItem.tierOf(st))) return true;
        }
        return false;
    }

    public static int equippedCount(ServerPlayer p) { return get(p).count(); }

    // ------------------------------------------------------------------ chỉ số

    private static AttributeInstance instance(ServerPlayer p, String attr) {
        ResourceLocation id = attr.startsWith("vanilla:") ? ResourceLocation.withDefaultNamespace(attr.substring(8)) : modId(attr);
        return BuiltInRegistries.ATTRIBUTE.getHolder(id).map(p::getAttribute).orElse(null);
    }

    private static String modPath(int slotIdx, String attr) {
        return "honcot/" + slotIdx + "/" + attr.substring(attr.lastIndexOf('.') + 1).replace("vanilla:", "");
    }

    /** Gỡ + áp lại chỉ số Hồn Cốt (gọi từ StatService.refresh). */
    public static void apply(ServerPlayer p) {
        Set<String> attrs = new HashSet<>();
        for (Bone b : Bone.values()) for (Bone.Stat st : b.stats) attrs.add(st.attr());
        for (String a : attrs) {
            AttributeInstance inst = instance(p, a);
            if (inst == null) continue;
            for (int i = 0; i < BoneSlot.COUNT; i++) inst.removeModifier(modId(modPath(i, a)));
        }
        int lv = CultivationService.get(p).level();
        BoneSlots s = get(p);
        for (int i = 0; i < BoneSlot.COUNT; i++) {
            ItemStack st = s.items().get(i);
            Bone b = BoneSlots.boneOf(st);
            if (b == null) continue;
            int tier = HonCotItem.tierOf(st), roll = HonCotItem.rollOf(st);
            if (!HonCotMath.canEquip(lv, tier)) continue;
            for (Bone.Stat stat : b.stats) {
                AttributeInstance inst = instance(p, stat.attr());
                if (inst == null) continue;
                inst.addOrUpdateTransientModifier(new AttributeModifier(modId(modPath(i, stat.attr())),
                        HonCotMath.statValue(stat.base(), tier, roll),
                        stat.mult() ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    // ------------------------------------------------------------------ sự kiện

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }
}
