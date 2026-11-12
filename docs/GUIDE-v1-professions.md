# GUIDE-v1 — Nghề phụ (Professions) — API cho agent sau

**Phạm vi**: hạng mục E (nghề phụ + 2 trạm chế tạo). Pháp khí hộ thể (F) dùng API bên dưới.
**Package**: `com.hukhong.profession` (trạm: `com.hukhong.profession.station`, UI: `client.screen.profession`, `client.screen.profile.NgheTab`).

## 1. Dữ liệu

| Thành phần | Vị trí | Ghi chú |
|------------|--------|---------|
| `Profession` | `profession/Profession.java` | `NONE, LUYEN_DAN, PHU_VAN, DOAN_TAO`; `id` (luyen_dan…), `vi`, `nameKey()`, `descKey()`, `needKey()` |
| `ProfessionData(profession, level, xp)` | `profession/ProfessionData.java` | Record, Codec |
| Attachment `hukhong:profession` | `ProfessionRegistry.DATA` | copyOnDeath, lưu NBT |
| `ProfessionMath` | `profession/ProfessionMath.java` | `cap(realmIndex)`, `need(level)`, hằng số Kim/Đan |
| `ClientProfession.get()` | `profession/ClientProfession.java` | Bản sao phía client |

Công thức:
- **Trần cấp** = `min(20, 1 + realmIndex*2)`, `realmIndex = CultivationData.realmIndex()` (= `Realm.indexOf(level)`).
- **XP lên cấp** `need(lv) = round(20 * lv^1.5)`. Tại trần, thanh xp kẹp ở `need(cap)`.

## 2. ProfessionService (API chính)

```java
// Đọc (Player, server + client)
ProfessionData get(Player p)
Profession profession(Player p)
int level(Player p, Profession prof)          // 0 nếu không mang nghề đó
int cap(Player p)                             // trần theo cảnh giới
boolean kimBonus(Player p)                    // DOAN_TAO + Pháp Tu hệ KIM

// Ghi (ServerPlayer) — mọi lệnh đều gửi Sync cho client
boolean choose(ServerPlayer p, Profession prof)          // chỉ khi đang NONE, vĩnh viễn
void addXp(ServerPlayer p, Profession prof, long amount) // chỉ khi p đang mang đúng prof; lên cấp -> thông báo + âm thanh
void addCraftXp(ServerPlayer p, Profession prof, long base) // như addXp, nhưng DOAN_TAO + kimBonus được x1.5

// Bonus rèn (Đoán Tạo Sư + Kim)
int rollTierBonus(Player p, RandomSource r)   // 0 hoặc 1 (xác suất ProfessionMath.KIM_TIER_CHANCE = 0.30)
```

## 3. Công thức chế tạo (ProfessionRecipes)

```java
public record Need(Supplier<? extends Item> item, int count) {}
public record Recipe(String id, Profession prof, List<Need> needs, int xp, Output out) {}
@FunctionalInterface public interface Output { ItemStack make(int level, boolean tierUp); }

ProfessionRecipes.PHU     // Phù Án: 15 công thức (mỗi Pháp Khí phù/trận) — KHÔNG sửa khi làm giáp
ProfessionRecipes.FORGE   // Đoán Tạo Đài: CopyOnWriteArrayList
ProfessionRecipes.registerForge(Recipe r)  // THÊM công thức rèn — dùng cho 4 pháp khí hộ thể
```

- Mỗi công thức khớp **đúng** các `Need` trên 3 ô nguyên liệu (không thừa ô nào, đủ số lượng). Thứ tự ô không quan trọng.
- `out.make(level, tierUp)`: `level` = cấp nghề hiện tại của thợ; `tierUp` = true khi Kim bonus trúng → nên cộng +1 bậc phẩm.
- Ví dụ đăng ký giáp (đợt sau), gọi lúc khởi động mod:
```java
ProfessionRecipes.registerForge(new ProfessionRecipes.Recipe("tinh_tam_quan", Profession.DOAN_TAO,
        List.of(new ProfessionRecipes.Need(() -> ..., 4)), 40,
        (lv, up) -> new ItemStack(...)));
```
- Đoán Tạo Đài hiện có 1 công thức placeholder: **Linh Thiết Phôi** = 4 sắt thỏi (`Items.IRON_INGOT`) + Linh Thạch Hạ (`ResourceRegistry.MATERIALS "linh_thach_ha"`).

