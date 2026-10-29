# DESIGN-v2: Chuỗi nhiệm vụ phụ (Quest Chains)

**Last Updated**: 2026-10-29 — Bản đầu: 4 chuỗi phụ nối vào tuyến chính, engine `story/chain/QuestChains`
**Status**: Đã triển khai (code + thoại JSON), chưa merge lang (xem `docs/research/lang-q3.txt`)
**Liên quan**: `DESIGN-v1-story-flow.md` (tuyến chính), `story/npc/Dialogue.java` (action `qc`, `doc`)

## 1. Tóm tắt (Pyramid)

Tuyến chính quá tuyến tính nên thêm 4 chuỗi phụ 3-6 bước. Mỗi chuỗi: **giao bằng NPC xuất hiện ngẫu nhiên cạnh người chơi** (gợi cảm giác sự kiện thế giới), có **bước thao tác thật** (hộ tống, phục kích, dọn Hư Thi, mở Bí Cảnh ẩn), có **lựa chọn rẽ nhánh** ghi thành cờ, và **thoại về sau tham chiếu cờ** (Mộ Dao bình luận, trưởng lão nội gián mở thêm lựa chọn nếu đã tra hỏi hộ vệ Sứ Đồ).

## 2. Cơ chế dữ liệu

| Cờ trong `QuestData.flags` | Ý nghĩa |
|---|---|
| `qc.<chuỗi>.<n>` | đang ở bước n (chỉ một cờ bước mỗi chuỗi) |
| `qc.<chuỗi>.done` | đã xong, không giao lại |
| `qc.<chuỗi>.c.<tên>` | lựa chọn rẽ nhánh (vĩnh viễn) — thoại sau đọc bằng `flags` / `not_flags` |
| `qc.<chuỗi>.at:x:z` | đích đến của bước hiện tại (hang ẩn, làng Hư Hóa, điểm đến hộ tống) |
| `qc.told.*` | Mộ Dao đã nhắc lựa chọn này (chỉ nói 1 lần) |

Action thoại mới: `{"type":"qc","chain":"merchant","stage":2}` (chuyển bước, stage 9 = hoàn thành) và `{"type":"doc","id":"a2"}` (nhận Văn Kiện). Bước theo sự kiện (kill, tới nơi, vào Bí Cảnh) do `QuestChains.onPlayerTick` 1s/lần xác định. Gợi ý (actionbar, 5s) cho biết khoảng cách + hướng la bàn tới đích và việc cần làm.

**Giao việc**: `QuestChains.OFFERS` — mỗi ~20s, nếu ngoài vùng an toàn, cách lần giao trước ≥5 phút, 50% xác suất, sinh NPC (tag `hk_qc_offer`) cách người chơi 9-14 block trên mặt đất an toàn (`NpcPlacement`), nhắn chat. NPC tự biến mất sau 5 phút. Điều kiện (chương/cấp) lấy từ `QuestData.current()` và cảnh giới.

## 3. Bốn chuỗi

### A. Hàng Hóa Bị Phục Kích (`merchant`, chương ≥2, cấp ≥10) — 6 bước
1. Thương nhân Tôn Lập nhờ hộ tống ~110 block. **Chọn**: lấy phí (`c.fee`) / giúp không công (`c.free`).
2. Hộ tống: NPC đi theo tuyến tính sẵn (`EscortMover`, ~4,3 block/s, chờ nếu tụt >12 block).
3. Giữa đường 3 "Hộ Vệ Thương Hội" rút kiếm — thực chất Sứ Đồ cải trang. Hạ chúng.
4. Hỏi thương nhân. **Chọn**: tra hỏi tên còn sống (`c.truth`, nhận Văn Kiện a2) / mặc kệ (`c.ignore`).
5. Tiếp tục hộ tống.
6. Tới nơi, nhận thưởng: phí (700 exp + Khai Mạch Đan) hoặc quà (500 exp + Khai Mạch Đan + cờ `c.favor`).
**Về sau**: Mộ Dao nhắc Sứ Đồ cải trang nếu `c.truth`; chuỗi C mở thêm lựa chọn "vạch trần có bằng chứng".

