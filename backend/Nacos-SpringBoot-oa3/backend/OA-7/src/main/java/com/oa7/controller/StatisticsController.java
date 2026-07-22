package com.oa7.controller;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.EmpDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.MonthlyReportDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.Emp;
import com.oa7.pojo.Holiday;
import com.oa7.pojo.MonthlyReport;
import com.oa7.service.MonthlyReportService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计查询控制器 - 月度考勤统计
 */
@RestController
@RequestMapping("/statistics")
@CrossOrigin
public class StatisticsController {

    @Autowired
    private MonthlyReportDao monthlyReportDao;

    @Autowired
    private MonthlyReportService monthlyReportService;

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private HolidayDao holidayDao;

    /**
     * 个人月度考勤统计
     *
     * @param empId     员工编号
     * @param yearMonth 年月 (YYYY-MM)，默认上个月
     */
    @GetMapping("/personal/monthly")
    public RESP personalMonthly(@RequestParam int empId,
                                @RequestParam(required = false) String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = YearMonth.now().minusMonths(1).toString();
        }

        // 优先从 monthly_report 表读取（当月数据实时计算，避免脏缓存）
        YearMonth ym = YearMonth.parse(yearMonth);
        boolean isCurrentMonth = ym.equals(YearMonth.from(LocalDate.now()));
        MonthlyReport report = null;
        if (!isCurrentMonth) {
            report = monthlyReportDao.selectByEmpAndMonth(empId, yearMonth);
        }
        if (report != null) {
            return RESP.ok(report);
        }

