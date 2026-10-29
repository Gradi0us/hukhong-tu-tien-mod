package com.hukhong.pvp;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.TuTinhText;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.story.npc.StoryNpcRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** NPC "Chu Dau Truong" (StoryNpc id arena_master) o rim san: chuot phai de xem luat + goi y lenh thach dau. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ArenaMaster {
    private ArenaMaster() {}

    public static final String NPC_ID = "arena_master";

    /** Dam bao co dung mot Chu Dau Truong gan san. */
    public static void ensure(ServerLevel ow, BlockPos arena) {
        AABB box = new AABB(arena).inflate(24, 8, 24);
        if (!ow.getEntitiesOfClass(StoryNpc.class, box, n -> NPC_ID.equals(n.npcId())).isEmpty()) return;
        StoryNpc n = StoryNpcRegistry.STORY_NPC.get().create(ow);
        if (n == null) return;
        BlockPos at = arena.offset(0, 0, -(ArenaBuilder.RADIUS - 3));
        n.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        n.finalizeSpawn(ow, ow.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        n.setNpcId(NPC_ID);
        n.setHome(at);
        ow.addFreshEntity(n);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof StoryNpc n) || !NPC_ID.equals(n.npcId())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        ServerLevel ow = p.getServer().overworld();
        if (!DuelManager.open(ow)) { p.sendSystemMessage(Component.translatable("hukhong.pvp.npc.closed")); return; }
        long min = PvpMath.minStake(CultivationService.get(p).level());
        p.sendSystemMessage(Component.translatable("hukhong.pvp.npc.rules", PvpMath.BURN_PCT, PvpMath.ROUNDS, PvpMath.ROUND_TICKS / 1200, TuTinhText.of(min)));
        p.sendSystemMessage(Component.translatable("hukhong.pvp.npc.usage").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/duel "))));
        if (Bracket.registering()) p.sendSystemMessage(Component.translatable("hukhong.pvp.npc.bracket").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel join"))));
    }
}
