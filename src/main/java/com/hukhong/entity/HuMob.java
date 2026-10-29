package com.hukhong.entity;

public interface HuMob {
    int huLevel();
    /** Dat lai cap hien thi (khi chi so bi tinh lai o cap khac, vd Bi Canh). Mac dinh: khong lam gi. */
    default void setHuLevel(int level) {}
    double huWeight();
    float honTinhChance();
}
