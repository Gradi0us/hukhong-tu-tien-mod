# PLAN-v2-world-story-integration — Mở rộng thế giới gắn chặt cốt truyện

**Created**: 2026-10-29
**Last Updated**: 2026-10-29 — Bản đầu: kế thừa PLAN-v1-world-enrichment, thêm "Mạch Truyện Thế Giới" nối mọi khu vực / sự kiện / đấu giá / Lịch K vào truyện chính
**Research Target**: Kế hoạch mở rộng thế giới + liên kết cốt truyện (mod Hư Không Tu Tiên Lục)
**Mode**: Planning · **Level**: L3-Analyze · **Language**: vi
**Context**: PLAN-v1-world-enrichment, DESIGN-v1-story-flow, DESIGN-v2-quest-chains, PLAN-v2-tu-tien-mod-canon; code `story/`, `city/`, `events/`, `chg/`, `endgame/`, `liberation/`
**Status**: Đã chốt 5/6 câu (§11); chờ xác nhận thứ tự làm
**Researcher**: Claude Agent (deep-research skill)

---

## Key Findings

| # | Finding | Chi tiết | Bằng chứng | Độ tin cậy |
|---|---------|----------|------------|------------|
| 1 | **Thế giới và truyện đang chạy song song, không chạm nhau** | 65 mục của PLAN-v1 (địa điểm, sự kiện, đấu giá) không có trường nào nói "thuộc phe nào, chương nào, mở manh mối gì" | PLAN-v1 §3 bảng A–D: không có cột lore / chương | CAO |
| 2 | **7/10 thành không xuất hiện trong truyện** | Truyện chính chỉ dùng Lưu Ly Cảng, Hắc Thạch Quan, Cổ Tiên Thành và 4 hang Chấp Hành Giả. Thành 3–9 chỉ là nơi dịch chuyển | `city/CityDef.java:8-17`; DESIGN-v1-story-flow §2 cột "Địa điểm" | CAO |
| 3 | **Manh mối quá ít, chỉ có một nguồn** | 12 Văn Kiện (a1–a6, b1–b6), gần như chỉ rơi từ Sứ Đồ và chuỗi phụ | `story/StoryDocs.java:23-26`, `story/SuDo.java:44-46` | CAO |
| 4 | **Có sẵn "móng" để nối, không cần làm lại** | Cờ `QuestData.flags`, `Objectives.set`, action thoại `qc`/`doc`/`step_done`, `CorruptionData.hearts`, `LiberationManager`, `WorldTier` | DESIGN-v2-quest-chains §2; `liberation/LiberationManager.java:53`; `endgame/WorldTier.java:12` | CAO |
| 5 | **Giải pháp: một lớp "Mạch Truyện" mỏng nằm giữa** | Mỗi sự kiện / địa điểm / lô đấu giá khai báo trong JSON: phe, chương, cờ cần, cờ đặt, manh mối. Một engine `StoryHooks` đọc chung | Thiết kế §2 | TRUNG BÌNH (chưa làm thử) |
| 6 | **Thứ tự nên đổi** | Làm lớp Mạch Truyện (S0) cùng lúc với Lịch (W0). Nếu làm sau thì phải sửa lại ~65 file JSON | Fermi §7 | CAO |

---

## Table of Contents
1. Bối cảnh (SCQA)
2. Thiết kế: Mạch Truyện Thế Giới
3. Bản đồ 10 thành ↔ chương ↔ cung truyện
4. Nối từng mảng PLAN-v1 vào truyện
5. Lật ngược giả định (Inversion)
6. Góc nhìn các bên (UC5)
7. Ưu tiên (RICE) + Fermi + lộ trình
8. Rủi ro + điều kiện hoàn thành
9. Câu hỏi chốt
10. Kết luận + sơ đồ tổng hợp

---

## 1. Bối cảnh (SCQA)

- **Situation:** Truyện chính đã có 11 chương, gắn theo mốc cảnh giới (DESIGN-v1). Có 4 chuỗi nhiệm vụ phụ (DESIGN-v2). PLAN-v1 đã liệt kê ~65 nội dung thế giới, cùng Lịch Thiên Cơ và Đấu Giá Hành.
- **Complication:** Người chơi đi giữa các mốc truyện (mỗi chương ~10 cấp, nhiều giờ chơi) mà thế giới không nhắc gì đến Dạ Khuyết, Sứ Đồ hay Hư Không. Làm thêm 65 nội dung "trung lập" sẽ càng làm truyện loãng.
- **Question:** Làm sao để mỗi thứ người chơi gặp ngoài thế giới đều đẩy truyện đi tiếp, hoặc ít nhất kể thêm về truyện?
- **Answer:** Thêm lớp dữ liệu **Mạch Truyện Thế Giới**: mọi nội dung đều thuộc một phe và một chương, và mở hoặc đặt cờ truyện. Lịch K hiện cả sự kiện cốt truyện. 10 thành, mỗi thành có một cung truyện nhỏ. Văn Kiện tăng từ 12 lên ~30 và rơi từ địa điểm, sự kiện, đấu giá.

---

## 2. Thiết kế: Mạch Truyện Thế Giới

### 2.1 Sáu phe (MECE)

