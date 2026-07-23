package com.oa7.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class AttendanceRule {
    private Integer id;

    @JsonProperty("ruleName")
    private String rule_name;

    @JsonProperty("deptId")
    private Integer dept_id;

    @JsonProperty("workStartTime")
    private LocalTime work_start_time;

    @JsonProperty("workEndTime")
    private LocalTime work_end_time;

    @JsonProperty("lateThresholdMin")
    private Integer late_threshold_min;

    @JsonProperty("earlyThresholdMin")
    private Integer early_threshold_min;

    private Boolean enabled;

    @JsonProperty("missingToleranceMin")
    private Integer missing_tolerance_min;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;
}
