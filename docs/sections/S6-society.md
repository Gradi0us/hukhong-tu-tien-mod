# S6 — Xã Hội: Hệ Tuổi, Tông Môn, Thành & Chức Vị

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2.1 (R1, R5, R10, R12–R14, R21 từ PLAN-v2 §14); trước đó v1: viết mới theo PLAN-v2 canon (§4, §6, §7, §8) + R3
**Status**: ✅ Current (v2.1; còn 1 câu mở ở mục 7)
**Phụ thuộc**: PLAN-v2 canon · R3-pills-age-sect · S3 (đường cong EXP) · S4 (raid, Ấn Hồn Đài, Phế Tích) · S7 (đan dược chi tiết)
**Quy ước**: **[ĐXK]** = lựa chọn thiết kế của S6 (chỉnh được bằng datapack) · **[GIẢ ĐỊNH]** = chờ user xác nhận · **Chu Kỳ** = 7 ngày game ≈ 2h20 online (cùng đơn vị với S4 raid).

---

## Thay đổi v2.1

- **R1**: bỏ hẳn chết vì tuổi; Thiên Nhân Ngũ Suy dừng ở GĐ V (gỡ bằng đột phá / Thọ Nguyên Đan); xóa "Thọ Tận", "Hồi Quang", cờ Bảo Hộ. `onlineTicks` là đồng hồ tuổi duy nhất, chặn AFK (§1.2, §1.5).
- **R5**: Phá Cảnh Đan, Thiên Địa Linh Vật, Tẩy Tủy Đan, Thọ Nguyên Đan, Thần Đan **bind** người nhặt/luyện; đan khác giao dịch được; đổi Cống Hiến lấy đan bind có giới hạn/Chu Kỳ (§1.5, §2.1, §5).
- **R10**: tiền tệ duy nhất = Linh Thạch (4 bậc); Emerald chỉ cho dân làng vanilla (§3).
- **R12**: Ấn Hồn Đài có ở mọi thành/tông/Thánh Địa; cấp thành chỉ giới hạn trần phẩm Lõi; Khảo Hạch ở Thiên Kiếp Đài (§3.1).
- **R13**: Đệ Tử Tông Môn = nhận Thư Tiến Cử vào 1 tông Nhân (không tự vào tông); Hậu Duệ Thế Gia có sàn tư chất C; Con Hoang bị debuff thật (§1.3).
- **R14**: tuổi tối đa vào tông Thiên ≤40 · Địa ≤45 · Nhân ≤55; Thư Tiến Cử bỏ qua giới hạn tuổi (§1.6, §2.2).
- **R22**: mỗi tông có Linh Khí Đài (§2.7: chỗ theo cấp tông, phí/giờ, quyền theo bậc); Bế Quan làm tuổi trôi — ngoại lệ của "AFK dừng tuổi" (§1.2); vẫn không chết vì già.
- **R21**: onboarding mở khóa dần theo cảnh giới (mục 6 mới). Câu hỏi 1, 3, 4 đã giải quyết.

---

## Key Findings

| # | Kết luận | Độ tin cậy |
|---|----------|-----------|
| 1 | 1 ngày game = 1 tháng ⇒ **4 giờ online = 1 tuổi**; 110h = 27,5 năm, 500h = 125 năm (Fermi §1.1) | CAO |
| 2 | Tuổi tính theo **tick online của từng người**, không theo đồng hồ thế giới ⇒ ngủ/`/time set`/server vắng không làm già | CAO |
| 3 | Bảng Thọ Nguyên đảm bảo người chơi chậm nhất (500h) luôn còn đệm ≥ 5× thời gian họ ở cảnh giới đó; áp lực tuổi chỉ đánh vào người **đình trệ** | CAO |
| 4 | Thiên Nhân Ngũ Suy = 5 giai đoạn × 8h online, không giảm tốc độ nhận EXP; **dừng ở V, không ai chết vì tuổi** (R1) | CAO |
| 5 | 7 tông dùng chung một "khung" (bậc, cống hiến, trần/Chu Kỳ) chỉ khác tham số, khảo hạch, nhiệm vụ, buff ⇒ dễ data-driven | CAO |
| 6 | Chống exploit dựa vào 4 đòn bẩy: **trần theo Chu Kỳ, bind tài khoản, trọng số theo chức vị, hạn chót vắng mặt** | TRUNG BÌNH (cần playtest) |

---

## 1. Hệ Tuổi

### 1.1 Thang thời gian + kiểm chứng Fermi

1 ngày game = 24.000 tick = 20 phút thực = 1 tháng tuổi. Suy ra 12 ngày = 4h = 1 tuổi (khớp canon §6).

| Mốc chơi | Phép tính | Tuổi thêm |
|----------|-----------|-----------|
| 110h (cơ duyên, tư chất B) | 110h × 3 ngày/h = 330 ngày = 330 tháng | **27,5 năm** |
| 500h (không vật liệu) | 500 × 3 = 1.500 ngày = 1.500 tháng | **125 năm** |

Sanity check: xuất thân 16 tuổi → lên Thần lúc ~43 tuổi (nhanh) hoặc ~141 tuổi (chậm). Thọ Nguyên cuối phải ≥ 141 mà vẫn có áp lực ⇒ bảng §1.4.

