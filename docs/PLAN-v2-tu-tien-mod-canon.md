# PLAN-v2-tu-tien-mod-canon

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2: áp dụng 25 câu trả lời của user + thêm hệ Tuổi, Tông môn, Chức vị thành, Đan dược, Vật liệu đột phá, Sứ Đồ, scaling multiplayer
**Status**: ✅ Current (thay thế PLAN-v1)
**Mục đích**: Luật chung (single source of truth) cho mọi agent. KHÔNG được mâu thuẫn. Muốn đổi → ghi "Đề xuất đổi canon" cuối section.

Tên mod tạm: **Hư Không Tu Tiên Lục** (modid `hukhong`).

> **[GIẢ ĐỊNH]** = Opus tự diễn giải câu trả lời chưa rõ của user → giữ nguyên trừ khi user sửa.

---

## 0. Design Pillars
1. Mỗi 10 cấp là một **phá cảnh** có nghi thức (vật liệu đột phá + chọn Lõi ở Ấn Hồn Đài).
2. **Cơ duyên quyết định tốc độ**: có vật liệu/cơ duyên ≈ 110 giờ lên Thần; không có ≈ 500 giờ (vẫn lên được, rất chậm).
3. Thế giới bị đe dọa thật (mất thành → phế tích), nhưng có đường tái chiếm.
4. Mỗi đạo khác nhau về cơ chế (luật cứng), không chỉ khác số.
5. Vanilla là nền; data-driven (datapack JSON) để cân bằng.
6. **Multiplayer là công dân hạng nhất**: boss scale theo số người, một thành chỉ một Thành Chủ.

## 1. Cảnh giới

| Cấp | Cảnh giới |
|-----|-----------|
| 1–9 | Thường Dân |
| 10–19 | Tu Hành Nhất Giả |
| 20–29 | Tu Hành Tông Sư |
| 30–39 | Tu Hành Đại Tông Sư |
| 40–49 | Tu Hành Vương Giả |
| 50–59 | Tu Hành Hoàng Giả (user chốt giữ) |
| 60–69 | Tu Hành Đế Giả |
| 70–79 | Tu Hành Thánh Giả |
| 80–89 | Tu Hành Đấu Giả |
| 90–95 | Tu Hành Đấu Kình I→V |
| 96–98 | Luyện Thần Giả |
| 99 | Bán Thần |
| 100 | Thần (sau Khảo Hạch Thăng Thần) |

### 1.1 Hai cổng ở mỗi mốc (10, 20, …, 90)
1. **Bình Cảnh (vật liệu)**: ở cấp cuối trước mốc (9, 19, …, 89) thanh EXP vào trạng thái Bình Cảnh. Dùng **Phá Cảnh Đan** đúng phẩm HOẶC **Thiên Địa Linh Vật** tương đương → qua ngay. Không có → vẫn qua được bằng EXP nhưng cần thêm rất nhiều (S3 hiệu chỉnh sao cho cả hành trình không vật liệu ≈ 500 giờ, có vật liệu ≈ 110 giờ ở tư chất B).
2. **Ấn Hồn Đài (Lõi)**: đến mốc, EXP khóa đến khi về thành/tông môn/thánh địa chọn Lõi.
- Phẩm Phá Cảnh Đan theo mốc: mốc 10 → Nhất phẩm … mốc 90 → Cửu phẩm; Thăng Thần → Thần Đan.
- Mốc phụ trong Đấu Kình (91–95), 96, 99: chỉ Bình Cảnh nhẹ (×3 EXP), không Lõi mới.
- **Spawner vô hiệu**: quái sinh từ spawner (vanilla/mod) cho **0 EXP tu luyện, 0 Hồn Tinh, 0 vật liệu tu tiên**.

## 2. Tài nguyên

