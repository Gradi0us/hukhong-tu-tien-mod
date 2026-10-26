# S4 — Thế Giới: Worldgen, Quái, Sứ Đồ, Boss, Sự Kiện, Nhiệm Vụ

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2 (Writer W4): áp dụng PLAN-v2 canon §7–§10 + S1-lore v2 (tên 6 thành Nhân, 6 loại Sứ Đồ, Văn Kiện A/B, vùng hang ổ) + S3 (EXP)
**Status**: 🔄 Draft v2, chờ user chốt mục 8 (3 câu)
**Ký hiệu CHG** (Chấp Hành Giả, theo canon §9 & S1): **I Thạch Hống (Thể, 67) · II Bạch Từ Âm (Đạo, 78) · III Lăng Uyên (Kiếm, 86) · IV Cơ Vô Tận (Ma, 98)**.
**Quy ước số liệu**: mọi con số là *đề xuất mặc định*, đặt trong datapack JSON. "Cấp vùng" = cấp người chơi khuyến nghị. Tọa độ: tâm (0,0) = điểm spawn thế giới; +X = Đông, +Z = Nam (Bắc = −Z).

---

## 0a. Thay đổi v2.1 (theo PLAN-v2 §14)

- R24: worldborder bán kính 15.000 (30.000×30.000): giãn tọa độ thành/tông/hang ổ và bán kính 5 vòng độ khó ×2, khoảng cách tối thiểu ×2, spacing cấu trúc ×2 (giữ số lượng), thời gian đi bộ ×2, sửa ASCII map. Văn tả vùng Cảnh Báo/cột khói/arena giữ nguyên.
- R3: chống farm dùng `KillRateGate` chung (>30 con cùng loài / 64 block / 10 phút, thưởng đến ×0,1, mọi đạo).
- R4: EXP vanilla cấp 1–9 m=1,5 phẳng (≈130 con/cấp), nhiệm vụ nhập môn gánh phần còn lại; từ cấp 10 giữ 1,5%.
- R15: Văn Kiện chuẩn tại S4 (6/14/28/55% x hệ số loại); bắt sống Ám Ngữ <=60%, 1 lần/ngày/người; Hoàng Đô vòng 4, Lưu Ly Cảng là cửa vào phổ biến.
- R16: n boss = người gây >=5% sát thương hoặc hồi máu đáng kể trong 48 block; loot cần đóng góp tối thiểu.
- R18: Khảo Hạch cá nhân (tối đa 3 hộ pháp); kết thúc là lựa chọn cá nhân ghi Bia Công Trạng; Tầng II dimension mới; Hấp Thụ chỉ đổi ngoại hình.
- R19: Văn Kiện/manh mối CHG chung toàn server; kích hoạt sớm cần party >=2 hoặc cảnh giới >= cấp CHG - 5.

## 0. Thay đổi v2 (so với v1)

| # | Thay đổi | Mục |
|---|----------|-----|
| 1 | Worldborder **bán kính 15.000 (30.000×30.000, R24)**: 5 vòng co lại (0–1.600 / 1.600–4.400 / 4.400–8.000 / 8.000–11.600 / 11.600+ gồm bốn góc vuông). Đặt **10 thành + 7 tông môn** theo vòng độ khó, có tọa độ mặc định, cách chỉ đường (bản đồ, cột khói, Lộ Bia, NPC, la bàn). ASCII map vẽ lại | §1 |
| 2 | Thành Lớn ngẫu nhiên của v1 **bỏ** → 10 thành cố định. "Pháo Đài Chấp Hành Giả đặt theo la bàn" của v1 **bỏ** → **Hư Sào (hang ổ) cố định theo seed**, theo vùng đặc thù trong S1 (Viêm Sơn, Kim Sa, Tuyết Kiếm, Huyết Trạch) | §1.2, §4.6 |
| 3 | **Sứ Đồ Dạ Khuyết** (6 loại × 4 bậc, cấp ≤ 65, tên theo S1): nơi xuất hiện, trà trộn trong thành, bảng rơi **Văn Kiện** theo bậc, ngưỡng lộ hang ổ / kích hoạt sớm CHG I, II | §3.6 |
| 4 | **CHG cấp mới 67/78/86/98**: đổi đạo/cơ chế theo cấp & đạo mới, **Giáng Lâm** ngẫu nhiên (tần suất, điều kiện), hang ổ theo seed, manh mối III/IV, khác biệt **Tuyến truyện vs Tự do** | §4.4, §4.6 |
| 5 | **Multiplayer**: công thức HP canon `×(1+0,75(n−1))`, chia loot cá nhân, boss thế giới cấm bay, boss khác có chiêu khóa bay | §4.0 |
| 6 | **Tập kích**: đồng hồ theo mức Xâm Thực **có trần cứng**, hoãn khi không có cư dân/chức vị online, vai trò từng chức vị trong phòng thủ, tái chiếm Phế Tích (giữ v1 5.5 + bổ sung chức vị) | §5.3–§5.5 |
| 7 | **Spawner = 0 EXP/loot tu tiên**; **EXP quái theo S3** (`E(L)=40·L^2.15`, vanilla m=1,5 phẳng ở cấp 1–9 rồi 1,5% từ cấp 10 (R4)). Bảng mốc tham chiếu cũ (1.000 / 30.000 / 400.000) **bị xóa**, thay bằng công thức | §3.2–§3.5 |
| 8 | **Tầng Thế Giới II** (Thiên Khuyết Giới / Hư Giới Dung): khác biệt map/quái/sự kiện giữa Phong Ấn và Hấp Thụ | §7 |
| 9 | Câu hỏi mở đã phán quyết hết ở v2.1; thêm "Đề xuất đổi canon" | §8 |

**Đã chốt từ canon (đóng câu hỏi v1)**: mất thành → Phế Tích + tái chiếm (Q1); tập kích theo Xâm Thực có trần (Q2); hoãn khi không ai online (Q3); border bán kính 15.000 (Q6, R24); boss thế giới cấm bay, boss khác có chiêu khóa bay (Q5). Bí Cảnh = dimension tạm (Q4, giữ đề xuất A).

---

## Key Findings

| # | Kết luận | Lý do |
|---|----------|-------|
| 1 | Border bán kính 15.000 (R24) → đi bộ ≈ 50–60 phút từ spawn ra góc xa; dùng **5 vòng đồng tâm** + **bốn góc vuông** (bán kính 11.600–21.200) làm chỗ đặt hang ổ nguy hiểm nhất | Tận dụng hình vuông: diện tích vòng 5 ≈ 53% bản đồ nhưng gần như không có thôn |
| 2 | Thành/tông đặt **tăng dần theo cấp** (Nhân → Địa → Thiên từ trong ra ngoài) và có **hệ chỉ đường 5 lớp** → người chơi luôn tìm được điểm kế tiếp | Yêu cầu "tìm được dễ" |
| 3 | **Văn Kiện tích lũy theo chuỗi A/B (S1)**: 3 cùng chuỗi → lộ hang ổ, 6 → kích hoạt sớm; kéo người chơi vào vòng lặp "săn Sứ Đồ → đọc → hé truyện" | Khớp S1 §3.4, canon §9 |
| 4 | Giáng lâm CHG là **sự kiện hiếm có điều kiện + khóa toàn cục** (1 CHG/lần), không để người chơi bị ép đánh boss 4–6 người khi đang chơi một mình | Multiplayer là công dân hạng nhất |
| 5 | Tập kích tính theo **giờ online đủ điều kiện**, trần cứng 90 phút/đợt, ≤ 3 đợt/ngày/thành | Không bao giờ "khóa" người chơi; không mất thành khi offline |
| 6 | Vanilla/spawner gần như **không cho EXP** từ cấp 10 → ép ra vùng Hư Hóa; cấp 1–9 dựa vào nhiệm vụ nhập môn vì m=1,5 vẫn chậm (R4) | Canon §1.1, S3 §1.4 |

---

## 1. Map / Worldgen Overworld

### 1.1 Năm vòng (tâm = spawn, hình vuông ±15.000)

| Vòng | Bán kính (block) | Diện tích | Cấp vùng | Tên | Đặc điểm |
|------|------------------|----------:|----------|-----|----------|
| 1 | 0–1.600 | 8,0 M | 1–19 | Nhân Giới | Spawn, Lưu Ly Cảng, thôn; Hư Hóa thấp xuất hiện đêm |
| 2 | 1.600–4.400 | 52,8 M | 15–39 | Tông Giới | 5 thành Nhân còn lại, 4 tông Nhân, Phế Tích đầu tiên, Bí Cảnh Bạch/Hoàng |
| 3 | 4.400–8.000 | 140,4 M | 35–59 | Biên Cương | 3 thành Địa, 2 tông Địa, Thánh Địa, Thiên Môn Đài, Xâm Thực đầu tiên |
| 4 | 8.000–11.600 | 221,6 M | 55–79 | Hoang Vực | Thiên Khải Hoàng Đô, Vạn Tượng Thánh Tông, Hư Sào I & II, boss thế giới |
| 5 | 11.600 → góc (≤ 21.200) | 477,2 M | 75–100 | Cận Hư Không | Hư Sào III & IV, Xâm Thực dày, gần như không thôn |

Ghi chú: (a) khoảng cách tính theo **spawn thế giới**; (b) worldborder cố định bán kính 15.000 (R24; giãn gấp đôi so với bản v2); (c) cấp vùng chồng lấn có chủ đích; (d) đi bộ chạy nước rút ≈ 5,6–7 block/giây: spawn → Hoàng Đô ≈ 22 phút, spawn → góc xa nhất ≈ 50–60 phút.

### 1.2 Mười thành & bảy tông môn — vị trí mặc định

Vị trí mặc định là **neo cố định** (cộng/trừ lệch ≤ 400 block theo biome hợp lệ, theo seed). Tên thành Nhân theo S1 §6; tên "Nhân Thành #1–6" chỉ là mã nội bộ.

