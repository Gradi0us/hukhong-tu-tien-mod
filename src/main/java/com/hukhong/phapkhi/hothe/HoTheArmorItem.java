package com.hukhong.phapkhi.hothe;

import com.hukhong.phapkhi.PhapKhiTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;

import java.util.List;

/** Một món Pháp Khí hộ thể (loại x phẩm). Là ArmorItem để render lớp giáp; chỉ số thêm nằm ở getDefaultAttributeModifiers. */
public final class HoTheArmorItem extends ArmorItem {
    public final HoTheKind kind;
    public final PhapKhiTier tier;
    private ItemAttributeModifiers mods;

    public HoTheArmorItem(HoTheKind kind, PhapKhiTier tier, Holder<ArmorMaterial> material, Item.Properties props) {
        super(material, kind.type, props.durability(kind.type.getDurability(20)));
        this.kind = kind;
        this.tier = tier;
    }

    /** Giáp vanilla của material + chỉ số riêng của món (máu, độ cứng, kháng đẩy, tốc chạy). */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        if (mods == null) {
            ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
            for (ItemAttributeModifiers.Entry e : super.getDefaultAttributeModifiers().modifiers())
                b.add(e.attribute(), e.modifier(), e.slot());
            EquipmentSlotGroup grp = EquipmentSlotGroup.bySlot(kind.slot());
            int t = tier.ordinal();
            switch (kind) {
                case HO_TAM_GIAP -> b.add(Attributes.MAX_HEALTH, mod("max_health", HoTheKind.HO_HP[t], AttributeModifier.Operation.ADD_VALUE), grp);
                case KIM_CANG_HO_THOI -> {
                    b.add(Attributes.ARMOR_TOUGHNESS, mod("toughness", HoTheKind.KIM_TOUGHNESS[t], AttributeModifier.Operation.ADD_VALUE), grp);
                    b.add(Attributes.KNOCKBACK_RESISTANCE, mod("knockback", HoTheKind.KIM_KNOCKBACK[t], AttributeModifier.Operation.ADD_VALUE), grp);
                }
                case PHI_VAN_NGOA -> b.add(Attributes.MOVEMENT_SPEED, mod("speed", HoTheKind.PHI_SPEED[t], AttributeModifier.Operation.ADD_MULTIPLIED_BASE), grp);
                default -> { }
            }
            mods = b.build();
        }
        return mods;
    }

    private AttributeModifier mod(String path, double amount, AttributeModifier.Operation op) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("hukhong", "hothe/" + kind.id + "_" + path), amount, op);
    }

    /** Món hộ thể {@code k} đang mặc trên người chơi, hoặc null. */
    public static HoTheArmorItem worn(Player p, HoTheKind k) {
        ItemStack st = p.getItemBySlot(k.slot());
        return st.getItem() instanceof HoTheArmorItem h && h.kind == k ? h : null;
    }

    /** Nhân chi phí lách: 1 nếu không mặc Phi Vân Ngoa, còn lại theo phẩm 0.9 .. 0.6. */
    public static float dodgeCostMult(Player p) {
        HoTheArmorItem h = worn(p, HoTheKind.PHI_VAN_NGOA);
        return h == null ? 1f : (float) HoTheKind.phiDodgeMult(h.tier.ordinal());
    }

    public MutableComponent tierName() {
        return Component.translatableWithFallback("hukhong.phapkhi.tier." + tier.id, tier.vi + " Phẩm").withColor(tier.rgb);
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent n = Component.translatableWithFallback(kind.langKey(), kind.vi).withColor(tier.rgb);
        return n.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(tierName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback(kind.tipKey(), kind.id).withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatableWithFallback("hukhong.hothe.tip.only", "Chỉ mặc được pháp khí hộ thể")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
