package com.oa7.pojo;

import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class AttendanceRule {
    private Integer id;
    private String ruleName;            // e.g. "默认规则", "技术部规则"
    private Integer deptId;             // null = 全局默认规则
    private LocalTime workStartTime;    // default 09:00
    private LocalTime workEndTime;      // default 18:00
    private Integer lateThresholdMin;   // grace period minutes, default 0
    private Integer earlyThresholdMin;  // grace period minutes, default 0
    private Boolean enabled;            // default true
    private Integer missingToleranceMin; // 缺时宽限（分钟），默认30
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