| Tên | Dùng cho |
|-----|---------|
| Linh Lực (Mana) | Kiếm tu (và chung) |
| Thánh Lực | Đạo tu |
| Bá Khí | Thể tu |
| Ô Uế Lực + **Ác Tính** (thanh rủi ro, thay "Tâm Ma" cho Ma tu) | Ma tu |
| Tâm Ma | mọi đạo khi dùng đồ ma / Đan độc quá mức |
| **Đan Độc** | tích khi dùng đan dược nhiều, giảm hiệu quả đan; thải bằng thiền/đan giải độc |
| **Thọ Nguyên** | tuổi thọ tối đa theo cảnh giới (xem §6) |

## 3. Bốn đạo — luật cứng v2

| Đạo | Luật cứng | Bay |
|-----|-----------|-----|
| **Kiếm Tu** | Công cao nhất; Bản Mệnh Kiếm random, không rơi khi chết (triệu hồi lại) | Cấp 20, tốn Linh Lực |
| **Đạo Tu** | Không cần ăn; **đòn thường lên quái = 0** (chỉ phép giết quái); **đòn thường lên người chơi = ×1**; là đạo **hỗ trợ**: buff Máu, Tấn Công, Tốc Độ, Linh Lực cho đồng đội [GIẢ ĐỊNH: "tc" = tấn công + tốc độ, cả hai đều có] | Cấp 60, cánh thiên sứ |
| **Thể Tu** | Không dùng vũ khí; hệ Bá Khí | Cấp 60, **chỉ tốn Bá Khí** |
| **Ma Tu** | Hấp Huyết nội tại (mọi mục tiêu). **Hấp thu sức mạnh (Huyết Tế, tăng trưởng) CHỈ từ NPC** (quái + dân làng + NPC tu sĩ), **không từ boss và người chơi**. Có 2 lộ: **Hấp Thu** (nhanh, tích Ác Tính) và **Tu Luyện thường** như Đạo tu (chậm, an toàn). **Không có khả năng hỗ trợ người khác** (không buff/heal đồng đội). Ác Tính quá ngưỡng → **Mất Kiểm Soát** (tấn công bừa vài giây) → vượt tiếp → **Tẩu Hỏa Nhập Ma** = chết ngay + Căn Cơ Tổn Thương nặng + mất hết Huyết Tế; hồi sinh thì tu luyện lại bình thường [GIẢ ĐỊNH: không phải xóa nhân vật]. 4 nhánh Thuần/Kiếm/Thể/Đạo giữ nguyên | Ma Kiếm cấp 20; còn lại cấp 60 |

### 3.1 Đa đạo (Song/Tam năng)
- **Dùng đồng thời mọi đạo đã có**; mọi chi phí tài nguyên **×2** (song) / **×3** (tam) [GIẢ ĐỊNH: tam = ×3].
- Phạt EXP giữ: song ×1.6, tam ×2.5.
- Xung đột luật cứng xử lý **theo nguồn đòn**: đòn quyền/cước kỹ năng Thể tu được phép; đòn thường tay không của Đạo tu lên quái = 0 trừ khi nhân vật có Thể tu (khi đó dùng luật Thể tu). Vũ khí cầm tay bị cấm nếu có Thể tu, trừ khi chỉ dùng kỹ năng Kiếm từ Bản Mệnh Kiếm (phi kiếm không cầm tay).

## 4. Tư chất & xuất thân (giữ v1) + hệ số EXP
- D 8% · C 24% · B 28% · A 22% · S 13% · SS 4% · SSS 1%; hệ số EXP D0.6 · C0.8 · B1.0 · A1.15 · S1.3 · SS1.45 · SSS1.6.
- Đơn 75% · Song 20% · Tam 5%. A+ có Kỹ Năng Bản Mệnh.
- Xuất thân: Thành Thị, Thôn Làng, Đệ Tử Tông Môn, Hậu Duệ Thế Gia, Con Hoang, Di Dân Phế Tích. **Mỗi xuất thân có tuổi khởi đầu** (§6).

