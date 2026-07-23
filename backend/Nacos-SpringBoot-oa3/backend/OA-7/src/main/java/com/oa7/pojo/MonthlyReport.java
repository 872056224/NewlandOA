package com.oa7.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度考勤统计实体类 - 对应 day.monthly_report 表
 */
@Data
public class MonthlyReport {
    private Integer id;
    private String yearMonth;
    private Integer empId;
    private String empName;
    private Integer deptId;
    private Integer workDays;
    private Integer actualDays;
    private Integer lateCount;
    private Integer earlyCount;
    private Integer leaveCount;
    private Integer absenceCount;
    private Integer missingCardCount;
    private Integer missingDuration;  // 当月累计缺时时长（分钟）
    private BigDecimal attendanceRate;
    private LocalDateTime createdAt;
}
