# S8 — UI/UX, Animation và VFX

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — Bản đầu, bám PLAN-v2 (+§14 R1–R22), S2 §2.5 (bộ phím), S3 (Lõi/Ấn Hồn Đài)
**Status**: Draft v1 (chờ review)
**Phạm vi**: giao diện, UX, hoạt họa, hiệu ứng hình/âm, pipeline tài nguyên. Không đổi canon; số liệu game xem S2/S3/S6/S7.

---

## Thay đổi v2.1

- **R25**: phím kỹ năng mặc định Q/E/R + T (Tối Thượng); G bật/tắt "Thế Tu Luyện"; chuyển trang đa đạo đổi sang `V` (§2.2).
- **R23**: thêm UI cảnh báo Tẩu Hỏa 3 tầng và màn hình "Phế Tu - Làm Lại" (§4.4, §2.2b).
- **R26**: tùy chọn tắt hiệu ứng toàn màn hình (client, kể cả khi chơi server); font chốt dùng pixel font giấy phép mở có dấu tiếng Việt (§1.3, §5.5).
- **R28**: tài nguyên hình/âm do AI tự làm (texture bằng script Python/Pillow, model JSON, particle JSON, âm thanh tổng hợp); xóa 3 câu hỏi mở đã trả lời (§5).

---

## Key Findings

| # | Kết luận | Độ tin cậy |
|---|---------|-----------|
| 1 | Người chơi bình thường: **dùng model vanilla + particle + render layer**; GeckoLib chỉ cho **quái/boss, vật thể (Đan Lô, Ấn Hồn Đài), cánh/kiếm bay**. GeckoLib armor cho người chơi bị loại vì xung đột với giáp vanilla | CAO |
| 2 | P0 chỉ cần **HUD + Tạo nhân vật + Ấn Hồn Đài + Bảng tu sĩ** (bản chữ, khung đơn giản); mọi màn khác trễ hơn | CAO |
| 3 | Mọi hiệu ứng P0 dùng **particle vanilla tô màu (DustParticleOptions) + âm vanilla pitch khác** → 0 asset ngoài | CAO |
| 4 | Màu **không bao giờ là kênh thông tin duy nhất** (kèm biểu tượng/chữ) để hỗ trợ mù màu | CAO |
| 5 | UI mở khóa dần theo cảnh giới (R21) → màn hình ẩn khỏi người chưa tới | CAO |

---

## 1. Phong cách hình ảnh chung

**Hướng**: "ngọc giản – cuộn trục" trên nền pixel Minecraft. Texture UI 16×/32×, viền 1px đậm, bóng đổ 1px, không gradient mịn, không bo góc mượt (bo kiểu bậc thang pixel). Cuộn trục = nền giấy be + hai trục gỗ ở đầu; ngọc giản = thanh ngọc xanh nhạt có chỉ vàng. Giữ tỉ lệ pixel nguyên (scale GUI 2–4), không phóng nửa pixel.

### 1.1 Bảng màu theo đạo

| Đạo | Chủ đạo | Phụ | Nhấn | Hex gợi ý (chính / phụ / nhấn) | Biểu tượng (cho mù màu) |
|-----|---------|-----|------|-------------------------------|-------------------------|
| Kiếm Tu | Xanh thép lạnh | Trắng bạc | Xanh lam sáng | `#5FA8D3` / `#E8EEF2` / `#B8F0FF` | Thanh kiếm |
| Đạo Tu | Vàng thánh | Trắng ngà | Hồng nhạt | `#F2C94C` / `#FFF8E1` / `#FFD6E0` | Đóa sen/cánh |
| Thể Tu | Đỏ cam đất | Nâu đồng | Vàng nghệ | `#D2691E` / `#8B5A2B` / `#FFB347` | Nắm đấm |
| Ma Tu | Đỏ máu sẫm | Tím đen | Tím điện | `#8B0000` / `#2A0A2E` / `#B14AFF` | Giọt máu/sừng |
| Hư Hóa (kẻ địch) | Tím than | Xám tro | Xanh lục độc | `#3B2A5E` / `#4A4A4A` / `#6BFF8A` | Mắt dọc |

Quy tắc: mỗi đạo có tài nguyên riêng (Linh Lực xanh lam, Thánh Lực vàng, Bá Khí cam, Ô Uế Lực tím + Ác Tính đỏ); Tâm Ma = tím nhạt; Đan Độc = lục đục. Đa đạo: thanh tài nguyên xếp chồng, mỗi thanh một màu + biểu tượng.

### 1.2 Màu theo phẩm Lõi (và dùng chung cho phẩm đan khi thích hợp)

| Phẩm | Màu | Hex | Viền/hiệu ứng |
|------|-----|-----|---------------|
| Bạch | Trắng | `#EDEDED` | Không |
| Hoàng | Vàng | `#E5C100` | Viền mảnh |
| Tử | Tím | `#9B59D0` | Viền + lấp lánh nhẹ |
| Hắc | Đen/chàm | `#1C1C2E` + viền `#7C7CA8` | Viền sáng, hạt tối |
| Hồng | Đỏ rực | `#E03A3A` | Viền + vầng sáng |
| Kim | Vàng kim ánh | `#FFD700` gradient 3 bước | Viền kép + tia sáng quay (animated) |

Phẩm luôn kèm **số sao/chữ** (Bạch ★1 … Kim ★6) và hình khung khác nhau (đơn → kép → nhọn → vương miện).

### 1.3 Font và khung

