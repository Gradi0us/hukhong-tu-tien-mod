package com.hukhong.phapkhi;

/** Mô tả ngắn tiếng Việt (fallback trong code; bản dịch chính thức nằm ở docs/research/lang-phapkhi.txt). */
final class PhapKhiText {
    private PhapKhiText() {}

    static String tip(PhapKhiKind k) {
        return switch (k) {
            case KIEM -> "Phù Công hệ Kiếm: kiếm khí xuyên thấu.";
            case HOA -> "Phù Công hệ Hỏa: cầu lửa nổ, thiêu đốt.";
            case LOI -> "Phù Công hệ Lôi: sét đánh, lan chuỗi.";
            case BANG -> "Phù Công hệ Băng: băng đâm, làm chậm.";
            case THO -> "Phù Công hệ Thổ: gai đá trồi, hất tung.";
            case MA -> "Phù Công hệ Ma: huyết trảo, kéo địch lại.";
            case QUANG -> "Phù Công hệ Quang: mưa cột sáng thánh.";
            case HO_THAN -> "Lá chắn hấp thụ theo % máu tối đa, 8 giây.";
            case KIM_CANG -> "Giảm sát thương nhận vào, 6 giây.";
            case DON_KHONG -> "Dịch chuyển theo hướng nhìn, bất tử thoáng chốc.";
            case HOI_XUAN -> "Hồi % máu tối đa.";
            case TU_LINH -> "Trận: người trong trận hồi linh lực + máu.";
            case KHON -> "Trận: địch vào bị trói 1 giây, chậm 50%.";
            case SAT -> "Trận: địch trong trận chịu sát thương liên tục.";
            case HO_TRAN -> "Trận: người trong trận nhận ít sát thương hơn.";
        };
    }
}
