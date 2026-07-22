package com.oa2.service;

import com.oa2.util.RESP;

public interface MakeupRequestService {
    RESP apply(int empId, String date, String type, String requestTime, String reason);
    RESP getMyList(int empId, int currentPage, int pageSize);
}
