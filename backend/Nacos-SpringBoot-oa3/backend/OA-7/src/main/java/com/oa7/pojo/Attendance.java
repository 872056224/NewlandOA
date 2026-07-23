package com.oa7.pojo;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.TodayStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录实体类 - 对应 day.attendance 表
 */
@Data
public class Attendance {
    private Long id;
    private Integer empId;
    private LocalDate date;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private TodayStatus todayStatus;
    private AttendanceStatus attendanceStatus;
    private Integer missingDuration;  // 缺时时长（分钟）
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
