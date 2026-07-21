package com.oa7.controller;

import com.oa7.service.LeaveService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/pending")
    public RESP pending(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize) {
        return leaveService.getPending(currentPage, pageSize);
    }

    @GetMapping("/list")
    public RESP list(@RequestParam String status,
                     @RequestParam(defaultValue = "1") int currentPage,
                     @RequestParam(defaultValue = "10") int pageSize) {
        return leaveService.getByStatus(status, currentPage, pageSize);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable String id) {
        return leaveService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable String id) {
        return leaveService.reject(id);
    }
}
