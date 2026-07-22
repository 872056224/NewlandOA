package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.service.AttendanceService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/attendance")
@CrossOrigin
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /** 签到 */
    @PostMapping("/check-in")
    public RESP checkIn(
            @RequestParam(value = "coordinates", required = false) String coordinates,
            HttpSession session, HttpServletRequest request) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        String clientIp = getClientIp(request);
        return attendanceService.checkIn(emp.getNumber(), coordinates, clientIp);
    }

    /** 签退 */
    @PostMapping("/check-out")
    public RESP checkOut(
            @RequestParam(value = "coordinates", required = false) String coordinates,
            HttpSession session, HttpServletRequest request) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        String clientIp = getClientIp(request);
        return attendanceService.checkOut(emp.getNumber(), coordinates, clientIp);
    }

    /** 今日考勤状态 */
    @GetMapping("/today")
    public RESP today(HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return attendanceService.getTodayStatus(emp.getNumber());
    }

    /** 历史考勤记录 */
    @GetMapping("/history")
    public RESP history(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize,
                        HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return attendanceService.getHistory(emp.getNumber(), currentPage, pageSize);
    }

    /** 旧接口兼容：my-records → attendance history */
    @GetMapping("/my-records")
    public RESP myRecords(HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return attendanceService.getHistory(emp.getNumber(), 1, 9999);
    }

    @GetMapping("/my-records/page")
    public RESP myRecordsPage(@RequestParam(defaultValue = "1") int currentPage,
                              @RequestParam(defaultValue = "10") int pageSize,
                              HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return attendanceService.getHistory(emp.getNumber(), currentPage, pageSize);
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            return request.getRemoteAddr();
        }
        return clientIp.split(",")[0].trim();
    }
}
