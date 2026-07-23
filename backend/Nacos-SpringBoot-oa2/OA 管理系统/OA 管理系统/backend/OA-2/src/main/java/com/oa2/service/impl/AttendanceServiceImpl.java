package com.oa2.service.impl;

import com.oa2.constant.TodayStatus;
import com.oa2.dao.AttendanceDao;
import com.oa2.pojo.Attendance;
import com.oa2.service.AttendanceService;
import com.oa2.util.LocationUtil;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
            data.put("todayStatus", TodayStatus.NOT_CHECKED_IN);
            data.put("checkInTime", null);
            data.put("checkOutTime", null);
        } else {
            data.put("todayStatus", resolveDisplayStatus(att, today));
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
        // 为历史记录计算正确的显示状态和缺时时长
        for (Attendance att : list) {
            att.setTodayStatus(resolveDisplayStatus(att, att.getDate()));
            att.setMissingDuration(att.getCheckInTime() != null && att.getCheckOutTime() != null
                ? computeMissingDuration(att.getCheckInTime().toLocalTime(), att.getCheckOutTime().toLocalTime())
                : 0);
        }
        // 过滤掉节假日和休息日（前端不展示）
        int removed = 0;
        java.util.Iterator<Attendance> it = list.iterator();
        while (it.hasNext()) {
            Attendance att = it.next();
            if (att.getTodayStatus() == TodayStatus.HOLIDAY || att.getTodayStatus() == TodayStatus.REST_DAY) {
                it.remove();
                removed++;
            }
        }
        total = Math.max(0, total - removed);
        return RESP.ok(list, currentPage, total);
    }

    /**
     * 判断考勤记录的显示状态：
     * - 签到+签退 → 已签退
     * - 仅签到无签退 → 签到异常
     * - 无签到 → 未签到（除非请假）
     */
    private TodayStatus resolveDisplayStatus(Attendance att, LocalDate date) {
        // 优先使用数据库中的节假日/休息日状态（节假日管理页面修改后同步过来）
        if (att.getTodayStatus() == TodayStatus.LEAVE) {
            return TodayStatus.LEAVE;
        }
        if (att.getTodayStatus() == TodayStatus.HOLIDAY || att.getTodayStatus() == TodayStatus.REST_DAY) {
            return att.getTodayStatus();  // 保持节假日/休息日状态
        }
        if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
            return TodayStatus.CHECKED_OUT;  // 已签退
        }
        if (att.getCheckInTime() != null && att.getCheckOutTime() == null) {
            return TodayStatus.ANOMALY;  // 签到异常
        }
        return TodayStatus.NOT_CHECKED_IN;  // 未签到
    }

    @Override
    public RESP getMonthlyMissingDuration(int empId, String yearMonth) {
        java.time.YearMonth ym = java.time.YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        if (ym.equals(java.time.YearMonth.now())) {
            end = LocalDate.now();
        }
        // 从 attendance 表获取当月所有有签到记录的明细，实时计算缺时
        List<Attendance> records = attendanceDao.selectByEmpAndDateRange(empId, start, end);
        int totalMinutes = 0;
        for (Attendance a : records) {
            // 跳过请假/节假日/休息日（这些日期不计算缺时）
            TodayStatus ts = a.getTodayStatus();
            if (ts == TodayStatus.LEAVE || ts == TodayStatus.HOLIDAY || ts == TodayStatus.REST_DAY) continue;
            if (a.getCheckInTime() != null && a.getCheckOutTime() != null) {
                totalMinutes += computeMissingDuration(
                    a.getCheckInTime().toLocalTime(),
                    a.getCheckOutTime().toLocalTime());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("empId", empId);
        result.put("yearMonth", yearMonth);
        result.put("totalMinutes", totalMinutes);
        return RESP.ok(result);
    }

    /** 核心工作时间 */
    private static final LocalTime CORE_START = LocalTime.of(9, 0);
    private static final LocalTime CORE_END = LocalTime.of(18, 0);
    private static final int TOLERANCE = 30;

    /** 实时计算单日缺时（与 OA-7 算法一致） */
    private int computeMissingDuration(LocalTime checkIn, LocalTime checkOut) {
        long missingX = 0;
        if (checkIn.isAfter(CORE_START)) {
            missingX += Duration.between(CORE_START, checkIn).toMinutes();
        }
        if (checkOut.isBefore(CORE_END)) {
            missingX += Duration.between(checkOut, CORE_END).toMinutes();
        }
        long totalMinutes = Duration.between(checkIn, checkOut).toMinutes();
        long coreMinutes = Duration.between(CORE_START, CORE_END).toMinutes();
        long overtime = Math.max(0, totalMinutes - coreMinutes);
        if (missingX > TOLERANCE) return (int) missingX;
        if (overtime > TOLERANCE) return 0;
        return (int) Math.max(0, missingX - overtime);
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
