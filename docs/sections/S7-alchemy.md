# S7 — Đan Dược, Linh Vật, Luyện Đan, Vật Liệu Đột Phá

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2.1 (R1, R5, R6, R7, R9, R10 từ PLAN-v2 §14); trước đó v1: viết mới theo PLAN-v2 §1.1, §2, §5, §6, §11
**Status**: ✅ Current (v2.1; còn 2 câu mở ở §8.1)
**Phụ thuộc canon**: PLAN-v2 §1.1 (hai cổng mốc), §2 (Đan Độc, Tâm Ma, Ác Tính), §5 (Tẩy Hồn), §6 (Tuổi), §7 (Đan Đỉnh Tông), §10 (multiplayer), §11 (khung đan dược)
**Nguồn tham khảo**: R3-pills-age-sect.md (cảm hứng phẩm đan, ngưỡng hết tác dụng, tuổi dược thảo). Mọi tên đan/hỏa/linh vật dưới đây là tên nguyên bản hoặc tên chung. Mọi con số là **đề xuất ban đầu** (datapack JSON, S3/Reviewer hiệu chỉnh).

---

## Thay đổi v2.1

- **R1**: đồng hồ tuổi duy nhất = `onlineTicks` cá nhân (S6); tuổi dược thảo dùng đồng hồ tick server riêng `WorldClock.serverTicks` (không đọc giờ thế giới, ngủ / `/time set` không ảnh hưởng); Cố Bản Đan tính theo giờ online.
- **R1**: Thọ Nguyên Đan lấy số của S6 (+5…+100 năm theo phẩm 1–9); xóa Trường Sinh Thần Đan (+5.000 năm); Ngũ Suy dừng ở V nên đan chỉ gỡ debuff.
- **R5**: Phá Cảnh Đan (kể cả Tiểu Phá Đan), Thiên Địa Linh Vật, Tẩy Tủy Đan, Thọ Nguyên Đan, Thần Đan **bind** người nhặt/luyện; đan khác giao dịch được; không đấu giá đan bind.
- **R6**: thêm nhóm **Tiểu Phá Đan** (7 loại, §2.A2) cho mốc nhẹ 91–95/96/99, miễn luật "thấp 2 bậc vô hiệu".
- **R7**: Thăng Thần chỉ dùng Thông Thiên Thần Đan; Hỗn Độn Thanh Liên Tử chuyển thành vật phẩm Tầng Thế Giới II.
- **R9**: §6 Tẩy Hồn chỉ còn con trỏ tới S3 §4.5 (≈10–12 giờ Hắc/Hồng; không trừ Thọ, không mất EXP).
- **R10**: Linh Thạch 4 bậc là tiền tệ chốt (bỏ [GIẢ ĐỊNH] và câu hỏi tiền tệ).

---

## Key Findings

| # | Kết luận | Độ tin cậy |
|---|----------|-----------|
| 1 | **Mốc đột phá có 2 đường vật liệu song song**: Phá Cảnh Đan (luyện/mua) HOẶC Thiên Địa Linh Vật (nhặt, có quái canh). Cả hai đều "cơ duyên", không bắt buộc. | CAO |
| 2 | **Đan Độc (0–100) là phanh chính** chống lạm dụng; mọi đan đều cộng độc, thải bằng thiền/Giải Độc Đan. | CAO |
| 3 | **Linh dược có tuổi (niên)**; Dược Viên tăng tuổi theo `WorldClock.serverTicks` (không phụ thuộc giờ thế giới) với **trần theo cấp viên**, phẩm 8–9 chỉ có ở hoang dã/Bí Cảnh. | CAO |
| 4 | **Ai cũng luyện được** (đề xuất), nhưng phẩm tối đa bị khóa bởi **cấp Đan Đạo (1–9)** + phẩm Lô + loại Hỏa. Đan Đỉnh Tông là nguồn đan phương chính, không phải cổng bắt buộc. | TRUNG BÌNH |
| 5 | **Tẩy Hồn tốn ≈ 10–12 giờ chơi (Hắc/Hồng)**, chuẩn ở S3 §4.5; không trừ Thọ, không mất EXP. | CAO |
| 6 | Đơn vị thời gian tuổi: **24.000 `onlineTicks` = 1 tháng tuổi; 4 giờ online = 1 tuổi** (S6, R1). "Ngày game" chỉ là thời lượng hiệu ứng, không phải đồng hồ tuổi. | CAO |

---

## 0. Quy ước chung

- **Phẩm đan** P ∈ {1..9} (Nhất→Cửu) + **Thần Đan**. **Bậc cảnh** B = floor(cấp/10) + 1 (cấp 1–9 → B1 … cấp 90–99 → B10).
- **Hiệu quả theo phù hợp**: đan phẩm P dùng cho nhân vật bậc B:
  - |P − B| ≤ 1: ×1.0.
  - P ≤ B − 2: ×0.25 ("ngưỡng hết tác dụng", gợi ý R3 §1) — Phá Cảnh Đan và Thọ Nguyên Đan **miễn** luật này.
  - P ≥ B + 2: dùng được nhưng **Đan Độc ×2** ("Hư Bất Thụ Bổ").
- **Nguồn đan**: **L** luyện · **R** rơi (boss/sự kiện/Bí Cảnh) · **M** mua (Đan Phường, đấu giá, tông môn) · **C** cơ duyên (sự kiện, Bí Cảnh ẩn).
- **Spawner vô hiệu** (PLAN §1.1): quái từ spawner **không rơi** đan, đan phương, linh vật, linh dược. Chỉ quái tự nhiên / quái canh Linh Vật / boss / sự kiện mới tính (đánh dấu bằng `MobSpawnType`, xem R3 §6).

---

## 1. Phẩm đan, chất lượng, Đan Độc

### 1.1 Chín phẩm + Thần Đan

