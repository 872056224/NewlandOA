package com.oa2.service.impl;

import com.oa2.constant.TodayStatus;
import com.oa2.dao.AdminDao;
import com.oa2.dao.AttendanceDao;
import com.oa2.dao.LeaveDao;
import com.oa2.pojo.Leave;
import com.oa2.service.LeaveService;
import com.oa2.service.NotificationService;
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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminDao adminDao;

    @Autowired
    private AttendanceDao attendanceDao;

    @Override
    public RESP apply(int number, String name, String deptName, String type,
                      String startDate, String endDate, String reason, String duration) {
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
        leave.setDuration(duration != null ? duration : "FULL_DAY");

        int ret = leaveDao.insert(leave);
        if (ret > 0) {
            // 全天请假 → 更新对应日期的 attendance today_status = LEAVE
            if ("FULL_DAY".equals(leave.getDuration())) {
                try {
                    LocalDate sd = LocalDate.parse(startDate.substring(0, 10));
                    LocalDate ed = LocalDate.parse(endDate.substring(0, 10));
                    for (LocalDate d = sd; !d.isAfter(ed); d = d.plusDays(1)) {
                        attendanceDao.updateTodayStatus(number, d, TodayStatus.LEAVE);
                    }
                } catch (Exception e) {
                    // 日期解析失败不影响主流程
                }
            } else if ("HALF_DAY_AM".equals(leave.getDuration()) || "HALF_DAY_PM".equals(leave.getDuration())) {
                try {
                    LocalDate d = LocalDate.parse(startDate.substring(0, 10));
                    attendanceDao.updateTodayStatus(number, d, TodayStatus.LEAVE);
                } catch (Exception e) {
                    // 静默处理
                }
            }

            // 通知所有管理员
            notifyAdmins("leave_submitted", "新请假申请",
                    name + " 提交了" + type + "申请（" + startDate + " ~ " + endDate + "）",
                    leave.getId());
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

    private void notifyAdmins(String type, String title, String content, String bizId) {
        try {
            List<Integer> adminIds = adminDao.selectAllIds();
            for (int adminId : adminIds) {
                notificationService.sendNotification(adminId, type, title, content, bizId);
            }
        } catch (Exception e) {
            System.err.println("通知管理员失败: " + e.getMessage());
        }
    }
}