### 1.2 Cách tính tuổi + multiplayer

| Quy tắc | Nội dung |
|---------|----------|
| Nguồn thời gian | `onlineTicks` của từng người (Data Attachment). **24.000 tick online = +1 tháng** |
| Không già khi | offline · đang menu loading · ngủ (đêm bị bỏ qua không tính) · `/time set` · **AFK** (không input/di chuyển > 5 phút, R1) — **ngoại lệ R22**: AFK hợp lệ trong **Bế Quan** tại Linh Khí Đài thì tuổi **vẫn trôi** (1h = 1/4 tuổi; đài Thánh Địa ×0,5). Vẫn không chết vì già: quá Thọ → Ngũ Suy dừng ở GĐ V. Ví dụ: 10h Bế Quan/cấp ⇒ +2,5 tuổi/cấp |
| Vẫn già khi | Đang chơi (online, không AFK); mọi nguồn "thời gian thế giới" khác bị bỏ qua |
| Server ít người | Không ảnh hưởng: ai không online thì không già. Lịch thế giới (Năm/Tháng, chỉ để trang trí và hiển thị sự kiện) **tách khỏi** tuổi cá nhân |
| Thánh Địa (linh mạch) | Trong vòng Thánh Địa tuổi chạy ×0,5 [ĐXK, S4 cần ghi chú] |
| Hiển thị | HUD: `Tuổi 23 / Thọ 100`; vàng khi còn ≤10 năm, đỏ khi ≤3 năm |

### 1.3 Tuổi khởi đầu theo xuất thân [ĐXK]

| Xuất thân | Tuổi | Hệ quả |
|-----------|------|--------|
| Con Hoang | 6 | Nhiều năm nhất, nhưng **debuff thật** (R13): DV khởi đầu −100 ở mọi thành, không quà khởi đầu |
| Thôn Làng | 8 | Trung tính |
| Thành Thị | 10 | DV khởi đầu +50 ở thành quê |
| Di Dân Phế Tích | 12 | Nhiều Linh Dược/Động Phủ (S4) |
| Đệ Tử Tông Môn | 14 | Nhận **Thư Tiến Cử** vào 1 tông Nhân cấp (không phải Huyết Ảnh); **không** tự vào tông, phải đến cổng tông nộp thư (miễn giới hạn tuổi, R13/R14) |
| Hậu Duệ Thế Gia | 16 | Tư chất roll có **sàn C** (nhận vào canon, R13); ít năm nhất |

(Số tuổi lấy từ đề xuất S1 v2.) Tính toán worst-case dùng tuổi 16.

### 1.4 Thọ Nguyên theo cảnh giới [ĐXK]

Giả lập "đường chậm" (500h, tuổi đầu 16): Lv10 @12h, 20 @40h, 30 @85h, 40 @140h, 50 @200h, 60 @265h, 70 @330h, 80 @395h, 90 @450h, 100 @500h. (S3 phải khớp; nếu S3 đổi, tính lại cột đệm.)

| Cảnh giới | Cấp | Thọ Nguyên | Tuổi lúc vào (chậm) | Đệm: giờ tối đa ở lại | Chậm thực tế ở lại |
|-----------|-----|-----------|----------------------|------------------------|---------------------|
| Thường Dân | 1–9 | 50 | 16 | 136h | 12h |
| Nhất Giả | 10–19 | 80 | 19 | 244h | 28h |
| Tông Sư | 20–29 | 100 | 26 | 296h | 45h |
| Đại Tông Sư | 30–39 | 120 | 37 | 332h | 55h |
| Vương Giả | 40–49 | 140 | 51 | 356h | 60h |
| Hoàng Giả | 50–59 | 170 | 66 | 416h | 65h |
| Đế Giả | 60–69 | 200 | 82 | 472h | 65h |
| Thánh Giả | 70–79 | 240 | 98 | 566h | 65h |
| Đấu Giả | 80–89 | 300 | 115 | 740h | 55h |
| Đấu Kình | 90–95 | 400 | 129 | 1.086h | 50h (đến cấp 100) |
| Luyện Thần Giả | 96–98 | 600 | — | — | — |
| Bán Thần | 99 | 1.000 | — | — | — |
| Thần | 100 | ∞ (hệ Tuổi tắt) | — | — | — |

Đệm tối thiểu = 5,9× thời gian đường chậm (Vương Giả) ⇒ đệm này chỉ để Ngũ Suy hiếm khi xuất hiện; không ai bị kết liễu vì tuổi (R1). Áp lực thật chỉ xuất hiện với người ở lì một cảnh giới 250–700h (build, afk, né Bình Cảnh).

### 1.5 Cảnh báo, Thiên Nhân Ngũ Suy, đường gia hạn

**Cảnh báo (trước khi hết Thọ)**

| Còn lại | Cảnh báo |
|---------|----------|
| ≤10 năm (40h) | Toast vàng + thư NPC Dược Sư, HUD vàng |
| ≤3 năm (12h) | HUD đỏ, nhắc **mỗi lần đăng nhập**, NPC tặng **Cứu Mệnh Đan** (1 lần/cảnh giới, phẩm chuẩn của cảnh giới đó) |
| ≤1 năm (4h) | Toast đỏ mỗi 30 phút + chỉ đường tới Đan Phòng gần nhất |