| Phe | Mục tiêu trong truyện | Xuất hiện qua |
|---|---|---|
| **Dạ Khuyết & Sứ Đồ** | Mở Hư Không, hút thế giới | Nguyệt Thực, Ma Tu Đội Lốt, người trả giá bí ẩn ở đấu giá, Tâm Hư Hóa |
| **Chấp Hành Giả** (4) | Canh 4 Mảnh, giữ hang | Bia Đá chỉ hang, Thiên Kiếp Lôi Vũ gần hang, Hồn Cốt đem ra đấu giá |
| **Tông Môn** | Giữ đạo thống; có nội gián | Đại Hội Tông Môn, Chiêu Đồ, Tàn Tích Tông Môn |
| **10 Thành** | Sống sót, buôn bán | Thú Triều, Thương Đoàn, Chợ Phiên, Đấu Giá Hành |
| **Yêu Tộc** | Bị Hư Hóa làm cuồng | Yêu Vương, Sào Huyệt, Cuồng Thú |
| **Tiền Nhân** (thời trước Hư Không) | Để lại sự thật về nguồn gốc Hư Không | Động Phủ, Cổ Chiến Trường, Thạch Khắc, Thủy Cung |

### 2.2 Trường lore chung cho mọi file JSON (sự kiện / địa điểm / mẫu nhiệm vụ / lô đấu giá)

```json
"lore": {
  "faction": "da_khuyet",            // 1 trong 6 phe
  "chapter": [3, 10],                // chỉ xuất hiện trong khoảng chương này
  "requires_flags": ["c6_lair"],     // cờ truyện cần có (tùy chọn)
  "sets_flags": ["wl.blood_moon_1"], // cờ đặt khi hoàn thành
  "clue": "c3",                      // Văn Kiện / manh mối trao (tùy chọn)
  "rumor": "hukhong.rumor.xxx",      // câu tin đồn tiểu nhị / thầy bói kể trước
  "modao_line": "hukhong.dlg.modao.wl.xxx" // Mộ Dao bình luận sau đó (1 lần)
}
```

**Engine `story/world/StoryHooks`** (mới, khoảng 300 dòng):
- `allowed(player, lore)`: lọc theo chương (`QuestData.chapter`), cờ và chế độ. Ở chế độ Tự do thì bỏ qua lọc chương.
- `complete(player, lore)`: đặt cờ, trao Văn Kiện, xếp lời Mộ Dao (dùng lại cơ chế `qc.told.*`).
- Dùng chung cho Lịch, địa điểm, đấu giá và sinh nhiệm vụ. **Đây là điểm nối duy nhất, nên không có mảng nào phải tự viết logic truyện.**

### 2.3 Thế Lực Hư Ảnh (thanh đo toàn thế giới)

- Một số 0–100 cho mỗi vùng (theo thành gần nhất). Lưu trong `CorruptionData`, vì Hư Hóa theo chunk đã có sẵn.
- **Tăng khi:** thua Thú Triều hoặc tập kích, bỏ qua Nguyệt Thực, để Tâm Hư Hóa sống.
- **Giảm khi:** giải phóng vùng (`LiberationManager`), hạ Sứ Đồ, thắng sự kiện.
- **Ảnh hưởng:** tần suất Nguyệt Thực và tập kích, giá ở thành, Chấp Hành Giả mạnh thêm 0–15%, và một câu khác nhau trong cảnh kết.
- Hiện trên Lịch K, bên cạnh mỗi thành: "Hư Ảnh: 42%".

### 2.4 Văn Kiện mở rộng: 12 → 30, chia 3 bộ

| Bộ | Số | Nội dung | Rơi từ | Đủ bộ thì |
|---|---|---|---|---|
| A — Mật lệnh Dạ Khuyết | 6 → 10 | Kế hoạch, vị trí hang | Sứ Đồ, Ma Tu Đội Lốt, Truy Nã | Lộ hang sớm hơn 1 chương |
| B — Nhật ký Sứ Đồ | 6 → 10 | Ai là nội gián, Mộ Dao | Chuỗi phụ, Thương Đoàn bị cướp, đấu giá | Mở thêm lựa chọn ở chương 6 và 9 |
| **C — Di thư Tiền Nhân** (mới) | 10 | Hư Không từ đâu đến, Dạ Khuyết trước kia là ai | Bia Đá, Động Phủ, Cổ Chiến Trường, Thạch Khắc, Thủy Cung | Mở **kết thúc thứ ba** (§4.6) |

---

## 3. Bản đồ 10 thành ↔ chương ↔ cung truyện

Mỗi thành có một cung truyện nhỏ gồm 3 bước, dùng lại engine `QuestChains`. Cung chỉ mở khi đúng chương.

