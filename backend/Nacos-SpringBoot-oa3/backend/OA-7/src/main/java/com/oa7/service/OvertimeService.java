package com.oa7.service;

import com.oa7.util.RESP;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

public interface OvertimeService {
    RESP getPending(int currentPage, int pageSize, HttpSession session);
    RESP approve(int id, BigDecimal actualHours, HttpSession session);
    RESP reject(int id, String reason, HttpSession session);
}