**Thiên Nhân Ngũ Suy** (vượt Thọ Nguyên; **dừng ở GĐ V**, R1; mỗi giai đoạn 2 năm = 8h online; **không** giảm tốc độ nhận EXP, nên đột phá luôn khả thi; đột phá cảnh giới hoặc Thọ Nguyên Đan đủ lớn → xóa ngay):

| GĐ | Tên | Debuff cộng dồn |
|----|-----|-----------------|
| I | Đầu Hoa Tàn | −10% hồi máu tự nhiên; hạt hoa héo quanh người |
| II | Hãn Xuất Dịch Lạnh | −10% tài nguyên tối đa (Linh Lực/Thánh Lực/Bá Khí/Ô Uế) |
| III | Y Cấu Bất Tịnh | −10% HP tối đa, −15% sát thương; giá chợ +20% |
| IV | Thân Phát Uế Khí | −20% HP tối đa, −25% sát thương; NPC tránh nói chuyện (đan phòng vẫn bán Thọ Đan) |
| V | Bất Lạc Bản Tọa | −40% HP tối đa, −35% sát thương, cấm bay, thiền hiệu quả ×0,5 |

**Dừng ở GĐ V (R1)**: không có "Thọ Tận", không chết vì tuổi, không "Hồi Quang". Ở GĐ V vô hạn (debuff nặng), banner đỏ mỗi lần đăng nhập, NPC Dược Sư nhắc đường gỡ: đột phá cảnh giới hoặc Thọ Nguyên Đan đủ lớn → xóa Ngũ Suy ngay.

**Thọ Nguyên Đan** (khung; S7 chi tiết):

| Phẩm | Nhất | Nhị | Tam | Tứ | Ngũ | Lục | Thất | Bát | Cửu |
|------|------|-----|-----|----|-----|-----|-----|-----|-----|
| +Năm Thọ | 5 | 8 | 12 | 18 | 25 | 35 | 50 | 70 | 100 |

Phẩm chuẩn của cảnh giới = ⌈cấp/10⌉ (cấp <10 coi như Nhất). Phẩm thấp hơn chuẩn: hiệu lực ×0,6 mỗi bậc chênh. Tổng "Thọ Bổ Sung" ≤ 50% Thọ cơ bản; giữ khi đột phá. Mỗi viên +Đan Độc; cooldown 4h online. **Bind (R5)**: Thọ Nguyên Đan ràng buộc người luyện/nhặt, không giao dịch; Thần Đan không bán. Giá Đan Phòng tính bằng Linh Thạch (§3).

**Đường gia hạn**: ① đột phá ② mua Đan Phòng (Nhân thành bán tới Tam phẩm, Địa tới Lục, Thiên tới Cửu) ③ đổi Điểm Cống Hiến ④ Cứu Mệnh Đan miễn phí ⑤ Thánh Địa làm chậm tuổi.

### 1.6 Tuổi ảnh hưởng gì

| Lĩnh vực | Luật |
|----------|------|
| Ứng tuyển tông (R14) | Tuổi tối đa: Thiên ≤ 40 · Địa ≤ 45 · Nhân ≤ 55; **Thư Tiến Cử bỏ qua giới hạn tuổi** |
| "Thiên Tài Trẻ" | Đạt cấp 20 trước 18 tuổi, cấp 40 trước 25, cấp 60 trước 35 ⇒ danh hiệu, DV +300 mọi thành, tỉ lệ mời tông ×3 (1 lần/ngưỡng) |
| "Cơ Duyên Hoàng Hôn" | Tuổi ≥ 80% Thọ: sự kiện lão tu sĩ trao đan phương Thọ Nguyên hoặc truyền công (6%/Chu Kỳ) |
| NPC | Xưng hô (tiểu hữu <20 / đạo hữu / tiền bối ≥100); trẻ <14 bị thương nhân hét giá +10%; cảnh giới thấp mà quá 100 tuổi bị NPC khinh ("Lão Bất Tử"), DV −50 |

---

## 2. Tông Môn

### 2.1 Khung chung

**Gia nhập**

| Đường | Luật |
|-------|------|
| Được mời | Mỗi lần qua mốc (cấp 10, 20, 30…) có sự kiện "Sứ Giả Tông Môn". Xác suất = nền × hệ số tư chất. Nền: Nhân 12% · Địa 6% · Thiên 2%. Hệ số: D0,5 · C0,8 · B1 · A1,5 · S2,5 · SS4 · SSS6. Bỏ qua yêu cầu cảnh giới thấp hơn 1 mốc, tuổi tối đa +5. Chỉ mời tông mà đạo/nghề phù hợp |
| Ứng tuyển | Đến cổng tông, thỏa tư chất + cảnh giới + tuổi → làm khảo hạch (§2.3). Trượt: chờ 1 năm tuổi (4h online) |
| Tiến cử | Chân Truyền+ (người chơi) bảo lãnh 1 người/Chu Kỳ: bỏ tuổi, tư chất −1 bậc. Người được tiến cử phản tông ⇒ người tiến cử mất 30% Cống Hiến |
| Một tông | Chỉ 1 tông cùng lúc (canon) |

