# S5 — Kiến trúc kỹ thuật & Roadmap

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2.1 (R1, R3, R8, R16, R17, R19, R20 từ PLAN-v2 §14); trước đó v2: áp dụng PLAN-v2 canon (§1.1, §3, §6, §7, §8, §10, §12); thêm 9 module, 4 schema mới, roadmap P0→P3 + quy trình AI↔user
**Status**: v2.1 (không còn câu mở)
**Nguồn**: `PLAN-v2-tu-tien-mod-canon.md`, `S5-tech.md` v1, R2-tech-neoforge.md. Tên API lấy từ kiến thức NeoForge 1.21.1; chỗ nào có **[KIỂM TRA]** phải đối chiếu docs.neoforged.net / source trước khi dựa vào. **[GIẢ ĐỊNH]** = S5 tự diễn giải chỗ canon chưa nói, giữ nguyên trừ khi user sửa.

## Thay đổi v2.1

- **R1**: đồng hồ tuổi duy nhất = `onlineTicks` cá nhân (24.000 tick = 1 tháng; chỉ khi online, chặn AFK; ngủ và `/time set` không tính); viết lại §3.8; Ngũ Suy dừng ở V, không chết vì tuổi.
- **R3**: `KillRateGate` trong `gates` (>30 con cùng loài / 64 block / 10 phút → ×0,1), kèm code ngắn ở §3.2; mọi đạo dùng chung.
- **R8**: Ấn Hồn Đài / Phá Chấp roll bằng `rollSeed` lưu trước khi mở UI (module `cores`, §5.3).
- **R16**: n boss = số người gây ≥5% sát thương hoặc hồi máu đáng kể trong 48 block; loot cần đóng góp tối thiểu (§3.3).
- **R17**: Huyết Tế quota toàn cục/người/ngày; dân làng nhân giống ×0,1; giết dân làng → Ác Danh (§3.6).
- **R19**: tiến độ manh mối CHG chung toàn server; sự kiện sớm cần party ≥2 hoặc cảnh giới ≥ cấp CHG −5 (§3.9, §4.4).
- **R22**: module Linh Khí Đài (§3.10): `SpiritAltar` BlockEntity + `AfkTracker` phân biệt AFK tại đài vs ngoài, tuổi trôi khi Bế Quan, chặn Bình Cảnh, đi qua `RewardGate`; xếp P1 cùng tông môn (§6).
- **R23**: Tẩu Hỏa Nhập Ma = phế tu vĩnh viễn: `tauHoaNhapMa` reset toàn bộ tu vi (cấp 1, Lõi, Hồn Cốt, đạo, tư chất), giữ túi đồ, vô hiệu đồ bind; 3 tầng cảnh báo (§3.7).
- **R24**: worldborder bán kính 15.000 (`setSize(30000)`, đường kính 30.000); vị trí thành/hang ổ trong ±15.000 (§3.8, §3.9).
- **R25**: keybind Q/E/R/T + phím G bật/tắt "Thế Tu Luyện", xử lý xung đột phím vanilla Q/E (§3.7b).
- **R20**: Đế Hoàn Đan (phẩm 6) vào P1 (§6). Xóa 2 câu hỏi mở đã giải quyết.

---

## Thay đổi v2

| # | Thay đổi | Lý do (canon) |
|---|----------|---------------|
| 1 | Chốt nền tảng: NeoForge 1.21.1, chỉ GeckoLib, AI viết code, dedicated server từ P0. Bỏ bảng so sánh dài (v1 §1) và câu hỏi 1-4 (đã trả lời) | §12 |
| 2 | Thêm module: `age`, `sect`, `city`, `alchemy`, `breakthrough`, `apostles`, `executors`, `worldtier`, `scaling` | §1.1, §6-§11 |
| 3 | Hiện thực bằng event: spawner = 0 EXP, boss HP scale theo người, cấm bay vùng boss, đa đạo ×2/×3, Huyết Tế chỉ từ NPC, Ác Tính → Mất Kiểm Soát → Tẩu Hỏa, worldborder bán kính 15.000, reset tu vi khi Tẩu Hỏa | §1.1, §3, §3.1, §10, §9 |
| 4 | Schema JSON mới: đan dược + công thức, tông môn, Sứ Đồ (bảng rơi Văn Kiện), sự kiện Chấp Hành Giả | §7, §9, §11 |
| 5 | Roadmap P0→P3 viết lại; thêm quy trình làm việc AI viết code ↔ user chạy thử | §12 |
| 6 | Rủi ro mở rộng R1→R18; thay "Câu hỏi cho user" bằng 2 câu còn mở | — |

## Tóm tắt (Pyramid)

**NeoForge 1.21.1 + ModDevGradle + Java 21 + GeckoLib.** Trạng thái người chơi chia thành **vài Data Attachment nhỏ theo module** (`CultivationData` lõi + `AgeData`, `SectMembership`, `AlchemyData`...), trạng thái toàn server nằm trong **`SavedData`** (thành, tông, xâm thực, Chấp Hành Giả, Tầng Thế Giới). Mọi "luật cứng" là event hook nhỏ đi qua **các cổng thống nhất** (`Resources.tryConsume`, `RewardGate`, `FlightRules`, `DamageRules`) để đa đạo, spawner, boss không bị lọt đường vòng. Nội dung (Lõi, đan, công thức, tông, Sứ Đồ, Chấp Hành Giả) là **datapack JSON**. P0 vẫn nhỏ (Đạo+Thể, cấp 1-30, 1 thành, 10 Lõi, Bình Cảnh mốc 10/20) nhưng **chạy được multiplayer trên dedicated server ngay từ đầu**.

---

## 1. Nền tảng (đã chốt)

| Quyết định | Lý do ngắn |
|------------|-----------|
| **NeoForge 1.21.1**, Java 21, ModDevGradle | API ổn định, tài liệu/mã mẫu nhiều nhất → AI viết code chính xác nhất; Attachment, payload codec, `LivingIncomingDamageEvent` có sẵn |
| **Chỉ GeckoLib** (quái + cánh Đạo tu) | Ít phụ thuộc nhất; Hồn Cốt tự làm slot, **không dùng Curios**, không Iron's Spells (khó giữ luật Đạo/Thể riêng) |
| **AI viết code**, user build/chạy thử/báo lỗi | Quy trình ở mục 6.3 |
| **Dedicated server từ P0** | Logic luôn ở server; client chỉ vẽ HUD/UI; test `runServer` + 2 client ở mọi mốc |
| Không port 26.x / Fabric | Port chỉ cân nhắc sau P3, ngoài phạm vi tài liệu này |

---

## 2. Kiến trúc module

```
+==============================================================================+
|                      HUKHONG v2 — COMPONENT DIAGRAM                          |
+==============================================================================+

 CLIENT ONLY                  COMMON (server = nguồn sự thật)
┌───────────────┐   ┌───────────────────────────────────────────────────────────┐
│ ui            │   │ core: CultivationData (Attachment), CultivationService     │
│  CharCreate   │   │   addExp / lock / realm table                              │
│  HUD layers   │◄──┤                                                            │
│  SoulSealScr  │   └──┬──────────┬───────────┬────────────┬─────────────────────┘
│  Alchemy scr  │      │          │           │            │
│  keybinds     │      ▼          ▼           ▼            ▼
└──────▲────────┘  ┌────────┐ ┌─────────┐ ┌─────────┐ ┌───────────────────────┐
       │ payload   │ age    │ │break-   │ │ cores   │ │ gates (cổng thống nhất)│
┌──────┴────────┐  │ tuổi,  │ │through  │ │ skills  │ │ Resources.tryConsume   │
│ network       │  │ thọ    │ │ Bình    │ │ datapack│ │ RewardGate (spawner)   │
│ Sync*Payload  │  │ nguyên │ │ Cảnh    │ └────┬────┘ │ FlightRules            │
│ OpenSoulSeal  │  └────────┘ └────┬────┘      │      │ DamageRules            │
│ CastSkill     │                  │           │      └───────────┬────────────┘
│ ControlLost   │            ┌─────▼───────────▼──┐               │
└───────────────┘            │ alchemy            │      ┌────────▼───────────┐
                             │ Đan Lô, recipe,    │      │ pathrules          │
 SERVER-WIDE (SavedData)     │ Đan Độc, PillItem  │      │ 4 đạo + đa đạo +   │
┌─────────────────────────┐  └────────────────────┘      │ Ma tu (Ác Tính)    │
│ city   : 10 thành,      │                              └────────────────────┘
│          chức vị,       │  ┌────────────────────┐   ┌────────────────────────┐
│          1 Thành Chủ    │  │ sect: 7 tông, ứng  │   │ scaling: HP boss theo  │
│ sect   : sổ tông, quest │  │ tuyển, bậc, buff   │   │ n người (48 block),    │
│ executors: CHG + lair   │  └────────────────────┘   │ ActiveBosses, no-fly   │
│ worldtier: Tầng I / II  │  ┌────────────────────┐   └────────────────────────┘
│ invasion/corruption     │  │ apostles: Sứ Đồ,   │   ┌────────────────────────┐
└────────────┬────────────┘  │ Văn Kiện → manh mối│   │ mobs/entities: GeckoLib│
             │               └─────────┬──────────┘   │ Hư Hóa, boss, Sứ Đồ    │
             └──────────────►executors◄┘              └────────────────────────┘
```

