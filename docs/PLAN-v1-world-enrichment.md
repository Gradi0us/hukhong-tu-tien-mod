# PLAN-v1-world-enrichment — Làm giàu thế giới Hư Không

**Created**: 2026-10-29
**Last Updated**: 2026-10-29 — Bản đầu, theo yêu cầu của người chơi
**Status**: ⬜ Chờ duyệt
**Phạm vi đã chốt**: cả 4 mảng (địa điểm, sự kiện động, sinh vật & hệ sinh thái, NPC sống & xã hội) + **Đấu Giá Hành** (có Hồn Cốt) + **Lịch Thiên Cơ** (xem trong K, dịch chuyển miễn phí tới sự kiện). Quy mô: **rất nhiều, phủ rộng**.
**Liên quan**: REVIEW-v1-tu-tien-mod-implementation-gap.md (gap G13 manh mối, G15 đột kích mọi thành, G10 đan), PLAN-v1-vfx-boss-mob-scaling.md (khung `MobMove`, quái theo vai trò)

---

## 0. Tóm tắt

| # | Ý chính | Vì sao |
|---|---|---|
| 1 | **Dựng 6 khung dữ liệu trước, nội dung sau** | Muốn hơn 60 thứ mới thì mỗi thứ phải là một file JSON + ít code. Viết tay từng thứ sẽ không đủ token và khó bảo trì |
| 2 | **Lịch Thiên Cơ là xương sống** | Sự kiện, đấu giá, boss, thương đoàn đều đăng ký vào lịch. Lịch hiện trong tab K và có nút dịch chuyển miễn phí |
| 3 | **Nối vào hệ có sẵn, không làm lại** | Dùng lại `travel/`, `currency/` (Tử Tinh), `honcot/`, `structures/` (ProcKit + .nbt), `city/` (Market, Board), `events/` (VoidRaid, WorldBoss), `city/ambient/AmbientNpc` |
| 4 | **Sửa 3 gap của REVIEW-v1 trong lúc làm** | Bia đá truyền thừa → manh mối (G13). Thú triều → nhắm mọi thành (G15). Linh thảo / khoáng → nguyên liệu cho đan còn thiếu (G10) |
| 5 | **Khối lượng ước tính** | ~65 nội dung mới + 6 khung ≈ 16–20 đợt agent, tức khoảng 8–10 lượt chạy song song |

---

## 1. Nền tảng: 6 khung dữ liệu (làm trước)

```
            ┌──────────────── LỊCH THIÊN CƠ (F1) ────────────────┐
            │ lịch theo ngày game · báo trước · nút dịch chuyển  │
            └───┬──────────────┬──────────────┬──────────────┬───┘
                │              │              │              │
        Sự kiện (B)     Đấu giá (F5)   Boss / Thú triều   Thương đoàn
                │              │              │              │
   ┌────────────▼──┐  ┌────────▼──────┐  ┌────▼─────────┐  ┌▼────────────┐
   │ F2 Địa điểm   │  │ F5 Kinh tế    │  │ F4 Sinh vật  │  │ F3 NPC sống │
   │ (POI JSON)    │  │ đấu giá, thư  │  │ (JSON loài)  │  │ (lịch sinh  │
   └───────┬───────┘  └───────────────┘  └──────┬───────┘  │ hoạt, thiện │
           │                                    │          │ cảm, quest) │
           └──────────── F6 Tài nguyên (linh thảo, khoáng mạch) ───────┘
```

