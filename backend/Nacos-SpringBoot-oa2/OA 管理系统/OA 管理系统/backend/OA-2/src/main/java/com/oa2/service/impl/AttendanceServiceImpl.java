package com.oa2.service.impl;

import com.oa2.constant.TodayStatus;
import com.oa2.dao.AttendanceDao;
import com.oa2.pojo.Attendance;
import com.oa2.service.AttendanceService;
import com.oa2.util.LocationUtil;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceDao attendanceDao;

    /** 上班标准时间 09:00 */
    private static final LocalTime STANDARD_IN = LocalTime.of(9, 0);
    /** 下班标准时间 18:00 */
    private static final LocalTime STANDARD_OUT = LocalTime.of(18, 0);

    @Override
    public RESP checkIn(int empId, String coordinates, String clientIp) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 已签到则不允许重复签到
        Attendance existing = attendanceDao.selectByEmpAndDate(empId, today);
        if (existing != null && TodayStatus.CHECKED_IN == existing.getTodayStatus()) {
            return RESP.error("您已签到，无需重复签到");
        }
        if (existing != null && TodayStatus.CHECKED_OUT == existing.getTodayStatus()) {
            return RESP.error("您已签退，无法再次签到");
        }
        // 请假状态不允许签到
        if (existing != null && TodayStatus.LEAVE == existing.getTodayStatus()) {
            return RESP.error("您今天已请假，无需签到");
        }

        // 获取地址
        String address = resolveAddress(coordinates);
        // 执行签到（UPSERT），存地址
        attendanceDao.checkIn(empId, today, now, address);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("time", now.toString());
        data.put("address", address);
        data.put("status", getTimingStatus(now.toLocalTime(), "in"));
        return RESP.ok(data);
    }

    @Override
    public RESP checkOut(int empId, String coordinates, String clientIp) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance existing = attendanceDao.selectByEmpAndDate(empId, today);
        if (existing == null) {
            return RESP.error("请先签到再签退");
        }
        if (TodayStatus.CHECKED_OUT == existing.getTodayStatus()) {
            return RESP.error("您已签退，无需重复签退");
        }
        if (TodayStatus.LEAVE == existing.getTodayStatus()) {
            return RESP.error("您今天已请假，无需签退");
        }

        String address = resolveAddress(coordinates);
        attendanceDao.checkOut(empId, today, now, address);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("time", now.toString());
        data.put("address", address);
        data.put("status", getTimingStatus(now.toLocalTime(), "out"));
        return RESP.ok(data);
    }

    @Override
    public RESP getTodayStatus(int empId) {
        LocalDate today = LocalDate.now();
        Attendance att = attendanceDao.selectByEmpAndDate(empId, today);

        Map<String, Object> data = new LinkedHashMap<>();
        if (att == null) {
            // 当天没有记录（凌晨定时任务还没跑或员工未处理）
            data.put("todayStatus", TodayStatus.NOT_CHECKED_IN);
            data.put("checkInTime", null);
            data.put("checkOutTime", null);
        } else {
            data.put("todayStatus", att.getTodayStatus());
            data.put("checkInTime", att.getCheckInTime() != null ? att.getCheckInTime().toString() : null);
            data.put("checkOutTime", att.getCheckOutTime() != null ? att.getCheckOutTime().toString() : null);
            data.put("checkInAddress", att.getCheckInAddress());
            data.put("checkOutAddress", att.getCheckOutAddress());
            if (att.getCheckInTime() != null) {
                data.put("checkInStatus", getTimingStatus(att.getCheckInTime().toLocalTime(), "in"));
            }
            if (att.getCheckOutTime() != null) {
                data.put("checkOutStatus", getTimingStatus(att.getCheckOutTime().toLocalTime(), "out"));
            }
        }
        return RESP.ok(data);
    }

    @Override
    public RESP getHistory(int empId, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<Attendance> list = attendanceDao.selectByEmpPage(empId, offset, pageSize);
        int total = attendanceDao.countByEmp(empId);
        return RESP.ok(list, currentPage, total);
    }

    /** 解析地址 */
    private String resolveAddress(String coordinates) {
        if (coordinates != null && !coordinates.isEmpty()) {
            String address = LocationUtil.getAddressFromCoordinates(coordinates);
            if (address != null && !address.contains("错误") && !address.contains("失败")) {
                return address;
            }
        }
        String ipAddress = LocationUtil.getLocationByIp(null);
        return ipAddress != null ? ipAddress : coordinates != null ? coordinates : "未知位置";
    }

    /** 判断签到/签退的时间状态 */
    private String getTimingStatus(LocalTime time, String type) {
        if ("in".equals(type)) {
            if (time.isBefore(STANDARD_IN) || time.equals(STANDARD_IN)) {
                return "正常";
            }
            int lateMin = time.toSecondOfDay() / 60 - STANDARD_IN.toSecondOfDay() / 60;
            return "迟到 " + (lateMin / 60) + "小时" + (lateMin % 60) + "分钟";
        } else {
            if (time.isAfter(STANDARD_OUT) || time.equals(STANDARD_OUT)) {
                return "正常";
            }
            int earlyMin = STANDARD_OUT.toSecondOfDay() / 60 - time.toSecondOfDay() / 60;
            return "早退 " + (earlyMin / 60) + "小时" + (earlyMin % 60) + "分钟";
        }
    }
}