## 4. Trạm

| Block | Đăng ký | Nghề | Ô |
|-------|---------|------|---|
| Phù Án `hukhong:phu_an` | `ProfessionRegistry.PHU_AN` | PHU_VAN | 3 vào (0-2) + 1 ra (3) |
| Đoán Tạo Đài `hukhong:doan_tao_dai` | `ProfessionRegistry.DOAN_TAO_DAI` | DOAN_TAO | như trên |

- Cả hai dùng chung `ProfessionStationBlockEntity` (craft khi bấm nút, menu button id 0), `StationMenu`, `StationScreen`.
- `ProfessionStationBlockEntity.craft(ServerPlayer)`: kiểm tra ô ra trống → khớp công thức → đúng nghề (`level > 0`) → tiêu nguyên liệu → ra kết quả → cộng xp.
- Recipe bàn crafting: `data/hukhong/recipe/phu_an.json`, `doan_tao_dai.json`.

## 5. Đan Lô (Luyện Đan Sư)

Trong `DanLoBlockEntity.start()`:
- Không có nghề LUYEN_DAN → báo `hukhong.profession.need.luyen_dan`, không luyện.
- `recipe.tier > level/2 + 1` → báo `hukhong.profession.tier_locked` (tier 1 mở ở cấp 1).
- Khi bắt đầu lưu `brewer` (UUID) và `brewLevel`. Khi ra đan: số viên +1 với xác suất `brewLevel * 3%`; cộng xp `5 * tier` cho thợ nếu còn online.

## 6. Lệnh, mạng, UI

- Lệnh: `/hukhong nghe <luyen_dan|phu_van|doan_tao>` (quyền 2, `ProfessionCommand.node()` gắn trong `HuKhongCommand`).
- Mạng (`ProfessionNet`): `Sync(prof, level, xp)` server→client (gửi khi login, respawn, và mỗi lần đổi dữ liệu); `Choose(prof)` client→server (server kiểm tra lại).
- Tab **Nghề** (index 8) trong `ProfileScreen`: khi chưa có nghề, bấm một nghề rồi bấm lần nữa để xác nhận; khi có nghề, hiện cấp / trần / thanh xp.

## 7. Ngôn ngữ, tài nguyên, tools

- Lang: các khóa `hukhong.profession.*`, `hukhong.station.*`, `block.hukhong.phu_an|doan_tao_dai`, `item.hukhong.linh_thiet_phoi` (vi_vn + en_us, cuối file).
- Texture: `tools/gen_profession_blocks.py` (Pillow) sinh 16x16 placeholder.
- Creative: 2 trạm vào tab Functional Blocks; Linh Thiết Phôi vào tab Ingredients (`ProfessionRegistry.register`).

## 8. Chưa làm / lưu ý

- Chưa chạy trong game (theo yêu cầu). Chỉ kiểm tra bằng `./gradlew build`.
- Bonus Kim "+1 bậc phẩm" được hiểu là 30% cơ hội mỗi lần rèn (`KIM_TIER_CHANCE`); chỉnh trong `ProfessionMath`.
- Codec của block `ProfessionStationBlock` chỉ trả về Phù Án (chỉ ảnh hưởng đến serialize định nghĩa block, không ảnh hưởng gameplay).
- Phù Án dùng Giấy + nguyên tố vanilla + Linh Thảo (3 ô). Nguyên tố: KIEM=sắt nhỏ, HOA=bột lửa, LOI=lapis, BANG=tuyết, THO=đất sét, MA=hoa héo, QUANG=bột phát sáng, HO_THAN=vàng nhỏ, KIM_CANG=sắt thỏi, DON_KHONG=ngọc ender, HOI_XUAN=dưa lát, TU_LINH=ngọc lục bảo, KHON=cát linh hồn, SAT=kem magma, HO_TRAN=kim cương.
- Đan Lô: nếu thợ offline lúc ra đan thì không nhận xp (đan vẫn ra).
- Lang hiện có chữ "Pháp Tu hệ Kim" trong mô tả; kiểm tra lại văn phong khi duyệt.
