package com.hukhong.client.renderer;

import com.hukhong.HuKhong;
import com.hukhong.entity.TamMa;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Tâm Ma: model người chơi, skin chủ nhân (PlayerInfo) hoặc skin mặc định, nhuộm tối mờ + mắt đỏ phát sáng. */
public class TamMaRenderer extends HumanoidMobRenderer<TamMa, PlayerModel<TamMa>> {
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/entity/tam_ma_eyes.png");

    public TamMaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        addLayer(new TintLayer(this));
        addLayer(new EyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TamMa e) {
        return e.getOwnerId().map(id -> {
            var con = Minecraft.getInstance().getConnection();
            PlayerInfo info = con == null ? null : con.getPlayerInfo(id);
            return info != null ? info.getSkin().texture() : net.minecraft.client.resources.DefaultPlayerSkin.get(id).texture();
        }).orElse(net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture());
    }

    /** Thân gốc chỉ còn bóng mờ (translucent); lớp Tint vẽ đè màu tối. */
    @Override
    protected boolean isBodyVisible(TamMa e) { return false; }

    @Override
    protected RenderType getRenderType(TamMa e, boolean body, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(e));
    }

    private static final class TintLayer extends RenderLayer<TamMa, PlayerModel<TamMa>> {
        TintLayer(RenderLayerParent<TamMa, PlayerModel<TamMa>> p) { super(p); }

        @Override
        public void render(PoseStack ps, MultiBufferSource b, int light, TamMa e, float a, float b2, float pt, float age, float yaw, float pitch) {
            VertexConsumer vc = b.getBuffer(RenderType.entityTranslucent(getTextureLocation(e)));
            getParentModel().renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, 0xB8402850); // ARGB: tím đen, ~72% đục
        }
    }

    private static final class EyesLayer extends RenderLayer<TamMa, PlayerModel<TamMa>> {
        EyesLayer(RenderLayerParent<TamMa, PlayerModel<TamMa>> p) { super(p); }

        @Override
        public void render(PoseStack ps, MultiBufferSource b, int light, TamMa e, float a, float b2, float pt, float age, float yaw, float pitch) {
            getParentModel().renderToBuffer(ps, b.getBuffer(RenderType.eyes(EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