| Module | Trách nhiệm | Dữ liệu / API chính |
|--------|-------------|---------------------|
| `core` | Trạng thái người chơi, EXP, khóa mốc, nhập đạo | `AttachmentType<CultivationData>` (Codec, `copyOnDeath`) |
| `age` | Tuổi theo **`onlineTicks` cá nhân** (24.000 tick = 1 tháng tuổi; chỉ khi online, chặn AFK), Thọ Nguyên, Thiên Nhân Ngũ Suy (dừng ở V, không chết vì tuổi — R1) | `AgeData` attachment; `PlayerTickEvent.Post` cộng `onlineTicks` mỗi 20 tick |
| `breakthrough` | Bình Cảnh ở cấp 9/19/…/89: trạng thái, yêu cầu EXP ×k nếu không vật liệu, Phá Cảnh Đan / Thiên Địa Linh Vật, Tâm Kiếp 1% | Cờ trong `CultivationData`; datapack registry `breakthrough_material`; hook trong `CultivationService.addExp` |
| `cores` | Lõi/Kỹ năng/Set từ JSON, **Ấn Hồn Đài 4 ô** (3 Lõi + "Phó Mặc Số Phận") | Datapack registry (`DataPackRegistryEvent.NewRegistry`, sync client) **[KIỂM TRA]**; `MenuType`; server roll bằng `rollSeed` **lưu vào attachment trước khi mở UI** (R8: relog/thoát không reroll) + lưu offer đang chờ |
| `pathrules` | Luật cứng 4 đạo, đa đạo, Ma tu (Hấp Huyết, Huyết Tế, Ác Tính) | `LivingIncomingDamageEvent`, `LivingDeathEvent`, `PlayerTickEvent.Post` |
| `gates` | 4 cổng dùng chung: chi phí tài nguyên, thưởng (EXP/Hồn Tinh/vật liệu; kèm **`KillRateGate`**, R3), bay, sát thương | Class tĩnh; **mọi** đường tiêu/nhận phải đi qua (AP1, AP12) |
| `alchemy` | Đan Lô (BlockEntity + Menu), công thức luyện, Đan Hỏa, Đan Độc, phẩm Nhất→Cửu + Thần | Custom `RecipeType`/`RecipeSerializer` **[KIỂM TRA]**; datapack registry `pill`; 1 `PillItem` + data component |
| `sect` | 7 tông: mời ngẫu nhiên, ứng tuyển (tư chất + cảnh giới + tuổi), bậc đệ tử, nhiệm vụ riêng, buff | `SectMembership` attachment; `SectSavedData`; datapack registry `sect`, `sect_quest` |
| `city` | 10 thành cố định, chức vị Cư Dân→Thành Chủ, **1 Thành Chủ/server**, Phế Tích, tái chiếm | `CitySavedData` (xác thực chức vị tại server, 1 thread); `CityRank` attachment |
| `apostles` | Sứ Đồ Dạ Khuyết (cấp ≤ 65), Văn Kiện rơi theo cấp | Datapack registry `apostle`; item `Văn Kiện` + data component `ClueData` |
| `executors` | 4 Chấp Hành Giả: event scheduler, tiến độ manh mối, **hang ổ theo seed** | `ExecutorsSavedData`; datapack registry `executor`; `LevelTickEvent.Post` **[KIỂM TRA]** tên event |
| `worldtier` | Tầng Thế Giới I/II (sau kết thúc Phong Ấn/Hấp Thụ) | `WorldTierSavedData`; hệ số quái/loot theo tầng; cờ hương vị kết thúc |
| `scaling` | HP boss theo n người trong 48 block, loot chia theo người tham gia, `ActiveBosses` | `AttributeModifier` trên `MAX_HEALTH`; xem 3.2 |
| `flight` | Bay theo đạo + cấp + vùng boss | `Abilities.mayfly`, `FlightRules` |
| `entity` | Quái Hư Hóa, Sứ Đồ, Hư Tu, boss | GeckoLib, `EntityAttributeCreationEvent` |
| `worldgen` | 10 thành (jigsaw, vị trí cố định trong ±15.000, R24), Hư Hóa lan | `structure`/`template_pool`; `CorruptionSavedData` |
| `events` | Tập kích thành (chỉ khi có người liên quan online) | `InvasionScheduler` |
| `ui`/`network` | Tạo nhân vật, HUD, Ấn Hồn Đài, Đan Lô; đồng bộ | `RegisterGuiLayersEvent`, `RegisterPayloadHandlersEvent`, `StreamCodec.composite` |

**Nguyên tắc**: (1) mọi thay đổi trạng thái xảy ra trên **server**; client chỉ nhận payload để vẽ. (2) Mọi tiêu tài nguyên / nhận thưởng / bay / sát thương đi qua **cổng `gates`**: không viết đường tắt trong skill hay item. (3) Dữ liệu người chơi chia attachment theo module để sync nhỏ và đổi schema độc lập (R9).

---

## 3. Hiện thực luật bằng event / API

### 3.1 Bảng luật → hook

| Luật (canon) | Event / hook | Ghi chú |
|--------------|--------------|---------|
| Spawner = 0 EXP/Hồn Tinh/vật liệu (§1.1) | `FinalizeSpawnEvent` đánh dấu entity (`MobSpawnType.SPAWNER`, `TRIAL_SPAWNER`); `RewardGate` + `LivingDropsEvent` lọc | Mục 3.2. Dấu lưu bằng attachment `serialize(Codec.BOOL)` để sống qua unload chunk |
| Chống farm cùng loài (R3) | `LivingDeathEvent` → `RewardGate.reward(victim)` gọi `KillRateGate.multiplier` | >30 con / 64 block / 10 phút → thưởng giảm dần đến ×0,1; **mọi đạo**, Ma tu dùng chung. Mục 3.2 |
| Boss HP scale (§10) | `scaling.BossScaling.update()` gọi từ `customServerAiStep()` của boss mỗi 20 tick | Mục 3.3 |
| Cấm bay vùng boss (§10) | `FlightRules.canFly` kiểm `ActiveBosses` (boss thế giới luôn; CHG/boss cuối theo cờ `no_fly`) + hủy `isFallFlying()` | Mục 3.4 |
| Đa đạo: chi phí ×2/×3 (§3.1) | Cổng `Resources.tryConsume` nhân `PathMultiplier(paths.size())` | Phạt EXP song ×1.6 / tam ×2.5 nằm trong `addExp`. Mục 3.5 |
| Đa đạo: xung đột theo nguồn đòn (§3.1) | `DamageRules.resolve()` xét tag `hukhong:spell`, `hukhong:the_skill`, `hukhong:flying_sword` | Mục 3.5 |
| Đạo tu không đói | `PlayerTickEvent.Post` đặt `FoodData`; mạnh hơn: mixin chặn `causeFoodExhaustion` | **[KIỂM TRA]** client nhấp nháy |
| Đạo tu đòn thường lên **quái** = 0, lên **người chơi** ×1 (§3) | `LivingIncomingDamageEvent`: target `instanceof ServerPlayer` → bỏ qua luật | Phép có `DamageType` riêng + tag |
| Đạo tu hỗ trợ đồng đội (buff Máu/Tấn Công/Tốc Độ/Linh Lực) | Skill hỗ trợ = `MobEffect` / `AttributeModifier` tạm trên đồng đội (cùng nhóm/party) | **[GIẢ ĐỊNH]** "đồng đội" = người chơi cùng team vanilla hoặc trong 32 block; S2 chốt |
| Thể tu không dùng vũ khí | `LivingIncomingDamageEvent` (tay cầm thuộc tag `hukhong:weapons`) + `LivingEntityUseItemEvent.Start` hủy cung/nỏ | Phi kiếm từ Bản Mệnh Kiếm (đa đạo) hợp lệ |
| Bay Kiếm 20 / Đạo 60 / Thể 60 / Ma Kiếm 20 | `FlightRules` | Thể tu chỉ trừ Bá Khí |
| Hấp Huyết nội tại (Ma tu, mọi mục tiêu) | `LivingDamageEvent.Post`: `attacker.heal(dealt * pct)` | Dùng sát thương thực sau giáp. Hấp huyết là hồi máu, **không** phải "Huyết Tế" |
| Huyết Tế chỉ từ NPC (§3) | `LivingDeathEvent` + `HuyetTe.isValidSource()` | Quota + dân làng nhân giống ×0,1 + Ác Danh (R17). Mục 3.6 |
| Ma tu không hỗ trợ người khác (§3) | `SupportGuard.canCast(caster, skill)` trong `CastSkillPayload` handler | **[GIẢ ĐỊNH]** có Ma tu trong `paths` → mọi skill tag `support` bị chặn, kể cả khi đa đạo |
| Ác Tính → Mất Kiểm Soát → Tẩy Hỏa Nhập Ma (§3) | `EvilService` + tick + `ControlLostPayload` | Mục 3.7 |
| Khóa EXP ở mốc 10 cấp (§1.1) | `CultivationService.addExp` kiểm `level % 10 == 0 && !coreChosen[level/10]`; chặn `PlayerXpEvent.XpChange`, `LivingExperienceDropEvent` | |
| Bình Cảnh ở cấp 9/19/…/89 (§1.1) | `breakthrough` trong `addExp`: không có `bypass` → EXP cần ×k (S3 hiệu chỉnh) | Mốc phụ 91-95/96/99: Bình Cảnh nhẹ ×3 |
| Worldborder bán kính 15.000 (§9, R24) | `ServerStartedEvent`: set size 30000 (đường kính) tâm (0,0), **một lần** (cờ SavedData để admin đổi được) | Mục 3.8 |
| Tập kích chỉ khi có người online liên quan (§8) | `InvasionScheduler` kiểm cư dân/chức vị online hoặc người chơi trong bán kính; không có → hoãn | |
| Thành Chủ duy nhất (§8) | `CitySavedData.promoteToLord()` kiểm `lordUuid == null || NPC` trong 1 thao tác server | Server tick đơn luồng nên không race; vẫn kiểm điều kiện tại thời điểm ghi |

