package com.oa2.pojo;

import lombok.Data;
import java.time.LocalDateTime;

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
