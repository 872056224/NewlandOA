package com.oa2.service;

import com.oa2.util.RESP;

public interface RetroactiveSignService {
    RESP apply(int number, String signDate, String type, String reason);
    RESP getMyList(int number, int currentPage, int pageSize);
}
