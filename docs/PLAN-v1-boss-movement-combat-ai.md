# PLAN-v1: Hệ thống di chuyển & chiến đấu của boss (kiểu Elden Ring / Better Combat)

**Created**: 2026-11-10
**Last Updated**: 2026-11-10 — Bản kế hoạch đầu, chờ người chơi duyệt
**Status**: ⬜ Chờ duyệt
**Phạm vi**: 13 boss đã có bộ chiêu riêng (NTTD 2 dạng, Dạ Khuyết, CHG I–IV, Hư Vương, Sa Hạt Vương, Băng Nhãn, Độc Mẫu, Sứ Đồ). 8 dạng Hư Vương + boss Bí Cảnh dùng lại khung này ở đợt sau.

---

## Key Findings (hiện trạng, đã kiểm tra code)

| # | Hiện trạng | Hệ quả |
|---|-----------|--------|
| 1 | Boss chỉ có 2 trạng thái: **đi thẳng tới người chơi** hoặc **đứng tung chiêu** | Không lùi, không đi vòng, không né — khác hẳn boss Elden Ring |
| 2 | Chiêu lớn đã có vạch cảnh báo + cutscene, nhưng **giữa các chiêu boss không đánh thường** | Nhịp trận trống, người chơi đứng chém tự do |
| 3 | Boss xoay mặt tức thì theo người chơi (kiểu Minecraft) | Đòn trông "dính" mục tiêu, không có cảm giác cam kết đòn |
| 4 | Animation: hầu hết boss chỉ có `idle/walk/attack/skill_a/b/c/phase/death`. Riêng Dạ Khuyết có thêm `thrust/flurry/rise/dive`; NTTD dạng 1 có `slam/combo/teleport/pillars/ult/transform` | Cần thêm animation di chuyển (lùi, lướt, đi ngang, nhảy) cho từng model |

---

## 1. Bộ khung chung — "Boss Combat Brain"

Một lớp dùng chung (`boss/ai/BossBrain` + `BossMovement` + `BossProfile`), mỗi boss chỉ khai báo **hồ sơ** (profile) của mình. Hai phần chính:

### 1.1 Máy trạng thái chiến đấu

```
            ┌──────────┐  thấy người chơi   ┌──────────┐
            │  QUAN SÁT│ ─────────────────► │ ĐI VÒNG  │◄────────┐
            └──────────┘                    └────┬─────┘         │
                                                 │ chọn đòn       │ xong đòn / hồi
              ┌──────────────┬───────────────────┼──────────┐    │
              ▼              ▼                   ▼          ▼    │
         ┌────────┐    ┌──────────┐        ┌──────────┐ ┌──────┐ │
         │ ÁP SÁT │    │ CHUỖI ĐÒN│        │ CHIÊU LỚN│ │ NÉ / │ │
         │(lao,   │──► │ THƯỜNG   │──────► │(đã có)   │ │ LÙI  │─┘
         │ nhảy)  │    │ 2–4 nhát │        └──────────┘ └──────┘
         └────────┘    └──────────┘
                ▲ bị đánh trúng / người chơi hồi máu → PHẢN ỨNG (né, phản đòn, trừng phạt)
```

| Trạng thái | Làm gì |
|-----------|--------|
| **Đi vòng (strafe)** | Đi ngang quanh người chơi, mặt hướng vào, giữ khoảng cách "ưa thích" của boss; đổi chiều ngẫu nhiên 2–5 giây/lần |
| **Áp sát (gap-close)** | Người chơi quá xa → lao/nhảy/dịch chuyển tới (tùy boss) |
| **Chuỗi đòn thường** | 2–4 nhát cận chiến, mỗi nhát có thời gian vung khác nhau (nhanh / chậm / **trễ cố ý**) |
| **Né / lùi** | Lùi bước, nhảy lùi, lướt ngang — có thời gian hồi để không né vô hạn |
| **Phản ứng** | Bị đánh liên tục → né hoặc phản đòn; người chơi uống thuốc/hồi máu → lao vào trừng phạt; người chơi chạy xa → đuổi |

**Chọn đòn theo khoảng cách**: mỗi đòn khai báo tầm `gần (0–4) / vừa (4–10) / xa (10+)`, trọng số, thời gian hồi, pha mở khóa. Bộ chọn có trọng số + chống lặp (không dùng 1 đòn quá 2 lần liên tiếp).

### 1.2 "Mượt như Better Combat" — các quy tắc chuyển động

