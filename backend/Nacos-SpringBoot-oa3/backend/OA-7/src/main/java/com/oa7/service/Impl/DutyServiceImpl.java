package com.oa7.service.Impl;

import com.oa7.dao.DutyDao;
import com.oa7.pojo.Duty;
import com.oa7.service.DutyService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DutyServiceImpl implements DutyService {
    @Autowired private DutyDao dutyDao;

    @Override
    public RESP selectAll() {
        return RESP.ok(dutyDao.selectByPageHelper());
    }

    @Override
    public String add(Duty duty) {
        if (dutyDao.selectByName(duty) != null) return "false";
        return dutyDao.addDuty(duty) > 0 ? "true" : "false";
    }

    @Override
    public String update(int dutyId, Duty duty) {
        duty.setDuty_id(dutyId);
        return dutyDao.updateDutyNameById(duty) > 0 ? "true" : "false";
    }
}
