package com.hukhong.artifact;

import com.hukhong.HuKhong;
import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.banmenh.BanMenhService;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Tiến hóa pháp khí các đạo bằng Hồn Tinh, đối xứng Bản Mệnh Kiếm: cùng bảng chi phí (BanMenhMath), trần theo cảnh giới.
 * Đạo = Pháp Bảo (cánh), Thể = Bá Thể, Ma = Ma Hồn. Kích hoạt: Shift + chuột phải bằng Hồn Tinh, hoặc nút ở màn K.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ArtifactService {
    private ArtifactService() {}

    private static final Path[] ORDER = {Path.DAO, Path.THE, Path.MA};

    public static ArtifactData get(Player p) { return p.getData(ArtifactAttachments.ARTIFACT.get()); }

    /** Đạo pháp khí mặc định cho Shift+chuột phải: đạo đã nhập chưa đầy, không thì đạo đầu tiên có pháp khí. */
    static Path defaultPath(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        ArtifactData a = get(p);
        Path first = null;
        for (Path path : ORDER) {
            if (!d.paths().contains(path)) continue;
            if (first == null) first = path;
            if (a.get(path) < ArtifactMath.MAX_STAGE) return path;
        }
        return first;
    }

    /** Tiến hóa 1 giai đoạn: kiểm đạo, trần cảnh giới, đủ Hồn Tinh (cả túi); trừ Hồn Tinh, lưu, tính lại chỉ số. */
    public static boolean tryUpgrade(ServerPlayer p, Path path) {
        if (!ArtifactMath.hasArtifact(path)) return false;
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        if (!d.paths().contains(path)) {
            msg(p, "hukhong.artifact.evolve.path", "Chưa nhập đạo này.");
            return false;
        }
        ArtifactData a = get(p);
        int cur = a.get(path);
        int have = BanMenhService.countTinh(p, false);
        int code = ArtifactMath.check(cur, d.level(), have);
        switch (code) {
            case BanMenhMath.MAXED -> { msg(p, "hukhong.artifact.evolve.max", "Pháp khí đã đạt Thần phẩm."); return false; }
            case BanMenhMath.REALM_CAP -> { msg(p, "hukhong.artifact.evolve.realm", "Cảnh giới chưa đủ để tiến hóa tiếp (trần theo cấp)."); return false; }
            case BanMenhMath.NEED_TINH -> {
                p.displayClientMessage(Component.translatableWithFallback("hukhong.artifact.evolve.need",
                        "Cần %s Hồn Tinh (hiện có %s).", ArtifactMath.tinhCost(cur + 1), have), true);
                return false;
            }
            default -> { }
        }
        BanMenhService.takeTinh(p, ArtifactMath.tinhCost(cur + 1), false);
        int ns = cur + 1;
        p.setData(ArtifactAttachments.ARTIFACT.get(), a.with(path, ns));
        StatService.refresh(p);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.5, 0.8, 0.5, 0.6);
            sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        p.sendSystemMessage(Component.translatableWithFallback("hukhong.artifact.evolve.done", "%s tiến hóa: %s",
                Component.translatable("hukhong.artifact." + path.id), Component.translatable("hukhong.banmenh.stage." + ns)));
        return true;
    }

    private static void msg(ServerPlayer p, String key, String vi) {
        p.displayClientMessage(Component.translatableWithFallback(key, vi), true);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !e.getItemStack().is(ModItems.HON_TINH.get())) return;
        if (!e.getEntity().isShiftKeyDown() || !(e.getEntity() instanceof ServerPlayer sp)) return;
        Path path = defaultPath(sp);
        if (path == null) return; // chưa có đạo nào có pháp khí (Kiếm dùng Bản Mệnh Kiếm)
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        tryUpgrade(sp, path);
    }

    // ------------------------------------------------------------------ chỉ số

    private static void mod(ServerPlayer p, Holder<Attribute> attr, String id, double v, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation rl = ModAttributes.id("artifact/" + id);
        if (v == 0) inst.removeModifier(rl);
        else inst.addOrUpdateTransientModifier(new AttributeModifier(rl, v, op));
    }

    /** Gỡ + áp lại bonus pháp khí (transient). Chỉ áp cho đạo đã nhập. Gọi từ StatService.refresh. */
    public static void applyPassives(ServerPlayer p) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        ArtifactData a = get(p);
        int dao = d.paths().contains(Path.DAO) ? BanMenhMath.effectiveStage(a.dao(), d.level()) : 0;
        int the = d.paths().contains(Path.THE) ? BanMenhMath.effectiveStage(a.the(), d.level()) : 0;
        int ma = d.paths().contains(Path.MA) ? BanMenhMath.effectiveStage(a.ma(), d.level()) : 0;
        var mulBase = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        var add = AttributeModifier.Operation.ADD_VALUE;
        mod(p, ModAttributes.holder(ModAttributes.RESOURCE_REGEN), "dao_regen", ArtifactMath.daoRegen(dao), mulBase);
        mod(p, ModAttributes.holder(ModAttributes.MAX_RESOURCE), "dao_max_resource", ArtifactMath.daoMaxResource(dao), mulBase);
        mod(p, ModAttributes.holder(ModAttributes.SPELL_POWER), "dao_spell", ArtifactMath.daoSpell(dao), mulBase);
        mod(p, Attributes.MAX_HEALTH, "the_health", ArtifactMath.theHealth(the), add);
        mod(p, ModAttributes.holder(ModAttributes.DEFENSE), "the_defense", ArtifactMath.theDefense(the), mulBase);
        mod(p, ModAttributes.holder(ModAttributes.ATTACK_POWER), "ma_attack", ArtifactMath.maAttack(ma), mulBase);
        mod(p, ModAttributes.holder(ModAttributes.LIFESTEAL), "ma_lifesteal", ArtifactMath.maLifesteal(ma), add);
    }
}