| Cấp | Thành | Mã | (X, Z) | r | Hướng | Vòng | Ghi chú đặt vị trí |
|-----|-------|----|--------|---:|-------|------|--------------------|
| Nhân | **Lưu Ly Cảng** | N#1 | (1.000, −600) | 1.160 | ĐB gần spawn | 1 | Cần sông lớn/biển; **trạm bán Địa Đồ** (hub chỉ đường) |
| Nhân | **Hắc Thạch Quan** | N#2 | (600, 3.600) | 3.650 | Nam | 2 | Gần Bá Thể Môn (S1); cửa ải, hay bị tập kích |
| Nhân | **Cổ Tiên Thành** | N#3 | (−3.400, 1.000) | 3.540 | Tây | 2 | Di tích Cửu Thần; mật độ Cổng Bí Cảnh ×2 quanh thành |
| Nhân | **Huyết Nguyệt Phường** | N#4 | (−2.000, −2.600) | 3.280 | Tây Bắc | 2 | Dưới lòng đất, lối vào mặt đất giấu trong rừng Dark Forest; cửa ngõ Huyết Ảnh Ma Môn |
| Nhân | **Đan Hà Thành** | N#5 | (3.800, 2.000) | 4.300 | Đông Nam | 2 | Sông đỏ; trụ sở Đan Đỉnh Tông |
| Nhân | **Cơ Xảo Thành** | N#6 | (−1.800, 3.200) | 3.670 | Tây Nam | 2 | Đồi/mỏ; trụ sở Thiên Công Khí Các |
| Địa | **Lạc Nhật Thành** | | (−5.600, −600) | 5.630 | Tây | 3 | Sa mạc/Badlands (S1); gần Kim Sa Cổ Địa |
| Địa | **Thanh Vân Thành** | | (3.000, 5.400) | 6.180 | Nam-ĐN | 3 | Thành lớn nhất; Ấn Hồn Đài chính |
| Địa | **Bắc Hàn Thành** | | (1.000, −6.600) | 6.680 | Bắc | 3 | Băng giá; gần Tuyết Kiếm Lĩnh |
| Thiên | **Thiên Khải Hoàng Đô** | | (6.600, −5.000) | 8.280 | Đông Bắc | 4 (mép 3) | Quan Đạo hội tụ về đây ("trung tâm" chính trị/giao thông, xem §8) |

| Cấp | Tông môn | (X, Z) | r | Vòng | Bối cảnh |
|-----|----------|--------|---:|------|----------|
| Nhân | **Bá Thể Môn** | (400, 2.200) | 2.240 | 2 | Windswept Hills; cách Hắc Thạch Quan ≈ 1.400 |
| Nhân | **Đan Đỉnh Tông** | (3.400, 800) | 3.500 | 2 | Gần sông, linh dược; cách Đan Hà Thành ≈ 1.260 |
| Nhân | **Thiên Công Khí Các** | (−3.000, 3.400) | 4.540 | 2 | Núi có mỏ; cách Cơ Xảo Thành ≈ 1.200 |
| Nhân | **Huyết Ảnh Ma Môn** (ẩn) | (−3.200, −3.000) | 4.380 | 2 | Dark Forest; **không hiện trên bản đồ/cột khói**, xem 1.5 |
| Địa | **Quang Minh Đạo Cung** | (−6.800, 1.800) | 7.040 | 3 | Cao nguyên sáng; thân Lạc Nhật (S1), cách ≈ 2.680 |
| Địa | **Thiên Kiếm Tông** | (−3.000, −6.400) | 7.070 | 3 | Jagged/Frozen Peaks; cách Bắc Hàn ≈ 4.000 |
| Thiên | **Vạn Tượng Thánh Tông** | (−6.000, 7.200) | 9.380 | 4 | Đối xứng Hoàng Đô qua tâm; tòa chi điện nằm trong Hoàng Đô |

Khoảng cách tối thiểu: thành–thành ≥ 2.000; tông–thành ≥ 1.200; mọi cấu trúc dân sự cách mép border ≥ 1.400.

**Khai thác hang ổ & điểm đặc thù** (cố định theo seed, §4.6): 4 Hư Sào ở vùng riêng (Viêm Sơn / Kim Sa Cổ Địa / Tuyết Kiếm Lĩnh / Huyết Trạch); 1 **Thiên Môn Đài** trong Thánh Địa tại (−1.400, 5.800), vòng 3 (v1 chỉ nói "vòng 3").

### 1.3 Cách chỉ đường (5 lớp, từ rẻ đến đắt)

| Lớp | Cơ chế | Hiệu quả |
|-----|--------|----------|
| 1 | **Địa Đồ Sơ Khai** phát lúc khởi đầu: hiện Lưu Ly Cảng + 2 thành/tông gần nhất; NPC Họa Sư ở Lưu Ly Cảng bán **Địa Đồ Từng Vòng** (V2 / V3 / V4 / V5) – mỗi tấm lộ toàn bộ thành/tông/Thánh Địa của vòng đó (không lộ Ma Môn, Hư Sào) | Luôn biết chỗ cần đi kế tiếp |
| 2 | **Lộ Bia** (cột đá chỉ đường) mỗi ≈ 500 block dọc **Quan Đạo** (đồ thị nối: mỗi thành → 2 thành gần nhất + tông gần nhất; mọi tuyến hội tụ về Hoàng Đô). Mặt bia ghi "→ Lạc Nhật Thành 2.480 block (Tây)". Không rải đường lát thật, chỉ Lộ Bia + vài đoạn đường đất | Đi theo chuỗi bia không lạc |
| 3 | **Cột khói/Hỏa Đài** trên mỗi thành & tông (trừ Ma Môn): Nhân thấy từ ≈ 200 block, Địa ≈ 300, Thiên ≈ 400 (phụ thuộc view distance, tối đa chunk đã tải) | Khoảng cuối dễ thấy |
| 4 | **NPC**: Thương Đội đi theo Quan Đạo giữa các thành (theo chân là tới); Lữ Khách "Gặp Cao Nhân" (§6.2) chỉ đường; Hộ Vệ cổng thành hỏi "đi đâu?" rồi đánh dấu bản đồ | Tìm bằng hội thoại |
| 5 | **Thiên Cơ La Bàn** (vật phẩm, mua ở Cổ Tiên Thành / Thanh Vân Thành): chỉ thành, tông, Thánh Địa chưa ghé gần nhất; sau khi lộ Hư Sào (§3.6) thì thêm mục tiêu đó | Dành cho người lạc / late game |

Thành/tông đã **thăm lần đầu** tự ghi vào bản đồ người chơi (và chia sẻ khi cùng party).

### 1.4 Bảng cấu trúc (bán kính 15.000, 30.000²)

Chỉ số lượng/spacing tính theo diện tích vòng; 1 chunk = 16 block. Spacing = chunk trung bình giữa hai cấu trúc cùng loại (`structure_set`, tách theo vòng).

| Cấu trúc | Số lượng mục tiêu (V1/V2/V3/V4/V5) | Spacing (chunk) | Biome | Nội dung chính |
|----------|-------------------------------------|-----------------|-------|----------------|
| **Thôn Linh** | 12 / 40 / 25 / 0 / 0 | 52 / 72 / 148 | Plains, Meadow, Savanna, Taiga | Dân thường, linh dược nhỏ, nhiệm vụ cấp 1–9 |
| **10 thành + 7 tông** | cố định, xem 1.2 | — | theo bảng | Ấn Hồn Đài, chợ, bảng nhiệm vụ, Trận Nhãn |
| **Thánh Địa** | 0 / 0 / 3 / 3 / 2 (một là Thiên Môn Đài) | cố định theo seed | `hukhong:linh_mach` | Thiền ×1,5–2, Ấn Hồn Đài, không cửa hàng |
| **Phế Tích Hư Hóa** | 1 / 6 / 14 / 20 / 25 | 186 / 198 / 208 / 274 | Mọi biome, rìa Xâm Thực | Thành/thôn cũ; ≈ 30% bị Dạ Điện chiếm (§3.6); có chìa tái chiếm |
| **Cứ Điểm Dạ Điện** (Tam Tuyến) | 0 / 4 / 8 / 8 / 4 | cố định theo seed | Dark Forest, hang, Phế Tích | Nơi xuất hiện Sứ Đồ (§3.6); hồi sinh 2 ngày thực |
| **Tháp Tiền Đồn** | 0 / 0 / 4 / 5 / 3 | ≈ 90–150 | Trong/gần Xâm Thực | Mini-boss, Hồn Tinh (không chỉ vị trí Hư Sào – tránh đè hệ Văn Kiện) |
| **Lõi Xâm Thực** | 0 / 0 / 12 / 18 / 28 (+4 lõi lớn tại Hư Sào) | seed | `hukhong:hu_hoa` | Xem 1.5 |
| **Hư Sào** (4 hang ổ CHG) | 4 duy nhất | seed | 4 vùng đặc thù | §4.6 |
| **Cổng Bí Cảnh** | sự kiện sinh động | — | mọi biome | §5.1 |

Vùng đặc thù (biome tag mới, báo S5): `viem_son` (nham thạch), `kim_sa_co_dia` (sa mạc, đền cổ), `tuyet_kiem_linh` (núi tuyết), `huyet_trach` (đầm lầy máu). Vòng 1 hạn chế Phế Tích (≤ 1) để người mới không bị đè.

### 1.5 Vùng Xâm Thực & Huyết Ảnh Ma Môn ẩn

| Thuộc tính | Thiết kế |
|------------|----------|
| Biome | `hukhong:hu_hoa` — trời tím xám, sương đen; thay biome gốc dưới tâm |
| Khối bị nhiễm | Cỏ → Hư Thảo; Đất → Đất Ô Uế; Đá → Đá Hư Hóa; Gỗ → Gỗ Mục Hư; Nước → Nước Đục (Suy Yếu nhẹ); Lá → Lá Tàn |
| Nguồn lan | **Lõi Xâm Thực** (block hiếm, HP 200); phá để dừng lan |
| Tốc độ | +1 block / 10 phút thực (online) mỗi Lõi; đêm ×1,5; **trần 400 block/Lõi** (Lõi Hư Sào 1.200); chỉ chạy chunk đã tải; bù offline tối đa 6 giờ khi chunk tải lại |
| Tác động người chơi | Debuff **Ô Nhiễm** cộng dồn, quái Hư Hóa spawn ×2 trong vùng; Di Dân Phế Tích kháng nhẹ |
| **Chỉ số XT toàn cục** (dùng cho §4.6, §5.3) | `XT = 100 × Σ(bán kính_i / trần_i) ÷ số Lõi ban đầu`, Lõi đã đốt tính 0. XT = 0–100, không phải % diện tích |
| Chỉ số quanh thành `I` (0–1) | `I = clamp(1 − d/1.000, 0, 1)`, d = khoảng cách từ mép Xâm Thực gần nhất đến mép thành (≤ 0 → 1) |
| Tác động thành | `I` kéo ngắn đồng hồ tập kích (§5.3) |

