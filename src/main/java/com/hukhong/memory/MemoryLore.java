package com.hukhong.memory;

import java.util.List;

/**
 * Tám Ký Ức Hư Không về Phản Thần Dạ Khuyết (xem docs/PLAN-v2-tu-tien-mod-canon.md §9).
 * Mỗi chuỗi là một khoá lang + bản tiếng Việt (dự phòng) + bản tiếng Anh; lang/en_us + vi_vn được ghép từ đây.
 */
public final class MemoryLore {
    private MemoryLore() {}

    /** Một dòng văn: khoá lang, bản Việt, bản Anh. */
    public record L(String key, String vi, String en) {
        public net.minecraft.network.chat.MutableComponent tr() { return com.hukhong.auction.Tx.t(key, vi, en); }
    }

    /** id 1..8; poiKind = loại địa điểm được ưu tiên tặng ký ức này (xem MemoryService.grantUnseen). */
    public record Memory(int id, L name, List<L> lines, int rgb, String poiKind) {}

    public static final int COUNT = 8;
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};

    private static L l(String key, String vi, String en) { return new L(key, vi, en); }

    public static final List<Memory> ALL = List.of(
            new Memory(1, l("hukhong.memory.1.name", "Thiên Môn còn nguyên", "The Heavenly Gate, Still Whole"), List.of(
                    l("hukhong.memory.1.line.1", "Thuở trời đất còn non, Thiên Môn sừng sững giữa các tầng mây.", "When heaven and earth were young, the Heavenly Gate stood tall above the clouds."),
                    l("hukhong.memory.1.line.2", "Chư thần canh giữ từng bậc thang, ánh kim chưa một vết nứt.", "The gods kept watch on every step, its golden light unbroken."),
                    l("hukhong.memory.1.line.3", "Bên kia cánh cổng là Hư Không, vực thẳm không tên, không ánh sáng.", "Beyond the gate lay the Void, a nameless abyss without light."),
                    l("hukhong.memory.1.line.4", "Thần linh hứa rằng cánh cửa ấy sẽ không bao giờ mở.", "The gods swore that the gate would never open."),
                    l("hukhong.memory.1.line.5", "Và thế gian tin lời hứa ấy, như tin vào ngày mai.", "And the world believed that oath as surely as it believed in tomorrow.")),
                    0xFFD866, "stele"),
            new Memory(2, l("hukhong.memory.2.name", "Dạ Khuyết, Thần Trăng Khuyết", "Dạ Khuyết, the Crescent Moon God"), List.of(
                    l("hukhong.memory.2.line.1", "Dạ Khuyết là thần của trăng khuyết, kẻ canh cổng mạnh nhất Thiên Môn.", "Dạ Khuyết, god of the waning moon, was the strongest gatekeeper of the Heavenly Gate."),
                    l("hukhong.memory.2.line.2", "Ánh trăng trên vai hắn lạnh hơn mọi vị thần khác.", "The moonlight on his shoulders was colder than any other god's."),
                    l("hukhong.memory.2.line.3", "Nhưng trong tim hắn cháy một khát vọng: vượt qua giới hạn của chính mình.", "Yet in his heart burned one desire: to surpass the limits of himself."),
                    l("hukhong.memory.2.line.4", "Hắn nhìn xuống Hư Không, và lần đầu tiên, hắn không còn sợ.", "He gazed down into the Void, and for the first time, he was not afraid."),
                    l("hukhong.memory.2.line.5", "Khát vọng ấy là vết nứt đầu tiên trên lớp vỏ thần thánh.", "That longing was the first crack in his divine shell.")),
                    0xC9D6FF, "grave"),
            new Memory(3, l("hukhong.memory.3.name", "Lời thì thầm bên kia cổng", "A Whisper Beyond the Gate"), List.of(
                    l("hukhong.memory.3.line.1", "Một đêm nọ, có tiếng thì thầm lọt qua khe cửa.", "One night, a whisper slipped through the seam of the gate."),
                    l("hukhong.memory.3.line.2", "Không lời, không tiếng, chỉ là một ý nghĩ lạ gõ vào tâm trí.", "No words, no sound, only a strange thought knocking at the mind."),
                    l("hukhong.memory.3.line.3", "“Vượt ra ngoài giới hạn... ngươi có thể trở thành hơn cả thần.”", "“Step beyond your limits... you could become more than a god.”"),
                    l("hukhong.memory.3.line.4", "Dạ Khuyết lắng nghe, và từ đêm ấy không bao giờ ngủ yên nữa.", "Dạ Khuyết listened, and from that night on he never slept in peace again."),
                    l("hukhong.memory.3.line.5", "Lời thì thầm ấy sẽ theo hắn đến tận ngày phản bội.", "That whisper would follow him all the way to the day of betrayal.")),
                    0xA050FF, "shrine"),
            new Memory(4, l("hukhong.memory.4.name", "Đêm trăng khuyết", "The Night of the Crescent Moon"), List.of(
                    l("hukhong.memory.4.line.1", "Đêm trăng khuyết, khi trời không một vì sao.", "On the night of the crescent moon, when not a single star shone."),
                    l("hukhong.memory.4.line.2", "Dạ Khuyết đặt tay lên cánh cổng Thiên Môn, và khẽ đẩy.", "Dạ Khuyết laid his hand on the Heavenly Gate and gently pushed."),
                    l("hukhong.memory.4.line.3", "Một khe nứt mảnh như sợi tóc xé toạc ánh sáng.", "A crack as thin as a hair tore open the light."),
                    l("hukhong.memory.4.line.4", "Hư Không tràn qua, lạnh buốt, câm lặng, và đói khát.", "The Void poured through, cold, silent, and hungry."),
                    l("hukhong.memory.4.line.5", "Đó là đêm đầu tiên thiên giới biết đến mùi phản bội.", "It was the first night heaven knew the taste of betrayal.")),
                    0x8A1FD0, "rift"),
            new Memory(5, l("hukhong.memory.5.name", "Cổ chiến trường", "The Ancient Battlefield"), List.of(
                    l("hukhong.memory.5.line.1", "Trên cổ chiến trường, tu sĩ nhân gian đứng thành hàng chặn Hư Không.", "On the ancient battlefield, mortal cultivators stood in a line against the Void."),
                    l("hukhong.memory.5.line.2", "Không thần, không tiên, chỉ có người phàm cầm kiếm.", "No gods, no immortals; only mortals holding swords."),
                    l("hukhong.memory.5.line.3", "Máu nhuộm đất đến nỗi cỏ cũng không mọc nổi.", "Blood soaked the earth until not even grass would grow."),
                    l("hukhong.memory.5.line.4", "Họ ngã xuống, nhưng khe nứt không khép thêm được một tấc nào.", "They fell, yet the crack closed not a single inch more."),
                    l("hukhong.memory.5.line.5", "Gió qua chiến trường vẫn vang tiếng gọi của những người chưa kịp lui.", "The wind across the field still carries the calls of those who never retreated.")),
                    0xC01028, "battlefield"),
            new Memory(6, l("hukhong.memory.6.name", "Kiếm Trủng", "The Sword Mound"), List.of(
                    l("hukhong.memory.6.line.1", "Nơi đất trũng kiếm cắm dày như rừng, kiếm tiên cuối cùng đứng lên.", "In the hollow where swords stood thick as a forest, the last sword immortal rose."),
                    l("hukhong.memory.6.line.2", "Kiếm không rơi, không gãy: hắn cắm xuống bằng cả một đời tu luyện.", "His blade did not fall or break; he drove it down with the labor of a whole life."),
                    l("hukhong.memory.6.line.3", "Ánh kiếm chạm đất, phong ấn lan ra như một vòng sóng.", "Where the light touched the ground, the seal spread like a ring of waves."),
                    l("hukhong.memory.6.line.4", "Khe nứt khép được một nửa, và kiếm tiên không bao giờ đứng dậy nữa.", "Half the crack closed, and the sword immortal never rose again."),
                    l("hukhong.memory.6.line.5", "Thanh kiếm ấy vẫn còn đó, chờ người đủ tư cách rút ra.", "That blade still waits, for one worthy enough to draw it.")),
                    0x9FE8FF, "sword_mound"),
            new Memory(7, l("hukhong.memory.7.name", "Bốn Chấp Hành Giả", "The Four Executors"), List.of(
                    l("hukhong.memory.7.line.1", "Sau trận đại chiến, bốn Chấp Hành Giả được chọn để gìn giữ Thiên Môn.", "After the great war, four Executors were chosen to guard the Heavenly Gate."),
                    l("hukhong.memory.7.line.2", "Mỗi người nhận một mảnh Thiên Môn: một mảnh cửa, một mảnh lời hứa.", "Each received one fragment of the Gate: one piece of the door, one piece of the vow."),
                    l("hukhong.memory.7.line.3", "Họ thề giữ bốn mảnh ấy cho đến khi thiên địa cạn tàn.", "They swore to keep those fragments until heaven and earth ran dry."),
                    l("hukhong.memory.7.line.4", "Bốn ánh sáng hội tụ về một điểm, rồi tắt dần trong sương mù.", "Four lights converged on a single point, then faded into the mist."),
                    l("hukhong.memory.7.line.5", "Không ai biết mảnh cuối cùng đang nằm ở đâu.", "No one knows where the final fragment lies now.")),
                    0xFFB060, "ruin"),
            new Memory(8, l("hukhong.memory.8.name", "Nửa thân phong ấn", "Half a Body, Sealed"), List.of(
                    l("hukhong.memory.8.line.1", "Dạ Khuyết bị phong ấn đến nửa thân, xiềng xích bằng chính ánh trăng của mình.", "Dạ Khuyết was sealed to the waist, bound in chains forged from his own moonlight."),
                    l("hukhong.memory.8.line.2", "Hư Không vẫn gọi hắn mỗi đêm, nhưng hắn chỉ mỉm cười.", "The Void still calls him every night, but he only smiles."),
                    l("hukhong.memory.8.line.3", "Không phải vì hối hận, mà vì hắn biết cánh cửa sẽ mở lại.", "Not from regret, but because he knows the gate will open again."),
                    l("hukhong.memory.8.line.4", "Khi ngày ấy đến, hắn sẽ trở về, và thiên giới sẽ biết thế nào là sợ hãi.", "When that day comes he will return, and heaven will learn what true fear is."),
                    l("hukhong.memory.8.line.5", "Bây giờ, hắn chỉ còn việc chờ đợi.", "For now, waiting is all he has left to do.")),
                    0x6A1A9A, "abyss"));

    /** Lời kết khi gom đủ tám ký ức. */
    public static final List<L> FINALE = List.of(
            l("hukhong.memory.final.sub.1", "Tám mảnh ký ức đã hợp lại thành một.", "Eight fragments of memory have become one."),
            l("hukhong.memory.final.sub.2", "Dạ Khuyết vẫn đợi, bên kia cánh cổng khép hờ.", "Dạ Khuyết still waits, beyond a gate left ajar."),
            l("hukhong.memory.final.sub.3", "Ngươi là người giữ ký ức, cũng là người giữ cánh cửa.", "You are now a keeper of memories, and of the door itself."));

    public static Memory byId(int id) { return id >= 1 && id <= COUNT ? ALL.get(id - 1) : null; }

    public static String roman(int id) { return id >= 1 && id <= COUNT ? ROMAN[id - 1] : "?"; }

    /** Ký ức ưu tiên cho loại địa điểm; 0 = không có (tặng ngẫu nhiên trong các ký ức chưa thấy). */
    public static int preferredFor(String poiKind) {
        for (Memory m : ALL) if (m.poiKind().equals(poiKind)) return m.id();
        return 0;
    }
}