### 3.2 Code — spawner = 0 EXP

```java
public final class SpawnerMarks {
    public static final Supplier<AttachmentType<Boolean>> SPAWNER_MOB =
        ATTACH.register("spawner_mob", () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL).build());          // [KIỂM TRA] chữ ký serialize ở 1.21.1

    @SubscribeEvent
    public static void onFinalize(FinalizeSpawnEvent e) {
        MobSpawnType t = e.getSpawnType();
        if (t == MobSpawnType.SPAWNER || t == MobSpawnType.TRIAL_SPAWNER)   // [KIỂM TRA] tên enum
            e.getEntity().setData(SPAWNER_MOB, true);
    }
    public static boolean is(LivingEntity e) { return e.hasData(SPAWNER_MOB) && e.getData(SPAWNER_MOB); }
}

public final class RewardGate {            // MỌI nguồn EXP / Hồn Tinh / vật liệu gọi hàm này
    public static boolean canReward(LivingEntity victim) { return !SpawnerMarks.is(victim); }
    /** Hệ số thưởng 0..1 cho MỌI nguồn EXP / Hồn Tinh / vật liệu (R3). */
    public static float reward(LivingEntity victim) {
        return canReward(victim) ? KillRateGate.multiplier(victim) : 0f;
    }
}

public final class KillRateGate {          // R3: MỘT cổng chống farm, mọi đạo, nằm trong module gates
    static final int COUNT = 30, WINDOW = 12_000 /* 10 phút */, CAP = 256;
    static final double RADIUS = 64; static final float FLOOR = 0.1f;
    private static final Map<ResourceLocation, ArrayDeque<long[]>> KILLS = new HashMap<>(); // loài -> {tick,x,z} (bộ nhớ, ≤CAP/loài)

    public static float multiplier(LivingEntity v) {
        long now = v.getServer().getTickCount();                // tick server, chỉ cho cửa sổ chống farm (không liên quan tuổi)
        var q = KILLS.computeIfAbsent(BuiltInRegistries.ENTITY_TYPE.getKey(v.getType()), k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst()[0] > WINDOW) q.pollFirst();
        int n = 0;
        for (long[] k : q) { double dx = k[1] - v.getX(), dz = k[2] - v.getZ(); if (dx*dx + dz*dz <= RADIUS*RADIUS) n++; }
        if (q.size() >= CAP) q.pollFirst();
        q.addLast(new long[]{ now, (long) v.getX(), (long) v.getZ() });
        return n < COUNT ? 1f : Math.max(FLOOR, 1f - 0.9f * (n - COUNT + 1) / COUNT);   // giảm tuyến tính, sàn ×0,1
    }
}

@SubscribeEvent
public static void onDrops(LivingDropsEvent e) {       // vật phẩm tu tiên từ quái spawner bị xóa
    if (SpawnerMarks.is(e.getEntity()))
        e.getDrops().removeIf(i -> i.getItem().is(ModItemTags.NO_SPAWNER_DROP));
}
```

Mọi đường nhận EXP/Hồn Tinh/vật liệu nhân `RewardGate.reward(victim)`; ngưỡng/bán kính/cửa sổ đọc từ `gates.json` (S3 §1.6). Không viết cổng riêng cho Ma tu (S2 §2.4.5 cũ bỏ, giữ Ác Danh).

Lưu ý: spawner của mod (nếu có) phải spawn qua `finalizeSpawn` với `MobSpawnType.SPAWNER` hoặc tự gọi `setData`. Mob từ spawner **vẫn chạy luật Hấp Huyết** (chỉ không cho thưởng tu luyện); Huyết Tế từ mob spawner = 0 **[GIẢ ĐỊNH]** (chung tinh thần §1.1).

### 3.3 Code — HP boss theo số người (§10)

`HP = HP_gốc × (1 + 0.75 × (n − 1))`, n = số người trong 48 block **đã gây ≥5% sát thương lên boss hoặc hồi máu/đỡ đòn đáng kể cho đồng đội** (R16; người đứng xem không tính), 1 ≤ n ≤ 10, cập nhật động.

```java
public final class BossScaling {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hukhong", "boss_scaling");
    public static final double RADIUS = 48, PER_PLAYER = 0.75;
    public static final int MAX_N = 10;

    /** Gọi từ customServerAiStep() của mọi boss implements ScalingBoss. */
    public static void update(LivingEntity boss) {
        if (!(boss.level() instanceof ServerLevel sl) || boss.tickCount % 20 != 0) return;

        int n = 0;
        for (ServerPlayer p : sl.players())
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(boss) <= RADIUS * RADIUS
                && Contribution.counts(boss, p)) n++;      // >=5% damage hoặc heal đáng kể (R16)
        n = Mth.clamp(n, 1, MAX_N);

        double want = PER_PLAYER * (n - 1);                        // HP = base × (1 + want)
        AttributeInstance hp = boss.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier cur = hp.getModifier(ID);
        if ((cur == null ? 0 : cur.amount()) == want) return;      // không đổi -> không làm gì

        float ratio = boss.getHealth() / boss.getMaxHealth();      // giữ TỈ LỆ máu, chống hồi máu/mất máu khi n đổi
        hp.removeModifier(ID);
        if (want > 0)
            hp.addTransientModifier(new AttributeModifier(ID, want, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        boss.setHealth(ratio * boss.getMaxHealth());
    }
}
```

`AttributeModifier` ở 1.21 dùng `ResourceLocation` làm id và là record (`amount()`) **[KIỂM TRA]**. Loot (R16): boss ghi `Map<UUID, Float> damageDealt` + `healDone` (đóng góp tối thiểu để được roll; Đạo tu hỗ trợ tính qua hồi máu/buff); khi chết, mỗi người đủ ngưỡng nhận 1 lượt roll loot table riêng (§10). Ghi `n` tối đa từng đạt vào log để cân bằng.

### 3.4 Code — cấm bay vùng boss + cổng bay

```java
public final class ActiveBosses {            // boss no-fly đang sống, theo level, trong bộ nhớ
    private static final Map<ResourceKey<Level>, Set<Entity>> BOSSES = new HashMap<>();
    @SubscribeEvent public static void onJoin(EntityJoinLevelEvent e)  { /* nếu boss.noFly() -> add */ }
    @SubscribeEvent public static void onLeave(EntityLeaveLevelEvent e){ /* remove */ }
    public static boolean inNoFlyZone(ServerPlayer p) {
        for (Entity b : BOSSES.getOrDefault(p.level().dimension(), Set.of()))
            if (p.distanceToSqr(b) <= 64 * 64) return true;       // bán kính vùng cấm: config, mặc định 64
        return false;
    }
}

// trong FlightRules.canFly(ServerPlayer p, CultivationData d):
//   if (ActiveBosses.inNoFlyZone(p)) return false;     // boss thế giới luôn, CHG/boss cuối theo cờ
//   return canFlyByPathAndLevel(d);
// trong PlayerTickEvent.Post (mục 3.3 v1): thêm  if (p.isFallFlying() && inNoFlyZone) p.stopFallFlying();
```