Thanh tẩy (3 lớp): **Tịnh Thạch** (bán kính 8) → **Trận Tịnh Hóa** (đạo Đạo; 24–48 block, 1 lớp/phút) → **Phá Lõi + đốt Lõi** (dừng lan vĩnh viễn; 2–4 người).

**Huyết Ảnh Ma Môn (ẩn)**: không hiện bản đồ/cột khói/Lộ Bia; đường vào: (a) vật phẩm **Huyết Ảnh Lệnh** mua ở Huyết Nguyệt Phường, (b) người chơi Ma tu với Ác Tính ≥ ngưỡng được NPC "Huyết Nô" ở Phường dẫn đường, (c) tự dò trong Dark Forest quanh (−3.200, −3.000), 1 trong 3 "cây dấu" dẫn vào lối hang; sau khi vào lần đầu, ghi vào bản đồ.

### 1.6 ASCII map (sơ đồ, Bắc ở trên; vòng 1–4 là dấu chấm từ trong ra; độ lệch tỉ lệ ≈ ±10%; border bán kính 15.000, mọi tọa độ/khoảng cách ×2 so với bản v2)

```
 +-----------------------------------------------------+
 |                     BẮC                             |
 |                     M<-- III Lăng Uyên (Tuyết Kiếm) |
 |                    .............                    |
 |               ...                 ...               |
 |            ..                         ..            |
 |         ..        ...      B  ....       ..         |
 |        ..     ...   K             ...     ..        |
 |       .      ..      .........      .H      .       |
 |      .     ..      m.4        ..      ..     .      |
 |     ..     .     ..   .......   ..     .     ..     |
 |  TÂY..     .   L ..   .. * 1.   ..     .     ..  ĐÔNG
 |     ..     . Q   ..3  .......  d5.     .     ..     |
 |      J<--II ..     .k 6   t   ..      ..     .      |
 |   (Kim Sa)    ..   .....2...      ..      .         |
 |        ..     ...      +      V   ... I<--I (Viêm) |
 |         ..    X  ....         ....       ..         |
 |           ...                         ...           |
 |               ...                 ...               |
 |     N<-- IV Cơ Vô Tận (Huyết Trạch, góc TN)         |
 |                     NAM                             |
 +-----------------------------------------------------+

CHÚ GIẢI
 *  spawn (0,0)                 1 Lưu Ly Cảng   2 Hắc Thạch Quan   3 Cổ Tiên Thành
 4  Huyết Nguyệt Phường         5 Đan Hà Thành  6 Cơ Xảo Thành     (thành Nhân)
 L  Lạc Nhật   V Thanh Vân   B Bắc Hàn   (thành Địa)    H Thiên Khải Hoàng Đô (Thiên)
 t Bá Thể Môn  d Đan Đỉnh  k Thiên Công Khí Các  m Huyết Ảnh Ma Môn (ẩn)   (tông Nhân)
 Q Quang Minh  K Thiên Kiếm (tông Địa)      X Vạn Tượng (tông Thiên)
 + Thiên Môn Đài (Thánh Địa)    I J M N  Hư Sào CHG I / II / III / IV (VỊ TRÍ MẪU của một seed)
 .  mép vòng 1 / 2 / 3 / 4 (vòng 5 = phần còn lại ra tới 4 góc)
```

Hư Sào mẫu (seed ví dụ): **I** (7.600, 6.000) r 9.680 · **II** (−11.200, 2.400) r 11.460 · **III** (−3.000, −13.400) r 13.730 · **IV** (−12.000, 11.400) r 16.560. Luật đặt (theo seed): II trong cung phương Tây ±25°, r 11.000–12.600; III trong cung phương Bắc ±25°, r 12.600–14.400 (khớp S1: Kim Sa gần Lạc Nhật, Tuyết Kiếm gần Bắc Hàn); I trong cung Đông/Nam/ĐN, r 8.600–10.400; IV trong một **góc vuông** còn trống, r ≥ 15.000. Cách nhau ≥ 5.000, cách thành/tông ≥ 3.000, cách mép border ≥ 1.200.

---

## 2. Chiều Không Gian

### 2.1 Hư Không Bản Nguyên (thay The End)

| Hạng mục | Thiết kế |
|----------|----------|
| ID | `hukhong:ban_nguyen` |
| Điều kiện mở | Gom **4 Mảnh Thiên Môn** (Địa Môn / Quang Môn / Phong Môn / Huyết Môn — S1). **Mỗi người tham chiến nhận 1 mảnh cá nhân** lần đầu hạ CHG (100%), các lần sau 15%. Ghép tại **Thiên Môn Đài** (Thánh Địa (−1.400, 5.800), vòng 3): đặt 4 mảnh vào 4 góc + 1 Hồn Tinh Hồng. (Khi chơi Phong Ấn: ghép = đóng Hư Không Giới, xem §7) |
| Yêu cầu vào | Khuyến nghị cấp ≥ 80; không cưỡng chế |
| Chia sẻ | Cổng mở 10 phút; người vào sau trong cửa sổ đó vào cùng phiên |
| Địa hình | Quần đảo đen-tím: **Vành Ngoài**, **Vành Giữa** (4 đảo-điện thờ mô phỏng ý niệm 4 CHG), **Tâm Bản Nguyên** (đấu trường 80 block) |
| Rơi xuống | Bị đẩy về đảo xuất phát, mất 20% HP tối đa + Ô Nhiễm |
| Hồi sinh | Hồi sinh ở Thiên Môn Đài, vào lại trong 10 phút |
| Khảo Hạch | Cá nhân, tối đa 3 hộ pháp (R18). Thử thách 3 của Khảo Hạch Thăng Thần = trận **Nguyên Thủy Tịch Diệt** |

### 2.2 Boss cuối — xem §4.5.

### 2.3 Bí Cảnh = dimension tạm thời (giữ v1)

| Thuộc tính | Thiết kế |
|------------|----------|
| Kỹ thuật | Dimension động từ template, xóa khi hết giờ. Singleplayer ≤ 1 instance; server ≤ 3 |
| Vào | Qua Cổng Bí Cảnh (có thời hạn) |
| Loại | Cổ Động, Tiên Phủ, Chiến Trường (sóng vô hạn, script không phải spawner), Hư Giới, Thư Viện |
| Phần thưởng | Hồn Tinh theo phẩm, Đá Tẩy Luyện, Hồn Cốt hiếm, Thiên Cơ Phù, truyền thừa; **10–12% rơi Dấu Vết III/IV ở phòng cuối Bí Cảnh Hắc/Hồng** (§4.6) |
| Tử vong | Giữ đồ (gamerule), mất cơ duyên đang tích; chết 3 lần bị loại |

---

## 3. Quái

### 3.1 Quy tắc cấp quái theo vùng

| Quy tắc | Mô tả |
|---------|-------|
| Cấp quái = cấp vùng ± 3 | Quái vòng 1 ≤ cấp 22 |
| Điều chỉnh | Không scale theo cấp người chơi; scale **HP boss theo số người** (§4.0) |
| Elite | 5% quái vùng: +50% HP, +25% Công, tiền tố ngẫu nhiên; **EXP ×5** (S3), loot ×2 |
| Ngày/đêm | Đêm ×1,5 spawn Hư Hóa ở vòng 1–2 |
| Cấp hiển thị | `[Lv.xx] Tên`; thấp hơn người chơi ≥ 10 cấp → EXP ×0,1 |
| Giới hạn spawn | Cap 12 quái/chunk, 20 trong Xâm Thực; **Kết Giới Thành** bán kính 64 chặn spawn Hư Hóa/Hư Tu tự nhiên (trừ tập kích và Sứ Đồ trà trộn) |

### 3.2 Biến thể Hư Hóa (vanilla bị nhiễm)

EXP = `w × E(L)/200` (S3 §1.4, cột "EXP" là giá trị tại cấp giữa khoảng). Hệ số w do S4 đặt theo độ khó.

| # | Vanilla | Biến thể | Cấp | w | EXP @ giữa | Đặc điểm | Loot |
|---|---------|----------|-----|--:|----------:|----------|------|
| 1 | Zombie | Thi Hư | 5–15 | 1,0 | 28 | HP ×3; chết trả 1 Phân Thân nhỏ | Hồn Tinh Bạch 20% |
| 2 | Skeleton | Cốt Hư | 8–18 | 1,1 | 55 | Mũi tên Ô Nhiễm | Bạch 22% |
| 3 | Creeper | Bạo Hư | 10–22 | 1,3 | 101 | Nổ gây Ô Nhiễm, tạo Đất Ô Uế | Đá Tẩy Luyện 8% |
| 4 | Spider | Nhện Hư | 12–24 | 1,2 | 120 | Tơ giam 2 giây | Bạch 25% |
| 5 | Enderman | Hư Hành | 28–45 | 1,8 | 799 | Dịch chuyển sau lưng, né 1 kỹ năng | Hoàng 15% |
| 6 | Witch | Hư Phù | 25–40 | 1,7 | 586 | Lọ ném tăng Tâm Ma | Đá Tẩy Luyện 15% |
| 7 | Phantom | Hư Ảnh | 30–48 | 1,8 | 949 | Bay, vô hiệu bay Kiếm tu 3 giây | Hoàng 18% |
| 8 | Piglin Brute | Hư Man | 40–58 | 2,2 | 1.894 | Phá khiên, Bạo Kích | Tử 10% |
| 9 | Ravager | Hư Thú | 45–65 | 2,5 | 2.759 | Phá tường thành | Tử 14% |
| 10 | Warden | Hư Quan (mini-boss) | 70–90 | 25 | 61.747 | Siêu thính theo rung động | Hắc 12%, Đá Tẩy Luyện |

