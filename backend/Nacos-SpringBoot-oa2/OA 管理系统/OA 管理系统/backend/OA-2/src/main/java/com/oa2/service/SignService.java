package com.oa2.service;

import com.oa2.pojo.Sign;
import com.oa2.util.RESP;

public interface SignService {

    RESP getMyRecordsPage(int number, int currentPage, int pageSize);

    RESP getMyRecords(int number);

    RESP checkIn(Sign sign, String coordinates, String clientIp);
}
