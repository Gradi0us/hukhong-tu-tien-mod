# S3 — Cảnh Giới, EXP, Hệ Lõi, Hồn Cốt, Kỹ Năng

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2.1 (sửa theo PLAN-v2 §14 R2–R18); trước đó v2 (W3): hiệu chỉnh EXP + Bình Cảnh (110 h / 500 h), nguồn EXP mới, Ấn Hồn Đài 4 ô (Phó Mặc Số Phận), Tẩy Hồn, 3 phím + 1 tối thượng, Khảo Hạch = Nguyên Thủy Tịch Diệt, phạt chết chốt B
**Status**: 🔄 v2.1 — đã áp phán quyết R1–R21 (PLAN-v2 §14); còn 1 câu mở (mục 9)
**Phụ thuộc**: PLAN-v2-tu-tien-mod-canon.md (§0.2, §1.1, §5, §10, §11) · S7 (tên đan chi tiết) · S6 (tuổi, tông môn)
**Quy ước dữ liệu**: mọi số liệu dưới đây là giá trị khởi điểm, nằm trong JSON datapack (`cores/*.json`, `exp_curve.json`, `gates.json`) để chỉnh không cần build lại. Số liệu chưa playtest (Fermi/mô hình, **không phải** kết quả đo).

---

## Thay đổi v2.1

- **R2**: thêm *Bù Tư Chất* (pity Bình Cảnh, §1.3.6) — D không vật liệu 826 → ≈633 h; tam năng 1.239 → ≈892 h; bảng 1.5 tính lại.
- **R3**: một cổng chống farm `KillRateGate` (>30 con cùng loài / 64 block / 10 phút → ×0,1) thay luật 32 block cũ (§1.6; mã ở S5 §gates).
- **R4**: EXP quái vanilla cấp 1–9 hệ số phẳng m = 1,5 (≈130 con/cấp) + nhiệm vụ nhập môn; từ cấp 10 về 1,5% (§1.6).
- **R6**: mốc nhẹ 91–95/96/99 dùng nhóm **Tiểu Phá Đan** (7 loại, miễn luật "thấp 2 bậc vô hiệu") thay vì mượn đan chính (§1.3.2).
- **R7**: Thăng Thần chỉ dùng Thông Thiên Thần Đan; Hỗn Độn Thanh Liên Tử là vật phẩm Tầng II (§7).
- **R8**: roll Ấn Hồn Đài / Phá Chấp theo seed lưu trước khi mở UI (§2.1, §2.6.4).
- **R23/R25**: Tẩu Hỏa Nhập Ma không còn là Căn Cơ Tổn Thương 3 tầng mà là phế tu vĩnh viễn/làm lại từ đầu (§8); phím kỹ năng mặc định đổi Z/X/C + R thành Q/E/R + T, G bật/tắt Thế Tu Luyện (§6.1).
- **R9**: Tẩy Hồn hạ chi phí (≈10–12 giờ ở Hắc/Hồng); không trừ Thọ, không mất EXP (§4.5).
- **R10**: tiền tệ duy nhất = Linh Thạch (Hạ/Trung/Thượng/Cực).
- **R12**: Ấn Hồn Đài có ở mọi thành/tông/Thánh Địa; cấp thành chỉ giới hạn trần phẩm Lõi (§2.1).
- **R22**: Linh Khí Đài — Thiền thêm chế độ Bế Quan (AFK ~0,1 E(L)/giờ, không qua Bình Cảnh); thêm nguồn EXP "Bế Quan" 2% và Fermi mốc giờ (§1.6–1.7).
- **R18**: Khảo Hạch cá nhân; Phong Ấn/Hấp Thụ là lựa chọn cá nhân ghi Bia Công Trạng; Tầng II = dimension mới; Hấp Thụ chỉ đổi ngoại hình (§7).

---

## Thay đổi v2

| # | Yêu cầu canon v2 | Thay đổi trong S3 | Mục |
|---|------------------|-------------------|-----|
| 1 | Pillar 2 + §1.1: tư chất B đủ vật liệu ≈ 110 h, không vật liệu ≈ 500 h | Giữ `E(L)=40·L^2.15`; thêm **Bình Cảnh**: EXP nhận **×0,25** + thanh Chướng = 1 chặng 10 cấp (tức cần EXP gấp **×4**); mốc nhẹ ×3. Kết quả B: **110 / ≈321 (một nửa) / ≈496 giờ** | 1.2 – 1.5 |
| 2 | Nguồn EXP: thêm sự kiện, tông môn, Sứ Đồ; spawner = 0 | Tỉ trọng mới (Sự kiện 10%, Tông môn 8%, Sứ Đồ 5%); spawner/trứng/lệnh = 0 EXP. **Sửa lỗi v1**: trần Thiền và Đan v1 cho phép vượt 1 cấp/giờ | 1.6 – 1.7 |
| 3 | Ấn Hồn Đài 4 ô | Thêm ô **Phó Mặc Số Phận**: bảng Δ phẩm theo tư chất, 1% Mắc Kẹt → nhiệm vụ **Phá Chấp**. **Bỏ ô Hỗn Loạn** (vai trò "ngẫu nhiên" chuyển sang ô Phó Mặc) | 2.1, 2.4, 2.6 |
| 4 | Tẩy Hồn đắt + vật phẩm hiếm | Chi phí mới + **Luân Hồi Hoa / Tẩy Hồn Thạch** (nguồn, tỉ lệ, pity) | 4.5 |
| 5 | 3 phím + 1 tối thượng | Q/E/R + T (R25; G = Thế Tu Luyện); bỏ phím V cũ; Hồn Cốt chủ động tranh slot | 5, 6 |
| 6 | Khảo Hạch | Thử thách 3 = **Nguyên Thủy Tịch Diệt**; **Thần Đan** bắt buộc cho Nghi thức Thăng Thần | 7 |
| 7 | Phạt chết | **Chốt B – Căn Cơ Tổn Thương**; bỏ A/C (giữ 1 dòng gamerule hardcore) | 8 |
| 8 | Câu hỏi + đề xuất canon | Còn 3 câu mở; đề xuất canon mới (các đề xuất v1 đã được canon v2 nhận) | 9 |

Ghi chú: (a) mục con của §1 đánh số lại (v1 §1.3 → §1.5, §1.4 → §1.6, §1.5 → §1.7); (b) canon §5 nói Điểm Vận gồm "cảnh giới" — S3 thể hiện qua **bảng xác suất gốc theo Lõi #** (2.2) nên không cộng thêm điểm; (c) ô Phó Mặc là ngoại lệ có chủ đích của "trần phẩm" (2.2).

---

## Key Findings

| # | Kết luận | Độ tin cậy |
|---|----------|-----------|
| 1 | `E(L) = 40 · L^2.15` giữ nguyên, tổng ≈ 24,9 triệu EXP. Tư chất B: **110 h** (đủ vật liệu) / **≈321 h** (một nửa) / **≈496 h** (không vật liệu) | TRUNG BÌNH (mô hình Fermi, chưa playtest) |
| 2 | **Hệ số Bình Cảnh = EXP nhận ×0,25, thanh Chướng = EXP của cả chặng 10 cấp vừa đi** → mỗi cổng thiếu vật phẩm tốn đúng 4 lần giờ của chặng đó (12–56 h/cổng). 9 cổng chính = 368 h; 7 mốc nhẹ ≈ 17,7 h | CAO (suy trực tiếp từ công thức) |
| 3 | Hành trình đủ vật liệu cần 9 đan/Linh Vật chính + 7 mốc nhẹ + 1 Thông Thiên Thần Đan = **17 vật phẩm**. Cổng đắt nhất = mốc 70/80/90 (56 h/cổng) → đan Thất–Cửu phẩm nên hiếm nhất (S7) | TRUNG BÌNH |
| 4 | Ô Phó Mặc có kỳ vọng Δ phẩm ≈ +0,10…+0,17 để bù việc **mù Lõi**; 1% Mắc Kẹt chỉ tốn ≈ 20 phút (không phạt vĩnh viễn) | TRUNG BÌNH |
| 5 | Tẩy Hồn (v2.1, R9) tốn ≈ 4–6 giờ (Bạch/Hoàng) → ≈ 10–12 giờ chơi tập trung (Hắc/Hồng) để gom vật phẩm, chưa kể Linh Thạch | THẤP – TRUNG BÌNH (giả định thời lượng Bí Cảnh 25 phút) |
| 6 | Sau **Bù Tư Chất** (R2): D không vật liệu ≈ **633 h**; song năng ≈ 670 h; tam năng ≈ **892 h** (thô chưa bù: 826 / 793 / 1.239). Không ai chết vì tuổi (R1) nên Thọ chỉ là debuff, không còn ràng buộc cứng | CAO |
| 7 | Phạt chết chốt B; không bao giờ rớt cấp/cảnh giới, không mất Lõi/Hồn Cốt | CAO (user đã chốt) |

---

## 1. Đường cong EXP (cấp 1→100) và Bình Cảnh

### 1.1 Công thức

```
E(L)  = round(40 · L^2.15)          // EXP cần để lên từ cấp L → L+1 (hệ số tư chất B, đơn năng) — GIỮ NGUYÊN v1
EXP_gain_thực = EXP_gốc × k_tư_chất // D 0.6 · C 0.8 · B 1.0 · A 1.15 · S 1.3 · SS 1.45 · SSS 1.6
Song năng: E × 1.6 · Tam năng: E × 2.5 (canon §3.1; áp cả lên thanh Chướng)
Trần cộng dồn bonus EXP ngoài tư chất (tông môn + đan + buff + sự kiện): tối đa +50%  // bảo vệ mô hình giờ chơi
```

Trung bình có trọng số theo tỉ lệ tư chất canon (8/24/28/22/13/4/1 %) ≈ **×1.02** → người chơi "điển hình" ≈ B.

### 1.2 Bảng mốc (E(L), EXP chặng, cổng vật liệu)