### 3.3 Hư Tu — tu sĩ bị đồng hóa

| # | Tên | Đạo | Cấp | w | EXP @ giữa | Cơ chế | Loot |
|---|-----|-----|-----|--:|----------:|--------|------|
| 1 | Hư Kiếm Tu | Kiếm | 30–50 | 1,8 | 1.002 | Ngự kiếm bay, Kiếm Khí xa; để lại Kiếm Hư | Hoàng, mảnh kiếm |
| 2 | Hư Kiếm Khôi | Kiếm | 55–75 | 3,0 | 4.741 | 3 phân thân, chỉ bản thật nhận sát thương | Tử 15% |
| 3 | Hư Đạo Sĩ | Đạo | 30–50 | 1,9 | 1.057 | Bố trận nhỏ, hồi máu quái khác | Đá Tẩy Luyện 20% |
| 4 | Hư Thánh Giả | Đạo | 60–80 | 3,5 | 6.487 | Cánh đen, thiên phạt AoE, phản phép 20% | Tử 20% |
| 5 | Hư Thể Tu | Thể | 30–50 | 1,8 | 1.002 | Tay không, tích Bá Khí đen | Hoàng |
| 6 | Hư Kim Cương | Thể | 55–75 | 3,2 | 5.058 | Miễn 50% vật lý; yếu phép/Ô Uế | Tử 15% |
| 7 | Hư Ma Đầu | Ma | 40–60 | 2,2 | 1.978 | Hấp Huyết; mỗi mạng giết tăng stat | Tử 18% |
| 8 | Hư Tâm Ma | Ma | 65–85 | 4,0 | 8.599 | +10 Tâm Ma/đòn | Hắc 10% |

Quy tắc: Hư Tu có **Đạo Luật** như người chơi đạo gốc (người chơi học cách chống từng đạo).

### 3.4 Quái gốc Hư Không

| # | Tên | Cấp | w | EXP @ giữa | Cơ chế | Loot |
|---|-----|-----|--:|----------:|--------|------|
| 1 | Hư Trùng | 20–40 | 0,4 | 120 | Bầy 8–12, nổ khi chết | Bạch |
| 2 | Hư Ảnh Quỷ | 35–55 | 1,7 | 1.219 | Chỉ nhận sát thương khi hiện hình | Hoàng |
| 3 | Hư Thạch Giáp | 45–65 | 2,6 | 2.869 | Phá khiên Bá Khí; yếu sau lưng | Đá Tẩy Luyện 25% |
| 4 | Hư Mục Sư | 50–70 | 2,8 | 3.726 | Ký sinh, hồi/giáp nhóm | Tử 12% |
| 5 | Hư Lang Vương | 60–80 | 3,8 | 7.043 | Triệu hồi bầy | Tử 20% |
| 6 | Hư Liên Sát | 70–90 | 4,2 | 10.373 | Xích kéo, **khóa bay** | Hắc 10% |
| 7 | Hư Nguyên Thể | 80–100 | 5,0 | 15.908 | Chỉ chịu Thần Thức/Ô Uế/Bá Khí; vật lý −90% | Hồng 8% |

### 3.5 EXP quái, Spawner, Loot chung

**Công thức EXP (khớp S3; thay bảng mốc cũ)**

```
base(L)        = E(L) / 200,   E(L) = round(40 · L^2.15)          // 1 con "chuẩn" cùng cấp (≈ 70 con/cấp ở tỉ trọng 35%)
EXP_quái       = w · base(L_quái) · k_tư_chất · f_chênh_cấp        // w = hệ số loài (bảng 3.2–3.4), Elite ×5
EXP_vanilla    = m(L_người) · base(L_người) · k_tư_chất,  m = 1,5 (phẳng, R4) khi cấp người chơi 1–9;
                 m = 0,015 (1,5%) từ cấp ≥ 10
f_chênh_cấp    = 0,1 nếu quái thấp hơn người chơi ≥ 10 cấp; 1 trong các trường hợp còn lại
EXP_spawner    = 0
```

| Cấp L | E(L) | base = E/200 | Vanilla (1,5%) | Vanilla (m=1,5, cấp ≤ 9) |
|------:|-----:|-------------:|---------------:|-----------------------:|
| 1 | 40 | 0,2 | — | 0,3 |
| 5 | 1.273 | 6,4 | — | 9,6 |
| 9 | 4.505 | 22,5 | — | 33,8 |
| 10 | 5.650 | 28 | 0,42 | — |
| 20 | 25.077 | 125 | 1,9 | — |
| 40 | 111.298 | 557 | 8,4 | — |
| 60 | 266.125 | 1.331 | 20 | — |
| 80 | 493.973 | 2.470 | 37 | — |
| 98 | 764.181 | 3.821 | 57 | — |

Nhận xét: vanilla từ cấp 10 chỉ còn 1,5% → không thể cày; cấp 1–9 với m=1,5 cần ≈130 con/cấp (R4), phần còn lại do nhiệm vụ nhập môn gánh nên **EXP cấp 1–9 chủ yếu từ nhiệm vụ nhập môn + Thi Hư/Cốt Hư (vòng 1, từ cấp 5)**. Xem §8.

**Spawner**

| Quy tắc | Chi tiết |
|---------|----------|
| Nhận diện | Gắn tag `hukhong:spawner_born` khi `MobSpawnType` là `SPAWNER` hoặc `TRIAL_SPAWNER` (đề xuất thêm: `SPAWN_EGG`, `COMMAND`/`MOB_SUMMONED` không tính) |
| Hệ quả | **0 EXP tu luyện, 0 Hồn Tinh, 0 vật liệu tu tiên**; vẫn rơi loot vanilla gốc |
| Cấu trúc mod | Không đặt spawner trong Cứ Điểm/Bí Cảnh (dùng script sóng) |
| Chống farm (`KillRateGate`, R3) | >30 con cùng loài trong bán kính 64 block / 10 phút → thưởng giảm dần đến ×0,1; áp mọi đạo (Ma dùng chung, giữ Ác Danh) |

**Loot chung**

| Vật phẩm | Nguồn | Tỉ lệ | Công dụng |
|----------|-------|-------|-----------|
| **Hồn Tinh** (Bạch→Hồng) | Hư Hóa/Hư Tu/Hư Không/Sứ Đồ (không từ spawner) | Theo bảng; Elite ×2 | Tăng phẩm Lõi, nhiên liệu Tịnh Thạch |
| **Đá Tẩy Luyện** | Quái cấp 20+, rương, Bí Cảnh | 5–25% | Tinh luyện Lõi, chế Tịnh Thạch |
| Mảnh Hư Hóa | Mọi Hư Hóa | 40% | Chế đồ |
| Hồn Cốt | Boss từ tầng 2 | §4 | Slot phụ |
| **Văn Kiện** | Sứ Đồ | §3.6 | Manh mối CHG I/II |

Phẩm Hồn Tinh theo vòng: V1 Bạch; V2 Bạch/Hoàng; V3 Hoàng/Tử; V4 Tử/Hắc; V5 Hắc/Hồng (Hồng ≤ 3% mỗi lần rơi trừ boss).

### 3.6 Sứ Đồ Dạ Khuyết (quái/NPC, tổ chức **Dạ Điện**, mô hình **Tam Tuyến**: tế bào 3 người)

Cấp ≤ 65, bậc theo S1: **Hạ Sứ ≤ 25 · Trung Sứ 26–45 · Thượng Sứ 46–60 · Đại Sứ 61–65**. Dấu nhận biết: **Nguyệt Khuyết ấn** (cổ tay trái; hiện khi trăng tròn hoặc có Thanh Tâm Đan).

**Sáu loại × bậc (cấp), vai trò trong game**

| Loại | Vai trò / chiêu | Hạ Sứ | Trung Sứ | Thượng Sứ | Đại Sứ | Xuất hiện chính |
|------|-----------------|:----:|:--------:|:---------:|:------:|-----------------|
| **Ảnh Tuyến** | NPC giả (Đổi Mặt); lộ diện thì đánh lén + bỏ trốn, bom khói | 12–25 | 30–45 | 50–58 | — | **Trà trộn trong thành/tông**; sau lộ diện ở Cứ Điểm |
| **Dạ Kiếm** | Cận chiến, tập kích đêm, lướt kiếm | 15–25 | 28–45 | 48–60 | — | Quan Đạo ban đêm, Phế Tích, Cứ Điểm; đi cặp trong tập kích đặc biệt |
| **Hư Chú Sư** | Gây Hư Hóa, đổi mặt, bố trận mở khe nhỏ triệu quái Hư Hóa | 18–25 | 30–45 | 50–60 | — | Rìa Xâm Thực, Cứ Điểm; thấy cạnh Ảnh Tuyến mới thay mặt |
| **Huyết Tế Tư** | Hiến tế dân làng, hồi máu đồng bọn, Thượng/Đại Sứ có Huyết Vực nhỏ | — | 30–45 | 50–60 | 62–65 | Cứ Điểm (trung tâm), Thôn vòng 2–3 (hiến tế), Phế Tích bị chiếm |
| **Ám Ngữ Giả** | Truyền tin, mang Văn Kiện; yếu, chạy nhanh, bỏ chạy khi bị chặn | 8–25 | 26–40 | — | — | **Thương Đội ngụy trang** theo Quan Đạo; trà trộn chợ thành |
| **Dạ Hộ Pháp** | Cận vệ ưu tú, giáp nặng, canh nơi quan trọng | — | — | 52–60 | 62–65 | Cứ Điểm lớn, bảo vệ Huyết Tế Tư/Đại Sứ |

**Nơi xuất hiện**

