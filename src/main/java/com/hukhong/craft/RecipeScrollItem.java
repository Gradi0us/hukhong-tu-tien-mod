package com.hukhong.craft;

import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Quyen pho: 1 id cong thuc (data component hukhong:recipe_id). Chuot phai: hoc neu dung nghe, tieu hao.
 * Ten hien thi = "<loai>: <ten cong thuc>".
 */
public class RecipeScrollItem extends Item {
    public enum Kind {
        DAN("item.hukhong.dan_phuong", Profession.LUYEN_DAN, RecipeBook.DAN),
        PHU("item.hukhong.phu_pho", Profession.PHU_VAN, RecipeBook.PHU),
        KHI("item.hukhong.khi_pho", Profession.DOAN_TAO, RecipeBook.KHI);

        public final String key;
        public final Profession prof;
        public final String prefix;

        Kind(String key, Profession prof, String prefix) { this.key = key; this.prof = prof; this.prefix = prefix; }
    }

    public final Kind kind;

    public RecipeScrollItem(Kind kind, Properties props) {
        super(props.stacksTo(1));
        this.kind = kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = stack.get(CraftRegistry.RECIPE_ID.get());
        RecipeBook.Entry e = id == null ? null : RecipeBook.find(id);
        if (e == null) return super.getName(stack);
        return Component.translatable(kind.key).append(": ").append(e.name());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        String id = st.get(CraftRegistry.RECIPE_ID.get());
        if (id == null) return InteractionResultHolder.pass(st);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
        if (ProfessionService.profession(sp) != kind.prof) {
            sp.displayClientMessage(Component.translatable("hukhong.scroll.wrong_prof", Component.translatable(kind.prof.needKey())), true);
            return InteractionResultHolder.fail(st);
        }
        if (RecipeBook.knows(sp, id)) {
            sp.displayClientMessage(Component.translatable("hukhong.scroll.known"), true);
            return InteractionResultHolder.fail(st);
        }
        RecipeBook.learn(sp, id);
        if (!sp.isCreative()) st.shrink(1);
        sp.displayClientMessage(Component.translatable("hukhong.scroll.learned", getName(st)), true);
        return InteractionResultHolder.success(st);
    }
}
