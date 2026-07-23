package com.oa7.pojo;

import lombok.Data;

/**
 * @name: chenle
 * @Date: 2021/12/3 0:51
 * @Author: IAO
 * @Description: ...
 */
@Data
public class O {

    private String date;
    private int totalEmployees;
    private int onLeave;
    private int signed;
    private int unsigned;
    private int missingDuration;  // 缺时长人数（核心工作时间覆盖不足30分钟）
    private int anomaly;          // 打卡异常（仅签到无签退）
}
