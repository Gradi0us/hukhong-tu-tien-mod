package com.hukhong.client.endgame;

import com.hukhong.HuKhong;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.endgame.MoDaoCombat;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer Mộ Dao chiến đấu: kiếm ở tay hay trong vỏ theo cờ DRAWN đồng bộ từ server. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MoDaoCombatRenderer {
    private MoDaoCombatRenderer() {}

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }
    private static final ResourceLocation GEO = rl("geo/entity/npc_mo_dao_combat.geo.json"),
            TEX = rl("textures/entity/npc_mo_dao_combat.png"), ANIM = rl("animations/entity/npc_mo_dao_combat.animation.json");

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(EndgameRegistry.MO_DAO_COMBAT.get(), c -> new GeoEntityRenderer<MoDaoCombat>(c, new Model()) {
            { this.shadowRadius = 0.5f; }
        });
    }

    static final class Model extends GeoModel<MoDaoCombat> {
        @Override public ResourceLocation getModelResource(MoDaoCombat m) { return GEO; }
        @Override public ResourceLocation getTextureResource(MoDaoCombat m) { return TEX; }
        @Override public ResourceLocation getAnimationResource(MoDaoCombat m) { return ANIM; }

        @Override
        public void setCustomAnimations(MoDaoCombat m, long id, AnimationState<MoDaoCombat> st) {
            boolean drawn = m.drawn();
            getBone("sword").ifPresent(b -> b.setHidden(!drawn));
            getBone("sword_blade").ifPresent(b -> b.setHidden(!drawn));
            getBone("sword_sheathed").ifPresent(b -> b.setHidden(drawn));
        }
    }
}