## 5. Lõi
- Phẩm: Bạch → Hoàng → Tử → Hắc → Hồng → Kim (Thần Lõi). Roll theo **Điểm Vận** (tư chất + Hồn Tinh + cảnh giới). Hệ (set) 2/4/6. Thần Lõi tính +1 cho mọi Hệ.
- **Ấn Hồn Đài hiện 4 ô**: 3 Lõi cùng phẩm + ô thứ 4 **"Phó Mặc Số Phận"** (úp): phẩm roll rộng — có thể **cao hơn** mức tư chất thường đạt, cũng có thể **thấp hơn**; **1% "Mắc Kẹt"** = rơi vào **Tâm Kiếp Bình Cảnh**: kẹt ở cấp hiện tại, EXP đóng băng cho đến khi hoàn thành nhiệm vụ "Phá Chấp" (một thử thách tâm cảnh) [GIẢ ĐỊNH: không kẹt vĩnh viễn].
- **Lõi đã chọn là vĩnh viễn**; đổi chỉ bằng **Tẩy Hồn** — chi phí rất lớn + cần vật phẩm hiếm (Luân Hồi Hoa / Tẩy Hồn Thạch).
- Kỹ năng: **3 phím chủ động + 1 tối thượng**.
- Hồn Cốt 6 slot (giữ v1).

## 6. Hệ Tuổi (mới)
- Thời gian: **1 ngày Minecraft = 1 tháng tuổi** (12 ngày game ≈ 4 giờ thật = 1 tuổi) [đề xuất, S6 hiệu chỉnh].
- Tuổi khởi đầu theo xuất thân (6–16 tuổi). Tuổi ảnh hưởng: điều kiện vào tông môn, một số sự kiện/cơ duyên, NPC đối xử.
- **Thọ Nguyên** tăng theo cảnh giới. Hết thọ nguyên → **không chết ngay**: nhận "Thiên Nhân Ngũ Suy" (debuff tăng dần) cho đến khi đột phá hoặc dùng Thọ Nguyên Đan. Luật bắt buộc: người chơi bình thường (≤ 500 giờ) không bị kết liễu vì tuổi mà không có cảnh báo và đường gia hạn.

## 7. Tông Môn — 7 tông, 3 cấp

| Cấp | Tông môn | Đạo |
|-----|---------|-----|
| Thiên | **Vạn Tượng Thánh Tông** | Đa đạo |
| Địa | **Thiên Kiếm Tông** | Kiếm |
| Địa | **Quang Minh Đạo Cung** | Đạo |
| Nhân | **Bá Thể Môn** | Thể |
| Nhân | **Huyết Ảnh Ma Môn** (ẩn) | Ma |
| Nhân | **Đan Đỉnh Tông** | Luyện đan |
| Nhân | **Thiên Công Khí Các** | Luyện khí + trận pháp |

- Gia nhập: (a) **ngẫu nhiên được mời** (sự kiện, tỉ lệ theo tư chất/thành tích) hoặc (b) **tự đến ứng tuyển**: xét **tư chất + cảnh giới + tuổi** (tông cấp cao đòi cao hơn).
- Mỗi tông có **nhiệm vụ riêng** (không thuộc tuyến chính), **bậc đệ tử** (Ngoại → Nội → Chân Truyền → Trưởng Lão), điểm cống hiến, **buff tông môn**.
- Chỉ thuộc 1 tông tại một thời điểm.

## 8. Thành — 10 thành, 3 cấp + chức vị

| Cấp | Số lượng | Tên cố định |
|-----|---------|-------------|
| Thiên | 1 | **Thiên Khải Hoàng Đô** |
| Địa | 3 | **Lạc Nhật Thành, Thanh Vân Thành, Bắc Hàn Thành** |
| Nhân | 6 | S1 đặt tên |

