package com.hukhong.resource;

import com.hukhong.world.calendar.CalendarData;
import com.hukhong.world.calendar.LinhKhiTrieu;
import com.hukhong.world.season.Season;
import com.hukhong.zone.ZoneData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Linh thao: tuoi 0 (mam) -> 1 (non) -> 2 (chin, thu hoach). Chi lon vao mua nua (hoac trong Linh Khi Trieu); ngoai mua heo dan.
 * Nhan chuot phai khi chin = thu hoach + moc lai (tai sinh). {@code cultivated}: do nguoi choi trong/cham -> khong tan ngoai mua.
 */
public class HerbBlock extends BushBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, HerbMath.MAX_AGE);
    public static final BooleanProperty CULTIVATED = BooleanProperty.create("cultivated");
    public static final MapCodec<HerbBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("herb").forGetter(b -> b.herb), propertiesCodec()).apply(i, HerbBlock::new));
    private static final VoxelShape[] SHAPES = {
            Block.box(4, 0, 4, 12, 5, 12), Block.box(3, 0, 3, 13, 9, 13), Block.box(2, 0, 2, 14, 13, 14)};

    private final String herb;

    public HerbBlock(String herb, Properties p) {
        super(p);
        this.herb = herb;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(CULTIVATED, false));
    }

    public String herb() { return herb; }

    @Override protected MapCodec<? extends BushBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE, CULTIVATED); }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPES[s.getValue(AGE)]; }

    @Override protected boolean mayPlaceOn(BlockState s, BlockGetter l, BlockPos p) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.TERRACOTTA) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.FARMLAND);
    }

    @Override public ItemStack getCloneItemStack(LevelReader l, BlockPos p, BlockState s) { return new ItemStack(item()); }

    public Item item() {
        HerbDef d = HerbDefs.get(herb);
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(d != null ? d.item() : "hukhong:" + herb));
    }

    @Override protected boolean isRandomlyTicking(BlockState s) { return true; }

    @Override protected void randomTick(BlockState s, ServerLevel lv, BlockPos pos, RandomSource r) {
        boolean trieu = trieuAt(lv, pos);
        if (lv.getRawBrightness(pos, 0) < 8 && !trieu) return; // can anh sang de lon / heo
        tickWith(s, lv, pos, r, Season.current(lv), trieu);
    }

    /** Mot buoc tang truong/heo voi mua cho truoc (GameTest dung truc tiep). */
    public void tickWith(BlockState s, ServerLevel lv, BlockPos pos, RandomSource r, Season season, boolean trieu) {
        HerbDef d = HerbDefs.get(herb);
        if (d == null) return;
        boolean bloom = HerbMath.blooms(d.seasonMask(), season.ordinal(), trieu);
        int age = s.getValue(AGE);
        boolean grow = r.nextInt(HerbMath.growDenom(trieu)) == 0, wilt = r.nextInt(4) == 0;
        if (HerbMath.witherAway(age, bloom, s.getValue(CULTIVATED), wilt)) { lv.removeBlock(pos, false); return; }
        int n = HerbMath.nextAge(age, bloom, grow, wilt);
        if (n != age) lv.setBlock(pos, s.setValue(AGE, n), 2);
    }

    /** Dang trong vung Linh Khi Trieu dang dien ra? (chi Overworld) */
    public static boolean trieuAt(ServerLevel lv, BlockPos pos) {
        if (lv.dimension() != Level.OVERWORLD) return false;
        for (CalendarData.Act a : CalendarData.get(lv.getServer().overworld()).active().values())
            if (LinhKhiTrieu.TYPE.equals(a.defId()) && LinhKhiTrieu.inZone(pos.getX(), pos.getZ(), a.x(), a.z())) return true;
        return false;
    }

    /** Pham chat thu hoach tai pos: cap vung xam thuc neu co, neu khong cap theo khoang cach toi spawn; Trieu +1. */
    public static int qualityAt(ServerLevel lv, BlockPos pos) {
        int zl = ZoneData.levelAt(pos.getX(), pos.getZ());
        if (zl <= 0) {
            BlockPos sp = lv.getSharedSpawnPos();
            double dx = pos.getX() - sp.getX(), dz = pos.getZ() - sp.getZ();
            zl = HerbMath.zoneLevelAt(Math.sqrt(dx * dx + dz * dz));
        }
        return HerbMath.quality(zl, trieuAt(lv, pos));
    }

    public List<ItemStack> drops(ServerLevel lv, BlockPos pos, RandomSource r) {
        int q = qualityAt(lv, pos);
        ItemStack st = new ItemStack(item(), HerbMath.dropCount(q, r.nextBoolean()));
        if (q > 0) st.set(ResourceRegistry.HERB_QUALITY.get(), q);
        List<ItemStack> out = new ArrayList<>();
        out.add(st);
        return out;
    }

    /** Thu hoach cay chin: roi do, cay ve tuoi 0 (tai sinh) va thanh cay da cham soc. false neu chua chin. */
    public boolean harvest(ServerLevel lv, BlockPos pos, BlockState s) {
        if (s.getValue(AGE) < HerbMath.MAX_AGE) return false;
        for (ItemStack st : drops(lv, pos, lv.getRandom())) Block.popResource(lv, pos, st);
        lv.setBlock(pos, s.setValue(AGE, 0).setValue(CULTIVATED, true), 3);
        return true;
    }

    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos pos, Player p, BlockHitResult h) {
        if (s.getValue(AGE) < HerbMath.MAX_AGE) return InteractionResult.PASS;
        if (l instanceof ServerLevel lv) harvest(lv, pos, s);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override public BlockState playerWillDestroy(Level l, BlockPos pos, BlockState s, Player p) {
        if (l instanceof ServerLevel lv && !p.isCreative() && s.getValue(AGE) >= HerbMath.MAX_AGE)
            for (ItemStack st : drops(lv, pos, lv.getRandom())) Block.popResource(lv, pos, st);
        return super.playerWillDestroy(l, pos, s, p);
    }
}
