package com.oa7.service;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.HolidayType;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.AttendanceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    @Autowired
    private AttendanceRuleService attendanceRuleService;

    /** 默认上班时间 09:00（当 AttendanceRule 不可用时回退） */
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    /** 默认下班时间 18:00 */
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);

    /**
     * 获取生效的上班时间 — 优先从 AttendanceRule 读取
     */
    private LocalTime getEffectiveStartTime() {
        try {
            AttendanceRule rule = attendanceRuleService.getDefaultRule();
            if (rule != null && rule.getWork_start_time() != null) {
                return rule.getWork_start_time();
            }
        } catch (Exception e) {
            log.debug("读取考勤规则失败，使用默认值", e);
        }
        return DEFAULT_START_TIME;
    }

    /**
     * 获取生效的下班时间
     */
    private LocalTime getEffectiveEndTime() {
        try {
            AttendanceRule rule = attendanceRuleService.getDefaultRule();
            if (rule != null && rule.getWork_end_time() != null) {
                return rule.getWork_end_time();
            }
        } catch (Exception e) {
            log.debug("读取考勤规则失败，使用默认值", e);
        }
        return DEFAULT_END_TIME;
    }

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

        // 更新数据库
        attendanceDao.updateAttendanceStatus(att.getId(), finalStatus);

        // 根据最终状态同步更新 today_status（前端签到记录页展示用）
        TodayStatus newTodayStatus = null;
        if (finalStatus == AttendanceStatus.LEAVE) {
            newTodayStatus = TodayStatus.LEAVE;
        } else if (finalStatus == AttendanceStatus.HOLIDAY) {
            newTodayStatus = TodayStatus.HOLIDAY;
        } else if (finalStatus == AttendanceStatus.REST_DAY) {
            newTodayStatus = TodayStatus.REST_DAY;
        } else if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
            newTodayStatus = TodayStatus.CHECKED_OUT;
        } else if (att.getCheckInTime() != null) {
            newTodayStatus = TodayStatus.CHECKED_IN;
        } else {
            newTodayStatus = TodayStatus.NOT_CHECKED_IN;
        }
        if (newTodayStatus != null) {
            att.setTodayStatus(newTodayStatus);
            attendanceDao.updateTodayStatusByEmpAndDate(empId, date, newTodayStatus);
        }

        // 计算并更新缺时时长
        // 请假/节假日/休息日 → 缺时为 0
        int missingMin = 0;
        if (finalStatus != AttendanceStatus.LEAVE
            && finalStatus != AttendanceStatus.HOLIDAY
            && finalStatus != AttendanceStatus.REST_DAY) {
            missingMin = computeMissingDuration(att);
        }
        attendanceDao.updateMissingDuration(att.getId(), missingMin);

        log.debug("考勤重算：员工 {} 日期 {} 状态 => {}, 缺时 {}min", empId, date, finalStatus, missingMin);

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

        // 5. 正常签到签退判断（使用 AttendanceRule 配置的时间）
        if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
            LocalTime checkIn = att.getCheckInTime().toLocalTime();
            LocalTime checkOut = att.getCheckOutTime().toLocalTime();

            LocalTime effectiveStart = getEffectiveStartTime();
            LocalTime effectiveEnd = getEffectiveEndTime();

            boolean late = checkIn.isAfter(effectiveStart);
            boolean early = checkOut.isBefore(effectiveEnd);

            if (late && early) return AttendanceStatus.LATE_EARLY;
            if (late) return AttendanceStatus.LATE;
            if (early) return AttendanceStatus.EARLY;
            return AttendanceStatus.NORMAL;
        }

        // 6. 仅签到（未签退）→ 缺卡
        if (att.getCheckInTime() != null && att.getCheckOutTime() == null) {
            return AttendanceStatus.MISSING_CARD;
        }

        // 7. 仅签退（未签到）→ 缺卡（与仅签到未签退同属 MISSING_CARD）
        if (att.getCheckInTime() == null && att.getCheckOutTime() != null) {
            return AttendanceStatus.MISSING_CARD;
        }

        // 8. 未签到 && 未签退 → 旷工
        // TODO Phase 2: 增加待审批补卡/补签检查（如有待审批申请则不应标记为旷工）
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

    /** 核心工作时间 09:00-18:00 */
    private static final LocalTime CORE_START = LocalTime.of(9, 0);
    private static final LocalTime CORE_END = LocalTime.of(18, 0);
    /** 缺时宽限默认值（分钟） */
    private static final int DEFAULT_TOLERANCE = 30;

    /**
     * 获取缺时宽限（从考勤规则读取）
     */
    private int getToleranceMinutes() {
        try {
            AttendanceRule rule = attendanceRuleService.getDefaultRule();
            if (rule != null && rule.getMissing_tolerance_min() != null) {
                return rule.getMissing_tolerance_min();
            }
        } catch (Exception e) {
            log.warn("读取缺时宽限失败，使用默认值", e);
        }
        return DEFAULT_TOLERANCE;
    }

    /**
     * 计算缺时时长
     *
     * 规则：
     * ① 缺时X > 宽限 → 全记入
     * ② 缺时X ≤ 宽限 AND 加班时长 > 宽限 → 全抵（记0）
     * ③ 缺时X ≤ 宽限 AND 加班时长 ≤ 宽限 → 记 max(0, 缺时X - 加班时长)
     */
    private int computeMissingDuration(Attendance att) {
        if (att.getCheckInTime() == null || att.getCheckOutTime() == null) {
            return 0;
        }
        LocalTime checkIn = att.getCheckInTime().toLocalTime();
        LocalTime checkOut = att.getCheckOutTime().toLocalTime();

        // 迟到+早退分钟
        long missingX = 0;
        if (checkIn.isAfter(CORE_START)) {
            missingX += Duration.between(CORE_START, checkIn).toMinutes();
        }
        if (checkOut.isBefore(CORE_END)) {
            missingX += Duration.between(checkOut, CORE_END).toMinutes();
        }

        // 加班时长 = 总工时 - 核心工时
        long totalMinutes = Duration.between(checkIn, checkOut).toMinutes();
        long coreMinutes = Duration.between(CORE_START, CORE_END).toMinutes(); // 540min
        long overtime = Math.max(0, totalMinutes - coreMinutes);

        int tolerance = getToleranceMinutes();

        // ① 超过宽限 → 全记
        if (missingX > tolerance) {
            return (int) Math.min(missingX, Integer.MAX_VALUE);
        }

        // ② 没超过宽限，但加班够多 → 全抵
        if (overtime > tolerance) {
            return 0;
        }

        // ③ 没超过宽限，加班也不够 → 抵多少算多少
        long result = Math.max(0, missingX - overtime);
        return (int) Math.min(result, Integer.MAX_VALUE);
    }
}