- Chữ thường: font Minecraft mặc định (giữ hợp phong cách, hỗ trợ tiếng Việt đủ dấu). Tiêu đề: **font pixel giấy phép mở có dấu tiếng Việt** (R26; hoặc glyph sinh bằng Pillow) (bitmap font provider, ~50 glyph tên cảnh giới/đạo) — P1; P0 dùng chữ vanilla + `§l`.
- Khung: 3 kiểu — *Ngọc giản* (HUD, thông tin ngắn), *Cuộn trục* (Văn Kiện, nhật ký, Tông môn), *Đài đá* (Ấn Hồn Đài, Đan Lô, vật thể thế giới). Cùng một sprite 9-slice cho mỗi kiểu để AI/người tái dùng.

---

## 2. Màn hình và mockup

Quy ước: **phím mặc định** là đề xuất, mọi phím đổi được qua Controls. "Mở khóa" theo R21.

### 2.1 Tạo nhân vật (roll tư chất)
- **Mục đích**: lần đầu vào thế giới: chọn xuất thân, xem kết quả roll tư chất/đạo (D→SSS, Đơn/Song/Tam).
- **Thành phần**: ô xuất thân (6), nút "Roll", hiển thị tư chất với hiệu ứng quay số, đạo trúng, Kỹ Năng Bản Mệnh (A+), tuổi khởi đầu, nút "Chấp nhận".
- **Tương tác**: chọn xuất thân → Roll → xem → Chấp nhận. Roll **một lần** (seed lưu server trước khi mở UI, R8) — thoát/relog không reroll. Reroll chỉ qua vật phẩm hiếm nếu canon cho.
- **Phím**: tự mở lần đầu; `Esc` không bỏ qua (chọn mặc định ngẫu nhiên nếu bấm bỏ qua sau xác nhận).
- **Mở khóa**: lúc sinh ra.

```
+================== SINH RA TRONG HƯ KHÔNG ==================+
| Xuất thân:  [Thành Thị][Thôn Làng][Đệ Tử Tông][Thế Gia]    |
|             [Con Hoang][Di Dân Phế Tích]      (đang: Thôn)  |
|------------------------------------------------------------|
|   TƯ CHẤT:   [ ?? ]  ->  S  ████████░  (hệ số EXP x1.3)    |
|   ĐẠO:       Kiếm + Thể  (Song năng)                       |
|   Bản Mệnh:  Kiếm Tâm Thông Minh                           |
|   Tuổi khởi đầu: 9                                          |
|                                      [ ROLL ]  [ CHẤP NHẬN ]|
+------------------------------------------------------------+
```

### 2.2 HUD
- **Mục đích**: thông tin chiến đấu tức thời, gọn, không che tầm nhìn.
- **Thành phần** (hiện/ẩn theo cảnh giới):
  1. Máu vanilla giữ nguyên + thanh HP số nhỏ.
  2. Thanh tài nguyên theo đạo (chồng khi đa đạo).
  3. Thanh EXP: thay thanh XP vanilla; ở **Bình Cảnh** viền đỏ cam nhấp nháy + biểu tượng khóa; ở mốc chờ Lõi: biểu tượng Ấn Hồn Đài.
  4. **4 ô kỹ năng**: Phím 1/2/3 + Tối Thượng (to hơn, viền vàng), ô xám + số giây khi cooldown, vòng quét ngược chiều kim đồng hồ; đa đạo: mũi tên `V` đổi trang; biểu tượng **Thế Tu Luyện** (bật/tắt bằng `G`) cạnh 4 ô.
  5. Tuổi: "Tuổi 17 · Thọ 80" (góc, mờ) — đổi màu theo Thiên Nhân Ngũ Suy (R1).
  6. Buff/debuff: hàng biểu tượng nhỏ (Căn Cơ Tổn Thương, Đan Độc, Tâm Ma, Hư Hóa, Ác Tính).
- **Phím**: `Alt+H` ẩn/hiện HUD tu tiên; `V` chuyển trang kỹ năng; **`G` bật/tắt "Thế Tu Luyện"** (R25): bật thì kỹ năng mặc định `Q`/`E`/`R` + `T` (Tối Thượng); tắt thì `Q` (vứt đồ) và `E` (túi đồ) trả về vanilla. HUD hiện biểu tượng thế đang bật/tắt; mọi phím đổi được trong Controls.
- **Mở khóa**: Thường Dân chỉ thấy EXP + 1 ô; ô 2 ở cấp 10, ô 3 ở 20/30 (theo kỹ năng), Tối Thượng ở 70.

```
                                               [Tuổi 17 | Thọ 80]
 HP ♥♥♥♥♥♥♥♥♥♥                                  [☠Tâm Ma][⚗Độc x2]
 LL ██████████░░ 84/100  (kiếm)
 BK ██████░░░░░░ 48/80   (thể)
 EXP ████████████░░ Lv27 -> Lv28
 [R: Trảm Khí][V: Kiếm Vực 6s][B: Phi Kiếm][X:Tối Thượng]  G⇄
```

Ác Tính (Ma tu): thanh dọc nhỏ bên cạnh thanh Ô Uế Lực, có vạch ngưỡng "Mất Kiểm Soát" và "Tẩu Hỏa", rung nhẹ khi gần ngưỡng.

