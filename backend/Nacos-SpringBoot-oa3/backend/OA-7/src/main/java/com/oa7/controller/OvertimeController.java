package com.oa7.controller;

import com.oa7.service.OvertimeService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

@RestController
@RequestMapping("/attendance/overtime")
@CrossOrigin
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @GetMapping("/pending")
    public RESP pending(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize,
                        HttpSession session) {
        return overtimeService.getPending(currentPage, pageSize, session);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable int id,
                        @RequestParam(required = false) BigDecimal actualHours,
                        HttpSession session) {
        return overtimeService.approve(id, actualHours, session);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable int id,
                       @RequestParam(required = false) String reason,
                       HttpSession session) {
        return overtimeService.reject(id, reason, session);
    }
}