**Quy tắc đạo** (tông có nhận đạo khác không)
- **Chính đạo**: người có đạo của tông (đạo chính hoặc một đạo trong Song/Tam năng) ⇒ buff đầy đủ.
- **Dị đạo**: đạo khác ⇒ chỉ nhận ở Ngoại/Nội (không Chân Truyền), tư chất yêu cầu +1 bậc, buff ×0,5.
- Tông **Nghề** (Đan Đỉnh, Thiên Công) nhận mọi đạo như nhau.
- Quang Minh Đạo Cung **từ chối mọi Ma Tu**. Huyết Ảnh Ma Môn **chỉ nhận Ma Tu**. Vạn Tượng **chỉ nhận Song/Tam năng**.

**Bậc đệ tử**

| Bậc | Nhân | Địa | Thiên | Thêm |
|-----|------|-----|-------|------|
| Ngoại | khảo hạch | khảo hạch | khảo hạch | — |
| Nội | Lv20 · CH tích lũy 500 | Lv30 · 1.500 | Lv40 · 4.000 | Thắng Quan Khảo NPC "Đại Bỉ" |
| Chân Truyền | Lv40 · 3.000 | Lv50 · 8.000 | Lv60 · 20.000 | Hoàn thành chuỗi "Truyền Thừa" của tông |
| Trưởng Lão | Lv60 · 12.000 | Lv70 · 30.000 | Lv80 · 70.000 | Chuỗi "Hộ Tông"; tối đa 2 người chơi/tông (Thiên: 3) |

**Điểm Cống Hiến (CH)**

| Mục | Luật |
|-----|------|
| Kiếm | Nhiệm vụ tông (40–120 CH, cooldown 1 Chu Kỳ) · nộp vật liệu · hộ tông khi raid. Trần theo Chu Kỳ: Nhân 400 · Địa 800 · Thiên 1.600. Nhiệm vụ quái chỉ tính quái **không phải spawner** (canon §1.1) |
| Tiêu | Cửa hàng tông, học kỹ năng tông, đổi Thọ/Phá Cảnh Đan (đan **bind**, giới hạn 1 viên/loại/Chu Kỳ — R5). Xét bậc bằng CH **tích lũy** (tiêu không làm tụt bậc) |
| Ràng buộc | CH không giao dịch được |

Fermi kiểm: Trưởng Lão Nhân 12.000 CH ÷ 400 = 30 Chu Kỳ ≈ 70h; Thiên 70.000 ÷ 1.600 ≈ 44 Chu Kỳ ≈ 102h ⇒ nằm trong hành trình 110–500h.

**Buff theo bậc (chung)**: Ngoại 25% · Nội 50% · Chân Truyền 100% · Trưởng Lão 100% + Lệnh Bài (hiệu triệu, 1/Chu Kỳ). Số "%" nhân vào buff đặc trưng bên dưới.

### 2.7 Linh Khí Đài (R22)

Mỗi tông có **1 Linh Khí Đài** trong sơn môn; chỉ đệ tử tông đó dùng (Thánh Địa có đài mạnh hơn — S3 §1.7). Số liệu EXP/chế độ ở S3 §1.7, cơ chế ở S5 §3.10.

| Mục | Nhân | Địa | Thiên | Ghi chú |
|-----|------|-----|-------|---------|
| Số chỗ Bế Quan đồng thời | 4 | 8 | 12 | Thánh Địa 24; hết chỗ → xếp hàng, chiếm chỗ không hoạt động > 10 phút bị đá |
| Phí Bế Quan mỗi giờ | 3 CH **hoặc** 1 Linh Thạch Hạ | 6 CH hoặc 2 LT Hạ | 12 CH hoặc 1 LT Trung | CH tiêu → không tụt bậc (tính theo CH tích lũy); Tu luyện chủ động miễn phí |
| Phí Thánh Địa/giờ | — | — | — | 20 CH hoặc 1 LT Trung |

| Bậc đệ tử | Hệ số | Quyền |
|-----------|------:|-------|
| Ngoại | ×1 | Bế Quan tối đa 4 h/phiên; chỗ thường |
| Nội | ×1,5 | 6 h/phiên; ưu tiên hàng chờ |
| Chân Truyền | ×2 | 8 h/phiên; giữ chỗ 1 slot riêng |
| Trưởng Lão | ×2 | 12 h/phiên; giữ chỗ + mở/đóng đài cho tông |

Fermi: Bế Quan Ngoại ở tông Nhân 10 h/cấp × 3 CH = 30 CH/cấp, trần CH/Chu Kỳ Nhân 400 ⇒ dùng CH chỉ bao ≈ 13 cấp/Chu Kỳ trong khi còn nhiệm vụ khác ⇒ phần lớn người dùng Linh Thạch; chống "mua cấp bằng tiền" nhờ cổng Bình Cảnh (S3).

---

### 2.2 Bảy tông