### 2.3 Ấn Hồn Đài 4 ô (lật thẻ "Phó Mặc Số Phận")
- **Mục đích**: nghi thức chọn Lõi ở mỗi mốc 10, 20, …, 90 (PLAN §1.1, §5). Điểm nhấn cảm xúc nhất của mod.
- **Thành phần**: 3 thẻ Lõi (mở), 1 thẻ úp "Phó Mặc Số Phận", thông tin Lõi (tên, phẩm, Hệ, kỹ năng gợi ý), đếm Hệ đã sở hữu (2/4/6), nút "Chọn", nút Reroll (Thiên Cơ Phù, nếu có).
- **Tương tác**: di chuột → thẻ nổi lên, hiện tooltip; bấm thẻ thường → xác nhận; bấm thẻ úp → **lật thẻ** (xem 4 bên dưới): thẻ xoay 3D (trục Y) 0,8s, ánh sáng dồn từ rìa, sau đó phẩm hiện — cao hơn thì tia vàng nổ + chuông; thấp hơn thì thẻ xám, chỉ tiếng "tụp" nhẹ; **1% Mắc Kẹt**: thẻ nứt vỡ, màn hình tối + gió, vào Tâm Kiếp Bình Cảnh (nhiệm vụ Phá Chấp). Lõi chọn xong là **vĩnh viễn**: bắt buộc hộp thoại xác nhận có chữ cảnh báo.
- **Phím**: chuột/`1`–`4` chọn thẻ, `Enter` xác nhận, `Esc` đóng (không mất lượt; seed đã lưu — R8).
- **Mở khóa**: cấp 10 (mốc đầu); mọi thành/tông/Thánh Địa có Ấn Hồn Đài (R12); trần phẩm theo cấp thành hiển thị trên đài.

```
+============= ẤN HỒN ĐÀI — Mốc Cấp 20 (Trần phẩm: Hắc) =============+
|  [Lõi 1]        [Lõi 2]        [Lõi 3]        [  ?  ]              |
|  Kiếm Tâm       Tử Điện        Cương Cốt      PHÓ MẶC             |
|  Phẩm: Tử       Phẩm: Tử      Phẩm: Tử       SỐ PHẬN              |
|  Hệ: Thiên      Hệ: Lôi       Hệ: Địa        (úp, phẩm bất định) |
|  ----------------------------------------------------------------  |
|  Hệ hiện có: Lôi 1/2   Địa 0/2   Thiên 1/2                         |
|          [ CHỌN ]            [ Reroll: Thiên Cơ Phù x1 ]           |
+--------------------------------------------------------------------+
Lật thẻ:  [?] -> xoay 0.8s -> ┌── cao hơn: tia vàng nổ, chuông
                               ├── thấp hơn: thẻ xám, tiếng tụp
                               └── 1% Mắc Kẹt: thẻ nứt vỡ, màn hình tối
```

### 2.4 Bảng tu sĩ
- **Mục đích**: tổng quan nhân vật; thay inventory tab phụ.
- **Thành phần** (tab): *Chỉ số* (Công, Thủ, TT, Tốc…, radar nhỏ), *Lõi* (danh sách Lõi theo mốc + Hệ 2/4/6), *Hồn Cốt* (6 slot hình người), *Kỹ năng* (kéo thả vào ô, xem tiến hóa), *Căn Cơ/Tuổi* (Thọ Nguyên, buff, Căn Cơ Tổn Thương), *Cảnh giới* (EXP, Bình Cảnh).
- **Phím**: `K`. **Mở khóa**: ngay đầu (tab Lõi/Hồn Cốt ẩn đến cấp 10).

```
+----------------- BẢNG TU SĨ -----------------+
| [Chỉ số][Lõi][Hồn Cốt][Kỹ năng][Tuổi][Cảnh giới] |
| Cảnh giới: Tu Hành Tông Sư (Lv 24)          |
| Công 540  Thủ 310  TT 220  Tốc 1.18          |
| Hệ: Lôi 2/2 ✔ (Lôi Phạt +10% sát thương)    |
| Hồn Cốt: [Đầu][Tim][Tay][Chân][Lưng][Mắt]    |
+---------------------------------------------+
```

### 2.5 Đan Lô (minigame nhiệt)
- **Mục đích**: luyện đan có kỹ năng, không chỉ bấm nút (S7 giữ số liệu).
- **Thành phần**: ô nguyên liệu, ô công thức, thanh **nhiệt** (đỏ = quá, lam = thiếu, xanh lục = vùng vàng), kim nhiệt dao động, nút thêm Đan Hỏa/giảm lửa, thanh tiến độ, thanh Đan Độc dự kiến.
- **Tương tác**: giữ phím/chuột để tăng nhiệt, thả để nguội; giữ kim trong vùng vàng đủ thời gian; vùng vàng hẹp dần theo phẩm đan; có thể "Dừng sớm" (đan thấp phẩm). Kết quả hiện thẻ phẩm + hiệu ứng khói.
- **Phím**: chuột trái giữ = tăng lửa, `Space` = xả nhiệt, `Esc` = thoát (mất nguyên liệu nếu đang chạy).
- **Mở khóa**: cấp 10 (Đan Đỉnh Tông / học công thức). Có chế độ "Tự động" cho phẩm thấp.

```
 ĐAN LÔ [Nhất phẩm: Hồi Khí Đan]
 [Dược liệu A][Dược liệu B][Đan Hỏa]
 Nhiệt: 低 ░░░░░░░[███▲███]░░░ 高   <- vùng vàng
 Tiến độ ██████░░░░ 60%     Độc dự kiến: ▮▯▯
 [giữ chuột: tăng lửa]  [Space: xả]  [Dừng sớm]
```

