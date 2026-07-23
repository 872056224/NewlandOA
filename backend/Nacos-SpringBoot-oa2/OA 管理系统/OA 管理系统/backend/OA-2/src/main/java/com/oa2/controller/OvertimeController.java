package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.service.OvertimeService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/attendance/overtime")
@CrossOrigin
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @PostMapping("/apply")
    public RESP apply(@RequestParam String overtimeDate,
                      @RequestParam String startTime,
                      @RequestParam String endTime,
                      @RequestParam(defaultValue = "") String reason,
                      HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return overtimeService.apply(emp.getNumber(), overtimeDate, startTime, endTime, reason);
    }

    @GetMapping("/my-list")
    public RESP myList(@RequestParam(defaultValue = "1") int currentPage,
                       @RequestParam(defaultValue = "10") int pageSize,
                       HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return overtimeService.getMyList(emp.getNumber(), currentPage, pageSize);
    }

    @GetMapping("/monthly-hours")
    public RESP monthlyHours(@RequestParam(defaultValue = "") String yearMonth,
                             HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        if (yearMonth.isEmpty()) {
            java.time.YearMonth ym = java.time.YearMonth.now();
            yearMonth = ym.toString();
        }
        return overtimeService.getMonthlyHours(emp.getNumber(), yearMonth);
    }
}
