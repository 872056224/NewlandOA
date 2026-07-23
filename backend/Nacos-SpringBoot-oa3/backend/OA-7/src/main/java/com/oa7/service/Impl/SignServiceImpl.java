package com.oa7.service.Impl;

import com.oa7.dao.AttendanceDao;
import com.oa7.dao.EmpDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.dao.SignDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.O;
import com.oa7.pojo.Sign;
import com.oa7.service.SignService;
import com.oa7.util.DU;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignServiceImpl implements SignService {

    @Autowired
    private SignDao signDao;
    @Autowired
    private AttendanceDao attendanceDao;
    @Autowired
    private HolidayDao holidayDao;
    @Autowired
    private EmpDao empDao;
    @Autowired
    private LeaveDao leaveDao;

    /** 核心工作时间 09:00 - 18:00 */
    private static final LocalTime CORE_START = LocalTime.of(9, 0);
    private static final LocalTime CORE_END = LocalTime.of(18, 0);
    /** 缺时宽限默认（分钟） */
    private static final int DEFAULT_TOLERANCE = 30;

    @Autowired
    private com.oa7.service.AttendanceRuleService attendanceRuleService;

    private int getTotalEmployeeCount() {
        return empDao.countUser();
    }

    /**
     * 获取缺时宽限（从考勤规则读取）
     */
    private int getToleranceMinutes() {
        try {
            com.oa7.pojo.AttendanceRule rule = attendanceRuleService.getDefaultRule();
            if (rule != null && rule.getMissingToleranceMin() != null) {
                return rule.getMissingToleranceMin();
            }
        } catch (Exception e) {
            // 使用默认值
        }
        return DEFAULT_TOLERANCE;
    }

    /**
     * 计算缺时时长（与 RecalculateAttendanceService 一致的逻辑）
     *
     * ① 缺时X > 宽限 → 全记
     * ② 缺时X ≤ 宽限 AND 加班 > 宽限 → 全抵
     * ③ 缺时X ≤ 宽限 AND 加班 ≤ 宽限 → max(0, 缺时X - 加班)
     */
    private int calcMissingMinutes(LocalTime checkIn, LocalTime checkOut) {
        if (checkIn == null || checkOut == null) return 0;
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
        int tolerance = getToleranceMinutes();

        if (missingX > tolerance) return (int) missingX;        // ①
        if (overtime > tolerance) return 0;                     // ②
        return (int) Math.max(0, missingX - overtime);          // ③
    }

    /** 打卡成功 = 签到 + 签退 都有 */
    private boolean isComplete(Attendance a) {
        return a.getCheckInTime() != null && a.getCheckOutTime() != null;
    }

    @Override
    public RESP todaySigned(int currentPage, int pageSize) {
        String today = DU.getNowSortString();
        int offset = (currentPage - 1) * pageSize;
        List<Sign> list = signDao.selectToDayYesByPage(offset, pageSize, today);
        int total = signDao.countToDayYes(today);
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public RESP dailyStatistics(int currentPage, int pageSize) {
        LocalDate today = LocalDate.now();
        int totalEmployees = getTotalEmployeeCount();

        List<LocalDate> workdayDates = holidayDao.selectAllWorkdayDates().stream()
                .filter(d -> !d.isAfter(today))
                .collect(Collectors.toList());
        int total = workdayDates.size();

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        if (start >= total) {
            return RESP.ok(Collections.emptyList(), currentPage, total);
        }
        List<LocalDate> pageDates = workdayDates.subList(start, end);

        List<O> statsList = new ArrayList<>();
        boolean isToday = false;
        for (LocalDate date : pageDates) {
            isToday = date.equals(today);
            int onLeave = leaveDao.countApprovedLeaveByDate(date.toString());
            List<Attendance> records = attendanceDao.selectByDate(date);

            int signed = 0;          // 签到+签退齐全
            int anomaly = 0;          // 打卡异常（仅签到无签退）
            int missingDuration = 0; // 当日缺时总分钟数

            for (Attendance a : records) {
                if (a.getCheckInTime() != null && a.getCheckOutTime() != null) {
                    signed++;
                    missingDuration += calcMissingMinutes(
                        a.getCheckInTime().toLocalTime(),
                        a.getCheckOutTime().toLocalTime());
                } else if (a.getCheckInTime() != null) {
                    anomaly++;
                }
            }

            // 未签到 = 总人数 - 请假 - 签到完成
            // 当天：打卡异常也当未签到（因为还没到结算时间）
            // 历史：打卡异常单独列出来
            int unsigned = isToday
                ? Math.max(0, totalEmployees - onLeave - signed)
                : Math.max(0, totalEmployees - onLeave - signed - anomaly);

            O o = new O();
            o.setDate(date.toString());
            o.setTotalEmployees(totalEmployees);
            o.setOnLeave(onLeave);
            o.setSigned(signed);
            o.setUnsigned(unsigned);
            o.setMissingDuration(missingDuration);
            o.setAnomaly(anomaly);
            statsList.add(o);
        }

        return RESP.ok(statsList, currentPage, total);
    }

    @Override
    public RESP dailyDetails(String date) {
        int totalEmployees = getTotalEmployeeCount();
        int onLeave = leaveDao.countApprovedLeaveByDate(date);

        LocalDate localDate = LocalDate.parse(date);
        List<Attendance> records = attendanceDao.selectByDate(localDate);

        int signed = 0;
        int anomaly = 0;
        int missingDuration = 0; // 当日缺时总分钟数
        for (Attendance a : records) {
            if (a.getCheckInTime() != null && a.getCheckOutTime() != null) {
                signed++;
                missingDuration += calcMissingMinutes(
                    a.getCheckInTime().toLocalTime(),
                    a.getCheckOutTime().toLocalTime());
            } else if (a.getCheckInTime() != null) {
                anomaly++;
            }
        }
        boolean isToday = localDate.equals(LocalDate.now());
        int unsigned = isToday
            ? Math.max(0, totalEmployees - onLeave - signed)
            : Math.max(0, totalEmployees - onLeave - signed - anomaly);

        Map<String, Object> result = new HashMap<>();
        result.put("totalEmployees", totalEmployees);
        result.put("onLeave", onLeave);
        result.put("signed", signed);
        result.put("unsigned", unsigned);
        result.put("missingDuration", missingDuration);
        result.put("anomaly", anomaly);
        result.put("expected", totalEmployees - onLeave);

        return RESP.ok(result);
    }

    @Override
    public RESP chartData() {
        LocalDate today = LocalDate.now();
        int totalEmployees = getTotalEmployeeCount();

        List<LocalDate> workdayDates = holidayDao.selectAllWorkdayDates().stream()
                .filter(d -> !d.isAfter(today))
                .limit(4)
                .collect(Collectors.toList());
        Collections.reverse(workdayDates);

        List<String> dateLabels = new ArrayList<>();
        List<Integer> signedData = new ArrayList<>();
        List<Integer> unsignedData = new ArrayList<>();
        List<Integer> leaveData = new ArrayList<>();

        for (LocalDate date : workdayDates) {
            int onLeave = leaveDao.countApprovedLeaveByDate(date.toString());
            List<Attendance> records = attendanceDao.selectByDate(date);

            int signedCount = 0;
            int anomalyCount = 0;
            for (Attendance a : records) {
                if (isComplete(a)) {
                    signedCount++;
                } else if (a.getCheckInTime() != null) {
                    anomalyCount++;
                }
            }

            dateLabels.add(date.toString());
            signedData.add(signedCount);
            // 柱状图：未签到 = 没打卡的 + 仅签到的（打卡异常当天也当未签到显示）
            unsignedData.add(Math.max(0, totalEmployees - onLeave - signedCount));
            leaveData.add(onLeave);
        }

        return RESP.ok(dateLabels, signedData, unsignedData, leaveData);
    }

    @Override
    public RESP unsigned(int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<Sign> list = signDao.selectNoByPage(offset, pageSize);
        int total = signDao.countUserNo();
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public RESP todayUnsigned(int currentPage, int pageSize) {
        String today = DU.getNowSortString();
        int offset = (currentPage - 1) * pageSize;
        List<Sign> list = signDao.selectToDayNoByPage(offset, pageSize, today);
        int total = signDao.countToDayNo(today);
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public String approve(int id) {
        List<Sign> all = signDao.selectAll();
        for (Sign s : all) {
            if (Integer.parseInt(s.getId()) == id) {
                s.setState("已签到");
                signDao.updateState(s, DU.getNowString());
                return "true";
            }
        }
        return "false";
    }
}