| Cấp đạt | Cảnh giới | E(L−1→L) | EXP chặng 10 cấp trước | Tích lũy từ cấp 1 | Cổng ở mốc (vật phẩm) |
|---------|-----------|---------:|-----------------------:|------------------:|-----------------------|
| 10 | Tu Hành Nhất Giả | 4.505 | 15.213 | 15.213 | Bình Cảnh: Phá Cảnh Đan [1] phẩm = Khai Mạch Đan / Thiên Địa Linh Vật cấp 1 |
| 20 | Tông Sư | 22.458 | 131.692 | 146.905 | [2] = Ngưng Nguyên Đan / cấp 2 |
| 30 | Đại Tông Sư | 55.746 | 394.531 | 541.436 | [3] = Thông Huyền Đan / cấp 3 |
| 40 | Vương Giả | 105.402 | 816.726 | 1.358.162 | [4] = Tụ Vương Đan / cấp 4 |
| 50 | Hoàng Giả | 172.180 | 1.406.906 | 2.765.068 | [5] = Hoàng Cực Đan / cấp 5 |
| 60 | Đế Giả | 256.680 | 2.171.706 | 4.936.774 | [6] = Đế Hoàn Đan / cấp 6 |
| 70 | Thánh Giả | 359.406 | 3.116.569 | 8.053.343 | [7] = Thánh Khai Đan / cấp 7 |
| 80 | Đấu Giả | 480.793 | 4.246.130 | 12.299.473 | [8] = Hóa Đấu Đan / cấp 8 |
| 90 | Đấu Kình I | 621.224 | 5.564.443 | 17.863.916 | [9] = Kình Thiên Đan / cấp 9 |
| 91–95, 96, 99 | Đấu Kình → Luyện Thần Giả → Bán Thần | 636k – 781k | (nằm trong chặng 90→100) | — | Bình Cảnh **nhẹ** (1.3.2) |
| 100 | Thần (sau Khảo Hạch) | 781.044 | 7.075.126 | 24.939.042 | Khảo Hạch + **Thông Thiên Thần Đan** (mục 7) |

Cấp 99→100 **không** nhận EXP thường: thanh EXP đầy ở 99 chỉ mở Khảo Hạch (mục 7).

### 1.3 Cơ chế Bình Cảnh

#### 1.3.1 Bình Cảnh chính (9 cổng: mốc 10, 20, …, 90)

```
Cấp 10k−1 (9, 19, …, 89): thanh EXP đầy 100%
        │
        ▼
 BÌNH CẢNH  (không lên cấp; thanh chính đóng băng ở 100%)
        │
        ├── Có vật phẩm đúng mốc ─────────► dùng → Chướng vỡ → lên cấp 10k ngay
        │
        └── Không có ─► tích thanh CHƯỚNG:
                          • EXP nhận trong Bình Cảnh × 0,25
                          • Kích thước Chướng = EXP của cả chặng 10 cấp vừa đi (cột "EXP chặng" bảng 1.2)
                          • Đầy Chướng → lên cấp 10k (≈ tốn ×4 giờ chặng đó)
```

- **Hệ số đã chọn**: `exp_gain_mult = 0,25`, `chuong_size = 1,0 × EXP chặng`. Hai tham số cùng cho **thời gian Chướng = 4 × giờ chặng** (xem 1.4).
- Mọi nguồn EXP đều chịu ×0,25 (kể cả Thiền, Đan EXP) — không có đường vòng.
- **Chuyển tiếp 50%**: nếu sau đó dùng vật phẩm, 50% EXP đã tích trong Chướng chuyển thành EXP thường của cấp mới (tối đa 1 thanh). Hiệu suất tích Chướng chỉ 12,5% nên không thể khai thác.
- **Theo S7 §2.A (tên đan lấy từ S7)**: phẩm **thấp hơn 1 bậc** giảm **40%** Chướng còn lại (không qua ngay); thấp hơn ≥ 2 bậc vô hiệu; phẩm **cao hơn dùng được** (thừa hiệu lực). Đan còn cho buff **Dư Âm** (+10%…+30% EXP 15 phút thật). Tu Vi Đan cộng vào Chướng chỉ ×0,25 (S7) — nhân thêm với ×0,25 của Bình Cảnh: coi là **đã gồm**, không nhân đôi (EXP nhận ×0,25 một lần).
- **Bù Tư Chất (R2)**: xem §1.3.6 — tư chất thấp / đa đạo nhận Phá Cảnh Đan pity khi Chướng đạt ngưỡng f.
- Vật phẩm **chỉ dùng được khi đang Bình Cảnh** (không "nạp trước", tránh phí).
- Sau Chướng: cấp 10k đạt → EXP khóa chờ **Ấn Hồn Đài** (mục 2) như cũ.

#### 1.3.2 Bình Cảnh nhẹ (7 mốc phụ: 91, 92, 93, 94, 95, 96, 99)

Mốc phụ L là thanh dẫn **vào** cấp L (tức thanh của cấp L−1; riêng 99 = thanh cấp 98).

| Mốc | Thanh liên quan | Vật phẩm phá | EXP thêm nếu thiếu (= 2·E) | Giờ thêm (B, tốc độ chặng 90→100) |
|-----|-----------------|--------------|---------------------------:|---------------------------------:|
| 91 | cấp 90 | Tiểu Kình Đan I | 1.272.658 | 2,34 |
| 92 | cấp 91 | Tiểu Kình Đan II | 1.303.254 | 2,39 |
| 93 | cấp 92 | Tiểu Kình Đan III | 1.334.240 | 2,45 |
| 94 | cấp 93 | Tiểu Kình Đan IV | 1.365.616 | 2,51 |
| 95 | cấp 94 | Tiểu Kình Đan V | 1.397.382 | 2,57 |
| 96 | cấp 95 | Tiểu Luyện Thần Đan | 1.429.538 | 2,63 |
| 99 | cấp 98 | Tiểu Bán Thần Đan | 1.528.362 | 2,81 |
| **Tổng** | | | **9.631.050** | **≈ 17,7** |

- Cơ chế: kích thước thanh của cấp đó **×3**; vạch 1/3 là điểm vào Bình Cảnh nhẹ. Đến vạch → dùng vật phẩm → qua ngay (bỏ phần 2/3 còn lại); không có → tích tiếp bằng EXP thường (**không** ×0,25).
- Không có Ấn Hồn Đài, không Lõi mới (canon §1.1).
- **R6**: mỗi mốc nhẹ dùng đúng 1 **Tiểu Phá Đan** riêng (7 loại, S7 §2.A2): gắn nhãn mốc nhẹ, **miễn** luật "thấp 2 bậc vô hiệu" (chỉ cần đúng mốc), nguồn quái Đấu Kình–Luyện Thần/bí cảnh (không cần chế Cửu phẩm 7 lần). Bind người nhận như Phá Cảnh Đan (R5).

#### 1.3.6 Bù Tư Chất — pity Bình Cảnh (R2)

Độ khó `h = hệ_số_đa_đạo / k_tư_chất` (B = 1; D = 1,67; C = 1,25; song năng = 1,6; tam năng = 2,5; tích khi kết hợp). Khi `h > 1`, mỗi Bình Cảnh (chính và nhẹ) có **ngưỡng pity f**: Chướng đạt `f × kích thước` → hệ thống ban **Cơ Duyên Phá Cảnh** = 1 Phá Cảnh Đan (hoặc Tiểu Phá Đan) **đúng phẩm cổng đó, ràng buộc người nhận** (R5), tự dùng ngay; phần Chướng còn lại bỏ.

| h | 1 | 1,25 (C) | 1,6 (song) | 1,67 (D) | 2,5 (tam) | > 2,5 |
|---|--:|---------:|-----------:|---------:|----------:|------:|
| f | 1,00 (không pity) | 0,90 | 0,80 | 0,70 | 0,64 | sàn 0,60 |

Tính: không VL = Đủ VL + f × Σ(Chướng + mốc nhẹ thô). D: 183 + 0,70 × 643 = **633 h**; tam năng: 275 + 0,64 × 964 = **892 h**. Dữ liệu `gates.json` → `pity_f_by_h`. Pity không áp khi có vật phẩm thật (đã dùng thì qua ngay); không cộng dồn với giảm 40% của phẩm thấp hơn 1 bậc (lấy lợi hơn).

#### 1.3.3 Bảng Chướng của 9 cổng chính

| Mốc | Chướng (EXP) | Giờ Chướng nếu thiếu vật phẩm (B) | Giờ chặng (đủ vật liệu) |
|-----|-------------:|----------------------------------:|------------------------:|
| 10 | 15.213 | 12 | 3 |
| 20 | 131.692 | 24 | 6 |
| 30 | 394.531 | 32 | 8 |
| 40 | 816.726 | 40 | 10 |
| 50 | 1.406.906 | 44 | 11 |
| 60 | 2.171.706 | 48 | 12 |
| 70 | 3.116.569 | 56 | 14 |
| 80 | 4.246.130 | 56 | 14 |
| 90 | 5.564.443 | 56 | 14 |
| **Tổng** | | **368** | **92** |

Gợi ý S7: mỗi vật phẩm mốc N "tiết kiệm" tới 12 h (mốc 10) … 56 h (mốc 70–90) — nên làm Nhất–Tam phẩm dễ kiếm, Thất–Cửu phẩm rất hiếm, để giá trị vật liệu tỉ lệ với cái nó tiết kiệm.

#### 1.3.4 Hiển thị UI

- **Thanh EXP chính**: khi đầy ở cấp 10k−1, thanh nứt, đổi màu xanh → xám-tím, biểu tượng xiềng xích; chữ "BÌNH CẢNH — cần Phá Cảnh Đan [N] phẩm hoặc Thiên Địa Linh Vật cấp N" (N theo mốc).
- **Thanh Chướng** (mỏng, ngay dưới): % tiến độ; số EXP bay hiện màu xám kèm "(×0,25)".
- **Tooltip**: "Ước tính còn ~X giờ nếu tiếp tục tu luyện (theo tốc độ 10 phút gần nhất)" — để người chơi tự cân nhắc đi săn vật liệu.
- **Có vật phẩm trong túi**: biểu tượng nhấp nháy cạnh thanh; chuột phải vật phẩm hoặc bấm nút "Phá Cảnh" trên HUD. Hiệu ứng khi phá: Chướng vỡ, chấn động nhẹ, sấm, thông báo cho người trong 48 block.
- **Cảnh báo sớm**: thanh cấp 10k−1 đạt 80% → thông báo "Bình Cảnh sắp tới — chuẩn bị [vật phẩm]".
- **Mốc nhẹ**: vạch 1/3 hiển thị sẵn trên thanh với biểu tượng xiềng nhỏ; tooltip ghi rõ "×3 EXP nếu không dùng vật phẩm".

