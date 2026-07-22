package com.oa7.constant;

/**
 * 最终状态 — 用于统计、报表、工资计算
 * 优先级（高→低）：
 * HOLIDAY > REST_DAY > LEAVE > DAY_OFF > BUSINESS_TRIP > FIELD_WORK > NORMAL > LATE > EARLY > MISSING_CARD > ABSENCE
 */
public enum AttendanceStatus {
    NORMAL("正常"),
    LATE("迟到"),
    EARLY("早退"),
    LATE_EARLY("迟到早退"),
    LEAVE("请假"),
    DAY_OFF("调休"),
    BUSINESS_TRIP("出差"),
    FIELD_WORK("外勤"),
    MISSING_CARD("缺卡"),
    ABSENCE("旷工"),
    HOLIDAY("节假日"),
    REST_DAY("休息日");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
