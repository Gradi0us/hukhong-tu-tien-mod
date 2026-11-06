package com.hukhong.story.kyngo;

import com.hukhong.core.CultivationService;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.CreatureRegistry;
import com.hukhong.creature.LinhThu;
import com.hukhong.currency.Wallet;
import com.hukhong.resource.ResourceRegistry;
import com.hukhong.skillbook.BookSkill;
import com.hukhong.skillbook.SkillBookRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Sinh, tương tác và dọn từng loại Kỳ Ngộ. Mọi khối đặt vào đều chỉ vào ô không khí và được ghi lại để trả lại. */
final class KyNgoEncounters {
    private KyNgoEncounters() {}

    static final String TAG = "hukhong_kyngo";
    static final long HELP_PRICE = 50;
    static final double DEMON_CHANCE = 0.30;
    static final double TRAP_CHANCE = 0.20;
    static final long CURSE_TICKS = 5 * 60 * 20;
    static final double REWARD_EXP_FRAC = 0.3;
    static final int[][] CAGE = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // ---- tiện ích ----

    static <T extends net.minecraft.world.entity.Entity> T make(EntityType<T> type, ServerLevel sl, BlockPos pos) {
        T e = type.create(sl);
        if (e != null) e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        return e;
    }

    static void add(KyNgoInstance i, Entity e) {
        e.addTag(TAG);
        e.addTag(TAG + "_" + i.token);
        if (i.level.addFreshEntity(e)) i.entities.add(e.getUUID());
    }

    /** Đặt khối chỉ khi ô hiện là không khí; ghi lại để dọn. */
    static boolean place(KyNgoInstance i, BlockPos pos, BlockState s) {
        if (!i.level.getBlockState(pos).isAir()) return false;
        i.level.setBlock(pos, s, 3);
        i.placed.put(pos.immutable(), s);
        return true;
    }

    static void discard(KyNgoInstance i, UUID u) {
        if (u == null) return;
        i.entities.remove(u);
        Entity e = i.level.getEntity(u);
        if (e != null) e.discard();
    }

    static ItemStack randomPill(RandomSource r) {
        var l = new ArrayList<>(ResourceRegistry.LINH_DUOC.values());
        return new ItemStack(l.get(r.nextInt(l.size())).get());
    }

    static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    static int findFood(ServerPlayer p) {
        for (int s = 0; s < p.getInventory().getContainerSize(); s++) {
            if (p.getInventory().getItem(s).get(DataComponents.FOOD) != null) return s;
        }
        return -1;
    }

    private static Entity main(KyNgoInstance i) {
        return i.main == null ? null : i.level.getEntity(i.main);
    }

    // ---- sinh ----

    static void spawn(KyNgoInstance i) {
        switch (i.type) {
            case LAO_NHAN -> spawnOldMan(i);
            case DONG_PHU -> spawnCave(i);
            case THI_THE -> spawnCorpse(i);
            case LINH_THU -> spawnBeast(i);
        }
    }

    private static void spawnOldMan(KyNgoInstance i) {
        Villager v = make(EntityType.VILLAGER, i.level, i.origin);
        if (v == null) return;
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setPersistenceRequired();
        v.setCustomName(Component.translatable("hukhong.kyngo.name.lao"));
        v.setCustomNameVisible(true);
        add(i, v);
        i.main = v.getUUID();
    }

