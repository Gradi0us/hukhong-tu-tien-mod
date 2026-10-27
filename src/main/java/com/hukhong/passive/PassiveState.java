package com.hukhong.passive;

/** Trạng thái runtime server của nội tại (không lưu). */
final class PassiveState {
    int kiemHits, daoStacks, theStacks, maShown;
    long lastBasicTick = -1, lastSkillTick = -1, theLast = Long.MIN_VALUE / 2, maCd = Long.MIN_VALUE / 2, maShownUntil;
    int appliedTiers = 0;
    double appliedArmor, appliedHp;
    // lần sync gần nhất
    int sKiem = -1, sDao = -1, sThe = -1, sMa = -1;
}