| Tông | Cấp / Đạo | Tư chất | Cảnh giới | Tuổi tối đa | Buff đặc trưng (100%) |
|------|-----------|---------|-----------|-------------|------------------------|
| **Vạn Tượng Thánh Tông** | Thiên / Đa đạo | Song ≥A; Tam ≥B | ≥ Lv30 | 40 | Phạt EXP đa đạo giảm (song ×1,6→×1,3; tam ×2,5→×1,8); chi phí tài nguyên đa đạo ×2→×1,6, ×3→×2,2 |
| **Thiên Kiếm Tông** | Địa / Kiếm | ≥B | ≥ Lv20 | 45 | +15% sát thương kiếm; Linh Lực hồi +20%; triệu hồi lại Bản Mệnh Kiếm nhanh −40% |
| **Quang Minh Đạo Cung** | Địa / Đạo | ≥B | ≥ Lv20 | 45 | Buff đồng đội +15% hiệu lực/+10% thời gian; Thánh Lực hồi +20%; giảm Tâm Ma −25% |
| **Bá Thể Môn** | Nhân / Thể | ≥C | ≥ Lv10 | 55 | +15% HP; Bá Khí hồi +20%; giảm sát thương nhận 8% |
| **Huyết Ảnh Ma Môn** (ẩn) | Nhân / Ma | ≥C | ≥ Lv10 | 55 | Hấp Huyết +20%; Ác Tính tích chậm −20%; "Ẩn Danh" che Ác Tính khỏi dò xét |
| **Đan Đỉnh Tông** | Nhân / Đan | ≥D | ≥ Lv10 | 55 | Tỉ lệ thành đan +10%; Đan Độc tích −20%; công thức tông |
| **Thiên Công Khí Các** | Nhân / Khí+Trận | ≥D | ≥ Lv10 | 55 | Độ bền/tỉ lệ thành khí +10%; bản vẽ Trận Pháp; Trận Nhãn thành +5% HP/mỗi 1 người chơi tông ở đó |

### 2.3 Khảo hạch nhập môn

| Tông | Cơ chế |
|------|--------|
| Vạn Tượng | "Tam Quan": 3 đợt Hư Hóa, mỗi đợt **khóa** mọi đạo trừ một; phải dọn bằng đạo bị chỉ định (10 phút). Tam năng: 3 đợt đủ; Song năng: 2 đợt |
| Thiên Kiếm | Đấu kiếm với Kiếm Đồng (Lv = mình −2), 3 phút, không dùng vật phẩm hồi |
| Quang Minh | "Thanh Tịnh Quan": giữ 3 bệnh nhân NPC sống 5 phút giữa các đợt Hư Hóa; Ác Tính = 0, Tâm Ma <20% |
| Bá Thể | "Cửu Trọng Thạch": chịu 9 đợt đá rơi tăng dần, rồi phá cọc bằng tay không trong 3 phút |
| Huyết Ảnh | "Huyết Tế Thử": hấp thu 5 Tử Tù NPC trong hầm tối mà Ác Tính không vượt 60% |
| Đan Đỉnh | Luyện 1 lô Nhất phẩm đạt chất "Thượng" (công thức + nguyên liệu tông cấp, 3 lần thử) |
| Thiên Công | Dựng Trận Đồ đúng bản vẽ (xếp block, 5 phút) + rèn 1 khí cụ chuẩn |

### 2.4 Cửa hàng + nhiệm vụ đặc trưng (≥4/tông, ngoài tuyến chính)

| Tông | Cửa hàng (CH) | Nhiệm vụ đặc trưng |
|------|---------------|---------------------|
| Vạn Tượng | Tinh Hoa Đa Đạo (giảm phạt tạm), Thọ Đan Ngũ-Lục (limit 1/Chu Kỳ), Thư Tiến Cử | ① Tam Đạo Đồng Sát (diệt elite chỉ định bằng đủ các đạo) ② Hội Giảng (nhận bí tịch 3 tông bạn) ③ Phong Ấn Dị Tượng ④ Hộ Tống Sứ Giả Thánh Địa |
| Thiên Kiếm | Kiếm phôi, Kiếm Phổ, Phá Cảnh Đan Kiếm-hệ | ① Ngộ Kiếm Ngàn Chém ② Tầm Kiếm Thất Lạc ③ Trảm Hư Tu cầm kiếm ④ Thủ Hộ Kiếm Trủng |
| Quang Minh | Thánh Thủy, Thanh Tâm Đan, Chiếu Yêu Kính | ① Thi Cứu Dịch Bệnh ② Thanh Tẩy Điểm Xâm Thực nhỏ ③ Tuần Phòng Nội Gián ④ Hộ Tống Hành Hương |
| Bá Thể | Bá Khí Dịch, Giáp Thể | ① Gánh Đá Lên Núi ② Quyền Đấu Thạch Nhân ③ Nghênh Địch Cổng Thành ④ Tầm Linh Dược Hàn |
| Huyết Ảnh | Huyết Đan, Thuốc Ẩn Danh, Lệnh Xóa Truy Nã | ① Thu Huyết Tế Bí Mật ② Săn Tu Sĩ Tà ③ Cướp Đoàn Buôn (không dân thành) ④ Đột Kích Hang Sứ Đồ |
| Đan Đỉnh | Đan Lô, Công Thức Tứ-Lục, Thọ Đan | ① Hái Dược 100 Năm ② Luyện Đan Thuê (đơn NPC) ③ Giải Dịch Độc ④ Thử Dược Tông |
| Thiên Công | Bản vẽ Trận, Tinh Thiết, Trận Kỳ | ① Sửa Trận Nhãn ② Dựng Trận Phòng Thôn ③ Khai Thác Tinh Thiết ④ Giải Trận Cổ |

### 2.5 Huyết Ảnh Ma Môn: cách tìm