| Nơi | Mô tả | Cap |
|-----|-------|-----|
| **Cứ Điểm Dạ Điện** | 24 trong world (V2×4, V3×8, V4×8, V5×4). Mỗi cứ điểm 1–3 "tế bào" (3 người/tế bào): ví dụ cứ điểm lớn = 6 Hạ + 4 Trung + 2 Thượng + 1 Đại. Hồi sinh 2 ngày thực | — |
| Phế Tích | ≈ 30% Phế Tích bị chiếm đóng (thay Quản Gia Phế Tích bằng Thượng Sứ chỉ huy) | 1 tế bào |
| Quan Đạo & rìa thành | Dạ Kiếm đêm; Ám Ngữ Giả ban ngày giả thương đội | 1 nhóm/đoạn, ≤ 2 nhóm/người chơi |
| Rìa Xâm Thực | Hư Chú Sư mở khe, Xâm Thực lan nhanh hơn ×1,3 quanh cứ điểm | — |
| **Trà trộn trong thành/tông** | Ảnh Tuyến (đổi mặt NPC thật) + Ám Ngữ Giả (thương nhân) + 1 Hư Chú Sư thấp (Hạ) sau khi tập kích | Nhân ≤ 2, Địa ≤ 3, Thiên ≤ 4 mỗi thành; tông ≤ 1–2 (S1: nội gián ở Quang Minh, Đan Đỉnh, Thanh Vân…) |
| Không spawn | Spawner; trong Kết Giới Thành (trừ trà trộn đã định sẵn) | — |

**Bóc mặt nạ (infiltrator)**: NPC giả có "thói quen lệch" (S1 §3.3). Cách phát hiện: (a) hỏi chuyện riêng (đối thoại kiểm tra, tỉ lệ đúng theo Bậc: Hạ 70% / Trung 55% / Thượng 40%); (b) trăng tròn hoặc **Thanh Tâm Đan** → hiện Nguyệt Khuyết ấn; (c) theo dõi đêm: hoạt động bất thường quanh Trận Nhãn/Kho; (d) chức vị **Hộ Vệ+** mở "Tra Hỏi" (§5.3 bảng vai trò). Lộ diện = chiến đấu + rơi Văn Kiện (nhân 1,5). Không bóc trong **3 ngày game** → +10% trọng số tập kích/gian tế và có thể "phá trận" (mở lỗ Hộ Thành Đại Trận, S1 A2) → sóng 1 khó hơn.

**EXP Sứ Đồ**: `w` theo bậc — Hạ 1,2 · Trung 2,0 · Thượng 3,5 · Đại 12 (×E(L)/200); Huyết Tế Tư/Dạ Hộ Pháp Đại Sứ 30.

**Bảng rơi Văn Kiện** (tỉ lệ cơ sở bậc: **chuẩn S4, R15**; S1 §3.4 đã đồng bộ)

| Bậc | P(rơi ≥1) cơ sở | Văn Kiện hợp lệ (S1 §3.4) |
|-----|:--------------:|--------------------------|
| Hạ Sứ | 6% | A1, A2 / B1, B2 |
| Trung Sứ | 14% | A2, A3, A4 / B2, B3, B4 |
| Thượng Sứ | 28% | A4, A5 / B4, B5 |
| Đại Sứ | 55% (đảm bảo 1 khi hạ Đại Sứ lần đầu mỗi cứ điểm) | A6 / B6 (+ A5/B5) |

Nhân theo loại: Ám Ngữ Giả ×2,0 · Ảnh Tuyến ×1,5 (khi lộ diện) · Hư Chú Sư ×1,0 · Huyết Tế Tư ×1,0 · Dạ Kiếm ×0,7 · Dạ Hộ Pháp ×0,6. Trần 100%.
Kỳ vọng: 1 cứ điểm lớn ≈ 2 Văn Kiện; bắt sống/chặn đoàn Ám Ngữ Giả (không giết ngay, chỉ gây choáng rồi "Khám Xét") = tối đa **60%** ra 1 Văn Kiện hợp lệ bậc cao nhất của nó, giới hạn **1 lần/ngày/người** (R15).

**Chọn chuỗi A/B và trùng lặp**
- Chọn ID: lấy ngẫu nhiên trong danh sách hợp lệ của bậc; chuỗi: Sứ Đồ ở trong cung phương ±60° của Hư Sào CHG nào → 70% chuỗi đó (A = I Thạch Hống, B = II Bạch Từ Âm), 30% chuỗi kia; ngoài cả hai cung → 50/50. Hiệu ứng: đi về hướng Hư Sào thì Văn Kiện càng nghiêng về CHG đó (gợi hướng).
- Văn Kiện chưa giải mã hiện "?"; giải mã ở **Bàn Giải Mã** (thành Nhân+, tông) hoặc miễn phí bằng kỹ năng Đạo tu (S2).
- Trùng ID: đổi thành **Mảnh Văn Kiện**; 3 mảnh = 1 Văn Kiện ngẫu nhiên đang thiếu hợp lệ cho bậc cao nhất trong 3 mảnh (không tắc nghẽn).

**Ngưỡng (mỗi chuỗi tính riêng; kho chung server, R19)**

| Số Văn Kiện cùng chuỗi (khác ID) | Hiệu ứng | Bản đồ |
|:--:|----------|--------|
| 1 | Phương hướng Hư Sào (cung phương) | Mũi tên ở rìa bản đồ |
| 2 | Vùng nghi vấn bán kính 800 | Vòng tròn mờ |
| **3** | **Lộ hang ổ**: tọa độ chính xác, cổng Hư Sào "thức tỉnh" cho phép vào; La Bàn chỉ tới | Marker Hư Sào |
| **6** | **Kích hoạt sớm** sự kiện Giáng Lâm của CHG đó (xem §4.6): Bàn Giải Mã → "Dẫn Dụ" chọn địa điểm trong V3–V5 (không trong 300 block thành), đếm ngược 10 phút. Dùng 1 lần/lần giáng lâm; Văn Kiện không mất nhưng cooldown như §4.6 | Marker sự kiện |

Server: kho Văn Kiện và manh mối CHG **chung toàn server** theo từng chuỗi (R19), mỗi lượt ghi nhận người đóng góp (danh vọng); singleplayer = của mình. **Kích hoạt sớm** sự kiện chỉ khi người kích hoạt có **party ≥ 2 người** hoặc **cảnh giới ≥ cấp CHG − 5** (R19).

---

## 4. Boss

| Tầng | Số người KN | Respawn | Ảnh hưởng |
|------|-------------|---------|-----------|
| 1. Khu vực | 1–3 | 3 ngày game | Mở khóa vùng |
| 2. Thế giới | 3–8 | Sự kiện, ≤ 1/tuần thực | Giảm Xâm Thực |
| 3. Tập kích | cả thành | Theo §5.3 | Đi cùng sóng |
| 4. CHG I–IV | 4–6 | Giáng Lâm hoặc vào Hư Sào | Mảnh Thiên Môn |
| 5. Cuối | 6–10 | Khảo Hạch, 1 lần | Thăng Thần |

### 4.0 Multiplayer — HP, loot, bay (canon §10)

**HP động**

```
HP = HP_gốc × (1 + 0,75 × (n − 1)),   n = số người chơi trong 48 block quanh boss mà **gây ≥ 5% sát thương hoặc hồi máu đáng kể**, 1 ≤ n ≤ 10 (R16)
```

| n | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|----|
| × HP | 1,00 | 1,75 | 2,50 | 3,25 | 4,00 | 4,75 | 5,50 | 6,25 | 7,00 | 7,75 |

- Cập nhật mỗi 2 giây; **giữ nguyên tỉ lệ %HP hiện tại** khi n đổi (không hồi, không mất). Giảm n chỉ áp dụng sau khi người rời ≥ 10 giây ngoài vòng (chống nhấp nháy/exploit).
- Sát thương boss **không** nhân theo n; số kỹ năng AoE/đòn đồng thời tăng +1 mỗi 3 người.
- **HP_gốc đề xuất** `= k × L²` (S2/S3 hiệu chỉnh theo DPS thực): khu vực k=30 · tập kích k=40 · thế giới k=50 · **CHG k=80** · boss cuối k=100. Ví dụ: Thi Vương Hư Hóa L18 = 9.720 · Hư Long Thanh Mộc L55 = 151.250 · **CHG I L67 = 359.120 · II L78 = 486.720 · III L86 = 591.680 · IV L98 = 768.320** · boss cuối L100 = 1.000.000 (tại n=5 nhân 4,0).

**Loot (cá nhân, mỗi người 1 lượt)**
- Đủ điều kiện (loot cần đóng góp tối thiểu, R16): ở trong 48 block khi boss chết **và** Điểm Đóng Góp ≥ 5%; chỉ có mặt không đủ. Điểm Đóng Góp = sát thương + 0,8 × hồi máu hiệu dụng + 1,0 × sát thương đỡ cho đồng đội + giá trị buff (Đạo tu có điểm dù đòn thường lên quái = 0).
- Mỗi người đủ điều kiện **roll độc lập** từng dòng loot (Hồn Tinh, Hồn Cốt, Thiên Cơ Phù…); nhiều người không làm giảm tỉ lệ của nhau. Loot nhìn thấy riêng từng người (instanced). **Mảnh Thiên Môn lần đầu**: 100% mỗi người.
- EXP boss: pool chia theo Điểm Đóng Góp; **trần cá nhân 2,0 × E(L)** mỗi trận. Pool: khu vực 0,3–0,6 E(L) · thế giới 2–3 E(L) · **CHG 5 E(L)** (I = 1,69 M; II = 2,34 M; III = 2,89 M; IV = 3,82 M) · tập kích 1–2 E(L).

**Bay**
- **Boss thế giới luôn cấm bay**: "Cấm Không Giới" bán kính 120 block quanh boss (Kiếm tu ngự kiếm, cánh Đạo, bay Thể, elytra đều bị khóa; chạm hiệu ứng = rơi mềm 3 giây).
- **Boss khác** (khu vực, tập kích, CHG, cuối) có **chiêu khóa bay** riêng, có thời lượng + cách phá (bảng 4.4/4.5), không cấm vĩnh viễn.

