# GUIDE-v1 — Nền tảng chế tạo (B0) — API cho agent sau

**Phạm vi**: PLAN-v1-profession-crafting.md §0 (phẩm chất, sổ công thức + quyển phổ + UI, nguyên liệu) và "Người chơi cũ" (§5).
**Package**: `com.hukhong.craft` (server + dữ liệu), `com.hukhong.client.screen.profession.RecipeBookPanel` (UI).
**Chưa làm ở bước này**: minigame Luyện Đan / Phù / Đoán Tạo (§1–§3). Phần đó dùng các API bên dưới.

---

## 1. Phẩm chất — `CraftQuality` + `CraftRegistry`

| Thành phần | Ghi chú |
|------------|---------|
| `hukhong:craft_quality` (int 0..3) | `CraftRegistry.CRAFT_QUALITY`. Thang = `AlchemyMath.Quality` (0 Hạ, 1 Trung, 2 Thượng, 3 Cực). Dùng lại enum có sẵn, không tạo enum mới. |
| `hukhong:crafter` (String) | `CraftRegistry.CRAFTER`. Tên người chế. |
| `CraftQuality.mult(q)` | Hệ số hiệu lực: Hạ 0.85, Trung 1.00, Thượng 1.15, Cực 1.30. |
| `CraftQuality.stamp(stack, q, crafterName)` | Gắn phẩm + người chế. `crafterName` null/rỗng = giữ nguyên tên cũ. Trả về chính stack. |
| `CraftQuality.ordinal(stack)` | Phẩm hiện có, mặc định Trung. Đọc `craft_quality`, rơi về `pill_quality` (đan cũ). |
| `CraftQuality.tooltip(q, crafter)` | Dòng "Thượng phẩm · do <tên> chế". |

**Tooltip**: `CraftEvents.onTooltip` (ItemTooltipEvent) tự thêm dòng cho mọi stack có `craft_quality` hoặc `pill_quality`. Không cần đăng ký thêm.

**Cách gắn phẩm khi chế**:
```java
ItemStack out = ...;                       // kết quả
CraftQuality.stamp(out, CraftQuality.THUONG, sp.getName().getString());
```
- Đan Lô (`DanLoBlockEntity.finish`) đã gắn `craft_quality` + `pill_quality` và tên thợ.
- Trạm Phù / Đoán Tạo (`ProfessionStationBlockEntity.craft`) gắn mặc định **Trung** nếu kết quả chưa có `craft_quality`. Bước B2/B3 nên tự gắn phẩm thật trước dòng đó (kết quả đã có component thì không bị ghi đè).
- Nguyên liệu có phẩm (Phù Chỉ, Tôi Dịch) đã gắn sẵn qua công thức bàn thường: đọc bằng `CraftQuality.ordinal(stack)`.

---

## 2. Sổ công thức — `RecipeBook` (server)

### 2.1 Id thống nhất

| Tiền tố | Nguồn | Nghề |
|---------|-------|------|
| `dan:<id>` | `alchemy.AlchemyRecipes.ALL` (Đan Lô) | LUYEN_DAN |
| `phu:<id>` | `profession.ProfessionRecipes.PHU` (Phù Án) | PHU_VAN |
| `khi:<id>` | `profession.ProfessionRecipes.FORGE` (Đoán Tạo Đài) | DOAN_TAO |

Hằng số: `RecipeBook.DAN`, `RecipeBook.PHU`, `RecipeBook.KHI`. `RecipeBook.prefixOf(prof)` trả tiền tố của nghề.

### 2.2 API

```java
// Đọc
boolean knows(Player p, String id)                 // server
Set<String> ids(Player p)
RecipeBook.Entry find(String id)                   // null nếu không có
List<RecipeBook.Entry> entries(Profession prof)    // cả 3 trạm theo nghề
List<RecipeBook.Entry> all()

// Ghi (ServerPlayer)
boolean learn(ServerPlayer p, String id)           // false nếu đã biết; đồng bộ client
void autoLearnBasics(ServerPlayer p, Profession prof)   // học mọi công thức tier <= cấp/4+1
void onLevelUp(ServerPlayer p, Profession prof, int from, int to)  // đã gọi trong ProfessionService.addXp
void sendTo(ServerPlayer p)                        // đồng bộ thủ công (hiếm khi cần)

// Quyển phổ
ItemStack randomScroll(RandomSource r, Profession prof, int minTier, int maxTier, ServerPlayer forPlayer)
```

