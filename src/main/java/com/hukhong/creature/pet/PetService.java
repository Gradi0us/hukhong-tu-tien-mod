package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.creature.CreatureDef;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.CreatureRegistry;
import com.hukhong.creature.LinhThu;
import com.hukhong.entity.HuHoa;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Thuan phuc + gioi han: 1 thu chien dau + 1 linh sung moi nguoi choi; thu chien dau len cap theo chu. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PetService {
    private PetService() {}

    public enum Result { OK, NOT_TAMEABLE, ALREADY_TAMED, WRONG_ITEM, TOO_HEALTHY, SLOT_FULL }

    public static PetData data(ServerPlayer p) { return p.getData(CreatureRegistry.PETS.get()); }

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

    /** Thuan phuc: thu duoi 30% HP + o con trong. Thanh cong -> thu thuoc ve p, ghi slot. */
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

    /** Thu chien dau len cap theo chu (giu ti le HP). */
    public static void syncLevel(LinhThu t) {
        if (!"battle".equals(t.kind().tameSlot) || !t.isTame() || t.level().isClientSide) return;
        if (!(t.getOwner() instanceof ServerPlayer o)) return;
        int lv = PetMath.petLevel(CultivationService.get(o).level());
        if (lv == t.huLevel()) return;
        float ratio = t.getHealth() / Math.max(1f, t.getMaxHealth());
        CreatureDef d = t.kind().def();
        HuHoa.applyStats(t, lv, d == null ? 1.0 : d.hpMul(), d == null ? 1.0 : d.dmgMul());
        t.setHealth(Math.max(1f, t.getMaxHealth() * ratio));
    }

    public static boolean release(ServerPlayer p, String slot) {
        PetData d = data(p);
        Optional<UUID> id = d.get(slot);
        if (id.isEmpty()) return false;
        if (p.serverLevel().getEntity(id.get()) instanceof LinhThu t) {
            t.setTame(false, false);
            t.setOwnerUUID(null);
        }
        p.setData(CreatureRegistry.PETS.get(), d.with(slot, null));
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

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof LinhThu t) || t.level().isClientSide || !t.isTame() || t.getOwnerUUID() == null) return;
        String slot = t.kind().tameSlot;
        if (slot == null || t.getServer() == null) return;
        ServerPlayer o = t.getServer().getPlayerList().getPlayer(t.getOwnerUUID());
        if (o == null) return;
        PetData d = data(o);
        if (d.get(slot).filter(t.getUUID()::equals).isPresent()) {
            o.setData(CreatureRegistry.PETS.get(), d.with(slot, null));
            o.displayClientMessage(Component.translatable("hukhong.pet.died", t.getDisplayName()), false);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("linhsu")
                .then(Commands.literal("list").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PetData d = data(p);
                    for (String s : new String[]{"battle", "spirit"}) {
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
