package com.hukhong.story.kyngo;

import com.hukhong.HuKhong;
import com.hukhong.creature.LinhThu;
import com.hukhong.currency.Wallet;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Kỳ Ngộ: sinh thực thể đúng loại, lựa chọn đúng token kết thúc/thưởng, token sai bị từ chối. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class KyNgoGameTests {
    private KyNgoGameTests() {}

    private static final BlockPos REL = new BlockPos(2, 2, 2);

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
    }

    private static ServerPlayer player(GameTestHelper h, BlockPos abs) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "hk-kyngo"));
        p.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static int alive(KyNgoInstance i) {
        int n = 0;
        for (UUID u : i.entities) {
            Entity e = i.level.getEntity(u);
            if (e != null && e.isAlive()) n++;
        }
        return n;
    }

    @GameTest(template = "empty")
    public static void allFourTypesSpawnEntities(GameTestHelper h) {
        floor(h);
        BlockPos abs = h.absolutePos(REL);
        ServerPlayer p = player(h, abs);
        for (KyNgoType t : KyNgoType.values()) {
            KyNgoInstance i = KyNgoService.startAt(p, t, abs);
            if (alive(i) == 0) { h.fail("loai " + t + " khong sinh thuc the nao"); return; }
            KyNgoService.finish(i);
            if (alive(i) != 0) { h.fail("loai " + t + " khong don sach"); return; }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldManLeaveWithRightTokenEnds(GameTestHelper h) {
        floor(h);
        BlockPos abs = h.absolutePos(REL);
        ServerPlayer p = player(h, abs);
        KyNgoInstance i = KyNgoService.startAt(p, KyNgoType.LAO_NHAN, abs);
        if (!(i.level.getEntity(i.main) instanceof net.minecraft.world.entity.npc.Villager)) { h.fail("khong co lao nhan"); return; }
        if (KyNgoService.resolve(p, "ffffff", "leave") != KyNgoService.Result.NO_SUCH) { h.fail("token sai khong bi tu choi"); return; }
        if (KyNgoService.resolve(p, i.token, "dance") != KyNgoService.Result.BAD_CHOICE) { h.fail("lua chon sai khong bi tu choi"); return; }
        ServerPlayer other = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "hk-kyngo-other"));
        other.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        if (KyNgoService.resolve(other, i.token, "leave") != KyNgoService.Result.NOT_OWNER) { h.fail("nguoi khac van dung duoc"); return; }
        p.setPos(abs.getX() + 40.5, abs.getY(), abs.getZ() + 0.5);
        if (KyNgoService.resolve(p, i.token, "leave") != KyNgoService.Result.TOO_FAR) { h.fail("xa qua van nhan"); return; }
        p.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        if (KyNgoService.resolve(p, i.token, "leave") != KyNgoService.Result.OK) { h.fail("token dung bi tu choi"); return; }
        if (KyNgoService.byToken(i.token) != null || alive(i) != 0) { h.fail("kyngo chua ket thuc"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldManHelpPaysTuTinh(GameTestHelper h) {
        floor(h);
        BlockPos abs = h.absolutePos(REL);
        ServerPlayer p = player(h, abs);
        Wallet.give(p, 200);
        KyNgoInstance i = KyNgoService.startAt(p, KyNgoType.LAO_NHAN, abs);
        if (KyNgoService.resolve(p, i.token, "help") != KyNgoService.Result.OK) { h.fail("help khong thanh cong"); return; }
        if (Wallet.count(p) != 150) { h.fail("khong tru 50 Tu Tinh: " + Wallet.count(p)); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldManRobCursesPlayer(GameTestHelper h) {
        floor(h);
        BlockPos abs = h.absolutePos(REL);
        ServerPlayer p = player(h, abs);
        KyNgoInstance i = KyNgoService.startAt(p, KyNgoType.LAO_NHAN, abs);
        if (KyNgoService.resolve(p, i.token, "rob") != KyNgoService.Result.OK) { h.fail("rob khong thanh cong"); return; }
        if (!p.hasEffect(MobEffects.WEAKNESS)) { h.fail("khong bi nguyen rua"); return; }
        if (Wallet.count(p) <= 0) { h.fail("khong duoc loot"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void caveChestAppearsAfterGuardsDie(GameTestHelper h) {
        floor(h);
        BlockPos abs = h.absolutePos(REL);
        ServerPlayer p = player(h, abs);
        KyNgoInstance i = KyNgoService.startAt(p, KyNgoType.DONG_PHU, abs);
        if (i.guards.size() != 3 || alive(i) != 3) { h.fail("khong co 3 ho ve"); return; }
        for (UUID u : i.guards) {
            Entity g = i.level.getEntity(u);
            if (g instanceof LivingEntity le) le.kill();
        }
        h.runAfterDelay(5, () -> {
            if (!i.level.getBlockState(abs).is(com.hukhong.treasure.TreasureRegistry.BAO_RUONG.get())) { h.fail("ruong chua xuat hien"); return; }
            if (Wallet.honTinh(p) < 1) { h.fail("khong nhan Hon Tinh"); return; }
            KyNgoService.finish(i);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void corpseSearchResolves(GameTestHelper h) {
        floor(h);
        BlockPos abs = h.absolutePos(REL);
        ServerPlayer p = player(h, abs);
        KyNgoInstance i = KyNgoService.startAt(p, KyNgoType.THI_THE, abs);
        if (!i.level.getBlockState(abs.east()).is(com.hukhong.treasure.TreasureRegistry.BAO_RUONG.get())) { h.fail("khong co ruong ben than: " + i.level.getBlockState(abs.east()) + " origin=" + i.level.getBlockState(abs) + " below=" + i.level.getBlockState(abs.below()) + " ents=" + alive(i)); return; }
        if (KyNgoService.resolve(p, i.token, "search") != KyNgoService.Result.OK) { h.fail("search khong thanh cong"); return; }
        if (!i.resolved) { h.fail("chua danh dau ket thuc"); return; }
        KyNgoService.finish(i);
        if (i.level.getBlockState(abs.east()).is(com.hukhong.treasure.TreasureRegistry.BAO_RUONG.get())) { h.fail("ruong khong duoc don"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void beastFreeTamesIt(GameTestHelper h) {
        floor(h);
        BlockPos abs = h.absolutePos(REL);
        ServerPlayer p = player(h, abs);
        KyNgoInstance i = KyNgoService.startAt(p, KyNgoType.LINH_THU, abs);
        if (!(i.level.getEntity(i.main) instanceof LinhThu beast)) { h.fail("khong co linh thu"); return; }
        if (!i.level.getBlockState(abs.east()).is(Blocks.SPRUCE_FENCE)) { h.fail("khong co long nhot: " + i.level.getBlockState(abs.east()) + " placed=" + i.placed.keySet()); return; }
        if (KyNgoService.resolve(p, i.token, "free") != KyNgoService.Result.OK) { h.fail("free khong thanh cong"); return; }
        if (!beast.isTame() || !p.getUUID().equals(beast.getOwnerUUID())) { h.fail("linh thu chua thuoc ve nguoi choi"); return; }
        if (!beast.isAlive()) { h.fail("linh thu bi xoa"); return; }
        if (i.level.getBlockState(abs.east()).is(Blocks.SPRUCE_FENCE)) { h.fail("long nhot khong duoc go"); return; }
        beast.discard();
        h.succeed();
    }
}