### 2.6 Tông môn
- **Mục đích**: gia nhập, nhiệm vụ tông, bậc đệ tử, Cống Hiến, Linh Khí Đài (R22).
- **Thành phần**: banner tông, bậc đệ tử (Ngoại→Trưởng Lão), điểm Cống Hiến, bảng nhiệm vụ, cửa hàng đổi Cống Hiến (đan bind giới hạn/tuần, R5), nút Linh Khí Đài (Tu luyện chủ động / Bế Quan), danh sách đồng môn.
- **Phím**: `O` (khi đã là đệ tử). Gần NPC tông môn mở menu thoại thay thế.
- **Mở khóa**: cấp 10 (ứng tuyển/thư tiến cử).

### 2.7 Thành / chức vị
- **Mục đích**: quan hệ với thành, chức vị (Cư Dân→Thành Chủ), tình trạng Xâm Thực, tập kích.
- **Thành phần**: bản đồ thành mini, thanh Xâm Thực + mức tập kích, danh sách chức vị/quyền hạn, nhiệm vụ thành, kho Phế Tích (nếu mất), danh sách cư dân/NPC quan trọng.
- **Phím**: `M` mở bản đồ mini (xem nhanh), `P` mở bảng Thành. **Mở khóa**: cấp 10.

### 2.8 Văn Kiện / nhật ký truyện
- **Mục đích**: lưu manh mối Sứ Đồ → Chấp Hành Giả (chung server, R19) và nhật ký cốt truyện.
- **Thành phần**: danh sách Văn Kiện (cuộn trục, hình con dấu Dạ Khuyết), thanh tiến độ manh mối cho từng CHG (I→IV), nút "Kích hoạt sớm" (điều kiện party ≥2 hoặc cảnh giới ≥ CHG−5), tab Nhật ký (Theo cốt truyện / Tự do), Bia Công Trạng (kết thúc Phong Ấn/Hấp Thụ).
- **Phím**: `J`. **Mở khóa**: cấp 30 (Sứ Đồ/Văn Kiện); nhật ký cơ bản từ đầu.

### 2.9 Thông báo sự kiện
- **Mục đích**: bí cảnh mở, tập kích thành, boss thế giới/Chấp Hành Giả xuất hiện.
- **Thành phần**: toast góc phải (nhỏ), Title giữa màn hình (lớn, hiếm), Bossbar (trận đang diễn ra), tiếng chuông, la bàn chỉ hướng bí cảnh trong Bảng tu sĩ.
- **Tương tác**: bấm vào toast → mở bản đồ/đánh dấu; sự kiện tập kích có đồng hồ đếm ngược. **Mở khóa**: tùy loại (tập kích từ cấp 10; boss thế giới theo cấp).

### 2.10 Bảng tóm tắt màn hình × phase

| Màn hình | Mở khóa (R21) | Phím | Phase |
|----------|---------------|------|-------|
| Tạo nhân vật | Sinh ra | – | P0 |
| HUD | Sinh ra (mở rộng theo cấp) | `Alt+H` | P0 |
| Ấn Hồn Đài | Cấp 10 | chuột | P0 |
| Bảng tu sĩ | Sinh ra | `K` | P0 (chữ) → P1 |
| Đan Lô | Cấp 10 | chuột | P1 |
| Tông môn | Cấp 10 | `O` | P1 |
| Thành/chức vị | Cấp 10 | `P` | P1–P2 |
| Văn Kiện/nhật ký | Cấp 30 | `J` | P2 |
| Thông báo sự kiện | Tùy | – | P0 (toast cơ bản) → P2 |

---

## 3. UX

### 3.1 Onboarding / hướng dẫn
- **Nguyên tắc** (theo R21): Thường Dân chỉ thấy EXP, đạo, tư chất. Mỗi hệ thống **mở khóa kèm một thẻ giới thiệu** (toast + nút "Xem" mở mục Tutorial ngắn ≤ 5 dòng, có hình).
- **Nhiệm vụ nhập môn** (cấp 1–9) đóng vai trò hướng dẫn: giết 1 quái, mở Bảng tu sĩ, dùng kỹ năng 1.
- **Gợi ý theo ngữ cảnh**: lần đầu Bình Cảnh → giải thích Phá Cảnh Đan; lần đầu tới mốc 10 → chỉ đường đến Ấn Hồn Đài gần nhất.
- Mục **Sổ tay** (nhật ký) tra cứu lại mọi hướng dẫn; có nút "tắt gợi ý".

### 3.2 Thông báo
| Loại | Dùng cho | Quy tắc |
|------|---------|---------|
| Toast | nhận vật phẩm, mở khóa, EXP mốc nhỏ | tối đa 3 toast cùng lúc, tự tắt 4s, gộp trùng |
| Title | đột phá, sự kiện lớn, boss xuất hiện | ≤ 1 title/30s; có thể bỏ qua bằng cài đặt |
| Bossbar | boss/Chấp Hành Giả/bí cảnh đang diễn ra | màu theo đạo CHG; tên + pha |
| Chat | log chi tiết | luôn có, có thể lọc |
| Âm báo | sự kiện nguy hiểm | tiếng riêng, kèm chữ |