| # | Thành | Chương | Cung truyện | NPC | Kết / boss | Nối truyện chính |
|---|---|---|---|---|---|---|
| 0 | Lưu Ly Cảng | 0–1 | (đã có) Mộ Dao, Ấn Hồn Đài | Mộ Dao | — | Mở đầu |
| 1 | Hắc Thạch Quan | 2 | (đã có) Hạng Chiêu, Hư Tu mang tín vật tông | Hạng Chiêu | — | Rival 1 |
| 2 | Cổ Tiên Thành | 3–4 | (đã có) Bí Cảnh đầu, Hứa Cổ | Hứa Cổ | — | Văn Kiện A/B |
| 3 | **Huyết Nguyệt Phường** (thành đổ nát) | 4 | **"Chợ Máu":** Chợ Đen ban đêm bán Hư Tinh; truy ra kẻ cung cấp là Sứ Đồ | Chủ Chợ Đen | Sứ Đồ Huyết Tế Tư | Văn Kiện A7. Mở Chợ Đen (A21) |
| 4 | **Đan Hà Thành** (cảng) | 5 | **"Hắc Triều":** quái biển đổ bộ; ngọc trai Hư Hóa | Hải Tướng | Mở boss biển (G19) | Hắc Triều (B16) vào Lịch |
| 5 | **Cổ Xảo Thành** | 5–6 | **"Đấu Giá Đẫm Máu":** ở Đấu Giá Hội đầu tiên, Hạng Chiêu tranh lô Hồn Cốt với một người mua bí ẩn (Sứ Đồ) | Đấu Giá Sư, Hạng Chiêu | Truy kẻ mua, đánh Sứ Đồ | Mở Đấu Giá Hội. Thêm 1 cảnh cho Rival |
| 6 | **Lạc Nhật Thành** | 7 | **"Lôi Vũ Viêm Sơn":** Thiên Kiếp Lôi Vũ mở đường vào hang CHG I | Lão Giả Bí Ẩn | Hang CHG I | Thay cho `c7_chg1` thiếu dẫn dắt |
| 7 | **Thanh Vân Thành** | 8 | **"Đại Hội Tông Môn":** chung kết đấu Hạng Chiêu (đã bị Từ Âm mê hoặc) | Tông chủ | Duel `c8_rival` diễn ra **tại võ đài** | Cờ `rival_saved` / `rival_lost` |
| 8 | **Bắc Hàn Thành** | 9 | **"Kiếm Trủng Tuyết":** Kiếm Trủng chứa kiếm của tiền nhân; mở đường lên Tuyết Kiếm Lĩnh | Kiếm Si | Hang CHG III | Cảnh Mộ Dao rời đi / hy sinh diễn ra ở đây |
| 9 | **Thiên Khải Hoàng Đô** (đổ nát) | 10 | **"Hoàng Đô Thất Thủ":** giải phóng Hoàng Đô (Liberation), lấy lại cổng Hư Không | Hoàng Tộc Sót Lại | Tâm Hư Hóa lớn → cổng `c10_gate` | Khảo Hạch, kết thúc |

Thêm hai luật:
- **Tập kích nhắm mọi thành** (sửa gap G15). Thành nào bị tập kích được chọn theo **chương hiện tại của người chơi gần nhất**, để cuộc tấn công luôn rơi vào nơi truyện đang diễn ra.
- **Thành mất (RUIN)** thì tự sinh nhiệm vụ "Tái chiếm" qua `LiberationManager`, thay vì chặn truyện (DESIGN-v1 hiện đang bỏ qua bước `c5_invasion`).

---

## 4. Nối từng mảng PLAN-v1 vào truyện

### 4.1 Lịch Thiên Cơ (tab K)
- Có hai làn: **Thế giới** (sự kiện lặp) và **Thiên Cơ Dự Ngôn** (sự kiện cốt truyện của chương hiện tại, màu tím).
- Sự kiện cốt truyện có nút **"Đến"**, dịch chuyển miễn phí giống sự kiện khác. Người chơi không còn phải hỏi "giờ đi đâu".
- Mỗi sự kiện hiện phe (biểu tượng) và Hư Ảnh của vùng.

### 4.2 Sự kiện (B) theo phe

