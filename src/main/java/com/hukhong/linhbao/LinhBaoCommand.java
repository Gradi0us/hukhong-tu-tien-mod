package com.hukhong.linhbao;

import com.hukhong.stats.StatService;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /hukhong linhbao (và /linhbao): xem tên, phẩm, bonus thật của Linh Bảo. Tiến hóa: nút ở màn Hồ Sơ hoặc Shift + chuột phải bằng Hồn Tinh. */
@EventBusSubscriber(modid = com.hukhong.HuKhong.MODID)
public final class LinhBaoCommand {
    private LinhBaoCommand() {}

    private static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("linhbao").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean any = false;
            for (LinhBaoType t : LinhBaoType.values()) {
                if (!LinhBao.has(p, t)) continue;
                any = true;
                p.sendSystemMessage(LinhBaoText.name(t).copy().withStyle(net.minecraft.ChatFormatting.AQUA));
                for (Component line : LinhBaoText.lines(t, LinhBao.stage(p, t), StatService.attackPower(p), StatService.spellPower(p)))
                    p.sendSystemMessage(line);
            }
            if (!any) p.sendSystemMessage(Component.translatableWithFallback("hukhong.linhbao.none",
                    "Đạo của bạn dùng Bản Mệnh riêng (Kiếm: Bản Mệnh Kiếm, Pháp: Linh Trượng)."));
            return 1;
        });
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hukhong").then(node()));
        e.getDispatcher().register(node());
    }
}
