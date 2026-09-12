package com.lidao.moran.systems.trees;

/**
 * 土壤档案：三种理化性质，各 0-8。
 * drainage 排水性（水在土中排走的速度）、aeration 气密性（根系透气）、retention 保水性（持水能力）。
 * 树种档案声明偏好区间，出区间按偏离扣生长倍率（现实：桃耐旱怕涝，排水差的土长不好）。
 */
public record SoilProfile(int drainage, int aeration, int retention) {

    public static final SoilProfile DEFAULT = new SoilProfile(5, 5, 5);
}
