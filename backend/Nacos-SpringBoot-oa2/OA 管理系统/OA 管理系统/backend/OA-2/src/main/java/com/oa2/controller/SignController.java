package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.pojo.Sign;
import com.oa2.service.SignService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/attendance")
@CrossOrigin
public class SignController {

    @Autowired
    private SignService signService;

    @GetMapping("/my-records/page")
    public RESP getMyRecordsPage(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return signService.getMyRecordsPage(emp.getNumber(), currentPage, pageSize);
    }

    @GetMapping("/my-records")
    public RESP getMyRecords(HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return signService.getMyRecords(emp.getNumber());
    }

    @PostMapping("/check-in")
    public RESP checkIn(
            @RequestBody Sign sign,
            @RequestParam(value = "coordinates", required = false) String coordinates,
            HttpSession session,
            HttpServletRequest request) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        sign.setNumber(emp.getNumber());
        // 获取客户端真实IP（经过网关时取 X-Forwarded-For）
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            // X-Forwarded-For 可能包含多个IP，取第一个
            clientIp = clientIp.split(",")[0].trim();
        }
        return signService.checkIn(sign, coordinates, clientIp);
    }
}