| # | Quy tắc | Cụ thể |
|---|---------|--------|
| M1 | **Tăng/giảm tốc** | Không đổi vận tốc tức thì; tăng tốc trong 3–6 tick, phanh trong 3–5 tick |
| M2 | **Giới hạn tốc độ xoay** | Mỗi boss có tốc độ xoay riêng (độ/tick). Boss nặng xoay chậm → có thể lách ra sau lưng |
| M3 | **Bám mục tiêu theo giai đoạn đòn** | Lúc lấy đà: bám người chơi. Lúc vung: **khóa hướng** (không xoay theo nữa). Lúc thu đòn: đứng yên → cửa sổ để người chơi phản công |
| M4 | **Chuyển động theo animation (root motion giả)** | Mỗi đòn có đường cong dịch chuyển: lao tới khi chém, lùi khi thu kiếm — boss tiến theo nhát chém thay vì đứng trượt |
| M5 | **Chuyển animation có pha trộn** | Đi → đứng → đánh blend 3–5 tick (GeckoLib transition), không giật khung |
| M6 | **Khựng khi trúng đòn (hit-stop)** | Đòn nặng trúng: boss + nạn nhân khựng 2–4 tick, camera rung nhẹ |
| M7 | **Chuỗi đòn có thể rẽ nhánh** | Sau nhát 2, nếu người chơi lùi → nối nhát lao tới; nếu né sang ngang → nối nhát quét |
| M8 | **Chân chạm đất** | Bước đi/lướt kèm bụi đất, âm thanh bước chân theo trọng lượng boss |
| M9 | **Cảnh báo theo nhịp** | Đòn thường cảnh báo bằng **ánh lóe trên vũ khí** (không vẽ vòng đỏ như chiêu lớn); đòn không thể đỡ có ánh đỏ |

### 1.3 Thông số chung (mỗi boss chỉnh trong profile)

`khoảng cách ưa thích`, `tốc độ đi vòng`, `tốc độ xoay`, `tỉ lệ né theo pha`, `thời gian hồi né`, `độ hung hăng` (khoảng nghỉ giữa các đòn), `kiểu áp sát`, `có phản đòn không`, `trọng lượng` (ảnh hưởng hit-stop, bụi, âm thanh).

---

## 2. Dạ Khuyết (kiểu Malenia) — CHI TIẾT

**Tính cách chiến đấu**: nhanh nhất game, liên tục ép người chơi, **hút máu** nên càng đánh trúng càng khó giết. Không bao giờ đứng yên quá 1 giây.

### 2.1 Hồ sơ

| Thông số | Pha 1 | Pha 2 (có cánh) | Pha 3 (trăng máu) |
|----------|-------|-----------------|-------------------|
| Khoảng cách ưa thích | 3–5 ô | 4–8 ô | 2–6 ô |
| Tốc độ xoay | nhanh (24°/tick) | 28°/tick | 32°/tick |
| Tỉ lệ né khi bị đánh | 30% | 45% | 55% |
| Nghỉ giữa các đòn | 14–22 tick | 10–18 | 6–14 |
| Áp sát | lướt thấp | lướt + bay ngắn | lướt chớp nhoáng để lại vệt máu |

### 2.2 Bộ di chuyển

| Kiểu | Mô tả | Animation |
|------|-------|-----------|
| **Đi vòng ngân kiếm** | Đi ngang chậm, kiếm chĩa xuống, mắt dõi theo — thỉnh thoảng đổi chiều đột ngột | `strafe_l`, `strafe_r` (mới) |
| **Lướt áp sát** | Lướt thấp 6–10 ô, kết thúc bằng 1 nhát đâm (`thrust`) | `dash` (mới) + `thrust` |
| **Lùi nhảy (backflip)** | Bị áp sát khi đang hồi → nhảy lùi 4–5 ô, đáp xuống rồi bắn Nguyệt Luân ngay | `backflip` (mới) |
| **Lướt ngang né** | Người chơi vung vũ khí → lướt ngang 3 ô, ra sau lưng nếu được | `sidestep_l/r` (mới) |
| **Bay lượn (pha 2+)** | Bay vòng trên đầu 1–2 giây rồi lao xuống (`dive`) | `hover` (mới) + `rise`/`dive` |
| **Lướt máu (pha 3)** | Biến mất thành vệt máu, hiện ra sau lưng người chơi | `blink` (mới) |

### 2.3 Chuỗi đòn thường (mới — xen giữa các chiêu lớn đã có)

