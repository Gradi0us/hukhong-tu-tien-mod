package com.hukhong.world;

import com.hukhong.HuKhong;
import com.hukhong.vfx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Hoạt ảnh cây đổ (2026-11-06): cả cây (gỗ + lá) thành khối hiển thị (BlockDisplay) xoay quanh gốc như vật cứng,
 * ngả rạp nhanh dần theo hướng bị húc, đập đất (bụi + rung + tiếng), nằm một lúc rồi tan thành lá và rơi gỗ.
 * Client tự nội suy chuyển động giữa các bước (interpolation) nên mượt.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TreeFallAnim {
    private TreeFallAnim() {}

    public static final int FALL_TICKS = 24, STEP = 2, LIE_TICKS = 30;
    /** Góc ngả tối đa (gần nằm hẳn). */
    public static final float MAX_ANGLE = (float) Math.toRadians(84);

    record Part(Display.BlockDisplay e, Vector3f off, BlockState state) {}

    static final class Fall {
        final ServerLevel lv;
        final Vec3 pivot;
        final Vector3f axis;
        final Vec3 dir;
        final List<Part> parts = new ArrayList<>();
        final float height;
        int age;

        Fall(ServerLevel lv, Vec3 pivot, Vec3 dir, float height) {
            this.lv = lv; this.pivot = pivot; this.dir = dir; this.height = height;
            this.axis = new Vector3f((float) dir.z, 0, (float) -dir.x).normalize(); // up x dir: ngọn ngả về phía dir
        }
    }

    static final List<Fall> FALLS = new ArrayList<>();

    /** Bắt đầu hoạt ảnh đổ cho các khối cây (đã kiểm là cây tự nhiên); tự đặt các khối thành không khí. */
    static void start(ServerLevel lv, List<BlockPos> tree, Vec3 from) {
        BlockPos base = null;
        double cx = 0, cz = 0;
        int top = Integer.MIN_VALUE;
        for (BlockPos p : tree) if (lv.getBlockState(p).is(BlockTags.LOGS) && (base == null || p.getY() < base.getY())) base = p;
        if (base == null) base = tree.get(0);
        for (BlockPos p : tree) {
            cx += p.getX() + 0.5; cz += p.getZ() + 0.5; top = Math.max(top, p.getY());
        }
        cx /= tree.size(); cz /= tree.size();
        Vec3 pivot = new Vec3(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        Vec3 dir = new Vec3(cx - from.x, 0, cz - from.z);
        if (dir.lengthSqr() < 1e-3) dir = new Vec3(lv.random.nextDouble() - 0.5, 0, lv.random.nextDouble() - 0.5);
        dir = dir.normalize();
        Fall f = new Fall(lv, pivot, dir, top - base.getY() + 1);
        for (BlockPos p : tree) {
            BlockState s = lv.getBlockState(p);
            if (s.isAir()) continue;
            Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(lv);
            if (d == null) continue;
            Vector3f off = new Vector3f((float) (p.getX() - pivot.x), (float) (p.getY() - pivot.y), (float) (p.getZ() - pivot.z));
            d.moveTo(pivot.x, pivot.y, pivot.z, 0, 0);
            CompoundTag t = d.saveWithoutId(new CompoundTag());
            t.put("block_state", NbtUtils.writeBlockState(s));
            t.put("transformation", transform(off, new Quaternionf()));
            t.putFloat("view_range", 2.0f);
            int bl = lv.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p), sk = lv.getBrightness(net.minecraft.world.level.LightLayer.SKY, p.above(20));
            CompoundTag br = new CompoundTag(); br.putInt("block", bl); br.putInt("sky", Math.max(sk, 12)); // sáng theo trời, không tối đen khi nằm xuống
            t.put("brightness", br);
            d.load(t);
            lv.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
            lv.addFreshEntity(d);
            f.parts.add(new Part(d, off, s));
        }
        if (f.parts.isEmpty()) return;
        lv.playSound(null, base, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.4f, 0.5f);
        lv.playSound(null, base, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
        lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, f.parts.get(0).state), pivot.x, pivot.y + 0.5, pivot.z, 20, 0.4, 0.4, 0.4, 0.1);
        FALLS.add(f);
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float x : v) l.add(FloatTag.valueOf(x));
        return l;
    }

    private static CompoundTag transform(Vector3f translation, Quaternionf rot) {
        CompoundTag t = new CompoundTag();
        t.put("translation", floats(translation.x, translation.y, translation.z));
        t.put("left_rotation", floats(rot.x, rot.y, rot.z, rot.w));
        t.put("scale", floats(1, 1, 1));
        t.put("right_rotation", floats(0, 0, 0, 1));
        return t;
    }

    private static void pose(Part p, Quaternionf rot, int lerp) {
        Vector3f tr = rot.transform(new Vector3f(p.off));
        CompoundTag t = p.e.saveWithoutId(new CompoundTag());
        t.put("transformation", transform(tr, rot));
        t.putInt("interpolation_duration", lerp);
        t.putInt("start_interpolation", 0);
        p.e.load(t);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (FALLS.isEmpty()) return;
        for (Iterator<Fall> it = FALLS.iterator(); it.hasNext(); ) {
            Fall f = it.next();
            f.age++;
            if (f.age <= FALL_TICKS && f.age % STEP == 0) {
                float k = (float) f.age / FALL_TICKS;
                float ang = MAX_ANGLE * k * k; // nhanh dần như rơi thật
                Quaternionf q = new Quaternionf().rotateAxis(ang, f.axis.x, f.axis.y, f.axis.z);
                for (Part p : f.parts) pose(p, q, STEP);
                if (f.age % 6 == 0) f.lv.playSound(null, f.pivot.x, f.pivot.y, f.pivot.z, SoundEvents.BAMBOO_WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 0.5f);
            }
            if (f.age == FALL_TICKS + 1) impact(f);
            if (f.age == FALL_TICKS + 4) { // nảy nhẹ khi chạm đất
                Quaternionf q = new Quaternionf().rotateAxis(MAX_ANGLE - 0.06f, f.axis.x, f.axis.y, f.axis.z);
                for (Part p : f.parts) pose(p, q, 3);
            }
            if (f.age == FALL_TICKS + 7) {
                Quaternionf q = new Quaternionf().rotateAxis(MAX_ANGLE, f.axis.x, f.axis.y, f.axis.z);
                for (Part p : f.parts) pose(p, q, 3);
            }
            if (f.age >= FALL_TICKS + LIE_TICKS) { vanish(f); it.remove(); }
        }
    }

    /** Vị trí thế giới của khối sau khi đổ. */
    private static Vec3 landed(Fall f, Part p) {
        Quaternionf q = new Quaternionf().rotateAxis(MAX_ANGLE, f.axis.x, f.axis.y, f.axis.z);
        Vector3f c = q.transform(new Vector3f(p.off).add(0.5f, 0.5f, 0.5f));
        return f.pivot.add(c.x, c.y, c.z);
    }

    private static void impact(Fall f) {
        ServerLevel lv = f.lv;
        Vec3 tip = f.pivot.add(f.dir.scale(f.height * 0.9));
        for (int i = 0; i <= (int) f.height; i++) { // bụi đất dọc thân cây vừa đập xuống
            Vec3 at = f.pivot.add(f.dir.scale(i));
            lv.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.3, at.z, 2, 0.4, 0.1, 0.4, 0.01);
            lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, lv.getBlockState(BlockPos.containing(at).below())),
                    at.x, at.y + 0.2, at.z, 10, 0.6, 0.15, 0.6, 0.15);
        }
        for (Part p : f.parts) if (!p.state.is(BlockTags.LOGS) && lv.random.nextInt(4) == 0) {
            Vec3 at = landed(f, p);
            lv.sendParticles(ParticleTypes.CHERRY_LEAVES, at.x, at.y, at.z, 2, 0.4, 0.3, 0.4, 0.02);
        }
        Fx.shake(lv, tip, (float) Math.min(1.0, 0.3 + f.height / 20.0), 10, 18f);
        lv.playSound(null, tip.x, tip.y, tip.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.7f, 0.5f);
        lv.playSound(null, tip.x, tip.y, tip.z, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 2f, 0.6f);
        lv.playSound(null, tip.x, tip.y, tip.z, SoundEvents.WOOD_FALL, SoundSource.BLOCKS, 2f, 0.4f);
    }

    /** Tan rã: lá vỡ, rơi lại 1/3 số gỗ tại chỗ thân nằm. */
    private static void vanish(Fall f) {
        int i = 0;
        for (Part p : f.parts) {
            Vec3 at = landed(f, p);
            boolean log = p.state.is(BlockTags.LOGS);
            if (log || lv(f).random.nextInt(3) == 0)
                f.lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, p.state), at.x, at.y, at.z, log ? 6 : 3, 0.3, 0.3, 0.3, 0.05);
            if (log && i++ % 3 == 0) Block.popResource(f.lv, BlockPos.containing(at), new net.minecraft.world.item.ItemStack(p.state.getBlock().asItem()));
            p.e.discard();
        }
        f.lv.playSound(null, f.pivot.x, f.pivot.y, f.pivot.z, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.2f, 0.8f);
    }

    private static ServerLevel lv(Fall f) { return f.lv; }
}
