package com.oa7.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalaryDetail {
    private Integer id;
    private Integer empId;
    private String yearMonth;
    private BigDecimal baseSalary;
    private Integer workDays;
    private BigDecimal dailyWage;
    private BigDecimal hourlyWage;
    private Integer actualAttendanceDays; // 实际出勤天数
    private Integer totalMissingMinutes;
    private BigDecimal missingDeduction;
    private BigDecimal overtimeHours;
    private BigDecimal overtimePay;
    private BigDecimal leaveDays;
    private BigDecimal leaveDeduction;
    private BigDecimal finalSalary;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非持久化
    private String empName;
    private String deptName;
    private String dutyName;
}
