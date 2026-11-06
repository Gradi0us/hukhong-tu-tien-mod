package com.hukhong.story.journey;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.LinhQuai;
import com.hukhong.creature.YeuThuAge;
import com.hukhong.resource.HerbDef;
import com.hukhong.resource.ResourceRegistry;
import com.hukhong.story.flow.FlowSteps;
import com.hukhong.story.flow.StoryFlow;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.objective.Objectives;
import com.hukhong.world.yeulam.CleansedForests;
import com.hukhong.world.yeulam.DaiSamLam;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lịch Luyện (2026-11-06): nhiệm vụ phụ có dẫn truyện chèn giữa các mốc cảnh giới, thay cho "cày tới cấp N" trơn.
 * Mỗi chương có bước JOURNEY bắt buộc (n nhiệm vụ, nhiệm vụ đầu theo cốt truyện chương), sau đó bước LEVEL vẫn tiếp tục
 * giao nhiệm vụ tùy chọn để người chơi luôn có việc: săn yêu thú, trừ tà, khám phá linh địa, giải phóng rừng bị Hư Không
 * xâm thực, hái linh thảo, săn yêu thú cổ niên. Trạng thái nằm trong cờ QuestData "lj:" (KIND|need|prog|x|z).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LichLuyen {
    private LichLuyen() {}

    public enum Task { SAN_YEU, TRU_MA, KHAM_PHA, GIAI_PHONG, HAI_THAO, SAN_HUNG }

    /** Nhiệm vụ cốt truyện đầu tiên của chương 1..9 (index = chương). */
    static final Task[] STORY = {null, Task.SAN_YEU, Task.KHAM_PHA, Task.GIAI_PHONG, Task.HAI_THAO, Task.SAN_HUNG,
            Task.GIAI_PHONG, Task.KHAM_PHA, Task.SAN_HUNG, Task.GIAI_PHONG, Task.GIAI_PHONG};

    public static final String F_TASK = "lj:", F_VISITED = "ljv:", F_COUNT = "ljc:", TAG_ELDER = "hk_lj_elder";
    public static final int REACH_R = 45;

    public record Cur(Task task, int need, int prog, int x, int z) {
        String enc() { return task.name() + "|" + need + "|" + prog + "|" + x + "|" + z; }

        static Cur dec(String s) {
            if (s == null || s.isEmpty()) return null;
            String[] a = s.split("\\|");
            try { return new Cur(Task.valueOf(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])); }
            catch (RuntimeException e) { return null; }
        }

        boolean hasTarget() { return task == Task.KHAM_PHA || task == Task.GIAI_PHONG || task == Task.SAN_HUNG && (x != 0 || z != 0); }
    }

    // ------------------------------------------------------------------ trạng thái
    static QuestData q(ServerPlayer p) { return DialogueService.quest(p); }

    public static Cur current(ServerPlayer p) { return Cur.dec(q(p).valueOf(F_TASK)); }

    static void put(ServerPlayer p, Cur c) { DialogueService.setQuest(p, q(p).with(F_TASK, c == null ? null : c.enc())); }

    static int count(ServerPlayer p) {
        try { return Integer.parseInt(q(p).valueOf(F_COUNT)); } catch (NumberFormatException e) { return 0; }
    }

    /** Bước tuyến chính có kèm Lịch Luyện: JOURNEY (bắt buộc) và LEVEL (tùy chọn, chương 1-10). */
    public static boolean applies(FlowSteps.Step s) {
        return s != null && (s.kind() == FlowSteps.Kind.JOURNEY || s.kind() == FlowSteps.Kind.LEVEL && s.chapter() >= 1);
    }

    static int chapter(ServerPlayer p) { return Math.max(1, Math.min(10, q(p).current())); }

    // ------------------------------------------------------------------ giao nhiệm vụ
    /** Có nhiệm vụ chưa thì giao (kèm lời dẫn). Gọi khi vào bước / mỗi giây. */
    public static void ensure(ServerPlayer p, FlowSteps.Step s) {
        if (!applies(s) || current(p) != null || !(p.level() instanceof ServerLevel)) return;
        int ch = chapter(p);
        boolean story = s.kind() == FlowSteps.Kind.JOURNEY && q(p).prog() == 0;
        Cur c = null;
        if (story) c = make(p, STORY[ch], ch);
        List<Task> pool = new ArrayList<>(List.of(Task.values()));
        java.util.Collections.shuffle(pool, new java.util.Random(p.getRandom().nextLong()));
        Task last = lastTask(p);
        for (int i = 0; c == null && i < pool.size(); i++) if (pool.get(i) != last) c = make(p, pool.get(i), ch);
        if (c == null) c = make(p, Task.TRU_MA, ch);
        put(p, c);
        DialogueService.setQuest(p, q(p).with("ljl:", c.task().name()));
        String intro = story ? "hukhong.lj.story." + ch : "hukhong.lj.intro." + c.task().name().toLowerCase() + "." + p.getRandom().nextInt(3);
        p.sendSystemMessage(Component.translatable("hukhong.lj.voice").withColor(0xC8A2FF)
                .append(Component.translatable(intro).withColor(0xE8DDFF)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    static Task lastTask(ServerPlayer p) {
        try { return Task.valueOf(q(p).valueOf("ljl:")); } catch (IllegalArgumentException e) { return null; }
    }

    /** Tạo nhiệm vụ; null nếu không khả thi (không tìm được linh địa phù hợp). */
    static Cur make(ServerPlayer p, Task t, int ch) {
        ServerLevel ow = p.server.overworld();
        return switch (t) {
            case SAN_YEU -> new Cur(t, 3 + ch / 2, 0, 0, 0);
            case TRU_MA -> new Cur(t, 8 + ch, 0, 0, 0);
            case HAI_THAO -> new Cur(t, 3 + ch / 3, 0, 0, 0);
            case SAN_HUNG -> {
                DaiSamLam.Region r = DaiSamLam.nearest(ow.getSeed(), p.getX(), p.getZ());
                yield new Cur(t, 1, 0, r == null ? 0 : r.cx(), r == null ? 0 : r.cz());
            }
            case KHAM_PHA -> {
                DaiSamLam.Region r = pickRegion(p, false);
                yield r == null ? null : new Cur(t, 1, 0, r.cx(), r.cz());
            }
            case GIAI_PHONG -> {
                DaiSamLam.Region r = pickRegion(p, true);
                yield r == null ? null : new Cur(t, 5 + ch / 2, 0, r.cx(), r.cz());
            }
        };
    }

    /** Đại Sâm Lâm gần nhất chưa từng khám phá (hoặc còn bị xâm thực nếu {@code corrupt}), quét 7x7 ô. */
    static DaiSamLam.Region pickRegion(ServerPlayer p, boolean corrupt) {
        ServerLevel ow = p.server.overworld();
        long seed = ow.getSeed();
        Set<String> seen = new HashSet<>(List.of(q(p).valueOf(F_VISITED).split(",")));
        int gx = Math.floorDiv((int) p.getX(), DaiSamLam.CELL), gz = Math.floorDiv((int) p.getZ(), DaiSamLam.CELL);
        DaiSamLam.Region best = null;
        double bd = Double.MAX_VALUE;
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            DaiSamLam.Region r = DaiSamLam.cell(seed, gx + i, gz + j);
            if (r == null) continue;
            if (corrupt ? !DaiSamLam.corruptedLive(ow, r) : seen.contains(r.cx() + ":" + r.cz())) continue;
            double d = Math.hypot(r.cx() - p.getX(), r.cz() - p.getZ());
            if (d < bd) { bd = d; best = r; }
        }
        return best;
    }

    // ------------------------------------------------------------------ mục tiêu
    /** Dòng mục tiêu: nhiệm vụ Lịch Luyện (+ mốc cảnh giới nếu là bước LEVEL). */
    public static boolean objective(ServerPlayer p, FlowSteps.Step s) {
        Cur c = current(p);
        if (!applies(s) || c == null) return false;
        Component t = Component.translatable("hukhong.lj.obj." + c.task().name().toLowerCase(), Math.min(c.prog(), c.need()), c.need());
        Component text = s.kind() == FlowSteps.Kind.JOURNEY
                ? Component.translatable("hukhong.lj.obj.journey", q(p).prog() + 1, s.n(), t)
                : Component.translatable("hukhong.lj.obj.level", t, s.n());
        BlockPos at = c.hasTarget() ? new BlockPos(c.x(), p.server.overworld().getSeaLevel(), c.z()) : null;
        Objectives.set(p, s.id(), text, at, at == null ? null : Level.OVERWORLD);
        return true;
    }

    // ------------------------------------------------------------------ tiến độ
    /** Mỗi giây (StoryFlow.onPlayerTick). */
    public static void tick(ServerPlayer p, FlowSteps.Step s) {
        if (!applies(s)) return;
        ensure(p, s);
        Cur c = current(p);
        if (c == null || !(p.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        switch (c.task()) {
            case KHAM_PHA -> {
                if (Math.hypot(p.getX() - c.x(), p.getZ() - c.z()) < REACH_R) finish(p, s, c);
            }
            case HAI_THAO -> {
                int n = herbs(p);
                if (n != c.prog()) { c = new Cur(c.task(), c.need(), n, c.x(), c.z()); put(p, c); objective(p, s); }
                if (n >= c.need()) finish(p, s, c);
            }
            case GIAI_PHONG -> {
                DaiSamLam.Region r = DaiSamLam.at(sl.getSeed(), p.getX(), p.getZ());
                if (r == null || r.cx() != c.x() || r.cz() != c.z()) return;
                if (p.tickCount % 100 < 20) spawnCorrupt(sl, p);
            }
            case SAN_HUNG -> {
                if (p.tickCount % 200 >= 20 || DaiSamLam.depth(sl.getSeed(), p.getX(), p.getZ()) < 0.25) return;
                boolean have = !sl.getEntitiesOfClass(LinhQuai.class, p.getBoundingBox().inflate(80), m -> m.getTags().contains(TAG_ELDER)).isEmpty();
                if (!have) spawnElder(sl, p, chapter(p));
            }
            default -> { }
        }
    }

    static Set<Item> HERB_ITEMS;

    static int herbs(ServerPlayer p) {
        if (HERB_ITEMS == null) {
            Set<Item> s = new HashSet<>();
            for (String id : HerbDef.IDS) BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id)).ifPresent(s::add);
            HERB_ITEMS = s;
        }
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (HERB_ITEMS.contains(st.getItem())) n += st.getCount();
        return n;
    }

    static BlockPos around(ServerLevel sl, ServerPlayer p, double dmin, double dmax) {
        double a = sl.random.nextDouble() * Math.PI * 2, d = dmin + sl.random.nextDouble() * (dmax - dmin);
        int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
        if (!sl.hasChunk(x >> 4, z >> 4)) return null;
        BlockPos bp = new BlockPos(x, sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        return sl.getFluidState(bp.below()).isEmpty() && sl.getBlockState(bp).isAir() ? bp : null;
    }

    /** Trong rừng cần giải phóng: luôn có quái cây Hư Hóa quanh người chơi (≤3). */
    static void spawnCorrupt(ServerLevel sl, ServerPlayer p) {
        if (sl.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        int near = sl.getEntitiesOfClass(LinhQuai.class, p.getBoundingBox().inflate(40), LinhQuai::corrupted).size();
        if (near >= 3) return;
        BlockPos bp = around(sl, p, 16, 30);
        if (bp == null) return;
        CreatureKind k = CreatureKind.PLANTS.get(sl.random.nextInt(CreatureKind.PLANTS.size()));
        LinhQuai m = DaiSamLam.spawnBeast(sl, k, bp, YeuThuAge.roll(0.3 + 0.07 * chapter(p), sl.random), MobSpawnType.EVENT);
        if (m != null) {
            m.setCorrupted(true);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, m.getX(), m.getY() + 1, m.getZ(), 40, 0.8, 1.2, 0.8, 0.1);
        }
    }

    /** Tuổi tối thiểu của yêu thú cổ niên theo chương. */
    public static YeuThuAge elderAge(int ch) { return ch <= 2 ? YeuThuAge.Y100 : ch <= 6 ? YeuThuAge.Y1000 : YeuThuAge.Y10000; }

    static void spawnElder(ServerLevel sl, ServerPlayer p, int ch) {
        BlockPos bp = around(sl, p, 26, 40);
        if (bp == null) return;
        CreatureKind k = CreatureKind.COMMON_BEASTS.get(sl.random.nextInt(CreatureKind.COMMON_BEASTS.size()));
        LinhQuai m = DaiSamLam.spawnBeast(sl, k, bp, elderAge(ch), MobSpawnType.EVENT);
        if (m == null) return;
        m.addTag(TAG_ELDER);
        m.setPersistenceRequired();
        p.sendSystemMessage(Component.translatable("hukhong.lj.elder_appear").withColor(0xFFB347));
        sl.playSound(null, bp, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.6f);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !(e.getSource().getEntity() instanceof ServerPlayer p) || !StoryFlow.active(p)) return;
        FlowSteps.Step s = StoryFlow.current(p);
        Cur c = current(p);
        if (!applies(s) || c == null) return;
        boolean counts = switch (c.task()) {
            case SAN_YEU -> v instanceof LinhQuai m && m.kind().beast();
            case TRU_MA -> v instanceof Enemy;
            case GIAI_PHONG -> v instanceof LinhQuai m && m.corrupted() && inRegion(v, c);
            case SAN_HUNG -> v instanceof LinhQuai m && m.kind().beast() && m.age() != null && m.age().ordinal() >= elderAge(chapter(p)).ordinal();
            default -> false;
        };
        if (!counts) return;
        c = new Cur(c.task(), c.need(), c.prog() + 1, c.x(), c.z());
        put(p, c);
        if (c.prog() >= c.need()) finish(p, s, c); else objective(p, s);
    }

    static boolean inRegion(LivingEntity v, Cur c) {
        DaiSamLam.Region r = DaiSamLam.at(((ServerLevel) v.level()).getSeed(), v.getX(), v.getZ());
        return r != null && r.cx() == c.x() && r.cz() == c.z();
    }

    // ------------------------------------------------------------------ hoàn thành
    static void finish(ServerPlayer p, FlowSteps.Step s, Cur c) {
        put(p, null);
        QuestData qd = q(p).with(F_COUNT, Integer.toString(count(p) + 1));
        if (c.task() == Task.KHAM_PHA) {
            String v = qd.valueOf(F_VISITED);
            List<String> l = new ArrayList<>(v.isEmpty() ? List.of() : List.of(v.split(",")));
            l.add(c.x() + ":" + c.z());
            while (l.size() > 12) l.remove(0);
            qd = qd.with(F_VISITED, String.join(",", l));
        }
        DialogueService.setQuest(p, qd);
        int lv = CultivationService.get(p).level();
        double mul = (s.kind() == FlowSteps.Kind.JOURNEY ? 0.35 : 0.22) * (c.task() == Task.GIAI_PHONG ? 2 : 1);
        CultivationService.addExp(p, Math.max(20, Math.round(CultivationService.expToNext(Math.max(1, lv)) * mul)));
        List<String> ld = new ArrayList<>(ResourceRegistry.LINH_DUOC.keySet());
        if (!ld.isEmpty()) {
            int n = c.task() == Task.GIAI_PHONG ? 2 : 1;
            for (int i = 0; i < n; i++) {
                ItemStack st = new ItemStack(ResourceRegistry.LINH_DUOC.get(ld.get(p.getRandom().nextInt(ld.size()))).get());
                if (!p.getInventory().add(st)) p.drop(st, false);
            }
        }
        if (c.task() == Task.GIAI_PHONG) cleanse(p, c);
        p.sendSystemMessage(Component.translatable("hukhong.lj.done." + c.task().name().toLowerCase()).withColor(0x9CFFB0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
        if (s.kind() == FlowSteps.Kind.JOURNEY) {
            int done = q(p).prog() + 1;
            DialogueService.setQuest(p, q(p).withProg(done));
            if (done >= s.n()) { StoryFlow.complete(p, s.id()); return; }
        }
        ensure(p, s);
        objective(p, s);
    }

    /** Giải phóng rừng: ghi nhận vĩnh viễn, quái Hư Hóa gần đó tan khí Hư Không, hiệu ứng thanh tẩy. */
    static void cleanse(ServerPlayer p, Cur c) {
        ServerLevel ow = p.server.overworld();
        DaiSamLam.Region r = DaiSamLam.at(ow.getSeed(), c.x(), c.z());
        CleansedForests.get(ow).add(r);
        if (p.level() == ow) {
            for (LinhQuai m : ow.getEntitiesOfClass(LinhQuai.class, p.getBoundingBox().inflate(96), LinhQuai::corrupted)) {
                m.setCorrupted(false);
                ow.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + 1, m.getZ(), 25, 0.6, 1, 0.6, 0.08);
            }
            for (int i = 0; i < 6; i++)
                ow.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 40, 6 + i * 3, 2, 6 + i * 3, 0.1);
            ow.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 2, p.getZ(), 1, 0, 0, 0, 0);
            ow.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.2f);
        }
        p.server.getPlayerList().broadcastSystemMessage(Component.translatable("hukhong.lj.cleansed", p.getDisplayName(), c.x(), c.z()).withColor(0x7FFFD4), false);
    }
}