#### 1.3.5 Dữ liệu (datapack `gates.json`)

```json
{
  "gates": [
    { "id": "binh_canh_10", "kind": "major", "bar_level": 9,
      "item": { "dan_pham": 1, "linh_vat_cap": 1 },
      "chuong": { "size_ref": "chang_exp", "size_mult": 1.0, "exp_gain_mult": 0.25, "carry_over": 0.5 } },
    { "id": "binh_canh_nhe_91", "kind": "light", "bar_level": 90,
      "bar_size_mult": 3.0, "checkpoint": 0.3333,
      "item": { "tieu_pha_dan": "tieu_kinh_dan_1" } }
  ],
  "partial_item": { "tiers_below": 1, "chuong_reduction": 0.4 },
  "kill_rate_gate": { "count": 30, "radius": 64, "window_s": 600, "floor_mult": 0.1 },
  "pity_f_by_h": { "1.0": 1.0, "1.25": 0.90, "1.6": 0.80, "1.67": 0.70, "2.5": 0.64, "floor": 0.60 }
}
```

### 1.4 Vì sao chọn ×0,25 + Chướng = 1 chặng (hiệu chỉnh)

```
Giờ Chướng_k = Chướng_k / (0,25 × EXP/giờ_k) = EXP_chặng_k / (0,25 × EXP_chặng_k / giờ_chặng_k) = 4 × giờ_chặng_k

Mục tiêu: Σ giờ (không vật liệu) ≈ 500
  = 110 (đủ vật liệu) + Σ Chướng chính (4 × 92 = 368) + Σ mốc nhẹ (≈ 17,7)
  = 495,7 ≈ 500  (sai lệch −0,9%)
```

- Chỉ một tham số "chặng" nên **mọi cổng cân đối** với chính chặng của nó (cổng mốc 10 không thể thành tường 50 h).
- Sanity check ngược: nếu chỉ nhân thanh cấp 10k−1 (thay vì cả chặng) thì cần hệ số ≈ ×30 mới đủ 368 h — quá vô lý với người chơi nhìn thanh EXP. Chọn "1 chặng ×0,25" dễ giải thích: *"muốn phá cảnh bằng sức thì phải tu lại gần như cả chặng, với hiệu quả một phần tư."*
- Rủi ro nhận thức: 56 h mài ở cấp 89 là rất dài. Giảm nhẹ bằng (1) tooltip giờ ước tính, (2) phẩm thấp hơn 1 bậc giảm 40%, (3) Linh Vật là đường song song với đan. Nếu playtest thấy quá nặng: hạ `exp_gain_mult` lên 0,33 (→ không vật liệu ≈ 410 h) mà **không** cần build lại.

### 1.5 Bảng giờ chơi từng cảnh giới (tư chất B, đơn năng)

**Giả định**: (1) 110 h "đủ vật liệu" **đã gồm** thời gian kiếm vật liệu qua chơi bình thường (boss, Bí Cảnh, nhiệm vụ tông môn, mua bằng Linh Thạch), không mài riêng; (2) "không vật liệu" = không dùng bất kỳ đan/Linh Vật phá cảnh nào (giờ thường chơi giữ nguyên); (3) "một nửa vật liệu" = có đủ cho mốc **20, 40, 60, 80** và mốc nhẹ **91–93**, thiếu mốc 10, 30, 50, 70, 90 và 94–96, 99 (thiếu đúng các cổng đắt → nặng hơn trung điểm 305 h); (4) Thần Đan + Khảo Hạch như nhau ở cả ba trường hợp.

| Chặng (giờ chơi khi ở cảnh giới) | Cấp | EXP chặng | EXP/giờ ngụ ý | Đủ VL (h) | Một nửa (h) | Không VL (h) | Tích lũy Đủ / Nửa / Không (h) |
|----------------------------------|-----|----------:|--------------:|----------:|------------:|-------------:|-------------------------------|
| Thường Dân | 1→10 | 15 k | 5 k | 3 | 15 | 15 | 3 / 15 / 15 |
| Tu Hành Nhất Giả | 10→20 | 132 k | 22 k | 6 | 6 | 30 | 9 / 21 / 45 |
| Tông Sư | 20→30 | 395 k | 49 k | 8 | 40 | 40 | 17 / 61 / 85 |
| Đại Tông Sư | 30→40 | 817 k | 82 k | 10 | 10 | 50 | 27 / 71 / 135 |
| Vương Giả | 40→50 | 1,41 M | 128 k | 11 | 55 | 55 | 38 / 126 / 190 |
| Hoàng Giả | 50→60 | 2,17 M | 181 k | 12 | 12 | 60 | 50 / 138 / 250 |
| Đế Giả | 60→70 | 3,12 M | 223 k | 14 | 70 | 70 | 64 / 208 / 320 |
| Thánh Giả | 70→80 | 4,25 M | 303 k | 14 | 14 | 70 | 78 / 222 / 390 |
| Đấu Giả | 80→90 | 5,56 M | 397 k | 14 | 70 | 70 | 92 / 292 / 460 |
| Đấu Kình → Luyện Thần → Bán Thần | 90→99 (đầy thanh 99) | 7,08 M | 544 k | 13 | 23,5 | 30,7 | 105 / 315,5 / 490,7 |
| Khảo Hạch + săn Thần Đan | 99→100 | — | — | 5 | 5 | 5 | **110 / ≈321 / ≈496** |

Chặng "Thường Dân 15 h không VL" là hệ quả của 9 cổng đầu: **S7 nên cho Nhất–Tam phẩm gần như miễn phí** để người mới không kẹt 12–32 h ở cấp 9/19/29.

Ghi chú tính: Chướng_k = 4 × giờ chặng_k (1.4); mốc nhẹ tính ở tốc độ 544 k EXP/giờ; "Một nửa" ở chặng 90→99 = 13 + 4 mốc nhẹ còn thiếu (94, 95, 96, 99 ≈ 10,5 h).

**Biên theo tư chất** (giờ ∝ 1/k_tư_chất, đơn năng; D/C/đa đạo đã áp **Bù Tư Chất** §1.3.6, mục tiêu R2: D ≤ ~650 h, tam năng ≤ ~900 h; hàng B–SSS không pity):

| Tư chất | k | Đủ VL | Một nửa | Không VL |
|---------|--:|------:|--------:|---------:|
| D (f 0,70) | 0,60 | 183 | 429 | **633** (thô 826) |
| C (f 0,90) | 0,80 | 138 | 375 | **572** (thô 620) |
| **B** | 1,00 | **110** | **321** | **496** |
| A | 1,15 | 96 | 279 | 431 |
| S | 1,30 | 85 | 247 | 381 |
| SS | 1,45 | 76 | 221 | 342 |
| SSS | 1,60 | 69 | 200 | 310 |
| Song năng (B, f 0,80) | ×1,6 | 176 | 446 | **670** (thô 793) |
| Tam năng (B, f 0,64) | ×2,5 | 275 | 612 | **892** (thô 1.239) |

**Điểm kiểm tra playtest** (B, đủ VL): cấp 30 ≈ 17 h · cấp 50 ≈ 38 h · cấp 70 ≈ 64 h · cấp 90 ≈ 92 h · cấp 99 ≈ 105 h. Nếu lệch > ±25% → chỉnh `EXP/giờ` (nguồn EXP) trước, KHÔNG chỉnh E(L).

### 1.6 Nguồn EXP và tỉ trọng (v2)

| Nguồn | Tỉ trọng | Công thức / quy tắc |
|-------|---------:|---------------------|
| Quái Hư Hóa / Hư Tu / Hư Không (thường, **không spawner**; tỉ trọng chuẩn cho S4, R4) | 24% (R22: −2 nhường Bế Quan) | `E(L)/200` mỗi con cùng cấp (≈ 52 con/cấp); tinh anh ×5 |
| **Sứ Đồ Dạ Khuyết** (cấp ≤ 65) | 5% (≈ 15% mỗi cấp trong dải 20–65, 0 ngoài dải) | `E(L)/40` mỗi Sứ Đồ cùng cấp; tinh anh ×3, cấp bậc cao ×8; target thấp hơn người chơi ≥ 10 cấp ×0,1; sau cấp 65 giảm 50% mỗi 5 cấp. Rơi Văn Kiện theo canon §9 |
| Nhiệm vụ chính + phụ | 14% | Chính ≈ 0,3–0,6 E(L); phụ ≈ 0,05–0,15 E(L) |
| **Nhiệm vụ Tông Môn** | 8% | ≈ 0,04–0,12 E(L) mỗi nhiệm vụ; tối đa 3 nhiệm vụ/ngày game + 1 nhiệm vụ tuần ≈ 0,3 E(L); buff tông môn tính trong trần +50% (1.1) |
| **Sự kiện ngẫu nhiên / thế giới** | 10% | Nhỏ (cơ duyên, đoàn buôn, tinh anh lang thang) ≈ 0,03–0,08 E(L); lớn (tập kích thành thắng, đợt Xâm Thực) ≈ 0,15–0,4 E(L); sự kiện Chấp Hành Giả: tính ở dòng Boss |
| Boss (khu vực, thế giới, Chấp Hành Giả, tập kích) | 12% | Khu vực ≈ 0,3–0,6 E(L); thế giới ≈ 2–3 E(L). Tổng EXP boss/sự kiện × `(1 + 0,75·(n−1))` (khớp HP, canon §10), **chia = 50% đều + 50% theo đóng góp** (sát thương + hồi + buff — Đạo tu hỗ trợ được tính) |
| Bí Cảnh | 12% | Tầng cuối ≈ 0,8–1,5 E(L); tỉ lệ mức nguy hiểm đã chọn |
| Thiền / tu luyện tại Thánh Địa | 6% | Xem 1.7 |
| **Bế Quan** (AFK tại Linh Khí Đài, R22) | 2% (tỉ trọng nhỏ; người chơi 110/500 giờ chỉ dùng phụ, nguồn chính vẫn là chơi chủ động) | ≈ 0,1 E(L)/giờ × hệ số bậc × `k_tư_chất`; chặn ở Bình Cảnh; xem 1.7 |
| Đan dược (Tăng Tu Vi) | 4% | ≈ 0,02–0,06 E(L) mỗi viên (S7 gán theo phẩm); cooldown 10 phút; Đan Độc tích làm giảm hiệu quả |
| Khám phá / ngộ đạo (lần đầu vào khu mới, cơ duyên) | 2% | Một lần/đối tượng |
| Quái vanilla (spawn tự nhiên) | 1% | `1,5% × E(L)/200` ≈ gần 0 — chỉ để không "chết đói" ở cấp 1–5 |

