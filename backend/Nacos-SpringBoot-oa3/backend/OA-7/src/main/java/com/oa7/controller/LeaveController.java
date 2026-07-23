package com.oa7.controller;

import com.oa7.service.LeaveService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/pending")
    public RESP pending(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize,
                        HttpSession session) {
        return leaveService.getPending(currentPage, pageSize, session);
    }

    @GetMapping("/list")
    public RESP list(@RequestParam String status,
                     @RequestParam(defaultValue = "1") int currentPage,
                     @RequestParam(defaultValue = "10") int pageSize,
                     HttpSession session) {
        return leaveService.getByStatus(status, currentPage, pageSize, session);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable String id, HttpSession session) {
        return leaveService.approve(id, session);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable String id, HttpSession session) {
        return leaveService.reject(id, session);
    }

    @PutMapping("/{id}/revoke")
    public RESP revoke(@PathVariable String id, HttpSession session) {
        return leaveService.revoke(id, session);
    }
}
