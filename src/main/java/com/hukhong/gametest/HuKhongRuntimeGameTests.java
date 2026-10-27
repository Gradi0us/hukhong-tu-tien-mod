package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.alchemy.DanDoc;
import com.hukhong.alchemy.DanLoBlockEntity;
import com.hukhong.banmenh.BanMenhAttachments;
import com.hukhong.banmenh.BanMenhType;
import com.hukhong.bicanh.BiCanhManager;
import com.hukhong.bicanh.BiCanhRegistry;
import com.hukhong.bicanh.BiCanhTier;
import com.hukhong.chg.ChgRegistry;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.endgame.WorldTier;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.ModEntities;
import com.hukhong.flight.FlightBans;
import com.hukhong.flight.FlightService;
import com.hukhong.skill.SkillService;
import com.hukhong.stats.StatService;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.npc.StoryNpcRegistry;
import com.hukhong.trial.TrialService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** GameTest runtime: bang chung trong the gioi that cho cac he thong truoc day chi co unit test. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class HuKhongRuntimeGameTests {
    private HuKhongRuntimeGameTests() {}

    // ------------------------------------------------------------ helpers

    private static ServerPlayer fake(GameTestHelper h, int level, Set<Path> paths) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-" + level));
        Map<Path, Aptitude> apt = Map.of();
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 2000, 2000, paths, apt, false));
        StatService.refresh(p);
        p.setHealth(p.getMaxHealth());
        return p;
    }

    /** FakePlayerNetHandler bo qua teleport(); thay bang ban co di chuyen that de kiem tra duoc vi tri. */
    private static void movableConnection(ServerPlayer p) {
        var conn = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(), false);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server, conn, p, cookie) {
            @Override public void teleport(double x, double y, double z, float yaw, float pitch) { p.moveTo(x, y, z, yaw, pitch); }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
    }

    private static void place(GameTestHelper h, ServerPlayer p, double rx, double ry, double rz) {
        Vec3 v = h.absoluteVec(new Vec3(rx, ry, rz));
        p.setPos(v.x, v.y, v.z);
        p.setYRot(0f);
        p.setXRot(0f);
        p.setYHeadRot(0f);
    }

    private static <T extends net.minecraft.world.entity.Entity> T spawn(GameTestHelper h, EntityType<T> t, double rx, double ry, double rz) {
        T e = t.create(h.getLevel());
        if (e == null) { h.fail("create null " + t); throw new IllegalStateException(); }
        Vec3 v = h.absoluteVec(new Vec3(rx, ry, rz));
        e.moveTo(v.x, v.y, v.z, 0f, 0f);
        if (e instanceof Mob m) {
            m.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
            GameTestSupport.deterministic(m);
            m.setNoAi(true);
            m.setPersistenceRequired();
        }
        h.getLevel().addFreshEntity(e);
        return e;
    }

    private static Zombie dummy(GameTestHelper h, double rx, double rz) {
        Zombie z = spawn(h, EntityType.ZOMBIE, rx, 1, rz);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);
        z.setHealth(100000);
        z.getAttribute(Attributes.ARMOR).setBaseValue(0);
        return z;
    }

    // ------------------------------------------------------------ 1. entity spawn & kill

    private static void spawnKill(GameTestHelper h, Supplier<? extends EntityType<? extends LivingEntity>> type) {
        LivingEntity e = spawn(h, type.get(), 2, 1, 2);
        if (e.getAttribute(Attributes.MAX_HEALTH) == null) h.fail("thieu MAX_HEALTH: " + type.get());
        if (e.getAttribute(Attributes.MOVEMENT_SPEED) == null) h.fail("thieu MOVEMENT_SPEED: " + type.get());
        if (e.getMaxHealth() <= 0 || e.getHealth() <= 0) h.fail("HP <= 0: " + type.get());
        h.runAfterDelay(5, () -> {
            if (!e.isAlive()) h.fail("chet som truoc khi giet: " + type.get() + " hp=" + e.getHealth() + " removed=" + e.getRemovalReason() + " src=" + e.getLastDamageSource() + " pos=" + e.position());
            e.kill();
            h.runAfterDelay(25, () -> {
                if (e.isAlive()) h.fail("kill() khong giet duoc: " + type.get());
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty") public static void entityHuThi(GameTestHelper h) { spawnKill(h, ModEntities.HU_THI); }
    @GameTest(template = "empty") public static void entityHuCot(GameTestHelper h) { spawnKill(h, ModEntities.HU_COT); }
    @GameTest(template = "empty") public static void entityHuChu(GameTestHelper h) { spawnKill(h, ModEntities.HU_CHU); }
    /** Bug "giet quai Hu Hoa khong co EXP": nguoi choi cap 1 giet HuThi -> EXP phai tang. */
    @GameTest(template = "empty")
    public static void killHuThiGivesExp(GameTestHelper h) {
        ServerPlayer p = fake(h, 1, Set.of(Path.THE));
        place(h, p, 1, 1, 1);
        com.hukhong.entity.HuThi m = spawn(h, ModEntities.HU_THI.get(), 2, 1, 2);
        long before = com.hukhong.core.CultivationService.get(p).exp();
        m.hurt(h.getLevel().damageSources().playerAttack(p), 1_000_000f);
        h.runAfterDelay(5, () -> {
            long after = com.hukhong.core.CultivationService.get(p).exp();
            if (m.isAlive()) h.fail("HuThi chua chet");
            if (after <= before) h.fail("EXP khong tang: " + before + " -> " + after + " mobLv=" + m.huLevel());
            h.succeed();
        });
    }

    @GameTest(template = "empty") public static void entityHuVuong(GameTestHelper h) { spawnKill(h, ModEntities.HU_VUONG); }
    @GameTest(template = "empty") public static void entityTamMa(GameTestHelper h) { spawnKill(h, ModEntities.TAM_MA); }
    @GameTest(template = "empty") public static void entitySuDoKiem(GameTestHelper h) { spawnKill(h, StoryRegistry.SU_DO_KIEM); }
    @GameTest(template = "empty") public static void entitySuDoChu(GameTestHelper h) { spawnKill(h, StoryRegistry.SU_DO_CHU); }
    @GameTest(template = "empty") public static void entitySuDoTe(GameTestHelper h) { spawnKill(h, StoryRegistry.SU_DO_TE); }
    @GameTest(template = "empty") public static void entityChg1(GameTestHelper h) { spawnKill(h, ChgRegistry.BOSSES.get(0)); }
    @GameTest(template = "empty") public static void entityChg2(GameTestHelper h) { spawnKill(h, ChgRegistry.BOSSES.get(1)); }
    @GameTest(template = "empty") public static void entityChg3(GameTestHelper h) { spawnKill(h, ChgRegistry.BOSSES.get(2)); }
    @GameTest(template = "empty") public static void entityChg4(GameTestHelper h) { spawnKill(h, ChgRegistry.BOSSES.get(3)); }
    @GameTest(template = "empty", batch = "bossdie") public static void entityNguyenThuyTichDiet(GameTestHelper h) { spawnKill(h, EndgameRegistry.BOSS); }
    @GameTest(template = "empty", batch = "bossdie") public static void entityDaKhuyet(GameTestHelper h) { spawnKill(h, EndgameRegistry.DA_KHUYET); }
    @GameTest(template = "empty") public static void entityStoryNpc(GameTestHelper h) { spawnKill(h, StoryNpcRegistry.STORY_NPC); }

    // ------------------------------------------------------------ 2. skills Q/E/R/T cho moi dao

    private static boolean cost0(Path path, int slot) { return path == Path.MA && slot == 0; }

    private static void skillCase(GameTestHelper h, Path path, int slot, boolean damages) {
        ServerPlayer p = fake(h, 60, Set.of(path));
        p.setData(BanMenhAttachments.BAN_MENH.get(), p.getData(BanMenhAttachments.BAN_MENH.get())
                .withType(BanMenhType.THANH_PHONG.id(), p.getUUID()));
        place(h, p, 2, 1, 2);
        Zombie z = dummy(h, 2, 3.5);
        SkillService.state(p).stance = true;
        int before = p.getData(ModAttachments.CULTIVATION).resource();
        float hp = z.getHealth();
        SkillService.cast(p, slot);
        int after = p.getData(ModAttachments.CULTIVATION).resource();
        if (cost0(path, slot)) { /* MA_1 mien phi */ } else if (after >= before) h.fail(path + " slot " + slot + " khong tru tai nguyen: " + before + " -> " + after);
        if (path == Path.MA && slot == 2) { // Huyet Hai: sat thuong theo nhip qua SkillScheduler
            h.runAfterDelay(30, () -> {
                if (z.getHealth() >= hp) h.fail("MA R khong gay sat thuong sau 30 tick: " + hp + " -> " + z.getHealth());
                h.succeed();
            });
            return;
        }
        if (damages && z.getHealth() >= hp) h.fail(path + " slot " + slot + " khong gay sat thuong: " + hp + " -> " + z.getHealth());
        h.succeed();
    }

    // E cua Dao/The/Ma va T cua Dao/The/Ma la ho tro/buff (khong gay sat thuong); con lai deu gay sat thuong len muc tieu truoc mat.
    @GameTest(template = "empty") public static void skillKiemQ(GameTestHelper h) { skillCase(h, Path.KIEM, 0, true); }
    @GameTest(template = "empty") public static void skillKiemE(GameTestHelper h) { skillCase(h, Path.KIEM, 1, true); }
    @GameTest(template = "empty") public static void skillKiemR(GameTestHelper h) { skillCase(h, Path.KIEM, 2, true); }
    @GameTest(template = "empty") public static void skillKiemT(GameTestHelper h) { skillCase(h, Path.KIEM, 3, true); }
    @GameTest(template = "empty") public static void skillDaoQ(GameTestHelper h) { skillCase(h, Path.DAO, 0, true); }
    @GameTest(template = "empty") public static void skillDaoE(GameTestHelper h) { skillCase(h, Path.DAO, 1, false); }
    @GameTest(template = "empty") public static void skillDaoR(GameTestHelper h) { skillCase(h, Path.DAO, 2, true); }
    @GameTest(template = "empty") public static void skillDaoT(GameTestHelper h) { skillCase(h, Path.DAO, 3, false); }
    @GameTest(template = "empty") public static void skillTheQ(GameTestHelper h) { skillCase(h, Path.THE, 0, true); }
    @GameTest(template = "empty") public static void skillTheE(GameTestHelper h) { skillCase(h, Path.THE, 1, false); }
    @GameTest(template = "empty") public static void skillTheR(GameTestHelper h) { skillCase(h, Path.THE, 2, true); }
    @GameTest(template = "empty") public static void skillTheT(GameTestHelper h) { skillCase(h, Path.THE, 3, false); }
    @GameTest(template = "empty") public static void skillMaQ(GameTestHelper h) { skillCase(h, Path.MA, 0, true); }
    @GameTest(template = "empty") public static void skillMaE(GameTestHelper h) { skillCase(h, Path.MA, 1, false); }
    @GameTest(template = "empty") public static void skillMaR(GameTestHelper h) { skillCase(h, Path.MA, 2, true); }
    @GameTest(template = "empty") public static void skillMaT(GameTestHelper h) { skillCase(h, Path.MA, 3, false); }

    // ------------------------------------------------------------ 3. bay

    private static ServerPlayer flier(GameTestHelper h, Path path) {
        ServerPlayer p = fake(h, 80, Set.of(path));
        p.setData(BanMenhAttachments.BAN_MENH.get(), p.getData(BanMenhAttachments.BAN_MENH.get())
                .withType(BanMenhType.THANH_PHONG.id(), p.getUUID()));
        place(h, p, 2, 1, 2);
        return p;
    }

    private static void flightCase(GameTestHelper h, Path path, boolean mayfly) {
        ServerPlayer p = flier(h, path);
        FlightService.toggle(p);
        if (!FlightService.isActive(p)) h.fail(path + ": toggle khong bat bay, mode=" + FlightService.modeFor(p) + " banned=" + FlightBans.isFlightBanned(p));
        if (p.getAbilities().mayfly != mayfly) h.fail(path + ": mayfly sau bat = " + p.getAbilities().mayfly + ", ky vong " + mayfly);
        FlightService.toggle(p);
        if (FlightService.isActive(p)) h.fail(path + ": toggle khong tat bay");
        if (p.getAbilities().mayfly) h.fail(path + ": mayfly con sau khi tat");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "flightok") public static void flightKiem(GameTestHelper h) { flightCase(h, Path.KIEM, true); }
    @GameTest(template = "empty", batch = "flightok") public static void flightDao(GameTestHelper h) { flightCase(h, Path.DAO, true); }
    @GameTest(template = "empty", batch = "flightok") public static void flightThe(GameTestHelper h) { flightCase(h, Path.THE, false); }
    @GameTest(template = "empty", batch = "flightok") public static void flightMa(GameTestHelper h) { flightCase(h, Path.MA, true); }

    /** Batch rieng: boss the gioi gan day cam bay cua moi dao (boss bi go sau khi xong de khong anh huong test khac). */
    @GameTest(template = "empty", batch = "flightban")
    public static void flightBannedNearWorldBoss(GameTestHelper h) {
        Zombie boss = spawn(h, EntityType.ZOMBIE, 4, 1, 4);
        boss.addTag(FlightBans.WORLD_BOSS_TAG);
        try {
            for (Path path : Path.values()) {
                ServerPlayer p = flier(h, path);
                if (!FlightBans.isFlightBanned(p)) h.fail(path + ": FlightBans khong nhan boss the gioi");
                FlightService.toggle(p);
                if (FlightService.isActive(p) || p.getAbilities().mayfly) h.fail(path + ": van bay duoc gan world_boss");
            }
        } finally {
            boss.discard();
        }
        h.succeed();
    }

    // ------------------------------------------------------------ 4. Dan Lo + Dan Doc

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void danLoBrewProducesPill(GameTestHelper h) {
        BlockPos rel = new BlockPos(0, 1, 0);
        h.setBlock(rel, AlchemyRegistry.DAN_LO.get());
        DanLoBlockEntity be = (DanLoBlockEntity) h.getBlockEntity(rel);
        if (be == null) { h.fail("khong co BlockEntity Dan Lo"); return; }
        ServerPlayer p = fake(h, 10, Set.of(Path.DAO));
        be.setItem(0, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 2));
        be.setItem(1, new ItemStack(AlchemyRegistry.THANH_DIEP.get(), 1));
        be.setItem(DanLoBlockEntity.FUEL, new ItemStack(Items.COAL, 4));
        be.button(0, p);
        if (be.data.get(2) != 1) { h.fail("brew khong bat dau"); return; }
        BlockPos abs = h.absolutePos(rel);
        var level = h.getLevel();
        var state = level.getBlockState(abs);
        for (int t = 0; t < 420 && be.data.get(2) == 1; t++) {
            int heat = be.data.get(0);
            int center = (be.data.get(3) + be.data.get(4)) / 2;
            if (heat < center - 2) be.button(1, p); else if (heat > center + 2) be.button(2, p);
            DanLoBlockEntity.serverTick(level, abs, state, be);
        }
        if (be.data.get(2) == 1) { h.fail("brew khong ket thuc"); return; }
        ItemStack out = be.getItem(DanLoBlockEntity.OUT);
        if (out.isEmpty()) { h.fail("khong ra dan (phe lieu?) inBand=" + be.data.get(7)); return; }
        if (!out.is(com.hukhong.item.ModItems.KHAI_MACH_DAN.get())) { h.fail("ra sai vat pham: " + out); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pillAddsDanDocAndSurvivesDeath(GameTestHelper h) {
        ServerPlayer p = fake(h, 10, Set.of(Path.DAO));
        p.setHealth(1f);
        ItemStack pill = new ItemStack(AlchemyRegistry.CHI_HUYET_DAN.get(), 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, pill);
        double before = DanDoc.get(p);
        pill.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        double after = DanDoc.get(p);
        if (after <= before) { h.fail("an dan khong cong Dan Doc: " + before + " -> " + after); return; }
        ServerPlayer neo = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-respawn"));
        neo.restoreFrom(p, false);
        double copied = DanDoc.get(neo);
        if (Math.abs(copied - after) > 1e-9) h.fail("Dan Doc mat sau chet (restoreFrom): " + after + " -> " + copied);
        h.succeed();
    }

    // ------------------------------------------------------------ 5. Bi Canh

    /**
     * GameTestServer chi nap 3 dimension vanilla (khong nap dimension datapack hukhong:bi_canh), nen enter() that
     * tra "nodim". Khi do kiem tra phan tra vi tri: dat the RET nhu enter() ghi, dich chuyen di xa roi goi evacuate().
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void biCanhEnterLeaveReturnsPosition(GameTestHelper h) throws Exception {
        ServerPlayer p = fake(h, 10, Set.of(Path.KIEM));
        place(h, p, 2, 1, 2);
        double x = p.getX(), y = p.getY(), z = p.getZ();
        var server = h.getLevel().getServer();
        BiCanhManager.reset();
        if (server.getLevel(BiCanhRegistry.DIM) != null) {
            var dimKey = p.level().dimension();
            String err = BiCanhManager.enter(server, p, BiCanhTier.BACH, List.of(p), true);
            if (err != null) { h.fail("enter loi: " + err); return; }
            if (p.level().dimension() != BiCanhRegistry.DIM) { h.fail("khong o dimension bi canh sau enter"); return; }
            if (!BiCanhManager.inRun(p)) { h.fail("inRun false"); return; }
            BiCanhManager.leave(p, null);
            if (p.level().dimension() != dimKey) { h.fail("khong tro ve dimension cu"); return; }
            if (BiCanhManager.inRun(p)) { h.fail("van inRun sau leave"); return; }
        } else {
            String err = BiCanhManager.enter(server, p, BiCanhTier.BACH, List.of(p), true);
            if (!"hukhong.bicanh.err.nodim".equals(err)) { h.fail("ky vong nodim, nhan " + err); return; }
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("dim", p.level().dimension().location().toString());
            tag.putDouble("x", x); tag.putDouble("y", y); tag.putDouble("z", z);
            p.getPersistentData().put("hukhong_bicanh_ret", tag);
            p.setPos(x + 30, y + 5, z + 30);
            Class<?> run = null;
            for (Class<?> c : BiCanhManager.class.getDeclaredClasses()) if (c.getSimpleName().equals("Run")) run = c;
            var m = BiCanhManager.class.getDeclaredMethod("evacuate", ServerPlayer.class, run, String.class);
            m.setAccessible(true);
            movableConnection(p);
            m.invoke(null, p, null, null);
            if (p.getPersistentData().contains("hukhong_bicanh_ret")) { h.fail("the RET con sau evacuate"); return; }
        }
        if (Math.abs(p.getX() - x) > 0.5 || Math.abs(p.getY() - y) > 0.5 || Math.abs(p.getZ() - z) > 0.5)
            h.fail("vi tri sau leave lech: " + p.position() + " vs " + x + "," + y + "," + z);
        BiCanhManager.reset();
        h.succeed();
    }

    // ------------------------------------------------------------ 6. endgame tier -> HuHoa

    @GameTest(template = "empty")
    public static void worldTierScalesHuHoaStats(GameTestHelper h) {
        var server = h.getLevel().getServer();
        int old = WorldTier.get();
        try {
            WorldTier.force(server, 1);
            var a = spawn(h, ModEntities.HU_THI.get(), 2, 1, 2);
            HuHoa.applyStats(a, 30, 1, 1);
            double hp1 = a.getMaxHealth(), atk1 = a.getAttributeValue(Attributes.ATTACK_DAMAGE);
            WorldTier.force(server, 2);
            var b = spawn(h, ModEntities.HU_THI.get(), 3, 1, 2);
            HuHoa.applyStats(b, 30, 1, 1);
            double hp2 = b.getMaxHealth(), atk2 = b.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (!(hp2 > hp1 * 1.4 && hp2 < hp1 * 1.6)) h.fail("HP tier2 khong ~x1.5: " + hp1 + " -> " + hp2);
            if (!(atk2 > atk1 * 1.2 && atk2 < atk1 * 1.4)) h.fail("ATK tier2 khong ~x1.3: " + atk1 + " -> " + atk2);
        } finally {
            WorldTier.force(server, old);
        }
        h.succeed();
    }

    // ------------------------------------------------------------ 7. Loi Kiep

    @GameTest(template = "empty", timeoutTicks = 3000)
    public static void trialLoiKiepRunsToCompletion(GameTestHelper h) throws Exception {
        ServerPlayer p = fake(h, 90, Set.of(Path.THE));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        place(h, p, 2, 1, 2);
        // dua FakePlayer vao danh sach nguoi choi de TrialService.onTick (tra cuu theo UUID) xu ly nhu nguoi that
        var pl = h.getLevel().getServer().getPlayerList();
        var f = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        f.setAccessible(true);
        @SuppressWarnings("unchecked") Map<UUID, ServerPlayer> map = (Map<UUID, ServerPlayer>) f.get(pl);
        map.put(p.getUUID(), p);
        TrialService.test(p, TrialService.Type.LOI_KIEP);
        if (!TrialService.active(p)) { map.remove(p.getUUID()); h.fail("session khong bat dau"); return; }
        h.onEachTick(() -> {
            if (!TrialService.active(p)) {
                map.remove(p.getUUID());
                if (!p.isAlive()) h.fail("FakePlayer chet trong Loi Kiep");
                if (h.getTick() < 60) h.fail("Loi Kiep ket thuc qua som (" + h.getTick() + " tick): session bi huy thay vi chay het");
                h.succeed();
            }
        });
    }

    // ------------------------------------------------------------ 8. Tam Ma / Tam Thuc

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void trialTamMaMovesToTamThucAndBack(GameTestHelper h) throws Exception {
        ServerPlayer p = fake(h, 90, Set.of(Path.THE));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000);
        p.setHealth(p.getMaxHealth());
        place(h, p, 2, 1, 2);
        var home = p.level().dimension();
        var pl = h.getLevel().getServer().getPlayerList();
        var f = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        f.setAccessible(true);
        @SuppressWarnings("unchecked") Map<UUID, ServerPlayer> map = (Map<UUID, ServerPlayer>) f.get(pl);
        map.put(p.getUUID(), p);
        TrialService.test(p, TrialService.Type.TAM_MA);
        if (!TrialService.active(p)) { map.remove(p.getUUID()); h.fail("session khong bat dau"); return; }
        boolean dimLoaded = h.getLevel().getServer().getLevel(TrialService.TAM_THUC) != null;
        boolean[] seen = {false};
        h.onEachTick(() -> {
            if (!seen[0]) {
                if (dimLoaded ? p.level().dimension() == TrialService.TAM_THUC : h.getTick() > 70) { // GameTestServer chi nap overworld: khi do thu che do du phong
                    seen[0] = true;
                    var tl = p.serverLevel();
                    var boss = tl.getEntities(ModEntities.TAM_MA.get(), e -> e.getTags().contains("hukhong_tam_ma"));
                    if (boss.isEmpty()) { map.remove(p.getUUID()); h.fail("khong co Tam Ma trong Tam Thuc"); return; }
                    boss.get(0).kill();
                } else if (h.getTick() > 200) { map.remove(p.getUUID()); h.fail("khong vao Tam Thuc sau 200 tick; active=" + TrialService.active(p) + " dim=" + p.level().dimension().location() + " tl=" + (h.getLevel().getServer().getLevel(TrialService.TAM_THUC) != null)); }
                return;
            }
            if (!TrialService.active(p)) {
                map.remove(p.getUUID());
                if (p.level().dimension() != home) h.fail("khong tro ve chieu goc: " + p.level().dimension().location());
                else h.succeed();
            }
        });
    }
}