### 3.3 Accessibility
- **Cỡ chữ**: tùy chọn UI scale 80–150% cho HUD tu tiên (độc lập GUI scale vanilla); không đặt chữ < 8px.
- **Mù màu**: ba chế độ (Deuteranopia/Protanopia/Tritanopia) hoán đổi bảng màu §1.1; mọi trạng thái đều có **biểu tượng + chữ** kèm màu (HP/Tài nguyên có nhãn, phẩm có sao, cooldown có số).
- **Giảm chuyển động / nhạy cảm ánh sáng**: tắt chớp, giảm rung màn hình, rút ngắn lật thẻ; giới hạn nhấp nháy ≤ 3 lần/giây.
- **Âm thanh**: phụ đề cho âm quan trọng (chuông cảnh báo, boss); thanh trượt âm riêng cho nhạc/effect tu tiên.
- **Điều khiển**: mọi phím đổi được; chế độ "bấm giữ → bấm một lần" cho Đan Lô.

### 3.4 Đa ngôn ngữ vi/en
- Mọi chuỗi qua `assets/hukhong/lang/vi_vn.json` và `en_us.json`; **không hardcode chuỗi** trong Java. Khóa theo mẫu: `screen.hukhong.<màn>.<phần>`, `skill.hukhong.<đạo>.<id>.name/desc`, `realm.hukhong.<n>`, `core.hukhong.<id>`.
- Mặc định `vi_vn` là nguồn; `en_us` fallback do AI dịch, người rà. Tên riêng (Hư Không, Ấn Hồn Đài…) giữ nguyên + chú thích tiếng Anh ở Sổ tay.
- Tiếng Việt dài hơn: layout phải chừa ≥ 30% độ rộng; kiểm thử tooltip tràn.
- Quy ước: tên kỹ năng giữ Hán-Việt cả hai bản (tên riêng), mô tả mới dịch.

---

## 4. Animation và VFX

### 4.1 Người chơi — kỹ thuật đề xuất

**Quyết định**: **không** thay model người chơi (giữ Steve/Alex + skin của người chơi, tương thích mod skin). Dùng 3 tầng:

| Tầng | Kỹ thuật | Dùng cho |
|------|---------|---------|
| T1 | **Particle + animation vanilla qua `RenderLayer`/`PlayerModel` pose hook** (đặt pose tay/chân trong sự kiện `RenderPlayerEvent`/`PoseStack`) | tư thế thiền, vung quyền, giương kiếm |
| T2 | **GeckoLib `GeoRenderLayer` / entity phụ đi theo** (không thay model gốc) | cánh thiên sứ, kiếm bay (Ngự Kiếm), vòng hào quang |
| T3 | **Shader/overlay nhẹ** (full-screen tint, post-effect tùy chọn) | Ma khí, Tẩu Hỏa, Lôi Kiếp |

Lý do: GeckoLib armor model xung đột với giáp vanilla; entity phụ + render layer ghép được với mọi skin. Pose do server quyết định (đồng bộ qua payload) để mọi client thấy giống nhau.

| Hiệu ứng | Kỹ thuật | Chi tiết | Ưu tiên |
|----------|----------|---------|---------|
| Tư thế thiền | T1: ngồi xếp bằng (dùng pose ngồi vanilla + tay đặt đầu gối), particle linh khí xoáy vào người | Loop khi `Thiền`; vòng sáng dưới chân theo màu đạo | P1 |
| Ngự Kiếm | T2: entity Bản Mệnh Kiếm (GeckoLib model 1 mesh) lơ lửng dưới chân, nghiêng theo hướng bay | Kiếm sáng màu theo phẩm; particle đuôi | P1 |
| Cánh thiên sứ (Đạo, cấp 60) | T2: GeckoLib render layer, **tiến hóa theo cảnh giới**: 1 cặp lông xám (60) → 1 cặp ánh vàng (70) → 2 cặp (80) → 3 cặp + vầng sáng (90+) | Animation idle vỗ nhẹ, bay mở rộng | P1 |
| Bá Khí hào quang (Thể) | T3+particle: viền cam quanh người, độ dày theo % Bá Khí; tối đa = vòng lửa cuộn | Bật khi dùng kỹ năng/đầy khí | P1 |
| Ma khí (Ma tu) | T2+T3: khói tím đen quấn tay/chân, mắt đỏ phát sáng (emissive layer); Ác Tính cao → vết nứt đỏ trên da | Cường độ theo Ác Tính; ghi nhận mốc Mất Kiểm Soát | P1 |
| Bay Thể tu (Đạp Không) | particle bậc thang khí dưới chân | – | P2 |

### 4.2 Bảng kỹ năng 3+1 của 4 đạo

Ưu tiên: **P0** = hiệu ứng tối thiểu (particle vanilla + âm vanilla), **P1** = hiệu ứng chính (particle tùy biến + model GeckoLib nhỏ), **P2** = bản đầy đủ (animation, beam, sync khung). Thời lượng = thời gian hiển thị hiệu ứng, không phải CD.

#### Kiếm Tu

| Kỹ năng | Hiệu ứng hình | Âm thanh | Thời lượng | Ưu tiên |
|---------|---------------|----------|-----------|---------|
| Phím 1 Trảm Khí | Lưỡi trăng xanh bay thẳng 8 block (particle `crit`/dust xanh xếp cung); tiến hóa: 3 → 4 nhát với vệt khác nhau | `player.attack.sweep` pitch cao + vút | 0,3s | P0 |
| Phím 2 Kiếm Vực | Vòng kiếm khí xoay quanh người bán kính 3→6 block; lớp particle `sweep_attack` quanh vòng | Tiếng gió kiếm lặp | 4s | P1 |
| Phím 3 Phi Kiếm Truy Hồn | 4/6/8 kiếm nhỏ (model GeckoLib `phi_kiem`) bay vòng cung tới mục tiêu, để vệt sáng | `trident.throw` + chuông nhỏ khi trúng | 1,5s | P1 |
| Tối Thượng Kiếm Ý Hóa Hình | Hào quang kiếm xanh sáng cuộn quanh người; một kiếm khổng lồ mờ treo trên đầu; Phi Kiếm CD nhanh thấy thêm tia | Chuông trầm + tiếng kiếm ngân | 12s | P2 |