Boss có chiêu "khóa bay" riêng gọi `FlightRules.lock(player, ticks)` cùng cổng này (không đặt `mayfly` rải rác).

### 3.5 Code — đa đạo: chi phí + sát thương theo nguồn

```java
public final class Resources {
    /** Cổng DUY NHẤT để tiêu tài nguyên. Skill, bay, Lõi đều gọi hàm này. */
    public static boolean tryConsume(ServerPlayer p, ResourceType type, float base) {
        CultivationData d = p.getData(Attachments.CULTIVATION);
        float cost = base * PathMultiplier.of(d.paths().size());   // 1 -> x1, 2 -> x2, 3 -> x3  [GIẢ ĐỊNH tam = x3]
        return ResourceStore.take(p, type, cost);
    }
}

// EXP: PathMultiplier.expPenalty(n): 1 -> 1.0, 2 -> 1.6, 3 -> 2.5  (áp trong CultivationService.addExp)

public final class DamageRules {                  // trả về true nếu đòn bị vô hiệu (amount = 0)
    static boolean nullify(CultivationData d, DamageSource src, LivingEntity target, ItemStack hand) {
        if (src.is(ModDamageTags.SPELL) || src.is(ModDamageTags.THE_SKILL)
            || src.is(ModDamageTags.FLYING_SWORD)) return false;          // phép / quyền cước kỹ năng / phi kiếm: hợp lệ
        boolean the = d.paths().contains("the"), dao = d.paths().contains("dao");
        if (the) return hand.is(ModItemTags.WEAPONS);                     // Thể tu: tay không OK, cầm vũ khí = 0 (luật Thể tu thắng luật Đạo)
        if (dao) return !(target instanceof Player);                      // Đạo tu: đòn thường lên quái = 0, lên người ×1
        return false;
    }
}
```

Xung đột Đạo+Kiếm (đòn kiếm cầm tay lên quái) → bị Đạo tu vô hiệu, chỉ phi kiếm/phép gây sát thương **[GIẢ ĐỊNH]** theo "chỉ phép giết quái".

### 3.6 Code — Huyết Tế chỉ từ NPC

```java
static boolean isValidSource(LivingEntity v) {
    if (v instanceof Player) return false;                                  // không từ người chơi
    if (v instanceof ScalingBoss || v.getType().is(ModEntityTags.BOSSES)) return false;  // không từ boss (kể cả Wither/Dragon qua tag)
    if (!RewardGate.canReward(v)) return false;                             // spawner [GIẢ ĐỊNH]
    return v instanceof Enemy || v instanceof Npc                           // quái, dân làng (Villager implements Npc)
        || v.getType().is(ModEntityTags.HUYET_TE_EXTRA);                    // NPC tu sĩ của mod
}

@SubscribeEvent
public static void onDeath(LivingDeathEvent e) {
    if (!(e.getSource().getEntity() instanceof ServerPlayer k)) return;
    if (!k.getData(Attachments.CULTIVATION).paths().contains("ma")) return;
    if (HuyetTe.isValidSource(e.getEntity())) HuyetTe.gain(k, e.getEntity());   // lộ Hấp Thu cộng thêm Ác Tính (3.7)
}
```

Danh sách boss dùng **tag `hukhong:bosses`** (đưa cả boss vanilla vào) thay vì dò lớp, để datapack chỉnh được.

**Quota + dân làng (R17)**:

```java
static float yield(ServerPlayer k, LivingEntity v) {
    float m = 1f;
    if (v instanceof Villager vil) {
        EvilService.addInfamy(k, 1);                         // giết dân làng BẤT KỲ -> Ác Danh
        if (vil.hasData(Marks.BRED)) m *= 0.1f;              // gắn ở BabyEntitySpawnEvent (nhân giống)
    }
    if (!HuyetTeQuota.tryTake(k, v.level()))  return 0f;     // quota toàn cục/ngày (SavedData) VÀ per-player/ngày (attachment)
    return m * RewardGate.reward(v);                         // vẫn qua KillRateGate
}
```

### 3.7 Code — Ác Tính → Mất Kiểm Soát → Tẩu Hỏa Nhập Ma

Ngưỡng đọc từ config/JSON (S2/S3 chốt số): `evil_berserk`, `evil_death`.

```java
public static void addEvil(ServerPlayer p, float amt) {
    EvilData e = p.getData(Attachments.EVIL);
    float v = e.value() + amt;
    if (v >= Cfg.EVIL_DEATH.get())            { tauHoaNhapMa(p); return; }
    if (v >= Cfg.EVIL_BERSERK.get() && !e.berserk())
        startBerserk(p, 100 /* ticks ~5s */);     // Mất Kiểm Soát
    p.setData(Attachments.EVIL, e.withValue(v));
}

// PlayerTickEvent.Post: nếu berserk -> mỗi 10 tick chọn LivingEntity gần nhất (kể cả đồng đội, trừ chính mình)
//   trong 4 block và gọi p.attack(target) trên SERVER; client nhận ControlLostPayload để khóa input:
//   MovementInputUpdateEvent (client) zero hóa input + chặn phím skill [KIỂM TRA].

// R23: phế tu vĩnh viễn. Cờ MaimedData.maimed=true được đặt TRƯỚC khi giết để copyOnDeath không khôi phục tu vi.
static void tauHoaNhapMa(ServerPlayer p) {
    HuyetTe.clearAll(p);                                   // mất hết Huyết Tế
    p.setData(Attachments.MAIMED, new MaimedData(true));   // đánh dấu chờ làm lại
    p.hurt(ModDamageSources.tauHoa(p.level()), Float.MAX_VALUE);
}
// PlayerEvent.Clone (isWasDeath) / respawn: nếu MAIMED.maimed -> ResetService.fullReset(p):
//   cấp 1, xóa Lõi + Hồn Cốt + đạo + tư chất + Ác Tính + Huyết Tế + onlineTicks tu luyện; GIỮ inventory;
//   đồ có component `hukhong:bound` bị vô hiệu (đặt `bound_disabled=true`); rồi mở lại màn tạo nhân vật (OpenCharacterCreationPayload).
// Khi đã maimed mà chưa làm lại: chặn mọi XP/kỹ năng tu tiên (RewardGate trả 0).

// 3 tầng cảnh báo (server tính ngưỡng, gửi WarnTierPayload(tier 1..3, secondsLeft) cho client; client phát âm thanh + overlay; server gửi chat):
//   tier1 Ác Tính >= 80, tier2 >= 90, tier3 >= 95 (đếm ngược 8s). Tier giảm khi Ác Tính xuống dưới ngưỡng (payload tier=0 để xóa).
```

`hukhong:tau_hoa` thêm vào tag `minecraft:bypasses_invulnerability` (và `bypasses_resistance`) bằng datapack để **Totem of Undying không cứu** **[KIỂM TRA]**. Lộ "Tu Luyện thường" (an toàn) không cộng Ác Tính.

### 3.7b Code — keybind Q/E/R/T và toggle G "Thế Tu Luyện" (R25)

```java
// Client: RegisterKeyMappingsEvent
KeyMapping SKILL1 = new KeyMapping("key.hukhong.skill1", KeyConflictContext.IN_GAME, InputConstants.KEY_Q, "key.categories.hukhong");
KeyMapping SKILL2 = ... KEY_E ; SKILL3 = ... KEY_R ; ULT = ... KEY_T ;
KeyMapping TOGGLE = new KeyMapping("key.hukhong.toggle_stance", ..., InputConstants.KEY_G, "key.categories.hukhong");
// Q (vứt đồ) và E (túi đồ) trùng vanilla. Xử lý xung đột: dùng KeyConflictContext tùy chỉnh `CULTIVATION_STANCE`
// (isActive() = ClientState.stanceOn && screen == null) cho SKILL1/SKILL2; khi stance bật, context vanilla IN_GAME của Q/E
// không còn kích hoạt cùng lúc (NeoForge KeyConflictContext.conflicts) => không vứt đồ/mở túi khi bấm kỹ năng. [KIỂM TRA: hành vi conflicts với key vanilla]
// Phương án dự phòng: ClientTickEvent kiểm stanceOn và hủy sự kiện (InputEvent.InteractionKeyMappingTriggered / ScreenEvent) cho drop & inventory.
// G bật/tắt: gửi ToggleStancePayload -> server lưu Attachments.STANCE (boolean), client đồng bộ; HUD hiện biểu tượng Thế Tu Luyện.
// Tắt thế: Q/E trả lại vanilla; mở túi bằng E vẫn hoạt động; khi stance bật, túi mở bằng phím khác (mặc định Tab-hold/I, đổi được trong Controls).
// Server chỉ chấp nhận SkillCastPayload khi STANCE bật (chống client gian lận). Mọi phím đổi được trong Controls.
```

