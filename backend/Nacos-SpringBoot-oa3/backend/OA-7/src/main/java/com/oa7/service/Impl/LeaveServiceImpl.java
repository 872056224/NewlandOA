package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.constant.AdminRole;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.LeaveDao;
import com.oa7.dao.NotificationDao;
import com.oa7.pojo.Admin;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.Leave;
import com.oa7.service.LeaveService;
import com.oa7.service.RecalculateAttendanceService;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveDao leaveDao;

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private RecalculateAttendanceService recalculateAttendanceService;

    @Autowired
    private AttendanceDao attendanceDao;

    @Override
    public RESP getPending(int currentPage, int pageSize, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);

        PageHelper.startPage(currentPage, pageSize);
        List<Leave> list;

        if (admin != null && admin.isDeptHead()) {
            // DEPT_HEAD：只看到本部门（通过 dept_id 整数对比，更可靠）
            list = leaveDao.selectPendingByEmpDept(admin.getDeptId());
        } else {
            list = leaveDao.selectPending();
        }

        PageInfo<Leave> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP getByStatus(String status, int currentPage, int pageSize, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);

        PageHelper.startPage(currentPage, pageSize);
        List<Leave> list;

        if (admin != null && admin.isDeptHead()) {
            list = leaveDao.selectByStatusByEmpDept(status, admin.getDeptId());
        } else {
            list = leaveDao.selectByStatus(status);
        }

        PageInfo<Leave> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(String id, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        Leave leave = leaveDao.selectById(id);
        if (leave == null) return RESP.error("请假单不存在");
        if (!"待审批".equals(leave.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + leave.getStatus());
        }

        // DEPT_HEAD 只能审批本部门的申请（通过 dept_id 对比）
        if (admin.isDeptHead()) {
            Integer deptId = leaveDao.selectDeptIdByLeaveId(id);
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权审批其他部门的请假申请");
            }
        }

        int ret = leaveDao.updateStatusWithVersion(id, "已批准", leave.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        // 通知员工本人
        notificationDao.insert("leave_approved", "请假已批准",
                "您的" + leave.getType() + "申请已获批准（" + leave.getStart_date() + " ~ " + leave.getEnd_date() + "）",
                leave.getNumber(), id);

        notificationDao.markAllReadByBizId(id);

        // 考勤重算
        LocalDate startDate = LocalDate.parse(leave.getStart_date().substring(0, 10));
        LocalDate endDate = LocalDate.parse(leave.getEnd_date().substring(0, 10));
        recalculateAttendanceService.recalculate(leave.getNumber(), startDate, endDate);

        return RESP.ok("操作成功");
    }

    @Override
    public RESP reject(String id, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        Leave leave = leaveDao.selectById(id);
        if (leave == null) return RESP.error("请假单不存在");
        if (!"待审批".equals(leave.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + leave.getStatus());
        }

        // DEPT_HEAD 只能拒绝本部门的申请
        if (admin.isDeptHead()) {
            Integer deptId = leaveDao.selectDeptIdByLeaveId(id);
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权拒绝其他部门的请假申请");
            }
        }

        int ret = leaveDao.updateStatusWithVersion(id, "已拒绝", leave.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        notificationDao.insert("leave_rejected", "请假已拒绝",
                "您的" + leave.getType() + "申请已被拒绝（" + leave.getStart_date() + " ~ " + leave.getEnd_date() + "）",
                leave.getNumber(), id);

        notificationDao.markAllReadByBizId(id);

        LocalDate startDate = LocalDate.parse(leave.getStart_date().substring(0, 10));
        LocalDate endDate = LocalDate.parse(leave.getEnd_date().substring(0, 10));
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Attendance att = attendanceDao.selectByEmpAndDate(leave.getNumber(), date);
            if (att != null && att.getTodayStatus() == TodayStatus.LEAVE) {
                attendanceDao.updateTodayStatusByEmpAndDate(leave.getNumber(), date, TodayStatus.NOT_CHECKED_IN);
            }
        }
        recalculateAttendanceService.recalculate(leave.getNumber(), startDate, endDate);

        return RESP.ok("操作成功");
    }

    @Override
    public RESP revoke(String id, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        Leave leave = leaveDao.selectById(id);
        if (leave == null) return RESP.error("请假单不存在");
        if (!"已批准".equals(leave.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + leave.getStatus());
        }

        // DEPT_HEAD 只能撤销本部门的申请
        if (admin.isDeptHead()) {
            Integer deptId = leaveDao.selectDeptIdByLeaveId(id);
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权撤销其他部门的请假申请");
            }
        }

        int ret = leaveDao.updateStatusWithVersion(id, "待审批", leave.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        notificationDao.insert("leave_revoked", "请假已撤销",
                "您的" + leave.getType() + "申请已被撤销（" + leave.getStart_date() + " ~ " + leave.getEnd_date() + "）",
                leave.getNumber(), id);

        notificationDao.markAllReadByBizId(id);

        LocalDate startDate = LocalDate.parse(leave.getStart_date().substring(0, 10));
        LocalDate endDate = LocalDate.parse(leave.getEnd_date().substring(0, 10));
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Attendance att = attendanceDao.selectByEmpAndDate(leave.getNumber(), date);
            if (att != null && att.getTodayStatus() == TodayStatus.LEAVE) {
                attendanceDao.updateTodayStatusByEmpAndDate(leave.getNumber(), date, TodayStatus.NOT_CHECKED_IN);
            }
        }
        recalculateAttendanceService.recalculate(leave.getNumber(), startDate, endDate);

        return RESP.ok("操作成功");
    }
}