| Khung | Nội dung | Dữ liệu | Nối vào |
|---|---|---|---|
| **F1 Lịch Thiên Cơ** | Bộ lập lịch sự kiện theo ngày game (1 ngày = 20 phút). Lịch lặp (ngày/tuần) + sự kiện ngẫu nhiên có trọng số. Thông báo trước 2 phút + 30 giây. Đồng bộ toàn server | `data/hukhong/hukhong/events/*.json` (id, loại, chu kỳ, thời lượng, địa điểm hoặc luật chọn, phần thưởng, cấp gợi ý) | Tab **K → "Lịch"**, minimap marker, HUD đếm ngược |
| **F2 Địa điểm (POI)** | Đặt địa điểm theo biome, khoảng cách tối thiểu, đất khô (`DrySites`). Hai kiểu dựng: `.nbt` nhập sẵn hoặc `ProcKit` thủ tục. Có trạng thái: đã khám phá / đã dọn / hồi sinh. Ghi vào bản đồ khi phát hiện | `poi/*.json` (biome, hiếm, cấp, cấu trúc, spawner quái, rương, puzzle, lore) | `structures/StructurePlacer`, `Prebuild` (dựng dần theo chunk, không chặn tạo thế giới), minimap |
| **F3 NPC sống** | Lịch sinh hoạt ngày/đêm (làm việc, ăn, ngủ, tụ tập), nhu cầu đơn giản, **thiện cảm** với người chơi (−100…100), mẫu hội thoại, **sinh nhiệm vụ ngẫu nhiên** (hộ tống, tìm người, giao hàng, săn, thu thập) | `npc_archetypes/*.json`, `quest_templates/*.json` | Mở rộng `AmbientNpc` + `StoryNpc`/`DialogueService`, Hiệp Hội (điểm), bảng thành |
| **F4 Sinh vật** | Họ sinh vật theo biome: thụ động / trung lập / thù địch / tinh anh / linh thú thuần phục / thú cưỡi. Bộ chiêu dùng khung `MobMove` (PLAN-v1 §4.3) | `creatures/*.json` (biome, nhóm, chỉ số theo cấp, chiêu, rơi đồ, thuần phục) | `HuHoa.applyStats`, `ModDrops`, `SpawnRules` |
| **F5 Kinh tế** | **Đấu Giá Hành** (§5), **Phi Kiếm Truyền Thư** (hòm thư để nhận đồ / tiền khi offline), phí giao dịch đốt Tử Tinh để chống lạm phát | SavedData server, cấu hình phí | `currency/Wallet`, `honcot/`, `city/CityMarket` |
| **F6 Tài nguyên** | Linh thảo mọc tự nhiên theo biome / mùa, khoáng mạch linh thạch. Có thời gian tái sinh; chất lượng theo nồng độ linh khí vùng | `resources/*.json` | Đan dược (`alchemy/`), luyện khí, cường hóa |

**Thêm: Mùa (4 mùa × 7 ngày game).** Mùa ảnh hưởng linh thảo nào nở, sự kiện nào xuất hiện, màu lá (shader / ambient). Lịch hiện mùa hiện tại.

**Ngân sách hiệu năng:**
- Địa điểm dựng dần khi chunk được nạp, không thêm thời gian tạo thế giới.
- Tối đa ~40 sinh vật môi trường mỗi người chơi.
- NPC sống chỉ chạy AI đầy đủ trong bán kính 64 block quanh người chơi; ngoài đó "ngủ".

---

## 2. Lịch Thiên Cơ + dịch chuyển miễn phí (yêu cầu chính)

### 2.1 Giao diện (tab K → "Lịch")