### 3.8 Code — worldborder bán kính 15.000 và đồng hồ tuổi `onlineTicks`

```java
@SubscribeEvent
public static void onServerStarted(ServerStartedEvent e) {
    ModWorldState st = ModWorldState.get(e.getServer());
    if (st.borderApplied()) return;                    // admin đổi tay thì không ghi đè
    WorldBorder wb = e.getServer().overworld().getWorldBorder();
    wb.setCenter(0, 0);
    wb.setSize(30000);                                 // R24: setSize = đường kính -> bán kính 15.000 (±15.000)
    st.markBorderApplied();
}
// Vị trí 10 thành, hang ổ CHG phải nằm trong ±15.000. Nether: border vanilla scale 1/8 [KIỂM TRA].

// age (R1): đồng hồ DUY NHẤT = onlineTicks cá nhân (định nghĩa ở S6). 24.000 tick = 1 tháng; 12 tháng = 1 tuổi.
// Chỉ tăng khi online và KHÔNG AFK. Không đọc giờ thế giới => ngủ và /time set không ảnh hưởng tuổi.
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Post e) {
    if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;   // tickCount chỉ chạy khi online
    if (!Cfg.AGE_ENABLED.get() || AfkTracker.isAfk(p)) return;        // AFK: không input/di chuyển > N phút
    AgeData a = p.getData(Attachments.AGE);
    long before = a.onlineTicks(), after = before + 20;
    p.setData(Attachments.AGE, a.withOnlineTicks(after));             // tuổi = tuổi_khởi_đầu + onlineTicks / (24000 * 12)
    if (after / 24_000L > before / 24_000L) AgeService.onMonth(p);    // đổi tháng: kiểm Thọ Nguyên
}
// Khi tuổi >= thọ nguyên(realm) -> Thiên Nhân Ngũ Suy tăng dần I..V; DỪNG ở V (debuff nặng,
// gỡ bằng đột phá / Thọ Nguyên Đan). KHÔNG BAO GIỜ chết vì tuổi (R1).
```

`ageEnabled` cho admin tắt khi test; không còn giới hạn "tối đa 12 tháng/lần kiểm" vì không có bước nhảy ngày.

### 3.9 Hang ổ Chấp Hành Giả theo seed + event scheduler

```
ExecutorLairPlanner.position(executorId, worldSeed):
    rng   = RandomSource.create(worldSeed ^ hash(executorId) ^ saltTrongJSON)
    angle = rng.nextDouble() * 2π ;  r = lerp(minDist, maxDist, rng.nextDouble())   # mặc định 5.000–13.000, trong ±15.000
    (x,z) = (r cosθ, r sinθ) ; chọn biome/độ cao hợp lệ bằng lấy mẫu xung quanh (tối đa 16 lần thử)
→ lưu vào ExecutorsSavedData (một lần) ; công trình đặt khi: (a) manh mối đủ → lộ vị trí + đánh dấu, (b) người chơi
  tiến vào 128 block ("bất ngờ gặp", tỉ lệ thấp, kiểm lúc chunk load) → StructureTemplate.placeInWorld [KIỂM TRA]
```

`ExecutorsSavedData`: `state` (UNKNOWN → CLUES → LAIR_REVEALED → EVENT_ACTIVE → DEFEATED), `clues` (tổng điểm, **chung toàn server** — R19), `lairPos`, `lastEventDay`. Scheduler quét mỗi đầu ngày game: điều kiện trong JSON (4.4) + có người chơi online đủ cấp thì mới roll sự kiện ngẫu nhiên; chỉ 1 sự kiện CHG đồng thời. **R19**: Văn Kiện/manh mối chung toàn server; kích hoạt sớm (`unlock_early_event`) chỉ khi **party ≥ 2 người** hoặc có người chơi **cảnh giới ≥ cấp CHG − 5** trong vùng.

---

### 3.10 Code — Linh Khí Đài (R22)

- **Block/BlockEntity**: `SpiritAltarBlock` + `SpiritAltarBE` (trường: `sectId`, `tier` tông/Thánh Địa, `slots[N]` = UUID người chiếm + `startTick`); `N` theo cấp tông (4/8/12; Thánh Địa 24, S6 §2.7). Chỉ đệ tử tông đúng mới `use` được; đài phải là block thật (không giả lập từ xa).
- **`AfkTracker`** thêm trạng thái `AFK_ALTAR` tách khỏi `AFK_IDLE`: `isAfk(p)` = idle ngoài đài (tuổi/EXP dừng như cũ, R1); `isAltarMeditating(p)` = đang ở slot hợp lệ, trong ≤3 block quanh đài.
- **Tuổi**: `onPlayerTick` (§3.8) chặn khi `isAfk(p) && !isAltarMeditating(p)`; Bế Quan **vẫn cộng `onlineTicks`** (ngoại lệ R22; Thánh Địa nhân 0,5).
- **EXP**: mỗi 20 tick gọi `RewardGate.reward(EXP, 0.1·E(L)/3600 · tierMul · altarMul · k_tư_chất)`; nếu cấp hiện tại là 10k−1 và thanh đầy → trả 0 (chặn Bình Cảnh). Không có `victim` nên không cộng Hồn Tinh/vật liệu/farm quái.
- **Dừng**: rời > 3 block, logout, nhận sát thương (`LivingDamageEvent`), vào chiến đấu, hết CH/Linh Thạch (trừ phí mỗi 3.600 tick qua `ResourceGate`), hết chỗ phiên → `stopBeQuan(p)`.

```java
// SpiritAltarBE: gọi mỗi 20 tick cho từng slot (server only)
void tickSlot(ServerPlayer p, Slot s) {
    if (!AfkTracker.isAltarMeditating(p) || p.distanceToSqr(worldPosition.getCenter()) > 9) { s.release(); return; }
    if (BreakthroughService.isAtBinhCanh(p)) return;                    // chặn Bình Cảnh, không tiêu phí
    if (s.ticks() % 3600 == 0 && !ResourceGate.pay(p, cost(sect, tier))) { s.release(); return; }
    double mul = rankMul(p) * (isHolyLand ? 1.5 : 1.0);                 // 1 / 1,5 / 2 / 2
    RewardGate.rewardExp(p, ExpSource.BE_QUAN, 0.1 * ExpCurve.E(p.level) / 3600.0 * 20 * mul);
}
@SubscribeEvent static void onHurt(LivingDamageEvent.Post e) { if (e.getEntity() instanceof ServerPlayer p) SpiritAltarBE.releaseOf(p); }
```

Test: unit cho chặn Bình Cảnh/hệ số bậc; GameTest cho dừng-khi-bị-đánh; **real smoke** AFK 1 giờ trên server (AP4); mọi đường nhận EXP của đài qua `RewardGate` (AP12).

---

## 4. Schema JSON (datapack)

Vị trí registry tùy chỉnh: `data/hukhong/hukhong/<registry>/<id>.json`; recipe dùng thư mục vanilla `data/hukhong/recipe/alchemy/` (1.21 dùng số ít) **[KIỂM TRA]**. Mọi file có trường `schema`; thêm trường mới dùng `optionalFieldOf` + default (R9). Chú thích `//` trong ví dụ chỉ để đọc, **xóa khi dùng thật** (JSON datapack không có comment). Loader **từ chối** (log lỗi rõ ràng) JSON vi phạm canon, vd Sứ Đồ cấp > 65 hay Văn Kiện chỉ về CHG III/IV.

**4.1 Đan dược + công thức luyện** (§11; ví dụ Phá Cảnh Đan Nhất phẩm, mốc 10)

```json
// data/hukhong/hukhong/pill/pha_canh_dan_1.json
{
  "schema": 1,
  "id": "hukhong:pha_canh_dan_1",
  "name_key": "pill.hukhong.pha_canh_dan_1",
  "grade": 1,                                   // 1..9, 10 = Thần Đan
  "category": "breakthrough",                   // breakthrough|heal|exp|lifespan|rebirth|calm|void_cure|poison
  "use_ticks": 40,
  "effects": [
    { "type": "breakthrough", "milestone": 10 } // satisfy Bình Cảnh mốc 10 (đúng phẩm mới có tác dụng)
  ],
  "dan_doc": 12,                                // Đan Độc cộng thêm khi dùng
  "dan_doc_efficiency": { "start": 40, "floor": 0.3 },   // Đan Độc >= 40 thì hiệu quả giảm dần, tối thiểu 30%
  "cooldown_ticks": 1200,
  "max_per_day": 3
}

// data/hukhong/recipe/alchemy/pha_canh_dan_1.json  (custom RecipeType hukhong:alchemy)
{
  "type": "hukhong:alchemy",
  "schema": 1,
  "result": { "pill": "hukhong:pha_canh_dan_1", "count": 1 },
  "station": { "min_tier": 1 },                 // cấp Đan Lô tối thiểu
  "fire": { "min": 2, "ideal": [3, 4], "max": 5 }, // Đan Hỏa
  "ingredients": [
    { "item": "hukhong:linh_thao", "count": 4 },
    { "tag": "hukhong:hon_tinh_bach", "count": 1 }
  ],
  "time_ticks": 600,
  "success": { "base": 0.55, "per_alchemy_level": 0.02, "max": 0.95 },
  "on_fail": { "result": "hukhong:dan_phe", "dan_doc_to_player": 0 },
  "learn": { "source": "hukhong:sect/dan_dinh_tong", "min_rank": "ngoai" }   // nguồn học chính (§11)
}
```

