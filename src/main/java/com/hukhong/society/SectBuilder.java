package com.hukhong.society;

import com.hukhong.city.CityBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * Sơn môn đơn giản dựng bằng code: sân vuông 29x29, tường thấp có cổng nam, chính điện phía bắc,
 * bệ trung tâm đặt Linh Khí Đài (R22) và bảng Tông Môn cạnh cổng. Vật liệu tường khác nhau theo tông.
 */
public final class SectBuilder {
    private SectBuilder() {}

    public static final int HALF = 14;
    private static final int FLAGS = 18;

    private static void set(ServerLevel l, int x, int y, int z, Block b) {
        l.setBlock(new BlockPos(x, y, z), b.defaultBlockState(), FLAGS);
    }

    private static Block wall(String id) {
        return switch (id) {
            case "ba_the_mon" -> Blocks.DEEPSLATE_BRICKS;
            case "dan_dinh_tong" -> Blocks.RED_NETHER_BRICKS;
            case "thien_cong_khi_cac" -> Blocks.POLISHED_ANDESITE;
            case "thien_kiem_tong" -> Blocks.QUARTZ_BRICKS;
            case "quang_minh_dao_cung" -> Blocks.SMOOTH_QUARTZ;
            case "van_tuong_thanh_tong" -> Blocks.PURPUR_BLOCK;
            case "huyet_anh_ma_mon" -> Blocks.CRIMSON_PLANKS;
            default -> Blocks.STONE_BRICKS;
        };
    }

    /** Dựng tại site (x,z); trả tâm (y = mặt nền). */
    public static BlockPos build(ServerLevel l, BlockPos site, SectDef def) {
        int cx = site.getX(), cz = site.getZ();
        int y0 = CityBuilder.prepare(l, cx, cz, HALF);
        Block w = wall(def.id());
        int ex = def.tier().ordinal();
        // sân lát + lối vào
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            boolean path = Math.abs(dx) <= 1;
            set(l, cx + dx, y0, cz + dz, path ? Blocks.COBBLESTONE : (((dx + dz) & 1) == 0 ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE));
        }
        // tường + cổng nam (dz = +HALF, |dx| <= 1)
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != HALF) continue;
            boolean corner = Math.abs(dx) == HALF && Math.abs(dz) == HALF;
            boolean gate = dz == HALF && Math.abs(dx) <= 1;
            int top = (corner ? 6 : 3) + ex;
            for (int h = 1; h <= top; h++) {
                if (gate && h <= 3) continue;
                set(l, cx + dx, y0 + h, cz + dz, w);
            }
            if (corner) set(l, cx + dx, y0 + top + 1, cz + dz, Blocks.LANTERN);
        }
        // chính điện (phía bắc)
        int hz0 = cz - HALF + 2;
        for (int dx = -5; dx <= 5; dx++) for (int dz = 0; dz <= 6; dz++) {
            boolean edge = Math.abs(dx) == 5 || dz == 0 || dz == 6;
            boolean door = dz == 6 && Math.abs(dx) <= 1;
            set(l, cx + dx, y0, hz0 + dz, Blocks.SPRUCE_PLANKS);
            for (int h = 1; h <= 4; h++) if (edge && !(door && h <= 2)) set(l, cx + dx, y0 + h, hz0 + dz, h == 4 ? w : (Math.abs(dx) == 5 && (dz == 0 || dz == 6) ? Blocks.OAK_LOG : Blocks.OAK_PLANKS));
            set(l, cx + dx, y0 + 5, hz0 + dz, Blocks.STONE_BRICK_SLAB);
        }
        set(l, cx, y0 + 4, hz0 + 3, Blocks.LANTERN);
        // bệ trung tâm 5x5 + Linh Khí Đài
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) set(l, cx + dx, y0, cz + dz, Blocks.CHISELED_STONE_BRICKS);
        set(l, cx, y0 + 1, cz, SocietyRegistry.LINH_KHI_DAI.get());
        for (int sx = -2; sx <= 2; sx += 4) for (int sz = -2; sz <= 2; sz += 4) {
            set(l, cx + sx, y0 + 1, cz + sz, Blocks.END_ROD);
            set(l, cx + sx, y0 + 2, cz + sz, Blocks.END_ROD);
        }
        // bảng Tông Môn + biển tên cạnh cổng
        set(l, cx + 3, y0 + 1, cz + HALF - 3, SocietyRegistry.TONG_MON_BANG.get());
        BlockPos sp = new BlockPos(cx, y0 + 1, cz + HALF + 2);
        l.setBlock(sp, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0), FLAGS);
        if (l.getBlockEntity(sp) instanceof SignBlockEntity s) {
            s.setText(s.getFrontText().setMessage(1, Component.translatable(def.nameKey())), true);
            s.setWaxed(true);
            s.setChanged();
        }
        decorate(l, def.id(), cx, y0, cz);
        return new BlockPos(cx, y0, cz);
    }

    /** Nét riêng của từng sơn môn (4 góc sân, ngoài bệ trung tâm và chính điện). */
    private static void decorate(ServerLevel l, String id, int cx, int y0, int cz) {
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int x = cx + sx * 9, z = cz + sz * 4 + 5;
            switch (id) {
                case "ba_the_mon" -> { set(l, x, y0 + 1, z, Blocks.HAY_BLOCK); set(l, x, y0 + 2, z, Blocks.CARVED_PUMPKIN); set(l, x, y0 + 1, z + 2, Blocks.ANVIL); }
                case "dan_dinh_tong" -> { set(l, x, y0 + 1, z, Blocks.CAULDRON); set(l, x, y0 + 1, z + 2, Blocks.BREWING_STAND); set(l, x + sx, y0 + 1, z, Blocks.SMOKER); }
                case "thien_cong_khi_cac" -> { set(l, x, y0 + 1, z, Blocks.SMITHING_TABLE); set(l, x, y0 + 1, z + 2, Blocks.GRINDSTONE); set(l, x + sx, y0 + 1, z, Blocks.IRON_BLOCK); }
                case "thien_kiem_tong" -> { for (int h = 1; h <= 4; h++) set(l, x, y0 + h, z, Blocks.QUARTZ_PILLAR); set(l, x, y0 + 5, z, Blocks.LIGHTNING_ROD); }
                case "quang_minh_dao_cung" -> { set(l, x, y0 + 1, z, Blocks.GOLD_BLOCK); set(l, x, y0 + 2, z, Blocks.GLOWSTONE); set(l, x, y0 + 3, z, Blocks.WHITE_STAINED_GLASS); }
                case "van_tuong_thanh_tong" -> { for (int h = 1; h <= 3; h++) set(l, x, y0 + h, z, Blocks.AMETHYST_BLOCK); set(l, x, y0 + 4, z, Blocks.AMETHYST_CLUSTER); set(l, x + sx, y0 + 1, z, Blocks.LECTERN); }
                case "huyet_anh_ma_mon" -> { set(l, x, y0, z, Blocks.CRIMSON_NYLIUM); set(l, x, y0 + 1, z, Blocks.RED_NETHER_BRICK_WALL); set(l, x, y0 + 2, z, Blocks.SOUL_LANTERN); set(l, x, y0 + 1, z + 2, Blocks.REDSTONE_BLOCK); }
                default -> { }
            }
        }
    }
}
