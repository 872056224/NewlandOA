package com.oa7.service.Impl;

import com.oa7.dao.*;
import com.oa7.pojo.*;
import com.oa7.service.SalaryService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalaryServiceImpl implements SalaryService {

    @Autowired
    private SalaryDao salaryDao;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private HolidayDao holidayDao;

    @Autowired
    private OvertimeDao overtimeDao;

    /** 岗位薪资标准 */
    private static final Map<Integer, BigDecimal> SALARY_MAP = new HashMap<>();
    static {
        SALARY_MAP.put(17, BigDecimal.valueOf(50000));
        SALARY_MAP.put(1,  BigDecimal.valueOf(35000));
        SALARY_MAP.put(2,  BigDecimal.valueOf(30000));
        SALARY_MAP.put(3,  BigDecimal.valueOf(25000));
        SALARY_MAP.put(4,  BigDecimal.valueOf(20000));
        SALARY_MAP.put(5,  BigDecimal.valueOf(6000));
        SALARY_MAP.put(9,  BigDecimal.valueOf(8000));
        SALARY_MAP.put(10, BigDecimal.valueOf(15000));
        SALARY_MAP.put(16, BigDecimal.valueOf(8000));
    }

    private static final int CORE_HOURS = 9;
    private static final BigDecimal OVERTIME_MULTIPLIER = BigDecimal.valueOf(2);
    private static final BigDecimal LEAVE_RATIO = BigDecimal.valueOf(0.8);

    @Override
    public RESP calculate(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        // 整月应出勤天数（22天，不论是否当前月）
        int fullMonthWorkDays = countFullMonthWorkDays(ym);

        // 截止今天的日期（当前月用今天，过往月用月底）
        LocalDate cutoff = ym.equals(YearMonth.now()) ? today : monthEnd;

        List<Emp> allEmps = empDao.selectByPageHelper();
        salaryDao.deleteByMonth(yearMonth);

        int calculated = 0;
        for (Emp emp : allEmps) {
            try {
                SalaryDetail detail = calculateOne(emp, yearMonth, monthStart, cutoff, monthEnd, fullMonthWorkDays);
                salaryDao.insert(detail);
                calculated++;
            } catch (Exception e) {
                System.err.println("工资核算失败: emp=" + emp.getNumber() + " " + e.getMessage());
            }
        }

        return RESP.ok("核算完成，共 " + calculated + " 人");
    }

    private SalaryDetail calculateOne(Emp emp, String yearMonth,
                                       LocalDate monthStart, LocalDate cutoff,
                                       LocalDate monthEnd, int fullMonthWorkDays) {
        SalaryDetail detail = new SalaryDetail();
        detail.setEmpId(emp.getNumber());
        detail.setYearMonth(yearMonth);
        detail.setWorkDays(fullMonthWorkDays);

        // 1. 基础月薪（优先取自定义月薪，没有则按职务默认）
        java.math.BigDecimal customSalary = emp.getBase_salary();
        BigDecimal baseSalary = customSalary != null
                ? customSalary
                : SALARY_MAP.getOrDefault(emp.getDuty_id(), BigDecimal.valueOf(5000));
        detail.setBaseSalary(baseSalary);

        // 2. 日/小时/分钟工资（按整月应出勤天数算）
        BigDecimal dailyWage = baseSalary.divide(BigDecimal.valueOf(fullMonthWorkDays), 4, RoundingMode.HALF_UP);
        BigDecimal hourlyWage = dailyWage.divide(BigDecimal.valueOf(CORE_HOURS), 4, RoundingMode.HALF_UP);
        detail.setDailyWage(dailyWage);
        detail.setHourlyWage(hourlyWage);

        // 3. 截止今天的实际出勤天数
        Integer actualDays = salaryDao.countActualAttendance(emp.getNumber(), monthStart, cutoff);
        if (actualDays == null) actualDays = 0;
        // 加上请假天数（请假算80%出勤）
        Integer leaveDays = salaryDao.countLeaveDays(emp.getNumber(), monthStart, cutoff);
        if (leaveDays == null) leaveDays = 0;
        detail.setLeaveDays(BigDecimal.valueOf(leaveDays));

        // 4. 基础工资 = 日薪 × 实际出勤天数
        BigDecimal basePay = dailyWage.multiply(BigDecimal.valueOf(actualDays))
                .setScale(2, RoundingMode.HALF_UP);
        //    请假工资 = 日薪 × 请假天数 × 0.8
        BigDecimal leavePay = dailyWage.multiply(BigDecimal.valueOf(leaveDays))
                .multiply(LEAVE_RATIO).setScale(2, RoundingMode.HALF_UP);
        detail.setLeaveDeduction(dailyWage.multiply(BigDecimal.valueOf(leaveDays))
                .subtract(leavePay).setScale(2, RoundingMode.HALF_UP));

        // 5. 缺时扣款
        Integer missingMin = salaryDao.sumMissingMinutes(emp.getNumber(), monthStart, cutoff);
        if (missingMin == null) missingMin = 0;
        detail.setActualAttendanceDays(actualDays);
        detail.setTotalMissingMinutes(missingMin);
        BigDecimal minuteWage = hourlyWage.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal missingDeduction = minuteWage.multiply(BigDecimal.valueOf(missingMin))
                .setScale(2, RoundingMode.HALF_UP);
        detail.setMissingDeduction(missingDeduction);

        // 6. 加班工资（整月范围，已批准的加班都应计入）
        java.math.BigDecimal otHours = overtimeDao.sumMonthlyHours(emp.getNumber(), monthStart, monthEnd);
        double otHoursVal = otHours != null ? otHours.doubleValue() : 0;
        detail.setOvertimeHours(BigDecimal.valueOf(otHoursVal));
        BigDecimal overtimePay = hourlyWage.multiply(OVERTIME_MULTIPLIER)
                .multiply(BigDecimal.valueOf(otHoursVal))
                .setScale(2, RoundingMode.HALF_UP);
        detail.setOvertimePay(overtimePay);

        // 7. 最终工资 = 基础工资 + 请假工资 - 缺时扣款 + 加班工资
        BigDecimal finalSalary = basePay.add(leavePay).subtract(missingDeduction).add(overtimePay)
                .setScale(2, RoundingMode.HALF_UP);
        detail.setFinalSalary(finalSalary);

        return detail;
    }

    /** 整月应出勤天数 */
    private int countFullMonthWorkDays(YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            String type = holidayDao.selectHolidayTypeByDate(d);
            if ("HOLIDAY".equals(type) || "REST_DAY".equals(type)) continue;
            count++;
        }
        return count;
    }

    @Override
    public RESP getByMonth(String yearMonth) {
        return RESP.ok(salaryDao.selectByMonth(yearMonth));
    }

    @Override
    public RESP getMySalary(int empId, String yearMonth) {
        SalaryDetail detail = salaryDao.selectByEmpAndMonth(empId, yearMonth);
        if (detail == null) return RESP.error("暂未核算");
        return RESP.ok(detail);
    }
}