| Phẩm | Tên gọi | Cấp dùng chính | Lô tối thiểu | Hỏa tối thiểu |
|------|---------|----------------|--------------|---------------|
| 1 | Nhất phẩm | 1–19 | Lô 1 | Thường Hỏa |
| 2 | Nhị phẩm | 10–29 | Lô 2 | Thường Hỏa |
| 3 | Tam phẩm | 20–39 | Lô 3 | Thường Hỏa |
| 4 | Tứ phẩm | 30–49 | Lô 4 | Thú Hỏa |
| 5 | Ngũ phẩm | 40–59 | Lô 5 | Thú Hỏa |
| 6 | Lục phẩm | 50–69 | Lô 6 | Thú Hỏa |
| 7 | Thất phẩm | 60–79 | Lô 7 | Dị Hỏa |
| 8 | Bát phẩm | 70–89 | Lô 8 | Dị Hỏa |
| 9 | Cửu phẩm | 80–99 | Lô 9 | Dị Hỏa |
| Thần | Thần Đan | 99 → 100 | Hồng Mông Lô | **Hợp Hỏa** (2 Dị Hỏa) |

### 1.2 Chất lượng trong cùng phẩm

| Chất lượng | Hiệu ứng | Đan Độc | Ghi chú |
|-----------|----------|---------|---------|
| Hạ | ×0.8 | ×1.2 | Luyện cẩu thả |
| Trung | ×1.0 | ×1.0 | Chuẩn |
| Thượng | ×1.2 | ×0.8 | |
| Cực | ×1.5 | ×0.5 | Vỏ đan sáng |
| **Đan Vân** (chỉ trên Cực) | ×1.5, +10% mỗi vân | ×0.5, −10% mỗi vân | 1–3 vân (xác suất 12% / 3% / 0.5% khi ra Cực) |

Tooltip hiển thị rõ chất lượng + số vân. Đan Vân không đổi phẩm, chỉ đổi giá trị và độc.

### 1.3 Đan Độc (PLAN §2)

**Thang 0–100.** Mỗi viên cộng: `Độc = hệ_số_nhóm × P × hệ_chất_lượng` (hệ nhóm: Phá Cảnh/Tu Vi/Thọ Nguyên = 2.0 · Hồi Phục = 0.5 · Tẩy Tủy = 4.0 · Thanh Tâm/Tịnh Hư = 1.0 · Giải Độc trừ thay vì cộng).

| Mức Đan Độc | Trạng thái | Hậu quả |
|-------------|-----------|---------|
| 0–30 | Thanh Tịnh | Không |
| 31–60 | Độc Tích | Hiệu quả đan −25% |
| 61–85 | Độc Nhập Kinh | Hiệu quả đan −50%; Tâm Ma tăng chậm +1%/ngày game |
| 86–99 | Độc Công Tâm | Hiệu quả −75%; Linh Lực/Bá Khí tối đa −15%; buồn nôn 10 giây khi dùng đan |
| 100 | Đan Độc Bùng Phát | Không dùng được đan 5 phút thật; Tâm Ma +20%; **không gây chết** |

**Thải độc**:
- Tự nhiên: −1 / ngày game.
- **Thiền** (ngồi yên, không chiến đấu): −6 / ngày game.
- **Giải Độc Đan** (§2.G): −15 → −60 một viên.
- **Đan Vân/Cực** giảm độc ngay khi nạp (cột trên).

Ví dụ: người chơi bậc B5 uống 3 viên Tu Vi Ngũ phẩm Trung (3 × 2 × 5 = 30 độc) → ranh giới Thanh Tịnh. Uống thêm nữa chạm ngưỡng Độc Tích. Phá Cảnh Đan Cửu phẩm Trung = 18 độc, không gây vấn đề nếu dùng cách quãng.

---

## 2. Danh mục đan dược (8 nhóm, 45 loại)

Số trong ngoặc ở cột "Hiệu ứng" là % của giá trị tối đa tương ứng. Hồi phục tính theo % để khớp mọi cảnh giới.

### A. Phá Cảnh Đan (10 loại — mỗi mốc 1 đan, mỗi đan chỉ dùng được ở **Bình Cảnh** của mốc đó)

| Mốc | Tên | Phẩm | Hiệu ứng | Giới hạn | Nguồn |
|-----|-----|------|----------|----------|-------|
| 10 | **Khai Mạch Đan** | 1 | Qua Bình Cảnh ngay; buff **Dư Âm** +10% EXP 15 phút thật | 1 viên/mốc | L, M, R |
| 20 | **Ngưng Nguyên Đan** | 2 | Như trên; Dư Âm +12% | 1/mốc | L, M, R |
| 30 | **Thông Huyền Đan** | 3 | Dư Âm +14% | 1/mốc | L, M, R |
| 40 | **Tụ Vương Đan** | 4 | Dư Âm +16% | 1/mốc | L, R, M(tông môn) |
| 50 | **Hoàng Cực Đan** | 5 | Dư Âm +18% | 1/mốc | L, R |
| 60 | **Đế Hoàn Đan** | 6 | Dư Âm +20% | 1/mốc | L, R |
| 70 | **Thánh Khai Đan** | 7 | Dư Âm +22% | 1/mốc | L, R, C |
| 80 | **Hóa Đấu Đan** | 8 | Dư Âm +25% | 1/mốc | L, R, C |
| 90 | **Kình Thiên Đan** | 9 | Dư Âm +30% | 1/mốc | L, R, C |
| Thăng Thần | **Thông Thiên Thần Đan** | Thần | Mở Khảo Hạch Thăng Thần; xem ghi chú | 1 đời | L(cực khó), C |