```
┌───────────────────────── LỊCH THIÊN CƠ ─────────────────────────┐
│ Mùa Xuân · Ngày 3/7 · 14:20                       [Hôm nay][Tuần]│
├──────────────────────────────────────────────────────────────────┤
│ ● ĐANG DIỄN RA                                                   │
│   ⚡ Linh Khí Triều — Thanh Vân Cốc     còn 06:12   EXP ×1.5      │
│      Cấp gợi ý 20–40                         [Dịch chuyển (0/1)] │
│ ○ SẮP TỚI                                                        │
│   🏮 Đấu Giá Hội — Lưu Ly Cảng          sau 08:40   Hồn Cốt hiếm  │
│   🐺 Thú Triều — Phong Hỏa Thành        sau 21:00   Thủ thành     │
│   👑 Yêu Vương Xuất Sơn — Băng Nguyên   ngày mai    Boss thế giới │
│ ○ TUẦN NÀY                                                       │
│   ⚔ Đại Hội Tông Môn (Ngày 6) · 🐉 Bí Cảnh Đại Khai (Ngày 7)     │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 Luật dịch chuyển miễn phí
- Mỗi sự kiện, mỗi người chơi được **1 lần dịch chuyển miễn phí** tới điểm tập kết ngay ngoài vùng sự kiện, dùng được từ 2 phút trước khi bắt đầu cho tới khi kết thúc.
- Không dùng được khi đang giao chiến (bị đánh trong 10 giây), đang trong Bí Cảnh hoặc Tâm Thức, hay đang hộ tống.
- Có hiệu ứng truyền tống 2 giây, hủy nếu bị đánh. Sau sự kiện có **nút quay về** điểm cũ (1 lần, trong 5 phút).
- Dùng lại `travel/TravelService` (đã có cooldown, kiểm tra an toàn).

### 2.3 Thông báo
- Toast + âm chuông 2 phút và 30 giây trước khi bắt đầu.
- Chat có nút bấm "[Đến ngay]".
- Minimap hiện marker sự kiện có đồng hồ.
- Tùy chọn trong ClientConfig: tắt thông báo theo loại.

---

## 3. Danh mục nội dung (≈65 mục)

Ký hiệu cột Đợt: **W1–W8** (xem §7). Cấp = cấp người chơi gợi ý.

### A. Địa điểm (24)

| # | Tên | Mô tả ngắn | Phần thưởng / ý nghĩa | Cấp | Đợt |
|---|---|---|---|---|---|
| A1 | **Bia Đá Truyền Thừa** | Bia đá rải rác, đọc được đoạn lore + manh mối hang Chấp Hành Giả | Sửa gap manh mối G13; EXP nhỏ | mọi | W2 |
| A2 | **Động Phủ Tiền Bối** | Hang có giải đố (cần gạt, trận đồ ngũ hành), cửa ấn khóa theo đạo | Sách kỹ năng Huyền/Địa phẩm, pháp bảo | 15–60 | W3 |
| A3 | **Cổ Chiến Trường** | Bãi xương, kiếm gãy cắm đất, quân hồn ban đêm | Tàn hồn → Hồn Cốt; vũ khí cổ | 25–50 | W3 |
| A4 | **Thổ Địa Miếu** | Miếu nhỏ ven đường, dâng hương nhận phúc | Buff 20 phút (may mắn rơi đồ / hồi linh) | mọi | W2 |
| A5 | **Linh Tuyền** | Suối nóng linh khí | Ngâm: hồi máu / linh lực nhanh, tu luyện ×1.3 | mọi | W2 |
| A6 | **Khách Điếm / Trạm Dịch** | Trên đường giữa các thành, có tiểu nhị và bảng tin đồn | Điểm dịch chuyển mới, nghe tin đồn sự kiện, ngủ qua đêm | mọi | W2 |
| A7 | **Thôn Nhỏ** (5 biến thể biome) | Dân làng, ruộng linh mễ, trưởng thôn | Nhiệm vụ ngẫu nhiên, mua nguyên liệu rẻ | mọi | W2 |
| A8 | **Tàn Tích Tông Môn** | Dungeon nhiều tầng, boss trưởng lão hóa ma | Bí tịch, đan cổ | 35–70 | W4 |
| A9 | **Phế Tháp Thông Thiên** | Tháp leo 10–30 tầng, mỗi tầng một thử thách | Phần thưởng theo tầng, bảng xếp hạng | 20–100 | W4 |
| A10 | **Đan Lô Cổ** | Lò đan khổng lồ bị bỏ hoang | Luyện đan tại đây +1 phẩm; nguyên liệu hiếm | 30+ | W3 |
| A11 | **Kiếm Trủng** | Mộ kiếm, hàng nghìn kiếm cắm (dành cho Kiếm tu) | Kiếm ý thử thách → vật liệu cường hóa Bản Mệnh | 30+ | W4 |
| A12 | **Ma Uyên** | Vực sâu ma khí (Ma tu) | Ma Công vật liệu; Ác Tính tăng nếu ở lâu | 35+ | W4 |
| A13 | **Đạo Quán Sơn Đỉnh** | Đạo quán trên núi (Đạo tu) | Đạo Linh vật liệu, giảng đạo (EXP) | 30+ | W4 |
| A14 | **Võ Đài Thiết Cốt** | Lò rèn thân thể (Thể tu) | Linh Thể vật liệu; đấu võ nhận thưởng | 30+ | W4 |
| A15 | **Ngũ Hành Trận Đài** | 6 trụ nguyên tố (Pháp tu) | Linh Trượng vật liệu theo hệ | 30+ | W4 |
| A16 | **Yêu Thú Sào Huyệt** | Hang thú theo biome, có thú mẹ tinh anh | Trứng / con non linh thú để thuần phục (C) | 15+ | W3 |
| A17 | **Tiên Đảo Lơ Lửng** | Đảo nổi trên trời, cầu mây nối nhau | Linh thảo hiếm, Phong Điêu (thú cưỡi bay) | 50+ | W5 |
| A18 | **Thủy Cung Trầm Uyên** | Đền dưới biển, cần nín thở / Tị Thủy Châu | Ngọc trai linh, chuẩn bị cho boss biển (G19) | 40+ | W5 |
| A19 | **Thác Linh Khí** | Thác nước, thiền dưới thác | Tu luyện ×1.5, ngộ đạo ngẫu nhiên (+điểm) | mọi | W2 |
| A20 | **Mộ Tu Sĩ Vô Danh** | Mộ đơn lẻ, mở ra có thể bị nguyền | Đồ quý / debuff ngẫu nhiên (rủi ro – phần thưởng) | 10+ | W3 |
| A21 | **Chợ Đen** | Chỉ mở ban đêm, ẩn trong hẻm thành | Đồ cấm (Độc Đan, Ma Công), giá cao | 20+ | W5 |
| A22 | **Đường Cổ Đạo** | Đường đá nối các thành, cột mốc, cầu, trạm nghỉ | Di chuyển nhanh hơn (+tốc trên đường), thương đoàn đi qua | — | W2 |
| A23 | **Khe Nứt Hư Không Nhỏ** | Vết nứt lẻ trong hoang dã, mở 5 phút | Dọn nhanh, rơi Hư Tinh | 10+ | W2 |
| A24 | **Thạch Khắc Ngộ Đạo** | Vách đá khắc kiếm ý / đạo văn | Ngộ ra chiêu biến thể (nâng cấp kỹ năng) | 40+ | W5 |

### B. Sự kiện thế giới động (18), đăng ký vào Lịch Thiên Cơ

| # | Sự kiện | Kiểu | Chu kỳ | Nội dung | Thưởng | Đợt |
|---|---|---|---|---|---|---|
| B1 | **Linh Khí Triều** | Vùng | 2–3 lần/ngày, ngẫu nhiên | Một vùng bán kính 64 phát sáng, sương linh khí | EXP ×1.5, linh thảo mọc nhanh | W1 |
| B2 | **Thiên Kiếp Lôi Vũ** | Vùng nguy hiểm | Ngẫu nhiên | Bão sét đánh ngẫu nhiên; đứng giữa mà sống thì "tôi luyện" | Lôi Tinh, điểm thể chất | W1 |
| B3 | **Thú Triều** | Thủ thành | 1/ngày, xoay vòng **mọi thành** | Đàn linh thú cuồng hóa tấn công một thành; lính + người chơi thủ | Danh vọng thành, Tử Tinh | W1 |
| B4 | **Bí Cảnh Đại Khai** | Dungeon công cộng | Mỗi tuần (Ngày 7) | Bí Cảnh lớn nhiều người, 30 phút, boss đặc biệt | Rương lớn, Hồn Cốt | W6 |
| B5 | **Thương Đoàn** | Hộ tống | 2/ngày | Đoàn xe đi giữa 2 thành theo Cổ Đạo; cướp tấn công | Tử Tinh, giảm giá ở thành đích | W3 |
| B6 | **Đại Hội Tông Môn** | Giải đấu | Mỗi tuần (Ngày 6) | Võ đài: đánh NPC đại diện tông môn theo vòng (PvE). Tùy chọn PvP | Danh hiệu, sách kỹ năng | W6 |
| B7 | **Đấu Giá Hội** | Đấu giá trực tiếp | 2/tuần | Hội trường, người điều khiển đấu giá NPC, 6–10 lô hiếm (§5.3) | Hồn Cốt, pháp bảo, đan quý | W5 |
| B8 | **Thiên Tài Địa Bảo Xuất Thế** | Tranh đoạt | Ngẫu nhiên | Cột sáng ở nơi xa; bảo vật được yêu thú canh giữ; người đến trước lấy | Linh vật đột phá (G2/G10) | W3 |
| B9 | **Yêu Vương Xuất Sơn** | Boss thế giới | Theo lịch (3 boss xoay vòng) | Boss biome hiện có, giờ đã báo trước | Như hiện tại + rương chung | W1 |
| B10 | **Hư Không Nguyệt Thực** | Đêm đặc biệt | Mỗi 7 ngày | Trăng tím, quái Hư Không ×2, đột kích mọi thành | Hư Tinh ×2 | W1 |
| B11 | **Chợ Phiên** | Kinh tế | Mỗi 3 ngày | Thương nhân lạ đến quảng trường, hàng hiếm giảm giá | Mua sắm | W2 |
| B12 | **Lễ Hội Đèn Lồng** | Lễ hội | Mỗi mùa 1 lần | Đèn lồng bay, trò chơi nhỏ (bắn đèn, đố chữ) | Buff, vật phẩm trang trí | W7 |
| B13 | **Sao Băng Rơi** | Tranh đoạt | Ngẫu nhiên đêm | Thiên thạch rơi để lại hố + khoáng Tinh Thần Sa | Vật liệu cường hóa | W2 |
| B14 | **Linh Dược Khai Hoa** | Thu thập | Theo mùa | Một loại linh thảo nở rộ ở một biome | Linh thảo hiếm | W2 |
| B15 | **Truy Nã Ma Tu** | Săn tiền thưởng | 1/ngày | Bảng truy nã ở thành; ma tu ẩn trong vùng, có manh mối | Tử Tinh, điểm Hiệp Hội | W3 |
| B16 | **Hắc Triều** | Biển | Mỗi 5 ngày | Thủy triều đen, quái biển đổ bộ cảng | Ngọc trai, mở đường boss biển | W5 |
| B17 | **Tông Môn Chiêu Đồ** | Xã hội | Mỗi mùa | Tông môn mở cửa thi tuyển ngoài lệ (vào muộn vẫn được) | Gia nhập tông môn | W6 |
| B18 | **Giảng Đạo Đại Hội** | Tu luyện | Mỗi tuần | Lão tổ giảng đạo ở quảng trường; ngồi nghe đủ giờ | EXP + ngộ đạo | W7 |

### C. Sinh vật & hệ sinh thái (≈22)

| # | Loài | Biome | Vai trò | Đặc biệt | Đợt |
|---|---|---|---|---|---|
| C1 | Linh Lộc | rừng | thụ động | Sừng = nguyên liệu đan; chạy khi thấy người | W2 |
| C2 | Hạc Tiên | đầm, núi | thụ động bay | Lông hạc = bùa phi hành | W2 |
| C3 | Ngọc Thố | đồng cỏ | thụ động | Thuần phục → linh sủng nhặt đồ | W3 |
| C4 | Linh Hồ (cáo 9 đuôi non) | rừng tuyết | trung lập | Thuần phục → linh sủng mê hoặc quái | W3 |
| C5 | Hỏa Huỳnh | đêm, đầm | môi trường | Đom đóm phát sáng, bắt làm đèn | W2 |
| C6 | Linh Ngư | sông, biển | môi trường | Câu được (cần câu linh), nguyên liệu | W3 |
| C7 | **Tuyết Lang** | băng nguyên | thú cưỡi | Thuần phục từ con non (A16); cưỡi nhanh trên tuyết | W3 |
| C8 | **Hỏa Kỳ Lân** | sa mạc / núi lửa | thú cưỡi hiếm | Cưỡi, miễn lửa, nhảy cao | W5 |
| C9 | **Phong Điêu** | tiên đảo | thú cưỡi bay | Bay (giới hạn sức bền) | W5 |
| C10 | Thiết Bối Hùng | rừng núi | tinh anh | Đỡ đòn, gầm làm choáng | W3 |
| C11 | Độc Văn Xà | đầm lầy | thù địch | Độc, ẩn trong cỏ | W3 |
| C12 | Sa Trùng | sa mạc | thù địch | Chui cát, trồi lên có báo trước | W3 |
| C13 | Băng Hồn | băng nguyên | thù địch phép | Làm chậm, đóng băng vùng | W3 |
| C14 | Mộc Yêu | rừng già | thù địch | Giả làm cây, trói rễ | W3 |
| C15 | Độc Thiềm | đầm lầy | thù địch tầm xa | Phun độc | W3 |
| C16 | Hải Quỷ | biển | thù địch | Kéo xuống nước | W5 |
| C17 | Thủy Xà | sông, biển | thù địch | Quấn siết | W5 |
| C18 | Quân Hồn (cổ chiến trường) | A3 | thù địch đêm | Đội hình 3–5, thương trận | W3 |
| C19 | Cuồng Thú (thú triều) | mọi | tinh anh sự kiện | Biến thể cuồng hóa của C1–C14, mắt đỏ | W1 |
| C20 | Yêu Thú Vương tiểu (6 biome) | mọi | mini-boss | Đầu đàn ở sào huyệt A16 | W4 |
| C21 | **Linh thảo tự nhiên** (12 loại) | theo biome / mùa | tài nguyên | Huyết Sâm, Băng Liên, Hỏa Linh Chi, Tử Linh Thảo, Ngưng Hồn Hoa, Kim Tinh Thảo… → đan G10 | W2 |
| C22 | **Khoáng mạch** (5 loại) | núi / hang | tài nguyên | Hạ/Trung/Thượng Linh Thạch, Huyền Thiết, Tinh Thần Sa | W2 |

**Thuần phục:**
- Có 3 bậc: linh sủng nhỏ (buff thụ động), linh thú chiến đấu (đánh cùng, theo cấp chủ), thú cưỡi.
- Cách thuần phục: cho ăn đúng linh thảo + đánh thắng khi thú dưới 30% máu + có thiện cảm.
- Thú có thể lên cấp; cần thức ăn. Mỗi người chơi được 1 thú chiến đấu và 1 thú cưỡi.

### D. NPC sống & xã hội (≈16 kiểu)

| # | Kiểu NPC | Ở đâu | Làm gì | Đợt |
|---|---|---|---|---|
| D1 | **Tán Tu lang thang** | hoang dã, khách điếm | Tu luyện, săn quái; có thể đồng hành, giao dịch, hoặc cướp nếu thiện cảm thấp | W3 |
| D2 | **Đối Thủ Cùng Thế Hệ** | xuất hiện theo mốc cấp | NPC có tên, mạnh lên cùng người chơi, thách đấu định kỳ; kết cục theo lựa chọn (nối cờ `rival_*` gap G8) | W6 |
| D3 | **Lão Giả Bí Ẩn** | ngẫu nhiên | Ẩn thế cao nhân; thử lòng → truyền 1 chiêu | W6 |
| D4 | Thương Nhân Lữ Hành | đường, thôn | Bán hàng vùng khác, mua đồ ta | W2 |
| D5 | Luyện Khí Sư | thành | Cường hóa trang bị, sửa, khảm | W5 |
| D6 | Đan Sư | thành | Mua bán đan, dạy công thức | W5 |
| D7 | **Thầy Bói** | quảng trường | Bói ra **sự kiện sắp tới** (gợi ý lịch), vận may hôm nay | W2 |
| D8 | Thợ Săn Tiền Thưởng | Hiệp Hội | Giao nhiệm vụ truy nã (B15) | W3 |
| D9 | Ma Tu Đội Lốt | thành | Trà trộn; lộ mặt ban đêm (bất ngờ) | W6 |
| D10 | Tiểu Nhị Khách Điếm | A6 | Kể tin đồn: chỉ hướng địa điểm chưa khám phá | W2 |
| D11 | Trưởng Thôn | A7 | Nhiệm vụ thôn; thôn lớn dần nếu giúp | W3 |
| D12 | Người Kể Chuyện | thành | Kể lore theo chương, mở khóa Bia Đá | W2 |
| D13 | Đệ Tử Tuần Tra | quanh tông môn | Tuần tra, đánh quái, chào hỏi theo thiện cảm tông môn | W3 |
| D14 | Tiểu Tặc | chợ | Móc túi Tử Tinh; đuổi bắt lấy lại | W3 |
| D15 | Đấu Giá Sư | Đấu Giá Hành | Điều khiển đấu giá trực tiếp (B7) | W5 |
| D16 | Cư dân có lịch sinh hoạt | thành, thôn | Sáng làm việc, trưa ăn ở tửu lâu, tối về nhà / thắp đèn | W2 |

**Thiện cảm:**
- Theo từng NPC có tên và theo phe (thành, tông môn, thôn). Tăng nhờ nhiệm vụ, quà, cứu giúp; giảm khi giết dân hoặc trộm.
- Mốc thiện cảm mở: giảm giá, nhiệm vụ đặc biệt, truyền công, đồng hành.

**Sinh nhiệm vụ ngẫu nhiên:** khoảng 10 mẫu (hộ tống, giao thư, tìm người lạc, thu thập linh thảo, săn N quái, đánh tinh anh, điều tra tin đồn, bảo vệ ruộng ban đêm, chuộc đồ bị trộm, dẫn đường). Tham số tự điền theo vùng và cấp. Giới hạn 3 nhiệm vụ / NPC / ngày.

---

## 4. Liên kết giữa các mảng (để thế giới "sống")

```
Linh thảo (C21) ──→ Đan dược ──→ Đấu giá / Chợ phiên ──→ Tử Tinh ──→ Thuê / Đấu giá
     ↑                                                        │
