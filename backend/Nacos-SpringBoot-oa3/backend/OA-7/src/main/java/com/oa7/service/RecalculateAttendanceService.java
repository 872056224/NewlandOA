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
            if (rule != null && rule.getWorkStartTime() != null) {
                return rule.getWorkStartTime();
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
            if (rule != null && rule.getWorkEndTime() != null) {
                return rule.getWorkEndTime();
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

        // 更新数据库 — 传枚举值，由 MyBatisEnumTypeHandler 转换为字符串
        attendanceDao.updateAttendanceStatus(att.getId(), finalStatus);

        // 计算并更新缺时时长（核心工作时间 09:00-18:00 未覆盖部分, 扣30分钟容差）
        int missingMin = computeMissingDuration(att);
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
    /** 容差分钟数 */
    private static final long TOLERANCE_MINUTES = 30;

    /**
     * 计算缺时时长：核心工作时间 09:00-18:00 内未覆盖的部分
     * 迟到/早退各扣30分钟容差，总和仍为正则记录
     */
    private int computeMissingDuration(Attendance att) {
        if (att.getCheckInTime() == null || att.getCheckOutTime() == null) {
            return 0;
        }
        LocalTime checkIn = att.getCheckInTime().toLocalTime();
        LocalTime checkOut = att.getCheckOutTime().toLocalTime();

        long missing = 0;
        if (checkIn.isAfter(CORE_START)) {
            missing += java.time.Duration.between(CORE_START, checkIn).toMinutes();
        }
        if (checkOut.isBefore(CORE_END)) {
            missing += java.time.Duration.between(checkOut, CORE_END).toMinutes();
        }
        // 扣减容差
        missing = Math.max(0, missing - TOLERANCE_MINUTES);
        return (int) Math.min(missing, Integer.MAX_VALUE);
    }
}