#### Đạo Tu

| Kỹ năng | Hiệu ứng hình | Âm thanh | Thời lượng | Ưu tiên |
|---------|---------------|----------|-----------|---------|
| Phím 1 Thánh Quang Tiễn | Tia vàng thẳng (beam mảnh, particle `end_rod` vàng), nổ nhỏ khi trúng; Thánh Quang Trận: vòng sáng dưới đất 6 block; Thiên Phạt: cột sáng từ trời | Tiếng chuông "ting", Thiên Phạt: sấm nhẹ | 0,4s / 5s / 1s | P0 (tiễn), P1 (trận/Phạt) |
| Phím 2 Trị Liệu | Vòng sáng xanh lục-vàng quanh mục tiêu, hạt trái tim bay lên; tia nối sang người thứ hai ở cấp 40 | `experience_orb.pickup` chậm | 1s | P0 |
| Phím 3 Ban Phúc | Vầng sáng đổi màu theo chế độ (Máu đỏ hồng/Tấn công cam/Tốc độ lam/Linh Lực xanh dương) dưới chân + biểu tượng nhỏ trên đầu | Tiếng hát nhẹ (4 biến thể pitch) | 1,2s | P1 |
| Tối Thượng Thánh Giáng | Cột sáng vàng lớn từ trời, cánh sáng khổng lồ mờ sau lưng, vòng AoE 12 block lan ra, hạt lông vũ rơi | Hợp xướng + chuông | 3s (AoE), buff 10s | P2 |

#### Thể Tu

| Kỹ năng | Hiệu ứng hình | Âm thanh | Thời lượng | Ưu tiên |
|---------|---------------|----------|-----------|---------|
| Phím 1 Quyền Phá | Vệt quyền cam, nổ nhỏ chạm mục tiêu; Long Hổ Quyền: bóng rồng-hổ (particle xếp dạng) lướt qua | `player.attack.knockback` / tiếng rống | 0,3s / 0,8s | P0 |
| Phím 2 Thiết Bố Sam | Viền kim loại ánh đồng quanh người, gợn sóng; phản chấn: vòng xung kích phát ra khi bị đánh | Kim loại "keng" | 6s | P1 |
| Phím 3 Chấn Địa | Vết nứt đất lan ra 4 block (particle block-crack từ block bên dưới), bụi bay lên, rung camera nhẹ | `generic.explode` trầm + `anvil.land` | 0,6s | P1 |
| Tối Thượng Kim Thân Bất Hoại | Da phủ lớp vàng kim lấp lánh, hào quang tăng mạnh, tiếng chuông lớn khi kích hoạt | Chuông trầm + tiếng vang | 8s | P2 |

#### Ma Tu

| Kỹ năng | Hiệu ứng hình | Âm thanh | Thời lượng | Ưu tiên |
|---------|---------------|----------|-----------|---------|
| Phím 1 Huyết Trảo | Vệt móng ba đường đỏ sẫm, giọt máu bay ngược về người (hút máu), flash đỏ nhẹ | `player.attack.sweep` chậm + "tạch" | 0,3s | P0 |
| Phím 2 Ma Khí Hộ Thể | Khiên khói tím bao quanh (ellipsoid particle), vỡ thì nổ vòng 6 block tím đỏ, tia máu về người | Tiếng thủy tinh vỡ trầm | 6s lá chắn; 5s nổ | P1 |
| Phím 3 Ma Ảnh | Lướt 6 block để lại dải bóng đen mờ; ảnh mờ biến mất | `enderman.teleport` pitch thấp | 0,4s | P1 |
| Tối Thượng Ma Thần Giáng Thế | Biến hình: sừng + cánh tối, mắt đỏ rực, mặt đất nứt tím quanh chân; Ác Tính +15 hiện chớp đỏ HUD | Tiếng gầm vang + nhạc nền tối | 12s | P2 |

### 4.3 Quái và boss — bộ animation

Quy tắc chung: model Blockbench + GeckoLib (`.geo.json` + `.animation.json`); texture 64×64/128×128 (boss lớn 256).

| Thực thể | Animation set | Ghi chú | Ưu tiên |
|----------|---------------|---------|---------|
| Hư Hóa (quái thường, vài biến thể) | `idle`, `walk`, `attack`, `hurt`, `death`, `spawn` (chui từ vết nứt) | Dùng chung skeleton cho mọi biến thể; đổi texture/phụ kiện | P1 |
| Hư Hóa tinh nhuệ/Sứ Đồ | + `skill` (1–2), `enrage` | Sứ Đồ mặc áo choàng tím Dạ Khuyết | P1 |
| Chấp Hành Giả I (Thạch Hống) | `idle`, `walk`, `attack_fist`, `skill_quake`, `skill_roar`, `phase2`, `death` | Phase 2: đá vỡ lộ lõi tím | P1–P2 |
| Chấp Hành Giả II (Bạch Từ Âm) | + `skill_beam`, `skill_summon`, `fly` | Cánh đen trắng, hào quang vỡ | P2 |
| Chấp Hành Giả III (Lăng Uyên) | + `skill_sword_rain`, `dash`, `phase2/3` | Kiếm bay bao quanh | P2 |
| Chấp Hành Giả IV (Cơ Vô Tận) | + `skill_blood`, `skill_domain`, `phase2/3/4` | Mô phỏng Ma, nhiều pha, hiệu ứng mạnh nhất | P2 |
| Nguyên Thủy Tịch Diệt (boss cuối) | `idle`, `walk`, `attack`, `skill_a/b/c`, `phase1→phase2→phase3`, `death` | Model khổng lồ; có thể dùng nhiều mô-đun nhỏ ghép | P2–P3 |

