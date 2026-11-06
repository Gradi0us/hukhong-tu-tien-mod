package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.hukhong.core.CultivationService;
import com.hukhong.creature.LinhThu;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /hkpet info | evolve <nhanh> (ai cung dung duoc, tieu xuc tac trong tui; dung cho bam trong chat)
 *          | setlevel <n> | bond <n> (op, test).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PetCommands {
    private PetCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hkpet")
                .then(Commands.literal("info").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (String s : new String[]{"battle", "spirit"}) {
                        LinhThu t = PetService.loaded(p, s);
                        if (t == null) { c.getSource().sendSuccess(() -> Tx.t("hukhong.pet.cmd.none", "Ô %s: trống hoặc chưa tải.", "Slot %s: empty or unloaded.", s), false); continue; }
                        var b = t.petBranch();
                        c.getSource().sendSuccess(() -> Tx.t("hukhong.pet.cmd.info", "%s · Lv.%s · tầng %s · %s · thân mật %s/100 · HP %s/%s",
                                "%s · Lv.%s · stage %s · %s · bond %s/100 · HP %s/%s",
                                t.getDisplayName(), PetService.ownerLevel(p), t.petStage(), b == null ? "-" : b.displayName(), t.bond(),
                                Math.round(t.getHealth()), Math.round(t.getMaxHealth())), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("evolve").then(Commands.argument("branch", StringArgumentType.word())
                        .suggests((cx, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(PetBranch.values()).map(x -> x.id), b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            PetBranch br = PetBranch.byId(StringArgumentType.getString(c, "branch"));
                            if (br == null) { c.getSource().sendFailure(Tx.t("hukhong.pet.cmd.bad_branch", "Nhánh không hợp lệ.", "Unknown branch.")); return 0; }
                            PetService.Evo last = PetService.Evo.NO_CATALYST;
                            LinhThu lastPet = null;
                            for (String s : new String[]{"battle", "spirit"}) {
                                LinhThu t = PetService.loaded(p, s);
                                if (t == null) continue;
                                PetService.Evo r = PetService.evolve(p, t, br, null);
                                if (r == PetService.Evo.OK) { c.getSource().sendSuccess(() -> PetService.evoText(r, t, p), false); return 1; }
                                last = r; lastPet = t;
                            }
                            if (lastPet == null) { c.getSource().sendFailure(Tx.t("hukhong.pet.cmd.no_pet", "Chưa có linh thú được tải.", "No loaded pet.")); return 0; }
                            LinhThu fp = lastPet;
                            PetService.Evo fr = last;
                            c.getSource().sendFailure(PetService.evoText(fr, fp, p));
                            return 0;
                        })))
                .then(Commands.literal("setlevel").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 100)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CultivationService.setLevel(p, IntegerArgumentType.getInteger(c, "n"));
                            for (String s : new String[]{"battle", "spirit"}) {
                                LinhThu t = PetService.loaded(p, s);
                                if (t != null) PetService.syncLevel(t);
                            }
                            c.getSource().sendSuccess(() -> Tx.t("hukhong.pet.cmd.level", "Cấp linh thú = cấp tu luyện = %s.", "Pet level = cultivation level = %s.",
                                    PetService.ownerLevel(p)), false);
                            return 1;
                        })))
                .then(Commands.literal("bond").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 100)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            int n = IntegerArgumentType.getInteger(c, "n");
                            for (String s : new String[]{"battle", "spirit"}) {
                                LinhThu t = PetService.loaded(p, s);
                                if (t != null) { t.setBond(n); PetForm.apply(t); }
                            }
                            c.getSource().sendSuccess(() -> Tx.t("hukhong.pet.cmd.bond", "Thân mật linh thú = %s.", "Pet bond = %s.", n), false);
                            return 1;
                        }))));
    }
}
