package com.hukhong.creature;

import com.hukhong.HuKhong;
import com.hukhong.creature.pet.PetService;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** F4: luong thuan phuc (thuc an dung + duoi 30% HP) va gioi han 1 thu chien dau + 1 linh sung. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class CreatureGameTests {
    private CreatureGameTests() {}

    @SuppressWarnings("unchecked")
    private static LinhThu spawn(GameTestHelper h, CreatureKind k, int x) {
        return h.spawnWithNoFreeWill((EntityType<LinhThu>) CreatureRegistry.type(k), new BlockPos(x, 2, 1));
    }

    private static ItemStack food(LinhThu t) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(PetService.tameItemId(t))));
    }

    private static ServerPlayer player(GameTestHelper h, String n) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), n));
    }

    @GameTest(template = "empty")
    public static void tamingNeedsRightHerbAndLowHealth(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-tame1");
        LinhThu t = spawn(h, CreatureKind.NGOC_THO, 1);
        if (PetService.tryTame(p, t, new ItemStack(Items.STICK)) != PetService.Result.WRONG_ITEM) { h.fail("wrong item must be rejected"); return; }
        if (PetService.tryTame(p, t, food(t)) != PetService.Result.TOO_HEALTHY) { h.fail("full HP creature must not be tameable"); return; }
        t.setHealth(t.getMaxHealth() * 0.29f);
        if (PetService.tryTame(p, t, food(t)) != PetService.Result.OK) { h.fail("<30% HP + right herb must tame"); return; }
        if (!t.isTame() || !p.getUUID().equals(t.getOwnerUUID())) { h.fail("owner must be the taming player"); return; }
        if (!PetService.data(p).spirit().filter(t.getUUID()::equals).isPresent()) { h.fail("spirit slot must hold the pet"); return; }
        if (PetService.tryTame(p, t, food(t)) != PetService.Result.ALREADY_TAMED) { h.fail("tame twice must be rejected"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void petLimitOneBattleOneSpirit(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-tame2");
        LinhThu rabbit = spawn(h, CreatureKind.NGOC_THO, 1), fox = spawn(h, CreatureKind.LINH_HO, 2);
        LinhThu cub = spawn(h, CreatureKind.THIET_BOI_HUNG_CON, 3), cub2 = spawn(h, CreatureKind.THIET_BOI_HUNG_CON, 4);
        for (LinhThu t : new LinhThu[]{rabbit, fox, cub, cub2}) t.setHealth(1f);
        if (PetService.tryTame(p, rabbit) != PetService.Result.OK) { h.fail("first spirit pet must tame"); return; }
        if (PetService.tryTame(p, fox) != PetService.Result.SLOT_FULL) { h.fail("second spirit pet must be refused (1 linh sung)"); return; }
        if (PetService.tryTame(p, cub) != PetService.Result.OK) { h.fail("battle pet uses its own slot"); return; }
        if (PetService.tryTame(p, cub2) != PetService.Result.SLOT_FULL) { h.fail("second battle pet must be refused (1 thu chien dau)"); return; }
        if (fox.isTame() || cub2.isTame()) { h.fail("refused creatures must stay wild"); return; }
        if (!PetService.release(p, "spirit")) { h.fail("release must free the slot"); return; }
        if (PetService.tryTame(p, fox) != PetService.Result.OK) { h.fail("after release the slot is free again"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void untameableKindsAreRefused(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-tame3");
        LinhThu deer = spawn(h, CreatureKind.LINH_LOC, 1), bear = spawn(h, CreatureKind.THIET_BOI_HUNG, 2);
        deer.setHealth(1f);
        bear.setHealth(1f);
        if (PetService.tryTame(p, deer) != PetService.Result.NOT_TAMEABLE || PetService.tryTame(p, bear) != PetService.Result.NOT_TAMEABLE) {
            h.fail("Linh Loc and the adult Thiet Boi Hung are not tameable"); return;
        }
        h.succeed();
    }
}
