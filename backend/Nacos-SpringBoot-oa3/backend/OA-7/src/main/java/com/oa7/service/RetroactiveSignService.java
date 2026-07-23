package com.oa7.service;

import com.oa7.util.RESP;

import javax.servlet.http.HttpSession;

public interface RetroactiveSignService {
    RESP getPending(int currentPage, int pageSize, HttpSession session);
    RESP approve(int id, HttpSession session);
    RESP reject(int id, HttpSession session);
    RESP revoke(int id, HttpSession session);
}
