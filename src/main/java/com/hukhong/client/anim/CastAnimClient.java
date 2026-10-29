package com.hukhong.client.anim;

import com.hukhong.HuKhong;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * Client: đăng ký 1 lớp animation "cast" cho mọi người chơi và phát tư thế khi nhận {@link CastAnimPayload}.
 * <p>Ưu tiên lớp = {@link #PRIORITY} (1000): trên các lớp tư thế cầm vũ khí của Better Combat (1..4) nhưng DƯỚI lớp
 * đòn đánh của Better Combat (2000) -> khi Better Combat đang diễn đòn đánh thì đòn đánh thắng, không bị tư thế cast đè.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CastAnimClient {
    private CastAnimClient() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "cast");
    public static final int PRIORITY = 1000;
    private static boolean warned;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, PRIORITY,
                player -> new ModifierLayer<IAnimation>()));
        LOG.info("[hukhong] cast animation layer registered (priority {})", PRIORITY);
    }

    /** Phát tư thế cho người chơi có entityId (bỏ qua nếu không phải người chơi client hoặc đã không còn trong thế giới). */
    @SuppressWarnings("unchecked")
    public static void play(int entityId, int animOrdinal) {
        try {
            Minecraft mc = Minecraft.getInstance();
            CastAnim a = CastAnim.of(animOrdinal);
            if (mc.level == null || a == null) return;
            Entity en = mc.level.getEntity(entityId);
            if (!(en instanceof AbstractClientPlayer ap) || !ap.isAlive()) return;
            ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(ap).get(LAYER);
            if (layer == null) return;
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE),
                    new KeyframeAnimationPlayer(CastAnimLibrary.get(a)));
            LOG.debug("[hukhong] cast anim {} -> {} (active={})", a, ap.getName().getString(), layer.isActive());
        } catch (RuntimeException ex) {
            if (!warned) { // chỉ log 1 lần, không làm hỏng gameplay vì một tư thế
                warned = true;
                LOG.warn("[hukhong] cast animation failed", ex);
            }
        }
    }
}
