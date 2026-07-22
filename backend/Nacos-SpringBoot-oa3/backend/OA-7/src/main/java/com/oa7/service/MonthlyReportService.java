package com.oa7.service;

import com.oa7.dao.AttendanceDao;
import com.oa7.dao.EmpDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.MonthlyReportDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.Emp;
import com.oa7.pojo.Holiday;
import com.oa7.pojo.MonthlyReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 月度考勤统计服务
 * - 每月1日 06:00 自动执行
 * - 统计上个月的考勤数据并按员工生成月度报告
 */
@Configuration
@EnableScheduling
public class MonthlyReportService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportService.class);

    @Autowired
    private MonthlyReportDao monthlyReportDao;

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private HolidayDao holidayDao;

    /**
     * 每月1日 06:00 生成上个月的月度考勤统计
     */
    @Scheduled(cron = "0 0 6 1 * ?")
    public void scheduledGenerate() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        log.info("定时任务: 开始生成月度考勤统计，月份: {}", lastMonth);
        generate(lastMonth.getYear(), lastMonth.getMonthValue());
    }

    /**
     * 生成指定年月的月度考勤统计
     *
     * @param year  年份
     * @param month 月份
     */
    public void generate(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        String yearMonthStr = yearMonth.toString(); // format: YYYY-MM
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        log.info("开始生成月度考勤统计: {}", yearMonthStr);

        // 1. 查询该月份的WORKDAY天数
        List<Holiday> holidays = holidayDao.selectByDateRange(monthStart, monthEnd);
        int workDays = (int) holidays.stream()
                .filter(h -> "WORKDAY".equals(h.getType()))
                .count();

        // 如果holiday表中没有workday数据，则按自然月工作日计算（减去周六日）
        if (workDays == 0) {
            workDays = countWeekdays(yearMonth);
        }

        // 2. 获取所有员工
        List<Integer> empNumbers = empDao.selectAllEmpNumber();

        int processedCount = 0;
        for (Integer empId : empNumbers) {
            try {
                // 3. 获取员工详细信息
                Emp emp = empDao.selectByEmpNumber(empId);
                if (emp == null) {
                    log.warn("员工不存在，编号: {}", empId);
                    continue;
                }

                // 4. 查询该员工当月的考勤记录
                List<Attendance> records = attendanceDao.selectByEmpAndDateRange(empId, monthStart, monthEnd);

                // 5. 统计各项考勤状态
                int normalCount = 0;
                int lateCount = 0;
                int earlyCount = 0;
                int leaveCount = 0;
                int absenceCount = 0;
                int missingCardCount = 0;

                for (Attendance record : records) {
                    if (record.getAttendanceStatus() == null) continue;
                    switch (record.getAttendanceStatus().name()) {
                        case "NORMAL":
                            normalCount++;
                            break;
                        case "LATE":
                            lateCount++;
                            break;
                        case "EARLY":
                            earlyCount++;
                            break;
                        case "LATE_EARLY":
                            lateCount++;
                            earlyCount++;
                            break;
                        case "LEAVE":
                            leaveCount++;
                            break;
                        case "ABSENCE":
                            absenceCount++;
                            break;
                        case "MISSING_CARD":
                            missingCardCount++;
                            break;
                        default:
                            break;
                    }
                }

                // 6. 计算实际出勤天数 (NORMAL + LATE + EARLY + LATE_EARLY 都算实际出勤)
                int actualDays = normalCount + lateCount + earlyCount;

                // 7. 计算出勤率
                BigDecimal attendanceRate;
                if (workDays <= 0) {
                    attendanceRate = BigDecimal.ZERO;
                } else {
                    attendanceRate = BigDecimal.valueOf(actualDays)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP);
                }

                // 8. 构建月度报告并插入
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

                monthlyReportDao.insertOrUpdate(report);
                processedCount++;

            } catch (Exception e) {
                log.error("生成员工月度统计异常，员工编号: {}, 月份: {}", empId, yearMonthStr, e);
            }
        }

        log.info("月度考勤统计生成完成，月份: {}, 处理员工数: {}/{}", yearMonthStr, processedCount, empNumbers.size());
    }

    /**
     * 计算自然月中的工作日天数（周一到周五）
     */
    private int countWeekdays(YearMonth yearMonth) {
        int count = 0;
        LocalDate date = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek().getValue() <= 5) { // Monday to Friday
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }
}
