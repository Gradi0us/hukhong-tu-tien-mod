package com.hukhong.bicanh;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.honcot.HonCotRegistry;
import com.hukhong.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/** Ánh xạ bảng thưởng thuần sang ItemStack thật (API công khai sẵn có). */
public final class BiCanhLoot {
    private BiCanhLoot() {}

    public static ItemStack toStack(BiCanhRewards.Entry e, Random r) {
        if (e.kind() == BiCanhRewards.Kind.HON_COT)
            return HonCotRegistry.stack(HonCotMath.pickBone(r.nextDouble()), e.sub(),
                    HonCotMath.rollQuality(r.nextDouble(), r.nextDouble()));
        return new ItemStack(item(e), e.count());
    }

    private static Item item(BiCanhRewards.Entry e) {
        return switch (e.kind()) {
            case HON_TINH -> ModItems.HON_TINH.get();
            case LINH_THAO -> AlchemyRegistry.LINH_THAO.get();
            case HUYET_THAM -> AlchemyRegistry.HUYET_THAM.get();
            case HOA_LIEN -> AlchemyRegistry.HOA_LIEN.get();
            case THANH_DIEP -> AlchemyRegistry.THANH_DIEP.get();
            case HOI_XUAN_DAN -> AlchemyRegistry.HOI_XUAN_DAN.get();
            case PHA_CANH_DAN -> switch (e.sub()) {
                case 0 -> ModItems.KHAI_MACH_DAN.get();
                case 1 -> ModItems.NGUNG_NGUYEN_DAN.get();
                case 2 -> ModItems.TIEU_KINH_DAN_V.get();
                case 3 -> ModItems.TIEU_BAN_THAN_DAN.get();
                case 4 -> AlchemyRegistry.THONG_HUYEN_DAN.get();
                default -> AlchemyRegistry.TU_VUONG_DAN.get();
            };
            default -> throw new IllegalArgumentException(e.kind().name());
        };
    }

    public static List<ItemStack> stacks(BiCanhTier t, Random r) {
        return BiCanhRewards.roll(t, r).stream().map(e -> toStack(e, r)).toList();
    }
}
