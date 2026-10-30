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


## 6. Cung truyện thành (Đợt 6, 2026-10-30)

7 cung, mỗi cung 3 bước, là chuỗi QuestChains (`qc.<id>.<n>`), engine `story/chain/CityArcs` + toán thuần `CityArcKit`. NPC giao việc đứng trong thành (dialogue JSON, `DialogueService` sinh qua `NpcPlacement`); mỗi cung có sự kiện Lịch lane truyện (`hukhong_events/arc_*.json`, lọc theo chương). Cung là tuỳ chọn: tuyến chính chỉ dùng `CityArcs.duelSpot / arenaTarget / modaoSpot` để chỉ đường và không bao giờ chờ cung.

| Cung (id) | Thành | Ch. | Bước 1 → 2 → 3 | Thưởng |
|---|---|---|---|---|
| Chợ Máu (`cho_mau`) | 3 Huyết Nguyệt Phường | 4 | đêm tại phường → theo dấu lái buôn → hạ Sứ Đồ Huyết Tế Tư (variant 2) | a7, c1 |
| Hắc Triều (`hac_trieu`) | 4 Đan Hà | 5 | dọn quái Hư ven nước → nhặt 5 ngọc trai Hư Hóa → thoại Hải Tướng | c2, cờ `wl.sea_threat` (boss biển sau này) |
| Đấu Giá Đẫm Máu (`dau_gia`) | 5 Cổ Xảo | 5–6 | cảnh đấu giá (Hạng Chiêu vs người mua bí ẩn, 8 dòng) → đuổi theo → hạ kẻ mua | b7, c3; Hạng Chiêu có thoại thêm (`qc.dau_gia.c.seen`) |
| Lôi Vũ Viêm Sơn (`loi_vu`) | 6 Lạc Nhật | 7 | tới hang CHG I → sống sót Lôi Vũ (sự kiện Lịch `arc_loi_vu` cố định hang I, hoặc Lôi Vũ thường trúng hang I) → báo Lão Giả | c4, `ChgService.revealLair(1)`, cờ `wl.chg1_path` |
| Đại Hội Tông Môn (`dai_hoi`) | 7 Thanh Vân | 8 | vòng 1 (1 đệ tử) → bán kết (2 tinh anh) tại võ đài (`ArenaBuilder` dựng cạnh thành) → chung kết = duel `c8_rival` ở võ đài | b8, c5; cờ `rival_saved/lost` như cũ |
| Kiếm Trủng Tuyết (`kiem_trung`) | 8 Bắc Hàn | 9 | tìm Kiếm Trủng → hạ 3 Kiếm Hồn → (hạ CHG III) cảnh Mộ Dao `c9_modao` ở Bắc Hàn | c6, c7, lộ đường CHG III |
| Hoàng Đô Thất Thủ (`hoang_do`) | 9 Thiên Khải | 10 | tới Hoàng Đô → `CityRetake` → thoại Hoàng Tộc | a9, c8, c9; dựng lại cổng Hư Không cạnh thành (cho `c10_gate`) |

Độ bền: cuộc chạm trán (sinh quái) chỉ trong RAM; đăng xuất thì dọn, vào lại sẽ sinh lại khi người chơi đến gần. Mộ Dao bình luận mỗi cung 1 lần (`hukhong.dlg.modao.wl.arc_*`).

### Nguồn Di thư C (c1–c10) — đủ cả 10 không phụ thuộc may rủi
| Nguồn | Di thư |
|---|---|
| Cung thành (bảng trên) | c1 Chợ Máu, c2 Hắc Triều, c3 Đấu Giá, c4 Lôi Vũ, c5 Đại Hội, c6+c7 Kiếm Trủng, c8+c9 Hoàng Đô |
| Hạ Chấp Hành Giả III (`ChapHanhGia.die` → `CityArcKit.chgClue`) | c10 |
| Vùng xâm thực bán kính ≥ 450 (`ZoneMath.clueFor`: `c(1 + (id*7+3) mod 10)`, theo id vùng) | bổ sung ngẫu nhiên theo seed |
| POI thế giới (`structures/`) | chưa có POI nào (Bia Đá, Động Phủ, Cổ Chiến Trường là Đợt 7) — thêm `lore.clue` khi làm |
JUnit `CityArcKitTest.allTenCDocsObtainable` khoá việc đủ 10.

## 7. Kết thúc "Siêu Độ" (`ending_truth`)
- Điều kiện (`EndgameService.truthStatus` / `SieuDoKit.Status`): 10 Di thư C, Hư Ảnh trung bình < 30%, 4 CHG, ≥ nửa số vùng xâm thực đã giải phóng. Màn chọn kết thúc luôn hiện lựa chọn thứ ba; thiếu thì mờ + tooltip/gợi ý từng mục (số liệu do server gửi trong `EndgameNet.OpenChoice`, client không đọc CULTIVATION).
- Sau khi hạ Nguyên Thủy Tịch Diệt và chọn Siêu Độ: `SieuDo` (một phiên mỗi lúc, RAM): INTRO (Dạ Khuyết, cutscene phụ đề) → TÂM CHIẾN (4 tàn ảnh CHG = SuDo yếu 50% máu, mỗi tàn ảnh một câu ký ức bằng subtitle) → GIỮ 4 MẢNH 60s (đứng trong vòng, nhảy né sóng Hư Không; Dạ Khuyết bất tử, noAI) → OUTRO (Mộ Dao về hoặc tàn hồn nếu `mo_dao_died`; Hạng Chiêu tỉnh nếu `rival_lost`) → `EndgameService.completeTruth`. Chết/rời arena = thất bại, vẫn còn lựa chọn.
- Kết quả: Thế Giới Tầng 2 biến thể "Linh Khí Phục Hưng" (`WorldTierData.variant`): vùng cũ chuyển sang giải phóng (tẩy dần), EXP tu luyện ×1.3 trong vùng (`WorldTier.spiritBoost`), Hư mob ×0.8 máu/sát thương, cờ Yêu Tộc trỗi dậy (`WorldTierData.yeuTocThreat`, `wl.yeu_toc_rising`), reset Hư Ảnh, danh hiệu "Siêu Độ Giả" (`title.sieu_do_gia`, tiền tố tên), bản mệnh dạng Thủ Hộ (`banmenh.thu_ho`: +5% hồi/tối đa tài nguyên; ngự bay đường dài chưa làm).
- Chưa kiểm bằng chơi thật (AP4): chỉ có JUnit (`SieuDoKitTest`) và GameTest `CityArcGameTests` (chưa chạy).

## Document Lineage
| Version | Document | Status |
|---|---|---|
| v2 | DESIGN-v2-quest-chains.md (file này) | Current |
