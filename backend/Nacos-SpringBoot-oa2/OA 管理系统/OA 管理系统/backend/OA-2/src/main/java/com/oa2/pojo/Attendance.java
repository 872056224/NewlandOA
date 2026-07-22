package com.oa2.pojo;

import com.oa2.constant.AttendanceStatus;
import com.oa2.constant.TodayStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Attendance {
    private Long id;
    private Integer empId;           // 员工编号
    private LocalDate date;          // 考勤日期
    private LocalDateTime checkInTime;   // 实际签到时间
    private LocalDateTime checkOutTime;  // 实际签退时间
    private TodayStatus todayStatus;     // NOT_CHECKED_IN/CHECKED_IN/CHECKED_OUT/LEAVE/MAKEUP_PENDING/DAY_OFF
    private AttendanceStatus attendanceStatus; // NORMAL/LATE/EARLY/LEAVE/ABSENCE/MISSING_CARD...
    private String checkInAddress;
    private String checkOutAddress;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