| Chuỗi | Diễn biến | Cách né |
|-------|-----------|---------|
| **Tam Nguyệt Trảm** | chém ngang → chém chéo → **xoay người chém trễ 10 tick** | đừng né sớm nhát 3 |
| **Đâm – Rút – Đâm** | đâm nhanh, rút kiếm lùi 1 bước, đâm lại ngay | né về phía sau 2 lần |
| **Kiếm Vũ Liên Hoàn** | 5–7 nhát cực nhanh (dùng `flurry`), mỗi nhát trúng hút máu | chạy ngang hoặc đỡ |
| **Phản Nguyệt** | **phản đòn**: bị đánh 3 lần trong 2 giây → gạt kiếm, lướt ra sau, chém | ngừng đánh khi thấy ánh trăng lóe trên kiếm |
| **Nhảy chém** | nhảy lên chém xuống, đáp đất nối 1 nhát quét | lùi xa ra rồi đánh lúc đáp |

**Nhánh rẽ**: sau nhát 2 Tam Nguyệt, nếu người chơi lùi > 4 ô → nối **Đâm Xuyên** (lao thẳng), nếu né ngang → nối **quét 270°**.

### 2.4 Phản ứng đặc biệt

- **Người chơi uống thuốc/hồi máu** → lập tức lướt tới đâm (Malenia-style punish), cảnh báo bằng tiếng kiếm ngân.
- **Bị đánh choáng/hết thế** → không quỵ, mà lùi nhảy + phục hồi (nàng "khó bắt nạt").
- **Người chơi đứng xa bắn tên/phép** → 60% lướt né ngang + áp sát, 40% bắn Nguyệt Luân đáp trả.
- **Khi máu dưới 30% ở pha 3** → độ hung hăng tối đa, thời gian nghỉ giữa đòn còn 4–8 tick.

### 2.5 Nhịp trận mẫu (30 giây pha 2)

```
0s  đi vòng ─► 2s lướt áp sát + đâm ─► 3s Tam Nguyệt (nhát 3 trễ)
6s  người chơi đánh trả ×3 ─► Phản Nguyệt (gạt + chém sau lưng)
9s  backflip ─► Nguyệt Luân ×3 ─► 12s bay lượn ─► Lao Không (chiêu lớn)
16s đi vòng ─► người chơi uống thuốc ─► 17s lướt trừng phạt
19s Kiếm Vũ Liên Hoàn (hút máu) ─► 23s Thủy Điểu (chiêu lớn) ─► 28s đi vòng
```

---

## 3. Nguyên Thủy Tịch Diệt — CHI TIẾT (2 dạng khác hẳn nhau)

### 3.1 Dạng 1 — Tu sĩ bị Hư Không nhập (kiểu Radagon/Marika)

**Tính cách**: một con người đang giằng co với thứ bên trong. Đòn búa **nặng, chậm, cam kết** — nhưng thỉnh thoảng giật cục, ra đòn nhanh bất thường (ý chí Hư Không chiếm quyền).

| Thông số | Giá trị |
|----------|---------|
| Khoảng cách ưa thích | 3–6 ô |
| Tốc độ xoay | chậm (10°/tick) khi đang vung, nhanh khi đi vòng |
| Tỉ lệ né | 15% (ít né, thay bằng dịch chuyển) |
| Nghỉ giữa đòn | 18–28 tick |

| Kiểu di chuyển | Mô tả | Animation |
|----------------|-------|-----------|
| **Bước nặng** | Đi chậm kéo búa trên đất, để lại vết nứt sáng | `walk` (có) + bụi |
| **Thuấn Hư bộ** | Dịch chuyển ngắn 5–8 ô (hiện có `teleport`) — dùng để áp sát hoặc tránh bị vây | `teleport` (có) |
| **Lùi giằng co** | Ôm đầu lùi 2–3 bước (ý chí người giành lại) → **cửa sổ đánh tự do 1,5s** | `struggle` (mới) |
| **Nhảy búa** | Nhảy cao 6 ô đập xuống (áp sát xa) | `leap` (mới) + `slam` |

| Chuỗi đòn thường | Diễn biến |
|------------------|-----------|
| **Búa Tam Giáng** | quét ngang → bổ dọc → **giật cục: dịch chuyển ra sau lưng bổ lần 3** |
| **Kéo Búa Hất** | kéo búa trên đất 3 ô rồi hất lên (đánh bay) |
| **Song Thể Kích** | 2 nhát: nhát đầu chậm (người), nhát 2 nhanh gấp đôi (Hư Không) — vệt sáng tím báo hiệu nhát nhanh |

**Phản ứng**: bị đánh liên tục → nổ sóng Hư Không đẩy lùi (không né). Người chơi hồi máu → ném búa xoay về phía người chơi.

### 3.2 Dạng 2 — Ý chí Hư Không (kiểu Elden Beast)