Ghi chú (R4): ở cấp 1–9 (chưa có Hư Hóa ở khu khởi đầu) quái vanilla nhận **hệ số phẳng m = 1,5** (`m × E(L)/200` mỗi con ≈ **130 con/cấp**); phần EXP còn lại do chuỗi **nhiệm vụ nhập môn** gánh (tỉ trọng nhiệm vụ cấp 1–9 nâng tạm). Từ cấp 10 m về 0,015 (1,5%).

**Spawner = 0 (canon §1.1)**:
- Quái sinh từ spawner (vanilla `SPAWNER`/`TRIAL_SPAWNER` và spawner của mod) bị gắn tag `hukhong:spawner_born` lúc `FinalizeSpawn` → khi chết: **0 EXP tu luyện, 0 Hồn Tinh, 0 vật liệu tu tiên** (loot table của mod kiểm tra tag). Loot vanilla giữ nguyên. *(S5 xác nhận API đọc `MobSpawnType`.)*
- Quái từ **trứng spawn / lệnh** cũng 0 EXP.
- **`KillRateGate` (R3)**: >30 con cùng loài hạ trong bán kính 64 block / 10 phút → mọi thưởng (EXP, Hồn Tinh, vật liệu tu tiên) giảm dần đến ×0,1; áp **mọi đạo** (Ma tu dùng chung, chỉ khác Ác Danh); khai báo trong `gates.json` (`kill_rate_gate`), mã ở S5.

### 1.7 Thiền

Thiền tại Thánh Địa: bar "Linh Khí ngày" = **0,03 × E(L) mỗi ngày game**; hết thì EXP thiền ×0,1. Ngoài Thánh Địa không có EXP thiền (chống AFK). Tất cả nguồn EXP đều nhân `k_tư_chất`.

**Linh Khí Đài (R22)** — mỗi tông 1 đài, Thánh Địa có đài mạnh hơn; chỉ đệ tử tông dùng (Thánh Địa: mọi người theo luật S6), số chỗ có hạn (S6 §2.7). Hai chế độ:

| Chế độ | Tốc độ | Quy tắc |
|--------|--------|---------|
| **Tu luyện chủ động** | = Thiền hiện tại (bar Linh Khí ngày 0,03 E(L)/ngày game; hết thì ×0,1) | Phải đang chơi, ngồi tại đài; hệ số bậc nhân **vào trần bar ngày** |
| **Bế Quan (AFK)** | ≈ 10% tốc độ chơi thường ≈ **0,1 E(L)/giờ** (≈ 10 giờ/cấp) × hệ số bậc | **Không qua Bình Cảnh** (dừng ở cấp 10k−1, thanh đóng băng); không cộng thưởng quái/farm; tốn Cống Hiến hoặc Linh Thạch mỗi giờ; làm tuổi trôi (S6 §1.2); dừng khi rời đài hoặc bị đánh |

Hệ số theo bậc đệ tử: **Ngoại ×1 · Nội ×1,5 · Chân Truyền ×2 · Trưởng Lão ×2**. Đài Thánh Địa mạnh hơn đài tông: Bế Quan **×1,5** so với đài tông (cùng bậc), tuổi trôi ×0,5 (luật Thánh Địa S6 §1.2) nhưng chỗ rất ít và phí cao. Mọi EXP Bế Quan cũng nhân `k_tư_chất` và đi qua `RewardGate`.

**Fermi — người CHỈ Bế Quan** (Ngoại ×1, tư chất B, k = 1, đài tông): mỗi cấp ≈ 10 giờ (E(L)/0,1E(L)) ⇒ tới cổng Bình Cảnh gần nhất: cấp 9 ≈ 80 h · 19 ≈ 180 h · 29 ≈ 280 h · 49 ≈ 480 h · 69 ≈ 680 h · 89 ≈ 880 h · 99 ≈ 980 h (rồi dừng, cần đan/Chướng chủ động; cả hành trình > 1.000 giờ). Chân Truyền/Trưởng Lão ×2: cấp 49 ≈ 240 h, cấp 99 ≈ 490 h; Nội ×1,5: cấp 99 ≈ 653 h. Sanity: ngay cả bậc ×2 (≈ 490 h tới 99) vẫn **không nhanh hơn** đường chơi chủ động 110 h (đủ VL) và chỉ bằng đường chậm 500 h ⇒ Bế Quan là "đường lười + tốn tài nguyên + tốn tuổi", không phá mô hình 110/500 giờ. Nút thắt còn lại: mỗi cổng Bình Cảnh vẫn cần vật liệu/Chướng (S3 §1.3).

> **Sửa lỗi v1**: v1 đặt Linh Khí ngày = 0,5 E(L) mỗi ngày game (= 20 phút thực) → thiền 1 giờ ≈ 1,5 E(L), nhanh hơn mức lên 1 cấp/giờ trung bình và phá vỡ tỉ trọng 7%. Mức mới tối đa ≈ 0,09 E(L)/giờ thực. Tương tự Đan EXP v1 (0,2–0,5 E(L)/viên, CD 10 phút) có thể vượt 1 cấp/giờ → hạ còn 0,02–0,06 E(L).

---

## 2. Hệ Lõi — nghi thức và xác suất

### 2.1 Nghi thức Ấn Hồn Đài (4 ô)

```
Cấp 10/20/.../90 đạt (sau khi qua Bình Cảnh, mục 1.3) → thanh EXP khóa (dư tối đa 1 cấp)
   ↓ đến Ấn Hồn Đài (thành/tông môn/thánh địa)
1. Đặt Hồn Tinh (0–3 viên, tùy chọn) vào 3 hốc
2. Hấp thụ 3 giây (hiệu ứng hồn hoàn trồi theo màu phẩm — xuất hiện dưới chân, đúng cảm giác Đấu La)
3. Roll PHẨM P (bảng 2.2) → 3 ô THƯỜNG: 3 Lõi cùng phẩm P (quy tắc 2.4)
                           + ô thứ 4 "PHÓ MẶC SỐ PHẬN" (úp): Lõi ẩn + phẩm ẩn (mục 2.6)
4. Chọn 1 trong 4 (hoặc reroll 3 ô thường bằng Thiên Cơ Phù)
5. Hồn hoàn (vòng sáng màu phẩm) hiện vĩnh viễn quanh chân → mở khóa EXP
   (riêng chọn Phó Mặc: 1% rơi vào Mắc Kẹt → nhiệm vụ Phá Chấp, mục 2.6.4)
```

Lõi đã chọn là **vĩnh viễn**. Đổi Lõi chỉ qua "Tẩy Hồn" (mục 4.5).

- **Seed (R8)**: mọi roll (phẩm P, 3 ô thường, ô Phó Mặc kể cả Δ và Mắc Kẹt) sinh bằng **`rollSeed` lưu vào dữ liệu người chơi TRƯỚC khi mở UI**. Thoát game / relog / đóng UI mở lại đều ra đúng kết quả đã roll; chỉ Thiên Cơ Phù (có đếm) mới đẩy seed sang bước kế.
- **Địa điểm (R12)**: Ấn Hồn Đài có ở **mọi thành, tông môn, Thánh Địa**. Cấp thành chỉ giới hạn **trần phẩm Lõi**: thành Nhân ≤ Hắc · thành Địa ≤ Hồng · thành Thiên = tất cả (kể cả Kim ở Thánh Địa). Phẩm roll vượt trần được kẹp xuống trần (ô Phó Mặc cũng kẹp). Khảo Hạch Thăng Thần luôn ở Thiên Kiếp Đài (Thánh Địa).

### 2.2 Phẩm và trần theo cảnh giới

| Phẩm | Màu | Niên hạn (canon) | Hệ số sức mạnh (giá trị Lõi) |
|------|-----|-----------------|---------------------------:|
| Bạch | trắng | 10 | ×1,0 |
| Hoàng | vàng | 100 | ×1,6 |
| Tử | tím | 1.000 | ×2,5 |
| Hắc | đen | 10.000 | ×4,0 |
| Hồng | đỏ | 100.000 | ×6,0 |
| Kim | vàng kim | Thần Lõi | cố định theo đạo |

**Trần phẩm theo Lõi** (3 ô thường không roll vượt trần, giống giới hạn niên hạn hồn hoàn; **ngoại lệ**: ô Phó Mặc vượt tối đa +1 phẩm, mục 2.6):

| Lõi # (cấp) | Trần phẩm | Xác suất gốc (tư chất B, 0 Hồn Tinh) |
|-------------|-----------|---------------------------------------|
| #1 (10) | Hoàng | Bạch 85 · Hoàng 15 |
| #2 (20) | Hoàng | Bạch 60 · Hoàng 40 |
| #3 (30) | Tử | Bạch 25 · Hoàng 55 · Tử 20 |
| #4 (40) | Tử | Bạch 10 · Hoàng 45 · Tử 45 |
| #5 (50) | Hắc | Hoàng 20 · Tử 45 · Hắc 35 |
| #6 (60) | Hắc | Hoàng 10 · Tử 40 · Hắc 50 |
| #7 (70) | Hồng | Tử 30 · Hắc 50 · Hồng 20 |
| #8 (80) | Hồng | Tử 15 · Hắc 50 · Hồng 35 |
| #9 (90) | Hồng | Tử 5 · Hắc 40 · Hồng 55 |
| #10 (100) | Kim | 100% Kim (Thần Lõi) |

### 2.3 Điểm Vận: tư chất + Hồn Tinh

`Điểm Vận = bonus tư chất + tổng bonus Hồn Tinh` (tối đa +60).
Mỗi **1 điểm** chuyển 1% xác suất từ phẩm thấp nhất đang >0 lên **phẩm trần** của Lõi đó.

| Tư chất | D | C | B | A | S | SS | SSS |
|---------|--:|--:|--:|--:|--:|---:|----:|
| Điểm Vận | −6 | −3 | 0 | +3 | +6 | +10 | +15 |

(Điểm âm chuyển ngược xuống phẩm thấp nhất.)

| Hồn Tinh phẩm | Bạch | Hoàng | Tử | Hắc | Hồng |
|---------------|-----:|------:|---:|----:|-----:|
| Điểm Vận | +2 | +5 | +10 | +18 | +30 |

