package com.hukhong.onboarding;

import com.hukhong.HuKhong;
import com.hukhong.city.CitySavedData;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuMob;
import com.hukhong.network.OnboardingHintPayload;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Chuỗi 5 gợi ý cho người chơi mới (sau khi tạo nhân vật xong). Tắt được bằng /hukguide off. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class OnboardingService {
    private OnboardingService() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        OnboardingData o = p.getData(OnboardingAttachments.ONBOARDING);
        if (o.disabled() || o.step() >= OnboardingData.TOTAL) return;
        CultivationData c = CultivationService.get(p);
        if (c.paths().isEmpty()) return; // chưa tạo nhân vật xong
        long now = p.level().getGameTime();
        long el = now - o.shownAt();
        boolean next = switch (o.step()) {
            case 0 -> true;
            case 1 -> el >= 600;
            case 2 -> el >= 300 && (o.killed() || c.bottleneck() || el >= 2400);
            case 3 -> el >= 600;
            case 4 -> el >= 300 && (isResident(p) || el >= 2400);
            default -> false;
        };
        if (next) show(p, o.step() + 1);
    }

    private static boolean isResident(ServerPlayer p) {
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        return ow != null && CitySavedData.get(ow).isResident(p.getUUID());
    }

    public static void show(ServerPlayer p, int step) {
        OnboardingData o = p.getData(OnboardingAttachments.ONBOARDING);
        p.setData(OnboardingAttachments.ONBOARDING, o.withStep(step, p.level().getGameTime()));
        boolean has = false;
        int x = 0, z = 0;
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        if (ow != null) {
            CitySavedData d = CitySavedData.get(ow);
            has = d.isBuilt();
            x = d.center().getX();
            z = d.center().getZ();
        }
        PacketDistributor.sendToPlayer(p, new OnboardingHintPayload(step, has, x, z));
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (e.getEntity() instanceof HuMob && e.getSource().getEntity() instanceof ServerPlayer p) {
            OnboardingData o = p.getData(OnboardingAttachments.ONBOARDING);
            if (!o.killed()) p.setData(OnboardingAttachments.ONBOARDING, o.withKilled());
        }
    }

    /** /hukguide off | on | reset  (không cần quyền: người chơi tự tắt/bật). */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("hukguide")
                .then(Commands.literal("off").executes(c -> set(c.getSource(), true, false)))
                .then(Commands.literal("on").executes(c -> set(c.getSource(), false, false)))
                .then(Commands.literal("reset").executes(c -> set(c.getSource(), false, true))));
    }

    private static int set(CommandSourceStack s, boolean disabled, boolean reset) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        OnboardingData o = reset ? OnboardingData.DEFAULT : p.getData(OnboardingAttachments.ONBOARDING);
        p.setData(OnboardingAttachments.ONBOARDING, o.withDisabled(disabled));
        s.sendSuccess(() -> Component.translatable(disabled ? "hukhong.onboard.off" : "hukhong.onboard.on"), false);
        return 1;
    }
}
