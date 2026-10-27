package com.hukhong.command;

import com.hukhong.age.AgeData;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /hukhong dump | setlevel <n> | addexp <n> | age  (permission 2). */
public final class HuKhongCommand {
    private HuKhongCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hukhong")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("dump").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    CultivationData d = CultivationService.get(p);
                    msg(c.getSource(), d.toString() + " expToNext=" + CultivationService.expToNext(d.level()));
                    return 1;
                }))
                .then(Commands.literal("setlevel").then(Commands.argument("n", IntegerArgumentType.integer(1, 100))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CultivationService.setLevel(p, IntegerArgumentType.getInteger(c, "n"));
                            msg(c.getSource(), "level=" + CultivationService.get(p).level());
                            return 1;
                        })))
                .then(Commands.literal("addexp").then(Commands.argument("n", LongArgumentType.longArg(1))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CultivationService.addExp(p, LongArgumentType.getLong(c, "n"));
                            CultivationData d = CultivationService.get(p);
                            msg(c.getSource(), "level=" + d.level() + " exp=" + d.exp());
                            return 1;
                        })))
                .then(Commands.literal("trial")
                        .then(Commands.literal("tam_ma").executes(c -> {
                            com.hukhong.trial.TrialService.test(c.getSource().getPlayerOrException(), com.hukhong.trial.TrialService.Type.TAM_MA);
                            return 1;
                        }))
                        .then(Commands.literal("loi_kiep").executes(c -> {
                            com.hukhong.trial.TrialService.test(c.getSource().getPlayerOrException(), com.hukhong.trial.TrialService.Type.LOI_KIEP);
                            return 1;
                        })))
                .then(Commands.literal("unlock").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean ok = CultivationService.unlockMilestone(p);
                    msg(c.getSource(), ok ? "milestone unlocked" : "not locked");
                    return 1;
                }))
                .then(Commands.literal("give_altar").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var st = new net.minecraft.world.item.ItemStack(com.hukhong.item.ModItems.AN_HON_DAI.get());
                    if (!p.getInventory().add(st)) p.drop(st, false);
                    var hs = new net.minecraft.world.item.ItemStack(com.hukhong.item.ModItems.HON_TINH.get(), 5);
                    if (!p.getInventory().add(hs)) p.drop(hs, false);
                    msg(c.getSource(), "give an_hon_dai + 5 hon_tinh");
                    return 1;
                }))
                .then(Commands.literal("give_bone")
                        .executes(c -> giveBone(c.getSource(), null, 1))
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((cx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(com.hukhong.honcot.Bone.values()).map(com.hukhong.honcot.Bone::id), b))
                                .executes(c -> giveBone(c.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"), 1))
                                .then(Commands.argument("tier", IntegerArgumentType.integer(0, 2))
                                        .executes(c -> giveBone(c.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"),
                                                IntegerArgumentType.getInteger(c, "tier"))))))
                .then(Commands.literal("core_unstuck").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.setData(com.hukhong.cores.CoreAttachments.CORE_SLOTS, com.hukhong.cores.CoreService.get(p).withStuckCleared());
                    msg(c.getSource(), "stuck cleared");
                    return 1;
                }))
                .then(Commands.literal("reroll").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    com.hukhong.charcreate.CharCreateService.reroll(p);
                    msg(c.getSource(), "reroll: " + CultivationService.get(p).roll());
                    return 1;
                }))
                .then(Commands.literal("invade")
                        .then(Commands.literal("warn").executes(c -> {
                            var ow = c.getSource().getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
                            String err = com.hukhong.events.InvasionManager.warn(ow, 400);
                            if (err != null) { c.getSource().sendFailure(Component.translatable(err)); return 0; }
                            return 1;
                        }))
                        .executes(c -> {
                            var ow = c.getSource().getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
                            String err = com.hukhong.events.InvasionManager.start(ow);
                            if (err != null) { c.getSource().sendFailure(Component.translatable(err)); return 0; }
                            return 1;
                        }))
                .then(Commands.literal("boss").executes(c -> {
                    var pl = c.getSource().getPlayerOrException();
                    com.hukhong.events.CorruptionManager.spawnBoss(pl.serverLevel(), pl.blockPosition().offset(6, 0, 0));
                    return 1;
                }))
                .then(Commands.literal("corrupt").then(Commands.argument("chunks", IntegerArgumentType.integer(1, 200)).executes(c -> {
                    var pl = c.getSource().getPlayerOrException();
                    int n = com.hukhong.events.CorruptionManager.seedAround(pl.serverLevel(), pl.blockPosition(), IntegerArgumentType.getInteger(c, "chunks"));
                    msg(c.getSource(), "corrupted +" + n + " chunks");
                    return 1;
                })))
                .then(Commands.literal("structure")
                        .then(Commands.literal("place")
                                .then(Commands.argument("category", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(c -> placeStructure(c, false))
                                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                .executes(c -> placeStructure(c, true)))))))
                        .then(Commands.literal("city")
                                .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(c -> buildCityAt(c)))))))
                .then(Commands.literal("city_restore").executes(c -> {
                    com.hukhong.events.InvasionManager.cancel();
                    var pos = com.hukhong.city.CityEvents.ensureBuilt(c.getSource().getServer(), true);
                    msg(c.getSource(), "city rebuilt at " + pos);
                    return 1;
                }))
                .then(Commands.literal("ma")
                        .then(Commands.literal("ac").then(Commands.argument("n", IntegerArgumentType.integer(0, 100)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            com.hukhong.pathrules.MaRules.setAcTinh(p, IntegerArgumentType.getInteger(c, "n"));
                            msg(c.getSource(), "ac_tinh=" + com.hukhong.pathrules.MaRules.data(p).acTinh);
                            return 1;
                        })))
                        .then(Commands.literal("lo").then(Commands.literal("hapthu").executes(c -> setLo(c.getSource(), true)))
                                .then(Commands.literal("thuong").executes(c -> setLo(c.getSource(), false)))))
                .then(Commands.literal("apostle")
                        .executes(c -> apostle(c.getSource(), -1))
                        .then(Commands.argument("variant", IntegerArgumentType.integer(0, 2))
                                .executes(c -> apostle(c.getSource(), IntegerArgumentType.getInteger(c, "variant")))))
                .then(Commands.literal("chg")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 4)).executes(c -> {
                            var b = com.hukhong.chg.ChgService.test(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
                            msg(c.getSource(), b == null ? "spawn failed" : "chg " + b.idx() + " descending");
                            return b == null ? 0 : 1;
                        })))
                .then(com.hukhong.bicanh.BiCanhCommand.node())
                .then(com.hukhong.endgame.EndgameCommand.node())
                .then(Commands.literal("age").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    AgeData a = p.getData(ModAttachments.AGE);
                    msg(c.getSource(), "age=" + a.ageYears() + " onlineTicks=" + a.onlineTicks()
                            + " months=" + a.onlineTicks() / AgeData.TICKS_PER_MONTH);
                    return 1;
                })));
    }

    /** Đổi lộ Ma (tạm: lệnh; Ma Môn sẽ gọi cùng hàm). Sang Tu Luyện thường xóa Huyết Tế. */
    /** /hukhong structure place <category> <name> [x z]: dat file cau truc (config/hukhong/structures) de thu; category trade thi them thuong nhan. */
    private static int placeStructure(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, boolean xz)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        String cat = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "category");
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "name");
        var st = com.hukhong.structures.StructureLibrary.pickNamed(cat, name);
        if (st == null) { src.sendFailure(Component.literal("structure not found/invalid: " + cat + "/" + name)); return 0; }
        var level = src.getLevel();
        int x = xz ? IntegerArgumentType.getInteger(c, "x") : src.getPlayerOrException().getBlockX();
        int z = xz ? IntegerArgumentType.getInteger(c, "z") : src.getPlayerOrException().getBlockZ();
        int y0 = com.hukhong.structures.StructurePlacer.surfaceY(level, x, z);
        var origin = new net.minecraft.core.BlockPos(x - st.sx / 2, y0, z - st.sz / 2);
        boolean trade = cat.equals("trade");
        com.hukhong.structures.StructurePlacer.enqueue(level, st, origin, trade ? com.hukhong.city.TradeDistrict::populate : null);
        msg(src, "queued " + cat + "/" + name + " " + st.sx + "x" + st.sy + "x" + st.sz + " at " + origin);
        return 1;
    }

    /** /hukhong structure city <id> <x> <z>: dung thanh <id> (CityBuilder.build, gom ca khu cho neu co) tai (x,z) de thu. */
    private static int buildCityAt(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        var d = com.hukhong.city.CityDef.byId(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"));
        if (d == null) { src.sendFailure(Component.literal("unknown city id")); return 0; }
        var pos = new net.minecraft.core.BlockPos(IntegerArgumentType.getInteger(c, "x"), 64, IntegerArgumentType.getInteger(c, "z"));
        var center = com.hukhong.city.CityBuilder.build(src.getLevel(), pos, d);
        msg(src, "city " + d.id() + " (" + d.category() + ") queued, center " + center);
        return 1;
    }

    private static int setLo(CommandSourceStack src, boolean absorb) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        var m = com.hukhong.pathrules.MaRules.data(p); // lệnh giữ nguyên hành vi cũ (không đòi đạo Ma, để test)
        m.absorb = absorb;
        if (!absorb) m.huyetTe = 0;
        msg(src, "ma lo=" + (absorb ? "hapthu" : "thuong"));
        return 1;
    }

    private static int apostle(CommandSourceStack src, int v) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var s = com.hukhong.story.StoryService.spawnNear(src.getPlayerOrException(), v);
        msg(src, s == null ? "spawn failed" : "apostle variant=" + s.variant() + " level=" + s.suLevel());
        return 1;
    }

    /** /hukhong give_bone [id] [tier 0..2]: Hồn Cốt thử (không id = ngẫu nhiên). */
    private static int giveBone(CommandSourceStack src, String id, int tier) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        com.hukhong.honcot.Bone b = id == null ? null : com.hukhong.honcot.Bone.byId(id);
        if (id != null && b == null) { src.sendFailure(Component.literal("unknown bone: " + id)); return 0; }
        var st = com.hukhong.honcot.HonCotEvents.testStack(p.getRandom(), b, tier);
        if (!p.getInventory().add(st)) p.drop(st, false);
        msg(src, "give " + st.getHoverName().getString());
        return 1;
    }

    private static void msg(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
    }
}
