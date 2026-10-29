package com.hukhong.client.boss;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.PhasedBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer boss theo pha: đổi texture (và glowmask đi kèm, vì layer glow suy từ tên texture) sang biến thể {@code <id>_p2/_p3.png}
 * nếu có, bật/tắt các xương phụ kiện theo {@link BossLook}. Cỡ cơ thể do thuộc tính SCALE (GeckoLib tự nhân getScale()).
 */
public class PhasedBossRenderer<T extends Mob & GeoEntity & PhasedBoss> extends GeoEntityRenderer<T> {
    private final String id;
    private final Map<ResourceLocation, Boolean> exists = new HashMap<>();

    public PhasedBossRenderer(EntityRendererProvider.Context ctx, String id) {
        super(ctx, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id)));
        this.id = id;
    }

    @Override public ResourceLocation getTextureLocation(T e) {
        ResourceLocation base = super.getTextureLocation(e);
        int ph = e.currentPhase();
        if (ph <= 1) return base;
        ResourceLocation v = ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath().replace(".png", "_p" + ph + ".png"));
        return exists.computeIfAbsent(v, k -> Minecraft.getInstance().getResourceManager().getResource(k).isPresent()) ? v : base;
    }

    @Override
    public void preRender(PoseStack ps, T e, BakedGeoModel model, MultiBufferSource bs, VertexConsumer vc,
                          boolean isReRender, float pt, int light, int overlay, int colour) {
        BossLook.apply(id, model, e.currentPhase());
        super.preRender(ps, e, model, bs, vc, isReRender, pt, light, overlay, colour);
    }
}
