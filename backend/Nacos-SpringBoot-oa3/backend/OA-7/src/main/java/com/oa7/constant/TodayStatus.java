package com.oa7.constant;

/**
 * 实时状态 — 仅用于当天展示，不参与统计
 */
public enum TodayStatus {
    NOT_CHECKED_IN("未签到"),
    CHECKED_IN("已签到"),
    CHECKED_OUT("已签退"),
    LEAVE_PENDING("请假审批中"),
    LEAVE("已请假"),
    MAKEUP_PENDING("补卡审批中"),
    ANOMALY("签到异常"),
    DAY_OFF("调休"),
    HOLIDAY("节假日"),
    REST_DAY("休息日"),
    BUSINESS_PENDING("出差审批中"),
    FIELD_PENDING("外勤审批中");

    private final String displayName;

    TodayStatus(String displayName) {
        this.displayName = displayName;
    }

    @com.fasterxml.jackson.annotation.JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
