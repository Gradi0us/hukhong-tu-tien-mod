package com.hukhong.passive.client;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientCache;
import com.hukhong.client.keybind.ClientSkillState;
import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;
import com.hukhong.passive.MaBoltPayload;
import com.hukhong.passive.PassiveMath;
import com.hukhong.skill.Skill;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** HUD bộ đếm nội tại cạnh thanh kỹ năng + đầu vào tia ma của Ma Tu (chuột trái tay không). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class PassiveClient {
    private PassiveClient() {}

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "passive_hud"), PassiveClient::render);
    }

    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.screen != null || p.isSpectator()) return;
        if (!p.getMainHandItem().isEmpty() || !ClientCache.cultivation().paths().contains(Path.MA)) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return; // vẫn đào block bình thường
        e.setCanceled(true);
        p.swing(InteractionHand.MAIN_HAND);
        PacketDistributor.sendToServer(new MaBoltPayload());
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        if (!com.hukhong.client.ClientConfig.showHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        CultivationData d = ClientCache.cultivation();
        Path active = Skill.skillPath(d, ClientSkillState.page());
        if (active == null) return;
        int x0 = g.guiWidth() / 2 + 91 + 34;
        int y = g.guiHeight() - 20 - 2 - 11 - 9;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x0, y, 0);
        pose.scale(0.8f, 0.8f, 1f);
        int yy = 0;
        for (Path path : Path.values()) {
            if (!d.paths().contains(path)) continue;
            int n;
            int max;
            String fb;
            switch (path) {
                case KIEM -> { n = ClientPassiveState.kiem(); max = PassiveMath.KIEM_EVERY; fb = "Kiếm Ý"; }
                case DAO -> { n = ClientPassiveState.dao(); max = PassiveMath.DAO_EVERY; fb = "Đạo Vận"; }
                case THE -> { n = ClientPassiveState.the(); max = PassiveMath.THE_STACK_CAP; fb = "Kiên Giáp"; }
                case MA -> { n = ClientPassiveState.ma(); max = PassiveMath.MA_EVERY; fb = "Nổ"; }
                default -> { continue; }
            }
            if (path != active && n == 0) continue;
            Component c = Component.translatableWithFallback("hukhong.passive.hud." + path.id, fb).copy().append(" " + n + "/" + max);
            g.drawString(mc.font, c, 0, yy, n > 0 ? path.argb : 0xFF888888, true);
            yy -= 10;
        }
        pose.popPose();
    }
}