Mỗi boss: **bossbar**, **phase transition** (animation riêng 2–3s, miễn thương), âm báo pha; telegraph (vùng đỏ dưới đất 1s trước đòn lớn) để người chơi đọc được.

### 4.4 Phá cảnh, Lôi Kiếp, chọn Lõi

| Sự kiện | Hiệu ứng | Thời lượng | Ưu tiên |
|---------|----------|-----------|---------|
| Đột phá (phá cảnh) | Trụ sáng lên trời theo màu đạo, vòng sóng lan, tiếng chuông, Title tên cảnh giới; mốc lớn (cấp 50/90/99) thêm vòng thứ hai | 4s | P0 (title+particle) → P1 |
| Lôi Kiếp (Khảo Hạch thử thách 2) | Mây đen tụ lại, tia sét có cảnh báo vòng đỏ trước 1s, màn hình tối + chớp; HUD đếm số đợt | 60–180s | P2 |
| Tâm Ma (thử thách 1) | Bóng đen hình bản thân, overlay xám, vi-nhạc u ám | tùy | P2 |
| Chọn Lõi (xem §2.3) | Thẻ lật, ánh sáng dồn; Lõi được chọn bay vào ngực, vỡ thành hạt sáng theo màu phẩm; Hệ đủ 2/4/6 hiện vòng sáng | 2–3s | P0 (đơn giản) → P1 |
| Tẩu Hỏa Nhập Ma | Màn hình đỏ đen, tiếng tim đập, người ngã xuống kèm khói tím; sau đó màn hình **"Phế Tu - Làm Lại"** (xác nhận, giữ túi đồ, mở lại tạo nhân vật) | 3s | P2 |

**UI cảnh báo Tẩu Hỏa 3 tầng (R23)** — mỗi tầng đủ 3 kênh âm thanh + màn hình + chat:

| Tầng | Ngưỡng Ác Tính | Âm thanh | Màn hình | Chat |
|------|----------------|----------|----------|------|
| 1 | ≥ 80 | Tim đập chậm | Viền đỏ nhẹ, thanh Ác Tính rung | "Ma khí dâng, Tâm Ma bất ổn" |
| 2 | ≥ 90 | Tim đập nhanh + chuông trầm | Viền đỏ đậm + title cảnh báo | "Sắp Tẩu Hỏa, dừng hấp thu hoặc thiền" |
| 3 | ≥ 95 | Còi liên tục | Màn hình đỏ đen + đếm ngược 8s | "Tẩu Hỏa sau N giây: phế tu vĩnh viễn" |

Tầng 3 có hộp xác nhận không thể bỏ qua bằng click nhầm (cần giữ phím). Tùy chọn "Tắt overlay toàn màn hình" (R26) chỉ tắt phần hình, vẫn giữ âm + chat + vạch đếm ngược nhỏ trên HUD.

---

## 5. Pipeline tài nguyên

### 5.1 Công cụ
| Mục | Công cụ |
|-----|---------|
| Model 3D (quái/boss/vật thể/cánh) | AI viết trực tiếp `.geo.json` + `.animation.json` (khối hộp, GeckoLib); có thể mở/chỉnh sau bằng Blockbench |
| Texture, icon, sprite UI | **Script Python + Pillow** sinh pixel art (palette cố định, seed) → `.png`; có thể thay bằng bản người vẽ |
| Particle | JSON `particles/*.json` + `ParticleProvider`; sprite 8×8/16×16 |
| Âm thanh | **Tổng hợp bằng script** (numpy/wave: sóng sin/vuông/noise + envelope, chuông, tim đập) → ffmpeg/oggenc xuất `.ogg` mono 44.1kHz |
| Phông bitmap | Pixel font giấy phép mở có dấu tiếng Việt (R26) → `font/*.json`; hoặc glyph sinh bằng Pillow |

### 5.2 Quy ước tên file/thư mục (`src/main/resources/assets/hukhong/`)
```
geo/<entity|block|item>/<ten>.geo.json
animations/<entity|block|item>/<ten>.animation.json
textures/entity/<ten>.png        textures/item/..   textures/gui/<man>/..
textures/gui/frame/{ngoc_gian,cuon_truc,dai_da}.png
textures/particle/<hieu_ung>_<n>.png
particles/<hieu_ung>.json
sounds/<nhom>/<ten>.ogg  +  sounds.json
lang/{vi_vn,en_us}.json
```
- Chữ thường, `snake_case`, không dấu, không khoảng trắng. Tiền tố: `kiem_`, `dao_`, `the_`, `ma_`, `hu_hoa_`, `chg_<so>_` (Chấp Hành Giả).
- Ví dụ: `entity/chg_1_thach_hong.geo.json`, `particles/kiem_tram_khi.json`, `sounds/skill/ma_huyet_trao.ogg`.
- Hằng ID trong code đặt ở một lớp `ModAssets` (một nguồn) để tránh gõ sai.

### 5.3 Ai làm gì (R28: AI tự làm tài nguyên, user không cần vẽ)

