package com.oa7.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 补卡申请实体类 - 对应 day.makeup_request 表
 */
@Data
public class MakeupRequest {
    private Integer id;
    private Integer empId;
    private String date;         // 补卡日期 YYYY-MM-DD
    private String type;         // CHECK_IN / CHECK_OUT
    private String requestTime;  // 申请时间 HH:mm
    private String reason;
    private String status;       // PENDING / APPROVED / REJECTED
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
