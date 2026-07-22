package com.oa7.constant;

public enum HolidayType {
    WORKDAY("工作日"),
    HOLIDAY("节假日"),
    REST_DAY("休息日");

    private final String displayName;

    HolidayType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
