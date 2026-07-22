package com.oa7.service;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.HolidayType;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.HolidayDao;
import com.oa7.pojo.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 日终考勤结算服务
 * - 每天 23:59 执行
 * - 对当天的每条考勤记录按规则计算 attendance_status
 */
@Configuration
@EnableScheduling
public class AttendanceSettlementService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSettlementService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private HolidayDao holidayDao;

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

            String holidayType = holidayDao.selectHolidayTypeByDate(today);

            int updatedCount = 0;
            for (Attendance record : records) {
                AttendanceStatus settlementStatus = determineStatus(record, holidayType);
                attendanceDao.updateAttendanceStatus(record.getId(), settlementStatus);
                updatedCount++;
            }

            log.info("日终考勤结算完成，共处理 {} 条记录，日期: {}", updatedCount, today);
        } catch (Exception e) {
            log.error("日终考勤结算异常，日期: {}", today, e);
        }
    }

    /**
     * 根据结算规则确定考勤状态（优先级从高到低）
     *
     * 规则顺序：
     * 1. holiday today       -> HOLIDAY
     * 2. rest day today      -> REST_DAY
     * 3. today_status=LEAVE  -> LEAVE
     * 4. today_status=DAY_OFF -> DAY_OFF
     * 5. 有 check_in 和 check_out：
     *    - check_in > 08:30 && check_out >= 17:30 -> LATE
     *    - check_in <= 08:30 && check_out < 17:30 -> EARLY
     *    - check_in > 08:30 && check_out < 17:30  -> LATE_EARLY
     *    - else                                   -> NORMAL
     * 6. 只有 check_in（未签退）                   -> MISSING_CARD
     * 7. 只有 check_out（未签到）                   -> ABSENCE
     * 8. NOT_CHECKED_IN                            -> ABSENCE
     */
    private AttendanceStatus determineStatus(Attendance record, String holidayType) {
        HolidayType hType = holidayType != null ? HolidayType.valueOf(holidayType) : null;
        if (HolidayType.HOLIDAY == hType) {
            return AttendanceStatus.HOLIDAY;
        }
        if (HolidayType.REST_DAY == hType) {
            return AttendanceStatus.REST_DAY;
        }

        TodayStatus todayStatus = record.getTodayStatus();
        if (TodayStatus.LEAVE == todayStatus) {
            return AttendanceStatus.LEAVE;
        }
        if (TodayStatus.DAY_OFF == todayStatus) {
            return AttendanceStatus.DAY_OFF;
        }

        // 同时有签到和签退 -> 判定迟到早退
        if (record.getCheckInTime() != null && record.getCheckOutTime() != null) {
            LocalTime checkIn = record.getCheckInTime().toLocalTime();
            LocalTime checkOut = record.getCheckOutTime().toLocalTime();

            LocalTime eightThirty = LocalTime.of(8, 30);
            LocalTime fiveThirty = LocalTime.of(17, 30);

            boolean late = checkIn.isAfter(eightThirty);
            boolean early = checkOut.isBefore(fiveThirty);

            if (late && early) return AttendanceStatus.LATE_EARLY;
            if (late) return AttendanceStatus.LATE;
            if (early) return AttendanceStatus.EARLY;
            return AttendanceStatus.NORMAL;
        }

        // 只有签到（未签退）-> 缺卡
        if (record.getCheckInTime() != null && record.getCheckOutTime() == null) {
            return AttendanceStatus.MISSING_CARD;
        }

        // 只有签退（未签到）-> 旷工
        if (record.getCheckInTime() == null && record.getCheckOutTime() != null) {
            return AttendanceStatus.ABSENCE;
        }

        // 未签到 -> 旷工
        return AttendanceStatus.ABSENCE;
    }
}
