package com.oa7.service;

import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.EmpDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日凌晨自动创建考勤记录
 *
 * 00:05 执行，为所有员工创建当天的 Attendance 记录。
 * 提前判断节假日和已批准的请假，设置对应的初始状态。
 */
@Configuration
@EnableScheduling
public class AutoCreateAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AutoCreateAttendanceService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private HolidayDao holidayDao;

    @Autowired
    private LeaveDao leaveDao;

    @Scheduled(cron = "0 5 0 * * ?")  // 00:05 执行（留一点时间余量）
    public void createDailyAttendance() {
        LocalDate today = LocalDate.now();
        log.info("开始创建今日考勤记录，日期: {}", today);

        try {
            // 获取所有员工
            List<Integer> empNumbers = empDao.selectAllActiveEmpNumbers();
            if (empNumbers.isEmpty()) {
                log.warn("无员工数据，跳过创建考勤记录");
                return;
            }

            // 查询 holiday
            String holidayType = holidayDao.selectHolidayTypeByDate(today);

            List<Attendance> batch = new ArrayList<>();

            for (int empId : empNumbers) {
                Attendance att = new Attendance();
                att.setEmpId(empId);
                att.setDate(today);

                if ("HOLIDAY".equals(holidayType)) {
                    att.setTodayStatus(TodayStatus.NOT_CHECKED_IN);
                } else if ("REST_DAY".equals(holidayType)) {
                    att.setTodayStatus(TodayStatus.NOT_CHECKED_IN);
                } else {
                    // 检查是否有已批准的请假
                    boolean hasLeave = leaveDao.countApprovedLeaveToday(empId, today.toString()) > 0;
                    if (hasLeave) {
                        att.setTodayStatus(TodayStatus.LEAVE);
                    } else {
                        att.setTodayStatus(TodayStatus.NOT_CHECKED_IN);
                    }
                }
                batch.add(att);
            }

            // 批量插入（使用 ON DUPLICATE KEY 避免重复）
            attendanceDao.batchInsertOrUpdate(batch);
            log.info("今日考勤记录创建完成，共 {} 条，日期: {}", batch.size(), today);

        } catch (Exception e) {
            log.error("创建今日考勤记录异常，日期: {}", today, e);
        }
    }
}