Hồn Tinh rơi từ quái Hư Hóa tinh anh (Bạch/Hoàng), boss khu vực (Tử/Hắc), boss thế giới/Bí Cảnh hiếm (Hồng). Có thể ghép 3 Hồn Tinh cùng phẩm → 1 phẩm trên (tỉ lệ 100%, tiện cho người chơi nhiều quái thường).

**Ví dụ #5 (trần Hắc)**: gốc 0/20/45/35 → tư chất A + Hồn Tinh Hắc (+21): Hoàng 0 · Tử 44 · Hắc 56. Tư chất SSS + Hồn Tinh Hồng (+45): 0/0/0/100 (đã chạm trần). Kỳ vọng Lõi #5 cho người B tay không: Hắc 35%.

**Bảo hiểm (pity)**: sau 2 lần liên tiếp nhận phẩm dưới "trung vị" của slot đó, lần Ấn Hồn kế bảo đảm ≥ phẩm giữa (chống ức chế).

### 2.4 Quy tắc 3 ô thường

- Cả 3 **cùng phẩm P** (ARAM).
- Lọc theo đạo: cả 3 thuộc đạo người chơi hoặc "chung". **Ô Hỗn Loạn của v1 đã bỏ** — vai trò "ngẫu nhiên toàn pool" chuyển sang ô Phó Mặc (2.6).
- Ít nhất 2 Hệ khác nhau trong 3 lựa chọn; 1 trong 3 có xác suất 50% thuộc Hệ người chơi đã có ≥1 Lõi (gợi ý build set).
- Ma tu thấy thêm pool "Ma" (xem Lõi Ma trong mục 3.2); nhánh lai nhận pool đạo gốc tương ứng.
- Đạo tu: mọi sát thương Lõi tính là phép (canon: cận chiến lên quái = 0).

### 2.5 Reroll bằng Thiên Cơ Phù

| Mục | Quy tắc |
|-----|---------|
| Hiệu quả | Roll lại **3 ô thường**, **giữ nguyên phẩm P**. Ô Phó Mặc **không** bị reroll (Lõi và Δ giữ nguyên — "số phận") |
| Giới hạn | Tối đa 2 lần/lần Ấn Hồn |
| Nguồn | Boss khu vực (hiếm), nhiệm vụ tông môn, chợ đổi Cống Hiến Điểm (giá tăng) |
| Biến thể | **Thiên Cơ Phù Kim** (cực hiếm, 1 lần/đời nhân vật theo nguồn quest): reroll cả phẩm P; phẩm cuối của Phó Mặc tính lại từ Δ đã giữ |

### 2.6 Ô thứ 4 — "Phó Mặc Số Phận"

#### 2.6.1 Bản chất
Ô úp (khói đen, dấu "?"). Chọn nó = **không biết trước** cả Lõi lẫn phẩm; lộ sau 2 giây khi chọn. Phẩm cuối = `clamp(P + Δ)`, với P là phẩm của 3 ô thường lần này và Δ roll theo bảng 2.6.3.

#### 2.6.2 Lõi lấy từ đâu
60% từ pool đạo + "chung"; 40% từ toàn pool hợp lệ (kể cả đạo khác, Hệ Hỗn Độn) miễn không vi phạm luật cứng (ví dụ Thể tu không nhận Lõi cần vũ khí; Lõi Ma chỉ Ma tu). Không áp quy tắc "gợi ý Hệ".

#### 2.6.3 Bảng xác suất Δ phẩm (so với 3 ô thường), theo tư chất

| Tư chất | Δ ≤ −2 | Δ = −1 | Δ = 0 (bằng) | Δ = +1 | Δ ≥ +2 | **Mắc Kẹt** | EV(Δ) |
|---------|-------:|-------:|-------------:|-------:|-------:|------------:|------:|
| D | 10% | 22% | 25% | 25% | 17% | 1% | +0,17 |
| C | 9% | 21% | 30% | 26% | 13% | 1% | +0,13 |
| B | 7% | 20% | 38% | 24% | 10% | 1% | +0,10 |
| A | 6% | 19% | 42% | 23% | 9% | 1% | +0,10 |
| S | 5% | 18% | 46% | 22% | 8% | 1% | +0,10 |
| SS | 5% | 17% | 48% | 21% | 8% | 1% | +0,10 |
| SSS | 4% | 16% | 50% | 21% | 8% | 1% | +0,13 |

(EV tính khi Δ ≤ −2 = −2 và Δ ≥ +2 = +2, chưa clamp. Tư chất thấp: biến động lớn hơn — "cơ hội đổi đời"; tư chất cao: ổn định quanh P.)

