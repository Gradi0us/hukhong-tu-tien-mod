package com.hukhong.onboarding;

import com.hukhong.core.Path;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Sách "Nhập Môn Tu Tiên": phần chung + một chương cho mỗi đạo người chơi đã chọn. Phát khi tạo nhân vật xong. */
public final class GuideBook {
    private GuideBook() {}

    private static final String[] COMMON = {
            "§l§5Nhập Môn Tu Tiên§r\n\nHư Không xâm thực trời đất. Ngươi là kẻ tu hành mới bước chân vào đạo.\n\nSách này ghi những điều cần biết đầu tiên.",
            "§lPhím cơ bản§r\n\nG: bật Thế Tu Luyện\nQ / E / R: kỹ năng\nT: Tuyệt Kỹ\nV: đổi trang (Đa Đạo)\nK: Bảng Tu Sĩ\nJ: Nhật ký truyện\nL: mục tiêu chính\nZ: bay\nB: Nhẫn Trữ Vật\nM / N / H: bản đồ",
            "§lTu luyện§r\n\nGiết quái Hư Hóa và yêu thú để nhận EXP và Hồn Tinh.\n\nĐứng trên Linh Khí Đài giữ Shift để Bế Quan.\n\nGặp Bình Cảnh thì dùng Phá Cảnh Đan để đột phá.",
            "§lLõi & Ấn Hồn Đài§r\n\nTới các mốc cấp, đến Ấn Hồn Đài ở Lưu Ly Cảng để chọn Lõi.\n\nNgồi + chuột phải Ấn Hồn Đài để mở đe cường hóa vũ khí.",
            "§lĐại Sâm Lâm§r\n\nRìa ngoài: yêu thú 10-100 năm.\nVùng giữa: 1.000-10.000 năm.\nHạch tâm: hồn thú cực mạnh.\n\nHồn thú §6Dị Chủng§r luôn rơi Hồn Cốt phẩm cao, nhưng thường có hộ vệ.",
            "§lHồn Cốt§r\n\nGiết yêu thú có thể rơi Hồn Cốt. Đeo được ở mọi cấp, tăng chỉ số và mở kỹ năng Bản Mệnh.\n\n§lNhẫn Trữ Vật§r\nĐồ cất trong nhẫn không rơi khi chết.",
    };

    private static String[] pages(Path p) {
        return switch (p) {
            case KIEM -> new String[]{
                    "§l§9Kiếm Tu§r\n\nTài nguyên: Linh Lực.\n\nQ: Thiên Kiếm Trấn\nE: Thủ Hộ Chi Kiếm\nT: Vạn Kiếm Quy Tông",
                    "Ngươi được ban §9Bản Mệnh Kiếm§r. Kiếm lớn lên cùng ngươi, đừng bỏ nó.\n\nTới cấp đủ có thể Ngự Kiếm phi hành (Z).\n\nĐánh nhanh, sát thương đơn mục tiêu cao."};
            case DAO -> new String[]{
                    "§l§6Đạo Tu§r\n\nTài nguyên: Thánh Lực.\n\nQ: Thánh Quang Tiễn\nE: Ban Phúc\nT: Thánh Giáng",
                    "Đạo Tu vừa đánh vừa hộ trợ: Ban Phúc giúp đồng đội, Thánh Giáng khắc chế ma vật.\n\nĐứng sau tuyến đầu, giữ khoảng cách."};
            case THE -> new String[]{
                    "§l§cThể Tu§r\n\nTài nguyên: Bá Khí.\n\nQ: Chấn Địa\nE: Thiết Bố Sam\nT: Kim Thân Bất Hoại",
                    "Thân thể là pháp bảo. Lao vào giữa bầy quái, dùng Thiết Bố Sam để chịu đòn.\n\nKim Thân Bất Hoại cứu mạng lúc nguy cấp."};
            case MA -> new String[]{
                    "§l§5Ma Tu§r\n\nTài nguyên: Ô Uế Lực.\n\nQ: Huyết Trảo\nE: Ma Khí Hộ Thể\nT: Ma Thần Giáng Thế",
                    "Hấp Thu cho EXP x1.45 nhưng tích Ác Tính. Tu luyện thường thì không tích và xóa Huyết Tế.\n\nSức mạnh lớn, cái giá cũng lớn."};
            case PHAP -> new String[]{
                    "§l§3Pháp Tu§r\n\nTài nguyên: Pháp Lực.\n\nKỹ năng Q / E / T đổi theo nguyên tố ngươi đã chọn.",
                    "Ngươi được ban §3Linh Trượng§r để thi pháp.\n\nĐánh từ xa, sát thương diện rộng. Giữ khoảng cách, đừng để quái áp sát."};
            case TRAN -> new String[]{
                    "§l§dTrận Tu§r\n\nTài nguyên: Trận Lực.\n\nQ: Khốn Trận\nE: Hộ Thân Trận",
                    "Bày trận để khống chế và bảo vệ. Dụ quái vào Khốn Trận rồi mới ra tay.\n\nĐộng Phủ Trận Bàn giúp ngươi dựng động phủ riêng."};
            case DAN -> new String[]{
                    "§l§6Đan Tu§r\n\nTài nguyên: Đan Hỏa.\n\nQ: Độc Đan Vũ\nE: Hồi Xuân Đan\nT: Cửu Chuyển Kim Đan",
                    "Chế tạo §6Đan Lô§r (sắt + lò nung + đá) để luyện đan.\n\nThu thập linh thảo trong rừng. Đan dược tốt giúp cả đội vượt Bình Cảnh."};
        };
    }

    public static ItemStack create(String author, Set<Path> paths) {
        List<Filterable<Component>> list = new ArrayList<>();
        for (String s : COMMON) list.add(Filterable.passThrough(Component.literal(s)));
        for (Path p : Path.values()) {
            if (!paths.contains(p)) continue;
            for (String s : pages(p)) list.add(Filterable.passThrough(Component.literal(s)));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough("Nhập Môn Tu Tiên"), "Hư Không", 0, list, true));
        return book;
    }

    public static void give(ServerPlayer p, Set<Path> paths) {
        ItemStack b = create(p.getGameProfile().getName(), paths);
        if (!p.getInventory().add(b)) p.drop(b, false);
    }
}
