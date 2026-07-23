package com.oa7.service;

import com.oa7.util.RESP;

import javax.servlet.http.HttpSession;

public interface LeaveService {
    RESP getPending(int currentPage, int pageSize, HttpSession session);
    RESP getByStatus(String status, int currentPage, int pageSize, HttpSession session);
    RESP approve(String id, HttpSession session);
    RESP reject(String id, HttpSession session);
    RESP revoke(String id, HttpSession session);
}