| Bước | Điều kiện |
|------|-----------|
| 1. Dấu Hiệu | Ma Tu lần đầu có Ác Tính ≥50% **hoặc** có ≥30 Huyết Tế NPC (kể cả đường Tu Luyện thường: dò Ô Uế Lực ≥50% Max) ⇒ sự kiện "Bóng Máu" (100%, 1 lần) |
| 2. Huyết Lệnh | Bóng Máu cho manh mối: 3 Mảnh Huyết Lệnh rơi từ Tu Sĩ Tà (rớt 8%) hoặc đổi ở Chợ Đen |
| 3. Cổng Ẩn | Ghép Huyết Lệnh ⇒ la bàn tới cổng trong Dark Forest (S4); cổng chỉ mở cho người có Ô Uế |
| 4. Khảo hạch | Huyết Tế Thử (§2.3). Không có xác suất mời ngẫu nhiên |

### 2.6 Rời tông / phản tông

| Hành vi | Hậu quả |
|---------|---------|
| Từ Môn (xin rời) | Mất toàn bộ CH chưa tiêu; mất buff; giữ kỹ năng tông tới Nội; mất kỹ năng Chân Truyền; chờ 2 Chu Kỳ; vào lại tông khác bắt đầu từ Ngoại |
| Phản tông (giết đồng môn · trộm Tàng Kinh · bán tin cho Sứ Đồ · vào tông đối địch ẩn) | Truy Sát Lệnh 5 Chu Kỳ (Elite đệ tử tông đuổi) · mất sạch CH · Căn Cơ Tổn Thương nhẹ 3 Chu Kỳ · bị 3 tông **cùng cấp** từ chối 10 Chu Kỳ · danh hiệu "Phản Đồ" |
| Người tiến cử | Xem §2.1 |

---

## 3. Thành & Chức Vị

**Tiền tệ (R10)**: mọi giá, phí cư trú, thuế chợ, bổng lộc, Quỹ Thành, giá Đan Phòng tính bằng **Linh Thạch** duy nhất, 4 bậc Hạ/Trung/Thượng/Cực (1 / 100 / 10.000 / 1.000.000 Hạ). **Emerald** chỉ dùng với dân làng vanilla.

### 3.1 Dịch vụ theo cấp thành

10 thành: Thiên 1 (Thiên Khải Hoàng Đô), Địa 3 (Lạc Nhật, Thanh Vân, Bắc Hàn), Nhân 6 (Hắc Thạch Quan, Lưu Ly Cảng, Cổ Tiên Thành, Huyết Nguyệt Phường, Đan Hà Thành, Cơ Xảo Thành — theo S1 v2).

| Dịch vụ | Nhân | Địa | Thiên |
|---------|------|-----|-------|
| Ấn Hồn Đài (R12) | Mọi mốc · trần phẩm Lõi ≤ Hắc | Mọi mốc · trần ≤ Hồng | Mọi mốc · mọi phẩm (kể cả Hồng) |
| Chợ | 1 chợ cơ bản | Chợ + đấu giá | Thị trường người chơi + đấu giá |
| Đan Phòng | Bán ≤Tam phẩm | ≤Lục | ≤Cửu |
| Bảng nhiệm vụ/Chu Kỳ | 6 | 10 | 16 |
| Kho riêng (ô/người) | 27 | 54 | 81 |
| Trận Nhãn | 1 vòng | 2 vòng | 3 vòng |

Ấn Hồn Đài có ở **mọi thành, tông môn và Thánh Địa** (R12); cấp thành chỉ giới hạn **trần phẩm Lõi** (Nhân ≤ Hắc, Địa ≤ Hồng, Thiên = tất cả), không giới hạn mốc. **Khảo Hạch Thăng Thần** diễn ra ở **Thiên Kiếp Đài** (Thánh Địa).

### 3.2 Chức vị: điều kiện thăng

DV = Danh Vọng thành (0–10.000). ĐPT = Điểm Phòng Thủ tích lũy ở thành đó. Giá trị DV/ĐPT là của Nhân; Địa ×1,5; Thiên ×2,5. Lv: Nhân / Địa / Thiên.

| Chức vị | DV | ĐPT | Cảnh giới | Khác |
|---------|----|-----|-----------|------|
| Cư Dân | 0 | 0 | — | Đăng ký + phí cư trú (Linh Thạch) |
| Hộ Vệ | 500 | 100 | 10/30/50 | Tham gia ≥1 raid |
| Đội Trưởng | 1.500 | 300 | 20/40/60 | Dẫn đội Hộ Vệ NPC thắng ≥2 raid |
| Thống Lĩnh | 3.500 | 700 | 30/50/70 | ≥8 raid |
| Phó Thành Chủ | 6.000 | 1.200 | 40/60/80 | Được Thành Chủ **hoặc** Hội Đồng đề cử |
| Thành Chủ | 8.000 | 2.000 | 50/70/90 | Thắng tranh chức (§3.3) |

**Kiếm DV**: nhiệm vụ thành 10–30; thắng raid 50–100; trần 300/Chu Kỳ. **Kiếm ĐPT**: raid theo sát thương/hồi máu/bảo vệ Trận Nhãn (tối đa ~150/raid); Tuần Tra +20/lần (trần 60/Chu Kỳ). Fermi: ĐPT 2.000 ≈ 20 Chu Kỳ tuần tra + vài raid ≈ 50–60h.

