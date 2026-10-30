package com.hukhong.poi;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.core.Path;
import com.hukhong.currency.Wallet;
import com.hukhong.effect.ModEffects;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Tuong tac voi diem hanh dong cua dia diem (chuot phai khoi), tieu nhi, va chan pha khoi quan trong. Moi hanh dong la ham public de GameTest goi truc tiep. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PoiActions {
    private PoiActions() {}

    public static final int DAY = 24000;

    // ------------------------------------------------------------------ su kien
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel l = p.serverLevel();
        if (l.dimension() != Level.OVERWORLD) return;
        PoiSavedData data = PoiSavedData.get(l);
        PoiInstance.Act a = data.actAt(e.getPos());
        if (a == null) return;
        PoiInstance i = data.ownerOf(e.getPos());
        if (i == null || !useAct(p, l, data, i, a)) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel l) || l.dimension() != Level.OVERWORLD || e.getPlayer().isCreative()) return;
        if (PoiSavedData.get(l).actAt(e.getPos()) != null) {
            e.setCanceled(true);
            e.getPlayer().displayClientMessage(Component.translatable("hukhong.poi.protected"), true);
        }
    }

    @SubscribeEvent
    public static void onTalk(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (!(e.getTarget() instanceof Villager v)) return;
        String key = v.getPersistentData().getString(PoiService.NPC_TAG);
        if (key.isEmpty()) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        ServerLevel ow = p.getServer().overworld();
        PoiInstance inn = PoiSavedData.get(ow).get(key);
        if (inn != null) talk(p, ow, PoiSavedData.get(ow), inn);
    }

    // ------------------------------------------------------------------ logic
    /** true = da xu ly (huy tuong tac mac dinh cua khoi). */
    public static boolean useAct(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, PoiInstance.Act a) {
        PoiDef d = PoiDefs.get(i.defId);
        if (d == null) return false;
        String k = a.kind();
        if (k.startsWith("sp_") || k.equals("center") || k.equals("spring") || k.equals("innkeeper") || k.equals("inn_door") || k.equals("meditate") || k.equals("rift")) return false;
        switch (k) {
            case "stele" -> stele(p, ow, data, i, d);
            case "offer" -> offer(p, ow, data, i, d);
            case "tablet" -> tablet(p, i);
            case "vault" -> vault(p, ow, data, i, d);
            case "grave" -> grave(p, ow, data, i, d);
            case "claim" -> claim(p, ow, data, i, d);
            case "lore_tablet" -> loreTablet(p, i, d);
            case "chest" -> {
                if (i.state == PoiInstance.CLEARED) return false;
                p.displayClientMessage(Component.translatable("hukhong.poi.guarded"), true);
            }
            default -> {
                if (k.startsWith("lever:")) pressLever(p, ow, data, i, Integer.parseInt(k.substring(6)));
                else if (k.startsWith("pillar:")) pressPillar(p, ow, data, i, Integer.parseInt(k.substring(7)));
                else return false;
            }
        }
        return true;
    }

    private static String cdKey(ServerPlayer p, String what) { return "cd:" + p.getUUID() + ":" + what; }

    public static void addExpFrac(ServerPlayer p, double frac) {
        CultivationService.addExp(p, Math.max(5, (long) (CultivationService.expToNext(CultivationService.get(p).level()) * frac)));
    }

    /** A1: doc loi bia (lore), chi dan toi hang CHG chua ha hoac nguon Di thu gan nhat chua nhat. */
    public static void stele(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d) {
        if (!StoryHooks.allowed(p, d.lore())) { p.displayClientMessage(Component.translatable("hukhong.poi.stele.faded"), true); return; }
        p.sendSystemMessage(Component.translatable(d.textKey() + "." + Math.floorMod(i.seed, 3)));
        String ck = cdKey(p, "read");
        if (!i.extra.getBoolean(ck)) {
            i.extra.putBoolean(ck, true);
            data.setDirty();
            addExpFrac(p, 0.02);
            StoryHooks.complete(p, d.lore());
        }
        PoiRumor.showLead(p, ow, data, PoiRumor.lead(p, ow, data, i));
    }

    /** A4: dang huong (Tu Tinh) -> buff may man 20 phut; moi nguoi 1 lan / ngay Lich. */
    public static void offer(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d) {
        long now = ow.getGameTime();
        String ck = cdKey(p, "offer");
        if (i.extra.getLong(ck) > now) { p.displayClientMessage(Component.translatable("hukhong.poi.offer.again"), true); return; }
        long cost = d.intParam("cost", 5);
        if (!Wallet.pay(p, cost)) { p.displayClientMessage(Component.translatable("hukhong.poi.offer.poor", cost), true); return; }
        i.extra.putLong(ck, now + DAY);
        data.setDirty();
        int ticks = d.intParam("buff_ticks", 24000);
        p.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, true));
        ow.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
        p.sendSystemMessage(Component.translatable("hukhong.poi.offer.ok", ticks / 1200));
        if (StoryHooks.allowed(p, d.lore())) StoryHooks.complete(p, d.lore());
    }

    /** A2 / A15: bia goi y (phan tu khoi dau). */
    public static void tablet(ServerPlayer p, PoiInstance i) {
        boolean cave = i.firstAct("vault") != null;
        int n = cave ? 4 : 5;
        int el = PoiPuzzle.elements(n)[PoiPuzzle.start(i.seed, n)];
        Component elem = Component.translatable("hukhong.poi.elem." + PoiPuzzle.ELEM_KEYS[el]);
        if (cave) p.sendSystemMessage(Component.translatable("hukhong.poi.cave.tablet", elem, Component.translatable("hukhong.poi.path." + PoiBuilders.sealPath(i.seed).id)));
        else p.sendSystemMessage(Component.translatable("hukhong.poi.wuxing.tablet", elem));
    }

    /** A2: bam can gat theo dung vong tuong sinh; sai thi tat ca ve vi tri tat. */
    public static void pressLever(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, int slot) {
        if (i.solved) { p.displayClientMessage(Component.translatable("hukhong.poi.cave.open"), true); return; }
        PoiPuzzle.Step st = PoiPuzzle.press(PoiPuzzle.slotOrder(i.seed, 4), i.progress, slot);
        PoiInstance.Act a = i.firstAct("lever:" + slot);
        if (st.reset()) {
            i.progress = 0;
            PoiBuilders.resetLevers(ow, i);
            ow.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.6f);
            p.displayClientMessage(Component.translatable("hukhong.poi.puzzle.wrong"), true);
        } else {
            i.progress = st.progress();
            if (a != null) PoiBuilders.setLever(ow, a, true);
            ow.playSound(null, p.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (st.solved()) {
                i.solved = true;
                i.stateTick = ow.getGameTime();
                PoiBuilders.setSeal(ow, i, true);
                ow.playSound(null, p.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0f, 0.7f);
                p.sendSystemMessage(Component.translatable("hukhong.poi.cave.solved"));
            } else p.displayClientMessage(Component.translatable("hukhong.poi.puzzle.step", st.progress(), 4), true);
        }
        data.setDirty();
    }

    /** A2: kho bau sau an khoa (cua khoa theo dao): can da giai do + dung dao; sach ky nang. Moi nguoi 1 lan / chu ky. */
    public static void vault(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d) {
        if (!i.solved) { p.displayClientMessage(Component.translatable("hukhong.poi.cave.sealed"), true); return; }
        Path seal = PoiBuilders.sealPath(i.seed);
        if (!CultivationService.get(p).paths().contains(seal)) {
            p.displayClientMessage(Component.translatable("hukhong.poi.cave.path", Component.translatable("hukhong.poi.path." + seal.id)), true);
            return;
        }
        String ck = "claimed:" + p.getUUID();
        if (i.extra.getBoolean(ck)) { p.displayClientMessage(Component.translatable("hukhong.poi.claimed"), true); return; }
        i.extra.putBoolean(ck, true);
        data.setDirty();
        PoiRewards.giveAll(p, d, ow.random);
        p.sendSystemMessage(Component.translatable("hukhong.poi.cave.reward"));
        if (StoryHooks.allowed(p, d.lore())) StoryHooks.complete(p, d.lore());
    }

    /** A20: mo mo - thuong / nguyen rua / am binh. */
    public static void grave(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d) {
        if (i.state != PoiInstance.READY) { p.displayClientMessage(Component.translatable("hukhong.poi.grave.opened"), true); return; }
        i.state = PoiInstance.CLEARED;
        i.stateTick = ow.getGameTime();
        data.setDirty();
        double r = ow.random.nextDouble();
        if (r < d.dblParam("p_reward", 0.55)) {
            PoiRewards.giveAll(p, d, ow.random);
            p.sendSystemMessage(Component.translatable("hukhong.poi.grave.reward"));
        } else if (r < d.dblParam("p_reward", 0.55) + d.dblParam("p_curse", 0.25)) {
            int t = d.intParam("curse_ticks", 3600);
            p.addEffect(new MobEffectInstance(ModEffects.HU_HOA, t, 0));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t, 0));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            p.sendSystemMessage(Component.translatable("hukhong.poi.grave.curse"));
        } else {
            PoiMobs.spawnAll(ow, i, d, ow.random);
            p.sendSystemMessage(Component.translatable("hukhong.poi.grave.ghosts"));
        }
        if (StoryHooks.allowed(p, d.lore())) StoryHooks.complete(p, d.lore());
    }

    /** A13: giang dao (cooldown / ngay Lich) hoac A15: ruong sau khi xong tran (can da giai do). */
    public static void claim(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d) {
        boolean puzzle = i.firstAct("pillar:0") != null;
        if (puzzle && !i.solved) { p.displayClientMessage(Component.translatable("hukhong.poi.wuxing.dormant"), true); return; }
        long now = ow.getGameTime();
        String ck = puzzle ? "claimed:" + p.getUUID() : cdKey(p, "claim");
        if (puzzle ? i.extra.getBoolean(ck) : i.extra.getLong(ck) > now) { p.displayClientMessage(Component.translatable("hukhong.poi.claimed"), true); return; }
        if (puzzle) i.extra.putBoolean(ck, true); else i.extra.putLong(ck, now + d.intParam("cooldown", DAY));
        data.setDirty();
        if (!puzzle) addExpFrac(p, d.dblParam("exp_frac", 0.04));
        PoiRewards.giveAll(p, d, ow.random);
        p.sendSystemMessage(Component.translatable(puzzle ? "hukhong.poi.wuxing.reward" : "hukhong.poi.sermon"));
        if (StoryHooks.allowed(p, d.lore())) StoryHooks.complete(p, d.lore());
    }

    /** A15: bam tru theo dung vong tuong sinh (5 tru). */
    public static void pressPillar(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance i, int slot) {
        if (i.solved) { p.displayClientMessage(Component.translatable("hukhong.poi.wuxing.done"), true); return; }
        PoiPuzzle.Step st = PoiPuzzle.press(PoiPuzzle.slotOrder(i.seed, 5), i.progress, slot);
        PoiInstance.Act a = i.firstAct("pillar:" + slot);
        if (st.reset()) {
            i.progress = 0;
            PoiBuilders.resetPillars(ow, i, i.seed);
            ow.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.6f);
            p.displayClientMessage(Component.translatable("hukhong.poi.puzzle.wrong"), true);
        } else {
            i.progress = st.progress();
            if (a != null) PoiBuilders.setPillarLit(ow, a, true);
            ow.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.6f + 0.15f * st.progress());
            if (st.solved()) {
                i.solved = true;
                i.stateTick = ow.getGameTime();
                p.sendSystemMessage(Component.translatable("hukhong.poi.wuxing.solved"));
            } else p.displayClientMessage(Component.translatable("hukhong.poi.puzzle.step", st.progress(), 5), true);
        }
        data.setDirty();
    }

    /** A8: bia trong phong boss - co qc.noi_gian.c.expose moi doc duoc phan bi mat (Mac Tung). */
    public static void loreTablet(ServerPlayer p, PoiInstance i, PoiDef d) {
        String flag = d.strParam("gate_flag", "qc.noi_gian.c.expose");
        if (DialogueService.quest(p).has(flag)) {
            p.sendSystemMessage(Component.translatable(d.textKey() + ".secret"));
            StoryHooks.complete(p, d.lore());
        } else p.sendSystemMessage(Component.translatable(d.textKey()));
    }

    /** Tieu nhi ke tin don: tro toi dia diem chua kham pha hoac buoc truyen ke tiep. */
    public static void talk(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance inn) {
        PoiService.discover(p, inn);
        PoiRumor.Rumor r = PoiRumor.pick(p, data, inn);
        p.sendSystemMessage(Component.translatable("hukhong.poi.npc.say", Component.translatable("hukhong.poi.npc.tieu_nhi"), r.text()));
        if (r.target() != null) PoiService.discover(p, r.target());
        PoiDef d = PoiDefs.get(inn.defId);
        if (d != null && StoryHooks.allowed(p, d.lore())) StoryHooks.complete(p, d.lore());
    }

    /** Vi tri chuot phai -> BlockPos tien ich cho test. */
    public static BlockPos posOf(PoiInstance.Act a) { return a.pos(); }
}
