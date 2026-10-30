package com.hukhong.client.boss;

import com.hukhong.HuKhong;
import com.hukhong.zone.form.HuVuongForm;
import com.hukhong.zone.form.HvForm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer 8 dang Hu Vuong: 1 loai thuc the, model/animation theo khung xuong (hvf_giant|quad|worm|spirit), texture theo dang (hvf_<dang>),
 * ban tint _p2/_p3 theo pha, chi hien xuong phu kien {@code acc_<dang>_<n>} cua dang hien tai (Thien Dien: mat 2 tu pha 2, mat 3 tu pha 3).
 * Chi doc du lieu dong bo cua thuc the (dang, pha) - khong dung CULTIVATION.
 */
public class HuVuongFormRenderer extends GeoEntityRenderer<HuVuongForm> {
    private static final int ACC_MAX = 4;
    private final Map<ResourceLocation, Boolean> exists = new HashMap<>();

    public HuVuongFormRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FormModel());
    }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, path); }

    private static final class FormModel extends GeoModel<HuVuongForm> {
        @Override public ResourceLocation getModelResource(HuVuongForm e) { return rl("geo/entity/hvf_" + e.form().skel.key() + ".geo.json"); }
        @Override public ResourceLocation getTextureResource(HuVuongForm e) { return rl("textures/entity/" + e.form().textureId() + ".png"); }
        @Override public ResourceLocation getAnimationResource(HuVuongForm e) { return rl("animations/entity/hvf_" + e.form().skel.key() + ".animation.json"); }
    }

    @Override public ResourceLocation getTextureLocation(HuVuongForm e) {
        ResourceLocation base = super.getTextureLocation(e);
        int ph = e.currentPhase();
        if (ph <= 1) return base;
        ResourceLocation v = ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath().replace(".png", "_p" + ph + ".png"));
        return exists.computeIfAbsent(v, k -> Minecraft.getInstance().getResourceManager().getResource(k).isPresent()) ? v : base;
    }

    @Override
    public void preRender(PoseStack ps, HuVuongForm e, BakedGeoModel model, MultiBufferSource bs, VertexConsumer vc,
                          boolean isReRender, float pt, int light, int overlay, int colour) {
        int ph = e.currentPhase();
        BossLook.apply("hvf", model, ph);
        HvForm cur = e.form();
        for (HvForm f : HvForm.values()) {
            for (int n = 1; n <= ACC_MAX; n++) {
                String bone = "acc_" + f.key() + "_" + n;
                boolean show = f == cur && (!f.multiFace() || faceVisible(n, ph));
                model.getBone(bone).ifPresent(b -> b.setHidden(!show));
            }
        }
        super.preRender(ps, e, model, bs, vc, isReRender, pt, light, overlay, colour);
    }

    /** Thien Dien: bone 1 = mat 2 (pha >= 2), bone 2 = mat 3 (pha 3), 3-4 luon hien (mu mien + vong chu). */
    private static boolean faceVisible(int n, int phase) {
        return switch (n) { case 1 -> phase >= 2; case 2 -> phase >= 3; default -> true; };
    }
}