Luật chung:
- Dùng phẩm **thấp hơn 1 bậc** so với mốc: giảm 40% EXP Bình Cảnh còn lại (không qua ngay). Thấp hơn ≥2 bậc: vô hiệu. Cao hơn: dùng được, thừa hiệu lực.
- Chất lượng **không** ảnh hưởng việc qua Bình Cảnh, chỉ ảnh hưởng độc và Dư Âm (×0.8…×1.5).
- Tu Vi Đan (§C) cộng vào thanh Bình Cảnh chỉ ×0.25 để không "mua đường vòng".
- **Bind (R5)**: Phá Cảnh Đan, Tiểu Phá Đan, Thiên Địa Linh Vật, Tẩy Tủy Đan, Thọ Nguyên Đan, Thần Đan **ràng buộc người nhặt/luyện/mua** (không trade, không đấu giá). Đan khác giao dịch được. Đổi Cống Hiến lấy đan bind có giới hạn/Chu Kỳ (S6).
- **Thần Đan (R7: đường Thăng Thần duy nhất)**: luyện hóa khi **bắt đầu** Khảo Hạch; nếu thua, vẫn giữ trạng thái "Thần Đan Dư Lực" cho lần thử sau (không mất viên).

### A2. Tiểu Phá Đan (7 loại — mốc nhẹ 91–95, 96, 99; R6)

Thuộc nhóm Phá Cảnh Đan (bind). Mỗi loại **chỉ dùng được ở đúng mốc nhẹ** của nó (vạch 1/3, S3 §1.3.2): dùng → qua ngay, bỏ phần 2/3 còn lại. **Miễn** luật "thấp 2 bậc vô hiệu" và luật phẩm thấp/cao (chỉ cần đúng mốc). Đan Độc tính theo phẩm danh nghĩa; không có Dư Âm.

| Mốc | Tên | Phẩm danh nghĩa | Giới hạn | Nguồn |
|-----|-----|----------------:|----------|-------|
| 91 | **Tiểu Kình Đan I** | 5 | 1/mốc | R (tinh anh Hư Hóa cấp 88–95, ~4%), Bí Cảnh, L (Lô 5) |
| 92 | **Tiểu Kình Đan II** | 5 | 1/mốc | như trên |
| 93 | **Tiểu Kình Đan III** | 5 | 1/mốc | như trên |
| 94 | **Tiểu Kình Đan IV** | 5 | 1/mốc | như trên |
| 95 | **Tiểu Kình Đan V** | 5 | 1/mốc | như trên |
| 96 | **Tiểu Luyện Thần Đan** | 7 | 1/mốc | R (boss khu vực / Hư Tu cấp 95+), Bí Cảnh, L (Lô 7) |
| 99 | **Tiểu Bán Thần Đan** | 9 | 1/mốc | R (boss thế giới, Bí Cảnh tầng cuối), C, L (Lô 9) |

Có quái/bí cảnh làm nguồn rơi chính để người chơi không phải chế Cửu phẩm 7 lần; mỗi mốc thiếu đan = tốn thêm ≈ 2,3–2,8 giờ (S3 §1.3.2).

### B. Hồi Phục (5)

| Tên | Phẩm | Hiệu ứng | Giới hạn | Nguồn |
|-----|------|----------|----------|-------|
| Chỉ Huyết Đan | 1 | Gỡ Chảy Máu, hồi 15% HP | CD 10 giây | L, M |
| Hồi Xuân Đan | 1 | Hồi 30% HP trong 5 giây | CD 20 giây | L, M, R |
| Bồi Nguyên Đan | 2 | Hồi 40% tài nguyên đạo (Linh Lực / Thánh Lực / Bá Khí; Ma tu: Ô Uế Lực) | CD 60 giây | L, M |
| Sinh Cơ Đan | 3 | Hồi 60% HP trong 8 giây, gỡ Gãy Xương/Suy Yếu | CD 90 giây | L, M |
| Tục Mệnh Đan | 5 | 30 phút "Tục Mệnh": khi HP < 15% tự hồi 50% HP + bất tử 3 giây (1 lần) | Tối đa 1 viên hiệu lực | L, R |

### C. Tăng Tu Vi (6) — cộng EXP theo % thanh cấp hiện tại

| Tên | Phẩm | Hiệu ứng | Giới hạn | Nguồn |
|-----|------|----------|----------|-------|
| Tụ Khí Đan | 1 | +5% thanh EXP | 3 viên/cấp đầy hiệu quả, viên 4+ ×0.25 | L, M |
| Dưỡng Linh Đan | 2 | +6% | như trên | L, M |
| Ngộ Đạo Đan | 4 | +8% và +20% EXP 30 phút thật (không cộng dồn) | như trên | L, R |
| Hóa Nguyên Đan | 5 | +9% | như trên | L, R |
| Huyền Thiên Tụ Linh Đan | 7 | +10% | như trên | L, R, C |
| Hỗn Nguyên Đại Đan | 9 | +12% | như trên | L, C |

### D. Thọ Nguyên (2 họ)

| Tên | Phẩm | Hiệu ứng | Giới hạn | Nguồn |
|-----|------|----------|----------|-------|
| **Diên Thọ Đan** (Thọ Nguyên Đan theo phẩm) | 1–9 | +Thọ Nguyên theo **bảng S6 §1.5**: **P1 +5 · P2 +8 · P3 +12 · P4 +18 · P5 +25 · P6 +35 · P7 +50 · P8 +70 · P9 +100 năm** | Phẩm chuẩn cảnh giới = ⌈cấp/10⌉; thấp hơn chuẩn ×0,6 mỗi bậc chênh; tổng Thọ Bổ Sung ≤ 50% Thọ cơ bản (giữ khi đột phá); CD 4 giờ online; mỗi viên +Đan Độc; **bind (R5)** | L, M, R |
| **Cố Bản Đan** | 3 | Hoãn "Thiên Nhân Ngũ Suy" 10 giờ online và gỡ 1 giai đoạn suy | CD 1 ngày thật | L, M |

Không còn "Trường Sinh Thần Đan" (+5.000 năm): Thần có Thọ ∞ (S6). Thọ Nguyên Đan chỉ **gỡ / lùi Thiên Nhân Ngũ Suy**, không cứu chết vì tuổi vì không ai chết vì tuổi (R1). Số năm theo S6; S6 hiệu chỉnh nếu lệch.

