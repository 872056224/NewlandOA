package com.oa7.service;

import com.oa7.pojo.Duty;
import com.oa7.util.RESP;

public interface DutyService {
    RESP selectAll();
    String add(Duty duty);
    String update(int dutyId, Duty duty);
}
