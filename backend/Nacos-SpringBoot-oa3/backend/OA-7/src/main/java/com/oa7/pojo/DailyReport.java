package com.oa7.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日报统计实体类 - 对应 day.daily_report 表
 */
@Data
public class DailyReport {
    private Integer id;
    private LocalDate reportDate;
    private Integer totalEmployees;
    private Integer normalCount;
    private Integer lateCount;
    private Integer earlyCount;
    private Integer lateEarlyCount;
    private Integer leaveCount;
    private Integer absenceCount;
    private Integer missingCardCount;
    private Integer holidayCount;
    private BigDecimal attendanceRate;
    private LocalDateTime createdAt;
}