- Người chơi là **cư dân**, không mặc định là thành chủ. **Chức vị**: Cư Dân → Hộ Vệ → Đội Trưởng → Thống Lĩnh → Phó Thành Chủ → **Thành Chủ** (mỗi thành 1 Thành Chủ trên server; NPC giữ chức nếu chưa có người chơi).
- Mất thành → **Phế Tích**, **tái chiếm được** (tốn công), kho riêng người chơi được giữ.
- Tập kích: tần suất **theo mức Xâm Thực, có trần**; chỉ diễn ra khi có người chơi online liên quan đến thành (cư dân/chức vị) hoặc ở gần — **hoãn** khi không ai online.

## 9. Thế giới, phản diện, cốt truyện

- **Giới hạn thế giới**: worldborder **15.000 block** [GIẢ ĐỊNH: đường kính 15.000, tức ±7.500 từ tâm].
- **Hư Không Xâm Thực** = phản diện **ngoại lai** (không có twist "từng là thần").
- **Phản Thần Dạ Khuyết**: một vị Thần **phản bội nhân loại vì sức mạnh**, mở cửa cho Hư Không → xâm thực nặng nề hơn. Tín đồ: **Sứ Đồ Dạ Khuyết**.
- **Sứ Đồ**: nhiều loại + cấp bậc, **cấp ≤ 65**. Hạ Sứ Đồ rơi **Văn Kiện (manh mối)** — cấp càng cao tỉ lệ càng cao — **chỉ về 2 Chấp Hành Giả đầu**. Gom đủ manh mối → lộ **nơi trú ngụ (hang ổ)** hoặc **kích hoạt sớm sự kiện** của Chấp Hành Giả đó.
- **4 Chấp Hành Giả** (giữ 4 Mảnh Thiên Môn), xuất hiện qua **sự kiện ngẫu nhiên** / phát hiện sau sự kiện / bất ngờ gặp hang ổ (tỉ lệ thấp):

| # | Tên | Đạo bị Hư Không mô phỏng | Cấp |
|---|-----|------|-----|
| I | Thạch Hống | Thể | **67** |
| II | Bạch Từ Âm | Đạo | **78** |
| III | Lăng Uyên | Kiếm | **86** |
| IV | Cơ Vô Tận | Ma | **98** (mạnh nhất) |

- Manh mối III/IV: rơi từ Chấp Hành Giả I/II + sự kiện cấp cao + tuyến truyện.
- **Hai cách chơi**: Theo cốt truyện (sự thật sáng tỏ dần) / Tự do (dựa vào sự kiện và gặp ngẫu nhiên).
- **Kết thúc**: **Phong Ấn** Hư Không Giới hoặc **Hấp Thụ** Hư Không Giới → cả hai mở **Tầng Thế Giới mới** (Thế Giới Tầng II: quái/loot mạnh hơn; khác biệt hương vị theo kết thúc).
- Boss cuối giữ tên v1: **Nguyên Thủy Tịch Diệt** (Chủ Hư Không). Khảo Hạch thử thách 3 = trận này.

## 10. Multiplayer
- HP boss sự kiện/thế giới/Chấp Hành Giả/boss cuối: `HP = HP_gốc × (1 + 0.75 × (n − 1))`, n = số người chơi trong 48 block (cập nhật động, tối đa n = 10). Loot chia theo người tham gia (mỗi người 1 lượt roll).
- Bay trong trận boss: tùy boss (có chiêu khóa bay); **boss thế giới luôn cấm bay**.

## 11. Đan dược (khung — S7 chi tiết)
- Phẩm: **Nhất → Cửu phẩm + Thần Đan**. Nhóm: Phá Cảnh, Hồi Phục, Tăng Tu Vi (EXP), Thọ Nguyên, Tẩy Tủy (nâng tư chất, cực hiếm), Thanh Tâm (giảm Tâm Ma/Ác Tính), Tịnh Hư (giải Hư Hóa), Độc Đan.
- Luyện đan: Đan Lô + Đan Hỏa + công thức; Đan Đỉnh Tông là nguồn học chính. Đan Độc giới hạn lạm dụng.

