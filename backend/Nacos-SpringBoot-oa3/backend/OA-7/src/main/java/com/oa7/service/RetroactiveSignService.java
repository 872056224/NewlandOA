package com.oa7.service;

import com.oa7.util.RESP;

public interface RetroactiveSignService {
    RESP getPending(int currentPage, int pageSize);
    RESP approve(int id);
    RESP reject(int id);
    RESP revoke(int id);
}