### 4.1 Boss khu vực (vòng 1–3)

| Boss | Cấp | Phase / cơ chế | Loot |
|------|-----|----------------|------|
| **Thi Vương Hư Hóa** | 18 | P1 càn quét + triệu Thi Hư. P2 (<50%) cuồng nộ +40% tốc, nổ Ô Nhiễm | Hoàng ×2, Đá Tẩy Luyện, 8% Hồn Cốt Bạch |
| **Quản Gia Phế Tích** (Hư Tu trấn thủ; nếu Phế Tích bị Dạ Điện chiếm: **Thượng Sứ chỉ huy**) | 32 | P1 kiếm; P2 3 phân thân; P3 (<20%) tự hủy, đạp Trận Nhãn chặn | Tử, chìa tái chiếm |
| **Hư Nhện Mẫu** | 38 | Nhện mẹ + trứng; P2 bẫy tơ | Tử, 12% Hồn Cốt Hoàng |

Khóa bay: Nhện Mẫu "Tơ Lưới" (trói người đang bay 4 giây, hủy khi chém đứt tơ).

### 4.2 Boss thế giới (vòng 3–5, sự kiện) — **cấm bay** (4.0)

| Boss | Cấp | Phase | Loot |
|------|-----|-------|------|
| **Hư Long Thanh Mộc** | 55 | P1 phun Ô Nhiễm; P2 bay (boss) thả bom rác; P3 mất cánh, ép cận chiến | Hắc ×2, 25% Hồn Cốt Tử, 10% Dấu Vết III/IV |
| **Thần Mộc Xích Quỷ** | 68 | P1 kéo/khóa mục tiêu; P2 lãnh địa xích mất Bá Khí/Linh Lực; P3 khóa toàn sân 30 giây | Hắc ×3, 30% Hồn Cốt Tử, 3% Hắc, 10% Dấu Vết |
| **Hư Hoàng Điện** | 78 | Điện thờ nhiều "mắt" phá lần lượt | Hồng 30%, Thiên Cơ Phù, 20% Dấu Vết |

Kích hoạt khi Xâm Thực khu vực ≥ 60% (hoặc định giờ mỗi 5 ngày game); thông báo + bản đồ vòng 200 block.

### 4.3 Boss tập kích

| Boss | Cấp | Cơ chế | Loot |
|------|-----|--------|------|
| **Hư Tướng Công Thành** | 40–70 | Đại pháo sống nhắm Trận Nhãn; hạ nhanh làm yếu sóng | Hồn Tinh theo vòng, Đá Tẩy Luyện ×3 |
| **Hư Mục Sư Thống Lĩnh** | 45–75 | Hồi/buff toàn quân; giết đầu | Hồn Tinh, 10% Thiên Cơ Phù |

Khóa bay: "Hư Màn" quanh Tâm Thành 20 giây khi sóng 4 bắt đầu (phá bằng hạ Hư Mục Sư).

### 4.4 Bốn Chấp Hành Giả (cập nhật theo cấp/đạo mới)

Chung: HP_gốc = 80 L²; **enrage sau 15 phút**; thất bại → Hư Sào/arena reset 10 phút; loot cá nhân: Mảnh Thiên Môn tương ứng (100% lần đầu, 15% sau), Hồn Tinh Hồng ×1–2, 40% Hồn Cốt Hắc, 10% Hồn Cốt Hồng, vật phẩm đạo, **Dấu Vết III/IV** (§4.6).

| # | Tên · Đạo · Cấp | Mảnh | Phase | Chiêu khóa bay (thời lượng / cách phá) |
|---|-----------------|------|-------|-----------------------------------------|
| I | **Thạch Hống** · Thể · 67 | Địa Môn | P1 (100–70%): lá cà, Địa Chấn có báo hiệu, **Giáp Đá** hấp thụ 60% sát thương tới khi phá bằng chuỗi combo (8% HP tối đa). P2 (70–35%): Bá Khí tích tụ, **4 Trụ Bá Khí** phải phá để ngắt đòn Sơn Băng. P3 (<35%): thể hóa khổng lồ, rung chấn; yếu trước phép/Ô Uế | **Trọng Lực Trấn**: 60 giây toàn sân, kéo mọi người xuống đất; phá 4 Trụ để hủy sớm |
| II | **Bạch Từ Âm** · Đạo · 78 | Quang Môn | P1: trận thánh quang đối nghịch (xóa buff người chơi, buff ảnh phụ). P2: cánh đen, thiên phạt vòng tròn, hồi máu Ảnh Thánh. P3: miễn vật lý → chỉ phép/Thần Thức đánh (ép party có phép); nàng thật lòng, thoại gây Tâm Ma nhẹ | **Thiên Lao Kết Giới**: mái vòm 30 giây, ai ở trên không bị sét kéo xuống; hủy khi giết Ảnh Thánh chủ trì |
| III | **Lăng Uyên** · Kiếm · 86 | Phong Môn | P1: Kiếm Vũ ngàn thanh (né). P2: 4 phân thân kiếm, đổi mục tiêu. P3 (<25%): **Nhất Kiếm** — chặn bằng Trận Nhãn/khiên. Lịch thiệp, cho người chơi 1 lần "xin lỗi/đấu công bằng" (dừng 10 giây nếu cả party thu kiếm) | **Kiếm Ý Cấm Không**: 15 giây; bay = bị Kiếm Trận chém +100% sát thương; xuống đất an toàn |
| IV | **Cơ Vô Tận** · Ma · 98 (mạnh nhất) | Huyết Môn | P1: Hấp Huyết, tăng theo số mạng thủ. P2: Tâm Ma lan, đủ Tâm Ma → bị điều khiển tạm. P3: hấp thụ quái nhỏ trong sân. Ma tu người chơi không thể Hấp Thu hắn (canon §3) | **Huyết Xích**: 20 giây (CD 60), xích kéo mọi người bay xuống; cắt xích bằng sát thương tập trung vào cột xích |

### 4.5 Boss cuối — Nguyên Thủy Tịch Diệt (Chủ Hư Không, cấp 100, k=100)

Giữ cơ chế v1: P1 Hiện Hình (ảnh CHG I–IV luân phiên, dùng 4 chiêu như bảng 4.4) → P2 Nuốt Chửng → P3 Phản Phệ (debuff theo đạo) → P4 Hư Vô Vĩnh Hằng (đếm ngược 90 giây). Khóa bay: P2 "Hố Đen" kéo mọi vật thể xuống rìa sân; **bay vẫn dùng được để tránh hố 5 giây** rồi bị kéo. Khuyến nghị 6–10 người. Loot: Hạt Bản Nguyên, Hồn Cốt Kim hiếm, Thiên Cơ Phù ×3. Singleplayer: Triệu Hồn Hóa Thân.

### 4.6 Giáng Lâm, Hư Sào (hang ổ), manh mối III/IV, Tuyến truyện vs Tự do

**(a) Giáng Lâm = sự kiện ngẫu nhiên**

| Hạng mục | CHG I | CHG II | CHG III | CHG IV |
|----------|-------|--------|---------|--------|
| Điều kiện mở khóa trạng thái | Luôn (sau XT ≥ 20) | XT ≥ 35 **và** đã hạ I ít nhất 1 lần **hoặc** có ≥ 3 Văn Kiện chuỗi B | **Cờ "Lăng Uyên lộ diện"** (đủ 4 Dấu Vết III) và XT ≥ 50 | **Cờ "Cơ Vô Tận lộ diện"** (đủ 5 Dấu Vết IV) và XT ≥ 65 |
| Có ≥1 người chơi online cấp ≥ | 52 | 62 | 70 | 80 |
| Xác suất cơ sở / ngày game (kiểm tra lúc bình minh) | 3% | 2% | 1,5% | 1% |
| Công thức | `p = p_cơ_sở × (1 + XT/50)` (XT=50 → ×2; XT=100 → ×3) | | | |
| Kỳ vọng chờ @XT=50 | ≈ 17 ngày game ≈ 5,6 giờ thực | ≈ 25 ≈ 8,3 giờ | ≈ 33 ≈ 11 giờ | ≈ 50 ≈ 17 giờ |
| Cooldown riêng sau mỗi giáng lâm (thắng/thua) | 30 ngày game | 40 | 50 | 60 |
| Khóa toàn cục | Tại một thời điểm chỉ 1 CHG giáng lâm; sau mỗi sự kiện 15 ngày game không CHG nào khác | | | |

(1 ngày game = 20 phút thực.) Hiển thị: bầu trời đổi màu, chat toàn cục, 5 phút đếm ngược (10 phút nếu kích hoạt sớm), arena bán kính 150 block ở tâm Lõi Xâm Thực/Khu vực nguy hiểm trong vòng của CHG (I: V3–V4, II: V4, III: V4–V5, IV: V5). Hiện diện 30 phút; không ai hạ → rút về Hư Sào, XT quanh arena +5%, cooldown vẫn tính. Giáng Lâm không tấn công thành (khác tập kích); mục tiêu là điểm Xâm Thực.

**(b) Hư Sào cố định theo seed, gặp ngẫu nhiên hiếm**

| Hạng mục | Thiết kế |
|----------|----------|
| Vị trí | Luật đặt ở 1.6; vùng đặc thù S1: I Viêm Sơn Hoang Nguyên, II Kim Sa Cổ Địa, III Tuyết Kiếm Lĩnh, IV Huyết Trạch. Cố định cả đời world |
| Trạng thái | **Ngủ** mặc định: cổng đóng, không marker. **Thức tỉnh** khi (i) lộ bằng Văn Kiện (§3.6, 3 văn kiện) hoặc (ii) trạng thái mở khóa của III/IV (đủ Dấu Vết) hoặc (iii) rơi vào "gặp ngẫu nhiên" |
| Gặp ngẫu nhiên | Vùng Cảnh Báo bán kính 400 quanh Hư Sào (≈ 0,5 M block²; 0,1% diện tích vòng 5): mỗi 5 phút ở trong, p = 1,5% (+0,5%/người thêm, tối đa 3%) → "Hư Sào Thức Tỉnh" (rung 60 giây, rời được; CHG không đuổi theo, thức tỉnh 30 phút) |
| Bên trong | Lõi Xâm Thực lớn (1.200 trần); arena 150 block; mini-boss/Hư Tu hộ vệ theo đạo CHG |
| Thấy từ xa | Không có cột khói; chỉ dấu hiệu môi trường (nham thạch dâng, cát cuộn, tuyết đỏ, sương máu) trong 600 block |

