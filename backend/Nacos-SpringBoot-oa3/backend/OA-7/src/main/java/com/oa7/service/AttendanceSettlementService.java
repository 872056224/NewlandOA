package com.oa7.service;

import com.oa7.dao.AttendanceDao;
import com.oa7.pojo.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

/**
 * 日终考勤结算服务
 * - 每天 23:59 执行
 * - 委托 RecalculateAttendanceService 进行状态重算
 */
@Configuration
@EnableScheduling
public class AttendanceSettlementService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSettlementService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private RecalculateAttendanceService recalculateService;

    /**
     * 每天 23:59 执行考勤结算
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void settleTodayAttendance() {
        LocalDate today = LocalDate.now();
        log.info("开始执行日终考勤结算，日期: {}", today);

        try {
            List<Attendance> records = attendanceDao.selectByDate(today);
            if (records.isEmpty()) {
                log.info("当天无考勤记录，跳过结算");
                return;
            }

            int updatedCount = 0;
            for (Attendance record : records) {
                recalculateService.recalculate(record.getEmpId(), today);
                updatedCount++;
            }

            log.info("日终考勤结算完成，共处理 {} 条记录，日期: {}", updatedCount, today);
        } catch (Exception e) {
            log.error("日终考勤结算异常，日期: {}", today, e);
        }
    }
}
