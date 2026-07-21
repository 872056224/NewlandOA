package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.service.RetroactiveSignService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/attendance/retroactive")
@CrossOrigin
public class RetroactiveSignController {

    @Autowired
    private RetroactiveSignService retroactiveSignService;

    @PostMapping("/apply")
    public RESP apply(@RequestBody RetroactiveRequest request, HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return retroactiveSignService.apply(emp.getNumber(), request.getSignDate(), request.getType(), request.getReason());
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
        return retroactiveSignService.getMyList(emp.getNumber(), currentPage, pageSize);
    }

    public static class RetroactiveRequest {
        private String signDate;
        private String type;
        private String reason;

        public String getSignDate() { return signDate; }
        public void setSignDate(String signDate) { this.signDate = signDate; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