**(c) Manh mối III/IV (Dấu Vết)**

| Nguồn | Dấu Vết | Tỉ lệ |
|-------|---------|-------|
| Hạ CHG I Thạch Hống | 2 Dấu Vết III ("Kiếm Ngân") lần đầu (100%); lần sau 30% × 1 | cá nhân |
| Hạ CHG II Bạch Từ Âm | 2 Dấu Vết IV ("Huyết Ấn Hư") lần đầu; lần sau 30% × 1 | cá nhân |
| Văn Kiện **A6 / B6** (Đại Sứ, S1: B6 gợi hướng III–IV) | mỗi tờ đọc xong cho +1 cả hai loại | khi đọc lần đầu |
| Sự kiện cấp cao | Phòng cuối Bí Cảnh Hắc 12% / Hồng 8% (1 mảnh, III hoặc IV ngẫu nhiên); Hư Long 10%; Xích Quỷ 10%; Hư Hoàng Điện 20%; thắng tập kích Địa 5% / Thiên 8% | |
| Tuyến truyện | Hồi 4–6 cấp 1–2 mảnh mỗi mốc (S1) | theo kịch bản |
| Ngưỡng | **III: 4 Dấu Vết** → lộ Hư Sào III + cờ "lộ diện" · **IV: 5** → idem. Kích hoạt sớm: III 6 · IV 8 | |

**(d) Tuyến truyện vs Tự do** [GIẢ ĐỊNH: chọn lúc tạo world, admin đổi được; mỗi world một chế độ]

| | Theo cốt truyện | Tự do |
|---|-----------------|-------|
| Kích hoạt Giáng Lâm | **Theo cờ chương** (S1 Hồi 3→CHG I; Hồi 4/5→II; Hồi 6→III; Hồi 7→IV): sau khi cờ bật, giáng lâm tự diễn trong ≤ 3 ngày game *hoặc* người chơi kích sớm; **tắt giáng lâm ngẫu nhiên** của CHG chưa tới chương | Ngẫu nhiên theo (a); không cờ chương |
| Hang ổ | NPC cốt truyện chỉ vị trí sau khi người chơi nộp ≥ 2 Văn Kiện (thay 3); cutscene hé lore ngay khi vào Hư Sào | Chỉ qua Văn Kiện (3), Dấu Vết, hoặc gặp ngẫu nhiên |
| Manh mối III/IV | Cấp theo chương (đủ ngưỡng đến lúc cần) + drop thường | Chỉ drop/sự kiện — có thể "bỏ qua" III/IV tới khi đủ |
| Thứ tự | Gần như tuyến tính I→II→III→IV | Bất kỳ thứ tự mà điều kiện (a) cho phép |
| Mảnh Thiên Môn | Như nhau | Như nhau |

---

## 5. Sự Kiện

### 5.1 Bí Cảnh mở (giữ v1)

| Cấp nguy hiểm | Màu | Cấp vào | Thời gian | Số người | Thưởng tiêu biểu |
|---------------|-----|---------|-----------|----------|------------------|
| Bạch | Trắng | 1–25 | 20' | 1–4 | Hồn Tinh Bạch ×3–5, linh dược |
| Hoàng | Vàng | 15–45 | 25' | 1–5 | Hoàng ×2–4, Đá Tẩy Luyện ×2, truyền thừa |
| Tử | Tím | 35–65 | 30' | 2–6 | Tử, Hồn Cốt Hoàng, Thiên Cơ Phù 10% |
| Hắc | Đen | 55–85 | 40' | 3–8 | Hắc, Hồn Cốt Tử, 12% Dấu Vết |
| Hồng | Đỏ | 75–100 | 45' | 4–10 | Hồng, Hồn Cốt Hắc/Hồng, 8% Dấu Vết |

Giới hạn cấp cứng (ngoài khoảng → bị đẩy ra sau 10 giây). Tần suất: singleplayer 1/2 giờ chơi; server 1/45 phút. Xác suất loại: Bạch 40 · Hoàng 28 · Tử 18 · Hắc 10 · Hồng 4. Cơ duyên bên trong: Thiên Tài Địa Bảo, Truyền Thừa, Linh Mạch Thất, Nghịch Cảnh. Cổ Tiên Thành: Cổng Bí Cảnh sinh ×2 trong 600 block.

### 5.2 Boss thế giới — xem §4.2.

### 5.3 Quân Đoàn Hư Không tập kích (cập nhật v2)

**Phạm vi**: 10 thành và 7 tông (trừ Ma Môn ẩn nếu chưa lộ); không tập kích thôn (trừ sự kiện hiến tế của Sứ Đồ, §3.6).

**Đồng hồ tập kích — theo Xâm Thực, có trần**

```
I        = chỉ số quanh thành (1.5), 0..1
T(I)     = 12h − 9,5h × min(I / 0,6, 1)        // giờ ONLINE ĐỦ ĐIỀU KIỆN giữa 2 đợt: 12h (I=0) → 2,5h (I ≥ 0,6)
Đồng hồ  = chỉ tăng khi "đủ điều kiện online"
```

| Trần cứng | Giá trị |
|-----------|---------|
| Khoảng cách tối thiểu 2 đợt cùng thành | **≥ 90 phút online** (kể cả khi T nhỏ hơn) |
| Số đợt tối đa/thành/ngày thực | **3** |
| Đồng thời toàn world | Server ≤ 2 đợt; singleplayer 1 |
| Sau thắng | Đồng hồ +25% T (thưởng) |
| Tối đa quái cùng lúc | Nhân 60 · Địa 90 · Thiên 120 (hàng đợi sóng) |
| Mỗi người chơi | ≤ 1 tập kích cùng lúc |

**Đủ điều kiện online** (canon §8): ≥ 1 người chơi (a) **là cư dân/chức vị của thành đó**, đang online — đồng hồ ×1; hoặc (b) *không thuộc thành nhưng trong 300 block* — đồng hồ ×0,5 và quy mô sóng ×0,5. Không ai thuộc (a)/(b) → **hoãn** (đồng hồ dừng; lưu tối đa 1 đợt; khi có người đủ điều kiện: ân hạn 10 phút sau đăng nhập, cảnh báo T-10). Thành NPC không người → không bị tập kích (không thất thủ âm thầm).

**Cảnh báo/quy mô**: T-10' chiêng; T-3' trời tím, đường quân hiện bản đồ; offline xem nhật ký. Quy mô theo cấp thành: Nhân 5 sóng, Địa 6, Thiên 7 (sóng thêm = Đại quân + khe không gian thả Elite).

| Sóng (Nhân) | Thành phần | Mục tiêu |
|-------------|------------|----------|
| 1 | Hư Hóa ×30–50 (+ lỗ trận nếu có gian tế chưa bắt) | Giữ tường |
| 2 | Hư Hóa + Hư Tu ×15–25 | Bảo vệ Trận Nhãn |
| 3 | Quái gốc Hư Không ×10 + Elite | AoE/khống chế |
| 4 | **Boss tập kích** + cận vệ (+ **Dạ Kiếm/Hư Chú Sư** nếu thành có nội gián) | Hạ boss nhanh |
| 5 | Đại quân + khe thả thêm | Giữ Tâm Thành |

Thắng: giữ Tâm Thành & Trận Nhãn đến hết giờ hoặc giết Hư Tướng. Thua: Trận Nhãn bị phá **và** Tâm Thành mất > 2 phút, hoặc toàn bộ NPC phòng thủ gục.

**Vai trò chức vị trong phòng thủ** (khung; chi tiết quyền ở S6)

| Chức vị | Vai trò trong tập kích |
|---------|-----------------------|
| Cư Dân | Chiến đấu, sửa tường cơ bản; nhận thưởng cơ bản; đủ điều kiện để đồng hồ chạy |
| Hộ Vệ | Chỉ huy 1 NPC lính, đặt trạm gác; **Tra Hỏi gian tế** (§3.6) |
| Đội Trưởng | Tiểu đội 5 NPC; chọn tuyến phòng thủ/cổng; ưu tiên mục tiêu |
| Thống Lĩnh | Điều khiển toàn bộ NPC quân thành (20–60); kích hoạt **Tử Thủ** (+20% giáp NPC, 60 giây, 1 lần/đợt); **hoãn tối đa +10' mỗi ngày** |
| Phó Thành Chủ | Quản Kho Quân Nhu (phân phối vật liệu), chỉnh **Mức Cảnh Giới** (trước đợt: tăng NPC canh gác) |
| Thành Chủ | **Bế Thành** (đóng cổng, +50% Trận Nhãn trong 3 phút, 1 lần/ngày); quyết định tái chiếm (5.5) |

Hậu quả: Thắng → Hồn Tinh/Đá Tẩy Luyện, Danh Vọng +, giảm Xâm Thực quanh thành 150 block, 5% Dấu Vết (§4.6). Thua → **Phế Tích Hư Hóa**: NPC chết/tản, Ấn Hồn Đài vô hiệu, chợ đóng, Xâm Thực bao quanh, mất Danh Vọng; **Kho riêng người chơi được giữ**; mọi chức vị tạm đình chỉ.

### 5.4 Singleplayer vs Server

| Khía cạnh | Singleplayer | Server |
|-----------|--------------|--------|
| Đồng hồ | Cùng công thức §5.3; người chơi là cư dân duy nhất | Theo thành, tách đồng hồ |
| Offline | Thế giới đóng băng | Hoãn khi không ai đủ điều kiện |
| Quy mô sóng | 1–2 người + NPC mạnh | Theo số người online gần thành |
| Mất thành | Tái chiếm dễ hơn (nguyên liệu ×0,7) | Tái chiếm bình thường; hardcore tuỳ chọn (không tái chiếm) |
| Bí Cảnh | 1 instance | ≤ 3 instance |