### E. Tẩy Tủy (2)

| Tên | Phẩm | Hiệu ứng | Giới hạn | Nguồn |
|-----|------|----------|----------|-------|
| Dịch Cân Đan | 3 | Vĩnh viễn +2% HP tối đa; độc +24 | Tối đa 5 viên/đời | L, R |
| **Tẩy Tủy Đan** (tên chung) | 6 | **Nâng tư chất đúng 1 bậc** (D→C→B→A→S), cập nhật hệ số EXP ngay; độc +72 | **Tối đa 2 viên/đời; không vượt S (SS, SSS chỉ do roll)**; **bind (R5)**; cần Đan Độc < 20; mỗi người luyện tối đa 1 viên / 7 ngày thật | **Cực hiếm**: đan phương duy nhất trong Bí Cảnh "Tẩy Tủy Trì" (boss) + C; nguyên liệu Linh Tuyền Tủy (Linh Vật cấp 6 phụ); thường L hoặc R |

### F. Thanh Tâm (4) — chia sẻ **CD chung 5 phút thật** giữa mọi Thanh Tâm Đan (chống xóa rủi ro Ma tu)

| Tên | Phẩm | Hiệu ứng | Nguồn |
|-----|------|----------|-------|
| Ninh Thần Đan | 1 | Tâm Ma −10% | L, M |
| **Thanh Tâm Đan** | 3 | Tâm Ma −30%, **Ác Tính −20%** (thanh) | L, M, R |
| Định Hồn Đan | 5 | Miễn "Mất Kiểm Soát" 5 phút thật (Ác Tính vẫn tăng); Tâm Ma −15% | L, R |
| Phá Chấp Đan | 6 | Trong nhiệm vụ "Phá Chấp" (Tâm Kiếp Bình Cảnh, PLAN §5): giảm 50% thời gian thử thách (không bỏ qua) | R, C |

Thanh Tâm Đan **không** chặn Tẩu Hỏa Nhập Ma khi Ác Tính đã vượt ngưỡng cuối; chỉ dùng được trước đó.

### G. Tịnh Hư & Giải Độc (3 + 2)

| Tên | Phẩm | Hiệu ứng | Nguồn |
|-----|------|----------|-------|
| **Tịnh Hư Đan** | 4 | Gỡ Hư Hóa tầng 1–2 [GIẢ ĐỊNH: S1/S4 định nghĩa tầng] | L, M |
| Hộ Hư Đan | 5 | Miễn Hư Hóa 30 phút thật trong vùng Xâm Thực | L, R |
| Vạn Hư Quy Nhất Đan | 8 | Gỡ mọi tầng Hư Hóa + miễn 1 giờ thật | L, R, C |
| Giải Độc Đan | 1 | Gỡ Độc (poison) + Đan Độc −15 | L, M |
| Bách Độc Giải Đan | 5 | Gỡ mọi debuff độc + Đan Độc −60 | L, R |

### H. Độc Đan (4) — vật phẩm ném/phết vào vũ khí; là sát thương trạng thái (phép), **Đạo tu dùng được** [GIẢ ĐỊNH]

| Tên | Phẩm | Hiệu ứng | Giới hạn | Nguồn |
|-----|------|----------|----------|-------|
| Xích Luyện Độc Đan | 2 | Độc 3% HP tối đa/giây × 10 giây | Boss kháng 80% | L, M |
| Hủ Cốt Đan | 4 | Giảm hồi phục mục tiêu 40% × 15 giây | Boss kháng 80% | L, R |
| Hàn Độc Đan | 5 | Chậm 30% + 2%HP/giây × 8 giây | Boss kháng 80% | L, R |
| Phong Linh Đan | 6 | Phong tỏa Linh Lực/Thánh Lực/Bá Khí mục tiêu 6 giây | Boss kháng 100% | R, C |

---

## 3. Thiên Địa Linh Vật & Linh Dược

### 3.1 Thiên Địa Linh Vật (cấp 1–9) — thay thế Phá Cảnh Đan đúng mốc

Dùng đúng cấp Linh Vật ở Bình Cảnh mốc tương ứng → qua ngay + **Dư Âm** lớn hơn đan (+15% đến +40%) nhưng **thêm Đan Độc 0** (hấp thu tự nhiên). **Bind (R5)**: ràng buộc người thu hái. Linh Vật mọc ở **Linh Vật Điểm** trong hầu hết hang ổ; có **quái canh giữ** (trong lúc quái sống, Linh Vật bị "Linh Khí Ngưng Tụ", không thu hái được). Tái sinh theo ngày thật. HP quái canh scale theo PLAN §10.