`RecipeBook.Entry` (record): `id`, `prof`, `tier`, `minLevel`, `display` (Supplier<ItemStack>), `ingredients` (List<Ing>), `requiredLevel()` = max(minLevel, 4*(tier-1)), `name()`.

Attachment: `CraftRegistry.KNOWN` (`KnownRecipes(Set<String> ids)`), copyOnDeath, lưu NBT. Client đọc từ `ClientRecipeBook.knows(id)` (đồng bộ bởi `RecipeBookNet.Sync`, gửi khi đăng nhập, hồi sinh, và mỗi lần học).

### 2.3 Quy tắc
- Công thức có `tier` (mặc định 1). Phù/Khí đặt tier theo nhóm 15 loại: `kind.ordinal()/3 + 1`.
- Tự học: khi chọn nghề (`ProfessionService.choose`), khi đăng nhập (`CraftEvents.onLogin`, cũng là migration cho người chơi cũ), khi lên cấp (`ProfessionService.addXp`). Idempotent.
- Đan Lô, Phù Án, Đoán Tạo Đài **từ chối** công thức chưa biết: báo `hukhong.recipe.unknown` ("Chưa lĩnh hội công thức").

---

## 3. Thêm công thức vào sổ

### 3.1 Công thức mới của trạm đã có (Phù / Khí)
Thêm `Recipe` vào `ProfessionRecipes.FORGE` bằng `registerForge(...)` (đã có). Cần truyền **tier** qua constructor 7 tham số:

```java
ProfessionRecipes.registerForge(new ProfessionRecipes.Recipe(
        "tinh_tam_quan", Profession.DOAN_TAO,
        List.of(new ProfessionRecipes.Need(() -> ..., 4)),
        40,
        (lv, up) -> new ItemStack(...),
        1,      // minLevel
        2));    // tier
```
Constructor 5 tham số (tier=1, minLevel=1) và 6 tham số (tier=1) vẫn có.

Công thức được hiển thị trong sổ và học được **ngay khi thêm vào `FORGE`** (không cần đăng ký thêm), miễn là có `tier`.

### 3.2 Công thức Đan Lô
Thêm vào `AlchemyRecipes.ALL` (đã có `tier`, `needs`, `result`, `yield`). Không cần làm gì thêm: `RecipeBook.all()` đọc trực tiếp.

### 3.3 Công thức mới cho một nghề khác
Thêm một tiền tố và một nhánh trong `RecipeBook.all()`, `prefixOf()` và `RecipeScrollItem.Kind`.

---

## 4. Quyển phổ

| Item | Id registry | Tiền tố | Nghề cần |
|------|-------------|---------|----------|
| Đan Phương | `hukhong:dan_phuong` | `dan:` | LUYEN_DAN |
| Phù Phổ | `hukhong:phu_pho` | `phu:` | PHU_VAN |
| Khí Phổ | `hukhong:khi_pho` | `khi:` | DOAN_TAO |

- Id công thức nằm trong data component `hukhong:recipe_id`. Tạo stack: `CraftRegistry.scroll(RecipeScrollItem.Kind.DAN, "dan:khai_mach")`.
- Chuột phải: sai nghề → báo; đã biết → báo; đúng nghề → học, tiêu 1 (trừ khi creative).
- Tên hiển thị "Đan Phương: <tên công thức>" (tên lấy từ `RecipeBook.find(id).name()`).

**Nguồn rơi / bán / thưởng** (đã nối):
- Boss: `CraftEvents.onDrops`, 4%, nghề ngẫu nhiên, bậc 2..(2/3/5 theo cấp boss).
- Cửa hàng: `ShopCatalog.scrolls` (mỗi nghề 1 quyển bậc 2 hoặc 3, giá 1200 / 4000 Hạ).
- Thưởng cấp nghề: `RecipeBook.onLevelUp`, mỗi 4 cấp nhận 1 quyển ngẫu nhiên đúng nghề, bậc tới `cấp/4+2`, chỉ công thức chưa biết.

---

## 5. Sổ công thức trong màn hình — `RecipeBookPanel`

