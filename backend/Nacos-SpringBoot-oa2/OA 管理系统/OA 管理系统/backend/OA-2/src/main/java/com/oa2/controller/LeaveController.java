package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.service.LeaveService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public RESP apply(@RequestBody LeaveRequest request, HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return leaveService.apply(
                emp.getNumber(),
                emp.getName(),
                emp.getDept_name(),
                request.getType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getReason()
        );
    }

    @GetMapping("/my-list")
    public RESP getMyList(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return leaveService.getMyList(emp.getNumber(), currentPage, pageSize);
    }

    @GetMapping("/today-status")
    public RESP getTodayStatus(HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return leaveService.getTodayStatus(emp.getNumber());
    }

    // 内部请求体类
    public static class LeaveRequest {
        private String type;
        private String startDate;
        private String endDate;
        private String reason;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