Bảng nguyên liệu Thiên Địa Linh Vật (thay Phá Cảnh Đan) là registry riêng `breakthrough_material`: `{ "item": "...", "milestone": 10, "min_grade": 1 }`.

**4.2 Tông môn** (§7; ví dụ Thiên Kiếm Tông)

```json
// data/hukhong/hukhong/sect/thien_kiem_tong.json
{
  "schema": 1,
  "id": "hukhong:thien_kiem_tong",
  "name_key": "sect.hukhong.thien_kiem_tong",
  "tier": "dia",                                 // thien|dia|nhan
  "paths": ["kiem"],
  "hidden": false,                               // Huyết Ảnh Ma Môn = true (cần cờ khám phá)
  "join": {
    "invite": { "weight": 3, "min_aptitude": "B", "min_level": 10 },       // mời ngẫu nhiên (sự kiện)
    "apply":  { "min_aptitude": "A", "min_level": 20, "age": { "min": 8, "max": 30 } }
  },
  "ranks": [
    { "id": "ngoai",     "contribution": 0,    "buffs": [ { "attribute": "hukhong:spell_power", "op": "add_multiplied_base", "value": 0.02 } ] },
    { "id": "noi",       "contribution": 800,  "buffs": [ { "attribute": "hukhong:spell_power", "op": "add_multiplied_base", "value": 0.05 } ] },
    { "id": "chan_truyen","contribution": 4000, "buffs": [ { "attribute": "hukhong:spell_power", "op": "add_multiplied_base", "value": 0.09 } ], "requires_level": 50 },
    { "id": "truong_lao", "contribution": 15000,"buffs": [ { "attribute": "hukhong:spell_power", "op": "add_multiplied_base", "value": 0.14 } ], "requires_level": 70 }
  ],
  "quests": ["hukhong:sect_quest/tkt_thu_thap_kiem_phach", "hukhong:sect_quest/tkt_diet_su_do"],
  "shop_loot_table": "hukhong:sect/thien_kiem_tong_shop",
  "exclusive": true                              // chỉ 1 tông tại một thời điểm
}
```

`SectBuffService` áp buff bằng `AttributeModifier` id cố định `hukhong:sect_<rank>` khi đăng nhập / đổi bậc (gỡ rồi thêm, tránh cộng dồn).

**4.3 Sứ Đồ + bảng rơi Văn Kiện** (§9; chỉ CHG I/II)

```json
// data/hukhong/hukhong/apostle/su_do_ha_cap.json
{
  "schema": 1,
  "id": "hukhong:su_do_ha_cap",
  "type_key": "apostle.hukhong.ha_cap",
  "rank": 1,                                     // 1 = hạ cấp ... n = cao cấp
  "level": 45,                                   // BẮT BUỘC <= 65 (loader reject nếu vượt)
  "base": "minecraft:pillager",
  "attributes": { "max_health": 180, "attack_damage": 14, "movement_speed": 0.28 },
  "exp": { "cultivation": 120, "vanilla": 0 },
  "spawn": { "type": "event_or_lair", "weight": 12 },   // KHÔNG spawn qua spawner vanilla
  "drops": [ { "loot_table": "hukhong:apostle/ha_cap_loot" } ],
  "clue_drop": {
    "item": "hukhong:van_kien",
    "chance_by_rank": { "1": 0.08, "2": 0.15, "3": 0.28 },   // cấp càng cao tỉ lệ càng cao
    "clue_for": ["hukhong:executor/thach_hong"],             // chỉ I hoặc II; III/IV bị loader reject
    "points": { "1": 1, "2": 2, "3": 4 },
    "once_per_player_per_mob": true
  },
  "model": "hukhong:geo/su_do_ha_cap.geo.json"
}
```

Rơi Văn Kiện đi qua `RewardGate` (mob từ spawner không rơi). Item `Văn Kiện` mang `ClueData{executor, points}`; người chơi đọc → server cộng điểm vào `ExecutorsSavedData` (chống nhân bản: item bị tiêu khi đọc, ghi `UUID` Văn Kiện đã dùng).

**4.4 Sự kiện Chấp Hành Giả** (§9; ví dụ Thạch Hống, cấp 67, Thể)

```json
// data/hukhong/hukhong/executor/thach_hong.json
{
  "schema": 1,
  "id": "hukhong:executor/thach_hong",
  "order": 1,                                    // I..IV
  "name_key": "executor.hukhong.thach_hong",
  "mimic_path": "the",
  "level": 67,
  "boss": {
    "entity": "hukhong:thach_hong",
    "hp_base": 52000,
    "scaling": { "radius": 48, "per_player": 0.75, "max_n": 10 },   // khớp canon §10
    "no_fly": true,
    "no_fly_radius": 64
  },
  "trigger": {
    "random_event": {
      "min_world_day": 20, "check_interval_days": 3, "chance": 0.12,
      "require": { "online_players_min": 1, "player_level_min": 55 }
    },
    "encounter_lair_chance": 0.02,               // tỉ lệ thấp: bất ngờ gặp hang ổ
    "clues": { "required": 12, "scope": "server", "on_complete": ["reveal_lair", "unlock_early_event"],
               "early_event_require": { "party_min": 2, "or_level_offset_min": -5 } }
  },
  "lair": {
    "seed_salt": 7301,
    "distance": { "min": 2500, "max": 6500 },
    "biomes": ["#minecraft:is_mountain", "#minecraft:is_badlands"],
    "template": "hukhong:lair/thach_hong"
  },
  "event": {
    "warning_ticks": 6000,
    "arena_radius": 48,
    "time_limit_ticks": 36000,
    "on_lose": { "retreat_ticks": 24000, "clue_penalty": 4 }
  },
  "rewards": {
    "per_participant_loot_table": "hukhong:executor/thach_hong_reward",
    "fragment": "hukhong:thien_mon_manh_1",
    "unlocks_clue_for": ["hukhong:executor/lang_uyen"],       // III/IV nhận manh mối từ I/II (§9)
    "min_damage_share": 0.03                                  // ngưỡng để được roll loot
  },
  "on_defeat": { "flag": "executor_1_defeated", "world_tier_progress": 1 }
}
```

Quái Hư Hóa (v1 §4.3) và Lõi (v1 §4.1) giữ nguyên schema; Lõi thêm trường tùy chọn `"offer_weight_tier": { "fate_card": true }` để Ấn Hồn Đài biết Lõi có thể rơi vào ô thứ 4 hay không.

---

## 5. Cấu trúc project, build, test, hiệu năng

### 5.1 Directory tree
```
hukhong/
├── build.gradle                 # ModDevGradle, Java 21 ; run configs: client, client2, server, data, gameTestServer
├── gradle.properties            # mod_id, neo_version (21.1.x), geckolib_version
├── src/main/java/com/hukhong/
│   ├── HuKhong.java
│   ├── core/  age/  breakthrough/  cores/  pathrules/  gates/  scaling/
│   ├── alchemy/  sect/  city/  apostles/  executors/  worldtier/
│   ├── flight/  combat/  entity/  worldgen/  events/  network/
│   └── client/ui/
├── src/main/resources/
│   ├── META-INF/neoforge.mods.toml
│   ├── assets/hukhong/{lang,models,textures,geo,animations}/
│   └── data/hukhong/
│       ├── hukhong/{core,pill,sect,sect_quest,apostle,executor,breakthrough_material}/*.json
│       ├── recipe/alchemy/*.json
│       ├── structure/ template_pool/ structure_set/
│       ├── tags/{damage_type,item,entity_type}/
│       └── advancement/ loot_table/
├── src/generated/               # datagen
└── src/test/ + src/gametest/    # JUnit + GameTest
```