```java
// Trong init(), SAU super.init() (cần leftPos/topPos):
book = new RecipeBookPanel(font, Profession.DOAN_TAO, leftPos - RecipeBookPanel.W - 4, topPos);

// render(): sau super.render(), trước renderTooltip
book.render(g, font, mx, my);

// Chuyển tiếp chuột (trả true = đã xử lý):
@Override public boolean mouseClicked(double mx, double my, int btn) {
    if (book != null && book.mouseClicked(mx, my, btn)) return true;
    return super.mouseClicked(mx, my, btn);
}
@Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    if (book != null && book.mouseScrolled(mx, my, sy)) return true;
    return super.mouseScrolled(mx, my, sx, sy);
}
```
- Panel tự lọc công thức đã biết của nghề đó, ghi cấp nghề client (`ClientProfession`), xám nếu thiếu cấp.
- Nút "Xếp nguyên liệu" gửi `RecipeBookNet.Arrange(id)`. Server gọi `menu.arrange(sp, id)` → `be.arrange(...)` → `RecipeBook.arrange(sp, box, slots, id, prof)`.
- Hiện có trong `DanLoScreen` (nghề LUYEN_DAN) và `StationScreen` (PHU_VAN / DOAN_TAO theo trạm).

**Để trạm mới dùng được**: thêm `arrange(ServerPlayer, String)` vào BE và Menu (làm theo `DanLoMenu.arrange`), và thêm nhánh trong `RecipeBookNet.arrangeOpen`.

**Xếp nguyên liệu** (`RecipeBook.arrange`): kiểm tra đủ nguyên liệu (túi + ô hiện có), trả về túi các món không thuộc công thức trong ô, rồi chuyển đúng số lượng vào ô. Công thức cần nhiều hơn số ô (vd Thần Đan có 4 loại, Đan Lô 3 ô) sẽ báo `hukhong.recipe.no_slot`.

---

## 6. Nguyên liệu mới (§0.3)

| Item | Id | Nguồn | Phẩm |
|------|----|-------|------|
| Chu Sa | `hukhong:chu_sa` | Quặng redstone (`CraftEvents.onBreak`, 30%, bỏ qua khi có Silk Touch); cửa hàng thảo dược (3 cái, 2 ngọc) | — |
| Phù Chỉ Hạ / Trung / Thượng | `hukhong:phu_chi_ha` / `_trung` / `_thuong` | Bàn thường: giấy + Linh Thảo / Huyết Tham / Hỏa Liên | craft_quality 0 / 1 / 2 (gắn qua công thức) |
| Linh Mặc | `hukhong:linh_mac` | Bàn thường: Chu Sa + mực + Linh Thạch Hạ | — |
| Tôi Dịch | `hukhong:toi_dich` | Bàn thường: chai nước (`minecraft:potion`) + Thanh Diệp | craft_quality 1 |
| Linh Thán | `hukhong:linh_than` | Bàn thường: than + Linh Thạch Hạ | — |

Lưu ý:
- Công thức Tôi Dịch dùng `minecraft:potion` (bất kỳ thuốc nào). Việc chỉ nhận chai nước cần component matching, chưa làm.
- Đan Lô vẫn dùng điều kiện bậc cũ `tier > lv/2+1` (`DanLoBlockEntity.start`), chưa đổi theo quy tắc tự học `tier <= lv/4+1`. Bước B1 quyết định.
- Đan Thần (`than_dan`) có 4 loại nguyên liệu, Đan Lô chỉ có 3 ô: công thức này không chế được và không xếp được (báo `no_slot`).
- Texture: `tools/gen_craft_items.py` (Pillow) → `textures/item/`. Model: `models/item/<id>.json`. Công thức: `data/hukhong/recipe/<id>.json`.

---

## 7. Lang và creative

- Khóa mới ở cuối `vi_vn.json` và `en_us.json`: `item.hukhong.*` (nguyên liệu, quyển phổ), `hukhong.craft.*`, `hukhong.recipe.*`, `hukhong.scroll.*`.
- Creative: mọi nguyên liệu và quyển phổ ở tab Ingredients (`CraftRegistry.register`).

## 8. File chính

| File | Vai trò |
|------|---------|
| `craft/CraftRegistry.java` | Component, attachment, item nguyên liệu/quyển phổ, creative |
| `craft/CraftQuality.java` | Phẩm: mult, stamp, tooltip |
| `craft/KnownRecipes.java` | Attachment (Set id) |
| `craft/RecipeBook.java` | API sổ công thức + xếp nguyên liệu |
| `craft/RecipeScrollItem.java` | Quyển phổ (tên động, chuột phải học) |
| `craft/RecipeBookNet.java` | Payload Sync / Arrange |
| `craft/ClientRecipeBook.java` | Cache client |
| `craft/CraftEvents.java` | Login/respawn, Chu Sa, boss, tooltip |
| `client/screen/profession/RecipeBookPanel.java` | UI tab sổ công thức |
| `profession/ProfessionService.java` | Hook: choose → autoLearn; addXp lên cấp → onLevelUp |
