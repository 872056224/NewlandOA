package com.oa7.controller;

import com.oa7.service.MakeupRequestService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/makeup")
@CrossOrigin
public class MakeupRequestController {

    @Autowired
    private MakeupRequestService makeupRequestService;

    @GetMapping("/pending")
    public RESP pending(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize) {
        return makeupRequestService.getPending(currentPage, pageSize);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable int id) {
        return makeupRequestService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable int id) {
        return makeupRequestService.reject(id);
    }
}