### 5.2 Build
- `ModDevGradle` (`net.neoforged.moddev`), `neoForge { version = "21.1.x" }`: số `x` lấy từ trang NeoForge **[KIỂM TRA]**; GeckoLib đúng artifact NeoForge 1.21.1 **[KIỂM TRA]**. **Ghim phiên bản** vào `gradle.properties`, không tự nâng.
- `client2` run config với `--username Dev2` để mở 2 client cùng máy **[KIỂM TRA]**; server test đặt `online-mode=false` trong `run/server.properties`.
- Dùng **datagen** cho lang/model/tag để tránh lỗi tay.

### 5.3 Cách test

| Lớp | Công cụ | Dùng để |
|-----|---------|---------|
| Luật thuần (EXP curve, Bình Cảnh, `PathMultiplier`, công thức `HP boss`, roll tư chất/Ấn Hồn Đài (relog giữ nguyên kết quả nhờ `rollSeed`), `KillRateGate`, đồng hồ tuổi `onlineTicks`, vị trí hang ổ theo seed) | JUnit, RNG có seed | Tách logic khỏi `Player` |
| Hành vi trong game | GameTest (`runGameTestServer`) | Đạo tu đánh quái = 0; mob spawner không thưởng; boss HP khi 1/3 người; Huyết Tế từ boss/người = 0; farm >30 con/64 block giảm ×0,1; `/time set` + ngủ không đổi tuổi |
| Cảm giác / UI | `runClient` + lệnh debug `/hukhong setlevel`, `roll`, `age`, `dump` | Playtest tay |
| Nhiều người | `runServer` + 2-3 client | Sync attachment, scaling boss, Thành Chủ duy nhất |

GameTest/JUnit xanh **chưa phải xong** (AP4): tiêu chí xong luôn có bước **user chơi thật** (mục 6.3).

### 5.4 Hiệu năng
- **Boss scaling**: chỉ chạy mỗi 20 tick/boss, tối đa vài boss cùng lúc; `ActiveBosses` là set trong bộ nhớ, không quét thế giới.
- **Hư Hóa lan / tập kích**: `CorruptionSavedData` chunk→mức, hàng đợi có ngân sách ms/tick, chỉ chunk đã load; scheduler 1 cái/`ServerLevel`, cap invasion đồng thời (1-2) và tổng mob (~60).
- **Attachment sync**: gửi khi đổi, tối đa 1 lần/tick/player; tài nguyên đổi liên tục gửi mỗi 5-10 tick.
- **Tuổi**: cộng `onlineTicks` mỗi 20 tick (một `long`), không scan.
- **KillRateGate**: hàng đợi ≤256 bản ghi/loài, cắt theo cửa sổ 10 phút mỗi lần gọi; không quét thế giới.
- **Event nóng** (`LivingIncomingDamageEvent`, `PlayerTickEvent`): return sớm, cache tag lookup. Mọi vòng lặp tick có ngân sách (AP3: không có chờ vô hạn).

---

## 6. Roadmap P0 → P3

Đơn vị thực tế khi **AI viết code** là **vòng build–test** (AI giao → user chạy → báo lỗi → AI sửa), không phải người-tuần. Ước lượng Fermi thô, sai số ×1.5 hai phía, user dành ~5-8 giờ/tuần cho chạy thử.

| Phase | Phạm vi | Vòng build-test | Wall-clock ước lượng |
|-------|---------|-----------------|----------------------|
| **P0** | Xem 6.1 | 25-40 | 5-8 tuần |
| **P1** | Cấp 31-60; đủ 4 đạo + đa đạo; bay; Lõi ~40 + set; Phá Cảnh Đan phẩm 3-6 (gồm **Đế Hoàn Đan** phẩm 6 — R20); Đan Lô + luyện đan + Đan Độc; Tuổi (`onlineTicks`)/Thọ Nguyên/Ngũ Suy dừng ở V đầy đủ; `KillRateGate`; 10 thành + chức vị + Thành Chủ duy nhất; 3-4 tông (Thiên Kiếm, Quang Minh, Bá Thể, Đan Đỉnh) **+ Linh Khí Đài mỗi tông (R22, §3.10)**; tập kích + Hư Hóa lan; khung `scaling` + 1-2 boss thế giới (cấm bay); worldborder 15.000 | 60-90 | 12-18 tuần |
| **P2** | Cấp 61-99; Hồn Cốt (slot tự làm); Bản Mệnh skill; tư chất S+; đủ 7 tông (Vạn Tượng, Huyết Ảnh ẩn); **Sứ Đồ + Văn Kiện**; **4 Chấp Hành Giả** (scheduler, hang ổ theo seed, boss); Phá Cảnh Đan phẩm 7-9 (phẩm 6 đã ở P1); Bí Cảnh; quest chính | 80-120 | 16-24 tuần |
| **P3** | Cấp 100 + Khảo Hạch Thăng Thần; Thần Đan; Nguyên Thủy Tịch Diệt (dimension Hư Không Bản Nguyên); kết thúc Phong Ấn/Hấp Thụ → **Tầng Thế Giới II**; cân bằng; tối ưu; đóng gói | 45-70 | 9-14 tuần |

### 6.1 P0 — phạm vi nhỏ có chủ đích (tránh AP2)

**Trong phạm vi**
- **2 đạo: Đạo Tu + Thể Tu**, cấp 1-30; màn tạo nhân vật rút gọn (chọn đạo + roll tư chất).
- **1 thành** (1 jigsaw structure, đặt bằng lệnh hoặc worldgen đơn giản) + `CitySavedData` (chỉ Cư Dân và trạng thái NORMAL/RUIN).
- **Ấn Hồn Đài 4 ô** (`MenuType`; 3 Lõi cùng phẩm + ô "Phó Mặc Số Phận", **không** làm Mắc Kẹt 1% ở P0) + **10 Lõi** JSON.
- **Bình Cảnh mốc 10 và 20** với **2 loại Phá Cảnh Đan** (Nhất phẩm, Nhị phẩm), cấp qua lệnh/loot (chưa có Đan Lô); không đan = EXP ×k.
- **3 quái Hư Hóa** (zombie/skeleton/spider-based, GeckoLib).
- **Tuổi cơ bản**: tuổi khởi đầu cố định, đếm theo `onlineTicks` (24.000 tick = 1 tháng), HUD hiển thị; chưa Thọ Nguyên/Ngũ Suy.
- **Luật nền**: spawner = 0 EXP (3.2), khóa EXP ở mốc, `Resources.tryConsume` (chưa đa đạo nhưng đã qua cổng), HUD.
- **Multiplayer**: chạy trên dedicated server + 2 client, dữ liệu mỗi người độc lập.

**Ngoài phạm vi**: bay, Hồn Cốt, Kiếm/Ma tu, đa đạo, xuất thân, tông môn, chức vị, Đan Lô/luyện đan, Sứ Đồ, Chấp Hành Giả, boss scaling, Tầng Thế Giới, worldborder, Tâm Kiếp. Tập kích bằng lệnh `/hukhong invade` là mục **cắt đầu tiên** nếu trễ.

