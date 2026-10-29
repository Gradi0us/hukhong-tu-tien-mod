package com.hukhong.client.boss;

import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.List;
import java.util.Map;

/**
 * Quy tắc hiện/ẩn xương theo pha cho từng boss. Xương phụ kiện chung do tools/gen_boss_phase.py thêm vào .geo.json:
 * {@code ph2_spikes} (gai, pha 2-3), {@code ph3_halo} (vành mảnh nổi, pha 3). Ngoài ra vài xương có sẵn được ẩn khi "vỡ"
 * (vương miện Hư Vương, giáp vai Băng Nhân...). Xương không có trong model thì bỏ qua.
 */
public final class BossLook {
    private BossLook() {}

    /** Xương hiện khi min &lt;= pha &lt;= max. */
    public record Rule(String bone, int min, int max) {}

    private static final List<Rule> COMMON = List.of(new Rule("ph2_spikes", 2, 3), new Rule("ph3_halo", 3, 3));

    private static final Map<String, List<Rule>> EXTRA = Map.of(
            "hu_vuong", List.of(new Rule("crown", 1, 1)),                                    // vương miện bị bóp nát
            "bang_nhan", List.of(new Rule("pauldron_r", 1, 2), new Rule("pauldron_l", 1, 2)), // giáp vai vỡ ở pha 3
            "chg_2", List.of(new Rule("halo", 2, 3)),                                        // hào quang lộ ra từ pha 2
            "da_khuyet", List.of(new Rule("halo", 2, 3), new Rule("wing", 3, 3)));           // trăng khuyết dần -> cánh ở pha 3

    public static void apply(String id, BakedGeoModel model, int phase) {
        for (Rule r : COMMON) set(model, r, phase);
        List<Rule> extra = EXTRA.get(id);
        if (extra != null) for (Rule r : extra) set(model, r, phase);
    }

    private static void set(BakedGeoModel model, Rule r, int phase) {
        boolean vis = phase >= r.min && phase <= r.max;
        model.getBone(r.bone).ifPresent(b -> b.setHidden(!vis));
    }
}
