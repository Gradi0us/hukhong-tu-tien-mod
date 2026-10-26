package com.hukhong.story.npc;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server: phiên thoại, điều kiện/hành động, sinh NPC trong thành (chỉ đọc API thành). Chỉ ở Tuyến truyện. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DialogueService {
    private DialogueService() {}

    private record Session(String npc, List<Dialogue.Choice> choices) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    public static final int CITY_RANGE = 120, SCAN_INTERVAL = 100;

    public static QuestData quest(ServerPlayer p) { return p.getData(StoryNpcRegistry.QUEST.get()); }

    public static void setQuest(ServerPlayer p, QuestData d) {
        p.setData(StoryNpcRegistry.QUEST.get(), d);
        PacketDistributor.sendToPlayer(p, new NpcNet.Sync(d));
    }

    public static boolean storyMode(ServerPlayer p) { return p.getData(StoryRegistry.STORY.get()).mode() == StoryData.MODE_STORY; }

    public static void start(ServerPlayer p, StoryNpc npc) {
        Dialogue d = DialogueManager.get(npc.npcId());
        if (d == null) return;
        String node = d.firstNode(p);
        if (node == null) return;
        open(p, d, node);
    }

    private static void open(ServerPlayer p, Dialogue d, String nodeId) {
        Dialogue.Node n = d.nodes().get(nodeId);
        if (n == null) { close(p); return; }
        for (Dialogue.Action a : n.actions()) a.run(p);
        List<Dialogue.Choice> vis = new ArrayList<>();
        for (Dialogue.Choice c : n.choices()) if (c.cond().test(p)) vis.add(c);
        SESSIONS.put(p.getUUID(), new Session(d.npc(), vis));
        List<String> keys = new ArrayList<>();
        for (Dialogue.Choice c : vis) keys.add(c.textKey());
        PacketDistributor.sendToPlayer(p, new NpcNet.View(d.nameKey(), n.text(), keys));
    }

    private static void close(ServerPlayer p) {
        SESSIONS.remove(p.getUUID());
        PacketDistributor.sendToPlayer(p, new NpcNet.View("", List.of(), List.of()));
    }

    public static void choose(ServerPlayer p, int idx) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || !storyMode(p)) return;
        if (idx < 0 || idx >= s.choices().size()) { close(p); return; } // idx -1 = đóng
        Dialogue.Choice c = s.choices().get(idx);
        for (Dialogue.Action a : c.actions()) a.run(p);
        Dialogue d = DialogueManager.get(s.npc());
        if (c.next() == null || d == null) close(p);
        else open(p, d, c.next());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PacketDistributor.sendToPlayer(p, new NpcNet.Sync(quest(p)));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { SESSIONS.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PacketDistributor.sendToPlayer(p, new NpcNet.Sync(quest(p)));
    }

    /** Người chơi ở Tuyến truyện đứng gần thành đã dựng -> bảo đảm NPC của thành đó có mặt. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % SCAN_INTERVAL != 33) return;
        if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD || !storyMode(p)) return;
        CityDef city = CityService.nearest(sl, p.getX(), p.getZ(), CITY_RANGE);
        if (city == null) return;
        BlockPos c = CityService.builtCenter(sl, city);
        if (c == null || !sl.hasChunkAt(c)) return;
        for (Dialogue d : DialogueManager.all()) {
            if (!d.city().equals(city.id())) continue;
            AABB box = new AABB(c).inflate(64, 40, 64);
            boolean present = sl.getEntitiesOfClass(StoryNpc.class, box).stream().anyMatch(n -> n.npcId().equals(d.npc()));
            if (present) continue;
            BlockPos pos = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.offset(3, 0, 3));
            StoryNpc n = StoryNpcRegistry.STORY_NPC.get().create(sl);
            if (n == null) continue;
            n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
            n.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            n.setNpcId(d.npc());
            sl.addFreshEntity(n);
        }
    }
}
