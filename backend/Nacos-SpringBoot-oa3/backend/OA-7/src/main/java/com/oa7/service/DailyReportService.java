package com.oa7.service;

import com.oa7.dao.AttendanceDao;
import com.oa7.dao.DailyReportDao;
import com.oa7.pojo.DailyReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 日报统计服务
 * - 每天 01:00 执行
 * - 统计前一天的考勤数据并生成日报
 */
@Configuration
@EnableScheduling
public class DailyReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyReportService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private DailyReportDao dailyReportDao;

    /**
     * 每天 01:00 生成前一天的日报统计
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成日报统计，日期: {}", yesterday);

        try {
            // 1. 查询考勤总人数
            int totalEmployees = attendanceDao.countByDate(yesterday);
            if (totalEmployees == 0) {
                log.warn("昨日无考勤记录，跳过日报生成，日期: {}", yesterday);
                return;
            }

            // 2. 按考勤状态分组统计
            List<Map<String, Object>> statusCounts = attendanceDao.countGroupByStatus(yesterday);

            int normalCount = 0;
            int lateCount = 0;
            int earlyCount = 0;
            int lateEarlyCount = 0;
            int leaveCount = 0;
            int absenceCount = 0;
            int missingCardCount = 0;
            int holidayCount = 0;

            for (Map<String, Object> row : statusCounts) {
                String status = (String) row.get("attendance_status");
                Number cnt = (Number) row.get("cnt");
                int count = cnt != null ? cnt.intValue() : 0;

                if (status == null) continue;

                switch (status) {
                    case "NORMAL":
                        normalCount = count;
                        break;
                    case "LATE":
                        lateCount = count;
                        break;
                    case "EARLY":
                        earlyCount = count;
                        break;
                    case "LATE_EARLY":
                        lateEarlyCount = count;
                        break;
                    case "LEAVE":
                        leaveCount = count;
                        break;
                    case "ABSENCE":
                        absenceCount = count;
                        break;
                    case "MISSING_CARD":
                        missingCardCount = count;
                        break;
                    case "HOLIDAY":
                        holidayCount = count;
                        break;
                    default:
                        log.debug("忽略未知考勤状态: {}", status);
                        break;
                }
            }

            // 3. 计算出勤率: normal_count / (total - holiday_count) * 100
            BigDecimal attendanceRate;
            int denominator = totalEmployees - holidayCount;
            if (denominator <= 0) {
                attendanceRate = BigDecimal.ZERO;
            } else {
                attendanceRate = BigDecimal.valueOf(normalCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
            }

            // 4. 构建日报对象并插入
            DailyReport report = new DailyReport();
            report.setReportDate(yesterday);
            report.setTotalEmployees(totalEmployees);
            report.setNormalCount(normalCount);
            report.setLateCount(lateCount);
            report.setEarlyCount(earlyCount);
            report.setLateEarlyCount(lateEarlyCount);
            report.setLeaveCount(leaveCount);
            report.setAbsenceCount(absenceCount);
            report.setMissingCardCount(missingCardCount);
            report.setHolidayCount(holidayCount);
            report.setAttendanceRate(attendanceRate);

            dailyReportDao.insertOrUpdate(report);

            log.info("日报统计生成完成，日期: {}, 总人数: {}, 正常: {}, 迟到: {}, 早退: {}, 迟到早退: {}, " +
                            "请假: {}, 旷工: {}, 缺卡: {}, 节假日: {}, 出勤率: {}%",
                    yesterday, totalEmployees, normalCount, lateCount, earlyCount, lateEarlyCount,
                    leaveCount, absenceCount, missingCardCount, holidayCount, attendanceRate);

        } catch (Exception e) {
            log.error("生成日报统计异常，日期: {}", yesterday, e);
        }
    }
}