## 12. Kỹ thuật
- **NeoForge 1.21.1** (AI chọn: API ổn định, tài liệu và mã mẫu nhiều nhất → AI viết code chính xác nhất; GeckoLib có sẵn). Chỉ GeckoLib; Curios không dùng (Hồn Cốt tự làm slot).
- **AI viết code**, user chạy thử và báo lỗi. Hỗ trợ dedicated server từ P0.
- Data Attachments (người chơi), SavedData (thành, tông môn, xâm thực, Chấp Hành Giả), payload sync, Ấn Hồn Đài dùng `MenuType`.

## 13. Phân công v2

| Vai | Model | Output |
|-----|-------|--------|
| Planner | Opus | PLAN-v2 (file này), DESIGN-v2 master |
| R3 | Haiku | research/R3-pills-age-sect.md |
| W1 | Sonnet | S1-lore.md (viết lại) |
| W2 | Sonnet | S2-paths.md (cập nhật) |
| W3 | Sonnet | S3-cores.md (cập nhật) |
| W4 | Sonnet | S4-world.md (cập nhật) |
| W5 | Sonnet | S5-tech.md (cập nhật) |
| W6 | Sonnet | S6-society.md (mới: tuổi, tông môn, chức vị thành) |
| W7 | Sonnet | S7-alchemy.md (mới: đan dược, luyện đan, vật liệu đột phá) |
| Reviewer | Sonnet | REVIEW-v1-tu-tien-mod-design.md (rà soát + cải thiện) |

## 14. Phán quyết sau REVIEW-v1 (v2.1 — Opus, 2026-10-26)

Ưu tiên: user đã chốt > file sở hữu miền (S3 EXP/Lõi · S4 bản đồ/quái · S6 tuổi/tông/thành · S7 đan) > file có mô hình tính.