Pipeline: `tools/gen_assets.py` (Pillow) sinh texture pixel; model/animation/particle viết thẳng JSON; `tools/gen_sounds.py` tổng hợp âm → `.ogg`; chạy lại script là tái tạo toàn bộ tài nguyên (đồng bộ ID qua `ModAssets`). Bản người làm có thể ghi đè sau mà không đổi code.

| Việc | AI làm được | Người làm |
|------|-------------|-----------|
| Class đăng ký particle/sound/renderer, JSON particle đơn giản, JSON lang | Có, đầy đủ | Rà lại |
| Layout GUI (code), màn hình, HUD, mockup → code | Có | Chỉnh pixel, kiểm tra cảm giác |
| Model GeckoLib JSON đơn giản (hộp, kiếm, cánh phẳng) | Có (placeholder) | Làm lại bằng Blockbench cho bản đẹp |
| Texture pixel (script Pillow) | Có, bản dùng được (R28) | Tùy chọn: vẽ lại cho đẹp hơn |
| Animation set chi tiết | Khung/keyframe cơ bản | Hoàn thiện nhịp, cảm giác |
| Âm thanh | Có: tổng hợp bằng script → `.ogg` (R28) | Tùy chọn: thay file chất lượng cao |
| Kiểm thử chạy thật (cảm giác, hiệu năng) | Không | Có |

### 5.4 Tiêu chí "đủ dùng" cho P0
- Mọi kỹ năng P0 có **ít nhất 1 particle + 1 âm** phân biệt được (không im lặng).
- Màn hình P0 dùng được bằng chuột/phím, không tràn khung ở scale GUI 2–4, vi/en đều hiển thị đúng.
- Placeholder được phép: texture màu phẳng + viền, model hộp, âm vanilla. **Không** chấp nhận: crash thiếu asset, texture "missing" tím-đen, chuỗi hiện khóa thô (`screen.hukhong...`).
- Kiểm bằng: chạy client + dedicated server, mở từng màn, dùng từng kỹ năng P0, không log lỗi `Missing`.

### 5.5 Hiệu năng
- **Particle**: ≤ 150 particle/hiệu ứng, ≤ 600 particle cùng lúc quanh người chơi; kỹ năng AoE dùng ít hạt to thay vì nhiều hạt nhỏ.
- **LOD**: > 32 block giảm 50% hạt, > 64 block chỉ hiển thị phần chính (không đuôi/khói); boss luôn hiển thị đầy đủ trong 128 block.
- **Entity GeckoLib**: giới hạn số entity phụ (kiếm bay) ≤ 8/người chơi; client bỏ render khi ngoài tầm.
- **Tùy chọn người chơi**: "Chất lượng hiệu ứng" (Cao/Vừa/Thấp/Tắt), "Tắt rung màn hình", "Tắt hiệu ứng toàn màn hình" (R26: client, áp cả khi chơi server; không đổi gameplay), "Giảm hiệu ứng người khác" (multiplayer).
- **Mạng**: không đồng bộ từng hạt; chỉ gửi **sự kiện** (id hiệu ứng + vị trí + màu), client tự sinh hạt.
- Kiểm: đo FPS ở 10 người chơi dùng Tối Thượng cùng lúc; mục tiêu ≥ 40 FPS máy trung bình.

---

## 6. Phân bổ theo phase

| Phase | UI/UX | Animation/VFX | Pipeline |
|-------|-------|---------------|----------|
| **P0** | Tạo nhân vật, HUD (EXP, tài nguyên, 4 ô kỹ năng, cooldown), Ấn Hồn Đài 4 ô (lật thẻ đơn giản), Bảng tu sĩ (chữ), toast cơ bản; vi/en lang file | Particle + âm vanilla cho Phím 1 4 đạo; đột phá (title + trụ sáng); chọn Lõi đơn giản | Cấu trúc thư mục, `ModAssets`, placeholder, tùy chọn tắt hiệu ứng |
| **P1** | Đan Lô minigame, Tông môn, Thành/chức vị, font pixel thư pháp, Tutorial theo R21, accessibility mù màu | Phím 2/3 các đạo; pose thiền; Ngự Kiếm, cánh thiên sứ, Bá Khí, Ma khí; quái Hư Hóa (GeckoLib) | Model Blockbench đầu tiên, LOD, texture thật |
| **P2** | Văn Kiện/nhật ký, thông báo sự kiện đầy đủ, bản đồ thành, Bia Công Trạng | Kỹ năng Tối Thượng 4 đạo; Chấp Hành Giả I–IV; Lôi Kiếp, Tâm Ma, Tẩu Hỏa | Telegraph/phase boss, tối ưu multiplayer |
| **P3** | Đánh bóng, animation chuyển màn, tuỳ biến HUD, âm thanh/ nhạc nền | Boss cuối, Thần Lõi (Kim) hiệu ứng, Tầng Thế Giới II hiệu ứng riêng | Pass hiệu năng toàn bộ, thay placeholder còn sót |

---

## Câu hỏi mở
- Không còn (3 câu cũ đã trả lời: phím Q/E/R/T + G theo R25; tùy chọn tắt hiệu ứng toàn màn hình theo R26; font pixel giấy phép mở theo R26).

## Đề xuất đổi canon (KHÔNG tự sửa PLAN-v2)
- Không có. Mọi số liệu giữ nguyên S2/S3.

## Document Lineage
| Version | Document | Status |
|---------|----------|--------|
| v1 | S8-ui-animation.md | ✅ Current (Draft) |
