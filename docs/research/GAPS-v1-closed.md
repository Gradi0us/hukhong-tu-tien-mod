# GAPS-v1 closed (design gaps from REVIEW-v2 / S2 S3 S6)
**Last Updated**: 2026-10-27 — 7 gaps handled; build + JUnit green; in-game NOT verified (AP4).

| # | Gap | Result | Where |
|---|-----|--------|-------|
| 1 | Ma lộ UI | DONE: `[Lộ Ma]` link in Profile (K) -> `MaLoScreen` -> `SetMaLoPayload` (server checks Ma path); `MaSyncPayload` now carries `absorb`. `/hukhong ma lo` kept | `client/screen/ma/MaLoScreen`, `network/SetMaLoPayload`, `MaRules.setLo` |
| 2 | Bản Mệnh Kiếm | DONE: stage = Hồn Tinh (offhand + right-click, or Shift+right-click anvil), realm is the cap; cost 2/4/8/12/20/32 (Hồn Tinh has no grade in mod -> count-based). Ngọc Tâm: regen +25%, max resource +15%, Lv40 skill cost x0.9, Lv80 free sword flight in combat. Trọng Nhạc speed -6%. No other Kiếm type has a flight-cost rule in S2 | `banmenh/BanMenhMath`, `BanMenhService.tryEvolve/applyPassives`, `BanMenhEvents.onAnvil`, `FlightService` |
| 3 | Ngũ Suy | DONE: stage II -10% max resource (attribute modifier + sync); stage V flight ban (`FlightBans.banKey`) and meditation x0.5 (Linh Khí Đài + Đạo/Thể meditation regen) | `AgeMath`, `AgeService`, `SectEvents`, `PathRules` |
| 4 | Linh Khí Đài age drift | DONE (canon S6 §1.2 R22): valid Bế Quan keeps ageing despite AFK (1h = 1/4 yr, same as online rate). Thánh Địa x0.5 NOT done: no Thánh Địa altar exists in code | `SectEvents.isBeQuan`, `AgeService` |
| 5 | Hư Hóa / Ma flight | PARTIAL by canon: S2 §2.1 only bans Ngự Kiếm while under the Hư Hóa effect -> done. No canon for "corrupted chunk" no-fly zone, nor Ma flight draining MaData (Ma flight cost is [GIẢ ĐỊNH]) -> NOT implemented | `FlightBans` |
| 6 | Bone bonuses | DONE: Tĩnh Tâm -20% Ác Tính gain (`addAcTinh`; "Tâm Ma tích" read as the Ác Tính meter, trial clone untouched); Kiếm Ý +25% range of Trảm Khí / Liên Hoàn Trảm / Kiếm Vực; Phong Hành -15% flight + air-jump cost | `HonCotMath`, `MaRules`, `SkillEffects`, `FlightService` |
| 7 | Altar fee / SectTrial | DONE: fee charged when accumulated sitting seconds hit a multiple of 3600 (persistent counter, not reset by new sit); SectTrial missing (unloaded) mob -> fail "unloaded" + cleanup instead of waiting for timeout | `SectMath.feeDue`, `SectEvents`, `SectTrial` |

Tests: `BanMenhMathTest`, `HonCotBonusTest`, `AgeMathTest` (+Ngũ Suy), `SectMathTest` (+fee).
Not changed: `stats/` constants, boss HP. Known leftovers: SkillHud still shows pre-discount cost (cosmetic); Ngọc Tâm/Ngũ Suy not verified in-game; new lang keys only in `lang-gaps.txt` (code uses fallbacks) — merge into vi_vn/en_us.
