package com.oa7.service;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.HolidayType;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 考勤重算服务 — 单一事实来源
 *
 * 所有审批通过、审批撤销、补卡/补签成功后必须调用此服务。
 * 按状态优先级确定最终考勤状态。
 */
@Service
public class RecalculateAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(RecalculateAttendanceService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private HolidayDao holidayDao;

    @Autowired
    private LeaveDao leaveDao;

    /** 默认上班时间 09:00 */
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    /** 默认下班时间 18:00 */
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);

    /**
     * 对指定员工+单日进行考勤重算
     * @return 计算后的最终状态，如果无记录返回 null
     */
    public AttendanceStatus recalculate(int empId, LocalDate date) {
        Attendance att = attendanceDao.selectByEmpAndDate(empId, date);
        if (att == null) {
            log.warn("考勤重算：员工 {} 在 {} 无考勤记录，跳过", empId, date);
            return null;
        }

        // 查询 Holiday 类型
        String holidayTypeStr = holidayDao.selectHolidayTypeByDate(date);
        HolidayType holidayType = null;
        if (holidayTypeStr != null) {
            try {
                holidayType = HolidayType.valueOf(holidayTypeStr);
            } catch (IllegalArgumentException e) {
                log.warn("无法识别的节假日类型: date={}, type={}", date, holidayTypeStr);
            }
        }

        AttendanceStatus finalStatus = determineFinalStatus(att, holidayType, empId, date);

        // 更新数据库 — 传枚举值，由 MyBatisEnumTypeHandler 转换为字符串
        attendanceDao.updateAttendanceStatus(att.getId(), finalStatus);
        log.debug("考勤重算：员工 {} 日期 {} 状态 => {}", empId, date, finalStatus);

        return finalStatus;
    }

    /**
     * 对指定员工+日期范围进行考勤重算
     * @return 日期 → 最终状态的映射
     */
    public Map<LocalDate, AttendanceStatus> recalculate(int empId, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, AttendanceStatus> results = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            AttendanceStatus status = recalculate(empId, date);
            results.put(date, status);
        }
        return results;
    }

    /**
     * 按状态优先级确定最终状态
     *
     * 优先级（高→低）：
     * HOLIDAY > REST_DAY > LEAVE > DAY_OFF
     * > NORMAL > LATE > EARLY > LATE_EARLY > MISSING_CARD > ABSENCE
     *
     * BUSINESS_TRIP 和 FIELD_WORK 将在 Phase 5 实现
     */
    private AttendanceStatus determineFinalStatus(Attendance att, HolidayType holidayType,
                                                   int empId, LocalDate date) {
        // 1. 节假日类型优先
        if (holidayType == HolidayType.HOLIDAY) {
            return AttendanceStatus.HOLIDAY;
        }
        if (holidayType == HolidayType.REST_DAY) {
            return AttendanceStatus.REST_DAY;
        }

        // 2. 检查 today_status
        TodayStatus todayStatus = att.getTodayStatus();

        // 3. 查询当天是否有已批准的请假（通过 leave 表）
        boolean hasApprovedLeave = checkApprovedLeave(empId, date);
        if (hasApprovedLeave || todayStatus == TodayStatus.LEAVE) {
            return AttendanceStatus.LEAVE;
        }

        // 4. 调休
        if (todayStatus == TodayStatus.DAY_OFF) {
            return AttendanceStatus.DAY_OFF;
        }

        // 5. 正常签到签退判断
        if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
            LocalTime checkIn = att.getCheckInTime().toLocalTime();
            LocalTime checkOut = att.getCheckOutTime().toLocalTime();

            boolean late = checkIn.isAfter(DEFAULT_START_TIME);
            boolean early = checkOut.isBefore(DEFAULT_END_TIME);

            if (late && early) return AttendanceStatus.LATE_EARLY;
            if (late) return AttendanceStatus.LATE;
            if (early) return AttendanceStatus.EARLY;
            return AttendanceStatus.NORMAL;
        }

        // 6. 仅签到（未签退）→ 缺卡
        if (att.getCheckInTime() != null && att.getCheckOutTime() == null) {
            return AttendanceStatus.MISSING_CARD;
        }

        // 7. 仅签退（未签到）→ 旷工
        if (att.getCheckInTime() == null && att.getCheckOutTime() != null) {
            return AttendanceStatus.ABSENCE;
        }

        // 8. 未签到 → 旷工
        return AttendanceStatus.ABSENCE;
    }

    /**
     * 检查员工当天是否有已批准的请假
     */
    private boolean checkApprovedLeave(int empId, LocalDate date) {
        try {
            int count = leaveDao.countApprovedLeaveToday(empId, date.toString());
            return count > 0;
        } catch (Exception e) {
            log.warn("查询请假状态失败: empId={}, date={}", empId, date, e);
            return false;
        }
    }
}