### 3.3 Quyền lợi, nghĩa vụ, tranh chức

| Chức | Quyền lợi | Nghĩa vụ |
|------|-----------|----------|
| Hộ Vệ | Bổng lộc/Chu Kỳ, giảm giá 5% | Tuần Tra 1 lần/Chu Kỳ |
| Đội Trưởng | Chỉ huy NPC Hộ Vệ, kho +9 ô | Có mặt ≥1/3 raid |
| Thống Lĩnh | Đặt vị trí phòng thủ, giảm giá 15% | Có mặt ≥1/2 raid |
| Phó Thành Chủ | Dùng Kho Thành, đề xuất thuế | Có mặt ≥1/2 raid |
| Thành Chủ | Đặt thuế chợ 0–10% vào **Quỹ Thành**; chi Quỹ để nâng Trận Nhãn, thuê NPC, mở sự kiện; ban danh hiệu; trục xuất 1 người/Chu Kỳ | Có mặt ≥2/3 raid; trả bổng lộc từ Quỹ |

Thiếu nghĩa vụ ⇒ "Uy Tín" giảm ⇒ dưới 50% thì mở phiên bất tín nhiệm.

**Tranh chức** (1 Thành Chủ/thành)

| Đường | Luật |
|-------|------|
| Bầu cử (mặc định) | Khi ghế do NPC giữ hoặc bỏ trống. 1 Chu Kỳ ứng cử, 1 Chu Kỳ bỏ phiếu. Điểm = 50% phiếu cư dân (trọng số theo chức: Cư Dân 1 · Hộ Vệ 2 · Đội Trưởng 3 · Thống Lĩnh 4 · Phó 5; NPC cư dân bỏ theo DV) + 50% ĐPT chuẩn hóa |
| Bất tín nhiệm | ≥60% phiếu trọng số ⇒ Thành Chủ mất chức, bầu lại |
| Khiêu chiến | Tùy chọn server `city_duel` (**mặc định tắt**): Thống Lĩnh+ thách Thành Chủ đấu tay đôi tại Tâm Thành, cooldown 5 Chu Kỳ |
| Nhường chức | Tự nguyện, người nhận phải đủ điều kiện |

**Vắng mặt**: Thành Chủ offline >7 ngày **thực** ⇒ quyền tạm sang Phó (nếu online) hoặc Quản Gia NPC; >14 ngày ⇒ mất chức, cấm ứng cử lại 5 Chu Kỳ. Chức thấp hơn: mất sau 30 ngày. Ghế trống ⇒ NPC giữ, raid chỉ chạy khi có cư dân online (canon §8).

### 3.4 Danh vọng, Ác Danh, Truy Nã

| Loại | Luật |
|------|------|
| DV thành | Âm được: <0 ⇒ chợ giá +30%, nhiệm vụ khóa; ≤−1.000 ⇒ cấm vào |
| Uy Tín tông | Chính là CH + bậc; phản tông xem §2.6 |
| Giết cư dân thành (kể cả Thôn trong bán kính 400 block) | Ác Danh +1 sao mỗi vụ (Ma Tu hấp thu dân cũng tính; quái và tu sĩ tà không tính) |

| Sao | Hậu quả |
|-----|---------|
| ★1 | Hộ Vệ cảnh cáo, −300 DV |
| ★2 | Hộ Vệ tấn công, mất cư trú |
| ★3 | Truy Nã thành đó + thành cùng vùng |
| ★4 | Thợ săn thưởng (Elite NPC) đuổi |
| ★5 | Mọi thành truy nã, kể cả Thiên Khải |

Giảm 1 sao mỗi Chu Kỳ không phạm thêm; chuộc tội: phạt Hồn Tinh, nhiệm vụ "Chuộc Tội", hoặc Lệnh Xóa Truy Nã của Huyết Ảnh (gói 1 lần).

---

## 4. Quan hệ giữa các hệ thống

| Cặp | Luật |
|-----|------|
| Tông ↔ Thành | Được giữ cả tông và chức vị thành, **trừ** Thành Chủ không đồng thời là Chân Truyền/Trưởng Lão (tránh quyền lực dồn). Đệ tử tông tham gia raid được CH + ĐPT; thành giữ "Hiệp Ước" với tông gần nhất (+5% CH raid) |
| Tông ↔ Danh vọng | Huyết Ảnh bị Quang Minh/Truy Nã săn; DV Ma Tu ở thành chính đạo bị cắt nửa tốc độ |
| Sứ Đồ trà trộn | Mỗi thành/tông có chỉ số **Nội Gián** (0–100%), tăng theo Xâm Thực (+2%/Chu Kỳ khi Xâm Thực chạm biên) và sự kiện. ≥40%: Trận Nhãn −10% hiệu lực; ≥70% trong 1 Chu Kỳ: raid kế tiếp bỏ qua 1 vòng Trận Nhãn ("Nội Ứng") |

**Phát hiện nội gián**