| # | Phán quyết | Sửa ở |
|---|-----------|-------|
| R1 | **Không bao giờ chết vì tuổi.** Thiên Nhân Ngũ Suy dừng ở giai đoạn V (debuff nặng, gỡ bằng đột phá/Thọ Nguyên Đan). Đồng hồ tuổi duy nhất = S6: `onlineTicks` cá nhân (chỉ khi online, chặn AFK), 24.000 tick = 1 tháng; ngủ và `/time set` không tính | S5 §3.8, S6, S7 |
| R2 | **Bù Tư Chất**: tư chất thấp/đa đạo được thêm nguồn Phá Cảnh Đan (pity Bình Cảnh) sao cho D không vật liệu ≤ ~650 giờ, tam năng ≤ ~900 giờ | S3 |
| R3 | **Một cổng chống farm `KillRateGate`** (trong `gates`): >30 con cùng loài trong bán kính 64 block / 10 phút → thưởng giảm dần đến ×0,1; áp mọi đạo; Ma tu dùng chung cổng (bỏ luật riêng S2 §2.4.5, giữ Ác Danh) | S2, S3, S4, S5 |
| R4 | **EXP vanilla cấp 1–9**: hệ số phẳng m = 1,5 (≈130 con/cấp) + nhiệm vụ nhập môn gánh phần còn lại; từ cấp 10 về 1,5%. Tỉ trọng quái Hư Hóa theo S3 | S3, S4 |
| R5 | **Bind**: Phá Cảnh Đan, Thiên Địa Linh Vật, Tẩy Tủy Đan, Thọ Nguyên Đan, Thần Đan = **ràng buộc người nhặt/luyện** (không trao đổi). Đan khác giao dịch được. Đổi điểm Cống Hiến → đan bind có giới hạn/tuần | S6, S7 |
| R6 | **Mốc nhẹ 91–95/96/99**: S7 thêm nhóm **Tiểu Phá Đan** (gắn nhãn mốc nhẹ, miễn luật "thấp 2 bậc vô hiệu", có nguồn quái/bí cảnh) | S3, S7 |
| R7 | **Thăng Thần dùng Thông Thiên Thần Đan**; Hỗn Độn Thanh Liên Tử chuyển thành vật phẩm Tầng II | S3, S7 |
| R8 | **Ấn Hồn Đài/Phá Chấp**: roll bằng seed lưu trước khi mở UI (thoát/relog không reroll được) | S3, S5 |
| R9 | **Tẩy Hồn** chuẩn = S3, hạ chi phí ≈10–12 giờ ở phẩm Hắc/Hồng; roll 3 ô thường, không Phó Mặc; không trừ Thọ, không mất EXP | S3, S7 |
| R10 | **Tiền tệ duy nhất = Linh Thạch** (4 bậc). Emerald chỉ cho dân làng vanilla | S2, S3, S6, S7 |
| R11 | **Căn Cơ Tổn Thương** chuẩn = S3 (≤3 tầng, −15%/tầng, 10 phút/tầng; Tẩu Hỏa = 3 tầng 30 phút; không mất EXP) | S2 |
| R12 | **Ấn Hồn Đài có ở mọi thành/tông/Thánh Địa**; cấp thành chỉ giới hạn **trần phẩm Lõi** (Nhân ≤ Hắc, Địa ≤ Hồng, Thiên = tất cả). Khảo Hạch ở Thiên Kiếp Đài (Thánh Địa) | S3, S6 |
| R13 | **Xuất thân Đệ Tử Tông Môn** = nhận Thư Tiến Cử vào 1 tông Nhân cấp (không tự vào tông). Hậu Duệ Thế Gia có sàn tư chất C (nhận vào canon). Con Hoang bị debuff thật (danh vọng thành thấp, không quà khởi đầu) | S2, S6 |
| R14 | **Tuổi tối đa vào tông**: Thiên ≤40, Địa ≤45, Nhân ≤55; Thư Tiến Cử bỏ qua giới hạn tuổi | S6 |
| R15 | **Bản đồ** chuẩn = S4 (Hoàng Đô ở vòng 4; Lưu Ly Cảng là cửa vào phổ biến; Thanh Vân là thành lớn). **Văn Kiện** chuẩn = S4 (6/14/28/55% × hệ số loại); bắt sống Ám Ngữ không còn 100% (≤60%, giới hạn 1 lần/ngày/người) | S1, S4 |
| R16 | **Boss multiplayer**: n = số người **gây ≥5% sát thương hoặc hồi máu đáng kể** trong 48 block; loot cần đóng góp tối thiểu; giữ tỉ lệ máu khi n đổi | S4, S5 |
| R17 | **Ma tu**: Huyết Tế từ NPC có quota toàn cục/người/ngày; dân làng sinh từ nhân giống gắn tag → Huyết Tế ×0,1; giết dân làng bất kỳ → Ác Danh | S2, S5 |
| R18 | **Khảo Hạch cá nhân** (tối đa 3 hộ pháp); kết thúc Phong Ấn/Hấp Thụ là lựa chọn **cá nhân**, ghi vào Bia Công Trạng; Tầng Thế Giới II = **dimension mới**; Hấp Thụ để lại hiệu ứng Hư Hóa **chỉ ngoại hình** | S1, S3, S4 |
| R19 | **Văn Kiện/manh mối Chấp Hành Giả chung toàn server**; kích hoạt sớm sự kiện cần party ≥2 người hoặc cảnh giới ≥ cấp CHG −5 | S4, S5 |
| R20 | Roadmap: Đế Hoàn Đan (phẩm 6) vào P1 | S5 |
| R21 | **Onboarding**: hệ thống mở khóa dần theo cảnh giới (Thường Dân chỉ thấy EXP/đạo/tư chất; Lõi từ cấp 10; đan/Linh Vật từ Bình Cảnh đầu; tông/thành chức vị từ cấp 10; Sứ Đồ/Văn Kiện từ cấp 30…) | S2, S6 |