### 5.5 Tái chiếm Phế Tích Hư Hóa (giữ v1, bổ sung chức vị)

| Bước | Nội dung |
|------|----------|
| 0 (mới) | **Phát Lệnh Tái Chiếm**: cần 1 người từng có chức vị **≥ Hộ Vệ** của thành đó, hoặc nhóm ≥ 3 cư dân cũ. **Thành Chủ Lưu Vong** có ưu tiên 7 ngày thực; hết hạn ai đủ điều kiện cũng được. [ĐỀ XUẤT, S6 hiệu chỉnh] |
| 1. Trinh sát | Tìm Trận Nhãn cũ (hư hỏng) |
| 2. Dọn Xâm Thực | ≥ 3 Tịnh Thạch quanh Tâm Thành, diệt quái chiếm đóng (Quản Gia Phế Tích, hoặc Thượng Sứ Dạ Điện — rơi Văn Kiện ×2) |
| 3. Sửa Trận Nhãn | 10 Đá Tẩy Luyện + 5 Hồn Tinh Tử+ + chìa tái chiếm |
| 4. Giữ 1 đợt "Thử Lửa" | Sóng 2 phút |
| 5. Phục hồi | Ấn Hồn Đài bật lại, NPC về dần (24 giờ game), Danh Vọng thấp 3 ngày. **Chức vị**: người dẫn đầu chiến dịch nhận chức tạm Đội Trưởng; chức cũ khôi phục dần theo cống hiến (S6); Thành Chủ Lưu Vong lấy lại ghế nếu vẫn tham gia |

Chi phí tái chiếm cố tình cao hơn phòng thủ thành công.

---

## 6. Nhiệm Vụ & Cơ Duyên (giữ v1, thêm 2 dòng)

| Loại | Nguồn | Nội dung | Thưởng | Lặp |
|------|-------|----------|--------|-----|
| Chính tuyến | Cốt truyện (S1) | Tìm 4 Mảnh, Khảo Hạch | Mốc cảnh giới | 1 lần |
| Tông môn | Sư phụ | Giao nộp, thanh trừ Hư Tu, **bắt gian tế Dạ Điện** | Cống hiến | Ngày/tuần |
| Thành | Bảng nhiệm vụ | Hộ tống, linh dược, phòng thủ | Tiền, Hồn Tinh | Ngày |
| Truy nã | Bảng truy nã | Săn Elite/Hư Tu, Ma Tu đối địch, **Sứ Đồ cấp cao** | Thưởng cao | Luân phiên |
| Phụ (Cơ Duyên) | NPC ngẫu nhiên | Chuỗi nhỏ 3 bước | Vật phẩm, tri thức | Ngẫu nhiên |

**Nhiệm vụ nhập môn** (cấp 1–9) gánh phần lớn EXP khởi đầu do vanilla m=1,5 vẫn chậm (R4).

| Cơ duyên | Tỉ lệ/30 phút (vòng 2–4) | Thưởng |
|----------|--------------------------|--------|
| Động Phủ Cổ | 4% | Hồn Tinh Tử, Hồn Cốt, kỹ năng |
| Linh Dược | 18% | Thuốc EXP, hồi Tâm Ma |
| Truyền Thừa | 2% (3% tư chất ≥ A) | Kỹ năng đạo, Thiên Cơ Phù |
| Gặp Cao Nhân | 6% | Buff / đổi nhiệm vụ / **chỉ đường thành-tông-Bí Cảnh** |
| Nghịch Thiên | 1% | Hồng 1 viên hoặc mất đồ |

SSS ×1,3; Con Hoang +20% Linh Dược/Động Phủ; Linh Mạch Thánh Địa ×2; pity: sau 3 giờ không có cơ duyên → đảm bảo 1.

---

## 7. Tầng Thế Giới II (sau khi kết thúc)

Mở sau **một trong hai kết thúc** (Phong Ấn / Hấp Thụ). Khảo Hạch là **cá nhân** (tối đa 3 hộ pháp) và lựa chọn kết thúc là của **từng người**, ghi vào **Bia Công Trạng** (R18). Tầng II là **dimension mới** (R18). Hấp Thụ để lại hiệu ứng Hư Hóa **chỉ ngoại hình**, không debuff. Mở ở cấp world (server: khi bất kỳ ai hoàn thành); người chơi khác vào được khi cấp ≥ 80. Tên tạm theo S1: **Thiên Khuyết Giới** (Phong Ấn) · **Hư Giới Dung** (Hấp Thụ).

| Hạng mục | Chung | Phong Ấn — Thiên Khuyết Giới | Hấp Thụ — Hư Giới Dung |
|----------|-------|------------------------------|-------------------------|
| Chiều | Dimension riêng `hukhong:tang_hai`, border bán kính 15.000, cùng cách chia 5 vòng nhưng seed phụ; cổng ở Thiên Môn Đài; quái cấp **85–125** | Sáng, đền cổ lơ lửng, đảo/đền trên mây, biome Kim-Trắng | Tím-đen, đất lổn nhổn, biome vỡ vụn, "vết nứt hấp thụ" |
| Overworld (Tầng I) | Vẫn chơi được, còn thành/tông | **Xâm Thực biến mất dần** (Lõi không sinh mới, Lõi còn lại nhanh chóng tàn; XT → 0 sau ≈ 3 ngày thực); Phế Tích phục hồi dần; tập kích **dừng**; Cứ Điểm Dạ Điện tắt | Xâm Thực **dừng ở mức hiện tại**, không lan, không thanh tẩy được hoàn toàn; tập kích Quân Đoàn dừng nhưng NPC thái độ sợ/ngờ (S1); Sứ Đồ biến mất |
| Quái | Mạnh hơn (HP ×1,5, Công ×1,3 so với cấp tương đương); EXP/loot dùng cùng công thức với w+30% | **Cổ Linh, Thần Tàn** (hệ thánh, kháng Ô Uế, yếu bạo lực vật lý ngắn hạn); Elite tiền tố "Thánh Quang" | **Hư Sinh thuần hóa** (đồng minh yếu có thể "thuần phục" 1 con; Hư Hóa kháng phép), Elite tiền tố "Hư Lực" |
| Sự kiện thay Tập kích | Đợt sự kiện theo giờ online giống §5.3 nhưng tên khác | **Liệt Ấn**: khe nứt phong ấn mở, phải giữ/gia cố **Ấn Trụ** trong 5 phút (hợp tác, bảo vệ) | **Hư Triều**: sóng Hư Lực tràn, người chơi có "Hư Lực" nhận buff ngẫu nhiên nhưng tăng Hư Hóa; thưởng cao, rủi ro cao |
| Boss | **Tàn Ảnh CHG I–IV** (cấp +20; loot Hồn Cốt Kim 3%) rải ở 4 vòng, HP_gốc k=90 | Boss đáy: **Dạ Khuyết** nếu chưa hạ (bị nhốt ở khe sâu nhất) | Boss: Dạ Khuyết "xin ở cùng" (nếu chưa hạ) – chiến đấu giành lại Hư Lực |
| Loot | Hồn Tinh Hồng thường gặp; Đá Tẩy Luyện nâng cấp | Vật liệu thánh (Phong Ấn Tinh, Ấn Văn) | Vật liệu Hư (Hư Lực Tinh); **Tàn Dạ** nếu hạ Dạ Khuyết ở Tầng I |
| Cơ duyên/Bí Cảnh | Bí Cảnh Hồng ×3 tần suất | Đền cổ: truyền thừa thánh | Vết nứt: truyền thừa Hư |
| Xâm Thực ở Tầng II | — | Không có | Thay bằng "vết nứt hấp thụ" (cục bộ, không lan) |

Ngoài vòng 100: S3 quyết EXP sau 100 (đề xuất: Tầng II cho "Thần Đạo Ngộ" thay EXP). Lore: S1 §8.

---

## 8. Câu hỏi cho User

Không còn câu hỏi mở. Đã phán quyết (PLAN-v2 §14): kho Văn Kiện chung toàn server (R19); Tầng II là dimension mới (R18); EXP vanilla cấp 1–9 m=1,5 (R4).

---

## Đề xuất đổi canon

1. **§9**: ghi "Hư Sào theo vùng đặc thù S1" và bỏ ý "Pháo Đài đặt theo la bàn" (S4 v1); thêm **chỉ số XT** (1.5) làm đơn vị điều kiện Giáng Lâm.
2. **§9**: bổ sung ngưỡng Văn Kiện (1/2/3/6) và Dấu Vết III 4 / IV 5; Mảnh Thiên Môn **cá nhân** mỗi người tham chiến.
3. **§10**: thêm "Điểm Đóng Góp" tính cả hồi máu/buff (Đạo tu) và trần EXP boss 2 E(L)/trận.
4. **§8**: định nghĩa "online đủ điều kiện" (cư dân ×1; người ở gần 300 block ×0,5) và trần cứng 90 phút/3 đợt/ngày.
5. Đề xuất thêm vào S5: biome `viem_son`, `kim_sa_co_dia`, `tuyet_kiem_linh`, `huyet_trach`, `hu_hoa`, `linh_mach`; block Tịnh Thạch; tag `hukhong:spawner_born`; `FlightLock` capability nguồn boss; Lộ Bia + đồ thị Quan Đạo.

---

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | S4-world.md (PLAN-v1 canon, border không giới hạn, Pháo Đài la bàn) | ✅ Superseded |
| v2 | S4-world.md (PLAN-v2 canon, border 15.000, Sứ Đồ, CHG 67/78/86/98, Tầng II) | Superseded bởi v2.1 |
| v2.1 | S4-world.md (this file): R3, R4, R15, R16, R18, R19 | ✅ Current |

**Phụ thuộc**: PLAN-v2 canon (§1.1, §7–§10), S1-lore v2 (tên thành/Sứ Đồ/Văn Kiện/Hư Sào), S2-paths (HP/DPS), S3-cores (E(L), EXP), S5-tech (SavedData, biome, FlightLock), S6-society (chức vị, tông môn).
