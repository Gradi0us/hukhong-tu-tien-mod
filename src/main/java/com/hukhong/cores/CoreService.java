package com.hukhong.cores;

import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import com.hukhong.network.CoreRevealPayload;
import com.hukhong.network.CoreSyncPayload;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Logic server của Ấn Hồn Đài: roll lưu trước, chọn, áp bị động / set bonus. */
public final class CoreService {
    private CoreService() {}

    public static final int MAX_HON_TINH_LUCK = 30;
    public static final int LUCK_PER_HON_TINH = 2;

    public static CoreSlots get(ServerPlayer p) { return p.getData(CoreAttachments.CORE_SLOTS); }

    private static void set(ServerPlayer p, CoreSlots s) { p.setData(CoreAttachments.CORE_SLOTS, s); }

    /** Chỉ số mốc: 10..90 -> 1..9, 95 -> 10, 99 -> 11 (R29). */
    public static int milestoneOf(CultivationData d) { return milestoneOfLevel(d.level()); }

    public static int milestoneOfLevel(int level) {
        if (level >= 99) return level == 99 ? 11 : 12;
        if (level >= 95) return 10;
        return level / 10;
    }

    /** Ngược của milestoneOfLevel: 1..9 -> x10, 10 -> 95, 11 -> 99, 12 -> 100. */
    public static int milestoneLevel(int m) {
        return m <= 9 ? m * 10 : m == 10 ? 95 : m == 11 ? 99 : 100;
    }

