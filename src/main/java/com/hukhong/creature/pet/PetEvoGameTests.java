package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.CreatureRegistry;
import com.hukhong.creature.LinhThu;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Tien hoa: can cap, nhanh lam doi mau toi da, tang 2 can than mat, du lieu PetData cu van doc duoc. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class PetEvoGameTests {
    private PetEvoGameTests() {}

    @SuppressWarnings("unchecked")
    private static LinhThu spawnTamed(GameTestHelper h, ServerPlayer p, CreatureKind k, int x) {
        LinhThu t = h.spawnWithNoFreeWill((EntityType<LinhThu>) CreatureRegistry.type(k), new BlockPos(x, 2, 1));
        t.setHealth(1f);
        if (PetService.tryTame(p, t) != PetService.Result.OK) throw new IllegalStateException("tame failed for " + k);
        return t;
    }

    private static ServerPlayer player(GameTestHelper h, String n) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), n));
    }

    private static ItemStack catalyst(PetBranch b) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(b.catalystId())));
    }

    @GameTest(template = "empty")
    public static void evolveNeedsPetLevel(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-evo1");
        CultivationService.setLevel(p, 10);
        LinhThu t = spawnTamed(h, p, CreatureKind.NGOC_THO, 1);
        ItemStack held = catalyst(PetBranch.HUYEN_GIAP);
        if (PetService.evolve(p, t, PetBranch.HUYEN_GIAP, held) != PetService.Evo.NO_LEVEL) { h.fail("level 10 pet must not evolve"); return; }
        if (t.petStage() != 0 || t.petBranch() != null) { h.fail("failed evolve must not change form"); return; }
        if (held.getCount() != 1) { h.fail("failed evolve must not consume the catalyst"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void branchChangesMaxHealth(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-evo2");
        CultivationService.setLevel(p, 30);
        LinhThu t = spawnTamed(h, p, CreatureKind.NGOC_THO, 1);
        double before = t.getMaxHealth();
        ItemStack held = catalyst(PetBranch.HUYEN_GIAP);
        if (PetService.evolve(p, t, PetBranch.HUYEN_GIAP, held) != PetService.Evo.OK) { h.fail("level 30 + catalyst must evolve"); return; }
        if (t.petStage() != 1 || t.petBranch() != PetBranch.HUYEN_GIAP) { h.fail("form must be stage 1 / Huyen Giap"); return; }
        if (!(t.getMaxHealth() > before)) { h.fail("Huyen Giap must raise max HP (before=" + before + ", after=" + t.getMaxHealth() + ")"); return; }
        if (held.getCount() != 0) { h.fail("catalyst must be consumed on success"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stageTwoNeedsBond(GameTestHelper h) {
        ServerPlayer p = player(h, "hk-evo3");
        CultivationService.setLevel(p, 60);
        LinhThu t = spawnTamed(h, p, CreatureKind.LINH_HO, 1);
        if (PetService.evolve(p, t, PetBranch.LINH_PHONG, catalyst(PetBranch.LINH_PHONG)) != PetService.Evo.OK) { h.fail("stage 1 must evolve at level 60"); return; }
        if (PetService.evolve(p, t, null, catalyst(PetBranch.LINH_PHONG)) != PetService.Evo.NO_BOND) { h.fail("stage 2 must need bond >= 50"); return; }
        t.setBond(50);
        if (PetService.evolve(p, t, null, catalyst(PetBranch.LINH_PHONG)) != PetService.Evo.OK) { h.fail("bond 50 + level 60 must reach stage 2"); return; }
        if (t.petStage() != 2) { h.fail("stage must be 2"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldPetDataStillDecodes(GameTestHelper h) {
        UUID a = UUID.randomUUID();
        CompoundTag old = new CompoundTag();
        old.put("battle", UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, a).getOrThrow());
        PetData d = PetData.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
        if (!a.equals(d.battle().orElse(null))) { h.fail("battle uuid must survive decode"); return; }
        if (d.spirit().isPresent() || d.battleFallen().isPresent() || d.spiritFallen().isPresent()) { h.fail("missing new fields must default to empty"); return; }
        h.succeed();
    }
}
