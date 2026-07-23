package com.oa2.service.impl;

import com.oa2.dao.AdminDao;
import com.oa2.dao.HolidayDao;
import com.oa2.dao.OvertimeDao;
import com.oa2.pojo.OvertimeRequest;
import com.oa2.service.NotificationService;
import com.oa2.service.OvertimeService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OvertimeServiceImpl implements OvertimeService {

    @Autowired
    private OvertimeDao overtimeDao;

    @Autowired
    private AdminDao adminDao;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private HolidayDao holidayDao;

    @Override
    public RESP apply(int empId, String overtimeDate, String startTime, String endTime, String reason) {
        LocalDate date = LocalDate.parse(overtimeDate);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        // 校验：只能申请非工作日
        String holidayType = holidayDao.selectHolidayTypeByDate(date);
        boolean isWorkday = (holidayType == null || "WORKDAY".equals(holidayType));
        if (isWorkday) {
            return RESP.error("只能在非工作日（周末/节假日）申请加班");
        }

        // 计算时长
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) return RESP.error("结束时间必须晚于开始时间");
        BigDecimal totalHours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);

        OvertimeRequest req = new OvertimeRequest();
        req.setEmpId(empId);
        req.setOvertimeDate(date);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setTotalHours(totalHours);
        req.setReason(reason != null ? reason : "");

        int ret = overtimeDao.insert(req);
        if (ret > 0) {
            // 通知相关管理员
            notifyAdmins(empId, "overtime_submitted", "新加班申请",
                    "员工提交了 " + overtimeDate + "（" + startTime + "~" + endTime + "）的加班申请",
                    String.valueOf(req.getId()));
            return RESP.ok("提交成功");
        }
        return RESP.error("提交失败");
    }

    @Override
    public RESP getMyList(int empId, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<OvertimeRequest> list = overtimeDao.selectByEmp(empId, offset, pageSize);
        int total = overtimeDao.countByEmp(empId);
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public RESP getMonthlyHours(int empId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Double hours = overtimeDao.sumMonthlyHours(empId, start, end);
        return RESP.ok(hours != null ? hours : 0.0);
    }

    private void notifyAdmins(int applicantNumber, String type, String title, String content, String bizId) {
        try {
            List<Integer> adminIds = adminDao.selectNotifyTargetIds(applicantNumber);
            for (int adminId : adminIds) {
                notificationService.sendNotification(adminId, type, title, content, bizId);
            }
        } catch (Exception e) {
            System.err.println("通知管理员失败: " + e.getMessage());
        }
    }
}