**Tính cách**: không còn là người. **Không đi bộ** — trôi lơ lửng, khổng lồ, chậm rãi nhưng chiếm cả sân; không bao giờ để người chơi đứng yên ở xa.

| Thông số | Giá trị |
|----------|---------|
| Khoảng cách ưa thích | 8–16 ô (đánh tầm xa) |
| Tốc độ xoay | rất chậm (6°/tick) → người chơi có thể chạy ra sau |
| Tỉ lệ né | 0% — thay bằng **dịch chuyển xuyên sân** |
| Nghỉ giữa đòn | 20–30 tick |

| Kiểu di chuyển | Mô tả |
|----------------|-------|
| **Trôi vòng** | Lơ lửng cách mặt đất 2–3 ô, trôi theo vòng tròn quanh tâm sân (không quanh người chơi) |
| **Lặn vào Hư Không** | Chìm xuống mặt đất thành vũng ánh sáng → trồi lên ở phía bên kia sân (Elden Beast dive) — khi trồi lên gây sóng nổ |
| **Lao xuyên** | Lao thẳng qua sân với thân mình (đòn di chuyển = đòn đánh), để lại vệt sao |
| **Bị áp sát dưới thân** | Người chơi đứng sát dưới bụng > 3 giây → bay vọt lên cao rồi rơi đè |

| Chuỗi đòn thường | Diễn biến |
|------------------|-----------|
| **Quét Tinh Hà** | đuôi/thân quét 180° rồi quét ngược lại |
| **Ấn Quang** | 3 vòng sáng nổ dưới chân người chơi theo nhịp 1–2–… **trễ** |
| **Cầu Truy Hồn** (đã có) dùng làm đòn tầm xa thường xuyên khi người chơi ở xa |

**Phản ứng**: người chơi chạy ra xa > 20 ô → lặn Hư Không trồi lên cạnh người chơi. Người chơi hồi máu → bắn chùm tia sao.

### 3.3 Nhịp trận mẫu dạng 2 (30 giây)

```
0s trôi vòng ─► 3s Cầu Truy Hồn (tầm xa) ─► 6s Lao Xuyên qua sân
9s Quét Tinh Hà ─► 12s lặn Hư Không ─► 14s trồi lên + sóng nổ
16s Ấn Quang (nhịp trễ) ─► 20s Mưa Tinh Tú (chiêu lớn) ─► 26s trôi vòng
```

---

## 4. Các boss còn lại

| Boss | Tính cách | Đi vòng / khoảng cách | Né / lùi | Áp sát | Chuỗi đòn thường | Phản ứng riêng |
|------|-----------|----------------------|----------|--------|------------------|----------------|
| **CHG I – Thạch Hống** (Godfrey) | Nặng, chậm, lì | Ít đi vòng, đi thẳng từng bước rung đất; 3–5 ô | Không né; bị đánh nhiều → dậm đất đẩy lùi | Nhảy rìu | Rìu 3 nhát (quét–quét–bổ trễ); pha 2: vồ chụp + đấm tay không | Người chơi hồi máu → ném đá |
| **CHG II – Bạch Từ Âm** (Maliketh) | Pha 1 như thú, pha 2 như kiếm sĩ | Pha 1 nhảy chồm khắp sân; pha 2 đi vòng 5–8 ô | Nhảy lùi trên tường/trụ (pha 1); lướt bóng (pha 2) | Vồ từ trên không | Pha 1: cào 4 nhát giật cục; pha 2: hắc kiếm 2 nhát + chém xoay | Bị đánh liên tiếp → bật nhảy lên cao, lao xuống |
| **CHG III – Lăng Uyên** (Morgott) | Chậm rãi, xảo quyệt | Đi vòng chậm 4–7 ô, gậy chống đất | Lùi 1 bước + ném dao | Bước dài 1 nhịp | Gậy 3 nhát **trễ 2 lần**; triệu kiếm giữa chuỗi | Người chơi đứng xa → mưa dao |
| **CHG IV – Cơ Vô Tận** (Mohg) | Uy nghi, tầm dài | Giữ 5–9 ô (tầm đinh ba) | Pha 2 lướt cánh ngắn | Đâm lao tới | Đinh ba: đâm–quét–đâm xoay; vệt lửa máu theo đường đi | Bị áp sát → quét 360° |
| **Hư Vương** (Rykard/Godrick) | Khổng lồ, không đuổi | Gần như đứng yên, xoay chậm; dùng **hút** kéo người chơi lại | Không né | Hút + nhảy rung đất | Rìu quét 2 nhát + dậm; pha 2 tay ghép đánh luân phiên | Người chơi ra sau lưng → quét đuôi/xà thần |
| **Sa Hạt Vương** (Radahn) | Lùi xa bắn, rồi bất ngờ lao | Tầm xa 10–16 ô | Lùi trượt trên cát (để lại vệt bụi) | **Lao trọng lực** 15 ô | Song kiếm 4 nhát (2 trái 2 phải) + đập chéo | Người chơi trốn sau vật cản → cầu trọng lực vòng qua |
| **Băng Nhãn** (Astel/Rennala) | Không đi bộ | Trôi + **dịch chuyển** 3–4 giây/lần, giữ 8–14 ô | Bị áp sát → dịch chuyển ngay | Dịch chuyển ra sau lưng | Gậy băng 2 nhát + nổ băng tại chỗ | Người chơi đuổi theo → để lại mìn băng tại chỗ cũ |
| **Độc Mẫu** | Đứng sau bầy con | Lùi về sau bầy, giữ 8–12 ô | Bị áp sát → phun độc lùi lại | Hầu như không | Cào 2 nhát + phun độc gần | Bầy con chết hết → hung hăng, chuyển sang cận chiến |
| **Sứ Đồ** (Margit) | Khiêu khích, nhịp lừa | Đi vòng 3–6 ô, thỉnh thoảng đứng khiêu khích | Lùi nhảy + ném dao | Bước gậy dài | Gậy 3–4 nhát với **2 nhát trễ**; nhát cuối triệu búa | Người chơi hồi máu → ném dao ngay |