| Sự kiện | Phe | Gắn truyện |
|---|---|---|
| B10 Nguyệt Thực | Dạ Khuyết | Từ chương 4: đêm Nguyệt Thực có 1 Sứ Đồ chỉ huy, rơi Văn Kiện A. Ở chương 10, Dạ Khuyết hiện ảo ảnh trên trời |
| B3 Thú Triều | Yêu Tộc | Thú cuồng mang dấu Hư Hóa. Dọn xong → manh mối Tâm Hư Hóa gần đó |
| B2 Lôi Vũ | CHG | Chỉ xảy ra gần hang CHG chưa bị hạ. Là cách tìm hang tự nhiên |
| B5 Thương Đoàn | Thành | Bị Sứ Đồ cải trang cướp (giống chuỗi `merchant`). Rơi Văn Kiện B |
| B6 Đại Hội Tông Môn | Tông Môn | Chung kết ở chương 8 là trận với Hạng Chiêu (§3 #7) |
| B7 Đấu Giá Hội | Thành / Dạ Khuyết | §4.4 |
| B8 Thiên Tài Địa Bảo | Tiền Nhân | Bảo vật kèm Di thư bộ C |
| B9 Yêu Vương | Yêu Tộc | Hạ cả 3 Yêu Vương → Mộ Dao kể chuyện Hư Hóa lan sang yêu tộc. Thưởng Hồn Cốt |
| B18 Giảng Đạo | Tông Môn | Lão tổ giảng đúng lore của chương hiện tại (một đoạn mỗi chương) |

### 4.3 Địa điểm (A)
- **Bia Đá (A1), Động Phủ (A2), Cổ Chiến Trường (A3), Thạch Khắc (A24), Thủy Cung (A18):** mỗi nơi có 1–2 Di thư bộ C. Ai đọc đủ 10 Di thư sẽ biết sự thật về Dạ Khuyết.
- **Khách Điếm (A6):** tiểu nhị kể tin đồn trỏ tới bước truyện kế tiếp **hoặc** tới địa điểm có Di thư chưa nhặt (đọc `lore.rumor`).
- **Tàn Tích Tông Môn (A8):** chính là tông của trưởng lão nội gián Mạc Tùng (chuỗi `noi_gian`). Vào được khi đã có cờ `qc.noi_gian.c.expose`.

### 4.4 Đấu Giá Hành
- **Lô cốt truyện:** Hồn Cốt Chấp Hành Giả (sau khi hạ), Văn Kiện B hiếm, bản đồ hang chưa lộ.
- **Người trả giá bí ẩn:** đôi khi là Sứ Đồ đội lốt. Nếu người chơi thắng lô, Sứ Đồ phục kích khi rời hội trường. Nếu thua, nhận nhiệm vụ truy kẻ mua.
- Hồn Cốt vẫn mua bán tự do; chỉ lô cốt truyện mới bị khóa theo chương.

### 4.5 NPC (D)
- **D2 Đối Thủ Cùng Thế Hệ = Hạng Chiêu**, không tạo nhân vật mới. Hạng Chiêu có mặt ở 3 sự kiện thế giới (đấu giá, Đại Hội, Thú Triều) ngoài 3 trận chính.
- **D9 Ma Tu Đội Lốt = Sứ Đồ.** Bắt được thì có Văn Kiện A.
- **D7 Thầy Bói:** báo trước sự kiện cốt truyện sắp tới trong Lịch.
- **Mộ Dao:** mỗi sự kiện có `modao_line`. Mộ Dao nhắc lại một lần (đã có cơ chế `qc.told.*`).

### 4.6 Kết thúc thứ ba (mới)
- Hiện có `ending_seal` và `ending_absorb`.
- **Thêm `ending_truth`:** cần đủ 10 Di thư bộ C và Hư Ảnh trung bình dưới 30%. Người chơi có thể nói chuyện với Dạ Khuyết thay vì đánh ngay. Trận cuối dễ hơn, có cảnh kết riêng, và mở **Thế Giới Tầng 2** với Hư Ảnh bị reset.

---

## 5. Lật ngược giả định (Inversion)

| # | Giả định sai | Lật ngược | Chân lý |
|---|---|---|---|
| 1 | "Càng nhiều nội dung thì thế giới càng giàu" | 65 nội dung trung lập làm truyện loãng hơn (Finding 1). Trước đây Pháp có 6 hệ nhưng VFX na ná nhau nên người chơi không nhận ra khác biệt (commit 33d4979) | Mỗi nội dung cần **lý do tồn tại trong truyện**, không chỉ có số lượng |
| 2 | "Truyện chính phải thật dài" | Chương dài ~10 cấp; giữa hai mốc người chơi không có tín hiệu truyện nào (DESIGN-v1 §1) | Truyện nên **rải đều** qua sự kiện, không kéo dài thêm chương |
| 3 | "Viết logic truyện vào từng sự kiện" | Thực tế đã sinh ra 4 chỗ kiểm tra chương rời rạc (`chapter_min` trong thoại, `QuestChains.OFFERS`, `StoryFlow`, Liberation) | **Một điểm nối duy nhất** (`StoryHooks`) |
| 4 | "Văn Kiện chỉ để đọc" | 12 Văn Kiện hiện chỉ mở một bước (`c4_docs`) | Văn Kiện là **chìa khóa**: mở lựa chọn và kết thúc |

---

## 6. Góc nhìn các bên (UC5)

| Góc nhìn | Mối quan tâm | Đánh giá |
|---|---|---|
| Người chơi (Design Thinking) | "Giờ đi đâu?", "chuyện này liên quan gì đến mình?" | Lịch K có làn Dự Ngôn và tin đồn giải quyết trực tiếp |
| Kỹ thuật | Lại thêm một cách kiểm tra chương; save cũ | `StoryHooks` thay các chỗ kiểm tra rời rạc. Cờ mới có tiền tố `wl.`, không đụng cờ cũ |
| Hiệu năng | Thêm sự kiện = thêm entity | Giữ ngân sách của PLAN-v1 §1 (tối đa 2 sự kiện cùng lúc) |
| Multiplayer | Mỗi người một chương khác nhau | Sự kiện thế giới chung cho mọi người; phần cốt truyện (Văn Kiện, cờ) tính riêng từng người. Thành bị tập kích theo người chơi có chương cao nhất ở gần |
| Token / chi phí | Người chơi ít token | S0 nhỏ (1 agent). Nội dung dùng JSON nên agent Haiku viết được |

**Tổng hợp:** lớp Mạch Truyện tốn ít (khoảng 1 đợt) nhưng nhân giá trị của mọi đợt sau. Làm trễ thì phải sửa lại toàn bộ JSON.

**Theo quy mô:**
- **1 người chơi:** cần làn Dự Ngôn và tin đồn để không bị lạc.
- **Server nhỏ 2–5 người:** cần tách cờ theo người chơi và chọn thành bị tập kích hợp lý.
- **Server lớn:** cần Hư Ảnh theo vùng và đấu giá chung toàn server.

---

## 7. Ưu tiên (RICE) + Fermi + lộ trình

### 7.1 RICE

Thang điểm: Reach = phần người chơi gặp tới (0–1); Impact 1–5; Confidence %; Effort = số lượt agent.

| Hạng mục | Reach | Impact | Conf | Effort | RICE |
|---|---|---|---|---|---|
| S0 `StoryHooks` + trường lore + Văn Kiện C | 1.0 | 4 | 80% | 1 | **3.2** |
| Lịch K làn Dự Ngôn (gộp vào W0) | 1.0 | 4 | 80% | 1 | **3.2** |
| Tập kích mọi thành + Tái chiếm | 0.8 | 4 | 70% | 1 | **2.2** |
| Cung 7 thành (§3, #3–9) | 0.7 | 5 | 60% | 3 | 0.7 |
| Đấu giá có lô cốt truyện + người mua bí ẩn | 0.6 | 3 | 70% | 0.5 (gộp vào W5) | 2.5 |
| Hư Ảnh theo vùng | 0.9 | 3 | 60% | 1 | 1.6 |
| Kết thúc thứ ba | 0.3 | 5 | 50% | 1 | 0.75 |

### 7.2 Lộ trình (thay PLAN-v1 §7, có chèn bước truyện)

| Đợt | Nội dung | Agent |
|---|---|---|
| **S0 + W0** | `StoryHooks`, trường `lore`, Văn Kiện bộ C (10 lời thoại), Lịch Thiên Cơ có 2 làn, nút "Đến" | 1 Sonnet + 1 Haiku (lang) |
| **W1 + S1** | Nguyệt Thực / Thú Triều / Lôi Vũ / Yêu Vương có phe. Tập kích mọi thành. Tái chiếm. Hư Ảnh | 2 Sonnet |
| **W2** | Bia Đá, Khách Điếm + tin đồn, Thầy Bói, linh thảo, Chợ Phiên (theo PLAN-v1) | 2 Sonnet + Haiku |
| **S2** | Cung truyện thành 3, 4, 5 (Chợ Máu, Hắc Triều, Đấu Giá Đẫm Máu) | 1 Sonnet |
| **W3–W4** | Theo PLAN-v1 (NPC sống, sinh vật, dungeon theo đạo). Mỗi file JSON có `lore` | 2 Sonnet |
| **W5** | Đấu Giá Hành + lô cốt truyện + người mua bí ẩn | 1 Sonnet |
| **S3** | Cung thành 6–9 (Lôi Vũ Viêm Sơn, Đại Hội, Kiếm Trủng, Hoàng Đô) + kết thúc thứ ba | 2 Sonnet |
| **W6–W8** | Theo PLAN-v1 (xã hội, lễ hội, rà soát) | như PLAN-v1 |

### 7.3 Fermi
- Phần mới so với PLAN-v1 là S0, S1, S2, S3: khoảng 6 lượt agent × ~250k ≈ **1,5M token**.
- Cộng PLAN-v1 (~4–4,5M) → tổng **≈ 5,5–6M token**.
- **Bản tối thiểu cho lượt token ít:** chỉ S0 + W0 ≈ 2 lượt ≈ 0,5M. Đã đủ để người chơi thấy truyện xuất hiện trong Lịch K.

---

## 8. Rủi ro + điều kiện hoàn thành

| Rủi ro | Giảm thiểu |
|---|---|
| Cờ truyện mâu thuẫn giữa nhiều nguồn | Chỉ `StoryHooks` được đặt cờ `wl.*`. JUnit kiểm tra mọi `requires_flags` đều có nguồn đặt |
| Người chơi đi lệch, bỏ cung thành | Cung không bắt buộc; truyện chính vẫn chạy theo cảnh giới |
| Save cũ | Cờ mới có tiền tố riêng; thiếu cờ thì coi như chưa làm |
| Chế độ Tự do | Bỏ lọc chương, vẫn rơi Di thư, không có làn Dự Ngôn |

**DoD (theo RULE AP1/AP4):**
1. JSON có `lore`.
2. `StoryHooks` được gọi từ Lịch, địa điểm và đấu giá (grep có callsite).
3. GameTest đi đúng đường người chơi: sự kiện → cờ → Văn Kiện → Mộ Dao nói.
4. Ảnh chụp trong game.
5. Lang vi/en đầy đủ.

---

## 9. Câu hỏi chốt

Các câu 1–4 của PLAN-v1 §9 vẫn chưa trả lời. Đề xuất mặc định trong ngoặc:
1. Lịch dùng ngày game hay giờ thực? (*ngày game*; Đấu Giá Hội dùng giờ thực)
2. Có PvP không? (*chỉ PvE*)
3. Phong Điêu bay tự do không? (*lướt, có giới hạn sức bền*)
4. Bắt đầu từ đâu? (*S0 + W0*)
5. **Mới:** có thêm kết thúc thứ ba `ending_truth` không? (*có*)
6. **Mới:** thành bị tập kích chọn theo chương của người chơi hay ngẫu nhiên? (*theo chương*)

---

## 10. Kết luận + sơ đồ tổng hợp

```
┌──────────────────────── MẠCH TRUYỆN THẾ GIỚI ─────────────────────────┐
│                                                                        │
│  Truyện chính (11 chương, theo cảnh giới) ──┐                          │
│  Chuỗi phụ (4)  ────────────────────────────┤                          │
│                                             ▼                          │
│        [ StoryHooks: phe · chương · cờ · Văn Kiện · lời Mộ Dao ]       │
│           ▲          ▲            ▲            ▲            ▲          │
│        Lịch K     Sự kiện B   Địa điểm A   Đấu giá     NPC D           │
│      (Dự Ngôn)   (Nguyệt Thực, (Bia Đá →   (lô truyện, (Hạng Chiêu,    │
│                  Lôi Vũ…)      Di thư C)   Sứ Đồ mua)  Sứ Đồ đội lốt)  │
│           │          │            │            │            │          │
│           └──────────┴─────┬──────┴────────────┴────────────┘          │
│                            ▼                                           │
│        Hư Ảnh theo vùng ──→ tần suất tập kích / CHG mạnh / giá         │
│        10 thành × cung truyện ──→ Tái chiếm khi RUIN                   │
│        Văn Kiện A+B+C ──→ lựa chọn ch.6/9 ──→ 3 kết thúc               │
│                            │                                           │
│                            ▼                                           │
│                  Thế Giới Tầng 2 (WorldTier)                           │
└────────────────────────────────────────────────────────────────────────┘
```

**Kết luận:** Không cần viết thêm truyện dài hơn. Chỉ cần làm cho thế giới **nói về truyện**: một lớp dữ liệu, một engine nối, 7 cung thành và 18 Văn Kiện mới. Làm S0 cùng Lịch trước.

---

## Sources & References

### Source 1: Local
| File | Relevance |
|---|---|
| docs/PLAN-v1-world-enrichment.md | 65 nội dung, Lịch, Đấu giá, lộ trình W0–W8 |
| docs/DESIGN-v1-story-flow.md | 11 chương, NPC, cờ |
| docs/DESIGN-v2-quest-chains.md | engine chuỗi phụ, cờ `qc.*` |
| docs/PLAN-v2-tu-tien-mod-canon.md | luật chung, cảnh giới |
| src/.../city/CityDef.java:8-17 | 10 thành |
| src/.../story/StoryDocs.java:23-26 | 12 Văn Kiện |
| src/.../liberation/LiberationManager.java:53 | giải phóng vùng |
| src/.../endgame/WorldTier.java:12 | tầng thế giới |

### Source 3: External (chưa kiểm lại trong phiên này vì giới hạn token)
| # | Nguồn | Liên quan |
|---|---|---|
| 1 | Henry Jenkins (2004). *Game Design as Narrative Architecture*. MIT Press (First Person) | Kể chuyện bằng môi trường: địa điểm mang mảnh truyện, như Di thư bộ C |
| 2 | Jesse Schell (2008). *The Art of Game Design: A Book of Lenses*. CRC Press | Lens "Story Machine": hệ thống sinh ra chuyện |
| 3 | The Witcher 3 (CD Projekt RED, 2015): nhiệm vụ "Bloody Baron" | Case study: nhiệm vụ phụ ảnh hưởng truyện chính, như cung truyện thành |
| 4 | Elden Ring (FromSoftware, 2022) | Case study: lore rải qua mô tả vật phẩm và bia đá thay vì cảnh cắt |

> "A game is a series of interesting decisions." — Sid Meier

Phiên này không chạy tìm kiếm web (Freshness Check) vì chủ đề là thiết kế nội bộ và người chơi còn ít token.

## Document Lineage
| Version | Document | Focus | Status |
|---|---|---|---|
| v1 | PLAN-v1-world-enrichment.md | 65 nội dung + Lịch + Đấu giá | Active (nội dung) |
| **v2** | **PLAN-v2-world-story-integration.md** | Lớp Mạch Truyện nối thế giới ↔ truyện; lộ trình gộp | ✅ Current (thứ tự làm) |

## Related Documents
| Document | Relationship |
|---|---|
| DESIGN-v1-story-flow.md | Truyện chính mà kế hoạch này nối vào |
| DESIGN-v2-quest-chains.md | Engine dùng lại cho cung thành |
| REVIEW-v1-tu-tien-mod-implementation-gap.md | Gap G8, G13, G15, G19 được gộp vào đây |

**Tài liệu nên làm tiếp:** SPEC-v1-story-hooks (schema JSON + API), DESIGN-v3-city-arcs (thoại chi tiết 7 cung thành).

## Token Summary
```
Files scanned:  4 docs + ~15 code files (grep) | ~25k tokens
External:       0 MCP + 0 web
Output:         ~6k tokens
Budget status:  OK
```

---

## 11. Quyết định đã chốt (2026-10-29)

| # | Câu hỏi | Chốt | Ảnh hưởng tới kế hoạch |
|---|---|---|---|
| 1 | Đơn vị lịch | **1 ngày Lịch = 4 ngày Minecraft** (80 phút thực) | Tuần Lịch = 7 ngày Lịch ≈ 9,3 giờ thực. Sự kiện lặp "mỗi ngày" ≈ mỗi 80 phút. Mùa = 7 ngày Lịch |
| 2 | PvP | **Có: Đấu Trường Sinh Tử ở Thiên Khải Hoàng Đô** (thành bậc Thiên, lớn nhất) | Mục mới **B19**, xem §11.1. Ngoài đấu trường vẫn chỉ PvE |
| 3 | Thú cưỡi | **Bỏ toàn bộ thú cưỡi** (C7 Tuyết Lang, C8 Hỏa Kỳ Lân, C9 Phong Điêu) | Sau này di chuyển bằng **ngự vũ khí bản mệnh** (dùng lại `flight/`). Tiên Đảo (A17) vào bằng ngự kiếm. Linh thú chỉ còn linh sủng và thú chiến đấu |
| 4 | Thứ tự | Đề xuất S0 + W0 trước; chờ người chơi xác nhận | — |
| 5 | Kết thúc thứ ba | **Có**, thiết kế ở §11.2 | — |
| 6 | Thành bị tập kích | **Trộn: 60% theo chương, 40% ngẫu nhiên** | Ngẫu nhiên chỉ chọn trong các thành đã mở khóa dịch chuyển, để người chơi tới được |

### 11.1 B19 — Đấu Trường Sinh Tử (PvP)

- **Nơi:** võ đài trong Thiên Khải Hoàng Đô. Nếu Hoàng Đô đang RUIN thì đấu trường đóng cho tới khi tái chiếm.
- **Thách đấu:** người chơi A thách B tại võ đài, chọn mức cược. Hai bên cùng đặt **số Tử Tinh bằng nhau**, tiền bị giữ trong ví cho tới khi xong trận.
- **Phí:** đốt 5% tổng cược (chống lạm phát). Người thắng nhận phần còn lại.
- **Khán giả:** được cược cho một bên, tỷ lệ chia theo tổng tiền mỗi bên (pari-mutuel), cũng đốt 5%.
- **Luật trận:**
  - Không chết thật: còn 1 máu thì thua, không rơi đồ, không mất EXP.
  - Đánh 3 hiệp, mỗi hiệp tối đa 3 phút; hết giờ thì ai còn % máu cao hơn thắng.
  - Cấm đan hồi phục.
  - Cân bằng cảnh giới: chênh trên 10 cấp thì bên thấp được buff tỉ lệ, hoặc người thách chọn "không cân bằng".
- **Chống gian lận:** không tự cược cho trận của mình ở phía đối thủ, mỗi cặp tối đa 3 trận/ngày Lịch, mức cược tối thiểu theo cảnh giới.
- **Lịch K:** "Giải Sinh Tử" mỗi tuần Lịch (bracket 8 người), có NPC tán tu điền chỗ trống khi ít người.
- **Gắn truyện:** chương 8, Hạng Chiêu bị mê hoặc có thể xuất hiện làm "nhà vô địch" thách người chơi (PvE).

### 11.2 Kết thúc thứ ba — "Siêu Độ" (`ending_truth`)

**Sự thật (lộ dần qua 10 Di thư bộ C):**
- Dạ Khuyết vốn là **Thủ Hộ Giả** của Tiền Nhân. Ngàn năm trước, ông tự làm "bình chứa" để phong ấn Hư Không.
- 4 Chấp Hành Giả là 4 đệ tử của ông, được giao canh 4 Mảnh.
- Ông muốn mở Hư Không không phải để hủy diệt. Ông đã kiệt sức, và nếu không có người thay thì Hư Không sẽ tự vỡ.

**Hai kết thúc hiện có** chỉ chọn giữa "phong lại" (kéo dài nỗi khổ) hoặc "hút lấy" (người chơi thành bình chứa mới).

**Kết thúc thứ ba: chuyển hóa Hư Không thành Linh Khí, giải thoát Dạ Khuyết.**

| Phần | Nội dung |
|---|---|
| Điều kiện | Đủ 10 Di thư C, Hư Ảnh trung bình dưới 30%, đã hạ đủ 4 CHG. Thiếu điều kiện thì lựa chọn hiện mờ kèm gợi ý thiếu gì |
| Trận cuối | Phase 1 như cũ. Phase 2 đổi thành **Tâm Chiến**: đánh 4 tàn ảnh CHG trong ký ức Dạ Khuyết, mỗi tàn ảnh nói một câu về quá khứ. Phase 3: không đánh, đứng giữ 4 Mảnh trong 60 giây giữa sóng Hư Không (né đòn) để chuyển hóa |
| Cảnh kết | Hư Không tan thành mưa linh quang. Dạ Khuyết nói lời cuối và trao **Ấn Thủ Hộ** |
| Nhân vật | Mộ Dao: nếu đã rời đi thì quay về; nếu đã hy sinh thì tàn hồn hiện ra chào. Hạng Chiêu: nếu `rival_lost` thì tỉnh lại, xin lỗi |
| Thế giới sau đó | **Thế Giới Tầng 2 "Linh Khí Phục Hưng"**: vùng Hư Hóa cũ thành vùng linh khí (tu luyện nhanh hơn), quái Hư Không yếu dần. Thay vào đó, **Yêu Tộc** trỗi dậy làm mối đe dọa mới (mở đường cho nội dung sau) |
| Thưởng | Danh hiệu "Siêu Độ Giả". Vũ khí bản mệnh lên dạng **Thủ Hộ**, mở khả năng ngự vũ khí bay đường dài (khớp quyết định #3) |

**Vì sao chọn hướng này:**
- Biến việc nhặt Di thư (khám phá thế giới) thành phần thưởng truyện lớn nhất.
- Cho Hư Ảnh (phòng thủ thành) ý nghĩa ở kết thúc.
- Đồng thời mở hướng nội dung sau kết thúc.

---

## 12. Bổ sung theo yêu cầu người chơi (2026-10-29, lượt 2)

Thứ tự làm đã chốt: **S0 + W0 trước** (phần nối cốt truyện + Lịch K).

### 12.1 Tách Ấn Hồn Đài khỏi khu thương nhân
- **Hiện trạng:** `CityMarket` đặt thương nhân quanh quảng trường / bàn thờ (`city/CityMarket.java:20-22`), nên thương nhân đứng chen với Ấn Hồn Đài (`city/AltarShrine.java:18-20`).
- **Mới:**
  - Ấn Hồn Đài thành **Quảng Trường Ấn Hồn** riêng:
    - Đài lớn gấp khoảng 2 lần (bán kính 9 → khoảng 18), 3 bậc thềm.
    - 8 trụ lửa hồn, vòng sáng xoay trên cao, hồ nước bao quanh có cầu.
    - Không có thương nhân nào trong bán kính 32 block.
  - Thương nhân chuyển hết sang **Khu Chợ** (§12.2).
- **Lưu ý:** việc này đảo lại quyết định cũ (đã bỏ khu chợ riêng, `city/TradeDistrict.java:5-7`). Cần sửa `CityProtection` và thêm marker vị trí chợ.

### 12.2 Khu Chợ (Thương Hội) + Uy Danh Chợ
- Mỗi thành có một khu chợ riêng, cách tâm thành 40–70 block, nằm trên đất khô (`DrySites`). Có cổng chào và các sạp hàng, và có điểm dịch chuyển riêng trên Lịch / bản đồ.
- **Hàng bán:** chỉ vật phẩm mod, gồm đan, nguyên liệu, **vật phẩm tiến hóa** (vũ khí bản mệnh / Linh Bảo / Linh Trượng) và sách kỹ năng phẩm thấp.
- **Uy Danh Chợ:** tính theo tổng Tử Tinh đã tiêu ở chợ, chung toàn server (mỗi người chơi một mức). Có 5 bậc:

| Bậc | Tên | Mở khóa |
|---|---|---|
| 1 | Khách Lạ | Hàng cơ bản |
| 2 | Khách Quen | Giảm 5%, thêm nguyên liệu tiến hóa |
| 3 | Khách Quý | Giảm 10%, đan phẩm cao |
| 4 | Thượng Khách | **Quầy Đặc Biệt:** Hồn Cốt (hàng xoay vòng mỗi ngày Lịch), **Trục Nâng Cấp Kỹ Năng** |
| 5 | Tôn Khách | Hồn Cốt phẩm cao, trục bậc cao, đặt hàng theo yêu cầu |

- Khi mở bậc mới thì có thông báo kèm âm thanh; tab K hiện thanh Uy Danh.
- **Trục Nâng Cấp Kỹ Năng** là vật phẩm mới: dùng để nâng 1 cấp cho một kỹ năng. Mua được ở bậc 4–5, và rơi hiếm ở Bí Cảnh cấp cao.

### 12.3 Hồn Tinh thành tiền trong ví (giống Tử Tinh)
- **Hiện trạng:** Hồn Tinh vẫn rơi ra thành vật phẩm ở khoảng 8 nơi, ví dụ `chg/ChapHanhGia.java:479`, `entity/HuVuong.java:312`, `entity/WorldBoss.java:354`, `events/VoidRaid.java:301`, `endgame/DaKhuyet.java:168`. Hồn Tinh còn là hàng bán trong `currency/ShopCatalog.java:83-86`.
- **Mới:**
  - Thêm số dư Hồn Tinh vào ví (`currency/Wallet`).
  - Mọi chỗ rơi Hồn Tinh gọi một hàm chung `Wallet.grantHonTinh(p, n)`, hiện popup icon và cộng vào số dư.
  - Nếu nhặt hoặc còn vật phẩm Hồn Tinh trong túi thì tự đổi vào ví (giống `CurrencyEvents` của Tử Tinh). Save cũ cũng được đổi như vậy.
  - Tab K hiện số Hồn Tinh.
- **Giá nâng cấp tăng dần theo cấp:**
  - Công thức: `cost(n) = base × n^1.6`, cộng thêm ×1.5 ở mỗi mốc 10.
  - Ví dụ với base 5:

    | Cấp | 1 | 5 | 10 | 20 | 30 |
    |---|---|---|---|---|---|
    | Hồn Tinh | 5 | ~66 | ~199 + mốc | ~600 | ~1150 |

  - Áp dụng cho bản mệnh, Linh Bảo, Linh Trượng, Hồn Cốt. Hằng số đặt trong một file `UpgradeCost` dùng chung (DRY).

### 12.4 Vùng Hư Không Xâm Thực có sẵn khi tạo map
- **Hiện trạng:**
  - Tâm Hư Hóa sinh và lan dần theo thời gian (`events/CorruptionManager.java:61, 85, 169`).
  - Đã có giải phóng vùng (`liberation/LiberationManager.java:53`).
  - Nhưng chưa có vùng lớn đặt sẵn từ đầu.
- **Mới:**
  - **Khi tạo thế giới**, đặt sẵn khoảng 8–14 vùng xâm thực. Bán kính ngẫu nhiên 120–700 block, không đè lên thành, không ở quá gần điểm xuất phát.
  - Vùng càng xa điểm xuất phát thì càng to.
  - Cấp quái và độ mạnh boss trung tâm theo bán kính:

| Bán kính | Cấp quái | Boss trung tâm |
|---|---|---|
| 120–250 | 20–40 | Hư Vương thường |
| 250–450 | 40–65 | Hư Vương tinh anh + 1 phase |
| 450–700 | 65–**85** | Hư Vương cổ, 3 phase, scale theo số người |

  - **Vào vùng:** tự nhận nhiệm vụ "Giải Phóng [tên vùng]". Mục tiêu trỏ tới tâm vùng, kèm khoảng cách và cấp gợi ý. Vùng xuất hiện trên bản đồ và Lịch.
  - **Hạ boss:** vùng sạch dần từ tâm ra ngoài (dùng lại Liberation). Hư Ảnh của thành gần nhất giảm mạnh; thưởng Hồn Tinh vào ví và Văn Kiện / Di thư.
  - **Nối truyện:** mỗi vùng thuộc phe Dạ Khuyết (S0 `lore`). Vùng lớn nhất là nơi Sứ Đồ cầm quân. Giải phóng đủ số vùng là một điều kiện của kết thúc Siêu Độ (§11.2).
  - **Hiệu năng:** đánh dấu chunk ngay khi tạo thế giới (chỉ lưu dữ liệu), còn biến đổi địa hình / màu và sinh quái chỉ làm khi chunk được nạp.

### 12.5 Lộ trình cập nhật

| Đợt | Nội dung |
|---|---|
| **S0 + W0** | Phần nối cốt truyện (`StoryHooks`, `lore`, Di thư C) + Lịch K có làn Dự Ngôn |
| **T1** | Tách Ấn Hồn Đài / Khu Chợ, Uy Danh Chợ, Quầy Đặc Biệt, Trục Nâng Cấp Kỹ Năng |
| **T2** | Hồn Tinh vào ví + công thức giá nâng cấp |
| **T3** | Vùng xâm thực đặt sẵn khi tạo map + nhiệm vụ tự nhận + boss theo kích thước |
| Sau đó | W1 → … theo §7.2 |

**Fermi:** T1 + T2 + T3 cần khoảng 3–4 lượt agent ≈ 0,8–1M token.