| Cấp | Mốc | Tên Linh Vật | Nơi tìm | Quái canh | Số điểm / thế giới | Tái sinh |
|-----|-----|--------------|---------|-----------|--------------------|----------|
| 1 | 10 | **Thanh Mộc Linh Quả** | Rừng Sồi, Bạch Dương, Cánh Rừng Anh Đào | Mộc Linh Hầu (cấp 8) | 40 | 3 ngày |
| 1 | 10 | **Xích Dương Hoa** | Đồng Cỏ Hoa, Savanna | Dương Hỏa Báo (cấp 10) | 30 | 3 ngày |
| 2 | 20 | **Hàn Tuyền Băng Liên** | Đỉnh Núi Tuyết, Vùng Băng | Băng Tinh Lang (cấp 18) | 30 | 5 ngày |
| 3 | 30 | **Hỏa Tâm Thạch Nhũ** | Badlands, hang Y < 0 | Hỏa Giáp Tích Dịch (cấp 28) | 20 | 5 ngày |
| 3 | 30 | **Địa Hoàng Tủy** | Hang Thạch Nhũ | Cổ Mạch Thạch Khôi (cấp 30) | 20 | 5 ngày |
| 4 | 40 | **Ngọc Phách Linh Tuyền** | Hang Lush, dưới sông ngầm | Ngọc Phách Giao (cấp 38) | 12 | 7 ngày |
| 5 | 50 | **Cửu Diệp Kim Ngô** | Rừng Nhiệt Đới sâu | Kim Vĩ Mãng (cấp 48, boss nhỏ) | 8 | 7 ngày |
| 6 | 60 | **Thiên Lôi Trúc Tâm** | Đỉnh Núi trong bão sấm | Lôi Dực Bằng (cấp 58, boss) | 6 | 10 ngày |
| 6 | 60 | **Linh Tuyền Tủy** (phụ, cũng làm nguyên liệu Tẩy Tủy Đan) | Bí Cảnh "Tẩy Tủy Trì" | Thủ Tủy Linh (cấp 62) | 3 | 14 ngày |
| 7 | 70 | **Huyết Bồ Đề** | Đầm Lầy, Rừng Tối | Huyết Bồ Đề Thụ Yêu (cấp 68, boss) | 4 | 14 ngày |
| 8 | 80 | **Nguyệt Hoa Tiên Lộ** | Bí Cảnh "Quảng Hàn Cung" | Hàn Nguyệt Thị Giả (cấp 78, boss) | 2 | 21 ngày |
| 9 | 90 | **Hồng Mông Tử Khí Chi** | Bí Cảnh "Hồng Mông Điện" | Hồng Mông Thủ Hộ (cấp 88, boss lớn) | 1 | 30 ngày |
| 9 | 90 | **Chu Thiên Tinh Hạch** | Lõi Tinh Thạch rơi từ trời (sự kiện sao rơi) | Tinh Hạch Cự Linh (cấp 86) | sự kiện | — |
| Tầng II | — | **Hỗn Độn Thanh Liên Tử** (vật phẩm Tầng Thế Giới II, R7; **không** thuộc đường Thăng Thần) | Hang ổ cuối trong dimension Tầng II | Nguyên Thủy Hư Ảnh | 1 duy nhất / server, hiện sau khi Tầng II mở | — |

Tên Bí Cảnh/tầng là dự kiến, S4 đặt vị trí chính xác; biome dùng tên vanilla cho dễ datapack. Linh Vật thu bằng tay, mỗi người chơi tham gia diệt quái canh có 1 lượt roll (PLAN §10); người gần nhất thu hái. Hạn chế thế giới: tổng điểm cấp 7–9 cực ít → không đủ cho cả server nếu chỉ dựa vào đây → bắt buộc có đường luyện đan song song.

### 3.2 Linh dược có tuổi (niên)

- Mỗi cây dược thảo lưu `age_years` (DataComponent trên item, SavedData/BlockEntity khi còn trồng).
- Chia 9 phẩm theo niên tối thiểu:

| Phẩm | Linh dược chủ | Niên tối thiểu | Nơi mọc hoang | Trồng được? |
|------|---------------|----------------|----------------|-------------|
| 1 | Hồi Linh Thảo | 10 | Đồng bằng, rừng | Dược Viên cấp 1+ |
| 2 | Tụ Khí Hoa | 30 | Cánh đồng hoa | cấp 1+ |
| 3 | Huyết Tham | 80 | Núi, hang | cấp 2+ |
| 4 | Ngọc Tủy Chi | 150 | Hang Lush/Thạch Nhũ | cấp 3+ |
| 5 | Kim Tuyến Liên | 300 | Đầm Lầy, Rừng Nhiệt Đới | cấp 3+ |
| 6 | Thiên Tâm Thảo | 600 | Đỉnh núi, Bí Cảnh sơ | cấp 4 |
| 7 | Cửu Khiếu Linh Chi | 1.000 | Bí Cảnh | cấp 4 |
| 8 | Tinh Nguyệt Thảo | 3.000 | Bí Cảnh cao | **Không** (hoang dã/Bí Cảnh) |
| 9 | Hồng Mông Thảo | 10.000 | Bí Cảnh cuối, Linh Mạch | **Không** |

- **Thu hái**: dùng **Ngọc Sừ** giữ 100% niên; tay không −50% niên (hỏng rễ). Điểm hoang tái sinh 7 ngày thật; thu hái quá 3 lần/tuần cho cùng điểm → niên mới giảm 20% mỗi lần (chống farm). Cây hoang sinh ra với niên ngẫu nhiên ±30% mốc phẩm.
- **Dược Viên** (khối nhiều mảnh: Linh Điền + Tụ Linh Trận Bàn của Thiên Công Khí Các; Linh Chủng lấy 30% khi hái):

| Cấp viên | Tốc độ tăng niên | Trần niên | Yêu cầu |
|----------|------------------|-----------|---------|
| 1 | +1 / ngày game | 30 | Linh Điền cơ bản |
| 2 | +3 | 100 | Tụ Linh Trận Bàn |
| 3 | +8 | 300 | Linh Thủy dẫn |
| 4 | +20 | 1.000 | Trên Linh Mạch Điểm |

  Tuổi dược thảo tính theo **`WorldClock.serverTicks`** (SavedData đếm tick server thực, tính lười khi chunk load; **không đọc giờ thế giới**, nên ngủ và `/time set` không làm cây già nhanh; không dùng `onlineTicks` vì đó là đồng hồ tuổi người chơi, R1). 24.000 serverTicks ≈ 1 "ngày" tăng niên. Ví dụ: Cửu Khiếu Linh Chi (1.000 niên) ở cấp 4: 50 "ngày" ≈ **16,7 giờ thật**.
- **Đột biến** (khi thu hoạch ở cấp 3+: 3%, cấp 4: 6%): 50% niên ×2; 35% nâng 1 phẩm (tối đa P7); 15% thành **Dị Thảo** (nguyên liệu Độc Đan).
- **Linh dược đặc biệt**: Thanh Tâm Thảo (Thanh Tâm), Hư Tịnh Liên (Tịnh Hư, mọc quanh vùng Xâm Thực), Độc Hạt Thảo (Độc Đan).

---

## 4. Luyện đan

