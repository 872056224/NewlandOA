package com.oa2.service;

import com.oa2.util.RESP;

import javax.servlet.http.HttpSession;

public interface OvertimeService {
    RESP apply(int empId, String overtimeDate, String startTime, String endTime, String reason);
    RESP getMyList(int empId, int currentPage, int pageSize);
    RESP getMonthlyHours(int empId, String yearMonth);
}