    /** Điểm Vận = tư chất cao nhất + Hồn Tinh trong túi (không tiêu hao). */
    public static int luckOf(ServerPlayer p, CultivationData d) {
        int apt = CoreRoller.aptitudeLuck(bestAptitude(d));
        int stars = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(ModItems.HON_TINH.get())) stars += s.getCount();
        for (ItemStack s : p.getInventory().offhand) if (s.is(ModItems.HON_TINH.get())) stars += s.getCount();
        return apt + Math.min(MAX_HON_TINH_LUCK, stars * LUCK_PER_HON_TINH);
    }

    public static Aptitude bestAptitude(CultivationData d) {
        Aptitude best = null;
        for (Path path : d.paths()) {
            Aptitude a = d.aptitude().get(path);
            if (a != null && (best == null || a.ordinal() > best.ordinal())) best = a;
        }
        return best == null ? Aptitude.B : best;
    }

    /** Lấy phiên đang chờ; nếu chưa có thì roll + LƯU TRƯỚC khi mở UI. Trả null nếu không thể (không khóa / Mắc Kẹt). */
    public static CoreSlots.Pending ensurePending(ServerPlayer p) {
        CultivationData d = CultivationService.get(p);
        if (!d.milestoneLocked()) return null;
        int m = milestoneOf(d);
        CoreSlots s = get(p);
        if (s.stuckAt() == m) return null;
        if (s.pending().isPresent() && s.pending().get().milestone() == m) return s.pending().get();
        var all = CoreRegistry.all(p.level().registryAccess());
        if (all.isEmpty()) return null;
        Set<String> pathIds = d.paths().stream().map(x -> x.id).collect(Collectors.toSet());
        Set<ResourceLocation> owned = new HashSet<>();
        s.picked().forEach(x -> owned.add(x.core()));
        CoreSlots.Pending pend = CoreRoller.roll(m, p.getRandom().nextLong(), luckOf(p, d), bestAptitude(d), pathIds, owned, all, s.bestTier());
        if (pend.ids().isEmpty()) return null; // cấp 100 nhưng chưa có đạo -> không có Thần Lõi để hiện
        set(p, s.withPending(pend));
        return pend;
    }

    /** Server xác thực lựa chọn (index 0..2 = ô thường, 3 = Phó Mặc Số Phận). */
    public static boolean choose(ServerPlayer p, int index) {
        CultivationData d = CultivationService.get(p);
        CoreSlots s = get(p);
        if (index < 0 || index > 3 || !d.milestoneLocked() || s.pending().isEmpty()) return false;
        CoreSlots.Pending pend = s.pending().get();
        int m = milestoneOf(d);
        if (pend.milestone() != m) return false;

        boolean than = m == CoreRoller.THAN_MILESTONE;
        if (than && (index > 2 || pend.ids().isEmpty())) return false; // Thần Lõi: không có ô úp
        if (index == 3 && pend.stuckRoll()) {
            set(p, s.withStuck(m));
            p.displayClientMessage(Component.translatable("hukhong.altar.stuck"), false);
            ((ServerLevel) p.level()).playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 0.6f);
            PacketDistributor.sendToPlayer(p, new CoreRevealPayload(3, true, CoreRevealPayload.NONE, 0));
            CoreSyncPayload.send(p);
            return true;
        }
        ResourceLocation core = index == 3 ? pend.fourth() : pend.ids().get(Math.min(index, pend.ids().size() - 1));
        int tier = than ? CoreTier.KIM.ordinal() : index == 3 ? pend.fourthTier() : pend.tier();
        set(p, s.withPicked(new CoreSlots.Picked(m, core, tier)));
        CultivationService.unlockMilestone(p);
        com.hukhong.stats.StatService.refresh(p);
        CoreSyncPayload.send(p);
        PacketDistributor.sendToPlayer(p, new CoreRevealPayload(index, false, core, tier));
        ringEffect(p, CoreTier.of(tier));
        return true;
    }

    // ---------------------------------------------------------------- hiệu ứng

    private static void ringEffect(ServerPlayer p, CoreTier t) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        Vector3f col = new Vector3f(((t.rgb >> 16) & 255) / 255f, ((t.rgb >> 8) & 255) / 255f, (t.rgb & 255) / 255f);
        DustParticleOptions dust = new DustParticleOptions(col, 1.4f);
        for (int ring = 0; ring < 3; ring++) {
            double r = 0.8 + ring * 0.6, y = p.getY() + 0.1 + ring * 0.9;
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI * 2 / 40;
                DustParticleOptions d = dust;
                if (t.rainbow()) { // phẩm cầu vồng: mỗi hạt một sắc (Thất Thải 7 sắc, Cửu Thải 9 sắc)
                    int bands = t == CoreTier.CUU_THAI ? 9 : 7;
                    int rgb = net.minecraft.util.Mth.hsvToRgb((i % bands) / (float) bands, 0.85f, 1f);
                    d = new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.4f);
                }
                sl.sendParticles(d, p.getX() + Math.cos(a) * r, y, p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        sl.sendParticles(com.hukhong.vfx.ModParticles.LINH_KHI.get(), p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        com.hukhong.vfx.ModSounds.play(sl, p, com.hukhong.vfx.ModSounds.CORE_RING, 1f, 1.0f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.6f + Math.min(t.ordinal(), 6) * 0.15f);
    }

    // ---------------------------------------------------------------- attribute

    /** Set bonus 2 món cùng Hệ (tối thiểu): {attribute, operation, giá trị}. */
    private record SetBonus(ResourceLocation attr, AttributeModifier.Operation op, double value) {}

    private static final Map<String, SetBonus> SET_BONUS = new HashMap<>();
    static {
        SET_BONUS.put("liet_diem", new SetBonus(modId("attack_power"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.10));
        SET_BONUS.put("huyet_ma", new SetBonus(vanilla("generic.max_health"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.05));
        SET_BONUS.put("tinh_than", new SetBonus(modId("spell_power"), AttributeModifier.Operation.ADD_VALUE, 2.0));
        SET_BONUS.put("cuong_the", new SetBonus(modId("defense"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.15));
        SET_BONUS.put("phong_loi", new SetBonus(vanilla("generic.movement_speed"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.10));
        SET_BONUS.put("thanh_quang", new SetBonus(vanilla("generic.max_health"), AttributeModifier.Operation.ADD_VALUE, 4.0));
    }

    private static ResourceLocation vanilla(String p) { return ResourceLocation.withDefaultNamespace(p); }

    private static ResourceLocation modId(String p) { return ResourceLocation.fromNamespaceAndPath("hukhong", p); }

    public static boolean hasSetBonus(String system, int count) { return count >= 2 && SET_BONUS.containsKey(system); }

    public static Map<String, Integer> systemCounts(CoreSlots s, java.util.function.Function<ResourceLocation, CoreDef> lookup) {
        Map<String, Integer> out = new HashMap<>();
        for (CoreSlots.Picked pk : s.picked()) {
            CoreDef def = lookup.apply(pk.core());
            if (def == null) continue;
            if (def.minTier() == CoreTier.KIM) SET_BONUS.keySet().forEach(k -> out.merge(k, 1, Integer::sum)); // Thần Lõi: +1 mọi Hệ
            else out.merge(def.system(), 1, Integer::sum);
        }
        return out;
    }

    /** Gỡ toàn bộ modifier Lõi rồi áp lại (login/đổi). Modifier là transient nên phải gọi lại sau login/respawn. */
    public static void applyAll(ServerPlayer p) {
        var access = p.level().registryAccess();
        var defs = CoreRegistry.all(access);
        CoreSlots s = get(p);

        Set<ResourceLocation> attrs = new HashSet<>();
        defs.values().forEach(c -> c.passives().forEach(ps -> attrs.add(ps.attribute())));
        SET_BONUS.values().forEach(b -> attrs.add(b.attr()));
        for (ResourceLocation a : attrs) {
            AttributeInstance inst = instance(p, a);
            if (inst == null) continue;
            for (int m = 1; m <= 12; m++) inst.removeModifier(modId("core/m" + m));
            for (String sys : SET_BONUS.keySet()) inst.removeModifier(modId("coreset/" + sys));
        }

        for (CoreSlots.Picked pk : s.picked()) {
            CoreDef def = defs.get(pk.core());
            if (def == null) continue;
            for (CoreDef.Passive ps : def.passives()) {
                AttributeInstance inst = instance(p, ps.attribute());
                if (inst == null) continue;
                inst.addTransientModifier(new AttributeModifier(modId("core/m" + pk.milestone()),
                        ps.valueAt(pk.coreTier()), ps.operation()));
            }
        }
        for (var en : systemCounts(s, defs::get).entrySet()) {
            if (!hasSetBonus(en.getKey(), en.getValue())) continue;
            SetBonus b = SET_BONUS.get(en.getKey());
            AttributeInstance inst = instance(p, b.attr());
            if (inst != null) inst.addTransientModifier(new AttributeModifier(modId("coreset/" + en.getKey()), b.value(), b.op()));
        }
    }

    private static AttributeInstance instance(ServerPlayer p, ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(id).map(p::getAttribute).orElse(null);
    }
}