### 4.1 Đan Lô (phẩm lô 1–9 + Hồng Mông Lô)

| Phẩm Lô | Tên | Nguồn |
|---------|-----|-------|
| 1 | Thổ Đào Lô | Craft vanilla (đất sét + lò nung) |
| 2 | Thanh Đồng Lô | Craft |
| 3 | Xích Đồng Lô | Craft (Thiên Công Khí Các) |
| 4 | Hàn Thiết Lô | Craft |
| 5 | Huyền Ngân Lô | Craft + boss drop |
| 6 | Tử Kim Lô | Thiên Công Khí Các, đặt riêng |
| 7 | Ô Kim Lô | Bí Cảnh / boss |
| 8 | Thiên Tinh Lô | Bí Cảnh |
| 9 | Hồng Mông Lô | C (cơ duyên) |

Lô phẩm ≥ phẩm đan. Mỗi bậc dư: **+4% thành công, −10% xác suất nổ lô**, tối đa +12%. Lô có **độ bền** (số lần luyện); nổ lô −30% độ bền.

### 4.2 Đan Hỏa

| Loại | Hỏa lực / Ổn định | Phẩm trần | Nguồn |
|------|-------------------|-----------|-------|
| **Thường Hỏa** | thấp / thấp | 3 | Than, củi, dung nham |
| **Thú Hỏa** | vừa / vừa | 6 | "Hỏa Tâm" từ boss thú (Hỏa Lân Vương, Thanh Loan, Huyền Quy) cấp 40–70 |
| **Dị Hỏa** | cao / cao | 9 (+Thần với Hợp Hỏa) | Thu phục, bên dưới |

**9 Dị Hỏa nguyên bản**:

| # | Tên | Đặc tính | Nơi | Cảnh giới thu phục |
|---|-----|----------|-----|--------------------|
| 1 | **Hàn Tinh Băng Diễm** | Ổn định nhất; nhiệt xuống chậm | Đỉnh núi băng sâu | 60 |
| 2 | **Địa Tâm Nham Hỏa** | Hỏa lực +20% | Hồ dung nham sâu Y < −50 | 62 |
| 3 | **Cửu U Minh Hỏa** | Giảm Đan Độc −15% khi luyện | Thung lũng Soul Sand | 65 |
| 4 | **Tử Lôi Thiên Diễm** | Nhiệt dao động mạnh nhưng +8% thành công khi giữ vùng vàng | Đỉnh núi lúc sấm sét | 68 |
| 5 | **Huyền Phong Lưu Hỏa** | Nhiệt tăng nhanh gấp đôi | Đỉnh Núi cao gió lớn | 70 |
| 6 | **Thanh Liên Vô Nhiễm Hỏa** | Chất lượng Đan +1 bậc xác suất cao | Hang Lush hiếm | 72 |
| 7 | **Huyết Nguyệt Ma Diễm** | Tăng Hấp thu cho Ma tu; −5% độc | Sự kiện Huyết Nguyệt | 75 |
| 8 | **Kim Sa Lưu Diễm** | Rút ngắn thời gian luyện 25% | Sa mạc sâu, Bí Cảnh | 78 |
| 9 | **Nguyên Thủy Hư Diễm** | Hỏa lực tối đa; chỉ luyện Cửu phẩm/Thần | Rơi từ Chấp Hành Giả III–IV | 90 |

**Cách thu phục (chung)**:
1. Tìm **Dị Hỏa Điểm** (hang ổ/Bí Cảnh/sự kiện).
2. Đánh bại **Hỏa Linh** (boss, HP scale n người, PLAN §10).
3. Dùng **Ngọc Hỏa Bình** (craft ở Thiên Công Khí Các) thu Hỏa Linh.
4. **Luyện Hóa**: minigame giữ nhiệt 3 phút, thất bại gây Trọng Thương (không chết) + Tâm Ma +10%.

Dị Hỏa ghi vào nhân vật (Data Attachment); tối đa 1 (cấp < 80), 2 (cấp 80+) → **Hợp Hỏa** cho Thần Đan. Mỗi người thu phục riêng (instance), điểm tái sinh 3 ngày thật.

### 4.3 Công thức (Đan Phương)

- **Đan Phương Quyển**: học 1 lần (item tiêu hao), lưu danh sách đã học.
- **Nguồn**: **Đan Đỉnh Tông** (mua bằng điểm cống hiến: Nhất–Tam ở Ngoại môn, Tứ–Lục ở Nội môn+, Thất–Cửu chỉ Trưởng Lão/Chân Truyền) · **rơi** (boss, Sứ Đồ cấp cao) · **Bí Cảnh** (đan phương cổ, gồm Tẩy Tủy Đan, Phá Cảnh Đan Thất–Cửu, Thần Đan).
- **Công thức mẫu Phá Cảnh Đan phẩm P**: 1 chủ dược phẩm P đủ niên + 2 phụ dược phẩm P − 1 + 1 Dẫn Dược (Linh Tuyền Thủy) + Linh Thạch.

### 4.4 Minigame "Khống Hỏa" (mô tả UI)

```
 +---------------------------------+
 |  [Đan Lô]   Tiến độ [=====>   ] |
 |   |   Nhiệt                     |
 |   |  ┌──┐  <- Vùng đỏ (quá nhiệt)|
 |   |  │██│                       |
 |   |  │▓▓│  <- Vùng vàng (đích)  |
 |   |  │░░│  <- Vùng xanh (lạnh)  |
 |   |  └──┘                       |
 |  [Quạt Lửa] giữ chuột  [Bạo Lô ▮▮▯▯] |
 +---------------------------------+
```