**Tiêu chí "xong" (user thử trong game, trên dedicated server)**
1. Tạo nhân vật → chọn Đạo Tu, roll tư chất; thoát vào lại, dữ liệu còn (kể cả tuổi), `copyOnDeath` đúng.
2. Đạo tu: thanh đói không giảm 10 phút; đánh zombie tay/cung = 0; 1 phép giết được zombie; đòn thường lên người chơi khác bình thường.
3. Thể tu: cầm kiếm đánh quái = 0; tay không có sát thương, Bá Khí tăng.
4. Quái từ **spawner** (đặt spawner zombie): giết = 0 EXP, 0 Hồn Tinh; quái tự nhiên/Hư Hóa cho EXP bình thường.
5. Lên cấp 9 → Bình Cảnh (thanh EXP báo rõ): không đan thì cần nhiều EXP hơn; dùng Phá Cảnh Đan Nhất phẩm → qua ngay; lên cấp 10 → khóa EXP; Ấn Hồn Đài hiện 4 ô, chọn → bị động có hiệu lực, mở khóa cấp 11. Dùng sai phẩm (đan Nhị phẩm ở mốc 10) → không tác dụng.
6. Lặp lại mốc 20 (đan Nhị phẩm, Lõi #2); mốc 30: hết nội dung P0, có thông báo.
7. Dedicated server + 2 client: HUD/tuổi/Lõi của mỗi người đúng; người A chọn Lõi không ảnh hưởng B; reconnect giữ nguyên.
8. Không còn đường lách: kỹ năng và (sau này) bay đều trừ qua `Resources.tryConsume` (grep không còn `setResource` trực tiếp).

### 6.2 Thứ tự làm P0 (6 mốc)

`core + sync + HUD + age cơ bản` → `pathrules (2 đạo) + RewardGate (spawner)` → `EXP + khóa mốc + Bình Cảnh + 2 Phá Cảnh Đan` → `Core registry + Ấn Hồn Đài 4 ô` → `3 quái + drop` → `thành + SavedData (+ lệnh invade nếu kịp)`. Mỗi mốc qua checklist "người chơi thấy/dùng được" (callsite/UI đã nối) trước khi sang mốc sau (AP1).

### 6.3 Quy trình làm việc: AI viết code ↔ user chạy thử

```
AI                                           USER
 │ 1. Context check: git log -10, git status  │
 │ 2. Viết code + datapack JSON cho 1 mốc    │
 │ 3. Chạy ./gradlew compileJava (+ test,    │
 │    runGameTestServer nếu có JDK 21)       │
 │ 4. Giao: danh sách file đổi + lệnh chạy   │
 │    + checklist thử (từ 6.1) ─────────────►│ 5. ./gradlew build  (báo nếu lỗi)
 │                                            │ 6. ./gradlew runServer  +  runClient, runClient2
 │                                            │ 7. Làm theo checklist, ghi PASS/FAIL từng mục
 │◄──────────── báo kết quả + log ────────────│
 │ 8. Lỗi: phân tích (UC4), sửa, quay lại 3   │
 │ 9. Tất cả PASS → đánh ✅ mốc (DoD 5 mục)   │
```

**Quy tắc**
- AI **không commit/push** trừ khi user cho phép rõ trong lượt đó (AP5). Mỗi mốc gợi ý 1 commit `feat(scope): ...` do user hoặc AI (khi được cho phép).
- Mốc chỉ được ✅ khi đủ 5 mục: spec, code, unit/GameTest, callsite nối, và **user xác nhận chạy thật** (AP1, AP4). Chưa đủ → 🔄 PARTIAL, ghi carry-over.
- Lỗi lặp lại 3 vòng cùng một chỗ → dừng, RCA (UC4) trước khi sửa tiếp; sửa một layer xong phải test lại từng đường dùng cổng `gates` (AP12).
- Mọi API đánh **[KIỂM TRA]**: AI làm "spike" nhỏ (compile + chạy 1 test) trước khi xây tiếp lên nó.

**Cách user gửi lỗi (copy/paste, càng đủ càng ít vòng)**
1. **Lỗi build**: chạy `./gradlew build --stacktrace`, gửi toàn bộ phần từ dòng `error:` đến hết `FAILURE`.
2. **Lỗi khi chạy / crash**: gửi `run/logs/latest.log` (hoặc 200 dòng quanh chữ `Exception`/`ERROR`) và file mới nhất trong `run/crash-reports/`. Với server: `run/logs/latest.log` của **cả server lẫn client** nếu lỗi sync.
3. **Lỗi hành vi (không crash)**: ghi (a) bước tái hiện, (b) kỳ vọng vs thực tế, (c) chạy `/hukhong dump` dán kết quả (level, exp, paths, tuổi, cờ Bình Cảnh), (d) đang ở server hay singleplayer, có mấy người.
4. Ghi rõ phiên bản mod (git hash hoặc ngày) để AI biết đang test bản nào.

---

## 7. Rủi ro kỹ thuật & giảm thiểu

| # | Rủi ro | Xác suất | Tác động | Cách giảm |
|---|--------|----------|----------|-----------|
| R1 | AI nhớ nhầm API (tên 1.20.x/1.21.x lẫn nhau: `AttributeModifier`, `recipe` vs `recipes`, event tick) | Cao | Trung bình | Ghim 1.21.1; spike cho mọi **[KIỂM TRA]**; lỗi compile rẻ, lỗi runtime thì GameTest; đối chiếu source NeoForge |
| R2 | "Hết đói" Đạo tu lọt đường khác | Thấp | Thấp | Reset food mỗi tick + GameTest chạy dài |
| R3 | Desync flight/vùng cấm bay client–server | Trung bình | Trung bình | Chỉ đổi `mayfly` ở server qua `FlightRules`; test dedicated |
| R4 | Phình phạm vi (tar pit, AP2): thêm module chưa cần | Cao | Cao | Khóa P0 (6.1); không mở P1 khi P0 chưa qua đủ 8 tiêu chí |
| R5 | Lag server: Hư Hóa lan, tập kích, boss nhiều người | Trung bình | Cao | Ngân sách ms/tick, cap mob, `ActiveBosses` trong bộ nhớ; profile bằng spark |
| R6 | Cân bằng khó (nhiều JSON: đan, tông, Lõi, CHG) | Cao | Trung bình | Data-driven + `/reload` + lệnh debug; bảng cân bằng S3 |
| R7 | GeckoLib lệch phiên bản | Thấp | Cao | Lớp bọc mỏng quanh renderer; ghim phiên bản |
| R8 | Mod khác (mixin) phá AI quái nền vanilla | Thấp | Thấp | Subclass đơn giản, test vài mod phổ biến |
| R9 | Mất save khi đổi schema Attachment/SavedData/JSON | Trung bình | Cao | Trường `schema`; `optionalFieldOf` + default; không đổi tên field sau release; chia attachment theo module |
| R10 | Đường vòng qua cổng `gates`: skill/item tự trừ tài nguyên hoặc cộng EXP → đa đạo ×2/×3 và spawner 0 EXP bị lọt | Trung bình | Cao | Một cổng duy nhất; review grep `setResource`/`addExp`/`giveExperience`; GameTest cho mọi nguồn |
| R11 | Dấu spawner bị rơi: spawn đường khác (spawner mod, trial spawner, `FinalizeSpawnEvent` không bắn ở một số đường) | Trung bình | Trung bình | Test từng loại spawner; dấu lưu bằng attachment; spawner mod dùng cùng API; **[KIỂM TRA]** |
| R12 | Boss scaling bị khai thác: ra/vào bán kính 48 để đổi HP; người chơi kéo thêm người phút chót | Trung bình | Trung bình | Giữ tỉ lệ máu khi đổi n (3.3); chống hồi máu: chỉ cập nhật mỗi 20 tick; log n lớn nhất; ngưỡng damage để được loot |
| R13 | Tẩu Hỏa/Totem/`keepInventory`: tử vong bị chặn hoặc xử lý sai | Thấp | Trung bình | Damage type vào tag `bypasses_invulnerability`; GameTest với Totem |
| R14 | Thành Chủ trùng/mất do thao tác đồng thời hoặc người chơi offline vĩnh viễn | Thấp | Trung bình | Mọi thay đổi chức vị qua `CitySavedData` trên server thread; điều kiện NPC nhường chỗ khi không hoạt động (S6 chốt) |
| R15 | Văn Kiện nhân bản / spam điểm manh mối | Trung bình | Trung bình | Văn Kiện có UUID, bị tiêu khi đọc; `once_per_player_per_mob`; điểm cộng chỉ ở server |
| R16 | Hang ổ theo seed rơi vào địa hình xấu / ngoài worldborder / chunk chưa sinh | Trung bình | Thấp | Lấy mẫu tối đa 16 lần, kẹp trong ±15.000; fallback lệch; đặt công trình lúc cần |
| R17 | Tuổi: AFK treo máy làm `onlineTicks` lệch; Ngũ Suy gây khó chịu | Thấp | Trung bình | Chỉ `onlineTicks` (không đọc giờ thế giới), `AfkTracker`, cờ `ageEnabled`; Ngũ Suy dừng ở V, không chết vì tuổi (R1); GameTest 1.000 giờ giả lập |
| R18 | Đa đạo đụng luật nguồn đòn, Đạo+Kiếm/Thể+Kiếm, Ma tu + hỗ trợ cho kết quả khó đoán | Trung bình | Trung bình | Một hàm `DamageRules.resolve` + bảng ví dụ kiểm thử đơn vị; ghi quyết định vào S2 |

---

## 8. Câu hỏi còn mở

Không còn (đã giải quyết v2.1: tiến độ manh mối chung server → R19; tuổi chỉ khi online → R1).

## Đề xuất đổi canon

- Làm rõ §3: Huyết Tế từ **mob spawner** = 0 (S5 đang giả định theo §1.1).
- Làm rõ §3: Ma tu **không hỗ trợ** có áp dụng cả khi đa đạo (Ma + Đạo) hay không (S5 giả định: có, chặn mọi skill `support`).

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | S5-tech.md (v1, kiến trúc + roadmap MVP) | NeoForge 1.21.1 vs 26.x, 2 đạo MVP | Superseded |
| v2 | S5-tech.md (bản trước) | Áp dụng PLAN-v2: module mới, luật bằng event, 4 schema, roadmap P0→P3, quy trình AI↔user | Superseded |
| **v2.1** | S5-tech.md (this file, sửa tại chỗ) | R1 đồng hồ tuổi, R3 KillRateGate, R8 seed, R16 n boss, R17 quota Huyết Tế, R19 manh mối chung, R20 Đế Hoàn Đan P1 | ✅ Current |