Linh Khí Triều (B1) ↑ mọc nhanh                               ▼
Thú triều (B3) ──→ Thành mất Danh vọng nếu thủ thất bại ──→ Giá cả tăng ──→ Thương đoàn (B5)
Tin đồn (D10, D7) ──→ chỉ địa điểm (A*) / sự kiện (B*) ──→ Lịch Thiên Cơ ──→ Dịch chuyển
Bia đá (A1) ──→ manh mối Chấp Hành Giả (sửa G13)
Đối thủ (D2) ──→ cờ lựa chọn truyện (sửa G8) ──→ kết thúc khác nhau
```

---

## 5. Đấu Giá Hành (yêu cầu riêng)

### 5.1 Hai hình thức
| | **Đấu giá thường (sàn)** | **Đấu Giá Hội (trực tiếp)** |
|---|---|---|
| Nơi | Tòa Đấu Giá Hành ở 4 thành lớn + cảng (dùng chung sàn toàn server) | Hội trường đấu giá, theo lịch 2 lần/tuần |
| Người bán | Người chơi + NPC ký gửi | Ban tổ chức (lô hiếm) + người chơi đăng ký trước |
| Cách mua | Đặt giá / mua ngay, thời hạn 12/24/48 giờ thực (hoặc ngày game, cấu hình được) | Đấu giá trực tiếp: Đấu Giá Sư hô giá, đồng hồ 20 giây mỗi lần trả giá, giơ thẻ = chuột phải |
| Món | Mọi vật phẩm giao dịch được: **Hồn Cốt**, đan, sách kỹ năng, nguyên liệu, trang bị, trứng linh thú | 6–10 lô: Hồn Cốt bậc cao, pháp bảo, đan Thông Thiên, thú cưỡi, bản đồ bí cảnh |
| Phí | 5% khi bán (đốt Tử Tinh) + phí đăng 1% | 10% hoa hồng |

### 5.2 Chi tiết kỹ thuật
- Dữ liệu là SavedData server: lô, người bán, giá, thời hạn, người trả cao nhất. Tiền đặt giá bị **giữ** trong ví và tự hoàn khi bị trả giá cao hơn.
- Nhận đồ và tiền qua **Phi Kiếm Truyền Thư** (hòm thư F5). Người chơi offline nhận khi đăng nhập.
- Giao diện có lọc theo loại (Hồn Cốt / đan / sách / nguyên liệu / trang bị), sắp xếp, tìm kiếm, và xem chi tiết Hồn Cốt (chỉ số, phẩm, khe).
- Chống gian lận: không tự mua đồ của mình, giới hạn 20 lô / người, giá tối thiểu theo phẩm.
- **NPC thị trường:** NPC ký gửi đồ thừa của server và mua bớt hàng tồn với giá sàn, để server ít người vẫn có chợ.

### 5.3 Đấu Giá Hội trực tiếp
```
Đấu Giá Sư: "Lô số 3 — Hồn Cốt Long Lân (Huyền phẩm), giá khởi điểm 2 Thượng Tử Tinh!"
[HUD]  Lô 3/8 · Giá hiện tại: 2.4 Thượng (Tán tu Mạc Phong) · ⏱ 14s · [Giơ thẻ +5%] [+10%] [Bỏ]
```
- NPC tán tu cũng trả giá (AI có ngân sách), để không khí sôi động khi ít người chơi.
- Lô cuối là "bảo vật bí ẩn" chưa giám định.

---

## 6. Điều kiện hoàn thành (DoD, theo RULE AP1/AP4)

Mỗi nội dung chỉ được tính ✅ khi có đủ:
1. Dữ liệu JSON và code.
2. Nối vào game: xuất hiện tự nhiên khi chơi, hoặc có trong Lịch.
3. GameTest đi qua đường của người chơi.
4. Ảnh chụp trong game ở `docs/world-review/`.
5. Chữ vi/en đầy đủ.
6. Hiện trên minimap / Lịch nếu có vị trí.

---

## 7. Lộ trình

| Đợt | Nội dung | Phụ thuộc | Agent |
|---|---|---|---|
| **W0** | F1 Lịch Thiên Cơ + tab K "Lịch" + dịch chuyển miễn phí; khung event JSON | — | Sonnet |
| **W1** | Sự kiện dùng hệ có sẵn: B1 Linh Khí Triều, B2 Lôi Vũ, B3 Thú Triều (**mọi thành**), B9 Yêu Vương theo lịch, B10 Nguyệt Thực; C19 Cuồng Thú | W0 | 2× Sonnet |
| **W2** | F2 khung POI + F6 tài nguyên + mùa. A1, A4–A7, A19, A22, A23; B11, B13, B14; C1, C2, C5, C21, C22; D4, D7, D10, D12, D16 | W0 | 2× Sonnet + Haiku (dữ liệu, lang) |
| **W3** | F3 NPC sống (thiện cảm, sinh nhiệm vụ) + F4 sinh vật (thuần phục bậc 1–2). A2, A3, A10, A16, A20; B5, B8, B15; C3, C4, C6, C7, C10–C15, C18; D1, D8, D11, D13, D14 | W2 | 2× Sonnet |
| **W4** | Dungeon và địa điểm theo đạo: A8, A9, A11–A15; C20 | W3 | 2× Sonnet |
| **W5** | F5 Đấu Giá Hành + Phi Kiếm Truyền Thư + B7 Đấu Giá Hội + D5, D6, D15. Biển: A17, A18, B16, C8, C9, C16, C17; A21, A24 | W3 | 2× Sonnet |
| **W6** | Xã hội sâu: D2 Đối thủ (nối cờ truyện), D3, D9; B4, B6, B17 | W4 | 2× Sonnet |
| **W7** | Lễ hội và tu luyện: B12, B18; tinh chỉnh kinh tế (phí, giá sàn) | W5 | Sonnet |
| **W8** | Rà soát: hiệu năng (đếm entity, tick), cân bằng phần thưởng, bộ ảnh `docs/world-review/`, multiplayer 2 client | tất cả | Sonnet |

**Fermi:** 9 đợt × 1,5–2 agent ≈ 16–18 lượt agent × ~250k token ≈ **4–4,5M token**. Với giới hạn tuần của người chơi, cần khoảng 2–3 tuần. Có thể làm theo thứ tự W0 → W1 → W5 (đấu giá) nếu muốn ưu tiên đấu giá.

---

## 8. Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Lag do nhiều sinh vật / NPC / sự kiện | Ngân sách entity, NPC ngủ ngoài 64 block, sự kiện tối đa 2 cùng lúc |
| Kinh tế lạm phát (Tử Tinh dư) | Phí đốt tiền, giá sàn, NPC mua hàng tồn có giới hạn |
| Thế giới tạo lâu hơn | Địa điểm dựng dần theo chunk, không dựng trước |
| Nội dung na ná nhau (như VFX Pháp) | Mỗi đợt có ảnh chụp kiểm tra bắt buộc (DoD #4) |
| Lỗi đồng bộ client (`CULTIVATION`) | Quy tắc: client chỉ đọc `ClientCache` / attachment có sync; thêm grep kiểm tra |
| Dịch chuyển miễn phí bị lạm dụng | 1 lần / sự kiện / người, không dùng khi giao chiến |

---

## 9. Câu hỏi chốt trước khi làm
1. **Đơn vị thời gian của lịch:** theo **ngày game** (1 ngày = 20 phút thực, sự kiện dày) hay **giờ thực** (ví dụ Đấu Giá Hội 20:00 thứ 4 và thứ 7)? Đề xuất: ngày game cho sự kiện nhỏ, giờ thực cho Đấu Giá Hội và Đại Hội Tông Môn.
2. **PvP:** cho phép PvP trong Đại Hội Tông Môn / cướp thương đoàn không, hay chỉ PvE?
3. **Thú cưỡi bay (Phong Điêu):** có cho bay tự do không, hay chỉ lướt có giới hạn sức bền?
4. **Thứ tự:** bắt đầu W0 (Lịch) → W1 (sự kiện) như đề xuất, hay ưu tiên **Đấu Giá Hành** trước?

---

## Document Lineage
| Version | Document | Status |
|---|---|---|
| v1 | PLAN-v1-world-enrichment.md | ✅ Current |
