package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.hukhong.core.CultivationService;
import com.hukhong.creature.CreatureDef;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.CreatureRegistry;
import com.hukhong.creature.LinhThu;
import com.hukhong.entity.HuHoa;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Thuan phuc + gioi han: 1 thu chien dau + 1 linh sung moi nguoi choi; thu chien dau len cap theo chu.
 * Tien hoa (cap 30 / 60, than mat 50), chet thi vao hang hoi sinh (2 phut) roi tai sinh ben chu.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PetService {
    private PetService() {}

    public enum Result { OK, NOT_TAMEABLE, ALREADY_TAMED, WRONG_ITEM, TOO_HEALTHY, SLOT_FULL }

    /** Ket qua tien hoa. */
    public enum Evo { OK, NOT_OWNED, MAX_STAGE, NO_LEVEL, NO_BOND, NO_CATALYST, BRANCH_MISMATCH }

    /** Thoi gian hoi sinh sau khi chet: 2 phut. */
    public static final long REVIVE_TICKS = 2400;
    static final String[] SLOTS = {"battle", "spirit"};
    /** Tang da nhac tien hoa cho tung thu (tranh nhac lap lai). */
    private static final Map<UUID, Integer> PROMPTED = new HashMap<>();

    public static PetData data(ServerPlayer p) { return p.getData(CreatureRegistry.PETS.get()); }

    /** Cap tu luyen cua chu = cap linh thu (linh sung cung dung cap nay de tien hoa). */
    public static int ownerLevel(ServerPlayer p) { return PetMath.petLevel(CultivationService.get(p).level()); }

    /** Thu dang o trong the gioi (null neu chua tai / khong co). */
    public static LinhThu loaded(ServerPlayer p, String slot) {
        return data(p).get(slot).map(u -> p.serverLevel().getEntity(u)).filter(LinhThu.class::isInstance).map(LinhThu.class::cast).orElse(null);
    }

    /** Co dung thuc an thuan phuc khong. */
    public static boolean isTameItem(LinhThu t, ItemStack held) {
        String id = tameItemId(t);
        return id != null && !held.isEmpty() && BuiltInRegistries.ITEM.getKey(held.getItem()).equals(ResourceLocation.parse(id));
    }

    public static String tameItemId(LinhThu t) {
        CreatureKind k = t.kind();
        if (k.tameSlot == null) return null;
        CreatureDef d = k.def();
        return d != null && d.tame() != null ? d.tame().item() : k.tameItem;
    }

    public static double tameFrac(LinhThu t) {
        CreatureDef d = t.kind().def();
        return d != null && d.tame() != null ? d.tame().hpFrac() : 0.3;
    }

    /** Thuan phuc co kiem tra thuc an. */
    public static Result tryTame(ServerPlayer p, LinhThu t, ItemStack held) {
        if (t.kind().tameSlot != null && !isTameItem(t, held)) return Result.WRONG_ITEM;
        return tryTame(p, t);
    }

    /** Thuan phuc: thu duoi 30% HP + o con trong (ca thu dang chet cho hoi sinh). Thanh cong -> thu thuoc ve p, ghi slot. */
    public static Result tryTame(ServerPlayer p, LinhThu t) {
        String slot = t.kind().tameSlot;
        if (slot == null) return Result.NOT_TAMEABLE;
        if (t.isTame()) return Result.ALREADY_TAMED;
        if (!PetMath.canTame(t.getHealth(), t.getMaxHealth(), tameFrac(t))) return Result.TOO_HEALTHY;
        PetData d = data(p);
        if (d.taken(slot)) return Result.SLOT_FULL;
        t.tame(p);
        t.setPersistenceRequired();
        t.getNavigation().stop();
        t.setTarget(null);
        t.setHealth(Math.max(t.getHealth(), t.getMaxHealth() * 0.5f));
        p.setData(CreatureRegistry.PETS.get(), d.with(slot, t.getUUID()));
        syncLevel(t);
        return Result.OK;
    }

    /** Thu chien dau len cap theo chu (giu ti le HP), roi ap chi so nhanh/than mat. */
    public static void syncLevel(LinhThu t) {
        if (!"battle".equals(t.kind().tameSlot) || !t.isTame() || t.level().isClientSide) return;
        if (!(t.getOwner() instanceof ServerPlayer o)) return;
        int lv = PetMath.petLevel(CultivationService.get(o).level());
        if (lv == t.huLevel()) return;
        float ratio = t.getHealth() / Math.max(1f, t.getMaxHealth());
        CreatureDef d = t.kind().def();
        HuHoa.applyStats(t, lv, d == null ? 1.0 : d.hpMul(), d == null ? 1.0 : d.dmgMul());
        PetForm.apply(t);
        t.setHealth(Math.max(1f, t.getMaxHealth() * ratio));
    }

    public static boolean release(ServerPlayer p, String slot) {
        PetData d = data(p);
        Optional<UUID> id = d.get(slot);
        boolean fallen = d.fallen(slot).isPresent();
        if (id.isEmpty() && !fallen) return false;
        if (id.isPresent() && p.serverLevel().getEntity(id.get()) instanceof LinhThu t) {
            t.setTame(false, false);
            t.setOwnerUUID(null);
        }
        p.setData(CreatureRegistry.PETS.get(), d.with(slot, null).withFallen(slot, null));
        return true;
    }

    /** Linh Ho me hoac: quai dang nham chu bi doi muc tieu sang quai khac gan do (hoac mat muc tieu + cham). true neu da me hoac. */
    public static boolean charm(LinhThu fox, ServerPlayer owner) {
        for (Mob m : fox.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(8), e -> e instanceof Enemy && e.getTarget() == owner)) {
            if (!PetMath.charmable(m.getMaxHealth())) continue;
            Mob other = fox.level().getEntitiesOfClass(Mob.class, m.getBoundingBox().inflate(10),
                            e -> e != m && e instanceof Enemy && !(e instanceof LinhThu))
                    .stream().min(Comparator.comparingDouble(m::distanceToSqr)).orElse(null);
            m.setTarget(other);
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, PetMath.CHARM_TICKS, 0, false, false));
            if (fox.level() instanceof ServerLevel sl)
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, m.getX(), m.getEyeY() + 0.4, m.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- tien hoa

    /**
     * Tien hoa tang tiep theo. want = nhanh (tang 1: chon nhanh; tang 2: phai trung nhanh hien tai, null = theo nhanh hien tai).
     * held != null: xuc tac dang cam tay (da khop nhanh); held == null: tim xuc tac trong tui chu. Chi tru xuc tac khi thanh cong.
     */
    public static Evo evolve(ServerPlayer p, LinhThu t, PetBranch want, ItemStack held) {
        if (!t.isTame() || !p.getUUID().equals(t.getOwnerUUID())) return Evo.NOT_OWNED;
        int stage = t.petStage();
        if (stage >= PetForm.MAX_STAGE) return Evo.MAX_STAGE;
        int target = stage + 1;
        if (ownerLevel(p) < PetForm.requiredLevel(target)) return Evo.NO_LEVEL;
        if (target == 2 && t.bond() < PetForm.EVO2_BOND) return Evo.NO_BOND;
        PetBranch branch = stage == 0 ? want : t.petBranch();
        if (stage > 0 && want != null && want != branch) return Evo.BRANCH_MISMATCH;
        if (branch == null) return Evo.NO_CATALYST;
        int invSlot = -1;
        if (held != null) {
            if (!branch.isCatalyst(held)) return Evo.NO_CATALYST;
        } else {
            invSlot = findCatalyst(p, branch);
            if (invSlot < 0) return Evo.NO_CATALYST;
        }
        if (!p.isCreative()) {
            if (held != null) held.shrink(1);
            else p.getInventory().getItem(invSlot).shrink(1);
        }
        t.setPetForm(target, branch);
        PetForm.apply(t);
        PetEvoFx.start(p, t, branch, target);
        return Evo.OK;
    }

    private static int findCatalyst(ServerPlayer p, PetBranch b) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (b.isCatalyst(p.getInventory().getItem(i))) return i;
        return -1;
    }

    /** Chuot phai thu sau khi thuoc ve minh: xuc tac = tien hoa, thuc an = boi bon than mat. */
    public static InteractionResult useItem(LinhThu t, Player p, ItemStack held) {
        PetBranch b = PetBranch.byCatalyst(held);
        boolean food = isTameItem(t, held);
        if (b == null && !food) return InteractionResult.PASS;
        if (t.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(p instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (b != null) {
            Evo r = evolve(sp, t, b, held);
            sp.displayClientMessage(evoText(r, t, sp), true);
            return InteractionResult.CONSUME;
        }
        if (t.bond() >= PetForm.BOND_MAX) {
            sp.displayClientMessage(Tx.t("hukhong.pet.feed.max", "%s đã thân thiết tối đa.", "%s is fully bonded.", t.getDisplayName()), true);
            return InteractionResult.CONSUME;
        }
        if (!sp.isCreative()) held.shrink(1);
        PetForm.addBond(t, 5);
        t.heal(t.getMaxHealth() * 0.25f);
        sp.displayClientMessage(Tx.t("hukhong.pet.feed", "%s được bồi dưỡng, thân mật %s/100.", "%s was fed, bond %s/100.", t.getDisplayName(), t.bond()), true);
        return InteractionResult.CONSUME;
    }

    public static Component evoText(Evo r, LinhThu t, ServerPlayer p) {
        int target = Math.min(PetForm.MAX_STAGE, t.petStage() + 1);
        return switch (r) {
            case OK -> Tx.t("hukhong.pet.evo.done", "%s đã tiến hóa: %s", "%s evolved: %s", t.getDisplayName(), t.petBranch().formName(t.petStage()));
            case NOT_OWNED -> Tx.t("hukhong.pet.evo.err.not_owned", "Đó không phải linh thú của bạn.", "That is not your pet.");
            case MAX_STAGE -> Tx.t("hukhong.pet.evo.err.max", "Linh thú đã tiến hóa tối đa.", "Pet is already at max evolution.");
            case NO_LEVEL -> Tx.t("hukhong.pet.evo.err.level", "Cần linh thú đạt Lv.%s (hiện Lv.%s).", "Pet needs Lv.%s (now Lv.%s).",
                    PetForm.requiredLevel(target), ownerLevel(p));
            case NO_BOND -> Tx.t("hukhong.pet.evo.err.bond", "Cần thân mật từ %s (hiện %s).", "Needs bond %s (now %s).", PetForm.EVO2_BOND, t.bond());
            case NO_CATALYST -> Tx.t("hukhong.pet.evo.err.catalyst", "Thiếu vật phẩm xúc tác của nhánh.", "Missing the branch catalyst item.");
            case BRANCH_MISMATCH -> Tx.t("hukhong.pet.evo.err.branch", "Linh thú đã theo nhánh %s.", "Pet already follows branch %s.",
                    t.petBranch() == null ? "" : t.petBranch().displayName());
        };
    }

    /** Nhac chu khi thu du cap/than mat de tien hoa: tuong tac duoc bang lenh chat (run_command). */
    private static void promptDue(ServerPlayer p) {
        int lv = ownerLevel(p);
        for (String s : SLOTS) {
            LinhThu t = loaded(p, s);
            if (t == null || !t.isAlive()) continue;
            int next = PetForm.nextStage(t.petStage(), lv, t.bond());
            if (next < 0 || PROMPTED.getOrDefault(t.getUUID(), 0) >= next) continue;
            PROMPTED.put(t.getUUID(), next);
            MutableComponent msg = Tx.t(next == 1 ? "hukhong.pet.prompt" : "hukhong.pet.prompt2",
                    next == 1 ? "Linh thú %s đã đạt Lv.%s — bấm nhánh để tiến hóa (cần xúc tác): " : "Linh thú %s đủ điều kiện tầng 2 — bấm để tiến hóa: ",
                    next == 1 ? "Pet %s reached Lv.%s — pick a branch to evolve (needs catalyst): " : "Pet %s can reach stage 2 — click to evolve: ",
                    t.getDisplayName(), lv).copy();
            if (next == 1) {
                for (PetBranch b : PetBranch.values()) {
                    Style st = Style.EMPTY.withColor(TextColor.fromRgb(b.rgb)).withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hkpet evolve " + b.id));
                    msg.append(Component.literal(" [").withStyle(Style.EMPTY)).append(b.displayName().copy().withStyle(st)).append(Component.literal("]"));
                }
            } else {
                PetBranch cur = t.petBranch();
                if (cur != null) msg.append(cur.displayName().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(cur.rgb)).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hkpet evolve " + cur.id))));
            }
            p.sendSystemMessage(msg);
        }
    }

    /** Thu da chet dem nguoc het han -> hoi sinh ben chu (bo 25 than mat, 50% mau). */
    private static void reviveDue(ServerPlayer p) {
        PetData d = data(p);
        long now = p.serverLevel().getGameTime();
        for (String s : SLOTS) {
            Optional<PetData.Fallen> f = d.fallen(s);
            if (f.isEmpty() || f.get().reviveAt() > now) continue;
            if (revive(p, s, f.get())) return; // lam lai o tick sau
        }
    }

    static boolean revive(ServerPlayer p, String slot, PetData.Fallen f) {
        CreatureKind k = CreatureKind.byId(f.kind());
        if (k == null) {
            p.setData(CreatureRegistry.PETS.get(), data(p).withFallen(slot, null));
            return false;
        }
        Entity e = CreatureRegistry.type(k).create(p.serverLevel());
        if (!(e instanceof LinhThu t)) return false;
        t.restoreSnapshot(f.snapshot());
        t.setPos(p.getX() + 1.5, p.getY(), p.getZ());
        t.setPetForm(f.stage(), f.branch().isEmpty() ? null : PetBranch.byId(f.branch()));
        t.setBond(Math.max(0, f.bond() - 25));
        t.tame(p);
        t.setPersistenceRequired();
        p.serverLevel().addFreshEntity(t);
        syncLevel(t);
        PetForm.apply(t);
        t.setHealth(t.getMaxHealth() * 0.5f);
        p.setData(CreatureRegistry.PETS.get(), data(p).withFallen(slot, null).with(slot, t.getUUID()));
        p.displayClientMessage(Tx.t("hukhong.pet.revived", "%s đã sống lại!", "%s has been revived!", t.getDisplayName()), false);
        return true;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof LinhThu t) || t.level().isClientSide || !t.isTame() || t.getOwnerUUID() == null) return;
        String slot = t.kind().tameSlot;
        if (slot == null || t.getServer() == null) return;
        ServerPlayer o = t.getServer().getPlayerList().getPlayer(t.getOwnerUUID());
        if (o == null) return;
        PetData d = data(o);
        if (d.get(slot).filter(t.getUUID()::equals).isPresent()) {
            PetData.Fallen f = new PetData.Fallen(t.kind().id, t.petStage(), t.petBranch() == null ? "" : t.petBranch().id,
                    ownerLevel(o), t.bond(), t.level().getGameTime() + REVIVE_TICKS, t.saveWithoutId(new CompoundTag()));
            o.setData(CreatureRegistry.PETS.get(), d.with(slot, null).withFallen(slot, f));
            o.displayClientMessage(Component.translatable("hukhong.pet.died", t.getDisplayName()), false);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % 5 == 0) PetSyncPayload.sync(p);
        if (p.tickCount % 20 == 7) reviveDue(p);
        if (p.tickCount % 40 == 13) promptDue(p);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("linhsu")
                .then(Commands.literal("list").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PetData d = data(p);
                    for (String s : SLOTS) {
                        Entity en = d.get(s).map(u -> p.serverLevel().getEntity(u)).orElse(null);
                        c.getSource().sendSuccess(() -> Component.translatable("hukhong.pet.slot." + s,
                                d.taken(s) ? (en != null ? en.getDisplayName() : Component.translatable("hukhong.pet.unloaded")) : Component.translatable("hukhong.pet.empty")), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("release").then(Commands.argument("slot", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String s = StringArgumentType.getString(c, "slot");
                    boolean ok = release(p, s);
                    c.getSource().sendSuccess(() -> Component.translatable(ok ? "hukhong.pet.released" : "hukhong.pet.none", s), false);
                    return ok ? 1 : 0;
                }))));
    }
}
