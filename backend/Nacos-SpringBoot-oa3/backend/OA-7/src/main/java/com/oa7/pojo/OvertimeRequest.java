package com.oa7.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class OvertimeRequest {
    private Integer id;
    private Integer empId;
    private LocalDate overtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalHours;
    private BigDecimal actualHours;
    private String reason;
    private String status;         // PENDING / APPROVED / REJECTED
    private String rejectReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非持久化（联表查询）
    private String empName;
    private String deptName;
}