---

## 5. Animation cần thêm (mỗi model)

| Nhóm | Animation | Boss cần |
|------|-----------|---------|
| Đi vòng | `strafe_l`, `strafe_r` | tất cả trừ Hư Vương, Băng Nhãn, NTTD dạng 2 |
| Né / lùi | `backstep`, `sidestep_l/r`, `backflip` | DK, CHG II–IV, Sứ Đồ, Sa Hạt, NTTD 1 |
| Áp sát | `dash`, `leap` | hầu hết |
| Chuỗi đòn | `combo_1..3` (mỗi boss 2–4 chuỗi) | tất cả |
| Đặc biệt | `hover`, `blink` (DK); `struggle` (NTTD 1); `dive_under`, `emerge` (NTTD 2); `taunt` (Sứ Đồ, CHG III) | theo bảng |

Animation viết tay bằng script (như `tools/gen_nttd_form1.py`), có pha trộn chuyển động (M5).

---

## 6. Thứ tự làm & phân công

| Bước | Việc | Agent | Phụ thuộc |
|------|------|-------|-----------|
| B1 | Khung chung: `BossBrain`, `BossMovement` (M1–M9), `BossProfile`, bộ chọn đòn theo tầm, hit-stop, test | 1 Opus | — |
| B2 | Dạ Khuyết (chi tiết §2) + animation | 1 Opus | B1 |
| B3 | NTTD dạng 1 + dạng 2 (§3) + animation | 1 Opus | B1 |
| B4 | CHG I–IV (§4) | 1 Opus | B1 |
| B5 | Hư Vương, Sa Hạt, Băng Nhãn, Độc Mẫu, Sứ Đồ (§4) | 1 Opus | B1 |
| B6 | Gộp, build, gametest, đẩy lên, chép jar | controller | B2–B5 |

B2–B5 chạy song song sau khi B1 xong. Không agent nào mở game client.

## 7. Cách kiểm tra (Definition of Done)

1. Gametest: boss **lùi** khi bị áp sát, **đi vòng** (góc quanh người chơi thay đổi ≥ 90° trong 5 giây), **né** khi bị tấn công (tỉ lệ ≈ thông số ±10%), **không ra khỏi sân** (BossArena), không kẹt tường.
2. Gametest: chuỗi đòn thường chạy hết và nối đúng nhánh rẽ.
3. Gametest: boss khóa hướng khi đang vung (M3).
4. Người chơi xem trong game: độ mượt, độ khó, nhịp — **cần bạn đánh thử** sau khi xong.

## Câu hỏi mở (cần bạn chốt)

1. Độ khó: giữ **sát Elden Ring** (Dạ Khuyết cực khó) hay **giảm 20–30%** cho hợp Minecraft?
2. Phản đòn của Dạ Khuyết (Phản Nguyệt) và trừng phạt khi uống thuốc — giữ hay bỏ?
3. Có cần làm thêm **né cho người chơi** (đã đề xuất trước đó) cùng đợt này không? Boss né được mà người chơi không né được thì hơi thiệt.

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | PLAN-v1-boss-movement-combat-ai.md (this file) | ⬜ Chờ duyệt |