### B. Đệ Tử Mất Tích (`disciple`, chương ≥3, cấp ≥20) — 5 bước
1. Lâm Thư nhờ tìm sư đệ; đích = làng Hư Hóa cách 130-170 block (gợi ý la bàn).
2. Tới làng: sinh 6 Hư Thi + đệ tử Lạc Đường; hạ hết.
3. Đệ tử bị Hư khí ăn mòn, giữ Văn Kiện của Sứ Đồ. **Chọn**: lấy Văn Kiện b2 rồi bỏ đi (`c.abandon`, +300 exp, xong) / ở lại bảo vệ (`c.saved`).
4. Đợt Hư Thi thứ hai quanh người chơi.
5. Nhận Văn Kiện b3 + 600 exp + Khai Mạch Đan.
**Về sau**: Mộ Dao bình luận khác nhau cho `c.saved` và `c.abandon`.

### C. Nội Gián Trưởng Lão (`noi_gian`, bắt đầu chương ≥4 cấp ≥30; xung đột chương ≥5 cấp ≥40) — 3 bước
1. Đệ tử bí mật đưa Văn Kiện b4: Trưởng lão Mạc Tùng thông đồng Dạ Khuyết. Giữ kín.
2. Đến chương 5, trưởng lão tìm đến. **Chọn**: vạch trần (`c.expose`, +800 exp, Văn Kiện a5) / vạch trần kèm bằng chứng hộ vệ (`c.expose`+`c.proof`, chỉ hiện khi `qc.merchant.c.truth`; +1200 exp, 2 Khai Mạch Đan) / tống tiền (`c.blackmail`, 3 Khai Mạch Đan) / chưa phải lúc.
8. (chỉ nhánh tống tiền) sau ~60s Dạ Khuyết phái 2 Sát Thủ trả thù, rồi hoàn thành.
**Về sau**: Mộ Dao nhắc `c.expose` hoặc `c.blackmail`.

### D. Lời Hấp Hối (`dying`, cấp ≥8, sự kiện thế giới) — 2 bước
1. Tu sĩ hấp hối trao mảnh bản đồ. **Chọn**: chuyển lời cho gia đình (`c.deliver`) / giữ lấy (`c.keep`). Đích hang ẩn cách 170-230 block.
2. Tới nơi: hé mở cổng Bí Cảnh ẩn (cấp theo `StoryFlow.storyTier`). Nhánh `c.keep`: 2 "Kẻ Truy Lùng Bản Đồ" phục kích. Vào Bí Cảnh = hoàn thành (+500 exp, có thể rơi Văn Kiện). Cổng hết hạn → quay lại bước 1.
**Về sau**: Mộ Dao nhắc `c.keep` / `c.deliver`.

## 4. Độ bền

- Hộ tống chỉ trong RAM: tắt server / đăng xuất → NPC bị dọn, chuỗi lui về chưa nhận (`stage 0`), có thể nhận lại. Escort quá 20 phút → thất bại.
- Mọi nhánh đều kết thúc được; không có bước nào phụ thuộc vật phẩm phải mua.
- Mộ Dao hiện đặt gossip ở **đầu** danh sách entry (xen 1 lượt thoại trước thoại tuyến chính; nói lại lần nữa là ra thoại chính).

## 5. Mở rộng (chưa làm)
- Bảng thông báo (SectBoard) giao cùng các chuỗi này; hiện chỉ giao bằng NPC.
- Cho Hạng Chiêu / Hứa Cổ tham chiếu thêm cờ.
- Lưu escort qua restart (hiện cố ý lui bước cho đơn giản).

## Document Lineage
| Version | Document | Status |
|---|---|---|
| v2 | DESIGN-v2-quest-chains.md (file này) | Current |
