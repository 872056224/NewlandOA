package com.oa2.service;

import com.oa2.util.RESP;

public interface LeaveService {
    RESP apply(int number, String name, String deptName, String type, String startDate, String endDate, String reason, String duration);
    RESP getMyList(int number, int currentPage, int pageSize);
    RESP getTodayStatus(int number);
}