| R22 | **Linh Khí Đài** (user chốt 2026-10-26): mỗi tông 1 đài, Thánh Địa có đài mạnh hơn; chỉ đệ tử tông dùng, hệ số theo bậc (Ngoại ×1 · Nội ×1,5 · Chân Truyền ×2 · Trưởng Lão ×2), số chỗ có hạn. 2 chế độ: **Tu luyện chủ động** (= Thiền S3 §1.7, cap ngày) và **Bế Quan (AFK)** ≈10% tốc độ chơi thường (~0,1 E(L)/giờ, ~10 giờ/cấp, cả hành trình >1.000 giờ). Bế Quan **không qua được Bình Cảnh**, tốn Cống Hiến hoặc Linh Thạch/giờ, **làm tuổi trôi** (ngoại lệ của "AFK dừng tuổi"; vẫn không chết vì già), dừng khi rời đài/bị đánh, không cộng dồn farm quái, phải ở đài thật | S3, S5, S6 |

| R23 | **Tẩu Hỏa Nhập Ma = phế tu vĩnh viễn** (user chốt): nhân vật không thể tu luyện tiếp, chỉ có thể **làm lại từ đầu** (reset toàn bộ tu vi: cấp 1, Lõi, Hồn Cốt, đạo, tư chất → qua lại màn tạo nhân vật; giữ túi đồ, đồ bind tu tiên bị vô hiệu). Bắt buộc **3 tầng cảnh báo** trước ngưỡng (âm thanh + màn hình + chat) và Mất Kiểm Soát vẫn là bước đệm. Thay giả định G4 cũ | S2, S3, S5, S8 |
| R24 | **Phạm vi thế giới = 15.000 block tính từ điểm spawn** (bán kính 15.000, đường kính 30.000). Thay giả định G3 (±7.500): giãn tọa độ thành/tông/vòng độ khó ×2 | S4, S5 |
| R25 | **Phím kỹ năng mặc định Q · E · R · T** (3 + tối thượng T). Vì Q (vứt đồ) và E (túi đồ) trùng vanilla: phím **G bật/tắt "Thế Tu Luyện"** — bật thì Q/E/R/T là kỹ năng, tắt thì trả về vanilla; mọi phím đổi được trong Controls | S2, S5, S8 |
| R26 | Có tùy chọn **tắt hiệu ứng toàn màn hình** (client) cả khi chơi server; font: pixel font giấy phép mở bất kỳ có dấu tiếng Việt | S8 |
| R27 | "tc" và "Mắc Kẹt": user giao Opus quyết → giữ diễn giải G1 (buff Tấn Công + Tốc Độ) và G2 (kẹt đến khi xong Phá Chấp) | — |
| R28 | Tài nguyên hình/âm: AI tự làm (texture pixel bằng script, model khối Blockbench JSON, particle JSON, âm thanh tổng hợp); user không cần vẽ. Có thể thay bằng bản người làm sau | S8 |

| R29 | **Thêm mốc chọn Lõi ở cấp 95 và 99** (user chốt): mốc Lõi = 10,20,…,90, **95**, **99**, 100 (Thần Lõi). Cấp 100 có **bộ chỉ số Thần** riêng (thần lực, bất tử tạm, miễn hiệu ứng…) — thiết kế ở Bước 8.5. **TODO phiên sau**: Bước 8.5 hệ chỉ số + làm lại bảng K (tràn khung, đè chữ, tab/cuộn) + chặn Lõi trùng (hoặc trùng = nâng phẩm) + thiếu Lõi #1 + đổi nhãn "May mắn/Độ bền giáp" sang chỉ số tu tiên + sát thương kỹ năng scale theo chỉ số + hồi tài nguyên theo S2 + chỉ số/kỹ năng riêng cho quái | S2, S3, S5, S8, code |

## Document Lineage
| Version | Document | Status |
|---------|----------|--------|
| v1 | PLAN-v1-tu-tien-mod-canon.md | Superseded |
| v2 | PLAN-v2-tu-tien-mod-canon.md | ✅ Current |
| v2.1 | §14 phán quyết sau REVIEW-v1 (cùng file) | ✅ Current |
