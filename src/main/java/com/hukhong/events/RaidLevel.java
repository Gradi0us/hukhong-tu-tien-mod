package com.hukhong.events;

import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Cap quai tap kich = cap trung binh nguoi choi trong 100 block quanh tam tap kich (+30% o vung nhiem). */
public final class RaidLevel {
    private RaidLevel() {}

    public static final double RADIUS = 100;

    /** Thuan: cap trung binh lam tron, kep [1, MAX_MOB_LEVEL]; vung nhiem +30%. */
    public static int fromAverage(double avg, boolean corrupted) {
        int lv = Math.max(1, Math.min(HuHoa.MAX_MOB_LEVEL, (int) Math.round(avg)));
        return CorruptBuff.level(corrupted, lv);
    }

    public static int raidLevel(ServerLevel sl, Vec3 center, ServerPlayer fallback, boolean corrupted) {
        long sum = 0;
        int n = 0;
        for (ServerPlayer q : sl.players()) {
            if (q.isSpectator() || !q.isAlive() || q.distanceToSqr(center) > RADIUS * RADIUS) continue;
            sum += CultivationService.get(q).level();
            n++;
        }
        if (n == 0) { sum = CultivationService.get(fallback).level(); n = 1; }
        return fromAverage(sum / (double) n, corrupted);
    }

    /** Dua quai HuMob ve cap {@code lv} (ghi lai cap + co gian HP/sat thuong/giap/toc do theo ti le cap), extraMul = he so them (vd 1.3 vung nhiem). */
    public static void apply(Mob m, int lv, boolean extraCorruptMul) {
        if (!(m instanceof HuMob h)) return;
        int old = h.huLevel();
        if (old != lv) {
            CompoundTag t = new CompoundTag();
            m.addAdditionalSaveData(t);
            t.putInt(HuHoa.TAG_LEVEL, lv);
            m.readAdditionalSaveData(t);
            if (h.huLevel() != lv) return; // loai quai khong luu cap qua NBT: giu nguyen
            scale(m, "hu_lv_hp", HuHoa.hpScale(lv) / HuHoa.hpScale(old) * (extraCorruptMul ? CorruptBuff.MUL : 1.0));
            scale(m, "hu_lv_atk", HuHoa.dmgScale(lv) / HuHoa.dmgScale(old) * (extraCorruptMul ? CorruptBuff.MUL : 1.0));
            setAdd(m, Attributes.ARMOR, "hu_lv_armor", Math.min(20, 0.25 * (lv - 1)));
            setAdd(m, Attributes.MOVEMENT_SPEED, "hu_lv_speed", Math.min(0.30, 0.005 * (lv - 1)));
        } else if (extraCorruptMul) {
            scale(m, "hu_lv_hp", CorruptBuff.MUL);
            scale(m, "hu_lv_atk", CorruptBuff.MUL);
        }
        m.setHealth(m.getMaxHealth());
    }

    private static net.minecraft.resources.ResourceLocation id(String s) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hukhong.HuKhong.MODID, s);
    }

    private static void scale(Mob m, String mod, double ratio) {
        Holder h = holder(mod);
        var inst = m.getAttribute(h.attr());
        if (inst == null) return;
        var cur = inst.getModifier(id(mod));
        double amount = cur == null ? 0 : cur.amount();
        inst.removeModifier(id(mod));
        inst.addPermanentModifier(new AttributeModifier(id(mod), (amount + 1) * ratio - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void setAdd(Mob m, net.minecraft.core.Holder<Attribute> a, String mod, double v) {
        var inst = m.getAttribute(a);
        if (inst == null) return;
        var cur = inst.getModifier(id(mod));
        var op = cur != null ? cur.operation() : (mod.endsWith("speed") ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE);
        inst.removeModifier(id(mod));
        inst.addPermanentModifier(new AttributeModifier(id(mod), v, op));
    }

    private record Holder(net.minecraft.core.Holder<Attribute> attr) {}

    private static Holder holder(String mod) {
        return new Holder(mod.equals("hu_lv_hp") ? Attributes.MAX_HEALTH : Attributes.ATTACK_DAMAGE);
    }
}
