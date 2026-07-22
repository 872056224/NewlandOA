package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.service.MakeupRequestService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/makeup")
@CrossOrigin
public class MakeupRequestController {

    @Autowired
    private MakeupRequestService makeupRequestService;

    /** 提交补卡申请 */
    @PostMapping("/apply")
    public RESP apply(@RequestBody MakeupRequest request, HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return makeupRequestService.apply(emp.getNumber(), request.getDate(),
                request.getType(), request.getRequestTime(), request.getReason());
    }

    /** 我的补卡记录 */
    @GetMapping("/my-list")
    public RESP myList(@RequestParam(defaultValue = "1") int currentPage,
                       @RequestParam(defaultValue = "10") int pageSize,
                       HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return makeupRequestService.getMyList(emp.getNumber(), currentPage, pageSize);
    }

    public static class MakeupRequest {
        private String date;
        private String type;
        private String requestTime;
        private String reason;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRequestTime() { return requestTime; }
        public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
