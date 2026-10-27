package com.hukhong.client.voidfx;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;

/** Bầu trời Hư Không: gradient tối, tinh vân tím trôi, vết nứt xa, sương mù tím; shard bay lơ lửng (hạt vanilla). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class VoidSkyEffects extends DimensionSpecialEffects {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "void");
    private static final ResourceLocation NEBULA = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/environment/void_nebula.png");
    private static final ResourceLocation CRACKS = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/environment/void_cracks.png");

    public VoidSkyEffects() { super(Float.NaN, false, SkyType.NONE, true, false); }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent e) { e.register(ID, new VoidSkyEffects()); }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) { return new Vec3(0.09, 0.06, 0.17); }

    @Override
    public boolean isFoggyAt(int x, int y) { return true; }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f frustumMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        float t = (ticks + partialTick);
        drawCube(frustumMatrix, NEBULA, t * 0.4f, 255, 255, 255, 255);
        drawCube(frustumMatrix, CRACKS, -t * 0.15f + 90f, 200, 170, 255, 190);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        return true;
    }

    private static void drawCube(Matrix4f frustum, ResourceLocation tex, float yawDeg, int r, int g, int b, int a) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        PoseStack ps = new PoseStack();
        ps.mulPose(frustum);
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawDeg));
        for (int i = 0; i < 6; i++) {
            ps.pushPose();
            switch (i) {
                case 1 -> ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
                case 2 -> ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90f));
                case 3 -> ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f));
                case 4 -> ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90f));
                case 5 -> ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90f));
                default -> { }
            }
            Matrix4f m = ps.last().pose();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bb.addVertex(m, -100f, -100f, -100f).setUv(0f, 0f).setColor(r, g, b, a);
            bb.addVertex(m, -100f, -100f, 100f).setUv(0f, 1f).setColor(r, g, b, a);
            bb.addVertex(m, 100f, -100f, 100f).setUv(1f, 1f).setColor(r, g, b, a);
            bb.addVertex(m, 100f, -100f, -100f).setUv(1f, 0f).setColor(r, g, b, a);
            BufferUploader.drawWithShader(bb.buildOrThrow());
            ps.popPose();
        }
    }

    private static boolean isVoid(ClientLevel l) {
        String p = l.dimension().location().getPath();
        return l.dimension().location().getNamespace().equals(HuKhong.MODID) && (p.equals("bicanh") || p.equals("hu_khong_ban_nguyen"));
    }

    /** Shard / bụi tím lơ lửng quanh người chơi. */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.player == null || mc.isPaused() || !isVoid(l) || l.random.nextInt(3) != 0) return;
        double x = mc.player.getX() + Mth.nextDouble(l.random, -24, 24);
        double y = mc.player.getY() + Mth.nextDouble(l.random, -6, 14);
        double z = mc.player.getZ() + Mth.nextDouble(l.random, -24, 24);
        l.addParticle(l.random.nextBoolean() ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0);
    }
}
