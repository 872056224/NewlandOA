package com.oa2.service;

import com.oa2.util.RESP;

public interface AttendanceService {
    /** 签到 */
    RESP checkIn(int empId, String coordinates, String clientIp);
    /** 签退 */
    RESP checkOut(int empId, String coordinates, String clientIp);
    /** 获取今日考勤状态 */
    RESP getTodayStatus(int empId);
    /** 获取历史考勤记录（分页） */
    RESP getHistory(int empId, int currentPage, int pageSize);

    RESP getMonthlyMissingDuration(int empId, String yearMonth);
}
