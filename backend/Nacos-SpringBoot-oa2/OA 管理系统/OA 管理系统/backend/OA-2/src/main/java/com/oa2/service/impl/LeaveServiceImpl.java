package com.oa2.service.impl;

import com.oa2.dao.LeaveDao;
import com.oa2.pojo.Leave;
import com.oa2.service.LeaveService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveDao leaveDao;

    @Override
    public RESP apply(int number, String name, String deptName, String type,
                      String startDate, String endDate, String reason) {
        Leave leave = new Leave();
        leave.setId(UUID.randomUUID().toString());
        leave.setNumber(number);
        leave.setName(name);
        leave.setType(type);
        leave.setDept_name(deptName);
        leave.setStart_date(startDate);
        leave.setEnd_date(endDate);
        leave.setReason(reason);
        leave.setStatus("待审批");

        int ret = leaveDao.insert(leave);
        if (ret > 0) {
            return RESP.ok("提交成功");
        }
        return RESP.error("提交失败，请重试");
    }

    @Override
    public RESP getMyList(int number, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<Leave> list = leaveDao.selectByNumberPage(number, offset, pageSize);
        int total = leaveDao.countByNumber(number);
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public RESP getTodayStatus(int number) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        int count = leaveDao.countApprovedLeaveToday(number, today);
        return RESP.ok(count > 0);
    }
}
