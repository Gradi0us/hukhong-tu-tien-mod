package com.hukhong.living;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.auction.Tx;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.city.ambient.AmbientRegistry;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import com.hukhong.skillbook.SkillBookRewards;
import com.hukhong.skillbook.SkillGrade;
import com.hukhong.story.world.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Sinh nhiem vu ngau nhien (10 mau) va theo doi tien do. Thuong Tu Tinh tinh theo cap; thien cam + phe tang khi xong.
 * Co che: DEN-NOI (RUMOR, ESCORT/GUIDE, GUARD_NIGHT), GIET (HUNT, ELITE, STOLEN), NOP-DO (GATHER, STOLEN buoc 2), GAP-NPC (DELIVER, FIND_LOST).
 * NPC/quai gan nhiem vu mang tag hk_q:ID, persistent, duoc don khi nhiem vu ket thuc. Dung truc tiep tu {@link NpcLife}.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class QuestService {
    private QuestService() {}

    public static final String TAG = "hk_q:";
    public static final int NAMES = 8;

    private static Item[] herbs() {
        return new Item[]{AlchemyRegistry.LINH_THAO.get(), AlchemyRegistry.THANH_DIEP.get(), AlchemyRegistry.HUYET_THAM.get(), AlchemyRegistry.HOA_LIEN.get()};
    }

    public static List<Quest> quests(ServerPlayer p) { return LivingData.get(p.server).of(p.getUUID()).quests; }

    // ---------------------------------------------------------------- sinh

    /** Sinh mot nhiem vu (chua nhan). origin = noi giao nhiem vu; force = ep mau (null = ngau nhien, khong trung mau dang lam). */
    public static Quest generate(ServerPlayer p, Random r, BlockPos origin, UUID giver, boolean special, Quest.Tmpl force) {
        List<Quest> have = quests(p);
        Quest.Tmpl t = force;
        if (t == null) {
            List<Quest.Tmpl> pool = new ArrayList<>(List.of(Quest.Tmpl.values()));
            for (Quest q : have) pool.remove(q.tmpl);
            if (pool.isEmpty()) pool = new ArrayList<>(List.of(Quest.Tmpl.values()));
            t = pool.get(r.nextInt(pool.size()));
        }
        int lv = CultivationService.get(p).level();
        Quest q = new Quest(Long.toString(r.nextLong() & 0xFFFFFFL, 36), t);
        q.origin = origin;
        q.giver = giver == null ? new UUID(0, 0) : giver;
        q.special = special;
        q.giverName = r.nextInt(NAMES);
        q.expire = p.level().getGameTime() + t.minutes * 1200L;
        int[] o;
        switch (t) {
            case ESCORT -> { o = QuestMath.offset(r, 60, 110); q.target = origin.offset(o[0], 0, o[1]); }
            case GUIDE -> { o = QuestMath.offset(r, 130, 200); q.target = origin.offset(o[0], 0, o[1]); }
            case DELIVER -> { o = QuestMath.offset(r, 70, 120); q.target = origin.offset(o[0], 0, o[1]); }
            case FIND_LOST -> { o = QuestMath.offset(r, 80, 140); q.target = origin.offset(o[0], 0, o[1]); }
            case ELITE -> { o = QuestMath.offset(r, 60, 100); q.target = origin.offset(o[0], 0, o[1]); }
            case STOLEN -> { o = QuestMath.offset(r, 60, 100); q.target = origin.offset(o[0], 0, o[1]); }
            case RUMOR -> { o = QuestMath.offset(r, 80, 140); q.target = origin.offset(o[0], 0, o[1]); }
            case GUARD_NIGHT -> { q.target = origin; q.need = 60; }
            case GATHER -> {
                Item[] h = herbs();
                q.item = BuiltInRegistries.ITEM.getKey(h[r.nextInt(h.length)]).toString();
                q.need = QuestMath.count(r, 6, 10, lv);
            }
            case HUNT -> q.need = QuestMath.count(r, 5, 9, lv);
        }
        q.reward = QuestMath.reward(lv, t.mult, special);
        return q;
    }

    /** Nhan nhiem vu: kiem tra gioi han, sinh NPC dong hanh/thu. false neu day. */
    public static boolean accept(ServerPlayer p, Quest q) {
        List<Quest> have = quests(p);
        if (have.size() >= LivingData.MAX_QUESTS) return false;
        have.add(q);
        LivingData.get(p.server).setDirty();
        ServerLevel sl = p.serverLevel();
        if (q.tmpl == Quest.Tmpl.ESCORT || q.tmpl == Quest.Tmpl.GUIDE) {
            AmbientNpc n = spawnNpc(sl, p.blockPosition().offset(2, 0, 1), q, AmbientNpc.CITIZEN);
            if (n != null) q.entity = n.getUUID();
        } else if (q.tmpl == Quest.Tmpl.DELIVER) {
            ItemStack letter = new ItemStack(Items.PAPER);
            letter.set(DataComponents.CUSTOM_NAME, Tx.t("hukhong.living.letter", "Thư gửi nhờ", "Letter to deliver"));
            giveItem(p, letter);
        }
        LivingData.get(p.server).setDirty();
        return true;
    }

    static void giveItem(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    // ---------------------------------------------------------------- mo ta

    public static Component name(Quest q) { return Tx.t("hukhong.living.name." + q.giverName, NAME_VI[q.giverName % NAMES], NAME_EN[q.giverName % NAMES]); }

    private static final String[] NAME_VI = {"Lão Trương", "Thím Hai", "Chú Ba Cảng", "Cô Liễu", "Anh Hổ", "Bà Lục", "Thợ Rèn Thạch", "Tiểu Mai"};
    private static final String[] NAME_EN = {"Old Truong", "Aunt Hai", "Uncle Ba the Docker", "Miss Lieu", "Brother Ho", "Granny Luc", "Smith Thach", "Little Mai"};

    public static Component herbName(Quest q) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.item.isEmpty() ? "minecraft:air" : q.item));
        return new ItemStack(it).getHoverName();
    }

    /** Mo ta ngan co tien do. */
    public static Component describe(Quest q, ServerPlayer p) {
        Component who = name(q);
        int dist = (int) QuestMath.distXZ(p.getBlockX(), p.getBlockZ(), q.target.getX(), q.target.getZ());
        String dir = direction(p, q.target);
        Component a = switch (q.tmpl) {
            case ESCORT -> Tx.t("hukhong.living.q.escort", "%s nhờ bạn hộ tống một người dân đến nơi cách %s block về hướng %s.", "%s asks you to escort a villager %s blocks %s.", who, dist, dir);
            case GUIDE -> Tx.t("hukhong.living.q.guide", "%s nhờ bạn dẫn một lữ khách lạc đường đến nơi cách %s block về hướng %s.", "%s asks you to guide a lost traveller %s blocks %s.", who, dist, dir);
            case DELIVER -> Tx.t("hukhong.living.q.deliver", "%s nhờ bạn đưa thư cho người ở cách %s block về hướng %s.", "%s asks you to deliver a letter %s blocks %s.", who, dist, dir);
            case FIND_LOST -> Tx.t("hukhong.living.q.find_lost", "%s nhờ bạn tìm người thân đi lạc, nghe nói ở cách %s block về hướng %s.", "%s asks you to find a missing relative, last seen %s blocks %s.", who, dist, dir);
            case GATHER -> Tx.t("hukhong.living.q.gather", "%s cần %s %s. Mang về giao cho dân cư gần đó (%s/%s).", "%s needs %s x %s. Hand them to a local (%s/%s).", who, q.need, herbName(q), countHave(p, q), q.need);
            case HUNT -> Tx.t("hukhong.living.q.hunt", "%s nhờ bạn diệt %s quái vật quanh vùng (%s/%s).", "%s asks you to slay %s monsters nearby (%s/%s).", who, q.need, q.have, q.need);
            case ELITE -> Tx.t("hukhong.living.q.elite", "%s nhờ bạn hạ một tên hung đồ cách %s block về hướng %s.", "%s asks you to kill an elite brigand %s blocks %s.", who, dist, dir);
            case RUMOR -> Tx.t("hukhong.living.q.rumor", "%s nghe đồn có chuyện lạ cách %s block về hướng %s. Hãy đến điều tra.", "%s heard a strange rumor %s blocks %s. Go and investigate.", who, dist, dir);
            case GUARD_NIGHT -> Tx.t("hukhong.living.q.guard_night", "%s nhờ bạn canh ruộng ban đêm: ở quanh đây %s giây khi trời tối (%s/%s).", "%s asks you to guard the field at night: stay nearby for %s seconds after dark (%s/%s).", who, q.need, q.have, q.need);
            case STOLEN -> q.stage == 0
                    ? Tx.t("hukhong.living.q.stolen0", "%s bị trộm mất đồ. Tên trộm trốn cách %s block về hướng %s, hãy hạ hắn.", "%s was robbed. The thief hides %s blocks %s, take him down.", who, dist, dir)
                    : Tx.t("hukhong.living.q.stolen1", "Bạn đã lấy lại đồ cho %s. Trả lại cho dân cư gần đó.", "You recovered the goods for %s. Hand them to a local.", who);
        };
        return a;
    }

    public static String direction(ServerPlayer p, BlockPos t) {
        double dx = t.getX() - p.getX(), dz = t.getZ() - p.getZ();
        double ang = Math.toDegrees(Math.atan2(dz, dx));
        String[] n = {"Đông", "Đông Nam", "Nam", "Tây Nam", "Tây", "Tây Bắc", "Bắc", "Đông Bắc"};
        return n[Math.floorMod((int) Math.round(ang / 45.0), 8)];
    }

    static int countHave(ServerPlayer p, Quest q) {
        if (q.item.isEmpty()) return 0;
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.item));
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(it)) n += s.getCount();
        return n;
    }

    // ---------------------------------------------------------------- ket thuc

    public static void complete(ServerPlayer p, Quest q) {
        if (!quests(p).remove(q)) return;
        cleanup(p.serverLevel(), q);
        Wallet.give(p, q.reward);
        if (p.connection != null) CultivationService.addExp(p, Math.max(1L, q.reward / 20));
        UUID g = q.giver;
        if (!new UUID(0, 0).equals(g)) AffectionService.change(p, g, Faction.THANH, q.special ? 8 : 5);
        else AffectionService.change(p, AffectionService.factionKey(Faction.THANH), 2);
        if (q.special) SkillBookRewards.give(p, SkillBookRewards.stack(new Random(p.getRandom().nextLong()),
                p.getRandom().nextInt(3) == 0 ? SkillGrade.HUYEN : SkillGrade.PHAM, null));
        LivingData d = LivingData.get(p.server);
        d.of(p.getUUID()).done++;
        d.setDirty();
        p.sendSystemMessage(Tx.t("hukhong.living.done", "Hoàn thành nhiệm vụ của %s! Nhận %s.", "Quest for %s complete! Received %s.", name(q), TuTinhText.of(q.reward)));
    }

    public static void fail(ServerPlayer p, Quest q, String why) {
        if (!quests(p).remove(q)) return;
        cleanup(p.serverLevel(), q);
        LivingData.get(p.server).setDirty();
        p.sendSystemMessage(Tx.t("hukhong.living.failed", "Nhiệm vụ của %s thất bại (%s).", "Quest for %s failed (%s).", name(q), why));
    }

    static void cleanup(ServerLevel sl, Quest q) {
        String tag = TAG + q.id;
        for (ServerLevel l : sl.getServer().getAllLevels()) {
            if (q.entity != null) {
                Entity e = l.getEntity(q.entity);
                if (e != null) e.discard();
            }
        }
        // thu con lai theo tag (quai/thu da spawn nhung chua luu uuid)
        java.util.List<Entity> mine = new ArrayList<>(); // snapshot: discard trong luc duyet lam hong iterator (NPE e == null)
        for (Entity e : sl.getAllEntities()) if (e != null && e.getTags().contains(tag)) mine.add(e);
        for (Entity e : mine) e.discard();
    }

    // ---------------------------------------------------------------- tuong tac / giet

    /** Nguoi choi tuong tac voi NPC dan cu: giao thu, tim nguoi, nop do. true neu da xu ly mot nhiem vu. */
    public static boolean interact(ServerPlayer p, AmbientNpc npc) {
        for (Quest q : new ArrayList<>(quests(p))) {
            boolean tagged = npc.getTags().contains(TAG + q.id);
            switch (q.tmpl) {
                case DELIVER, FIND_LOST -> { if (tagged) { complete(p, q); return true; } }
                case GATHER -> { if (nearOrigin(npc, q) && turnInGather(p, q)) return true; }
                case STOLEN -> { if (q.stage == 1 && nearOrigin(npc, q) && turnInStolen(p, q)) return true; }
                default -> { }
            }
        }
        return false;
    }

    private static boolean nearOrigin(AmbientNpc npc, Quest q) { return npc.blockPosition().distSqr(q.origin) < 90 * 90; }

    /** Nop thao duoc (du so luong thi tru va hoan thanh). */
    public static boolean turnInGather(ServerPlayer p, Quest q) {
        if (q.tmpl != Quest.Tmpl.GATHER || countHave(p, q) < q.need) return false;
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.item));
        int left = q.need;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (!s.is(it)) continue;
            int t = Math.min(left, s.getCount());
            s.shrink(t);
            left -= t;
        }
        complete(p, q);
        return true;
    }

    static boolean turnInStolen(ServerPlayer p, Quest q) {
        for (ItemStack s : p.getInventory().items) {
            CustomData cd = s.get(DataComponents.CUSTOM_DATA);
            if (cd != null && cd.copyTag().getString("hk_stolen").equals(q.id)) { s.shrink(1); complete(p, q); return true; }
        }
        return false;
    }

    /** Nguoi choi ha mot sinh vat: dem HUNT, hoan thanh ELITE, buoc 1 cua STOLEN. */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        for (Quest q : new ArrayList<>(quests(p))) {
            boolean tagged = victim.getTags().contains(TAG + q.id);
            switch (q.tmpl) {
                case HUNT -> {
                    if (victim instanceof Enemy && !tagged) {
                        q.have++;
                        LivingData.get(p.server).setDirty();
                        if (q.have >= q.need) complete(p, q);
                        else p.displayClientMessage(Tx.t("hukhong.living.progress", "Nhiệm vụ: %s/%s", "Quest: %s/%s", q.have, q.need), true);
                    }
                }
                case ELITE -> { if (tagged) complete(p, q); }
                case STOLEN -> {
                    if (tagged && q.stage == 0) {
                        q.stage = 1;
                        CompoundTag t = new CompoundTag();
                        t.putString("hk_stolen", q.id);
                        ItemStack box = new ItemStack(Items.AMETHYST_SHARD);
                        box.set(DataComponents.CUSTOM_NAME, Tx.t("hukhong.living.stolen_box", "Hộp gấm bị trộm", "Stolen jewel box"));
                        box.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
                        giveItem(p, box);
                        LivingData.get(p.server).setDirty();
                        p.sendSystemMessage(describe(q, p));
                    }
                }
                default -> { }
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (e.getSource().getEntity() instanceof ServerPlayer p) onKill(p, e.getEntity());
    }

    // ---------------------------------------------------------------- spawn

    static AmbientNpc spawnNpc(ServerLevel sl, BlockPos pos, Quest q, int role) {
        AmbientNpc n = AmbientRegistry.AMBIENT.get().create(sl);
        if (n == null) return null;
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        n.setup(role, sl.random.nextInt(AmbientRegistry.SKINS), pos, 24);
        n.addTag(TAG + q.id);
        n.setPersistenceRequired();
        sl.addFreshEntity(n);
        return n;
    }

    static BlockPos ground(ServerLevel sl, BlockPos t) {
        return new BlockPos(t.getX(), sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, t.getX(), t.getZ()), t.getZ());
    }

    static Mob spawnBrigand(ServerLevel sl, BlockPos pos, Quest q, boolean elite, int level) {
        Mob m = EntityType.VINDICATOR.create(sl);
        if (m == null) return null;
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        double hp = elite ? 60 + level * 3.0 : 26 + level * 1.0;
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        m.setHealth((float) hp);
        m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        m.setCustomName(elite ? Tx.t("hukhong.living.elite", "Hung đồ cầm đầu", "Brigand chief") : Tx.t("hukhong.living.thief", "Tên trộm", "Thief"));
        m.setCustomNameVisible(true);
        if (elite) m.setGlowingTag(true);
        m.addTag(TAG + q.id);
        m.setPersistenceRequired();
        sl.addFreshEntity(m);
        q.entity = m.getUUID();
        return m;
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % 20 != 7) return;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            List<Quest> qs = quests(p);
            if (qs.isEmpty()) continue;
            for (Quest q : new ArrayList<>(qs)) {
                try { tick(p, q); } catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().error("quest tick {}: {}", q.id, ex.toString(), ex); }
            }
        }
    }

    /** Mot nhip (1 giay) cho mot nhiem vu. Cong khai de GameTest goi truc tiep. */
    public static void tick(ServerPlayer p, Quest q) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (now > q.expire) { fail(p, q, "hết hạn"); return; }
        double dxz = QuestMath.distXZ(p.getBlockX(), p.getBlockZ(), q.target.getX(), q.target.getZ());
        switch (q.tmpl) {
            case ESCORT, GUIDE -> {
                Entity ent = q.entity == null ? null : sl.getEntity(q.entity);
                if (!(ent instanceof AmbientNpc n)) { // mat dau (chunk): sinh lai canh nguoi choi
                    AmbientNpc nn = spawnNpc(sl, p.blockPosition().offset(2, 0, 1), q, AmbientNpc.CITIZEN);
                    if (nn != null) q.entity = nn.getUUID();
                    return;
                }
                double d = n.distanceTo(p);
                if (d > 48) { n.teleportTo(p.getX() + 2, p.getY(), p.getZ() + 1); d = n.distanceTo(p); }
                else if (d > (q.tmpl == Quest.Tmpl.GUIDE ? 9 : 5)) n.getNavigation().moveTo(p, 1.15);
                if (dxz < 9 && d < 14) complete(p, q);
            }
            case DELIVER, FIND_LOST -> {
                if (q.entity == null || sl.getEntity(q.entity) == null) {
                    if (dxz < 80) {
                        BlockPos g = ground(sl, q.target);
                        if (sl.hasChunkAt(g)) {
                            AmbientNpc n = spawnNpc(sl, g, q, q.tmpl == Quest.Tmpl.FIND_LOST ? AmbientNpc.CHILD : AmbientNpc.CITIZEN);
                            if (n != null) q.entity = n.getUUID();
                        }
                    }
                }
            }
            case ELITE -> {
                if (q.entity == null || sl.getEntity(q.entity) == null) {
                    if (dxz < 80 && sl.hasChunkAt(ground(sl, q.target)) && q.have == 0) {
                        spawnBrigand(sl, ground(sl, q.target), q, true, CultivationService.get(p).level());
                        q.have = 1;
                    }
                }
            }
            case STOLEN -> {
                if (q.stage == 0 && (q.entity == null || sl.getEntity(q.entity) == null) && q.have == 0 && dxz < 80 && sl.hasChunkAt(ground(sl, q.target))) {
                    spawnBrigand(sl, ground(sl, q.target), q, false, CultivationService.get(p).level());
                    q.have = 1;
                }
            }
            case RUMOR -> { if (dxz < 7) { sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 12, 0.5, 0.5, 0.5, 0.1); complete(p, q); } }
            case GUARD_NIGHT -> {
                if (NpcSchedule.isNight(sl.getDayTime()) && dxz < 24) {
                    q.have++;
                    if (q.have % 20 == 0) spawnRaider(sl, p, q);
                    if (q.have >= q.need) complete(p, q);
                } else if (q.have > 0 && q.have % 10 == 0) {
                    p.displayClientMessage(Tx.t("hukhong.living.guard_hint", "Hãy ở gần ruộng khi trời tối.", "Stay near the field after dark."), true);
                }
            }
            default -> { }
        }
    }

    private static void spawnRaider(ServerLevel sl, ServerPlayer p, Quest q) {
        int n = 0;
        for (Entity e : sl.getEntities(EntityType.ZOMBIE, z -> z.getTags().contains(TAG + q.id))) n++;
        if (n >= 2) return;
        net.minecraft.world.entity.monster.Zombie z = EntityType.ZOMBIE.create(sl);
        if (z == null) return;
        BlockPos g = ground(sl, q.target.offset(sl.random.nextInt(11) - 5, 0, sl.random.nextInt(11) - 5));
        z.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0, 0);
        z.addTag(TAG + q.id);
        z.setCustomName(Tx.t("hukhong.living.raider", "Kẻ phá ruộng", "Field raider"));
        sl.addFreshEntity(z);
        z.setTarget(p);
    }
}
