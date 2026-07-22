package com.oa7.service;

import com.oa7.util.RESP;

public interface LeaveService {
    RESP getPending(int currentPage, int pageSize);
    RESP getByStatus(String status, int currentPage, int pageSize);
    RESP approve(String id);
    RESP reject(String id);
    RESP revoke(String id);
}
