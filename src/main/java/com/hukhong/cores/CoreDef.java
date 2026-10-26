package com.hukhong.cores;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

/**
 * Định nghĩa Lõi (datapack: data/hukhong/hukhong/core/*.json).
 * path: "any" hoặc id đạo (kiem/dao/the/ma). values: giá trị theo phẩm Bạch/Hoàng/Tử/Hắc/Hồng (Kim dùng giá trị cuối).
 * Lưu ý: mỗi Lõi chỉ nên có tối đa 1 bị động trên cùng một attribute.
 */
public record CoreDef(String nameKey, String system, String path, CoreTier minTier, List<Passive> passives) {
    public static final ResourceKey<Registry<CoreDef>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "core"));

    public record Passive(ResourceLocation attribute, AttributeModifier.Operation operation, List<Double> values) {
        public static final Codec<Passive> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("attribute").forGetter(Passive::attribute),
                AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                        .forGetter(Passive::operation),
                Codec.DOUBLE.listOf().fieldOf("values").forGetter(Passive::values)
        ).apply(i, Passive::new));

        public double valueAt(CoreTier t) {
            if (values.isEmpty()) return 0;
            return values.get(Math.min(t.ordinal(), values.size() - 1));
        }
    }

    public static final Codec<CoreDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(CoreDef::nameKey),
            Codec.STRING.fieldOf("system").forGetter(CoreDef::system),
            Codec.STRING.optionalFieldOf("path", "any").forGetter(CoreDef::path),
            CoreTier.CODEC.optionalFieldOf("min_tier", CoreTier.BACH).forGetter(CoreDef::minTier),
            Passive.CODEC.listOf().optionalFieldOf("passives", List.of()).forGetter(CoreDef::passives)
    ).apply(i, CoreDef::new));

    public boolean fitsPath(String pathId) { return path.equals("any") || path.equals(pathId); }
}