    private static void spawnCave(KyNgoInstance i) {
        BlockPos o = i.origin;
        // vòng đá cao 2 khối, bán kính 2 (5x5 viền)
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
            for (int dy = 0; dy <= 1; dy++) place(i, o.offset(dx, dy, dz), Blocks.STONE_BRICKS.defaultBlockState());
        }
        // 3 hộ vệ bên trong vòng
        for (BlockPos g : new BlockPos[]{o.offset(1, 0, 0), o.offset(-1, 0, 0), o.offset(0, 0, 1)}) {
            var z = make(EntityType.ZOMBIE, i.level, g);
            if (z == null) continue;
            z.setPersistenceRequired();
            add(i, z);
            i.guards.add(z.getUUID());
        }
    }

    private static void spawnCorpse(KyNgoInstance i) {
        ArmorStand as = make(EntityType.ARMOR_STAND, i.level, i.origin);
        if (as == null) return;
        as.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        as.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        as.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        as.setCustomName(Component.translatable("hukhong.kyngo.name.thi"));
        as.setCustomNameVisible(true);
        add(i, as);
        i.main = as.getUUID();
        BlockPos cp = i.origin.offset(1, 0, 0);
        if (place(i, cp, Blocks.CHEST.defaultBlockState()) && i.level.getBlockEntity(cp) instanceof Container c) {
            int slot = 0;
            List<BookSkill> sk = BookSkill.universal();
            if (!sk.isEmpty()) c.setItem(slot++, SkillBookRegistry.stack(sk.get(i.level.random.nextInt(sk.size()))));
            c.setItem(slot, randomPill(i.level.random));
        }
    }

    private static void spawnBeast(KyNgoInstance i) {
        CreatureKind k = i.level.random.nextBoolean() ? CreatureKind.NGOC_THO : CreatureKind.LINH_HO;
        if (!(CreatureRegistry.type(k).create(i.level) instanceof LinhThu b)) return;
        b.moveTo(i.origin.getX() + 0.5, i.origin.getY(), i.origin.getZ() + 0.5, i.level.random.nextFloat() * 360f, 0f);
        b.finalizeSpawn(i.level, i.level.getCurrentDifficultyAt(i.origin), MobSpawnType.EVENT, null);
        b.setNoAi(true);
        b.setPersistenceRequired();
        add(i, b);
        i.main = b.getUUID();
        for (int[] d : CAGE) place(i, i.origin.offset(d[0], 0, d[1]), Blocks.OAK_FENCE.defaultBlockState());
    }

    // ---- thông báo ----

    static void intro(KyNgoInstance i) {
        ServerPlayer p = i.owner;
        p.sendSystemMessage(Component.translatable("hukhong.kyngo.header"));
        p.sendSystemMessage(Component.translatable(i.type.introKey));
        p.sendSystemMessage(Component.translatable("hukhong.kyngo.pos", i.token,
                i.origin.getX(), i.origin.getY(), i.origin.getZ()));
        MutableComponent line = Component.translatable("hukhong.kyngo.choose");
        for (String c : i.type.choices()) {
            line.append(Component.literal(" "));
            line.append(Component.translatable("hukhong.kyngo.opt." + c).withStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hkkyngo " + i.token + " " + c))
                    .withColor(ChatFormatting.AQUA).withUnderlined(true)));
        }
        p.sendSystemMessage(line);
    }

    // ---- theo dõi mỗi tick (Động Phủ: hộ vệ chết hết -> mở rương) ----

    static void tick(KyNgoInstance i) {
        if (i.type != KyNgoType.DONG_PHU || i.rewarded) return;
        for (UUID u : i.guards) {
            Entity e = i.level.getEntity(u);
            if (e != null && e.isAlive()) return;
        }
        i.rewarded = true;
        RandomSource r = i.level.random;
        if (place(i, i.origin, Blocks.CHEST.defaultBlockState()) && i.level.getBlockEntity(i.origin) instanceof Container c) {
            c.setItem(0, randomPill(r));
            c.setItem(1, randomPill(r));
        }
        Wallet.grantHonTinh(i.owner, 1 + r.nextInt(3));
        Wallet.give(i.owner, 60 + r.nextInt(90));
        i.owner.sendSystemMessage(Component.translatable("hukhong.kyngo.dong.open"));
    }

    // ---- lựa chọn ----

    static KyNgoService.Result choose(KyNgoInstance i, ServerPlayer p, String choice) {
        RandomSource r = p.getRandom();
        switch (i.type) {
            case LAO_NHAN -> {
                if (choice.equals("help")) {
                    int food = findFood(p);
                    if (food >= 0) p.getInventory().getItem(food).shrink(1);
                    else if (!Wallet.pay(p, HELP_PRICE)) {
                        p.sendSystemMessage(Component.translatable("hukhong.kyngo.lao.noprice", HELP_PRICE));
                        return KyNgoService.Result.NO_PRICE;
                    }
                    discard(i, i.main);
                    if (r.nextDouble() < DEMON_CHANCE) {
                        var demon = make(EntityType.HUSK, i.level, i.origin);
                        if (demon != null) {
                            demon.setCustomName(Component.translatable("hukhong.kyngo.name.ma"));
                            demon.setCustomNameVisible(true);
                            add(i, demon);
                            demon.setTarget(p);
                        }
                        p.sendSystemMessage(Component.translatable("hukhong.kyngo.lao.demon"));
                        i.resolved = true; // yêu ma đánh tới khi hết hạn
                        return KyNgoService.Result.OK;
                    }
                    if (r.nextBoolean()) {
                        long exp = (long) (CultivationService.expToNext(CultivationService.get(p).level()) * REWARD_EXP_FRAC);
                        CultivationService.addExp(p, exp);
                        p.sendSystemMessage(Component.translatable("hukhong.kyngo.lao.reward_exp", exp));
                    } else {
                        ItemStack pill = randomPill(r);
                        give(p, pill);
                        p.sendSystemMessage(Component.translatable("hukhong.kyngo.lao.reward_pill", pill.getHoverName()));
                    }
                } else if (choice.equals("rob")) {
                    long gold = 80 + r.nextInt(70);
                    Wallet.give(p, gold);
                    ItemStack pill = randomPill(r);
                    give(p, pill);
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) CURSE_TICKS, 0));
                    p.sendSystemMessage(Component.translatable("hukhong.kyngo.lao.rob", gold, pill.getHoverName()));
                    discard(i, i.main);
                    finish(i);
                } else {
                    p.sendSystemMessage(Component.translatable("hukhong.kyngo.lao.leave"));
                    discard(i, i.main);
                    finish(i);
                }
            }
            case DONG_PHU -> {
                p.sendSystemMessage(Component.translatable("hukhong.kyngo.dong.leave"));
                finish(i);
            }
            case THI_THE -> {
                BlockPos cp = i.origin.offset(1, 0, 0);
                if (choice.equals("search")) {
                    if (r.nextDouble() < TRAP_CHANCE) {
                        for (BlockPos zp : new BlockPos[]{i.origin.offset(0, 0, 1), i.origin.offset(0, 0, -1)}) {
                            var z = make(EntityType.ZOMBIE, i.level, zp);
                            if (z != null) add(i, z);
                        }
                        p.sendSystemMessage(Component.translatable("hukhong.kyngo.thi.trap"));
                        i.resolved = true; // xác chết trỗi dậy; đồ trong rương còn đó
                        return KyNgoService.Result.OK;
                    }
                    if (i.level.getBlockEntity(cp) instanceof Container c) {
                        for (int s = 0; s < c.getContainerSize(); s++) {
                            ItemStack st = c.getItem(s);
                            if (!st.isEmpty()) {
                                give(p, st.copy());
                                c.setItem(s, ItemStack.EMPTY);
                            }
                        }
                    }
                    p.sendSystemMessage(Component.translatable("hukhong.kyngo.thi.loot"));
                } else {
                    p.sendSystemMessage(Component.translatable("hukhong.kyngo.thi.leave"));
                }
                finish(i);
            }
            case LINH_THU -> {
                if (choice.equals("free") && main(i) instanceof LinhThu b) {
                    b.setNoAi(false);
                    i.entities.remove(b.getUUID()); // giữ lại linh thú
                    b.tame(p);
                    p.sendSystemMessage(Component.translatable("hukhong.kyngo.linh.free", b.getDisplayName()));
                } else {
                    p.sendSystemMessage(Component.translatable("hukhong.kyngo.linh.leave"));
                }
                finish(i);
            }
        }
        return KyNgoService.Result.OK;
    }

    private static void finish(KyNgoInstance i) {
        KyNgoService.finish(i);
    }

    // ---- dọn ----

    static void cleanup(KyNgoInstance i) {
        for (UUID u : new ArrayList<>(i.entities)) {
            Entity e = i.level.getEntity(u);
            if (e != null) e.discard();
        }
        i.entities.clear();
        for (var en : i.placed.entrySet()) {
            BlockPos pos = en.getKey();
            BlockState want = en.getValue();
            BlockState now = i.level.getBlockState(pos);
            if (now.getBlock() != want.getBlock()) continue; // người chơi đã đổi: để nguyên
            if (i.level.getBlockEntity(pos) instanceof Container c) Containers.dropContents(i.level, pos, c);
            i.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        i.placed.clear();
    }
}
