package com.hukhong.linhbao;

import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.pathrules.MaRules;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sát thương cộng thêm của Linh Thể / Ma Công Bá Thể Quyết lên đòn thường. Một điểm gọi duy nhất: PathRules.onIncoming. */
public final class LinhBaoCombat {
    private LinhBaoCombat() {}

    private static final DustParticleOptions BRONZE = new DustParticleOptions(new Vector3f(1.0f, 0.62f, 0.2f), 0.9f);
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.04f, 0.2f), 0.9f);
    private static final Map<UUID, Long> FX_CD = new ConcurrentHashMap<>();

    public static void forget(UUID id) { FX_CD.remove(id); }

    /**
     * Phần sát thương cộng thêm cho đòn vừa resolve (after = sát thương sau DamageRules.resolve). 0 nếu không phải đòn thường cận chiến
     * (kỹ năng, đạn, cung...) hoặc đòn bị luật đạo triệt (after <= 0, vd Thể cầm vũ khí / Đạo đánh quái).
     * Cộng SAU bạo kích (không nhân thêm bạo kích) và TRƯỚC Trọng Thương (TrongThuong ở mức LOWEST) nên vẫn được khuếch đại bình thường.
     */
    public static float basicBonus(ServerPlayer atk, LivingEntity target, DamageSource src, float after) {
        if (after <= 0f || DamageRules.isSkill() || src.getDirectEntity() != atk) return 0f;
        var paths = atk.getData(ModAttachments.CULTIVATION).paths();
        boolean the = paths.contains(Path.THE), ma = paths.contains(Path.MA);
        if (!the && !ma) return 0f;
        double ap = StatService.attackPower(atk);
        double bonus = 0;
        if (the) {
            int st = LinhBao.stage(atk, LinhBaoType.LINH_THE);
            double b = LinhBaoMath.theHit(ap, st);
            bonus += b;
            fx(atk, target, LinhBaoType.LINH_THE, st);
        }
        if (ma) {
            int st = LinhBao.stage(atk, LinhBaoType.MA_CONG);
            double b = LinhBaoMath.maHit(ap, st);
            bonus += b;
            // Ác Tính: Mất Kiểm Soát tắt Hấp Huyết (cùng luật MaRules); phần Công đã tăng theo Ác Tính qua thuộc tính Công (có trần T6)
            if (!MaRules.noLifesteal(atk) && atk.getHealth() < atk.getMaxHealth()) {
                atk.heal((float) Math.min(b * LinhBaoMath.maLifesteal(st), atk.getMaxHealth() * LinhBaoMath.LS_HEAL_CAP_PCT));
            }
            fx(atk, target, LinhBaoType.MA_CONG, st);
        }
        return (float) bonus;
    }

    private static void fx(ServerPlayer atk, LivingEntity t, LinhBaoType type, int stage) {
        if (!(atk.level() instanceof ServerLevel lv)) return;
        long now = lv.getGameTime();
        Long last = FX_CD.get(atk.getUUID());
        if (last != null && now - last < 6) return;
        FX_CD.put(atk.getUUID(), now);
        Vec3 c = t.position().add(0, t.getBbHeight() * 0.55, 0);
        int n = 3 + stage;
        if (type == LinhBaoType.LINH_THE) {
            lv.sendParticles(BRONZE, c.x, c.y, c.z, n, 0.25, 0.3, 0.25, 0.02);
            if (stage >= 3) lv.sendParticles(ModParticles.BA_KHI.get(), c.x, c.y, c.z, 2, 0.2, 0.3, 0.2, 0.01);
        } else {
            lv.sendParticles(BLOOD, c.x, c.y, c.z, n, 0.25, 0.3, 0.25, 0.02);
            if (stage >= 3) lv.sendParticles(ModParticles.MA_KHI.get(), c.x, c.y, c.z, 2, 0.2, 0.3, 0.2, 0.01);
        }
    }
}