**Clamp**: phẩm cuối nằm trong [Bạch, min(Hồng, trần Lõi # + 1 phẩm)]. Đây là ngoại lệ có chủ đích của "trần phẩm" (2.2): ô Phó Mặc được **vượt trần tối đa 1 phẩm** (canon §5: "có thể cao hơn mức tư chất thường đạt"). Phần Δ dương vượt giới hạn Hồng đổi thành **+1 Tinh Luyện miễn phí mỗi bậc dư** (tối đa +3). Phía dưới, Δ âm tới sàn Bạch không mất thêm — nên ở Lõi #1–#2 (P thường là Bạch) ô Phó Mặc gần như chỉ có lợi phẩm, rủi ro nằm ở **Lõi mù** và 1% Mắc Kẹt.

**Ví dụ** — tư chất B, Lõi #5 (trần Hắc, P = Tử): Bạch 7% · Hoàng 20% · Tử 38% · Hắc 24% · Hồng 10% (vượt trần +1) · Mắc Kẹt 1% (tổng 100%).

#### 2.6.4 Mắc Kẹt — "Tâm Kiếp Bình Cảnh" và nhiệm vụ Phá Chấp
- Xảy ra khi **chọn** ô Phó Mặc, xác suất 1%. Ấn Hồn Đài đóng, **không nhận Lõi**; nhân vật kẹt ở cấp hiện tại (EXP vẫn đóng băng — vốn đã khóa chờ Lõi), chơi bình thường các hoạt động khác.
- Mở nhiệm vụ **Phá Chấp** (thử thách tâm cảnh, instance cá nhân; thứ tự Tam Chấp và mê cung roll bằng **seed lưu trước khi vào** — relog/thoát không reroll được, R8; ≈ 15–25 phút thực, bảo đảm **không kẹt vĩnh viễn**): 3 màn "Tam Chấp" thứ tự ngẫu nhiên:

| Màn | Nội dung | Điều kiện qua |
|-----|----------|---------------|
| Chấp Tham | Phòng báu vật vô hạn; nhặt càng nhiều Bóng Tham càng đông | Rời phòng sau khi nhặt ≤ 3 món |
| Chấp Sân | Bóng người thân/kẻ thù khiêu khích 90 giây; mỗi lần phản công làm đầy thanh Sân | Chỉ né/đỡ, thanh Sân không đầy |
| Chấp Si | Mê cung gương che mờ Lõi mong muốn | Chọn đúng lối theo manh mối trong 3 lần |

- Ma tu: Ác Tính tích gấp đôi trong màn Tham/Sân (đặc thù đạo). Chết hoặc thất bại: làm lại màn đó, **không** phạt chết (không Căn Cơ Tổn Thương).
- Thành công: thoát Kẹt, Ấn Hồn Đài mở lại 4 ô với **+10 Điểm Vận** (bù đắp) và **miễn Mắc Kẹt** ở mốc này.

#### 2.6.5 Cân bằng
- Mù Lõi là cái giá thật: kỳ vọng Δ ≈ +0,10…+0,17 phẩm chỉ bù cho việc mất quyền chọn. Mọi xác suất hiển thị trong tooltip ô Phó Mặc (minh bạch).
- Mắc Kẹt chi phí thực ≈ 20 phút (≈ 0,3% của 110 h) — cố ý nhẹ: canon muốn "rủi ro/kịch tính", không phạt tiến trình.

---

## 3. Các Hệ (Set) và Lõi mẫu

### 3.1 Bonus Hệ (2 / 4 / 6 món cùng Hệ)

| Hệ | 2 món | 4 món | 6 món |
|----|-------|-------|-------|
| **Liệt Diễm** | Thiêu Đốt +30% sát thương | Giết quái → nổ lửa AoE 3 ô (= 60% Công) | Thân Hỏa: lửa quanh người đốt kẻ cận chiến 5%/giây Công; miễn nhiễm cháy |
| **Huyết Ma** | Hấp Huyết +5% | Hồi máu thừa thành khiên (tối đa 30% HP) | Dưới 40% HP: +40% sát thương, +15% Hấp Huyết |
| **Tinh Thần** | Thần Thức +12% | Giảm hồi chiêu mọi chủ động 12% | Mỗi 5 lần dùng kỹ năng → lần sau miễn phí tài nguyên + ×1,5 sát thương |
| **Cương Thể** | Phòng +15% | Giảm sát thương nhận 10%, miễn choáng ngắn | Bá Khí/Thánh Lực không giảm ngoài combat 10s; phản 20% sát thương cận chiến |
| **Phong Lôi** | Tốc +10% | Đòn chí mạng xích sét nhảy 3 mục tiêu | Mỗi 8s Lôi Phong Hóa: 4s Tốc +40%, đòn kèm tia sét |
| **Thánh Quang** | Hồi máu nhận +15% | Aura 6 ô: đồng đội hồi 1%/giây; khắc quái Hư Hóa +20% | Đồng đội chết trong aura hồi sinh 1 lần/trận với 30% HP |
| **Hư Không Phản Phệ** | Tích Hư Nhiễm chậm hơn 25% | Sát thương +1%/điểm Hư Nhiễm (tối đa 30%) | Đỉnh Hư Nhiễm: Hư Không Bộc Phát (xóa Hư Nhiễm, AoE lớn, 60s CD) |
| **Hỗn Độn** | Mỗi 90s nhận 1 buff ngẫu nhiên nhỏ | Kết quả "xấu" của Lõi Hỗn Độn bị loại bỏ 50% | Mỗi 60s chọn 1 trong 3 phần thưởng ngẫu nhiên (mini-augment) |

Với 9 Lõi (không tính Thần Lõi), bonus 6 món đạt tối đa từ Lõi #6 trở đi — chỉ khi người chơi chuyên một Hệ. Thần Lõi tính là "joker": đếm +1 vào Hệ bất kỳ (người chơi chọn lúc nhận).

### 3.2 Bảng ≥30 Lõi mẫu (32 Lõi)

**Cách đọc số "a/b/c/d/e"** = giá trị theo phẩm Bạch/Hoàng/Tử/Hắc/Hồng. Công thức: giá trị đã nhân hệ số 2.2 và làm tròn; Tinh Luyện nhân thêm (mục 4). "Chủ" = chủ động (chiếm phím, mục 6). CD = cooldown.

**Hệ Liệt Diễm**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Hỏa Chủng | Chung | Đòn đánh/phép gây Thiêu Đốt 3 giây | — | dmg/giây 3/5/8/12/18 |
| Phượng Hoàng Niết Bàn | Chung | Chết → hồi sinh tại chỗ 1 lần, CD 10 phút | — | % HP hồi 20/30/40/50/60 |
| Diệm Kiếm Khí | Kiếm | Kiếm khí nhiễm lửa | Chủ: Hỏa Kiếm Phá (hình nón 6 ô, CD 12s) | % Công 120/180/260/380/550 |
| Dung Nham Quyền | Thể | Bá Khí đầy → quyền kế tiếp nổ | Chủ: Hỏa Sơn Quyền (đập đất, CD 14s) | % Công 130/190/280/410/600 |

**Hệ Huyết Ma**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Khát Huyết | Ma | Hấp Huyết thêm; giết quái tăng Ô Uế Lực | — | Hấp Huyết % 4/6/9/13/18 |
| Huyết Tế | Ma | — | Chủ: hy sinh 15% HP → +Công 8s, CD 25s | +Công % 30/45/70/100/150 |
| Huyết Đằng | Chung | Hồi thừa thành khiên | — | Khiên tối đa % HP 10/15/22/32/45 |
| Tâm Ma Phản Chiếu | Ma | Khi Tâm Ma đầy: thay vì mất kiểm soát → **Cuồng Hóa** 8s (vẫn kiểm soát được) | — | +sát thương % 20/30/45/65/90; sau Cuồng Hóa mệt 6s |

**Hệ Tinh Thần**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Tinh Hà Quán Đỉnh | Đạo | Hồi Thánh Lực nhanh hơn khi đứng yên | — | Hồi Linh % 15/24/38/60/90 |
| Thiên Cơ Diễn Toán | Chung | Mỗi 10 lần dùng kỹ năng → giảm 30% CD của kỹ năng kế | — | Giảm CD thêm % 5/8/12/18/25 |
| Tinh Thần Vẫn Lạc | Chung | — | Chủ: gọi sao rơi 5×5 ô sau 1,5s, CD 18s | % Thần Thức 150/230/340/500/750 |
| Nguyệt Ảnh Phân Thân | Kiếm | — | Chủ: tạo phân thân lặp lại chiêu 6s, CD 30s | % sát thương phân thân 40/55/75/100/140 |

**Hệ Cương Thể**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Kim Cương Bất Hoại | Thể | Phòng +, giảm sát thương nhận | — | Giảm dmg % 4/6/9/13/18 |
| Bá Khí Hộ Thể | Thể | Bá Khí ≥50% → khiên tự sinh | — | Khiên % HP 8/12/18/26/38 |
| Thiết Bì | Chung | Phản sát thương cận chiến | — | Phản % 8/12/18/26/38 |
| Đại Địa Chi Lực | Thể | — | Chủ: dậm chân choáng 1,5s, CD 20s | AoE dmg % Công 100/150/220/320/470 |

**Hệ Phong Lôi**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Lôi Ấn | Chung | 15% xác suất đòn gọi sét xích 2 mục tiêu | — | dmg sét % 40/60/90/130/190 |
| Ngự Phong Bộ | Kiếm | Tốc + khả năng né 1 đòn/20s | — | Tốc % 6/9/14/20/28 |
| Thiên Lôi Hàng Thế | Đạo | — | Chủ: sét cột lớn theo con trỏ, CD 16s | % Thần Thức 160/240/360/520/780 |
| Lôi Quyền | Thể | Quyền thứ 3 liên tiếp gây tê liệt 0,5s | — | dmg thêm % Công 30/45/70/100/150 |

**Hệ Thánh Quang**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Hộ Thánh Quang Vực | Đạo | Aura hồi máu 4 ô quanh người | — | Hồi %HP/giây 0,3/0,5/0,8/1,2/1,8 |
| Thiên Sứ Chi Vũ | Đạo | Cánh thiên sứ giảm tốn tài nguyên khi bay (dùng từ cấp 60) | Chủ: Phán Xét (nổ ánh sáng, CD 22s) | % Thần Thức 180/270/400/600/900 |
| Tịnh Hóa | Chung | Xóa debuff theo chu kỳ; sát thương lên quái Hư Hóa tăng | — | Khắc Hư % 8/12/18/26/38 |
| Thanh Tâm Chú | Chung | Tâm Ma tích chậm hơn | — | Giảm tích % 10/15/22/32/45 |

**Hệ Hư Không Phản Phệ**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Hư Không Hấp Thu | Chung | Giết quái Hư Hóa hồi tài nguyên nhưng tăng Hư Nhiễm | — | Hồi tài nguyên % 3/5/8/12/18 |
| Phản Phệ Chi Kiếm | Kiếm | Sát thương theo Hư Nhiễm hiện có | — | +dmg tối đa % 15/22/32/45/65 |
| Hư Không Phệ Hồn | Ma | Giết quái hút "hồn" → cộng Ô Uế Lực, dễ tích Tâm Ma | — | Ô Uế/hạ gục 4/6/9/13/19 |
| Vô Tướng | Chung | — | Chủ: tàng hình 4s, CD 28s | Thời gian s 3/3,5/4/5/6 |

**Hệ Hỗn Độn (kiểu ARAM, vui nhộn)**

| Lõi | Đạo | Bị động | Chủ động | Giá trị theo phẩm |
|-----|-----|---------|----------|-------------------|
| Cự Nhân Hóa | Chung | — | Chủ: phóng to ×1,5 trong 8s (hitbox cũng to!), CD 40s | +HP % 20/30/45/65/90; tầm đánh +1 |
| Biến Gà Phù | Chung | — | Chủ: biến kẻ địch thường thành gà 3s (không tác dụng boss), CD 30s | Thời gian s 2/3/4/5/6 |
| Phúc Họa Tương Y | Chung | Mỗi 60s: 1 buff ngẫu nhiên mạnh **và** 1 debuff nhẹ ngẫu nhiên | — | Buff/debuff %: 10/15/22/32/45 (debuff = 30% buff) |
| Hoán Vị Chú | Chung | — | Chủ: đổi chỗ với mục tiêu trong 15 ô (cả quái, đồng đội), CD 20s | Tầm ô 10/12/15/18/22 |

Cân bằng cho Lõi "điên rồ": mọi hiệu ứng vui đều có CD ≥ 20s hoặc cái giá nhỏ; không vô hiệu hóa boss; không hoán đổi vị trí ngoài vùng an toàn của Bí Cảnh. Các Lõi Ma (Khát Huyết, Huyết Tế, Tâm Ma Phản Chiếu, Hư Không Phệ Hồn) chỉ hiện trong pool Ma; Ma tu nhánh lai cũng thấy Lõi đạo gốc.

---

## 4. Tinh Luyện Lõi (+1 → +5) và Đá Tẩy Luyện

### 4.1 Hiệu quả

Mỗi cấp Tinh Luyện: **+6% mọi giá trị** (số trong bảng 3.2). Tại **+5**: nhận thêm "Viên Mãn" (hiệu ứng phụ nhỏ riêng của từng Lõi, ví dụ Hỏa Chủng: Thiêu Đốt lan sang kẻ gần). Trần Tinh Luyện theo phẩm: Bạch +3 · Hoàng +4 · Tử/Hắc/Hồng +5.

### 4.2 Vật liệu và xác suất

| Bước | Hồn Tinh (cùng phẩm Lõi) | Đá Tẩy Luyện | Linh Thạch (Hạ) | Thành công | Khi thất bại |
|------|-------------------------:|-------------:|------:|-----------:|--------------|
| +0→+1 | 1 | 0 | 100 | 100% | — |
| +1→+2 | 2 | 0 | 300 | 85% | Mất vật liệu, giữ +1 |
| +2→+3 | 4 | 1 | 800 | 65% | Mất vật liệu, giữ +2 |
| +3→+4 | 8 | 2 | 2.000 | 40% | Mất vật liệu, **rớt −1** (trừ khi dùng Đá Bảo Hộ) |
| +4→+5 | 16 | 3 | 5.000 | 20% | Mất vật liệu, rớt −1; 15% bị **Nứt** |

Tinh Luyện chỉ tại Ấn Hồn Đài hoặc Lò Luyện tông môn; thất bại 3 lần liên tiếp → lần sau +15% tỉ lệ (cộng dồn).

### 4.3 Rủi ro "Nứt"

Lõi Nứt: −20% mọi giá trị, không thể Tinh Luyện tiếp cho đến khi sửa bằng 1 Đá Tẩy Luyện + 1 Hồn Tinh cùng phẩm. **Không bao giờ xóa Lõi** (tránh mất hết tiến trình). Rủi ro nằm ở việc mất tài nguyên, không phải mất đồ vĩnh viễn.

### 4.4 Đá Tẩy Luyện (2 công dụng; Tẩy Hồn xem 4.5)

| Công dụng | Cách dùng |
|-----------|-----------|
| Tẩy dòng phụ | Mỗi Lõi có 1 "dòng phụ" ngẫu nhiên (+Tốc, +Hồi Linh, +Phòng… nhỏ). 1 đá = reroll dòng phụ. |
| Bảo hộ / sửa Nứt | 1 đá khi Tinh Luyện từ bước +3 trở lên → thất bại không rớt cấp; hoặc sửa Lõi Nứt |

Nguồn đá: nhiệm vụ tông môn, mảnh từ Bí Cảnh, chế từ khoáng Hư Không (hiếm).

### 4.5 Tẩy Hồn (đổi Lõi) — chi phí rất lớn

Tại Ấn Hồn Đài, đổi **1 Lõi** (1 slot). Roll lại **3 ô thường cùng phẩm hiện có** (không đổi phẩm, **không** có ô Phó Mặc, không Mắc Kẹt); Thiên Cơ Phù dùng được như 2.5.

| Phẩm Lõi hiện có | Linh Thạch (Hạ-quy đổi) | Hồn Tinh (cùng phẩm) | Tẩy Hồn Thạch | Luân Hồi Hoa |
|------------------|------------------------:|---------------------:|--------------:|-------------:|
| Bạch | 10.000 | 8 | 1 | 1 |
| Hoàng | 30.000 | 6 | 2 | 1 |
| Tử | 80.000 | 5 | 2 | 1 |
| Hắc | 200.000 | 4 | 3 | 1 |
| Hồng | 500.000 | 3 | 3 | 1 |

- Mất **100% Tinh Luyện** (về +0), xóa trạng thái Nứt. **Không trừ Thọ, không mất EXP** (R9). Slot vừa Tẩy bị khóa 3 ngày game.
- Lần Tẩy thứ n của cùng slot: Linh Thạch ×2^(n−1) (tối đa ×8). (So sánh: bước Tinh Luyện +4→+5 chỉ 5.000 Linh Thạch Hạ.)
- Đá Tẩy Luyện (4.4) **không** dùng cho Tẩy Hồn nữa.

**Nguồn vật phẩm hiếm** (loot chia theo người tham gia, mỗi người roll riêng — canon §10):

| Vật phẩm | Nguồn | Tỉ lệ / bảo hiểm (pity) |
|----------|-------|-------------------------|
| **Luân Hồi Hoa** | Bí Cảnh tầng cuối độ khó Cao (cấp ≥ 50) | 3% / lần · pity 40 lần |
| | Boss thế giới | 6% · pity 20 |
| | Chấp Hành Giả | 15% mỗi lần hạ · pity 7 |
| | Tông môn (Chân Truyền trở lên) | Đổi 1 hoa / 60 ngày game (≈ 20 giờ thực), giá bằng Điểm Cống Hiến (S6), **bind** |
| **Tẩy Hồn Thạch** | Tinh anh Hư Hóa cấp ≥ 40 | 0,15% |
| | Boss khu vực | 5% |
| | Bí Cảnh tầng cuối độ khó Cao | 12% |
| | Boss thế giới | 25% (1–2 viên) |
| | Ghép 5 Mảnh Thạch (rơi 10% ở mọi tầng Bí Cảnh cấp ≥ 40) | 100% |

**Ước lượng v2.1** (giả định 1 lượt Bí Cảnh cuối ≈ 25 phút; 1 Thạch ≈ 8,4 lượt, 1 Hoa ≈ 33 lượt, hai nguồn rơi song song): Lõi Hắc/Hồng cần 3 Thạch + 1 Hoa ≈ 26 lượt ≈ **10–12 giờ** kể cả Hồn Tinh + Linh Thạch; Bạch/Hoàng ≈ 4–6 giờ. Mục đích: đổi Lõi là quyết định lớn nhưng không còn là tường 20–30 giờ (R9).

---

## 5. Hồn Cốt (6 slot)

Slot: **Đầu · Thân · Tay Trái · Tay Phải · Chân Trái · Chân Phải** (slot tự làm, không dùng Curios — canon §12). Hồn Cốt là bị động/tự kích hoạt (không chiếm phím), không có phím riêng; Hồn Cốt chủ động duy nhất (#13) tranh 1 trong 3 phím kỹ năng (mục 6).

**Nguồn rơi**: boss khu vực (3–8%/lần), boss thế giới (15–25% + bảo hiểm), Bí Cảnh tầng cuối, chế tạo ghép mảnh. Mỗi slot rơi theo chủ đề: Đầu từ boss nhận thức (Hư Tu tướng), Thân từ boss thế giới, Tay từ boss cận chiến, Chân từ boss tốc độ. Phẩm Hồn Cốt: Tử / Hắc / Hồng theo niên hạn boss; trần mở khi tới cảnh giới 40 / 60 / 80.

| # | Hồn Cốt | Slot | Hiệu ứng (phẩm Hắc làm chuẩn) |
|---|---------|------|-------------------------------|
| 1 | Thiên Mục Cốt | Đầu | Thấy HP quái + tầm nhìn xuyên Hư Hóa; +8% Bạo Kích |
| 2 | Tĩnh Tâm Cốt | Đầu | Tâm Ma tích −20%; +10% Hồi Linh |
| 3 | Long Hồn Cốt | Thân | +15% HP; khi <30% HP: khiên 20% HP (CD 90s) |
| 4 | Hư Không Hạch Cốt | Thân | Hấp thụ 10% sát thương Hư Không thành tài nguyên |
| 5 | Lôi Chưởng Cốt | Tay Trái | Mỗi 10s đòn kế gọi sét 150% Công |
| 6 | Hộ Thể Cốt | Tay Trái | Chặn kỹ năng (Thể tu: tay không đỡ) giảm 25% sát thương nhận |
| 7 | Huyết Trảo Cốt | Tay Phải | +6% Hấp Huyết; đòn thứ 4 chảy máu 4s |
| 8 | Kiếm Ý Cốt | Tay Phải | Kiếm khí +25% tầm; hiệu quả lên Bản Mệnh Kiếm |
| 9 | Phong Hành Cốt | Chân Trái | +12% Tốc; bay/lướt tốn ít tài nguyên 15% |
| 10 | Cự Tượng Cốt | Chân Trái | Kháng đẩy lùi 100%; dậm chân choáng nhỏ |
| 11 | Ảnh Thiểm Cốt | Chân Phải | Lướt ngắn 6 ô (CD 14s), miễn sát thương 0,3s |
| 12 | Hồi Sinh Cốt | Chân Phải | Hồi 40% HP khi chết (CD 15 phút), không gồm Phượng Hoàng |
| 13 | Vạn Cốt Cộng Minh (chủ động, gán vào Q/E/R) | bất kỳ | Nổ năng lượng quanh người (CD 45s); mạnh hơn nếu đủ 6 Hồn Cốt |

---

## 6. Hệ thống kỹ năng (3 phím chủ động + 1 tối thượng)

### 6.1 Phím và nguồn kỹ năng

| Phím | Loại | Nguồn | CD điển hình |
|------|------|-------|--------------|
| **Q / E / R** (R25) | 3 kỹ năng chủ động | Kỹ năng đạo (mở theo cảnh giới) · chủ động của Lõi · Kỹ Năng Bản Mệnh (A+) · Vạn Cốt Cộng Minh (Hồn Cốt #13) | 8–22 s |
| **T** (R25) | Tối thượng (Ultimate) | Từ cấp 30: Tuyệt Kỹ của đạo; Thần Lõi nâng cấp T sau cấp 100 | 90–180 s |

Không còn phím V (v1) và G không còn là Hồn Cốt chủ động: **G = bật/tắt "Thế Tu Luyện"** (bật thì Q/E/R/T là kỹ năng, tắt thì Q/E về vanilla vứt đồ/túi đồ; R25) — đúng canon §5 "3 phím + 1 tối thượng".

Mỗi đạo có pool 8 kỹ năng đạo (mở dần ở các Lõi/mốc 10/20/…); người chơi **tự xếp** vào 3 phím. Lõi chủ động, kỹ năng đạo, Bản Mệnh và Vạn Cốt **tranh slot** → lựa chọn build.

**Lõi chủ động dư không phải Lõi chết**: Lõi chủ động không được gán phím vẫn tính Hệ (bonus 2/4/6), Tinh Luyện và phần bị động; chỉ chức năng chủ động bị khóa.

*Hoãn (AP2)*: bộ phím A/B đổi nhanh chỉ làm nếu playtest thấy 3 slot quá nghẹt; MVP chưa có.

### 6.2 Cooldown và tài nguyên

- CD cơ bản đặt theo từng kỹ năng; **giảm CD tối đa 40%** (50% sau Thần).
- Tài nguyên theo canon §2: Kiếm tu Linh Lực, Đạo tu Thánh Lực, Thể tu Bá Khí, Ma tu Ô Uế Lực.
- Thể tu: không kỹ năng nào dựa vũ khí; Đạo tu: chỉ phép; luật cứng canon giữ nguyên.

### 6.3 Kết hợp Lõi và kỹ năng (hệ thẻ — tag)

Mỗi kỹ năng/Lõi mang thẻ: `Hỏa · Lôi · Huyết · Quyền · Kiếm · Phép · Thánh · Hư`. Bị động Lõi có thể "bám" thẻ:

- Lôi Ấn + kỹ năng thẻ `Lôi` → sét xích từ kỹ năng tự động.
- Hỏa Chủng + Diệm Kiếm Khí → Thiêu Đốt áp dụng lên mọi mục tiêu bị kiếm khí trúng.
- Thiên Cơ Diễn Toán + Tinh Thần Vẫn Lạc (cùng Hệ Tinh Thần) → sao rơi hồi chiêu nhanh, kết hợp bonus 4 món thành combo.

Combo được **ghi vào datapack** (`synergies/*.json`) để thêm ví dụ mới mà không sửa code.

---

## 7. Khảo Hạch Thăng Thần (cấp 99 → 100)

**Điều kiện**: cấp 99, thanh EXP đầy (đã qua mốc nhẹ 99), 1 **Thiên Kiếp Lệnh** (chế từ 3 Mảnh Kiếp Lệnh rơi ở boss khu vực cuối/Bí Cảnh cao nhất), đứng ở **Thiên Kiếp Đài** (Thánh Địa), và **Thông Thiên Thần Đan** (tên theo S7 §2.A). Mỗi thử thách là instance riêng. **Ấn đã đạt được lưu vĩnh viễn** (đổi từ v1 "7 ngày": vì Thần Đan và Thiên Môn có thể mất thời gian kiếm).

**Thần Đan** (R7): Thăng Thần **chỉ** dùng **Thông Thiên Thần Đan**; luyện hóa khi **bắt đầu** Khảo Hạch; thua vẫn giữ "Thần Đan Dư Lực" cho lần sau (không mất viên). Hỗn Độn Thanh Liên Tử **không** thay Thần Đan — là vật phẩm Tầng Thế Giới II (S7).

**Cá nhân (R18)**: Khảo Hạch là trận **cá nhân** (1 dự thi + tối đa 3 hộ pháp). Kết cục Phong Ấn / Hấp Thụ là **lựa chọn của chính người thắng**, ghi vào **Bia Công Trạng**, không áp cho cả server.

**Ngân sách giờ** (đã tính trong 5 h bảng 1.5): thử thách 1 ≈ 0,4 h · thử thách 2 ≈ 0,3 h · thử thách 3 ≈ 1,2 h (gồm 1–2 lần thất bại) · chuẩn bị Thiên Kiếp Lệnh + săn Thần Đan ≈ 3 h.

### Thử thách 1 — Tâm Ma (thử thách tinh thần)

| Mục | Nội dung |
|-----|----------|
| Bối cảnh | Vào ảo cảnh; đối thủ là **Bóng Tâm Ma** dùng đúng Lõi + kỹ năng của người chơi (ở 80% sức mạnh) |
| Cơ chế | 3 pha: (1) đánh thường bóng; (2) ảo cảnh chọn lựa: NPC thân thuộc xuất hiện xin tha; Ma tu và đạo khác phải chọn theo cách đạo của mình; chọn "đúng đạo tâm" thì bóng yếu đi; (3) bóng nhân đôi nếu người chơi có Tâm Ma cao |
| Thắng | Hạ bóng; nhận **Đạo Tâm Ấn** |
| Gợi ý | Ma tu: tối ưu bằng Lõi Tâm Ma Phản Chiếu; thanh Tâm Ma đầy trong ảo cảnh → mất kiểm soát 3s (cảnh báo rõ) |

### Thử thách 2 — Lôi Kiếp (thử thách sinh tồn)

| Mục | Nội dung |
|-----|----------|
| Cơ chế | 9 đạo Lôi (9 sóng, ~6 phút); mỗi đạo có vòng cảnh báo 3s, sát thương 15–35% HP tối đa nếu trúng; sóng 7–9 kèm tiểu lôi hư ảnh |
| Né/chắn | Né (Kiếm/Ma), đỡ bằng Bá Khí (Thể), khiên Thánh Lực (Đạo); không dùng đan hồi máu quá 3 lần |
| Co-op | Tối đa 2 hộ pháp (đồng đội có thể chắn một phần sét, tăng độ khó 20% mỗi người) |
| Thắng | Sống đến sóng 9; nhận **Lôi Kiếp Ấn** |

### Thử thách 3 — Nguyên Thủy Tịch Diệt (thử thách chiến đấu = boss cuối, canon §9)

| Mục | Nội dung |
|-----|----------|
| Mở | Cần **Thiên Môn mở** = đủ 4 Mảnh Thiên Môn (từ 4 Chấp Hành Giả) → dẫn tới đấu trường **Hư Không Bản Nguyên**. Thử thách 1–2 làm trước được. *(Đề xuất canon #5)* |
| Boss | **Nguyên Thủy Tịch Diệt** (Chủ Hư Không), cấp danh nghĩa 100; học (adapt) loại sát thương người chơi dùng nhiều nhất (kháng +25%, tối đa 60%) |
| Người chơi | 1 người dự thi + tối đa 3 hộ pháp (cấp ≥ 90). `HP = HP_gốc × (1 + 0,75·(n−1))` (canon §10). Loot/EXP boss chia theo người tham gia; chỉ người dự thi nhận Ấn |
| Pha 1 | Phân thân Hư Không; phá 4 trụ — mỗi trụ tái hiện chiêu đặc trưng của một Chấp Hành Giả (Thể/Đạo/Kiếm/Ma). Bay được, trừ chiêu "Hư Không Trọng Lực" khóa bay 6s mỗi 30s |
| Pha 2 | Hấp thụ Hư Nhiễm của người chơi — Lõi Hệ Hư Không có thể đảo ngược thành lợi thế |
| Pha 3 | Bản thể thật; sàn dần sụp; "Tịch Diệt Giam Cầm" khóa bay hoàn toàn; sau 12 phút cuồng bạo (enrage) tăng dần |
| Thắng | Nhận **Tịch Diệt Ấn**; mở lựa chọn **cá nhân** Phong Ấn / Hấp Thụ (ghi Bia Công Trạng; **Tầng Thế Giới II = dimension mới**; Hấp Thụ chỉ để lại hiệu ứng Hư Hóa **ngoại hình**, không đổi chỉ số — R18) |

### Thần Lõi (Kim) theo đạo

Đủ 3 Ấn + Thần Đan → **Nghi thức Thăng Thần** (vòng hồn hoàn vàng kim, 10 giây). Nhận Thần Lõi (đếm +1 vào Hệ bất kỳ):

| Đạo | Thần Lõi | Bị động | Chủ động R nâng cấp |
|-----|----------|---------|---------------------|
| Kiếm Tu | **Thái Sơ Kiếm Ý** | Kiếm khí xuyên giáp, Bản Mệnh Kiếm tiến hóa lần cuối | Kiếm Vực: vạn kiếm trong vòng 12 ô, 12s |
| Đạo Tu | **Thiên Đạo Pháp Thân** | Thánh Lực không giảm khi dùng R; cánh sáng vĩnh viễn | Thiên Phạt: AoE lớn + hồi máu đồng đội |
| Thể Tu | **Bất Diệt Kim Thân** | Giảm 25% sát thương, khi <20% HP 1 lần bất tử 5s | Kim Thân Phá Thiên: dậm đất + quyền cực mạnh |
| Ma Tu | **Ma Thần Chân Thân** | Hấp Huyết +20%, Cuồng Hóa không mệt | Huyết Hải: vùng máu hút sống 10s |

Ma tu nhánh lai (Ma Kiếm/Thể/Đạo) chọn Thần Lõi **Ma** hoặc của đạo gốc, không cả hai.

### Thất bại thì sao

- Mỗi thất bại: **mất Thiên Kiếp Lệnh** (chế lại bằng nguyên liệu), mất **10% EXP** thanh cấp 99, cooldown thử lại 1 ngày game; **không rớt cấp**, không mất Lõi, không mất Ấn đã đạt.
- Bảo hiểm: thất bại 3 lần liên tiếp → nhận **Thiên Mệnh Bảo Hộ** (giảm 20% độ khó thử thách vừa thất bại, 1 lần).
- Chết trong Khảo Hạch: không áp dụng phạt chết thường (mục 8).

---

## 8. Phạt chết — CHỐT: Phương án B "Căn Cơ Tổn Thương"

| Mục | Quy tắc |
|-----|---------|
| Không mất | EXP, cấp/cảnh giới, Lõi, Hồn Cốt, Thần Lõi; Bản Mệnh Kiếm không rơi (canon §3) |
| Debuff | Mỗi lần chết +1 tầng **Căn Cơ Tổn Thương** (tối đa 3): mỗi tầng −15% chỉ số tối đa (HP, tài nguyên đạo), −25% hồi tài nguyên |
| Thời gian | 10 phút thực mỗi tầng, tự tháo từng tầng (không làm mới các tầng cũ) |
| Xóa nhanh | Đan Hồi Căn (S7), hoặc thiền ở Thánh Địa (−1 tầng/60 giây) |
| Ma tu Tẩu Hỏa Nhập Ma | R23: **không** dùng Căn Cơ Tổn Thương; chết ngay + **phế tu vĩnh viễn, làm lại từ đầu** (reset cấp 1, Lõi, Hồn Cốt, đạo, tư chất; giữ túi đồ, đồ bind tu tiên vô hiệu) + mất hết Huyết Tế; 3 tầng cảnh báo trước ngưỡng (xem S2 §2.4.3) |
| Ngoại lệ | Chết trong Khảo Hạch và nhiệm vụ Phá Chấp: không phạt |

Rơi đồ thường theo vanilla (`keepInventory` do S5 quyết định). Tùy chọn server: gamerule `hukhongHardcore` (mặc định `false`) bật chế độ phạt nặng hơn — chi tiết ngoài phạm vi S3.

---

## 9. Câu hỏi cho user (còn mở)

1. **Khảo Hạch sau cốt truyện**: thử thách 3 cần 4 Mảnh Thiên Môn (hạ cả 4 Chấp Hành Giả, tới cấp 98) và Thông Thiên Thần Đan. Chấp nhận việc **Thăng Thần bị khóa sau cốt truyện** ở cả chế độ Tự do?

(Đã giải quyết v2.1: Bù Tư Chất → R2; kết cục Phong Ấn/Hấp Thụ cá nhân → R18; độ đắt Tẩy Hồn → R9.)

---

## Đề xuất đổi canon (không tự sửa)

(Bốn đề xuất v1 — Hệ 2/4/6, Điểm Vận, Thần Lõi joker, hệ số EXP tư chất — **đã được canon v2 nhận**.)

1. **§1.1**: ghi cơ chế Bình Cảnh cụ thể (EXP ×0,25, Chướng = 1 chặng), mốc nhẹ ×3 với 7 Tiểu Phá Đan, và Bù Tư Chất (pity f).
2. **§5**: ghi ô Phó Mặc là Δ phẩm so với 3 ô thường (vượt trần tối đa +1), Mắc Kẹt → Phá Chấp miễn nhiễm sau đó; ô Hỗn Loạn không còn.
3. **§5**: Hồn Cốt chủ động không còn phím riêng (canon §5 "Hồn Cốt 6 slot giữ v1" có thể hiểu là còn phím G).
4. **§9/§10**: thử thách 3 Khảo Hạch yêu cầu Thiên Môn (4 Mảnh) và tách "trận Khảo Hạch cá nhân" khỏi "kết cục thế giới"; Khảo Hạch cho 1 dự thi + tối đa 3 hộ pháp.
5. **§1.1**: trần cộng dồn bonus EXP ngoài tư chất +50% (bảo vệ mô hình 110/500 h).

---

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | sections/S3-cores.md (bản trước) | EXP/Lõi/Hồn Cốt/kỹ năng/Khảo Hạch theo PLAN-v1 | ✅ Superseded |
| v2 | sections/S3-cores.md (bản trước) | Bình Cảnh 110/500 h, Phó Mặc, Tẩy Hồn, 3+1 phím, Tịch Diệt, phạt chết B | ✅ Superseded |
| **v2.1** | **sections/S3-cores.md (file này)** | Bù Tư Chất, Tiểu Phá Đan, KillRateGate, seed roll, Tẩy Hồn rẻ, Linh Thạch, Khảo Hạch cá nhân | 🔄 **Current** |
| — | PLAN-v2-tu-tien-mod-canon.md | Canon nguồn | ✅ Current |
| — | S7-alchemy.md | Tên Phá Cảnh Đan / Thần Đan | Tham chiếu |