        // 未生成则实时计算
        MonthlyReport computed = computePersonal(empId, yearMonth);
        if (computed == null) {
            return RESP.error("未找到该员工的考勤数据");
        }
        return RESP.ok(computed);
    }

    /**
     * 部门月度考勤统计总和
     *
     * @param deptId    部门ID
     * @param yearMonth 年月 (YYYY-MM)，默认上个月
     */
    @GetMapping("/department/monthly")
    public RESP departmentMonthly(@RequestParam int deptId,
                                   @RequestParam(required = false) String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = YearMonth.now().minusMonths(1).toString();
        }

        // 从 monthly_report 表查询部门下所有员工的月度统计
        List<MonthlyReport> reports = monthlyReportDao.selectByDeptAndMonth(deptId, yearMonth);
        if (reports.isEmpty()) {
            // 尝试实时聚合计算
            List<Emp> deptEmps = empDao.selectByPageHelper().stream()
                    .filter(e -> e.getDept_id() == deptId)
                    .collect(Collectors.toList());

            for (Emp emp : deptEmps) {
                MonthlyReport computed = computePersonal(emp.getNumber(), yearMonth);
                if (computed != null) {
                    reports.add(computed);
                }
            }
        }

        // 聚合统计
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("deptId", deptId);
        summary.put("yearMonth", yearMonth);
        summary.put("totalEmployees", reports.size());

        int totalWorkDays = reports.stream().mapToInt(r -> r.getWorkDays() != null ? r.getWorkDays() : 0).sum();
        int totalActualDays = reports.stream().mapToInt(r -> r.getActualDays() != null ? r.getActualDays() : 0).sum();
        int totalLate = reports.stream().mapToInt(r -> r.getLateCount() != null ? r.getLateCount() : 0).sum();
        int totalEarly = reports.stream().mapToInt(r -> r.getEarlyCount() != null ? r.getEarlyCount() : 0).sum();
        int totalLeave = reports.stream().mapToInt(r -> r.getLeaveCount() != null ? r.getLeaveCount() : 0).sum();
        int totalAbsence = reports.stream().mapToInt(r -> r.getAbsenceCount() != null ? r.getAbsenceCount() : 0).sum();
        int totalMissingCard = reports.stream().mapToInt(r -> r.getMissingCardCount() != null ? r.getMissingCardCount() : 0).sum();

        summary.put("totalWorkDays", totalWorkDays);
        summary.put("totalActualDays", totalActualDays);
        summary.put("totalLate", totalLate);
        summary.put("totalEarly", totalEarly);
        summary.put("totalLeave", totalLeave);
        summary.put("totalAbsence", totalAbsence);
        summary.put("totalMissingCard", totalMissingCard);

        // 计算出勤率 = 实际出勤总数 / (应出勤总数) * 100
        BigDecimal deptRate = BigDecimal.ZERO;
        if (totalWorkDays > 0) {
            deptRate = BigDecimal.valueOf(totalActualDays)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalWorkDays), 2, RoundingMode.HALF_UP);
        }
        summary.put("attendanceRate", deptRate);
        summary.put("details", reports);

        return RESP.ok(summary);
    }

    /**
     * 手动触发月度报表生成
     *
     * @param year  年份，默认上个月
     * @param month 月份，默认上个月
     */
    @PostMapping("/monthly/generate")
    public RESP generateMonthly(@RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) Integer month) {
        YearMonth target;
        if (year != null && month != null) {
            target = YearMonth.of(year, month);
        } else {
            target = YearMonth.now().minusMonths(1);
        }

        try {
            monthlyReportService.generate(target.getYear(), target.getMonthValue());
            return RESP.ok("月度报表生成完成: " + target.toString());
        } catch (Exception e) {
            return RESP.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 当月折线图数据：当月截至今天的各工作日签到人数
     * 横坐标=工作日日期，纵坐标=签到人数
     */
    @GetMapping("/monthly/trend")
    public RESP monthlyTrend(@RequestParam(required = false) String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = YearMonth.now().toString();
        }
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate today = LocalDate.now();
        LocalDate monthEnd = today.isAfter(ym.atEndOfMonth()) ? ym.atEndOfMonth() : today;

        // 该月所有WORKDAY日期
        List<LocalDate> workdays = holidayDao.selectByDateRange(monthStart, monthEnd).stream()
                .filter(h -> "WORKDAY".equals(h.getType()))
                .map(h -> h.getDate())
                .sorted()
                .collect(Collectors.toList());

        List<String> dateLabels = new ArrayList<>();
        List<Integer> signedData = new ArrayList<>();

        for (LocalDate date : workdays) {
            List<Attendance> records = attendanceDao.selectByDate(date);
            long signedCount = records.stream()
                    .filter(a -> a.getCheckInTime() != null)
                    .count();
            dateLabels.add(date.toString());
            signedData.add((int) signedCount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dateLabels);
        result.put("signed", signedData);
        result.put("yearMonth", yearMonth);
        return RESP.ok(result);
    }

    /**
     * 实时计算单个员工的月度考勤统计
     */
    private MonthlyReport computePersonal(int empId, String yearMonthStr) {
        YearMonth yearMonth = YearMonth.parse(yearMonthStr);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();

        // 如果是当月，只统计到今天为止（出勤率应基于已过天数）
        boolean isCurrentMonth = yearMonth.equals(YearMonth.from(today));
        LocalDate effectiveEnd = isCurrentMonth ? today : monthEnd;

        Emp emp = empDao.selectByEmpNumber(empId);
        if (emp == null) {
            return null;
        }

        // 查询截至 effectiveEnd 的 WORKDAY 天数
        List<Holiday> holidays = holidayDao.selectByDateRange(monthStart, effectiveEnd);
        int workDays = (int) holidays.stream()
                .filter(h -> "WORKDAY".equals(h.getType()))
                .count();
        if (workDays == 0) {
            workDays = countWeekdays(monthStart, effectiveEnd);
        }

        // 查询考勤记录（到有效截止日）
        List<Attendance> records = attendanceDao.selectByEmpAndDateRange(empId, monthStart, effectiveEnd);

        int normalCount = 0, lateCount = 0, earlyCount = 0;
        int leaveCount = 0, absenceCount = 0, missingCardCount = 0;

        for (Attendance record : records) {
            if (record.getAttendanceStatus() != null) {
                switch (record.getAttendanceStatus().name()) {
                    case "NORMAL": normalCount++; break;
                    case "LATE": lateCount++; break;
                    case "EARLY": earlyCount++; break;
                    case "LATE_EARLY": lateCount++; earlyCount++; break;
                    case "LEAVE": leaveCount++; break;
                    case "ABSENCE": absenceCount++; break;
                    case "MISSING_CARD": missingCardCount++; break;
                    default: break;
                }
            } else if (record.getCheckInTime() != null) {
                // 未结算但有签到记录 -> 算正常出勤
                normalCount++;
            } else if (record.getTodayStatus() == TodayStatus.LEAVE) {
                leaveCount++;
            }
        }

        int actualDays = normalCount + lateCount + earlyCount;

        BigDecimal attendanceRate = BigDecimal.ZERO;
        if (workDays > 0) {
            attendanceRate = BigDecimal.valueOf(actualDays)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP);
        }

        MonthlyReport report = new MonthlyReport();
        report.setYearMonth(yearMonthStr);
        report.setEmpId(empId);
        report.setEmpName(emp.getName());
        report.setDeptId(emp.getDept_id());
        report.setWorkDays(workDays);
        report.setActualDays(actualDays);
        report.setLateCount(lateCount);
        report.setEarlyCount(earlyCount);
        report.setLeaveCount(leaveCount);
        report.setAbsenceCount(absenceCount);
        report.setMissingCardCount(missingCardCount);
        report.setAttendanceRate(attendanceRate);

        return report;
    }

    /**
     * 计算日期范围内的周一到周五天数
     */
    private int countWeekdays(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate date = start;
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek().getValue() <= 5) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }
}