1. **Nhập Liệu**: đặt dược liệu vào ô công thức (thứ tự không quan trọng).
2. **Nung Luyện** (30–60 giây): nhiệt tự trôi theo Ổn định của Hỏa; giữ chuột = quạt tăng, thả = giảm. Giữ trong **vùng vàng** làm đầy thanh Tiến độ. Vùng vàng rộng hơn khi Đan Đạo cao. Chạm vùng đỏ làm đầy thanh **Bạo Lô**; đầy → **nổ lô**.
3. **Ngưng Đan**: vòng xung xuất hiện 3 lần, bấm đúng nhịp; độ chính xác → chất lượng.
4. **Đan Vân** (chỉ nếu Cực): 1–3 lần bấm hoàn hảo liên tiếp.

Mô phỏng chạy ở **server** (client chỉ gửi input mỗi tick, có timeout; không cho client tự khai kết quả). Đóng GUI giữa chừng = bỏ lò, mất 50% nguyên liệu.

### 4.5 Tỉ lệ thành công & nổ lô

`Thành công = Nền(P) + Kỹ năng minigame (0…+25%) + Lô dư (+4%/bậc) + Hỏa phù hợp (0…+8%) + Dược dư niên (+3% mỗi ×2 niên) − Thiếu Đan Đạo (−12% mỗi cấp)`, trần 95%.

| Phẩm | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | Thần |
|------|---|---|---|---|---|---|---|---|---|------|
| Nền | 80% | 70% | 60% | 50% | 40% | 30% | 22% | 15% | 10% | 5% |
| Tiêu Tinh Lực | 4 | 6 | 8 | 10 | 12 | 14 | 16 | 18 | 20 | 30 |

**Thất bại**: 70% Phế Đan (mất dược liệu), 20% Đan Phế (đan lỗi, độc), 10% **Nổ Lô** (+ thêm khi Bạo Lô đầy). Nổ lô: sát thương 40% HP tối đa (**không bao giờ chết**, dừng ở 1 HP), lô −30% độ bền, Tâm Ma +5%.

### 4.6 Cấp Luyện Đan Sư (Đan Đạo 1–9)

| Cấp | Danh hiệu | Phẩm tối đa | Điều kiện thăng |
|-----|-----------|-------------|-----------------|
| 1 | Đan Đồ | 1 | Luyện thành công 10 đan |
| 2 | Đan Sĩ | 2 | 30 đan Nhị phẩm + cảnh giới ≥ 10 |
| 3 | Đan Sư | 3 | Thử Luyện Đan Đỉnh Tông (Trung phẩm Tam) + cấp ≥ 20 |
| 4 | Đại Đan Sư | 4 | Thử Luyện + cấp ≥ 30 |
| 5 | Đan Quân | 5 | Thử Luyện + cấp ≥ 40 |
| 6 | Đan Vương | 6 | Thử Luyện + cấp ≥ 50 + 1 Thú Hỏa |
| 7 | Đan Hoàng | 7 | Thử Luyện + cấp ≥ 60 + 1 Dị Hỏa |
| 8 | Đan Thánh | 8 | Thử Luyện + cấp ≥ 70 |
| 9 | Đan Đế | 9 + Thần | Thử Luyện + cấp ≥ 80 + Hợp Hỏa |

**Đề xuất "ai luyện được?"**: **Mở cho mọi người chơi** (mọi đạo), Đan Đạo là kỹ năng nghề độc lập EXP chiến đấu. Chưa học đan phương → không luyện. Đan Đỉnh Tông chỉ cho đan phương tốt + Thử Luyện + buff (Lô giảm giá, nhiệt ổn định +10%). **Tinh Lực Đan Đạo** (pool giới hạn) = 60 + 10 × cấp Đan Đạo; hồi 1/phút thật khi online.

---

## 5. Kinh tế & cân bằng

### 5.1 Tiền tệ (R10 — chốt)

**Linh Thạch** là tiền tệ duy nhất (Emerald chỉ dùng với dân làng vanilla): Hạ = 1, Trung = 100 Hạ, Thượng = 100 Trung (10.000 Hạ), Cực = 100 Thượng (1 triệu Hạ).

### 5.2 Giá tham khảo đan Trung phẩm (Linh Thạch Hạ)

| Phẩm | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | Thần |
|------|---|---|---|---|---|---|---|---|---|------|
| Giá | 100 | 400 | 1.500 | 6.000 | 25.000 | 100.000 | 400.000 | 1,6 triệu | 6,5 triệu | Không bán |

Hạ ×0.5 · Thượng ×2 · Cực ×4 · mỗi vân +50%. Tẩy Tủy Đan **bind**, không bán/đấu giá (R5). Giá chỉ áp đan giao dịch được; đan bind mua ở NPC/tông môn thì bind người mua.

### 5.3 Đan là tiền tệ phụ?

**Không** làm tiền tệ chính. Nhưng tông môn nhận đan nộp đổi điểm cống hiến (cap hàng tuần), NPC thu mua giá bằng 40% giá bán, tối đa 5 viên/giờ.

### 5.4 Chống lạm phát (multiplayer)

| Biện pháp | Tác dụng |
|-----------|----------|
| Nguồn nguyên liệu hữu hạn (Linh Vật Điểm, điểm hoang tái sinh chậm) | Giới hạn sản lượng |
| Tinh Lực Đan Đạo | Giới hạn luyện/ngày/người |
| Độ bền Lô + nổ lô | Sink vật liệu |
| Đan Hạ/Trung phẩm giảm hiệu lực 50% sau 14 ngày thật (Ngọc Bình giữ nguyên); Thượng+, Phá Cảnh (kể cả Tiểu Phá Đan), Tẩy Tủy, Thọ Nguyên, Thần Đan **không** phân rã | Chống tích trữ vô hạn |
| NPC bán Phá Cảnh Đan Nhất–Tam: 2 viên/ngày thật/thành, giá ×1.5, bind người mua; Tứ+ chỉ qua tông môn/Cống Hiến (bind), không đấu giá | Ngăn mua dễ, chặn buôn đan bind (R5) |
| Tẩy Tủy Đan 1/tuần/người + 2/đời | Cực hiếm |
| Spawner vô hiệu | Chống farm tự động |