| Cách | Cơ chế | Chi phí/giới hạn |
|------|--------|-------------------|
| Quan sát | NPC nội gián có 15% mang dấu hiệu (câu thoại lạ, vắng mặt ban đêm) | Miễn phí |
| Chiếu Yêu Kính / Đạo tu "Quang Minh Nhãn" | Lộ ngụy trang trong 12 block | Kính của Quang Minh shop; cooldown |
| Tra Xét (Thành Chủ/Phó) | Chọn 1 khu, lộ 1 nội gián | Tốn Quỹ Thành, 1/Chu Kỳ |
| Thẩm Tâm (Trưởng Lão tông) | Như Tra Xét nhưng cho tông | 1/Chu Kỳ |

Bắt nhầm NPC thường: −100 DV (hoặc −50 CH), NPC đó nghỉ 1 Chu Kỳ. Hạ nội gián lộ diện = Elite Sứ Đồ cấp ≤65, rơi Văn Kiện (canon §9) và giảm Nội Gián −25%.

---

## 5. Chống khai thác multiplayer

| Exploit | Phòng thủ |
|---------|-----------|
| Farm Cống Hiến | Trần CH/Chu Kỳ (§2.1); nhiệm vụ cooldown theo tài khoản; vật liệu nộp phải có **dấu chủ** (người thu thập/chế tạo, hiệu lực 3 Chu Kỳ) nên mua chợ không đổi được CH; spawner 0 giá trị (canon); CH không giao dịch; Thọ/Phá Cảnh Đan mua bằng CH giới hạn 1/Chu Kỳ/loại và **bind** (R5) |
| Chiếm Thành Chủ rồi bỏ | Hạn chót vắng mặt 7/14 ngày (§3.3); nghĩa vụ có mặt raid; Quỹ Thành **không rút về cá nhân**; thành thất thủ khi Thành Chủ vắng ⇒ Ác Danh Thất Thủ, cấm ứng cử 10 Chu Kỳ; trục xuất 1 người/Chu Kỳ, kho riêng người bị trục xuất vẫn giữ |
| Alt account | Quyền bầu cần: cư trú ≥3 Chu Kỳ + Lv ≥10 + DV ≥100 + ≥2h online ở thành; **cùng IP/nhóm tài khoản chỉ tính 1 phiếu** (cấu hình whitelist cho gia đình); trọng số theo chức vị làm alt cấp thấp thua; ĐPT/CH chỉ tính hành vi thật (sát thương, hồi máu, Trận Nhãn); người tiến cử chịu trách nhiệm (§2.1); không chuyển DV/CH/Ác Danh; Đan Độc chặn dồn Thọ Đan |
| Buôn đan chủ chốt (R5) | Phá Cảnh Đan, Thiên Địa Linh Vật, Tẩy Tủy Đan, Thọ Nguyên Đan, Thần Đan **bind người nhặt/luyện**, không giao dịch/không trade/không thả cho người khác; đan khác giao dịch được. Đổi CH lấy đan bind: giới hạn 1 viên/loại/Chu Kỳ |

---

## 6. Mở khóa dần theo cảnh giới (Onboarding — R21) [ĐXK]

Chỉ **ẩn UI/thông báo**, không chặn luật (nhặt được đan vẫn dùng được). Mỗi tab mở kèm toast "Hệ thống mới".

| Mốc | Mở khóa |
|-----|---------|
| Cấp 1–9 (Thường Dân) | Chỉ thấy **EXP · đạo · tư chất** + HUD cơ bản (tuổi hiện nhỏ, chưa Thọ) |
| Bình Cảnh đầu (cấp 9 đầy EXP) | Thanh Chướng + **đan / Thiên Địa Linh Vật** (tooltip giải thích) |
| Cấp 10 | **Lõi** (Ấn Hồn Đài), **tông môn** (Sứ Giả, ứng tuyển), **thành & chức vị**, Danh Vọng/Cống Hiến, Thọ Nguyên + cảnh báo tuổi |
| Cấp 30 | **Sứ Đồ / Văn Kiện / manh mối Chấp Hành Giả**, phím R Tối Thượng |
| Cấp 40 | Hồn Cốt |
| Cấp 90+ | Gợi ý Khảo Hạch Thăng Thần |

## 7. Câu hỏi cho user (còn mở)

1. **Khiêu chiến Thành Chủ** bằng PvP: bật mặc định, hay chỉ bầu cử/bất tín nhiệm? *(Mặc định hiện tại: tắt.)*

(Đã giải quyết v2.1: Thọ Tận → R1; Đệ Tử Tông Môn → R13; Ấn Hồn Đài theo cấp thành → R12.)

## 8. Đề xuất đổi canon

| # | Canon | Đề xuất |
|---|-------|---------|
| 1 | §3 Tài nguyên | Thêm 3 chỉ số: **Danh Vọng (DV)**, **Cống Hiến (CH)**, **Ác Danh** |
| 2 | §7 Tông | Trưởng Lão người chơi giới hạn 2–3/tông |
| 3 | §8 Chức vị | Thành Chủ không kiêm Chân Truyền/Trưởng Lão; hạn chót vắng mặt 7/14 ngày thực |

---

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | S6-society.md (bản trước) | Tuổi · Tông Môn · Thành & Chức Vị (theo PLAN-v2) | ✅ Superseded |
| **v2.1** | S6-society.md (file này) | Bỏ chết vì tuổi, bind, Linh Thạch, Ấn Hồn Đài mọi nơi, xuất thân, tuổi vào tông, onboarding | ✅ **Current** |
