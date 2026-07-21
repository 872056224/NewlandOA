package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.LeaveDao;
import com.oa7.dao.NotificationDao;
import com.oa7.pojo.Leave;
import com.oa7.service.LeaveService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveDao leaveDao;

    @Autowired
    private NotificationDao notificationDao;

    @Override
    public RESP getPending(int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<Leave> list = leaveDao.selectPending();
        PageInfo<Leave> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP getByStatus(String status, int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<Leave> list = leaveDao.selectByStatus(status);
        PageInfo<Leave> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(String id) {
        Leave leave = leaveDao.selectById(id);
        if (leave == null) return RESP.error("请假单不存在");
        int ret = leaveDao.updateStatus(id, "已批准");
        if (ret > 0) {
            notificationDao.insert("leave_approved", "请假已批准",
                    "您的" + leave.getType() + "申请已获批准（" + leave.getStart_date() + " ~ " + leave.getEnd_date() + "）",
                    leave.getNumber(), id);
            return RESP.ok("操作成功");
        }
        return RESP.error("操作失败");
    }

    @Override
    public RESP reject(String id) {
        Leave leave = leaveDao.selectById(id);
        if (leave == null) return RESP.error("请假单不存在");
        int ret = leaveDao.updateStatus(id, "已拒绝");
        if (ret > 0) {
            notificationDao.insert("leave_rejected", "请假已拒绝",
                    "您的" + leave.getType() + "申请已被拒绝（" + leave.getStart_date() + " ~ " + leave.getEnd_date() + "）",
                    leave.getNumber(), id);
            return RESP.ok("操作成功");
        }
        return RESP.error("操作失败");
    }
}