Boss không scale đan; scale theo người (§10), người nhiều thì loot chia lượt roll.

---

## 6. Vật phẩm Tẩy Hồn

**Mục đích**: đổi Lõi đã chọn (PLAN §5). Làm ở Ấn Hồn Đài.

**Chuẩn nằm ở S3 §4.5 (R9)** — S7 không lặp lại số liệu để khỏi lệch:

- Chi phí theo phẩm Lõi hiện có (Linh Thạch Hạ-quy đổi): Bạch 10.000 · Hoàng 30.000 · Tử 80.000 · Hắc 200.000 · Hồng 500.000, kèm Hồn Tinh cùng phẩm, 1–3 **Tẩy Hồn Thạch**, 1 **Luân Hồi Hoa**.
- ≈ **10–12 giờ** chơi tập trung ở phẩm Hắc/Hồng (4–6 giờ Bạch/Hoàng).
- Roll lại **3 ô thường** cùng phẩm, **không** có ô Phó Mặc; **không trừ Thọ Nguyên, không mất EXP**.
- Nguồn Luân Hồi Hoa / Tẩy Hồn Thạch (Bí Cảnh, boss thế giới, Chấp Hành Giả, tông môn) xem S3 §4.5. Cả hai thuộc vật liệu hiếm; đổi chủ theo luật chung (không thuộc nhóm bind R5).

---

## 7. Bảng vật liệu theo mốc đột phá (S3 tham chiếu)

| Mốc | Phá Cảnh Đan (phẩm) | Chủ dược (niên) | HOẶC Linh Vật | Nơi lấy Linh Vật | Phương nguồn | Lô / Hỏa |
|-----|--------------------|-----------------|----------------|------------------|--------------|----------|
| 10 | Khai Mạch (1) | Hồi Linh Thảo (10) | Thanh Mộc Linh Quả / Xích Dương Hoa | Rừng / Thảo nguyên hoa | Ngoại môn Đan Đỉnh | Lô 1 / Thường |
| 20 | Ngưng Nguyên (2) | Tụ Khí Hoa (30) | Hàn Tuyền Băng Liên | Núi tuyết | Ngoại môn | Lô 2 / Thường |
| 30 | Thông Huyền (3) | Huyết Tham (80) | Hỏa Tâm Thạch Nhũ / Địa Hoàng Tủy | Badlands / Hang | Ngoại môn | Lô 3 / Thường |
| 40 | Tụ Vương (4) | Ngọc Tủy Chi (150) | Ngọc Phách Linh Tuyền | Hang Lush | Nội môn | Lô 4 / Thú |
| 50 | Hoàng Cực (5) | Kim Tuyến Liên (300) | Cửu Diệp Kim Ngô | Rừng Nhiệt Đới sâu | Nội môn | Lô 5 / Thú |
| 60 | Đế Hoàn (6) | Thiên Tâm Thảo (600) | Thiên Lôi Trúc Tâm | Đỉnh Núi bão sấm | Nội môn / Bí Cảnh | Lô 6 / Thú |
| 70 | Thánh Khai (7) | Cửu Khiếu Linh Chi (1.000) | Huyết Bồ Đề | Đầm Lầy / Rừng Tối | Chân Truyền / Bí Cảnh | Lô 7 / Dị |
| 80 | Hóa Đấu (8) | Tinh Nguyệt Thảo (3.000) | Nguyệt Hoa Tiên Lộ | Bí Cảnh Quảng Hàn Cung | Bí Cảnh | Lô 8 / Dị |
| 90 | Kình Thiên (9) | Hồng Mông Thảo (10.000) | Hồng Mông Tử Khí Chi / Chu Thiên Tinh Hạch | Bí Cảnh / sự kiện sao rơi | Bí Cảnh | Lô 9 / Dị |
| Thăng Thần | Thông Thiên Thần Đan | Hồng Mông Thảo ×3 + vật liệu Hư Không | — (R7: không có đường Linh Vật) | — | Bí Cảnh | Hồng Mông Lô / Hợp Hỏa |

**Mốc nhẹ 91–95, 96, 99**: dùng Tiểu Phá Đan (§2.A2), không cần Linh Vật. Mỗi mốc 10–60 có thể qua bằng đan phổ thông (mua/luyện) → khả thi ở tốc độ "có cơ duyên" (≈110 giờ). Mốc 70–90 yêu cầu Bí Cảnh/Dị Hỏa → chính là nơi "cơ duyên" quyết định.

---

## 8. Hoàn tất

### 8.1 Câu hỏi cho user (còn mở)

1. **Luyện đan**: đồng ý **mở cho mọi người chơi** (Đan Đạo độc lập, Đan Đỉnh Tông chỉ là nguồn học + buff), hay chỉ đệ tử Đan Đỉnh Tông?
2. **Tẩy Tủy lên A**: nâng B → A có tự động nhận **Kỹ Năng Bản Mệnh** (PLAN §4 "A+ có KNBM") không?

(Đã giải quyết v2.1: tiền tệ → R10; Linh Vật Thăng Thần → R7.)

### 8.2 Đề xuất đổi canon

| # | Section | Đề xuất |
|---|---------|---------|
| 1 | §2 | Chốt Đan Độc thang 0–100 với 5 ngưỡng (§1.3). |
| 2 | §4 | Tẩy Tủy Đan không nâng SS/SSS (trần S); thêm "Đan Đạo" (cấp 1–9) vào §11 như kỹ năng nghề. |
| 3 | §11 | Thêm "Tinh Lực Đan Đạo" làm giới hạn luyện/ngày. |

---

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | S7-alchemy.md (bản trước) | Đan dược, Linh Vật, Luyện đan, Tẩy Hồn, vật liệu đột phá | ✅ Superseded |
| **v2.1** | S7-alchemy.md (file này) | Tiểu Phá Đan, bind, Thọ Nguyên theo S6, Thần Đan duy nhất, Tẩy Hồn theo S3, Linh Thạch | ✅ **Current** |
