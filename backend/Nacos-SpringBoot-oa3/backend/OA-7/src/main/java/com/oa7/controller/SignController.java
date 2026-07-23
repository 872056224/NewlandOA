package com.oa7.controller;

import com.oa7.dao.AttendanceDao;
import com.oa7.pojo.Admin;
import com.oa7.service.SignService;
import com.oa7.pojo.Sign;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.DU;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员端 - 考勤管理控制器
 */
@RestController
@RequestMapping("/attendance")
@CrossOrigin
public class SignController {

    @Autowired
    private SignService signService;

    @Autowired
    private AttendanceDao attendanceDao;

    /**
     * 今日考勤统计概览
     */
    @GetMapping("/today/stats")
    public RESP todayStats(HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        boolean deptHead = admin != null && admin.isDeptHead();
        LocalDate today = LocalDate.now();

        Map<String, Object> stats = new LinkedHashMap<>();
        if (deptHead) {
            int deptId = admin.getDeptId();
            stats.put("total", attendanceDao.countByDateAndDept(today, deptId));
            stats.put("checkedIn", attendanceDao.countCheckedInByDateAndDept(today, deptId));
            stats.put("late", 0); // 简化处理
            stats.put("leave", attendanceDao.countLeaveByDateAndDept(today, deptId));
            stats.put("absence", attendanceDao.countAbsenceByDateAndDept(today, deptId));
        } else {
            stats.put("total", attendanceDao.countByDate(today));
            stats.put("checkedIn", attendanceDao.countCheckedInByDate(today));
            stats.put("late", attendanceDao.countLateByDate(today));
            stats.put("leave", attendanceDao.countLeaveByDate(today));
            stats.put("absence", attendanceDao.countAbsenceByDate(today));
        }
        return RESP.ok(stats);
    }

    /**
     * Task 5: 今日实时统计（含未签到人数）
     */
    @GetMapping("/today/realtime-stats")
    public RESP todayRealtimeStats(HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        boolean deptHead = admin != null && admin.isDeptHead();
        LocalDate today = LocalDate.now();

        int total, checkedIn, notCheckedIn, late, leave, absence;
        if (deptHead) {
            int deptId = admin.getDeptId();
            total = attendanceDao.countByDateAndDept(today, deptId);
            checkedIn = attendanceDao.countCheckedInByDateAndDept(today, deptId);
            notCheckedIn = attendanceDao.countAbsenceByDateAndDept(today, deptId);
            late = 0;
            leave = attendanceDao.countLeaveByDateAndDept(today, deptId);
            absence = attendanceDao.countAbsenceByDateAndDept(today, deptId);
        } else {
            total = attendanceDao.countByDate(today);
            checkedIn = attendanceDao.countCheckedInByDate(today);
            notCheckedIn = attendanceDao.countNotCheckedInByDate(today);
            late = attendanceDao.countLateByDate(today);
            leave = attendanceDao.countLeaveByDate(today);
            absence = attendanceDao.countAbsenceByDate(today);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("checkedIn", checkedIn);
        stats.put("notCheckedIn", notCheckedIn);
        stats.put("late", late);
        stats.put("onLeave", leave);
        stats.put("absence", absence);
        stats.put("date", today.toString());
        return RESP.ok(stats);
    }

    /**
     * Task 6: 昨日考勤结算统计
     */
    @GetMapping("/yesterday/stats")
    public RESP yesterdayStats(HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        boolean deptHead = admin != null && admin.isDeptHead();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("date", yesterday.toString());

        if (deptHead) {
            int deptId = admin.getDeptId();
            stats.put("total", attendanceDao.countByDateAndDept(yesterday, deptId));
            stats.put("checkedIn", attendanceDao.countCheckedInByDateAndDept(yesterday, deptId));
            stats.put("leave", attendanceDao.countLeaveByDateAndDept(yesterday, deptId));
            stats.put("absence", attendanceDao.countAbsenceByDateAndDept(yesterday, deptId));
        } else {
            List<Map<String, Object>> statusCounts = attendanceDao.countGroupByStatus(yesterday);
            for (Map<String, Object> row : statusCounts) {
                stats.put(String.valueOf(row.get("attendance_status")), row.get("cnt"));
            }
        }
        return RESP.ok(stats);
    }

    @GetMapping("/today/signed")
    public RESP todaySigned(@RequestParam int currentPage,
                            @RequestParam int pageSize) {
        return signService.todaySigned(currentPage, pageSize);
    }

    @GetMapping("/daily-statistics")
    public RESP dailyStatistics(@RequestParam int currentPage,
                                @RequestParam int pageSize,
                                HttpSession session) {
        return signService.dailyStatistics(currentPage, pageSize, session);
    }

    @GetMapping("/daily-details")
    public RESP dailyDetails(@RequestParam String date, HttpSession session) {
        return signService.dailyDetails(date, session);
    }

    @GetMapping("/statistics/chart")
    public RESP chartData(HttpSession session) {
        return signService.chartData(session);
    }

    @GetMapping("/unsigned")
    public RESP unsigned(@RequestParam int currentPage,
                         @RequestParam int pageSize) {
        return signService.unsigned(currentPage, pageSize);
    }

    @GetMapping("/today/unsigned")
    public RESP todayUnsigned(@RequestParam int currentPage,
                              @RequestParam int pageSize) {
        return signService.todayUnsigned(currentPage, pageSize);
    }

    @PutMapping("/{id}/approve")
    public String approve(@PathVariable int id) {
        return signService.approve(id);
    }
}
